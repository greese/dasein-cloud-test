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

package org.dasein.cloud.test.ci;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ci.CIServices;
import org.dasein.cloud.ci.Topology;
import org.dasein.cloud.ci.TopologySupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Tests support for Dasein Cloud topologies which represent complex, multi-resource templates that can be provisioned into
 * running resources.
 * <p>Created by George Reese: 5/31/13 10:57 AM</p>
 * @author George Reese
 * @version 2013.07 initial version
 * @since 2013.07
 */
public class StatelessTopologyTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessTopologyTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testTopologyId;

    public StatelessTopologyTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testTopologyId = tm.getTestTopologyId(DaseinTestManager.STATELESS, false);
    }

    @After
    public void after() {
        tm.end();
    }

    private void assertTopology(@Nonnull Topology topology) {
        assertNotNull("Topology ID may not be null", topology.getProviderTopologyId());
        assertNotNull("Topology name may not be null", topology.getName());
        assertNotNull("Topology description may not be null", topology.getDescription());
        assertNotNull("Topology state may not be null", topology.getCurrentState());
        assertTrue("Topology creation timestamp may not be negative", topology.getCreationTimestamp() >= 0L);
        assertNotNull("Topology tags may not be null", topology.getTags());
        assertNotNull("Owner ID may not be null", topology.getProviderOwnerId());
        //assertNotNull("Region ID may not be null", topology.getProviderRegionId());
        Iterable<Topology.VMDevice> vms = topology.getVirtualMachines();

        assertNotNull("VM list may not be null", vms);
        for( Topology.VMDevice vm : vms ) {
            assertNotNull("VM device ID may not be null", vm.getDeviceId());
            assertNotNull("VM name may not be null", vm.getName());
            assertTrue("VM CPU count must be non-negative and non-zero", vm.getCpuCount() > 0);
            assertNotNull("VM memory must not be null", vm.getMemory());
            assertNotNull("VM architecture must not be null", vm.getArchitecture());
            assertNotNull("VM platform must not be null", vm.getPlatform());
            assertTrue("VM capacity must be non-negative and non-zero", vm.getCapacity() > 0);
        }
        Iterable<Topology.VLANDevice> vlans = topology.getVLANs();

        assertNotNull("VLAN list may not be null", vlans);
        for( Topology.VLANDevice vlan : vlans ) {
            assertNotNull("VLAN device ID may not be null", vlan.getDeviceId());
            assertNotNull("VLAN name may not be null", vlan.getName());
        }
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if( services != null ) {
             TopologySupport support = services.getTopologySupport();

            if( support != null ) {
                tm.out("Subscribed", support.isSubscribed());
                tm.out("Public Library", support.supportsPublicLibrary());
            }
            else {
                tm.ok(tm.getProvider().getCloudName() + " does not support topologies");
            }
        }
        else {
            tm.ok(tm.getProvider().getCloudName() + " does not support compute services");
        }
    }

    @Test
    public void getBogusTopology() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if( services != null ) {
            TopologySupport support = services.getTopologySupport();

            if( support != null ) {
                Topology t = support.getTopology(UUID.randomUUID().toString());

                tm.out("Bogus Topology", t);
                assertNull("Bogus topology was supposed to be none, but got a valid topology.", t);
            }
            else {
                tm.ok("No topology support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void getTopology() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if( services != null ) {
            TopologySupport support = services.getTopologySupport();

            if( support != null ) {
                if( testTopologyId != null ) {
                    Topology t = support.getTopology(testTopologyId);

                    tm.out("Topology", t);
                    assertNotNull("Failed to find the test topology among possible images", t);
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.warn("No topology ID was identified, so this test is not valid");
                    }
                    else {
                        fail("No test topology exists for the getTopology test");
                    }
                }
            }
            else {
                tm.ok("No topology support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void topologyContent() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if( services != null ) {
            TopologySupport support = services.getTopologySupport();

            if( support != null ) {
                if( testTopologyId != null ) {
                    Topology t = support.getTopology(testTopologyId);

                    assertNotNull("Failed to find the test topology among possible topologies", t);
                    tm.out("Topology ID", t.getProviderTopologyId());
                    tm.out("Current State", t.getCurrentState());
                    tm.out("Name", t.getName());
                    tm.out("Created", new Date(t.getCreationTimestamp()));
                    tm.out("Owner Account", t.getProviderOwnerId());
                    tm.out("Region ID", t.getProviderRegionId());
                    tm.out("Data Center ID", t.getProviderDataCenterId());

                    Iterable<Topology.VMDevice> vms = t.getVirtualMachines();

                    //noinspection ConstantConditions
                    if( vms != null ) {
                        int i = 1;

                        for( Topology.VMDevice vm : vms ) {
                            tm.out("VM " + i + " Device ID", vm.getDeviceId());
                            tm.out("VM " + i + " Name", vm.getName());
                            tm.out("VM " + i + " Capacity", vm.getCapacity());
                            tm.out("VM " + i + " Architecture", vm.getArchitecture());
                            tm.out("VM " + i + " Platform", vm.getPlatform());
                            tm.out("VM " + i + " CPU Count", vm.getCpuCount());
                            tm.out("VM " + i + " Memory", vm.getMemory());
                            i++;
                        }
                    }
                    Iterable<Topology.VLANDevice> vlans = t.getVLANs();

                    //noinspection ConstantConditions
                    if( vlans != null ) {
                        int i = 1;

                        for( Topology.VLANDevice vlan : vlans ) {
                            tm.out("VLAN " + i + " Device ID", vlan.getDeviceId());
                            tm.out("VLAN " + i + " Name", vlan.getName());
                            i++;
                        }
                    }

                    Map<String,String> tags = t.getTags();

                    //noinspection ConstantConditions
                    if( tags != null ) {
                        for( Map.Entry<String,String> entry : tags.entrySet() ) {
                            tm.out("Tag " + entry.getKey(), entry.getValue());
                        }
                    }

                    tm.out("Description", t.getDescription());

                    assertTopology(t);

                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.warn("No topology ID was identified, so this test is not valid");
                    }
                    else {
                        fail("No test topology exists for the topologyContent test");
                    }
                }
            }
            else {
                tm.ok("No topology support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void listPrivateTopologies() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if( services != null ) {
            TopologySupport support = services.getTopologySupport();

            if( support != null ) {
                Iterable<Topology> topologies = support.listTopologies(null);
                int count = 0;

                assertNotNull("listTopologies() must return a non-null list of topologies even if a private library is not supported", topologies);
                for( Topology t : topologies ) {
                    count++;
                    tm.out("Topology", t);
                }
                tm.out("Total Topology Count", count);
                for( Topology t : topologies ) {
                    assertTopology(t);
                }
                if( count < 1 ) {
                    tm.warn("No topologies were provided so this test may not be valid");
                }
            }
            else {
                tm.ok("No topology support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }
}
