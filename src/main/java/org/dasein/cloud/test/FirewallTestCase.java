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

import java.util.Locale;
import java.util.UUID;

import junit.framework.Assert;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.RuleTarget;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.util.APITrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;

@SuppressWarnings("JUnit4AnnotatedMethodInJUnit3TestCase")
public class FirewallTestCase extends BaseTestCase {
    static public final String T_ADD_STD_INGRESS_ALLOW   = "testAddStandardIngressAllow";
    static public final String T_ADD_STD_INGRESS_DENY    = "testAddStandardIngressDeny";
    static public final String T_ADD_STD_EGRESS_ALLOW    = "testAddStandardEgressAllow";
    static public final String T_ADD_STD_EGRESS_DENY     = "testAddStandardEgressDeny";
    static public final String T_ADD_VLAN_INGRESS_ALLOW  = "testAddVLANIngressAllow";
    static public final String T_ADD_VLAN_INGRESS_DENY   = "testAddVLANIngressDeny";
    static public final String T_ADD_VLAN_EGRESS_ALLOW   = "testAddVLANEgressAllow";
    static public final String T_ADD_VLAN_EGRESS_DENY    = "testAddVLANEgressDeny";

    static public final String T_REV_STD_INGRESS_ALLOW   = "testRevokeStandardIngressAllow";
    static public final String T_REV_STD_INGRESS_DENY    = "testRevokeStandardIngressDeny";
    static public final String T_REV_STD_EGRESS_ALLOW    = "testRevokeStandardEgressAllow";
    static public final String T_REV_STD_EGRESS_DENY     = "testRevokeStandardEgressDeny";
    static public final String T_REV_VLAN_INGRESS_ALLOW  = "testRevokeVLANIngressAllow";
    static public final String T_REV_VLAN_INGRESS_DENY   = "testRevokeVLANIngressDeny";
    static public final String T_REV_VLAN_EGRESS_ALLOW   = "testRevokeVLANEgressAllow";
    static public final String T_REV_VLAN_EGRESS_DENY    = "testRevokeVLANEgressDeny";

    static public final String T_CREATE_VLAN_FIREWALL    = "testCreateVLANFirewall";
    static public final String T_DELETE_FIREWALL         = "testDeleteFirewall";
    static public final String T_FIREWALL_CONTENT        = "testFirewallContent";
    static public final String T_GET_FIREWALL            = "testGetFirewall";
    static public final String T_LIST_FIREWALL_RULES     = "testListFirewallRules";


    static private final String[] needsFirewalls = new String[] {
            T_FIREWALL_CONTENT, T_GET_FIREWALL, T_LIST_FIREWALL_RULES,
            T_ADD_STD_EGRESS_ALLOW, T_ADD_STD_EGRESS_DENY, T_ADD_STD_INGRESS_ALLOW, T_ADD_STD_INGRESS_DENY,
            T_REV_STD_EGRESS_ALLOW, T_REV_STD_EGRESS_DENY, T_REV_STD_INGRESS_ALLOW, T_REV_STD_INGRESS_DENY
    };

    static private final String[] needsVlans     = new String[] {
            T_CREATE_VLAN_FIREWALL, T_DELETE_FIREWALL,
            T_ADD_VLAN_EGRESS_ALLOW, T_ADD_VLAN_EGRESS_DENY, T_ADD_VLAN_INGRESS_ALLOW, T_ADD_VLAN_INGRESS_DENY,
            T_REV_VLAN_EGRESS_ALLOW, T_REV_VLAN_EGRESS_DENY, T_REV_VLAN_INGRESS_ALLOW, T_REV_VLAN_INGRESS_DENY
    };

    private String        killFirewallId;
    private CloudProvider provider;
    private Firewall      testFirewall;
    private String        testRuleId;
    private VLAN          testVlan;

    public FirewallTestCase(String name) { super(name); }

    private @Nonnull FirewallSupport getSupport() {
        NetworkServices services = provider.getNetworkServices();

        Assert.assertNotNull("No network services are part of this cloud", services);

        FirewallSupport support = services.getFirewallSupport();

        Assert.assertNotNull("No firewall support is part of this cloud", support);
        return support;
    }

    @Override
    public int getFirewallReuseCount() {
        return needsFirewalls.length;
    }

