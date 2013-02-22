package org.dasein.cloud.test.platform;

import org.dasein.cloud.test.DaseinTestManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Cameron Stokes (http://github.com/clstokes)
 * @since 2013-02-19
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({StatefulMonitoringTests.class})
public class PlatformTestSuite {
  @BeforeClass
  static public void setup() {
    DaseinTestManager.init();
  }

  @AfterClass
  static public void teardown() {
    DaseinTestManager.cleanUp();
  }
}
