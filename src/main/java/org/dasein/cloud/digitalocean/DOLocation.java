package org.dasein.cloud.digitalocean;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.dc.*;
import org.dasein.cloud.digitalocean.models.Regions;
import org.dasein.cloud.digitalocean.models.Size;
import org.dasein.cloud.digitalocean.models.Sizes;
import org.dasein.cloud.digitalocean.models.rest.DigitalOceanModelFactory;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.Hour;
import org.dasein.util.uom.time.Minute;
import org.dasein.util.uom.time.TimePeriod;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class DOLocation implements DataCenterServices {
    static private final Logger logger = DigitalOcean.getLogger(DOLocation.class);

    private DigitalOcean provider;
    private transient volatile DODataCenterCapabilities capabilities;

    DOLocation(@Nonnull DigitalOcean provider) { this.provider = provider; }

    @Nonnull
    @Override
    public DataCenterCapabilities getCapabilities() throws InternalException, CloudException {
        if( capabilities == null ) {
            capabilities = new DODataCenterCapabilities(provider);
        }
        return capabilities;
    }

    @Override
    public @Nullable DataCenter getDataCenter(@Nonnull String dataCenterId) throws InternalException, CloudException {
        Region region = getRegion(dataCenterId);
        if( region == null ) {
            return null;
        }
        return new DataCenter(dataCenterId, region.getName(), dataCenterId, region.isActive(), region.isAvailable());
    }

    @Override
    public @Nonnull String getProviderTermForDataCenter(@Nonnull Locale locale) {
        return "data center";
    }

    @Override
    public @Nonnull String getProviderTermForRegion(@Nonnull Locale locale) {
        return "region";
    }

    @Override
    public @Nullable Region getRegion(@Nonnull String providerRegionId) throws InternalException, CloudException {
        for( Region r : listRegions() ) {
            if( providerRegionId.equals(r.getProviderRegionId()) ) {
                return r;
            }
        }
        return null;
    }

    @Override
    public @Nonnull Collection<DataCenter> listDataCenters(@Nonnull String providerRegionId) throws InternalException, CloudException {
        return Arrays.asList(getDataCenter(providerRegionId));
    }

    @Override
    public Collection<Region> listRegions() throws InternalException, CloudException {
        APITrace.begin(provider, "listRegions");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<Region> cache = Cache.getInstance(provider, "regions", Region.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
            Collection<Region> regions = (Collection<Region>)cache.get(ctx);

            if( regions != null ) {
                return regions;
            }
            regions = new ArrayList<Region>();


            Regions availableregions;
			try {
				availableregions = (Regions)DigitalOceanModelFactory.getModel(provider, org.dasein.cloud.digitalocean.models.rest.DigitalOcean.REGIONS);

	            Set<org.dasein.cloud.digitalocean.models.Region> regionsQuery = availableregions.getRegions();
	            Iterator<org.dasein.cloud.digitalocean.models.Region> itr = regionsQuery.iterator();
	            while(itr.hasNext()) {
	            	org.dasein.cloud.digitalocean.models.Region s = itr.next();
	            	Region vmp = toRegion(s);
	            	regions.add(vmp);
	            }
	            cache.put(ctx, regions);
	            return regions;
			} catch (Exception e) {
				e.printStackTrace();
				throw new CloudException(e);
			}
        }
        finally {
            APITrace.end();
        }
    }

    @Nonnull
    @Override
    public Collection<ResourcePool> listResourcePools(String providerDataCenterId) throws InternalException, CloudException {
        throw new CloudException(provider.getCloudName() + " does not support resource pools");
    }

    @Nullable
    @Override
    public ResourcePool getResourcePool(String providerResourcePoolId) throws InternalException, CloudException {
        throw new CloudException(provider.getCloudName() + " does not support resource pools");
    }

    @Nonnull
    @Override
    public Collection<StoragePool> listStoragePools() throws InternalException, CloudException {
        throw new CloudException(provider.getCloudName() + " does not support storage pools");
    }

    @Nonnull
    @Override
    public StoragePool getStoragePool(String providerStoragePoolId) throws InternalException, CloudException {
        throw new CloudException(provider.getCloudName() + " does not support storage pools");
    }

    @Nonnull
    @Override
    public Collection<Folder> listVMFolders() throws InternalException, CloudException {
        throw new CloudException(provider.getCloudName() + " does not support vm folders");
    }

    @Nonnull
    @Override
    public Folder getVMFolder(String providerVMFolderId) throws InternalException, CloudException {
        throw new CloudException(provider.getCloudName() + " does not support vm folders");
    }

    public DataCenter toDatacenter(org.dasein.cloud.digitalocean.models.Region r) {

    	DataCenter dc = new DataCenter();
    	dc.setRegionId(r.getSlug());
    	dc.setProviderDataCenterId(r.getSlug());
    	dc.setName(r.getName());
    	dc.setActive(r.getActive());
    	dc.setAvailable(r.getActive());
    	return dc;
    }

    public Region toRegion(org.dasein.cloud.digitalocean.models.Region region) {

    	Region r = new Region();
    	r.setProviderRegionId(region.getSlug());
    	r.setName(region.getName());
    	r.setActive(region.getActive());
    	r.setJurisdiction(region.getSlug().substring(0,2).toUpperCase());
    	r.setAvailable(region.getActive());
    	return r;
    }
}
