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

package org.dasein.cloud.test.platform;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.DataFormat;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.platform.EndpointType;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.platform.PushNotificationSupport;
import org.dasein.cloud.platform.Topic;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 3/6/13 5:17 PM</p>
 *
 * @author George Reese
 */
public class StatefulNotificationsTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulNotificationsTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testTopicId;

    public StatefulNotificationsTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        if( name.getMethodName().equals("publish") || name.getMethodName().equals("subscribe") ) {
            testTopicId = tm.getTestTopicId(DaseinTestManager.STATEFUL, true);
        }
        else if( name.getMethodName().equals("removeTopic") ) {
            testTopicId = tm.getTestTopicId(DaseinTestManager.REMOVED, true);
        }
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void createTopic() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        PushNotificationSupport support = services.getPushNotificationSupport();

        if( support == null ) {
            tm.ok("Push notifications are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        PlatformResources r = DaseinTestManager.getPlatformResources();

        if( r == null ) {
            fail("Failed to initialize platform resources for all tests");
        }
        if( support.isSubscribed() ) {
            String topicId = r.provisionTopic(support, "provision", "dsncreatetopic");

            tm.out("New Topic", topicId);
            assertNotNull("Failed to create the notification topic though the request completed", topicId);

            Topic topic = support.getTopic(topicId);

            assertNotNull("Failed to create the notification topic though an ID was provided", topic);
        }
        else {
            tm.ok("Push notification support is not subscribed so this test is not valid");
        }
    }

    @Test
    public void publish() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        PushNotificationSupport support = services.getPushNotificationSupport();

        if( support == null ) {
            tm.ok("Push notifications are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testTopicId != null ) {
            support.publish(testTopicId, "Dasein Test Subject " + System.currentTimeMillis(), "This is a test");
            // not much else we can test right now
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to push notifications support so this test is invalid");
            }
            else {
                fail("No test topic was created to support this stateful test.");
            }
        }
    }

    @Test
    public void subscribe() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        PushNotificationSupport support = services.getPushNotificationSupport();

        if( support == null ) {
            tm.ok("Push notifications are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testTopicId != null ) {
            support.subscribe(testTopicId, EndpointType.EMAIL, DataFormat.PLAINTEXT, "test@example.com");
            // Can't really validate this
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to push notifications support so this test is invalid");
            }
            else {
                fail("No test topic was created to support this stateful test.");
            }
        }
    }

    @Test
    public void removeTopic() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        PushNotificationSupport support = services.getPushNotificationSupport();

        if( support == null ) {
            tm.ok("Push notifications are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testTopicId != null ) {
            Topic topic = support.getTopic(testTopicId);

            tm.out("Before", topic);
            assertNotNull("The test topic does not exist and thus removal cannot be tested", topic);
            support.removeTopic(testTopicId);
            topic = support.getTopic(testTopicId);

            tm.out("After", topic);
            assertNull("The test topic still exists post-removal", topic);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to push notifications support so this test is invalid");
            }
            else {
                fail("No test topic was created to support this stateful test.");
            }
        }
    }
}
