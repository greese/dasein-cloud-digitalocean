/**
 * Copyright (C) 2014 ACenterA, Inc.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.digitalocean.CloudProviderException;
import org.dasein.cloud.digitalocean.models.Action;
import org.dasein.cloud.digitalocean.models.Droplet;
import org.dasein.cloud.digitalocean.models.IDigitalOcean;
import org.dasein.cloud.digitalocean.models.actions.droplet.Create;
import org.json.JSONException;
import org.json.JSONObject;


public class DigitalOceanModelFactory {
	
	static private final Logger logger = org.dasein.cloud.digitalocean.DigitalOcean.getLogger(DigitalOceanModelFactory.class);

    //for get method
    private static String performHttpRequest(RESTMethod method, String token, String endpoint, int timeout) {
    	return performHttpRequest( method, token, endpoint, timeout, null);
    }
    
	private static String performHttpRequest(RESTMethod method, String token, String endpoint, int timeout, DigitalOceanAction action) {
		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".performHttpRequest(" + method + "," + token + "," + endpoint + "," + timeout+ ")");
        }

			//Ignore pagination use of per_page=-1 for all requests not consistent... ie images didn't work so using 99999999 instead
		    //
			String strUrl = endpoint + "?per_page=99999999";						
				
			if( logger.isTraceEnabled() ) {
				logger.trace("CALLING - " + method + " "  + endpoint);
			}
			HttpRequestBase req = null;
			if (method == RESTMethod.GET) {
				req = new HttpGet(strUrl);
			} else if (method == RESTMethod.POST) {
				req = new HttpPost(strUrl);
			} else if (method == RESTMethod.PUT) {
				req = new HttpPut(strUrl);
			} else if (method == RESTMethod.DELETE) {
				req = new HttpDelete(strUrl);
			}
			try {
				HttpParams my_httpParams = new BasicHttpParams();
				req.setHeader("Authorization", "Bearer "  + token);
                req.setHeader("Accept", "application/json");
                req.setHeader("Content-Type","application/json;charset=UTF-8");

				HttpConnectionParams.setConnectionTimeout(my_httpParams, timeout);
				HttpConnectionParams.setSoTimeout(my_httpParams, timeout);				
				if ((method == RESTMethod.POST) || (method == RESTMethod.PUT)) {
					
					JSONObject jsonToPost = action.getParameters();
					if (jsonToPost != null) {	
						StringEntity requestEntity = new StringEntity(					
								jsonToPost.toString(),
						    ContentType.APPLICATION_JSON);
					
						if (method == RESTMethod.POST) ((HttpPost)req).setEntity(requestEntity);
						if (method == RESTMethod.PUT) ((HttpPut)req).setEntity(requestEntity);
					}
					
					/*List<NameValuePair> params = new ArrayList<NameValuePair>();
					JSONObject j = action.getParameters();
					Iterator<String> i = j.keys();
					while(i.hasNext()) {
						String key = i.next();
						NameValuePair nvp = new NameValuePair(key, (String) j.get(key));
						params.add(nvp);	
					}*/
				}
				
			    HttpClient httpclient = new DefaultHttpClient(my_httpParams);	
			    HttpResponse response = httpclient.execute(req);
			    
			    if (method == RESTMethod.DELETE) {
		    		if ((response.getStatusLine().getStatusCode() == 204) ) {
					   	
				    	response = null;
						httpclient = null;
						 				
				    	return null;
				    } else {
				        //Error occured
				    	response = null;
						httpclient = null;
						  						
													
						throw new CloudException("Delete method returned un expected coded instead of 204");						
						
				    }
			    } else {
				     //TODO: Handle errors??
				    BufferedReader br1=new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				    			    
				    String s1 = br1.readLine();
				    String resp = "";
					while (s1 != null) {		
							resp += s1;
							s1=br1.readLine();						
					}		    
					br1.close();
					if( logger.isTraceEnabled() ) {
						logger.trace("RECEIVED - " + "[" + response.getStatusLine().getStatusCode() + "] " + resp);
					}	
					
					if ((response.getStatusLine().getStatusCode() == 200) || (response.getStatusLine().getStatusCode() == 201) ) {
					   	
				    	response = null;
						httpclient = null;
						 				
				    	return resp;
				    } else {
				    	 //Add Error
				    	 logger.error("Status:" + response.getStatusLine().getStatusCode() + " - " + resp);
				    	 return resp;
				    	 //throw new CloudException("Status:" + response.getStatusLine().getStatusCode() + " - " + resp);				    	 
				    }
			    }
			} catch (Exception e1) {
				e1.printStackTrace();
			} finally {
				try {req.releaseConnection();}catch (Exception e) {}
				if( logger.isTraceEnabled() ) {
		            logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".performHttpRequest(" + method + "," + token + "," + endpoint + "," + timeout + ")");
		        }
			} 
			
			
			return null;
		} 
	

//	final static Gson gson = new Gson();


	public static DigitalOceanRestModel getModel(CloudProvider provider, DigitalOcean model) throws UnsupportedEncodingException, CloudException {
		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".getModel(" + provider + "," +  model + ")");
        }
			
		String token = (String)provider.getContext().getConfigurationValue("token");
    	
		try {		
			String s = performHttpRequest(RESTMethod.GET, token,  getApiUrl(provider) + getEndpoint(model), 15000);
			
			JSONObject jso = new JSONObject(s);
								
			return model.fromJson(jso);							
		} catch (Exception e) {			
			throw new CloudProviderException(e);
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
		} catch (Exception e) {			
			throw new CloudProviderException(e);
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
		String url = provider.getContext().getEndpoint();
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
		//if using V1 ...
		//byte[][] keyPair = (byte[][])provider.getContext().getConfigurationValue("apiKey");
        //String apiShared = new String(keyPair[0], "utf-8");
        //String apiSecret = new String(keyPair[1], "utf-8");
        
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
			throw new CloudProviderException(e);
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
		} catch (Exception e) {
			//Got Error...
			throw new CloudProviderException(e);
		} finally {
			if( logger.isTraceEnabled() ) {
	            logger.trace("EXIT - " + DigitalOceanModelFactory.class.getName() + ".performAction(" + provider + "," + returnObject + ")");
	        }
		}
	}
	
	public static Droplet createInstance(CloudProvider provider, String dropletName, String sizeId, String theImageId, String regionId, String bootstrapKey, HashMap<String, Object> extraParameters) throws UnsupportedEncodingException, CloudException {

		if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + DigitalOceanModelFactory.class.getName() + ".createInstance(" + dropletName + "," + sizeId + "," + theImageId + "," + regionId + "," + extraParameters + ")");
		}


		try {
			Create action = new Create(dropletName, sizeId, theImageId, regionId);
			ArrayList<Long> ssh_key_ids = new ArrayList<Long>();
			//Extra parameter is not part of DaseinCloud.... as its cloud specific
			if (extraParameters != null) {
				if (extraParameters.containsKey("ssh_key_ids")) {
					try {					
						ssh_key_ids = (ArrayList<Long>) extraParameters.get("ssh_key_ids");						
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
					ssh_key_ids.add(Long.valueOf(bootstrapKey));
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
