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
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.Platform;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;

public class MachineImageTestCase extends BaseTestCase {
    static public final String T_GET_IMAGE     = "testGetImage";
    static public final String T_IMAGE_CONTENT = "testImageContent";
    static public final String T_LIST_SHARES   = "testListShares";

    @Rule
    public TestName testName         = new TestName();

    private String        imageToDelete;
    private CloudProvider provider;
    private MachineImage  testImage;

    public MachineImageTestCase(String name) { super(name); }

    public @Nonnull MachineImageSupport getSupport() {
        if( provider == null ) {
            Assert.fail("No provider configuration set up");
        }
        ComputeServices services = provider.getComputeServices();

        if( services == null ) {
            Assert.fail("Cloud does not have compute services");
        }
        MachineImageSupport support = services.getImageSupport();

        if( support == null ) {
            Assert.fail("No machine image support in this cloud");
        }
        return support;
    }

    @Before
    @Override
    public void setUp() throws CloudException, InternalException, InstantiationException, IllegalAccessException, IOException {
        begin();
        provider = getProvider();
        provider.connect(getTestContext());

        MachineImageSupport support = getSupport();

        if( getName().equals(T_IMAGE_CONTENT) || getName().equals(T_GET_IMAGE) ) {
            Iterator<MachineImage> images = support.listImages(ImageClass.MACHINE).iterator();

            if( images.hasNext() ) {
                testImage = images.next();
            }
            if( testImage == null ) {
                for( Platform platform : new Platform[] { Platform.UBUNTU, Platform.WINDOWS, Platform.RHEL, Platform.CENT_OS, Platform.SOLARIS } ) {
                    images = support.searchPublicImages(null, platform, null).iterator();
                    if( images.hasNext() ) {
                        testImage = images.next();
                        break;
                    }
                }
                if( testImage == null ) {
                    Assert.fail("Unable to test image content due to a lack of machine images");
                }
            }
        }
        else if( getName().equals(T_LIST_SHARES) ) {
            // TODO: create an image
            // TODO: share it
        }
    }

    @After
    @Override
    public void tearDown() {
        try {
            MachineImageSupport support = getSupport();

            if( imageToDelete != null ) {
                try {
                    support.remove(imageToDelete);
                }
                catch( Throwable t ) {
                    out("WARNING: Error removing temporarily created machine image during tear down: " + t.getMessage());
                }
            }
            testImage = null;
        }
        finally {
            end();
        }
    }

    @Test
    public void testMetaData() throws CloudException, InternalException {
        MachineImageSupport support = getSupport();
        Iterable<ImageClass> imageClasses = support.listSupportedImageClasses();

        //noinspection ConstantConditions
        if( imageClasses != null ) {
            for( ImageClass cls : imageClasses ) {
                out("Image term (" + cls + "):     " + support.getProviderTermForImage(Locale.getDefault(), cls) );
            }
        }
        out("Subscribed:                   " + support.isSubscribed());
        out("Supported image classes:      " + imageClasses);
        out("Supported image types:        " + support.listSupportedImageTypes());
        out("Supported image formats:      " + support.listSupportedFormats());
        out("Supported bundling formats:   " + support.listSupportedFormatsForBundling());
        out("Local bundling:               " + support.identifyLocalBundlingRequirement());
        out("Image sharing:                " + support.supportsImageSharing());
        out("Public image sharing:         " + support.supportsImageSharingWithPublic());

        Assert.assertNotNull("Image classes cannot be null", imageClasses);
        Assert.assertNotNull("Image types cannot be null", support.listSupportedImageTypes());
        Assert.assertNotNull("Image formats may not be null", support.listSupportedFormats());
        Assert.assertNotNull("Image bundling formats may not be null", support.listSupportedFormatsForBundling());
        Assert.assertNotNull("Local bundling requirement may not be null", support.identifyLocalBundlingRequirement());
        for( ImageClass cls : imageClasses ) {
            out("Public " + cls + " library:   " + support.supportsPublicLibrary(cls));
        }
        for( MachineImageType type : support.listSupportedImageTypes() ) {
            out("Image capture supported (" + type + "): " + support.supportsImageCapture(type));
        }
    }

    @Test
    public void testListPrivateLibrary() throws CloudException, InternalException {
        MachineImageSupport support = getSupport();
        boolean found = false;

        for( ImageClass cls : support.listSupportedImageClasses() ) {
            Iterable<MachineImage> images = support.listImages(cls);

            for( MachineImage img : images ) {
                out(cls + " image: " + img);
                found = true;
            }
        }
        if( !found ) {
            out("No machine images were found so this test may not actually be valid");
        }
    }

    private boolean search(@Nonnull MachineImageSupport support, @Nonnull Platform platform) throws CloudException, InternalException {
        Iterable<MachineImage> images = support.searchPublicImages(null, platform, null);
        boolean found = false;

        for( MachineImage image : images ) {
            out("Match: " + image);
            found = true;
        }
        return found;
    }

