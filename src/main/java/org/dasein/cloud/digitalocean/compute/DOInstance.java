/**
 * Copyright (C) 2012-2015 Dell, Inc.
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

import org.apache.log4j.Logger;
import org.dasein.cloud.*;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.digitalocean.models.*;
import org.dasein.cloud.digitalocean.models.actions.droplet.*;
import org.dasein.cloud.digitalocean.models.rest.DigitalOceanModelFactory;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.RawAddress;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Megabyte;
import org.dasein.util.uom.storage.Storage;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class DOInstance extends AbstractVMSupport<DigitalOcean> {
    static private final Logger logger = Logger.getLogger(DOInstance.class);

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
            	DigitalOceanModelFactory.performAction(getProvider(), action, vmId);
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
            	Droplet d = (Droplet) DigitalOceanModelFactory.getModelById(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.DROPLET, instanceId);
            	if (d != null) {
                    VirtualMachine server = toVirtualMachine(ctx, d);
                    if (server != null && server.getProviderVirtualMachineId().equals(instanceId)) {
                        return server;
                    }
                }
            } catch( CloudException e ) {
                if( e.getHttpCode() == HttpServletResponse.SC_NOT_FOUND) {
                    return null;
                }
                logger.error(e.getMessage());
                throw new CloudException(e);
            } catch (UnsupportedEncodingException e) {
            	logger.error(e.getMessage());
                throw new CloudException(e);
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
        return (DigitalOceanModelFactory.checkAction(getProvider(), "sizes") == 200);
    }


    @Override
    public @Nonnull Iterable<VirtualMachineProduct> listProducts(VirtualMachineProductFilterOptions options, Architecture architecture) throws InternalException, CloudException {
        String cacheName = "ALL";
        if( architecture != null ) {
            cacheName = architecture.name();
        }
        Cache<VirtualMachineProduct> cache = Cache.getInstance(getProvider(), "products" + cacheName, VirtualMachineProduct.class, CacheLevel.REGION, new TimePeriod<Day>(1, TimePeriod.DAY));
        Iterable<VirtualMachineProduct> products = cache.get(getContext());
        if( products != null && products.iterator().hasNext() ) {
            return products;
        }

        List<VirtualMachineProduct> list = new ArrayList<VirtualMachineProduct>();
        try {
            //Perform DigitalOcean query
            Sizes availableSizes = (Sizes)DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.SIZES);

            if (availableSizes != null) {
                Set<Size> sizes = availableSizes.getSizes();
                Iterator<Size> itr = sizes.iterator();
                while(itr.hasNext()) {
                    Size s = itr.next();

                    VirtualMachineProduct product = toProduct(s);
                    if( product != null ) {
                        list.add(product);
                    }
                }
                cache.put(getContext(), list);
            }
            else {
                logger.error("No product could be found, " + getProvider().getCloudName() + " provided no data for their sizesA PI.");
                throw new CloudException("No product could be found.");
            }

        } catch (UnsupportedEncodingException e) {
        	 logger.error(e.getMessage());
             throw new CloudException(e);
		}
        return list;
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

            Droplet droplet = DigitalOceanModelFactory.createInstance(getProvider(), hostname, product, cfg.getMachineImageId(), regionId, cfg.getBootstrapKey(), null);
            // returned droplet doesn't have enough information for our VirtualMachine to be complete, let's refresh
            try { Thread.sleep(2000L); } catch( InterruptedException e ) {}
            return getVirtualMachine(droplet.getId());
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
            
            List<ResourceStatus> list = new ArrayList<ResourceStatus>();
            try {
            	Droplets droplets = (Droplets)DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.DROPLETS );
            	if (droplets != null) {
	            	List<Droplet> dropletList = droplets.getDroplets();
	            	for( Droplet d : dropletList ) {
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
        return listVirtualMachines(null);
    }

    @Override
    public @Nonnull Iterable<VirtualMachine> listVirtualMachines(@Nullable VMFilterOptions options) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "listVirtualMachines");
        try {
            ProviderContext ctx = getProvider().getContext();

            if( ctx == null ) {
                throw new CloudException("No context was established for this request");
            }
            List<VirtualMachine> list = new ArrayList<VirtualMachine>();

            try {
            	Droplets droplets = (Droplets)DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.DROPLETS );
            	if (droplets != null) {
	            	List<Droplet> dropletList = droplets.getDroplets();
	            	for( Droplet d : dropletList ) {
	            		VirtualMachine vm = toVirtualMachine(ctx, d);
	                    if( options == null || options.matches(vm) ) {
	                        list.add(vm);
	                    }
	                }
            	}
            	
            } catch (UnsupportedEncodingException e) {
            	logger.error(e.getMessage(), e);
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
            
            try {
                DigitalOceanModelFactory.performAction(getProvider(), new Stop(), instanceId);
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
                DigitalOceanModelFactory.performAction(getProvider(), new Reboot(), instanceId);
            } catch (UnsupportedEncodingException e) {
            	 logger.error(e.getMessage());
                 throw new CloudException(e);
			}
        } finally {
            APITrace.end();
        }
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

        return new ResourceStatus(instance.getId(), instance.getStatus());
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
        if( instance.getSize() != null && instance.getSize().getSlug() != null ) {
            server.setProductId(instance.getSize().getSlug());
        }
        else {
            server.setProductId(instance.getSizeSlug());
        }
        String description = server.getName();
        if( instance.getImage().getName() != null ) {
            description += " (" + instance.getImage().getName() + ")";
        }
        server.setDescription(description);
        server.setProviderVirtualMachineId(instance.getId());
        server.setProviderMachineImageId(instance.getImage().getId());

        server.setProviderRegionId(ctx.getRegionId());
        server.setProviderDataCenterId(ctx.getRegionId());
        if( instance.getRegion() != null ) {
            server.setProviderRegionId(instance.getRegion().getSlug());
            server.setProviderDataCenterId(instance.getRegion().getSlug());
        }

        if( instance.getName().contains("64")) {
            server.setArchitecture(Architecture.I64);
        }
        else if( instance.getName().contains("32") || instance.getName().contains("386")) {
            server.setArchitecture(Architecture.I32);
        }
        else {
            server.setArchitecture(Architecture.I64);
        }
        server.setPlatform(Platform.guess(instance.getName()));
        if( Platform.UNKNOWN.equals(server.getPlatform()) ) {
            server.setPlatform(Platform.guess(instance.getImage().getName()));
        }
        if( Platform.UNKNOWN.equals(server.getPlatform()) ) {
            server.setPlatform(Platform.guess(instance.getImage().getDistribution()));
        }

        if( instance.getNetworks() != null ) {
            List<RawAddress> privateAddresses = new ArrayList<RawAddress>();
            List<RawAddress> publicAddresses = new ArrayList<RawAddress>();
            if( instance.getNetworks().getV4() != null ) {
                for (Network network : instance.getNetworks().getV4()) {
                    RawAddress address = new RawAddress(network.getIpAddress(), IPVersion.IPV4);
                    if( "public".equalsIgnoreCase(network.getType()) ) {
                        publicAddresses.add(address);
                    }
                    else {
                        privateAddresses.add(address);
                    }
                }
            }
            if( instance.getNetworks().getV6() != null ) {
                for (Network network : instance.getNetworks().getV6()) {
                    RawAddress address = new RawAddress(network.getIpAddress(), IPVersion.IPV6);
                    if( "public".equalsIgnoreCase(network.getType()) ) {
                        publicAddresses.add(address);
                    }
                    else {
                        privateAddresses.add(address);
                    }
                }
            }
            server.setPrivateAddresses(privateAddresses.toArray(new RawAddress[privateAddresses.size()]));
            server.setPublicAddresses(publicAddresses.toArray(new RawAddress[publicAddresses.size()]));
        }
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
