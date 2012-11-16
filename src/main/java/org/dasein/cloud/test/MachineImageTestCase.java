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

import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.storage.BlobStoreSupport;
import org.dasein.cloud.storage.StorageServices;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;

@SuppressWarnings("JUnit4AnnotatedMethodInJUnit3TestCase")
public class MachineImageTestCase extends BaseTestCase {
    static public final String T_ADD_PRIVATE_SHARE   = "testAddPrivateShare";
    static public final String T_ADD_PUBLIC_SHARE    = "testAddPublicShare";
    static public final String T_BUNDLE_VM           = "testBundleVirtualMachine";
    static public final String T_BUNDLE_VM_ASYNC     = "testBundleVirtualMachineAsync";
    static public final String T_CAPTURE_IMAGE       = "testCaptureImage";
    static public final String T_CAPTURE_IMAGE_ASYNC = "testCaptureImageAsync";
    static public final String T_GET_IMAGE           = "testGetImage";
    static public final String T_IMAGE_CONTENT       = "testImageContent";
    static public final String T_LIST_SHARES         = "testListShares";
    static public final String T_REGISTER            = "testRegisterBundle";
    static public final String T_RM_ALL_SHARES       = "testRemoveAllShares";
    static public final String T_RM_PRIVATE_SHARE    = "testRemovePrivateShare";
    static public final String T_RM_PUBLIC_SHARE     = "testRemovePublicShare";

    static public final int IMAGE_REUSE_COUNT = 6;
    static public final int VM_REUSE_COUNT    = 11;

    @Rule
    public TestName testName         = new TestName();

    private boolean            canTestBundling;
    private MachineImageFormat bundlingFormat;
    private String             killImageId;
    private CloudProvider      provider;
    private String             testBucket;
    private String             testObject;
    private MachineImage       testImage;
    private String             testLocation;
    private VirtualMachine     testVm;

    public MachineImageTestCase(String name) { super(name); }

    @Override
    public int getImageReuseCount() {
        return IMAGE_REUSE_COUNT;
    }

    @Override
    public int getVmReuseCount() {
        return VM_REUSE_COUNT;
    }

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

