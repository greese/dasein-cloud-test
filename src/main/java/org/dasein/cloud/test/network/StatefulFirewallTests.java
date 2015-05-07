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

import junit.framework.Assert;
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
import org.dasein.cloud.network.RuleTargetType;
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

import javax.annotation.Nonnull;
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

    private String testFirewallId;
    private String testRuleId;
    private String testVLANId;

    public StatefulFirewallTests() {
    }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        if( name.getMethodName().equals("createVLANFirewall") || name.getMethodName().equals("createVLANFirewallWithRule") ) {
            testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
        }
        else if( name.getMethodName().equals("launchVM") || name.getMethodName().equals("verifyDuplicateRejection") ||
                name.getMethodName().equals("createVLANFirewallAndAddAndRemoveIcmpRule") ) {
            ComputeServices services = tm.getProvider().getComputeServices();
            VirtualMachineSupport support;

            try {
                support = (services == null ? null : services.getVirtualMachineSupport());
                boolean vlanForVMProv = (support != null && !support.getCapabilities().identifyVlanRequirement().equals(Requirement.NONE));

                if( vlanForVMProv ) {
                    testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
                    if( testVLANId == null ) {
                        testVLANId = tm.getTestVLANId(DaseinTestManager.STATELESS, false, null);
                    }
                }
                NetworkServices networkServices = tm.getProvider().getNetworkServices();
                FirewallSupport firewallSupport;
                firewallSupport = (networkServices == null ? null : networkServices.getFirewallSupport());
                boolean vlanForFirewall = (firewallSupport != null && !firewallSupport.getCapabilities().requiresVLAN().equals(Requirement.NONE));


                if (vlanForFirewall) {
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

                        if( name.getMethodName().contains("IngressAllow") ) {
                            direction = Direction.INGRESS;
                            permission = Permission.ALLOW;
                        }
                        else if( name.getMethodName().contains("IngressDeny") ) {
                            direction = Direction.INGRESS;
                            permission = Permission.DENY;
                        }
                        else if( name.getMethodName().contains("EgressAllow") ) {
                            direction = Direction.EGRESS;
                            permission = Permission.ALLOW;
                        }
                        else if( name.getMethodName().contains("EgressDeny") ) {
                            direction = Direction.EGRESS;
                            permission = Permission.DENY;
                        }
                        if( direction != null && permission != null ) {
                            RuleTargetType type = RuleTargetType.CIDR;

                            if( name.getMethodName().contains("Global") && !name.getMethodName().contains("OldStyle") ) {
                                type = RuleTargetType.GLOBAL;
                            }
                            if( direction.equals(Direction.INGRESS) ) {
                                source = getRandomEndpoint(type);
                                destination = RuleTarget.getGlobal(testFirewallId);
                            }
                            else {
                                source = RuleTarget.getGlobal(testFirewallId);
                                destination = getRandomEndpoint(type);
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

    private @Nonnull RuleTarget getRandomEndpoint(@Nonnull RuleTargetType type) {
        switch( type ) {
            case CIDR:
                return RuleTarget.getCIDR("209.98.98.98/32");
            case GLOBAL:
                String id = tm.getTestAnyFirewallId("endpoint", true);

                if( id != null ) {
                    return RuleTarget.getGlobal(id);
                }
                break;
        }
        fail("Unable to generate an appropriate endpoint type");
        return null;
    }

    private void checkAddRule(Direction direction, Permission permission, boolean vlanTest, RuleTargetType type) throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();
        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        FirewallSupport support = services.getFirewallSupport();
        if( support == null ) {
            tm.ok("Firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        if( testFirewallId == null ) {
            if( !support.getCapabilities().supportsFirewallCreation(vlanTest) ) {
                tm.warn("Could not create a test firewall to verify rule adding, so this test is definitely not valid");
            }
            else if( support.getCapabilities().supportsRules(direction, permission, vlanTest) ) {
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
            sourceEndpoint = getRandomEndpoint(type);
            destinationEndpoint = RuleTarget.getGlobal(testFirewallId);
        }
        else {
            destinationEndpoint = getRandomEndpoint(type);
            sourceEndpoint = RuleTarget.getGlobal(testFirewallId);
        }
        boolean supported = false;

        for( RuleTargetType t : support.getCapabilities().listSupportedSourceTypes(vlanTest) ) {
            if( t.equals(sourceEndpoint.getRuleTargetType()) ) {
                supported = true;
                break;
            }
        }
        if( !supported ) {
            tm.ok("Source type " + sourceEndpoint.getRuleTargetType() + " is not supported");
            return;
        }
        supported = false;
        for( RuleTargetType t : support.getCapabilities().listSupportedDestinationTypes(vlanTest) ) {
            if( t.equals(destinationEndpoint.getRuleTargetType()) ) {
                supported = true;
                break;
            }
        }
        if( !supported ) {
            tm.ok("Destination type " + destinationEndpoint.getRuleTargetType() + " is not supported");
            return;
        }
        if( support.getCapabilities().supportsRules(direction, permission, vlanTest) ) {
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

    private void checkRemoveRule(Direction direction, Permission permission, boolean vlanTest, boolean oldStyle) throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();
        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        FirewallSupport support = services.getFirewallSupport();
        if( support == null ) {
            tm.ok("Firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        if( testRuleId == null ) {
            if( support.getCapabilities().supportsRules(direction, permission, vlanTest) ) {
                RuleTargetType type = RuleTargetType.CIDR;

                if( name.getMethodName().contains("Global") ) {
                    type = RuleTargetType.GLOBAL;
                }
                boolean supported = false;

                if( direction.equals(Direction.INGRESS) ) {
                    for( RuleTargetType t : support.getCapabilities().listSupportedSourceTypes(vlanTest) ) {
                        if( t.equals(type) ) {
                            supported = true;
                            break;
                        }
                    }
                }
                else {
                    for( RuleTargetType t : support.getCapabilities().listSupportedDestinationTypes(vlanTest) ) {
                        if( t.equals(type) ) {
                            supported = true;
                            break;
                        }
                    }
                }
                if( supported ) {
                    fail("No test rule exists even though these type of rules are supported");
                }
                else {
                    tm.ok("Rule targe type " + type + " is not supported");
                    return;
                }
            }
            else {
                tm.ok("Rule type not supported");
                return;
            }
        }
        if( !oldStyle ) {
            support.revoke(testRuleId);
        }
        else {
            FirewallRule test = null;

            for( FirewallRule rule : support.getRules(testFirewallId) ) {
                if( rule.getProviderRuleId().equals(testRuleId) ) {
                    test = rule;
                    break;
                }
            }
            assertNotNull("Test firewall rule cannot be found for " + testRuleId, test);
            if( direction.equals(Direction.INGRESS) ) {
                //noinspection deprecation
                support.revoke(testFirewallId, direction, permission, test.getSource(), test.getProtocol(), test.getDestinationEndpoint(), test.getStartPort(), test.getEndPort());
            }
            else {
                RuleTarget dest = test.getDestinationEndpoint();
                String source = null;

                switch( dest.getRuleTargetType() ) {
                    case CIDR: source = dest.getCidr(); break;
                    case GLOBAL: source = dest.getProviderFirewallId(); break;
                    case VLAN: source = dest.getProviderVlanId(); break;
                    case VM: source = dest.getProviderVirtualMachineId(); break;
                }
                Assert.assertNotNull("Unknown target type: " + dest.getRuleTargetType(), source);
                support.revoke(testFirewallId, direction, permission, source, test.getProtocol(), test.getSourceEndpoint(), test.getStartPort(), test.getEndPort());
                try { Thread.sleep(2000L); } // give provider time to propagate rule change
                catch( InterruptedException ignore ) { }
            }
        }
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

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        FirewallSupport support = services.getFirewallSupport();
        if( support == null ) {
            tm.ok("Firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        NetworkResources net = DaseinTestManager.getNetworkResources();
        if( net == null ) {
            fail("Network resources failed to initialize for " + tm.getProvider().getCloudName());
        }

        if( support.getCapabilities().supportsFirewallCreation(false) ) {
            String id = net.provisionFirewall("provisionFirewall", null);

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

    @Test
    public void createGeneralFirewallWithRule() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();
        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        FirewallSupport support = services.getFirewallSupport();
        if( support == null ) {
            tm.ok("Firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        NetworkResources net = DaseinTestManager.getNetworkResources();
        if( net == null ) {
            fail("Network resources failed to initialize for " + tm.getProvider().getCloudName());
        }

        int p = port++;

        if( support.getCapabilities().supportsFirewallCreation(false) ) {
            String id = net.provisionFirewall("provisionFirewall", null, net.constructRuleCreateOptions(p, Direction.INGRESS, Permission.ALLOW));

            tm.out("New Firewall", id);
            assertNotNull("No firewall was created by this test", id);
            Iterable<FirewallRule> rules = support.getRules(id);

            tm.out("Initial rules", rules);
            assertNotNull("Firewall rules are null post firewall create of " + id, rules);
            boolean hasRule = false;

            for( FirewallRule rule : support.getRules(id) ) {
                tm.out("\tRule", rule);
                RuleTarget source = rule.getSourceEndpoint();
                RuleTarget dest = rule.getDestinationEndpoint();

                if( source.getRuleTargetType().equals(RuleTargetType.CIDR) ) {
                    if( dest.getRuleTargetType().equals(RuleTargetType.GLOBAL) ) {
                        if( id.equals(dest.getProviderFirewallId()) ) {
                            if( NetworkResources.TEST_CIDR.equals(source.getCidr()) ) {
                                hasRule = true;
                                break;
                            }
                        }
                    }
                }
            }
            assertTrue("The initial rule was not created with the test firewall", hasRule);
        }
        else {
            try {
                net.provisionFirewall(name.getMethodName(), null, net.constructRuleCreateOptions(p, Direction.INGRESS, Permission.ALLOW));
                fail("Firewall provisioning completed even though general firewall creation is not supported");
            }
            catch( OperationNotSupportedException expected ) {
                tm.ok("Caught OperationNotSupportedException as expected for " + name.getMethodName());
            }
        }
    }

    @Test
    public void createVLANFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();
        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        FirewallSupport support = services.getFirewallSupport();
        if( support == null ) {
            tm.ok("Firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        NetworkResources net = DaseinTestManager.getNetworkResources();
        if( net == null ) {
            fail("Network resources failed to initialize for " + tm.getProvider().getCloudName());
        }

        if( support.getCapabilities().supportsFirewallCreation(true) ) {
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

    @Test
    public void createVLANFirewallWithRule() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        FirewallSupport support = services.getFirewallSupport();
        if( support == null ) {
            tm.ok("Firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        NetworkResources net = DaseinTestManager.getNetworkResources();
        if( net == null ) {
            fail("Network resources failed to initialize for " + tm.getProvider().getCloudName());
        }

        int p = port++;
        if( support.getCapabilities().supportsFirewallCreation(true) ) {
            if( testVLANId != null ) {
                String id = net.provisionFirewall("provision", testVLANId, net.constructRuleCreateOptions(p, Direction.INGRESS, Permission.ALLOW));

                tm.out("New VLAN Firewall", id);
                assertNotNull("No VLAN firewall was created by this test", id);

                Iterable<FirewallRule> rules = support.getRules(id);
                tm.out("Initial rules", rules);
                assertNotNull("Firewall rules are null post firewall create of " + id, rules);
                boolean hasRule = false;

                for( FirewallRule rule : support.getRules(id) ) {
                    tm.out("\tRule", rule);
                    RuleTarget source = rule.getSourceEndpoint();
                    RuleTarget dest = rule.getDestinationEndpoint();

                    if( source.getRuleTargetType().equals(RuleTargetType.CIDR) ) {
                        if( dest.getRuleTargetType().equals(RuleTargetType.GLOBAL) ) {
                            if( id.equals(dest.getProviderFirewallId()) ) {
                                if( NetworkResources.TEST_CIDR.equals(source.getCidr()) ) {
                                    hasRule = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                assertTrue("The initial rule was not created with the test firewall", hasRule);
            }
            else {
                fail("Firewall creation in VLANs is supposedly supported, but there's not test VLAN ID");
            }
        }
        else {
            String id = (testVLANId == null ? UUID.randomUUID().toString() : testVLANId);

            try {
                net.provisionFirewall(name.getMethodName(), id, net.constructRuleCreateOptions(p, Direction.INGRESS, Permission.ALLOW));
                fail("Firewall provisioning completed even though VLAN firewall creation is not supported");
            }
            catch( OperationNotSupportedException expected ) {
                tm.ok("Caught OperationNotSupportedException as expected for " + name.getMethodName());
            }
        }
    }

    @Test
    public void addGeneralIngressAllow() throws CloudException, InternalException {
        checkAddRule(Direction.INGRESS, Permission.ALLOW, false, RuleTargetType.CIDR);
    }

    @Test
    public void addGeneralIngressDeny() throws CloudException, InternalException {
        checkAddRule(Direction.INGRESS, Permission.DENY, false, RuleTargetType.CIDR);
    }

    @Test
    public void addGeneralEgressAllow() throws CloudException, InternalException {
        checkAddRule(Direction.EGRESS, Permission.ALLOW, false, RuleTargetType.CIDR);
    }

    @Test
    public void addGeneralEgressDeny() throws CloudException, InternalException {
        checkAddRule(Direction.EGRESS, Permission.DENY, false, RuleTargetType.CIDR);
    }

    @Test
    public void addVLANIngressAllow() throws CloudException, InternalException {
        checkAddRule(Direction.INGRESS, Permission.ALLOW, true, RuleTargetType.CIDR);
    }

    @Test
    public void addVLANIngressDeny() throws CloudException, InternalException {
        checkAddRule(Direction.INGRESS, Permission.DENY, true, RuleTargetType.CIDR);
    }

    @Test
    public void addVLANEgressAllow() throws CloudException, InternalException {
        checkAddRule(Direction.EGRESS, Permission.ALLOW, true, RuleTargetType.CIDR);
    }

    @Test
    public void addVLANEgressDeny() throws CloudException, InternalException {
        checkAddRule(Direction.EGRESS, Permission.DENY, true, RuleTargetType.CIDR);
    }

    @Test
    public void addGeneralIngressAllowGlobal() throws CloudException, InternalException {
        checkAddRule(Direction.INGRESS, Permission.ALLOW, false, RuleTargetType.GLOBAL);
    }

    @Test
    public void revokeGeneralIngressAllow() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.ALLOW, false, false);
    }

    @Test
    public void revokeGeneralIngressDeny() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.DENY, false, false);
    }

    @Test
    public void revokeGeneralEgressAllow() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.ALLOW, false, false);
    }

    @Test
    public void revokeGeneralEgressDeny() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.DENY, false, false);
    }

    @Test
    public void revokeVLANIngressAllow() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.ALLOW, true, false);
    }

    @Test
    public void revokeVLANIngressDeny() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.DENY, true, false);
    }

    @Test
    public void revokeVLANEgressAllow() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.ALLOW, true, false);
    }

    @Test
    public void revokeVLANEgressDeny() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.DENY, true, false);
    }

    @Test
    public void revokeGeneralIngressAllowOldStyle() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.ALLOW, false, true);
    }

    @Test
    public void revokeGeneralIngressDenyOldStyle() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.DENY, false, true);
    }

    @Test
    public void revokeGeneralEgressAllowOldStyle() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.ALLOW, false, true);
    }

    @Test
    public void revokeGeneralEgressDenyOldStyle() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.DENY, false, true);
    }

    @Test
    public void revokeVLANIngressAllowOldStyle() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.ALLOW, true, true);
    }

    @Test
    public void revokeVLANIngressDenyOldStyle() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.DENY, true, true);
    }

    @Test
    public void revokeVLANEgressAllowOldStyle() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.ALLOW, true, true);
    }

    @Test
    public void revokeVLANEgressDenyOldStyle() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.DENY, true, true);
    }

    @Test
    public void revokeGeneralIngressAllowOldStyleGlobal() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.ALLOW, false, true);
    }

    @Test
    public void revokeGeneralIngressDenyOldStyleGlobal() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.DENY, false, true);
    }

    @Test
    public void revokeGeneralEgressAllowOldStyleGlobal() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.ALLOW, false, true);
    }

    @Test
    public void revokeGeneralEgressDenyOldStyleGlobal() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.DENY, false, true);
    }

    @Test
    public void revokeVLANIngressAllowOldStyleGlobal() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.ALLOW, true, true);
    }

    @Test
    public void revokeVLANIngressDenyOldStyleGlobal() throws CloudException, InternalException {
        checkRemoveRule(Direction.INGRESS, Permission.DENY, true, true);
    }

    @Test
    public void revokeVLANEgressAllowOldStyleGlobal() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.ALLOW, true, true);
    }

    @Test
    public void revokeVLANEgressDenyOldStyleGlobal() throws CloudException, InternalException {
        checkRemoveRule(Direction.EGRESS, Permission.DENY, true, true);
    }

    @Test
    public void removeFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();
        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        FirewallSupport support = services.getFirewallSupport();
        if( support == null ) {
            tm.ok("Firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        if( testFirewallId != null ) {
            if (support.getCapabilities().supportsFirewallDeletion()) {
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
                try {
                    support.delete(testFirewallId);
                    fail("Firewall deletion not supported but completed without error");
                }
                catch (OperationNotSupportedException e) {
                    tm.ok("Caught not supported exception for delete Firewall in cloud that does not support firewall deletion");
                }
            }
        }
        else {
            if( !support.getCapabilities().supportsFirewallCreation(true) && !support.getCapabilities().supportsFirewallCreation(false) ) {
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

    @Test
    public void launchVM() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();
        if( services == null ) {
            tm.ok("No compute services in " + tm.getProvider().getCloudName());
            return;
        }

        VirtualMachineSupport support = services.getVirtualMachineSupport();
        if( support == null ) {
            tm.ok("No virtual machine support in " + tm.getProvider().getCloudName());
            return;
        }

        NetworkServices networkServices = tm.getProvider().getNetworkServices();
        if( networkServices == null ) {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
            return;
        }
        if( !networkServices.hasFirewallSupport() ) {
            tm.ok("Firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        boolean inVlan = !support.getCapabilities().identifyVlanRequirement().equals(Requirement.NONE);
        String testSubnetId = null;

        if( inVlan && testVLANId == null ) {
            fail("No test VLAN exists to test launching a VM behind a firewall");
        }
        else if( inVlan ) {
            testSubnetId = tm.getTestSubnetId(DaseinTestManager.STATEFUL, true, testVLANId, null);
        }
        ComputeResources compute = DaseinTestManager.getComputeResources();

        if( compute == null ) {
            fail("Compute resources failed to initialize for " + tm.getProvider().getCloudName());
        }
        String productId = tm.getTestVMProductId();

        assertNotNull("Unable to identify a VM product for test launch", productId);
        String imageId = tm.getTestImageId(DaseinTestManager.STATELESS, false);

        assertNotNull("Unable to identify a test image for test launch", imageId);
        VMLaunchOptions options = VMLaunchOptions.getInstance(productId, imageId, "dsnfw" + (System.currentTimeMillis()%10000), "Dasein Firewall Launch " + System.currentTimeMillis(), "Test launch for a VM in a firewall");

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
                options.inSubnet(null, dataCenterId, testVLANId, testSubnetId);
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
                if( fw.getCapabilities().supportsFirewallCreation(inVlan) ) {
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
        assertNotNull("The VM firewalls should not be null", fwIds);
        assertEquals("The number of firewalls is incorrect", 1, fwIds.length);
        assertEquals("The firewall IDs do not match the test firewall", testFirewallId, fwIds[0]);
    }

    @Test
    public void createVLANFirewallAndAddAndRemoveIcmpRule() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();
        if( services == null ) {
            tm.ok("Networking is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        FirewallSupport support = services.getFirewallSupport();
        if( support == null ) {
            tm.ok("Firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        if( testFirewallId == null ) {
            if( !support.getCapabilities().supportsFirewallCreation(true) ) {
                tm.warn("Could not create a test firewall to verify rule adding, so this test is definitely not valid");
            } else {
                fail("Firewall creation is supported, however no test firewall was found");
            }
            return;
        }

        try {
            String ruleId = support.authorize(testFirewallId, "0.0.0.0/0", Protocol.ICMP, -1, -1);
            assertNotNull("Failed to generate a VLAN ICMP rule", ruleId);
        }
        finally {
            Iterable<FirewallRule> rules = support.getRules(testFirewallId);

            for( FirewallRule rule : rules ) {
                tm.out(testFirewallId + " - " + rule.getProtocol());
                support.revoke(rule.getProviderRuleId());
            }
            rules = support.getRules(testFirewallId);
            assertFalse("The rules have not been deleted", rules.iterator().hasNext());
        }
    }

    @Test
    public void verifyDuplicateRejection() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Networking is not supported in "  + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        FirewallSupport support = services.getFirewallSupport();
        if( support == null ) {
            tm.ok("Firewalls are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testFirewallId == null ) {
            if( !support.getCapabilities().supportsFirewallCreation(true) ) {
                tm.warn("Could not create a test firewall to verify rule adding, so this test is definitely not valid");
            } else {
                fail("Firewall creation is supported, however no test firewall was found");
            }
            return;
        }
        try {
            //testFirewallId = "fw-" + testVLANId; // Hack to enable GCE to use this test, leave comment in
            String ruleId = support.authorize(testFirewallId, "0.0.0.0/0", Protocol.ICMP, -1, -1);
            assertNotNull("Failed to generate a VLAN ICMP rule", ruleId);
            try {
                support.authorize(testFirewallId, "0.0.0.0/0", Protocol.ICMP, -1, -1);
                fail("should have generated a duplicate rule exception.");
            } catch ( CloudException ex) {
                tm.ok("Exception occurred as expected when trying to create a duplicate rule: " + ex.getMessage());
            }
        }
        finally {
            Iterable<FirewallRule> rules = support.getRules(testFirewallId);
            for( FirewallRule rule : rules ) {
                tm.out(testFirewallId + " - " + rule.getProtocol());
                support.revoke(rule.getProviderRuleId());
            }
            rules = support.getRules(testFirewallId);
            assertFalse("The rules have not been deleted", rules.iterator().hasNext());
        }
    }
}
