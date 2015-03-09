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
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.NetworkFirewallSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.RuleTargetType;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests to validate compatibility with stateless Dasein Cloud operations for network firewalls/ACLs.
 * <p>Created by George Reese: 2/27/13 9:59 AM</p>
 * @author George Reese
 * @version 2013.04
 * @since 2013.04
 */
public class StatelessNetworkFirewallTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessNetworkFirewallTests.class);
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

    public StatelessNetworkFirewallTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testFirewallId = tm.getTestNetworkFirewallId(DaseinTestManager.STATELESS, false, null);
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
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
        boolean subscribed = support.isSubscribed();

        tm.out("Subscribed", subscribed);
        tm.out("Term for Network Firewall", support.getCapabilities().getProviderTermForNetworkFirewall(Locale.getDefault()));
        tm.out("Precedence Requirement", support.getCapabilities().identifyPrecedenceRequirement());
        tm.out("Zero Precedence Highest", support.getCapabilities().isZeroPrecedenceHighest());
        tm.out("Supports Network Firewall Creation", support.getCapabilities().supportsNetworkFirewallCreation());

        Iterable<RuleTargetType> sources = support.getCapabilities().listSupportedSourceTypes();

        tm.out("Source Types", sources);

        Iterable<RuleTargetType> destinations = support.getCapabilities().listSupportedDestinationTypes();

        tm.out("Destination Types", destinations);

        Iterable<Direction> directions = support.getCapabilities().listSupportedDirections();

        tm.out("Directions", directions);

        Iterable<Permission> permissions = support.getCapabilities().listSupportedPermissions();

        tm.out("Permissions", permissions);

        assertNotNull("The provider term for a network firewall may not be null", support.getCapabilities().getProviderTermForNetworkFirewall(Locale.getDefault()));
        assertNotNull("Precedence requirement may not be null", support.getCapabilities().identifyPrecedenceRequirement());
        assertNotNull("List of source types may not be null", sources);
        assertNotNull("List of destination types may not be null", destinations);
        assertNotNull("List of supported directions may not be null", directions);
        assertNotNull("List of supported permissions may not be null", permissions);
        if( subscribed ) {
            assertTrue("There must be at least one source type", sources.iterator().hasNext());
            assertTrue("There must be at least one destination type", destinations.iterator().hasNext());
            assertTrue("There must be at least one supported direction", directions.iterator().hasNext());
            assertTrue("There must be at least one supported permission", permissions.iterator().hasNext());
        }
    }

    @Test
    public void getBogusNetworkFirewall() throws CloudException, InternalException {
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
        Firewall fw = support.getFirewall(UUID.randomUUID().toString());

        tm.out("Bogus Network Firewall", fw);
        assertNull("Found a network firewall for a bogus ID when none should have been found", fw);
    }

    @Test
    public void getNetworkFirewall() throws CloudException, InternalException {
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
            Firewall fw = support.getFirewall(testFirewallId);

            tm.out("Network Firewall", fw);
            assertNotNull("No network firewall was found for the test firewall ID " + testFirewallId, fw);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Test was not run because this account is not subscribed for network firewalls");
            }
            else {
                fail("No test network firewall exists for the test " + name.getMethodName());
            }
        }
    }

    private void assertFirewall(@Nonnull Firewall firewall) {
        assertNotNull("The firewall ID may not be null", firewall.getProviderFirewallId());
        assertNotNull("The firewall name may not be null", firewall.getName());
        assertNotNull("The firewall description may not be null", firewall.getDescription());
        assertNotNull("The firewall region may not be null", firewall.getRegionId());
        assertNotNull("The firewall VLAN may not be null", firewall.getProviderVlanId());
        assertNotNull("The firewall tags may not be null", firewall.getTags());
        assertEquals("The firewall is in the wrong region", tm.getContext().getRegionId(), firewall.getRegionId());
    }

    @Test
    public void networkFirewallContent() throws CloudException, InternalException {
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

            assertNotNull("Unable to find the test firewall", firewall);
            tm.out("Firewall ID", firewall.getProviderFirewallId());
            tm.out("Active", firewall.isActive());
            tm.out("Available", firewall.isAvailable());
            tm.out("Name", firewall.getName());
            tm.out("Region ID", firewall.getRegionId());
            tm.out("VLAN ID", firewall.getProviderVlanId());
            tm.out("Subnets", Arrays.toString(firewall.getSubnetAssociations()));
            Map<String,String> tags = firewall.getTags();

            //noinspection ConstantConditions
            if( tags != null ) {
                for( Map.Entry<String,String> entry : tags.entrySet() ) {
                    tm.out("Tag " + entry.getKey(), entry.getValue());
                }
            }
            tm.out("Description", firewall.getDescription());
            assertFirewall(firewall);
        }
        else {
            if( support.isSubscribed() ) {
                fail("No test firewall has been established, but " + tm.getProvider().getCloudName() + " supports network firewalls");
            }
            else {
                tm.ok("No network firewalls in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
        }
    }

    @Test
    public void listFirewalls() throws CloudException, InternalException {
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
        Iterable<Firewall> firewalls = support.listFirewalls();
        int count = 0;

        assertNotNull("The list of firewalls may be empty, but it may never be null", firewalls);
        for( Firewall fw : firewalls ) {
            count++;
            tm.out("Network Firewall", fw);
        }
        tm.out("Total Network Firewall Count", count);
        if( !support.isSubscribed() ) {
            assertEquals("The network firewall count should be zero since this account is not subscribed to this service", 0, count);
        }
        else if( count == 0 ) {
            tm.warn("This test is likely invalid as no network firewalls were provided in the results for validation");
        }
        if( count > 0 ) {
            for( Firewall fw : firewalls ) {
                assertFirewall(fw);
            }
        }
    }

    @Test
    public void listFirewallStatus() throws CloudException, InternalException {
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
        Iterable<ResourceStatus> firewalls = support.listFirewallStatus();
        int count = 0;

        assertNotNull("The list of network firewall status may be empty, but it may never be null", firewalls);
        for( ResourceStatus fw : firewalls ) {
            count++;
            tm.out("Network Firewall Status", fw);
        }
        tm.out("Total Network Firewall Status Count", count);
        if( !support.isSubscribed() ) {
            assertEquals("The network firewall status count should be zero since this account is not subscribed to this service", 0, count);
        }
        else if( count == 0 ) {
            tm.warn("This test is likely invalid as no network firewall status items were provided in the results for validation");
        }
    }

    @Test
    public void compareFirewallListAndStatus() throws CloudException, InternalException {
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
        HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
        Iterable<Firewall> firewalls = support.listFirewalls();
        Iterable<ResourceStatus> status = support.listFirewallStatus();

        assertNotNull("listFirewalls() must return at least an empty collections and may not be null", firewalls);
        assertNotNull("listFirewallStatus() must return at least an empty collection and may not be null", status);
        for( ResourceStatus s : status ) {
            Map<String,Boolean> current = map.get(s.getProviderResourceId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(s.getProviderResourceId(), current);
            }
            current.put("status", true);
        }
        for( Firewall fw : firewalls ) {
            Map<String,Boolean> current = map.get(fw.getProviderFirewallId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(fw.getProviderFirewallId(), current);
            }
            current.put("firewall", true);
        }
        for( Map.Entry<String,Map<String,Boolean>> entry : map.entrySet() ) {
            Boolean s = entry.getValue().get("status");
            Boolean f = entry.getValue().get("firewall");

            assertTrue("Status and network firewall lists do not match for " + entry.getKey(), s != null && f != null && s && f);
        }
        tm.out("Matches");
    }

    private void assertRule(@Nonnull String fwId, @Nonnull FirewallRule rule) {
        assertNotNull("The firewall rule ID may not be null", rule.getProviderRuleId());
        assertNotNull("The firewall ID may not be null", rule.getFirewallId());
        assertEquals("The firewall ID for the rule should match the firewall", fwId, rule.getFirewallId());
        assertTrue("Precedence must be non-negative", rule.getPrecedence() >= 0);
        assertNotNull("The firewall rule direction must be non-null", rule.getDirection());
        assertNotNull("The firewall permission must not be null", rule.getPermission());
        assertNotNull("The firewall source must not be null", rule.getSourceEndpoint());
        assertNotNull("The firewall destination must not be null", rule.getDestinationEndpoint());
        assertNotNull("The firewall protocol must not be null", rule.getProtocol());
    }

    @Test
    public void listRules() throws CloudException, InternalException {
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
            Iterable<FirewallRule> rules = support.listRules(testFirewallId);
            int count = 0;

            assertNotNull("The rules associated with a network firewall may be empty, but they may not be null", rules);
            for( FirewallRule rule : rules ) {
                count++;
                tm.out("Rule for " + testFirewallId, rule);
            }
            if( count < 1 ) {
                tm.warn("No rules were associated with this network firewall, so the test may be invalid");
            }
            else if( count > 0 ) {
                for( FirewallRule rule : rules ) {
                    assertRule(testFirewallId, rule);
                }
            }
        }
        else {
            if( support.isSubscribed() ) {
                fail("No test network firewall has been established, but " + tm.getProvider().getCloudName() + " supports VLAN firewalls");
            }
            else {
                tm.ok("No network firewalls in " + tm.getProvider().getCloudName());
            }
        }
    }

    @Test
    public void ruleContent() throws CloudException, InternalException {
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
        for( Firewall fw : support.listFirewalls() ) {
            String id = fw.getProviderFirewallId();

            if( id != null ) {
                Iterator<FirewallRule> rules = support.listRules(id).iterator();

                if( rules.hasNext() ) {
                    FirewallRule rule = rules.next();

                    tm.out("Rule ID", rule.getProviderRuleId());
                    tm.out("Firewall ID", rule.getFirewallId());
                    tm.out("Precedence", rule.getPrecedence());
                    tm.out("Permission", rule.getPermission());
                    tm.out("Direction", rule.getDirection());
                    tm.out("Source", rule.getSourceEndpoint());
                    tm.out("Protocol", rule.getProtocol());
                    tm.out("Destination", rule.getDestinationEndpoint());
                    tm.out("Start Port", rule.getStartPort());
                    tm.out("End Port", rule.getEndPort());

                    assertRule(id, rule);
                    return;
                }
            }
        }
        if( !support.isSubscribed() ) {
            tm.ok("This account is not subscribed for firewalls");
        }
        else {
            tm.warn("No firewall rules exist in this cloud currently, so this test may not be valid");
        }
    }
}
