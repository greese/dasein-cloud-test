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

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.Protocol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FirewallTestCase extends BaseTestCase {
    public FirewallTestCase(String name) { super(name); }

    private String        firewallToDelete = null;
    private CloudProvider provider         = null;
    private String        testFirewall     = null;
    
    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        provider = getProvider();
        provider.connect(getTestContext());
        String name = getName();
        
        if( name.equals("testAddFirewallRule") || name.equals("testDeleteFirewall") || name.equals("testRemoveFirewallRule") ) {
            testFirewall = provider.getNetworkServices().getFirewallSupport().create("dsntest" + (System.currentTimeMillis()%10000), "Firewall for testing.");
            if( name.equals("testRemoveFirewallRule") ) {
                provider.getNetworkServices().getFirewallSupport().authorize(testFirewall, "216.243.161.193/32", Protocol.TCP, 80, 80);
            }
            firewallToDelete = testFirewall;
        }
        else if( name.equals("testFirewallContent") || name.equals("testGetFirewall") || name.equals("testListFirewallRules") ) {
            for( Firewall fw : provider.getNetworkServices().getFirewallSupport().list() ) {
                if( fw.isActive() ) {
                    testFirewall = fw.getProviderFirewallId();
                    break;
                }
            }
            if( testFirewall == null ) {
                testFirewall = provider.getNetworkServices().getFirewallSupport().create("dsntest" + (System.currentTimeMillis()%10000), "Firewall for testing.");
                firewallToDelete = testFirewall;                
            }
        }
    }

    @After
    @Override
    public void tearDown() {
        try {
            if( firewallToDelete != null ) {
                provider.getNetworkServices().getFirewallSupport().delete(firewallToDelete);
                firewallToDelete = null;
            }
        }
        catch( Throwable t ) {
            t.printStackTrace();
        }
        try {
            if( provider != null ) {
                provider.close();
            }
        }
        catch( Throwable t ) {
            t.printStackTrace();
        }        
    }

    @Test
    public void testAddFirewallRule() throws CloudException, InternalException {
        provider.getNetworkServices().getFirewallSupport().authorize(testFirewall, "216.243.161.193/32", Protocol.TCP, 80, 80);
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        boolean added = false;
        
        for( FirewallRule rule : provider.getNetworkServices().getFirewallSupport().getRules(testFirewall) ) {
            if( rule.getCidr().equals("216.243.161.193/32") ) {
                if( rule.getProtocol().equals(Protocol.TCP) ) {
                    if( rule.getStartPort() == 80 ) {
                        if( rule.getEndPort() == 80 ) {
                            added = true;
                        }
                    }
                }
            }
        }
        assertTrue("Firewall rule was not added", added);
    }
    
    @Test
    public void testCreateFirewall() throws InternalException, CloudException {
        begin();
        testFirewall = provider.getNetworkServices().getFirewallSupport().create("dsntest" + (System.currentTimeMillis()%10000), "Dasein firewall create test");
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        Firewall fw = provider.getNetworkServices().getFirewallSupport().getFirewall(testFirewall);
        
        assertNotNull("Did not create a firewall", fw);
        firewallToDelete = testFirewall;
        end();
    }
    
    @Test
    public void testDeleteFirewall() throws InternalException, CloudException {
        begin();
        provider.getNetworkServices().getFirewallSupport().delete(testFirewall);
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        Firewall fw = provider.getNetworkServices().getFirewallSupport().getFirewall(testFirewall);
        
        assertNull("Deleted firewall still exists", fw);
        firewallToDelete = null;
        end();
    }
    
    @Test
    public void testFirewallContent() throws InternalException, CloudException {
        begin();
        Firewall fw = provider.getNetworkServices().getFirewallSupport().getFirewall(testFirewall);
        
        assertNotNull("A name must be set", fw.getName());
        assertNotNull("A description must be set", fw.getDescription());
        assertEquals("Firewall region must match request region", provider.getContext().getRegionId(), fw.getRegionId());
        assertNotNull("Firewall must have a description", fw.getDescription());
        out("ID: " + fw.getProviderFirewallId());
        out("Name: " + fw.getName());
        out("Description: " + fw.getDescription());
        out("Region: " + fw.getRegionId());
        end();
    }
    
    @Test
    public void testGetBogusFirewall() throws InternalException, CloudException {
        begin();
        Firewall fw = provider.getNetworkServices().getFirewallSupport().getFirewall(UUID.randomUUID().toString());
        
        assertNull("Found a matching firewall for bogus ID", fw);
        end();
    }
    
    @Test 
    public void testGetFirewall() throws InternalException, CloudException {
        begin();
        Firewall fw = provider.getNetworkServices().getFirewallSupport().getFirewall(testFirewall);
        
        assertNotNull("Could not find target firewall", fw);
        assertEquals("ID does not match returned firewall", testFirewall, fw.getProviderFirewallId());
        end();
    }
    
    @Test
    public void testListFirewalls() throws InternalException, CloudException {
        begin();
        Iterable<Firewall> firewalls = provider.getNetworkServices().getFirewallSupport().list();

        assertNotNull("Firewall listing may not be null", firewalls);
        try {
            for( Firewall firewall : firewalls ) {
                out("Firewall: " + firewall);
            }
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }  
        end();
    }
    
    @Test
    public void testListFirewallRules() throws InternalException, CloudException {
        begin();
        Collection<FirewallRule> rules = provider.getNetworkServices().getFirewallSupport().getRules(testFirewall);
        
        assertNotNull("Firewall rules may not be null", rules);
        try {
            for( FirewallRule rule : rules ) {
                out("Rule: " + rule);
            }
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }
    
    @Test
    public void testMetaData() {
        begin();
        String term = provider.getNetworkServices().getFirewallSupport().getProviderTermForFirewall(Locale.getDefault());
        
        assertNotNull("Must provide a provider name for firewalls", term);
        out("Firewall term: " + term);
        end();
    }
    
    @Test
    public void testRemoveFirewallRule() throws CloudException, InternalException {
        begin();
        provider.getNetworkServices().getFirewallSupport().revoke(testFirewall, "216.243.161.193/32", Protocol.TCP, 80, 80);
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        boolean removed = true;
        
        for( FirewallRule rule : provider.getNetworkServices().getFirewallSupport().getRules(testFirewall) ) {
            if( rule.getCidr().equals("216.243.161.193/32") ) {
                if( rule.getProtocol().equals(Protocol.TCP) ) {
                    if( rule.getStartPort() == 80 ) {
                        if( rule.getEndPort() == 80 ) {
                            removed = false;
                        }
                    }
                }
            }
        }
        assertTrue("Firewall rule was not removed", removed);
        end();
    }
    
    @Test
    public void testSubscription() {
        begin();
        // Currently a NO-OP
        out("Note: test not yet implemented");
        end();
    }
}
