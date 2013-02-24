package org.dasein.cloud.test.network;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.network.Direction;
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

import java.util.Locale;

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

    public StatelessFirewallTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            if( support != null ) {
                tm.out("Subscribed", support.isSubscribed());
                tm.out("Term for Firewall", support.getProviderTermForFirewall(Locale.getDefault()));
                tm.out("Supports Firewall Creation (General)", support.supportsFirewallCreation(false));
                tm.out("Supports Firewall Creation (VLAN)", support.supportsFirewallCreation(true));
                boolean general = false;
                boolean vlan = false;

                for( Direction direction : Direction.values() ) {
                    for( Permission permission : Permission.values() ) {
                        boolean b = support.supportsRules(direction, permission, false);

                        if( b ) {
                            general = true;
                        }
                        tm.out("Supports " + direction + "/" + permission + " (General)", b);
                        b = support.supportsRules(direction, permission, true);
                        if( b ) {
                            vlan = true;
                        }
                        tm.out("Supports " + direction + "/" + permission + " (VLAN)", b);
                    }
                }
                tm.out("Rule Precedence Req (General)", support.identifyPrecedenceRequirement(false));
                tm.out("Rule Precedence Req (VLAN)", support.identifyPrecedenceRequirement(true));
                tm.out("Zero Highest Precedence", support.isZeroPrecedenceHighest());
                tm.out("Supported Directions (General)", support.listSupportedDirections(false));
                tm.out("Supported Directions (VLAN)", support.listSupportedDirections(true));
                tm.out("Supported Permissions (General)", support.listSupportedPermissions(false));
                tm.out("Supported Permissions (VLAN)", support.listSupportedPermissions(true));
                tm.out("Supported Source Types (General)", support.listSupportedSourceTypes(false));
                tm.out("Supported Source Types (VLAN)", support.listSupportedSourceTypes(true));
                tm.out("Supported Destination Types (General)", support.listSupportedDestinationTypes(false));
                tm.out("Supported Destination Types (VLAN)", support.listSupportedDestinationTypes(true));

                if( !general ) {
                    assertFalse("General firewalls are not supported, so it makes no sense that you can create them", support.supportsFirewallCreation(false));
                }
                if( !vlan ) {
                    assertFalse("VLAN firewalls are not supported, so it makes no sense that you can create them", support.supportsFirewallCreation(true));
                }
                assertNotNull("The provider term for firewall may not be null for any locale", Locale.getDefault());
                assertNotNull("Requirement for precedence in general firewall rules may not be null", support.identifyPrecedenceRequirement(false));
                assertNotNull("Requirement for precedence in VLAN firewall rules may not be null", support.identifyPrecedenceRequirement(true));

                Iterable<RuleTargetType> types = support.listSupportedSourceTypes(false);

                assertNotNull("Supported source types for general firewall rules may not be null", types);
                if( general ) {
                    assertTrue("There must be at least one source type for general firewall rules", types.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for general firewall rules, so no source types should exist", types.iterator().hasNext());
                }

                types = support.listSupportedSourceTypes(true);
                assertNotNull("Supported source types for VLAN firewall rules may not be null", types);
                if( vlan ) {
                    assertTrue("There must be at least one source type for VLAN firewall rules", types.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for VLAN firewall rules, so no source types should exist", types.iterator().hasNext());
                }

                types = support.listSupportedDestinationTypes(false);
                assertNotNull("Supported destination types for general firewall rules may not be null", types);
                if( general ) {
                    assertTrue("There must be at least one destination type for general firewall rules", types.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for general firewall rules, so no destination types should exist", types.iterator().hasNext());
                }

                types = support.listSupportedDestinationTypes(true);
                assertNotNull("Supported destination types for VLAN firewall rules may not be null", types);
                if( vlan ) {
                    assertTrue("There must be at least one destination type for VLAN firewall rules", types.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for VLAN firewall rules, so no destination types should exist", types.iterator().hasNext());
                }

                Iterable<Direction> directions = support.listSupportedDirections(false);

                assertNotNull("Supported directions for general firewall rules may not be null", directions);
                if( general ) {
                    assertTrue("There must be at least one direction available for general firewall rules", directions.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for general firewall rules, so no directions should be enumerated", directions.iterator().hasNext());
                }

                directions = support.listSupportedDirections(true);
                assertNotNull("Supported directions for VLAN firewall rules may not be null", directions);
                if( general ) {
                    assertTrue("There must be at least one direction available for VLAN firewall rules", directions.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for VLAN firewall rules, so no directions should be enumerated", directions.iterator().hasNext());
                }

                Iterable<Permission> permissions = support.listSupportedPermissions(false);

                assertNotNull("Supported permissions for general firewall rules may not be null", permissions);
                if( general ) {
                    assertTrue("There must be at least one permission available for general firewall rules", permissions.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for general firewall rules, so no permissions should be enumerated", permissions.iterator().hasNext());
                }

                permissions = support.listSupportedPermissions(true);
                assertNotNull("Supported permissions for VLAN firewall rules may not be null", permissions);
                if( general ) {
                    assertTrue("There must be at least one permission available for VLAN firewall rules", permissions.iterator().hasNext());
                }
                else {
                    assertFalse("There is no support for VLAN firewall rules, so no permissions should be enumerated", permissions.iterator().hasNext());
                }

            }
            else {
                tm.ok("No firewall support in this cloud");
            }
        }
        else {
            tm.ok("No network services in this cloud");
        }
    }
}
