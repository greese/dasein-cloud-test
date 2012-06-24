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

import java.util.Locale;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.Snapshot;
import org.dasein.cloud.compute.SnapshotState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SnapshotTestCase extends BaseTestCase {
    private CloudProvider cloud            = null;
    private String        snapshotToDelete = null;
    private String        testSnapshot     = null;
    private String        volumeToDelete   = null;
    
    public SnapshotTestCase(String name) { super(name); }
    
    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        String name = getName();
        
        cloud = getProvider();
        cloud.connect(getTestContext());
        if( name.equals("testRemoveSnapshot") ) {
            volumeToDelete = allocateVolume(cloud);
        }
        if( name.equals("testRemoveSnapshot") ) {
            snapshotToDelete = cloud.getComputeServices().getSnapshotSupport().create(volumeToDelete, "Dasein Test Volume");
            
            if( snapshotToDelete == null ) {
                String vm = launch(cloud);
                
                cloud.getComputeServices().getVolumeSupport().attach(volumeToDelete, vm, cloud.getComputeServices().getVolumeSupport().listPossibleDeviceIds(Platform.UNIX).iterator().next());
                try { Thread.sleep(30000L); }
                catch( InterruptedException e ) { }
                snapshotToDelete = cloud.getComputeServices().getSnapshotSupport().create(volumeToDelete, "Dasein Test Volume");
                if( snapshotToDelete == null ) {
                    throw new CloudException("Unable to create snapshot for testing purposes");
                }
            }
            Snapshot snapshot = cloud.getComputeServices().getSnapshotSupport().getSnapshot(snapshotToDelete);
            
            while( !snapshot.getCurrentState().equals(SnapshotState.AVAILABLE) ) {
                if( snapshot.getCurrentState().equals(SnapshotState.DELETED) ) {
                    throw new CloudException("Snapshot was never made");
                }
                try { Thread.sleep(5000L); }
                catch( InterruptedException e ) { }
                snapshot = cloud.getComputeServices().getSnapshotSupport().getSnapshot(snapshotToDelete);
            }
            testSnapshot = snapshotToDelete;
        }
    }
    
    @After
    @Override
    public void tearDown() {
        try {
            if( snapshotToDelete != null ) {
                cloud.getComputeServices().getSnapshotSupport().remove(snapshotToDelete);
                snapshotToDelete = null;
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( volumeToDelete != null ) {
                cloud.getComputeServices().getVolumeSupport().remove(volumeToDelete);
                volumeToDelete = null;
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
    public void testListSnapshots() throws InternalException, CloudException {
        begin();
        Iterable<Snapshot> snapshots = cloud.getComputeServices().getSnapshotSupport().listSnapshots();
        
        assertNotNull("Snapshots must not be null", snapshots);
        try {
            for( Snapshot snapshot : snapshots ) {
                out("Snapshot: " + snapshot);
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
        cloud.getComputeServices().getSnapshotSupport().supportsSnapshotSharing();
        cloud.getComputeServices().getSnapshotSupport().supportsSnapshotSharingWithPublic();
        
        String term = cloud.getComputeServices().getSnapshotSupport().getProviderTermForSnapshot(Locale.getDefault());
        
        assertNotNull("The term for snapshot must have a value", term);
        
        try {
            out("Term:           " + term);
            out("Sharing:        " + cloud.getComputeServices().getSnapshotSupport().supportsSnapshotSharing());
            out("Public Sharing: " + cloud.getComputeServices().getSnapshotSupport().supportsSnapshotSharingWithPublic());
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }
    
    @Test
    public void testRemoveSnapshot() throws InternalException, CloudException {
        begin();
        cloud.getComputeServices().getSnapshotSupport().remove(snapshotToDelete);
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        Snapshot snapshot = cloud.getComputeServices().getSnapshotSupport().getSnapshot(testSnapshot);
        
        assertTrue("Snapshot was not removed", snapshot == null || !snapshot.getCurrentState().equals(SnapshotState.AVAILABLE));
        snapshotToDelete = null;
        testSnapshot = null;
        end();
    }
    
    @Test
    public void testSubscription() throws InternalException, CloudException {
        begin();
        assertTrue("Tests will not work unless subscribed", cloud.getComputeServices().getSnapshotSupport().isSubscribed());
        out("Subscribed: true");
        end();
    }
}
