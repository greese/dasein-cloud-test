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

import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.CalendarWrapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Random;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests validating the implementation of stateful image/template management features for Dasein Cloud.
 * <p>Created by George Reese: 2/19/13 2:35 PM</p>
 *
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatefulImageTests {
    static private final Random random = new Random();
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulImageTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String bundleLocation;
    private String provisionedImage;
    private String testShareAccount;
    private String testImageId;
    private String testVMId;
    private String testDataCenterId;

    public StatefulImageTests() {
    }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        testDataCenterId = DaseinTestManager.getDefaultDataCenterId(false);

        if( !name.getMethodName().startsWith("capture") ) {
            testImageId = tm.getTestImageId(DaseinTestManager.STATEFUL, true);
        }
        testVMId = tm.getTestVMId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, testDataCenterId);
        testShareAccount = System.getProperty("shareAccount");
        if( testImageId != null ) {
            if( name.getMethodName().equals("addPrivateShare") || name.getMethodName().equals("addPublicShare") ||
                    name.getMethodName().equals("removePrivateShare") || name.getMethodName().equals("removePublicShare") || name.getMethodName().equals("removeAllShares") ) {
                ComputeServices services = tm.getProvider().getComputeServices();

                if( services != null ) {
                    MachineImageSupport support = services.getImageSupport();

                    if( support != null ) {
                        try {
                            support.removeAllImageShares(testImageId);
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
            if( testShareAccount != null && ( name.getMethodName().equals("removePrivateShare") || name.getMethodName().equals("removeAllShares") ) ) {
                ComputeServices services = tm.getProvider().getComputeServices();

                if( services != null ) {
                    MachineImageSupport support = services.getImageSupport();

                    if( support != null ) {
                        try {
                            support.addImageShare(testImageId, testShareAccount);
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
            if( name.getMethodName().equals("removePublicShare") || name.getMethodName().equals("removeAllShares") ) {
                ComputeServices services = tm.getProvider().getComputeServices();

                if( services != null ) {
                    MachineImageSupport support = services.getImageSupport();

                    if( support != null ) {
                        try {
                            support.addPublicShare(testImageId);
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    @After
    public void after() {
        try {
            if( provisionedImage != null ) {
                try {
                    //noinspection ConstantConditions
                    tm.getProvider().getComputeServices().getImageSupport().remove(provisionedImage);
                }
                catch( Throwable t ) {
                    tm.warn("Unable to de-provision image " + provisionedImage + ": " + t.getMessage());
                }
            }
            testVMId = null;
            testImageId = null;
            testShareAccount = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void listShares() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( testImageId != null ) {
                    MachineImage image = support.getImage(testImageId);

                    assertNotNull("Failed to find the test image among possible images", image);
                    Iterable<String> shares = support.listShares(testImageId);

                    tm.out("Image Shares", shares);
                    assertNotNull("Image shares may not be null", shares);
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.warn("No image ID was identified, so this test is not valid");
                    }
                    else if( !support.getCapabilities().supportsImageCapture(MachineImageType.STORAGE) && !support.getCapabilities().supportsImageCapture(MachineImageType.VOLUME) ) {
                        tm.ok("No custom images, so sharing doesn't really make any sense");
                    }
                    else {
                        fail("No test image exists for the getImage test");
                    }
                }
            }
            else {
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void addPrivateShare() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( testImageId != null ) {
                    if( testShareAccount != null ) {
                        Iterable<String> shares = support.listShares(testImageId);

                        tm.out("Before", shares);
                        if( support.getCapabilities().supportsImageSharing() ) {
                            support.addImageShare(testImageId, testShareAccount);

                            boolean found = false;

                            shares = support.listShares(testImageId);

                            long timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 3L );

                            while( timeout > System.currentTimeMillis() ) {
                                found = false;
                                for( String share : shares ) {
                                    if( share.equals(testShareAccount) ) {
                                        found = true;
                                        break;
                                    }
                                }
                                if( found ) {
                                    break;
                                }
                                try {
                                    Thread.sleep(15000L);
                                }
                                catch( InterruptedException ignore ) {
                                }
                                try {
                                    shares = support.listShares(testImageId);
                                }
                                catch( Throwable ignore ) {
                                }
                            }
                            tm.out("After", shares);
                            for( String share : shares ) {
                                if( share.equals(testShareAccount) ) {
                                    found = true;
                                    break;
                                }
                            }
                            Assert.assertTrue("Did not find the new share among the listed shares", found);
                        }
                        else {
                            try {
                                support.addImageShare(testImageId, testShareAccount);
                                fail("Private image sharing is not supported, but the operation completed without error");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("Caught OperationNotSupportedException while attempting to share an image (OK)");
                            }
                        }
                    }
                    else {
                        tm.warn("Unable to test account sharing due to no shareAccount property having been set (test invalid)");
                    }
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.warn("No image ID was identified, so this test is not valid");
                    }
                    else if( !support.getCapabilities().supportsImageCapture(MachineImageType.STORAGE) && !support.getCapabilities().supportsImageCapture(MachineImageType.VOLUME) ) {
                        tm.ok("No custom images, so sharing doesn't really make any sense");
                    }
                    else {
                        fail("No test image exists for the " + name.getMethodName() + " test");
                    }
                }
            }
            else {
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void removePrivateShare() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( testImageId != null ) {
                    if( testShareAccount != null ) {
                        Iterable<String> shares = support.listShares(testImageId);

                        tm.out("Before", shares);
                        if( support.getCapabilities().supportsImageSharing() ) {
                            support.removeImageShare(testImageId, testShareAccount);

                            boolean found = false;

                            shares = support.listShares(testImageId);

                            long timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 3L );

                            while( timeout > System.currentTimeMillis() ) {
                                found = false;
                                for( String share : shares ) {
                                    if( share.equals(testShareAccount) ) {
                                        found = true;
                                        break;
                                    }
                                }
                                if( !found ) {
                                    break;
                                }
                                try {
                                    Thread.sleep(15000L);
                                }
                                catch( InterruptedException ignore ) {
                                }
                                try {
                                    shares = support.listShares(testImageId);
                                }
                                catch( Throwable ignore ) {
                                }
                            }
                            tm.out("After", shares);
                            for( String share : shares ) {
                                if( share.equals(testShareAccount) ) {
                                    found = true;
                                    break;
                                }
                            }
                            Assert.assertFalse("The test account remains among the shared accounts", found);
                        }
                        else {
                            try {
                                support.removeImageShare(testImageId, testShareAccount);
                                fail("Private image sharing is not supported, but the operation completed without error");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("Caught OperationNotSupportedException while attempting to remove an image share (OK)");
                            }
                        }
                    }
                    else {
                        tm.warn("Unable to test account share removal due to no shareAccount property having been set (test invalid)");
                    }
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.warn("No image ID was identified, so this test is not valid");
                    }
                    else if( !support.getCapabilities().supportsImageCapture(MachineImageType.STORAGE) && !support.getCapabilities().supportsImageCapture(MachineImageType.VOLUME) ) {
                        tm.ok("No custom images, so sharing doesn't really make any sense");
                    }

                    else {
                        fail("No test image exists for the " + name.getMethodName() + " test");
                    }
                }
            }
            else {
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void addPublicShare() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( testImageId != null ) {
                    if( support.getCapabilities().supportsImageSharingWithPublic() ) {
                        tm.out("Before", support.isImageSharedWithPublic(testImageId));
                        support.addPublicShare(testImageId);

                        long timeout = ( System.currentTimeMillis() + CalendarWrapper.MINUTE * 3L );
                        boolean shared = false;

                        while( timeout > System.currentTimeMillis() ) {
                            shared = support.isImageSharedWithPublic(testImageId);
                            if( shared ) {
                                break;
                            }
                            try {
                                Thread.sleep(15000L);
                            }
                            catch( InterruptedException ignore ) {
                            }
                        }
                        tm.out("After", shared);
                        assertTrue("Image remains private", shared);
                    }
                    else {
                        try {
                            support.addPublicShare(testImageId);
                            fail("Public image sharing is not supported, but the public share operation succeeded");
                        }
                        catch( OperationNotSupportedException expected ) {
                            tm.ok("Caught OperationNotSupportedException while attempting to remove an image share (OK)");
                        }
                    }
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.warn("No image ID was identified, so this test is not valid");
                    }
                    else if( !support.getCapabilities().supportsImageCapture(MachineImageType.STORAGE) && !support.getCapabilities().supportsImageCapture(MachineImageType.VOLUME) ) {
                        tm.ok("No custom images, so sharing doesn't really make any sense");
                    }
                    else {
                        fail("No test image exists for the " + name.getMethodName() + " test");
                    }
                }
            }
            else {
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void removePublicShare() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( testImageId != null ) {
                    if( support.getCapabilities().supportsImageSharingWithPublic() ) {
                        tm.out("Before", support.isImageSharedWithPublic(testImageId));
                        support.removePublicShare(testImageId);

                        long timeout = ( System.currentTimeMillis() + CalendarWrapper.MINUTE * 3L );
                        boolean shared = true;

                        while( timeout > System.currentTimeMillis() ) {
                            shared = support.isImageSharedWithPublic(testImageId);
                            if( !shared ) {
                                break;
                            }
                            try {
                                Thread.sleep(15000L);
                            }
                            catch( InterruptedException ignore ) {
                            }
                        }
                        tm.out("After", shared);
                        assertFalse("Image remains public", shared);
                    }
                    else {
                        try {
                            support.removePublicShare(testImageId);
                            fail("Public image sharing is not supported, but the public share operation succeeded");
                        }
                        catch( OperationNotSupportedException expected ) {
                            tm.ok("Caught OperationNotSupportedException while attempting to remove an image share (OK)");
                        }
                    }
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.warn("No image ID was identified, so this test is not valid");
                    }
                    else if( !support.getCapabilities().supportsImageCapture(MachineImageType.STORAGE) && !support.getCapabilities().supportsImageCapture(MachineImageType.VOLUME) ) {
                        tm.ok("No custom images, so sharing doesn't really make any sense");
                    }
                    else {
                        fail("No test image exists for the " + name.getMethodName() + " test");
                    }
                }
            }
            else {
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void removeAllShares() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( testImageId != null ) {
                    tm.out("Before [Public]", support.isImageSharedWithPublic(testImageId));
                    tm.out("Before [Private]", support.listShares(testImageId));
                    try {
                        support.removeAllImageShares(testImageId);
                    }
                    catch( OperationNotSupportedException e ) {
                        fail("This operation should not throw an OperationNotSupportedException (just a NO-OP in clouds without sharing)");
                    }
                    long timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 3L );
                    boolean shared;

                    while( timeout > System.currentTimeMillis() ) {
                        shared = support.isImageSharedWithPublic(testImageId);
                        if( !shared ) {
                            shared = support.listShares(testImageId).iterator().hasNext();
                            if( !shared ) {
                                break;
                            }
                        }
                        try {
                            Thread.sleep(15000L);
                        }
                        catch( InterruptedException ignore ) {
                        }
                    }
                    tm.out("After [Public]", support.isImageSharedWithPublic(testImageId));
                    tm.out("After [Private]", support.listShares(testImageId));

                    assertFalse("Image remains public", support.isImageSharedWithPublic(testImageId));
                    assertFalse("Image still has private shares", support.listShares(testImageId).iterator().hasNext());
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.warn("No image ID was identified, so this test is not valid");
                    }
                    else if( !support.getCapabilities().supportsImageCapture(MachineImageType.STORAGE) && !support.getCapabilities().supportsImageCapture(MachineImageType.VOLUME) ) {
                        tm.ok("No custom images, so sharing doesn't really make any sense");
                    }
                    else {
                        fail("No test image exists for the " + name.getMethodName() + " test");
                    }
                }
            }
            else {
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    static private boolean capturedOnce = false;

    @Test
    public void capture() throws CloudException, InternalException {
        if( capturedOnce ) {
            try {
                Thread.sleep(CalendarWrapper.MINUTE * 2L);
            }
            catch( InterruptedException ignore ) {
            }
        }
        else {
            capturedOnce = true;
        }
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services == null ) {
            tm.ok("No compute services in this cloud " + tm.getProvider().getCloudName());
            return;
        }
        ComputeResources computeResources = DaseinTestManager.getComputeResources();
        if( computeResources == null ) {
            fail("Compute resources failed to initialise");
        }

        VirtualMachineSupport vmSupport = services.getVirtualMachineSupport();
        MachineImageSupport support = services.getImageSupport();

        if( support == null || vmSupport == null ) {
            tm.ok("No image or VM support in this cloud " + tm.getProvider().getCloudName());
            return;

        }
        if( testVMId == null ) {
            if( !support.isSubscribed() ) {
                tm.warn("No test VM was identified for image capture, so this test is not valid");
                return;
            }
            else {
                fail("No test VM exists for the " + name.getMethodName() + " test");
            }
        }
        VirtualMachine vm = vmSupport.getVirtualMachine(testVMId);

        assertNotNull("The test virtual machine " + testVMId + " does not exist", vm);

        // make sure to put the VM into the right state for capturing
        computeResources.prepareVmForImaging(vm, vmSupport, support);

        ImageCreateOptions options = ImageCreateOptions.getInstance(vm, "dsncap" + ( System.currentTimeMillis() % 10000 ), "Dasein Capture Image Test");
        options.withMetaData("dsntestcase", "true");

        String imageId = vm.getProviderMachineImageId();
        MachineImageType type = null;

        MachineImage source = support.getImage(imageId);

        if( source != null ) {
            type = source.getType();
        }
        else {
            for( MachineImageType t : support.getCapabilities().listSupportedImageTypes() ) {
                type = t; // pray
            }
        }
        if( type == null ) {
            type = MachineImageType.VOLUME; // or not; qui sait?
        }
        if( support.getCapabilities().supportsImageCapture(type) ) {
            provisionedImage = options.build(tm.getProvider());
            tm.out("New Image", provisionedImage);
            assertNotNull("The image ID returned from provisioning the image was null", provisionedImage);

            long timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 20L );

            while( timeout > System.currentTimeMillis() ) {
                try {
                    MachineImage image = support.getImage(provisionedImage);

                    assertNotNull("The image disappeared after it was created, but before it became available", image);
                    assertFalse("The image is now in a deleted state, but before it became available", MachineImageState.DELETED.equals(image.getCurrentState()));
                    tm.out("--> Current State", image.getCurrentState());
                    if( MachineImageState.ACTIVE.equals(image.getCurrentState()) ) {
                        break;
                    }
                }
                catch( Throwable t ) {
                    tm.warn("Error fetching captured image " + provisionedImage);
                }
                try {
                    Thread.sleep(15000L);
                }
                catch( InterruptedException ignore ) {
                }
            }
            MachineImage image = support.getImage(provisionedImage);

            assertNotNull("The image disappeared after it was created, but before it became available", image);
            assertEquals("The image never entered an ACTIVE state during the allotted time window", MachineImageState.ACTIVE, image.getCurrentState());
        }
        else {
            try {
                provisionedImage = options.build(tm.getProvider());
                // past this point every possibility is a failure
                assertNull("Image returned as null despite @Nonnull contract in core. Fix it!", provisionedImage);
                assertNotNull("captureImage returned an imageId despite the capability claiming it's not supported");
            }
            catch( OperationNotSupportedException expected ) {
                // this is good
                tm.ok("Caught OperationNotSupportedException while attempting to capture image in cloud that does not support capture");
            }
        }
    }

    @Test
    public void captureAsync() throws Throwable {
        if( capturedOnce ) {
            try {
                Thread.sleep(CalendarWrapper.MINUTE * 2L);
            }
            catch( InterruptedException ignore ) {
            }
        }
        else {
            capturedOnce = true;
        }
        ComputeServices services = tm.getProvider().getComputeServices();
        if( services == null ) {
            tm.ok("No compute services in this cloud " + tm.getProvider().getCloudName());
            return;
        }
        ComputeResources computeResources = DaseinTestManager.getComputeResources();
        if( computeResources == null ) {
            fail("Compute resources failed to initialise");
        }

        VirtualMachineSupport vmSupport = services.getVirtualMachineSupport();
        MachineImageSupport support = services.getImageSupport();

        if( support == null || vmSupport == null ) {
            tm.ok("No image or VM support in this cloud " + tm.getProvider().getCloudName());
            return;
        }
        if( testVMId == null ) {
            if( !support.isSubscribed() ) {
                tm.warn("No test VM was identified for image capture, so this test is not valid");
                return;
            }
            else {
                fail("No test VM exists for the " + name.getMethodName() + " test");
            }
        }
        VirtualMachine vm = vmSupport.getVirtualMachine(testVMId);
        assertNotNull("The test virtual machine " + testVMId + " does not exist", vm);

        // make sure to put the VM into the right state for capturing
        computeResources.prepareVmForImaging(vm, vmSupport, support);

        String imageId = vm.getProviderMachineImageId();
        MachineImageType type = null;

        MachineImage source = support.getImage(imageId);

        if( source != null ) {
            type = source.getType();
        }
        else {
            for( MachineImageType t : support.getCapabilities().listSupportedImageTypes() ) {
                type = t; // pray
            }
        }
        if( type == null ) {
            type = MachineImageType.VOLUME; // or not; qui sait?
        }
        ImageCreateOptions options = ImageCreateOptions.getInstance(vm, "dsncap" + ( System.currentTimeMillis() % 10000 ), "Dasein Capture Image Test");

        options.withMetaData("dsntestcase", "true");

        AsynchronousTask<MachineImage> task = new AsynchronousTask<MachineImage>();

        if( support.getCapabilities().supportsImageCapture(type) ) {
            support.captureImageAsync(options, task);

            tm.out("Task", "");

            long timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 30L );

            while( timeout > System.currentTimeMillis() ) {
                if( task.isComplete() ) {
                    Throwable t = task.getTaskError();

                    if( t != null ) {
                        tm.out("-->", "Failure: " + t.getMessage());
                        throw t;
                    }
                    tm.out("-->", "Complete");
                    break;
                }
                else {
                    tm.out("-->", task.getPercentComplete() + "%");
                }
                try {
                    Thread.sleep(15000L);
                }
                catch( InterruptedException ignore ) {
                }
            }
            MachineImage image = task.getResult();

            tm.out("New Image", image);
            assertNotNull("The image ID returned from provisioning the image was null", image);

            provisionedImage = image.getProviderMachineImageId();

            timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 30L );

            while( timeout > System.currentTimeMillis() ) {
                try {
                    MachineImage img = support.getImage(provisionedImage);

                    assertNotNull("The image disappeared after it was created, but before it became available", img);
                    assertFalse("The image is now in a deleted state, but before it became available", MachineImageState.DELETED.equals(img.getCurrentState()));
                    tm.out("--> Current State", img.getCurrentState());
                    if( MachineImageState.ACTIVE.equals(img.getCurrentState()) ) {
                        break;
                    }
                }
                catch( Throwable t ) {
                    tm.warn("Error fetching captured image " + provisionedImage);
                }
                try {
                    Thread.sleep(15000L);
                }
                catch( InterruptedException ignore ) {
                }
            }
            image = support.getImage(provisionedImage);

            assertNotNull("The image disappeared after it was created, but before it became available", image);
            assertEquals("The image never entered an ACTIVE state during the allotted time window", MachineImageState.ACTIVE, image.getCurrentState());
        }
        else {
            try {
                support.captureImageAsync(options, task);
            }
            catch( OperationNotSupportedException expected ) {
                tm.ok("Caught OperationNotSupportedException while attempting to capture image in cloud that does not support capture");
            }
        }
    }

    @Test
    public void captureReboot() throws CloudException, InternalException {
        if( capturedOnce ) {
            try {
                Thread.sleep(CalendarWrapper.MINUTE * 2L);
            }
            catch( InterruptedException ignore ) {
            }
        }
        else {
            capturedOnce = true;
        }
        ComputeServices services = tm.getProvider().getComputeServices();
        if( services == null ) {
            tm.ok("No compute services in this cloud " + tm.getProvider().getCloudName());
            return;
        }
        ComputeResources computeResources = DaseinTestManager.getComputeResources();
        if( computeResources == null ) {
            fail("Compute resources failed to initialise");
        }

        VirtualMachineSupport vmSupport = services.getVirtualMachineSupport();
        MachineImageSupport support = services.getImageSupport();

        if( support == null || vmSupport == null ) {
            tm.ok("No image or VM support in this cloud " + tm.getProvider().getCloudName());
            return;
        }
        if( testVMId == null ) {
            if( !support.isSubscribed() ) {
                tm.warn("No test VM was identified for image capture, so this test is not valid");
                return;
            }
            else {
                fail("No test VM exists for the " + name.getMethodName() + " test");
            }
        }
        VirtualMachine vm = vmSupport.getVirtualMachine(testVMId);
        assertNotNull("The test virtual machine " + testVMId + " does not exist", vm);

        // make sure to put the VM into the right state for capturing
        computeResources.prepareVmForImaging(vm, vmSupport, support);

        ImageCreateOptions options = ImageCreateOptions.getInstance(vm, "dsncap" + ( System.currentTimeMillis() % 10000 ), "Dasein Capture Image Test", false);

        options.withMetaData("dsntestcase", "true");

        String imageId = vm.getProviderMachineImageId();
        MachineImageType type = null;

        MachineImage source = support.getImage(imageId);

        if( source != null ) {
            type = source.getType();
        }
        else {
            for( MachineImageType t : support.getCapabilities().listSupportedImageTypes() ) {
                type = t; // pray
            }
        }
        if( type == null ) {
            type = MachineImageType.VOLUME; // or not; qui sait?
        }
        if( support.getCapabilities().supportsImageCapture(type) ) {
            provisionedImage = options.build(tm.getProvider());
            tm.out("New Image", provisionedImage);
            assertNotNull("The image ID returned from provisioning the image was null", provisionedImage);

            long timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 20L );

            while( timeout > System.currentTimeMillis() ) {
                try {
                    MachineImage image = support.getImage(provisionedImage);

                    assertNotNull("The image disappeared after it was created, but before it became available", image);
                    assertFalse("The image is now in a deleted state, but before it became available", MachineImageState.DELETED.equals(image.getCurrentState()));
                    tm.out("--> Current State", image.getCurrentState());
                    if( MachineImageState.ACTIVE.equals(image.getCurrentState()) ) {
                        break;
                    }
                }
                catch( Throwable t ) {
                    tm.warn("Error fetching captured image " + provisionedImage);
                }
                try {
                    Thread.sleep(15000L);
                }
                catch( InterruptedException ignore ) {
                }
            }
            MachineImage image = support.getImage(provisionedImage);

            assertNotNull("The image disappeared after it was created, but before it became available", image);
            assertEquals("The image never entered an ACTIVE state during the allotted time window", MachineImageState.ACTIVE, image.getCurrentState());
        }
        else {
            try {
                provisionedImage = options.build(tm.getProvider());
            }
            catch( OperationNotSupportedException expected ) {
                tm.ok("Caught OperationNotSupportedException while attempting to capture image in cloud that does not support capture");
            }
        }
    }

    @Test
    public void bundleVM() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( !support.getCapabilities().identifyLocalBundlingRequirement().equals(Requirement.REQUIRED) ) {
                    VirtualMachineSupport vmSupport = services.getVirtualMachineSupport();

                    if( vmSupport != null ) {
                        if( testVMId != null ) {
                            VirtualMachine vm = vmSupport.getVirtualMachine(testVMId);

                            assertNotNull("The test virtual machine " + testVMId + " does not exist", vm);

                            if( support.getCapabilities().listSupportedFormatsForBundling().iterator().hasNext() ) {
                                MachineImageFormat fmt = support.getCapabilities().listSupportedFormatsForBundling().iterator().next();

                                bundleLocation = support.bundleVirtualMachine(testVMId, fmt, "dsnbucket" + random.nextInt(100000), "dsnimgbundle");
                                tm.out("Bundle Location", bundleLocation);
                                assertNotNull("The bundle location returned from bundling the image was null", bundleLocation);

                                ImageCreateOptions options = ImageCreateOptions.getInstance(fmt, bundleLocation, vm.getPlatform(), "dsnimgbdl" + random.nextInt(100000), "Dasein Test Bundle Image");

                                options.withMetaData("dsntestcase", "true");

                                provisionedImage = support.registerImageBundle(options).getProviderMachineImageId();
                                long timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 20L );

                                while( timeout > System.currentTimeMillis() ) {
                                    try {
                                        MachineImage image = support.getImage(provisionedImage);

                                        assertNotNull("The image disappeared after it was created, but before it became available", image);
                                        tm.out("--> Current State", image.getCurrentState());
                                        if( MachineImageState.ACTIVE.equals(image.getCurrentState()) ) {
                                            break;
                                        }
                                    }
                                    catch( Throwable t ) {
                                        tm.warn("Error fetching captured image " + provisionedImage);
                                    }
                                    try {
                                        Thread.sleep(15000L);
                                    }
                                    catch( InterruptedException ignore ) {
                                    }
                                }
                                MachineImage image = support.getImage(provisionedImage);

                                assertNotNull("The image disappeared after it was created, but before it became available", image);
                                assertEquals("The image never entered an ACTIVE state during the allotted time window", MachineImageState.ACTIVE, image.getCurrentState());
                            }
                            else {
                                try {
                                    bundleLocation = support.bundleVirtualMachine(testVMId, MachineImageFormat.OVF, "dsnbundlefail" + random.nextInt(100000), "dsnimgbundle");
                                    fail("Bundling completed even though bundling is not supposed to be supported");
                                }
                                catch( OperationNotSupportedException expected ) {
                                    tm.ok("Caught OperationNotSupportedException while attempting to bundle image in cloud that does not support bundling");
                                }
                                try {
                                    ImageCreateOptions options = ImageCreateOptions.getInstance(MachineImageFormat.OVF, "/dev/null", vm.getPlatform(), "dsnimgbdl" + random.nextInt(100000), "Dasein Test Bundle Image");

                                    options.withMetaData("dsntestcase", "true");

                                    provisionedImage = support.registerImageBundle(options).getProviderMachineImageId();
                                    fail("Registration succeeded from fake bundle even for some incomprehensible reason");
                                }
                                catch( OperationNotSupportedException expected ) {
                                    tm.ok("Caught OperationNotSupportedException while attempting to register image in cloud that does not support bundling");
                                }
                            }
                        }
                        else {
                            if( !support.isSubscribed() ) {
                                tm.warn("No test VM was identified for image capture, so this test is not valid");
                            }
                            else {
                                fail("No test VM exists for the " + name.getMethodName() + " test");
                            }
                        }
                    }
                    else {
                        fail("The non-existence of VM support along with the existence of image support makes no sense");
                    }
                }
                else {
                    tm.ok("Bundling must occur locally on the virtual machine and thus cannot be tested here");
                }
            }
            else {
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void bundleVMAsync() throws Throwable {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( !support.getCapabilities().identifyLocalBundlingRequirement().equals(Requirement.REQUIRED) ) {
                    VirtualMachineSupport vmSupport = services.getVirtualMachineSupport();

                    if( support != null ) {
                        if( testVMId != null ) {
                            VirtualMachine vm = vmSupport.getVirtualMachine(testVMId);

                            assertNotNull("The test virtual machine " + testVMId + " does not exist", vm);

                            AsynchronousTask<String> task = new AsynchronousTask<String>();

                            if( support.getCapabilities().listSupportedFormatsForBundling().iterator().hasNext() ) {
                                MachineImageFormat fmt = support.getCapabilities().listSupportedFormatsForBundling().iterator().next();

                                support.bundleVirtualMachineAsync(testVMId, fmt, "dsnbucket" + random.nextInt(100000), "dsnimgbundle", task);
                                tm.out("Task", "");

                                long timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 20L );

                                while( timeout > System.currentTimeMillis() ) {
                                    if( task.isComplete() ) {
                                        Throwable t = task.getTaskError();

                                        if( t != null ) {
                                            tm.out("-->", "Failure: " + t.getMessage());
                                            throw t;
                                        }
                                        tm.out("-->", "Complete");
                                        break;
                                    }
                                    else {
                                        tm.out("-->", task.getPercentComplete() + "%");
                                    }
                                    try {
                                        Thread.sleep(15000L);
                                    }
                                    catch( InterruptedException ignore ) {
                                    }
                                }
                                bundleLocation = task.getResult();

                                tm.out("Bundle Location", bundleLocation);
                                assertNotNull("The bundle location returned from bundling the image was null", bundleLocation);

                                ImageCreateOptions options = ImageCreateOptions.getInstance(fmt, bundleLocation, vm.getPlatform(), "dsnimgbdl" + random.nextInt(100000), "Dasein Test Bundle Image");

                                options.withMetaData("dsntestcase", "true");

                                provisionedImage = support.registerImageBundle(options).getProviderMachineImageId();

                                timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 20L );

                                while( timeout > System.currentTimeMillis() ) {
                                    try {
                                        MachineImage img = support.getImage(provisionedImage);

                                        assertNotNull("The image disappeared after it was created, but before it became available", img);
                                        tm.out("--> Current State", img.getCurrentState());
                                        if( MachineImageState.ACTIVE.equals(img.getCurrentState()) ) {
                                            break;
                                        }
                                    }
                                    catch( Throwable t ) {
                                        tm.warn("Error fetching captured image " + provisionedImage);
                                    }
                                    try {
                                        Thread.sleep(15000L);
                                    }
                                    catch( InterruptedException ignore ) {
                                    }
                                }
                                MachineImage image = support.getImage(provisionedImage);

                                assertNotNull("The image disappeared after it was created, but before it became available", image);
                                assertEquals("The image never entered an ACTIVE state during the allotted time window", MachineImageState.ACTIVE, image.getCurrentState());
                            }
                            else {
                                try {
                                    support.bundleVirtualMachineAsync(testVMId, MachineImageFormat.OVF, "dsnbdlfail" + random.nextInt(100000), "dsnimgbundle", task);
                                    fail("Bundling completed even though bundling is not supposed to be supported");
                                }
                                catch( OperationNotSupportedException expected ) {
                                    tm.ok("Caught OperationNotSupportedException while attempting to bundle image in cloud that does not support bundling");
                                }
                            }
                        }
                        else {
                            if( !support.isSubscribed() ) {
                                tm.warn("No test VM was identified for image capture, so this test is not valid");
                            }
                            else {
                                fail("No test VM exists for the " + name.getMethodName() + " test");
                            }
                        }
                    }
                    else {
                        fail("The non-existence of VM support along with the existence of image support makes no sense");
                    }
                }
                else {
                    tm.ok("Bundling must occur locally on the virtual machine and thus cannot be tested here");
                }
            }
            else {
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }
}
