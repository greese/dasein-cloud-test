package org.dasein.cloud.test;

import org.dasein.cloud.test.compute.StatefulImageTests;
import org.dasein.cloud.test.compute.StatefulSnapshotTests;
import org.dasein.cloud.test.compute.StatefulVMTests;
import org.dasein.cloud.test.compute.StatefulVolumeTests;
import org.dasein.cloud.test.identity.StatefulIAMTests;
import org.dasein.cloud.test.identity.StatefulKeypairTests;
import org.dasein.cloud.test.network.StatefulDNSTests;
import org.dasein.cloud.test.network.StatefulFirewallTests;
import org.dasein.cloud.test.network.StatefulNetworkFirewallTests;
import org.dasein.cloud.test.network.StatefulStaticIPTests;
import org.dasein.cloud.test.platform.StatefulCDNTests;
import org.dasein.cloud.test.platform.StatefulMonitoringTests;
import org.dasein.cloud.test.network.StatefulVLANTests;
import org.dasein.cloud.test.platform.StatefulNotificationsTests;
import org.dasein.cloud.test.platform.StatefulRDBMSTests;
import org.dasein.cloud.test.storage.StatefulObjectStoreTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/17/13 7:58 PM</p>
 *
 * @author George Reese
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        StatefulObjectStoreTests.class,
        StatefulVMTests.class,
        StatefulKeypairTests.class,
        StatefulImageTests.class,
        StatefulVolumeTests.class,
        StatefulSnapshotTests.class,
        StatefulVLANTests.class,
        StatefulMonitoringTests.class,
        StatefulFirewallTests.class,
        StatefulStaticIPTests.class,
        StatefulNetworkFirewallTests.class,
        StatefulRDBMSTests.class,
        StatefulIAMTests.class,
        StatefulDNSTests.class,
        StatefulCDNTests.class,
        StatefulNotificationsTests.class
})
public class StatefulTestSuite {
}
