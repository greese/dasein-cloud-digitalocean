/**
 * Copyright (C) 2014 Dell, Inc.
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

package org.dasein.cloud.digitalocean;

import java.util.Properties;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.dasein.cloud.AbstractCloud;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.digitalocean.models.Regions;
import org.dasein.cloud.digitalocean.models.rest.DigitalOceanModelFactory;
	

/**
 * Add header info here
 * @version 2014.03 initial version
 * @since 2014.03
 */
public class DigitalOcean extends AbstractCloud {
    static private final Logger logger = getLogger(DigitalOcean.class);

    public
    @Nullable
    String getDOUrl() throws InternalException, CloudException {        
        String url = getUrl();        
        return url;        
    }
    
    private transient volatile DOProvider provider;

    public
    @Nonnull
    DOProvider getEC2Provider() {
        if (provider == null) {
            provider = DOProvider.valueOf(getProviderName());
        }
        return provider;
    }
    

    public @Nullable String getUrl() throws InternalException, CloudException {
        ProviderContext ctx = getContext();
        String url;
        
        url = (ctx == null ? null : ctx.getCloud().getEndpoint());
        if (url == null) {
        	return "https://api.digitalocean.com/";
        }
        
        
        if (url.endsWith("//")) {
			url = url.substring(0, url.length()-1);
		} else {
			if (!url.endsWith("/")) {
				url = url + "/";
			}
		}
        
        if (!url.startsWith("http")) {
            String cloudUrl = ctx.getCloud().getEndpoint();

            if (cloudUrl != null && cloudUrl.startsWith("http:")) {
                return "http://" + url;
            }
            return "https://" + url;
        } else {
            return url;
        }
        
    }

    
	static private @Nonnull String getLastItem(@Nonnull String name) {
        int idx = name.lastIndexOf('.');

        if( idx < 0 ) {
            return name;
        }
        else if( idx == (name.length()-1) ) {
            return "";
        }
        return name.substring(idx + 1);
    }

    static public @Nonnull Logger getLogger(@Nonnull Class<?> cls) {
        String pkg = getLastItem(cls.getPackage().getName());

        if( pkg.equals("digitalocean") ) {
            pkg = "";
        }
        else {
            pkg = pkg + ".";
        }
        return Logger.getLogger("dasein.cloud.digitalocean.std." + pkg + getLastItem(cls.getName()));
    }

    static public @Nonnull Logger getWireLogger(@Nonnull Class<?> cls) {
        return Logger.getLogger("dasein.cloud.digitalocean.wire." + getLastItem(cls.getPackage().getName()) + "." + getLastItem(cls.getName()));
    }

    public DigitalOcean() { }

    @Override
    public @Nonnull String getCloudName() {
        ProviderContext ctx = getContext();
        String name = (ctx == null ? null : ctx.getCloud().getCloudName());

        return (name == null ? "DigitalOcean" : name);
    }

    @Override
    public @Nonnull ContextRequirements getContextRequirements() {
        // define the information needed to connect to this cloud in the form of context requirements
        // this digitalocean defines a single keypair that any client must provide to the ProviderContext when connecting
        return new ContextRequirements(
                new ContextRequirements.Field("token", "The Token key used to connect to this cloud", ContextRequirements.FieldType.TEXT, true)
        );
    }

    @Override
    public @Nonnull DOLocation getDataCenterServices() {
        return new DOLocation(this);
    }
    
    @Override
    public @Nonnull DOComputeServices getComputeServices() {
        return new DOComputeServices(this);
    }
    
    @Override
    public @Nonnull IdentityServices getIdentityServices() {
        return new IdentityServices(this);
    }
    
    
    

    @Override
    public @Nonnull String getProviderName() {
        ProviderContext ctx = getContext();
        String name = (ctx == null ? null : ctx.getCloud().getProviderName());

        return (name == null ? "DigitalOcean" : name);
    }

   
    @Override
    public @Nullable String testContext() {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOcean.class.getName() + ".testContext()");
        }
        try {
            ProviderContext ctx = getContext();

            if( ctx == null ) {
                logger.warn("No context was provided for testing");
                return null;
            }
            try {
            	
                // TODO: Can we simplify this call so it can  be faster?
                // return null if they are not
                // return an account number if they are
            	//logger.debug("TEST API KEY : " + ctx.getConfigurationValue("token"));
            	String token = (String)ctx.getConfigurationValue("token");
            	if (token == null) {
            		logger.error("No token parameter as provided");
            		return null;
            	}
            	
        		Regions r = (Regions)DigitalOceanModelFactory.getModel(this, org.dasein.cloud.digitalocean.models.rest.DigitalOcean.REGIONS);
        		if (r.getRegions().size() > 0) {
        			return ctx.getAccountNumber();
        		}
        		return null;
            	            	
            }
            catch( Throwable t ) {
                logger.error("Error querying API key: " + t.getMessage());
                t.printStackTrace();
                return null;
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT - " + DigitalOcean.class.getName() + ".textContext()");
            }
        }
    }
    
    public @Nonnull String getVMProductsResource() {
        ProviderContext ctx = getContext();
        String value;
       
        if( ctx == null ) {
            value = null;
        }
        else {

            Properties p = ctx.getCustomProperties();

            if( p == null ) {
                value = null;
            }
            else {            	
                value = p.getProperty("vmproducts");
            }
        }
        if( value == null ) {
        	
        	//TODO: Should we use getCloud()? instead of digitalocean ?
            value = System.getProperty("digitalocean.vmproducts");
        }
        if( value == null ) {
        	//We should add this resource as example only if we want to enforce it...
            value = "/org/dasein/cloud/digitalocean/vmproducts.json";
        }
        return value;
    }
}