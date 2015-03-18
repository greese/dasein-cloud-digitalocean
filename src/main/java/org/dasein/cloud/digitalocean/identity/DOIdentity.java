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
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.digitalocean.DigitalOcean;
import org.dasein.cloud.identity.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DOIdentity implements IdentityAndAccessSupport {
    static private final Logger logger = DigitalOcean.getLogger(DOIdentity.class);
    
    private DigitalOcean provider;

    public DOIdentity(@Nonnull DigitalOcean cloud) {
        provider = cloud;
    }
    

    @Override
    public boolean supportsAccessControls() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsConsoleAccess() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsAPIAccess() throws CloudException, InternalException {
        return true;
    }
    

	@Override
	@Nonnull
	public String[] mapServiceAction(@Nonnull ServiceAction arg0) {
		return new String[] {};	
	}


	@Override
	public void addUserToGroups(@Nonnull String arg0, @Nonnull String... arg1)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");		
	}


	@Override
	@Nonnull
	public CloudGroup createGroup(@Nonnull String arg0, @Nullable String arg1,
			boolean arg2) throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
		
	}


	@Override
	@Nonnull
	public CloudUser createUser(@Nonnull String arg0, @Nullable String arg1,
			@Nullable String... arg2) throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	@Nonnull
	public AccessKey enableAPIAccess(@Nonnull String arg0)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public void enableConsoleAccess(@Nonnull String arg0, @Nonnull byte[] arg1)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	@Nullable
	public CloudGroup getGroup(@Nonnull String arg0) throws CloudException,
			InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	@Nullable
	public CloudUser getUser(@Nonnull String arg0) throws CloudException,
			InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	@Nonnull
	public Iterable<CloudGroup> listGroups(@Nullable String arg0)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	@Nonnull
	public Iterable<CloudGroup> listGroupsForUser(@Nonnull String arg0)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	@Nonnull
	public Iterable<CloudPolicy> listPoliciesForGroup(@Nonnull String arg0)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	@Nonnull
	public Iterable<CloudPolicy> listPoliciesForUser(@Nonnull String arg0)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	@Nonnull
	public Iterable<CloudUser> listUsersInGroup(@Nonnull String arg0)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	@Nonnull
	public Iterable<CloudUser> listUsersInPath(@Nullable String arg0)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public void removeAccessKey(@Nonnull String arg0, @Nonnull String arg1)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public void removeConsoleAccess(@Nonnull String arg0)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public void removeGroup(@Nonnull String arg0) throws CloudException,
			InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public void removeGroupPolicy(@Nonnull String arg0, @Nonnull String arg1)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public void removeUser(@Nonnull String arg0) throws CloudException,
			InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public void removeUserFromGroup(@Nonnull String arg0, @Nonnull String arg1)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public void removeUserPolicy(@Nonnull String arg0, @Nonnull String arg1)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public void saveGroup(@Nonnull String arg0, @Nullable String arg1,
			@Nullable String arg2) throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	@Nonnull
	public String[] saveGroupPolicy(@Nonnull String arg0, @Nonnull String arg1,
			@Nonnull CloudPermission arg2, @Nullable ServiceAction arg3,
			@Nullable String arg4) throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public void saveUser(@Nonnull String arg0, @Nullable String arg1,
			@Nullable String arg2) throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	@Nonnull
	public String[] saveUserPolicy(@Nonnull String arg0, @Nonnull String arg1,
			@Nonnull CloudPermission arg2, @Nullable ServiceAction arg3,
			@Nullable String arg4) throws CloudException, InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}


	@Override
	public void removeAccessKey(@Nonnull String arg0) throws CloudException,
			InternalException {
		throw new OperationNotSupportedException("Provider does not support access keys");
	}

}
