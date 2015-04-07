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

import org.dasein.cloud.test.cloud.StatelessAuthenticationTests;
import org.dasein.cloud.test.cloud.StatelessDCTests;
import org.dasein.cloud.test.compute.StatelessAffinityGroupTests;
import org.dasein.cloud.test.compute.StatelessImageTests;
import org.dasein.cloud.test.compute.StatelessSnapshotTests;
import org.dasein.cloud.test.compute.StatelessVMMonitoringTests;
import org.dasein.cloud.test.compute.StatelessVMTests;
import org.dasein.cloud.test.compute.StatelessVolumeTests;
import org.dasein.cloud.test.ci.StatelessHttpLoadBalancerTests;
import org.dasein.cloud.test.ci.StatelessTopologyTests;
import org.dasein.cloud.test.identity.StatelessIAMTests;
import org.dasein.cloud.test.identity.StatelessKeypairTests;
import org.dasein.cloud.test.network.StatelessDNSTests;
import org.dasein.cloud.test.network.StatelessFirewallTests;
import org.dasein.cloud.test.network.StatelessLoadBalancerTests;
import org.dasein.cloud.test.network.StatelessNetworkFirewallTests;
import org.dasein.cloud.test.network.StatelessStaticIPTests;
import org.dasein.cloud.test.network.StatelessVLANTests;
import org.dasein.cloud.test.platform.StatelessCDNTests;
import org.dasein.cloud.test.platform.StatelessMQTests;
import org.dasein.cloud.test.platform.StatelessMonitoringTests;
import org.dasein.cloud.test.platform.StatelessNotificationsTests;
import org.dasein.cloud.test.platform.StatelessRDBMSTests;
import org.dasein.cloud.test.storage.StatelessObjectStoreTests;
import org.dasein.cloud.test.storage.StatelessOfflineStoreTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/17/13 8:12 PM</p>
 *
 * @author George Reese
 * @version 2013.07 added MQ support (issue #6)
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        StatelessObjectStoreTests.class,
        StatelessOfflineStoreTests.class,
        StatelessAuthenticationTests.class,
        StatelessDCTests.class,
        StatelessAffinityGroupTests.class,
        StatelessVMTests.class,
        StatelessKeypairTests.class,
        StatelessImageTests.class,
        StatelessVolumeTests.class,
        StatelessSnapshotTests.class,
        StatelessVLANTests.class,
        StatelessFirewallTests.class,
        StatelessStaticIPTests.class,
        StatelessLoadBalancerTests.class,
        StatelessNetworkFirewallTests.class,
        StatelessRDBMSTests.class,
        StatelessIAMTests.class,
        StatelessDNSTests.class,
        StatelessCDNTests.class,
        StatelessMQTests.class,
        StatelessMonitoringTests.class,
        StatelessNotificationsTests.class,
        StatelessVMMonitoringTests.class,
        StatelessTopologyTests.class,
        StatelessHttpLoadBalancerTests.class
})
public class StatelessTestSuite {
}
