package org.dasein.cloud.test.network;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
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
    }

    @After
    public void after() {
        tm.end();
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
                            String id = resources.provisionSubnet(support, "provision", testVLANId, "dsnsub", vlan.getProviderDataCenterId());
                            tm.out("New Subnet", id);
                        }
                        else {
                            try {
                                resources.provisionSubnet(support, "provision", testVLANId, "dsnsubfail", null);
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
                        String id = resources.provisionVLAN(support, "provision", "dnsvlan", null);

                        tm.out("New VLAN", id);
                    }
                    else {
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
}
