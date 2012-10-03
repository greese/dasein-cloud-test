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
import java.util.UUID;

import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.util.CalendarWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MachineImageTestCase extends BaseTestCase {
    private CloudProvider cloud           = null;
    private String        imageToDelete   = null;
    private String        serverToKill    = null;
    private String        testImage       = null;
    
    public MachineImageTestCase(String name) { super(name); }

    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        String name = getName();
        
        cloud = getProvider();
        cloud.connect(getTestContext());
        if( name.equals("testGetImage") || name.equals("testImageContent") ) {
            for( MachineImage image : cloud.getComputeServices().getImageSupport().listMachineImages() ) {
                if( image.getCurrentState().equals(MachineImageState.ACTIVE) ) {
                    testImage = image.getProviderMachineImageId();
                }
                break;
            }
            if( testImage == null ) {
                for( MachineImage image : cloud.getComputeServices().getImageSupport().listMachineImagesOwnedBy(null) ) {
                    testImage = image.getProviderMachineImageId();
                    break;
                }
            }
            if( testImage == null ) {
                throw new InternalException("There are no images in the cloud to test against.");
            }
        }
        if( cloud.getComputeServices().getImageSupport().supportsCustomImages() ) {
            if( name.equals("testCreateImageStandard") || name.equals("testDeleteImage") ) {
                serverToKill = launch(cloud, true);
                VirtualMachine vm = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverToKill);
                
                while( !vm.getCurrentState().equals(VmState.RUNNING) ) {
                    try { Thread.sleep(15000L); }
                    catch( InterruptedException e ) { }
                    vm = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverToKill);
                    if( vm == null || vm.getCurrentState().equals(VmState.TERMINATED) ) {
                        throw new CloudException("Virtual machine disappeared.");
                    }
                }
                if( !vm.isImagable() ) {
                    long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 20L);

                    System.out.println("Stopping server for imaging...");
                    cloud.getComputeServices().getVirtualMachineSupport().stop(vm.getProviderVirtualMachineId());
                    while( !vm.isImagable() ) {
                        if(  System.currentTimeMillis() >= timeout ) {
                            throw new CloudException("Server never stopped for imaging.");
                        }
                        try { Thread.sleep(15000L); }
                        catch( InterruptedException e ) { }
                        vm = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverToKill);
                        if( vm == null || vm.getCurrentState().equals(VmState.TERMINATED) ) {
                            throw new CloudException("Virtual machine disappeared.");
                        }
                    }
                }
            }
            if( name.equals("testDeleteImage") ) {
                AsynchronousTask<String> task = cloud.getComputeServices().getImageSupport().imageVirtualMachine(serverToKill, "dsn" + System.currentTimeMillis(), "Dasein Delete Test Image");
                
                while( !task.isComplete() ) {
                    try { Thread.sleep(15000L); }
                    catch( InterruptedException e ) { }
                }
                if( task.getTaskError() != null ) {
                    throw new CloudException(task.getTaskError());
                }
                imageToDelete = task.getResult();
                testImage = imageToDelete;
                MachineImage image = cloud.getComputeServices().getImageSupport().getMachineImage(imageToDelete);
                
                while( image.getCurrentState().equals(MachineImageState.PENDING) ) {
                    try { Thread.sleep(15000L); }
                    catch( InterruptedException e ) { }
                    image = cloud.getComputeServices().getImageSupport().getMachineImage(imageToDelete);
                }            
                // need to make sure the servers are killed before running the delete test (likely not really needed, but safe)
                VirtualMachine vm = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverToKill);
                long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*15L);
                
                if( vm != null && !vm.getCurrentState().equals(VmState.TERMINATED) ) {
                    cloud.getComputeServices().getVirtualMachineSupport().terminate(serverToKill);
                }
                while( vm != null && !vm.getCurrentState().equals(VmState.TERMINATED) ) {
                    if( System.currentTimeMillis() >= timeout ) {
                        break;
                    }
                    vm = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverToKill);
                }    
                serverToKill = null;
            }
        }
    }

    @After
    @Override
    public void tearDown() {
        try {
            if( serverToKill != null ) {
                cloud.getComputeServices().getVirtualMachineSupport().terminate(serverToKill);                
                long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*15L);
                VirtualMachine vm = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverToKill);
                
                // need to make sure the servers are killed before zapping any image created from them
                while( vm != null && !vm.getCurrentState().equals(VmState.TERMINATED) ) {
                    if( System.currentTimeMillis() >= timeout ) {
                        break;
                    }
                    vm = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(serverToKill);
                }
                serverToKill = null;                
            }
        }
        catch( Throwable ignore ) {
            // ignroe
        }
        try {
            if( imageToDelete != null ) {
                cloud.getComputeServices().getImageSupport().remove(imageToDelete);
                imageToDelete = null;
                testImage = null;
            }
        }
        catch( Throwable ignore ) {
            // ignroe
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
    public void testCreateImageStandard() throws Throwable {
        begin();
        if( cloud.getComputeServices().getImageSupport().supportsCustomImages() ) {
            AsynchronousTask<String> task = cloud.getComputeServices().getImageSupport().imageVirtualMachine(serverToKill, "dsn" + System.currentTimeMillis(), "Dasein Test Image");
            
            out(task);
            if( task.getTaskError() != null ) {
                throw task.getTaskError();
            }
            imageToDelete = task.getResult();
            MachineImage image = cloud.getComputeServices().getImageSupport().getMachineImage(imageToDelete);
            
            assertNotNull("No image was created", image);
            try {
                out("New Image: " + imageToDelete);
                while( image.getCurrentState().equals(MachineImageState.PENDING) ) {
                    try { Thread.sleep(15000L); }
                    catch( InterruptedException e ) { }
                    image = cloud.getComputeServices().getImageSupport().getMachineImage(imageToDelete);
                }
            }
            catch( Throwable notPartOfTest ) {
                // ignore
            }
            assertTrue("Image never enterered the available state", image.getCurrentState().equals(MachineImageState.ACTIVE));
        }
        end();
    }
    
    @Test
    public void testDeleteImage() throws CloudException, InternalException {
        begin();
        if( cloud.getComputeServices().getImageSupport().supportsCustomImages() ) {
            cloud.getComputeServices().getImageSupport().remove(imageToDelete);
            try { Thread.sleep(15000L); }
            catch( InterruptedException e ) { }
            MachineImage image = cloud.getComputeServices().getImageSupport().getMachineImage(imageToDelete);
            long timeout = System.currentTimeMillis() + CalendarWrapper.HOUR;
            
            while( image != null && !image.getCurrentState().equals(MachineImageState.DELETED) ) {
                if( System.currentTimeMillis() >= timeout ) {
                    throw new CloudException("Timeout waiting for delete");
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException e ) { }
                image = cloud.getComputeServices().getImageSupport().getMachineImage(imageToDelete);
            } 
        }
        end();
    }
    
    @Test
    public void testGetBogusImage() throws CloudException, InternalException {
        begin();
        MachineImage image = cloud.getComputeServices().getImageSupport().getMachineImage(UUID.randomUUID().toString());
        
        assertNull("Found a machine image matching the random ID", image);
        end();          
    }
    
    @Test 
    public void testGetImage() throws CloudException, InternalException {
        begin();
        MachineImage image = cloud.getComputeServices().getImageSupport().getMachineImage(testImage);
        
        assertNotNull("No machine image matching the test ID was found", image);
        assertEquals("The ID of the retrieved machine image does not match", testImage, image.getProviderMachineImageId());
        out("Image: " + image);
        end();        
    }
    
    @Test
    public void testImageContent() throws CloudException, InternalException {
        begin();
        MachineImage image = cloud.getComputeServices().getImageSupport().getMachineImage(testImage);
        
        assertNotNull("No machine image matching the test ID was found", image);
        assertEquals("The ID of the retrieved machine image does not match", testImage, image.getProviderMachineImageId());
        assertNotNull("A machine image must have a state", image.getCurrentState());
        assertNotNull("A machine image must have a name", image.getName());
        assertNotNull("A machine image must have a description", image.getDescription());
        assertNotNull("A machine image must have an architecture", image.getArchitecture());
        assertNotNull("A machine image must have a platform", image.getPlatform());
        assertNotNull("A machine image must have a type", image.getType());
        assertNotNull("A machine image must have a software definition, even if only an empty string", image.getSoftware());
        assertEquals("Region does not match", cloud.getContext().getRegionId(), image.getProviderRegionId());
        out("Image ID:          " + image.getProviderMachineImageId());
        out("State:             " + image.getCurrentState());
        out("Owner:             " + image.getProviderOwnerId());
        out("Region:            " + image.getProviderRegionId());
        out("Name:              " + image.getName());
        out("Architecture:      " + image.getArchitecture());
        out("Platform:          " + image.getPlatform());
        out("Software:          " + image.getSoftware());
        out("Type:              " + image.getType());
        end();         
    }
    
    @Test 
    public void testListImages() throws CloudException, InternalException {
        begin();
        Iterable<MachineImage> images = cloud.getComputeServices().getImageSupport().listMachineImages();

        assertNotNull("Machine image listing may not be null", images);
        try {
            for( MachineImage image : images ) {
                out("Image: " + image);
            }
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }        
        end();
    }
    
    @Test 
    public void testListPublicImages() throws CloudException, InternalException {
        begin();
        Iterable<MachineImage> images = cloud.getComputeServices().getImageSupport().listMachineImagesOwnedBy(null);

        assertNotNull("Machine image listing may not be null", images);
        try {
            for( MachineImage image : images ) {
                out("Image: " + image);
            }
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }        
        end();
    }
    
    @Test
    public void testMetaData() throws CloudException, InternalException {
        begin();
        MachineImageSupport imageSupport = cloud.getComputeServices().getImageSupport();
        
        assertNotNull("A provider term must exist for machine image", imageSupport.getProviderTermForImage(Locale.getDefault()));
        assertNotNull("A list (even empty) of supported image formats must be provided", imageSupport.listSupportedFormats());
        out("Provider term:  " + imageSupport.getProviderTermForImage(Locale.getDefault()));
        out("Public Library: " + imageSupport.hasPublicLibrary());
        
        StringBuilder str = new StringBuilder();
        for( MachineImageFormat fmt : imageSupport.listSupportedFormats() ) {
            str.append(fmt.name() + ",");
        }
        out("Format:         " + str);
        end();
    }
    
    @Test
    public void testShareImage() {
        if( cloud.getComputeServices().getImageSupport().supportsImageSharing() ) {
            // TODO: share
        }
    }
    
    @Test
    public void testSubscription() throws CloudException, InternalException {
        begin();
        MachineImageSupport imageSupport = cloud.getComputeServices().getImageSupport();
        
        assertTrue("No subscription exists for image services", imageSupport.isSubscribed());
        end();
    }
}
