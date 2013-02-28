/**
 * Copyright (C) 2009-2012 enStratus Networks Inc
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

package org.dasein.cloud.test;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.identity.CloudGroup;
import org.dasein.cloud.identity.CloudPermission;
import org.dasein.cloud.identity.CloudPolicy;
import org.dasein.cloud.identity.CloudUser;
import org.dasein.cloud.identity.IdentityAndAccessSupport;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.util.APITrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.UUID;

public class IAMTestCase extends BaseTestCase {
    private CloudProvider cloud         = null;
    private CloudGroup    groupToDelete = null;
    private CloudUser     userToDelete  = null;
    
    public IAMTestCase(String name) { super(name); }

    private @Nonnull IdentityAndAccessSupport getSupport() {
        IdentityServices svc = cloud.getIdentityServices();

        assertNotNull("No identity services exist for this provider", svc);

        IdentityAndAccessSupport support = svc.getIdentityAndAccessSupport();

        assertNotNull("No identity and access support exists for this provider", support);

        return support;
    }

    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        String name = getName();
        
        cloud = getProvider();
        cloud.connect(getTestContext());
        if( name.equals("testGroupContent") || name.equals("testRemoveGroup") || name.equals("testJoinGroup") || name.equals("testListUsersInGroup") || name.equals("testListGroupsForUser") || name.equals("testRemoveUserFromGroup") || name.equals("testCreateGroupPermission") || name.equals("testListGroupPermissions") ) {
            groupToDelete = getSupport().createGroup("DSN Test" + System.currentTimeMillis(), "/dsntest", false);
        }
        if( name.equals("testUserContent") || name.equals("testJoinGroup") || name.equals("testListUsersInGroup") || name.equals("testListGroupsForUser") || name.equals("testRemoveUser") || name.equals("testRemoveUserFromGroup") ) {
            userToDelete = getSupport().createUser("dsn" + System.currentTimeMillis(), "/dsntest");
        }
        if( name.equals("testListUsersInGroup") || name.equals("testListGroupsForUser") || name.equals("testRemoveUserFromGroup") ) {
            getSupport().addUserToGroups(userToDelete.getProviderUserId(), groupToDelete.getProviderGroupId());            
        }
        if( name.equals("testListGroupPermissions") ) {
            getSupport().saveGroupPolicy(groupToDelete.getProviderGroupId(), "DSN" + System.currentTimeMillis(), CloudPermission.ALLOW, FirewallSupport.ANY, null);            
        }
    }
    
    @After
    @Override
    public void tearDown() {
        try {
            if( userToDelete != null ) {
                getSupport().removeUser(userToDelete.getProviderUserId());
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( groupToDelete != null ) {
                getSupport().removeGroup(groupToDelete.getProviderGroupId());
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        APITrace.report(getName());
        APITrace.reset();
        try {
            if( cloud != null ) {
                cloud.close();
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
    }
    
    @Test
    public void testCreateGroupPermission() throws InternalException, CloudException {
        begin();
        String name = "DSN" + System.currentTimeMillis();

        getSupport().saveGroupPolicy(groupToDelete.getProviderGroupId(), name, CloudPermission.ALLOW, FirewallSupport.CREATE_FIREWALL, null);
        boolean found = false;
        for( CloudPolicy policy : getSupport().listPoliciesForGroup(groupToDelete.getProviderGroupId()) ) {
            if( policy.getName().equals(name) ) {
                if( policy.getPermission().equals(CloudPermission.ALLOW) ) {
                    if( policy.getAction().equals(FirewallSupport.CREATE_FIREWALL) ) {
                        if( policy.getResourceId() == null ) {
                            found = true;
                            break;
                        }
                    }
                }
            }
        }
        assertTrue("Unable to find new group permission", found);
        end();
    }
}
