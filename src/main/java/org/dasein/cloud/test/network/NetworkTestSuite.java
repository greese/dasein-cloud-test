package org.dasein.cloud.test.network;

import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.cloud.StatelessAuthenticationTests;
import org.dasein.cloud.test.cloud.StatelessDCTests;
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
        StatefulVLANTests.class,
        StatelessVLANTests.class,
        StatelessFirewallTests.class
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
