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

package org.dasein.cloud.digitalocean.compute;


import org.dasein.cloud.AbstractCapabilities;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.digitalocean.DigitalOcean;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Describes the capabilities of AWS with respect to Dasein image operations.
 * <p>Created by Francis Lavalliere: 10/05/2014 14:40</p>
 *
 * @author Francis Lavalliere
 * @version 2014.08 initial version
 * @since 2014.08
 */
public class DOImageCapabilities extends AbstractCapabilities<DigitalOcean> implements ImageCapabilities {

    public DOImageCapabilities(DigitalOcean cloud) {
        super(cloud);
    }

    @Override
    public boolean canBundle(@Nonnull VmState vmState) throws CloudException, InternalException {
        return false;//VmState.RUNNING.equals(vmState);
    }

    @Override
    public boolean canImage(@Nonnull VmState vmState) throws CloudException, InternalException {
        return false;//
    }

    @Nonnull
    @Override
    public String getProviderTermForImage(@Nonnull Locale locale, @Nonnull ImageClass imageClass) {
        return "image";
    }

    @Nonnull
    @Override
    public String getProviderTermForCustomImage(@Nonnull Locale locale, @Nonnull ImageClass imageClass) {
        return getProviderTermForImage(locale, imageClass);
    }

    @Nonnull
    @Override
    public Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Nonnull
    @Override
    public Iterable<MachineImageFormat> listSupportedFormats() throws CloudException, InternalException {
        return null;
    }

    @Nonnull
    @Override
    public Iterable<MachineImageFormat> listSupportedFormatsForBundling() throws CloudException, InternalException {
        return null;
    }

    @Nonnull
    @Override
    public Iterable<ImageClass> listSupportedImageClasses() throws CloudException, InternalException {
    	return null;
    }

    @Nonnull
    @Override
    public Iterable<MachineImageType> listSupportedImageTypes() throws CloudException, InternalException {
    	return null;
    }

    @Override
    public boolean supportsDirectImageUpload() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsImageCapture(@Nonnull MachineImageType machineImageType) {
        return false;
    }

    @Override
    public boolean supportsImageSharing() {
        return false;
    }

    @Override
    public boolean supportsImageSharingWithPublic() {
        return false;
    }

    @Override
    public boolean supportsPublicLibrary(@Nonnull ImageClass imageClass) {
        return false;
    }
}
