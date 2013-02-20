package org.dasein.cloud.test.compute;

import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.MachineImage;
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

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/19/13 2:35 PM</p>
 *
 * @author George Reese
 */
public class StatefulImageTests {
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

    private String provisionedImage;
    private String testShareAccount;
    private String testImageId;
    private String testVMId;

    public StatefulImageTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testImageId = tm.getTestImageId(DaseinTestManager.STATEFUL, true);
        testVMId = tm.getTestVMId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, null);
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
            if( testShareAccount != null && (name.getMethodName().equals("removePrivateShare") || name.getMethodName().equals("removeAllShares")) ) {
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
                        if( support.supportsImageSharing() ) {
                            support.addImageShare(testImageId, testShareAccount);

                            boolean found = false;

                            shares = support.listShares(testImageId);
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
                        if( support.supportsImageSharing() ) {
                            support.removeImageShare(testImageId, testShareAccount);

                            boolean found = false;

                            shares = support.listShares(testImageId);
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
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( testImageId != null ) {
                    if( support.supportsImageSharingWithPublic() ) {
                        tm.out("Before", support.isImageSharedWithPublic(testImageId));
                        support.addPublicShare(testImageId);
                        tm.out("After", support.isImageSharedWithPublic(testImageId));
                        assertTrue("Image remains private", support.isImageSharedWithPublic(testImageId));
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
                    if( support.supportsImageSharingWithPublic() ) {
                        tm.out("Before", support.isImageSharedWithPublic(testImageId));
                        support.removePublicShare(testImageId);
                        tm.out("After", support.isImageSharedWithPublic(testImageId));
                        assertFalse("Image remains public", support.isImageSharedWithPublic(testImageId));
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
                    tm.out("After [Public]", support.isImageSharedWithPublic(testImageId));
                    tm.out("After [Private]", support.listShares(testImageId));

                    assertFalse("Image remains public", support.isImageSharedWithPublic(testImageId));
                    assertFalse("Image still has private shares", support.listShares(testImageId).iterator().hasNext());
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.warn("No image ID was identified, so this test is not valid");
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
    public void capture() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport vmSupport = services.getVirtualMachineSupport();
            MachineImageSupport support = services.getImageSupport();

            if( support != null && vmSupport != null ) {
                if( testVMId != null ) {
                    VirtualMachine vm = vmSupport.getVirtualMachine(testVMId);

                    assertNotNull("The test virtual machine " + testVMId + " does not exist", vm);
                    ImageCreateOptions options = ImageCreateOptions.getInstance(vm, "dsncap", "Dasein Capture Image Test");

                    options.withMetaData("dsntestcase", "true");

                    String imageId = vm.getProviderMachineImageId();
                    MachineImageType type = null;

                    MachineImage source = support.getImage(imageId);

                    if( source != null ) {
                        type = source.getType();
                    }
                    else {
                        for( MachineImageType t : support.listSupportedImageTypes() ) {
                            type = t; // pray
                        }
                    }
                    if( type == null ) {
                        type = MachineImageType.VOLUME; // or not; qui sait?
                    }
                    if( support.supportsImageCapture(type) ) {
                        provisionedImage = options.build(tm.getProvider());
                        tm.out("New Image", provisionedImage);
                        assertNotNull("The image ID returned from provisioning the image was null", provisionedImage);

                        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*20L);

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
                            try { Thread.sleep(15000L); }
                            catch( InterruptedException ignore ) { }
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
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void captureAsync() throws Throwable {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport vmSupport = services.getVirtualMachineSupport();
            MachineImageSupport support = services.getImageSupport();

            if( support != null && vmSupport != null ) {
                if( testVMId != null ) {
                    VirtualMachine vm = vmSupport.getVirtualMachine(testVMId);

                    assertNotNull("The test virtual machine " + testVMId + " does not exist", vm);

                    String imageId = vm.getProviderMachineImageId();
                    MachineImageType type = null;

                    MachineImage source = support.getImage(imageId);

                    if( source != null ) {
                        type = source.getType();
                    }
                    else {
                        for( MachineImageType t : support.listSupportedImageTypes() ) {
                            type = t; // pray
                        }
                    }
                    if( type == null ) {
                        type = MachineImageType.VOLUME; // or not; qui sait?
                    }
                    ImageCreateOptions options = ImageCreateOptions.getInstance(vm, "dsncap", "Dasein Capture Image Test");

                    options.withMetaData("dsntestcase", "true");

                    AsynchronousTask<MachineImage> task = new AsynchronousTask<MachineImage>();

                    if( support.supportsImageCapture(type) ) {
                        support.captureImageAsync(options, task);

                        tm.out("Task", "");

                        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*20L);

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
                            try { Thread.sleep(15000L); }
                            catch( InterruptedException ignore ) { }
                        }
                        MachineImage image = task.getResult();

                        tm.out("New Image", image);
                        assertNotNull("The image ID returned from provisioning the image was null", image);

                        provisionedImage = image.getProviderMachineImageId();

                        timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*20L);

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
                            try { Thread.sleep(15000L); }
                            catch( InterruptedException ignore ) { }
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
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }
}
