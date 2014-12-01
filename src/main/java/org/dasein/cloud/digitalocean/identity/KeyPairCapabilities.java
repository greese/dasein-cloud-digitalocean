package org.dasein.cloud.digitalocean.identity;

import org.bouncycastle.ocsp.Req;
import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.identity.ShellKeyCapabilities;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Created by mariapavlova on 27/11/2014.
 */
public class KeyPairCapabilities extends AbstractCapabilities<DigitalOcean> implements ShellKeyCapabilities {
    public KeyPairCapabilities(DigitalOcean provider) {
        super(provider);
    }

    @Nonnull
    @Override
    public Requirement identifyKeyImportRequirement() throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Nonnull
    @Override
    public String getProviderTermForKeypair(@Nonnull Locale locale) {
        return "keypair";
    }
}
