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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.digitalocean.models.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public enum DigitalOcean implements IDigitalOcean {
	
	REGIONS,		
	REGION, 
	DROPLETS,
	DROPLET,
    DROPLET_ACTIONS,
	SIZES,
	SIZE,
    IMAGES_PUBLIC, // only public
    IMAGES, // all images
	IMAGE,
    IMAGE_ACTIONS,
	ACTIONS,
	ACTION,
	KEYS,
	KEY;

    static Gson gson = new Gson();

	@Override
	public  String toString() {
		switch(this) {
		case REGIONS: return "v2/regions";
		case REGION: return "v2/regions/%s";
		case DROPLETS: return "v2/droplets";
        case DROPLET: return "v2/droplets/%s";
        case DROPLET_ACTIONS: return DROPLET + "/actions";
		case IMAGES_PUBLIC: return "v2/images/?public=true";
        case IMAGES: return "v2/images/?private=true";
		case IMAGE: return "v2/images/%s";
        case IMAGE_ACTIONS: return IMAGE + "/actions";
		case SIZES: return "v2/sizes";
		case SIZE: return "v2/size/%s";
		case KEYS: return "v2/account/keys";
		case KEY: return "v2/account/keys/%s";
		case ACTIONS: return "v2/actions";
		case ACTION: return "v2/actions/%s";
		default: throw new IllegalArgumentException("DigitalOcean endpoint not configured.");
		}		
	}

	public DigitalOceanRestModel fromJson(JSONObject jso) throws JsonSyntaxException, JSONException, CloudException {
		if (jso.has("id")) {
			//GOt an error
			throw new CloudException(jso.getString("message"));
		}
		
		switch(this) {
			case DROPLET: {
				return gson.fromJson(jso.getJSONObject("droplet").toString(), Droplet.class);
			}

			case DROPLETS: {
				JSONArray jsArray = jso.getJSONArray("droplets");
				Droplets droplets = new Droplets();
				for (int i = 0; i < jsArray.length(); i++) {
					Object u = gson.fromJson(jsArray.getJSONObject(i).toString(), Droplet.class);
					droplets.addDroplet((Droplet)u);
				}
				return droplets;
			}

			case SIZE: {
				return gson.fromJson(jso.getJSONObject("size").toString(), Size.class);
			}

			case SIZES: {
				JSONArray jsArray = jso.getJSONArray("sizes");
				Sizes sizes = new Sizes();
				for (int i = 0; i < jsArray.length(); i++) {
					Object u = gson.fromJson(jsArray.getJSONObject(i).toString(), Size.class);
					sizes.addSize((Size)u);
				}
				return sizes;
			}
			
			case REGIONS: {
				JSONArray jsArray = jso.getJSONArray("regions");
				Regions regions = new Regions();
				for (int i = 0; i < jsArray.length(); i++) {
					try {
						regions.addRegion(gson.fromJson(jsArray.getJSONObject(i).toString(), Region.class));
					} catch (Exception ignore) {
					}
				}
				return regions;
			}

			case REGION: {
				return gson.fromJson(jso.getJSONObject("region").toString(), Region.class);
			}

            case DROPLET_ACTIONS:
            case IMAGE_ACTIONS:
			case ACTIONS: {
				//V2 event = actions
				JSONArray jsArray = jso.getJSONArray("actions");
				Actions actions = new Actions();
				for (int i = 0; i < jsArray.length(); i++) {
					Object u = gson.fromJson(jsArray.getJSONObject(i).toString(), Action.class);
					actions.addAction((Action)u);
				}
				return actions;
			}
			
			case ACTION: {
				return gson.fromJson(jso.getJSONObject("action").toString(), Action.class);
			}

			case KEY: {
				return gson.fromJson(jso.getJSONObject("ssh_key").toString(), Key.class);
			}

			case KEYS: {
				JSONArray jsArray = jso.getJSONArray("ssh_keys");
				Keys keys = new Keys();
				for (int i = 0; i < jsArray.length(); i++) {
					Object u = gson.fromJson(jsArray.getJSONObject(i).toString(), Key.class);
					keys.addKey((Key)u);
				}
				return keys;
			}

			case IMAGE: {
				return gson.fromJson(jso.getJSONObject("image").toString(), Image.class);
			}

            case IMAGES:
			case IMAGES_PUBLIC: {
				JSONArray jsArray = jso.getJSONArray("images");
				Images images = new Images();
						
				for (int i = 0; i < jsArray.length(); i++) {
					Object u = gson.fromJson(jsArray.getJSONObject(i).toString(), Image.class);
					images.addImage((Image)u);
				}
                JSONObject meta = jso.getJSONObject("meta");
                if( meta.has("total") ) {
                    images.setTotal(meta.getInt("total"));
                }
				
				return images;
			}

			default:
                throw new IllegalArgumentException("DigitalOcean JSON to Object not implemented");
		}		
	}
}

