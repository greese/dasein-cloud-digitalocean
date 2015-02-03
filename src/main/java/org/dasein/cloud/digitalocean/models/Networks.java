package org.dasein.cloud.digitalocean.models;

import java.util.Set;

/**
 * Created by mariapavlova on 21/10/2014.
 */
public class Networks {
    Set<Network> v4;
    Set<Network> v6;

    public Set<Network> getV4() {
        return v4;
    }

    public Set<Network> getV6() {
        return v6;
    }
}
