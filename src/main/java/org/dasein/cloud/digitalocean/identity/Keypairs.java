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

import org.apache.log4j.Logger;
import org.dasein.cloud.*;
import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.digitalocean.models.Action;
import org.dasein.cloud.digitalocean.models.Key;
import org.dasein.cloud.digitalocean.models.Keys;
import org.dasein.cloud.digitalocean.models.actions.sshkey.Create;
import org.dasein.cloud.digitalocean.models.actions.sshkey.Destroy;
import org.dasein.cloud.digitalocean.models.rest.DigitalOceanModelFactory;
import org.dasein.cloud.identity.AbstractShellKeySupport;
import org.dasein.cloud.identity.SSHKeypair;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.identity.ShellKeyCapabilities;
import org.dasein.cloud.util.APITrace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Keypairs extends AbstractShellKeySupport<DigitalOcean> {
	static private final Logger logger = DigitalOcean.getLogger(Keypairs.class);

    private volatile transient KeyPairCapabilities capabilities;

	public Keypairs(@Nonnull DigitalOcean provider) {
		super(provider);
	}
	
	@Override
	public void deleteKeypair(@Nonnull String providerId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Keypair.deleteKeypair");
        try {
            DigitalOceanModelFactory.performAction(getProvider(), new Destroy(), providerId);
        }
        finally {
            APITrace.end();
        }
	}

	@Override
	public @Nullable String getFingerprint(@Nonnull String providerId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Keypair.getFingerprint");
        try {
            SSHKeypair kp = getKeypair(providerId);
            if( kp != null ) {
                return kp.getFingerprint();
            }
            return null;
        }
        finally {
            APITrace.end();
        }
	}

    @Override
    public @Nullable SSHKeypair getKeypair(@Nonnull String providerId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Keypair.getKeypair");
        try {
            Key key  = (Key) DigitalOceanModelFactory.getModelById(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.KEY, providerId);
            return toSSHKeypair(key);
        }
        catch( CloudException e ) {
            if( e.getHttpCode() == 404 ) {
                return null; // not found
            }
            logger.error(e.getMessage());
            throw new CloudException(e);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull ShellKeyCapabilities getCapabilities() throws CloudException, InternalException {
        if( capabilities == null ) {
            capabilities = new KeyPairCapabilities(getProvider());
        }
        return capabilities;
    }

    @Override
    public @Nonnull SSHKeypair importKeypair(@Nonnull String name, @Nonnull String publicKey) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Keypair.importKeypair");
        try {
            String regionId = getContext().getRegionId();
            if( regionId == null ) {
                throw new CloudException("No region was set for this request.");
            }

            Create action = new Create(name, publicKey);

            Key k = (Key)DigitalOceanModelFactory.performAction(getProvider(), action, org.dasein.cloud.digitalocean.models.rest.DigitalOcean.KEY);

            SSHKeypair kp = toSSHKeypair(k);
            if( kp != null ) {
                return kp;
            }
            else {
                throw new CloudException("Unable to import keypair "+name);
            }
        }
        finally {
            APITrace.end();
        }
    }

    private @Nullable SSHKeypair toSSHKeypair(@Nullable Key key) throws InternalException {
        if( key == null || key.getName() == null || key.getFingerprint() == null ) {
            return null;
        }
        SSHKeypair kp = new SSHKeypair();
        kp.setFingerprint(key.getFingerprint());
        kp.setName(key.getName());
        kp.setPrivateKey(null);
        kp.setPublicKey(key.getPublicKey());
        kp.setProviderKeypairId(key.getId());
        kp.setProviderOwnerId(getContext().getAccountNumber());
        kp.setProviderRegionId(getContext().getRegionId());
        return kp;
    }

    @Override
	public @Nonnull Iterable<SSHKeypair> list() throws InternalException, CloudException {
        APITrace.begin(getProvider(), "Keypair.list");
        List<SSHKeypair> keypairs = new ArrayList<SSHKeypair>();
        try {
            Keys keys = (Keys) DigitalOceanModelFactory.getModel(getProvider(), org.dasein.cloud.digitalocean.models.rest.DigitalOcean.KEYS);
            if (keys == null) {
                return null;
            }
            for( Key k : keys.getKeys() ) {
                SSHKeypair kp = toSSHKeypair(k);
                if( kp != null ) {
                    keypairs.add(kp);
                }
            }
        }
        finally {
            APITrace.end();
        }
        return keypairs;
	}
    
	@Override
	public @Nonnull SSHKeypair createKeypair(@Nonnull String keyName)
			throws InternalException, CloudException {
		throw new OperationNotSupportedException("Provider " + getProvider().getCloudName() + " does not support key generation, it only supports key import");
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {		
        try {
            list();
            return true;
        }
        catch( CloudException e ) {
            return false;
        }
    }

	@Override
	@Nonnull
	public String[] mapServiceAction(@Nonnull ServiceAction arg0) {
		return new String[] {};//throw new OperationNotSupportedException("Not sure what this method is for, not yet implemetned");		
	}
}
