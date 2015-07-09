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

package org.dasein.cloud.digitalocean.dc;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.dc.*;
import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.digitalocean.NoContextException;
import org.dasein.cloud.digitalocean.models.Regions;
import org.dasein.cloud.digitalocean.models.rest.DigitalOceanModelFactory;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.uom.time.Hour;
import org.dasein.util.uom.time.TimePeriod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class DOLocation extends AbstractDataCenterServices<DigitalOcean> {
    static private final Logger logger = DigitalOcean.getLogger(DOLocation.class);

    private transient volatile DODataCenterCapabilities capabilities;

    public DOLocation(@Nonnull DigitalOcean provider) {
        super(provider);
    }

    @Override
    public @Nonnull DataCenterCapabilities getCapabilities() throws InternalException, CloudException {
        if( capabilities == null ) {
            capabilities = new DODataCenterCapabilities(getProvider());
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
        APITrace.begin(getProvider(), "listRegions");
        try {
            Cache<Region> cache = Cache.getInstance(getProvider(), "regions", Region.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
            Collection<Region> regions = (Collection<Region>)cache.get(getContext());
            if( regions != null ) {
                return regions;
            }
            regions = new ArrayList<Region>();

            Regions availableRegions = (Regions)DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.REGIONS);
            for( org.dasein.cloud.digitalocean.models.Region s : availableRegions.getRegions() ) {
                Region vmp = toRegion(s);
                regions.add(vmp);
            }
            cache.put(getContext(), regions);
            return regions;
        }
        finally {
            APITrace.end();
        }
    }

    public DataCenter toDatacenter(org.dasein.cloud.digitalocean.models.Region r) {
    	return new DataCenter(r.getSlug(), r.getName(), r.getSlug(), r.getActive(), r.getAvailable());
    }

    public Region toRegion(org.dasein.cloud.digitalocean.models.Region region) {

    	Region r = new Region();
    	r.setProviderRegionId(region.getSlug());
    	r.setName(region.getName());
    	r.setActive(region.getActive());
        String slugName = region.getSlug().toLowerCase();
        String jurisdiction = Jurisdiction.US.name();
        if( slugName.startsWith("ams") || slugName.startsWith("lon") ){
            jurisdiction = Jurisdiction.EU.name();
        }
        else if( slugName.startsWith("sgp") ){
            jurisdiction = Jurisdiction.SG.name();
        }
    	r.setJurisdiction(jurisdiction);
    	r.setAvailable(region.getActive());
    	return r;
    }
}
