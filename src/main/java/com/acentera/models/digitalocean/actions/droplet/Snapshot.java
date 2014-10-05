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

import com.acentera.models.digitalocean.actions.ActionType;
import com.acentera.models.digitalocean.actions.DigitalOceanPostAction;

public class Snapshot extends DigitalOceanPostAction {	
		
	public Snapshot() {
		actionType = ActionType.DROPLET;
	}
	@Override	
	public  String getType() {			
		return "power_on";
	}
	
}

