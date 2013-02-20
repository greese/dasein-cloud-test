package org.dasein.cloud.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/17/13 8:10 PM</p>
 *
 * @author George Reese
 */
public abstract class AbstractStatelessTestSuite {
    @BeforeClass
    static public void setup() {
        DaseinTestManager.init();
    }

    @AfterClass
    static public void teardown() {
        DaseinTestManager.cleanUp();
    }
}
