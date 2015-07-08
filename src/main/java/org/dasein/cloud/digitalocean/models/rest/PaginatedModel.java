package org.dasein.cloud.digitalocean.models.rest;

/**
 * Created by stas on 08/07/15.
 */
public class PaginatedModel implements DigitalOceanRestModel {

    private int total;

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotal() {
        return total;
    }

}
