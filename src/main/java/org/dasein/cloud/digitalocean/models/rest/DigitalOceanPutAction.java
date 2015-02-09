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

package org.dasein.cloud.digitalocean.models.rest;

import org.dasein.cloud.CloudException;
import org.json.JSONException;
import org.json.JSONObject;


public class DigitalOceanPutAction extends DigitalOceanAction {
	
	public DigitalOceanPutAction() {
		method = RESTMethod.PUT;
	}
		
	public String getType() {
		return "unknown";
	}
	
	public JSONObject getDefaultJSON() throws CloudException, JSONException {
		JSONObject postParameter = new JSONObject();
		postParameter.put("type",  getType());
		return postParameter;
	}
	
	public JSONObject getPara() throws CloudException, JSONException {
		return getDefaultJSON();	
	}
	
}

