/**
 * Copyright (C) 2009-2015 Dell, Inc.
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

package org.dasein.cloud.test.identity;

import org.dasein.cloud.Cloud;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.identity.CloudGroup;
import org.dasein.cloud.identity.CloudPermission;
import org.dasein.cloud.identity.CloudPolicy;
import org.dasein.cloud.identity.CloudUser;
import org.dasein.cloud.identity.IdentityAndAccessSupport;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.CalendarWrapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nullable;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests of the stateless features of Dasein Cloud IAM support.
 * <p>Created by George Reese: 2/28/13 7:24 AM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatefulIAMTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulIAMTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testGroupId;
    private String testPolicyId;
    private String testUserId;

    public StatefulIAMTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        IdentityServices services = tm.getProvider().getIdentityServices();
        IdentityAndAccessSupport support = null;

        if( services != null ) {
            support = services.getIdentityAndAccessSupport();
        }
        if( name.getMethodName().equals("joinGroup") ) {
            testGroupId = tm.getTestGroupId(DaseinTestManager.STATEFUL, true);
            testUserId = tm.getTestUserId(DaseinTestManager.STATEFUL, true, null);

            if( testUserId != null && testGroupId != null && support != null ) {
                try {
                    Iterable<CloudGroup> groups = support.listGroupsForUser(testUserId);
                    boolean present = false;

                    for( CloudGroup group : groups ) {
                        if( testGroupId.equals(group.getProviderGroupId()) ) {
                            present = true;
                            break;
                        }
                    }
                    if( present ) {
                        support.removeUserFromGroup(testUserId,  testGroupId);
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        else if( name.getMethodName().equals("leaveGroup") ) {
            testGroupId = tm.getTestGroupId(DaseinTestManager.STATEFUL, true);
            testUserId = tm.getTestUserId(DaseinTestManager.STATEFUL, true, null);

            if( testUserId != null && testGroupId != null ) {
                if( support != null ) {
                    try {
                        Iterable<CloudGroup> groups = support.listGroupsForUser(testUserId);
                        boolean present = false;

                        for( CloudGroup group : groups ) {
                            if( testGroupId.equals(group.getProviderGroupId()) ) {
                                present = true;
                                break;
                            }
                        }
                        if( !present ) {
                            support.addUserToGroups(testUserId, testGroupId);
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        else if( name.getMethodName().equals("removeGroup") ) {
            testGroupId = tm.getTestGroupId(DaseinTestManager.REMOVED, true);
        }
        else if( name.getMethodName().equals("removeUser") || name.getMethodName().equals("enableConsoleAccess") ) {
            testUserId = tm.getTestUserId(DaseinTestManager.REMOVED, true, null);
        }
        else if( name.getMethodName().equals("removeGroupPolicy") ) {
            testGroupId = tm.getTestGroupId(DaseinTestManager.STATEFUL, true);
            if( testGroupId != null && support != null ) {
                try {
                    for( CloudPolicy policy : support.listPoliciesForGroup(testGroupId) ) {
                        try { support.removeGroupPolicy(testGroupId, policy.getProviderPolicyId()); }
                        catch( Throwable ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try {
                    String[] ids = support.modifyGroupPolicy(testGroupId, "DSN" + System.currentTimeMillis(), CloudPermission.ALLOW, FirewallSupport.CREATE_FIREWALL, null);

                    if( ids.length > 0 ) {
                        testPolicyId = ids[0];
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        else if( name.getMethodName().equals("removeUserPolicy") ) {
            testUserId = tm.getTestUserId(DaseinTestManager.STATEFUL, true, null);
            if( testUserId != null && support != null ) {
                try {
                    for( CloudPolicy policy : support.listPoliciesForUser(testUserId) ) {
                        try { support.removeUserPolicy(testUserId, policy.getProviderPolicyId()); }
                        catch( Throwable ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try {
                    String[] ids = support.modifyUserPolicy(testUserId, "DSN" + System.currentTimeMillis(), CloudPermission.ALLOW, FirewallSupport.CREATE_FIREWALL, null);

                    if( ids.length > 0 ) {
                        testPolicyId = ids[0];
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        else if( name.getMethodName().equals("saveGroupPolicy") ) {
            testGroupId = tm.getTestGroupId(DaseinTestManager.STATEFUL, true);
            if( testGroupId != null && support != null ) {
                try {
                    for( CloudPolicy policy : support.listPoliciesForGroup(testGroupId) ) {
                        try { support.removeGroupPolicy(testGroupId, policy.getProviderPolicyId()); }
                        catch( Throwable ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        else if( name.getMethodName().equals("saveUserPolicy") ) {
            testUserId = tm.getTestUserId(DaseinTestManager.STATEFUL, true, null);
            if( testUserId != null && support != null ) {
                try {
                    for( CloudPolicy policy : support.listPoliciesForUser(testUserId) ) {
                        try { support.removeUserPolicy(testUserId, policy.getProviderPolicyId()); }
                        catch( Throwable ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
    }

    @After
    public void after() {
        try {
            testGroupId = null;
            testPolicyId = null;
            testUserId = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void createGroup() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        IdentityResources identity = DaseinTestManager.getIdentityResources();

        assertNotNull("The tests failed to initialize a proper set of identity services", identity);
        String groupId = identity.provisionGroup(support, "provision", "dsncgrp");

        tm.out("New Group", groupId);
        assertNotNull("The newly created group ID may not be null", groupId);

        CloudGroup group = support.getGroup(groupId);

        assertNotNull("No group exists in the cloud for the new ID " + groupId, group);
        assertEquals("The IDs for the requested group and the created group do not match", groupId, group.getProviderGroupId());
    }

    @Test
    public void saveGroupPolicy() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        if( testGroupId != null ) {
            Iterable<CloudPolicy> policies = support.listPoliciesForGroup(testGroupId);
            boolean found = false;

            tm.out("Before", policies);

            for( CloudPolicy policy : policies ) {
                if( policy.getPermission().equals(CloudPermission.ALLOW) ) {
                    if( FirewallSupport.CREATE_FIREWALL.equals(policy.getAction()) ) {
                        if( policy.getResourceId() == null ) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            assertFalse("Test policy exists before the start of the test", found);

            String policyName = "DSN" + System.currentTimeMillis();

            String[] ids = support.modifyGroupPolicy(testGroupId, policyName, CloudPermission.ALLOW, FirewallSupport.CREATE_FIREWALL, null);

            policies = support.listPoliciesForGroup(testGroupId);
            tm.out("After", policies);

            for( CloudPolicy policy : policies ) {
                boolean matches = false;

                for( String id : ids ) {
                    if( id.equals(policy.getProviderPolicyId()) ) {
                        matches = true;
                        break;
                    }
                }
                if( matches ) {
                    if( policy.getPermission().equals(CloudPermission.ALLOW) ) {
                        if( FirewallSupport.CREATE_FIREWALL.equals(policy.getAction()) ) {
                            if( policy.getResourceId() == null ) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
            assertTrue("Unable to find new group permission", found);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void removeGroupPolicy() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        if( testGroupId != null ) {
            Iterable<CloudPolicy> policies = support.listPoliciesForGroup(testGroupId);
            boolean found = false;

            tm.out("Before", policies);

            for( CloudPolicy policy : policies ) {
                if( policy.getProviderPolicyId().equals(testPolicyId) ) {
                    if( policy.getPermission().equals(CloudPermission.ALLOW) ) {
                        if( FirewallSupport.CREATE_FIREWALL.equals(policy.getAction()) ) {
                            if( policy.getResourceId() == null ) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }

            assertTrue("Unable to find new group permission", found);

            support.removeGroupPolicy(testGroupId, testPolicyId);

            policies = support.listPoliciesForGroup(testGroupId);
            tm.out("After", policies);

            found = false;

            for( CloudPolicy policy : policies ) {
                if( policy.getProviderPolicyId().equals(testPolicyId) ) {
                    if( policy.getPermission().equals(CloudPermission.ALLOW) ) {
                        if( FirewallSupport.CREATE_FIREWALL.equals(policy.getAction()) ) {
                            if( policy.getResourceId() == null ) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
            assertFalse("Test policy exists before the start of the test", found);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running the test " + name.getMethodName());
            }
        }
    }


    @Test
    public void removeGroup() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        if( testGroupId != null ) {
            CloudGroup group = support.getGroup(testGroupId);

            assertNotNull("The test group does not exist prior to running this test", group);
            tm.out("Before", group);

            support.removeGroup(testGroupId);

            group = support.getGroup(testGroupId);
            tm.out("After", group);
            assertNull("The test group still exists after the removal", group);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void createUser() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        IdentityResources identity = DaseinTestManager.getIdentityResources();

        assertNotNull("The tests failed to initialize a proper set of identity services", identity);
        String userId = identity.provisionUser(support, "provision", "dsncusr");

        tm.out("New User", userId);
        assertNotNull("The newly created user ID may not be null", userId);

        CloudUser user = support.getUser(userId);
        support.enableConsoleAccess(userId, "Passw0rd".getBytes());
        String console = support.getCapabilities().getConsoleUrl();
        assertNotNull("No user exists in the cloud for the new ID " + userId, user);
        assertEquals("The IDs for the requested user and the created user do not match", userId, user.getProviderUserId());
    }

    @Test
    public void enableConsoleAccess() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        if( support.getCapabilities().supportsApiAccess() ) {
            support.enableConsoleAccess(testUserId, ("dI(head{oab)Ju&w"+(System.currentTimeMillis()%10000)).getBytes());
            tm.ok("Enabling console access was successful");
        }
        else {
            try {
                support.enableConsoleAccess(testUserId, ("dI(head{oab)Ju&w" + (System.currentTimeMillis() % 10000)).getBytes());
                fail("Enabling console access is reported as not supported, however the method executes successfully");
            }
            catch(Throwable t) {
                tm.ok("Enabling console access is reported as not supported, the method fails as expected");
            }
        }
    }

    @Test
    public void joinGroup() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        if( testUserId != null && testGroupId != null ) {
            Iterable<CloudGroup> groups = support.listGroupsForUser(testUserId);
            boolean present = false;

            tm.out("Before", groups);
            for( CloudGroup group : groups ) {
                if( testGroupId.equals(group.getProviderGroupId()) ) {
                    present = true;
                    break;
                }
            }
            assertFalse("That user is already a member of the test group", present);
            support.addUserToGroups(testUserId, testGroupId);

            long timeout = System.currentTimeMillis() + CalendarWrapper.MINUTE;

            while( timeout > System.currentTimeMillis() ) {
                groups = support.listGroupsForUser(testUserId);
                for( CloudGroup group : groups ) {
                    if( testGroupId.equals(group.getProviderGroupId()) ) {
                        present = true;
                        break;
                    }
                }
                if( present ) {
                    break;
                }
                try { Thread.sleep(5000L); }
                catch( InterruptedException ignore ) { }
            }
            tm.out("After", groups);
            assertTrue("The user is not a member of the target group after one minute", present);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test user/group exists for running " + name.getMethodName());
            }
        }
    }

    @Test
    public void leaveGroup() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        if( testUserId != null && testGroupId != null ) {
            Iterable<CloudGroup> groups = support.listGroupsForUser(testUserId);
            boolean present = false;

            tm.out("Before", groups);
            for( CloudGroup group : groups ) {
                if( testGroupId.equals(group.getProviderGroupId()) ) {
                    present = true;
                    break;
                }
            }
            assertTrue("The user is not a member of the target group and so cannot be tested for removal", present);
            support.removeUserFromGroup(testUserId, testGroupId);

            long timeout = System.currentTimeMillis() + CalendarWrapper.MINUTE;

            while( timeout > System.currentTimeMillis() ) {
                present = false;
                groups = support.listGroupsForUser(testUserId);
                for( CloudGroup group : groups ) {
                    if( testGroupId.equals(group.getProviderGroupId()) ) {
                        present = true;
                        break;
                    }
                }
                if( !present ) {
                    break;
                }
                try { Thread.sleep(5000L); }
                catch( InterruptedException ignore ) { }
            }
            tm.out("After", groups);
            assertFalse("That user remains a member of the group after one minute", present);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test user/group exists for running " + name.getMethodName());
            }
        }
    }

    @Test
    public void saveUserPolicy() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        if( testUserId != null ) {
            Iterable<CloudPolicy> policies = support.listPoliciesForUser(testUserId);
            boolean found = false;

            tm.out("Before", policies);

            for( CloudPolicy policy : policies ) {
                if( policy.getPermission().equals(CloudPermission.ALLOW) ) {
                    if( FirewallSupport.CREATE_FIREWALL.equals(policy.getAction()) ) {
                        if( policy.getResourceId() == null ) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            assertFalse("Test policy exists before the start of the test", found);

            String policyName = "DSN" + System.currentTimeMillis();

            String[] ids = support.modifyUserPolicy(testUserId, policyName, CloudPermission.ALLOW, FirewallSupport.CREATE_FIREWALL, null);

            policies = support.listPoliciesForUser(testUserId);
            tm.out("After", policies);

            for( CloudPolicy policy : policies ) {
                boolean matches = false;

                for( String id : ids ) {
                    if( id.equals(policy.getProviderPolicyId()) ) {
                        matches = true;
                        break;
                    }
                }
                if( matches ) {
                    if( policy.getPermission().equals(CloudPermission.ALLOW) ) {
                        if( FirewallSupport.CREATE_FIREWALL.equals(policy.getAction()) ) {
                            if( policy.getResourceId() == null ) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
            assertTrue("Unable to find new user permission", found);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test user exists for running the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void removeUserPolicy() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        if( testUserId != null && testPolicyId != null ) {
            Iterable<CloudPolicy> policies = support.listPoliciesForUser(testUserId);
            boolean found = false;

            tm.out("Before", policies);

            for( CloudPolicy policy : policies ) {
                if( policy.getProviderPolicyId().equals(testPolicyId) ) {
                    if( policy.getPermission().equals(CloudPermission.ALLOW) ) {
                        if( FirewallSupport.CREATE_FIREWALL.equals(policy.getAction()) ) {
                            if( policy.getResourceId() == null ) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }

            assertTrue("Unable to find user permission before removal", found);

            support.removeUserPolicy(testUserId, testPolicyId);

            found = false;
            policies = support.listPoliciesForUser(testUserId);
            tm.out("After", policies);

            for( CloudPolicy policy : policies ) {
                if( policy.getProviderPolicyId().equals(testPolicyId) ) {
                    if( policy.getPermission().equals(CloudPermission.ALLOW) ) {
                        if( FirewallSupport.CREATE_FIREWALL.equals(policy.getAction()) ) {
                            if( policy.getResourceId() == null ) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
            assertFalse("Test policy exists even after removal", found);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test user/policy exists for running the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void removeUser() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        if( testUserId != null ) {
            CloudUser user = support.getUser(testUserId);

            assertNotNull("The test user does not exist prior to running this test", user);
            tm.out("Before", user);

            support.removeUser(testUserId);

            user = support.getUser(testUserId);
            tm.out("After", user);
            assertNull("The test user still exists after the removal", user);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test user exists for running the test " + name.getMethodName());
            }
        }
    }

    /**
     * Common boilerplate code to initialise the support class, log if not supported
     * @return support instance if available, else {@code null}
     */
    private @Nullable IdentityAndAccessSupport getIASupport() {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return null;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
        }
        return support;
    }
}
