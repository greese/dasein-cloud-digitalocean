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

package org.dasein.cloud.digitalocean;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.dasein.cloud.*;
import org.dasein.cloud.digitalocean.compute.DOComputeServices;
import org.dasein.cloud.digitalocean.dc.DOLocation;
import org.dasein.cloud.digitalocean.identity.IdentityServices;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Properties;
	

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
                new ContextRequirements.Field("token", "The Token key used to connect to this cloud", ContextRequirements.FieldType.TOKEN, true)
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

            	if( getComputeServices().getVirtualMachineSupport().isSubscribed() ) {
        			return ctx.getAccountNumber();
        		}
        		return null;
            	            	
            }
            catch( Throwable t ) {
                logger.error("Error querying API key: " + t.getMessage(), t);
                return null;
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT - " + DigitalOcean.class.getName() + ".textContext()");
            }
        }
    }

    public @Nonnull HttpClient getClient() throws InternalException {
        return getClient(false);
    }

    public @Nonnull HttpClient getClient(boolean multipart) throws InternalException {
        ProviderContext ctx = getContext();
        if( ctx == null ) {
            throw new InternalException("No context was specified for this request");
        }

        final HttpParams params = new BasicHttpParams();
        int timeout = 15000;
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setSoTimeout(params, timeout);


        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        if( !multipart ) {
            HttpProtocolParams.setContentCharset(params, Consts.UTF_8.toString());
        }
        HttpProtocolParams.setUserAgent(params, "Dasein Cloud");

        Properties p = ctx.getCustomProperties();
        if( p != null ) {
            String proxyHost = p.getProperty("proxyHost");
            String proxyPortStr = p.getProperty("proxyPort");
            int proxyPort = 0;
            if( proxyPortStr != null ) {
                proxyPort = Integer.parseInt(proxyPortStr);
            }
            if( proxyHost != null && proxyHost.length() > 0 && proxyPort > 0 ) {
                params.setParameter(ConnRoutePNames.DEFAULT_PROXY,
                        new HttpHost(proxyHost, proxyPort)
                );
            }
        }
        DefaultHttpClient client = new DefaultHttpClient(params);
        client.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(
                    final HttpRequest request,
                    final HttpContext context) throws HttpException, IOException {
                if( !request.containsHeader("Accept-Encoding") ) {
                    request.addHeader("Accept-Encoding", "gzip");
                }
                request.setParams(params);
            }
        });
        client.addResponseInterceptor(new HttpResponseInterceptor() {
            public void process(
                    final HttpResponse response,
                    final HttpContext context) throws HttpException, IOException {
                HttpEntity entity = response.getEntity();
                if( entity != null ) {
                    Header header = entity.getContentEncoding();
                    if( header != null ) {
                        for( HeaderElement codec : header.getElements() ) {
                            if( codec.getName().equalsIgnoreCase("gzip") ) {
                                response.setEntity(
                                        new GzipDecompressingEntity(response.getEntity()));
                                break;
                            }
                        }
                    }
                }
            }
        });
        return client;
    }
}