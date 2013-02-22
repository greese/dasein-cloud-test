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

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.SubnetCreateOptions;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.util.APITrace;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;

@SuppressWarnings("JUnit4AnnotatedMethodInJUnit3TestCase")
public class VLANTestCase extends BaseTestCase {
    private CloudProvider cloud          = null;
    private String        subnetToRemove = null;
    private String        testSubnet     = null;
    private String        testVlan       = null;
    private String        vlanToRemove   = null;

    private boolean subnetsSupported;
    private boolean vlansSupported;

    public VLANTestCase(String name) { super(name); }

    protected @Nonnull VLANSupport getSupport() {
        NetworkServices services = cloud.getNetworkServices();

        Assert.assertNotNull("Network services must be configured for this test to work", services);

        VLANSupport support = services.getVlanSupport();

        Assert.assertNotNull("VLAN support must be configured for this test to work", support);

        return support;
    }

    @Before
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        begin();

        cloud = getProvider();
        cloud.connect(getTestContext());
        NetworkServices services = cloud.getNetworkServices();

        if( services == null ) {
            return;
        }
        VLANSupport support = services.getVlanSupport();

        if( support == null ) {
            return;
        }
        vlansSupported = support.isSubscribed();
        if( vlansSupported ) {
            subnetsSupported = !support.getSubnetSupport().equals(Requirement.NONE);
        }
        String name = getName();

        cloud = getProvider();
        cloud.connect(getTestContext());

        if( name.equals("testVlanContent") || (name.equals("testProvisionSubnet") && support.allowsNewSubnetCreation()) ) {
            VLAN v = findTestVLAN(cloud, support, true, true);

            if( v != null ) {
                testVlan = v.getProviderVlanId();
            }
        }
        else if( name.equals("testRemoveVlan") && support.allowsNewVlanCreation() ) {
            VLAN v = findTestVLAN(cloud, support, false, true);

            if( v != null ) {
                testVlan = v.getProviderVlanId();
                vlanToRemove = testVlan;
            }
        }
        else if( name.equals("testSubnetContent") || name.equals("testListSubnets") ) {
            if( !support.getSubnetSupport().equals(Requirement.NONE)  ) {
                for( VLAN vlan : support.listVlans() ) {
                    for( Subnet subnet : support.listSubnets(vlan.getProviderVlanId()) ) {
                        testSubnet = subnet.getProviderSubnetId();
                        testVlan = vlan.getProviderVlanId();
                    }
                    if( testSubnet != null ) {
                        break;
                    }
                }
                if( testVlan == null && support.allowsNewVlanCreation() ) {
                    vlanToRemove = support.createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[]{"192.168.1.1"}, new String[]{"192.168.1.1"}).getProviderVlanId();
                    testVlan = vlanToRemove;
                }
                if( testVlan != null ) {
                    for( Subnet subnet : support.listSubnets(testVlan) ) {
                        testSubnet = subnet.getProviderSubnetId();
                    }
                    if( testSubnet == null ) {
                        if( name.equals("testSubnetContent") && support.allowsNewSubnetCreation() ) {
                            SubnetCreateOptions options = SubnetCreateOptions.getInstance(testVlan, "10.0.1.0/24", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test");

                            subnetToRemove = support.createSubnet(options).getProviderSubnetId();
                            testSubnet = subnetToRemove;
                        }
                    }
                }
            }
        }
        else if( name.equals("testRemoveSubnet") && vlansSupported && support.allowsNewVlanCreation() && support.allowsNewSubnetCreation() ) {
            vlanToRemove = support.createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[]{"192.168.1.1"}, new String[]{"192.168.1.1"}).getProviderVlanId();
            testVlan = vlanToRemove;

