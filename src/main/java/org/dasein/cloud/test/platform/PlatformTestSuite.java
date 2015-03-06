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

package org.dasein.cloud.test.platform;

import org.dasein.cloud.test.DaseinTestManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Cameron Stokes (http://github.com/clstokes)
 * @version 2013.07 added MQ support (issue #6)
 * @since 2013.02
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        StatefulPushNotificationTests.class,
        StatefulMonitoringTests.class,
        StatefulRDBMSTests.class,
        StatefulCDNTests.class,
        StatefulNotificationsTests.class,
        StatelessMonitoringTests.class,
        StatelessRDBMSTests.class,
        StatelessCDNTests.class,
        StatelessNotificationsTests.class,
        StatelessMQTests.class
})

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
