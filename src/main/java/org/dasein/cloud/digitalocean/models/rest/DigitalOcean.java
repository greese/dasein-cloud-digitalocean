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

import java.util.Date;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.digitalocean.models.Action;
import org.dasein.cloud.digitalocean.models.Actions;
import org.dasein.cloud.digitalocean.models.Droplet;
import org.dasein.cloud.digitalocean.models.Droplets;
import org.dasein.cloud.digitalocean.models.IDigitalOcean;
import org.dasein.cloud.digitalocean.models.Image;
import org.dasein.cloud.digitalocean.models.Images;
import org.dasein.cloud.digitalocean.models.Key;
import org.dasein.cloud.digitalocean.models.Keys;
import org.dasein.cloud.digitalocean.models.Region;
import org.dasein.cloud.digitalocean.models.Regions;
import org.dasein.cloud.digitalocean.models.Size;
import org.dasein.cloud.digitalocean.models.Sizes;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public enum DigitalOcean implements IDigitalOcean {
	
	REGIONS,		
	REGION, 
	DROPLETS,
	DROPLET,
	SIZES,
	SIZE,	
	IMAGES,
	IMAGE,	
	//v1 only EVENTS,
	//v1 only EVENT,
	ACTIONS,
	ACTION,
	KEYS,
	KEY;
	

	static Gson gson = null;
	static GsonBuilder gsonBuilder = new GsonBuilder();	
	static {
		
		gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
		gson = gsonBuilder.create();
	}
	
	@Override
	public  String toString() {
		switch(this) {
		case REGIONS: return "v2/regions";
		case REGION: return "v2/regions/%s";
		case DROPLETS: return "v2/droplets";
		case DROPLET: return "v2/droplets/%s";
		case IMAGES: return "v2/images";
		case IMAGE: return "v2/images/%s";
		case SIZES: return "v2/sizes";
		case SIZE: return "v2/size/%s";
		case KEYS: return "v2/account/keys";
		case KEY: return "v2/account/keys/%s";
		//V2 EVENTS=ACTIONS
		//v1 only case EVENTS: 
		case ACTIONS:
				return "v2/actions";
		//V2 EVENT=ACTION
		//v2 only case EVENT: 
		case ACTION:
				return "v2/actions/%s";
		default: throw new IllegalArgumentException("DigialOcean endpoint not configured.");
		}		
	}

	public DigitalOceanRestModel fromJson(JSONObject jso) throws JsonSyntaxException, JSONException, CloudException {
		if (jso.has("id")) {
			//GOt an error
			throw new CloudException(jso.getString("message"));
		}
		
		switch(this) {
			case DROPLET: {
				
				Droplet drop = toDroplet(jso.getJSONObject("droplet"));
				return drop;
			}			
			case DROPLETS: {
				JSONArray jsArray = jso.getJSONArray("droplets");
				Droplets droplets = new Droplets();
						
				for (int i = 0; i < jsArray.length(); i++) {
					
					Droplet drop = toDroplet(jsArray.getJSONObject(i));					
					droplets.addDroplet(drop);
				}
				
				return droplets;
			}
			case SIZE: {
				Object u = gson.fromJson(jso.getJSONObject("size").toString(), Size.class);
				return (Size)u;
			}			
			case SIZES: {
				JSONArray jsArray = jso.getJSONArray("sizes");
				Sizes SIZES = new Sizes();
						
				for (int i = 0; i < jsArray.length(); i++) {
					Object u = gson.fromJson(jsArray.getJSONObject(i).toString(), Size.class);
					SIZES.addSize((Size)u);
				}
				
				return SIZES;
			}
			
			case REGIONS: {
				JSONArray jsArray = jso.getJSONArray("regions");
				Regions regions = new Regions();
						
				for (int i = 0; i < jsArray.length(); i++) {
					try {
						Object u = gson.fromJson(jsArray.getJSONObject(i).toString(), Region.class);
						regions.addRegion((Region)u);
					}catch (Exception ee) {
						ee.printStackTrace();
					}
				}
				
				return regions;
			}
			case REGION: {
				Object u = gson.fromJson(jso.getJSONObject("region").toString(), Region.class);
				return (Droplet)u;
			}
			
			//case EVENTS: throw new Depre
			case ACTIONS: {
				//V2 event = actions
				JSONArray jsArray = jso.getJSONArray("actions");
				Actions actions = new Actions();
						
				for (int i = 0; i < jsArray.length(); i++) {					
					Object u = gson.fromJson(jsArray.getJSONObject(i).toString(), Action.class);
					actions.addAction((Action)u);
					//lstRes.add((Region)u);
				}
				
				return actions;
			}
			
			//case EVENT:
			case ACTION: {
				Object u = gson.fromJson(jso.getJSONObject("action").toString(), Action.class);
				return (Action)u;
			}
			case KEY: {
				Object u = gson.fromJson(jso.getJSONObject("ssh_key").toString(), Key.class);
				return (Key)u;
			}			
			case KEYS: {
				JSONArray jsArray = jso.getJSONArray("ssh_keys");
				Keys KEYS = new Keys();
						
				for (int i = 0; i < jsArray.length(); i++) {
					Object u = gson.fromJson(jsArray.getJSONObject(i).toString(), Key.class);
					KEYS.addKey((Key)u);
				}
				
				return KEYS;
			}
			case IMAGE: {
				Object u = gson.fromJson(jso.getJSONObject("image").toString(), Image.class);
				return (Image)u;
			}			
			case IMAGES: {
				JSONArray jsArray = jso.getJSONArray("images");
				Images images = new Images();
						
				for (int i = 0; i < jsArray.length(); i++) {
					Object u = gson.fromJson(jsArray.getJSONObject(i).toString(), Image.class);
					images.addImage((Image)u);
				}
				
				return images;
			}
			default: throw new IllegalArgumentException("DigitalOcean JSON to Object not implemented");
		}		
	}

	private Droplet toDroplet(JSONObject jso) throws JsonSyntaxException, JSONException {
		Droplet drop = (Droplet)gson.fromJson(jso.toString(), Droplet.class);
		
		//V2
		//Get Networking
		try {			
			JSONObject net = jso.getJSONObject("networks");
			if (net.has("v4")) {
				JSONArray ipV4 = net.getJSONArray("v4");
				int len = ipV4.length();
				for (int z = 0; z < len; z++) {
					JSONObject netinfo = ipV4.getJSONObject(z);
					
					String ipAddress = netinfo.getString("ip_address");
					String type = netinfo.getString("type");
					if (type.compareToIgnoreCase("public") == 0) {
						drop.setPublicIp(ipAddress);
					} else { 
						drop.setPrivateIp(ipAddress);
					}														
				}
			}
			//TODO v6?
		} catch (Exception e) {
			//Couldnt find network?
		}
		
		
		
		

		return drop;
	}
}

