/**
 * Copyright (C) 2009-2013 Enstratius, Inc.
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
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.RuleTarget;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.compute.ComputeResources;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/23/13 10:22 PM</p>
 *
 * @author George Reese
 */
public class StatefulFirewallTests {
    static private DaseinTestManager tm;

    static private int port = 81;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulFirewallTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String  testFirewallId;
    private String  testRuleId;
    private String  testVLANId;

    public StatefulFirewallTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        if( name.getMethodName().equals("createVLANFirewall") ) {
            testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
        }
        else if( name.getMethodName().equals("launchVM") ) {
            ComputeServices services = tm.getProvider().getComputeServices();
            VirtualMachineSupport support;

            try {
                support = (services == null ? null : services.getVirtualMachineSupport());
                boolean vlan = (support != null && support.identifyVlanRequirement().equals(Requirement.REQUIRED));

                if( vlan ) {
                    testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
                    if( testVLANId == null ) {
                        testVLANId = tm.getTestVLANId(DaseinTestManager.STATELESS, false, null);
                    }
                    testFirewallId = tm.getTestVLANFirewallId(DaseinTestManager.STATEFUL, true, testVLANId);
                }
                else {
                    testFirewallId = tm.getTestGeneralFirewallId(DaseinTestManager.STATEFUL, true);
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        else if( name.getMethodName().equals("removeFirewall") ) {
            testFirewallId = tm.getTestAnyFirewallId(DaseinTestManager.REMOVED, true);
        }
        else if( name.getMethodName().startsWith("addGeneral") ) {
            testFirewallId = tm.getTestGeneralFirewallId(DaseinTestManager.STATEFUL, true);
        }
        else if( name.getMethodName().startsWith("addVLAN") ) {
            testFirewallId = tm.getTestVLANFirewallId(DaseinTestManager.STATEFUL, true, null);
        }
        else if( name.getMethodName().startsWith("revoke") ) {
            if( name.getMethodName().startsWith("revokeGeneral") ) {
                testFirewallId = tm.getTestGeneralFirewallId(DaseinTestManager.STATEFUL, true);
            }
            else {
                testFirewallId = tm.getTestVLANFirewallId(DaseinTestManager.STATEFUL, true, null);
            }
            if( testFirewallId != null ) {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services != null ) {
                    FirewallSupport support = services.getFirewallSupport();

                    if( support != null ) {
                        RuleTarget source, destination;
                        Permission permission = null;
                        Direction direction = null;
                        int p = port++;

                        if( name.getMethodName().endsWith("IngressAllow") ) {
                            direction = Direction.INGRESS;
                            permission = Permission.ALLOW;
                        }
                        else if( name.getMethodName().endsWith("IngressDeny") ) {
                            direction = Direction.INGRESS;
                            permission = Permission.DENY;
                        }
                        else if( name.getMethodName().endsWith("EgressAllow") ) {
                            direction = Direction.EGRESS;
                            permission = Permission.ALLOW;
                        }
                        else if( name.getMethodName().endsWith("EgressDeny") ) {
                            direction = Direction.EGRESS;
                            permission = Permission.DENY;
                        }
                        if( direction != null && permission != null ) {
                            if( direction.equals(Direction.INGRESS) ) {
                                source = RuleTarget.getCIDR("209.98.98.98/32");
                                destination = RuleTarget.getGlobal(testFirewallId);
                            }
                            else {
                                source = RuleTarget.getGlobal(testFirewallId);
                                destination = RuleTarget.getCIDR("209.98.98.98/32");
                            }
                            try {
                                testRuleId = support.authorize(testFirewallId, direction, permission, source, Protocol.TCP, destination, p, p, 0);
                            }
                            catch( Throwable ignore ) {
                                // ignore
                            }
                        }
                    }
                }
            }
        }
    }

    @After
    public void after() {
        try {
            testVLANId = null;
            testFirewallId = null;
            testRuleId = null;
        }
        finally {
            tm.end();
        }
    }

    private void checkAddRule(Direction direction, Permission permission, boolean vlanTest) throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getProvider().getCloudName());
            return;
        }

        FirewallSupport support = services.getFirewallSupport();

        if( support == null ) {
            tm.ok("Firewalls are not supported in " + tm.getProvider().getCloudName());
            return;
        }
        if( testFirewallId == null ) {
            if( !support.supportsFirewallCreation(vlanTest) ) {
                tm.warn("Could not create a test firewall to verify rule adding, so this test is definitely not valid");
            }
            else if( support.supportsRules(direction, permission, vlanTest) ) {
                fail("No test firewall even though these type of rules are supported");
            }
            else {
                tm.ok("Rule type " + direction + "/" + permission + " not supported");
            }
            return;
        }
        int p = port++;

        RuleTarget sourceEndpoint, destinationEndpoint;

        if( direction.equals(Direction.INGRESS) ) {
            sourceEndpoint = RuleTarget.getCIDR("209.98.98.98/32");
            destinationEndpoint = RuleTarget.getGlobal(testFirewallId);
        }
        else {
            destinationEndpoint = RuleTarget.getCIDR("209.98.98.98/32");
            sourceEndpoint = RuleTarget.getGlobal(testFirewallId);
        }
        if( support.supportsRules(direction, permission, vlanTest) ) {
            String ruleId = support.authorize(testFirewallId, direction, permission, sourceEndpoint, Protocol.TCP, destinationEndpoint, p, p, 0);
            boolean found = false;

            //ALLOW:sg-22c5d74e:GLOBAL:sg-22c5d74e:EGRESS:TCP:87:87:CIDR:209.98.98.98/32
            tm.out("New Rule", ruleId);
            for( FirewallRule rule : support.getRules(testFirewallId) ) {
                if( rule.getProviderRuleId().equals(ruleId) ) {
                    found = true;
                    break;
                }
            }
            tm.out("Listed", found);
            assertTrue("Failed to identify new rule in the list of firewall rules", found);
        }
        else {
            try {
                support.authorize(testFirewallId, direction, permission, sourceEndpoint, Protocol.TCP, destinationEndpoint, p, p, 0);
            }
            catch( OperationNotSupportedException e ) {
                tm.ok("OperationNotSupportedException caught indicating lack of support for " + direction + "/" + permission + "/" + vlanTest);
            }
        }
    }

    private void checkRemoveRule(Direction direction, Permission permission, boolean vlanTest) throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getProvider().getCloudName());
            return;
        }

        FirewallSupport support = services.getFirewallSupport();

        if( support == null ) {
            tm.ok("Firewalls are not supported in " + tm.getProvider().getCloudName());
            return;
        }
        if( testRuleId == null ) {
            if( support.supportsRules(direction, permission, vlanTest) ) {
                fail("No test rule exists even though these type of rules are supported");
            }
            else {
                tm.ok("Rule type not supported");
                return;
            }
        }
        support.revoke(testRuleId);
        boolean found = false;
        for( FirewallRule rule : support.getRules(testFirewallId) ) {
            if( rule.getProviderRuleId().equals(testRuleId) ) {
                found = true;
            }
        }
        tm.out("Rule Present", found);
        assertFalse("Found the test rule among the rules for the firewall post-removal", found);
    }

    @Test
    public void createGeneralFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                NetworkResources net = DaseinTestManager.getNetworkResources();

                if( net != null ) {
                    if( support.supportsFirewallCreation(false) ) {
                        String id = net.provisionFirewall("provisionKeypair", null);

                        tm.out("New Firewall", id);
                        assertNotNull("No firewall was created by this test", id);
                    }
                    else {
                        try {
                            net.provisionFirewall(name.getMethodName(), null);
                            fail("Firewall provisioning completed even though general firewall creation is not supported");
                        }
                        catch( OperationNotSupportedException expected ) {
                            tm.ok("Caught OperationNotSupportedException as expected for " + name.getMethodName());
                        }
                    }
                }
                else {
                    fail("Network resources failed to initialize for " + tm.getProvider().getCloudName());
                }
            }
            else {
                tm.ok("Firewalls are not supported in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("Network services are not supported in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void createVLANFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                NetworkResources net = DaseinTestManager.getNetworkResources();

                if( net != null ) {
                    if( support.supportsFirewallCreation(true) ) {
                        if( testVLANId != null ) {
                            String id = net.provisionFirewall("provision", testVLANId);

                            tm.out("New VLAN Firewall", id);
                            assertNotNull("No VLAN firewall was created by this test", id);
                        }
                        else {
                            fail("Firewall creation in VLANs is supposedly supported, but there's not test VLAN ID");
                        }
                    }
                    else {
                        String id = (testVLANId == null ? UUID.randomUUID().toString() : testVLANId);

                        try {
                            net.provisionFirewall(name.getMethodName(), id);
                            fail("Firewall provisioning completed even though VLAN firewall creation is not supported");
                        }
                        catch( OperationNotSupportedException expected ) {
                            tm.ok("Caught OperationNotSupportedException as expected for " + name.getMethodName());
                        }
                    }
                }
                else {
                    fail("Network resources failed to initialize for " + tm.getProvider().getCloudName());
                }
            }
            else {
                tm.ok("Firewalls are not supported in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("Network services are not supported in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void addGeneralIngressAllow() throws CloudException, InternalException {
        checkAddRule(Direction.INGRESS, Permission.ALLOW, false);
    }

    @Test
    public void addGeneralIngressDeny() throws CloudException, InternalException {
        checkAddRule(Direction.INGRESS, Permission.DENY, false);
    }

    @Test
    public void addGeneralEgressAllow() throws CloudException, InternalException {
        checkAddRule(Direction.EGRESS, Permission.ALLOW, false);
    }

    @Test
    public void addGeneralEgressDeny() throws CloudException, InternalException {
        checkAddRule(Direction.EGRESS, Permission.DENY, false);
    }

    @Test
    public void addVLANIngressAllow() throws CloudException, InternalException {
        checkAddRule(Direction.INGRESS, Permission.ALLOW, true);
    }

    @Test
    public void addVLANIngressDeny() throws CloudException, InternalException {
        checkAddRule(Direction.INGRESS, Permission.DENY, true);
    }

    @Test
    public void addVLANEgressAllow() throws CloudException, InternalException {
        checkAddRule(Direction.EGRESS, Permission.ALLOW, true);
    }

    @Test
    public void addVLANEgressDeny() throws CloudException, InternalException {
        checkAddRule(Direction.EGRESS, Permission.DENY, true);
    }

    @Test
    public void revokeGeneralIngressAllow() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.ALLOW, false);
    }

    @Test
    public void revokeGeneralIngressDeny() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.DENY, false);
    }

    @Test
    public void revokeGeneralEgressAllow() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.ALLOW, false);
    }

    @Test
    public void revokeGeneralEgressDeny() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.DENY, false);
    }

    @Test
    public void revokeVLANIngressAllow() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.ALLOW, true);
    }

    @Test
    public void revokeVLANIngressDeny() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.DENY, true);
    }

    @Test
    public void revokeVLANEgressAllow() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.ALLOW, true);
    }

    @Test
    public void revokeVLANEgressDeny() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.DENY, true);
    }

    @Test
    public void removeFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                if( testFirewallId != null ) {
                    Firewall firewall = support.getFirewall(testFirewallId);

                    tm.out("Before", firewall);
                    assertNotNull("Test firewall no longer exists, cannot test removing it", firewall);
                    tm.out("Active", firewall.isActive());
                    support.delete(testFirewallId);
                    try { Thread.sleep(5000L); }
                    catch( InterruptedException ignore ) { }
                    firewall = support.getFirewall(testFirewallId);
                    tm.out("After", firewall);
                    tm.out("Active", (firewall == null ? "false" : firewall.isActive()));
                    assertTrue("The firewall remains available", (firewall == null || !firewall.isActive()));
                }
                else {
                    if( !support.supportsFirewallCreation(true) && !support.supportsFirewallCreation(false) ) {
                        tm.ok("Firewall creation/deletion is not supported in " + tm.getProvider().getCloudName());
                    }
                    else if( support.isSubscribed() ) {
                        fail("No test firewall for " + name.getMethodName());
                    }
                    else {
                        tm.ok("Firewall support is not subscribed so this test is not entirely valid");
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
            if( support != null ) {
            }
            else {
                tm.ok("No virtual machine support in " + tm.getProvider().getCloudName());
                return;
            }
        }
        else {
            tm.ok("No compute services in " + tm.getProvider().getCloudName());
            return;
        }
        boolean inVlan = support.identifyVlanRequirement().equals(Requirement.REQUIRED);
        String testSubnetId = null;

        if( inVlan && testVLANId == null ) {
            fail("No test VLAN exists to test launching a VM behind a firewall");
        }
        else if( inVlan ) {
            testSubnetId = tm.getTestSubnetId(DaseinTestManager.STATEFUL, true, testVLANId, null);
        }
        ComputeResources compute = DaseinTestManager.getComputeResources();

        if( compute != null ) {
            String productId = tm.getTestVMProductId();

            assertNotNull("Unable to identify a VM product for test launch", productId);
            String imageId = tm.getTestImageId(DaseinTestManager.STATELESS, false);

            assertNotNull("Unable to identify a test image for test launch", imageId);
            VMLaunchOptions options = VMLaunchOptions.getInstance(productId, imageId, "dsnnetl" + (System.currentTimeMillis()%10000), "Dasein Network Launch " + System.currentTimeMillis(), "Test launch for a VM in a network");

            if( testFirewallId != null ) {
                options.behindFirewalls(testFirewallId);
                if( testSubnetId != null ) {
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
            }
            else {
                NetworkServices net = tm.getProvider().getNetworkServices();
                FirewallSupport fw = (net == null ? null : net.getFirewallSupport());

                if( fw != null && fw.isSubscribed()  ) {
                    if( fw.supportsFirewallCreation(inVlan) ) {
                        fail("No test firewall was established for testing");
                    }
                    else {
                        tm.warn("Unable to test the ability to launch a VM behind a firewall due to lack of ability to create firewalls, test is invalid");
                    }
                }
                else {
                    tm.ok("Launching behind firewalls is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
                }
                return;
            }

            String vmId = compute.provisionVM(support, "fwLaunch", options, options.getDataCenterId());

            tm.out("Virtual Machine", vmId);
            assertNotNull("No error received launching VM behind firewall, but there was no virtual machine", vmId);

            VirtualMachine vm = support.getVirtualMachine(vmId);

            assertNotNull("Launched VM does not exist", vm);
            tm.out("Behind firewalls", Arrays.toString(vm.getProviderFirewallIds()));
            String[] fwIds = vm.getProviderFirewallIds();

            assertTrue("The firewall IDs do not match", fwIds.length == 1 && fwIds[0].equals(testFirewallId));
        }
    }
}
