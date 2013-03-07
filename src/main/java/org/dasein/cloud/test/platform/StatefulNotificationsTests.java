package org.dasein.cloud.test.platform;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
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
}