    public @Nonnull VirtualMachineSupport getVMSupport() {
        if( provider == null ) {
            Assert.fail("No provider configuration set up");
        }
        ComputeServices services = provider.getComputeServices();

        if( services == null ) {
            Assert.fail("Cloud does not have compute services");
        }
        VirtualMachineSupport support = services.getVirtualMachineSupport();

        if( support == null ) {
            Assert.fail("No VM support in this cloud");
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
        else if( getName().equals(T_LIST_SHARES) || getName().equals(T_RM_PRIVATE_SHARE) || getName().equals(T_RM_ALL_SHARES) ) {
            testImage = findTestImage(provider, support, false, false, true);
            if( support.supportsImageSharing() ) {
                String shareAccount = getTestShareAccount();

                Assert.assertNotNull("Cannot test image sharing unless the test.shareAccount property is set to a second account", shareAccount);
                support.addImageShare(testImage.getProviderMachineImageId(), shareAccount);
                Assert.assertTrue("Test account was not found among the active shares", support.listShares(testImage.getProviderMachineImageId()).iterator().hasNext());
            }
        }
        else if( getName().equals(T_CAPTURE_IMAGE) || getName().equals(T_CAPTURE_IMAGE_ASYNC) ) {
            VirtualMachineSupport vmSupport = getVMSupport();

            testVm = findTestVirtualMachine(provider, vmSupport, false, true);
        }
        else if( getName().equals(T_ADD_PRIVATE_SHARE) || getName().equals(T_ADD_PUBLIC_SHARE) || getName().equals(T_RM_PUBLIC_SHARE) ) {
            testImage = findTestImage(provider, support, false, false, true);
            if( getName().equals(T_RM_PUBLIC_SHARE) && support.supportsImageSharingWithPublic() ) {
                support.addPublicShare(testImage.getProviderMachineImageId());
                Assert.assertTrue("Image was not properly publicly shared", support.isImageSharedWithPublic(testImage.getProviderMachineImageId()));
            }
        }
        else if( getName().equals(T_BUNDLE_VM) || getName().equals(T_BUNDLE_VM_ASYNC) || getName().equals(T_REGISTER) ) {
            VirtualMachineSupport vmSupport = getVMSupport();

            testVm = findTestVirtualMachine(provider, vmSupport, false, true);
            if( Requirement.REQUIRED.equals(support.identifyLocalBundlingRequirement()) ) {
                canTestBundling = false;
                out("WARNING: Cannot test any bundling because bundling must occur locally - NOT TESTED");
            }
            else {
                Iterable<MachineImageFormat> formats = support.listSupportedFormatsForBundling();

                if( !formats.iterator().hasNext() ) {
                    out("Image bundling is not supported; will expect an error during this test");
                }
                else {
                    String id = testVm.getProviderMachineImageId();

                    if( id != null ) {
                        MachineImage img = support.getImage(id);

                        if( img != null ) {
                            for( MachineImageFormat format : formats ) {
                                if( format.equals(img.getStorageFormat()) ) {
                                    bundlingFormat = format;
                                    canTestBundling = true;
                                    break;
                                }
                            }

                        }
                    }
                    if( !canTestBundling ) {
                        out("WARNING: The image format of the test VM cannot be bundled - NOT TESTED");
                    }
                }
                if( canTestBundling ) {
                    StorageServices services = provider.getStorageServices();

                    if( services == null ) {
                        Assert.fail("Cannot bundle without storage services");
                    }
                    BlobStoreSupport blobSupport = services.getBlobStoreSupport();

                    if( blobSupport == null ) {
                        Assert.fail("Cannot bundle without an object store");
                    }
                    testBucket = blobSupport.createBucket("dsnimg" + (System.currentTimeMillis()%10000), true).getBucketName();
                    Assert.assertNotNull("Test bucket is null", testBucket);
                    testObject = getName();
                    if( getName().equals(T_REGISTER) ) {
                        testLocation = getSupport().bundleVirtualMachine(testVm.getProviderVirtualMachineId(), bundlingFormat, testBucket, testObject);
                    }
                }
            }
        }
    }

    @After
    @Override
    public void tearDown() {
        try {
            if( testBucket != null ) {
                try {
                    provider.getStorageServices().getBlobStoreSupport().clearBucket(testBucket);
                }
                catch( Throwable t ) {
                    out("WARNING: Unable to clear test bucket " + testBucket + ": " + t.getMessage());
                }
            }
            if( testImage != null && (getName().equals(T_ADD_PRIVATE_SHARE) || getName().equals(T_LIST_SHARES)) ) {
                try {
                    if( getSupport().supportsImageSharing() ) {
                        getSupport().removeAllImageShares(testImage.getProviderMachineImageId());
                    }
                }
                catch( Throwable t ) {
                    out("WARNING: Unable to clean up shares for " + testImage.getProviderMachineImageId() + ": " + t.getMessage());
                }
            }
            if( testImage != null && getName().equals(T_ADD_PUBLIC_SHARE) ) {
                try {
                    if( getSupport().supportsImageSharingWithPublic() ) {
                        getSupport().removePublicShare(testImage.getProviderMachineImageId());
                    }
                }
                catch( Throwable t ) {
                    out("WARNING: Unable to clean up public sharing for " + testImage.getProviderMachineImageId() + ": " + t.getMessage());
                }
            }
            cleanUp();
            if( killImageId != null ) {
                cleanImage(getSupport(), killImageId);
                killImageId = null;
            }
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
        boolean supported = false;
        String id = testVm.getProviderMachineImageId();
        MachineImage tmp = null;

        if( id != null ) {
            tmp = getSupport().getImage(id);
        }
        if( tmp == null ) {
            for( MachineImageType t : getSupport().listSupportedImageTypes() ) {
                if( getSupport().supportsImageCapture(t) ) {
                    supported = true;
                    break;
                }
            }
        }
        else {
            supported = getSupport().supportsImageCapture(tmp.getType());
        }
        try {
            String name = getClass().getName().substring(0, 3).toLowerCase() + "img-" + getName() + (System.currentTimeMillis()%10000);
            ImageCreateOptions options = ImageCreateOptions.getInstance(testVm, name, getName() + " test case execution");
            MachineImage image = getSupport().captureImage(options);

            Assert.assertTrue("Image capture is not supported, yet the capture attempt succeeded without error", supported);
            out("Captured: " + image);
            Assert.assertNotNull("Created a null machine image", image);
            killImageId = image.getProviderMachineImageId();
            Assert.assertFalse("Machine image is not in a proper state", MachineImageState.DELETED.equals(image.getCurrentState()));
        }
        catch( OperationNotSupportedException e ) {
            out("Capture not supported " + (supported ? "(ERROR)" : "(OK)"));
            Assert.assertFalse("Notified that capture is not supported, but it is supposed to be according to meta-data", supported);
        }
    }

    @Test
    public void testCaptureImageAsync() throws CloudException, InternalException {
        boolean supported = false;
        String id = testVm.getProviderMachineImageId();
        MachineImage tmp = null;

        if( id != null ) {
            tmp = getSupport().getImage(id);
        }
        if( tmp == null ) {
            for( MachineImageType t : getSupport().listSupportedImageTypes() ) {
                if( getSupport().supportsImageCapture(t) ) {
                    supported = true;
                    break;
                }
            }
        }
        else {
            supported = getSupport().supportsImageCapture(tmp.getType());
        }
        String name = getClass().getName().substring(0, 3).toLowerCase() + "img-" + getName() + (System.currentTimeMillis()%10000);
        ImageCreateOptions options = ImageCreateOptions.getInstance(testVm, name, getName() + " test case execution");
        AsynchronousTask<MachineImage> task = new AsynchronousTask<MachineImage>();

        try {
            getSupport().captureImageAsync(options, task);
            Assert.assertTrue("Image capture is not supported, yet the capture attempt succeeded without error", supported);
            while( !task.isComplete() ) {
                out("Status: " + task.getPercentComplete() + "%");
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
            }
            MachineImage image = task.getResult();

            out("Captured: " + image);
            //noinspection ThrowableResultOfMethodCallIgnored
            Assert.assertNull("The process produced an error: " + task.getTaskError(), task.getTaskError());
            Assert.assertNotNull("The process failed to product a meaningful result", image);
            Assert.assertFalse("Machine image is not in a proper state", MachineImageState.DELETED.equals(image.getCurrentState()));
        }
        catch( OperationNotSupportedException e ) {
            out("Capture not supported " + (supported ? "(ERROR)" : "(OK)"));
            Assert.assertFalse("Notified that capture is not supported, but it is supposed to be according to meta-data", supported);
        }
    }

    @Test
    public void testBundleVirtualMachine() throws CloudException, InternalException {
        if( canTestBundling ) {
            String location = getSupport().bundleVirtualMachine(testVm.getProviderVirtualMachineId(), bundlingFormat, testBucket, testObject);

            out("Bundle location: " + location);
            Assert.assertNotNull("Bundle location cannot be null on success", location);
        }
        else {
            if( !getSupport().listSupportedFormatsForBundling().iterator().hasNext() ) {
                try {
                    getSupport().bundleVirtualMachine(testVm.getProviderVirtualMachineId(), MachineImageFormat.OVF, "testBucket", "testObject");
                    Assert.fail("Bundling supposedly succeeded even though bundling formats are supported");
                }
                catch( OperationNotSupportedException e ) {
                    out("Operation not supported (OK)");
                }
            }
        }
    }

    @Test
    public void testBundleVirtualMachineAsync() throws CloudException, InternalException {
        if( canTestBundling ) {
            AsynchronousTask<String> task = new AsynchronousTask<String>();

            getSupport().bundleVirtualMachineAsync(testVm.getProviderVirtualMachineId(), bundlingFormat, testBucket, testObject, task);

            out("Bundle task: " + task);
            Assert.assertNotNull("Bundle task cannot be null on success", task);
            while( !task.isComplete() ) {
                out("Status: " + task.getPercentComplete() + "%");
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
            }
            String location = task.getResult();

            out("Bundle location: " + location);
            Assert.assertNotNull("Bundle location cannot be null on success", location);
        }
        else {
            if( !getSupport().listSupportedFormatsForBundling().iterator().hasNext() ) {
                try {
                    getSupport().bundleVirtualMachineAsync(testVm.getProviderVirtualMachineId(), MachineImageFormat.OVF, "testBucket", "testObject", new AsynchronousTask<String>());
                    Assert.fail("Bundling supposedly succeeded even though bundling formats are supported");
                }
                catch( OperationNotSupportedException e ) {
                    out("Operation not supported (OK)");
                }
            }
        }
    }

    @Test
    public void testRegisterBundle() throws CloudException, InternalException {
        if( canTestBundling ) {
            String name = getName() + "-" + (System.currentTimeMillis() % 10000);
            ImageCreateOptions options = ImageCreateOptions.getInstance(bundlingFormat, testLocation, testVm.getPlatform(), name, name);
            MachineImage image = getSupport().registerImageBundle(options);

            out("Registered: " + image);
            Assert.assertNotNull("Registering an image must return a valid image", image);
            killImageId = image.getProviderMachineImageId();
        }
    }

    @Test
    public void testAddPrivateShare() throws CloudException, InternalException {
        MachineImageSupport support = getSupport();
        String shareAccount = getTestShareAccount();

        if( getSupport().supportsImageSharing() ) {
            Assert.assertNotNull("Cannot test image sharing unless the test.shareAccount property is set to a second account", shareAccount);
        }
        else if( shareAccount == null ) {
            shareAccount = UUID.randomUUID().toString();
        }
        try {
            Iterable<String> shares = support.listShares(testImage.getProviderMachineImageId());

            out("Before: " + shares);
            support.addImageShare(testImage.getProviderMachineImageId(), shareAccount);
            Assert.assertTrue("An attempt to share an image succeeded even though sharing is not supposed to be supported", support.supportsImageSharing());
            boolean found = false;

            shares = support.listShares(testImage.getProviderMachineImageId());
            out("After: " + shares);
            for( String share : shares ) {
                if( share.equals(shareAccount) ) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue("Did not find the new share among the listed shares", found);
        }
        catch( OperationNotSupportedException e ) {
            out("Not supported " + (support.supportsImageSharing() ? "(ERROR)" : "(OK)"));
            Assert.assertFalse("An attempt to share failed even though sharing is supposedly supported", support.supportsImageSharing());
        }
    }

    @Test
    public void testRemovePrivateShare() throws CloudException, InternalException {
        MachineImageSupport support = getSupport();
        String shareAccount = getTestShareAccount();

        if( getSupport().supportsImageSharing() ) {
            Assert.assertNotNull("Cannot test image sharing unless the test.shareAccount property is set to a second account", shareAccount);
        }
        else if( shareAccount == null ) {
            shareAccount = UUID.randomUUID().toString();
        }
        try {
            Iterable<String> shares = support.listShares(testImage.getProviderMachineImageId());

            out("Before: " + shares);
            support.removeImageShare(testImage.getProviderMachineImageId(), shareAccount);
            Assert.assertTrue("An attempt to unshare an image succeeded even though sharing is not supposed to be supported", support.supportsImageSharing());
            boolean found = false;

            shares = support.listShares(testImage.getProviderMachineImageId());
            out("After: " + shares);
            for( String share : shares ) {
                if( share.equals(shareAccount) ) {
                    found = true;
                    break;
                }
            }
            Assert.assertFalse("Found the old share among the listed shares", found);
        }
        catch( OperationNotSupportedException e ) {
            out("Not supported " + (support.supportsImageSharing() ? "(ERROR)" : "(OK)"));
            Assert.assertFalse("An attempt to unshare failed even though sharing is supposedly supported", support.supportsImageSharing());
        }
    }

    @Test
    public void testAddPublicShare() throws CloudException, InternalException {
        MachineImageSupport support = getSupport();

        try {
            out("Before: " + support.isImageSharedWithPublic(testImage.getProviderMachineImageId()));
            support.addPublicShare(testImage.getProviderMachineImageId());
            Assert.assertTrue("An attempt to share an image succeeded even though sharing is not supposed to be supported", support.supportsImageSharingWithPublic());
            boolean p = support.isImageSharedWithPublic(testImage.getProviderMachineImageId());

            out("After: " + p);
            Assert.assertTrue("The test image is not shown as publicly shared", p);
        }
        catch( OperationNotSupportedException e ) {
            out("Not supported " + (support.supportsImageSharing() ? "(ERROR)" : "(OK)"));
            Assert.assertFalse("An attempt to share failed even though sharing is supposedly supported", support.supportsImageSharingWithPublic());
        }
    }

    @Test
    public void testRemovePublicShare() throws CloudException, InternalException {
        MachineImageSupport support = getSupport();

        try {
            out("Before: " + support.isImageSharedWithPublic(testImage.getProviderMachineImageId()));
            support.removePublicShare(testImage.getProviderMachineImageId());
            Assert.assertTrue("An attempt to unshare an image succeeded even though sharing is not supposed to be supported", support.supportsImageSharingWithPublic());
            boolean p = support.isImageSharedWithPublic(testImage.getProviderMachineImageId());

            out("After: " + p);
            Assert.assertFalse("The test image is still shown as publicly shared", p);
        }
        catch( OperationNotSupportedException e ) {
            out("Not supported " + (support.supportsImageSharing() ? "(ERROR)" : "(OK)"));
            Assert.assertFalse("An attempt to unshare failed even though sharing is supposedly supported", support.supportsImageSharingWithPublic());
        }
    }

    @Test
    public void testRemoveAllShares() throws CloudException, InternalException {
        MachineImageSupport support = getSupport();

        try {
            Iterable<String> shares = support.listShares(testImage.getProviderMachineImageId());

            out("Before: " + shares);
            support.removeAllImageShares(testImage.getProviderMachineImageId());
            Assert.assertTrue("An attempt to unshare an image succeeded even though sharing is not supposed to be supported", support.supportsImageSharing());

            shares = support.listShares(testImage.getProviderMachineImageId());
            Assert.assertFalse("A share still remains with the image", shares.iterator().hasNext());
        }
        catch( OperationNotSupportedException e ) {
            out("Not supported " + (support.supportsImageSharing() ? "(ERROR)" : "(OK)"));
            Assert.assertFalse("An attempt to unshare failed even though sharing is supposedly supported", support.supportsImageSharing());
        }
    }
}
