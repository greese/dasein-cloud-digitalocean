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

package org.dasein.cloud.digitalocean;

import org.dasein.cloud.CloudException;

import javax.annotation.Nonnull;

/**
 * An error in configuring DigitalOcean's context in some manner.
 * <p>Created by George Reese: 04/21/2014 11:17 AM</p>
 * @author George Reese
 * @version 2014.7 initial version
 * @since 2014.7
 */
public class ConfigurationException extends CloudException {
    public ConfigurationException(@Nonnull String message) {
        super(message);
    }

    public ConfigurationException(@Nonnull Throwable cause) {
        super(cause);
    }
}
