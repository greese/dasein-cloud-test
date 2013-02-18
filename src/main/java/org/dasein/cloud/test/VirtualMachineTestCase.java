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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.identity.ShellKeySupport;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.SubnetState;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.util.APITrace;
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
        if( name.equals("testTerminate") || name.equals("testStart") || name.equals("testStop") || name.equals("testPause") || name.equals("testUnpause") || name.equals("testSuspend") || name.equals("testResume") || name.equals("testFilter")) {
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
            if( s.identifyStaticIPRequirement().equals(Requirement.REQUIRED) ) {
                NetworkServices services = cloud.getNetworkServices();

                if( services == null ) {
                    throw new CloudException("A static IP is required to launch a virtual machine, but no network services exist.");
                }
                IpAddressSupport support = services.getIpAddressSupport();

                if( support == null ) {
                    throw new CloudException("A static IP is required to launch a virtual machine, but no IP address support exists.");
                }
                for( IPVersion version : support.listSupportedIPVersions() ) {
                    try {
                        testLaunchOptions.withStaticIps(identifyTestIPAddress(cloud, version));
                    }
                    catch( CloudException ignore ) {
                        // try again, maybe
                    }
                }
                if( testLaunchOptions.getStaticIpIds().length < 1 ) {
                    throw new CloudException("Unable to provisionVM the required IP address for this test");
                }
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
        killTestAddress(cloud);
        try {
            if( cloud != null ) {
                cloud.close();
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        APITrace.report(getName());
        APITrace.reset();
        if( cloud != null ) {
            cloud.close();
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
