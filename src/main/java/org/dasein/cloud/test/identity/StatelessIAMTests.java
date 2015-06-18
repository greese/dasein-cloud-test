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
import org.dasein.cloud.identity.CloudGroup;
import org.dasein.cloud.identity.CloudPolicy;
import org.dasein.cloud.identity.CloudUser;
import org.dasein.cloud.identity.IdentityAndAccessSupport;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import java.util.UUID;

import static org.junit.Assert.*;
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

    public StatelessIAMTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testGroupId = tm.getTestGroupId(DaseinTestManager.STATELESS, false);
        testUserId = tm.getTestUserId(DaseinTestManager.STATELESS, false, testGroupId);
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        tm.out("Subscribed", support.isSubscribed());
        tm.out("Supports Access Controls", support.getCapabilities().supportsAccessControls());
        tm.out("Supports API Access", support.getCapabilities().supportsAPIAccess());
        tm.out("Supports Console Access", support.getCapabilities().supportsConsoleAccess());
    }

    @Test
    public void getBogusGroup() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        CloudGroup group = support.getGroup(UUID.randomUUID().toString());

        tm.out("Bogus Group", group);
        assertNull("A bogus group was found with a random UUID as an identifier", group);
    }

    @Test
    public void getGroup() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testGroupId != null ) {
            CloudGroup group = support.getGroup(testGroupId);

            tm.out("Group", group);
            assertNotNull("No group was found under the test group ID", group);
        }
        else {
            if( !support.isSubscribed() ) {
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
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testGroupId != null ) {
            CloudGroup group = support.getGroup(testGroupId);

            assertNotNull("No group was found under the test group ID", group);
            tm.out("Group ID", group.getProviderGroupId());
            tm.out("Name", group.getName());
            tm.out("Owner Account", group.getProviderOwnerId());
            tm.out("Path", group.getPath());
            assertGroup(group);
            assertEquals("ID does not match requested group", testGroupId, group.getProviderGroupId());
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running " + name.getMethodName());
            }
        }
    }

    @Test
    public void listGroups() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Iterable<CloudGroup> groups = support.listGroups(null);
        int count = 0;

        assertNotNull("The groups listing may not be null regardless of subscription level or requested path base", groups);

        for( CloudGroup group : groups ) {
            count++;
            tm.out("Group", group);
        }
        tm.out("Total Group Count", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
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
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testGroupId != null ) {
            Iterable<CloudPolicy> policies = support.listPoliciesForGroup(testGroupId);
            int count = 0;

            assertNotNull("The policies listing may not be null regardless of subscription level or requested group", policies);

            for( CloudPolicy policy : policies ) {
                count++;
                tm.out(testGroupId + " Group Policy", policy);
            }
            tm.out("Total Group Policy Count in " + testGroupId, count);
            if( count < 1 ) {
                if( !support.isSubscribed() ) {
                    tm.ok("Not subscribed to IAM services, so no policies exist");
                }
                else {
                    tm.warn("No policies were returned so this test may be invalid");
                }
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running " + name.getMethodName());
            }
        }
    }


    @Test
    public void getBogusUser() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        CloudUser user = support.getUser(UUID.randomUUID().toString());

        tm.out("Bogus User", user);
        assertNull("A bogus user was found with a random UUID as an identifier", user);
    }

    @Test
    public void getUser() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testUserId != null ) {
            CloudUser user = support.getUser(testUserId);

            tm.out("User", user);
            assertNotNull("No user was found under the test user ID", user);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test user exists for running " + name.getMethodName());
            }
        }
    }

    private void assertUser(@Nonnull CloudUser user) {
        assertNotNull("The user ID may not be null", user.getProviderUserId());
        assertNotNull("The owner account may not be null", user.getProviderOwnerId());
        assertNotNull("The user name may not be null", user.getUserName());
    }

    @Test
    public void userContent() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testUserId != null ) {
            CloudUser user = support.getUser(testUserId);

            assertNotNull("No user was found under the test user ID", user);
            tm.out("User ID", user.getProviderUserId());
            tm.out("User Name", user.getUserName());
            tm.out("Owner Account", user.getProviderOwnerId());
            tm.out("Path", user.getPath());

            assertUser(user);
            assertEquals("The ID for the returned user does not match the one requested", testUserId, user.getProviderUserId());
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test user exists for running " + name.getMethodName());
            }
        }
    }

    @Test
    public void listUsersInPath() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Iterable<CloudUser> users = support.listUsersInPath(null);
        int count = 0;

        assertNotNull("The users listing may not be null regardless of subscription level or requested path base", users);

        for( CloudUser user : users ) {
            count++;
            tm.out("User", user);
        }
        tm.out("Total User Count", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
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
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testGroupId != null ) {
            Iterable<CloudUser> users = support.listUsersInGroup(testGroupId);
            int count = 0;

            assertNotNull("The users listing may not be null regardless of subscription level or requested group", users);

            for( CloudUser user : users ) {
                count++;
                tm.out(testGroupId + " User", user);
            }
            tm.out("Total User Count in " + testGroupId, count);
            if( count < 1 ) {
                if( !support.isSubscribed() ) {
                    tm.ok("Not subscribed to IAM services, so no users exist");
                }
                else {
                    tm.warn("No users were returned so this test may be invalid");
                }
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running " + name.getMethodName());
            }
        }
    }

    @Test
    public void listGroupsForUser() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testUserId != null ) {
            Iterable<CloudGroup> groups = support.listGroupsForUser(testUserId);
            int count = 0;

            assertNotNull("The groups listing may not be null regardless of subscription level or requested user", groups);

            for( CloudGroup group : groups ) {
                count++;
                tm.out(testUserId + " Group", group);
            }
            tm.out("Total Group Count for " + testUserId, count);
            if( count < 1 ) {
                if( !support.isSubscribed() ) {
                    tm.ok("Not subscribed to IAM services, so no groups exist");
                }
                else {
                    tm.warn("No groups were returned so this test may be invalid");
                }
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test group exists for running " + name.getMethodName());
            }
        }
    }

    @Test
    public void listUserPolicies() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services == null ) {
            tm.ok("Identity services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

        if( support == null ) {
            tm.ok("Identity and access management is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testUserId != null ) {
            Iterable<CloudPolicy> policies = support.listPoliciesForUser(testUserId);
            int count = 0;

            assertNotNull("The policies listing may not be null regardless of subscription level or requested user", policies);

            for( CloudPolicy policy : policies ) {
                count++;
                tm.out(testUserId + " User Policy", policy);
            }
            tm.out("Total Policy Count in " + testUserId, count);
            if( count < 1 ) {
                if( !support.isSubscribed() ) {
                    tm.ok("Not subscribed to IAM services, so no policies exist");
                }
                else {
                    tm.warn("No policies were returned so this test may be invalid");
                }
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to IAM services");
            }
            else {
                fail("No test user exists for running " + name.getMethodName());
            }
        }
    }
}
