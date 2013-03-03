package org.dasein.cloud.test.storage;

import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.platform.StatefulMonitoringTests;
import org.dasein.cloud.test.platform.StatefulRDBMSTests;
import org.dasein.cloud.test.platform.StatelessRDBMSTests;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite for executing only storage-based test cases.
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        StatefulObjectStoreTests.class,
        StatelessObjectStoreTests.class
})
public class StorageTestSuite {
  @BeforeClass
  static public void setup() {
    DaseinTestManager.init();
  }

  @AfterClass
  static public void teardown() {
    DaseinTestManager.cleanUp();
  }
}
