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
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallConstraints;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.RuleTargetType;
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
 * [Class Documentation]
 * <p>Created by George Reese: 2/22/13 10:43 AM</p>
 *
 * @author George Reese
 */
public class StatelessFirewallTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessFirewallTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testGeneralFirewallId;
    private String testVLANFirewallId;

    public StatelessFirewallTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testGeneralFirewallId = tm.getTestGeneralFirewallId(DaseinTestManager.STATELESS, false);
        testVLANFirewallId = tm.getTestVLANFirewallId(DaseinTestManager.STATELESS, false, null);
    }

    @After
    public void after() {
        tm.end();
    }

    private void assertFirewall(@Nonnull Firewall firewall, boolean vlan) throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();
        FirewallSupport support = null;
        if( services != null ) {
            support = services.getFirewallSupport();
        }
        assertNotNull("The firewall ID may not be null", firewall.getProviderFirewallId());
        assertNotNull("The firewall name may not be null", firewall.getName());
        assertNotNull("The firewall description may not be null", firewall.getDescription());
        assertNotNull("The firewall region may not be null", firewall.getRegionId());
        if( vlan ) {
            assertNotNull("The firewall VLAN may not be null", firewall.getProviderVlanId());
        }
        else if (support != null && support.getCapabilities().requiresVLAN().equals(Requirement.NONE)){
            assertNull("The firewall VLAN must be null", firewall.getProviderVlanId());
        }
        assertNotNull("The firewall tags may not be null", firewall.getTags());
        assertEquals("The firewall is in the wrong region", tm.getContext().getRegionId(), firewall.getRegionId());
        assertNotNull("The firewall rules may not be null", firewall.getRules());
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

    private void content(@Nonnull String id, @Nonnull Firewall firewall, boolean vlan) throws CloudException, InternalException {
        tm.out("Firewall ID", firewall.getProviderFirewallId());
        tm.out("Active", firewall.isActive());
        tm.out("Available", firewall.isAvailable());
        tm.out("Name", firewall.getName());
        tm.out("Region ID", firewall.getRegionId());
        tm.out("VLAN ID", firewall.getProviderVlanId());
        tm.out("Subnets", Arrays.toString(firewall.getSubnetAssociations()));
        tm.out("Rules", Arrays.toString(firewall.getRules().toArray()));
        Map<String,String> tags = firewall.getTags();

        //noinspection ConstantConditions
        if( tags != null ) {
            for( Map.Entry<String,String> entry : tags.entrySet() ) {
                tm.out("Tag " + entry.getKey(), entry.getValue());
            }
        }
        tm.out("Description", firewall.getDescription());

        assertFirewall(firewall, vlan);
        assertEquals("The requested firewall ID does not match the actual firewall ID", id, firewall.getProviderFirewallId());

    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                tm.out("Subscribed", support.isSubscribed());
                tm.out("Term for Firewall", support.getCapabilities().getProviderTermForFirewall(Locale.getDefault()));
                tm.out("Supports Firewall Creation (General)", support.getCapabilities().supportsFirewallCreation(false));
                tm.out("Supports Firewall Creation (VLAN)", support.getCapabilities().supportsFirewallCreation(true));
                boolean general = false;
                boolean vlan = false;

                for( Direction direction : Direction.values() ) {
                    for( Permission permission : Permission.values() ) {
                        boolean b = support.getCapabilities().supportsRules(direction, permission, false);

                        if( b ) {
                            general = true;
                        }
                        tm.out("Supports " + direction + "/" + permission + " (General)", b);
                        b = support.getCapabilities().supportsRules(direction, permission, true);
                        if( b ) {
                            vlan = true;
                        }
                        tm.out("Supports " + direction + "/" + permission + " (VLAN)", b);
                    }
                }
                tm.out("Rule Precedence Req (General)", support.getCapabilities().identifyPrecedenceRequirement(false));
                tm.out("Rule Precedence Req (VLAN)", support.getCapabilities().identifyPrecedenceRequirement(true));
                tm.out("Zero Highest Precedence", support.getCapabilities().isZeroPrecedenceHighest());
                tm.out("Supported Directions (General)", support.getCapabilities().listSupportedDirections(false));
                tm.out("Supported Directions (VLAN)", support.getCapabilities().listSupportedDirections(true));
                tm.out("Supported Permissions (General)", support.getCapabilities().listSupportedPermissions(false));
                tm.out("Supported Permissions (VLAN)", support.getCapabilities().listSupportedPermissions(true));
                tm.out("Supported Source Types (General)", support.getCapabilities().listSupportedSourceTypes(false));
                tm.out("Supported Source Types (VLAN)", support.getCapabilities().listSupportedSourceTypes(true));
                tm.out("Supported Destination Types (General)", support.getCapabilities().listSupportedDestinationTypes(false));
                tm.out("Supported Destination Types (VLAN)", support.getCapabilities().listSupportedDestinationTypes(true));

                FirewallConstraints constraints = support.getCapabilities().getFirewallConstraintsForCloud();

                assertNotNull("Firewall constraints may not be null", constraints);

                Iterable<FirewallConstraints.Constraint> cfields = constraints.getConstraints();

                tm.out("Constrained fields", cfields);
                assertNotNull("Firewall constraints may not define empty files (may be an empty list)", cfields);

                for( FirewallConstraints.Constraint c : FirewallConstraints.Constraint.values() ) {
                    FirewallConstraints.Level l = constraints.getConstraintLevel(c);

                    tm.out("Constraint " + c.name(), l);
                    assertNotNull("Constraint level may not be null, but it was for " + c, l);
                }
                if( !general ) {
                    assertFalse("General firewalls are not supported, so it makes no sense that you can create them", support.getCapabilities().supportsFirewallCreation(false));
                }
                if( !vlan ) {
                    assertFalse("VLAN firewalls are not supported, so it makes no sense that you can create them", support.getCapabilities().supportsFirewallCreation(true));
                }
                assertNotNull("The provider term for firewall may not be null for any locale", Locale.getDefault());
                assertNotNull("Requirement for precedence in general firewall rules may not be null", support.getCapabilities().identifyPrecedenceRequirement(false));
                assertNotNull("Requirement for precedence in VLAN firewall rules may not be null", support.getCapabilities().identifyPrecedenceRequirement(true));

                Iterable<RuleTargetType> types = support.getCapabilities().listSupportedSourceTypes(false);

                assertNotNull("Supported source types for general firewall rules may not be null", types);
                if( general ) {
                    assertTrue("There must be at least one source type for general firewall rules", types.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for general firewall rules, so no source types should exist", types.iterator().hasNext());
                }

                types = support.getCapabilities().listSupportedSourceTypes(true);
                assertNotNull("Supported source types for VLAN firewall rules may not be null", types);
                if( vlan ) {
                    assertTrue("There must be at least one source type for VLAN firewall rules", types.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for VLAN firewall rules, so no source types should exist", types.iterator().hasNext());
                }

                types = support.getCapabilities().listSupportedDestinationTypes(false);
                assertNotNull("Supported destination types for general firewall rules may not be null", types);
                if( general ) {
                    assertTrue("There must be at least one destination type for general firewall rules", types.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for general firewall rules, so no destination types should exist", types.iterator().hasNext());
                }

                types = support.getCapabilities().listSupportedDestinationTypes(true);
                assertNotNull("Supported destination types for VLAN firewall rules may not be null", types);
                if( vlan ) {
                    assertTrue("There must be at least one destination type for VLAN firewall rules", types.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for VLAN firewall rules, so no destination types should exist", types.iterator().hasNext());
                }

                Iterable<Direction> directions = support.getCapabilities().listSupportedDirections(false);

                assertNotNull("Supported directions for general firewall rules may not be null", directions);
                if( general ) {
                    assertTrue("There must be at least one direction available for general firewall rules", directions.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for general firewall rules, so no directions should be enumerated", directions.iterator().hasNext());
                }

                directions = support.getCapabilities().listSupportedDirections(true);
                assertNotNull("Supported directions for VLAN firewall rules may not be null", directions);
                if( vlan ) {
                    assertTrue("There must be at least one direction available for VLAN firewall rules", directions.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for VLAN firewall rules, so no directions should be enumerated", directions.iterator().hasNext());
                }

                Iterable<Permission> permissions = support.getCapabilities().listSupportedPermissions(false);

                assertNotNull("Supported permissions for general firewall rules may not be null", permissions);
                if( general ) {
                    assertTrue("There must be at least one permission available for general firewall rules", permissions.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for general firewall rules, so no permissions should be enumerated", permissions.iterator().hasNext());
                }

                permissions = support.getCapabilities().listSupportedPermissions(true);
                assertNotNull("Supported permissions for VLAN firewall rules may not be null", permissions);
                if( vlan ) {
                    assertTrue("There must be at least one permission available for VLAN firewall rules", permissions.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for VLAN firewall rules, so no permissions should be enumerated", permissions.iterator().hasNext());
                }

            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void getBogusFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                Firewall fw = support.getFirewall(UUID.randomUUID().toString());

                tm.out("Bogus Firewall", fw);
                assertNull("Found a firewall for a bogus ID when none should have been found", fw);
            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void getGeneralFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                if( testGeneralFirewallId != null ) {
                    Firewall fw = support.getFirewall(testGeneralFirewallId);

                    tm.out("General Firewall", fw);
                    assertNotNull("Unable to find the test firewall", fw);
                }
                else {
                    if( support.getCapabilities().listSupportedDirections(false).iterator().hasNext() ) {
                        fail("No test firewall has been established, but " + tm.getProvider().getCloudName() + " supports general firewalls");
                    }
                    else {
                        tm.ok("No general firewalls in " + tm.getProvider().getCloudName());
                    }
                }
            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void vlanFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                if( testVLANFirewallId != null ) {
                    Firewall fw = support.getFirewall(testVLANFirewallId);

                    tm.out("VLAN Firewall", fw);
                    assertNotNull("Unable to find the test firewall", fw);
                }
                else {
                    if( support.getCapabilities().listSupportedDirections(true).iterator().hasNext() ) {
                        fail("No test firewall has been established, but " + tm.getProvider().getCloudName() + " supports VLAN firewalls");
                    }
                    else {
                        tm.ok("No VLAN firewalls in " + tm.getProvider().getCloudName());
                    }
                }
            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void generalFirewallContent() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                boolean inVlan = support.getCapabilities().requiresVLAN().equals(Requirement.REQUIRED);
                if( testGeneralFirewallId != null && !inVlan) {
                    Firewall fw = support.getFirewall(testGeneralFirewallId);

                    assertNotNull("Unable to find the test firewall", fw);
                    content(testGeneralFirewallId, fw, inVlan);
                }
                else {
                    if( support.getCapabilities().listSupportedDirections(false).iterator().hasNext() ) {
                        fail("No test firewall has been established, but " + tm.getProvider().getCloudName() + " supports general firewalls");
                    }
                    else {
                        tm.ok("No general firewalls in " + tm.getProvider().getCloudName());
                    }
                }
            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void firewallConstraints() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                if( testGeneralFirewallId != null ) {
                    Firewall fw = support.getFirewall(testGeneralFirewallId);

                    assertNotNull("Unable to find the test firewall", fw);
                    Map<FirewallConstraints.Constraint,Object> constraints = support.getActiveConstraintsForFirewall(testGeneralFirewallId);

                    tm.out("Firewall constraints (" + testGeneralFirewallId + "): ", constraints);
                    assertNotNull("Unable to load firewall constraints for " + testGeneralFirewallId, constraints);

                }
                else {
                    if( support.getCapabilities().listSupportedDirections(false).iterator().hasNext() ) {
                        fail("No test firewall has been established, but " + tm.getProvider().getCloudName() + " supports general firewalls");
                    }
                    else {
                        tm.ok("No general firewalls in " + tm.getProvider().getCloudName());
                    }
                }
            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void vlanFirewallContent() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                if( testVLANFirewallId != null ) {
                    Firewall fw = support.getFirewall(testVLANFirewallId);

                    assertNotNull("Unable to find the test firewall", fw);
                    content(testVLANFirewallId, fw, true);
                }
                else {
                    if( support.getCapabilities().listSupportedDirections(true).iterator().hasNext() ) {
                        fail("No test firewall has been established, but " + tm.getProvider().getCloudName() + " supports VLAN firewalls");
                    }
                    else {
                        tm.ok("No VLAN firewalls in " + tm.getProvider().getCloudName());
                    }
                }
            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void listFirewalls() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                Iterable<Firewall> firewalls = support.list();
                int count = 0;

                assertNotNull("The list of firewalls may be empty, but it may never be null", firewalls);
                for( Firewall fw : firewalls ) {
                    count++;
                    tm.out("Firewall", fw);
                }
                tm.out("Total Firewall Count", count);
                if( support.isSubscribed() && count == 0 ) {
                    tm.warn("This test is likely invalid as no firewalls were provided in the results for validation");
                }
                if( count > 0 ) {
                    for( Firewall fw : firewalls ) {
                        assertFirewall(fw, fw.getProviderVlanId() != null);
                    }
                }
            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void listFirewallStatus() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                Iterable<ResourceStatus> firewalls = support.listFirewallStatus();
                int count = 0;

                assertNotNull("The list of firewall status may be empty, but it may never be null", firewalls);
                for( ResourceStatus fw : firewalls ) {
                    count++;
                    tm.out("Firewall Status", fw);
                }
                tm.out("Total Firewall Status Count", count);
                if( support.isSubscribed() && count == 0 ) {
                    tm.warn("This test is likely invalid as no firewall status items were provided in the results for validation");
                }
            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void compareFirewallListAndStatus() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
                Iterable<Firewall> firewalls = support.list();
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

                    assertTrue("Status and firewall lists do not match for " + entry.getKey(), s != null && f != null && s && f);
                }
                tm.out("Matches");
            }
            else {
                tm.ok("No firewall support in this cloud");
            }
        }
        else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void listRulesForGeneralFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                if( testGeneralFirewallId != null ) {
                    Iterable<FirewallRule> rules = support.getRules(testGeneralFirewallId);
                    int count = 0;

                    assertNotNull("The rules associated with a firewall may be empty, but they may not be null", rules);
                    for( FirewallRule rule : rules ) {
                        count++;
                        tm.out("Rule for " + testGeneralFirewallId, rule);
                    }
                    if( count < 1 ) {
                        tm.warn("No rules were associated with this firewall, so the test may be invalid");
                    }
                    else if( count > 0 ) {
                        for( FirewallRule rule : rules ) {
                            assertRule(testGeneralFirewallId, rule);
                        }
                    }
                }
                else {
                    if( support.getCapabilities().listSupportedDirections(false).iterator().hasNext() ) {
                        fail("No test firewall has been established, but " + tm.getProvider().getCloudName() + " supports general firewalls");
                    }
                    else {
                        tm.ok("No general firewalls in " + tm.getProvider().getCloudName());
                    }
                }
            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void listRulesForVLANFirewall() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                if( testVLANFirewallId != null ) {
                    Iterable<FirewallRule> rules = support.getRules(testVLANFirewallId);
                    int count = 0;

                    assertNotNull("The rules associated with a firewall may be empty, but they may not be null", rules);
                    for( FirewallRule rule : rules ) {
                        count++;
                        tm.out("Rule for " + testVLANFirewallId, rule);
                    }
                    if( count < 1 ) {
                        tm.warn("No rules were associated with this firewall, so the test may be invalid");
                    }
                    else if( count > 0 ) {
                        for( FirewallRule rule : rules ) {
                            assertRule(testVLANFirewallId, rule);
                        }
                    }
                }
                else {
                    if( support.getCapabilities().listSupportedDirections(true).iterator().hasNext() ) {
                        fail("No test firewall has been established, but " + tm.getProvider().getCloudName() + " supports VLAN firewalls");
                    }
                    else {
                        tm.ok("No VLAN firewalls in " + tm.getProvider().getCloudName());
                    }
                }
            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void ruleContent() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                for( Firewall fw : support.list() ) {
                    String id = fw.getProviderFirewallId();

                    if( id != null ) {
                        Iterator<FirewallRule> rules = support.getRules(id).iterator();

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
                else if( support.getCapabilities().listSupportedDirections(false).iterator().hasNext() || support.getCapabilities().listSupportedDirections(true).iterator().hasNext() ) {
                    tm.warn("No firewall rules exist in this cloud currently, so this test may not be valid");
                }
                else {
                    tm.ok("This cloud does not support any kind of firewall rules in this region");
                }
            }
            else {
                tm.ok("No firewall support in " + tm.getProvider().getCloudName());
            }
        }
        else {
            tm.ok("No network services in " + tm.getProvider().getCloudName());
        }
    }
}
