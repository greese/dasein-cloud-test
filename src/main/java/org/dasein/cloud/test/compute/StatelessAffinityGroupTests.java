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

package org.dasein.cloud.test.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AffinityGroup;
import org.dasein.cloud.compute.AffinityGroupFilterOptions;
import org.dasein.cloud.compute.AffinityGroupSupport;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.*;
import org.junit.rules.TestName;

import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Test cases to validate an implementation of Affinty Group support
 * User: daniellemayne
 * Date: 23/07/2014
 * Time: 09:23
 */
public class StatelessAffinityGroupTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessAffinityGroupTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if (tm != null) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testAffinityGroupId;
    private String testDataCenterId;

    public StatelessAffinityGroupTests() {
    }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        try {
            testDataCenterId = DaseinTestManager.getDefaultDataCenterId(true);

            if (testDataCenterId != null) {
                ComputeServices computeServices = tm.getProvider().getComputeServices();
                if (computeServices.hasAffinityGroupSupport()) {
                    AffinityGroupSupport support = computeServices.getAffinityGroupSupport();
                    AffinityGroupFilterOptions options = AffinityGroupFilterOptions.getInstance().withDataCenterId(testDataCenterId);

                    for (AffinityGroup ag : support.list(options)) {
                        if (testAffinityGroupId == null) {
                            testAffinityGroupId = ag.getAffinityGroupId();
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
            // ignore
        }
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void getBogusAffinityGroup() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices computeServices = tm.getProvider().getComputeServices();
        if (computeServices.hasAffinityGroupSupport()) {
            AffinityGroupSupport services = computeServices.getAffinityGroupSupport();
            AffinityGroup ag = services.get(UUID.randomUUID().toString());

            tm.out("Bogus Affinity Group", ag);
            assertNull("Dummy affinity group must be null, but one was found", ag);

        } else {
            tm.ok("Affinity groups not supported in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void getAffinityGroup() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());

        ComputeServices computeServices = tm.getProvider().getComputeServices();
        if (computeServices.hasAffinityGroupSupport()) {
            if (testAffinityGroupId != null) {
                AffinityGroupSupport services = computeServices.getAffinityGroupSupport();
                AffinityGroup ag = services.get(testAffinityGroupId);

                tm.out("Affinity Group", ag);
                assertNotNull("Failed to find the test affinity group", ag);
            } else {
                fail("No testAffinityGroup found for getAffinityGroup test");
            }
        } else {
            tm.ok("Affinity groups not supported in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void affinityGroupContent() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());

        ComputeServices computeServices = tm.getProvider().getComputeServices();
        if (computeServices.hasAffinityGroupSupport()) {
            if (testAffinityGroupId != null) {
                AffinityGroupSupport services = computeServices.getAffinityGroupSupport();
                AffinityGroup ag = services.get(testAffinityGroupId);

                assertNotNull("Failed to find the test affinity group", ag);
                tm.out("Affinity Group ID", ag.getAffinityGroupId());
                tm.out("Name", ag.getAffinityGroupName());
                tm.out("Description", ag.getDescription());
                tm.out("Datacenter ID", ag.getDataCenterId());
                tm.out("Created", ag.getCreationTimestamp());

                Map<String, String> tags = ag.getTags();
                assertNotNull("Tags may not be null", ag.getTags());

                for (Map.Entry<String, String> entry : tags.entrySet()) {
                    tm.out("Tag " + entry.getKey(), entry.getValue());
                }
                assertNotNull("Affinity Group ID must not be null", ag.getAffinityGroupId());
                assertNotNull("Affinity group name must not be null", ag.getAffinityGroupName());
                assertNotNull("Datacenter id must not be null", ag.getDataCenterId());
            } else {
                fail("No testAffinityGroup found for affinityGroupContent test");
            }
        } else {
            tm.ok("Affinity groups not supported in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void listAffinityGroups() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());

        ComputeServices computeServices = tm.getProvider().getComputeServices();
        if (computeServices.hasAffinityGroupSupport()) {
            if (testDataCenterId != null) {
                if (testAffinityGroupId != null) {
                    AffinityGroupSupport services = computeServices.getAffinityGroupSupport();
                    AffinityGroupFilterOptions options = AffinityGroupFilterOptions.getInstance().withDataCenterId(testDataCenterId);
                    Iterable<AffinityGroup> affinityGroups = services.list(options);
                    boolean found = false;
                    int count = 0;

                    assertNotNull("Null set of affinity groups returned from listAffinityGroups()", affinityGroups);
                    for (AffinityGroup affinityGroup : affinityGroups) {
                        count++;
                        tm.out("Affinity group", affinityGroup);
                        if (affinityGroup.getAffinityGroupId().equals(testAffinityGroupId)) {
                            found = true;
                        }
                    }
                    tm.out("Total Affinity Group Count", count);
                    assertTrue("Did not find the test affinity group ID among returned affinity groups", found);
                } else {
                    fail("No testAffinityGroup found for affinityGroupContent test");
                }
            } else {
                fail("Unable to find datacenter id for listAffinityGroups test");
            }
        } else {
            tm.ok("Affinity groups not supported in " + tm.getProvider().getCloudName());
        }
    }
}
