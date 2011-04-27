/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wordpress.salaboy.model.messages;

import java.io.Serializable;

/**
 *
 * @author esteban
 */
public class VehicleHitsCornerMessage implements Serializable {
    private Long vehicleId;
    private int cornerX;
    private int cornery;

    public VehicleHitsCornerMessage(Long vehicleId, int cornerX, int cornery) {
        this.vehicleId = vehicleId;
        this.cornerX = cornerX;
        this.cornery = cornery;
    }
    
    
    public int getCornerX() {
        return cornerX;
    }

    public void setCornerX(int cornerX) {
        this.cornerX = cornerX;
    }

    public int getCornery() {
        return cornery;
    }

    public void setCornery(int cornery) {
        this.cornery = cornery;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }
     
}