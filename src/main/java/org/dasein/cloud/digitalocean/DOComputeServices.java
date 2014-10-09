/**
 * Copyright (C) 2009-2014 Dell, Inc.
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

package org.dasein.cloud.digitalocean;

import org.dasein.cloud.compute.AbstractComputeServices;
import org.dasein.cloud.digitalocean.compute.DOInstance;
import org.dasein.cloud.digitalocean.compute.DOImage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DOComputeServices extends AbstractComputeServices {
    private DigitalOcean cloud;
    
    public DOComputeServices(@Nonnull DigitalOcean cloud) { this.cloud = cloud; }
    
    @Override
    public @Nonnull DOInstance getVirtualMachineSupport() {
        return new DOInstance(cloud);
    }
    
    @Override
    public @Nonnull DOImage getImageSupport() {
        return new DOImage(cloud);
    }
    
}
