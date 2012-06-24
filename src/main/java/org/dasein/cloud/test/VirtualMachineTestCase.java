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
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VirtualMachineTestCase extends BaseTestCase {
    private CloudProvider cloud         = null;
    private String        testVm        = null;
    private String        vmToTerminate = null;
    
    public VirtualMachineTestCase(String name) { super(name); }
    
    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        String name = getName();
        
        cloud = getProvider();
        cloud.connect(getTestContext());
        if( name.equals("testVirtualMachineContent") ) {
            for( VirtualMachine vm : cloud.getComputeServices().getVirtualMachineSupport().listVirtualMachines() ) {
                testVm = vm.getProviderVirtualMachineId();
                break;
            }
            if( testVm == null ) {
                vmToTerminate = launch(cloud);
                testVm = vmToTerminate;
            }
        }
        if( name.equals("testTerminate") ) {
            vmToTerminate = launch(cloud);
            testVm = vmToTerminate;            
        }
        if( (name.equals("testEnableAnalytics") || name.equals("testDisableAnalytics")) && cloud.getComputeServices().getVirtualMachineSupport().supportsAnalytics() ) {
            vmToTerminate = launch(cloud);
            testVm = vmToTerminate;                        
        }
    }
    
    @After
    @Override
    public void tearDown() {
        try {
            if( vmToTerminate != null ) {
                cloud.getComputeServices().getVirtualMachineSupport().terminate(vmToTerminate);
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
    public void testDisableAnalytics() throws CloudException, InternalException {
        begin();
        cloud.getComputeServices().getVirtualMachineSupport().disableAnalytics(testVm);
        end();
    }
    
    @Test
    public void testEnableAnalytics() throws CloudException, InternalException {
        begin();
        cloud.getComputeServices().getVirtualMachineSupport().enableAnalytics(testVm);
        end();
    }
    
    @Test
    public void testGetBogusVirtualMachine() throws InternalException, CloudException {
        begin();
        VirtualMachine vm = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(UUID.randomUUID().toString());
        
        assertNull("Found a VM matching the bogus ID", vm);
        end();
    }
    
    @Test
    public void testLaunchVirtualMachine() throws InternalException, CloudException {
        begin();
        VirtualMachineSupport vmSupport = cloud.getComputeServices().getVirtualMachineSupport();
        VirtualMachine vm = vmSupport.launch(getTestMachineImageId(), vmSupport.getProduct(getTestProduct()), getTestDataCenterId(), getTestHostname(), "dsnvm test", null, null, false, false, new String[0]);
        
        assertNotNull("Failed to return a launched virtual machine", vm);
        assertNotNull("VM has no ID", vm.getProviderOwnerId());
        vmToTerminate = vm.getProviderVirtualMachineId();
        assertEquals("Virtual machine failed to launch in target region", cloud.getContext().getRegionId(), vm.getProviderRegionId());
        assertEquals("Virtual machine failed to launch in target data center", getTestDataCenterId(), vm.getProviderDataCenterId());
        assertEquals("Virtual machine not owned by launcher", cloud.testContext(), vm.getProviderOwnerId());
        out("Launched: " + vm);
        end();
    }
    
    @Test
    public void testListVirtualMachines() throws InternalException, CloudException {
        begin();
        Iterable<VirtualMachine> vms = cloud.getComputeServices().getVirtualMachineSupport().listVirtualMachines();
        
        assertNotNull("Virtual machine listing may not be null", vms);
        try {
            for( VirtualMachine vm : vms ) {
                out("VM: " + vm);
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
        VirtualMachineSupport vmSupport = cloud.getComputeServices().getVirtualMachineSupport();
        
        assertNotNull("You must specify a provider term for virtual machine", vmSupport.getProviderTermForServer(Locale.getDefault()));
        out("Term:       " + vmSupport.getProviderTermForServer(Locale.getDefault()));
        out("Analytics:  " + vmSupport.supportsAnalytics());
        end();
    }
    
    @Test
    public void testProductList() throws CloudException, InternalException {
        begin();
        VirtualMachineSupport vmSupport = cloud.getComputeServices().getVirtualMachineSupport();
        int count = 0;
        
        for( Architecture architecture : Architecture.values() ) {
            Iterable<VirtualMachineProduct> products = vmSupport.listProducts(architecture);
            
            assertNotNull("Received a null product list for " + architecture, products);
            for( VirtualMachineProduct product : products ) {
                out("Product: " + product);
                assertNotNull("Product ID cannot be null", product.getProductId());
                assertTrue("CPU count must be at least 1", product.getCpuCount() > 0);
                assertNotNull("Product name cannot be null", product.getName());
                assertNotNull("Product description cannot be null", product.getDescription());
                assertTrue("Disk size must be at least 1 GB", product.getDiskSizeInGb() > 0);
                assertTrue("RAM size must be at least 256 MB", product.getRamInMb() >= 256);
                count++;
            }
        }
        assertTrue("No products exist in this cloud and therefore no VMs are provisionable", count > 0);
        end();
    }
    
    @Test
    public void testSubscription() throws CloudException, InternalException {
        begin();
        VirtualMachineSupport vmSupport = cloud.getComputeServices().getVirtualMachineSupport();
        
        assertTrue("Account must be subscribed to test virtual machine support", vmSupport.isSubscribed());
        end();        
    }
    
    @Test
    public void testTerminate() throws CloudException, InternalException {
        begin();
        cloud.getComputeServices().getVirtualMachineSupport().terminate(vmToTerminate);
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        VirtualMachine vm = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(vmToTerminate);
        
        assertTrue("VM is still running", vm == null || !vm.getCurrentState().equals(VmState.RUNNING));
        vmToTerminate = null;
        testVm = null;
        end();
    }
    
    @Test 
    public void testVirtualMachineContent() throws InternalException, CloudException {
        begin();
        VirtualMachine vm = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(testVm);
        
        assertNotNull("No VM matching the test ID was found", vm);
        assertEquals("The ID of the retrieved VM does not match", testVm, vm.getProviderVirtualMachineId());
        assertNotNull("A VM must have a name", vm.getName());
        assertNotNull("A VM must have a description", vm.getDescription());
        assertNotNull("A VM must have an owner", vm.getProviderOwnerId());
        assertEquals("The VM region must match", cloud.getContext().getRegionId(), vm.getProviderRegionId());
        assertNotNull("The VM data center cannot be null", vm.getProviderDataCenterId());
        assertNotNull("A VM must have an architecture", vm.getArchitecture());
        assertNotNull("A VM must have a platform", vm.getPlatform());
        assertNotNull("A VM must have a product", vm.getProduct());
        try {
            out("VM ID:         " + vm.getProviderVirtualMachineId());
            out("Name:          " + vm.getName());
            out("Owner:         " + vm.getProviderOwnerId());
            out("Region:        " + vm.getProviderRegionId());
            out("Data Center:   " + vm.getProviderDataCenterId());
            out("VLAN:          " + vm.getProviderVlanId());
            out("Subnet:        " + vm.getProviderSubnetId());
            String[] addrs = vm.getPrivateIpAddresses();
            out("Private IP:    " + ((addrs == null || addrs.length < 1) ? "none" : addrs[0]));
            addrs = vm.getPublicIpAddresses();
            out("Public IP:     " + ((addrs == null || addrs.length < 1) ? "none" : addrs[0]));
            out("Machine image: " + vm.getProviderMachineImageId());
            out("Created:       " + (new Date(vm.getCreationTimestamp())));
            out("Architecture:  " + vm.getArchitecture());
            out("Platform:      " + vm.getPlatform());
            out("Assigned:      " + vm.getProviderAssignedIpAddressId());
            out("Product:       " + vm.getProduct());
            out("State:         " + vm.getCurrentState());
            out("Description:\n" + vm.getDescription());
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }
}
