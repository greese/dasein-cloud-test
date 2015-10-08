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
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Tests of the stateless features of Dasein Cloud IAM support.
 * <p>Created by George Reese: 2/28/13 7:24 AM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatelessIAMTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessIAMTests.class);
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
    private String testUserId;
    private IdentityServices identityServices;
    private IdentityAndAccessSupport identityAndAccessSupport;

    public StatelessIAMTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        identityServices = tm.getProvider().getIdentityServices();

        if( identityServices == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        identityAndAccessSupport = identityServices.getIdentityAndAccessSupport();
        if( identityAndAccessSupport == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        testGroupId = tm.getTestGroupId(DaseinTestManager.STATELESS, false);
        testUserId = tm.getTestUserId(DaseinTestManager.STATELESS, false, testGroupId);
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        if( identityAndAccessSupport == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        tm.out("Subscribed", identityAndAccessSupport.isSubscribed());
        tm.out("Supports Access Controls", identityAndAccessSupport.getCapabilities().supportsAccessControls());
        tm.out("Supports API Access", identityAndAccessSupport.getCapabilities().supportsApiAccess());
        tm.out("Supports Console Access", identityAndAccessSupport.getCapabilities().supportsConsoleAccess());
    }

    @Test
    public void getBogusGroup() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        CloudGroup group = identityAndAccessSupport.getGroup(UUID.randomUUID().toString());

        tm.out("Bogus Group", group);
        assertNull("A bogus group was found with a random UUID as an identifier", group);
    }

    @Test
    public void getGroup() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        if( testGroupId != null ) {
            CloudGroup group = identityAndAccessSupport.getGroup(testGroupId);

            tm.out("Group", group);
            assertNotNull("No group was found under the test group ID", group);
        }
        else {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running " + name.getMethodName());
            }
        }
    }

    private void assertGroup(@Nonnull CloudGroup group) {
        assertNotNull("The group ID may not be null", group.getProviderGroupId());
        assertNotNull("The owner account may not be null", group.getProviderOwnerId());
        assertNotNull("The name may not be null", group.getName());
    }

    @Test
    public void groupContent() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        if( testGroupId != null ) {
            CloudGroup group = identityAndAccessSupport.getGroup(testGroupId);

            assertNotNull("No group was found under the test group ID", group);
            tm.out("Group ID", group.getProviderGroupId());
            tm.out("Name", group.getName());
            tm.out("Owner Account", group.getProviderOwnerId());
            tm.out("Path", group.getPath());
            assertGroup(group);
            assertEquals("ID does not match requested group", testGroupId, group.getProviderGroupId());
        }
        else {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running " + name.getMethodName());
            }
        }
    }

    @Test
    public void listGroups() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        Iterable<CloudGroup> groups = identityAndAccessSupport.listGroups(null);
        int count = 0;

        assertNotNull("The groups listing may not be null regardless of subscription level or requested path base", groups);

        for( CloudGroup group : groups ) {
            count++;
            tm.out("Group", group);
        }
        tm.out("Total Group Count", count);
        if( count < 1 ) {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services, so no groups exist");
            }
            else {
                tm.warn("No groups were returned so this test may be invalid");
            }
        }
        for( CloudGroup group : groups ) {
            assertGroup(group);
        }
    }

    @Test
    public void listGroupPolicies() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        if( testGroupId != null ) {
            Iterable<CloudPolicy> policies = identityAndAccessSupport.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderGroupId(testGroupId));
            int count = 0;

            assertNotNull("The policies listing may not be null regardless of subscription level or requested group", policies);

            for( CloudPolicy policy : policies ) {
                count++;
                tm.out(testGroupId + " Group Policy", policy);
            }
            tm.out("Total Group Policy Count in " + testGroupId, count);
            if( count < 1 ) {
                if( !identityAndAccessSupport.isSubscribed() ) {
                    tm.ok("Not subscribed to IAM services, so no policies exist");
                }
                else {
                    tm.warn("No policies were returned so this test may be invalid");
                }
            }
        }
        else {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running " + name.getMethodName());
            }
        }
    }


    @Test
    public void listAccountManagedPolicies() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        Iterable<CloudPolicy> policies = identityAndAccessSupport.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.ACCOUNT_MANAGED_POLICY));
        int count = 0;

        assertNotNull("The policies listing may not be null regardless of subscription level", policies);

        for( CloudPolicy policy : policies ) {
            count++;
            tm.out("Managed Policy", policy);
        }
        tm.out("Total Managed Policy Count", count);
        boolean supportsAccountManagedPolicies = false;
        for( CloudPolicyType type : identityAndAccessSupport.getCapabilities().listSupportedPolicyTypes() ) {
            if( CloudPolicyType.ACCOUNT_MANAGED_POLICY.equals(type) ) {
                supportsAccountManagedPolicies = true;
                break;
            }
        }
        if( count < 1 ) {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services, so no policies exist");
            }
            else if( supportsAccountManagedPolicies ) {
                fail("Provider " + tm.getProvider().getProviderName() + " declares its support for provider managed policies, however there were no policies returned");
            }
            else {
                tm.warn("No policies were returned so this test may be invalid");
            }
        }
    }

    @Test
    public void listAccessKeys() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        Iterable<AccessKey> accessKeys = identityAndAccessSupport.listAccessKeys(null);
        int count = 0;

        assertNotNull("The access keys listing may not be null regardless of subscription level", accessKeys);

        for( AccessKey accessKey : accessKeys ) {
            count++;
            tm.out("Access Key", accessKey);
        }
        tm.out("Total Access Key Count", count);
        boolean supportsAccessKeys = identityAndAccessSupport.getCapabilities().supportsApiAccess();

        if( count < 1 ) {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services, so no policies exist");
            }
            else if( supportsAccessKeys ) {
                fail("Provider " + tm.getProvider().getProviderName() + " declares its support for provider managed policies, however there were no policies returned");
            }
            else {
                tm.warn("No policies were returned so this test may be invalid");
            }
        }
    }

    @Test
    public void listPolicies() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        Iterable<CloudPolicy> policies = identityAndAccessSupport.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.PROVIDER_MANAGED_POLICY));
        int count = 0;

        assertNotNull("The policies listing may not be null regardless of subscription level", policies);

        for( CloudPolicy policy : policies ) {
            count++;
            tm.out("Managed Policy", policy);
        }
        tm.out("Total Managed Policy Count", count);
        boolean supportsProviderManagedPolicies = false;
        for( CloudPolicyType type : identityAndAccessSupport.getCapabilities().listSupportedPolicyTypes() ) {
            if( CloudPolicyType.PROVIDER_MANAGED_POLICY.equals(type) ) {
                supportsProviderManagedPolicies = true;
                break;
            }
        }
        if( count < 1 ) {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services, so no policies exist");
            }
            else if( supportsProviderManagedPolicies ) {
                fail("Provider " + tm.getProvider().getProviderName() + " declares its support for provider managed policies, however there were no policies returned");
            }
            else {
                tm.warn("No policies were returned so this test may be invalid");
            }
        }
    }

    @Test
    public void getBogusUser() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        CloudUser user = identityAndAccessSupport.getUser(UUID.randomUUID().toString());

        tm.out("Bogus User", user);
        assertNull("A bogus user was found with a random UUID as an identifier", user);
    }

    @Test
    public void getUser() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        if( testUserId != null ) {
            CloudUser user = identityAndAccessSupport.getUser(testUserId);

            tm.out("User", user);
            assertNotNull("No user was found under the test user ID", user);
        }
        else {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test user exists for running " + name.getMethodName());
            }
        }
    }

    @Test
    public void getPolicy() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);
        boolean supportsProviderManagedPolicies = false;
        for( CloudPolicyType type : identityAndAccessSupport.getCapabilities().listSupportedPolicyTypes() ) {
            if( CloudPolicyType.PROVIDER_MANAGED_POLICY.equals(type) ) {
                supportsProviderManagedPolicies = true;
                break;
            }
        }

        if( supportsProviderManagedPolicies ) {
            Iterator<CloudPolicy> policiesIterator = identityAndAccessSupport.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.PROVIDER_MANAGED_POLICY)).iterator();
            assertTrue("List of policies must include at least one policy", policiesIterator.hasNext());
            String testPolicyId = policiesIterator.next().getProviderPolicyId();
            CloudPolicy policy = identityAndAccessSupport.getPolicy(testPolicyId, null);
            tm.out("Policy", policy);
            assertNotNull("No policy was found under the test policy ID [" + testPolicyId + "]", policy);
            assertPolicy(policy, CloudPolicyType.PROVIDER_MANAGED_POLICY);
        }
        else {
            try {
                Iterable<CloudPolicy> policies = identityAndAccessSupport.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.PROVIDER_MANAGED_POLICY));
                assertNotNull("List of policies may not be null", policies);
                assertFalse("List of policies should be empty since managed policies are declared as not supported",
                        policies.iterator().hasNext()
                );
            }
            catch(CloudException|InternalException e) {
                tm.ok("Managed policies are not supported");
            }
        }
    }

    private void assertPolicy(@Nonnull CloudPolicy policy, CloudPolicyType type) {
        assertNotNull("The policy ID may not be null", policy.getProviderPolicyId());
        assertNotNull("The policy name may not be null", policy.getName());
        assertEquals("The policy type is wrong", type, policy.getType());
    }

    private void assertUser(@Nonnull CloudUser user) {
        assertNotNull("The user ID may not be null", user.getProviderUserId());
        assertNotNull("The owner account may not be null", user.getProviderOwnerId());
        assertNotNull("The user name may not be null", user.getUserName());
    }

    @Test
    public void userContent() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        if( testUserId != null ) {
            CloudUser user = identityAndAccessSupport.getUser(testUserId);

            assertNotNull("No user was found under the test user ID", user);
            tm.out("User ID", user.getProviderUserId());
            tm.out("User Name", user.getUserName());
            tm.out("Owner Account", user.getProviderOwnerId());
            tm.out("Path", user.getPath());

            assertUser(user);
            assertEquals("The ID for the returned user does not match the one requested", testUserId, user.getProviderUserId());
        }
        else {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test user exists for running " + name.getMethodName());
            }
        }
    }

    @Test
    public void listUsersInPath() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        Iterable<CloudUser> users = identityAndAccessSupport.listUsersInPath(null);
        int count = 0;

        assertNotNull("The users listing may not be null regardless of subscription level or requested path base", users);

        for( CloudUser user : users ) {
            count++;
            tm.out("User", user);
        }
        tm.out("Total User Count", count);
        if( count < 1 ) {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services, so no users exist");
            }
            else {
                tm.warn("No users were returned so this test may be invalid");
            }
        }
        for( CloudUser user : users ) {
            assertUser(user);
        }
    }

    @Test
    public void listUsersInGroup() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        if( testGroupId != null ) {
            Iterable<CloudUser> users = identityAndAccessSupport.listUsersInGroup(testGroupId);
            int count = 0;

            assertNotNull("The users listing may not be null regardless of subscription level or requested group", users);

            for( CloudUser user : users ) {
                count++;
                tm.out(testGroupId + " User", user);
            }
            tm.out("Total User Count in " + testGroupId, count);
            if( count < 1 ) {
                if( !identityAndAccessSupport.isSubscribed() ) {
                    tm.ok("Not subscribed to IAM services, so no users exist");
                }
                else {
                    tm.warn("No users were returned so this test may be invalid");
                }
            }
        }
        else {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running " + name.getMethodName());
            }
        }
    }

    @Test
    public void listGroupsForUser() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        if( testUserId != null ) {
            Iterable<CloudGroup> groups = identityAndAccessSupport.listGroupsForUser(testUserId);
            int count = 0;

            assertNotNull("The groups listing may not be null regardless of subscription level or requested user", groups);

            for( CloudGroup group : groups ) {
                count++;
                tm.out(testUserId + " Group", group);
            }
            tm.out("Total Group Count for " + testUserId, count);
            if( count < 1 ) {
                if( !identityAndAccessSupport.isSubscribed() ) {
                    tm.ok("Not subscribed to IAM services, so no groups exist");
                }
                else {
                    tm.warn("No groups were returned so this test may be invalid");
                }
            }
        }
        else {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running " + name.getMethodName());
            }
        }
    }

    @Test
    public void listUserPolicies() throws CloudException, InternalException {
        assumeNotNull(identityServices);
        assumeNotNull(identityAndAccessSupport);

        if( testUserId != null ) {
            Iterable<CloudPolicy> policies = identityAndAccessSupport.listPolicies(CloudPolicyFilterOptions.getInstance(CloudPolicyType.INLINE_POLICY).withProviderUserId(testUserId));
            int count = 0;

            assertNotNull("The policies listing may not be null regardless of subscription level or requested user", policies);

            for( CloudPolicy policy : policies ) {
                count++;
                tm.out(testUserId + " User Policy", policy);
                assertPolicy(policy, CloudPolicyType.INLINE_POLICY);
            }
            tm.out("Total Policy Count in " + testUserId, count);
            if( count < 1 ) {
                if( !identityAndAccessSupport.isSubscribed() ) {
                    tm.ok("Not subscribed to IAM services, so no policies exist");
                }
                else {
                    tm.warn("No policies were returned so this test may be invalid");
                }
            }
        }
        else {
            if( !identityAndAccessSupport.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test user exists for running " + name.getMethodName());
            }
        }
    }
}
