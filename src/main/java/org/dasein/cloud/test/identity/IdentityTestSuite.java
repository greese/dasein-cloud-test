package org.dasein.cloud.test.identity;

import org.dasein.cloud.test.DaseinTestManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/17/13 8:13 PM</p>
 *
 * @author George Reese
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        StatefulKeypairTests.class,
        StatefulIAMTests.class,
        StatelessKeypairTests.class,
        StatelessIAMTests.class
})
public class IdentityTestSuite {
    @BeforeClass
    static public void setup() {
        DaseinTestManager.init();
    }

    @AfterClass
    static public void teardown() {
        DaseinTestManager.cleanUp();
    }
}
