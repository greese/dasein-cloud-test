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

package org.dasein.cloud.test.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.SnapshotSupport;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.compute.VolumeFilterOptions;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * Stateful integration tests verifying operations on volumes.
 * <p>Created by George Reese: 2/20/13 6:10 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatefulVolumeTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulVolumeTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String provisionedVolume;
    private String testSnapshotId;
    private String testVLANId;
    private String testVMId;
    private String testVolumeId;

    public StatefulVolumeTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        if( name.getMethodName().equals("createNFSVolume") ) {
            testVLANId = tm.getTestVLANId(DaseinTestManager.STATELESS, false, null);
            if( testVLANId == null ) {
                testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
            }
        }
        else if( name.getMethodName().equals("createFromSnapshot") ) {
            testSnapshotId = tm.getTestSnapshotId(DaseinTestManager.STATELESS, false);
            if( testSnapshotId == null ) {
                testSnapshotId = tm.getTestSnapshotId(DaseinTestManager.STATEFUL, true);
            }
        }
        else if( name.getMethodName().equals("removeVolume") ) {
            testVolumeId = tm.getTestVolumeId(DaseinTestManager.REMOVED, true, null, null);
        }
        else if( name.getMethodName().equals("filterVolumes") ) {
            ComputeServices services = tm.getProvider().getComputeServices();

            if( services != null ) {
                VolumeSupport support = services.getVolumeSupport();

                if( support != null ) {
                    try {
                        //noinspection ConstantConditions
                        testVolumeId = DaseinTestManager.getComputeResources().provisionVolume(support, "filter", "dsnfilter", null, null);
                    }
                    catch( Throwable t ) {
                        tm.warn("Failed to provisionKeypair VM for filter test: " + t.getMessage());
                    }
                }
            }
        }
        else if( name.getMethodName().equals("attach") ) {
            testVMId = tm.getTestVMId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, null);
            String dc = null;

            if( testVMId != null ) {
                try {
                    VirtualMachine vm = tm.getProvider().getComputeServices().getVirtualMachineSupport().getVirtualMachine(testVMId);

                    if( vm != null ) {
                        dc = vm.getProviderDataCenterId();
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
            testVolumeId = tm.getTestVolumeId(DaseinTestManager.STATEFUL, true, null, dc);

            if( testVolumeId != null ) {
                try {
                    @SuppressWarnings("ConstantConditions") Volume v = tm.getProvider().getComputeServices().getVolumeSupport().getVolume(testVolumeId);

                    if( v != null && v.getProviderVirtualMachineId() != null ) {
                        //noinspection ConstantConditions
                        tm.getProvider().getComputeServices().getVolumeSupport().detach(testVolumeId, true);
                        try { Thread.sleep(60000L); }
                        catch( InterruptedException ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        else if( name.getMethodName().equals("detach") ) {
            testVMId = tm.getTestVMId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, null);
            String dc = null;

            if( testVMId != null ) {
                try {
                    VirtualMachine vm = tm.getProvider().getComputeServices().getVirtualMachineSupport().getVirtualMachine(testVMId);

                    if( vm != null ) {
                        dc = vm.getProviderDataCenterId();
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
            testVolumeId = tm.getTestVolumeId(DaseinTestManager.STATEFUL, true, null, dc);

            if( testVolumeId != null && testVMId != null ) {
                try {
                    VolumeSupport support = tm.getProvider().getComputeServices().getVolumeSupport();
                    VirtualMachine vm = tm.getProvider().getComputeServices().getVirtualMachineSupport().getVirtualMachine(testVMId);

                    if( vm != null ) {
                        Volume v = support.getVolume(testVolumeId);
                        String a = (v == null ? null : v.getProviderVirtualMachineId());

                        if( a == null ) {
                            for( String deviceId : support.getCapabilities().listPossibleDeviceIds(vm.getPlatform()) ) {
                                try {
                                    support.attach(testVolumeId, testVMId, deviceId);
                                    break;
                                }
                                catch( Throwable ignore ) {
                                    // ignore
                                }
                            }
                        }
                    }
                    long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE* 5L);

                    while( timeout > System.currentTimeMillis() ) {
                        Volume volume = support.getVolume(testVolumeId);

                        if( volume == null ) {
                            break;
                        }
                        if( volume.getProviderVirtualMachineId() != null ) {
                            break;
                        }
                        try { Thread.sleep(30000L); }
                        catch( InterruptedException e ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        else if( name.getMethodName().equals("attachToBogusVM") ) {
            testVolumeId = tm.getTestVolumeId(DaseinTestManager.STATEFUL, true, null, null);

            if( testVolumeId != null ) {
                try {
                    @SuppressWarnings("ConstantConditions") Volume v = tm.getProvider().getComputeServices().getVolumeSupport().getVolume(testVolumeId);

                    if( v != null && v.getProviderVirtualMachineId() != null ) {
                        //noinspection ConstantConditions
                        tm.getProvider().getComputeServices().getVolumeSupport().detach(testVolumeId, true);
                        try { Thread.sleep(60000L); }
                        catch( InterruptedException ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        else if( name.getMethodName().equals("detachUnattachedVolume") ) {
            testVolumeId = tm.getTestVolumeId(DaseinTestManager.STATEFUL, true, null, null);

            if( testVolumeId != null ) {
                try {
                    @SuppressWarnings("ConstantConditions") Volume v = tm.getProvider().getComputeServices().getVolumeSupport().getVolume(testVolumeId);

                    if( v != null && v.getProviderVirtualMachineId() != null ) {
                        //noinspection ConstantConditions
                        tm.getProvider().getComputeServices().getVolumeSupport().detach(testVolumeId, true);
                        try { Thread.sleep(60000L); }
                        catch( InterruptedException ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
    }

    @After
    public void after() {
        try {
            ComputeServices services = tm.getProvider().getComputeServices();

            if( services != null ) {
                VolumeSupport support = services.getVolumeSupport();

                if( support != null ) {
                    if( testVolumeId != null ) {
                        try {
                            Volume v = support.getVolume(testVolumeId);

                            if( v != null && v.getProviderVirtualMachineId() != null ) {
                                support.detach(testVolumeId, true);
                            }
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                    if( provisionedVolume != null ) {
                        try {
                            support.detach(provisionedVolume, true);
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                        try { Thread.sleep(10000L); }
                        catch( InterruptedException ignore ) { }
                        try {
                            support.remove(provisionedVolume);
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
            provisionedVolume = null;
            testSnapshotId = null;
            testVolumeId = null;
            testVLANId = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void filterVolumes() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                Iterable<Volume> volumes = support.listVolumes(VolumeFilterOptions.getInstance(".*[Ff][Ii][Ll][Tt][Ee][Rr].*"));
                boolean found = false;
                int count = 0;

                assertNotNull("Filtering must return at least an empty collections and may not be null", volumes);
                for( Volume volume : volumes ) {
                    count++;
                    if( volume.getProviderVolumeId().equals(testVolumeId) ) {
                        found = true;
                    }
                    tm.out("Volume", volume);
                }
                tm.out("Total Volume Count", count);
                if( count < 1 && support.isSubscribed() ) {
                    if( testVolumeId == null ) {
                        tm.warn("No volumes were listed and thus the test may be in error");
                    }
                    else {
                        fail("Should have found test volume " + testVolumeId + ", but none were found");
                    }
                }
                if( testVolumeId != null ) {
                    assertTrue("Did not find the test filter volume " + testVolumeId + " among the filtered volumes", found);
                }
                else if( !support.isSubscribed() ) {
                    tm.warn("No test volumes existed for filter test, so results may not be valid");
                }
                else {
                    fail("Cannot test volume filtering without a test volume");
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void createBlockVolume() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                boolean supported = support.isSubscribed();

                if( supported ) {
                    supported = false;
                    for( VolumeFormat fmt : support.getCapabilities().listSupportedFormats() ) {
                        if( fmt.equals(VolumeFormat.BLOCK) ) {
                            supported = true;
                        }
                    }
                }

                String productId = tm.getTestVolumeProductId();
                VolumeCreateOptions options = null;

                if( productId != null ) {
                    Storage<Gigabyte> size = null;

                    if( support.getCapabilities().isVolumeSizeDeterminedByProduct() ) {
                        VolumeProduct product = null;

                        for( VolumeProduct prd : support.listVolumeProducts() ) {
                            if( prd.getProviderProductId().equals(productId) ) {
                                product = prd;
                                break;
                            }
                        }
                        if( product != null ) {
                            size = product.getVolumeSize();
                        }
                    }
                    if( size == null ) {
                        size = support.getCapabilities().getMinimumVolumeSize();
                    }
                    options = VolumeCreateOptions.getInstance(productId, size, "dsnvolprv" + (System.currentTimeMillis()%10000), "Volume Provisioning Test", 0);
                }
                if( options == null ) {
                    options = VolumeCreateOptions.getInstance(support.getCapabilities().getMinimumVolumeSize(), "dsnvolprv" + (System.currentTimeMillis()%10000), "Volume Provisioning Test");
                }

                if( supported ) {
                    provisionedVolume = options.build(tm.getProvider());

                    tm.out("New Block Volume", provisionedVolume);
                }
                else {
                    try {
                        provisionedVolume = options.build(tm.getProvider());
                        fail("Block volumes are either not subscribed or supported, yet the operation completed");
                    }
                    catch( OperationNotSupportedException expected ) {
                        tm.ok("Got an OperationNotSupportedException from " + name.getMethodName() + " as expected");
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void createNFSVolume() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                boolean supported = support.isSubscribed();

                if( supported ) {
                    supported = false;
                    for( VolumeFormat fmt : support.getCapabilities().listSupportedFormats() ) {
                        if( fmt.equals(VolumeFormat.NFS) ) {
                            supported = true;
                        }
                    }
                }
                if( testVLANId != null ) {
                    String productId = tm.getTestVolumeProductId();
                    VolumeCreateOptions options = null;

                    if( productId != null ) {
                        Storage<Gigabyte> size = null;

                        if( support.getCapabilities().isVolumeSizeDeterminedByProduct() ) {
                            VolumeProduct product = null;

                            for( VolumeProduct prd : support.listVolumeProducts() ) {
                                if( prd.getProviderProductId().equals(productId) ) {
                                    product = prd;
                                    break;
                                }
                            }
                            if( product != null ) {
                                size = product.getVolumeSize();
                            }
                        }
                        if( size == null ) {
                            size = support.getCapabilities().getMinimumVolumeSize();
                        }
                        options = VolumeCreateOptions.getNetworkInstance(productId, testVLANId, size, "dsnnfsvol" + (System.currentTimeMillis()%10000), "Dasein NFS volume test");
                    }
                    if( options == null ) {
                        options = VolumeCreateOptions.getNetworkInstance(testVLANId, support.getCapabilities().getMinimumVolumeSize(), "dsnvolprv" + (System.currentTimeMillis()%10000), "Volume Provisioning Test");
                    }

                    if( supported ) {
                        provisionedVolume = options.build(tm.getProvider());

                        tm.out("New NFS Volume", provisionedVolume);
                    }
                    else {
                        try {
                            provisionedVolume = options.build(tm.getProvider());
                            fail("NFS volumes are either not subscribed or supported, yet the operation completed");
                        }
                        catch( OperationNotSupportedException expected ) {
                            tm.ok("Got an OperationNotSupportedException from " + name.getMethodName() + " as expected");
                        }
                    }
                }
                else {
                    if( !supported ) {
                        tm.ok("Either network volumes are not supported or volumes are not subscribed");
                    }
                    else {
                        fail("Unable to test network volume provisioning due to a lack of a network in which to test");
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void createFromSnapshot() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                boolean supported = true;

                if( testSnapshotId == null ) {
                    SnapshotSupport snapSupport = services.getSnapshotSupport();

                    if( snapSupport == null || !snapSupport.isSubscribed() ) {
                        supported = false;
                        testSnapshotId = UUID.randomUUID().toString();
                    }
                    else {
                        fail("No test snapshot ID even though snapshots are supported");
                    }
                }
                String productId = tm.getTestVolumeProductId();
                VolumeCreateOptions options = null;

                if( productId != null ) {
                    Storage<Gigabyte> size = null;

                    if( support.getCapabilities().isVolumeSizeDeterminedByProduct() ) {
                        VolumeProduct product = null;

                        for( VolumeProduct prd : support.listVolumeProducts() ) {
                            if( prd.getProviderProductId().equals(productId) ) {
                                product = prd;
                                break;
                            }
                        }
                        if( product != null ) {
                            size = product.getVolumeSize();
                        }
                    }
                    if( size == null ) {
                        size = support.getCapabilities().getMinimumVolumeSize();
                    }
                    options = VolumeCreateOptions.getInstanceForSnapshot(productId, testSnapshotId, size, "dsnvolprv" + (System.currentTimeMillis()%10000), "Volume Provisioning Test", 0);
                }
                if( options == null ) {
                    options = VolumeCreateOptions.getInstanceForSnapshot(testSnapshotId, support.getCapabilities().getMinimumVolumeSize(), "dsnvolprv" + (System.currentTimeMillis()%10000), "Volume Provisioning Test");
                }

                if( support.isSubscribed() && supported ) {
                    provisionedVolume = options.build(tm.getProvider());

                    tm.out("New Volume from Snapshot", provisionedVolume);
                }
                else {
                    try {
                        provisionedVolume = options.build(tm.getProvider());
                        fail("Volume snapshots are either not subscribed or supported, yet the operation completed");
                    }
                    catch( OperationNotSupportedException expected ) {
                        tm.ok("Got an OperationNotSupportedException from " + name.getMethodName() + " as expected");
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void attach() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                if( !support.isSubscribed() ) {
                    tm.warn("Not subscribed to volume services, test will not run properly");
                    return;
                }
                if( testVolumeId != null ) {
                    Volume volume = support.getVolume(testVolumeId);

                    assertNotNull("Test volume is null and so the test cannot be run", volume);

                    tm.out("Attachment Before", volume.getProviderVirtualMachineId());
                    assertNull("Attachment must be null before running this test", volume.getProviderVirtualMachineId());
                    if( testVMId != null ) {
                        @SuppressWarnings("ConstantConditions") VirtualMachine vm = services.getVirtualMachineSupport().getVirtualMachine(testVMId);

                        assertNotNull("Virtual machine for test went away", vm);

                        boolean attached = false;

                        for( String device : support.getCapabilities().listPossibleDeviceIds(vm.getPlatform()) ) {
                            try {
                                if( volume.getFormat().equals(VolumeFormat.NFS) ) {
                                    try {
                                        support.attach(testVolumeId, testVMId, device);
                                        fail("Attachment to NFS volume succeeded even though it should not");
                                    }
                                    catch( OperationNotSupportedException expected ) {
                                        tm.ok("NFS volumes cannot be attached");
                                        return;
                                    }
                                }
                                else {
                                    support.attach(testVolumeId, testVMId, device);
                                }
                                attached = true;
                                break;
                            }
                            catch( CloudException e ) {
                                tm.warn("Failed to mount using " + device + ", will hopefully try again");
                            }
                        }
                        assertTrue("Unable to attach using any available device", attached);

                        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE* 5L);

                        while( timeout > System.currentTimeMillis() ) {
                            volume = support.getVolume(testVolumeId);

                            assertNotNull("Volume disappeared during attachment", volume);
                            tm.out("---> Attachment", volume.getProviderVirtualMachineId());
                            if( volume.getProviderVirtualMachineId() != null ) {
                                assertEquals("Volume attachment does not match target server", testVMId, volume.getProviderVirtualMachineId());
                                return;
                            }
                            try { Thread.sleep(30000L); }
                            catch( InterruptedException e ) { }
                        }
                        fail("System timed out verifying attachment");
                    }
                    else {
                        fail("No test VM exists for this test");
                    }
                }
                else {
                    if( support.isSubscribed() ) {
                        fail("No test volume for " + name.getMethodName());
                    }
                    else {
                        tm.ok("Volume service is not subscribed so this test is not entirely valid");
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void detach() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                if( !support.isSubscribed() ) {
                    tm.warn("Not subscribed to volume services, test will not run properly");
                    return;
                }
                if( testVolumeId != null ) {
                    Volume volume = support.getVolume(testVolumeId);

                    assertNotNull("Test volume is null and so the test cannot be run", volume);

                    tm.out("Attachment Before", volume.getProviderVirtualMachineId());
                    assertNotNull("Volume must be attached to something before attempting to detach it", volume.getProviderVirtualMachineId());
                    support.detach(testVolumeId, true);

                    long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE* 5L);

                    while( timeout > System.currentTimeMillis() ) {
                        volume = support.getVolume(testVolumeId);

                        assertNotNull("Volume disappeared during detachment", volume);
                        tm.out("---> Attachment", volume.getProviderVirtualMachineId());
                        if( volume.getProviderVirtualMachineId() == null ) {
                            return;
                        }
                        try { Thread.sleep(30000L); }
                        catch( InterruptedException e ) { }
                    }
                    fail("System timed out verifying attachment");
                }
                else {
                    if( support.isSubscribed() ) {
                        fail("No test volume for " + name.getMethodName());
                    }
                    else {
                        tm.ok("Volume service is not subscribed so this test is not entirely valid");
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void attachToBogusVM() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                if( !support.isSubscribed() ) {
                    tm.warn("Not subscribed to volume services, test will not run properly");
                    return;
                }
                if( testVolumeId != null ) {
                    Volume volume = support.getVolume(testVolumeId);

                    assertNotNull("Test volume is null and so the test cannot be run", volume);

                    tm.out("Attachment Before", volume.getProviderVirtualMachineId());
                    assertNull("Attachment must be null before running this test", volume.getProviderVirtualMachineId());
                    String id = UUID.randomUUID().toString();
                    boolean succeeded = false;

                    for( String device : support.getCapabilities().listPossibleDeviceIds(Platform.UBUNTU) ) {
                        if( volume.getFormat().equals(VolumeFormat.NFS) ) {
                            try {
                                support.attach(testVolumeId, id, device);
                                fail("Attachment to NFS volume succeeded even though it should not");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("NFS volumes cannot be attached");
                                return;
                            }
                        }
                        else {
                            try {
                                support.attach(testVolumeId, id, device);
                                succeeded = true;
                                break;
                            }
                            catch( CloudException expected ) {
                                tm.ok("--> Failed with " + device);
                            }
                        }
                    }
                    assertFalse("The system reported that it successfully attached to a server that does not exist", succeeded);
                }
                else {
                    if( support.isSubscribed() ) {
                        fail("No test volume for " + name.getMethodName());
                    }
                    else {
                        tm.ok("Volume service is not subscribed so this test is not entirely valid");
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void detachUnattachedVolume() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                if( !support.isSubscribed() ) {
                    tm.warn("Not subscribed to volume services, test will not run properly");
                    return;
                }
                if( testVolumeId != null ) {
                    Volume volume = support.getVolume(testVolumeId);

                    assertNotNull("Test volume is null and so the test cannot be run", volume);

                    tm.out("Attachment Before", volume.getProviderVirtualMachineId());
                    assertNull("Volume must be unattached before attempting this test", volume.getProviderVirtualMachineId());
                    try {
                        support.detach(testVolumeId, true);
                        fail("Detachment should have failed for an unattached volume");
                    }
                    catch( CloudException expected ) {
                        tm.ok("Caught a CloudException: " + expected.getMessage());
                    }
                }
                else {
                    fail("No test volume for " + name.getMethodName());
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }


    @Test
    public void removeVolume() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                if( testVolumeId != null ) {
                    Volume volume = support.getVolume(testVolumeId);

                    tm.out("Before", volume);
                    assertNotNull("Test volume no longer exists, cannot test removing it", volume);
                    tm.out("State", volume.getCurrentState());
                    assertFalse("Test volume is deleted, cannot test removing it", VolumeState.DELETED.equals(volume.getCurrentState()));
                    support.remove(testVolumeId);
                    volume = support.getVolume(testVolumeId);
                    tm.out("After", volume);
                    tm.out("State", (volume == null ? VolumeState.DELETED : volume.getCurrentState()));
                    assertTrue("The volume remains available", (volume == null || VolumeState.DELETED.equals(volume.getCurrentState())));
                }
                else {
                    if( support.isSubscribed() ) {
                        fail("No test volume for deletion test");
                    }
                    else {
                        tm.ok("Volume service is not subscribed so this test is not entirely valid");
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

}
