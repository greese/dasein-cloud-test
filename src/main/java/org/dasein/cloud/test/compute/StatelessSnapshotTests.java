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
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.Snapshot;
import org.dasein.cloud.compute.SnapshotSupport;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests stateless aspects of volumes snapshots as they should appear to consumers of the Dasein Cloud API.
 * <p>Created by George Reese: 2/26/13 9:09 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatelessSnapshotTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessSnapshotTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testSnapshotId;

    public StatelessSnapshotTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testSnapshotId = tm.getTestSnapshotId(DaseinTestManager.STATELESS, false);
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services == null ) {
            tm.ok("Compute services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        SnapshotSupport support = services.getSnapshotSupport();

        if( support == null ) {
            tm.ok("Snapshots are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        tm.out("Subscribed", support.isSubscribed());
        tm.out("Term for Snapshot", support.getCapabilities().getProviderTermForSnapshot(Locale.getDefault()));
        tm.out("Volume Attached During Snapshot", support.getCapabilities().identifyAttachmentRequirement());
        tm.out("Snapshot Creation", support.getCapabilities().supportsSnapshotCreation());
        tm.out("Snapshot Copying", support.getCapabilities().supportsSnapshotCopying());
        tm.out("Snapshot Sharing", support.getCapabilities().supportsSnapshotSharing());
        tm.out("    -> with Public?", support.getCapabilities().supportsSnapshotSharingWithPublic());
        assertNotNull("The provider term for a snapshot may not be null", support.getCapabilities().getProviderTermForSnapshot(Locale.getDefault()));
    }

    @Test
    public void getBogusSnapshot() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services == null ) {
            tm.ok("Compute services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        SnapshotSupport support = services.getSnapshotSupport();

        if( support == null ) {
            tm.ok("Snapshots are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Snapshot s = support.getSnapshot(UUID.randomUUID().toString());

        tm.out("Bogus Snapshot", s);
        assertNull("Found a valid snapshot under the random UUID generated for this test", s);
    }

    @Test
    public void getSnapshot() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services == null ) {
            tm.ok("Compute services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        SnapshotSupport support = services.getSnapshotSupport();

        if( support == null ) {
            tm.ok("Snapshots are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testSnapshotId == null ) {
            if( !support.isSubscribed() ) {
                tm.ok("Snapshot support is not subscribed, so this functionality cannot be tested");
            }
            else {
                fail("No test snapshot is in place to execute this test");
            }
            return;
        }
        Snapshot s = support.getSnapshot(testSnapshotId);

        tm.out("Snapshot", s);
        assertNotNull("The test snapshot was not found in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName(), s);
    }

    private void assertSnapshot(@Nonnull Snapshot snapshot) {
        assertNotNull("Snapshot ID may not be null", snapshot.getProviderSnapshotId());
        assertNotNull("The current state of the snapshot must not be null", snapshot.getCurrentState());
        assertNotNull("The snapshot name may not be null", snapshot.getName());
        assertNotNull("The snapshot description may not be null", snapshot.getDescription());
        if( snapshot.getSnapshotTimestamp() < 2L ) {
            tm.warn("Meaningless creation timestamp with snapshot; does the cloud provider support this information?");
        }
        assertNotNull("Snapshot owner may not be null", snapshot.getOwner());
        assertNotNull("Snapshot region may not be null", snapshot.getRegionId());
        assertTrue("Snapshot size must be greater than 0", snapshot.getSizeInGb() > 0);
        assertNotNull("Snapshot tags may not be null", snapshot.getTags());
    }

    @Test
    public void snapshotContent() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services == null ) {
            tm.ok("Compute services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        SnapshotSupport support = services.getSnapshotSupport();

        if( support == null ) {
            tm.ok("Snapshots are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testSnapshotId == null ) {
            if( !support.isSubscribed() ) {
                tm.ok("Snapshot support is not subscribed, so this functionality cannot be tested");
            }
            else {
                fail("No test snapshot is in place to execute this test");
            }
            return;
        }
        Snapshot s = support.getSnapshot(testSnapshotId);

        assertNotNull("The test snapshot was not found in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName(), s);

        Iterable<String> shares = support.listShares(s.getProviderSnapshotId());

        tm.out("Snapshot ID", s.getProviderSnapshotId());
        tm.out("Current State", s.getCurrentState());
        tm.out("Name", s.getName());
        tm.out("Created", new Date(s.getSnapshotTimestamp()));
        tm.out("Owner Account", s.getOwner());
        tm.out("Region ID", s.getRegionId());
        tm.out("Volume ID", s.getVolumeId());
        tm.out("Size (GB)", s.getSizeInGb());
        tm.out("Progress", s.getProgress());
        tm.out("Shares", shares);
        Map<String,String> tags = s.getTags();

        //noinspection ConstantConditions
        if( tags != null ) {
            for( Map.Entry<String,String> entry : tags.entrySet() ) {
                tm.out("Tag " + entry.getKey(), entry.getValue());
            }
        }
        tm.out("Description", s.getDescription());

        assertSnapshot(s);
    }

    @Test
    public void listSnapshots() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services == null ) {
            tm.ok("Compute services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        SnapshotSupport support = services.getSnapshotSupport();

        if( support == null ) {
            tm.ok("Snapshots are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Iterable<Snapshot> snapshots = support.listSnapshots();
        int count = 0;

        assertNotNull("Snapshot list may be empty, but it cannot be null");
        for( Snapshot snapshot : snapshots ) {
            count++;
            tm.out("Snapshot", snapshot);
        }
        tm.out("Total Snapshot Count", count);

        if( !support.isSubscribed() ) {
            assertEquals("The account is not subscribed for snapshots, but the snapshot count was non-zero", 0, count);
        }
        else if( count == 0 ) {
            tm.warn("No snapshots were found in the cloud so this test may not be valid");
        }
        else {
            for( Snapshot snapshot : snapshots ) {
                assertSnapshot(snapshot);
            }
        }
    }

    @Test
    public void listSnapshotStatus() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services == null ) {
            tm.ok("Compute services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        SnapshotSupport support = services.getSnapshotSupport();

        if( support == null ) {
            tm.ok("Snapshots are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Iterable<ResourceStatus> snapshots = support.listSnapshotStatus();
        int count = 0;

        assertNotNull("Snapshot status list may be empty, but it cannot be null");
        for( ResourceStatus snapshot : snapshots ) {
            count++;
            tm.out("Snapshot Status", snapshot);
        }
        tm.out("Total Snapshot Status Count", count);

        if( !support.isSubscribed() ) {
            assertEquals("The account is not subscribed for snapshots, but the snapshot status count was non-zero", 0, count);
        }
        else if( count == 0 ) {
            tm.warn("No snapshot status instances were found in the cloud so this test may not be valid");
        }
    }

    @Test
    public void compareVSnapshotListAndStatus() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services == null ) {
            tm.ok("Compute services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        SnapshotSupport support = services.getSnapshotSupport();

        if( support == null ) {
            tm.ok("Snapshots are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
        Iterable<Snapshot> snapshots = support.listSnapshots();
        Iterable<ResourceStatus> status = support.listSnapshotStatus();

        assertNotNull("listSnapshots() must return at least an empty collections and may not be null", snapshots);
        assertNotNull("listSnapshotStatus() must return at least an empty collection and may not be null", status);
        for( ResourceStatus s : status ) {
            Map<String,Boolean> current = map.get(s.getProviderResourceId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(s.getProviderResourceId(), current);
            }
            current.put("status", true);
        }
        for( Snapshot snapshot : snapshots ) {
            Map<String,Boolean> current = map.get(snapshot.getProviderSnapshotId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(snapshot.getProviderSnapshotId(), current);
            }
            current.put("snapshot", true);
        }
        for( Map.Entry<String,Map<String,Boolean>> entry : map.entrySet() ) {
            Boolean s = entry.getValue().get("status");
            Boolean v = entry.getValue().get("snapshot");

            assertTrue("Status and snapshot lists do not match for " + entry.getKey(), s != null && v != null && s && v);
        }
        tm.out("Matches");
    }
}
