/**
 * Copyright (C) 2009-2013 Dell, Inc.
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

package org.dasein.cloud.test.network;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.*;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.compute.ComputeResources;
import org.dasein.util.CalendarWrapper;
import org.junit.*;
import org.junit.rules.TestName;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/21/13 1:15 PM</p>
 *
 * @author George Reese
 */
public class StatefulVLANTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulVLANTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testVLANId;
    private String testSubnetId;
    private String testInternetGatewayId;

    public StatefulVLANTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        if( name.getMethodName().equals("provisionSubnet") ) {
            testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
            if( testVLANId == null ) {
                // VLANs are a special case where we will re-use existing VLANs for adding resources
                testVLANId = tm.getTestVLANId(DaseinTestManager.STATELESS, false, null);
            }
        }
        else if( name.getMethodName().equals("removeVLAN") ) {
            testVLANId = tm.getTestVLANId(DaseinTestManager.REMOVED, true, null);
        }
        else if( name.getMethodName().equals("removeSubnet") ) {
            testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
            testSubnetId = tm.getTestSubnetId(DaseinTestManager.REMOVED, true, testVLANId, null);
            if( testVLANId != null ) {
              try {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services != null ) {
                  VLANSupport support = services.getVlanSupport();

                  if( support != null && support.isConnectedViaInternetGateway(testVLANId) ) {
                    support.removeInternetGateway(testVLANId);
                  }
                }
              }
              catch(Throwable ignore ) {
                // ignore
              }
            }
        }
        else if( name.getMethodName().equals("launchVM") ) {
            testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
            if( testVLANId == null ) {
                testVLANId = tm.getTestVLANId(DaseinTestManager.STATELESS, false, null);
            }
            if( testVLANId != null ) {
                testSubnetId = tm.getTestSubnetId(DaseinTestManager.STATEFUL, true, testVLANId, null);
            }
        }
        else if( name.getMethodName().equals("connectInternetGateway") ) {
            testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
            if( testVLANId == null ) {
              testVLANId = tm.getTestVLANId(DaseinTestManager.STATELESS, false, null);
            }
            if( testVLANId != null ) {
              try {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services != null ) {
                  VLANSupport support = services.getVlanSupport();

                  if( support != null && support.isConnectedViaInternetGateway(testVLANId) ) {
                    support.removeInternetGateway(testVLANId);
                  }
                }
              }
              catch(Throwable ignore ) {
                // ignore
              }
            }
        }
        else if( name.getMethodName().equals("removeInternetGateway") ) {
            testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
            testInternetGatewayId = tm.getTestInternetGatewayId(DaseinTestManager.STATEFUL, false, testVLANId, null);
        }
    }

    @After
    public void after() {
        try {
            if( testVLANId != null ) {
                try {
                    NetworkServices services = tm.getProvider().getNetworkServices();

                    if( services != null ) {
                        VLANSupport support = services.getVlanSupport();

                        if( support != null && support.isConnectedViaInternetGateway(testVLANId) ) {
                            support.removeInternetGateway(testVLANId);
                            testInternetGatewayId = null;
                        }
                    }
                }
                catch(Throwable ignore ) {
                    // ignore
                }
            }
            testVLANId = null;
            testSubnetId = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void provisionSubnet() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                boolean supported = (support.allowsNewSubnetCreation() && support.isSubscribed());

                if( testVLANId != null ) {
                    NetworkResources resources = DaseinTestManager.getNetworkResources();

                    if( resources != null ) {
                        if( supported ) {
                            VLAN vlan = support.getVlan(testVLANId);

                            assertNotNull("The test VLAN does not exist", vlan);
                            String id = resources.provisionSubnet(support, "provisionKeypair", testVLANId, "dsnsub", vlan.getProviderDataCenterId());
                            tm.out("New Subnet", id);
                            try { Thread.sleep(1500L); }
                            catch( InterruptedException ignore ) { }
                            assertNotNull("Could not find the subnet in the cloud after provisioning", support.getSubnet(id));
                        }
                        else {
                            try {
                                resources.provisionSubnet(support, "provisionKeypair", testVLANId, "dsnsubfail", null);
                                fail("Subnet provisioning completed even though it isn't supported");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("Caught OperationNotSupportedException for " + name.getMethodName() + " as expected");
                            }
                        }
                    }
                    else {
                        fail("The network resources failed to initialize for testing");
                    }
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.ok("No test VLAN was identified for tests due to a lack of subscription to VLAN support");
                    }
                    else {
                        fail("No test VLAN was found for running the stateless test: " + name.getMethodName());
                    }
                }
            }
            else {
                tm.ok("No VLAN support in this cloud");
            }
        }
        else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void provisionVLAN() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                boolean supported = (support.allowsNewVlanCreation() && support.isSubscribed());
                NetworkResources resources = DaseinTestManager.getNetworkResources();

                if( resources != null ) {
                    if( supported ) {
                        String id = resources.provisionVLAN(support, "provisionKeypair", "dnsvlan", null);
                        tm.out("New VLAN", id);
                        try { Thread.sleep(1500L); }
                        catch( InterruptedException ignore ) { }
                        assertNotNull("Could not find the new VLAN in the cloud after creation", support.getVlan(id));
                    }
                    else if( support.isSubscribed() ) {
                        try {
                            resources.provisionVLAN(support, "provision", "dnsvlan", null);
                            fail("VLAN provisioning completed even though it isn't supported");
                        }
                        catch( OperationNotSupportedException expected ) {
                            tm.ok("Caught OperationNotSupportedException for " + name.getMethodName() + " as expected");
                        }
                    }
                }
                else {
                    fail("The network resources failed to initialize for testing");
                }
            }
            else {
                tm.ok("No VLAN support in this cloud");
            }
        }
        else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void removeVLAN() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                if( testVLANId != null ) {
                    VLAN vlan = support.getVlan(testVLANId);

                    tm.out("Before", vlan);
                    assertNotNull("Test VLAN no longer exists, cannot test removing it", vlan);
                    tm.out("State", vlan.getCurrentState());
                    support.removeVlan(testVLANId);
                    try { Thread.sleep(5000L); }
                    catch( InterruptedException ignore ) { }
                    vlan = support.getVlan(testVLANId);
                    tm.out("After", vlan);
                    tm.out("State", (vlan == null ? "DELETED" : vlan.getCurrentState()));
                    assertNull("The VLAN remains available", vlan);
                }
                else {
                    if( !support.allowsNewVlanCreation() ) {
                        tm.ok("VLAN creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    }
                    else if( support.isSubscribed() ) {
                        fail("No test VLAN for deletion test");
                    }
                    else {
                        tm.ok("VLAN service is not subscribed so this test is not entirely valid");
                    }
                }
            }
            else {
                tm.ok("No VLAN support in this cloud");
            }
        }
        else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void removeSubnet() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                if( testSubnetId != null ) {
                    Subnet subnet = support.getSubnet(testSubnetId);

                    tm.out("Before", subnet);
                    assertNotNull("Test subnet no longer exists, cannot test removing it", subnet);
                    tm.out("State", subnet.getCurrentState());
                    support.removeSubnet(testSubnetId);
                    try { Thread.sleep(5000L); }
                    catch( InterruptedException ignore ) { }
                    subnet = support.getSubnet(testSubnetId);
                    tm.out("After", subnet);
                    tm.out("State", (subnet == null ? "DELETED" : subnet.getCurrentState()));
                    assertNull("The subnet remains available", subnet);
                }
                else {
                    if( !support.allowsNewSubnetCreation() ) {
                        tm.ok("Subnet creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    }
                    else if( support.isSubscribed() ) {
                        fail("No test subnet for deletion test");
                    }
                    else {
                        tm.ok("VLAN service is not subscribed so this test is not entirely valid");
                    }
                }
            }
            else {
                tm.ok("No VLAN support in this cloud");
            }
        }
        else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void launchVM() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();
        VirtualMachineSupport support;

        if( services != null ) {
            support = services.getVirtualMachineSupport();
            if( support == null ) {
                tm.ok("No virtual machine support in " + tm.getProvider().getCloudName());
                return;
            }
        }
        else {
            tm.ok("No compute services in " + tm.getProvider().getCloudName());
            return;
        }
        ComputeResources compute = DaseinTestManager.getComputeResources();

        if( compute != null ) {
            String productId = tm.getTestVMProductId();

            assertNotNull("Unable to identify a VM product for test launch", productId);
            String imageId = tm.getTestImageId(DaseinTestManager.STATELESS, false);

            assertNotNull("Unable to identify a test image for test launch", imageId);
            VMLaunchOptions options = VMLaunchOptions.getInstance(productId, imageId, "dsnnetl" + (System.currentTimeMillis()%10000), "Dasein Network Launch " + System.currentTimeMillis(), "Test launch for a VM in a network");

            if( testSubnetId != null ) {
                tm.out("Subnet Id", testSubnetId);
                @SuppressWarnings("ConstantConditions") Subnet subnet = tm.getProvider().getNetworkServices().getVlanSupport().getSubnet(testSubnetId);

                assertNotNull("Subnet went away before test could be executed", subnet);
                String dataCenterId = subnet.getProviderDataCenterId();

                if( dataCenterId == null ) {
                    for( DataCenter dc : tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()) ) {
                        dataCenterId = dc.getProviderDataCenterId();
                    }
                }
                assertNotNull("Could not identify a data center for VM launch", dataCenterId);
                options.inDataCenter(dataCenterId);
                options.inVlan(null, dataCenterId, testSubnetId);
            }
            else if( testVLANId != null ) {
                @SuppressWarnings("ConstantConditions") VLAN vlan = tm.getProvider().getNetworkServices().getVlanSupport().getVlan(testVLANId);

                assertNotNull("VLAN went away before test could be executed", vlan);
                String dataCenterId = vlan.getProviderDataCenterId();

                if( dataCenterId == null ) {
                    for( DataCenter dc : tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()) ) {
                        dataCenterId = dc.getProviderDataCenterId();
                    }
                }
                assertNotNull("Could not identify a data center for VM launch", dataCenterId);
                options.inDataCenter(dataCenterId);
                options.inVlan(null, dataCenterId, testVLANId);
            }
            else {
                if( !support.identifyVlanRequirement().equals(Requirement.NONE) ) {
                    fail("No test VLAN or subnet in which to launch a VM");
                }
                else {
                    tm.ok("Launching into VLANs is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
                }
                return;
            }

            String vmId = compute.provisionVM(support, "vlanLaunch", options, options.getDataCenterId());

            tm.out("Virtual Machine", vmId);
            assertNotNull("No error received launching VM in VLAN/subnet, but there was no virtual machine", vmId);

            VirtualMachine vm = support.getVirtualMachine(vmId);

            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 5L);

            while( timeout > System.currentTimeMillis() ) {
                if( vm == null ) {
                    break;
                }
                if( vm.getProviderVlanId() != null ) {
                    break;
                }
                if( VmState.RUNNING.equals(vm.getCurrentState()) ) {
                    break;
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
                try { vm = support.getVirtualMachine(vmId); }
                catch( Throwable ignore ) { }
            }
            assertNotNull("Launched VM does not exist", vm);
            tm.out("In VLAN", vm.getProviderVlanId());
            tm.out("In Subnet", vm.getProviderSubnetId());
            assertEquals("The subnet for the launched VM does not match the target subnet", testSubnetId, vm.getProviderSubnetId());
            assertEquals("The VLAN for the launched VM does not match the target VLAN", testVLANId, vm.getProviderVlanId());
        }
    }

    @Test
    public void connectInternetGateway() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                if( testVLANId != null ) {
                  boolean connected = support.isConnectedViaInternetGateway(testVLANId);
                  NetworkResources resources = DaseinTestManager.getNetworkResources();
                  if( resources != null ) {
                    if( support.supportsInternetGatewayCreation() ) {
                        tm.out("Before", connected);
                        assertFalse("The VLAN is already connected via an internet gateway and thus this test cannot run", connected);
                        resources.provisionInternetGateway(support, "provisionKeypair", testVLANId);
                        connected = support.isConnectedViaInternetGateway(testVLANId);
                        try { Thread.sleep(2000L); }
                        catch( InterruptedException ignore ) { }
                        tm.out("After", connected);
                        assertTrue("The VLAN is not connected via an Internet Gateway", connected);
                    }
                    else {
                        try {
                            resources.provisionInternetGateway(support, "provisionKeypair", testVLANId);
                            fail("Internet gateway creation completed even though it is not supported");
                        }
                        catch( OperationNotSupportedException expected ) {
                            tm.ok("Caught OperationNotSupportedException as expected for " + name.getMethodName());
                        }
                    }
                  } else {
                    fail("The network resources failed to initialize for testing");
                  }
                }
                else {
                    if( !support.allowsNewVlanCreation() ) {
                        tm.ok("VLAN creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    }
                    else if( support.isSubscribed() ) {
                        fail("No test VLAN for " + name.getMethodName() + " test");
                    }
                    else {
                        tm.ok("VLAN service is not subscribed so this test may not be entirely valid");
                    }
                }
            }
            else {
                tm.ok("No VLAN support in this cloud");
            }
        }
        else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void removeInternetGateway() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();
        if( services != null ) {
            VLANSupport support = services.getVlanSupport();
            if( support != null ) {
                if( testVLANId != null ) {
                  if( support.supportsInternetGatewayCreation() ) {
                    boolean connected = support.isConnectedViaInternetGateway(testVLANId);
                    tm.out("Before", connected);
                    if( connected ) {
                      if( testInternetGatewayId != null ) {
                        InternetGateway iGateway = support.getInternetGatewayById(testInternetGatewayId);
                        tm.out("Before", iGateway);
                        assertNotNull("Test internet gateway no longer exists, cannot test removing it", iGateway);
                        String foundId = iGateway.getProviderInternetGatewayId();
                        assertNotNull("The test internet gateway id was null", foundId);
                        String iGatewayIdByVlan = support.getAttachedInternetGatewayId(testVLANId);
                        assertTrue( "Gateway found by Id and Gateway found by VLAN do not match", iGatewayIdByVlan.equalsIgnoreCase( foundId ) );
                        support.removeInternetGateway(testVLANId);
                        try { Thread.sleep(5000L); }
                        catch( InterruptedException ignore ) { }
                        iGateway = support.getInternetGatewayById(testInternetGatewayId);
                        tm.out("After", iGateway);
                        assertNull("The internet gateway remains available", iGateway);
                      }
                      else {
                        tm.ok("No internet gateway with id " + testInternetGatewayId);
                      }
                      connected = support.isConnectedViaInternetGateway(testVLANId);
                      tm.out("After", connected);
                      assertFalse("The VLAN is still connected via an Internet Gateway", connected);
                    }
                    else {
                      tm.ok("No internet gateway is connected to " + testVLANId);
                    }
                  }
                  else {
                    if( !support.supportsInternetGatewayCreation() ) {
                      tm.ok("Internet Gateway creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    }
                  }
                }
                else {
                    if( !support.allowsNewVlanCreation() ) {
                      tm.ok("VLAN creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    }
                    else if( support.isSubscribed() ) {
                      fail("No test VLAN for deletion test");
                    }
                    else {
                      tm.ok("VLAN service is not subscribed so this test is not entirely valid");
                    }
                }
            }
          else {
              tm.ok("No VLAN support in this cloud");
          }
      }
      else {
          tm.ok("No network services in this cloud");
      }
    }
}
