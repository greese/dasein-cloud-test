package org.dasein.cloud.test;

import org.dasein.cloud.test.cloud.StatelessAuthenticationTests;
import org.dasein.cloud.test.cloud.StatelessDCTests;
import org.dasein.cloud.test.compute.StatelessImageTests;
import org.dasein.cloud.test.compute.StatelessSnapshotTests;
import org.dasein.cloud.test.compute.StatelessVMTests;
import org.dasein.cloud.test.compute.StatelessVolumeTests;
import org.dasein.cloud.test.identity.StatelessIAMTests;
import org.dasein.cloud.test.identity.StatelessKeypairTests;
import org.dasein.cloud.test.network.StatelessDNSTests;
import org.dasein.cloud.test.network.StatelessFirewallTests;
import org.dasein.cloud.test.network.StatelessNetworkFirewallTests;
import org.dasein.cloud.test.network.StatelessStaticIPTests;
import org.dasein.cloud.test.network.StatelessVLANTests;
import org.dasein.cloud.test.platform.StatelessCDNTests;
import org.dasein.cloud.test.platform.StatelessMonitoringTests;
import org.dasein.cloud.test.platform.StatelessRDBMSTests;
import org.dasein.cloud.test.storage.StatelessObjectStoreTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/17/13 8:12 PM</p>
 *
 * @author George Reese
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        StatelessObjectStoreTests.class,
        StatelessAuthenticationTests.class,
        StatelessDCTests.class,
        StatelessVMTests.class,
        StatelessKeypairTests.class,
        StatelessImageTests.class,
        StatelessVolumeTests.class,
        StatelessSnapshotTests.class,
        StatelessVLANTests.class,
        StatelessFirewallTests.class,
        StatelessStaticIPTests.class,
        StatelessNetworkFirewallTests.class,
        StatelessRDBMSTests.class,
        StatelessIAMTests.class,
        StatelessDNSTests.class,
        StatelessCDNTests.class,
        StatelessMonitoringTests.class
})
public class StatelessTestSuite {
}