    @Test
    public void testSearchPublicLibrary() throws CloudException, InternalException {
        MachineImageSupport support = getSupport();
        boolean found = false;

        for( Platform platform : new Platform[] { Platform.UBUNTU, Platform.WINDOWS, Platform.RHEL, Platform.CENT_OS, Platform.SOLARIS } ) {
            if( search(support, platform) ) {
                found = true;
                break;
            }
        }
        if( !found ) {
            out("No machine images were found so this test may not actually be valid");
        }
    }

    @Test
    public void testImageContent() throws CloudException, InternalException {
        out("ID:             " + testImage.getProviderMachineImageId());
        out("Current state:  " + testImage.getCurrentState());
        out("Owner:          " + testImage.getProviderOwnerId());
        out("Region:         " + testImage.getProviderRegionId());
        out("Name:           " + testImage.getName());
        out("Architecture:   " + testImage.getArchitecture());
        out("Platform:       " + testImage.getPlatform());
        out("Public:         " + getSupport().isImageSharedWithPublic(testImage.getProviderMachineImageId()));
        out("Image class:    " + testImage.getImageClass());
        out("Image type:     " + testImage.getType());
        out("Storage format: " + testImage.getStorageFormat());
        out("Software:       " + testImage.getSoftware());
        out("Description:    " + testImage.getDescription());

        Assert.assertNotNull("Machine image ID may not be null", testImage.getProviderMachineImageId());
        Assert.assertNotNull("Current state may not be null", testImage.getCurrentState());
        Assert.assertNotNull("Owner may not be null", testImage.getProviderOwnerId());
        Assert.assertNotNull("Region may not be null", testImage.getProviderRegionId());
        //noinspection ConstantConditions
        Assert.assertEquals("The region does not match the test region", provider.getContext().getRegionId(), testImage.getProviderRegionId());
        Assert.assertNotNull("Name may not be null", testImage.getName());
        Assert.assertNotNull("Architecture may not be null", testImage.getArchitecture());
        Assert.assertNotNull("Platform may not be null", testImage.getPlatform());
        Assert.assertNotNull("Image class may not be null", testImage.getImageClass());
        Assert.assertNotNull("Image type may not be null", testImage.getType());
        if( MachineImageType.STORAGE.equals(testImage.getType()) ) {
            Assert.assertNotNull("Images of type STORAGE must have a storage format", testImage.getStorageFormat());
        }
        else {
            Assert.assertNull("Images of type VOLUME must not have a storage format", testImage.getStorageFormat());
        }
        Assert.assertNotNull("Software must not be null. If there is no bundled software, it should be an empty string", testImage.getSoftware());
        Assert.assertNotNull("Description may not be null", testImage.getDescription());
    }

    @Test
    public void testGetImage() throws CloudException, InternalException {
        MachineImage image = getSupport().getImage(testImage.getProviderMachineImageId());

        out("Image: " + image);
        Assert.assertNotNull("Did not find the test machine image", image);
        Assert.assertEquals("Image IDs do not match", testImage.getProviderMachineImageId(), image.getProviderMachineImageId());
    }

    @Test
    public void testGetBogusImage() throws CloudException, InternalException {
        String id = UUID.randomUUID().toString();
        MachineImage image = getSupport().getImage(id);

        out("Bogus image: " + image);
        Assert.assertNull("Found a bogus machine image for bogus ID '" + id + "'", image);
    }

    @Test
    public void testListShares() throws CloudException, InternalException {
        Iterable<String> shares = getSupport().listShares(testImage.getProviderMachineImageId());

        out("Image shares: " + shares);
        Assert.assertNotNull("Image shares may not be null", shares);
    }

    @Test
    public void testCaptureImage() throws CloudException, InternalException {
        // TODO: test capturing an image
    }

    @Test
    public void testCaptureImageAsync() throws CloudException, InternalException {
        // TODO: test async capture
    }

    @Test
    public void testBundleVirtualMachine() throws CloudException, InternalException {
        // TODO: bundle virtual machine
    }

    @Test
    public void testBundleVirtualMachineAsync() throws CloudException, InternalException {
        // TODO: bundle virtual machine
    }

    @Test
    public void testRegisterBundle() throws CloudException, InternalException {
        // TODO: test register bundle
    }

    @Test
    public void testAddPrivateShare() throws CloudException, InternalException {
        // TODO: test add share
    }

    @Test
    public void testRemovePrivateShare() throws CloudException, InternalException {
        // TODO: remove private share
    }

    @Test
    public void testAddPublicShare() throws CloudException, InternalException {
        // TODO: add public share
    }

    @Test
    public void testRemovePublicShare() throws CloudException, InternalException {
        // TODO: remove public share
    }

    @Test
    public void testRemoveAllShares() throws CloudException, InternalException {
        // TODO: test remove all shares
    }

    /*
    private String        serverToKill    = null;

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
    */
}
