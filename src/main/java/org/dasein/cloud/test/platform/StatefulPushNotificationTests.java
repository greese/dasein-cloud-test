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

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.platform.PushNotificationSupport;
import org.dasein.cloud.platform.Topic;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.*;
import org.junit.rules.TestName;

import java.util.Collection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Verifies the stateful elements of cloud notification support like AWS SNS.
 *
 * @author Cameron Stokes (http://github.com/clstokes)
 * @since 2013-03-05
 */
public class StatefulPushNotificationTests {

    static private final Logger logger = Logger.getLogger(StatefulPushNotificationTests.class);

    private static final String DASEIN_PREFIX = "dasein-topic-";

    private String provisionedTopicId;

    static private DaseinTestManager tm;

    @Rule
    public final TestName name = new TestName();

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulPushNotificationTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    public StatefulPushNotificationTests() {
    }

    @Before
    public void before() {
        String methodName = name.getMethodName();
        tm.begin(methodName);
        assumeTrue(!tm.isTestSkipped());
        if( "testListTopics".equals(methodName) ) {
            addTestTopic();
        }
        else if( "testGetTopic".equals(methodName) ) {
            addTestTopic();
        }
        else if( "testRemoveTopic".equals(methodName) ) {
            addTestTopic();
        }
        else {
            logger.debug("No pre-test work for: " + methodName);
        }
    }

    @After
    public void after() {
        try {
            if( provisionedTopicId != null ) {
                getSupport().removeTopic(provisionedTopicId);
            }
        }
        catch( Throwable ex ) {
            logger.warn(ex);
        }
        provisionedTopicId = null;
        tm.end();
    }

    private void addTestTopic() {
        PushNotificationSupport support = getSupport();
        if( support == null || provisionedTopicId != null ) {
            return;
        }
        try {
            String topicName = DASEIN_PREFIX + getRandomId();
            Topic topic = support.createTopic(topicName);
            provisionedTopicId = topic.getProviderTopicId();
        }
        catch( Throwable ex ) {
            logger.warn(ex);
        }
    }

    /**
     * Requires {@link #addTestTopic()} ()} to be run first.
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Test
    public void testListTopics() throws CloudException, InternalException {
        PushNotificationSupport support = getSupport();
        if( support != null ) {
            Iterable<Topic> topics = support.listTopics();
            assertNotNull(topics);
            for( Topic Topic : topics ) {
                assertTopic(Topic);
            }
        }
        else {
            tm.ok("No PushNotificationSupport in this cloud");
        }
    }

    /**
     * Requires {@link #addTestTopic()} ()} to be run first.
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Test
    public void testGetTopic() throws CloudException, InternalException {
        PushNotificationSupport support = getSupport();
        if( support != null ) {
            Topic topic = support.getTopic(provisionedTopicId);
            assertTopic(topic);
        }
        else {
            tm.ok("No PushNotificationSupport in this cloud");
        }
    }

    @Test
    public void testAddTopic() throws CloudException, InternalException {
        PushNotificationSupport support = getSupport();
        if( support == null ) {
            tm.ok("No PushNotificationSupport in this cloud");
            return;
        }
        try {
            String topicName = DASEIN_PREFIX + getRandomId();
            Topic topic = support.createTopic(topicName);
            provisionedTopicId = topic.getProviderTopicId();
        }
        catch( OperationNotSupportedException expected ) {
            tm.ok("OperationNotSupportedException thrown.");
        }
    }

    /**
     * Requires {@link #addTestTopic()} to be run first.
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Test
    public void testRemoveTopic() throws CloudException, InternalException {
        PushNotificationSupport support = getSupport();
        if( support == null ) {
            tm.ok("No PushNotificationSupport in this cloud");
            return;
        }
        try {
            support.removeTopic(provisionedTopicId);
        }
        catch( OperationNotSupportedException expected ) {
            tm.ok("OperationNotSupportedException thrown.");
        }
    }

    private long getRandomId() {
        return System.currentTimeMillis() % 10000;
    }

    private void assertTopic(Topic topic) {
        assertNotNull(topic);

        assertNotNull(topic.getProviderTopicId());
    }

    private PushNotificationSupport getSupport() {
        PlatformServices services = getServices();

        if( services != null ) {
            return services.getPushNotificationSupport();
        }
        return null;
    }

    private PlatformServices getServices() {
        CloudProvider provider = tm.getProvider();
        return provider.getPlatformServices();
    }

}
