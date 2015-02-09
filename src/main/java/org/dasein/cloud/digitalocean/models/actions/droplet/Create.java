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
package org.dasein.cloud.digitalocean.models.actions.droplet;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.digitalocean.models.rest.DigitalOceanPostAction;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Create extends DigitalOceanPostAction {	

	//Required
	String name = "";
	String size = null;
	String image = null;	
	String region = null;
	
	//Optional
	boolean ipv6 = false;
	ArrayList<Long> ssh_key_ids = new ArrayList<Long>();
	boolean private_networking = true;
	boolean backups_enabled = false;
	
	public Create( String name, String size, String image_or_imageId, String region_slug_or_id) {
		this.name = name;
		this.image = image_or_imageId;
		this.size = size;
		this.region = region_slug_or_id;		
	}	
	
	public Create(String dropletName, Integer sizeId, int theImageId,
			Long regionId) {
		this.name = dropletName;
		this.image = String.valueOf(theImageId);
		this.size = String.valueOf(sizeId);
		this.region = String.valueOf(regionId);
	}

	public void setSshKeyIds(List<Long> keyIds) {
		Iterator<Long> itr = keyIds.iterator();
		ssh_key_ids.clear();
		ssh_key_ids.addAll(keyIds);
	}
	
	public void setPrivateNetworking(boolean b) {		
		this.private_networking = b;		
	}
	
	public boolean getPrivateNetworking() {		
		return this.private_networking;		
	}
	
	public void setBackups(boolean enabled) {		
		this.backups_enabled = enabled;		
	}
	
	public boolean getBackupsEnabled() {		
		return this.backups_enabled;		
	}
	
	
	
	public JSONObject getParameters() throws CloudException, JSONException {
		JSONObject postData = new JSONObject();

		if (this.name == null) {
			throw new CloudException("Missing required parameter 'name'");
		}
		postData.put("name",  this.name);
		
		if (this.size == null) {
			throw new CloudException("Missing required parameter 'size' for 'id' or 'slug' value");
		}		
		postData.put("size",  this.size);
		
		if (this.image == null) {
			throw new CloudException("Missing required parameter image for 'id' or 'slug' value");
		}
		
		postData.put("image",  this.image);
		
		if (this.region == null) {
			throw new CloudException("Missing required parameter 'region' for 'id' or 'slug' value");
		}		
		
		postData.put("region",  this.region);
		
		if (this.ssh_key_ids != null) {
			if (!this.ssh_key_ids.isEmpty()) { 
				postData.put("ssh_keys", this.ssh_key_ids);
			}
		}

		postData.put("private_networking", this.private_networking);			
		postData.put("backups", this.backups_enabled);
		postData.put("ipv6", this.ipv6);
			
		return postData;
	}
	
	@Override
	public  String toString() {			
		return "v2/droplets";
	}
	
}

