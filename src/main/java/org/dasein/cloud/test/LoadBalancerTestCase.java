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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.LbAlgorithm;
import org.dasein.cloud.network.LbListener;
import org.dasein.cloud.network.LbProtocol;
import org.dasein.cloud.network.LoadBalancer;
import org.dasein.cloud.network.LoadBalancerSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LoadBalancerTestCase extends BaseTestCase {
    private CloudProvider cloud               = null;
    private String        lbToKill            = null;
    private String        secondaryDataCenter = null;
    private String        secondaryServer     = null;
    private String        testAddress         = null;
    private String[]      testDcIds           = new String[0];
    private LbListener    testListener        = null;
    private String        testLoadBalancer    = null;
    private String[]      testServerIds       = new String[0];
    
    public LoadBalancerTestCase(String name) { super(name); }
    
    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        String name = getName();
        
        cloud = getProvider();
        cloud.connect(getTestContext());
        
        LoadBalancerSupport support = cloud.getNetworkServices().getLoadBalancerSupport();
        
        if( name.equals("testAddDataCenter") || name.equals("testRemoveDataCenter") ) {
            if( support.isDataCenterLimited() ) {
                for( DataCenter dc : cloud.getDataCenterServices().listDataCenters(cloud.getContext().getRegionId()) ) {
                    if( !dc.getProviderDataCenterId().equals(getTestDataCenterId()) ) {
                        secondaryDataCenter = dc.getProviderDataCenterId();
                        break;
                    }
                }
            }
        }
        if( name.equals("testAddVirtualMachine") || name.equals("testRemoveVirtualMachine") ) {
            secondaryServer = launch(cloud);
        }
        if( name.equals("testGetLoadBalancer") || name.equals("testLoadBalancerContent") ) {
            for( LoadBalancer lb : support.listLoadBalancers() ) {
                testLoadBalancer = lb.getProviderLoadBalancerId();
                break;
            }
            if( testLoadBalancer == null ) {
                lbToKill = makeTestLoadBalancer(cloud);
                testLoadBalancer = lbToKill;
            }
        }
        if( name.equals("testAddDataCenter") || name.equals("testRemoveDataCenter") ) {
            if( support.isDataCenterLimited() ) {   
                lbToKill = makeTestLoadBalancer(cloud);
                testLoadBalancer = lbToKill;
            }
        }
        if( name.equals("testAddVirtualMachine") || name.equals("testRemoveVirtualMachine") ) {
            lbToKill = makeTestLoadBalancer(cloud);
            testLoadBalancer = lbToKill;            
        }
        if( name.equals("testRemoveDataCenter") ) {
            if( support.isDataCenterLimited() ) {
                support.addDataCenters(testLoadBalancer, secondaryDataCenter);
                try { Thread.sleep(5000L); }
                catch( InterruptedException e ) { }
            }
        }
        if( name.equals("testRemoveVirtualMachine") ) {
            support.addServers(testLoadBalancer, secondaryServer);
            try { Thread.sleep(5000L); }
            catch( InterruptedException e ) { }
        }
        if( name.equals("testCreateLoadBalancer") ) {
            if( !support.isAddressAssignedByProvider() ) {
                for( IPVersion version : support.listSupportedIPVersions() ) {
                    try {
                        testAddress = identifyTestIPAddress(cloud, version);
                    }
                    catch( CloudException ignore ) {
                        // try again, maybe?
                    }
                }
                if( testAddress == null ) {
                    throw new CloudException("Unable to provision an IP address to test load balancers");
                }
            }
            if( support.isDataCenterLimited() ) {
                testDcIds = new String[] { getTestDataCenterId() };
            }
            
            testListener = new LbListener();
            
            for( LbAlgorithm algorithm : support.listSupportedAlgorithms() ) {
                testListener.setAlgorithm(algorithm);
                break;
            }
            for( LbProtocol protocol : support.listSupportedProtocols() ) {
                testListener.setNetworkProtocol(protocol);
                break;
            }
            testListener.setPrivatePort(2000);
            testListener.setPublicPort(2000);

            testServerIds = new String[0];
            if( support.requiresServerOnCreate() ) {
                lbVmToKill = launch(cloud);
                testServerIds = new String[] { lbVmToKill };
                while( true ) {
                    try { Thread.sleep(10000L); }
                    catch( InterruptedException e ) { }
                    VirtualMachine vm = cloud.getComputeServices().getVirtualMachineSupport().getVirtualMachine(lbVmToKill);
                    
                    if( !vm.getCurrentState().equals(VmState.PENDING) ) {
                        break;
                    }
                }
            }            
        }
    }

    @After
    @Override
    public void tearDown() {
        try {
            if( secondaryServer != null ) {
                cloud.getComputeServices().getVirtualMachineSupport().terminate(secondaryServer);
                secondaryServer = null;
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( lbVmToKill != null ) {
                cloud.getComputeServices().getVirtualMachineSupport().terminate(lbVmToKill);
                lbVmToKill = null;
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( lbToKill != null ) {
                cloud.getNetworkServices().getLoadBalancerSupport().remove(lbToKill);
                lbToKill = null;
                testLoadBalancer = null;
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        killTestAddress();
        try {
            if( cloud != null ) {
                cloud.close();
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
    }
    
    @Test 
    public void testAddDataCenter() throws CloudException, InternalException {
        begin();
        LoadBalancerSupport support = cloud.getNetworkServices().getLoadBalancerSupport();
        
        if( support.isDataCenterLimited() ) {
            support.addDataCenters(testLoadBalancer, secondaryDataCenter);
            try { Thread.sleep(5000L); }
            catch( InterruptedException e ) { }
            
            LoadBalancer lb = support.getLoadBalancer(testLoadBalancer);
            boolean found = false;
            
            for( String id : lb.getProviderDataCenterIds() ) {
                if( id.equals(secondaryDataCenter) ) {
                    found = true;
                    break;
                }
            }
            assertTrue("Added data center not found in list", found);
        }
        end();
    }
    
    @Test
    public void testAddVirtualMachine() throws CloudException, InternalException {
        begin();
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        LoadBalancerSupport support = cloud.getNetworkServices().getLoadBalancerSupport();

        support.addServers(testLoadBalancer, secondaryServer);
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
            
        LoadBalancer lb = support.getLoadBalancer(testLoadBalancer);
        boolean found = false;
            
        for( String id : lb.getProviderServerIds() ) {
            if( id.equals(secondaryServer) ) {
                found = true;
                break;
            }
        }
        if( !found ) {
            try { Thread.sleep(30000L); }
            catch( InterruptedException e ) { }
            lb = support.getLoadBalancer(testLoadBalancer);
            for( String id : lb.getProviderServerIds() ) {
                if( id.equals(secondaryServer) ) {
                    found = true;
                    break;
                }
            }   
            if( !found ) {
                try { Thread.sleep(30000L); }
                catch( InterruptedException e ) { }
                lb = support.getLoadBalancer(testLoadBalancer);
                for( String id : lb.getProviderServerIds() ) {
                    if( id.equals(secondaryServer) ) {
                        found = true;
                        break;
                    }
                }            
            }
        }
        assertTrue("Added virtual machine ID not found in list", found);
        end();
    }
    
    @Test
    public void testCreateLoadBalancer() throws CloudException, InternalException {
        begin();
        lbToKill = cloud.getNetworkServices().getLoadBalancerSupport().create("dsn" + System.currentTimeMillis(), "Test LB", testAddress, testDcIds, new LbListener[] { testListener }, testServerIds);
        testLoadBalancer = lbToKill;
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        LoadBalancer lb = cloud.getNetworkServices().getLoadBalancerSupport().getLoadBalancer(lbToKill);
        
        assertNotNull("Could not find newly created load balancer", lb);
        end();
    }
    
    @Test
    public void testGetBogusLoadBalancer() throws CloudException, InternalException {
        begin();
        LoadBalancer lb = cloud.getNetworkServices().getLoadBalancerSupport().getLoadBalancer(UUID.randomUUID().toString());
        
        assertNull("Found a matching load balancer for a bogus ID", lb);
        end();
    }
    
    @Test
    public void testGetLoadBalancer() throws CloudException, InternalException {
        begin();
        LoadBalancer lb = cloud.getNetworkServices().getLoadBalancerSupport().getLoadBalancer(testLoadBalancer);
        
        assertNotNull("No load balancer matched test ID of " + testLoadBalancer, lb);
        assertEquals("ID does not match", testLoadBalancer, lb.getProviderLoadBalancerId());
        end();
    }
    
    @Test
    public void testListLoadBalancers() throws CloudException, InternalException {
        begin();
        Iterable<LoadBalancer> list = cloud.getNetworkServices().getLoadBalancerSupport().listLoadBalancers();
        assertNotNull("Load balancer list may not be null, just empty", list);
        try {
            for( LoadBalancer lb : list ) {
                out("Load Balancer: " + lb);
            }
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }
    
    @Test
    public void testLoadBalancerContent() throws CloudException, InternalException {
        begin();
        LoadBalancer lb = cloud.getNetworkServices().getLoadBalancerSupport().getLoadBalancer(testLoadBalancer);
        
        assertNotNull("No load balancer matched test ID of " + testLoadBalancer, lb);
        assertEquals("ID does not match", testLoadBalancer, lb.getProviderLoadBalancerId());
        assertNotNull("No address is listed for load balancer", lb.getAddress());
        assertNotNull("No address type is set for the load balancer", lb.getAddressType());
        assertNotNull("No state established for load balancer", lb.getCurrentState());
        assertNotNull("You must provider a value for load balancer description", lb.getDescription());
        assertNotNull("You must provider a value for load balancer name", lb.getName());
        assertNotNull("Listeners must have a value, even if empty", lb.getListeners());
        assertNotNull("Servers must have a value, even if empty", lb.getProviderServerIds());
        assertNotNull("Load balancer must have an owner account", lb.getProviderOwnerId());
        assertEquals("Load balancer region does not match", cloud.getContext().getRegionId(), lb.getProviderRegionId());
        assertNotNull("Public ports must have a value, even if empty", lb.getPublicPorts());
        try {
            StringBuilder str = new StringBuilder();
            
            out("Load Balancer ID: " + lb.getProviderLoadBalancerId());
            out("State:            " + lb.getCurrentState());
            out("Name:             " + lb.getName());
            out("Address:          " + lb.getAddress());
            out("Type:             " + lb.getAddressType());
            out("Owner ID:         " + lb.getProviderOwnerId());
            out("Region ID:        " + lb.getProviderRegionId());
            for( String id : lb.getProviderDataCenterIds() ) {
                str.append(id);
                str.append(",");
            }
            out("Data Centers:     " + str);
            str = new StringBuilder();
            for( String id : lb.getProviderServerIds() ) {
                str.append(id);
                str.append(",");
            }
            out("Servers:          " + str);    
            str = new StringBuilder();
            for( long port : lb.getPublicPorts() ) {
                str.append(port);
                str.append(",");
            }
            out("Public Ports:     " + str);  
            str = new StringBuilder();
            for( LbListener listener : lb.getListeners() ) {
                str.append(listener);
                str.append(",");
            }
            out("Listeners:        " + str);              
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }
    
    @Test
    public void testMetaData() throws CloudException, InternalException {
        begin();
        LoadBalancerSupport support = cloud.getNetworkServices().getLoadBalancerSupport();
        
        int maxPorts = support.getMaxPublicPorts();
        
        assertTrue("Max public ports must be at least 0", maxPorts > -1); 

        String term = support.getProviderTermForLoadBalancer(Locale.getDefault());
        
        assertNotNull("Term must not be null", term);
        
        Iterable<LbAlgorithm> algorithms = support.listSupportedAlgorithms();
        
        assertNotNull("Load balancer algorithms must not be null", algorithms);
        
        Iterable<LbProtocol> protocols = support.listSupportedProtocols();
        
        assertNotNull("Load balancer protocols must not be null", protocols);
        
        out("Term:                " + term);
        out("Max Public Ports:    " + maxPorts);
        out("LB Monitoring:       " + support.supportsMonitoring());
        out("Create w/Servers:    " + support.requiresServerOnCreate());
        out("Auto-assign Address: " + support.isAddressAssignedByProvider());
        out("Data-center Limited: " + support.isDataCenterLimited());
        out("Create w/Listener:   " + support.requiresListenerOnCreate());
        out("Algorithms:");
        for( LbAlgorithm algorithm : algorithms ) {
            out("\t" + algorithm);
        }
        out("Protocol:");
        for( LbProtocol protocol : protocols ) {
            out("\t" + protocol);
        }
        end();
    }
    
    @Test 
    public void testRemoveDataCenter() throws CloudException, InternalException {
        begin();
        LoadBalancerSupport support = cloud.getNetworkServices().getLoadBalancerSupport();
        
        if( support.isDataCenterLimited() ) {
            support.removeDataCenters(testLoadBalancer, secondaryDataCenter);
            try { Thread.sleep(5000L); }
            catch( InterruptedException e ) { }
            
            LoadBalancer lb = support.getLoadBalancer(testLoadBalancer);
            boolean found = false;
            
            for( String id : lb.getProviderDataCenterIds() ) {
                if( id.equals(secondaryDataCenter) ) {
                    found = true;
                    break;
                }
            }
            assertTrue("Removed data center still found in list", !found);
        }
        end();
    }
    
    @Test
    public void testRemoveVirtualMachine() throws CloudException, InternalException {
        begin();
        LoadBalancerSupport support = cloud.getNetworkServices().getLoadBalancerSupport();
        
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        support.removeServers(testLoadBalancer, secondaryServer);
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
            
        LoadBalancer lb = support.getLoadBalancer(testLoadBalancer);
        boolean found = false;
            
        for( String id : lb.getProviderServerIds() ) {
            if( id.equals(secondaryServer) ) {
                found = true;
                break;
            }
        }
        if( found ) {
            found = false;
            try { Thread.sleep(30000L); }
            catch( InterruptedException e ) { }
            lb = support.getLoadBalancer(testLoadBalancer);
            for( String id : lb.getProviderServerIds() ) {
                if( id.equals(secondaryServer) ) {
                    found = true;
                    break;
                }
            }      
            if( found ) {
                found = false;
                try { Thread.sleep(30000L); }
                catch( InterruptedException e ) { }
                lb = support.getLoadBalancer(testLoadBalancer);
                for( String id : lb.getProviderServerIds() ) {
                    if( id.equals(secondaryServer) ) {
                        found = true;
                        break;
                    }
                }           
            }
        }
        assertTrue("Removed server still in list of servers", !found);
        end();
    }
    
    @Test
    public void testSubscription() throws CloudException, InternalException {
        begin();
        cloud.getNetworkServices().getLoadBalancerSupport().isSubscribed();
        out("Subscribed: true");
        end();
    }
}
