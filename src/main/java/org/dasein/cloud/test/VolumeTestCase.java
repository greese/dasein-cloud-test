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

import java.util.Collection;
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
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;

public class VolumeTestCase extends BaseTestCase {
    private CloudProvider cloud          = null;
    private String        serverToKill   = null;
    private String        testVolume     = null;
    private String        volumeToDelete = null;
    
    public VolumeTestCase(String name) { super(name); }
    
    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        String name = getName();
        
        cloud = getProvider();
        cloud.connect(getTestContext());
        if( name.equals("testVolumeContent") || name.equals("testGetVolume") ) {
            testVolume = null;
            for( Volume volume : cloud.getComputeServices().getVolumeSupport().listVolumes() ) {
                if( volume.getCurrentState().equals(VolumeState.AVAILABLE) ) {
                    testVolume = volume.getProviderVolumeId();
                }
            }
            if( testVolume == null ) {
                volumeToDelete = allocateVolume(cloud);
                testVolume = volumeToDelete;
            }
        }
        if( name.equals("testRemoveVolume") || name.equals("testAttachVolume") || name.equals("testDetachVolume") || name.equals("testAttachVolumeToNoServer") || name.equals("testDetachUnattachedVolume") ) {
            volumeToDelete = allocateVolume(cloud);
            testVolume = volumeToDelete;            
        }
        if( name.equals("testAttachVolume") || name.equals("testDetachVolume")) {
            serverToKill = launch(cloud);
            try { Thread.sleep(15000L); }
            catch( InterruptedException e ) { }
            VirtualMachine server = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverToKill);
            long timeout = System.currentTimeMillis() + getLaunchWindow();

