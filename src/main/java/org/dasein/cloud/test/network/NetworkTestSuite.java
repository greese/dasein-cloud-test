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

package org.dasein.cloud.test.network;

import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.ci.StatefulCITests;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Basic test suite for networkoperations.
 * <p>Created by George Reese: 2/18/13 6:40 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        StatefulLoadBalancerTests.class,
        StatefulVLANTests.class,
        StatefulFirewallTests.class,
        StatefulStaticIPTests.class,
        StatefulNetworkFirewallTests.class,
        StatefulDNSTests.class,
        StatelessLoadBalancerTests.class,
        StatelessVLANTests.class,
        StatelessFirewallTests.class,
        StatelessStaticIPTests.class,
        StatelessNetworkFirewallTests.class,
        StatelessDNSTests.class,
        StatefulCITests.class
})
public class NetworkTestSuite {
    @BeforeClass
    static public void setup() {
        DaseinTestManager.init();
    }

    @AfterClass
    static public void teardown() {
        DaseinTestManager.cleanUp();
    }
}
