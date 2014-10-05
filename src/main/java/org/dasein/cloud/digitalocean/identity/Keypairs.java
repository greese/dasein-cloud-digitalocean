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

package org.dasein.cloud.digitalocean.identity;

import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.identity.SSHKeypair;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.identity.ShellKeySupport;
import org.dasein.cloud.util.APITrace;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.acentera.models.DigitalOceanModelFactory;
import com.acentera.models.digitalocean.Action;
import com.acentera.models.digitalocean.Droplet;
import com.acentera.models.digitalocean.Key;
import com.acentera.models.digitalocean.Keys;
import com.acentera.models.digitalocean.actions.droplet.Resize;
import com.acentera.models.digitalocean.actions.key.Create;
import com.acentera.models.digitalocean.actions.key.Destroy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Keypairs implements ShellKeySupport {
	static private final Logger logger = DigitalOcean.getLogger(Keypairs.class);
	
	private DigitalOcean provider = null;
	
	public Keypairs(@Nonnull DigitalOcean provider) {
		this.provider =  provider;
	}
	
	@Override
	public void deleteKeypair(@Nonnull String name) throws InternalException, CloudException {
        APITrace.begin(provider, "Keypair.deleteKeypair");
        try {
        	
        	try {
        		
    			Keys kSet  = (Keys) DigitalOceanModelFactory.getModel(provider, com.acentera.models.DigitalOcean.KEYS);
            	
            	if (kSet == null) {
            		throw new CloudException("Key item waas not found on " + provider.getCloudName());
            	}
            	
            	Iterator<Key> itr = kSet.getKey().iterator();
            	Key foundKey = null;
            	while(itr.hasNext() && foundKey == null) {
            		Key k = itr.next();            	
	            	if( k != null && k.getName() != null & k.getName().equals(name) && k.getFingerprint() != null ) {
	            		foundKey = k;                    	
	                }
            	}
            	
            	if (foundKey == null) {
            		throw new CloudException("Key item waas not found on " + provider.getCloudName());
            	}
            	
            	Destroy action = new Destroy();            
            	
            	Action evt = (Action)DigitalOceanModelFactory.performAction(provider, action, foundKey.getId());
            	
            	return;
            } catch( CloudException e ) {
                logger.error(e.getMessage());
                throw new CloudException(e);
            } catch (UnsupportedEncodingException e) {
            	logger.error(e.getMessage());
                throw new CloudException(e);
			}
        	
        }
        finally {
            APITrace.end();
        }
	}

	@Override
	public @Nullable String getFingerprint(@Nonnull String name) throws InternalException, CloudException {
        APITrace.begin(provider, "Keypair.getFingerprint");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new InternalException("No context was established for this call.");
            }
            try {
 	           
            	Keys kSet  = (Keys) DigitalOceanModelFactory.getModel(provider, com.acentera.models.DigitalOcean.KEYS);
            	
            	if (kSet == null) {
            		return null;            		
            	}
            	Iterator<Key> itr = kSet.getKey().iterator();
            	while(itr.hasNext()) {
            		Key k = itr.next();            	
	            	if( k != null && k.getName() != null & k.getName().equals(name) && k.getFingerprint() != null ) {
                    	return k.getFingerprint();
	                }
            	}
            	
            	//Not found
            	return null;
            } catch( CloudException e ) {
                logger.error(e.getMessage());
                throw new CloudException(e);
            } catch (UnsupportedEncodingException e) {
            	logger.error(e.getMessage());
                throw new CloudException(e);
			}          
        }
        finally {
            APITrace.end();
        }
	}

    @Override
    public Requirement getKeyImportSupport() throws CloudException, InternalException {
        return Requirement.OPTIONAL;
    }

    @Override
    public @Nullable SSHKeypair getKeypair(@Nonnull String name) throws InternalException, CloudException {
        APITrace.begin(provider, "Keypair.getKeypair");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new CloudException("No context was established for this call.");
            }
            
            
            try {
            	           
                
            	
            
            	Keys kSet  = (Keys) DigitalOceanModelFactory.getModel(provider, com.acentera.models.DigitalOcean.KEYS);
            	
            	if (kSet == null) {
            		return null;            		
            	}
            	Iterator<Key> itr = kSet.getKey().iterator();
            	while(itr.hasNext()) {
            		Key k = itr.next();            	
	            	if( k != null && k.getName() != null & k.getName().equals(name) && k.getFingerprint() != null ) {
	                    SSHKeypair kp = new SSHKeypair();
	
	                    kp.setFingerprint(k.getFingerprint());
	                    kp.setName(k.getName());
	                    kp.setPrivateKey(null);
	                    kp.setPublicKey(k.getPublicKey());
	                    kp.setProviderKeypairId(k.getId());
	                    kp.setProviderOwnerId(ctx.getAccountNumber());
	                    //all regions
	                    //kp.setProviderRegionId(regionId);
	                    return kp;
	                }
            	}
            	
            	//Not found
            	return null;
            } catch( CloudException e ) {
                logger.error(e.getMessage());
                throw new CloudException(e);
            } catch (UnsupportedEncodingException e) {
            	logger.error(e.getMessage());
                throw new CloudException(e);
			}          
                          
        }
        finally {
            APITrace.end();
        }
    }

	@Override
	public @Nonnull String getProviderTermForKeypair(@Nonnull Locale locale) {
		return "keypair";
	}

    @Override
    public @Nonnull SSHKeypair importKeypair(@Nonnull String name, @Nonnull String material) throws InternalException, CloudException {
        APITrace.begin(provider, "Keypair.importKeypair");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new CloudException("No context was established for this call.");
            }
            String regionId = ctx.getRegionId();

            if( regionId == null ) {
                throw new CloudException("No region was set for this request.");
            }

            try {
            	Create action = new Create(name, material);            
            	
            	Key k = (Key)DigitalOceanModelFactory.performAction(provider, action, com.acentera.models.DigitalOcean.KEY);
            	
                SSHKeypair key = new SSHKeypair();

                //In digitalOcean the imported data is the public key value....
                key.setPublicKey(material);
                key.setFingerprint(k.getFingerprint());
                key.setName(name);
                key.setProviderKeypairId(name);
                key.setProviderOwnerId(ctx.getAccountNumber());
                //Its available for all regions
                ///key.setProviderRegionId(regionId);
                return key;
                
            } catch( CloudException e ) {
                logger.error(e.getMessage());
                throw new CloudException(e);
            } catch (UnsupportedEncodingException e) {
            	logger.error(e.getMessage());
                throw new CloudException(e);
			}
            
            

        }
        finally {
            APITrace.end();
        }
    }
    
    @Override
	public @Nonnull Collection<SSHKeypair> list() throws InternalException, CloudException {
        APITrace.begin(provider, "Keypair.list");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new CloudException("No context was established for this call.");
            }
            ArrayList<SSHKeypair> keypairs = new ArrayList<SSHKeypair>();
            
            
            Keys kSet;
			try {
				kSet = (Keys) DigitalOceanModelFactory.getModel(provider, com.acentera.models.DigitalOcean.KEYS);
				
	        	if (kSet == null) {
	        		return null;            		
	        	}
	        	Iterator<Key> itr = kSet.getKey().iterator();
	        	while(itr.hasNext()) {
	        		Key k = itr.next();            	
	            	if( k != null && k.getName() != null && k.getFingerprint() != null ) {
	                    SSHKeypair kp = new SSHKeypair();
	
	                    kp.setFingerprint(k.getFingerprint());
	                    kp.setName(k.getName());
	                    kp.setPrivateKey(null);
	                    kp.setPublicKey(k.getPublicKey());
	                    kp.setProviderKeypairId(k.getId());
	                    kp.setProviderOwnerId(ctx.getAccountNumber());
	                    //all regions
	                    //kp.setProviderRegionId(regionId);
	                    keypairs.add(kp);
	                }
	        	}
			} catch (UnsupportedEncodingException e) {
			}
        	
        	//Not found
        	return keypairs;
        }
        finally {
            APITrace.end();
        }
	}
    
	@Override
	@Nonnull
	public SSHKeypair createKeypair(@Nonnull String arg0)
			throws InternalException, CloudException {
		throw new OperationNotSupportedException("Provider does not generate keys, you must send them a public key and they will ensure to write that public key into the authorization_keys files");		
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {		
		throw new OperationNotSupportedException("Not sure what this method is for, not yet implemetned");
	}

	@Override
	@Nonnull
	public String[] mapServiceAction(@Nonnull ServiceAction arg0) {
		return new String[] {};//throw new OperationNotSupportedException("Not sure what this method is for, not yet implemetned");		
	}
}
