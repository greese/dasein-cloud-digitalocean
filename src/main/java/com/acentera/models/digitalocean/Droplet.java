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
package com.acentera.models.digitalocean;

import java.util.HashMap;

import org.dasein.cloud.compute.VmState;

import com.acentera.models.DigitalOceanRestModel;

public class Droplet implements DigitalOceanRestModel {
	Long id;
	String name;
	Long image_id;
	Long region_id;
	Size size;
	String ip_address;
	String private_ip_address;
	String status;
	Long event_id;
	HashMap<String, Object> attr = new HashMap<String,Object>();
 	

	public Long  getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getSize() {
		return this.size.getSlug();
	}
	public void setSize(Size s) {
		this.size = s;
	}
	
	public Long getImageId() {
		return this.image_id;
	}

	public Long getRegionId() {
		return this.region_id;
	}
	
	public String getPrivateIp() {
		if (this.private_ip_address == null) {
			return this.getIp();
		} else {
			if (this.private_ip_address.trim().compareTo("") == 0) {
				return this.getIp();
			}
			return this.private_ip_address;
		}
	}
	
	public String getIp() {
		return this.ip_address;
	}
	
	public VmState getStatus() {
		if (this.status.compareTo("active") == 0) {
			return VmState.RUNNING;
		} else if (this.status.compareTo("new") == 0) {
			return VmState.PENDING;
		} else if (this.status.compareTo("off") == 0) {
			return VmState.STOPPED;
		} else if (this.status.compareTo("archive") == 0) {
			return VmState.TERMINATED;
		}
		return null;
	}

	public void setEventId(long long1) {
		this.event_id = long1;		
	}	
	public Long getEventId() {
		return this.event_id;
	}

	public String getProvider() {
		return "DO";
	}

	public void setAttribute(String str, Object val) {
		// TODO Auto-generated method stub
		attr.put(str,  val);
	}
	public Object getAttribute(String key) {
		if (attr.containsKey(key)){ 
			return attr.get(key);
		} else {
			return "";
		}
	}
	
}
