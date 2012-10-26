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

import java.awt.datatransfer.StringSelection;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.identity.ShellKeySupport;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.SubnetState;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Megabyte;
import org.dasein.util.uom.storage.Storage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;

public class VirtualMachineTestCase extends BaseTestCase {
    private CloudProvider   cloud             = null;
    private VMLaunchOptions testLaunchOptions = null;
    private String          testVm            = null;
    private String          vmToTerminate     = null;
    
    public VirtualMachineTestCase(String name) { super(name); }

    private @Nonnull VirtualMachineSupport getSupport() {
        //noinspection ConstantConditions
        return cloud.getComputeServices().getVirtualMachineSupport();
    }

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
        if( name.equals("testTerminate") || name.equals("testStart") || name.equals("testStop") || name.equals("testPause") || name.equals("testUnpause") || name.equals("testSuspend") || name.equals("testResume") ) {
            vmToTerminate = launch(cloud);
            testVm = vmToTerminate;
        }
        if( (name.equals("testEnableAnalytics") || name.equals("testDisableAnalytics")) && cloud.getComputeServices().getVirtualMachineSupport().supportsAnalytics() ) {
            vmToTerminate = launch(cloud);
            testVm = vmToTerminate;                        
        }
        if( name.startsWith("testLaunch") ) {
            String hostName = "dsntestlaunch-" + (System.currentTimeMillis()%10000);

            testLaunchOptions = VMLaunchOptions.getInstance(getTestProduct(), getTestMachineImageId(), hostName, hostName, "DSN Test Host - " + getName());
            VirtualMachineSupport s = getSupport();

            testLaunchOptions.inDataCenter(getTestDataCenterId());
            if( s.identifyPasswordRequirement().equals(Requirement.REQUIRED) ) {
                testLaunchOptions.withBootstrapUser("dasein", "x" + System.currentTimeMillis());
            }
            if( s.identifyRootVolumeRequirement().equals(Requirement.REQUIRED) ) {
                String productId = null;

                for( VolumeProduct p :cloud.getComputeServices().getVolumeSupport().listVolumeProducts() ) {
                    productId = p.getProviderProductId();
                }
                assertNotNull("Cannot identify a volume product for the root volume.", productId);
                testLaunchOptions.withRootVolumeProduct(productId);
            }
            if( s.identifyShellKeyRequirement().equals(Requirement.REQUIRED) ) {
                ShellKeySupport sks = cloud.getIdentityServices().getShellKeySupport();
                String keyId = null;

                if( sks.getKeyImportSupport().equals(Requirement.REQUIRED) ) {
                    fail("Import not yet supported in test cases.");
                }
                else {
                    keyId = sks.createKeypair(hostName).getProviderKeypairId();
                }
                //noinspection ConstantConditions
                testLaunchOptions.withBoostrapKey(keyId);
            }
            if( s.identifyVlanRequirement().equals(Requirement.REQUIRED) ) {
                VLANSupport vs = cloud.getNetworkServices().getVlanSupport();

                assertNotNull("No VLAN support but a vlan is required.", vs);
                String vlanId = null;

                VLAN testVlan = null;

                for( VLAN vlan : vs.listVlans() ) {
                    if( vlan.getCurrentState().equals(VLANState.AVAILABLE) && (!vs.isVlanDataCenterConstrained() || vlan.getProviderDataCenterId().equals(getTestDataCenterId())) ) {
                        testVlan = vlan;
                    }
                }
                assertNotNull("Test VLAN could not be found.", testVlan);
                if( vs.getSubnetSupport().equals(Requirement.NONE) ) {
                    vlanId = testVlan.getProviderVlanId();
                }
                else {
                    for( Subnet subnet : vs.listSubnets(testVlan.getProviderVlanId()) ) {
                        if( subnet.getCurrentState().equals(SubnetState.AVAILABLE) && (!vs.isSubnetDataCenterConstrained() || subnet.getProviderDataCenterId().equals(getTestDataCenterId())) ) {
                            vlanId = subnet.getProviderSubnetId();
                        }
                    }
                }
                assertNotNull("No test VLAN/subnet was identified.", vlanId);
                testLaunchOptions.inVlan(null, getTestDataCenterId(), testVlan.getProviderVlanId());
            }
            if( cloud.hasNetworkServices() && cloud.getNetworkServices().hasFirewallSupport() ) {
                String id = getTestFirewallId();

                if( id != null ) {
                    testLaunchOptions.behindFirewalls(id);
                }
            }
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

        VirtualMachine vm = vmSupport.launch(testLaunchOptions);

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
        VirtualMachineSupport vmSupport = getSupport();
        
        assertNotNull("You must specify a provider term for virtual machine", vmSupport.getProviderTermForServer(Locale.getDefault()));
        out("Term:                       " + vmSupport.getProviderTermForServer(Locale.getDefault()));
        out("Subscribed:                 " + vmSupport.isSubscribed());
        out("Max VMs:                    " + vmSupport.getMaximumVirtualMachineCount());
        out("API termination prevention: " + vmSupport.isAPITerminationPreventable());
        out("Analytics:                  " + vmSupport.supportsAnalytics());
        out("Basic analytics:            " + vmSupport.isBasicAnalyticsSupported());
        out("Extended analytics:         " + vmSupport.isExtendedAnalyticsSupported());
        out("User data:                  " + vmSupport.isUserDataSupported());
        out("Shell keys:                 " + vmSupport.identifyShellKeyRequirement());
        out("Root volume:                " + vmSupport.identifyRootVolumeRequirement());
        out("Password:                   " + vmSupport.identifyPasswordRequirement());
        out("VLAN:                       " + vmSupport.identifyVlanRequirement());
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
                assertNotNull("Product ID cannot be null", product.getProviderProductId());
                assertTrue("CPU count must be at least 1", product.getCpuCount() > 0);
                assertNotNull("Product name cannot be null", product.getName());
                assertNotNull("Product description cannot be null", product.getDescription());
                Storage<Gigabyte> disk = product.getRootVolumeSize();
                
                assertNotNull("No disk size is specified", disk);
                assertTrue("Disk size must be non-negative", disk.getQuantity().intValue() > -1);
                
                Storage<Megabyte> ram = product.getRamSize();
                
                assertNotNull("No RAM size is specified");
                assertTrue("RAM size must be non-negative", ram.getQuantity().intValue() > -1);
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
        catch( InterruptedException ignore ) { }
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
        assertNotNull("A VM must have a product", vm.getProductId());
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
            out("Product:       " + vm.getProductId());
            out("State:         " + vm.getCurrentState());
            out("Pause/unpause: " + getSupport().supportsPauseUnpause(vm));
            out("Start/stop:    " + getSupport().supportsStartStop(vm));
            out("Suspnd/resume: " + getSupport().supportsSuspendResume(vm));
            out("Description:\n" + vm.getDescription());
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }

