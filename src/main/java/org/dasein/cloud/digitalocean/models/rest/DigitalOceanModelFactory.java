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
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.digitalocean.models.Action;
import org.dasein.cloud.digitalocean.models.Droplet;
import org.dasein.cloud.digitalocean.models.IDigitalOcean;
import org.dasein.cloud.digitalocean.models.actions.droplet.Create;
import org.dasein.cloud.identity.SSHKeypair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DigitalOceanModelFactory {

    static private final Logger wire = org.dasein.cloud.digitalocean.DigitalOcean.getWireLogger(DigitalOceanModelFactory.class);
	static private final Logger logger = org.dasein.cloud.digitalocean.DigitalOcean.getLogger(DigitalOceanModelFactory.class);

    //for get method
    private static String performHttpRequest(RESTMethod method, String token, String endpoint, int timeout) throws CloudException {
    	return performHttpRequest( method, token, endpoint, timeout, null);
    }
    
	private static String performHttpRequest(RESTMethod method, String token, String endpoint, int timeout, DigitalOceanAction action) throws CloudException {
		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".performHttpRequest(" + method + "," + token + "," + endpoint + "," + timeout+ ")");
        }

        // Ignore pagination use of per_page=-1 for all requests
        // String strUrl = endpoint + "?per_page=-1";
        String strUrl = endpoint + "?per_page=50000";

        if( logger.isTraceEnabled() ) {
            logger.trace("CALLING - " + method + " "  + endpoint);
        }
        HttpResponse response;
        String responseBody;
        try {
            response = sendRequest(method, token, endpoint, timeout, action);
            responseBody = IOUtils.toString(response.getEntity().getContent());
            if( wire.isDebugEnabled() ) {
                wire.debug(responseBody);
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
                logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".performHttpRequest(" + method + "," + token + "," + endpoint + "," + timeout + ")");
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
    private static HttpResponse sendRequest(RESTMethod method, String token, String strUrl, int timeout, DigitalOceanAction action) throws CloudException {
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
            HttpParams httpParams = new BasicHttpParams();
            req.setHeader("Authorization", "Bearer " + token);
            req.setHeader("Accept", "application/json");
            req.setHeader("Content-Type", "application/json;charset=UTF-8");

            HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
            HttpConnectionParams.setSoTimeout(httpParams, timeout);

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

            HttpClient httpClient = new DefaultHttpClient(httpParams);

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
                //Error occured
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

	public static DigitalOceanRestModel getModel(CloudProvider provider, DigitalOcean model) throws UnsupportedEncodingException, CloudException {
		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".getModel(" + provider + "," +  model + ")");
        }
			
		String token = (String)provider.getContext().getConfigurationValue("token");
    	
		try {		
			String s = performHttpRequest(RESTMethod.GET, token,  getApiUrl(provider) + getEndpoint(model), 15000);
			
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
	
	public static DigitalOceanRestModel getModelById(CloudProvider provider, DigitalOcean model, String id) throws UnsupportedEncodingException, CloudException {

		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".getModel(" + provider + "," +  model + "," + id + ")");
        }

		String token = (String)provider.getContext().getConfigurationValue("token");
		try {
			String s = performHttpRequest(RESTMethod.GET, token,  getApiUrl(provider) + getEndpoint(model, id), 15000);
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

	private static String getApiUrl(CloudProvider provider) {
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

	public static Action performAction(CloudProvider provider, DigitalOceanAction doa, String id) throws UnsupportedEncodingException, CloudException {

		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".destroyDroplet(" + provider + "," + id + ")");
		}

		String token = (String) provider.getContext().getConfigurationValue("token");

		String s = performHttpRequest(doa.getRestMethod(), token,  getApiUrl(provider) + getEndpoint(doa, id), 15000, doa);
		
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
	
	public static DigitalOceanRestModel performAction(CloudProvider provider, DigitalOceanAction doa, IDigitalOcean returnObject) throws UnsupportedEncodingException, CloudException {

		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".performAction(" + provider + ", " + returnObject + ")");
		}

		String token = (String)provider.getContext().getConfigurationValue("token");
        
		String s = performHttpRequest(doa.getRestMethod(), token,  getApiUrl(provider) + getEndpoint(doa), 15000, doa);
		
		try {			
			JSONObject jso = new JSONObject(s);
			return returnObject.fromJson(jso);				
		} catch (JSONException e) {
            throw new CloudException(e);
        } finally {
			if( logger.isTraceEnabled() ) {
	            logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".performAction(" + provider + "," + returnObject + ")");
	        }
		}
	}

    /**
     * Return HTTP status code for an action request sent via HEAD method
     * @param provider
     * @param actionUrl
     * @return
     * @throws UnsupportedEncodingException
     * @throws CloudException
     */
    public static int checkAction(CloudProvider provider, String actionUrl) throws UnsupportedEncodingException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".checkAction(" + provider + ")");
        }

        String token = (String) provider.getContext().getConfigurationValue("token");

        try {
            return sendRequest(RESTMethod.GET, token,  getApiUrl(provider) + "v2/" + actionUrl, 15000, null).getStatusLine().getStatusCode();
        } finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".checkAction(" + provider + ")");
            }
        }
    }

    public static Droplet createInstance(CloudProvider provider, String dropletName, String sizeId, String theImageId, String regionId, String bootstrapKey, HashMap<String, Object> extraParameters) throws UnsupportedEncodingException, CloudException {

		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".createInstance(" + dropletName + "," + sizeId + "," + theImageId + "," + regionId + "," + extraParameters + ")");
		}

        HashMap<String, SSHKeypair> keypairByName = new HashMap<String, SSHKeypair>();
        HashMap<String, SSHKeypair> keypairById = new HashMap<String, SSHKeypair>();
        try {
            Iterator<SSHKeypair> i = provider.getIdentityServices().getShellKeySupport().list().iterator();
            while (i.hasNext()) {
                SSHKeypair next = i.next();
                keypairByName.put(next.getName(), next);
                keypairById.put(next.getProviderKeypairId(), next);
            }
        } catch (InternalException e) {
            throw new CloudException ("Could not retrieve account ssh key list for " + provider.getContext().getAccountNumber(), e);
        }

        try {
			Create action = new Create(dropletName, sizeId, theImageId, regionId);
			List<Long> ssh_key_ids = new ArrayList<Long>();
			//Extra parameter is not part of DaseinCloud.... as its cloud specific
			if (extraParameters != null) {
				if (extraParameters.containsKey("ssh_key_ids")) {
					try {					
						ssh_key_ids = (List<Long>) extraParameters.get("ssh_key_ids");
					} catch (Exception ee) {
						throw new CloudException("Parameter 'ssh_key_ids' must be of type ArrayList<Long>");
					}
				}
				
							
				if (extraParameters.containsKey("ssh_key_id")) {
					Long sshKeyId = Long.valueOf("" + extraParameters.get("ssh_key_ids"));
					ssh_key_ids.add(sshKeyId);									
				}
				
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
			}
		
			if(bootstrapKey!=null) {
				if (!bootstrapKey.isEmpty()) {
                    String id = null;
                    if (keypairById.containsKey(bootstrapKey))
                        id = keypairById.get(bootstrapKey).getProviderKeypairId();
                    else if (keypairByName.containsKey(bootstrapKey))
                        id = keypairByName.get(bootstrapKey).getProviderKeypairId();
                    else
                        throw new CloudException("Received a bootstrap key but it doesn't seem to match an existing key pair");
					ssh_key_ids.add(Long.valueOf(id)); // FIXME(maria): Long??? it's a string
				}
			}
			action.setSshKeyIds(ssh_key_ids);
			
			return (Droplet)performAction(provider, action, DigitalOcean.DROPLET);
		} finally {
				
			if( logger.isTraceEnabled() ) {
	            logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".createInstance(" + dropletName + "," + sizeId + "," + theImageId + "," + regionId + "," + extraParameters + ")");
			}
		}
	}

	public static Droplet getDropletByInstance(CloudProvider provider, String dropletInstanceId) throws UnsupportedEncodingException, CloudException {
		// TODO Auto-generated method stub
		return (Droplet)getModelById(provider, DigitalOcean.DROPLET, dropletInstanceId);		
	}

	public static Action getEventById(CloudProvider provider, String id) throws UnsupportedEncodingException, CloudException {		 
		return (Action)getModelById(provider, DigitalOcean.ACTION, id);				
	}


}
