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

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.util.CalendarWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
            
            while( !server.getCurrentState().equals(VmState.RUNNING) ) {
                try { Thread.sleep(15000L); }
                catch( InterruptedException e ) { }
                server = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverToKill);
            }
            if( name.equals("testDetachVolume") ) {
                String device = null;
                
                for( String id : cloud.getComputeServices().getVolumeSupport().listPossibleDeviceIds(server.getPlatform()) ) {
                    device = id;
                    break;
                }
                cloud.getComputeServices().getVolumeSupport().attach(testVolume, serverToKill, device);
                while( true ) {
                    try { Thread.sleep(5000L); }
                    catch( InterruptedException e ) { }
                    Volume volume = cloud.getComputeServices().getVolumeSupport().getVolume(testVolume);
                    
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
            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 10L);
            
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
    public void testCreateVolume() throws InternalException, CloudException {
        begin();
        volumeToDelete = cloud.getComputeServices().getVolumeSupport().create(null, 5, getTestDataCenterId());
        assertNotNull("No volume created", volumeToDelete);
        end();
    }
    
    @Test
    public void testDetachVolume() throws InternalException, CloudException {
        begin();
        if( cloud.getComputeServices().hasVirtualMachineSupport() ) {
            cloud.getComputeServices().getVolumeSupport().detach(testVolume);
            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 10L);
            
            while( timeout > System.currentTimeMillis() ) {
                Volume volume = cloud.getComputeServices().getVolumeSupport().getVolume(testVolume);
                
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
    public void testMetaData() throws InternalException, CloudException {
        begin();
        VolumeSupport support = cloud.getComputeServices().getVolumeSupport();
        
        String term = support.getProviderTermForVolume(Locale.getDefault());
        
        assertNotNull("You must specify the provider term for volume", term);        
        out("Term for Volume: " + term);
        
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
    
    @Test
    public void testSubscription() throws CloudException, InternalException {
        begin();
        assertTrue("Cannot run tests when not susbcribed", cloud.getComputeServices().getVolumeSupport().isSubscribed());
        end();
    }
    
    @Test
    public void testVolumeContent() throws InternalException, CloudException {
        begin();
        Volume volume = cloud.getComputeServices().getVolumeSupport().getVolume(testVolume);
        
        assertNotNull("Volume was not found", volume);
        assertEquals("Volume ID does not match searched ID", testVolume, volume.getProviderVolumeId());
        assertNotNull("Volume must have a name", volume.getName());
        assertNotNull("Volume must have a state", volume.getCurrentState());
        assertEquals("Volume region does not match search", cloud.getContext().getRegionId(), volume.getProviderRegionId());
        assertNotNull("Volume must have a data center ID", volume.getProviderDataCenterId());
        assertTrue("Volume must have a size", volume.getSizeInGigabytes() > 0);
        try {
            out("Volume ID:      " + volume.getProviderVolumeId());
            out("State:          " + volume.getCurrentState());
            out("Name:           " + volume.getName());
            out("Region ID:      " + volume.getProviderRegionId());
            out("Data Center ID: " + volume.getProviderDataCenterId());
            out("Created:        " + new Date(volume.getCreationTimestamp()));
            out("Size (in GB):   " + volume.getSizeInGigabytes());
            out("From Snapshot:  " + volume.getProviderSnapshotId());
            out("Device ID:      " + volume.getDeviceId());
            out("Attachment:     " + volume.getProviderVirtualMachineId());
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }
}
