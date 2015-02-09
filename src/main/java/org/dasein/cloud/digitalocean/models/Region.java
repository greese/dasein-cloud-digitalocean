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

package org.dasein.cloud.digitalocean.models;

import org.dasein.cloud.digitalocean.models.rest.DigitalOceanRestModel;

//{"name":"Amsterdam 3","features":["virtio","private_networking","backups","ipv6","metadata"],"sizes":["512mb","1gb","2gb","4gb","8gb","16gb","32gb","48gb","64gb"],"slug":"ams3","available":true}
public class Region implements DigitalOceanRestModel {
	String id;
	String name;
	String slug;
	Boolean available;
	String[] sizes;
	String[] features;

	public String getId() {
		return this.slug;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getSlug() {
		return this.slug;
	}
	
	public Boolean getActive() {
		return this.available;
	}
	
	public Boolean getAvailable() {
		return this.available;
	}
}
