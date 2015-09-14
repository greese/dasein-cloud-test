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

package org.dasein.cloud.test.cloud;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.dc.Folder;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.dc.ResourcePool;
import org.dasein.cloud.dc.StoragePool;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Test cases to validate an implementation of Dasein Cloud data center services.
 * <p>Created by George Reese: 2/18/13 5:51 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatelessDCTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessDCTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testDataCenterId;
    private String testResourcePoolId;
    private String testStoragePoolId;
    private String testFolderId;

    public StatelessDCTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        try {
            testDataCenterId = DaseinTestManager.getDefaultDataCenterId(true);

            DataCenterServices services = tm.getProvider().getDataCenterServices();

            if (name.getMethodName().contains("Pool") && testDataCenterId != null) {
                if (services.getCapabilities().supportsResourcePools()) {
                    for ( ResourcePool rp : services.listResourcePools(testDataCenterId)) {
                        if (testResourcePoolId == null) {
                            testResourcePoolId = rp.getProvideResourcePoolId();
                            break;
                        }
                    }
                }
                if (services.getCapabilities().supportsStoragePools()) {
                    for ( StoragePool storagePool : services.listStoragePools()) {
                        if (testStoragePoolId == null) {
                            testStoragePoolId = storagePool.getStoragePoolId();
                            break;
                        }
                    }
                }
            }

            if (name.getMethodName().contains("Folder")) {
                if (services.getCapabilities().supportsFolders()) {
                    for (Folder folder : services.listVMFolders()) {
                        if (testFolderId == null) {
                            testFolderId = folder.getId();
                            break;
                        }
                    }
                }
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void configuration() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        tm.out("DC Services", services);
        assertNotNull("Data center services must be implemented for all clouds", services);
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        tm.out("Term for Region", services.getCapabilities().getProviderTermForRegion(Locale.getDefault()));
        tm.out("Term for DataCenter", services.getCapabilities().getProviderTermForDataCenter(Locale.getDefault()));
        assertNotNull("The provider term for region may not be null", services.getCapabilities().getProviderTermForRegion(Locale.getDefault()));
        assertNotNull("The provider term for data center may not be null", services.getCapabilities().getProviderTermForDataCenter(Locale.getDefault()));
    }

    @Test
    public void getBogusRegion() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        Region region = services.getRegion(UUID.randomUUID().toString());

        tm.out("Bogus Region", region);
        assertNull("Dummy region must be null, but one was found", region);
    }

    @Test
    public void getRegion() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        Region region = services.getRegion(tm.getContext().getRegionId());

        tm.out("Region", region);
        assertNotNull("Failed to find the region associated with the current operational context", region);
    }

    @Test
    public void regionContent() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        Region region = services.getRegion(tm.getContext().getRegionId());

        assertNotNull("Failed to find the region associated with the current operational context", region);
        tm.out("Region ID", region.getProviderRegionId());
        tm.out("Active", region.isActive());
        tm.out("Available", region.isAvailable());
        tm.out("Name", region.getName());
        tm.out("Jurisdiction", region.getJurisdiction());
        assertNotNull("Region ID may not be null", region.getProviderRegionId());
        assertNotNull("Region name may not be null", region.getName());
        assertNotNull("Region jurisdiction may not be null", region.getJurisdiction());
    }

    @Test
    public void listRegions() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        Iterable<Region> regions = services.listRegions();
        boolean found = false;
        int count = 0;

        assertNotNull("Null set of regions returned from listRegions()", regions);
        for( Region region : regions ) {
            count++;
            tm.out("Region", region);
            if( region.getProviderRegionId().equals(tm.getContext().getRegionId()) ) {
                found = true;
            }
        }
        tm.out("Total Region Count", count);
        assertTrue("There must be at least one region", count > 0);
        assertTrue("Did not find the context region ID among returned regions", found);
    }

    @Test
    public void getBogusDataCenter() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        DataCenter dc = services.getDataCenter(UUID.randomUUID().toString());

        tm.out("Bogus Data Center", dc);
        assertNull("Dummy data center must be null, but one was found", dc);
    }

    @Test
    public void getDataCenter() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        DataCenter dc = services.getDataCenter(testDataCenterId);

        tm.out("Data Center", dc);
        assertNotNull("Failed to find the test data center", dc);
    }

    @Test
    public void dataCenterContent() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        DataCenter dc = services.getDataCenter(testDataCenterId);

        assertNotNull("Failed to find the test data center", dc);
        tm.out("Data Center ID", dc.getProviderDataCenterId());
        tm.out("Active", dc.isActive());
        tm.out("Available", dc.isAvailable());
        tm.out("Name", dc.getName());
        tm.out("Region ID", dc.getRegionId());
        assertNotNull("Data center ID must not be null", dc.getProviderDataCenterId());
        assertNotNull("Data center name must not be null", dc.getName());
        assertEquals("Data center should be in the current region", tm.getContext().getRegionId(), dc.getRegionId());
    }

    @Test
    public void listDataCenters() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        Iterable<DataCenter> dataCenters = services.listDataCenters(tm.getContext().getRegionId());
        boolean found = false;
        int count = 0;

        assertNotNull("Null set of data centers returned from listDataCenters()", dataCenters);
        for( DataCenter dc : dataCenters ) {
            count++;
            tm.out("Data Center", dc);
            if( dc.getProviderDataCenterId().equals(testDataCenterId) ) {
                found = true;
            }
        }
        tm.out("Total Data Center Count", count);
        assertTrue("There must be at least one data center in this region", count > 0);
        assertTrue("Did not find the test data center ID among returned data centers", found);
    }

    @Test
    public void regionIntegrity() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        for( Region region : services.listRegions() ) {
            if( region.isActive() ) {
                int count = 0;

                for( DataCenter dc : services.listDataCenters(region.getProviderRegionId()) ) {
                    if( dc.isActive() ) {
                        count++;
                    }
                }
                tm.out("Data Centers in " + region, count);
                assertTrue("An active region must have at least one active data center; " + region.getProviderRegionId() + " has none", count > 0);
            }
        }
    }

    //Resource pool tests
    @Test
    public void getBogusResourcePool() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        if (services.getCapabilities().supportsResourcePools()) {
            ResourcePool rp = services.getResourcePool(UUID.randomUUID().toString());
            tm.out("Bogus Resource pool", rp);
            assertNull("Dummy resource pool must be null, but one was found", rp);
        }
        else {
            tm.ok("Resource pools not supported in "+tm.getProvider().getCloudName());
        }
    }

    @Test
    public void getResourcePool() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        if (testResourcePoolId != null) {
            ResourcePool rp = services.getResourcePool(testResourcePoolId);

            tm.out("Resource Pool", rp+" ["+rp.getProvideResourcePoolId()+"]");
            assertNotNull("Failed to find the test resource pool", rp);
        }
        else {
            if (services.getCapabilities().supportsResourcePools()) {
                fail("No test resource pool exists and thus no test for getResourcePool could be run");
            }
            else {
                tm.ok("Resource pools not supported in "+tm.getProvider().getCloudName());
            }
        }
    }

    @Test
    public void resourcePoolContent() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        if (testResourcePoolId != null) {
            ResourcePool rp = services.getResourcePool(testResourcePoolId);

            assertNotNull("Failed to find the test resource pool", rp);
            tm.out("Resource Pool ID", rp.getProvideResourcePoolId());
            tm.out("Name", rp.getName());
            tm.out("Data center ID", rp.getDataCenterId());
            tm.out("Available", rp.isAvailable());
            assertNotNull("Resource Pool ID must not be null", rp.getProvideResourcePoolId());
            assertNotNull("Resource Pool name must not be null", rp.getName());
            assertNotNull("Data center id must not be null", rp.getDataCenterId());
        }
        else {
            if (services.getCapabilities().supportsResourcePools()) {
                fail("No test resource pool exists and thus no test for resourcePoolContent could be run");
            }
            else {
                tm.ok("Resource pools not supported in "+tm.getProvider().getCloudName());
            }
        }
    }

    @Test
    public void listResourcePools() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        if (services.getCapabilities().supportsResourcePools() ) {
            Iterable<ResourcePool> resourcePools = services.listResourcePools(testDataCenterId);
            boolean found = false;
            int count = 0;

            assertNotNull("Null set of resource pools returned from listResourcePools()", resourcePools);
            for( ResourcePool resourcePool : resourcePools ) {
                count++;
                tm.out("Resource Pool", resourcePool+" ["+resourcePool.getProvideResourcePoolId()+"]");
                if( resourcePool.getProvideResourcePoolId().equals(testResourcePoolId) ) {
                    found = true;
                }
            }
            tm.out("Total Resource Pool Count", count);
            assertTrue("There must be at least one Resource Pool in this datacenter", count > 0);
            assertTrue("Did not find the test Resource Pool ID among returned Resource Pools", found);
        }
        else {
            tm.ok("Resource pools not supported in "+tm.getProvider().getCloudName());
        }
    }
    //End resource pool tests

    //Storage pool tests
    @Test
    public void getBogusStoragePool() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        if (services.getCapabilities().supportsStoragePools()) {
            StoragePool storagePool = services.getStoragePool(UUID.randomUUID().toString());
            tm.out("Bogus Storage pool", storagePool);
            assertNull("Dummy storage pool must be null, but one was found", storagePool);
        }
        else {
            tm.ok("Storage pools not supported in "+tm.getProvider().getCloudName());
        }
    }

    @Test
    public void getStoragePool() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        if (testStoragePoolId != null) {
            StoragePool storagePool = services.getStoragePool(testStoragePoolId);

            tm.out("Storage Pool", storagePool+" ["+storagePool.getStoragePoolId()+"]");
            assertNotNull("Failed to find the test storage pool", storagePool);
        }
        else {
            if (services.getCapabilities().supportsStoragePools()) {
                fail("No test storage pool exists and thus no test for getStoragePool could be run");
            }
            else {
                tm.ok("Storage pools not supported in "+tm.getProvider().getCloudName());
            }
        }
    }

    @Test
    public void storagePoolContent() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        if (testStoragePoolId != null) {
            StoragePool storagePool = services.getStoragePool(testStoragePoolId);

            assertNotNull("Failed to find the test storage pool", storagePool);
            tm.out("Storage Pool ID", storagePool.getStoragePoolId());
            tm.out("Name", storagePool.getStoragePoolName());
            tm.out("Data center ID", storagePool.getDataCenterId());
            tm.out("Region ID", storagePool.getRegionId());
            tm.out("Affinity group", storagePool.getAffinityGroupId());
            tm.out("Capacity", storagePool.getCapacity());
            tm.out("Provisioned", storagePool.getProvisioned());
            tm.out("Free space", storagePool.getFreeSpace());
            assertNotNull("Storage Pool ID must not be null", storagePool.getStoragePoolId());
            assertNotNull("Storage Pool name must not be null", storagePool.getStoragePoolName());
        }
        else {
            if (services.getCapabilities().supportsStoragePools()) {
                fail("No test storage pool exists and thus no test for storagePoolContent could be run");
            }
            else {
                tm.ok("Storage pools not supported in "+tm.getProvider().getCloudName());
            }
        }
    }

    @Test
    public void listStoragePools() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        if (services.getCapabilities().supportsStoragePools() ) {
            Iterable<StoragePool> storagePools = services.listStoragePools();
            boolean found = false;
            int count = 0;

            assertNotNull("Null set of storage pools returned from listStoragePools()", storagePools);
            for( StoragePool storagePool : storagePools ) {
                count++;
                tm.out("Storage Pool", storagePool+" ["+storagePool.getStoragePoolId()+"]");
                if( storagePool.getStoragePoolId().equals(testStoragePoolId) ) {
                    found = true;
                }
            }
            tm.out("Total Storage Pool Count", count);
            assertTrue("There must be at least one Storage Pool in this datacenter", count > 0);
            assertTrue("Did not find the test Storage Pool ID among returned Storage Pools", found);
        }
        else {
            tm.ok("Storage pools not supported in "+tm.getProvider().getCloudName());
        }
    }
    //End storage pool tests

    //VM folder tests
    @Test
    public void getBogusVMFolder() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        if (services.getCapabilities().supportsFolders()) {
            Folder folder = services.getVMFolder(UUID.randomUUID().toString());
            tm.out("Bogus VM folder", folder);
            assertNull("Dummy VM folder must be null, but one was found", folder);
        }
        else {
            tm.ok("Folders not supported in "+tm.getProvider().getCloudName());
        }
    }

    @Test
    public void getVMFolder() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        if (testFolderId != null) {
            Folder folder = services.getVMFolder(testFolderId);

            tm.out("VMFolder", folder+" ["+folder.getId()+"]");
            assertNotNull("Failed to find the test folder", folder);
        }
        else {
            if (services.getCapabilities().supportsFolders()) {
                fail("No test folder exists and thus no test for getVMFolder could be run");
            }
            else {
                tm.ok("Folders not supported in "+tm.getProvider().getCloudName());
            }
        }
    }

    @Test
    public void vmFolderContent() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        if (testFolderId != null) {
            Folder folder = services.getVMFolder(testFolderId);

            assertNotNull("Failed to find the test folder", folder);
            tm.out("VM folder ID", folder.getId());
            tm.out("Name", folder.getName());
            tm.out("Type", folder.getType());
            if (folder.getParent() != null) {
                tm.out("Parent", folder.getParent().getName());
            }

            List<Folder> children = folder.getChildren();
            for (Folder child : children) {
                tm.out("Child folder", child.getName());
            }
            assertNotNull("VM folder ID must not be null", folder.getId());
            assertNotNull("VM folder name must not be null", folder.getName());
            assertNotNull("Type must not be null", folder.getType());
        }
        else {
            if (services.getCapabilities().supportsFolders()) {
                fail("No test folder exists and thus no test for vmFolderContent could be run");
            }
            else {
                tm.ok("Folders not supported in "+tm.getProvider().getCloudName());
            }
        }
    }

    @Test
    public void listVMFolders() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        if (services.getCapabilities().supportsFolders() ) {
            Iterable<Folder> folders = services.listVMFolders();
            boolean found = false;
            int count = 0;

            assertNotNull("Null set of folders returned from listVMFolders()", folders);
            for( Folder folder : folders ) {
                count++;
                tm.out("VM folder", folder+" ["+folder.getId()+"]");
                if( folder.getId().equals(testFolderId) ) {
                    found = true;
                }
            }
            tm.out("Total VM folder Count", count);
            assertTrue("There must be at least one VM folder", count > 0);
            assertTrue("Did not find the test folder ID among returned folders", found);
        }
        else {
            tm.ok("Folders not supported in "+tm.getProvider().getCloudName());
        }
    }
    //End VM folder tests
}
