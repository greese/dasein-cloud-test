/**
 * Copyright (C) 2009-2014 Dell, Inc.
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

package org.dasein.cloud.test.ci;

import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.compute.StatefulImageTests;
import org.dasein.cloud.test.compute.StatefulSnapshotTests;
import org.dasein.cloud.test.compute.StatefulVMTests;
import org.dasein.cloud.test.compute.StatefulVolumeTests;
import org.dasein.cloud.test.compute.StatelessImageTests;
import org.dasein.cloud.test.compute.StatelessSnapshotTests;
import org.dasein.cloud.test.compute.StatelessVMMonitoringTests;
import org.dasein.cloud.test.compute.StatelessVMTests;
import org.dasein.cloud.test.compute.StatelessVolumeTests;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * [Class Documentation]
 * @author George Reese
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        StatelessTopologyTests.class,
        StatefulTopologyTests.class,
        StatefulReplicapoolTests.class
})
public class CITestSuite {
    @BeforeClass
    static public void setup() {
        DaseinTestManager.init();
    }

    @AfterClass
    static public void teardown() {
        DaseinTestManager.cleanUp();
    }
}
