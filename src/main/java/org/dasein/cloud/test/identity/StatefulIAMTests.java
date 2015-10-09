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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.identity.*;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.CalendarWrapper;
import org.junit.*;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

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
    static private final String DSN_PREFIX = "dsn-";
    static private final String MANAGED_POLICY_PREFIX = DSN_PREFIX + "mng-";
    static private final String INLINE_POLICY_PREFIX = DSN_PREFIX + "inl-";

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
    private ServiceAction testAction;

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
        if( support != null ) {
            try {
                testAction = support.listServiceActions(null).iterator().next();
            } 
            catch(Throwable ignore) {
            }
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
                    for( CloudPolicy policy : support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderGroupId(testGroupId)) ) {
                        try { support.removePolicy(policy.getProviderPolicyId(), CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderGroupId(testGroupId)); }
                        catch( Throwable ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try {
                    testPolicyId = support.createPolicy(CloudPolicyOptions.getInstance(INLINE_POLICY_PREFIX + System.currentTimeMillis(), CloudPolicyRule.getInstance(CloudPermission.ALLOW, testAction)).withProviderGroupId(testGroupId));
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
                    for( CloudPolicy policy : support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderUserId(testUserId)) ) {
                        try { support.removePolicy(policy.getProviderPolicyId(), CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderUserId(testUserId)); }
                        catch( Throwable ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try {
                    testPolicyId = support.createPolicy(CloudPolicyOptions.getInstance(INLINE_POLICY_PREFIX + System.currentTimeMillis(), CloudPolicyRule.getInstance(CloudPermission.ALLOW, testAction)).withProviderUserId(testUserId));
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
                    for( CloudPolicy policy : support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderGroupId(testGroupId)) ) {
                        try { support.removePolicy(policy.getProviderPolicyId(), CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderGroupId(testGroupId)); }
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
                    for( CloudPolicy policy : support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderUserId(testUserId)) ) {
                        try { support.removePolicy(policy.getProviderPolicyId(), CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderUserId(testUserId)); }
                        catch( Throwable ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        else if( name.getMethodName().equals("removeManagedPolicy") || name.getMethodName().equals("attachDetachUserPolicy") || name.getMethodName().equals("attachDetachGroupPolicy") ) {
            if( support != null ) {
                try {
                    for( CloudPolicy policy : support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY)) ) {
                        try { support.removePolicy(policy.getProviderPolicyId(), CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY)); }
                        catch( Throwable ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try {
                    testPolicyId = support.createPolicy(CloudPolicyOptions.getInstance(MANAGED_POLICY_PREFIX + System.currentTimeMillis(), CloudPolicyRule.getInstance(CloudPermission.ALLOW, testAction)));
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        else if( name.getMethodName().equals("createManagedPolicy") ) {
            if( support != null ) {
                try {
                    removeAllManagedPolicies();
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
    }

    private void removeAllManagedPolicies() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        for( CloudPolicy policy : support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY)) ) {
            // only delete dasein test policies
            if( policy.getName().startsWith(MANAGED_POLICY_PREFIX) ) {
                try {
                    for (CloudUser user : support.listUsersForPolicy(policy.getProviderPolicyId())) {
                        support.detachPolicyFromUser(policy.getProviderPolicyId(), user.getProviderUserId());
                    }
                    for (CloudGroup group : support.listGroupsForPolicy(policy.getProviderPolicyId())) {
                        support.detachPolicyFromUser(policy.getProviderPolicyId(), group.getProviderGroupId());
                    }
                    support.removePolicy(policy.getProviderPolicyId(), CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY));
                } catch (Throwable ignore) {
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
    public void attachDetachUserPolicy() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        testUserId = tm.getTestUserId(DaseinTestManager.STATEFUL, true, null);
        if( testUserId == null ) {
            if( !support.isSubscribed() ) {
                tm.ok("Test user is not available, but service is not subscribed for so it's ok");
                return;
            }
            else {
                fail("Test user is not available");
            }
        }
        if( testPolicyId == null ) {
            fail("Test policy is not available");
        }
        Iterable<CloudPolicy> policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY).withProviderUserId(testUserId));
        tm.out("Before", policies);
        assertNotNull("List of attached policies must not be empty no matter what", policies);
        assertFalse("List of attached policies must be empty", policies.iterator().hasNext());

        // attach
        support.attachPolicyToUser(testPolicyId, testUserId);

        policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY).withProviderUserId(testUserId));
        tm.out("After attachment", policies);
        assertNotNull("List of attached policies must not be empty no matter what", policies);
        assertTrue("List of attached policies must not be empty", policies.iterator().hasNext());

        // detach
        support.detachPolicyFromUser(testPolicyId, testUserId);

        policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY).withProviderUserId(testUserId));
        tm.out("After detachment", policies);
        assertNotNull("List of attached policies must not be empty no matter what", policies);
        assertFalse("List of attached policies must be empty", policies.iterator().hasNext());
    }

    @Test
    public void attachDetachGroupPolicy() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        testGroupId = tm.getTestGroupId(DaseinTestManager.STATEFUL, true);
        if( testGroupId == null ) {
            if( !support.isSubscribed() ) {
                tm.ok("Test group is not available, but service is not subscribed for so it's ok");
                return;
            }
            else {
                fail("Test group is not available");
            }
        }
        if( testPolicyId == null ) {
            fail("Test policy is not available");
        }
        Iterable<CloudPolicy> policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY).withProviderGroupId(testGroupId));
        tm.out("Before", policies);
        assertNotNull("List of attached policies must not be empty no matter what", policies);
        assertFalse("List of attached policies must be empty", policies.iterator().hasNext());

        // attach
        support.attachPolicyToGroup(testPolicyId, testGroupId);

        policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY).withProviderGroupId(testGroupId));
        tm.out("After attachment", policies);
        assertNotNull("List of attached policies must not be empty no matter what", policies);
        assertTrue("List of attached policies must not be empty", policies.iterator().hasNext());

        // detach
        support.detachPolicyFromGroup(testPolicyId, testGroupId);

        policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY).withProviderGroupId(testGroupId));
        tm.out("After detachment", policies);
        assertNotNull("List of attached policies must not be empty no matter what", policies);
        assertFalse("List of attached policies must be empty", policies.iterator().hasNext());
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

    private boolean findPolicyWithAction(@Nonnull Iterable<CloudPolicy> policies,
                                         @Nullable String matchProviderPolicyId,
                                         @Nonnull ServiceAction action) throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) return false;
        for( CloudPolicy policy : policies ) {
            if( matchProviderPolicyId == null || matchProviderPolicyId.equalsIgnoreCase(policy.getProviderPolicyId()) && policy.getName().startsWith(DSN_PREFIX) ) {
                for( CloudPolicyRule rule : support.getPolicyRules(policy.getProviderPolicyId(), CloudPolicyFilterOptions.getInstance(policy.getType()).withProviderGroupId(policy.getProviderGroupId()).withProviderUserId(policy.getProviderUserId())) ) {
                    if( rule.getPermission().equals(CloudPermission.ALLOW) ) {
                        if( Arrays.binarySearch(rule.getActions(), action) >= 0 ) {
                            if( rule.getResources().length == 0 ) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Test
    public void saveGroupPolicy() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        if( testGroupId != null ) {
            Iterable<CloudPolicy> policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderGroupId(testGroupId));
            tm.out("Before", policies);

            assertFalse("Test policy exists before the start of the test",
                    findPolicyWithAction(policies, null, testAction));

            String policyName = INLINE_POLICY_PREFIX + System.currentTimeMillis();

            String id = support.createPolicy(CloudPolicyOptions.getInstance(policyName, CloudPolicyRule.getInstance(CloudPermission.ALLOW, testAction)).withProviderGroupId(testGroupId));

            policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderGroupId(testGroupId));
            tm.out("After", policies);

            assertTrue("Unable to find new group permission",
                    findPolicyWithAction(policies, id, testAction));
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
            Iterable<CloudPolicy> policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderGroupId(testGroupId));
            tm.out("Before", policies);

            assertTrue("Unable to find new group permission", findPolicyWithAction(policies, testPolicyId, testAction));

            support.removePolicy(testPolicyId, CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderGroupId(testGroupId));

            policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderGroupId(testGroupId));
            tm.out("After", policies);

            assertFalse("Test policy exists still exists after being removed",
                    findPolicyWithAction(policies, testPolicyId, testAction));
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
            Iterable<CloudPolicy> policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderUserId(testUserId));
            tm.out("Before", policies);

            assertFalse("Test policy exists before the start of the test",
                    findPolicyWithAction(policies, null, testAction));

            String policyName = INLINE_POLICY_PREFIX + System.currentTimeMillis();

            String id = support.createPolicy(CloudPolicyOptions.getInstance(policyName, CloudPolicyRule.getInstance(CloudPermission.ALLOW, testAction)).withProviderUserId(testUserId));

            policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderUserId(testUserId));
            tm.out("After", policies);

            assertTrue("Unable to find new user permission",
                    findPolicyWithAction(policies, id, testAction));
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
            Iterable<CloudPolicy> policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderUserId(testUserId));
            tm.out("Before", policies);

            assertTrue("Unable to find user permission before removal",
                    findPolicyWithAction(policies, testPolicyId, testAction));

            support.removePolicy(testPolicyId, CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderUserId(testUserId));

            policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderUserId(testUserId));
            tm.out("After", policies);

            assertFalse("Test policy exists even after removal",
                    findPolicyWithAction(policies, testPolicyId, testAction));
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

    @Test
    public void removeManagedPolicy() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        if( testPolicyId != null ) {
            Iterable<CloudPolicy> policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY));
            tm.out("Before", policies);

            assertTrue("Unable to find managed policy before removal",
                    findPolicyWithAction(policies, testPolicyId, testAction));

            for( CloudUser user : support.listUsersForPolicy(testPolicyId) ) {
                support.detachPolicyFromUser(testPolicyId, user.getProviderUserId());
            }
            for( CloudGroup group : support.listGroupsForPolicy(testPolicyId) ) {
                support.detachPolicyFromUser(testPolicyId, group.getProviderGroupId());
            }

            support.removePolicy(testPolicyId, CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY));

            policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY));
            tm.out("After", policies);

            assertFalse("Test policy exists even after removal",
                    findPolicyWithAction(policies, testPolicyId, testAction));
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else if( !supportsPolicyType(CloudPolicyType.ACCOUNT_MANAGED_POLICY) )  {
                tm.ok("Driver does not support account managed policies");
            }
            else {
                fail("No test policy exists for running the test " + name.getMethodName());
            }
        }
    }

    private boolean supportsPolicyType( CloudPolicyType type ) throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return false; }
        for( CloudPolicyType t : support.getCapabilities().listSupportedPolicyTypes() ) {
            if( t.equals(type)) return true;
        }
        return false;
    }

    @Test
    public void createManagedPolicy() throws CloudException, InternalException {
        IdentityAndAccessSupport support = getIASupport();
        if( support == null ) { return; }

        Iterable<CloudPolicy> policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY));
        tm.out("Before", policies);

        assertFalse("Test policy exists before the start of the test",
                findPolicyWithAction(policies, null, testAction));

        String policyName = MANAGED_POLICY_PREFIX + System.currentTimeMillis();

        String id = support.createPolicy(CloudPolicyOptions.getInstance(policyName, CloudPolicyRule.getInstance(CloudPermission.ALLOW, testAction)));

        policies = support.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY));
        tm.out("After", policies);

        assertTrue("Unable to find new managed policy permission",
                findPolicyWithAction(policies, id, testAction));
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