            SubnetCreateOptions options = SubnetCreateOptions.getInstance(testVlan, "10.0.1.0/24", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test");

            subnetToRemove = support.createSubnet(options).getProviderSubnetId();
            testSubnet = subnetToRemove;
        }
    }
    
    @After
    public void tearDown() {
        try {
            try {
                if( subnetToRemove != null ) {
                    getSupport().removeSubnet(subnetToRemove);
                }
            }
            catch( Throwable ignore ) {
                // ignore me
            }
            cleanUp(cloud);
            try {
                if( vlanToRemove != null ) {
                    getSupport().removeVlan(vlanToRemove);
                }
            }
            catch( Throwable ignore ) {
                // ignore me
            }
            APITrace.report(getName());
            APITrace.reset();
            try {
                if( cloud != null ) {
                    cloud.close();
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
    public void testProvisionSubnet() throws CloudException, InternalException {
        if( subnetsSupported ) {
            if( getSupport().allowsNewSubnetCreation() ) {
                SubnetCreateOptions options = SubnetCreateOptions.getInstance(testVlan, "10.0.1.0/24", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test");

                subnetToRemove = getSupport().createSubnet(options).getProviderSubnetId();
                assertNotNull("Did not return any subnet", subnetToRemove);
                out(subnetToRemove);
                try { Thread.sleep(5000L); }
                catch( InterruptedException e ) { }
                Subnet subnet = getSupport().getSubnet(subnetToRemove);

                assertNotNull("Not able to find newly created subnet", subnet);
                assertEquals("Created subnet does not match", subnetToRemove, subnet.getProviderSubnetId());
            }
            else {
                try {
                    SubnetCreateOptions options = SubnetCreateOptions.getInstance(testVlan, "10.0.1.0/24", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test");

                    subnetToRemove = getSupport().createSubnet(options).getProviderSubnetId();
                    fail("Implementations that do not support subnet creation should throw OperationNotSupportedException");
                }
                catch( OperationNotSupportedException e ) {
                    out("Received operation not supported exception (OK)");
                }
            }
        }
        else {
            out("No subnet support (OK)");
        }
    }
    
    @Test
    public void testProvisionVlan() throws CloudException, InternalException {
        if( vlansSupported ) {
            if( getSupport().allowsNewVlanCreation() ) {
                vlanToRemove = getSupport().createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[] { "192.168.1.1" },  new String[] { "192.168.1.1" }).getProviderVlanId();
                assertNotNull("Did not return any VLAN ID", vlanToRemove);
                out(vlanToRemove);
                try { Thread.sleep(5000L); }
                catch( InterruptedException e ) { }
                VLAN vlan = getSupport().getVlan(vlanToRemove);

                assertNotNull("Not able to find newly created vlan", vlan);
                assertEquals("Created vlan ID does not match", vlanToRemove, vlan.getProviderVlanId());
            }
            else {
                try {
                    vlanToRemove = getSupport().createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[] { "192.168.1.1" },  new String[] { "192.168.1.1" }).getProviderVlanId();
                    fail("Implementations that do not support VLAN creation should throw OperationNotSupportedException");
                }
                catch( OperationNotSupportedException e ) {
                    out("Received operation not supported exception (OK)");
                }
            }
        }
        else {
            out("No VLAN support (OK)");
        }
    }
    
    @Test
    public void testRemoveSubnet() throws CloudException, InternalException {
        if( subnetsSupported ) {
            if( getSupport().allowsNewSubnetCreation() ) {
                if( subnetToRemove != null ) {
                    getSupport().removeSubnet(subnetToRemove);
                    try { Thread.sleep(5000L); }
                    catch( InterruptedException e ) { }
                    Subnet subnet = getSupport().getSubnet(subnetToRemove);

                    assertNull("Subnet was not removed", subnet);
                    subnetToRemove = null;
                }
            }
            else {
                out("Subnet creation not supported (OK)");
            }
        }
        else {
            out("No subnet support (OK)");
        }
    }
    
    @Test
    public void testRemoveVlan() throws CloudException, InternalException {
        if( vlansSupported ) {
            if( vlanToRemove != null ) {
                getSupport().removeVlan(vlanToRemove);
                try { Thread.sleep(5000L); }
                catch( InterruptedException e ) { }
                VLAN vlan = getSupport().getVlan(vlanToRemove);

                assertNull("VLAN was not removed", vlan);
                vlanToRemove = null;
            }
            else if( getSupport().allowsNewVlanCreation() ) {
                out("WARNING: No test VLAN is available for removal, test is invalid");
            }
            else {
                out("VLAN creation/removal is not supported (OK)");
            }
        }
        else {
            out("No VLAN support (OK)");
        }
    }
}
