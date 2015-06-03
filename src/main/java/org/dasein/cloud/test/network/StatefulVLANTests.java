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

package org.dasein.cloud.test.network;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.network.*;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.compute.ComputeResources;
import org.dasein.util.CalendarWrapper;
import org.junit.*;
import org.junit.rules.TestName;

import java.util.Collection;

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
    private String testRoutingTableId;
    private String testVLANVMId;
    private String testDataCenterId;
    private String testFirewallId;

    private String[] cidrs = new String[]{"192.168.20.0/28", "192.168.40.0/28", "192.168.60.0/28", "192.168.80.0/28", "192.168.100.0/28"};

    public StatefulVLANTests() {
    }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        testDataCenterId = DaseinTestManager.getDefaultDataCenterId(false);

        NetworkServices services = null;
        VLANSupport support = null;
        try {
            services = tm.getProvider().getNetworkServices();
            if( services != null ) {
                support = services.getVlanSupport();
            }
        } catch( Exception ignore ) {
            tm.out("Before: Unable to initialize NetworkServices or VLANSupport");
            fail("Require network services and vlan support to provision resources");
        }

        if( name.getMethodName().equals("provisionSubnet") || name.getMethodName().equals("provisionRoutingTable") ||
                name.getMethodName().equals("launchVM") || name.getMethodName().equals("connectInternetGateway") ||
                name.getMethodName().equals("removeInternetGateway") || name.getMethodName().equals("addRouteToVM") ||
                name.getMethodName().equals("addRouteToNetworkInterface") || name.getMethodName().equals("addRouteToGateway")
                ) {
            testVLANId = getVLANId(support, name.getMethodName(), null, null);
            // in the event it is new...
            try {
                Integer count = 0;
                while( count < 5 ) {
                    VLAN v = support.getVlan(testVLANId);
                    if( v != null && v.getCurrentState().equals(VLANState.AVAILABLE) ) {
                        count = 5;
                    } else {
                        try {
                            Thread.sleep(5000L);
                        } catch( InterruptedException ignore ) {
                        }
                    }
                    count++;
                }
            } catch( Exception e ) {
                tm.out("Exception while getting vlan for " + name.getMethodName());
            }
        }
        if( name.getMethodName().equals("removeVLAN")
                || name.getMethodName().equals("removeSubnet")
                || name.getMethodName().equals("removeRoutingTable") ) {
            testVLANId = getVLANId(support, name.getMethodName(), DaseinTestManager.REMOVED, true);
            // wait...
            try {
                Thread.sleep(5000L);
            } catch( InterruptedException ignore ) {
            }
        }
