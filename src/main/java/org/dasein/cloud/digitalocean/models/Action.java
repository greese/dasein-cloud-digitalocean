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

package org.dasein.cloud.digitalocean.models;

import org.dasein.cloud.digitalocean.models.rest.DigitalOceanRestModel;

public class Action implements DigitalOceanRestModel {
	Long id;
	String status;
	String type;
	String started_at;
	String completed_at;
	String resource_id;
	String resource_type;
	String region;
	
	public Long getId() {
		return this.id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getStatus() {
		return this.status;
	}
	public void setStatus(String s) {
		this.status = s;
	}
	
	public String getType() {
		return this.type;
	}
	public void setType(String s) {
		this.type = s;
	}
	
	public String getStartedAt() {
		return this.started_at;
	}
	public void setStartedAt(String s) {
		this.started_at = s;
	}
	
	public String getCompletedAt() {
		return this.completed_at;
	}
	public void setCompletedAt(String s) {
		this.completed_at = s;
	}
	
	
	public String getResouurceId() {
		return this.resource_id;
	}
	public void setResourceId(String s) {
		this.resource_id = s;
	}
	
	public String getResouurceType() {
		return this.resource_type;
	}
	public void setResourceType(String s) {
		this.resource_type = s;
	}
	
	public String getRegion() {
		return this.region;
	}
	public void setREgion(String r) {
		this.region = r;
	}
	
	public boolean isComplete() {
		//Mark as completed if completed or errored
		if ( (this.getStatus().compareTo("completed") == 0) || (this.getStatus().compareTo("errored") == 0)) {
			return true;
		}		
		return false;
	}
	
	public boolean isError() {
		if (this.getStatus().compareTo("errored") == 0) {
			return true;
		}		
		return false;
	}
	
	
}
