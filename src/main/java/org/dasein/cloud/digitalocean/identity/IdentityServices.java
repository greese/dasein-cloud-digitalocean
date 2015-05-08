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

package org.dasein.cloud.digitalocean.identity;

import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.digitalocean.identity.Keypairs;
import org.dasein.cloud.identity.AbstractIdentityServices;

public class IdentityServices extends AbstractIdentityServices {
    private DigitalOcean cloud;
    
    public IdentityServices(DigitalOcean cloud) { this.cloud = cloud; }

    @Override
    public Keypairs getShellKeySupport() {
        return new Keypairs(cloud);
    }
}