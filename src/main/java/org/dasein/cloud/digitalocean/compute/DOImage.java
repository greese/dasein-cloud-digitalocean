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

import org.apache.log4j.Logger;
import org.dasein.cloud.*;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.digitalocean.models.Image;
import org.dasein.cloud.digitalocean.models.Images;
import org.dasein.cloud.digitalocean.models.rest.DigitalOceanModelFactory;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.Jiterator;
import org.dasein.util.JiteratorPopulator;
import org.dasein.util.PopulatorThread;
import org.dasein.util.uom.time.Minute;
import org.dasein.util.uom.time.TimePeriod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class DOImage extends AbstractImageSupport<DigitalOcean> {
    static private final Logger logger = Logger.getLogger(DOImage.class);

    private DigitalOcean provider = null;
    private volatile transient ImageCapabilities capabilities;

    public DOImage(DigitalOcean provider) {
        super(provider);
        this.provider = provider;
    }

    @Override
    public void addImageShare(@Nonnull String providerImageId, @Nonnull String accountNumber) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image sharing is not supported by "+getProvider().getCloudName());
    }

    @Override
    public void addPublicShare(@Nonnull String providerImageId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image sharing is not supported by "+getProvider().getCloudName());
    }

    @Override
    public ImageCapabilities getCapabilities() {
        if (capabilities == null) {
            capabilities = new DOImageCapabilities(provider);
        }
        return capabilities;
    }

    @Override
    protected MachineImage capture(@Nonnull ImageCreateOptions options, @Nullable AsynchronousTask<MachineImage> task) throws CloudException, InternalException {
        ProviderContext ctx = provider.getContext();
        if (ctx == null) {
            throw new CloudException("No context was set for this request");
        }
        return captureImage(ctx, options, task);
    }

    private
    @Nonnull
    MachineImage captureImage(@Nonnull ProviderContext ctx, @Nonnull ImageCreateOptions options, @Nullable AsynchronousTask<MachineImage> task) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image capture is not supported by "+getProvider().getCloudName());
    }

    @Override
    public
    @Nonnull
    Iterable<MachineImage> searchImages(String accountNumber, String keyword, Platform platform, Architecture architecture, ImageClass... imageClasses) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.searchImages");
        try {
            ArrayList<MachineImage> results = new ArrayList<MachineImage>();
            Collection<MachineImage> images = new ArrayList<MachineImage>();
            if (accountNumber == null) {
                images.addAll((Collection<MachineImage>) searchPublicImages(ImageFilterOptions.getInstance()));
            }
            images.addAll((Collection<MachineImage>) listImages(ImageFilterOptions.getInstance()));

            for (MachineImage image : images) {
                if (image != null) {
                    if (keyword != null) {
                        if (!image.getProviderMachineImageId().contains(keyword) && !image.getName().contains(keyword) && !image.getDescription().contains(keyword)) {
                            continue;
                        }
                    }
                    if (platform != null) {
                        Platform p = image.getPlatform();

                        if (!platform.equals(p)) {
                            if (platform.isWindows()) {
                                if (!p.isWindows()) {
                                    continue;
                                }
                            } else if (platform.equals(Platform.UNIX)) {
                                if (!p.isUnix()) {
                                    continue;
                                }
                            } else {
                                continue;
                            }
                        }
                    }
                    if (architecture != null) {
                        if (architecture != image.getArchitecture()) {
                            continue;
                        }
                    }
                    results.add(image);
                }
            }

            return results;
        } finally {
            APITrace.end();
        }
    }

    private
    @Nonnull
    Iterable<MachineImage> executeImageSearch(boolean sharedImages, @Nonnull ImageFilterOptions options) throws CloudException, InternalException {
        APITrace.begin(provider, "Image.executeImageSearch");

        // this method only works for machine images in DO
        if( options != null && options.getImageClass() != null && !options.getImageClass().equals(ImageClass.MACHINE)) {
            return Collections.emptyList();
        }

        try {
            final String regionId = getContext().getRegionId();
            if( regionId == null ) {
                throw new CloudException("No region was set for this request");
            }

            Architecture architecture = options.getArchitecture();
            if( architecture != null && !architecture.equals(Architecture.I32) && !architecture.equals(Architecture.I64) ) {
                if( !options.isMatchesAny() ) {
                    return Collections.emptyList();
                }
            }

            Cache<MachineImage> cache = Cache.getInstance(provider, "images" + ( sharedImages ? "pub" : "prv" ) + "-" + regionId, MachineImage.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Minute>(5, TimePeriod.MINUTE));
            Collection<MachineImage> cachedImages = ( Collection<MachineImage> ) cache.get(getContext());
            if( cachedImages != null ) {
                return cachedImages;
            }
            final List<MachineImage> results = new ArrayList<MachineImage>();
            final org.dasein.cloud.digitalocean.models.rest.DigitalOcean cmd = sharedImages ? org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGES_PUBLIC : org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGES;

            Images images = (Images) DigitalOceanModelFactory.getModel(getProvider(), cmd);
            int total = images.getTotal();
            int page = 1;
            while( true ) {
                for( Image image : images.getImages() ) {
                    MachineImage machineImage = toImage(image);
                    if( machineImage != null && options.matches(machineImage) ) {
                        results.add(machineImage);
                    }
                    else {
                        total--; // remove the defective image from the count
                    }
                }
                if( total <= 0 || total == results.size() ) {
                    break;
                }
                images = (Images) DigitalOceanModelFactory.getModel(getProvider(), cmd, ++page);
            }

//            cache.put(getContext(), results);
            return results;
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            throw new CloudException(e);
        } finally {
            APITrace.end();
        }
    }


    private MachineImage toImage(org.dasein.cloud.digitalocean.models.Image image) throws InternalException, CloudException {
        if (image == null) {
            return null;
        }
        if( image.getRegions().length > 0 && !Arrays.asList(image.getRegions()).contains(getContext().getRegionId()) ) {
            return null;
        }

        MachineImageState mis = MachineImageState.ACTIVE;
        if( image.getRegions().length == 0 ) {
            mis = MachineImageState.DELETED;
        }
        Architecture arch = Architecture.I64;
        if (image.getName().contains("x32")) {
            arch = Architecture.I32;
        }

        Platform platform = Platform.UNKNOWN;
        if (image.getDistribution().compareToIgnoreCase("Ubuntu") == 0) {
            platform = Platform.UBUNTU;
        } else if (image.getDistribution().compareToIgnoreCase("CentOS") == 0) {
            platform = Platform.CENT_OS;
        } else if (image.getDistribution().compareToIgnoreCase("Fedora") == 0) {
            platform = Platform.FEDORA_CORE;
        }

        MachineImage machineImage = MachineImage.getInstance(
                getContext().getAccountNumber(),
                getContext().getRegionId(),
                image.getId(),
                ImageClass.MACHINE,
                mis,
                image.getName(),
                image.getDistribution(),
                arch,
                platform
        );
        machineImage.withStorageFormat(MachineImageFormat.RAW);
        String software = null;
        int pos = image.getName().indexOf(" on ");
        if (pos >= 1) {
            software = image.getName().substring(0, pos);
        }
        if (software != null) {
            machineImage.withSoftware(software);
        }
        if( image.getPublic() ) {
            machineImage.sharedWithPublic();
        }
        return machineImage;
    }

    private ResourceStatus toStatus(org.dasein.cloud.digitalocean.models.Image image) throws InternalException, CloudException {
        if (image == null) {
            return null;
        }
        if( image.getRegions().length > 0 && !Arrays.asList(image.getRegions()).contains(getContext().getRegionId()) ) {
            return null;
        }

        MachineImageState state = MachineImageState.DELETED;
        String vmId = String.valueOf(image.getId());
        if( image.getRegions().length > 0 ) {
            state = MachineImageState.ACTIVE;
        }
        return new ResourceStatus(vmId, state);
    }

    @Override
    public @Nullable MachineImage getImage(@Nonnull String providerImageId) throws CloudException, InternalException {
        APITrace.begin(provider, "Image.getImage");
        try {
            ProviderContext ctx = provider.getContext();

            if (ctx == null) {
                throw new CloudException("No context was set for this request");
            }
            Image image = (Image) DigitalOceanModelFactory.getModelById(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGE, providerImageId);
            return toImage(image);
        }
        catch( UnsupportedEncodingException e ) {
            throw new CloudException(e);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public boolean isImageSharedWithPublic(@Nonnull String machineImageId) throws CloudException, InternalException {
        APITrace.begin(provider, "Image.isImageSharedWithPublic");
        try {
            MachineImage image = getImage(machineImageId);

            if (image == null) {
                return false;
            }
            String p = (String) image.getTag("public");

            return (p != null && p.equalsIgnoreCase("true"));
        } finally {
            APITrace.end();
        }
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Override
    public @Nonnull Iterable<ResourceStatus> listImageStatus(final @Nonnull ImageClass cls) throws CloudException, InternalException {
        if( !cls.equals(ImageClass.MACHINE) ) {
            return Collections.emptyList();
        }

        provider.hold();
        PopulatorThread<ResourceStatus> populator = new PopulatorThread<ResourceStatus>(new JiteratorPopulator<ResourceStatus>() {
            @Override
            public void populate(@Nonnull Jiterator<ResourceStatus> iterator) throws Exception {
                APITrace.begin(provider, "Image.listImageStatus");
                try {
                    for (ResourceStatus status : executeStatusList(cls)) {
                        iterator.push(status);
                    }
                }
                finally {
                    provider.release();
                    APITrace.end();
                }
            }
        });

        populator.populate();
        return populator.getResult();

    }

    private @Nonnull Iterable<ResourceStatus> executeStatusList(@Nonnull ImageClass cls) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.executeStatusList");
        try {
            List<ResourceStatus> results = new ArrayList<ResourceStatus>();

            Images images = (Images) DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGES);
            int total = images.getTotal();
            int page = 1;
            while( true ) {
                for( Image image: images.getImages() ) {
                    ResourceStatus status = toStatus(image);
                    if (status != null ) {
                        results.add(status);
                    }
                    else {
                        total--;
                    }
                }
                if( total <= 0 || total == results.size() ) {
                    break;
                }
                images = (Images) DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGES, ++page);
            }

            return results;
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            throw new CloudException(e);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public
    @Nonnull
    Iterable<String> listShares(@Nonnull String forMachineImageId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image sharing not supported by " + getProvider().getCloudName());
    }

    @Override
    public
    @Nonnull
    String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }

    @Override
    public
    @Nonnull
    MachineImage registerImageBundle(@Nonnull ImageCreateOptions options) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image bundling not supported by " + getProvider().getCloudName());
    }

    @Override
    public void remove(@Nonnull String providerImageId, boolean checkState) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image removal not supported by " + getProvider().getCloudName());
    }

    @Override
    public void removeAllImageShares(@Nonnull String providerImageId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image sharing not supported by " + getProvider().getCloudName());
    }

    @Override
    public void removeImageShare(@Nonnull String providerImageId, @Nonnull String accountNumber) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image sharing not supported by " + getProvider().getCloudName());
    }

    @Override
    public void removePublicShare(@Nonnull String providerImageId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image sharing not supported by " + getProvider().getCloudName());
    }

    @Override
    public
    @Nonnull
    Iterable<MachineImage> listImages(@Nullable ImageFilterOptions options) throws CloudException, InternalException {
        final ImageFilterOptions opts;

        if (options == null) {
            opts = ImageFilterOptions.getInstance();
        } else {
            opts = options;
        }
        // this method only works for machine images in DO
        if( opts.getImageClass() != null && !opts.getImageClass().equals(ImageClass.MACHINE)) {
            return Collections.emptyList();
        }

        provider.hold();
        PopulatorThread<MachineImage> populator = new PopulatorThread<MachineImage>(new JiteratorPopulator<MachineImage>() {
            @Override
            public void populate(@Nonnull Jiterator<MachineImage> iterator) throws Exception {
                APITrace.begin(getProvider(), "Image.listImages");
                try {
                    for (MachineImage img : executeImageSearch(false, opts)) {
                        iterator.push(img);
                    }
                }
                finally {
                    provider.release();
                    APITrace.end();
                }
            }
        });

        populator.populate();
        return populator.getResult();
    }

    @Override
    public
    @Nonnull
    Iterable<MachineImage> searchPublicImages(final @Nonnull ImageFilterOptions options) throws CloudException, InternalException {
        provider.hold();
        PopulatorThread<MachineImage> populator = new PopulatorThread<MachineImage>(new JiteratorPopulator<MachineImage>() {
            @Override
            public void populate(@Nonnull Jiterator<MachineImage> iterator) throws Exception {
                APITrace.begin(getProvider(), "searchPublicImages");
                try {
                    try {
                        for (MachineImage img : executeImageSearch(true, options)) {
                            iterator.push(img);
                        }
                    } finally {
                        provider.release();
                    }
                } finally {
                    APITrace.end();
                }
            }
        });

        populator.populate();
        return populator.getResult();
    }

    @Override
    public void updateTags(@Nonnull String imageId, @Nonnull Tag... tags) throws CloudException, InternalException {
        updateTags(new String[]{imageId}, tags);
    }

    @Override
    public void updateTags(@Nonnull String[] imageIds, @Nonnull Tag... tags) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image tagging not supported by " + getProvider().getCloudName());
    }

    @Override
    public void removeTags(@Nonnull String imageId, @Nonnull Tag... tags) throws CloudException, InternalException {
        removeTags(new String[]{imageId}, tags);
    }

    @Override
    public void removeTags(@Nonnull String[] imageIds, @Nonnull Tag... tags) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image tagging not supported by " + getProvider().getCloudName());
    }

}
