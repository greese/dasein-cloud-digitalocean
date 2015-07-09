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

package org.dasein.cloud.digitalocean.models.rest;

import org.apache.commons.io.IOUtils;
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
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.digitalocean.models.Action;
import org.dasein.cloud.digitalocean.models.Actions;
import org.dasein.cloud.digitalocean.models.Droplet;
import org.dasein.cloud.digitalocean.models.IDigitalOcean;
import org.dasein.cloud.digitalocean.models.actions.droplet.Create;
import org.dasein.cloud.identity.SSHKeypair;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DigitalOceanModelFactory {

    static private final Logger wire = org.dasein.cloud.digitalocean.DigitalOcean.getWireLogger(DigitalOceanModelFactory.class);
	static private final Logger logger = org.dasein.cloud.digitalocean.DigitalOcean.getLogger(DigitalOceanModelFactory.class);

    //for get method
    private static String performHttpRequest(org.dasein.cloud.digitalocean.DigitalOcean provider, RESTMethod method, String token, String endpoint) throws CloudException, InternalException {
    	return performHttpRequest(provider, method, token, endpoint, null);
    }
    
	private static String performHttpRequest(org.dasein.cloud.digitalocean.DigitalOcean provider, RESTMethod method, String token, String endpoint, DigitalOceanAction action) throws CloudException, InternalException {
		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".performHttpRequest(" + method + "," + token + "," + endpoint + ")");
            logger.trace("CALLING - " + method + " "  + endpoint);
        }
        HttpResponse response;
        String responseBody = null;
        try {
            response = sendRequest(provider, method, token, endpoint, action);
            if( response.getEntity() != null ) {
                responseBody = IOUtils.toString(response.getEntity().getContent());
                if( wire.isDebugEnabled() ) {
                    wire.debug(responseBody);
                }
            }
            if (logger.isTraceEnabled()) {
                logger.trace("RECEIVED - " + "[" + response.getStatusLine().getStatusCode() + "] " + responseBody);
            }

            if( response.getStatusLine().getStatusCode() >= 300 ) {
                JSONObject ob = new JSONObject(responseBody);
                String message = null;
                String code = null;
                if( ob != null ) {
                    code = ob.getString("id");
                    message = ob.getString("message");
                }
                logger.error("Status:" + response.getStatusLine().getStatusCode() + " - " + responseBody);
                throw new CloudException(CloudErrorType.GENERAL, response.getStatusLine().getStatusCode(), code, message);
            }
            return responseBody;

        } catch (JSONException e) {
            throw new CloudException(e);
        } catch (IOException e) {
            throw new CloudException(e);
        } finally {
            if (logger.isTraceEnabled()) {
                logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".performHttpRequest(" + method + "," + token + "," + endpoint + ")");
            }
            if (wire.isDebugEnabled()) {
                wire.debug("--------------------------------------------------------------------------------------");
                wire.debug("");
            }
        }
    }

    /**
     * Sent http request to the server
     * @return Http response
     * @throws CloudException
     */
    private static HttpResponse sendRequest(org.dasein.cloud.digitalocean.DigitalOcean provider, RESTMethod method, String token, String strUrl, DigitalOceanAction action) throws CloudException, InternalException {
        HttpRequestBase req = null;
        if (method == RESTMethod.GET) {
            req = new HttpGet(strUrl);
        } else if (method == RESTMethod.POST) {
            req = new HttpPost(strUrl);
        } else if (method == RESTMethod.PUT) {
            req = new HttpPut(strUrl);
        } else if (method == RESTMethod.DELETE) {
            req = new HttpDelete(strUrl);
        } else if (method == RESTMethod.HEAD) {
            req = new HttpHead(strUrl);
        }

        try {
            req.setHeader("Authorization", "Bearer " + token);
            req.setHeader("Accept", "application/json");
            req.setHeader("Content-Type", "application/json;charset=UTF-8");

            StringEntity requestEntity = null;
            if (req instanceof HttpEntityEnclosingRequestBase && action != null) {
                JSONObject jsonToPost = action.getParameters();
                if (jsonToPost != null) {
                    requestEntity = new StringEntity(
                            jsonToPost.toString(),
                            ContentType.APPLICATION_JSON);
                    ((HttpEntityEnclosingRequestBase) req).setEntity(requestEntity);
                }
            }

            HttpClient httpClient = provider.getClient();

            if (wire.isDebugEnabled()) {
                wire.debug("");
                wire.debug("--------------------------------------------------------------------------------------");
            }

            if (wire.isDebugEnabled()) {
                wire.debug(req.getRequestLine().toString());
                for (Header header : req.getAllHeaders()) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");

                if (requestEntity != null) {
                    try {
                        wire.debug(EntityUtils.toString(requestEntity));
                        wire.debug("");
                    } catch (IOException ignore) {
                    }
                }
            }

            HttpResponse response = null;
            int retryCount = 0;

            while (retryCount < 6) {
                response = httpClient.execute(req);

                if (wire.isDebugEnabled()) {
                    wire.debug(response.getStatusLine().toString());
                }

                if (method == RESTMethod.DELETE) {
                    if ((response.getStatusLine().getStatusCode() == 204)) {
                        break;
                    } else {
                        retryCount++;
                        Thread.sleep(5000);
                    }
                } else {
                    break;
                }
            }
            if (method == RESTMethod.DELETE && (response.getStatusLine().getStatusCode() != 204)) {
                //Error occurred
                throw new CloudException("Delete method returned unexpected code, despite retrying.");
            }
            return response;
        } catch (JSONException e) {
            throw new CloudException("Problem sending request.", e);
        } catch (InterruptedException e) {
            throw new CloudException("Problem sending request.", e);
        } catch (ClientProtocolException e) {
            throw new CloudException("Problem sending request.", e);
        } catch (IOException e) {
            throw new CloudException("Problem sending request.", e);
        } finally {
            try {
//                req.releaseConnection();
            } catch (Exception e) {
            }

        }
    }

    public static DigitalOceanRestModel getModel(org.dasein.cloud.digitalocean.DigitalOcean provider, DigitalOcean model) throws CloudException, InternalException {
        return getModel(provider, model, 0);
    }

	public static DigitalOceanRestModel getModel(org.dasein.cloud.digitalocean.DigitalOcean provider, DigitalOcean model, int page) throws CloudException, InternalException {
		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".getModel(" + provider + "," +  model + ")");
        }
			
		String token = (String) provider.getContext().getConfigurationValue("token");
    	
		try {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(getApiUrl(provider)).append(getEndpoint(model));
            if( page > 0 ) {
                if( urlBuilder.indexOf("?") > 0 ) {
                    urlBuilder.append('&');
                }
                else {
                    urlBuilder.append('?');
                }
                urlBuilder.append("page=").append(page);
            }
			String responseText = performHttpRequest(provider, RESTMethod.GET, token, urlBuilder.toString());
			JSONObject jso = new JSONObject(responseText);
								
			return model.fromJson(jso);
        } catch (JSONException e) {
            throw new CloudException(e);
		} finally {
			if( logger.isTraceEnabled() ) {
	            logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".getModel(" + provider + "," + model + ")");
	        }
		}
	}
	
	public static DigitalOceanRestModel getModelById(org.dasein.cloud.digitalocean.DigitalOcean provider, DigitalOcean model, String id) throws CloudException, InternalException {

		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".getModel(" + provider + "," +  model + "," + id + ")");
        }

		String token = (String) provider.getContext().getConfigurationValue("token");
		try {
			String s = performHttpRequest(provider, RESTMethod.GET, token,  getApiUrl(provider) + getEndpoint(model, id));
			JSONObject jso = new JSONObject(s);
			return model.fromJson(jso);				
		} catch (JSONException e) {
            throw new CloudException(e);
        } finally {
			if( logger.isTraceEnabled() ) {
	            logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".getModel(" + provider + "," + model + ")");
	        }
		}
	}

	private static String getEndpoint(IDigitalOcean d) {
		return d.toString();				
	}
	
	private static String getEndpoint(IDigitalOcean d, String id) {
		if (id == null) return getEndpoint(d);
		
		return String.format(d.toString(), id);				
	}

	private static String getApiUrl(org.dasein.cloud.digitalocean.DigitalOcean provider) {
		String url = provider.getContext().getCloud().getEndpoint();
		if (url == null) {
			//Return the default digitalocean endpoint.
			url = "https://api.digitalocean.com/";
		} else {
			if (url.endsWith("//")) {
				url = url.substring(0, url.length()-1);
			} else {
				if (!url.endsWith("/")) {
					url = url + "/";
				}
			}
		}
		return url;
	}

	public static Action performAction(org.dasein.cloud.digitalocean.DigitalOcean provider, DigitalOceanAction doa, String id) throws CloudException, InternalException {

		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".destroyDroplet(" + provider + "," + id + ")");
		}

		String token = (String) provider.getContext().getConfigurationValue("token");

		String s = performHttpRequest(provider, doa.getRestMethod(), token,  getApiUrl(provider) + getEndpoint(doa, id), doa);
		
		try {
			//Delete have no output...
			if (doa.getRestMethod() == RESTMethod.DELETE) {
				return null;
			}
			
			JSONObject jso = new JSONObject(s);
			Action result = (Action) DigitalOcean.ACTION.fromJson(jso);				
			
			if (!result.isError()) {
				return (Action) DigitalOcean.ACTION.fromJson(jso);				
			} else {
				//Not sure why in API V2 they removed the message of errors... we are now left blind
				throw new CloudException("An error occured while performing " + doa + " with parameters : " + doa.getParameters());
			}
		} catch (JSONException e) {			
			throw new CloudException(e);
		} finally {
			if( logger.isTraceEnabled() ) {
	            logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".destroyDroplet(" + provider + "," + id + ")");
	        }
		}
	}
	
	public static DigitalOceanRestModel performAction(@Nonnull org.dasein.cloud.digitalocean.DigitalOcean provider, DigitalOceanAction doa, IDigitalOcean returnObject) throws CloudException, InternalException {

		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".performAction(" + provider.getCloudName() + ", " + returnObject + ")");
		}

		String token = (String) provider.getContext().getConfigurationValue("token");
        
		String s = performHttpRequest(provider, doa.getRestMethod(), token,  getApiUrl(provider) + getEndpoint(doa), doa);
		
		try {			
			JSONObject jso = new JSONObject(s);
			return returnObject.fromJson(jso);				
		} catch (JSONException e) {
            throw new CloudException(e);
        } finally {
			if( logger.isTraceEnabled() ) {
	            logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".performAction(" + provider.getCloudName() + "," + returnObject + ")");
	        }
		}
	}

    /**
     * Return HTTP status code for an action request sent via HEAD method
     * @param provider
     * @param actionUrl
     * @return status code
     * @throws InternalException
     * @throws CloudException
     */
    public static int checkAction(@Nonnull org.dasein.cloud.digitalocean.DigitalOcean provider, String actionUrl) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".checkAction(" + provider.getCloudName() + ")");
        }

        String token = (String) provider.getContext().getConfigurationValue("token");

        try {
            return sendRequest(provider, RESTMethod.HEAD, token,  getApiUrl(provider) + "v2/" + actionUrl, null).getStatusLine().getStatusCode();
        } finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".checkAction(" + provider.getCloudName() + ")");
            }
        }
    }

    public static Droplet createInstance(org.dasein.cloud.digitalocean.DigitalOcean provider, String dropletName, String sizeId, String theImageId, String regionId, String bootstrapKey, Map<String, Object> extraParameters) throws CloudException, InternalException {

		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".createInstance(" + dropletName + "," + sizeId + "," + theImageId + "," + regionId + "," + extraParameters + ")");
		}

        try {
			Create action = new Create(dropletName, sizeId, theImageId, regionId);
			List<String> ssh_key_ids = new ArrayList<String>();
			//Extra parameter is not part of DaseinCloud.... as its cloud specific
			if (extraParameters != null) {
				if (extraParameters.containsKey("backup_enabled")) {
					try {
						action.setBackups((Boolean)extraParameters.get("backup_enabled"));
					} catch (Exception ee) {
						throw new CloudException("Parameter 'backup_enabled' must be of type Boolean");
					}
				}
				if (extraParameters.containsKey("private_networking")) {
					try {
						action.setPrivateNetworking((Boolean)extraParameters.get("private_networking"));
					} catch (Exception ee) {
						throw new CloudException("Parameter 'private_networking' must be of type Boolean");
					}
				}
                if (extraParameters.containsKey("user_data")){
                    try {
                        action.setUserdata((String)extraParameters.get("user_data"));
                    } catch (Exception ee) {
                        throw new CloudException("Parameter 'user_data' must be of type String");
                    }
                }
			}
		
			if( bootstrapKey != null ) {
                ssh_key_ids.add(bootstrapKey);
			}
			action.setSshKeyIds(ssh_key_ids);
			
			return (Droplet) performAction(provider, action, DigitalOcean.DROPLET);
		} finally {
				
			if( logger.isTraceEnabled() ) {
	            logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".createInstance(" + dropletName + "," + sizeId + "," + theImageId + "," + regionId + "," + extraParameters + ")");
			}
		}
	}

	public static Droplet getDropletByInstance(org.dasein.cloud.digitalocean.DigitalOcean provider, String dropletInstanceId) throws CloudException, InternalException {
		return (Droplet) getModelById(provider, DigitalOcean.DROPLET, dropletInstanceId);
	}

	public static Action getEventById(org.dasein.cloud.digitalocean.DigitalOcean provider, String id) throws CloudException, InternalException {
		return (Action) getModelById(provider, DigitalOcean.ACTION, id);
	}

    public static Actions getDropletEvents(org.dasein.cloud.digitalocean.DigitalOcean provider, String dropletId) throws CloudException, InternalException {
        return (Actions) getModelById(provider, DigitalOcean.DROPLET_ACTIONS, dropletId);
    }

}