    @Override
    public int getVlanReuseCount() {
        return needsVlans.length;
    }

    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        begin();
        provider = getProvider();
        provider.connect(getTestContext());
        if( getName().equals(T_FIREWALL_CONTENT) || getName().equals(T_GET_FIREWALL) ) {
            testFirewall = findTestFirewall(provider, getSupport(), true, true, true);
        }
        if( getName().equals(T_CREATE_VLAN_FIREWALL) || getName().equals(T_DELETE_FIREWALL) ) {
            @SuppressWarnings("ConstantConditions") VLANSupport vlanSupport = provider.getNetworkServices().getVlanSupport();

            testVlan = findTestVLAN(provider, vlanSupport, true, true);
            if( testVlan != null ) {
                boolean required = false;

                for( Direction d : Direction.values() ) {
                    for( Permission p : Permission.values() ) {
                        if( getSupport().supportsRules(d, p, true) ) {
                            required = true;
                            break;
                        }
                    }
                }
                if( required ) {
                    Assert.fail("Unable to set up a VLAN for VLAN firewall tests");
                }
            }
        }
        if( getName().equals(T_DELETE_FIREWALL) ) {
            String name = getName() + (System.currentTimeMillis()%10000);

            if( !getSupport().supportsRules(Direction.INGRESS, Permission.ALLOW, false) ) {
                if( !getSupport().supportsRules(Direction.EGRESS, Permission.ALLOW, false) ) {
                    if( !getSupport().supportsRules(Direction.INGRESS, Permission.DENY, false) ) {
                        if( !getSupport().supportsRules(Direction.EGRESS, Permission.DENY, false) ) {
                            killFirewallId = getSupport().createInVLAN(name, name, testVlan.getProviderVlanId());
                        }
                    }
                }
            }
            if( killFirewallId == null ) {
                killFirewallId = getSupport().create(name, name);
            }
        }
        if( getName().equals(T_LIST_FIREWALL_RULES)
                || getName().equals(T_ADD_STD_EGRESS_ALLOW) || getName().equals(T_ADD_STD_EGRESS_DENY) || getName().equals(T_ADD_STD_INGRESS_ALLOW) || getName().equals(T_ADD_STD_INGRESS_DENY)
                || getName().equals(T_REV_STD_EGRESS_ALLOW) || getName().equals(T_REV_STD_EGRESS_DENY) || getName().equals(T_REV_STD_INGRESS_ALLOW) || getName().equals(T_REV_STD_INGRESS_DENY) ) {
            testFirewall = findTestFirewall(provider, getSupport(), false, false, true);
            Assert.assertNotNull("No test firewall was created", testFirewall);
            if( !getName().startsWith("testAdd") ) {
                //noinspection ConstantConditions
                testRuleId = getSupport().authorize(testFirewall.getProviderFirewallId(), "209.98.98.98/32", Protocol.TCP, 80, 80);
            }
        }
        if( getName().equals(T_ADD_VLAN_EGRESS_ALLOW) || getName().equals(T_ADD_VLAN_EGRESS_DENY) || getName().equals(T_ADD_VLAN_INGRESS_ALLOW) || getName().equals(T_ADD_VLAN_INGRESS_DENY)
                || getName().equals(T_REV_VLAN_EGRESS_ALLOW) || getName().equals(T_REV_VLAN_EGRESS_DENY) || getName().equals(T_REV_VLAN_INGRESS_ALLOW) || getName().equals(T_REV_VLAN_INGRESS_DENY) ) {
            @SuppressWarnings("ConstantConditions") VLANSupport vlanSupport = provider.getNetworkServices().getVlanSupport();

            if( testVlan != null ) {
                boolean required = false;

                for( Direction d : Direction.values() ) {
                    for( Permission p : Permission.values() ) {
                        if( getSupport().supportsRules(d, p, true) ) {
                            required = true;
                            break;
                        }
                    }
                }
                if( required ) {
                    Assert.fail("Unable to set up a VLAN for VLAN firewall tests");
                }
            }
            String name = getName() + (System.currentTimeMillis()%10000);

            if( getSupport().supportsRules(Direction.INGRESS, Permission.ALLOW, true) || getSupport().supportsRules(Direction.EGRESS, Permission.ALLOW, true) || getSupport().supportsRules(Direction.INGRESS, Permission.DENY, true) || getSupport().supportsRules(Direction.EGRESS, Permission.DENY, false) ) {
                killFirewallId = getSupport().createInVLAN(name, name, testVlan.getProviderVlanId());
                testFirewall = getSupport().getFirewall(killFirewallId);
            }
            if( !getName().startsWith("testAdd") && testFirewall != null ) {
                testRuleId = getSupport().authorize(testFirewall.getProviderFirewallId(), "209.98.98.98/32", Protocol.TCP, 80, 80);
            }
        }
    }

    @After
    @Override
    public void tearDown() {
        try {
            try {
                if( testFirewall != null && getName().startsWith("testAdd") ) {
                    for( FirewallRule rule : getSupport().getRules(testFirewall.getProviderFirewallId()) ) {
                        getSupport().revoke(rule.getProviderRuleId());
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
            testRuleId = null;
            cleanUp(provider);
            if( killFirewallId != null ) {
                cleanFirewall(getSupport(), killFirewallId);
                killFirewallId = null;
            }
            testFirewall = null;
            testVlan = null;
            APITrace.report(getName());
            APITrace.reset();
            try {
                if( provider != null ) {
                    provider.close();
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        finally {
            end();
        }
    }

    @Test
    public void testMetaData() throws CloudException, InternalException {
        FirewallSupport support = getSupport();
        boolean something = false;

        out("Firewall term:                       " + support.getProviderTermForFirewall(Locale.getDefault()));
        out("Subscribed:                          " + support.isSubscribed());
        out("Supports other firewalls as sources: " + support.supportsFirewallSources());
        for( Direction d : Direction.values() ) {
            for( Permission p : Permission.values() ) {
                boolean s = support.supportsRules(d, p, false);

                if( s ) {
                    something = true;
                }
                out("Supports global " + d + "/" + p + ": " + s);
                s = support.supportsRules(d, p, true);
                if( s ) {
                    something = true;
                }
                out("Supports VLAN " + d + "/" + p + ":   " + s);
            }
        }
        Assert.assertNotNull("Provider term may not be null", support.getProviderTermForFirewall(Locale.getDefault()));
        Assert.assertTrue("No kinds of firewall rules are supported", something);
    }

    @Test
    public void testListFirewalls() throws CloudException, InternalException {
        Iterable<Firewall> firewalls = getSupport().list();
        boolean found = false;

        Assert.assertNotNull("The list of firewalls may not be null", firewalls);
        for( Firewall fw : firewalls ) {
            out("Firewall: " + fw);
            found = true;
        }
        if( !found ) {
            out("WARNING: No firewalls were in the firewall list, so test may not be valid");
        }
    }

    @Test
    public void testListFirewallStatus() throws CloudException, InternalException {
        Iterable<ResourceStatus> firewalls = getSupport().listFirewallStatus();
        boolean found = false;

        Assert.assertNotNull("The list of firewall status may not be null", firewalls);
        for( ResourceStatus fw : firewalls ) {
            out("Firewall status: " + fw);
            found = true;
        }
        if( !found ) {
            out("WARNING: No firewalls were in the firewall status list, so test may not be valid");
        }
    }

    @Test
    public void testFirewallContent() throws CloudException, InternalException {
        out("ID:          " + testFirewall.getProviderFirewallId());
        out("Active:      " + testFirewall.isActive());
        out("Available:   " + testFirewall.isAvailable());
        out("Region ID:   " + testFirewall.getRegionId());
        out("Name:        " + testFirewall.getName());
        out("VLAN ID:     " + testFirewall.getProviderVlanId());
        out("Description: " + testFirewall.getDescription());

        Assert.assertNotNull("Firewall ID may not be null", testFirewall.getProviderFirewallId());
        Assert.assertNotNull("Region ID may not be null", testFirewall.getRegionId());
        Assert.assertNotNull("Name may not be null", testFirewall.getName());
        Assert.assertNotNull("Description may not be null", testFirewall.getDescription());
    }

    @Test
    public void testGetFirewall() throws CloudException, InternalException {
        Firewall fw = getSupport().getFirewall(testFirewall.getProviderFirewallId());

        out("Got: " + fw);
        Assert.assertNotNull("Could not find the test firewall", fw);
    }

    @Test
    public void testGetBogusFirewall() throws CloudException, InternalException {
        Firewall fw = getSupport().getFirewall(UUID.randomUUID().toString());

        out("Got: " + fw);
        Assert.assertNull("Found a firewall matching the bogus ID", fw);
    }

    @Test
    public void testCreateStandardFirewall() throws CloudException, InternalException {
        try {
            killFirewallId = getSupport().create(getName() + (System.currentTimeMillis()%10000), "Test firewall for " + getName());

            out("Created: " + killFirewallId);
            Assert.assertNotNull("The create method must throw an exception or return a firewall ID", killFirewallId);

            Firewall fw = getSupport().getFirewall(killFirewallId);

            Assert.assertNotNull("The newly created firewall doesn't really exist", fw);
        }
        catch( OperationNotSupportedException e ) {
            if( !getSupport().supportsRules(Direction.INGRESS, Permission.ALLOW, false) ) {
                if( !getSupport().supportsRules(Direction.EGRESS, Permission.ALLOW, false) ) {
                    if( !getSupport().supportsRules(Direction.INGRESS, Permission.DENY, false) ) {
                        if( !getSupport().supportsRules(Direction.EGRESS, Permission.DENY, false) ) {
                            out("Got error indicating lack of support for standard firewalls (OK)");
                        }
                    }
                }
            }
            Assert.fail("Support for standard firewalls does not exist even though meta-data claims it is");
        }
    }

    @Test
    public void testCreateVLANFirewall() throws CloudException, InternalException {
        try {
            String vlanId;

            if( testVlan == null ) {
                vlanId = UUID.randomUUID().toString();
            }
            else {
                vlanId = testVlan.getProviderVlanId();
            }
            killFirewallId = getSupport().createInVLAN(getName() + (System.currentTimeMillis()%10000), "Test firewall for " + getName(), vlanId);

            out("Created: " + killFirewallId);
            Assert.assertNotNull("The create method must throw an exception or return a firewall ID", killFirewallId);

            Firewall fw = getSupport().getFirewall(killFirewallId);

            Assert.assertNotNull("The newly created firewall doesn't really exist", fw);
        }
        catch( OperationNotSupportedException e ) {
            if( !getSupport().supportsRules(Direction.INGRESS, Permission.ALLOW, true) ) {
                if( !getSupport().supportsRules(Direction.EGRESS, Permission.ALLOW, true) ) {
                    if( !getSupport().supportsRules(Direction.INGRESS, Permission.DENY, true) ) {
                        if( !getSupport().supportsRules(Direction.EGRESS, Permission.DENY, true) ) {
                            out("Got error indicating lack of support for VLAN firewalls (OK)");
                            return;
                        }
                    }
                }
            }
            Assert.fail("Support for VLAN firewalls does not exist even though meta-data claims it is");
        }
    }

    @Test
    public void testDeleteFirewall() throws CloudException, InternalException {
        getSupport().delete(killFirewallId);
        out("Deleted");
        Firewall fw = getSupport().getFirewall(killFirewallId);

        Assert.assertTrue("Found the test firewall after delete", fw == null || !fw.isActive());
    }

    @Test
    public void testListFirewallRules() throws CloudException, InternalException {
        Iterable<FirewallRule> rules = getSupport().getRules(testFirewall.getProviderFirewallId());
        boolean found = false;

        Assert.assertNotNull("Firewall rules may not be null", rules);
        for( FirewallRule rule : rules ) {
            out("Rule: " + rule);
            found = true;
        }
        Assert.assertTrue("No firewall rule was found even though one is known to exist", found);
    }

    static private int port = 81;

    private void testAddRule(Direction direction, Permission permission) throws CloudException, InternalException {
        if( testFirewall == null ) {
            if( getSupport().supportsRules(direction, permission, getName().contains("VLAN")) ) {
                Assert.fail("No test firewall even though these type of rules are supported");
            }
            else {
                out("Rule type not supported (OK)");
                return;
            }
        }
        int p = port++;

        try {
            RuleTarget sourceEndpoint, destinationEndpoint;
            String firewallId = testFirewall.getProviderFirewallId();

            if( firewallId == null ) {
                Assert.fail("Firewall has no ID");
            }
            if( direction.equals(Direction.INGRESS) ) {
                sourceEndpoint = RuleTarget.getCIDR("209.98.98.98/32");
                destinationEndpoint = RuleTarget.getGlobal(firewallId);
            }
            else {
                destinationEndpoint = RuleTarget.getCIDR("209.98.98.98/32");
                sourceEndpoint = RuleTarget.getGlobal(firewallId);
            }
            String ruleId = getSupport().authorize(firewallId, direction, permission, sourceEndpoint, Protocol.TCP, destinationEndpoint, p, p, 0);

            out("Created rule: " + ruleId);
            for( FirewallRule rule : getSupport().getRules(firewallId) ) {
                if( rule.getProviderRuleId().equals(ruleId) ) {
                    return;
                }
            }
            Assert.fail("Failed to identify new rule in the list of firewall rules");
        }
        catch( OperationNotSupportedException e ) {
            boolean vlan = (testFirewall.getProviderVlanId() != null);

            Assert.assertFalse("Attempt to authorize failed even though support is indicated", getSupport().supportsRules(direction, permission, vlan));
            out("Error indicating lack of support for " + direction + "/" + permission + "/" + vlan + " (OK)");
        }
    }

    @Test
    public void testAddStandardIngressAllow() throws CloudException, InternalException {
        testAddRule(Direction.INGRESS, Permission.ALLOW);
    }

    @Test
    public void testAddStandardIngressDeny() throws CloudException, InternalException {
        testAddRule(Direction.INGRESS, Permission.DENY);
    }

    @Test
    public void testAddStandardEgressAllow() throws CloudException, InternalException {
        testAddRule(Direction.EGRESS, Permission.ALLOW);
    }

    @Test
    public void testAddStandardEgressDeny() throws CloudException, InternalException {
        testAddRule(Direction.EGRESS, Permission.DENY);
    }

    @Test
    public void testAddVLANIngressAllow() throws CloudException, InternalException {
        testAddRule(Direction.INGRESS, Permission.ALLOW);
    }

    @Test
    public void testAddVLANIngressDeny() throws CloudException, InternalException {
        testAddRule(Direction.INGRESS, Permission.DENY);
    }

    @Test
    public void testAddVLANEgressAllow() throws CloudException, InternalException {
        testAddRule(Direction.EGRESS, Permission.ALLOW);
    }

    @Test
    public void testAddVLANEgressDeny() throws CloudException, InternalException {
        testAddRule(Direction.EGRESS, Permission.DENY);
    }

    private void testRemoveRule(Direction direction, Permission permission) throws CloudException, InternalException {
        if( testRuleId == null ) {
            if( getSupport().supportsRules(direction, permission, getName().contains("VLAN")) ) {
                Assert.fail("No test firewall even though these type of rules are supported");
            }
            else {
                out("Rule type not supported (OK)");
                return;
            }
        }
        getSupport().revoke(testRuleId);
        out("Removed rule: " + testRuleId);
        for( FirewallRule rule : getSupport().getRules(testFirewall.getProviderFirewallId()) ) {
            if( rule.getProviderRuleId().equals(testRuleId) ) {
                Assert.fail("Found the test rule among the rules for the firewall post-removal");
            }
        }
    }

    @Test
    public void testRevokeStandardIngressAllow() throws CloudException, InternalException {
        testRemoveRule(Direction.INGRESS, Permission.ALLOW);
    }

    @Test
    public void testRevokeStandardIngressDeny() throws CloudException, InternalException {
        testRemoveRule(Direction.INGRESS, Permission.DENY);
    }

    @Test
    public void testRevokeStandardEgressAllow() throws CloudException, InternalException {
        testRemoveRule(Direction.EGRESS, Permission.ALLOW);
    }

    @Test
    public void testRevokeStandardEgressDeny() throws CloudException, InternalException {
        testRemoveRule(Direction.EGRESS, Permission.DENY);
    }

    @Test
    public void testRevokeVLANIngressAllow() throws CloudException, InternalException {
        testRemoveRule(Direction.INGRESS, Permission.ALLOW);
    }

    @Test
    public void testRevokeVLANIngressDeny() throws CloudException, InternalException {
        testRemoveRule(Direction.INGRESS, Permission.DENY);
    }

    @Test
    public void testRevokeVLANEgressAllow() throws CloudException, InternalException {
        testRemoveRule(Direction.EGRESS, Permission.ALLOW);
    }

    @Test
    public void testRevokeVLANEgressDeny() throws CloudException, InternalException {
        testRemoveRule(Direction.EGRESS, Permission.DENY);
    }
}
