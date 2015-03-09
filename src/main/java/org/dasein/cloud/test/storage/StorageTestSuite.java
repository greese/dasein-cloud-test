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

package org.dasein.cloud.test.storage;

import org.dasein.cloud.test.DaseinTestManager;
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
        StatefulOfflineStoreTests.class,
        StatelessObjectStoreTests.class,
        StatelessOfflineStoreTests.class
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
