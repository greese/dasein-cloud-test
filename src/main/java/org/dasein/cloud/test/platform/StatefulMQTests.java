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

import java.util.Iterator;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.platform.MQMessageIdentifier;
import org.dasein.cloud.platform.MQMessageReceipt;
import org.dasein.cloud.platform.MQSupport;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.uom.time.Second;
import org.dasein.util.uom.time.TimePeriod;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Implements integration testing for stateful operations against cloud message queue services.
 * <p>Created by George Reese: 7/24/13 10:32 AM</p>
 * @author George Reese
 * @version 2013.07 initial version (issue #6)
 * @since 2013.07
 */
public class StatefulMQTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulMQTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();
    
    public final int testMessageCount = 5;

    private String testQueueId;

    public StatefulMQTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        if( name.getMethodName().equalsIgnoreCase("removeMessageQueue") ) {
            testQueueId = tm.getTestQueueId(DaseinTestManager.REMOVED, true);
        } 
        else if( !name.getMethodName().equalsIgnoreCase("createMessageQueue") ) {
            testQueueId = tm.getTestQueueId(DaseinTestManager.STATEFUL, true);
        }
        
        if (name.getMethodName().startsWith("receiveMessage")) {
        	int count = 1;
        	if (name.getMethodName().endsWith("s")) {
        		count = testMessageCount;
        	} 
        	PlatformServices services = tm.getProvider().getPlatformServices();
        	MQSupport support = services.getMessageQueueSupport();
        	try {
        		for (int i = 0; i < count; i++) {
        			support.sendMessage(testQueueId, "send message to " + testQueueId + " for receive test");
        		}
			} catch (CloudException e) {
				tm.warn("failed to send message to queue " + testQueueId + ", got exception: " + e.getMessage());
			} catch (InternalException e) {
				tm.warn("failed to send message to queue " + testQueueId + ", got exception: " + e.getMessage());
			}
        }
    }

    @After
    public void after() throws CloudException, InternalException {
        try {
        	testQueueId = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void createMessageQueue() throws CloudException, InternalException {
        
    	PlatformServices services = tm.getProvider().getPlatformServices();
    	if (services == null) {
    		tm.ok("Platform service is not implemented");
    		return;
    	}
    	
    	MQSupport support = services.getMessageQueueSupport();
    	if (support == null) {
    		tm.ok("Message queue is not implemented");
    		return;
    	}
    	
    	PlatformResources resources = DaseinTestManager.getPlatformResources();
    	if (resources != null) {
    		testQueueId = resources.provisionMQ(support, "createMqs", "dsnmq");
    		tm.out("New message queue", testQueueId);
    		assertNotNull(testQueueId);
    	} else {
    		fail("No platform resources were initialized for the test run");
    	}
    }

    @Test
    public void removeMessageQueue() throws CloudException, InternalException {
       
    	PlatformServices services = tm.getProvider().getPlatformServices();
    	if (services == null) {
    		tm.ok("Platform service is not implemented");
    		return;
    	}
    	
    	MQSupport support = services.getMessageQueueSupport();
    	if (support == null) {
    		tm.ok("Message queue is not implemented");
    		return;
    	}
    	
    	PlatformResources resources = DaseinTestManager.getPlatformResources();
    	if (resources != null) {
    		String mqId = resources.provisionMQ(support, "deleteMqs", "dsnmq");
    		tm.out("provision new message queue", mqId);
    		assertNotNull("provision new message queue for test remove mq failed", mqId);
    		support.removeMessageQueue(mqId, "test remove message queue");
    		tm.out("Remove message queue", mqId);
    	} else {
    		fail("No platform resources were initialized for the test run");
    	}
    }

    @Test
    public void sendMessage() throws CloudException, InternalException {
    	
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
         
         if ( testQueueId != null ) {
        	 MQMessageIdentifier identifier = support.sendMessage(testQueueId, "queue message for stateful mq test");
        	 tm.ok("send message " + identifier.getProviderMessageId() + " to queue " + testQueueId + " successed");
        	 assertNotNull("send message to queue " + testQueueId + " failed", identifier);
         } else {
             if( !support.isSubscribed() ) {
                 tm.ok("Not subscribed to MQ support so this test is invalid");
             } else {
                 fail("No test message queue was found to support this stateless test. Please create one and run again.");
             }
         }
    }
    
    @Test
    public void receiveMessage() throws CloudException, InternalException {
    	
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
        
        if ( testQueueId != null ) {
        	MQMessageReceipt message = support.receiveMessage(testQueueId);
        	tm.ok("receive message " + message.getIdentifier().getProviderMessageId() + " from queue " + testQueueId + " successed");
        	assertNotNull("receive message from queue " + testQueueId + " failed", message.getIdentifier());
        } else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to MQ support so this test is invalid");
            } else {
                fail("No test message queue was found to support this stateless test. Please create one and run again.");
            }
        }   
    }
    
    @Test
    public void receiveMessages() throws CloudException, InternalException {
    	
		final TimePeriod<Second> waitTime = (TimePeriod<Second>) TimePeriod.valueOf(2, "second");
		final TimePeriod<Second> visibilityTimeout = (TimePeriod<Second>) TimePeriod.valueOf(120, "second");
    	
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
        
        if ( testQueueId != null) {
        	Iterator<MQMessageReceipt> messageIter = support.receiveMessages(testQueueId, waitTime, testMessageCount, visibilityTimeout).iterator();
        	int count = 0;
        	for(; messageIter.hasNext(); count++) {
        		messageIter.next();
        	}
        	assertEquals("not receive all the sent messages", count, testMessageCount);
        	
        } else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to MQ support so this test is invalid");
            } else {
                fail("No test message queue was found to support this stateless test. Please create one and run again.");
            }
        }   
    	
    }
    
}
