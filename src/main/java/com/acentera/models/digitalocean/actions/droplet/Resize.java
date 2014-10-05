
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
package com.acentera.models.digitalocean.actions.droplet;

import org.dasein.cloud.CloudException;
import org.json.JSONException;
import org.json.JSONObject;

import com.acentera.models.digitalocean.actions.ActionType;
import com.acentera.models.digitalocean.actions.DigitalOceanPostAction;

public class Resize extends DigitalOceanPostAction {	
	
	String size  = null;
	public Resize(String sizeSlug) {
		this.size = sizeSlug;
		actionType = ActionType.DROPLET;
	}
	
	
	@Override
	public String getType() {
		return "resize";
	}
	
	@Override
	public JSONObject getParameters() throws CloudException, JSONException {
		JSONObject postParam = getDefaultJSON();
		postParam.put("size", size);
		return postParam;
	}
	
}

