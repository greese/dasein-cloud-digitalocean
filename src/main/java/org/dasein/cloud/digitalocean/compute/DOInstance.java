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
import static org.dasein.cloud.digitalocean.models.rest.DigitalOcean.DROPLETS;
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

import static org.dasein.cloud.digitalocean.models.rest.DigitalOceanModelFactory.getModel;


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

            DigitalOceanModelFactory.performAction(getProvider(), action, vmId);
            vm = getVirtualMachine(vmId);
            return vm;
        } catch( CloudException e ) {
            logger.error(e.getMessage());
            throw new CloudException(e);
        } finally {
            APITrace.end();
        }
    }

    /**
     * Wait for specified number of minutes for all pending droplet events to complete
     * @param instanceId Id of the droplet
     * @param timeout Time in minutes to wait for events to complete
     * @throws InternalException
     * @throws CloudException
     */
    void waitForAllDropletEventsToComplete(@Nonnull String instanceId, int timeout) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "listVirtualMachineStatus");
        try {
            // allow maximum five minutes for events to complete
            long wait = System.currentTimeMillis() + timeout * 60 * 1000;
            boolean eventsPending = false;
            while( System.currentTimeMillis() < wait ) {
                Actions actions = DigitalOceanModelFactory.getDropletEvents(getProvider(), instanceId);
                for( Action action : actions.getActions() ) {
                    if( "in-progress".equalsIgnoreCase(action.getStatus()) ) {
                        eventsPending = true;
                    }
                }
                if( !eventsPending ) {
                    break;
                }
                try {
                    // must be careful here not to cause rate limits
                    Thread.sleep(30000);
                }
                catch( InterruptedException e ) {
                    break;
                }
            }
            // if events are still pending the cloud will fail the next operation anyway
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void start(@Nonnull String instanceId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "startVM");
        try {
            waitForAllDropletEventsToComplete(instanceId, 5);
            VirtualMachine vm = getVirtualMachine(instanceId);
            if( vm == null ) {
                throw new CloudException("No such instance: " + instanceId);
            }
            // only start if droplet is stopped, otherwise DO will give us an error
            if( VmState.STOPPED.equals(vm.getCurrentState() ) ) {
                DigitalOceanModelFactory.performAction(getProvider(), new Start(), instanceId);
            }
        } catch( CloudException e ) {
            logger.error(e.getMessage());
            throw new CloudException(e);
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
            Droplet d = (Droplet) DigitalOceanModelFactory.getModelById(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.DROPLET, instanceId);
            if (d != null) {
                VirtualMachine server = toVirtualMachine(d);
                if (server != null && server.getProviderVirtualMachineId().equals(instanceId)) {
                    return server;
                }
            }
            return null;
        } catch( CloudException e ) {
            if( e.getHttpCode() == HttpServletResponse.SC_NOT_FOUND) {
                return null;
            }
            logger.error(e.getMessage());
            throw new CloudException(e);
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
        try {
            // TODO: HEAD requests seem to be broken now (21/05/2015), so replacing with a GET - temporarily
            // https://www.digitalocean.com/community/questions/head-requests-return-404-error
            DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.SIZES);
            return true;
//            return (DigitalOceanModelFactory.checkAction(getProvider(), "sizes") == 200);
        } catch (CloudException e) {
            return false;
        }
    }

    // TODO: remove this soon
    public @Nonnull Iterable<VirtualMachineProduct> listProducts( @Nonnull VirtualMachineProductFilterOptions options, @Nullable Architecture architecture ) throws InternalException, CloudException {
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
        //Perform DigitalOcean query
        Sizes availableSizes = (Sizes)DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.SIZES);

        if (availableSizes != null) {
            for( Size s : availableSizes.getSizes() ) {
                VirtualMachineProduct product = toProduct(s);
                if( product != null ) {
                    list.add(product);
                }
            }
            cache.put(getContext(), list);
        }
        else {
            logger.error("No product could be found, " + getProvider().getCloudName() + " provided no data for their sizes API.");
            throw new CloudException("No product could be found.");
        }
        return list;
    }

    @Override
    public @Nonnull Iterable<VirtualMachineProduct> listProducts(String machineImageId, VirtualMachineProductFilterOptions options) throws InternalException, CloudException {
        MachineImage image = getProvider().getComputeServices().getImageSupport().getImage(machineImageId);
        String cacheName = "ALL";
        if( image != null ) {
            cacheName = image.getArchitecture().name();
        }
        Cache<VirtualMachineProduct> cache = Cache.getInstance(getProvider(), "products" + cacheName, VirtualMachineProduct.class, CacheLevel.REGION, new TimePeriod<Day>(1, TimePeriod.DAY));
        Iterable<VirtualMachineProduct> products = cache.get(getContext());
        if( products != null && products.iterator().hasNext() ) {
            return products;
        }

        List<VirtualMachineProduct> list = new ArrayList<VirtualMachineProduct>();
        //Perform DigitalOcean query
        Sizes availableSizes = (Sizes)DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.SIZES);

        if (availableSizes != null) {
            for( Size s : availableSizes.getSizes() ) {
                VirtualMachineProduct product = toProduct(s);
                if( product != null ) {
                    list.add(product);
                }
            }
            cache.put(getContext(), list);
        }
        else {
            logger.error("No product could be found, " + getProvider().getCloudName() + " provided no data for their sizes API.");
            throw new CloudException("No product could be found.");
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
            try { Thread.sleep(5000L); } catch( InterruptedException e ) {} // wait 5 sec for vm to get into a better shape
            return getVirtualMachine(droplet.getId());
        } finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<ResourceStatus> listVirtualMachineStatus() throws InternalException, CloudException {
        APITrace.begin(getProvider(), "listVirtualMachineStatus");
        try {
            List<ResourceStatus> list = new ArrayList<ResourceStatus>();
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
            List<VirtualMachine> results = new ArrayList<VirtualMachine>();

            Droplets droplets = (Droplets) DigitalOceanModelFactory.getModel(getProvider(), DROPLETS);
            int page = 1;
            int total = droplets.getTotal();
            while( droplets.getDroplets().size() > 0 ) {
                for( Droplet d : droplets.getDroplets() ) {
                    VirtualMachine vm = toVirtualMachine(d);
                    if( (options == null || options.matches(vm)) &&
                            vm.getProviderRegionId().equalsIgnoreCase(getContext().getRegionId()) ) {
                        results.add(vm);
                    }
                    else {
                        total --;
                    }
                }
                if( total <= 0 || total == results.size() ) {
                    break;
                }
                droplets = (Droplets) getModel(getProvider(), DROPLETS, ++page);

            }
            return results;
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void stop(@Nonnull String instanceId, boolean force) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "stopVM");
        try {
            waitForAllDropletEventsToComplete(instanceId, 5);
            VirtualMachine vm = getVirtualMachine(instanceId);
            if( vm == null ) {
                throw new CloudException("No such instance: " + instanceId);
            }
            // only stop if droplet is running, otherwise DO will give us an error
            if( VmState.RUNNING.equals(vm.getCurrentState() ) ) {
                DigitalOceanModelFactory.performAction(getProvider(), new Stop(), instanceId);
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
            // only reboot if droplet is running, otherwise DO will give us an error
            if( VmState.RUNNING.equals(vm.getCurrentState() ) ) {
                DigitalOceanModelFactory.performAction(getProvider(), new Reboot(), instanceId);
            }
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void terminate(@Nonnull String instanceId, @Nullable String explanation) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "terminateVM");
        try {
            if( getVirtualMachine(instanceId) == null ) {
              throw new CloudException("No such instance found: " + instanceId);
            }
            DigitalOceanModelFactory.performAction(getProvider(), new Destroy(), instanceId);
        } finally {
            APITrace.end();
        }
    }

    private @Nullable ResourceStatus toStatus(@Nullable Droplet instance) throws CloudException {
        if( instance == null ) {
            return null;
        }

        return new ResourceStatus(instance.getId(), instance.getStatus());
    }

    private @Nullable VirtualMachine toVirtualMachine(@Nullable Droplet instance) throws CloudException, InternalException {
        if( instance == null ) {
            return null;
        }
        
        VirtualMachine server = new VirtualMachine();

        server.setPersistent(true);
        server.setProviderOwnerId(getContext().getAccountNumber());
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

        if( instance.getRegion() != null ) {
            server.setProviderRegionId(instance.getRegion().getSlug());
            server.setProviderDataCenterId(instance.getRegion().getSlug());
        }
        else {
            server.setProviderRegionId(getContext().getRegionId());
            server.setProviderDataCenterId(getContext().getRegionId());
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