//        DISABLED
//        if( name.getMethodName().equals("removeVLANwithFirewallRule") ) {
//            testVLANId = getVLANId(support, name.getMethodName(), DaseinTestManager.REMOVED, true);
//            // wait...
//            try {
//                Thread.sleep(5000L);
//            } catch( InterruptedException ignore ) {
//            }
//            testFirewallId = tm.getTestVLANFirewallId(DaseinTestManager.STATEFUL, true, testVLANId);
//        }
        if( name.getMethodName().equals("removeRoutingTable") || name.getMethodName().equals("addRouteToVM") ||
                name.getMethodName().equals("addRouteToNetworkInterface") || name.getMethodName().equals("addRouteToGateway")
                ) {
            testRoutingTableId = tm.getTestRoutingTableId(DaseinTestManager.STATEFUL, false, testVLANId, null);
            if( testRoutingTableId == null ) {
                testRoutingTableId = tm.getTestRoutingTableId(DaseinTestManager.STATEFUL, true, testVLANId, null);
                // wait...
                try {
                    Thread.sleep(5000L);
                } catch( InterruptedException ignore ) {
                }
            }
        }
        if( name.getMethodName().equals("removeSubnet") ) {
            testSubnetId = tm.getTestSubnetId(DaseinTestManager.REMOVED, true, testVLANId, null);
            // wait...
            try {
                Thread.sleep(5000L);
            } catch( InterruptedException ignore ) {
            }
            if( testSubnetId == null ) {
                testSubnetId = tm.getTestSubnetId(DaseinTestManager.STATELESS, false, testVLANId, null);
            }
            if( testSubnetId == null ) {
                testSubnetId = tm.getTestSubnetId(DaseinTestManager.STATEFUL, false, testVLANId, null);
            }
            if( testSubnetId == null ) {
                testSubnetId = tm.getTestSubnetId(DaseinTestManager.STATEFUL, true, testVLANId, null);
                // wait...
                try {
                    Thread.sleep(5000L);
                } catch( InterruptedException ignore ) {
                }
            }
            if( testVLANId != null ) {
                try {
                    if( support != null && support.isConnectedViaInternetGateway(testVLANId) ) {
                        support.removeInternetGateway(testVLANId);
                    }
                } catch( Throwable t ) {
                    tm.out("Before: Unable to initialize test variables for " + name.getMethodName() + " because " + t.getMessage());
                }
            }
        }
        if( name.getMethodName().equals("launchVM") ) {
            if( testVLANId != null ) {
                testSubnetId = tm.getTestSubnetId(DaseinTestManager.STATEFUL, false, testVLANId, null);
                if( testSubnetId == null ) {
                    testSubnetId = tm.getTestSubnetId(DaseinTestManager.STATEFUL, true, testVLANId, testDataCenterId);
                    // wait...
                    try {
                        Thread.sleep(5000L);
                    } catch( InterruptedException ignore ) {
                    }
                }
            }
        }
        if( name.getMethodName().equals("connectInternetGateway") ) {
            if( testVLANId != null ) {
                try {
                    // remove internet gateway so we can add a new one
                    if( support != null && support.isConnectedViaInternetGateway(testVLANId) ) {
                        support.removeInternetGateway(testVLANId);
                    }
                } catch( Throwable t ) {
                    tm.out("Before: Issue during initialization for " + name.getMethodName() + " because " + t.getMessage());
                }
            }
        }
        if( name.getMethodName().equals("removeInternetGateway") || name.getMethodName().equals("addRouteToGateway") ) {
            testInternetGatewayId = tm.getTestInternetGatewayId(DaseinTestManager.STATEFUL, false, testVLANId, null);
            if( testInternetGatewayId == null ) {
                testInternetGatewayId = tm.getTestInternetGatewayId(DaseinTestManager.STATEFUL, true, testVLANId, null);
            }
        }
        if( name.getMethodName().equals("addRouteToVM") || name.getMethodName().equals("addRouteToNetworkInterface") ) {
            try {
                if( support != null && testRoutingTableId != null ) {
                    RoutingTable rtb = support.getRoutingTable(testRoutingTableId);
                    if( rtb != null ) {
                        testVLANVMId = tm.getTestVLANVMId(DaseinTestManager.STATEFUL, VmState.RUNNING, rtb.getProviderVlanId(), true, null);
                        if( testVLANVMId == null ) {
                            testVLANVMId = tm.getTestVLANVMId(DaseinTestManager.STATELESS, VmState.RUNNING, rtb.getProviderVlanId(), true, null);
                        }
                        if( testVLANVMId != null ) {
                            try {
                                Integer count = 0;
                                ComputeServices cs = tm.getProvider().getComputeServices();
                                VirtualMachineSupport vs = cs.getVirtualMachineSupport();
                                while( count < 20 ) {
                                    VirtualMachine vm = vs.getVirtualMachine(testVLANVMId);
                                    if( vm.getCurrentState().equals(VmState.RUNNING) ) {
                                        count = 20;
                                    } else {
                                        try {
                                            Thread.sleep(5000L);
                                        } catch( InterruptedException ignore ) {
                                        }
                                    }
                                    count++;
                                }
                            } catch( Exception e ) {
                                tm.out("Exception while getting virtual machine for " + name.getMethodName());
                            }
                        } else {
                            tm.out("Unable to produce virtual machine for " + name.getMethodName());
                        }
                        Route[] routes = rtb.getRoutes();
                        for( Route rt : routes ) {
                            try {
                                VLAN v = support.getVlan(rtb.getProviderVlanId());
                                if( !rt.getDestinationCidr().equalsIgnoreCase(v.getCidr()) ) {
                                    support.removeRoute(testRoutingTableId, rt.getDestinationCidr());
                                }
                            } catch( Exception e ) {
                                tm.out("Before: Unable to initialize test variables for " + name.getMethodName() + " because " + e.getMessage());
                            }
                        }
                    }
                }
            } catch( Throwable t ) {
                tm.out("Before: Unable to initialize test variables for " + name.getMethodName() + " because " + t.getMessage());
            }
        }
    }

    @After
    public void after() {
        testVLANId = null;
        testSubnetId = null;
        testFirewallId = null;
        tm.end();
    }

    private String getVLANId(VLANSupport support, String name, String label, Boolean provision) {
        if( label == null ) {
            label = DaseinTestManager.STATEFUL;
        }
        if( provision == null ) {
            provision = false;
        }
        String vId = tm.getTestVLANId(label, provision, null);
        if( vId == null ) {
            vId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
        }
        if( vId == null ) {
            // VLANs are a special case where we will re-use existing VLANs for adding resources
            vId = tm.getTestVLANId(DaseinTestManager.STATELESS, false, null);
            if( vId == null ) {
                vId = tm.getTestVLANId(DaseinTestManager.STATELESS, true, null);
            }
        }
        if( vId != null ) {
            // wait for vlan to be available
            try {
                Thread.sleep(5000L);
            } catch( InterruptedException ignore ) {
            }
            try {
                VLAN v = support.getVlan(vId);
                if( v != null ) {
                    return vId;
                } else {
                    return null;
                }
            } catch( Exception e ) {
                tm.out("Before: Unable to get test VLAN for " + name);
                return null;
            }
        } else {
            tm.out("Before: Unable to get test VLAN for " + name);
            return null;
        }
    }

    @Test
    public void provisionSubnet() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                boolean supported = ( support.getCapabilities().allowsNewSubnetCreation() && support.isSubscribed() );

                if( testVLANId != null ) {
                    NetworkResources resources = DaseinTestManager.getNetworkResources();

                    if( resources != null ) {
                        if( supported ) {
                            VLAN vlan = support.getVlan(testVLANId);

                            assertNotNull("The test VLAN does not exist", vlan);
                            String id = resources.provisionSubnet(support, "provisionSubnet", testVLANId, "dsnsub", vlan.getProviderDataCenterId());
                            tm.out("New Subnet", id);
                            try {
                                Thread.sleep(1500L);
                            } catch( InterruptedException ignore ) {
                            }
                            assertNotNull("Could not find the subnet in the cloud after provisioning", support.getSubnet(id));
                        } else {
                            try {
                                resources.provisionSubnet(support, "provisionSubnet", testVLANId, "dsnsubfail", null);
                                fail("Subnet provisioning completed even though it isn't supported");
                            } catch( OperationNotSupportedException expected ) {
                                tm.ok("Caught OperationNotSupportedException for " + name.getMethodName() + " as expected");
                            }
                        }
                    } else {
                        fail("The network resources failed to initialize for testing");
                    }
                } else {
                    if( !support.isSubscribed() ) {
                        tm.ok("No test VLAN was identified for tests due to a lack of subscription to VLAN support");
                    }
                    else if (!support.getCapabilities().allowsNewVlanCreation()) {
                        tm.ok("No test VLAN was identified due to a lack of support for creating VLANs");
                    } else {
                        fail("No test VLAN was found for running the stateful test: " + name.getMethodName());
                    }
                }
            } else {
                tm.ok("No VLAN support in this cloud");
            }
        } else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void provisionVLAN() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                boolean supported = ( support.getCapabilities().allowsNewVlanCreation() && support.isSubscribed() );
                NetworkResources resources = DaseinTestManager.getNetworkResources();

                if( resources != null ) {
                    if( supported ) {
                        String id = resources.provisionVLAN(support, "provisionVlan", "dnsvlan", null);
                        tm.out("New VLAN", id);
                        try {
                            Thread.sleep(1500L);
                        } catch( InterruptedException ignore ) {
                        }
                        assertNotNull("Could not find the new VLAN in the cloud after creation", support.getVlan(id));
                    } else if( support.isSubscribed() ) {
                        try {
                            resources.provisionVLAN(support, "provision", "dnsvlan", null);
                            fail("VLAN provisioning completed even though it isn't supported");
                        } catch( OperationNotSupportedException expected ) {
                            tm.ok("Caught OperationNotSupportedException for " + name.getMethodName() + " as expected");
                        }
                    }
                } else {
                    fail("The network resources failed to initialize for testing");
                }
            } else {
                tm.ok("No VLAN support in this cloud");
            }
        } else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void provisionRoutingTable() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                boolean supported = ( support.getCapabilities().allowsNewRoutingTableCreation() && support.isSubscribed() );

                if( testVLANId != null ) {
                    NetworkResources resources = DaseinTestManager.getNetworkResources();

                    if( resources != null ) {
                        if( supported ) {
                            VLAN vlan = support.getVlan(testVLANId);
                            assertNotNull("The test VLAN does not exist", vlan);
                            String id = resources.provisionRoutingTable(support, vlan.getProviderVlanId(), "provisionRoutingTable", "dnsrtb");
                            tm.out("New Routing Table", id);
                            testRoutingTableId = id;
                            try {
                                Thread.sleep(3500L);
                            } catch( InterruptedException ignore ) {
                            }
                            assertNotNull("Could not find the new Routing Table in the cloud after creation", support.getRoutingTable(id));
                        } else if( support.isSubscribed() ) {
                            try {
                                String id = resources.provisionRoutingTable(support, testVLANId, "provisionRoutingTable", "dnsrtb");
                                fail("Route Table provisioning completed even though it isn't supported");
                            } catch( OperationNotSupportedException expected ) {
                                tm.ok("Caught OperationNotSupportedException for " + name.getMethodName() + " as expected");
                            }
                        }
                    } else {
                        fail("The network resources failed to initialize for testing");
                    }
                } else {
                    if( !support.isSubscribed() ) {
                        tm.ok("No test VLAN was identified for tests due to a lack of subscription to VLAN support");
                    } else if (!support.getCapabilities().allowsNewVlanCreation()) {
                        tm.ok("No test VLAN was identified due to a lack of support for creating VLANs");
                    } else {
                        fail("No test VLAN was found for running the stateful test: " + name.getMethodName());
                    }
                }
            }
            else {
                tm.ok("No VLAN support in this cloud");
            }
        } else {
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
                    if( support.getCapabilities().allowsNewVlanCreation() ) {
                        VLAN vlan = support.getVlan(testVLANId);

                        tm.out("Before", vlan);
                        assertNotNull("Test VLAN no longer exists, cannot test removing it", vlan);
                        tm.out("State", vlan.getCurrentState());
                        support.removeVlan(testVLANId);
                        try {
                            Thread.sleep(5000L);
                        }
                        catch( InterruptedException ignore ) {
                        }
                        vlan = support.getVlan(testVLANId);
                        tm.out("After", vlan);
                        tm.out("State", ( vlan == null ? "DELETED" : vlan.getCurrentState() ));
                        assertNull("The VLAN remains available", vlan);
                    }
                    else {
                        try {
                            support.removeVlan(testVLANId);
                            fail("VLAN creation/deletion is not supported in " + tm.getProvider().getCloudName() + ", however the removeVlan call has succeeded");
                        } catch( Exception ignore ) {
                            tm.ok("VLAN create/deletion is not supported in " + tm.getProvider().getCloudName() + ", and removeVlan did not succeed");
                        }
                    }
                }
                else {
                    if( !support.getCapabilities().allowsNewVlanCreation() ) {
                        tm.ok("VLAN creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    } else if( support.isSubscribed() ) {
                        fail("No test VLAN for deletion test");
                    } else {
                        tm.ok("VLAN service is not subscribed so this test is not entirely valid");
                    }
                }
            }
            else {
                tm.ok("No VLAN support in this cloud");
            }
        } else {
            tm.ok("No network services in this cloud");
        }
    }

