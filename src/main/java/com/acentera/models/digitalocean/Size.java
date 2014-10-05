/**
 * Copyright (C) 2014 ACenterA, Inc.
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
package com.acentera.models.digitalocean;

import com.acentera.models.DigitalOceanRestModel;

public class Size implements DigitalOceanRestModel {
	String slug;
	Integer memory;
	Integer vcpus;
	Integer disk;
	Double transfer;
	Double price_monthly;
	Double price_hourly;
	String[] regions;
	
	public String getId() {
		return this.slug;
	}
	
	public String getSlug() {
		return this.slug;
	}		

	public void setSlong(String s) {
		this.slug = s;
	}		

	public Integer getMemory() {
		return this.memory;
	}
	public void setMemory(Integer mem) {
		this.memory = mem;
	}
	public Integer getCpus() {
		return this.vcpus;
	}
	public void setCpus(Integer cpu) {
		this.vcpus = cpu;
	} 
	public Integer getDisk() {
		return this.disk;
	}
	public void setDisk(Integer disk) {
		this.disk = disk;
	} 
	public Double getTransfer() {
		return this.transfer;
	}
	public void setTransfer(Double trans) {
		this.transfer = trans;
	} 
	public void setRegion(String[] reg) {
		this.regions = reg;		
	}
	public String[] getRegions() {
		return this.regions;
	}
	
	public Double getHourlyPrice() {
		return this.price_hourly;
	}
	public void setHourlyPrice(Double price) {
		this.price_hourly = price;
	} 
		
	public Double getMonthlyPrice() {
		return this.price_monthly;
	}
	public void setMonthlyPrice(Double price) {
		this.price_monthly = price;
	} 
	
	
}
