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
    Iterable<MachineImage> executeImageSearch(int pass, boolean forPublic, @Nonnull ImageFilterOptions options) throws CloudException, InternalException {
        APITrace.begin(provider, "Image.executeImageSearch");
        try {
            final ProviderContext ctx = provider.getContext();

            if (ctx == null) {
                throw new CloudException("No context was set for this request");
            }
            final String regionId = ctx.getRegionId();
            if (regionId == null) {
                throw new CloudException("No region was set for this request");
            }

            Architecture architecture = options.getArchitecture();

            if (architecture != null && !architecture.equals(Architecture.I32) && !architecture.equals(Architecture.I64)) {
                if (!options.isMatchesAny()) {
                    return Collections.emptyList();
                }
            }

            Cache<MachineImage> cache = Cache.getInstance(provider, "images"+regionId, MachineImage.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Minute>(5, TimePeriod.MINUTE));
            Collection<MachineImage> imgList = (Collection<MachineImage>) cache.get(ctx);

            final List<MachineImage> listAll = new ArrayList<MachineImage>();
            if (imgList == null) {
                try {
                    Images images = (Images) DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGES);
                    if (images != null) {
                        Set<org.dasein.cloud.digitalocean.models.Image> s = images.getImages();
                        Iterator<org.dasein.cloud.digitalocean.models.Image> itr = s.iterator();
                        while (itr.hasNext()) {
                            org.dasein.cloud.digitalocean.models.Image d = itr.next();
                            MachineImage machineImage = toImage(d);
                            if( machineImage != null ) {
                                listAll.add(machineImage);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    throw new CloudException(e);
                }

                cache.put(ctx, listAll);
                imgList = listAll;
            }

            // now build a filtered list of images
            final List<MachineImage> res = new ArrayList<MachineImage>();

            Iterator<MachineImage> itrMachine = imgList.iterator();
            while (itrMachine.hasNext()) {
                MachineImage image = itrMachine.next();
                if (image != null) {
                    if( options != null ) {
                        if( options.matches(image) ) {
                            res.add(image);
                        }
                    }
                    else {
                        res.add(image);
                    }
                }
            }
            return res;
        } finally {
            APITrace.end();
        }
    }

    private MachineImage toImage(org.dasein.cloud.digitalocean.models.Image instance) {
        if (instance == null) {
            return null;
        }

        ProviderContext ctx = getProvider().getContext();
        String regionId = ctx.getRegionId();

        if( !Arrays.asList(instance.getRegions()).contains(regionId) ) {
            return null;
        }

        MachineImageState mis = MachineImageState.ACTIVE;

        Architecture arch = Architecture.I64;
        if (instance.getName().contains("x32")) {
            arch = Architecture.I32;
        }

        Platform platform = Platform.UNKNOWN;
        if (instance.getDistribution().compareToIgnoreCase("Ubuntu") == 0) {
            platform = Platform.UBUNTU;
        } else if (instance.getDistribution().compareToIgnoreCase("CentOS") == 0) {
            platform = Platform.CENT_OS;
        } else if (instance.getDistribution().compareToIgnoreCase("Fedora") == 0) {
            platform = Platform.FEDORA_CORE;
        }

        MachineImage image = MachineImage.getImageInstance(
                ctx.getAccountNumber(),
                regionId,
                instance.getId(),
                ImageClass.MACHINE,
                mis,
                instance.getName(),
                instance.getDistribution(),
                arch,
                platform,
                MachineImageFormat.RAW
        );
        String software = null;
        int pos = instance.getName().indexOf(" on ");
        if (pos >= 1) {
            software = instance.getName().substring(0, pos);
        }
        if (software != null) {
            image.withSoftware(software);
        }
        return image;
    }

    private ResourceStatus toStatus(org.dasein.cloud.digitalocean.models.Image instance) {
        if (instance == null) {
            return null;
        }

        MachineImageState state = MachineImageState.DELETED;
        String vmId = String.valueOf(instance.getId());
        if (instance.getRegions().length <= 0) {
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

            ImageFilterOptions options = ImageFilterOptions.getInstance();

            for (MachineImage image : searchPublicImages(options)) {
                if (image.getProviderMachineImageId().equals(providerImageId)) {
                    return image;
                }
            }
            for (MachineImage image : listImages(options)) {
                if (image.getProviderMachineImageId().equals(providerImageId)) {
                    return image;
                }
            }
            return null;
        } finally {
            APITrace.end();
        }
    }

    @Override
    public boolean isImageSharedWithPublic(@Nonnull String machineImageId) throws CloudException, InternalException {
        APITrace.begin(provider, "Image.isImageSharedWithPublic");
        try {
            MachineImage image = getMachineImage(machineImageId);

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
        provider.hold();
        PopulatorThread<ResourceStatus> populator = new PopulatorThread<ResourceStatus>(new JiteratorPopulator<ResourceStatus>() {
            @Override
            public void populate(@Nonnull Jiterator<ResourceStatus> iterator) throws Exception {
                APITrace.begin(provider, "Image.listImageStatus");
                try {
                    try {
                        TreeSet<String> ids = new TreeSet<String>();

                        for (ResourceStatus status : executeStatusList(1, cls)) {
                            ids.add(status.getProviderResourceId());
                            iterator.push(status);
                        }
                        for (ResourceStatus status : executeStatusList(2, cls)) {
                            if (!ids.contains(status.getProviderResourceId())) {
                                iterator.push(status);
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

    private @Nonnull Iterable<ResourceStatus> executeStatusList(int pass, @Nonnull ImageClass cls) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.executeStatusList");
        try {
            ProviderContext ctx = provider.getContext();

            if (ctx == null) {
                throw new CloudException("No context was set for this request");
            }

            
           /* Cache<ResourceStatus> cache = Cache.getInstance(provider, "images_resource", ResourceStatus.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Minute>(5, TimePeriod.MINUTE));
            Collection<ResourceStatus> resList = (Collection<ResourceStatus>)cache.get(ctx);

            if( resList != null ) {
                return resList;
            }*/

            ArrayList<ResourceStatus> list = new ArrayList<ResourceStatus>();

            try {

                Images images = (Images) DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.IMAGES);
                if (images != null) {
                    Set<org.dasein.cloud.digitalocean.models.Image> s = images.getImages();
                    Iterator<org.dasein.cloud.digitalocean.models.Image> itr = s.iterator();
                    while (itr.hasNext()) {
                        org.dasein.cloud.digitalocean.models.Image d = itr.next();
                        ResourceStatus status = toStatus(d);
                        if (status != null) {
                            list.add(status);
                        }
                    }
                }

            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new CloudException(e);
            }

            return list;
        } finally {
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
        provider.hold();
        PopulatorThread<MachineImage> populator = new PopulatorThread<MachineImage>(new JiteratorPopulator<MachineImage>() {
            @Override
            public void populate(@Nonnull Jiterator<MachineImage> iterator) throws Exception {
                APITrace.begin(getProvider(), "Image.listImages");
                try {
                    try {
                        TreeSet<String> ids = new TreeSet<String>();

                        for (MachineImage img : executeImageSearch(1, false, opts)) {
                            ids.add(img.getProviderMachineImageId());
                            iterator.push(img);
                        }
                        for (MachineImage img : executeImageSearch(2, false, opts)) {
                            if (!ids.contains(img.getProviderMachineImageId())) {
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
                        for (MachineImage img : executeImageSearch(1, true, options)) {
                            iterator.push(img);
                        }
                        for (MachineImage img : executeImageSearch(2, true, options)) {
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
