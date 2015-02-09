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

import com.google.gson.annotations.SerializedName;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.digitalocean.models.rest.DigitalOceanRestModel;

import javax.annotation.Nullable;


public class Droplet implements DigitalOceanRestModel {
	String id;
	String name;
	Image image;
	Region region;

	Size size;
    @SerializedName("size_slug")
    String sizeSlug; // this is a workaround for a minimised response on droplet create

	Networks networks;
	String status;

    public String getSizeSlug() {
        return sizeSlug;
    }

    public String getId() {

		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public Size getSize() {
		return size;
	}

	public Image getImage() {
		return image;
	}

    public Networks getNetworks() {
        return networks;
    }

    public Region getRegion() {
		return region;
	}
	
	public @Nullable VmState getStatus() {
		if( "active".equals(status) ) {
			return VmState.RUNNING;
		}
        else if( "new".equals(status) ) {
			return VmState.PENDING;
		}
        else if( "off".equals(status) ) {
			return VmState.STOPPED;
		}
        else if( "archive".equals(status) ) {
			return VmState.TERMINATED;
		}
		return null;
	}

}
