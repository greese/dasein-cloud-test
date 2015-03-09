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
import org.dasein.cloud.platform.MQSupport;
import org.dasein.cloud.platform.MessageQueue;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

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
 * Integration tests for validating stateless functionality against cloud message queue services.
 * <p>Created by George Reese: 7/24/13 8:40 AM</p>
 * @author George Reese
 * @version 2013.07 initial version (issue #6)
 */
public class StatelessMQTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessMQTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testMQId;

    public StatelessMQTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testMQId = tm.getTestQueueId(DaseinTestManager.STATELESS, false);
    }

    @After
    public void after() {
        tm.end();
    }

    private void assertMessageQueue(MessageQueue q) {
        assertNotNull("The message queue owner account may not be null", q.getProviderOwnerId());
        assertNotNull("The message queue region may not be null", q.getProviderRegionId());
        assertNotNull("The message queue ID may not be null", q.getProviderMessageQueueId());
        assertNotNull("The message queue name may not be null", q.getName());
        assertNotNull("The message queue description may not be null", q.getDescription());
        assertNotNull("The message queue state may not be null", q.getCurrentState());
        assertNotNull("The message queue delay may not be null", q.getDelay());
        assertNotNull("The message queue tags may not be null", q.getTags());
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        MQSupport support = services.getMessageQueueSupport();

        if( support == null ) {
            tm.ok("Message queues are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        tm.out("Subscribed", support.isSubscribed());
        tm.out("Term for Message Queue", support.getProviderTermForMessageQueue(Locale.getDefault()));
    }

    @Test
    public void getBogusQueue() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        MQSupport support = services.getMessageQueueSupport();

        if( support == null ) {
            tm.ok("Message queues are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        MessageQueue q = support.getMessageQueue(UUID.randomUUID().toString());

        tm.out("Bogus Message Queue", q);
        assertNull("Found a matching message queue for the randomly generated MQ ID", q);
    }

    @Test
    public void getQueue() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        MQSupport support = services.getMessageQueueSupport();

        if( support == null ) {
            tm.ok("Message queues are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testMQId != null ) {
            MessageQueue q = support.getMessageQueue(testMQId);

            tm.out("Message Queue", q);
            assertNotNull("No message queue was found matching the test ID " + testMQId, q);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to MQ support so this test is invalid");
            }
            else {
                fail("No test message queue was found to support this stateless test. Please create one and run again.");
            }
        }
    }

    @Test
    public void queueContent() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        MQSupport support = services.getMessageQueueSupport();

        if( support == null ) {
            tm.ok("Message queues are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testMQId != null ) {
            MessageQueue q = support.getMessageQueue(testMQId);

            assertNotNull("No message queue was found matching the test ID " + testMQId, q);
            tm.out("MQ ID", q.getProviderMessageQueueId());
            tm.out("Current State", q.getCurrentState());
            tm.out("Name", q.getName());
            tm.out("Owner Account", q.getProviderOwnerId());
            tm.out("Region ID", q.getProviderRegionId());
            tm.out("Endpoint", q.getEndpoint());
            tm.out("Delay", q.getDelay());
            tm.out("Retention Period", q.getRetentionPeriod());
            tm.out("Visibility Timeout", q.getVisibilityTimeout());
            tm.out("Max Message Size", q.getMaximumMessageSize());
            Map<String,String> tags = q.getTags();

            //noinspection ConstantConditions
            if( tags != null ) {
                for( Map.Entry<String,String> entry : tags.entrySet() ) {
                    tm.out("Tag " + entry.getKey(), entry.getValue());
                }
            }
            tm.out("Description", q.getDescription());
            assertMessageQueue(q);
            assertEquals("The message queue ID from the result does not match the request", testMQId, q.getProviderMessageQueueId());
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to MQ support so this test is invalid");
            }
            else {
                fail("No test message queue was found to support this stateless test. Please create one and run again.");
            }
        }
    }

    @Test
    public void listQueues() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        MQSupport support = services.getMessageQueueSupport();

        if( support == null ) {
            tm.ok("Message queues are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Iterable<MessageQueue> queues = support.listMessageQueues();
        int count = 0;

        assertNotNull("The list of message queues must be non-null, even if not supported or subscribed", queues);
        for( MessageQueue q : queues ) {
            count++;
            tm.out("Message Queue", q);
        }
        tm.out("Total MQ Count", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
                tm.ok("No message queue subscription, so this test is not valid");
            }
            else {
                tm.warn("No message queues were returned, so this test is not valid");
            }
        }
        else if( !support.isSubscribed() ) {
            fail("Message queues were returned for an account without a message queue subscription");
        }
        for( MessageQueue q : queues ) {
            assertMessageQueue(q);
        }
    }

    @Test
    public void listQueueStatus() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        MQSupport support = services.getMessageQueueSupport();

        if( support == null ) {
            tm.ok("Message queues are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Iterable<ResourceStatus> queues = support.listMessageQueueStatus();
        int count = 0;

        assertNotNull("The list of message queue status objects must be non-null, even if not supported or subscribed", queues);
        for( ResourceStatus q : queues ) {
            count++;
            tm.out("MQ Status", q);
        }
        tm.out("Total MQ Status Count", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
                tm.ok("No message queue subscription, so this test is not valid");
            }
            else {
                tm.warn("No message queue status was returned, so this test is not valid");
            }
        }
        else if( !support.isSubscribed() ) {
            fail("Message queue status objects were returned for an account without a message queue subscription");
        }
    }

    @Test
    public void compareQueueListAndStatus() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        MQSupport support = services.getMessageQueueSupport();

        if( support == null ) {
            tm.ok("Message queues are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
        Iterable<MessageQueue> queues = support.listMessageQueues();
        Iterable<ResourceStatus> status = support.listMessageQueueStatus();

        assertNotNull("listMessageQueues() must return at least an empty collections and may not be null", queues);
        assertNotNull("listMessageQueueStatus() must return at least an empty collection and may not be null", status);
        for( ResourceStatus s : status ) {
            Map<String,Boolean> current = map.get(s.getProviderResourceId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(s.getProviderResourceId(), current);
            }
            current.put("status", true);
        }
        for( MessageQueue q : queues ) {
            Map<String,Boolean> current = map.get(q.getProviderMessageQueueId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(q.getProviderMessageQueueId(), current);
            }
            current.put("queue", true);
        }
        for( Map.Entry<String,Map<String,Boolean>> entry : map.entrySet() ) {
            Boolean s = entry.getValue().get("status");
            Boolean q = entry.getValue().get("queue");

            assertTrue("Status and message queue lists do not match for " + entry.getKey(), s != null && q != null && s && q);
        }
        tm.out("Matches");
    }
}
