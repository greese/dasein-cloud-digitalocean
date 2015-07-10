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
import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.digitalocean.models.Action;
import org.dasein.cloud.digitalocean.models.Droplet;
import org.dasein.cloud.digitalocean.models.Image;
import org.dasein.cloud.digitalocean.models.Images;
import org.dasein.cloud.digitalocean.models.actions.image.Destroy;
import org.dasein.cloud.digitalocean.models.actions.droplet.Snapshot;
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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.dasein.cloud.digitalocean.models.rest.DigitalOceanModelFactory.*;

public class DOImage extends AbstractImageSupport<DigitalOcean> {
    static private final Logger logger = Logger.getLogger(DOImage.class);
    private static final String DO_OWNER_ID = "--digitalocean--";

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
        APITrace.begin(getProvider(), "Image.capture");
        try {
            // TODO: replace with HEAD request (checkDropletExists)
            Droplet droplet = DigitalOceanModelFactory.getDropletByInstance(getProvider(), options.getVirtualMachineId());
            if( droplet == null ) {
                throw new InternalException("Virtual machine "+options.getVirtualMachineId()+" does not exist, unable to capture");
            }
            getProvider().getComputeServices().getVirtualMachineSupport().waitForAllDropletEventsToComplete(options.getVirtualMachineId(), 5);
            List<String> previousSnapshotIds = Arrays.asList(droplet.getSnapshotIds());
            Action action = performAction(getProvider(), new Snapshot(options.getName()), options.getVirtualMachineId());
            while( !action.isComplete() ) {
                try {
                    Thread.sleep(2000L);
                }
                catch( InterruptedException e ) {
                }
                if( action.isError() ) {
                    throw new CloudException(action.getStatus());
                }
                action = DigitalOceanModelFactory.getEventById(getProvider(), action.getId());
            }
            droplet = DigitalOceanModelFactory.getDropletByInstance(getProvider(), options.getVirtualMachineId());
            // create a new list as Arrays.asList returns an unmodifiable list
            List<String> newSnapshotIds = new ArrayList<String>();
            newSnapshotIds.addAll(Arrays.asList(droplet.getSnapshotIds()));
            newSnapshotIds.removeAll(previousSnapshotIds);

            // there may be more than one new snapshots so have to traverse to find the one
            for( String snapshotId : newSnapshotIds ) {
                Image image = (Image) DigitalOceanModelFactory.getModelById(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGE, snapshotId);
                if( options.getName().equalsIgnoreCase(image.getName()) ) {
                    // that's *probably* the one
                    return toImage(image);
                }
            }
            // not found any snapshot :-/
            throw new CloudException("Unable to create or find the captured image for VM "+options.getVirtualMachineId());
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<MachineImage> searchImages(String accountNumber, String keyword, Platform platform, Architecture architecture, ImageClass... imageClasses) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.searchImages");
        try {
            List<MachineImage> results = new ArrayList<MachineImage>();
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

    private @Nonnull Iterable<MachineImage> executeImageSearch(boolean publicImagesOnly, @Nonnull ImageFilterOptions options) throws CloudException, InternalException {
        APITrace.begin(provider, "Image.executeImageSearch");
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

            String cacheName = "ALL";
            if( !options.getWithAllRegions() ) {
                cacheName = regionId;
            }
            Cache<MachineImage> cache = Cache.getInstance(provider, "images" + ( publicImagesOnly ? "pub" : "prv" ) + "-" + cacheName, MachineImage.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Minute>(5, TimePeriod.MINUTE));
            Collection<MachineImage> cachedImages = ( Collection<MachineImage> ) cache.get(getContext());
            if( cachedImages != null ) {
                return cachedImages;
            }
            final List<MachineImage> results = new ArrayList<MachineImage>();
            final org.dasein.cloud.digitalocean.models.rest.DigitalOcean cmd = publicImagesOnly ? org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGES_PUBLIC : org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGES;

            Images images = (Images) getModel(getProvider(), cmd);
            int total = images.getTotal();
            int page = 1;
            while( images.getImages().size() > 0 ) { // let's check >0 just in case
                for( Image image : images.getImages() ) {
                    MachineImage machineImage = toImage(image);
                    // check if image regions match the requested region if any
                    if( !options.getWithAllRegions() && image.getRegions().length > 0 && !Arrays.asList(image.getRegions()).contains(getContext().getRegionId()) ) {
                        total--;
                        continue;
                    }

                    if( machineImage != null && publicImagesOnly == machineImage.isPublic() ) {
                        if( options.getWithAllRegions() ) {
                            // explode image to all regions, update total count
                            int regions = image.getRegions().length;
                            total += regions - 1;
                            for( String region : image.getRegions() ) {
                                machineImage.setProviderRegionId(region);
                                results.add(machineImage);
                                machineImage = toImage(image);
                            }
                        }
                        else {
                            // only add for one region as requested
                            results.add(machineImage);
                        }
                    }
                    else {
                        total--; // remove the defective image from the count
                    }
                }
                if( total <= 0 || total == results.size() ) {
                    break;
                }
                images = (Images) getModel(getProvider(), cmd, ++page);
            }
            cache.put(getContext(), results);
            return results;
        }
        catch (Throwable e) {
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

        MachineImageState mis = MachineImageState.ACTIVE;
        if( image.getRegions().length == 0 ) {
            mis = MachineImageState.DELETED;
        }
        Architecture arch = Architecture.I64;
        if (image.getName().contains("x32")) {
            arch = Architecture.I32;
        }

        Platform platform = Platform.guess(image.getDistribution());
        if( platform.equals(Platform.UNKNOWN) ) {
            platform = Platform.guess(image.getName());
        }
        String ownerId = getContext().getAccountNumber();
        if( image.getPublic() ) {
            ownerId = DO_OWNER_ID;
        }
        MachineImage machineImage = MachineImage.getInstance(
                ownerId,
                getContext().getRegionId(),
                image.getId(),
                ImageClass.MACHINE,
                mis,
                image.getName(),
                image.getDistribution(),
                arch,
                platform
        );
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

    private ResourceStatus toStatus(org.dasein.cloud.digitalocean.models.Image image) throws InternalException {
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
            Image image = (Image) getModelById(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGE, providerImageId);
            return toImage(image);
        }
        catch( CloudException e ) {
            if( e.getHttpCode() == 404 ) {
                return null;
            }
            throw e;
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
            return image.isPublic();
        } finally {
            APITrace.end();
        }
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        // TODO: send meta (HEAD) request to verify
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

            Images images = (Images) getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGES);
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
                images = (Images) getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGES, ++page);
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
    String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }

    @Override
    public void remove(@Nonnull String providerImageId, boolean checkState) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.remove");
        try {
            DigitalOceanModelFactory.performAction(getProvider(), new Destroy(), providerImageId);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public
    @Nonnull
    Iterable<MachineImage> listImages(final @Nullable ImageFilterOptions options) throws CloudException, InternalException {
        final ImageFilterOptions opts;

        if (options == null) {
            opts = ImageFilterOptions.getInstance();
        } else {
            opts = options;
        }
        provider.hold();
        PopulatorThread<MachineImage> populator = new PopulatorThread<MachineImage>(new JiteratorPopulator<MachineImage>() {
            @Override
            public void populate(@Nonnull Jiterator<MachineImage> iterator) throws Exception {
                APITrace.begin(getProvider(), "Image.listImages");
                try {
                    for (MachineImage img : executeImageSearch(false, opts)) {
                        if( options.matches(img) ) {
                            iterator.push(img);
                        }
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
                            if( options.matches(img) ) {
                                iterator.push(img);
                            }
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
