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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/19/13 7:24 AM</p>
 *
 * @author George Reese
 */
public class StatelessImageTests {
    static private final Random random = new Random();

    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessImageTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testImageId;

    public StatelessImageTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testImageId = DaseinTestManager.getSystemProperty("test.machineImage");
        if( testImageId == null ) {
            testImageId = tm.getTestImageId(DaseinTestManager.STATELESS, false);
        }
    }

    @After
    public void after() {
        tm.end();
    }

    private void assertImageContent(@Nonnull MachineImage image, @Nullable ImageClass expectedClass, Boolean checkPublic) {
        assertNotNull("The image ID may not be null", image.getProviderMachineImageId());
        assertNotNull("Image name may not be null", image.getName());
        assertNotNull("Image description may not be null", image.getDescription());
        assertNotNull("Image state may not be null", image.getCurrentState());
        assertNotNull("Image class may not be null", image.getImageClass());
        if( expectedClass != null ) {
            assertEquals("The image was not of the expected image class", expectedClass, image.getImageClass());
        }
        if( !ImageClass.MACHINE.equals(image.getImageClass()) ) {
            assertNull("Kernel image ID makes sense only for machine images in certain clouds", image.getKernelImageId());
        }
        assertNotNull("Image owner ID may not be null", image.getProviderOwnerId());
        assertNotNull("Image region may not be null", image.getProviderRegionId());
        assertNotNull("Image architecture may not be null", image.getArchitecture());
        assertNotNull("Image platform may not be null", image.getPlatform());
        assertTrue("Creation timestamp may not be negative", image.getCreationTimestamp() >= 0L);
        assertNotNull("Image type may not be null", image.getType());
        assertNotNull("Image software may not be null", image.getSoftware());
        if( MachineImageType.STORAGE.equals(image.getType()) ) {
            assertNotNull("Storage format must not be null for STORAGE-based images", image.getStorageFormat());
        }
        else {
            assertNull("Storage format must be null for VOLUME-based images", image.getStorageFormat());
        }
        Map<String,String> tags = image.getTags();

        assertNotNull("Image meta-data (tags) must not be null", tags);

        if( Platform.UNKNOWN.equals(image.getPlatform()) ) {
            Platform p = Platform.guess(image.getName());

            assertTrue("There is a better guess for platform than UNKNOWN based on the image name", p.equals(Platform.UNKNOWN));
            p = Platform.guess(image.getDescription());
            assertTrue("There is a better guess for platform than UNKNOWN based on the image description", p.equals(Platform.UNKNOWN));
            for( Map.Entry<String,String> entry : tags.entrySet() ) {
                p = Platform.guess(entry.getValue());
                assertTrue("There is a better guess for platform than UNKNOWN based on the image tag " + entry.getKey(), p.equals(Platform.UNKNOWN));
            }
        }
        String metaPublic = (String) image.getTag("public");
        if( metaPublic != null ) {
            Boolean isPublic = Boolean.valueOf(metaPublic);
            assertEquals("When 'public' metatag is set it should match isPublic()", isPublic, image.isPublic());
        }

        if( checkPublic != null ) {
            assertEquals("Image isPublic? status is incorrect", checkPublic, image.isPublic());
        }
    }

    private void assertImageContent(@Nonnull Iterable<MachineImage> images, @Nonnull ImageClass expectedClass, Boolean checkPublic) {
        for( MachineImage image : images ) {
            assertImageContent(image, expectedClass, checkPublic);
        }
    }

    private void assertListEquals(@Nonnull String errorMessage, @Nonnull Iterable<MachineImage> expected, @Nonnull Iterable<MachineImage> actual) {
        int expectedCount = 0, actualCount = 0;

        for( MachineImage img : expected ) {
            boolean found = false;

            for( MachineImage test : actual ) {
                if( test.getProviderMachineImageId().equals(img.getProviderMachineImageId()) ) {
                    found = true;
                    break;
                }
            }
            assertTrue(errorMessage + " [" + img + "]", found);
            expectedCount++;
        }
        for( MachineImage test : actual ) {
            boolean found = false;

            for( MachineImage img : expected ) {
                if( test.getProviderMachineImageId().equals(img.getProviderMachineImageId()) ) {
                    found = true;
                    break;
                }
            }
            assertTrue(errorMessage + " [" + test + "]", found);
            actualCount++;
        }
        assertTrue(errorMessage + " [" + expectedCount + " vs " + actualCount + "]", expectedCount == actualCount);
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                tm.out("Subscribed", support.isSubscribed());

                tm.out("Image Upload Support", support.getCapabilities().supportsDirectImageUpload());
                tm.out("Image Sharing with Accounts", support.getCapabilities().supportsImageSharing());
                tm.out("Image Sharing with Public", support.getCapabilities().supportsImageSharingWithPublic());
                Iterable<ImageClass> supportedClasses = support.getCapabilities().listSupportedImageClasses();

                tm.out("Supported Image Classes", supportedClasses);

                Iterable<MachineImageFormat> supportedFormats = support.getCapabilities().listSupportedFormats();

                tm.out("Supported Formats", supportedFormats);

                for( ImageClass cls : ImageClass.values() ) {
                    tm.out("Term for " + cls + " Images", support.getCapabilities().getProviderTermForImage(Locale.getDefault(), cls));
                    tm.out("Term for Custom " + cls + " Images", support.getCapabilities().getProviderTermForCustomImage(Locale.getDefault(), cls));
                    tm.out("Public " + cls + " Library", support.getCapabilities().supportsPublicLibrary(cls));
                }
                tm.out("Must Bundle on VM (Not Remote)", support.getCapabilities().identifyLocalBundlingRequirement());

                Iterable<MachineImageFormat> bundlingFormats = support.getCapabilities().listSupportedFormatsForBundling();

                tm.out("Supported Bundling Formats", bundlingFormats);

                Iterable<MachineImageType> types = support.getCapabilities().listSupportedImageTypes();

                tm.out("Supported Image Types", types);

                for( MachineImageType type : types ) {
                    tm.out("Image Capture of VMs", support.getCapabilities().supportsImageCapture(type));
                }

                if( !support.isSubscribed() ) {
                    tm.warn("Because this account is not subscribed for image services, this test cannot verify functionality against image services");
                }
                assertNotNull("Supported image classes must be non-null", supportedClasses);
                assertTrue("At least one image class must be supported", supportedClasses.iterator().hasNext());
                boolean machine = false;
                for( ImageClass cls : supportedClasses ) {
                    if( cls.equals(ImageClass.MACHINE) ) {
                        machine = true;
                    }
                }
                assertTrue("Machine images must be supported in any cloud with image support", machine);
                assertNotNull("Supported formats must be non-null", supportedFormats);
                for( ImageClass cls : ImageClass.values() ) {
                    assertNotNull("Term for " + cls + " images must not be null whether or not supported", support.getCapabilities().getProviderTermForImage(Locale.getDefault(), cls));
                    assertNotNull("Term for custom " + cls + " images must not be null whether or not supported", support.getCapabilities().getProviderTermForCustomImage(Locale.getDefault(), cls));
                }
                assertNotNull("Local bundling requirement must not be null", support.getCapabilities().identifyLocalBundlingRequirement());
                assertNotNull("The list of bundling formats must not be null", bundlingFormats);
                assertNotNull("The list of image types must not be null", types);
                assertTrue("At least one image type must be supported", types.iterator().hasNext());
            }
            else {
                tm.ok(tm.getProvider().getCloudName() + " does not support images/templates");
            }
        }
        else {
            tm.ok(tm.getProvider().getCloudName() + " does not support compute services");
        }
    }

    @Test
    public void getBogusImage() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                MachineImage image = support.getImage(UUID.randomUUID().toString());

                tm.out("Bogus Image", image);
                assertNull("Bogus image was supposed to be none, but got a valid image.", image);
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
    public void getImage() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( testImageId != null ) {
                    MachineImage image = support.getImage(testImageId);

                    tm.out("Image", image);
                    assertNotNull("Failed to find the test image among possible images", image);
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
    public void imageContent() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( testImageId != null ) {
                    MachineImage image = support.getImage(testImageId);

                    assertNotNull("Failed to find the test image among possible images", image);
                    tm.out("Image ID", image.getProviderMachineImageId());
                    tm.out("Current State", image.getCurrentState());
                    tm.out("Name", image.getName());
                    tm.out("Created", new Date(image.getCreationTimestamp()));
                    tm.out("Owner Account", image.getProviderOwnerId());
                    tm.out("Region ID", image.getProviderRegionId());
                    tm.out("Data Center ID", image.getProviderDataCenterId());
                    tm.out("Image Class", image.getImageClass());
                    tm.out("Architecture", image.getArchitecture());
                    tm.out("Platform", image.getPlatform());
                    tm.out("Kernel Image ID", image.getKernelImageId());
                    tm.out("Software", image.getSoftware());
                    tm.out("Type", image.getType());
                    tm.out("Storage Format", image.getStorageFormat());

                    Map<String,String> tags = image.getTags();

                    //noinspection ConstantConditions
                    if( tags != null ) {
                        for( Map.Entry<String,String> entry : tags.entrySet() ) {
                            tm.out("Tag " + entry.getKey(), entry.getValue());
                        }
                    }

                    tm.out("Description", image.getDescription());

                    assertImageContent(image, null, null);

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
    public void listMachineImages() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                boolean supported = false;

                for( ImageClass cls : support.getCapabilities().listSupportedImageClasses() ) {
                    if( cls.equals(ImageClass.MACHINE) ) {
                        supported = true;
                    }
                }
                Iterable<MachineImage> images = support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE));
                int count = 0;

                assertNotNull("listImages() must return a non-null list of images even if the image class is not supported", images);
                for( MachineImage image : images ) {
                    count++;
                    tm.out("Image", image);
                }
                tm.out("Total Machine Image Count", count);
                if( !supported ) {
                    assertTrue("Because machine images are not supported, the list of images should be empty", count == 0);
                }
                if( count > 0 ) {
                    assertImageContent(images, ImageClass.MACHINE, null);
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

    /*
     * This test should only be run when setup properly, so it removes a SPECIFIC machine image... Use with CAUTION!
     */
    /*
    @Test
    public void removeMachineImage() throws CloudException, InternalException {
        String manual_name_of_image_to_be_removed = "qa-project-2_roger-test";
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                boolean supported = false;

                for( ImageClass cls : support.getCapabilities().listSupportedImageClasses() ) {
                    if( cls.equals(ImageClass.MACHINE) ) {
                        supported = true;
                    }
                }

                Iterable<MachineImage> images = support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE));
                int count1 = 0;

                assertNotNull("listImages() must return a non-null list of images even if the image class is not supported", images);
                for( MachineImage image : images ) {
                    count1++;
                    tm.out("Image", image);
                }
                support.remove(manual_name_of_image_to_be_removed);

                int count2 = 0;

                // re-catalog the images...
                images = support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE));
                assertNotNull("listImages() must return a non-null list of images even if the image class is not supported", images);
                for( MachineImage image : images ) {
                    count2++;
                    tm.out("Image", image);
                }
                assertTrue("Image was not successfully deleted", count1 == (++count2));
            }
            else {
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }
    */

    @Test
    public void listKernelImages() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                boolean supported = false;

                for( ImageClass cls : support.getCapabilities().listSupportedImageClasses() ) {
                    if( cls.equals(ImageClass.KERNEL) ) {
                        supported = true;
                    }
                }
                Iterable<MachineImage> images = support.listImages(ImageFilterOptions.getInstance(ImageClass.KERNEL));
                int count = 0;

                assertNotNull("listImages() must return a non-null list of images even if the image class is not supported", images);
                for( MachineImage image : images ) {
                    count++;
                    tm.out("Image", image);
                }
                tm.out("Total Kernel Image Count", count);
                if( !supported ) {
                    assertTrue("Because kernel images are not supported, the list of images should be empty", count == 0);
                }
                else if( count == 0 ) {
                    tm.warn("No kernel images were returned and so this test may not be valid");
                }
                if( count > 0 ) {
                    assertImageContent(images, ImageClass.KERNEL, null);
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
    public void listRamdiskImages() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                boolean supported = false;

                for( ImageClass cls : support.getCapabilities().listSupportedImageClasses() ) {
                    if( cls.equals(ImageClass.RAMDISK) ) {
                        supported = true;
                    }
                }
                Iterable<MachineImage> images = support.listImages(ImageFilterOptions.getInstance(ImageClass.RAMDISK));
                int count = 0;

                assertNotNull("listImages() must return a non-null list of images even if the image class is not supported", images);
                for( MachineImage image : images ) {
                    count++;
                    tm.out("Image", image);
                }
                tm.out("Total Ramdisk Image Count", count);
                if( !supported ) {
                    assertTrue("Because ramdisk images are not supported, the list of images should be empty", count == 0);
                }
                else if( count == 0 ) {
                    tm.warn("No ramdisk images were returned and so this test may not be valid");
                }
                if( count > 0 ) {
                    assertImageContent(images, ImageClass.RAMDISK, null);
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
    public void listMachineImageStatus() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                boolean supported = false;

                for( ImageClass cls : support.getCapabilities().listSupportedImageClasses() ) {
                    if( cls.equals(ImageClass.MACHINE) ) {
                        supported = true;
                    }
                }
                Iterable<ResourceStatus> images = support.listImageStatus(ImageClass.MACHINE);
                int count = 0;

                assertNotNull("listImageStatus() must return a non-null list of images even if the image class is not supported", images);
                for( ResourceStatus status : images ) {
                    count++;
                    tm.out("Image Status", status);
                }
                tm.out("Total Machine Image Status Count", count);
                if( !supported ) {
                    assertTrue("Because machine images are not supported, the list of images should be empty", count == 0);
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
    public void listKernelImageStatus() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                boolean supported = false;

                for( ImageClass cls : support.getCapabilities().listSupportedImageClasses() ) {
                    if( cls.equals(ImageClass.KERNEL) ) {
                        supported = true;
                    }
                }
                Iterable<ResourceStatus> images = support.listImageStatus(ImageClass.KERNEL);
                int count = 0;

                assertNotNull("listImageStatus() must return a non-null list of images even if the image class is not supported", images);
                for( ResourceStatus status : images ) {
                    count++;
                    tm.out("Image Status", status);
                }
                tm.out("Total Kernel Image Status Count", count);
                if( !supported ) {
                    assertTrue("Because kernel images are not supported, the list of images should be empty", count == 0);
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
    public void listRamdiskImageStatus() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                boolean supported = false;

                for( ImageClass cls : support.getCapabilities().listSupportedImageClasses() ) {
                    if( cls.equals(ImageClass.RAMDISK) ) {
                        supported = true;
                    }
                }
                Iterable<ResourceStatus> images = support.listImageStatus(ImageClass.RAMDISK);
                int count = 0;

                assertNotNull("listImageStatus() must return a non-null list of images even if the image class is not supported", images);
                for( ResourceStatus status : images ) {
                    count++;
                    tm.out("Image Status", status);
                }
                tm.out("Total Ramdisk Image Status Count", count);
                if( !supported ) {
                    assertTrue("Because ramdisk images are not supported, the list of images should be empty", count == 0);
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
    public void compareImageListAndStatus() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
                Iterable<MachineImage> images = support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE));
                Iterable<ResourceStatus> status = support.listImageStatus(ImageClass.MACHINE);

                assertNotNull("listImages() must return at least an empty collections and may not be null", images);
                assertNotNull("listImageStatus() must return at least an empty collections and may not be null", status);
                for( ResourceStatus s : status ) {
                    Map<String,Boolean> current = map.get(s.getProviderResourceId());

                    if( current == null ) {
                        current = new HashMap<String, Boolean>();
                        map.put(s.getProviderResourceId(), current);
                    }
                    current.put("status", true);
                }
                for( MachineImage image : images ) {
                    Map<String,Boolean> current = map.get(image.getProviderMachineImageId());

                    if( current == null ) {
                        current = new HashMap<String, Boolean>();
                        map.put(image.getProviderMachineImageId(), current);
                    }
                    current.put("image", true);
                }
                for( Map.Entry<String,Map<String,Boolean>> entry : map.entrySet() ) {
                    Boolean s = entry.getValue().get("status");
                    Boolean i = entry.getValue().get("image");

                    assertTrue("Status and image lists do not match for " + entry.getKey(), s != null && i != null && s && i);
                }
                tm.out("Matches");
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
    public void findTestLinuxOrWindowsInPrivateLibrary() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                boolean supported = false;

                for( ImageClass cls : support.getCapabilities().listSupportedImageClasses() ) {
                    if( cls.equals(ImageClass.MACHINE) ) {
                        supported = true;
                    }
                }
                Iterable<MachineImage> images = support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).onPlatform(Platform.UBUNTU));
                int ubuntu = 0;

                assertNotNull("listImages() must return a non-null list of images even if the image class is not supported", images);
                for( MachineImage image : images ) {
                    ubuntu++;
                    tm.out("Ubuntu Image", image);
                }
                tm.out("Total Ubuntu Image Count", ubuntu);
                if( !supported ) {
                    assertTrue("Because machine images are not supported, the list of images should be empty", ubuntu == 0);
                }
                for( MachineImage image : images ) {
                    assertEquals("The platform for the image " + image.getProviderMachineImageId() + " is not Ubuntu", Platform.UBUNTU, image.getPlatform());
                }

                images = support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).onPlatform(Platform.CENT_OS));
                int centos = 0;

                assertNotNull("listImages() must return a non-null list of images even if the image class is not supported", images);
                for( MachineImage image : images ) {
                    centos++;
                    tm.out("CentOS Image", image);
                }
                tm.out("Total CentOS Image Count", centos);
                if( !supported ) {
                    assertTrue("Because machine images are not supported, the list of images should be empty", centos == 0);
                }
                for( MachineImage image : images ) {
                    assertEquals("The platform for the image " + image.getProviderMachineImageId() + " is not CentOS", Platform.CENT_OS, image.getPlatform());
                }

                images = support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).onPlatform(Platform.RHEL));
                int rhel = 0;

                assertNotNull("listImages() must return a non-null list of images even if the image class is not supported", images);
                for( MachineImage image : images ) {
                    rhel++;
                    tm.out("RHEL Image", image);
                }
                tm.out("Total RHEL Image Count", rhel);
                if( !supported ) {
                    assertTrue("Because machine images are not supported, the list of images should be empty", rhel == 0);
                }
                for( MachineImage image : images ) {
                    assertEquals("The platform for the image " + image.getProviderMachineImageId() + " is not RHEL", Platform.RHEL, image.getPlatform());
                }

                images = support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).onPlatform(Platform.WINDOWS));
                int windows = 0;

                assertNotNull("listImages() must return a non-null list of images even if the image class is not supported", images);
                for( MachineImage image : images ) {
                    windows++;
                    tm.out("Windows Image", image);
                }
                tm.out("Total Windows Image Count", windows);
                if( !supported ) {
                    assertTrue("Because machine images are not supported, the list of images should be empty", windows == 0);
                }
                for( MachineImage image : images ) {
                    assertEquals("The platform for the image " + image.getProviderMachineImageId() + " is not Windows", Platform.WINDOWS, image.getPlatform());
                }
                if( windows == 0 && ubuntu == 0 ) {
                    boolean shouldHave = false;

                    for( MachineImageType type : MachineImageType.values() ) {
                        if( support.getCapabilities().supportsImageCapture(type) ) {
                            shouldHave = true;
                        }
                    }
                    if( shouldHave || !support.getCapabilities().supportsPublicLibrary(ImageClass.MACHINE) ) {
                        tm.warn("No private Ubuntu or Windows images were found; the test may have failed or there may be nothing to find");
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
    public void findAllPublicImages() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                boolean supported = false;

                for( ImageClass cls : support.getCapabilities().listSupportedImageClasses() ) {
                    if( cls.equals(ImageClass.MACHINE) ) {
                        supported = true;
                    }
                }

                Iterable<MachineImage> images = support.searchPublicImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).matchingAny());
                int count = 0;

                assertNotNull("listImages() must return a non-null list of images even if the image class is not supported", images);
                for( MachineImage image : images ) {
                    count++;
                    tm.out("Image", image);
                }
                tm.out("Total Machine Image Count", count);
                if( !supported ) {
                    assertTrue("Because machine images are not supported, the list of images should be empty", count == 0);
                }
                if( count > 0 ) {
                    assertImageContent(images, ImageClass.MACHINE, Boolean.TRUE);
                }
            }
        }
    }

    @Test
    public void findUbuntuOrWindowsOrRHELInPublicLibrary() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                boolean supported = false;

                for( ImageClass cls : support.getCapabilities().listSupportedImageClasses() ) {
                    if( cls.equals(ImageClass.MACHINE) ) {
                        supported = true;
                    }
                }
                Iterable<MachineImage> images = support.searchPublicImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).onPlatform(Platform.UBUNTU));
                int ubuntu = 0;

                assertNotNull("searchPublicImages() must return a non-null list of images even if the image class is not supported or public libraries are not supported", images);
                for( MachineImage image : images ) {
                    ubuntu++;
                    tm.out("Ubuntu Public Image", image);
                }
                tm.out("Total Public Ubuntu Image Count", ubuntu);
                if( !supported || !support.getCapabilities().supportsPublicLibrary(ImageClass.MACHINE) ) {
                    assertTrue("Because public machine image libraries are not supported, the list of images should be empty", ubuntu == 0);
                }
                for( MachineImage image : images ) {
                    // if there are more than 100 images, check only one in five
                    if( ubuntu < 100 || random.nextInt(100) < 20 ) {
                        assertEquals("The platform for the image " + image.getProviderMachineImageId() + " is not Ubuntu", Platform.UBUNTU, image.getPlatform());
                        assertTrue("The image " + image.getProviderMachineImageId() + " is actually private", support.isImageSharedWithPublic(image.getProviderMachineImageId()));
                    }
                }

                images = support.searchPublicImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).onPlatform(Platform.RHEL));
                int rhel = 0;

                assertNotNull("searchPublicIMages() must return a non-null list of images even if the image class is not supported", images);
                for( MachineImage image : images ) {
                    rhel++;
                    tm.out("RHEL Image", image);
                }
                tm.out("Total Public RHEL Image Count", rhel);
                if( !supported || !support.getCapabilities().supportsPublicLibrary(ImageClass.MACHINE) ) {
                    assertTrue("Because public machine image libraries are not supported, the list of images should be empty", ubuntu == 0);
                }
                for( MachineImage image : images ) {
                    // if there are more than 100 images, check only one in five
                    if( rhel < 100 || random.nextInt(100) < 20 ) {
                        assertEquals("The platform for the image " + image.getProviderMachineImageId() + " is not RHEL", Platform.RHEL, image.getPlatform());
                        assertTrue("The image " + image.getProviderMachineImageId() + " is actually private", support.isImageSharedWithPublic(image.getProviderMachineImageId()));
                    }
                }


                images = support.searchPublicImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).onPlatform(Platform.WINDOWS));
                int windows = 0;

                assertNotNull("searchPublicImages() must return a non-null list of images even if the image class is not supported or public libraries are not supported", images);
                for( MachineImage image : images ) {
                    windows++;
                    tm.out("Windows Public Image", image);
                }
                tm.out("Total Public Windows Image Count", windows);
                if( !supported || !support.getCapabilities().supportsPublicLibrary(ImageClass.MACHINE) ) {
                    assertTrue("Because public machine images libraries are not supported, the list of images should be empty", windows == 0);
                }
                for( MachineImage image : images ) {
                    // if there are more than 100 images, check only one in five
                    if( windows < 100 || random.nextInt(100) < 20 ) {
                        assertEquals("The platform for the image " + image.getProviderMachineImageId() + " is not Windows", Platform.WINDOWS, image.getPlatform());
                        assertTrue("The image " + image.getProviderMachineImageId() + " is actually private", support.isImageSharedWithPublic(image.getProviderMachineImageId()));
                    }
                }
                if( windows == 0 && ubuntu == 0 ) {
                    if( supported && support.getCapabilities().supportsPublicLibrary(ImageClass.MACHINE) ) {
                        tm.warn("No private Ubuntu or Windows images were found; the test may have failed or there may be nothing to find");
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

    @SuppressWarnings("deprecation")
    @Test
    public void deprecation() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                assertListEquals("The deprecated listImages(ImageClass) method should match listImages(ImageFilterOptions=ImageClass)", support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE)), support.listImages(ImageClass.MACHINE));
                tm.out("listImages(ImageClass)", "Matches listImages(ImageFilterOptions)");
                assertListEquals("The deprecated listImages(ImageClass,String) method should match listImages(ImageFilterOptions=ImageClass,Account=String)", support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).withAccountNumber(tm.getContext().getAccountNumber())), support.listImages(ImageClass.MACHINE, tm.getContext().getAccountNumber()));
                tm.out("listImages(ImageClass,String)", "Matches listImages(ImageFilterOptions)");
                assertListEquals("The deprecated listMachineImages() method should match listImages(ImageFilterOptions=ImageClass.MACHINE)", support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE)), support.listMachineImages());
                tm.out("listMachineImages", "Matches listImages(ImageFilterOptions)");
                assertListEquals("The deprecated listMachineImagesOwnedBy(null) method should match listImages(ImageFilterOptions=ImageClass.MACHINE)", support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE)), support.listMachineImagesOwnedBy(null));
                tm.out("listMachineImagesOwnedBy(null)", "Matches listImages(ImageFilterOptions)");
                assertListEquals("The deprecated listMachineImagesOwnedBy(me) method should match listImages(ImageFilterOptions=ImageClass.MACHINE,Account=me)", support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).withAccountNumber(tm.getContext().getAccountNumber())), support.listMachineImagesOwnedBy(tm.getContext().getAccountNumber()));
                tm.out("listMachineImagesOwnedBy(String)", "Matches listImages(ImageFilterOptions)");
                ArrayList<MachineImage> expected = new ArrayList<MachineImage>();

                for( MachineImage img : support.searchPublicImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).onPlatform(Platform.UBUNTU)) ) {
                    expected.add(img);
                }
                for( MachineImage img : support.listImages(ImageFilterOptions.getInstance(ImageClass.MACHINE).onPlatform(Platform.UBUNTU)) ) {
                    if( !expected.contains(img) ) {
                        expected.add(img);
                    }
                }
                assertListEquals("The deprecated searchMachineImages(null,UBUNTU,null) method should match listImages(ImageFilterOptions=Platform.UBUNTU,ImageClass=MACHINE) + searchPublicImage(ImageFilterOptions=Platform.UBUNTU,ImageClass=MACHINE)", expected, support.searchMachineImages(null, Platform.UBUNTU, null));
                tm.out("searchMachineImages(String)", "Matches listImages(ImageFilterOptions=Platform.UBUNTU,ImageClass=MACHINE) + searchPublicImage(ImageFilterOptions=Platform.UBUNTU,ImageClass=MACHINE)");
                boolean customSupported = support.supportsDirectImageUpload();

                if( !customSupported ) {
                    for( MachineImageType type : MachineImageType.values() ) {
                        if( support.supportsImageCapture(type) ) {
                            customSupported = true;
                            break;
                        }
                    }
                }
                tm.out("supportsCustomImages()", (support.supportsCustomImages() == customSupported) ? "Matches supportsDirectImageUpload() || supportsImageCapture()" : "Does not match");
                assertEquals("The deprecated support.supportsCustomImages() should match supportsDirectImageUpload() || supportsImageCapture() for any image type", customSupported, support.supportsCustomImages());
                if( testImageId != null ) {
                    MachineImage actualImage = support.getMachineImage(testImageId);
                    MachineImage expectedImage = support.getImage(testImageId);

                    tm.out("getMachineImage(" + testImageId + ")", "Expected " + expectedImage + " ; Actual " + actualImage);
                    assertEquals("The results did not match", expectedImage, actualImage);
                }
                assertEquals("Term for old getProviderTermForImage() does not match the term for a machine image", support.getProviderTermForImage(Locale.getDefault(), ImageClass.MACHINE), support.getProviderTermForImage(Locale.getDefault()));
                tm.out("getProviderTermForImage(Locale)", "Matches getProviderTermForImage(Locale,ImageClass.MACHINE)");
                boolean pl = false;

                for( ImageClass cls : ImageClass.values() ) {
                    if( support.getCapabilities().supportsPublicLibrary(cls) ) {
                        pl = true;
                        break;
                    }
                }
                tm.out("hasPublicLibrary()/supportsPublicLibrary()", "Expected " + pl + " ; Got " + support.hasPublicLibrary());
                assertEquals("hasPublicLibrary() should match supportsPublicLibrary(ImageClass) across image classes", pl, support.hasPublicLibrary());
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