//    @Test DISABLED as specific to GCE edge case
    public void removeVLANwithFirewallRule() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();
            FirewallSupport firewallSupport = services.getFirewallSupport();

            if( support != null ) {
                if( testVLANId != null ) {
                    VLAN vlan = support.getVlan(testVLANId);

                    firewallSupport.authorize(testFirewallId, "0.0.0.0/0", Protocol.ICMP, 0, 0);

                    tm.out("Before", vlan);
                    assertNotNull("Test VLAN no longer exists, cannot test removing it", vlan);
                    tm.out("State", vlan.getCurrentState());
                    support.removeVlan(testVLANId);
                    try {
                        Thread.sleep(5000L);
                    } catch( InterruptedException ignore ) {
                    }
                    vlan = support.getVlan(testVLANId);
                    tm.out("After", vlan);
                    tm.out("State", ( vlan == null ? "DELETED" : vlan.getCurrentState() ));
                    assertNull("The VLAN remains available", vlan);
                } else {
                    if( !support.getCapabilities().allowsNewVlanCreation() ) {
                        tm.ok("VLAN creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    } else if( support.isSubscribed() ) {
                        fail("No test VLAN for deletion test");
                    } else {
                        tm.ok("VLAN service is not subscribed so this test is not entirely valid");
                    }
                }
            } else {
                tm.ok("No VLAN support in this cloud");
            }
        } else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void removeRoutingTable() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                if( testRoutingTableId != null ) {
                    RoutingTable rtb = support.getRoutingTable(testRoutingTableId);

                    tm.out("Before", rtb);
                    assertNotNull("Test route table no longer exists, cannot test removing it", rtb);

                    support.removeRoutingTable(testRoutingTableId);

                    try {
                        Thread.sleep(5000L);
                    } catch( InterruptedException ignore ) {
                    }

                    rtb = support.getRoutingTable(testRoutingTableId);

                    tm.out("After", rtb);
                    assertNull("The route table remains available", rtb);
                } else {
                    if( !support.getCapabilities().allowsNewRoutingTableCreation() ) {
                        tm.ok("Route Table creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    } else if( support.isSubscribed() ) {
                        fail("No test route table for delete test");
                    } else {
                        tm.ok("VLAN service is not subscribed so this test is not entirely valid");
                    }
                }
            } else {
                tm.ok("No VLAN support in this cloud");
            }
        } else {
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
                    try {
                        Thread.sleep(5000L);
                    } catch( InterruptedException ignore ) {
                    }
                    subnet = support.getSubnet(testSubnetId);
                    tm.out("After", subnet);
                    tm.out("State", ( subnet == null ? "DELETED" : subnet.getCurrentState() ));
                    assertNull("The subnet remains available", subnet);
                } else {
                    if( !support.getCapabilities().allowsNewSubnetCreation() ) {
                        tm.ok("Subnet creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    } else if( support.isSubscribed() ) {
                        fail("No test subnet for deletion test");
                    } else {
                        tm.ok("VLAN service is not subscribed so this test is not entirely valid");
                    }
                }
            } else {
                tm.ok("No VLAN support in this cloud");
            }
        } else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void launchVM() throws CloudException, InternalException {
        NetworkServices networkServices = tm.getProvider().getNetworkServices();
        if( networkServices == null ) {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
            return;
        }
        VLANSupport vlanSupport = networkServices.getVlanSupport();
        if( vlanSupport == null ) {
            tm.ok("No VLAN support in " + tm.getProvider().getCloudName());
            return;
        }

        ComputeServices computeServices = tm.getProvider().getComputeServices();
        if( computeServices == null ) {
            tm.ok("No compute services in " + tm.getProvider().getCloudName());
            return;
        }

        VirtualMachineSupport vmSupport = computeServices.getVirtualMachineSupport();
        if( vmSupport == null ) {
            tm.ok("No virtual machine support in " + tm.getProvider().getCloudName());
            return;
        }
        DataCenterServices dcServices = tm.getProvider().getDataCenterServices();

        ComputeResources compute = DaseinTestManager.getComputeResources();
        assertNotNull("No compute resources for the tests, something is very wrong", compute);

        String productId = tm.getTestVMProductId();

        assertNotNull("Unable to identify a VM product for test launch", productId);
        String imageId = tm.getTestImageId(DaseinTestManager.STATELESS, false);

        assertNotNull("Unable to identify a test image for test launch", imageId);
        VMLaunchOptions options = VMLaunchOptions.getInstance(productId, imageId, "dsnnetl" + ( System.currentTimeMillis() % 10000 ), "Dasein Network Launch " + System.currentTimeMillis(), "Test launch for a VM in a network");

        if( testSubnetId != null ) {
            tm.out("Subnet Id", testSubnetId);
            @SuppressWarnings( "ConstantConditions" ) Subnet subnet = tm.getProvider().getNetworkServices().getVlanSupport().getSubnet(testSubnetId);
            assertNotNull("Subnet went away before test could be executed", subnet);
            String dataCenterId = subnet.getProviderDataCenterId();
            if (testDataCenterId != null)
                dataCenterId = testDataCenterId;
            else
            if( dataCenterId == null ) {
                for( DataCenter dc : tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()) ) {
                    dataCenterId = dc.getProviderDataCenterId();
                }
            }
            assertNotNull("Could not identify a data center for VM launch", dataCenterId);
            options.inDataCenter(dataCenterId);
            options.inSubnet(null, dataCenterId, testVLANId, testSubnetId);
        }
        else if( testVLANId != null ) {
            @SuppressWarnings("ConstantConditions") VLAN vlan = tm.getProvider().getNetworkServices().getVlanSupport().getVlan(testVLANId);

            assertNotNull("VLAN went away before test could be executed", vlan);
            String dataCenterId = vlan.getProviderDataCenterId();
            if (testDataCenterId != null)
                dataCenterId = testDataCenterId;
            else
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
            if (!tm.getProvider().getNetworkServices().getVlanSupport().getCapabilities().allowsNewVlanCreation()) {
                tm.ok("No test VLAN was identified due to a lack of support for creating VLANs");
            }
            else if( !vmSupport.getCapabilities().identifyVlanRequirement().equals(Requirement.NONE) ) {
                fail("No test VLAN or subnet in which to launch a VM");
            } else {
                tm.ok("Launching into VLANs is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            return;
        }

        String vmId = null;
        try {
            vmId = compute.provisionVM(vmSupport, "vlanLaunch", options, options.getDataCenterId());
            // we should check that the cloud behaviour matches the declared capabilities
            if( options.getVlanId() != null && Requirement.NONE.equals(vmSupport.getCapabilities().identifyVlanRequirement()) ) {
                assertNull("The capabilities for "+tm.getProvider().getCloudName()+" dictate that setting VLAN upon VM launch is not possible, however the VM has been launched successfully", vmId);
            }
        }
        catch( Exception ignore ) {
            if( options.getVlanId() != null && Requirement.NONE.equals(vmSupport.getCapabilities().identifyVlanRequirement()) ) {
                tm.ok("The capabilities for "+tm.getProvider().getCloudName()+" dictate that setting VLAN upon VM launch is not possible, and the VM launch has failed as expected");
                return;
            }
            else {
                throw new InternalException(ignore);
            }
        }

        tm.out("Virtual Machine", vmId);
        assertNotNull("No error received launching VM in VLAN/subnet, but there was no virtual machine", vmId);

        VirtualMachine vm = vmSupport.getVirtualMachine(vmId);

        long timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 5L );

        while( timeout > System.currentTimeMillis() ) {
            if( vm == null ) {
                break;
            }
            if( vm.getProviderVlanId() != null ) {
                break;
            }
            try {
                Thread.sleep(15000L);
            } catch( InterruptedException ignore ) {
            }
            try {
                vm = vmSupport.getVirtualMachine(vmId);
            } catch( Throwable ignore ) {
            }
        }
        assertNotNull("Launched VM does not exist", vm);
        tm.out("In VLAN", vm.getProviderVlanId());
        if (!vlanSupport.getCapabilities().getSubnetSupport().equals(Requirement.NONE)) {
            tm.out("In Subnet", vm.getProviderSubnetId());
        }
        assertEquals("The subnet for the launched VM does not match the target subnet", testSubnetId, vm.getProviderSubnetId());
        assertEquals("The VLAN for the launched VM does not match the target VLAN", testVLANId, vm.getProviderVlanId());
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
                        if( support.getCapabilities().supportsInternetGatewayCreation() ) {
                            tm.out("Before", connected);
                            assertFalse("The VLAN is already connected via an internet gateway and thus this test cannot run", connected);
                            resources.provisionInternetGateway(support, "provisionInternetGateway", testVLANId);
                            try {
                                Thread.sleep(5000L);
                            } catch( InterruptedException ignore ) {
                            }
                            connected = support.isConnectedViaInternetGateway(testVLANId);
                            tm.out("After", connected);
                            assertTrue("The VLAN is not connected via an Internet Gateway", connected);
                        } else {
                            try {
                                resources.provisionInternetGateway(support, "provisionInternetGateway", testVLANId);
                                fail("Internet gateway creation completed even though it is not supported");
                            } catch( OperationNotSupportedException expected ) {
                                tm.ok("Caught OperationNotSupportedException as expected for " + name.getMethodName());
                            }
                        }
                    } else {
                        fail("The network resources failed to initialize for testing");
                    }
                } else {
                    if( !support.getCapabilities().allowsNewVlanCreation() ) {
                        tm.ok("VLAN creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    } else if( support.isSubscribed() ) {
                        fail("No test VLAN for " + name.getMethodName() + " test");
                    } else {
                        tm.ok("VLAN service is not subscribed so this test may not be entirely valid");
                    }
                }
            } else {
                tm.ok("No VLAN support in this cloud");
            }
        } else {
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
                    if( support.getCapabilities().supportsInternetGatewayCreation() ) {
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
                                assertTrue("Gateway found by Id and Gateway found by VLAN do not match", iGatewayIdByVlan.equalsIgnoreCase(foundId));

                                support.removeInternetGateway(testVLANId);

                                try {
                                    Thread.sleep(5000L);
                                } catch( InterruptedException ignore ) {
                                }

                                iGateway = support.getInternetGatewayById(testInternetGatewayId);

                                tm.out("After", iGateway);
                                assertNull("The internet gateway remains available", iGateway);

                            } else {
                                tm.ok("No internet gateway with id " + testInternetGatewayId);
                            }
                        } else {
                            tm.ok("No internet gateway is connected to " + testVLANId);
                        }
                    } else {
                        if( !support.getCapabilities().supportsInternetGatewayCreation() ) {
                            tm.ok("Internet Gateway creation/deletion is not supported in " + tm.getProvider().getCloudName());
                        }
                    }
                } else {
                    if( !support.getCapabilities().allowsNewVlanCreation() ) {
                        tm.ok("VLAN creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    } else if( support.isSubscribed() ) {
                        fail("No test VLAN for deletion test");
                    } else {
                        tm.ok("VLAN service is not subscribed so this test is not entirely valid");
                    }
                }
            } else {
                tm.ok("No VLAN support in this cloud");
            }
        } else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void addRouteToVM() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            ComputeServices computeServices = tm.getProvider().getComputeServices();

            if( computeServices != null ) {
                VLANSupport support = services.getVlanSupport();

                if( support != null ) {
                    VirtualMachineSupport computeSupport = computeServices.getVirtualMachineSupport();

                    if( computeSupport != null ) {
                        if( testRoutingTableId != null ) {
                            if( testVLANVMId != null ) {
                                RoutingTable rtb = support.getRoutingTable(testRoutingTableId);
                                tm.out("Route Table", rtb);
                                assertNotNull("The test route table was not found in the cloud", rtb);

                                VirtualMachine vm = computeSupport.getVirtualMachine(testVLANVMId);
                                tm.out("Virtual Machine", vm);
                                assertNotNull("Did not find the test virtual machine " + testVLANVMId, vm);

                                String successfulCidr = "";
                                for( String destinationCidr : cidrs ) {
                                    try {
                                        support.addRouteToVirtualMachine(testRoutingTableId, IPVersion.IPV4, destinationCidr, vm.getProviderVirtualMachineId());
                                        successfulCidr = destinationCidr;
                                        break;
                                    } catch( Exception e ) {
                                        // ignore
                                    }
                                }

                                if( !successfulCidr.equalsIgnoreCase("") ) {
                                    try {
                                        Thread.sleep(5000L);
                                    } catch( InterruptedException ignore ) {
                                    }

                                    rtb = support.getRoutingTable(testRoutingTableId);
                                    tm.out("Route Table", rtb);
                                    assertNotNull("The test route table was not found in the cloud", rtb);

                                    Route[] routes = rtb.getRoutes();
                                    Boolean rightRoute = false;
                                    for( Route route : routes ) {
                                        String vmId = route.getGatewayVirtualMachineId();
                                        String destCidr = route.getDestinationCidr();

                                        if( destCidr.equalsIgnoreCase(successfulCidr) && vmId != null ) {
                                            if( vmId.equalsIgnoreCase(vm.getProviderVirtualMachineId()) ) {
                                                rightRoute = true;
                                            }
                                        }
                                    }
                                    assertTrue("The created route was not found in the route table", rightRoute);
                                } else {
                                    fail("Unable to add route for: " + name.getMethodName());
                                }
                            } else if( computeSupport.isSubscribed() ) {
                                fail("No test virtual machine exists and thus no test could be run for " + name.getMethodName());
                            }
                        } else {
                            if( !support.isSubscribed() ) {
                                tm.ok("No test route table was identified for tests due to a lack of subscription to VLAN support");
                            } else if( support.getCapabilities().getRoutingTableSupport().equals(Requirement.NONE) ) {
                                tm.ok("Route Tables are not supported so there is no test for " + name.getMethodName());
                            } else {
                                fail("No test route table was found for running the stateful test: " + name.getMethodName());
                            }
                        }
                    } else {
                        tm.ok("No virtual machine support in this cloud");
                    }
                } else {
                    tm.ok("No VLAN support in this cloud");
                }
            } else {
                tm.ok("No compute services in this cloud");
            }
        } else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void addRouteToNetworkInterface() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            ComputeServices computeServices = tm.getProvider().getComputeServices();

            if( computeServices != null ) {
                VLANSupport support = services.getVlanSupport();

                if( support != null ) {
                    VirtualMachineSupport computeSupport = computeServices.getVirtualMachineSupport();

                    if( computeSupport != null ) {
                        if( testRoutingTableId != null ) {
                            if( testVLANVMId != null ) {
                                RoutingTable rtb = support.getRoutingTable(testRoutingTableId);
                                tm.out("Route Table", rtb);
                                assertNotNull("The test route table was not found in the cloud", rtb);

                                VirtualMachine vm = computeSupport.getVirtualMachine(testVLANVMId);
                                tm.out("Virtual Machine", vm);
                                assertNotNull("Did not find the test virtual machine " + testVLANVMId, vm);

                                String successfulCidr = "";
                                for( String destinationCidr : cidrs ) {
                                    try {
                                        support.addRouteToNetworkInterface(testRoutingTableId, IPVersion.IPV4, destinationCidr, vm.getProviderNetworkInterfaceIds()[0]);
                                        successfulCidr = destinationCidr;
                                        break;
                                    } catch( Exception e ) {
                                        // ignore
                                    }
                                }

                                if( !successfulCidr.equalsIgnoreCase("") ) {
                                    try {
                                        Thread.sleep(5000L);
                                    } catch( InterruptedException ignore ) {
                                    }

                                    rtb = support.getRoutingTable(testRoutingTableId);
                                    tm.out("Route Table", rtb);
                                    assertNotNull("The test route table was not found in the cloud", rtb);

                                    Route[] routes = rtb.getRoutes();
                                    Boolean rightRoute = false;
                                    for( Route route : routes ) {
                                        String eniId = route.getGatewayNetworkInterfaceId();
                                        String destCidr = route.getDestinationCidr();

                                        if( destCidr.equalsIgnoreCase(successfulCidr) && eniId != null ) {
                                            if( eniId.equalsIgnoreCase(vm.getProviderNetworkInterfaceIds()[0]) ) {
                                                rightRoute = true;
                                            }
                                        }
                                    }
                                    assertTrue("The created route was not found in the route table", rightRoute);
                                } else {
                                    fail("Unable to add route for: " + name.getMethodName());
                                }
                            } else if( computeSupport.isSubscribed() ) {
                                fail("No test virtual machine exists and thus no test could be run for " + name.getMethodName());
                            }
                        } else {
                            if( !support.isSubscribed() ) {
                                tm.ok("No test route table was identified for tests due to a lack of subscription to VLAN support");
                            } else if( support.getCapabilities().getRoutingTableSupport().equals(Requirement.NONE) ) {
                                tm.ok("Route Tables are not supported so there is no test for " + name.getMethodName());
                            } else {
                                fail("No test route table was found for running the stateful test: " + name.getMethodName());
                            }
                        }
                    } else {
                        tm.ok("No virtual machine support in this cloud");
                    }
                } else {
                    tm.ok("No VLAN support in this cloud");
                }
            } else {
                tm.ok("No compute services in this cloud");
            }
        } else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void addRouteToGateway() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                if( testRoutingTableId != null && testInternetGatewayId != null ) {

                    RoutingTable rtb = support.getRoutingTable(testRoutingTableId);
                    tm.out("Route Table", rtb);
                    assertNotNull("The test route table was not found in the cloud", rtb);

                    InternetGateway ig = support.getInternetGatewayById(testInternetGatewayId);
                    tm.out("Internet Gateway", ig);
                    assertNotNull("Did not find the test internet gateway " + testInternetGatewayId, ig);

                    String successfulCidr = "";
                    for( String destinationCidr : cidrs ) {
                        try {
                            support.addRouteToGateway(testRoutingTableId, IPVersion.IPV4, destinationCidr, testInternetGatewayId);
                            successfulCidr = destinationCidr;
                            break;
                        } catch( Exception e ) {
                            // ignore
                        }
                    }

                    if( !successfulCidr.equalsIgnoreCase("") ) {
                        try {
                            Thread.sleep(5000L);
                        } catch( InterruptedException ignore ) {
                        }

                        rtb = support.getRoutingTable(testRoutingTableId);
                        tm.out("Route Table", rtb);
                        assertNotNull("The test route table was not found in the cloud", rtb);

                        Route[] routes = rtb.getRoutes();
                        Boolean rightRoute = false;
                        for( Route route : routes ) {
                            String igId = route.getGatewayId();
                            String destCidr = route.getDestinationCidr();

                            if( destCidr.equalsIgnoreCase(successfulCidr) && igId != null ) {
                                if( igId.equalsIgnoreCase(testInternetGatewayId) ) {
                                    rightRoute = true;
                                }
                            }
                        }
                        assertTrue("The created route was not found in the route table", rightRoute);
                    } else {
                        fail("Unable to add route for: " + name.getMethodName());
                    }
                } else {
                    if( !support.isSubscribed() ) {
                        tm.ok("No test route table was identified for tests due to a lack of subscription to VLAN support");
                    } else if( support.getCapabilities().getRoutingTableSupport().equals(Requirement.NONE) ) {
                        tm.ok("Route Tables are not supported so there is no test for " + name.getMethodName());
                    } else {
                        if( testRoutingTableId == null ) {
                            fail("No test route table was found for running the stateful test: " + name.getMethodName());
                        } else {
                            fail("No test internet gateway was found for running the stateful test: " + name.getMethodName());
                        }
                    }
                }
            } else {
                tm.ok("No VLAN support in this cloud");
            }
        } else {
            tm.ok("No network services in this cloud");
        }
    }

}
