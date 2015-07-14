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

import org.dasein.cloud.*;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.util.NamingConstraints;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

/**
 * Describes the capabilities of DigitalOcean with respect to Dasein virtual machine operations.
 * <p>Created by Francis Lavalliere</p>
 * @author Francis Lavalliere
 * @version 2014.05 initial version
 * @since 2014.05
 */
public class DOInstanceCapabilities extends AbstractCapabilities<DigitalOcean> implements VirtualMachineCapabilities {

    public DOInstanceCapabilities(@Nonnull DigitalOcean cloud) { super(cloud); }

    @Override
    public boolean canAlter(@Nonnull VmState fromState) throws CloudException, InternalException {
    	 return fromState.equals(VmState.STOPPED);
    }

    @Override
    public boolean canClone(@Nonnull VmState fromState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canPause(@Nonnull VmState fromState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canReboot(@Nonnull VmState fromState) throws CloudException, InternalException {
        return (fromState.equals(VmState.RUNNING));
    }

    @Override
    public boolean canResume(@Nonnull VmState fromState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canStart(@Nonnull VmState fromState) throws CloudException, InternalException {
        return fromState.equals(VmState.STOPPED);
    }

    @Override
    public boolean canStop(@Nonnull VmState fromState) throws CloudException, InternalException {
        return fromState.equals(VmState.RUNNING);
    }

    @Override
    public boolean canSuspend(@Nonnull VmState fromState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canTerminate(@Nonnull VmState fromState) throws CloudException, InternalException {
        return !VmState.PENDING.equals(fromState) && !VmState.TERMINATED.equals(fromState);
    }

    @Override
    public boolean canUnpause(@Nonnull VmState fromState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public int getMaximumVirtualMachineCount() throws CloudException, InternalException {
        return Capabilities.LIMIT_UNKNOWN;
    }

    @Override
    public int getCostFactor(@Nonnull VmState vmState) throws CloudException, InternalException {
        return 100;
    }

    @Override
    public @Nonnull String getProviderTermForVirtualMachine(@Nonnull Locale locale) throws CloudException, InternalException {
        return "droplet";
    }

    @Override
    public @Nonnull NamingConstraints getVirtualMachineNamingConstraints() {
        return NamingConstraints.getAlphaNumeric(1, 100).withNoSpaces().constrainedBy('.', '-');
    }

    @Override
    public @Nullable VisibleScope getVirtualMachineVisibleScope() {
        return null;
    }

    @Override
    public @Nullable VisibleScope getVirtualMachineProductVisibleScope() {
        return null;
    }

    @Override
    public @Nullable VMScalingCapabilities getVerticalScalingCapabilities() throws CloudException, InternalException {
    	 return VMScalingCapabilities.getInstance(false, true, false);
    }

    @Nonnull
    @Override
    public Requirement identifyDataCenterLaunchRequirement() throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Override
    public @Nonnull Requirement identifyImageRequirement(@Nonnull ImageClass cls) throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Override
    public @Nonnull Requirement identifyShellKeyRequirement(Platform platform) throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Override
    public boolean isUserDataSupported() throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean isUserDefinedPrivateIPSupported() throws CloudException, InternalException {
        return false;
    }

    static private volatile Collection<Architecture> architectures;

    @Override
    public @Nonnull Iterable<Architecture> listSupportedArchitectures() throws InternalException, CloudException {
        if (architectures == null) {
            architectures = Arrays.asList(Architecture.I32, Architecture.I64);
        }
        return architectures;
    }

    @Override
    public boolean supportsSpotVirtualMachines() throws InternalException, CloudException {
        return false;
    }

    @Override
    public boolean supportsAlterVM() {
        return true;
    }

    @Override
    public boolean supportsClone() {
        return false;
    }

    @Override
    public boolean supportsPause() {
        return false;
    }

    @Override
    public boolean supportsReboot() {
        return true;
    }

    @Override
    public boolean supportsResume() {
        return false;
    }

    @Override
    public boolean supportsStart() {
        return true;
    }

    @Override
    public boolean supportsStop() {
        return true;
    }

    @Override
    public boolean supportsSuspend() {
        return false;
    }

    @Override
    public boolean supportsTerminate() {
        return false;
    }

    @Override
    public boolean supportsUnPause() {
        return false;
    }

    @Override
	@Nonnull
	public Requirement identifyPasswordRequirement(Platform arg0)
			throws CloudException, InternalException {
		return Requirement.NONE;
	}

	@Override
	@Nonnull
	public Requirement identifyRootVolumeRequirement() throws CloudException,
			InternalException {
		return Requirement.NONE;
	}

	@Override
	@Nonnull
	public Requirement identifyStaticIPRequirement() throws CloudException,
			InternalException {
		return Requirement.NONE;
	}

	@Override
	@Nonnull
	public Requirement identifySubnetRequirement() throws CloudException,
			InternalException {
		return Requirement.NONE;
	}

	@Override
	@Nonnull
	public Requirement identifyVlanRequirement() throws CloudException,
			InternalException {
		return Requirement.NONE;
	}

	@Override
	public boolean isAPITerminationPreventable() throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public boolean isBasicAnalyticsSupported() throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public boolean isExtendedAnalyticsSupported() throws CloudException,
			InternalException {
		return false;
	}

    @Override
    public boolean isVMProductDCConstrained() throws CloudException, InternalException{
        return false;
    }

    @Override
    public boolean supportsCloudStoredShellKey() throws CloudException, InternalException{
        return true;
    }

    @Override
    public boolean supportsClientRequestToken() throws CloudException, InternalException{
        return false;
    }
}
