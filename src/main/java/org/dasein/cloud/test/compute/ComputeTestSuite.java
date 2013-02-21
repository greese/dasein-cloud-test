package org.dasein.cloud.test.compute;

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
        StatefulVMTests.class,
        StatelessVMTests.class,
        StatefulImageTests.class,
        StatelessImageTests.class,
        StatelessVolumeTests.class
})
public class ComputeTestSuite {
    @BeforeClass
    static public void setup() {
        DaseinTestManager.init();
    }

    @AfterClass
    static public void teardown() {
        DaseinTestManager.cleanUp();
    }
}