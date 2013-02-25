package org.dasein.cloud.test;

import org.dasein.cloud.test.compute.StatefulImageTests;
import org.dasein.cloud.test.compute.StatefulVMTests;
import org.dasein.cloud.test.compute.StatefulVolumeTests;
import org.dasein.cloud.test.identity.StatefulKeypairTests;
import org.dasein.cloud.test.network.StatefulFirewallTests;
import org.dasein.cloud.test.network.StatefulStaticIPTests;
import org.dasein.cloud.test.platform.StatefulMonitoringTests;
import org.dasein.cloud.test.network.StatefulVLANTests;
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
        StatefulVMTests.class,
        StatefulKeypairTests.class,
        StatefulImageTests.class,
        StatefulVolumeTests.class,
        StatefulVLANTests.class,
        StatefulMonitoringTests.class,
        StatefulFirewallTests.class,
        StatefulStaticIPTests.class
})
public class StatefulTestSuite {
}
