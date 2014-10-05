/**
 * Copyright (C) 2014 ACenterA, Inc. 
 * 
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
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.*;
import org.dasein.util.uom.time.Minute;
import org.dasein.util.uom.time.TimePeriod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.acentera.models.DigitalOceanModelFactory;
import com.acentera.models.digitalocean.Action;
import com.acentera.models.digitalocean.Droplet;
import com.acentera.models.digitalocean.Images;
import com.acentera.models.digitalocean.actions.droplet.Stop;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.OperationNotSupportedException;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DOImage extends AbstractImageSupport {
	static private final Logger logger = Logger.getLogger(DOImage.class);
	
	private DigitalOcean provider = null;
    private volatile transient ImageCapabilities capabilities;

    public DOImage(DigitalOcean provider) {
		super(provider);
        this.provider = provider;
	}

    @Override
    public void addImageShare(@Nonnull String providerImageId, @Nonnull String accountNumber) throws CloudException, InternalException {
        throw new CloudException("This provider does not support adding images");        
    }

    @Override
    public void addPublicShare(@Nonnull String providerImageId) throws CloudException, InternalException {
    	throw new CloudException("This provider does not support adding images");
    }

    @Override
    public ImageCapabilities getCapabilities() {
        if( capabilities == null ) {
            capabilities = new DOImageCapabilities(provider);
        }
        return capabilities;
    }

    @Override
    protected MachineImage capture(@Nonnull ImageCreateOptions options, @Nullable AsynchronousTask<MachineImage> task) throws CloudException, InternalException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new CloudException("No context was set for this request");
        }
        return captureImage(ctx, options, task);
    }
    
    private @Nonnull MachineImage captureImage(@Nonnull ProviderContext ctx, @Nonnull ImageCreateOptions options, @Nullable AsynchronousTask<MachineImage> task) throws CloudException, InternalException {
    	throw new CloudException("No captureImage enabled API on specified cloud provider");
    	/*occurred during imaging, but no machine image was specified");
        APITrace.begin(provider, "captureImage");
        
        try {
            if( task != null ) {
                task.setStartTime(System.currentTimeMillis());
            }
            VirtualMachine vm = null;

            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 30L);

            while( timeout > System.currentTimeMillis() ) {
                try {
                    //noinspection ConstantConditions
                    vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(options.getVirtualMachineId());
                    if( vm == null || VmState.TERMINATED.equals(vm.getCurrentState()) ) {
                        break;
                    }
                    
                    if( VmState.RUNNING.equals(vm.getCurrentState()) || VmState.STOPPED.equals(vm.getCurrentState()) ) {
                        break;
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
            }
            if( vm == null ) {
                throw new CloudException("No such virtual machine: " + options.getVirtualMachineId());
            }
            String lastMessage = null;
            int attempts = 5;

            //while( attempts > 0 ) {

            	
            	Snapshot action = new Snapshot();
            	           
                try {
                	Action evt = DigitalOceanModelFactory.performAction(getProvider(), action, instanceId);                	
                } catch (UnsupportedEncodingException e) {
                	 logger.error(e.getMessage());
                     throw new CloudException(e);
				}
                
                
            //}
            if( lastMessage == null ) {
                lastMessage = "Unknown error";
            }
            throw new CloudException(lastMessage);
        }
        finally {
            APITrace.end();
        }*/
    }

    private MachineImage captureWindows(@Nonnull ProviderContext ctx, @Nonnull ImageCreateOptions options, @Nonnull String bucket, @Nullable AsynchronousTask<MachineImage> task) throws CloudException, InternalException {
    	throw new CloudException("No captureImage enabled API on specified cloud provider");
    }

    
    @Override
	public @Nonnull Iterable<MachineImage> searchImages(String accountNumber, String keyword, Platform platform, Architecture architecture, ImageClass... imageClasses) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "Image.searchImages");
        try{
            ArrayList<MachineImage> results = new ArrayList<MachineImage>();
            Collection<MachineImage> images = new ArrayList<MachineImage>();
            if(accountNumber == null){
                images.addAll((Collection<MachineImage>)searchPublicImages(ImageFilterOptions.getInstance()));
            }
            images.addAll((Collection<MachineImage>)listImages(ImageFilterOptions.getInstance()));

            for( MachineImage image : images ) {
                if(image != null){
                    if( keyword != null ) {
                        if( !image.getProviderMachineImageId().contains(keyword) && !image.getName().contains(keyword) && !image.getDescription().contains(keyword) ) {
                            continue;
                        }
                    }
                    if( platform != null ) {
                        Platform p = image.getPlatform();

                        if( !platform.equals(p) ) {
                            if( platform.isWindows() ) {
                                if( !p.isWindows() ) {
                                    continue;
                                }
                            }
                            else if( platform.equals(Platform.UNIX) ){
                                if( !p.isUnix() ) {
                                    continue;
                                }
                            }
                            else {
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
        }
        finally {
            APITrace.end();
        }
    }    
    
    private @Nonnull Iterable<MachineImage> executeImageSearch(int pass, boolean forPublic, @Nonnull ImageFilterOptions options) throws CloudException, InternalException {
        APITrace.begin(provider, "Image.executeImageSearch");
        try {
            final ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new CloudException("No context was set for this request");
            }
            final String regionId = ctx.getRegionId();
            if( regionId == null ) {
                throw new CloudException("No region was set for this request");
            }

            Architecture architecture = options.getArchitecture();

            if( architecture != null && !architecture.equals(Architecture.I32) && !architecture.equals(Architecture.I64) ) {
                if( !options.isMatchesAny() ) {
                    return Collections.emptyList();
                }
            }
            
            Cache<MachineImage> cache = Cache.getInstance(provider, "images", MachineImage.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Minute>(5, TimePeriod.MINUTE));
            Collection<MachineImage> imgList = (Collection<MachineImage>)cache.get(ctx);

            if( imgList == null ) {                           
            
		            final ArrayList<MachineImage> list = new ArrayList<MachineImage>();
		
		
		        	try {
		            	
		            	Images images = (Images)DigitalOceanModelFactory.getModel(getProvider(), com.acentera.models.DigitalOcean.IMAGES );
		            	if (images != null) {
			            	Set<com.acentera.models.digitalocean.Image> s = images.getImages();
			            	Iterator<com.acentera.models.digitalocean.Image> itr = s.iterator();
			            	while(itr.hasNext()) {
			            		com.acentera.models.digitalocean.Image d = itr.next();
			            		MachineImage[] status = toImage(d);
			            		if( status != null ) {
			            			int len = status.length;
			            			for(int i = 0; i<len; i++ ) {
			            				list.add(status[i]);
			            			}
			                    }
			            	}
		            	}
		            	
		            } catch( Exception e ) {
		                logger.error(e.getMessage());
		                throw new CloudException(e);
		            }
		            
		            
		        	cache.put(ctx, list);
		        	imgList = list;	
            }
            
            Map<String,String> parameters = new HashMap<String,String>();            
            final ImageFilterOptions filterOptions = fillImageFilterParameters(forPublic, options, parameters);

            final ArrayList<MachineImage> res = new ArrayList<MachineImage>();

            Iterator<MachineImage> itrMachine = imgList.iterator();
            while(itrMachine.hasNext()) {
            	MachineImage image  = itrMachine.next();
            	 if( image != null && ( filterOptions != null && filterOptions.matches(image) )) {
                     res.add(image);
                 }
            }
            
            return res;
           
            
            
        }
        finally {
            APITrace.end();
        }
    }

    private MachineImage[] toImage(com.acentera.models.digitalocean.Image instance) {
        if( instance == null ) {
            return null;
        }
                     
        ProviderContext ctx = getProvider().getContext();
                
        MachineImageState mis = MachineImageState.ACTIVE;
        if ( instance.getRegions().length<=0) {
        	mis = MachineImageState.DELETED;
        	return null;
        }
        
        int nbImages = instance.getRegions().length;
        MachineImage[] res = new MachineImage[nbImages];
        for (int i = 0; i < nbImages; i++) {
	        
	        Architecture arch = Architecture.I64;
	        if (instance.getName().contains("x32")) {
	        	arch = Architecture.I32;
	        }
	        
	        System.out.println("DISTRI: "+ instance.getDistribution());
	        Platform platform = Platform.UNKNOWN;
	        if (instance.getDistribution().compareToIgnoreCase("Ubuntu") == 0) {
	        	platform = Platform.UBUNTU;
	        } else if (instance.getDistribution().compareToIgnoreCase("CentOS") == 0) {
	        	platform = Platform.CENT_OS;
	        } else if (instance.getDistribution().compareToIgnoreCase("Fedora") == 0) {
	        	platform = Platform.FEDORA_CORE;
	        }
	        
	        System.out.println(" PLATFORM IS : " + platform);
	        MachineImage image = MachineImage.getImageInstance(
	        		ctx.getAccountNumber()
	        		, ctx.getRegionId()
	        		, instance.getId()
	        		, ImageClass.MACHINE
	        		, mis
	        		, instance.getName()
	        		, instance.getDistribution()
	        		, arch
	        		, platform
	        		, MachineImageFormat.RAW
			);
	        String software = null;
	        int pos = instance.getName().indexOf(" on ");
	        if ( pos >= 1) {
	        	software = instance.getName().substring(0, pos);
	        }
	        image.setSoftware(software);
	        image.setProviderRegionId(instance.getRegions()[i]);
	        res[i] = image;
        }
        return res;
	}
    
    private ResourceStatus toStatus(com.acentera.models.digitalocean.Image instance) {
        if( instance == null ) {
            return null;
        }
     
        
        MachineImageState state = MachineImageState.DELETED;
        String vmId = String.valueOf(instance.getId());        
        if ( instance.getRegions().length<=0) {
        	state = MachineImageState.ACTIVE;
        }
        return new ResourceStatus(vmId, state);
	}

	@Override
    public @Nullable MachineImage getImage(@Nonnull String providerImageId) throws CloudException, InternalException {
        APITrace.begin(provider, "Image.getImage");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new CloudException("No context was set for this request");
            }
            
            ImageFilterOptions options = ImageFilterOptions.getInstance();

            for( MachineImage image : searchPublicImages(options) ) {
                if( image.getProviderMachineImageId().equals(providerImageId) ) {
                    return image;
                }
            }
            for( MachineImage image : listImages(options) ) {
                if( image.getProviderMachineImageId().equals(providerImageId) ) {
                    return image;
                }
            }
            return null;
        }
        finally {
            APITrace.end();
        }
    }
	
    @Override
    public @Nonnull String getProviderTermForImage(@Nonnull Locale locale) {
          return getProviderTermForImage(locale, ImageClass.MACHINE);
      }

    @Override
    @Deprecated
    public @Nonnull String getProviderTermForImage(@Nonnull Locale locale, @Nonnull ImageClass cls) {
        return getCapabilities().getProviderTermForImage(locale, cls);
    }

    @Override
    @Deprecated
    public @Nonnull String getProviderTermForCustomImage(@Nonnull Locale locale, @Nonnull ImageClass cls) {
        return getCapabilities().getProviderTermForCustomImage(locale, cls);
    }

    @Override
    public boolean hasPublicLibrary() {
        return true;
    }

    @Override
    public @Nonnull Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
        return getCapabilities().identifyLocalBundlingRequirement();
    }

    @Override
    public boolean isImageSharedWithPublic(@Nonnull String machineImageId) throws CloudException, InternalException {
        APITrace.begin(provider, "Image.isImageSharedWithPublic");
        try {
            MachineImage image = getMachineImage(machineImageId);

            if( image == null ) {
                return false;
            }
            String p = (String)image.getTag("public");

            return (p != null && p.equalsIgnoreCase("true"));
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        throw new CloudException("Not supported operation in this cloud?");
    }

    public @Nonnull Iterable<ResourceStatus> listImageStatus(final @Nonnull ImageClass cls) throws CloudException, InternalException {
            provider.hold();
            PopulatorThread<ResourceStatus> populator = new PopulatorThread<ResourceStatus>(new JiteratorPopulator<ResourceStatus>() {
                @Override
                public void populate(@Nonnull Jiterator<ResourceStatus> iterator) throws Exception {
                    APITrace.begin(provider, "Image.listImageStatus");
                    try {
                        try {
                            TreeSet<String> ids = new TreeSet<String>();

                            for( ResourceStatus status : executeStatusList(1, cls) ) {
                                ids.add(status.getProviderResourceId());
                                iterator.push(status);
                            }
                            for( ResourceStatus status : executeStatusList(2, cls) ) {
                                if( !ids.contains(status.getProviderResourceId()) ) {
                                    iterator.push(status);
                                }
                            }
                        }
                        finally {
                            provider.release();
                        }
                    }
                    finally {
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

            if( ctx == null ) {
                throw new CloudException("No context was set for this request");
            }

            
           /* Cache<ResourceStatus> cache = Cache.getInstance(provider, "images_resource", ResourceStatus.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Minute>(5, TimePeriod.MINUTE));
            Collection<ResourceStatus> resList = (Collection<ResourceStatus>)cache.get(ctx);

            if( resList != null ) {
                return resList;
            }*/
            
            ArrayList<ResourceStatus> list = new ArrayList<ResourceStatus>();

            try {
            	
            	Images images = (Images)DigitalOceanModelFactory.getModel(getProvider(), com.acentera.models.DigitalOcean.IMAGES );
            	if (images != null) {
	            	Set<com.acentera.models.digitalocean.Image> s = images.getImages();
	            	Iterator<com.acentera.models.digitalocean.Image> itr = s.iterator();
	            	while(itr.hasNext()) {
	            		com.acentera.models.digitalocean.Image d = itr.next();
	            		ResourceStatus status = toStatus(d);
	            		if( status != null ) {
	                        list.add(status);
	                    }
	            	}
            	}
            	
            } catch( Exception e ) {
                logger.error(e.getMessage());
                throw new CloudException(e);
            }
                        
            return list;
        }
        finally {
            APITrace.end();
        }
    }

    private @Nonnull ImageFilterOptions fillImageFilterParameters(boolean forPublic, @Nonnull ImageFilterOptions options, @Nonnull Map<String,String> parameters) throws CloudException, InternalException {
        int filter = 1;

        if( forPublic ) {
            parameters.put("Filter." + filter + ".Name", "state");
            parameters.put("Filter." + (filter++) + ".Value.1", "available");
        }

        if( options.isMatchesAny() && options.getCriteriaCount() > 1 ) {
            if( forPublic ) {
                return options;
            }
            else {
                options.withAccountNumber(getContext().getAccountNumber());
                return options;
            }
        }

        String owner = options.getAccountNumber();

        if( owner != null ) {
            parameters.put("Owner", owner);
        }

        Architecture architecture = options.getArchitecture();

        if( architecture != null && (architecture.equals(Architecture.I32) || architecture.equals(Architecture.I64)) ) {
            parameters.put("Filter." + filter + ".Name", "architecture");
            parameters.put("Filter." + (filter++) + ".Value.1", Architecture.I32.equals(options.getArchitecture()) ? "i386" : "x86_64");
        }

        Platform platform = options.getPlatform();

        if( platform != null && platform.equals(Platform.WINDOWS) ) {
            parameters.put("Filter." + filter + ".Name", "platform");
            parameters.put("Filter." + (filter++) + ".Value.1", "windows");
        }

        ImageClass cls= options.getImageClass();
        String t = "machine";

        if( cls != null ) {
            switch( cls ) {
                case MACHINE: t = "machine"; break;
                case KERNEL: t = "kernel"; break;
                case RAMDISK: t = "ramdisk"; break;
            }
            parameters.put("Filter." + filter + ".Name", "image-type");
            parameters.put("Filter." + (filter++) + ".Value.1", t);
        }

        Map<String, String> extraParameters = new HashMap<String, String>();

        parameters.putAll(extraParameters);
        String regex = options.getRegex();

        options = ImageFilterOptions.getInstance();

        if( regex != null ) {
            options.matchingRegex(regex);
        }
        if( platform != null ) {
            options.onPlatform(platform);
        }
        return options;
    }

    @Override
    public @Nonnull Iterable<String> listShares(@Nonnull String forMachineImageId) throws CloudException, InternalException {
        return sharesAsList(forMachineImageId);
    }

    @Override
    public @Nonnull Iterable<ImageClass> listSupportedImageClasses() throws CloudException, InternalException {
        return getCapabilities().listSupportedImageClasses();
    }


    @Override
    public @Nonnull Iterable<MachineImageType> listSupportedImageTypes() throws CloudException, InternalException {
        return getCapabilities().listSupportedImageTypes();
    }

    @Override
    public @Nonnull Iterable<MachineImageFormat> listSupportedFormats() throws CloudException, InternalException {
        return getCapabilities().listSupportedFormats();
    }

    @Override
    public @Nonnull Iterable<MachineImageFormat> listSupportedFormatsForBundling() throws CloudException, InternalException {
        return getCapabilities().listSupportedFormatsForBundling();
    }

    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {        
        return new String[0];
    }

    @Override
    public @Nonnull MachineImage registerImageBundle(@Nonnull ImageCreateOptions options) throws CloudException, InternalException {
    	throw new CloudException("Operation Not supported");
    }

    @Override
    public void remove( @Nonnull String providerImageId, boolean checkState ) throws CloudException, InternalException {
    	throw new CloudException("Operation Not supported");
    }

    @Override
    public void removeAllImageShares(@Nonnull String providerImageId) throws CloudException, InternalException {
    	throw new CloudException("Operation Not supported");
    }

    @Override
    public void removeImageShare(@Nonnull String providerImageId, @Nonnull String accountNumber) throws CloudException, InternalException {
    	throw new CloudException("Operation Not supported");
    }

    @Override
    public void removePublicShare(@Nonnull String providerImageId) throws CloudException, InternalException {
    	throw new CloudException("Operation Not supported");
    }

    @Override
    public @Nonnull Iterable<MachineImage> listImages(@Nullable ImageFilterOptions options) throws CloudException, InternalException {
        final ImageFilterOptions opts;

        if( options == null ) {
            opts = ImageFilterOptions.getInstance();
        }
        else {
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

                        for( MachineImage img : executeImageSearch(1, false, opts) ) {
                            ids.add(img.getProviderMachineImageId());
                            iterator.push(img);
                        }
                        for( MachineImage img : executeImageSearch(2, false, opts) ) {
                            if( !ids.contains(img.getProviderMachineImageId()) ) {
                                iterator.push(img);
                            }
                        }
                    }
                    finally {
                        provider.release();
                    }
                }
                finally {
                    APITrace.end();
                }
            }
        });

        populator.populate();
        return populator.getResult();
    }

    @Override
    public @Nonnull Iterable<MachineImage> searchPublicImages(final @Nonnull ImageFilterOptions options) throws CloudException, InternalException {
        provider.hold();
        PopulatorThread<MachineImage> populator = new PopulatorThread<MachineImage>(new JiteratorPopulator<MachineImage>() {
            @Override
            public void populate(@Nonnull Jiterator<MachineImage> iterator) throws Exception {
                APITrace.begin(getProvider(), "searchPublicImages");
                try {
                    try {
                        for( MachineImage img : executeImageSearch(1, true, options) ) {
                            iterator.push(img);
                        }
                        for( MachineImage img : executeImageSearch(2, true, options) ) {
                            iterator.push(img);
                        }
                    }
                    finally {
                        provider.release();
                    }
                }
                finally {
                    APITrace.end();
                }
            }
        });

        populator.populate();
        return populator.getResult();
    }

    private void setPrivateShare(@Nonnull String imageId, boolean allowed, @Nonnull String ... accountIds) throws CloudException, InternalException {
    	throw new CloudException("Operation Not supported");
    }

    private void setPublicShare(@Nonnull String imageId, boolean allowed) throws CloudException, InternalException {
    	throw new CloudException("Operation Not supported");
    }

    private @Nonnull List<String> sharesAsList(@Nonnull String forMachineImageId) throws CloudException, InternalException {
    	throw new CloudException("Operation Not supported");
    }

    @Override
    public boolean supportsCustomImages() {
        return true;
    }

    @Override
    public boolean supportsImageCapture(@Nonnull MachineImageType type) throws CloudException, InternalException {
        return getCapabilities().supportsImageCapture(type);
    }

    @Override
    public boolean supportsImageSharing() throws CloudException, InternalException{
        return getCapabilities().supportsImageSharing();
    }

    @Override
    public boolean supportsImageSharingWithPublic() throws CloudException, InternalException{
        return getCapabilities().supportsImageSharingWithPublic();
    }

    @Override
    public boolean supportsPublicLibrary(@Nonnull ImageClass cls) throws CloudException, InternalException {
        return getCapabilities().supportsPublicLibrary(cls);
    }

   
    

    @Override
    public void updateTags(@Nonnull String imageId, @Nonnull Tag... tags) throws CloudException, InternalException {
        updateTags(new String[]{imageId}, tags);
    }

    @Override
    public void updateTags(@Nonnull String[] imageIds, @Nonnull Tag... tags) throws CloudException, InternalException {
		throw new CloudException("Operation not supported");        
    }

    @Override
    public void removeTags(@Nonnull String imageId, @Nonnull Tag... tags) throws CloudException, InternalException {
        removeTags(new String[]{imageId}, tags);
    }

    @Override
    public void removeTags(@Nonnull String[] imageIds, @Nonnull Tag... tags) throws CloudException, InternalException {
    	throw new CloudException("Operation not supported");
    }

}