            while( timeout > System.currentTimeMillis() ) {
                if( server.getCurrentState().equals(VmState.RUNNING) ) {
                    break;
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException e ) { }
                server = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverToKill);
                if( server == null ) {
                    throw new CloudException("Server has disappeared while waiting for it to be running");
                }
            }
            if( name.equals("testDetachVolume") ) {
                String device = null;
                
                for( String id : cloud.getComputeServices().getVolumeSupport().listPossibleDeviceIds(server.getPlatform()) ) {
                    device = id;
                    break;
                }
                cloud.getComputeServices().getVolumeSupport().attach(testVolume, serverToKill, device);
                timeout = System.currentTimeMillis() + getStateChangeWindow();
                while( timeout > System.currentTimeMillis() ) {
                    try { Thread.sleep(5000L); }
                    catch( InterruptedException e ) { }
                    Volume volume = cloud.getComputeServices().getVolumeSupport().getVolume(testVolume);

                    if( volume == null ) {
                        throw new CloudException("Volume went away while waiting for it to become available");
                    }
                    if( !volume.getCurrentState().equals(VolumeState.PENDING) ) {
                        break;
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
                if( serverToKill != null ) {
                    Volume volume = cloud.getComputeServices().getVolumeSupport().getVolume(volumeToDelete);
                    long timeout = System.currentTimeMillis() + getStateChangeWindow();

                    while( timeout > System.currentTimeMillis() ) {
                        if( volume == null || volume.getCurrentState().equals(VolumeState.AVAILABLE) ) {
                            break;
                        }
                        try { Thread.sleep(5000L); }
                        catch( InterruptedException e ) { }
                        volume = cloud.getComputeServices().getVolumeSupport().getVolume(volumeToDelete);
                    }
                    try { cloud.getComputeServices().getVolumeSupport().detach(volumeToDelete); }
                    catch( Throwable ignore ) { }
                }
                cloud.getComputeServices().getVolumeSupport().remove(volumeToDelete);
                volumeToDelete = null;
                testVolume = null;
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( serverToKill != null ) {
                cloud.getComputeServices().getVirtualMachineSupport().terminate(serverToKill);
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( cloud != null ) {
                cloud.close();
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
    }

    private @Nonnull VolumeSupport getSupport() {
        ComputeServices services = cloud.getComputeServices();

        assertNotNull("No compute services are defined in this cloud", services);

        VolumeSupport support = services.getVolumeSupport();

        assertNotNull("No volume support is defined in this cloud", services);
        return support;
    }

    @Test
    public void testSubscription() throws CloudException, InternalException {
        begin();
        try {
            out("Subscribed: " + getSupport().isSubscribed());
        }
        finally {
            end();
        }
    }

    @Test
    public void testMetaData() throws InternalException, CloudException {
        begin();
        try {
            VolumeSupport support = getSupport();

            String term = support.getProviderTermForVolume(Locale.getDefault());

            out("Term for Volume:         " + term);
            assertNotNull("You must specify the provider term for volume", term);

            out("Minimum volume size:     " + support.getMinimumVolumeSize());
            out("Maximum volume size:     " + support.getMaximumVolumeSize());
            out("Size defined by product: " + support.isVolumeSizeDeterminedByProduct());
            out("Product required:        " + support.getVolumeProductRequirement());
            out("Max allocated volumes:   " + (support.getMaximumVolumeCount() == -2 ? "unknown" : support.getMaximumVolumeCount()));

            Iterable<String> devices = support.listPossibleDeviceIds(Platform.UNIX);

            assertNotNull("You must specify device IDs to match " + Platform.UNIX, devices);
            boolean hasOne = false;
            out("Devices for " + Platform.UNIX + ":");
            for( String id : devices ) {
                out("\t" + id);
                hasOne = true;
            }
            assertTrue("You must have at least one device ID for " + Platform.UNIX, hasOne);

            devices = support.listPossibleDeviceIds(Platform.WINDOWS);

            assertNotNull("You must specify device IDs to match " + Platform.WINDOWS, devices);
            hasOne = false;
            out("Devices for " + Platform.WINDOWS + ":");
            for( String id : devices ) {
                out("\t" + id);
                hasOne = true;
            }
            assertTrue("You must have at least one device ID for " + Platform.WINDOWS, hasOne);
        }
        finally {
            end();
        }
    }

    @Test
    public void testListProducts() throws InternalException, CloudException {
        begin();
        try {
            for( VolumeProduct product : getSupport().listVolumeProducts() ) {
                out("Product: " + product.getName() + " [" + product.getProviderProductId() + "]");
            }
        }
        finally {
            end();
        }
    }

    @Test
    public void testGetProduct() throws InternalException, CloudException {
        begin();
        try {
            Iterator<VolumeProduct> products = getSupport().listVolumeProducts().iterator();

            if( products.hasNext() ) {
                VolumeProduct prd = products.next();

                out("ID:              " + prd.getProviderProductId());
                out("Name:            " + prd.getName());
                out("Type:            " + prd.getType());
                out("Size:            " + prd.getVolumeSize());
                out("Min IOPS:        " + prd.getMinIops());
                out("Max IOPS:        " + prd.getMaxIops());
                out("Currency:        " + prd.getCurrency());
                out("Storage Cost:    " + prd.getMonthlyGigabyteCost());
                out("IOPS Cost:       " + prd.getIopsCost());
                out("Description:     " + prd.getDescription());
                assertNotNull("ID is null", prd.getProviderProductId());
                assertNotNull("Name is null", prd.getName());
                assertNotNull("Description is null", prd.getDescription());
                assertNotNull("Type is null", prd.getType());
                assertTrue("Min IOPS is negative", prd.getMinIops() > -1);
                assertTrue("Max IOPS is negative", prd.getMaxIops() > -1);
                assertTrue("Max IOPS is less than min", prd.getMaxIops() >= prd.getMinIops());
            }
        }
        finally {
            end();
        }
    }

    @Test
    public void testCreateVolume() throws InternalException, CloudException {
        begin();
        try {
            Storage<Gigabyte> size = getSupport().getMinimumVolumeSize();
            String name = "dsn" + System.currentTimeMillis();
            VolumeCreateOptions options;
            VolumeProduct prd = null;
            int iops = 0;

            for( VolumeProduct p : getSupport().listVolumeProducts() ) {
                prd = p;
            }
            if( prd == null ) {
                assertFalse("A product is required to create a volume, but no products are provided", getSupport().getVolumeProductRequirement().equals(Requirement.REQUIRED));
                options = VolumeCreateOptions.getInstance(size, name, name);
            }
            else {
                iops = (prd.getMinIops() > 0 ? prd.getMinIops() : (prd.getMaxIops() > 0 ? 1 : 0));
                if( getSupport().isVolumeSizeDeterminedByProduct() ) {
                    Storage<Gigabyte> s = prd.getVolumeSize();

                    if( s != null && s.getQuantity().intValue() > 0 ) {
                        size = s;
                    }
                }
                options = VolumeCreateOptions.getInstance(prd.getProviderProductId(), size, name, name, iops);
            }
            volumeToDelete = getSupport().createVolume(options);
            out("Created: " + volumeToDelete);
            assertNotNull("No volume created", volumeToDelete);
            assertNotNull("Could not find volume after created", getSupport().getVolume(volumeToDelete));
        }
        finally {
            end();
        }
    }

    @Test
    public void testAttachVolume() throws InternalException, CloudException {
        begin();
        if( cloud.getComputeServices().hasVirtualMachineSupport() ) {
            VirtualMachine server = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverToKill);
            String device = null;

            for( String id : cloud.getComputeServices().getVolumeSupport().listPossibleDeviceIds(server.getPlatform()) ) {
                device = id;
                break;
            }
            cloud.getComputeServices().getVolumeSupport().attach(testVolume, serverToKill, device);
            long timeout = System.currentTimeMillis() + getStateChangeWindow();

            while( timeout > System.currentTimeMillis() ) {
                Volume volume = cloud.getComputeServices().getVolumeSupport().getVolume(testVolume);

                if( volume.getProviderVirtualMachineId() != null ) {
                    assertEquals("Volume attachment does not match target server", serverToKill, volume.getProviderVirtualMachineId());
                    end();
                    return;
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException e ) { }
            }
            fail("System timed out verifying attachment");
        }
        end();
    }


    @Test
    public void testAttachVolumeToNoServer() throws InternalException, CloudException {
        begin();
        if( cloud.getComputeServices().hasVirtualMachineSupport() ) {
            String device = null;
            
            for( String id : cloud.getComputeServices().getVolumeSupport().listPossibleDeviceIds(Platform.UNIX) ) {
                device = id;
                break;
            }
            try {
                cloud.getComputeServices().getVolumeSupport().attach(testVolume, UUID.randomUUID().toString(), device);
            }
            catch( CloudException expected ) {
                end();
                return;
            }
            fail("System did not error when attempting to attach to a fake server");
        }
        end();
    }
    
    @Test
    public void testDetachVolume() throws InternalException, CloudException {
        begin();
        if( cloud.getComputeServices().hasVirtualMachineSupport() ) {
            Volume volume = cloud.getComputeServices().getVolumeSupport().getVolume(testVolume);

            while( volume != null && !volume.getCurrentState().equals(VolumeState.AVAILABLE) ) {
                try { Thread.sleep(5000L); }
                catch( InterruptedException e ) { }
                volume = cloud.getComputeServices().getVolumeSupport().getVolume(testVolume);
            }
            assertNotNull("Volume to be detached was null", volume);
            cloud.getComputeServices().getVolumeSupport().detach(testVolume);
            long timeout = System.currentTimeMillis() + getStateChangeWindow();
            
            while( timeout > System.currentTimeMillis() ) {
                volume = cloud.getComputeServices().getVolumeSupport().getVolume(testVolume);
                if( volume.getProviderVirtualMachineId() == null ) {
                    end();
                    return;
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException e ) { }
            }
            fail("System timed out verifying detachment");
        }
        end();
    }
    
    @Test
    public void testDetachUnattachedVolume() throws InternalException, CloudException {
        begin();
        if( cloud.getComputeServices().hasVirtualMachineSupport() ) {
            try {
                cloud.getComputeServices().getVolumeSupport().detach(testVolume);
                fail("The specified volume was not attached but the detach operation succeeded");
            }
            catch( CloudException expected ) {
                // expected
            }
        }
        end();
    }
    
    @Test
    public void testGetBogusVolume() throws InternalException, CloudException {
        begin();
        Volume v = cloud.getComputeServices().getVolumeSupport().getVolume(UUID.randomUUID().toString());
        
        assertNull("Found a volume for bogus ID", v);
        end();
    }
    
    @Test
    public void testGetVolume() throws InternalException, CloudException {
        begin();
        Volume v = cloud.getComputeServices().getVolumeSupport().getVolume(testVolume);
        
        assertNotNull("Could not find target volume " + testVolume, v);
        assertEquals("Target volume did not match query", testVolume, v.getProviderVolumeId());
        end();
    }
    
    @Test
    public void testListVolumes() throws InternalException, CloudException {
        begin();
        Iterable<Volume> volumes = cloud.getComputeServices().getVolumeSupport().listVolumes();
        
        assertNotNull("Volume list cannot be null", volumes);
        try {
            for( Volume volume : volumes ) {
                out("Volume: " + volume);
            }
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }
    
    @Test
    public void testVolumeContent() throws InternalException, CloudException {
        begin();
        Volume volume = getSupport().getVolume(testVolume);
        
        assertNotNull("Volume was not found", volume);
        assertEquals("Volume ID does not match searched ID", testVolume, volume.getProviderVolumeId());
        assertNotNull("Volume must have a name", volume.getName());
        assertNotNull("Volume must have a state", volume.getCurrentState());
        assertEquals("Volume region does not match search", cloud.getContext().getRegionId(), volume.getProviderRegionId());
        assertNotNull("Volume must have a data center ID", volume.getProviderDataCenterId());
        assertTrue("Volume must have a size", volume.getSizeInGigabytes() > 0);
        try {
            out("ID:             " + volume.getProviderVolumeId());
            out("State:          " + volume.getCurrentState());
            out("Name:           " + volume.getName());
            out("Region ID:      " + volume.getProviderRegionId());
            out("Data Center ID: " + volume.getProviderDataCenterId());
            out("Product ID:     " + volume.getProviderProductId());
            out("Created:        " + new Date(volume.getCreationTimestamp()));
            out("Size (in GB):   " + volume.getSizeInGigabytes());
            out("IOPS:           " + volume.getIops());
            out("From Snapshot:  " + volume.getProviderSnapshotId());
            out("Device ID:      " + volume.getDeviceId());
            out("Attachment:     " + volume.getProviderVirtualMachineId());
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }


    @Test
    public void testRemoveVolume() throws InternalException, CloudException {
        begin();
        cloud.getComputeServices().getVolumeSupport().remove(volumeToDelete);
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        Volume v = cloud.getComputeServices().getVolumeSupport().getVolume(volumeToDelete);

        assertTrue("Volume is still active", v == null || !v.getCurrentState().equals(VolumeState.AVAILABLE));
        end();
    }
}
