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
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.junit.*;
import org.junit.rules.TestName;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests of stateful functions in Dasein Cloud snapshot management support.
 * <p>Created by George Reese: 2/27/13 8:25 AM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatefulSnapshotTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulSnapshotTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String provisionedSnapshotId;

    private String testShareAccount;
    private String testSnapshotId;
    private String testSourceRegion;
    private String testVolumeId;
	private String testDataCenterId;

    static private int postfix = 1;

    public StatefulSnapshotTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        testDataCenterId = DaseinTestManager.getDefaultDataCenterId(false);

        ComputeServices services = tm.getProvider().getComputeServices();
        SnapshotSupport support = null;

        if( services != null ) {
            support = services.getSnapshotSupport();
        }

        if( name.getMethodName().equals("createSnapshot") ) {
            testVolumeId = tm.getTestVolumeId(DaseinTestManager.STATEFUL, true, null, testDataCenterId);
            if( testVolumeId != null ) {
                if( support != null ) {
                    try {
                        if( support.getCapabilities().identifyAttachmentRequirement().equals(Requirement.REQUIRED) ) {
                            String vmId = tm.getTestVMId(DaseinTestManager.STATEFUL + (postfix++), VmState.RUNNING, true, testDataCenterId);

                            if( vmId != null ) {
                                @SuppressWarnings("ConstantConditions") VirtualMachine vm = services.getVirtualMachineSupport().getVirtualMachine(vmId);

                                if( vm != null ) {
                                    VolumeSupport volumeSupport = services.getVolumeSupport();

                                    if( volumeSupport != null ) {
                                        for( String deviceId : volumeSupport.getCapabilities().listPossibleDeviceIds(vm.getPlatform()) ) {
                                            try {
                                                volumeSupport.attach(testVolumeId, vmId, deviceId);
                                                break;
                                            }
                                            catch( Throwable ignore ) {
                                                // ignore
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        else if( name.getMethodName().equals("copySnapshot") ) {
            if( support != null ) {
                Snapshot sourceSnapshot = null;

                try {
                    for( Region r : tm.getProvider().getDataCenterServices().listRegions() ) {
                        //noinspection ConstantConditions
                        if( !r.getProviderRegionId().equals(tm.getContext().getRegionId()) ) {
                            for( Snapshot snapshot : support.listSnapshots() ) {
                                if( snapshot.getCurrentState().equals(SnapshotState.AVAILABLE) ) {
                                    sourceSnapshot = snapshot;
                                    break;
                                }
                            }
                        }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                if( sourceSnapshot != null ) {
                    testSnapshotId = sourceSnapshot.getProviderSnapshotId();
                    testSourceRegion = sourceSnapshot.getRegionId();
                }
            }
        }
        else if( name.getMethodName().equals("filterSnapshots") ) {
            if( support != null ) {
                try {
                    testSnapshotId = DaseinTestManager.getComputeResources().provisionSnapshot(support, "filter", "dsnfilter", null);
                }
                catch( Throwable t ) {
                    tm.warn("Failed to provision test VM for snapshot filter test: " + t.getMessage());
                }
            }
        }
        else if( name.getMethodName().equals("removeSnapshot") ) {
            testSnapshotId = tm.getTestSnapshotId(DaseinTestManager.REMOVED, true);
            if( testSnapshotId != null ) {
                long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*5L);

                while( timeout > System.currentTimeMillis() ) {
                    try {
                        Snapshot s = support.getSnapshot(testSnapshotId);

                        if( s == null || !SnapshotState.PENDING.equals(s.getCurrentState()) ) {
                            break;
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                    try { Thread.sleep(15000L); }
                    catch( InterruptedException ignore ) { }
                }
            }
        }
        else {
            testSnapshotId = tm.getTestSnapshotId(DaseinTestManager.STATEFUL, true);
            testShareAccount = System.getProperty("shareAccount");
            if( name.getMethodName().equals("addPrivateShare") || name.getMethodName().equals("addPublicShare") ||
                    name.getMethodName().equals("removePrivateShare") || name.getMethodName().equals("removePublicShare") || name.getMethodName().equals("removeAllShares") ) {

                if( support != null ) {
                    try {
                        support.removeAllSnapshotShares(testSnapshotId);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
            if( testShareAccount != null && (name.getMethodName().equals("removePrivateShare") || name.getMethodName().equals("removeAllShares")) ) {
                if( support != null ) {
                    try {
                        support.addSnapshotShare(testSnapshotId, testShareAccount);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
            if( name.getMethodName().equals("removePublicShare") || name.getMethodName().equals("removeAllShares") ) {
                if( support != null ) {
                    try {
                        support.addPublicShare(testSnapshotId);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
    }

    @After
    public void after() {
        try {
            if( provisionedSnapshotId != null ) {
                ComputeServices services = tm.getProvider().getComputeServices();

                if( services != null ) {
                    SnapshotSupport support = services.getSnapshotSupport();

                    if( support != null ) {
                        try {
                            support.remove(provisionedSnapshotId);
                        }
                        catch( Throwable t ) {
                            tm.warn("Failed to clean up provisioned test snapshot " + provisionedSnapshotId + ": " + t.getMessage());
                        }
                    }
                }
            }
            provisionedSnapshotId = null;
            testSnapshotId = null;
            testSourceRegion = null;
            testVolumeId = null;
            testShareAccount = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void filterSnapshots() throws CloudException, InternalException {
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
        Iterable<Snapshot> snapshots = support.listSnapshots(SnapshotFilterOptions.getInstance(".*[Ff][Ii][Ll][Tt][Ee][Rr].*"));
        boolean found = false;
        int count = 0;

        assertNotNull("Filtering must return at least an empty collections and may not be null", snapshots);
        for( Snapshot snapshot : snapshots ) {
            count++;
            if( snapshot.getProviderSnapshotId().equals(testSnapshotId) ) {
                found = true;
            }
            tm.out("Snapshot", snapshot);
        }
        tm.out("Total Snapshot Count", count);
        if( count < 1 && support.isSubscribed() ) {
            if( testSnapshotId == null ) {
                tm.warn("No snapshots were listed and thus the test may be in error");
            }
            else {
                Snapshot snapshot = support.getSnapshot(testSnapshotId);

                if( snapshot == null || !snapshot.getName().contains("dsnfilter") ) {
                    tm.warn("This cloud did not retain the snapshot meta-data, so no snapshots match");
                    found = true; // a little hack to deal with this case
                }
                else {
                    fail("Should have found test snapshot " + testSnapshotId + ", but none were found");
                }
            }
        }
        if( testSnapshotId != null ) {
            assertTrue("Did not find the test filter snapshot " + testSnapshotId + " among the filtered snapshots", found);
        }
        else if( !support.isSubscribed() ) {
            tm.warn("No test snapshots existed for filter test due to a lack of snapshot subscription, so results may not be valid");
        }
        else {
            fail("Cannot test snapshot filtering without a test snapshot");
        }
    }

    @Test
    public void createSnapshot() throws CloudException, InternalException {
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
        if( testVolumeId != null ) {
            SnapshotCreateOptions options = SnapshotCreateOptions.getInstanceForCreate(testVolumeId, "dsnsnap" + (System.currentTimeMillis()%10000), "Test Dasein Cloud Snapshot");

            if( support.isSubscribed() ) {
                provisionedSnapshotId = options.build(tm.getProvider());
                tm.out("New Snapshot", provisionedSnapshotId);
                // NOTE: snapshots are allowed to be null
                if( provisionedSnapshotId == null ) {
                    tm.warn("It is possible to get null when making new snapshots, but this test is not truly valid");
                }
            }
            else {
                try {
                    provisionedSnapshotId = options.build(tm.getProvider());
                    fail("Account claims not to be subscribed, but the snapshot creation succeeded");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException for unsubscribed account");
                }
            }
        }
        else {
            if( support.isSubscribed() ) {
                fail("No test volume exists for creating a test snapshot");
            }
            else {
                tm.ok("The account is not subscribed for snapshot services");
            }
        }
    }

    @Test
    public void copySnapshot() throws CloudException, InternalException {
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
        if( !support.getCapabilities().supportsSnapshotCopying() ) {
            if( testSnapshotId == null ) {
                testSnapshotId = "nonsense";
            }
            if( testSourceRegion == null ) {
                testSourceRegion = "nonsense";
            }
            try {
                provisionedSnapshotId = support.createSnapshot(SnapshotCreateOptions.getInstanceForCopy(testSourceRegion, testSnapshotId, "dsnsnap" + (System.currentTimeMillis()%10000), "Test Dasein Cloud Snapshot"));
                fail("No error occurred copying even though copying is not supported");
            }
            catch( OperationNotSupportedException e ) {
                tm.ok("Snapshot copy unsupported");
            }
        }
        else {
            if( testSnapshotId == null ) {
                tm.warn("Unable to identify a source snapshot for test copy");
            }
            else {
                provisionedSnapshotId = support.createSnapshot(SnapshotCreateOptions.getInstanceForCopy(testSourceRegion, testSnapshotId, "dsnsnap" + (System.currentTimeMillis()%10000), "Test Dasein Cloud Snapshot"));
                tm.out("Snapshot Copy", provisionedSnapshotId);
                assertNotNull("Copy must result in the creation of a new snapshot", provisionedSnapshotId);
            }
        }
    }

    @Test
    public void removeSnapshot() throws CloudException, InternalException {
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
        if( testSnapshotId != null ) {
            Snapshot snapshot = support.getSnapshot(testSnapshotId);

            assertNotNull("No test snapshot exists for removal test", snapshot);
            tm.out("Before", snapshot.getCurrentState());
            support.remove(testSnapshotId);
            snapshot = support.getSnapshot(testSnapshotId);
            tm.out("After", snapshot == null ? SnapshotState.DELETED : snapshot.getCurrentState());
            assertTrue("The test snapshot still exists", snapshot == null || SnapshotState.DELETED.equals(snapshot.getCurrentState()));
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("This account is not subscribed for snapshot support, so this test cannot be run");
            }
            else {
                fail("No test snapshot exists for executing this test");
            }
        }
    }

    @Test
    public void listShares() throws CloudException, InternalException {
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
        if( support.getCapabilities().supportsSnapshotSharing() ) {
            if( testSnapshotId != null ) {
                Snapshot snapshot = support.getSnapshot(testSnapshotId);

                assertNotNull("Failed to find the test snapshot among possible snapshots", snapshot);
                Iterable<String> shares = support.listShares(testSnapshotId);

                tm.out("Image Shares", shares);
                assertNotNull("Snapshot shares may not be null", shares);
            }
            else {
                if( !support.isSubscribed() ) {
                    tm.warn("No snapshot ID was identified, so this test is not valid");
                }
                else {
                    fail("No test snapshot exists for " + name.getMethodName());
                }
            }
        }
        else {
            Iterable<String> shares = support.listShares(testSnapshotId);
            assertTrue("Snapshot sharing not supported, result should be empty list", !shares.iterator().hasNext());
        }
    }

    @Test
    public void addPrivateShare() throws CloudException, InternalException {
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
        if( testSnapshotId != null ) {
            if( testShareAccount != null ) {
                Iterable<String> shares = support.listShares(testSnapshotId);

                tm.out("Before", shares);
                if( support.getCapabilities().supportsSnapshotSharing() ) {
                    support.addSnapshotShare(testSnapshotId, testShareAccount);

                    boolean found = false;

                    shares = support.listShares(testSnapshotId);
                    tm.out("After", shares);
                    for( String share : shares ) {
                        if( share.equals(testShareAccount) ) {
                            found = true;
                            break;
                        }
                    }
                    assertTrue("Did not find the new share among the listed shares", found);
                }
                else {
                    try {
                        support.addSnapshotShare(testSnapshotId, testShareAccount);
                        fail("Private snapshot sharing is not supported, but the operation completed without error");
                    }
                    catch( OperationNotSupportedException expected ) {
                        tm.ok("Caught OperationNotSupportedException while attempting to share a snapshot");
                    }
                }
            }
            else {
                tm.warn("Unable to test account sharing due to no shareAccount property having been set (test invalid)");
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.warn("No snapshot ID was identified because snapshots are not subscribed, so this test is not valid");
            }
            else {
                fail("No test snapshot exists for the " + name.getMethodName() + " test");
            }
        }
    }

    @Test
    public void removePrivateShare() throws CloudException, InternalException {
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
        if( testSnapshotId != null ) {
            if( testShareAccount != null ) {
                Iterable<String> shares = support.listShares(testSnapshotId);

                tm.out("Before", shares);
                if( support.getCapabilities().supportsSnapshotSharing() ) {
                    support.removeSnapshotShare(testSnapshotId, testShareAccount);

                    boolean found = false;

                    shares = support.listShares(testSnapshotId);
                    tm.out("After", shares);
                    for( String share : shares ) {
                        if( share.equals(testShareAccount) ) {
                            found = true;
                            break;
                        }
                    }
                    assertFalse("The test account remains among the shared accounts", found);
                }
                else {
                    try {
                        support.removeSnapshotShare(testSnapshotId, testShareAccount);
                        fail("Private image sharing is not supported, but the operation completed without error");
                    }
                    catch( OperationNotSupportedException expected ) {
                        tm.ok("Caught OperationNotSupportedException while attempting to remove a snapshot share");
                    }
                }
            }
            else {
                tm.warn("Unable to test account share removal due to no shareAccount property having been set (test invalid)");
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.warn("No snapshot ID was identified because snapshots are not subscribed, so this test is not valid");
            }
            else {
                fail("No test snapshot exists for the " + name.getMethodName() + " test");
            }
        }
    }

    @Test
    public void addPublicShare() throws CloudException, InternalException {
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
        if( testSnapshotId != null ) {
            if( support.getCapabilities().supportsSnapshotSharingWithPublic() ) {
                tm.out("Before", support.isPublic(testSnapshotId));
                support.addPublicShare(testSnapshotId);
                boolean shared = support.isPublic(testSnapshotId);
                tm.out("After", shared);
                assertTrue("Image remains private", shared);
            }
            else {
                try {
                    support.addPublicShare(testSnapshotId);
                    fail("Public snapshot sharing is not supported, but the public share operation succeeded");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException while attempting to add a public snapshot share");
                }
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.warn("No snapshot ID was identified due to lack of subscription, so this test is not valid");
            }
            else {
                fail("No test snapshot exists for the " + name.getMethodName() + " test");
            }
        }
    }

    @Test
    public void removePublicShare() throws CloudException, InternalException {
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
        if( testSnapshotId != null ) {
            if( support.getCapabilities().supportsSnapshotSharingWithPublic() ) {
                tm.out("Before", support.isPublic(testSnapshotId));
                support.removePublicShare(testSnapshotId);
                // race condition here - provider sometime takes time to update
                try { Thread.sleep(5000L); }
                catch( InterruptedException ignore ) { }
                boolean shared = support.isPublic(testSnapshotId);
                tm.out("After", shared);
                assertFalse("Snapshot remains public", shared);
            }
            else {
                try {
                    support.removePublicShare(testSnapshotId);
                    fail("Public snapshot sharing is not supported, but the public share operation succeeded");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException while attempting to remove a public snapshot share");
                }
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.warn("No snapshot ID was identified due to lack of subscription, so this test is not valid");
            }
            else {
                fail("No test snapshot exists for the " + name.getMethodName() + " test");
            }
        }
    }

    @Test
    public void removeAllShares() throws CloudException, InternalException {
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
        if( testSnapshotId != null ) {
            tm.out("Before [Public]", support.isPublic(testSnapshotId));
            tm.out("Before [Private]", support.listShares(testSnapshotId));
            try {
                support.removeAllSnapshotShares(testSnapshotId);
            }
            catch( OperationNotSupportedException e ) {
                fail("This operation should not throw an OperationNotSupportedException (just a NO-OP in clouds without sharing)");
            }
            boolean shared = support.isPublic(testSnapshotId);
            Iterable<String> shares = support.listShares(testSnapshotId);

            tm.out("After [Public]", shared);
            tm.out("After [Private]", shares);

            assertFalse("Snapshot remains public", shared);
            assertFalse("Snapshot still has private shares", shares.iterator().hasNext());
        }
        else {
            if( !support.isSubscribed() ) {
                tm.warn("No snapshot ID was identified due to lack of subscription, so this test is not valid");
            }
            else {
                fail("No test snapshot exists for the " + name.getMethodName() + " test");
            }
        }
    }

/*
    @Test
    public void mountVolumeFromSnapshot() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services == null ) {
            tm.ok("Compute services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        SnapshotSupport support = services.getSnapshotSupport();

        VolumeSupport VolumeSupport = services.getVolumeSupport();

        if( support == null ) {
            tm.ok("Snapshots are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        testVolumeId = tm.getTestVolumeId(DaseinTestManager.STATEFUL, true, null, testDataCenterId);
        testVmId = tm.getTestVMId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, testDataCenterId);

        VolumeSupport.attach(testVolumeId, testVmId, "sdb");

        System.out.println("PUT A BREAKPOINT HERE TO PARTITION, FORMAT DISK, AND TOUCH A TEST FILE ON IT. --Havent worked out how to automate this yet");
        if( testVolumeId != null ) {
            SnapshotCreateOptions options = SnapshotCreateOptions.getInstanceForCreate(testVolumeId, tm.getUserName() + "dsnsnap" + (System.currentTimeMillis()%10000), "Test Dasein Cloud Snapshot");

            provisionedSnapshotId = support.createSnapshot(options);

            String newVolume = null;
            if( support.isSubscribed() ) {
                try {
                    VolumeCreateOptions volumeOptions = VolumeCreateOptions.getInstanceForSnapshot(provisionedSnapshotId, new Storage<Gigabyte>(1, Storage.GIGABYTE), "snap-vol-" + (System.currentTimeMillis()%10000), "mountVolumeFromSnapshot test").inDataCenter(testDataCenterId);
                    newVolume = volumeOptions.build(tm.getProvider());
                    VolumeSupport.attach(newVolume, testVmId, "sdc");
                    System.out.println("PUT ANOTHER BREAKPOINT HERE TO LOOK ON VM THAT PARTITION SDC1 EXISTS AND CONTAINS YOUR TEST FILE");
                } catch (Exception ex) {
                    // ignore
                } finally {
                    VolumeSupport.detach(newVolume);
                    VolumeSupport.remove(newVolume);
                    services.getSnapshotSupport().remove(provisionedSnapshotId);
                }
            }
        }
    }
*/
}
