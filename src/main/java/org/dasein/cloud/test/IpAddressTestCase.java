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

import java.io.IOException;
import java.util.UUID;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.AddressType;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpForwardingRule;
import org.dasein.cloud.network.Protocol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IpAddressTestCase extends BaseTestCase {
    private String        addressToRelease = null;
    private CloudProvider provider         = null;
    private String        testAddress      = null;
    private String        testRuleId       = null;
    private String        vmToKill         = null;
    
    public IpAddressTestCase(String name) { super(name); }
    
    // TODO: IPv6 support
    private void assertValidAddress(String address) {
        assertNotNull("IP address value is empty", address);
        String[] parts = address.split("\\.");
        
        assertEquals("Address " + address + " is not a valid IPv4 address", 4, parts.length);
        for( int i=0; i<parts.length; i++ ) {
            try {
                int x = Integer.parseInt(parts[i]);
                
                assertTrue("Address " + address + " is not a valid IPv4 address", x > -1 && x < 256);
            }
            catch( NumberFormatException e ) {
                fail("Address " + address + " is not a valid IPv4 address");
            }
        }
    }
    
    private void initAddress() throws InternalException, CloudException {
        String name = getName();
        
        if( name.startsWith("testReleasePublicIpFromPool") ) {
            if( provider.getNetworkServices().getIpAddressSupport().isRequestable(AddressType.PUBLIC) ) {
                addressToRelease = provider.getNetworkServices().getIpAddressSupport().request(AddressType.PUBLIC);
                testAddress = addressToRelease;
            }
        }
        else if( name.startsWith("testReleasePrivateIpFromPool") ) {
            if( provider.getNetworkServices().getIpAddressSupport().isRequestable(AddressType.PRIVATE) ) {
                addressToRelease = provider.getNetworkServices().getIpAddressSupport().request(AddressType.PRIVATE);
                testAddress = addressToRelease;
            }
        }  
        else if( name.startsWith("testReleasePublicIpFromServer") ) {
            if( provider.getNetworkServices().getIpAddressSupport().isAssigned(AddressType.PUBLIC) ) {
                for( IpAddress addr : provider.getNetworkServices().getIpAddressSupport().listPublicIpPool(true) ) {
                    testAddress = addr.getProviderIpAddressId();
                    break;
                }
                if( testAddress == null ) {
                    if( provider.getNetworkServices().getIpAddressSupport().isRequestable(AddressType.PUBLIC) ) {
                        addressToRelease = provider.getNetworkServices().getIpAddressSupport().request(AddressType.PUBLIC);
                        testAddress = addressToRelease;
                    }
                }
                if( testAddress == null ) {
                    throw new CloudException("Unable to identify a free IP address for test execution.");
                } 
            }
        } 
        else if( name.startsWith("testReleasePrivateIpFromServer") ) {
            if( provider.getNetworkServices().getIpAddressSupport().isAssigned(AddressType.PRIVATE) ) {
                for( IpAddress addr : provider.getNetworkServices().getIpAddressSupport().listPublicIpPool(true) ) {
                    testAddress = addr.getProviderIpAddressId();
                    break;
                }
                if( testAddress == null ) {
                    if( provider.getNetworkServices().getIpAddressSupport().isRequestable(AddressType.PUBLIC) ) {
                        addressToRelease = provider.getNetworkServices().getIpAddressSupport().request(AddressType.PUBLIC);
                        testAddress = addressToRelease;
                    }
                }
                if( testAddress == null ) {
                    throw new CloudException("Unable to identify a free IP address for test execution.");
                } 
            }
        } 
        else if( name.equals("testAssignPrivateIp") ) {
            if( provider.getNetworkServices().getIpAddressSupport().isAssigned(AddressType.PRIVATE) ) {
                if( provider.getNetworkServices().getIpAddressSupport().isRequestable(AddressType.PRIVATE) ) {
                    addressToRelease = provider.getNetworkServices().getIpAddressSupport().request(AddressType.PRIVATE);
                    testAddress = addressToRelease;
                }
                else {
                    for( IpAddress addr : provider.getNetworkServices().getIpAddressSupport().listPrivateIpPool(true) ) {
                        testAddress = addr.getProviderIpAddressId();
                        break;
                    }
                    if( testAddress == null ) {
                        throw new CloudException("Unable to identify a free IP address for test execution.");
                    }
                }
            }
        }
        else if( name.equals("testAssignPublicIp") ) {
            if( provider.getNetworkServices().getIpAddressSupport().isAssigned(AddressType.PUBLIC) ) {
                if( provider.getNetworkServices().getIpAddressSupport().isRequestable(AddressType.PUBLIC) ) {
                    addressToRelease = provider.getNetworkServices().getIpAddressSupport().request(AddressType.PUBLIC);
                    testAddress = addressToRelease;
                }
                else {
                    for( IpAddress addr : provider.getNetworkServices().getIpAddressSupport().listPublicIpPool(true) ) {
                        testAddress = addr.getProviderIpAddressId();
                        break;
                    }
                    if( testAddress == null ) {
                        throw new CloudException("Unable to identify a free IP address for test execution.");
                    }
                }                
            }
        }
        else if( name.equals("testForward") || name.equals("testStopForward") || name.equals("testStopBogusForward") ) {
            if( provider.getNetworkServices().getIpAddressSupport().isRequestable(AddressType.PUBLIC) ) {
                addressToRelease = provider.getNetworkServices().getIpAddressSupport().request(AddressType.PUBLIC);
                testAddress = addressToRelease;
            }
            else {
                for( IpAddress addr : provider.getNetworkServices().getIpAddressSupport().listPublicIpPool(true) ) {
                    testAddress = addr.getProviderIpAddressId();
                    break;
                }
                if( testAddress == null ) {
                    throw new CloudException("Unable to identify a free IP address for test execution.");
                }
            }
        }
        else if( name.equals("testGetAddress") || name.equals("testAddressContent") || name.equals("testListForwardRules") ) {
            for( IpAddress addr : provider.getNetworkServices().getIpAddressSupport().listPublicIpPool(false) ) {
                testAddress = addr.getProviderIpAddressId();
                break;
            }
            if( testAddress == null && !name.equals("testListForwardRules") ) {
                for( IpAddress addr : provider.getNetworkServices().getIpAddressSupport().listPrivateIpPool(false) ) {
                    testAddress = addr.getProviderIpAddressId();
                    break;
                }
            }            
            if( testAddress == null && provider.getNetworkServices().getIpAddressSupport().isRequestable(AddressType.PUBLIC) ) {
                addressToRelease = provider.getNetworkServices().getIpAddressSupport().request(AddressType.PUBLIC);
                testAddress = addressToRelease;
            }
            if( testAddress == null && provider.getNetworkServices().getIpAddressSupport().isRequestable(AddressType.PRIVATE) ) {
                addressToRelease = provider.getNetworkServices().getIpAddressSupport().request(AddressType.PRIVATE);
                testAddress = addressToRelease;
            }
        }
    }
    
    private void initVm() throws InternalException, CloudException {
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();
        String name = getName();
        
        if( name.equals("testAssignPrivateIp") || name.equals("testReleasePrivateIpFromServer") ) {
            if( services.isAssigned(AddressType.PRIVATE) ) {
                vmToKill = launch(provider);
            }
        }
        else if( name.equals("testAssignPublicIp") || name.equals("testReleasePublicIpFromServer")  ) {
            if( services.isAssigned(AddressType.PUBLIC) ) {
                vmToKill = launch(provider);
            }
        }
        else if( name.equals("testForward") || name.equals("testStopForward") ) {
            if( services.isForwarding() ) {
                vmToKill = launch(provider);
            }
        }
        if( vmToKill != null ) {
            VmState currentState;
            
            do {
                try { Thread.sleep(5000L); }
                catch( InterruptedException e ) { }
                VirtualMachine vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(vmToKill);
                currentState = (vm == null ? VmState.TERMINATED : vm.getCurrentState());
            }
            while( !currentState.equals(VmState.RUNNING) && !currentState.equals(VmState.TERMINATED));
        }
    }
    
    @Before
    @Override
    public void setUp() throws CloudException, InternalException, InstantiationException, IllegalAccessException, IOException {
        provider = getProvider();
        provider.connect(getTestContext());
        initAddress();
        initVm();
        if( getName().equals("testStopForward") ) {
            IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();
            
            if( services.isForwarding() ) {
                testRuleId = services.forward(testAddress, 9321, Protocol.TCP, 22, vmToKill);
            }
        }
        else if( getName().equals("testReleasePrivateIpFromServer") ) {
            IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();

            if( services.isAssigned(AddressType.PRIVATE) ) {
                services.assign(testAddress, vmToKill);
            }
        }
        else if( getName().equals("testReleasePublicIpFromServer") ) {
            IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();

            if( services.isAssigned(AddressType.PUBLIC) ) {
                services.assign(testAddress, vmToKill);
            }
        }        
    }
    
    @After
    @Override
    public void tearDown() throws CloudException, InternalException {
        try {
            if( testRuleId != null ) {
                provider.getNetworkServices().getIpAddressSupport().stopForward(testRuleId);
                testRuleId = null;
            }
        }
        catch( Throwable ignore ) {
            ignore.printStackTrace();
        }
        try {
            if( addressToRelease != null ) {
                provider.getNetworkServices().getIpAddressSupport().releaseFromServer(addressToRelease);
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( addressToRelease != null ) {
                provider.getNetworkServices().getIpAddressSupport().releaseFromPool(addressToRelease);
                addressToRelease = null;
            }
        }
        catch( Throwable ignore ) {
            ignore.printStackTrace();
        }
        try {
            if( vmToKill != null ) {
                provider.getComputeServices().getVirtualMachineSupport().terminate(vmToKill);
                vmToKill = null;
            }
        }
        catch( Throwable ignore ) {
            ignore.printStackTrace();
        }
        try {
            if( provider != null ) {
                provider.close();
                provider = null;
            }
        }
        catch( Throwable ignore ) {
            ignore.printStackTrace();
        }
    }
    
    @Test
    public void testAddressContent() throws CloudException, InternalException {
        begin();
        if( testAddress == null ) {
            return;
        }
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();

        IpAddress addr = services.getIpAddress(testAddress);
        
        assertNotNull("Address not found: " + testAddress, addr);
        assertEquals("Address does not match target", testAddress, addr.getProviderIpAddressId());
        out("Address ID: " + addr.getProviderIpAddressId());        
        assertValidAddress(addr.getAddress());
        out("IP: " + addr.getAddress());
        assertNotNull("An address must be either public or private", addr.getAddressType());
        out("Type: " + addr.getAddressType());
        assertEquals("Region does not match", provider.getContext().getRegionId(), addr.getRegionId());
        out("Region: " + addr.getRegionId());
        out("Server: " + addr.getServerId());
        out("LB: " + addr.getProviderLoadBalancerId());
        end();
    }
    
    @Test
    public void testAssignPrivateIp() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();
        
        if( services.isAssigned(AddressType.PRIVATE) ) {
            services.assign(testAddress, vmToKill);
            try { Thread.sleep(5000L); }
            catch( InterruptedException e ) { }
            
            IpAddress addr = services.getIpAddress(testAddress);
            
            assertNotNull("Address did not exist: " + testAddress, addr);
            assertEquals("Server assignment does not match", vmToKill, addr.getServerId());
        }
        end();
    }
    
    @Test
    public void testAssignPublicIp() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();
        
        if( services.isAssigned(AddressType.PUBLIC) ) {
            services.assign(testAddress, vmToKill);
            try { Thread.sleep(5000L); }
            catch( InterruptedException e ) { }
            
            IpAddress addr = services.getIpAddress(testAddress);
            
            assertNotNull("Address did not exist: " + testAddress, addr);
            assertEquals("Server assignment does not match", vmToKill, addr.getServerId());
        }
        end();
    }
    
    @Test
    public void testForward() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();
        
        if( services.isForwarding() ) {
            String id = services.forward(testAddress, 9321, Protocol.TCP, 22, vmToKill);
            
            assertNotNull("No rule ID was returned", id);
            try { Thread.sleep(5000L); }
            catch( InterruptedException e ) { }

            boolean found = false;
            
            for( IpForwardingRule rule : services.listRules(testAddress) ) {
                if( rule.getProviderRuleId().equals(id) ) {
                    out(rule.toString());
                }
                found = true;
                break;
            }
            assertTrue("Rule silently failed (no rule listed)", found);
        }
        else {
            try {
                services.forward(testAddress == null ? "99" : testAddress, 9321, Protocol.TCP, 22, vmToKill == null ? "99" : vmToKill);
                fail("Assigning public IPs is supposedly not supported, but the operation returned successfully with: " + addressToRelease);
            }
            catch( OperationNotSupportedException success ) {
                // success
            }            
        }
        end();
    }
    
    @Test
    public void testGetAddress() throws CloudException, InternalException {
        begin();
        if( testAddress == null ) {
            return;
        }
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();

        IpAddress addr = services.getIpAddress(testAddress);
        
        assertNotNull("Address not found: " + testAddress, addr);
        assertEquals("Address does not match target", testAddress, addr.getProviderIpAddressId());
        end();
    }
    
    @Test
    public void testGetBogusAddress() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();

        IpAddress addr = services.getIpAddress(UUID.randomUUID().toString());
        
        assertNull("Found address at: " + addr, addr);
        end();
    }
    
    @Test
    public void testListAllPublicAddresses() throws CloudException, InternalException {
        begin();
        assertNotNull("No address list returned", provider.getNetworkServices().getIpAddressSupport().listPublicIpPool(false));
        end();
    }
    
    @Test 
    public void testListAllPrivateAddresses() throws CloudException, InternalException {
        begin();
        assertNotNull("No address list returned", provider.getNetworkServices().getIpAddressSupport().listPrivateIpPool(false));
        end();
    }
    
    @Test
    public void testListAvailablePublicAddresses() throws CloudException, InternalException {
        begin();
        Iterable<IpAddress> list = provider.getNetworkServices().getIpAddressSupport().listPublicIpPool(true);
        
        assertNotNull("No address list returned", list);
        for( IpAddress address : list ) {
            assertNull("No load balancer value should exist for an unassigned IP (" + address.getProviderLoadBalancerId() + ")", address.getProviderLoadBalancerId());
            assertNull("No server value should exist for an unassigned IP (" + address.getServerId() + ")", address.getServerId());
        }
        end();
    }
    
    @Test 
    public void testListAvailablePrivateAddresses() throws CloudException, InternalException {
        begin();
        Iterable<IpAddress> list = provider.getNetworkServices().getIpAddressSupport().listPrivateIpPool(true);

        assertNotNull("No address list returned", list);
        for( IpAddress address : list ) {
            assertNull("No load balancer value should exist for an unassigned IP (" + address.getProviderLoadBalancerId() + ")", address.getProviderLoadBalancerId());
            assertNull("No server value should exist for an unassigned IP (" + address.getServerId() + ")", address.getServerId());
        }
        end();
    }
    
    @Test
    public void testListForwardRules() throws CloudException, InternalException {
        begin();
        if( provider.getNetworkServices().getIpAddressSupport().isForwarding() ) {
            Iterable<IpForwardingRule> rules = provider.getNetworkServices().getIpAddressSupport().listRules(testAddress);
            
            assertNotNull("No rule list returned", rules);
            for( IpForwardingRule rule : rules ) {
                assertNotNull("Rule does not have an ID", rule.getProviderRuleId());
                assertNotNull("Rule does not specify a protocol", rule.getProtocol());
                assertEquals("Address does not match rule", testAddress, rule.getAddressId());
            }
        }
        end();
    }
    
    @Test
    public void testMetaData() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();

        out("Public assigned: " + services.isAssigned(AddressType.PUBLIC));
        out("Private assigned: " + services.isAssigned(AddressType.PRIVATE));
        out("Forwarding: " + services.isForwarding());
        out("Public requestable: " + services.isRequestable(AddressType.PUBLIC));
        out("Private requestable: " + services.isRequestable(AddressType.PRIVATE));
        end();
    }
    
    @Test
    public void testReleasePrivateIpFromPool() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();
        
        if( services.isRequestable(AddressType.PRIVATE) ) {
            services.releaseFromPool(addressToRelease);
            try { Thread.sleep(5000L); }
            catch( InterruptedException e ) { }
            IpAddress tmp = services.getIpAddress(addressToRelease);
            
            assertNull("Address still exists after release: " + addressToRelease, tmp);
            addressToRelease = null;
        }
        end();
    }
    
    @Test
    public void testReleasePrivateIpFromServer() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();
        
        if( services.isAssigned(AddressType.PRIVATE) ) {
            try {
                services.releaseFromServer(testAddress);
                try { Thread.sleep(5000L); }
                catch( InterruptedException e ) { }
                IpAddress tmp = services.getIpAddress(testAddress);
                
                assertTrue("Address is still attached: " + testAddress, tmp.getServerId() == null || !tmp.getServerId().equals(vmToKill));
            }
            catch( OperationNotSupportedException e ) {
                // some clouds may not support releasing a private IP
            }
        }
        end();
    }
    
    @Test
    public void testReleasePublicIpFromPool() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();
        
        if( services.isRequestable(AddressType.PUBLIC) ) {
            services.releaseFromPool(addressToRelease);
            try { Thread.sleep(30000L); }
            catch( InterruptedException e ) { }
            IpAddress tmp = services.getIpAddress(addressToRelease);
            
            assertNull("Address still exists after release: " + addressToRelease, tmp);
            addressToRelease = null;
        }
        end();
    }
    
    @Test
    public void testReleasePublicIpFromServer() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();
        
        if( services.isAssigned(AddressType.PUBLIC) ) {
            services.releaseFromServer(testAddress);
            try { Thread.sleep(30000L); }
            catch( InterruptedException e ) { }
            IpAddress tmp = services.getIpAddress(testAddress);
            
            assertTrue("Address is still attached: " + testAddress, tmp.getServerId() == null || !tmp.getServerId().equals(vmToKill));
        }
        end();
    }
    
    @Test
    public void testRequestPrivateIp() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();
        
        if( services.isRequestable(AddressType.PRIVATE) ) {
            addressToRelease = services.request(AddressType.PRIVATE);
            assertNotNull("No address resulted from your allocation request", addressToRelease);
        }
        else {
            try {
                addressToRelease = services.request(AddressType.PRIVATE);
                fail("Requesting private IPs is supposedly not supported, but the operation returned successfully with: " + addressToRelease);
            }
            catch( OperationNotSupportedException success ) {
                // success
            }
        }
        end();
    }
    
    @Test
    public void testRequestPublicIp() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();
        
        if( services.isRequestable(AddressType.PUBLIC) ) {
            addressToRelease = services.request(AddressType.PUBLIC);
            assertNotNull("No address resulted from your allocation request", addressToRelease);
        }
        else {
            try {
                addressToRelease = services.request(AddressType.PUBLIC);
                fail("Requesting public IPs is supposedly not supported, but the operation returned successfully with: " + addressToRelease);
            }
            catch( OperationNotSupportedException success ) {
                // success
            }
        }
        end();
    }
    
    @Test
    public void testStopBogusForward() throws CloudException, InternalException {
        begin();
        if( provider.getNetworkServices().getIpAddressSupport().isForwarding() ) {
            boolean success = false;
            
            try {
                provider.getNetworkServices().getIpAddressSupport().stopForward("-2");
                success = true;
            }
            catch( Throwable t ) {
                // desired outcome
            }
            assertTrue("Attempt to stop forwarding for bogus rule incorrectly succeeded", !success);
        }
        else {
            try {
                provider.getNetworkServices().getIpAddressSupport().stopForward("-2");
                fail("Attempts to stop forwarding should throw OperationNotSupportedException when forwarding is not supported");
            }
            catch( OperationNotSupportedException e ) {
                // success
            }
        }
        end();
    }
    
    @Test
    public void testStopForward() throws CloudException, InternalException {
        begin();
        if( provider.getNetworkServices().getIpAddressSupport().isForwarding() ) {
            provider.getNetworkServices().getIpAddressSupport().stopForward(testRuleId);
            boolean found = false;
            
            for( IpForwardingRule rule : provider.getNetworkServices().getIpAddressSupport().listRules(testAddress) ) {
                if( rule.getProviderRuleId().equals(testRuleId) ) {
                    found = true;
                }
                break;
            }
            assertTrue("The test rule is still in place", !found);
            testRuleId = null;
        }
        else {
            try {
                provider.getNetworkServices().getIpAddressSupport().stopForward("-2");
                fail("Attempts to stop forwarding should throw OperationNotSupportedException when forwarding is not supported");
            }
            catch( OperationNotSupportedException e ) {
                // success
            }
        }
        end();
    }
    
    @Test
    public void testSubscribed() throws CloudException, InternalException {
        begin();
        IpAddressSupport services = provider.getNetworkServices().getIpAddressSupport();

        assertTrue("No subscription exists for this service", services.isSubscribed());
        end();
    }
}
