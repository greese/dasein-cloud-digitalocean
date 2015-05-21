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

package org.dasein.cloud.digitalocean.compute;


import org.dasein.cloud.*;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.digitalocean.DigitalOcean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Describes the capabilities of DigitalOcean with respect to Dasein image operations.
 *
 * @author Maria Pavlova
 * @author Francis Lavalliere
 * @version 2015.01 initial version
 * @since 2015.01
 */
public class DOImageCapabilities extends AbstractCapabilities<DigitalOcean> implements ImageCapabilities {

    public DOImageCapabilities(DigitalOcean cloud) {
        super(cloud);
    }

    @Override
    public boolean canBundle(@Nonnull VmState vmState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canImage(@Nonnull VmState vmState) throws CloudException, InternalException {
        return VmState.STOPPED.equals(vmState);
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

    @Nullable
    @Override
    public VisibleScope getImageVisibleScope() {
        return VisibleScope.ACCOUNT_REGION;
    }

    @Nonnull
    @Override
    public Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Nonnull
    @Override
    public Iterable<MachineImageFormat> listSupportedFormats() throws CloudException, InternalException {
        return Arrays.asList(MachineImageFormat.RAW);
    }

    @Nonnull
    @Override
    public Iterable<MachineImageFormat> listSupportedFormatsForBundling() throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<ImageClass> listSupportedImageClasses() throws CloudException, InternalException {
    	return Arrays.asList(ImageClass.MACHINE);
    }

    private final transient static List<MachineImageType> imageTypes = Arrays.asList(MachineImageType.VOLUME);

    @Nonnull @Override
    public Iterable<MachineImageType> listSupportedImageTypes() throws CloudException, InternalException {
        return imageTypes;
    }

    @Override
    public boolean supportsDirectImageUpload() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsImageCapture(@Nonnull MachineImageType machineImageType) {
        return imageTypes.contains(machineImageType);
    }

    @Override
    public boolean supportsImageCopy() throws CloudException, InternalException {
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
    public boolean supportsListingAllRegions() throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean supportsPublicLibrary(@Nonnull ImageClass imageClass) {
        return true;
    }

    @Override
    public boolean imageCaptureDestroysVM() throws CloudException, InternalException {
        return false;
    }
}
