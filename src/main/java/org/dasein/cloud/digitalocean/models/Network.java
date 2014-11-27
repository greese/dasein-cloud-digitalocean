package org.dasein.cloud.digitalocean.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by mariapavlova on 21/10/2014.
 */
public class Network {
    @SerializedName("ip_address")
    String ipAddress;
    String netmask;
    String gateway;
    String type;

    public String getIpAddress() {
        return ipAddress;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public String getType() {
        return type;
    }
}
