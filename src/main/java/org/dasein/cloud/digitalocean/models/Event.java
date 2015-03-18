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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.digitalocean.models.rest.DigitalOceanRestModel;


public class Event implements DigitalOceanRestModel {
	Long id;
	String percentage = "";
	String action_status;
	String message;
	Long droplet_id;
	Long event_type_id;
	

	public Long  getId() {
		return this.id;
	}
	
	public String getPercentage() {
		return this.percentage;
	}
	public void setPercentage(String pct) {
		this.percentage = pct;
	}
	
	public boolean isError() {
		try {
			return (this.action_status.compareTo("error") == 0);
		} catch (Exception ee) {
			return false;
		}
	}
	
	public void throwIfError() throws CloudException {		
		if (this.isError()) {
			throw new CloudException(getMessage());
		}		
	}
	
	public boolean isSuccess() {
		return !isError();
	}
	
	public String getActionStatus() {
		return this.action_status;
	}
	
	public void setActionStatus(String a) {
		this.action_status = a;
	}
	
	public void setMessage(String msg) {
		this.message = msg;
	}
	public String getMessage() {
		return this.message; 
	}

	public Long getDropletId() {
		return this.droplet_id;
	}
	public void setDropletId(Long id) {
		this.droplet_id = id;
	}
	
	public Long getEventTypeId() {
		return this.event_type_id;
	}	
	
	public void setEventTypeId(Long id) {
		this.event_type_id = id;
	}

	public boolean isComplete() {		
		String perc = this.getPercentage();
		if (perc.startsWith("100")) {
			return true;
		}
		return false;
	}
	
}
