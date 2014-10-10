package org.dasein.cloud.digitalocean;

import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.dc.DataCenterCapabilities;

import java.util.Locale;

/**
 * Created by mariapavlova on 10/10/2014.
 */
public class DODataCenterCapabilities extends AbstractCapabilities<DigitalOcean> implements DataCenterCapabilities {
    public DODataCenterCapabilities(DigitalOcean provider) {
        super(provider);
    }

    @Override
    public String getProviderTermForDataCenter(Locale locale) {
        return "data center";
    }

    @Override
    public String getProviderTermForRegion(Locale locale) {
        return "region";
    }

    @Override
    public boolean supportsAffinityGroups() {
        return false;
    }

    @Override
    public boolean supportsResourcePools() {
        return false;
    }

    @Override
    public boolean supportsStoragePools() {
        return false;
    }

    @Override
    public boolean supportsFolders() {
        return false;
    }
}
