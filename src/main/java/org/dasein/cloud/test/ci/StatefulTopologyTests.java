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
import org.dasein.cloud.ci.TopologyProvisionOptions;
import org.dasein.cloud.ci.TopologyProvisionOptions.AccessConfig;
import org.dasein.cloud.ci.TopologyProvisionOptions.DiskType;
import org.dasein.cloud.ci.TopologyProvisionOptions.MaintenanceOption;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
public class StatefulTopologyTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulTopologyTests.class);
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

    public StatefulTopologyTests() { }

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

    /*
     * set and get metadata in a template
     */
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

    /*
     * create new topology and verify it.
     */
    @Test
    public void createTopology() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if( services != null ) {
            TopologySupport support = services.getTopologySupport();

            if( support != null ) {
                TopologyProvisionOptions withTopologyOptions = TopologyProvisionOptions.getInstance("instance-template-2", "description", "f1-micro", true);

                List<String> tags = new ArrayList<String>();
                tags.add("http-server");
                tags.add("https-server");
                tags.add("generic-tag");

                List<String> sshKeys = new ArrayList<String>();
                sshKeys.add("roger.unwin:ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCvM53nrgEnFrMe6V53Th1GRZrPUxmoWSD+OMXlvluaEFn58yQANUqOAZK20inpbiw2xDwwHscR0ijGXPmCl7PC6vUIYuNixzSckpuKAc+Ml2qjodw0FAq4jy0jW/S9Vgu4E+0zoIucLbPhkfT6aoV7Qg1N1801FUh3MU5XHPOP5kVlBhXISondnSGHJccJGHVmr8VeJZPNLBIz4ZUOo4pmg5gX7uzNVFmj8UJ3nP2x+Qfmn7a0C9+aseLIwynVj/oJV1S8BAK/46RUjlfjlsgsCwduaulFjIQRc6dMb7knD4DkCTUwH5KDl+tblm2yjcGr0w6uXiG0qc+kxBi4sRqR roger.unwin@enstratius.com");
                withTopologyOptions = withTopologyOptions.withSshKeys(sshKeys.toArray(new String[sshKeys.size()]));

                Map<String, String> metadata = new HashMap<String, String>();
                metadata.put("roger", "here");
                withTopologyOptions = withTopologyOptions.withMetadata(metadata);

                withTopologyOptions = withTopologyOptions.withAutomaticRestart(false);
                withTopologyOptions = withTopologyOptions.withMaintenanceOption(MaintenanceOption.TERMINATE_VM_INSTANCE);

                withTopologyOptions = withTopologyOptions.withTags(tags);
                withTopologyOptions = withTopologyOptions.withNetworkInterface("name", "https://www.googleapis.com/compute/v1/projects/qa-project-2/global/networks/default", true); // ,accessConfigs);
                withTopologyOptions = withTopologyOptions.withAttachedDisk("instance-template-2", DiskType.STANDARD_PERSISTENT_DISK, "https://www.googleapis.com/compute/v1/projects/debian-cloud/global/images/debian-7-wheezy-v20150127", true, true);
                boolean result = support.createTopology(withTopologyOptions);
            } else {
                tm.ok("No topology support in this cloud");
            }
        } else {
            tm.ok("No compute services in this cloud");
        }
    }

    /*
     * delete a topology
     */
    @Test
    public void deletePrivateTopologies() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if( services != null ) {
            TopologySupport support = services.getTopologySupport();

            if( support != null ) {

                TopologyProvisionOptions withTopologyOptions = TopologyProvisionOptions.getInstance("instance-template-1", "description", "f1-micro", true);
                withTopologyOptions = withTopologyOptions.withNetworkInterface("name", "https://www.googleapis.com/compute/v1/projects/qa-project-2/global/networks/default", false);
                withTopologyOptions = withTopologyOptions.withAttachedDisk("instance-template-2", DiskType.SSD_PERSISTENT_DISK, "https://www.googleapis.com/compute/v1/projects/debian-cloud/global/images/debian-7-wheezy-v20150127", true, true);

                List<String> sshKeys = new ArrayList<String>();
                sshKeys.add("roger.unwin:ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCvM53nrgEnFrMe6V53Th1GRZrPUxmoWSD+OMXlvluaEFn58yQANUqOAZK20inpbiw2xDwwHscR0ijGXPmCl7PC6vUIYuNixzSckpuKAc+Ml2qjodw0FAq4jy0jW/S9Vgu4E+0zoIucLbPhkfT6aoV7Qg1N1801FUh3MU5XHPOP5kVlBhXISondnSGHJccJGHVmr8VeJZPNLBIz4ZUOo4pmg5gX7uzNVFmj8UJ3nP2x+Qfmn7a0C9+aseLIwynVj/oJV1S8BAK/46RUjlfjlsgsCwduaulFjIQRc6dMb7knD4DkCTUwH5KDl+tblm2yjcGr0w6uXiG0qc+kxBi4sRqR roger.unwin@enstratius.com");
                withTopologyOptions = withTopologyOptions.withSshKeys(sshKeys.toArray(new String[sshKeys.size()]));

                Map<String, String> metadata = new HashMap<String, String>();
                metadata.put("roger", "here");
                withTopologyOptions = withTopologyOptions.withMetadata(metadata);

                withTopologyOptions = withTopologyOptions.withAutomaticRestart(true);
                withTopologyOptions = withTopologyOptions.withMaintenanceOption(MaintenanceOption.MIGRATE_VM_INSTANCE);

                List<String> tags = new ArrayList<String>();
                tags.add("http-server");
                tags.add("generic-tag");
                withTopologyOptions = withTopologyOptions.withTags(tags);

                boolean result1 = support.createTopology(withTopologyOptions);

                List<String> topologyIds = new ArrayList<String>();
                topologyIds.add("instance-template-1");
                topologyIds.add("instance-template-2");
                boolean result2 = support.removeTopologies(topologyIds.toArray(new String[topologyIds.size()]));
            } else {
                tm.ok("No topology support in this cloud");
            }
        } else {
            tm.ok("No compute services in this cloud");
        }
    }
}
