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
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.platform.PushNotificationSupport;
import org.dasein.cloud.platform.Subscription;
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

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 3/6/13 5:17 PM</p>
 *
 * @author George Reese
 */
public class StatelessNotificationsTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessNotificationsTests.class);
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

    public StatelessNotificationsTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testTopicId = tm.getTestTopicId(DaseinTestManager.STATELESS, false);
    }

    @After
    public void after() {
        tm.end();
    }

    private void assertSubscription(@Nonnull Subscription subscription) {
        assertNotNull("The subscription ID may not be null", subscription.getProviderSubscriptionId());
        assertEquals("The topic does not match the one it should match", testTopicId, subscription.getProviderTopicId());
        assertNotNull("The subscription owner account may not be null", subscription.getProviderOwnerId());
        assertEquals("The region ID of the returned subscription does not match the current context", tm.getContext().getRegionId(), subscription.getProviderRegionId());
        assertNotNull("The name may not be null", subscription.getName());
        assertNotNull("The description may not be null", subscription.getDescription());
        assertNotNull("The data format may not be null", subscription.getDataFormat());
        assertNotNull("The endpoint may not be null", subscription.getEndpoint());
        assertNotNull("The endpoint type may not be null", subscription.getEndpointType());
    }

    private void assertTopic(@Nonnull Topic topic) {
        assertNotNull("The topic ID may not be null", topic.getProviderTopicId());
        assertNotNull("The topic name may not be null", topic.getName());
        assertNotNull("The topic description may not be null", topic.getDescription());
        assertNotNull("The topic owner account may not be null", topic.getProviderOwnerId());
        assertEquals("The region ID of the returned topic does not match the current context", tm.getContext().getRegionId(), topic.getProviderRegionId());
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
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
        tm.out("Subscribed", support.isSubscribed());
        tm.out("Term for Topic", support.getProviderTermForTopic(Locale.getDefault()));
        tm.out("Term for Subscription", support.getProviderTermForSubscription(Locale.getDefault()));

        assertNotNull("The provider term for a topic may not be null", support.getProviderTermForTopic(Locale.getDefault()));
        assertNotNull("The provider term for a subscription may not be null", support.getProviderTermForSubscription(Locale.getDefault()));
    }

    @Test
    public void getBogusTopic() throws CloudException, InternalException {
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
        Topic topic = support.getTopic(UUID.randomUUID().toString());

        tm.out("Bogus Topic", topic);
        assertNull("The bogus topic was non-null", topic);
    }

    @Test
    public void getTopic() throws CloudException, InternalException {
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

            tm.out("Topic", topic);
            assertNotNull("The test topic was not found in the cloud", topic);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to push notifications support so this test is invalid");
            }
            else {
                fail("No test topic was found to support this stateless test. Please create one and run again.");
            }
        }
    }

    @Test
    public void topicContent() throws CloudException, InternalException {
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

            assertNotNull("The test topic was not found in the cloud", topic);

            tm.out("Topic ID", topic.getProviderTopicId());
            tm.out("Active", topic.isActive());
            tm.out("Name", topic.getName());
            tm.out("Owner Account", topic.getProviderOwnerId());
            tm.out("Region ID", topic.getProviderRegionId());
            tm.out("Description", topic.getDescription());

            assertTopic(topic);
            assertEquals("The ID of the returned topic does not match the requested ID", testTopicId, topic.getProviderTopicId());
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to push notifications support so this test is invalid");
            }
            else {
                fail("No test topic was found to support this stateless test. Please create one and run again.");
            }
        }
    }

    @Test
    public void listTopics() throws CloudException, InternalException {
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
        Iterable<Topic> topics = support.listTopics();
        int count = 0;

        assertNotNull("The list of topics may not be null", topics);
        for( Topic topic : topics ) {
            count++;
            tm.out("Topic", topic);
        }
        tm.out("Total Topic Count", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
                tm.ok("The topic count was 0 as it should be in an unsubscribed account");
            }
            else {
                tm.warn("No topics were identified in the account so this test is potentially invalid");
            }
        }
        else if( !support.isSubscribed() ) {
            fail("Found topics in the account even though it is marked as unsubscribed");
        }
        for( Topic topic : topics ) {
            assertTopic(topic);
        }
    }

    @Test
    public void listTopicStatus() throws CloudException, InternalException {
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
        Iterable<ResourceStatus> topics = support.listTopicStatus();
        int count = 0;

        assertNotNull("The list of topics may not be null", topics);
        for( ResourceStatus topic : topics ) {
            count++;
            tm.out("Topic Status", topic);
        }
        tm.out("Total Topic Status Count", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
                tm.ok("The topic status count was 0 as it should be in an unsubscribed account");
            }
            else {
                tm.warn("No topic status objects were identified in the account so this test is potentially invalid");
            }
        }
        else if( !support.isSubscribed() ) {
            fail("Found topic status instances in the account even though it is marked as unsubscribed");
        }
    }

    @Test
    public void compareTopicListAndStatus() throws CloudException, InternalException {
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
        HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
        Iterable<Topic> topics = support.listTopics();
        Iterable<ResourceStatus> status = support.listTopicStatus();

        assertNotNull("listTopics() must return at least an empty collections and may not be null", topics);
        assertNotNull("listTopicStatus() must return at least an empty collection and may not be null", status);
        for( ResourceStatus s : status ) {
            Map<String,Boolean> current = map.get(s.getProviderResourceId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(s.getProviderResourceId(), current);
            }
            current.put("status", true);
        }
        for( Topic t : topics ) {
            Map<String,Boolean> current = map.get(t.getProviderTopicId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(t.getProviderTopicId(), current);
            }
            current.put("topic", true);
        }
        for( Map.Entry<String,Map<String,Boolean>> entry : map.entrySet() ) {
            Boolean s = entry.getValue().get("status");
            Boolean t = entry.getValue().get("topic");

            assertTrue("Status and topic lists do not match for " + entry.getKey(), s != null && t != null && s && t);
        }
        tm.out("Matches");
    }

    @Test
    public void listSubscriptions() throws CloudException, InternalException {
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
            Iterable<Subscription> list = support.listSubscriptions(testTopicId);
            int count = 0;

            assertNotNull("The list of subscriptions may not be null", list);
            for( Subscription subscription : list ) {
                count++;
                tm.out("Subscription", subscription);
            }
            tm.out("Total Subscription Count to " + testTopicId, count);
            if( count < 1 ) {
                if( !support.isSubscribed() ) {
                    tm.ok("The susbcription count was 0 as it should be in an unsubscribed account");
                }
                else {
                    tm.warn("No subscriptions were identified for the test topic in the account so this test is potentially invalid");
                }
            }
            else if( !support.isSubscribed() ) {
                fail("Found subscriptions in the account even though it is marked as unsubscribed");
            }
            for( Subscription subscription : list ) {
                assertSubscription(subscription);
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to push notifications support so this test is invalid");
            }
            else {
                fail("No test topic was found to support this stateless test. Please create one and run again.");
            }
        }
    }
}
