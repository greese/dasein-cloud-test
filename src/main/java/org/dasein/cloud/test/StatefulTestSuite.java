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

package org.dasein.cloud.test;

import org.dasein.cloud.test.ci.StatefulCITests;
import org.dasein.cloud.test.ci.StatefulHttpLoadBalancerTests;
import org.dasein.cloud.test.ci.StatefulTopologyTests;
import org.dasein.cloud.test.compute.StatefulImageTests;
import org.dasein.cloud.test.compute.StatefulSnapshotTests;
import org.dasein.cloud.test.compute.StatefulVMTests;
import org.dasein.cloud.test.compute.StatefulVolumeTests;
import org.dasein.cloud.test.identity.StatefulIAMTests;
import org.dasein.cloud.test.identity.StatefulKeypairTests;
import org.dasein.cloud.test.network.StatefulDNSTests;
import org.dasein.cloud.test.network.StatefulFirewallTests;
import org.dasein.cloud.test.network.StatefulLoadBalancerTests;
import org.dasein.cloud.test.network.StatefulNetworkFirewallTests;
import org.dasein.cloud.test.network.StatefulStaticIPTests;
import org.dasein.cloud.test.network.StatefulVLANTests;
import org.dasein.cloud.test.network.StatefulVpnTests;
import org.dasein.cloud.test.platform.StatefulCDNTests;
import org.dasein.cloud.test.platform.StatefulMonitoringTests;
import org.dasein.cloud.test.platform.StatefulPushNotificationTests;
import org.dasein.cloud.test.platform.StatefulRDBMSTests;
import org.dasein.cloud.test.storage.StatefulObjectStoreTests;
import org.dasein.cloud.test.storage.StatefulOfflineStoreTests;
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
        StatefulOfflineStoreTests.class,
        StatefulVMTests.class,
        StatefulKeypairTests.class,
        StatefulImageTests.class,
        StatefulVolumeTests.class,
        StatefulSnapshotTests.class,
        StatefulVLANTests.class,
        StatefulMonitoringTests.class,
        StatefulFirewallTests.class,
        StatefulStaticIPTests.class,
        StatefulLoadBalancerTests.class,
        StatefulNetworkFirewallTests.class,
        StatefulRDBMSTests.class,
        StatefulIAMTests.class,
        StatefulDNSTests.class,
        StatefulCDNTests.class,
        StatefulPushNotificationTests.class,
        StatefulTopologyTests.class,
        StatefulCITests.class,
        StatefulHttpLoadBalancerTests.class,
        StatefulVpnTests.class
})
public class StatefulTestSuite {
}
