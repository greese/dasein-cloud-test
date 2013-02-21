/**
 * Copyright (C) 2009-2012 enStratus Networks Inc
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

package org.dasein.cloud.test;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.Snapshot;
import org.dasein.cloud.compute.SnapshotCreateOptions;
import org.dasein.cloud.compute.SnapshotState;
import org.dasein.cloud.compute.SnapshotSupport;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.compute.VolumeFilterOptions;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.util.APITrace;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("JUnit4AnnotatedMethodInJUnit3TestCase")
public class VolumeTestCase extends BaseTestCase {
    static public final String T_ATTACH_VOLUME      = "testAttachVolume";
    static public final String T_ATTACH_NO_SERVER   = "testAttachVolumeToNoServer";
    static public final String T_CREATE_FROM_SNAP   = "testCreateVolumeFromSnapshot";
    static public final String T_CREATE_VOLUME      = "testCreateVolume";
    static public final String T_DETACH_VOLUME      = "testDetachVolume";
    static public final String T_DETACH_UNATTACHED  = "testDetachUnattachedVolume";
    static public final String T_FILTER             = "testFilter";
    static public final String T_GET_VOLUME         = "testGetVolume";
    static public final String T_REMOVE_VOLUME      = "testRemoveVolume";
    static public final String T_VOLUME_CONTENT     = "testVolumeContent";

    static private VirtualMachine testVm     = null;
    static private int            vmUse      = 0;

    static public final String[] NEEDS_VMS   = new String[] { T_ATTACH_VOLUME, T_DETACH_VOLUME, T_DETACH_UNATTACHED, T_CREATE_FROM_SNAP, T_FILTER };
    static public final String[] NEEDS_VLANS = new String[] { T_VOLUME_CONTENT, T_GET_VOLUME, T_ATTACH_VOLUME, T_DETACH_VOLUME, T_DETACH_UNATTACHED, T_FILTER, T_REMOVE_VOLUME, T_ATTACH_NO_SERVER, T_CREATE_FROM_SNAP, T_CREATE_VOLUME };

    private CloudProvider provider       = null;
    private Snapshot      testSnapshot   = null;
    private Volume        testVolume     = null;
    private VLAN          testVlan       = null;
    private String        volumeToDelete = null;

    public VolumeTestCase(@Nonnull String name) { super(name); }

    private void createTestVm() throws CloudException, InternalException {
        vmUse++;

        if( isNetwork() ) {
            return;
        }
        if( testVm == null ) {
            ComputeServices services = provider.getComputeServices();
            VirtualMachineSupport vmSupport;

            if( services != null ) {
                vmSupport = services.getVirtualMachineSupport();
                if( vmSupport != null ) {
                    testVm = vmSupport.getVirtualMachine(launch(provider));
                    if( testVm == null ) {
                        Assert.fail("Virtual machine failed to be reflected as launched");
                    }

                    long timeout = System.currentTimeMillis() + getLaunchWindow();
                    VirtualMachine vm = testVm;

                    while( timeout > System.currentTimeMillis() ) {
                        if( vm == null ) {
                            Assert.fail("VM " + testVm.getProviderVirtualMachineId() + " disappeared during launch");
                        }
                        if( VmState.RUNNING.equals(vm.getCurrentState()) ) {
                            break;
                        }
                        try { Thread.sleep(30000L); }
                        catch( InterruptedException ignore ) { }
                        try { vm = vmSupport.getVirtualMachine(testVm.getProviderVirtualMachineId()); }
                        catch( Throwable ignore ) { }
                    }
                }
            }
        }
    }

    private @Nullable Volume createTestVolume() throws CloudException, InternalException {
        VolumeSupport support = getSupport();
        VolumeProduct product = null;

        if( support.getVolumeProductRequirement().equals(Requirement.REQUIRED) || support.isVolumeSizeDeterminedByProduct() ) {
            for( VolumeProduct prd : support.listVolumeProducts() ) {
                Float thisCost = prd.getMonthlyGigabyteCost();
                Float currentCost = (product == null ? null : product.getMonthlyGigabyteCost());

                if( currentCost == null || (thisCost == null ? 0.0f : thisCost) < currentCost ) {
                    product = prd;
                }
            }
        }
        Storage<Gigabyte> size;

        if( product == null ) {
            size = support.getMinimumVolumeSize();
        }
        else {
            size = product.getVolumeSize();
            if( size == null ) {
                size = support.getMinimumVolumeSize();
            }
        }
        String name = "dsnvol-" + getName() + "-" + (System.currentTimeMillis()%10000);
        VolumeCreateOptions options;

        if( product == null ) {
            if( testVlan == null ) {
                options = VolumeCreateOptions.getInstance(size, name, name);
            }
            else {
                options = VolumeCreateOptions.getNetworkInstance(testVlan.getProviderVlanId(), size, name, name);
            }
        }
        else {
            if( testVlan == null ) {
                options = VolumeCreateOptions.getInstance(product.getProviderProductId(), size, name, name, 0);
            }
            else {
                options = VolumeCreateOptions.getNetworkInstance(product.getProviderProductId(), testVlan.getProviderVlanId(), size, name, name);
            }
        }
        if( testVm != null ) {
            options.inDataCenter(testVm.getProviderDataCenterId());
        }
        else {
            String dc = getTestDataCenterId();

            if( dc != null ) {
                options.inDataCenter(dc);
            }
        }
        volumeToDelete = support.createVolume(options);
        long timeout = System.currentTimeMillis() + getStateChangeWindow();
        Volume volume = getSupport().getVolume(volumeToDelete);

        while( timeout > System.currentTimeMillis() ) {
            if( volume == null || VolumeState.DELETED.equals(volume.getCurrentState()) ) {
                Assert.fail("Volume disappeared during creation");
            }
            if( VolumeState.AVAILABLE.equals(volume.getCurrentState()) ) {
                break;
            }
            try { Thread.sleep(15000L); }
            catch( InterruptedException ignore ) { }
            try { volume = getSupport().getVolume(volumeToDelete); }
            catch( Throwable ignore ) { }
        }
        return volume;
    }

    private VolumeSupport getSupport() {
        if( provider == null ) {
            Assert.fail("No provider configuration set up");
        }
        ComputeServices services = provider.getComputeServices();

        Assert.assertNotNull("No compute services exist in " + provider.getCloudName(), services);

        VolumeSupport support = services.getVolumeSupport();

        Assert.assertNotNull("No volume services exist in " + provider.getCloudName(), support);
        return support;
    }

    @Override
    public int getVlanReuseCount() {
        return NEEDS_VLANS.length;
    }

    private boolean isNetwork() throws CloudException, InternalException {
        boolean hasNetwork = false;

        for( VolumeFormat format : getSupport().listSupportedFormats() ) {
            if( format.equals(VolumeFormat.BLOCK) ) {
                return false;
            }
            else if( format.equals(VolumeFormat.NFS) ) {
                hasNetwork = true;
            }
        }
        return hasNetwork;
    }

    private void attach() throws CloudException, InternalException {
        Volume volume = testVolume;

        if( testVolume != null && testVolume.getFormat().equals(VolumeFormat.NFS) ) {
            return;
        }
        VirtualMachine vm = testVm;
        boolean attached = false;

        Assert.assertNotNull("Could not attach test VM to test volume because the volume disappeared", volume);
        Assert.assertNotNull("Could not attach test VM to test volume because the VM disappeared", vm);

        for( String device : getSupport().listPossibleDeviceIds(testVm.getPlatform()) ) {
            try {
                getSupport().attach(volume.getProviderVolumeId(), vm.getProviderVirtualMachineId(), device);
                attached = true;
                break;
            }
            catch( CloudException e ) {
                out("WARNING: Failed to mount using " + device + ", will hopefully try again");
            }
        }
        Assert.assertTrue("Unable to attach using any available device", attached);

        long timeout = System.currentTimeMillis() + getStateChangeWindow();

        while( timeout > System.currentTimeMillis() ) {
            try {
                //noinspection ConstantConditions
                vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(testVm.getProviderVirtualMachineId());
            }
            catch( Throwable ignore ) {
                // ignore
            }
            try { volume = getSupport().getVolume(testVolume.getProviderVolumeId()); }
            catch( Throwable ignore ) { }
            if( vm == null || volume == null ) {
                Assert.fail("VM or volume disappeared while waiting for attachment to be reflected");
            }
            if( vm.getProviderVirtualMachineId().equals(volume.getProviderVirtualMachineId()) ) {
                return;
            }
            try { Thread.sleep(15000L); }
            catch( InterruptedException e ) { }
        }
    }

    @Before
    @Override
    public void setUp() throws CloudException, InternalException, InstantiationException, IllegalAccessException, IOException {
        begin();
        provider = getProvider();
        provider.connect(getTestContext());
        for( String test : NEEDS_VLANS ) {
            if( test.equals(getName()) ) {
                NetworkServices services = provider.getNetworkServices();
                VLANSupport support = null;

                if( services != null ) {
                    support = services.getVlanSupport();
                }
                testVlan = findTestVLAN(provider, support, true, true);
                if( !isNetwork() ) {
                    testVlan = null; // we have to call findTestVLAN() to up the counter so any provisioned VLANs are properly deleted
                }
            }
        }
        for( String test : NEEDS_VMS ) {
            if( test.equals(getName()) ) {
                createTestVm();
            }
        }
        if( !getSupport().isSubscribed() ) {
            return;
        }
        if( getName().equals(T_VOLUME_CONTENT) || getName().equals(T_GET_VOLUME) ) {
            for( Volume v : getSupport().listVolumes() ) {
                if( testVolume == null || VolumeState.AVAILABLE.equals(v.getCurrentState()) ) {
                    testVolume = v;
                }
            }
            if( testVolume == null ) {
                testVolume = createTestVolume();
            }
            Assert.assertNotNull("Unable to execute volume content test due to lack of test volume", testVolume);
        }
        else if( getName().equals(T_ATTACH_VOLUME) || getName().equals(T_DETACH_VOLUME) || getName().equals(T_DETACH_UNATTACHED) || getName().equals(T_REMOVE_VOLUME) || getName().equals(T_ATTACH_NO_SERVER) || getName().equals(T_FILTER)) {
            testVolume = createTestVolume();
            Assert.assertNotNull("Unable to execute volume content test due to lack of test volume", testVolume);
            if( getName().equals(T_DETACH_VOLUME) || getName().equals(T_FILTER) ) {
                attach();
            }
        }
        else if( getName().equals(T_CREATE_FROM_SNAP) ) {
            testVolume = createTestVolume();

            ComputeServices services = provider.getComputeServices();
            SnapshotSupport support = null;

            if( services != null ) {
                support = services.getSnapshotSupport();
            }
            if( support != null && support.isSubscribed() ) {
                if( support.identifyAttachmentRequirement().equals(Requirement.REQUIRED) ) {
                    attach();
                }
                Assert.assertNotNull("Unable to execute volume content test due to lack of test volume", testVolume);
                try {
                    SnapshotCreateOptions options = SnapshotCreateOptions.getInstanceForCreate(testVolume.getProviderVolumeId(), "dsnsnap-" + getName() + (System.currentTimeMillis() %10000), "Test volume creation from snapshot");

                    testSnapshot = support.getSnapshot(support.createSnapshot(options));

                    Assert.assertNotNull("The test snapshot does not exist", testSnapshot);

                    long timeout = System.currentTimeMillis() + getStateChangeWindow();
                    String id = testSnapshot.getProviderSnapshotId();

                    while( timeout > System.currentTimeMillis() ) {
                        try {
                            Snapshot s = support.getSnapshot(id);

                            if( s == null || !SnapshotState.PENDING.equals(s.getCurrentState()) ) {
                                break;
                            }
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                        try { Thread.sleep(15000L); }
                        catch( InterruptedException e ) { }
                    }
                }
                catch( Throwable t ) {
                    Assert.fail("Unable to create a test snapshot for creating a volume: " + t.getMessage());
                }
                finally {
                    try {
                        getSupport().remove(testVolume.getProviderVolumeId());
                        testVolume = null;
                        volumeToDelete = null;
                    }
                    catch( Throwable t ) {
                        out("Warning: Unable to clean up test volume for temporary snapshot");
                    }
                }
            }
        }
    }

    @After
    @Override
    public void tearDown() {
        try {
            if( volumeToDelete != null ) {
                try {
                    String id = null;

                    try {
                        Volume volume = getSupport().getVolume(volumeToDelete);

                        if( volume != null ) {
                            id = volume.getProviderVolumeId();
                        }
                    }
                    catch( Throwable ignore ) {
                        id = volumeToDelete;
                    }
                    if( id != null ) {
                        try {
                            getSupport().detach(id);
                            long timeout = System.currentTimeMillis() + getStateChangeWindow();

                            while( timeout > System.currentTimeMillis() ) {
                                try {
                                    Volume v = getSupport().getVolume(id);

                                    if( v == null || v.getProviderVirtualMachineId() == null ) {
                                        break;
                                    }
                                }
                                catch( Throwable ignore ) {
                                    // ignore
                                }
                                try { Thread.sleep(15000L); }
                                catch( InterruptedException e ) { }
                            }
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                        long timeout = System.currentTimeMillis() + getStateChangeWindow();

                        while( timeout > System.currentTimeMillis() ) {
                            try {
                                Volume v = getSupport().getVolume(id);

                                if( v == null || !VolumeState.PENDING.equals(v.getCurrentState()) ) {
                                    break;
                                }
                            }
                            catch( Throwable ignore ) {
                                // ignore
                            }
                            try { Thread.sleep(15000L); }
                            catch( InterruptedException e ) { }
                        }
                        try {
                            getSupport().remove(id);
                        }
                        catch( Throwable e ) {
                            out("WARNING: Error cleaning up test volume: " + e.getMessage());
                        }
                    }
                }
                finally {
                    volumeToDelete = null;
                }
            }
            if( testSnapshot != null ) {
                try {
                    ComputeServices services = provider.getComputeServices();

                    if( services != null ) {
                        SnapshotSupport support = services.getSnapshotSupport();

                        if( support != null ) {
                            support.remove(testSnapshot.getProviderSnapshotId());
                        }
                    }
                }
                catch( Throwable e ) {
                    out("WARNING: Error cleaning up test snapshot " + testSnapshot + ": " + e.getMessage());
                    testSnapshot = null;
                }
            }
            if( vmUse >= NEEDS_VMS.length && testVm != null ) {
                try {
                    VirtualMachine vm;

                    try {
                        @SuppressWarnings("ConstantConditions") VirtualMachineSupport vmSupport = provider.getComputeServices().getVirtualMachineSupport();

                        assert vmSupport != null;
                        vm = vmSupport.getVirtualMachine(testVm.getProviderVirtualMachineId());
                        if( vm != null ) {
                            vmSupport.terminate(vm.getProviderVirtualMachineId());
                        }
                    }
                    catch( Throwable e ) {
                        out("WARNING: Error tearing down virtual machine: " + e.getMessage());
                    }
                }
                finally {
                    testVm = null;
                }
            }
            APITrace.report(getName());
            APITrace.reset();
            try {
                if( provider != null ) {
                    provider.close();
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        finally {
            end();
        }
    }

    @Test
    public void testAttachVolume() throws InternalException, CloudException {
        if( !getSupport().isSubscribed() ) {
            out("Warning: Not subscribed, test will not run");
            return;
        }
        if( testVm != null ) {
            if( testVolume != null && testVolume.getFormat().equals(VolumeFormat.NFS) ) {
                out("Cannot attach NFS volumes (OK)");
            }
            else {
                boolean attached = false;

                for( String device : getSupport().listPossibleDeviceIds(testVm.getPlatform()) ) {
                    try {
                        getSupport().attach(testVolume.getProviderVolumeId(), testVm.getProviderVirtualMachineId(), device);
                        attached = true;
                        break;
                    }
                    catch( CloudException e ) {
                        out("WARNING: Failed to mount using " + device + ", will hopefully try again");
                    }
                }
                Assert.assertTrue("Unable to attach using any available device", attached);

                long timeout = System.currentTimeMillis() + getStateChangeWindow();

                while( timeout > System.currentTimeMillis() ) {
                    Volume volume = getSupport().getVolume(testVolume.getProviderVolumeId());

                    Assert.assertNotNull("Volume disappeared during attachment", volume);
                    out("Attachment: " + volume.getProviderVirtualMachineId());
                    if( volume.getProviderVirtualMachineId() != null ) {
                        assertEquals("Volume attachment does not match target server", testVm.getProviderVirtualMachineId(), volume.getProviderVirtualMachineId());
                        return;
                    }
                    try { Thread.sleep(30000L); }
                    catch( InterruptedException e ) { }
                }
                Assert.fail("System timed out verifying attachment");
            }
        }
        else {
            out("Virtual machine services not supported (OK)");
        }
    }

    @Test
    public void testDetachVolume() throws InternalException, CloudException {
        if( !getSupport().isSubscribed() ) {
            out("Warning: Not subscribed, test will not run");
            return;
        }
        if( testVm != null ) {
            if( testVolume != null && testVolume.getFormat().equals(VolumeFormat.NFS) ) {
                out("Cannot attach NFS volumes (OK)");
            }
            else if( testVolume != null ) {
                Volume volume = getSupport().getVolume(testVolume.getProviderVolumeId());
                long timeout = System.currentTimeMillis() + getStateChangeWindow();

                while( timeout > System.currentTimeMillis() ) {
                    if( volume != null && volume.getCurrentState().equals(VolumeState.AVAILABLE) && volume.getProviderVirtualMachineId() != null ) {
                        break;
                    }
                    try { Thread.sleep(25000L); }
                    catch( InterruptedException e ) { }
                    try { volume = getSupport().getVolume(testVolume.getProviderVolumeId()); }
                    catch( Throwable ignore ) { }
                }
                assertNotNull("Volume to be detached was null", volume);
                getSupport().detach(testVolume.getProviderVolumeId());

                timeout = System.currentTimeMillis() + getStateChangeWindow();

                while( timeout > System.currentTimeMillis() ) {
                    try { volume = getSupport().getVolume(testVolume.getProviderVolumeId()); }
                    catch( Throwable ignore ) { }
                    if( volume == null ) {
                        Assert.fail("Volume disappeared during detachment");
                    }
                    out("Attachment: " + volume.getProviderVirtualMachineId());
                    if( volume.getProviderVirtualMachineId() == null ) {
                        return;
                    }
                    try { Thread.sleep(15000L); }
                    catch( InterruptedException e ) { }
                }
                fail("System timed out verifying detachment");
            }
        }
        else {
            out("Virtual machine services not supported (OK)");
        }
    }

    @Test
    public void testAttachVolumeToNoServer() throws InternalException, CloudException {
        if( !getSupport().isSubscribed() ) {
            out("Warning: Not subscribed, test will not run");
            return;
        }
        if( testVolume != null && testVolume.getFormat().equals(VolumeFormat.NFS) ) {
            out("NFS attachments not supported (OK)");
        }
        else {
            ComputeServices services = provider.getComputeServices();

            if( services != null ) {
                VirtualMachineSupport vmSupport = services.getVirtualMachineSupport();

                if( vmSupport != null ) {
                    Iterator<String> ids = getSupport().listPossibleDeviceIds(Platform.UNIX).iterator();
                    String device;

                    if( ids.hasNext() ) {
                        device = ids.next();
                    }
                    else {
                        device = "0";
                    }
                    try {
                        getSupport().attach(testVolume.getProviderVolumeId(), UUID.randomUUID().toString(), device);
                        Assert.fail("System did not error when attempting to attach to a fake server");
                    }
                    catch( CloudException expected ) {
                        out("Received error attaching to a non-existent virtual machine (OK)");
                    }
                }
            }
            else {
                out("Virtual machine services not supported (OK)");
            }
        }
    }

    @Test
    public void testDetachUnattachedVolume() throws InternalException, CloudException {
        if( !getSupport().isSubscribed() ) {
            out("Warning: Not subscribed, test will not run");
            return;
        }
        if( testVm != null ) {
            try {
                Volume volume = getSupport().getVolume(testVolume.getProviderVolumeId());
                long timeout = System.currentTimeMillis() + getStateChangeWindow();

                while( timeout > System.currentTimeMillis() ) {
                    if( volume != null && volume.getCurrentState().equals(VolumeState.AVAILABLE) ) {
                        break;
                    }
                    try { Thread.sleep(25000L); }
                    catch( InterruptedException e ) { }
                    try { volume = getSupport().getVolume(testVolume.getProviderVolumeId()); }
                    catch( Throwable ignore ) { }
                }
                assertNotNull("Volume to be detached was null", volume);
                getSupport().detach(testVolume.getProviderVolumeId());
                fail("The specified volume was not attached but the detach operation succeeded");
            }
            catch( CloudException expected ) {
                out("Received error detaching unattached volume (OK)");
            }
        }
        else {
            out("Virtual machine services not supported (OK)");
        }
    }

    @Test
    public void testRemoveVolume() throws InternalException, CloudException {
        if( !getSupport().isSubscribed() ) {
            out("Warning: Not subscribed, test will not run");
            return;
        }
        getSupport().remove(testVolume.getProviderVolumeId());
        out("Removed: " + testVolume.getProviderVolumeId());
        Volume volume = getSupport().getVolume(testVolume.getProviderVolumeId());

        Assert.assertTrue("Test volume still exists", volume == null || VolumeState.DELETED.equals(volume.getCurrentState()));
    }

    @Test
    public void testFilter() throws InternalException, CloudException {
        if( !getSupport().isSubscribed() ) {
            out("Warning: Not subscribed, test will not run");
            return;
        }
        boolean found = false;

        for( Volume volume : getSupport().listVolumes(VolumeFilterOptions.getInstance().attachedTo(testVm.getProviderVirtualMachineId())) ) {
            out("Volume: " + volume.getProviderVolumeId() + " -> " + volume.getProviderVirtualMachineId());
            Assert.assertEquals("Volume attachment returned from cloud provider does not match target virtual machine", testVm.getProviderVirtualMachineId(), volume.getProviderVirtualMachineId());
            found = true;
        }
        Assert.assertTrue("Did not find the test volume among the mounted volumes", found);
    }
}
