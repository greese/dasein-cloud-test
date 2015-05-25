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
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/20/13 6:10 PM</p>
 *
 * @author George Reese
 */
public class StatelessVolumeTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessVolumeTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testProductId;
    private String testVolumeId;
    private String testDataCenterId;

    public StatelessVolumeTests() { }

    private void assertProductContent(@Nonnull VolumeSupport support, @Nonnull VolumeProduct product) throws CloudException, InternalException {
        assertNotNull("Volume product ID may not be null", product.getProviderProductId());
        assertNotNull("Volume product name may not be null", product.getName());
        assertNotNull("Volume product description may not be null", product.getDescription());
        assertNotNull("Volume product type may not be null", product.getType());
        assertTrue("Volume product max IOPS must be greater than or equal to the min IOPS", product.getMaxIops() >= product.getMinIops());

        Float f = product.getIopsCost();

        if( f != null ) {
            assertTrue("Volume product IOPS cost must be non-negative", f >= 0.00f);
        }

        f = product.getMonthlyGigabyteCost();

        if( f != null ) {
            assertTrue("Volume product monthly per-GB cost must be non-negative", f >= 0.00f);
        }

        Storage<Gigabyte> size = product.getVolumeSize();

        if( support.getCapabilities().isVolumeSizeDeterminedByProduct() ) {
            assertNotNull("Volume size may not be null when the volume size is determined by the product", size);
            assertTrue("Volume size must be a positive number when the volume size is determined by the product", size.floatValue() >= 0.01f);
        }
        else if( size != null ) {
            assertTrue("Volume size must be non-negative", size.floatValue() >= 0.00f);
        }
    }

    private void assertVolumeContent(@Nonnull VolumeSupport support, @Nonnull Volume volume) throws CloudException, InternalException {
        assertNotNull("Volume ID may not be null", volume.getProviderVolumeId());
        assertNotNull("Volume state may not be null", volume.getCurrentState());
        assertNotNull("Volume name may not be null", volume.getName());
        assertNotNull("Volume description may not be null", volume.getDescription());
        assertTrue("Volume creation date must be non-negative", volume.getCreationTimestamp() > -1L);
        if( volume.getCreationTimestamp() < 2L ) {
            tm.warn("Meaningless creation timestamp with volumes; does the cloud provider support this information?");
        }
        assertNotNull("Volume region ID may not be null", volume.getProviderRegionId());
        assertNotNull("Volume data center ID may not be null", volume.getProviderDataCenterId());
        if( support.getCapabilities().getVolumeProductRequirement().equals(Requirement.REQUIRED) ) {
            assertNotNull("Volume product may not be null when a product requirement is REQUIRED", volume.getProviderProductId());
        }
        if( volume.isAttached() ) {
            assertNotNull("The volume virtual machine ID may not be null when attached", volume.getProviderVirtualMachineId());
        }
        else {
            assertNull("The volume virtual machine ID mustbe null when not attached", volume.getProviderVirtualMachineId());
        }
        Storage<Gigabyte> size = volume.getSize();

        assertNotNull("The volume size may not be null", size);
        assertTrue("The volume size must be greater than 0", size.floatValue() > 0.00f);
        assertNotNull("Volume type may not be null", volume.getType());
        assertNotNull("Volume format may not be null", volume.getFormat());
        if( volume.isRootVolume() ) {
            assertNotNull("Guest OS may not be null for root volumes", volume.getGuestOperatingSystem());
        }
        assertNotNull("Tags may be empty, but they may not be null", volume.getTags());
    }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        testDataCenterId = DaseinTestManager.getDefaultDataCenterId(true);

        testVolumeId = tm.getTestVolumeId(DaseinTestManager.STATELESS, true, null, testDataCenterId);
        testProductId = tm.getTestVolumeProductId();
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                boolean subscribed = support.isSubscribed();

                tm.out("Subscribed", subscribed);

                tm.out("Term for Volume", support.getCapabilities().getProviderTermForVolume(Locale.getDefault()));

                int maxVolumes = support.getCapabilities().getMaximumVolumeCount();

                tm.out("Max Volume Count", (maxVolumes == -2 ? "Unknown" : (maxVolumes == -1 ? "Unlimited" : String.valueOf(maxVolumes))));

                Storage<Gigabyte> maxSize = support.getCapabilities().getMaximumVolumeSize();

                tm.out("Max Volume Size", maxSize == null ? "Unknown" : maxSize);
                tm.out("Min Volume Size", support.getCapabilities().getMinimumVolumeSize());
                tm.out("Product Required", support.getCapabilities().getVolumeProductRequirement());
                tm.out("Product Determines Size", support.getCapabilities().isVolumeSizeDeterminedByProduct());

                Iterable<VolumeFormat> formats = support.getCapabilities().listSupportedFormats();

                tm.out("Supported Formats", formats);

                for( Platform platform : Platform.values() ) {
                    Iterable<String> deviceIds = support.getCapabilities().listPossibleDeviceIds(platform);

                    tm.out("Device IDs [" + platform + "]", deviceIds);
                }
                assertNotNull("The provider term for a volume must not be null", support.getCapabilities().getProviderTermForVolume(Locale.getDefault()));
                assertTrue("Maximum volumes must be -2 or greater", maxVolumes >= -2);
                assertTrue("Maximum volume size must be non-negative", maxSize == null || maxSize.intValue() > -1);
                assertNotNull("Minimum volume size may not be null", support.getCapabilities().getMinimumVolumeSize());
                assertTrue("Minimum volume size must be at least 1K", support.getCapabilities().getMinimumVolumeSize().intValue() >= 0.01f);
                assertNotNull("Product requirement must not be null", support.getCapabilities().getVolumeProductRequirement());
                assertNotNull("Supported formats must be non-null and have at least one member when subscribed", formats);
                assertTrue("There must be at least one supported format when subscribed", !subscribed || formats.iterator().hasNext());

                for( Platform platform : Platform.values() ) {
                    Iterable<String> deviceIds = support.getCapabilities().listPossibleDeviceIds(platform);

                    assertNotNull("The list of device IDs for " + platform + " may not be null", deviceIds);
                    assertTrue("There must be at least one device ID for " + platform, deviceIds.iterator().hasNext());
                }
            }
            else {
                tm.ok(tm.getProvider().getCloudName() + " does not support volumes");
            }
        }
        else {
            tm.ok(tm.getProvider().getCloudName() + " does not support compute services");
        }
    }


    @Test
    public void volumeProductContent() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                if( testProductId != null ) {
                    VolumeProduct product = null;

                    for( VolumeProduct p : support.listVolumeProducts() ) {
                        if( p.getProviderProductId().equals(testProductId) ) {
                            product = p;
                            break;
                        }
                    }
                    assertNotNull("Unable to find the test volume product " + testProductId, product);

                    tm.out("Volume Product ID", product.getProviderProductId());
                    tm.out("Name", product.getName());
                    tm.out("Type", product.getType());
                    tm.out("Size", product.getVolumeSize());
                    tm.out("Min IOPS", product.getMinIops());
                    tm.out("Max IOPS", product.getMaxIops());
                    tm.out("Currency", product.getCurrency());
                    tm.out("IOPS Cost", product.getIopsCost());
                    tm.out("Storage Cost", product.getMonthlyGigabyteCost());
                    tm.out("Description", product.getDescription());

                    assertProductContent(support, product);
                }
                else {
                    if( support.getCapabilities().getVolumeProductRequirement().equals(Requirement.REQUIRED) ) {
                        fail("No test product exists for this test even though products are supported");
                    }
                    else {
                        tm.ok("Volume products are not required in this cloud");
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void listVolumeProducts() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                Iterable<VolumeProduct> products = support.listVolumeProducts();
                int count = 0;

                assertNotNull("The volume product list may possibly be empty, but it may not be null", products);
                for( VolumeProduct product : products ) {
                    count++;
                    tm.out("Volume Product", product);
                }
                tm.out("Total Volume Product Count", count);

                if( !support.isSubscribed() ) {
                    assertTrue("The account is not subscribed, but the volume count is non-zero", count == 0);
                }
                else if( support.getCapabilities().getVolumeProductRequirement().equals(Requirement.NONE) ) {
                    assertEquals("This cloud does not support volume products, but there's at least one product", 0, count);
                }
                else if( support.getCapabilities().getVolumeProductRequirement().equals(Requirement.REQUIRED) ) {
                    assertTrue("There must be at least one product in this cloud", count > 0);
                }
                for( VolumeProduct product : products ) {
                    assertProductContent(support, product);
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }


    @Test
    public void getBogusVolume() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                Volume volume = support.getVolume(UUID.randomUUID().toString());

                tm.out("Bogus Volume", volume);
                assertNull("Bogus volume was supposed to be none, but got a valid volume.", volume);
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
     public void getVolume() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                if( testVolumeId != null ) {
                    Volume volume = support.getVolume(testVolumeId);

                    tm.out("Volume", volume);
                    assertNotNull("Unable to find the test volume in the cloud", volume);
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.ok("No test volume exists, but the account is not subscribed");
                    }
                    else {
                        fail("No test volume exists in support for the " + name.getMethodName() + " test");
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void volumeContent() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                if( testVolumeId != null ) {
                    Volume volume = support.getVolume(testVolumeId);

                    assertNotNull("Unable to find the test volume in the cloud", volume);
                    tm.out("Volume ID", volume.getProviderVolumeId());
                    tm.out("Current State", volume.getCurrentState());
                    tm.out("Name", volume.getName());
                    tm.out("Created", new Date(volume.getCreationTimestamp()));
                    tm.out("Region ID", volume.getProviderRegionId());
                    tm.out("Data Center ID", volume.getProviderDataCenterId());
                    tm.out("Product ID", volume.getProviderProductId());
                    tm.out("IIOPS", volume.getIops());
                    tm.out("Size", volume.getSize());
                    tm.out("Snapshot ID", volume.getProviderSnapshotId());
                    tm.out("Attached",volume.isAttached());
                    tm.out("Attached To", volume.getProviderVirtualMachineId());
                    tm.out("Device ID", volume.getDeviceId());
                    tm.out("Type", volume.getType());
                    tm.out("Format", volume.getFormat());
                    tm.out("In VLAN", volume.getProviderVlanId());
                    tm.out("Is Root", volume.isRootVolume());
                    tm.out("Guest OS", volume.getGuestOperatingSystem());
                    tm.out("Media Link", volume.getMediaLink());

                    Map<String,String> tags = volume.getTags();

                    //noinspection ConstantConditions
                    if( tags != null ) {
                        for( Map.Entry<String,String> entry : tags.entrySet() ) {
                            tm.out("Tag " + entry.getKey(), entry.getValue());
                        }
                    }
                    tm.out("Description", volume.getDescription());

                    assertVolumeContent(support, volume);
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.ok("No test volume exists, but the account is not subscribed");
                    }
                    else {
                        fail("No test volume exists in support for the " + name.getMethodName() + " test");
                    }
                }

            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void listVolumes() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                Iterable<Volume> volumes = support.listVolumes();
                int count = 0;

                assertNotNull("The volumes list may be empty, but it may not be null", volumes);
                for( Volume volume : volumes ) {
                    count++;
                    tm.out("Volume", volume);
                }
                tm.out("Total Volume Count", count);

                if( !support.isSubscribed() ) {
                    assertTrue("The account is not subscribed, but the volume count is non-zero", count == 0);
                }
                else if( count == 0 ) {
                    tm.warn("No volumes were returned from the list request so the results of this test are questionable");
                }
                if( count > 0 ) {
                    for( Volume volume : volumes ) {
                        assertVolumeContent(support, volume);
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void listVolumeStatus() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                Iterable<ResourceStatus> volumes = support.listVolumeStatus();
                int count = 0;

                assertNotNull("The volume status list may be empty, but it may not be null", volumes);
                for( ResourceStatus volume : volumes ) {
                    count++;
                    tm.out("Volume Status", volume);
                }
                tm.out("Total Volume Status Count", count);

                if( !support.isSubscribed() ) {
                    assertTrue("The account is not subscribed, but the volume status count is non-zero", count == 0);
                }
                else if( count == 0 ) {
                    tm.warn("No volume status instances were returned from the list request so the results of this test are questionable");
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void compareVolumeListAndStatus() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
                Iterable<Volume> volumes = support.listVolumes();
                Iterable<ResourceStatus> status = support.listVolumeStatus();

                assertNotNull("listVolumes() must return at least an empty collections and may not be null", volumes);
                assertNotNull("listVolumeStatus() must return at least an empty collection and may not be null", status);
                for( ResourceStatus s : status ) {
                    Map<String,Boolean> current = map.get(s.getProviderResourceId());

                    if( current == null ) {
                        current = new HashMap<String, Boolean>();
                        map.put(s.getProviderResourceId(), current);
                    }
                    current.put("status", true);
                }
                for( Volume volume : volumes ) {
                    Map<String,Boolean> current = map.get(volume.getProviderVolumeId());

                    if( current == null ) {
                        current = new HashMap<String, Boolean>();
                        map.put(volume.getProviderVolumeId(), current);
                    }
                    current.put("volume", true);
                }
                for( Map.Entry<String,Map<String,Boolean>> entry : map.entrySet() ) {
                    Boolean s = entry.getValue().get("status");
                    Boolean v = entry.getValue().get("volume");

                    assertTrue("Status and volume lists do not match for " + entry.getKey(), s != null && v != null && s && v);
                }
                tm.out("Matches");
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }
}