    @Test
    public void testStart() throws InternalException, CloudException {
        begin();
        try {
            VirtualMachine vm = getSupport().getVirtualMachine(testVm);

            assertNotNull("Test virtual machine does not exist.", vm);
            if( getSupport().supportsStartStop(vm) ) {
                assertTrue("Expected successful start.", start(getSupport(), testVm));
                vm = getSupport().getVirtualMachine(testVm);
                assertNotNull("VM " + testVm + " has ceased to exist.", vm);
                out("VM state: " + vm.getCurrentState());
                assertEquals("VM is not running.", VmState.RUNNING, vm.getCurrentState());
            }
            else {
                assertFalse("Expected error during start but got no error.", start(getSupport(), testVm));
                out("Start/stop not supported (OK)");
            }
        }
        finally {
            end();
        }
    }

    @Test
    public void testStop() throws InternalException, CloudException {
        begin();
        try {
            VirtualMachine vm = getSupport().getVirtualMachine(testVm);

            assertNotNull("Test virtual machine does not exist.", vm);
            if( getSupport().supportsStartStop(vm) ) {
                assertTrue("Expected successful stop.", stop(getSupport(), testVm));
                vm = getSupport().getVirtualMachine(testVm);
                assertNotNull("VM " + testVm + " has ceased to exist.", vm);
                out("VM state: " + vm.getCurrentState());
                assertEquals("VM is not stopped.", VmState.STOPPED, vm.getCurrentState());
            }
            else {
                assertFalse("Expected error during stop but got no error.", stop(getSupport(), testVm));
                out("Start/stop not supported (OK)");
            }
        }
        finally {
            end();
        }
    }

