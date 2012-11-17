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
    public void testSubscription() throws CloudException, InternalException {
        begin();
        assertTrue("Account not subscribed for this feature, not testable", getSupport().isSubscribed());
        end();
    }
    
    @Test
    public void testCreateGroup() throws InternalException, CloudException {
        begin();
        groupToDelete = getSupport().createGroup("DSN Test" + System.currentTimeMillis(), "/dsntest", false);
        assertNotNull("No cloud group was created", groupToDelete);
        out("ID:       " + groupToDelete.getProviderGroupId());
        out("Owner ID: " + groupToDelete.getProviderOwnerId());
        out("Name:     " + groupToDelete.getName());
        out("Path:     " + groupToDelete.getPath());
        assertNotNull("ID cannot be null", groupToDelete.getProviderGroupId());
        assertNotNull("Owner cannot be null", groupToDelete.getProviderOwnerId());
        assertNotNull("Name must not be null", groupToDelete.getName());
        end();
    }
    
    @Test
    public void testListGroups() throws InternalException, CloudException {
        begin();
        for( CloudGroup group : getSupport().listGroups(null) ) {
            out("Group: " + group);
        }
        end();
    }

    @Test
    public void testGroupContent() throws InternalException, CloudException {
        begin();
        CloudGroup group = getSupport().getGroup(groupToDelete.getProviderGroupId());
        
        assertNotNull("Test group was not found", group);
        out("ID:       " + groupToDelete.getProviderGroupId());
        out("Owner ID: " + groupToDelete.getProviderOwnerId());
        out("Name:     " + groupToDelete.getName());
        out("Path:     " + groupToDelete.getPath());
        assertEquals("Groups do not match", groupToDelete, group);
        end();
    }

    @Test
    public void testGetBogusGroup() throws InternalException, CloudException {
        begin();
        UUID uuid = UUID.randomUUID();
        CloudGroup group = getSupport().getGroup(uuid.toString());

        assertNull("Bogus group exists", group);
        end();
    }

    @Test
    public void testRemoveGroup() throws InternalException, CloudException {
        begin();
        String groupId = groupToDelete.getProviderGroupId();
        
        groupToDelete = null;
        getSupport().removeGroup(groupId);
        for( CloudGroup group : getSupport().listGroups(null) ) {
            if( groupId.equals(group.getProviderGroupId()) ) {
                fail("Found group that was supposed to be deleted");
            }
        }
        end();
    }

    @Test
    public void testListGroupPermissions() throws InternalException, CloudException {
        begin();
        for( CloudPolicy policy : getSupport().listPoliciesForGroup(groupToDelete.getProviderGroupId()) ) {
            out("Policy: " + policy);
        }
        end();
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
    
    @Test
    public void testCreateUser() throws InternalException, CloudException {
        begin();
        userToDelete = getSupport().createUser("dsn" + System.currentTimeMillis(), "/dsntest");
        assertNotNull("No cloud user was created", userToDelete);
        out("ID:       " + userToDelete.getProviderUserId());
        out("Owner ID: " + userToDelete.getProviderOwnerId());
        out("UserName: " + userToDelete.getUserName());
        out("Path:     " + userToDelete.getPath());
        assertNotNull("ID cannot be null", userToDelete.getProviderUserId());
        assertNotNull("Owner cannot be null", userToDelete.getProviderOwnerId());
        assertNotNull("User name must not be null", userToDelete.getUserName());
        end();
    }

    @Test
    public void testListUsers() throws InternalException, CloudException {
        begin();
        for( CloudUser user : getSupport().listUsersInPath(null) ) {
            out("User: " + user);
        }
        end();
    }
    
    @Test
    public void testJoinGroup() throws InternalException, CloudException {
        begin();
        getSupport().addUserToGroups(userToDelete.getProviderUserId(), groupToDelete.getProviderGroupId());
        
        boolean groupFound = false, userFound = false;
        
        for( CloudGroup group : getSupport().listGroupsForUser(userToDelete.getProviderUserId()) ) {
            if( group.equals(groupToDelete) ) {
                groupFound = true;
            }
        }
        for( CloudUser user : getSupport().listUsersInGroup(groupToDelete.getProviderGroupId()) ) {
            if( user.equals(userToDelete) ) {
                userFound = true;
            }
        }
        assertTrue("Group was not among user's groups", groupFound);
        assertTrue("User was not found in group", userFound);
        end();
    }

    @Test
    public void testListUsersInGroup() throws InternalException, CloudException {
        begin();
        for( CloudUser user : getSupport().listUsersInGroup(groupToDelete.getProviderGroupId()) ) {
            out("User: " + user);
        }
        end();
    }

    @Test
    public void testListGroupsForUser() throws InternalException, CloudException {
        begin();
        for( CloudGroup group : getSupport().listGroupsForUser(userToDelete.getProviderUserId()) ) {
            out("Group: " + group);
        }
        end();
    }
    
    @Test
    public void testUserContent() throws InternalException, CloudException {
        begin();
        CloudUser user = getSupport().getUser(userToDelete.getProviderUserId());
        
        assertNotNull("Cloud user does not exist", user);
        out("ID:       " + user.getProviderUserId());
        out("Owner ID: " + user.getProviderOwnerId());
        out("UserName: " + user.getUserName());
        out("Path:     " + user.getPath());
        assertNotNull("ID cannot be null", user.getProviderUserId());
        assertNotNull("Owner cannot be null", user.getProviderOwnerId());
        assertNotNull("User name must not be null", user.getUserName());
        end();
    }

    @Test
    public void testGetBogusUser() throws InternalException, CloudException {
        begin();
        UUID uuid = UUID.randomUUID();
        CloudUser user = getSupport().getUser(uuid.toString());
        
        assertNull("Bogus user exists", user);
        end();
    }

    @Test
    public void testRemoveUser() throws InternalException, CloudException {
        begin();
        String userId = userToDelete.getProviderUserId();

        userToDelete = null;
        getSupport().removeUser(userId);
        for( CloudUser user : getSupport().listUsersInPath(null) ) {
            if( userId.equals(user.getProviderUserId()) ) {
                fail("Found user that was supposed to be deleted");
            }
        }
        end();
    }

    @Test
    public void testRemoveUserFromGroup() throws InternalException, CloudException {
        begin();
        getSupport().removeUserFromGroup(userToDelete.getProviderUserId(), groupToDelete.getProviderGroupId());
        boolean groupFound = false, userFound = false;

        for( CloudGroup group : getSupport().listGroupsForUser(userToDelete.getProviderUserId()) ) {
            if( group.equals(groupToDelete) ) {
                groupFound = true;
            }
        }
        for( CloudUser user : getSupport().listUsersInGroup(groupToDelete.getProviderGroupId()) ) {
            if( user.equals(userToDelete) ) {
                userFound = true;
            }
        }
        assertTrue("Group was among user's groups", !groupFound);
        assertTrue("User was found in group", !userFound);
        end();
    }    
}
