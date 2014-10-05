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

import com.acentera.models.DigitalOceanRestModel;

public class Key implements DigitalOceanRestModel {
	String id;
	String name;
	String fingerprint;
	String public_key;
	
	public String getId() {
		return this.id;
	}
	public void setId(String s) {
		this.id = s;
	}
	public String getName() {
		return this.name;
	}		

	public void setName(String s) {
		this.name = s;
	}		

	public String getFingerprint() {
		return this.fingerprint;
	}		

	public void setFingerprint(String s) {
		this.fingerprint = s;
	}

	public String getPublicKey() {
		return this.public_key;
	}		

	public void setPublicKey(String s) {
		this.public_key = s;
	}
	
}