    @Test
    public void testResume() throws InternalException, CloudException {
        begin();
        try {
            VirtualMachine vm = getSupport().getVirtualMachine(testVm);

            assertNotNull("Test virtual machine does not exist.", vm);
            if( getSupport().supportsSuspendResume(vm) ) {
                assertTrue("Expected successful resume.", resume(getSupport(), testVm));
                vm = getSupport().getVirtualMachine(testVm);
                assertNotNull("VM " + testVm + " has ceased to exist.", vm);
                out("VM state: " + vm.getCurrentState());
                assertEquals("VM is not running.", VmState.RUNNING, vm.getCurrentState());
            }
            else {
                assertFalse("Expected error during resume but got no error.", resume(getSupport(), testVm));
                out("Suspend/resume not supported (OK)");
            }
        }
        finally {
            end();
        }
    }

    @Test
    public void testSuspend() throws InternalException, CloudException {
        begin();
        try {
            VirtualMachine vm = getSupport().getVirtualMachine(testVm);

            assertNotNull("Test virtual machine does not exist.", vm);
            if( getSupport().supportsSuspendResume(vm) ) {
                assertTrue("Expected successful suspend.", suspend(getSupport(), testVm));
                vm = getSupport().getVirtualMachine(testVm);
                assertNotNull("VM " + testVm + " has ceased to exist.", vm);
                out("VM state: " + vm.getCurrentState());
                assertEquals("VM is not suspended.", VmState.SUSPENDED, vm.getCurrentState());
            }
            else {
                assertFalse("Expected error during suspend but got no error.", suspend(getSupport(), testVm));
                out("Suspend/resume not supported (OK)");
            }
        }
        finally {
            end();
        }
    }

    @Test
    public void testUnpause() throws InternalException, CloudException {
        begin();
        try {
            VirtualMachine vm = getSupport().getVirtualMachine(testVm);

            assertNotNull("Test virtual machine does not exist.", vm);
            if( getSupport().supportsPauseUnpause(vm) ) {
                assertTrue("Expected successful unpause.", unpause(getSupport(), testVm));
                vm = getSupport().getVirtualMachine(testVm);
                assertNotNull("VM " + testVm + " has ceased to exist.", vm);
                out("VM state: " + vm.getCurrentState());
                assertEquals("VM is not running.", VmState.RUNNING, vm.getCurrentState());
            }
            else {
                assertFalse("Expected error during unpause but got no error.", unpause(getSupport(), testVm));
                out("Pause/unpause not supported (OK)");
            }
        }
        finally {
            end();
        }
    }

    @Test
    public void testPause() throws InternalException, CloudException {
        begin();
        try {
            VirtualMachine vm = getSupport().getVirtualMachine(testVm);

            assertNotNull("Test virtual machine does not exist.", vm);
            if( getSupport().supportsPauseUnpause(vm) ) {
                assertTrue("Expected successful pause.", pause(getSupport(), testVm));
                vm = getSupport().getVirtualMachine(testVm);
                assertNotNull("VM " + testVm + " has ceased to exist.", vm);
                out("VM state: " + vm.getCurrentState());
                assertEquals("VM is not paused.", VmState.PAUSED, vm.getCurrentState());
            }
            else {
                assertFalse("Expected error during pause but got no error.", pause(getSupport(), testVm));
                out("Pause/unpause not supported (OK)");
            }
        }
        finally {
            end();
        }
    }
}
