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
import org.dasein.cloud.network.NICCreateOptions;
import org.dasein.cloud.network.NetworkInterface;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.network.VLAN;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VLANTestCase extends BaseTestCase {
    private CloudProvider cloud          = null;
    private String        nicToRemove    = null;
    private String        testNIC        = null;
    private String        subnetToRemove = null;
    private String        testSubnet     = null;
    private String        testVlan       = null;
    private String        vlanToRemove   = null;
    
    public VLANTestCase(String name) { super(name); }
    
    @Before
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        out("SETUP: " + getName());
        try {
            String name = getName();
            
            cloud = getProvider();
            cloud.connect(getTestContext());

            VLANSupport support = cloud.getNetworkServices().getVlanSupport();
            
            if( name.equals("testVlanContent") ) {
                for( VLAN vlan : cloud.getNetworkServices().getVlanSupport().listVlans() ) {
                    testVlan = vlan.getProviderVlanId();
                    break;
                }
                if( testVlan == null && support.allowsNewVlanCreation() ) {
                    vlanToRemove = cloud.getNetworkServices().getVlanSupport().createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[] { "192.168.1.1" },  new String[] { "192.168.1.1" }).getProviderVlanId();
                    testVlan = vlanToRemove;
                }
            }
            else if( name.equals("testProvisionSubnet") && support.allowsNewSubnetCreation() ) {
                vlanToRemove = cloud.getNetworkServices().getVlanSupport().createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[] { "192.168.1.1" },  new String[] { "192.168.1.1" }).getProviderVlanId();
                testVlan = vlanToRemove; 
            }
            else if( name.equals("testProvisionNIC") && support.allowsNewNetworkInterfaceCreation() ) {
                vlanToRemove = cloud.getNetworkServices().getVlanSupport().createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[] { "192.168.1.1" },  new String[] { "192.168.1.1" }).getProviderVlanId();
                testVlan = vlanToRemove;
                if( support.allowsNewSubnetCreation() ) {
                    subnetToRemove = cloud.getNetworkServices().getVlanSupport().createSubnet("10.0.1.0/24", testVlan, "dsngettest-" + System.currentTimeMillis(), "DSN Get Test").getProviderSubnetId();
                    testSubnet = subnetToRemove;                    
                }
            }
            else if( name.equals("testSubnetContent") || name.equals("testListSubnets") ) {
                if( !cloud.getNetworkServices().getVlanSupport().getSubnetSupport().equals(Requirement.NONE)  ) {
                    for( VLAN vlan : cloud.getNetworkServices().getVlanSupport().listVlans() ) {
                        for( Subnet subnet : cloud.getNetworkServices().getVlanSupport().listSubnets(vlan.getProviderVlanId()) ) {
                            testSubnet = subnet.getProviderSubnetId();
                            testVlan = vlan.getProviderVlanId();
                        }
                        if( testSubnet != null ) {
                            break;
                        }
                    }
                    if( testVlan == null && cloud.getNetworkServices().getVlanSupport().allowsNewVlanCreation() ) {
                        vlanToRemove = cloud.getNetworkServices().getVlanSupport().createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[] { "192.168.1.1" },  new String[] { "192.168.1.1" }).getProviderVlanId();
                        testVlan = vlanToRemove;
                    }
                    if( testVlan != null ) {
                        for( Subnet subnet : cloud.getNetworkServices().getVlanSupport().listSubnets(testVlan) ) {
                            testSubnet = subnet.getProviderSubnetId();
                        }
                        if( testSubnet == null ) {
                            if( name.equals("testSubnetContent") && cloud.getNetworkServices().getVlanSupport().allowsNewSubnetCreation() ) {
                                subnetToRemove = cloud.getNetworkServices().getVlanSupport().createSubnet("10.0.1.0/24", testVlan, "dsngettest-" + System.currentTimeMillis(), "DSN Get Test").getProviderSubnetId();
                                testSubnet = subnetToRemove;
                            }
                        }
                    }
                }
            }
            else if( name.equals("testRemoveVlan") && cloud.getNetworkServices().getVlanSupport().allowsNewVlanCreation() ) {
                vlanToRemove = cloud.getNetworkServices().getVlanSupport().createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[] { "192.168.1.1" },  new String[] { "192.168.1.1" }).getProviderVlanId();
                testVlan = vlanToRemove;            
            }
            else if( name.equals("testRemoveSubnet") && cloud.getNetworkServices().getVlanSupport().allowsNewVlanCreation() && cloud.getNetworkServices().getVlanSupport().allowsNewSubnetCreation() ) {
                vlanToRemove = cloud.getNetworkServices().getVlanSupport().createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[] { "192.168.1.1" },  new String[] { "192.168.1.1" }).getProviderVlanId();
                testVlan = vlanToRemove;
                subnetToRemove = cloud.getNetworkServices().getVlanSupport().createSubnet("10.0.1.0/24", testVlan, "dsngettest-" + System.currentTimeMillis(), "DSN Get Test").getProviderSubnetId();
                testSubnet = subnetToRemove;            
            }
            else if( name.equals("testRemoveNIC") && support.isNetworkInterfaceSupportEnabled() ) {
                NICCreateOptions options;
                
                if( support.allowsNewVlanCreation() ) {
                    vlanToRemove = cloud.getNetworkServices().getVlanSupport().createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[] { "192.168.1.1" },  new String[] { "192.168.1.1" }).getProviderVlanId();
                    testVlan = vlanToRemove;
                }
                else {
                    fail("Unable to create a vlan for testing nics");
                }
                if( support.allowsNewSubnetCreation() ) {
                    subnetToRemove = cloud.getNetworkServices().getVlanSupport().createSubnet("10.0.1.0/24", testVlan, "dsngettest-" + System.currentTimeMillis(), "DSN Get Test").getProviderSubnetId();
                    testSubnet = subnetToRemove;
                    options = NICCreateOptions.getInstanceForSubnet(testSubnet, "testRemoveNIC" + System.currentTimeMillis(), "testRemoveNIC");
                }
                else {
                    options = NICCreateOptions.getInstanceForSubnet(testVlan, "testRemoveNIC" + System.currentTimeMillis(), "testRemoveNIC");                    
                }
                testNIC = support.createNetworkInterface(options).getProviderNetworkInterfaceId();
                nicToRemove = testNIC;
            }
            if( vlanToRemove != null ) {
                out("Created VLAN " + vlanToRemove + " for test case " + name);            
            }
            if( subnetToRemove != null ) {
                out("Created subnet " + subnetToRemove + " for test case " + name);
            }
        }
        finally {
            out("SETUP COMPLETE");            
        }
    }
    
    @After
    public void tearDown() {
        out("TEAR DOWN");
        try {
            if( nicToRemove != null ) {
                cloud.getNetworkServices().getVlanSupport().removeNetworkInterface(nicToRemove);
            }
        }
        catch( Throwable ignore ) {
            // ignore me
        }
        try {
            if( subnetToRemove != null ) {
                cloud.getNetworkServices().getVlanSupport().removeSubnet(subnetToRemove);
            }
        }
        catch( Throwable ignore ) {
            // ignore me
        }
        try {
            if( vlanToRemove != null ) {
                cloud.getNetworkServices().getVlanSupport().removeVlan(vlanToRemove);
            }
        }
        catch( Throwable ignore ) {
            // ignore me
        }
        try {
            if( cloud != null ) {
                cloud.close();
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        out("TEAR DOWN COMPLETE");
    }
    
    @Test
    public void testGetBogusSubnet() throws CloudException, InternalException {
        begin();
        if( !cloud.getNetworkServices().getVlanSupport().getSubnetSupport().equals(Requirement.NONE) ) {
            if( cloud.getNetworkServices().getVlanSupport().allowsNewSubnetCreation() ) {
                Subnet subnet = cloud.getNetworkServices().getVlanSupport().getSubnet(UUID.randomUUID().toString());
                
                assertNull("Found a matching subnet for test", subnet);
            }
        }
        end();        
    }
    
    @Test
    public void testGetBogusVlan() throws CloudException, InternalException {
        begin();
        VLAN vlan = cloud.getNetworkServices().getVlanSupport().getVlan(UUID.randomUUID().toString());
        
        assertNull("Found a matching VLAN for test", vlan);
        end();        
    }
    
    @Test
    public void testListSubnets() throws CloudException, InternalException {
        begin();
        if( !getVlanSupport().getSubnetSupport().equals(Requirement.NONE)) {
            VLANSupport vlanSupport = getVlanSupport();
            Iterable<Subnet> subnets = vlanSupport.listSubnets(testVlan);
            
            assertNotNull("Subnet list cannot be null", subnets);
            try {
                for( Subnet subnet : subnets ) {
                    out("Subnet: " + subnet);
                }
            }
            catch( Throwable notPartOfTest ) {
                // ignore this
            }
        }
        end();
    }
    
    @Test
    public void testListVlans() throws CloudException, InternalException {
        begin();
        VLANSupport vlanSupport = cloud.getNetworkServices().getVlanSupport();
        Iterable<VLAN> vlans = vlanSupport.listVlans();
        
        assertNotNull("VLAN list cannot be null", vlans);
        try {
            for( VLAN vlan : vlans ) {
                out("VLAN: " + vlan);
            }
        }
        catch( Throwable notPartOfTest ) {
            // ignore this
        }
        end();
    }

    private VLANSupport getVlanSupport() { 
        NetworkServices services = cloud.getNetworkServices();
        
        return (services == null ? null : services.getVlanSupport());
    }
    
    @Test
    public void testMetaData() throws CloudException, InternalException {
        begin();
        VLANSupport support = getVlanSupport();
        
        if( support == null ) {
            out("No support for VLANs in this cloud");
        }
        else {
            out("Term for VLAN:             " + support.getProviderTermForVlan(Locale.getDefault()));
            out("Term for Subnet:           " + support.getProviderTermForSubnet(Locale.getDefault()));
            out("Term for NIC:              " + support.getProviderTermForNetworkInterface(Locale.getDefault()));
            out("Allows VLAN Creation:      " + support.allowsNewVlanCreation());
            out("Allows Subnet Creation:    " + support.allowsNewSubnetCreation());
            out("Max NIC Count:             " + support.getMaxNetworkInterfaceCount());
            out("Max VLAN Count:            " + support.getMaxVlanCount());
            out("Vlan DC Constrained:       " + support.isVlanDataCenterConstrained());
            out("Subnet DC Constrained:     " + support.isSubnetDataCenterConstrained());
            out("Subnets in VLANs:          " + support.getSubnetSupport());
            out("NICs Enabled:              " + support.isNetworkInterfaceSupportEnabled());
            out("Internet Gateway Creation: " + support.supportsInternetGatewayCreation());
            out("Raw address routing:       " + support.supportsRawAddressRouting());
        }
        end();
    }
    
    @Test
    public void testProvisionSubnet() throws CloudException, InternalException {
        begin();
        if( cloud.getNetworkServices().getVlanSupport().allowsNewSubnetCreation() ) {
            subnetToRemove = cloud.getNetworkServices().getVlanSupport().createSubnet("10.0.1.0/24", testVlan, "dsngettest-" + System.currentTimeMillis(), "DSN Get Test").getProviderSubnetId();
            assertNotNull("Did not return any subnet", subnetToRemove);
            out(subnetToRemove);
            try { Thread.sleep(5000L); }
            catch( InterruptedException e ) { }
            Subnet subnet = cloud.getNetworkServices().getVlanSupport().getSubnet(subnetToRemove);
            
            assertNotNull("Not able to find newly created subnet", subnet);
            assertEquals("Created subnet does not match", subnetToRemove, subnet.getProviderSubnetId());
        }
        else {
            try {
                subnetToRemove = cloud.getNetworkServices().getVlanSupport().createSubnet("10.0.1.0/24", "any", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test").getProviderSubnetId();
                fail("Implementations that do not support subnet creation should throw OperationNotSupportedException");
            }
            catch( OperationNotSupportedException e ) {
                // expected
            }
        }
        end();
    }
    
    @Test
    public void testProvisionVlan() throws CloudException, InternalException {
        begin();
        if( cloud.getNetworkServices().getVlanSupport().allowsNewVlanCreation() ) {
            vlanToRemove = cloud.getNetworkServices().getVlanSupport().createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[] { "192.168.1.1" },  new String[] { "192.168.1.1" }).getProviderVlanId();
            assertNotNull("Did not return any VLAN ID", vlanToRemove);
            out(vlanToRemove);
            try { Thread.sleep(5000L); }
            catch( InterruptedException e ) { }
            VLAN vlan = cloud.getNetworkServices().getVlanSupport().getVlan(vlanToRemove);
            
            assertNotNull("Not able to find newly created vlan", vlan);
            assertEquals("Created vlan ID does not match", vlanToRemove, vlan.getProviderVlanId());
        }
        else {
            try {
                vlanToRemove = cloud.getNetworkServices().getVlanSupport().createVlan("10.0.0.0/16", "dsngettest-" + System.currentTimeMillis(), "DSN Get Test", "dasein.org", new String[] { "192.168.1.1" },  new String[] { "192.168.1.1" }).getProviderVlanId();
                fail("Implementations that do not support VLAN creation should throw OperationNotSupportedException");
            }
            catch( OperationNotSupportedException e ) {
                // expected
            }
        }
        end();
    }
    
    @Test
    public void testRemoveSubnet() throws CloudException, InternalException {
        begin();
        if( cloud.getNetworkServices().getVlanSupport().allowsNewSubnetCreation() ) {
            if( subnetToRemove != null ) {
                cloud.getNetworkServices().getVlanSupport().removeSubnet(subnetToRemove);
                try { Thread.sleep(5000L); }
                catch( InterruptedException e ) { }
                Subnet subnet = cloud.getNetworkServices().getVlanSupport().getSubnet(subnetToRemove);
        
                assertNull("Subnet was not removed", subnet);
                subnetToRemove = null;
            }
        }
        end();
    }
    
    @Test
    public void testRemoveVlan() throws CloudException, InternalException {
        begin();
        if( vlanToRemove != null ) {
            cloud.getNetworkServices().getVlanSupport().removeVlan(vlanToRemove);
            try { Thread.sleep(5000L); }
            catch( InterruptedException e ) { }
            VLAN vlan = cloud.getNetworkServices().getVlanSupport().getVlan(vlanToRemove);
    
            assertNull("VLAN was not removed", vlan);
            vlanToRemove = null;
        }
        end();
    }
    
    @Test
    public void testSubnetContent() throws CloudException, InternalException {
        begin();
        if( !cloud.getNetworkServices().getVlanSupport().getSubnetSupport().equals(Requirement.NONE) ) {
            Subnet subnet = cloud.getNetworkServices().getVlanSupport().getSubnet(testSubnet);
            
            assertNotNull("Subnet was not found", subnet);
            assertEquals("Subnet did not match target", testSubnet, subnet.getProviderSubnetId());
            assertNotNull("Subnet has no name", subnet.getName());
            assertNotNull("Subnet has no description", subnet.getDescription());
            assertNotNull("Subnet has no owner", subnet.getProviderOwnerId());
            assertNotNull("Subnet has no region", subnet.getProviderRegionId());
            assertNotNull("Subnet tags are null", subnet.getTags());
            assertNotNull("CIDR is null", subnet.getCidr());
            assertNotNull("State is null", subnet.getCurrentState());
            if( cloud.getNetworkServices().getVlanSupport().isSubnetDataCenterConstrained() ) {
                assertNotNull("Data center is null", subnet.getProviderDataCenterId());
            }
            out("ID:          " + subnet.getProviderSubnetId());
            out("Name:        " + subnet.getName());
            out("Owner:       " + subnet.getProviderOwnerId());
            out("CIDR:        " + subnet.getCidr());
            out("Addresses:   " + subnet.getAvailableIpAddresses());
            out("VLAN:        " + subnet.getProviderVlanId());
            out("Region:      " + subnet.getProviderRegionId());
            out("Data Center: " + subnet.getProviderDataCenterId());
            out("State:       " + subnet.getCurrentState());
            out("Description:\n" + subnet.getDescription());
        }
        end();
    }
    
    @Test
    public void testSubscription() throws CloudException, InternalException {
        begin();
        assertTrue("Account is not subscribed, tests will be invalid", cloud.getNetworkServices().getVlanSupport().isSubscribed());
        end();
    }
    
    @Test
    public void testVlanContent() throws CloudException, InternalException {
        begin();
        VLAN vlan = cloud.getNetworkServices().getVlanSupport().getVlan(testVlan);
        
        assertNotNull("VLAN was not found", vlan);
        assertEquals("VLAN did not match target", testVlan, vlan.getProviderVlanId());
        assertNotNull("VLAN has no name", vlan.getName());
        assertNotNull("VLAN has no description", vlan.getDescription());
        assertEquals("VLAN region mismatch", cloud.getContext().getRegionId(), vlan.getProviderRegionId());
        if( cloud.getNetworkServices().getVlanSupport().isVlanDataCenterConstrained() ) {
            assertNotNull("VLAN has no data center", vlan.getProviderDataCenterId());
        }
        out("ID:            " + vlan.getProviderVlanId());
        out("Name:          " + vlan.getName());
        out("Owner:         " + vlan.getProviderOwnerId());
        out("Current State: " + vlan.getCurrentState());
        out("CIDR:          " + vlan.getCidr());
        out("Region:        " + vlan.getProviderRegionId());
        out("Data Center:   " + vlan.getProviderDataCenterId());
        out("Domain Name:   " + vlan.getDomainName());
        out("Traffic:       " + Arrays.toString(vlan.getSupportedTraffic()));
        String[] ips = vlan.getDnsServers();
        out("DNS:           " + ((ips == null || ips.length < 1) ? "none" : ips.clone()[0]));
        ips = vlan.getNtpServers();
        out("NTP:           " + ((ips == null || ips.length < 1) ? "none" : ips.clone()[0]));
        end();
    }

    @Test
    public void testGetBogusNIC() throws CloudException, InternalException {
        begin();
        if( cloud.getNetworkServices().getVlanSupport().isNetworkInterfaceSupportEnabled() ) {
            NetworkInterface nic = cloud.getNetworkServices().getVlanSupport().getNetworkInterface(UUID.randomUUID().toString());

            out("Bogus NIC: " + nic);
            assertNull("Found a matching NIC for test", nic);
        }
        else {
            out("Cloud does not support network interfaces");
        }
        end();
    }

    @Test
    public void testListNICs() throws CloudException, InternalException {
        begin();
        VLANSupport support = cloud.getNetworkServices().getVlanSupport();
        
        if( support.isNetworkInterfaceSupportEnabled() ) {
            Iterable<NetworkInterface> nics = support.listNetworkInterfaces();

            assertNotNull("NIC list cannot be null", nics);
            try {
                for( NetworkInterface nic : nics ) {
                    out("NIC: " + nic);
                }
            }
            catch( Throwable notPartOfTest ) {
                // ignore this
            }
        }
        else {
            out("Cloud does not support network interfaces");
        }
        end();
    }
    
    @Test
    public void testProvisionNIC() throws CloudException, InternalException {
        begin();
        if( cloud.getNetworkServices().getVlanSupport().isNetworkInterfaceSupportEnabled() ) {
            NICCreateOptions options;
            
            if( testSubnet != null ) {
                options = NICCreateOptions.getInstanceForSubnet(testSubnet, "testProvisionNIC" + System.currentTimeMillis(), "testProvisionNIC " + System.currentTimeMillis());
            }
            else {
                options = NICCreateOptions.getInstanceForSubnet(testVlan, "testProvisionNIC" + System.currentTimeMillis(), "testProvisionNIC " + System.currentTimeMillis());            
            }
            String id = getTestFirewallId();
            
            if( id != null ) {
                options.behindFirewalls(id);
            }
            NetworkInterface nic = cloud.getNetworkServices().getVlanSupport().createNetworkInterface(options);
            
            out("Provisioned: " + nic);
            assertNotNull("No NIC was created", nic);
            nicToRemove = nic.getProviderNetworkInterfaceId();
        }
        else {
            out("Cloud does not support network interfaces");
        }
        end();
    }

    @Test
    public void testRemoveNIC() throws CloudException, InternalException {
        begin();
        if( cloud.getNetworkServices().getVlanSupport().isNetworkInterfaceSupportEnabled() ) {
            if( nicToRemove != null ) {
                cloud.getNetworkServices().getVlanSupport().removeNetworkInterface(nicToRemove);
                try { Thread.sleep(5000L); }
                catch( InterruptedException e ) { }
                NetworkInterface nic = cloud.getNetworkServices().getVlanSupport().getNetworkInterface(nicToRemove);

                assertNull("NIC was not removed", nic);
                nicToRemove = null;
            }
        }
        else {
            out("Cloud does not support network interfaces");
        }
        end();
    }
}
