package org.dasein.cloud.digitalocean;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.dc.Region;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class DOLocation implements DataCenterServices {
    static private final Logger logger = DigitalOcean.getLogger(DOLocation.class);

    private DigitalOcean provider;

    DOLocation(@Nonnull DigitalOcean provider) { this.provider = provider; }

    @Override
    public @Nullable DataCenter getDataCenter(@Nonnull String dataCenterId) throws InternalException, CloudException {
        for( Region region : listRegions() ) {
            for( DataCenter dc : listDataCenters(region.getProviderRegionId()) ) {
                if( dataCenterId.equals(dc.getProviderDataCenterId()) ) {
                    return dc;
                }
            }
        }
        return null;
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
        APITrace.begin(provider, "listDataCenters");
        try {
            Region region = getRegion(providerRegionId);

            if( region == null ) {
                throw new CloudException("No such region: " + providerRegionId);
            }
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<DataCenter> cache = Cache.getInstance(provider, "dataCenters", DataCenter.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Minute>(5, TimePeriod.MINUTE));
            Collection<DataCenter> dcList = (Collection<DataCenter>)cache.get(ctx);

            if( dcList != null ) {
                return dcList;
            }
            ArrayList<DataCenter> dataCenters = new ArrayList<DataCenter>();
            
            Regions availableregions;
			try {
				availableregions = (Regions)DigitalOceanModelFactory.getModel(provider, org.dasein.cloud.digitalocean.models.rest.DigitalOcean.REGIONS);
				
	            Set<org.dasein.cloud.digitalocean.models.Region> regions = availableregions.getRegions();
	            Iterator<org.dasein.cloud.digitalocean.models.Region> itr = regions.iterator();
	            while(itr.hasNext()) {
	            	org.dasein.cloud.digitalocean.models.Region s = itr.next();
	            	DataCenter vmp = toDatacenter(s);
	            	dataCenters.add(vmp);
	            }
	            cache.put(ctx, dataCenters);
	            return dataCenters;
			} catch (Exception e) {
				throw new CloudException(e);				
			}
        }
        finally {
            APITrace.end();
        }
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
