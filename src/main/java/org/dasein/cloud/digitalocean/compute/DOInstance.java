/**
 * Copyright (C) 2009-2014 Dell, Inc.
 * See annotations for authorship information
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.digitalocean.compute;

import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.AbstractVMSupport;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.VMFilterOptions;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VMScalingOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.digitalocean.DOInstanceCapabilities;
import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.util.APITrace;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Megabyte;
import org.dasein.util.uom.storage.Storage;

import com.acentera.models.DigitalOceanModelFactory;
import com.acentera.models.digitalocean.Action;
import com.acentera.models.digitalocean.Droplet;
import com.acentera.models.digitalocean.Droplets;
import com.acentera.models.digitalocean.Size;
import com.acentera.models.digitalocean.Sizes;
import com.acentera.models.digitalocean.actions.droplet.Destroy;
import com.acentera.models.digitalocean.actions.droplet.Reboot;
import com.acentera.models.digitalocean.actions.droplet.Resize;
import com.acentera.models.digitalocean.actions.droplet.Start;
import com.acentera.models.digitalocean.actions.droplet.Stop;

public class DOInstance extends AbstractVMSupport<DigitalOcean> {
    static private final Logger logger = Logger.getLogger(DOInstance.class);
    static private final Calendar UTC_CALENDAR = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));

    private transient volatile DOInstanceCapabilities capabilities;

    public DOInstance(DigitalOcean provider) {
        super(provider);
    }

    @Override
    public VirtualMachine alterVirtualMachine(@Nonnull String vmId, @Nonnull VMScalingOptions options) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "alterVirtualMachine");
        
        try {
        	String newProductId = options.getProviderProductId();
        	if (newProductId == null) {
        		throw new CloudException("Product Id must not be empty");
        	}
        	

        	VirtualMachine vm = getVirtualMachine(vmId);
        	
        	if (!getCapabilities().canAlter(vm.getCurrentState())) {
        		throw new CloudException("Droplet is currently " + vm.getCurrentState() + ". Please power it off to run this event.");        		
        	}
        	
        	if (vm.getProductId().compareTo(newProductId) == 0) {
        		throw new CloudException("Product Id must differ from current vm product id");
        	}
        	
    		Resize action = new Resize(newProductId);            
            
            
            try {
            	Action evt = DigitalOceanModelFactory.performAction(getProvider(), action, vmId);
            	
            	//TODO: We should wait until the action complete???
            	String eventId = String.valueOf(evt.getId());            	
            	
            	waitForEventComplete(getProvider(), evt);            	
            	
            	vm = getVirtualMachine(vmId);
            	
            	return vm;
            } catch( CloudException e ) {
                logger.error(e.getMessage());
                throw new CloudException(e);
            } catch (UnsupportedEncodingException e) {
            	logger.error(e.getMessage());
                throw new CloudException(e);
			}
            
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void start(@Nonnull String instanceId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "startVM");
        try {
            VirtualMachine vm = getVirtualMachine(instanceId);

            if( vm == null ) {
                throw new CloudException("No such instance: " + instanceId);
            }
                        
            Start action = new Start();            
            
            
            try {
            	Action evt = DigitalOceanModelFactory.performAction(getProvider(), action, instanceId);
            } catch( CloudException e ) {
                logger.error(e.getMessage());
                throw new CloudException(e);
            } catch (UnsupportedEncodingException e) {
            	logger.error(e.getMessage());
                throw new CloudException(e);
			}
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull VirtualMachine clone(@Nonnull String vmId, @Nonnull String intoDcId, @Nonnull String name, @Nonnull String description, boolean powerOn, @Nullable String... firewallIds) throws InternalException, CloudException {
        throw new OperationNotSupportedException("DigitalOcean instances cannot be cloned.");
    }


    private Architecture getArchitecture(String size) throws OperationNotSupportedException {
    	throw new OperationNotSupportedException("Operation not yet implemented.");
        /*if( size.equals("m1.small") || size.equals("c1.medium") ) {
            return Architecture.I32;
        } else {
            return Architecture.I64;
        }*/
    }

    public @Nonnull DOInstanceCapabilities getCapabilities() {
        if( capabilities == null ) {
            capabilities = new DOInstanceCapabilities(getProvider());
        }
        return capabilities;
    }


    @Override
    public @Nullable VirtualMachine getVirtualMachine(@Nonnull String instanceId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "getVirtualMachine");
        try {
            ProviderContext ctx = getProvider().getContext();

            if( ctx == null ) {
                throw new CloudException("No context was established for this request");
            }
                        
            try {
            	//TODO: We should implement this into the DigitalOceanHelper... maybe would be cleaner?
            	Droplet d = (Droplet) DigitalOceanModelFactory.getModelById(getProvider(), com.acentera.models.DigitalOcean.DROPLET, instanceId);
            	if (d == null) {
            		throw new CloudException("No such instance found:" + instanceId);
            	}
            	
            	VirtualMachine server = toVirtualMachine(ctx, d);
            	if( server != null && server.getProviderVirtualMachineId().equals(instanceId) ) {
                    return server;
                }
            } catch( CloudException e ) {
                logger.error(e.getMessage());
                return null;
                //throw new CloudException(e);
            } catch (UnsupportedEncodingException e) {
            	logger.error(e.getMessage());
                //throw new CloudException(e);
			}                        
            return null;
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nullable VirtualMachineProduct getProduct(@Nonnull String sizeId) throws CloudException, InternalException {
        for( Architecture a : getCapabilities().listSupportedArchitectures() ) {
            for( VirtualMachineProduct prd : listProducts(a) ) {
                if( prd.getProviderProductId().equals(sizeId) ) {
                    return prd;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isSubscribed() throws InternalException, CloudException {
    	throw new OperationNotSupportedException("DigitalOcean instances cannot be subscribed.");
    }

    @Override
    public @Nonnull Iterable<VirtualMachineProduct> listProducts(Architecture architecture) throws InternalException, CloudException {
        ProviderContext ctx = getProvider().getContext();

        ArrayList<VirtualMachineProduct> list = new ArrayList<VirtualMachineProduct>();
        try {
        	Sizes availablesizes = (Sizes)DigitalOceanModelFactory.getModel(getProvider(), com.acentera.models.DigitalOcean.SIZES);        	
        	
            if (availablesizes != null) {
            
	            Set<Size> sizes = availablesizes.getSizes();
	            Iterator<Size> itr = sizes.iterator();
	            while(itr.hasNext()) {
	            	Size s = itr.next();
	            	VirtualMachineProduct vmp = toProduct(s);
	            	list.add(vmp);
	            }
            }

            return list;        	
        } catch (UnsupportedEncodingException e) {
        	 logger.error(e.getMessage());
             throw new CloudException(e);
		}
        
        
        //throw new OperationNotSupportedException("DigitalOcean listProduct not yet implemented.");       
        
    }
    

    @Override
    public @Nonnull VirtualMachine launch(@Nonnull VMLaunchOptions cfg) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "launchVM");
        try {
            ProviderContext ctx = getProvider().getContext();
            if( ctx == null ) {
                throw new CloudException("No context was established for this request");
            }
            MachineImage img = getProvider().getComputeServices().getImageSupport().getMachineImage(cfg.getMachineImageId());
            if( img == null ) {
                throw new InternalException("No such machine image: " + cfg.getMachineImageId());
            }
            
           
            String hostname = cfg.getHostName();
            if( hostname == null ) {
                throw new InternalException("No hostname defined  as part of launch options.");
            }
            
            String product = cfg.getStandardProductId();
            if( product == null ) {
                throw new InternalException("No product defined as part of launch options.");
            }
            
            
            String regionId = cfg.getDataCenterId();         
            if( regionId == null ) {
            	if (ctx.getRegionId() != null) {
            		regionId = ctx.getRegionId();
            	} else {
            		throw new InternalException("No region defined as part of launch options.");
            	}
            }
            
            
            Droplet droplet;
			try {				
				
				droplet = DigitalOceanModelFactory.createInstance(getProvider(), hostname, product, cfg.getMachineImageId(), regionId, cfg.getBootstrapKey(), null);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage());
                throw new CloudException(e);
			}
            
            VirtualMachine server = toVirtualMachine(getContext(), droplet);            

            return server;
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<ResourceStatus> listVirtualMachineStatus() throws InternalException, CloudException {
        APITrace.begin(getProvider(), "listVirtualMachineStatus");
        try {
            ProviderContext ctx = getProvider().getContext();

            if( ctx == null ) {
                throw new CloudException("No context was established for this request");
            }
            
            
            
            ArrayList<ResourceStatus> list = new ArrayList<ResourceStatus>();
            try {
            	
            	Droplets droplets = (Droplets)DigitalOceanModelFactory.getModel(getProvider(), com.acentera.models.DigitalOcean.DROPLETS );
            	if (droplets != null) {
	            	Set<Droplet> s = droplets.getDroplet();
	            	Iterator<Droplet> itr = s.iterator();
	            	while(itr.hasNext()) {
	            		Droplet d = itr.next();
	            		ResourceStatus status = toStatus(d);
	            		if( status != null ) {
	                        list.add(status);
	                    }
	            	}
            	}
            	
            } catch( Exception e ) {
                logger.error(e.getMessage());
                throw new CloudException(e);
            }
                        
            return list;
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<VirtualMachine> listVirtualMachines() throws InternalException, CloudException {
        return listVirtualMachinesWithParams(null, null);
    }

    @Override
    public @Nonnull Iterable<VirtualMachine> listVirtualMachines(@Nullable VMFilterOptions options) throws InternalException, CloudException {
        Map<String, String> tags = ( ( options == null || options.isMatchesAny() ) ? null : options.getTags() );

        if( tags != null ) {
            // tag advantage of EC2-based filtering if we can...
            Map<String, String> extraParameters = new HashMap<String, String>();

            String regex = options.getRegex();

            if( regex != null ) {
                // still have to match on regex
                options = VMFilterOptions.getInstance(false, regex);
            } else {
                // nothing else to match on
                options = null;
            }
            return listVirtualMachinesWithParams(extraParameters, options);
        } else {
            return listVirtualMachinesWithParams(null, options);
        }
    }

    private @Nonnull Iterable<VirtualMachine> listVirtualMachinesWithParams(Map<String, String> extraParameters, @Nullable VMFilterOptions options) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "listVirtualMachines");
        try {
            ProviderContext ctx = getProvider().getContext();

            if( ctx == null ) {
                throw new CloudException("No context was established for this request");
            }
            Iterable<IpAddress> addresses = Collections.emptyList();
            NetworkServices services = getProvider().getNetworkServices();

            if( services != null ) {
                if( services.hasIpAddressSupport() ) {
                    IpAddressSupport support = services.getIpAddressSupport();

                    if( support != null ) {
                        addresses = support.listIpPool(IPVersion.IPV4, false);
                    }
                }
            }

            ArrayList<VirtualMachine> list = new ArrayList<VirtualMachine>();


            try {

            	
            	Droplets droplets = (Droplets)DigitalOceanModelFactory.getModel(getProvider(), com.acentera.models.DigitalOcean.DROPLETS );
            	if (droplets != null) {
	            	Set<Droplet> s = droplets.getDroplet();
	            	Iterator<Droplet> itr = s.iterator();
	            	while(itr.hasNext()) {
	            		Droplet d = itr.next();
	            		VirtualMachine vm = toVirtualMachine(ctx, d);
	
	                    if( options == null || options.matches(vm) ) {
	                        list.add(vm);
	                    }
	                }
            	}
            	
            } catch (UnsupportedEncodingException e) {
            	logger.error(e.getMessage());
                throw new CloudException(e);
			}   
            
            return list;
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void pause(@Nonnull String vmId) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Pause/unpause not supported by the EC2 API");
    }

    @Override
    public void stop(@Nonnull String instanceId, boolean force) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "stopVM");
        try {
            VirtualMachine vm = getVirtualMachine(instanceId);

            if( vm == null ) {
                throw new CloudException("No such instance: " + instanceId);
            }
            
            	Stop action = new Stop();            
                try {
                	Action evt = DigitalOceanModelFactory.performAction(getProvider(), action, instanceId);                	
                } catch (UnsupportedEncodingException e) {
                	 logger.error(e.getMessage());
                     throw new CloudException(e);
				}
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void reboot(@Nonnull String instanceId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "rebootVM");
        try {
        	VirtualMachine vm = getVirtualMachine(instanceId);

            if( vm == null ) {
                throw new CloudException("No such instance: " + instanceId);
            }
            
            try {
            	Reboot action = new Reboot();            
                
                Action evt = DigitalOceanModelFactory.performAction(getProvider(), action, instanceId);
                
                waitForEventComplete(getProvider(), evt);
                /*                
                */

            	
            } catch (UnsupportedEncodingException e) {
            	 logger.error(e.getMessage());
                 throw new CloudException(e);
			}
        } finally {
            APITrace.end();
        }
    }

    private void waitForEventComplete(DigitalOcean provider, Action evt) {
    	//TODO: We should have a Switch as in some case user might now want to wait on this thread..... use a callback to get event id?
    	/*
    	while (!evt.isComplete()) {                	
            try {
                Thread.sleep(10000L);
            }
            catch (InterruptedException ignored){}
            
            evt = DigitalOceanModelFactory.getEventById(getProvider(), String.valueOf(evt.getId()));                    
        }
        */
	}

	@Override
    public void resume(@Nonnull String vmId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Suspend/resume not supported by the EC2 API");
    }

    @Override
    public void suspend(@Nonnull String vmId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Suspend/resume not supported by the EC2 API");
    }

    @Override
    public void terminate(@Nonnull String instanceId, @Nullable String explanation) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "terminateVM");
        try {
        	  try {
        		  VirtualMachine vm = getVirtualMachine(instanceId);

                  if( vm == null ) {
                      throw new CloudException("No such instance found: " + instanceId);
                  }
                  
              	  Destroy action = new Destroy();            	
                  
                  Action evt = DigitalOceanModelFactory.performAction(getProvider(), action, instanceId);
                
              } catch (UnsupportedEncodingException e) {
            	  logger.error(e.getMessage());
                  throw new CloudException(e);
			}
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void unpause(@Nonnull String vmId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Pause/unpause not supported by the EC2 API");
    }

    private @Nullable ResourceStatus toStatus(@Nullable Droplet instance) throws CloudException {
        if( instance == null ) {
            return null;
        }
        
        VmState state = VmState.PENDING;
        String vmId = String.valueOf(instance.getId());
        state = instance.getStatus();

        return new ResourceStatus(vmId, state);
    }

    private @Nullable VirtualMachine toVirtualMachine(@Nonnull ProviderContext ctx, @Nullable Droplet instance) throws CloudException {
        if( instance == null ) {
            return null;
        }
        
        VirtualMachine server = new VirtualMachine();

        server.setPersistent(false);
        server.setProviderOwnerId(ctx.getAccountNumber());
        server.setCurrentState(instance.getStatus());
        server.setName(instance.getName());
        server.setProductId(instance.getSize());        
        server.setDescription(null);
        server.setProviderVirtualMachineId(String.valueOf(instance.getId()));
        
        /*if( istnance..getPlatform() == null ) {
            server.setPlatform(Platform.UNKNOWN);
        }*/
        server.setProviderRegionId(String.valueOf(instance.getRegionId()));

        if( server.getDescription() == null ) {
            server.setDescription(server.getName() + " (" + instance.getImageId() + ")");
        }
        /*if( server.getArchitecture() == null && server.getProductId() != null ) {
            server.setArchitecture(getArchitecture(server.getProductId()));
        } else if( server.getArchitecture() == null ) {
            server.setArchitecture(Architecture.I64);
        }*/

        return server;
    }

    private @Nullable VirtualMachineProduct toProduct(@Nonnull Size s) throws InternalException {
    	
        VirtualMachineProduct prd = new VirtualMachineProduct();

        prd.setProviderProductId(s.getId());                        
        prd.setName(s.getSlug());                                                
        prd.setDescription(s.getSlug());
        
        prd.setCpuCount(s.getCpus());
        prd.setRootVolumeSize(new Storage<Gigabyte>(s.getDisk(), Storage.GIGABYTE));
        
        
        prd.setRamSize(new Storage<Megabyte>(s.getMemory(), Storage.MEGABYTE));
        
        
        prd.setStandardHourlyRate((float) s.getHourlyPrice().floatValue());
        return prd;
    }

}
