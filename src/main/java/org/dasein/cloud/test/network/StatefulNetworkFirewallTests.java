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
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.NetworkFirewallSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.RuleTarget;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/27/13 11:19 AM</p>
 *
 * @author George Reese
 */
public class StatefulNetworkFirewallTests {
    static private final Random random = new Random();

    static private DaseinTestManager tm;

    static private int port = 81;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulNetworkFirewallTests.class);
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
    private String  testSubnetId;
    private String  testVLANId;

    public StatefulNetworkFirewallTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        if( name.getMethodName().equals("createFirewall") ) {
            testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
        }
        else if( name.getMethodName().equals("removeFirewall") ) {
            testFirewallId = tm.getTestNetworkFirewallId(DaseinTestManager.REMOVED, true, null);
        }
        else if( name.getMethodName().startsWith("add") ) {
            testFirewallId = tm.getTestNetworkFirewallId(DaseinTestManager.STATEFUL, true, null);
        }
        else if( name.getMethodName().startsWith("revoke") ) {
            testFirewallId = tm.getTestNetworkFirewallId(DaseinTestManager.STATEFUL, true, null);
            if( testFirewallId != null ) {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services != null ) {
                    NetworkFirewallSupport support = services.getNetworkFirewallSupport();

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
                                testRuleId = support.authorize(testFirewallId, direction, permission, source, Protocol.TCP, destination, p, p, random.nextInt(50) + 1);
                            }
                            catch( Throwable ignore ) {
                                // ignore
                            }
                        }
                    }
                }
            }
        }
        else if( name.getMethodName().equals("associateWithSubnet") ) {
            testFirewallId = tm.getTestNetworkFirewallId(DaseinTestManager.STATEFUL, true, null);
            if( testFirewallId != null ) {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services != null ) {
                    NetworkFirewallSupport support = services.getNetworkFirewallSupport();

                    if( support != null ) {
                        try {
                            Firewall fw = support.getFirewall(testFirewallId);

                            if( fw != null ) {
                                testVLANId = fw.getProviderVlanId();
                                if( testVLANId != null ) {
                                    testSubnetId = tm.getTestSubnetId(DaseinTestManager.STATEFUL, true, testVLANId,  null);
                                }
                            }
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
            testVLANId = null;
            testFirewallId = null;
            testRuleId = null;
        }
        finally {
            tm.end();
        }
    }

    private void checkAddRule(Direction direction, Permission permission) throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getProvider().getCloudName());
            return;
        }

        NetworkFirewallSupport support = services.getNetworkFirewallSupport();

        if( support == null ) {
            tm.ok("Network firewalls are not supported in " + tm.getProvider().getCloudName());
            return;
        }
        if( testFirewallId == null ) {
            if( !support.getCapabilities().supportsNetworkFirewallCreation() ) {
                tm.warn("Could not create a test firewall to verify rule adding, so this test is definitely not valid");
            }
            else {
                fail("No test firewall even though these type of rules are supported");
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
        String ruleId = support.authorize(testFirewallId, direction, permission, sourceEndpoint, Protocol.TCP, destinationEndpoint, p, p, 10);
        boolean found = false;

        //ALLOW:sg-22c5d74e:GLOBAL:sg-22c5d74e:EGRESS:TCP:87:87:CIDR:209.98.98.98/32
        tm.out("New Rule", ruleId);
        for( FirewallRule rule : support.listRules(testFirewallId) ) {
            if( rule.getProviderRuleId().equals(ruleId) ) {
                found = true;
                break;
            }
        }
        tm.out("Listed", found);
        assertTrue("Failed to identify new rule in the list of firewall rules", found);
    }

    private void checkRemoveRule() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getProvider().getCloudName());
            return;
        }

        NetworkFirewallSupport support = services.getNetworkFirewallSupport();

        if( support == null ) {
            tm.ok("Network firewalls are not supported in " + tm.getProvider().getCloudName());
            return;
        }
        if( testRuleId == null ) {
            fail("No test rule exists even though these type of rules are supported");
        }
        support.revoke(testRuleId);
        boolean found = false;
        for( FirewallRule rule : support.listRules(testFirewallId) ) {
            if( rule.getProviderRuleId().equals(testRuleId) ) {
                found = true;
            }
        }
        tm.out("Rule Present", found);
        assertFalse("Found the test rule among the rules for the network firewall post-removal", found);
    }

    @Test
    public void createFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        NetworkFirewallSupport support = services.getNetworkFirewallSupport();

        if( support == null ) {
            tm.ok("Network firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        NetworkResources net = DaseinTestManager.getNetworkResources();

        if( net != null ) {
            if( support.getCapabilities().supportsNetworkFirewallCreation() ) {
                if( testVLANId != null ) {
                    String id = net.provisionNetworkFirewall("provisionNetworkFirewall", testVLANId);

                    tm.out("New Network Firewall", id);
                    assertNotNull("No network firewall was created by this test", id);
                }
                else {
                    fail("Network firewall creation is supposedly supported, but there's not a test VLAN ID");
                }
            }
            else {
                String id = (testVLANId == null ? UUID.randomUUID().toString() : testVLANId);

                try {
                    net.provisionNetworkFirewall(name.getMethodName(), id);
                    fail("Network firewall provisioning completed even though network firewall creation is not supported");
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

    @Test
    public void addIngressAllow() throws CloudException, InternalException {
        checkAddRule(Direction.INGRESS, Permission.ALLOW);
    }

    @Test
    public void addIngressDeny() throws CloudException, InternalException {
        checkAddRule(Direction.INGRESS, Permission.DENY);
    }

    @Test
    public void addEgressAllow() throws CloudException, InternalException {
        checkAddRule(Direction.EGRESS, Permission.ALLOW);
    }

    @Test
    public void addEgressDeny() throws CloudException, InternalException {
        checkAddRule(Direction.EGRESS, Permission.DENY);
    }


    @Test
    public void revokeIngressAllow() throws CloudException, InternalException {
        checkRemoveRule();
    }

    @Test
    public void revokeIngressDeny() throws CloudException, InternalException {
        checkRemoveRule();
    }

    @Test
    public void revokeEgressAllow() throws CloudException, InternalException {
        checkRemoveRule();
    }

    @Test
    public void revokeEgressDeny() throws CloudException, InternalException {
        checkRemoveRule();
    }

    @Test
    public void removeFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        NetworkFirewallSupport support = services.getNetworkFirewallSupport();

        if( support == null ) {
            tm.ok("Network firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testFirewallId != null ) {
            Firewall firewall = support.getFirewall(testFirewallId);

            tm.out("Before", firewall);
            assertNotNull("Test firewall no longer exists, cannot test removing it", firewall);
            tm.out("Active", firewall.isActive());
            support.removeFirewall(testFirewallId);
            try { Thread.sleep(5000L); }
            catch( InterruptedException ignore ) { }
            firewall = support.getFirewall(testFirewallId);
            tm.out("After", firewall);
            tm.out("Active", (firewall == null ? "false" : firewall.isActive()));
            assertTrue("The firewall remains available", (firewall == null || !firewall.isActive()));
        }
        else {
            if( !support.getCapabilities().supportsNetworkFirewallCreation() ) {
                tm.ok("Firewall creation/deletion is not supported in " + tm.getProvider().getCloudName());
            }
            if( support.isSubscribed() ) {
                fail("No test firewall for " + name.getMethodName());
            }
            else {
                tm.ok("Firewall support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void associateWithSubnet() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        NetworkFirewallSupport support = services.getNetworkFirewallSupport();

        if( support == null ) {
            tm.ok("Network firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testFirewallId != null ) {
            if( testSubnetId != null ) {
                Firewall firewall = support.getFirewall(testFirewallId);

                assertNotNull("The test firewall no longer exists", firewall);
                tm.out("Before", Arrays.toString(firewall.getSubnetAssociations()));

                support.associateWithSubnet(testFirewallId, testSubnetId);

                try { Thread.sleep(5000L); }
                catch( InterruptedException ignore ) { }

                firewall = support.getFirewall(testFirewallId);
                assertNotNull("The test firewall no longer exists", firewall);

                tm.out("After", Arrays.toString(firewall.getSubnetAssociations()));

                boolean found = false;

                for( String association : firewall.getSubnetAssociations() ) {
                    if( association.equals(testSubnetId) ) {
                        found = true;
                        break;
                    }
                }
                assertTrue("Unable to find test subnet among the network firewall's subnet associations", found);
            }
            else {
                //noinspection ConstantConditions
                if( services.getVlanSupport() != null && services.getVlanSupport().getCapabilities().allowsNewSubnetCreation() ) {
                    fail("Unable to identify a test subnet for the test " + name.getMethodName());
                }
                else {
                    tm.ok("No subnets are supported in this cloud for association tests");
                }
            }
        }
        else {
            if( !support.getCapabilities().supportsNetworkFirewallCreation() ) {
                tm.ok("Firewall creation/deletion is not supported in " + tm.getProvider().getCloudName());
            }
            if( support.isSubscribed() ) {
                fail("No test firewall for " + name.getMethodName());
            }
            else {
                tm.ok("Firewall support is not subscribed so this test is not entirely valid");
            }
        }
    }
}
