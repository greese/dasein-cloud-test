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
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.IpForwardingRule;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Protocol;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("JUnit4AnnotatedMethodInJUnit3TestCase")
public class IpAddressTestCase extends BaseTestCase {
    static public final String T4_ADDRESS_CONTENT  = "test4AddressContent";
    static public final String T6_ADDRESS_CONTENT  = "test6AddressContent";
    static public final String T_GET_ADDRESS       = "testGetAddress";
    static public final String T4_RELEASE_ADDRESS  = "test4ReleaseAddress";
    static public final String T6_RELEASE_ADDRESS  = "test6ReleaseAddress";
    static public final String T4_ASSIGN_ADDRESS   = "test4AssignAddress";
    static public final String T6_ASSIGN_ADDRESS   = "test6AssignAddress";
    static public final String T_UNASSIGN_ADDRESS  = "testUnassignAddress";
    static public final String T4_LIST_RULES       = "test4ListForwardingRules";
    static public final String T6_LIST_RULES       = "test6ListForwardingRules";
    static public final String T4_FORWARD          = "test4Forward";
    static public final String T6_FORWARD          = "test6Forward";
    static public final String T4_STOP_FORWARD     = "test4StopForward";
    static public final String T6_STOP_FORWARD     = "test6StopForward";

    static public final int NEEDS_VMS = 9;

    static public void assertVersion(IpAddress address, IPVersion version) {
        String ip = address.getAddress();

        Assert.assertNotNull("IP is null and thus not valid " + version, ip);
        if( version.equals(IPVersion.IPV4) ) {
            String[] parts = ip.split("\\.");

            if( parts.length == 4 ) {
                for( String p : parts ) {
                    try {
                        int x = Integer.parseInt(p);

                        Assert.assertTrue("Invalid quad " + p + " in IPv4 address", x > -1 && x < 256);
                    }
                    catch( NumberFormatException e ) {
                        fail("Invalid quad " + p + " in IPv4 address");
                    }
                }
            }
            else {
                fail("IP " + ip + " is not a valid IPv4 address");
            }
        }
        else {
            try {
                Assert.assertNotNull("IP " + ip + " is not a valid IPv6 address", java.net.Inet6Address.getByName(ip));
            }
            catch( Exception e ) {
                fail("IP " + ip + " is not a valid IPv6 address");
            }
        }
    }

    static private VirtualMachine testVm;
    static private int            vmUse = 0;

    @Rule
    public  TestName      testName         = new TestName();

    private String         addressToRelease = null;
    private CloudProvider  provider         = null;
    private IpAddress      testAddress      = null;
    private String         testRuleId       = null;

    public IpAddressTestCase(String name) { super(name); }

    private void createTestVm() throws CloudException, InternalException {
        vmUse++;
        if( testVm == null ) {
            ComputeServices services = provider.getComputeServices();
            VirtualMachineSupport vmSupport;

            if( services != null ) {
                vmSupport = services.getVirtualMachineSupport();
                if( vmSupport != null ) {
                    testVm = vmSupport.getVirtualMachine(launch(provider));
                    if( testVm == null ) {
                        Assert.fail("Virtual machine failed to be reflected as launched");
                    }
                }
            }
        }
    }

    private boolean isSupported(@Nonnull IpAddressSupport support, @Nonnull IPVersion version) throws CloudException, InternalException {
        for( IPVersion v : support.listSupportedIPVersions() ) {
            if( version.equals(v) ) {
                return true;
            }
        }
        return false;
    }

    private @Nullable IpAddress requestTestAddress(@Nonnull IpAddressSupport support, @Nonnull IPVersion version) throws CloudException, InternalException {
        if( support.isRequestable(version) ) {
            addressToRelease = support.request(version);
            return support.getIpAddress(addressToRelease);
        }
        return null;
    }

    private @Nonnull IpAddressSupport getSupport() {
        if( provider == null ) {
            Assert.fail("No provider configuration set up");
        }
        NetworkServices services = provider.getNetworkServices();

        if( services == null ) {
            Assert.fail("No network services");
        }
        IpAddressSupport support = services.getIpAddressSupport();

        Assert.assertNotNull("No IP address support", support);
        return support;
    }

    @Before
    @Override
    public void setUp() throws CloudException, InternalException, InstantiationException, IllegalAccessException, IOException {
        begin();
        provider = getProvider();
        provider.connect(getTestContext());
        IpAddressSupport support = getSupport();

        if( getName().equals(T4_ADDRESS_CONTENT) || getName().equals(T6_ADDRESS_CONTENT) ) {
            IPVersion version = (getName().equals(T4_ADDRESS_CONTENT) ? IPVersion.IPV4 : IPVersion.IPV6);

            if( isSupported(support, version) ) {
                Iterator<IpAddress> addresses = support.listIpPool(version, false).iterator();

                if( addresses.hasNext() ) {
                    testAddress = addresses.next();
                }
                if( testAddress == null ) {
                    testAddress = requestTestAddress(support, version);
                }
                if( testAddress == null ) {
                    Assert.fail("Unable to test address content due to a lack of IP addresses in the " + version + " space");
                }
            }
        }
        else if( getName().equals(T_GET_ADDRESS) ) {
            for( IPVersion version : support.listSupportedIPVersions() ) {
                Iterator<IpAddress> addresses = support.listIpPool(version, false).iterator();

                if( addresses.hasNext() ) {
                    testAddress = addresses.next();
                    break;
                }
            }
            if( testAddress == null ) {
                for( IPVersion version : support.listSupportedIPVersions() ) {
                    if( support.isRequestable(version) ) {
                        testAddress = requestTestAddress(support, version);
                        break;
                    }
                }
                if( testAddress == null ) {
                    Assert.fail("Unable to test address content due to a lack of IP addresses for testing");
                }
            }
        }
        else if( getName().equals(T4_RELEASE_ADDRESS) || getName().equals(T6_RELEASE_ADDRESS) ) {
            IPVersion version = (getName().equals(T4_RELEASE_ADDRESS) ? IPVersion.IPV4 : IPVersion.IPV6);

            testAddress = requestTestAddress(support, version);
            if( testAddress == null && isSupported(support, version) ) {
                Assert.fail("Unable to test address content due to a lack of IP addresses in the " + version + " space");
            }
        }
        else if( getName().equals(T4_ASSIGN_ADDRESS) || getName().equals(T6_ASSIGN_ADDRESS) ) {
            createTestVm();

            IPVersion version = (getName().equals(T6_ASSIGN_ADDRESS) ? IPVersion.IPV6 : IPVersion.IPV4);

            if( isSupported(support, version) ) {
                Iterator<IpAddress> addresses = support.listIpPool(version, true).iterator();

                if( addresses.hasNext() ) {
                    testAddress = addresses.next();
                }
                if( testAddress == null ) {
                    testAddress = requestTestAddress(support, version);
                }
                if( testAddress == null ) {
                    Assert.fail("Unable to test address assignment due to a lack of IP addresses in the " + version + " space");
                }
                if( testVm == null && getSupport().isAssigned(version) ) {
                    Assert.fail("IP address support indicates " + version + " addresses are assigned, but there's no VM support");
                }

            }
        }
        else if( getName().equals(T_UNASSIGN_ADDRESS) ) {
            createTestVm();

            for( IPVersion version : support.listSupportedIPVersions() ) {
                if( support.isAssigned(version) ) {
                    Iterator<IpAddress> addresses = support.listIpPool(version, true).iterator();

                    if( addresses.hasNext() ) {
                        testAddress = addresses.next();
                    }
                    if( testAddress == null ) {
                        testAddress = requestTestAddress(support, version);
                    }
                    if( testAddress == null ) {
                        Assert.fail("Unable to test address de-assignment due to a lack of IP addresses in the " + version + " space");
                    }
                    if( testVm == null ) {
                        Assert.fail("IP address support indicates " + version + " addresses are assigned, but there's no VM support");
                    }
                    support.assign(testAddress.getProviderIpAddressId(), testVm.getProviderVirtualMachineId());
                    long timeout = System.currentTimeMillis() + getStateChangeWindow();
                    IpAddress address = testAddress;
                    VirtualMachine vm = testVm;

                    while( timeout > System.currentTimeMillis() ) {
                        try { address = support.getIpAddress(testAddress.getProviderIpAddressId()); }
                        catch( Throwable ignore ) { }
                        try {
                            //noinspection ConstantConditions
                            vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(testVm.getProviderVirtualMachineId());
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                        if( address == null || vm == null ) {
                            Assert.fail("Address or VM disappeared during setup");
                        }
                        if( address.getProviderIpAddressId().equals(vm.getProviderAssignedIpAddressId()) && vm.getProviderVirtualMachineId().equals(address.getServerId()) ) {
                            break;
                        }
                        try { Thread.sleep(20000L); }
                        catch( InterruptedException e ) { }
                    }
                    break;
                }
            }
        }
        else if( getName().equals(T4_LIST_RULES) || getName().equals(T6_LIST_RULES) ) {
            createTestVm();

            IPVersion version = (getName().equals(T4_LIST_RULES) ? IPVersion.IPV4 : IPVersion.IPV6);

            if( isSupported(support, version) ) {
                if( support.isForwarding(version) ) {
                    testAddress = requestTestAddress(support, version);
                    if( testAddress == null ) {
                        Assert.fail("Unable to test address forwarding due to a lack of IP addresses in the " + version + " space");
                    }
                    if( testVm != null ) {
                        try {
                            support.forward(testAddress.getProviderIpAddressId(), 9090, Protocol.TCP, 8080, testVm.getProviderVirtualMachineId());
                        }
                        catch( Throwable ignore ) {
                            out("Warning: Won't be able to properly test IP forwarding rules due to lack of ability to forward");
                        }
                    }
                }
                else {
                    Iterator<IpAddress> addresses = support.listIpPool(version, false).iterator();

                    if( addresses.hasNext() ) {
                        testAddress = addresses.next();
                    }
                    if( testAddress == null ) {
                        Assert.fail("Cannot run a proper IP forwarding rule list test with no address");
                    }
                }
            }
        }
        else if( getName().equals(T4_FORWARD) || getName().equals(T6_FORWARD) ) {
            createTestVm();

            IPVersion version = (getName().equals(T4_FORWARD) ? IPVersion.IPV4 : IPVersion.IPV6);

            testAddress = requestTestAddress(support, version);
            if( testAddress == null && support.isForwarding(version) ) {
                Assert.fail("Cannot test IP forwarding for " + version + " due to lack of test address");
            }
        }
        else if( getName().equals(T4_STOP_FORWARD) || getName().equals(T6_STOP_FORWARD) ) {
            createTestVm();

            IPVersion version = (getName().equals(T4_STOP_FORWARD) ? IPVersion.IPV4 : IPVersion.IPV6);

            if( isSupported(support, version) ) {
                testAddress = requestTestAddress(support, version);
                if( support.isForwarding(version) ) {
                    if( testAddress == null ) {
                        Assert.fail("Unable to test address stopping forwarding due to a lack of IP addresses in the " + version + " space");
                    }
                    if( testVm == null ) {
                        Assert.fail("Cannot create a forwarding rule to be removed due to lack of VM");
                    }
                    try {
                        testRuleId = support.forward(testAddress.getProviderIpAddressId(), 9090, Protocol.TCP, 8080, testVm.getProviderVirtualMachineId());
                        Assert.assertNotNull("Could not establish a test rule to remove", testRuleId);
                    }
                    catch( Throwable ignore ) {
                        out("Warning: Won't be able to properly test IP forwarding rules due to lack of ability to forward");
                    }
                }
            }
        }
    }

    @After
    @Override
    public void tearDown() {
        try {
            if( addressToRelease != null ) {
                try {
                    IpAddress address;

                    try {
                        address = getSupport().getIpAddress(addressToRelease);
                        if( address != null ) {
                            if( address.getServerId() != null ) {
                                getSupport().releaseFromServer(addressToRelease);
                            }
                            long timeout = System.currentTimeMillis() + getStateChangeWindow();

                            while( timeout > System.currentTimeMillis() ) {
                                try { address = getSupport().getIpAddress(addressToRelease); }
                                catch( Throwable ignore ) { }
                                if( address == null || address.getServerId() == null ) {
                                    break;
                                }
                                try { Thread.sleep(15000L); }
                                catch( InterruptedException e ) { }
                            }
                            if( address != null ) {
                                getSupport().releaseFromPool(addressToRelease);
                            }
                        }
                    }
                    catch( Throwable e ) {
                        out("WARNING: Error tearing down addresses: " + e.getMessage());
                    }
                }
                finally {
                    addressToRelease = null;
                    testAddress = null;
                }
            }
            if( testAddress != null && testVm != null ) {
                // this was a previously allocated IP address and not one created for tests
                // if it was used in a test though, we want to remove any assignment
                try {
                    IpAddress address = getSupport().getIpAddress(testAddress.getProviderIpAddressId());

                    if( address != null && address.isAssigned() && testVm.getProviderVirtualMachineId().equals(address.getServerId()) ) {
                        getSupport().releaseFromServer(address.getProviderIpAddressId());
                    }
                }
                catch( Throwable e ) {
                    out("WARNING: Error unassigning IP addresses: " + e.getMessage());
                }
            }
            if( testAddress != null ) {
                testAddress = null;
            }
            if( vmUse >= NEEDS_VMS && testVm != null ) {
                try {
                    VirtualMachine vm;

                    try {
                        VirtualMachineSupport vmSupport = provider.getComputeServices().getVirtualMachineSupport();

                        vm = vmSupport.getVirtualMachine(testVm.getProviderVirtualMachineId());
                        if( vm != null ) {
                            vmSupport.terminate(vm.getProviderVirtualMachineId());
                        }
                    }
                    catch( Throwable e ) {
                        out("WARNING: Error tearing down virtual machine: " + e.getMessage());
                    }
                }
                finally {
                    testVm = null;
                }
            }
            testRuleId = null;
        }
        finally {
            end();
        }
    }

    @Test
    public void testMetaData() throws CloudException, InternalException {
        IpAddressSupport support = getSupport();
        Iterable<IPVersion> versions = support.listSupportedIPVersions();

        out("Subscribed: " + support.isSubscribed());
        out("IP address term: " + support.getProviderTermForIpAddress(Locale.getDefault()));
        out("IP versions: " + versions);
        Assert.assertNotNull("IP address term may not be null", support.getProviderTermForIpAddress(Locale.getDefault()));
        Assert.assertNotNull("IP address versions may not be null", versions);
        Assert.assertTrue("At least one IP address version should be supported", versions.iterator().hasNext());
        for( IPVersion version : versions ) {
            out("Is requestable [" + version + "]: " + support.isRequestable(version));
            out("Is assigned [" + version + "]: " + support.isAssigned(version));
            out("Is forwarding [" + version + "]: " + support.isForwarding(version));
            out("Supports VLAN addresses [" + version + "]: " + support.supportsVLANAddresses(version));
        }
    }

    private void list(IPVersion version, boolean unassignedOnly) throws CloudException, InternalException {
        IpAddressSupport support = getSupport();
        boolean supported = false;

        for( IPVersion v : support.listSupportedIPVersions() ) {
            if( v.equals(version) ) {
                supported = true;
                break;
            }
        }
        if( !supported ) {
            Iterable<IpAddress> addresses = support.listIpPool(version, unassignedOnly);

            //noinspection ConstantConditions
            Assert.assertTrue("An unsupported protocol should return an empty list", addresses != null && !addresses.iterator().hasNext());
        }
        else {
            Iterable<IpAddress> addresses = support.listIpPool(version, unassignedOnly);
            boolean found = false;

            for( IpAddress address : addresses ) {
                out(version + " address: " + address);
                assertVersion(address, version);
                if( unassignedOnly ) {
                    Assert.assertTrue("Found an assigned IP address among the unassigned", !address.isAssigned() && address.getServerId() == null && address.getProviderLoadBalancerId() == null && address.getProviderNetworkInterfaceId() == null);
                }
                found = true;
            }
            if( !found ) {
                out("There were no " + version + " addresses in the region; this test may not be valid");
            }
        }
    }

    @Test
    public void test4ListAddresses() throws CloudException, InternalException {
        list(IPVersion.IPV4, false);
    }

    @Test
    public void test6ListAddresses() throws CloudException, InternalException {
        list(IPVersion.IPV6, false);
    }

    @Test
    public void test4ListAvailableAddresses() throws CloudException, InternalException {
        list(IPVersion.IPV4, true);
    }

    @Test
    public void test6ListAvailableAddresses() throws CloudException, InternalException {
        list(IPVersion.IPV6, true);
    }

    private void content(IPVersion version) throws CloudException, InternalException {
        if( isSupported(getSupport(), version) ) {
            out("ID:              " + testAddress.getProviderIpAddressId());
            out("Address:         " + testAddress.getAddress());
            out("Version:         " + testAddress.getVersion());
            out("Assigned:        " + testAddress.isAssigned());
            out("For VLAN:        " + testAddress.isForVlan());
            out("Region:          " + testAddress.getRegionId());
            out("Virtual machine: " + testAddress.getServerId());
            out("Load balancer:   " + testAddress.getProviderLoadBalancerId());
            out("NIC:             " + testAddress.getProviderNetworkInterfaceId());
            Assert.assertNotNull("IP address ID may not be null", testAddress.getProviderIpAddressId());
            Assert.assertNotNull("Region may not be null", testAddress.getRegionId());
            Assert.assertNotNull("Address may not be null for IP", testAddress.getAddress());
            assertVersion(testAddress, version);
            if( testAddress.isAssigned() ) {
                Assert.assertTrue("For an assigned IP address, one of load balancer, NIC, or server must not be null", testAddress.getServerId() != null || testAddress.getProviderLoadBalancerId() != null || testAddress.getProviderNetworkInterfaceId() != null);
            }
            else {
                Assert.assertTrue("For an unassigned IP address, load balancer, NIC, and server must all be null", testAddress.getServerId() == null && testAddress.getProviderLoadBalancerId() == null && testAddress.getProviderNetworkInterfaceId() == null);
            }
        }
        else {
            out(version + " not supported (OK)");
        }
    }

    @Test
    public void test4AddressContent() throws CloudException, InternalException {
        content(IPVersion.IPV4);
    }

    @Test
    public void test6AddressContent() throws CloudException, InternalException {
        content(IPVersion.IPV6);
    }

    @Test
    public void testGetAddress() throws CloudException, InternalException {
        IpAddress address = getSupport().getIpAddress(testAddress.getProviderIpAddressId());

        out("IP Address: " + address);
        Assert.assertNotNull("Target IP address does not exist", address);
    }

    @Test
    public void testGetBogusAddress() throws CloudException, InternalException {
        String id = UUID.randomUUID().toString();
        IpAddress address = getSupport().getIpAddress(id);

        out("Bogus Address [" + id + "]: " + address);
        Assert.assertNull("Found an IP address matching the random ID: " + id, address);
    }

    private void request(IPVersion version, boolean forVlan) throws CloudException, InternalException {
        IpAddressSupport support = getSupport();

        if( forVlan && support.supportsVLANAddresses(version) ) {
            addressToRelease = support.requestForVLAN(version);
            out("Requested [" + version + "]: " + addressToRelease);

            IpAddress address = support.getIpAddress(addressToRelease);

            Assert.assertNotNull("Did not find the newly allocated address " + addressToRelease + " in the cloud", address);
            assertVersion(address, version);
            Assert.assertTrue("Address does not indicate it is for a VLAN", address.isForVlan());
        }
        else if( forVlan ) {
            try {
                addressToRelease = support.requestForVLAN(version);
                Assert.fail("No exception was thrown when attempting to request an IP address from a VLAN when this functionality is not supported.");
            }
            catch( OperationNotSupportedException success ) {
                out("VLAN addresses are not supported (OK)");
            }
            catch( CloudException e ) {
                Assert.fail("Improper exception was thrown when attempting to request an IP address from a VLAN when this functionality is not supported: " + e.getMessage());
            }
            catch( InternalException e ) {
                Assert.fail("Improper exception was thrown when attempting to request an IP address from a VLAN when this functionality is not supported: " + e.getMessage());
            }
        }
        else if( support.isRequestable(version) ) {
            addressToRelease = support.request(version);
            out("Requested [" + version + "]: " + addressToRelease);

            IpAddress address = support.getIpAddress(addressToRelease);

            Assert.assertNotNull("Did not find the newly allocated address " + addressToRelease + " in the cloud", address);
            assertVersion(address, version);
        }
        else {
            try {
                addressToRelease = support.request(version);
                Assert.fail("No exception was thrown when attempting to request an IP address of the unsupported " + version + " version");
            }
            catch( OperationNotSupportedException success ) {
                out(version + " is not supported (OK)");
            }
            catch( CloudException e ) {
                Assert.fail("Improper exception for an attempt to request an unsupported IP version " + version + ": " + e.getMessage());
            }
            catch( InternalException e ) {
                Assert.fail("Improper exception for an attempt to request an unsupported IP version " + version + ": " + e.getMessage());
            }
        }
    }

    @Test
    public void test4RequestAddress() throws CloudException, InternalException {
        request(IPVersion.IPV4, false);
    }

    @Test
    public void test6RequestAddress() throws CloudException, InternalException {
        request(IPVersion.IPV6, false);
    }

    @Test
    public void test4RequestVLANAddress() throws CloudException, InternalException {
        request(IPVersion.IPV4, true);
    }

    @Test
    public void test6RequestVLANAddress() throws CloudException, InternalException {
        request(IPVersion.IPV6, true);
    }

    private void release(IPVersion version) throws CloudException, InternalException {
        IpAddressSupport support = getSupport();

        if( support.isRequestable(version) ) {
            Assert.assertNotNull("No test address exists for release test", testAddress);
            support.releaseFromPool(testAddress.getProviderIpAddressId());
            testAddress = support.getIpAddress(testAddress.getProviderIpAddressId());
            out("Released: " + (testAddress == null));
            Assert.assertNull("IP address " + testAddress + " still exists after release", testAddress);
        }
        else {
            out(version + " is not supported (OK)");
        }
    }

    @Test
    public void test4ReleaseAddress() throws CloudException, InternalException {
        release(IPVersion.IPV4);
    }

    @Test
    public void test6ReleaseAddress() throws CloudException, InternalException {
        release(IPVersion.IPV6);
    }

    private void assign(IPVersion version) throws CloudException, InternalException {
        IpAddressSupport support = getSupport();

        if( support.isAssigned(version) ) {
            support.assign(testAddress.getProviderIpAddressId(), testVm.getProviderVirtualMachineId());
            long timeout = System.currentTimeMillis() + getStateChangeWindow();
            VirtualMachine server = testVm;
            IpAddress address = testAddress;
            boolean vm = false, ip = false;

            while( timeout > System.currentTimeMillis() ) {
                try {
                    //noinspection ConstantConditions
                    server = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(testVm.getProviderVirtualMachineId());
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try { address = support.getIpAddress(testAddress.getProviderIpAddressId()); }
                catch( Throwable ignore ) { }
                Assert.assertNotNull("Virtual machine disappeared while waiting for assignment to be reflected", server);
                Assert.assertNotNull("IP address disappeared while waiting for assignment to be reflected", address);
                if( !vm && testAddress.getProviderIpAddressId().equals(server.getProviderAssignedIpAddressId()) ) {
                    vm = true;
                    out("Virtual machine assignment: " + server.getProviderAssignedIpAddressId());
                }
                if( !ip && testVm.getProviderVirtualMachineId().equals(address.getServerId()) ) {
                    ip = true;
                    out("IP address assignment: " + address.getServerId());
                }
                if( vm && ip ) {
                    break;
                }
            }
            if( !vm || !ip ) {
                Assert.fail("System timed out before IP assignment reflected in both VM and IP address");
            }
        }
        else if( isSupported(support, version) ) {
            try {
                support.assign(testAddress.getProviderIpAddressId(), testVm.getProviderVirtualMachineId());
                Assert.fail("No error was returned even though assignment of " + version + " is not supported");
            }
            catch( OperationNotSupportedException e ) {
                out("Attempt to assign address of version " + version + " not supported (OK)");
            }
            catch( CloudException e ) {
                Assert.fail("Invalid error type CloudException from unsupported operation: " + e.getMessage());
            }
            catch( InternalException e ) {
                Assert.fail("Invalid error type InternalException from unsupported operation: " + e.getMessage());
            }
        }
        else {
            out("No support for " + version + " (OK)");
        }
    }

    @Test
    public void test4AssignAddress() throws CloudException, InternalException {
        assign(IPVersion.IPV4);
    }

    @Test
    public void test6AssignAddress() throws CloudException, InternalException {
        assign(IPVersion.IPV6);
    }

    @Test
    public void testUnassignAddress() throws CloudException, InternalException {
        IpAddressSupport support = getSupport();

        if( testAddress == null ) {
            for( IPVersion version : support.listSupportedIPVersions() ) {
                if( support.isAssigned(version) ) {
                    Assert.fail(version + " supports IP assignment, but no test is available");
                }
            }
            out("IP address assignment is not supported (OK)");
        }
        else {
            support.releaseFromServer(testAddress.getProviderIpAddressId());
            long timeout = System.currentTimeMillis() + getStateChangeWindow();
            boolean vok = false, iok = false;
            IpAddress address = testAddress;
            VirtualMachine vm = testVm;

            while( timeout > System.currentTimeMillis() ) {
                try { address = support.getIpAddress(testAddress.getProviderIpAddressId()); }
                catch( Throwable ignore ) { }
                try {
                    //noinspection ConstantConditions
                    vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(testVm.getProviderVirtualMachineId());
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                Assert.assertNotNull("Virtual machine disappeared while waiting for de-assignment to be reflected", vm);
                Assert.assertNotNull("IP address disappeared while waiting for de-assignment to be reflected", address);
                if( !vok && vm.getProviderAssignedIpAddressId() == null ) {
                    vok = true;
                    out("Virtual machine assignment: " + vm.getProviderAssignedIpAddressId());
                }
                if( !iok && address.getServerId() == null ) {
                    iok = true;
                    out("IP address assignment: " + address.getServerId());
                }
                if( iok && vok ) {
                    break;
                }
            }
            if( !iok || !vok ) {
                Assert.fail("System timed out before IP de-assignment reflected in both VM and IP address");
            }
        }
    }

    private void listRules(IPVersion version) throws CloudException, InternalException {
        IpAddressSupport support = getSupport();

        if( support.isForwarding(version) ) {
            Iterable<IpForwardingRule> rules = support.listRules(testAddress.getProviderIpAddressId());
            boolean found = false;

            for( IpForwardingRule rule : rules ) {
                found = true;
                out("Rule: " + rule);
            }
            if( !found ) {
                out("Call to check IP rules succeeded, but there were no rules to validate");
            }
        }
        else if( isSupported(support, version) ) {
            Iterable<IpForwardingRule> rules = support.listRules(testAddress.getProviderIpAddressId());

            //noinspection ConstantConditions
            Assert.assertTrue("Found rules associated with " + testAddress, rules != null && !rules.iterator().hasNext());
        }
        else {
            out(version + " not supported (OK)");
        }
    }

    @Test
    public void test4ListForwardingRules() throws CloudException, InternalException {
        listRules(IPVersion.IPV4);
    }

    @Test
    public void test6ListForwardingRules() throws CloudException, InternalException {
        listRules(IPVersion.IPV6);
    }

    private void forward(@Nonnull IPVersion version) throws CloudException, InternalException {
        IpAddressSupport support = getSupport();

        if( support.isForwarding(version) ) {
            Assert.assertNotNull("Test address is null", testAddress);
            Assert.assertNotNull("Test VM is null", testVm);

            String ruleId = support.forward(testAddress.getProviderIpAddressId(), 9095, Protocol.TCP, 8085, testVm.getProviderVirtualMachineId());

            out("New rule: " + ruleId);
            Assert.assertNotNull("Forwarding must provide a rule ID", ruleId);

            boolean found = false;

            for( IpForwardingRule rule : support.listRules(testAddress.getProviderIpAddressId()) ) {
                if( ruleId.equals(rule.getProviderRuleId()) ) {
                    Assert.assertTrue("Matching rule does not match address", testAddress.getProviderIpAddressId().equals(rule.getAddressId()));
                    Assert.assertTrue("Matching rule does not match virtual machine", testVm.getProviderVirtualMachineId().equals(rule.getServerId()));
                    Assert.assertTrue("Public ports do not match", rule.getPublicPort() == 9095);
                    Assert.assertTrue("Private ports do not match", rule.getPrivatePort() == 8085);
                    Assert.assertTrue("Protocols do not match", Protocol.TCP.equals(rule.getProtocol()));
                    found = true;
                }
            }
            Assert.assertTrue("Did not find the newly created rule", found);
        }
    }

    @Test
    public void test4Forward() throws CloudException, InternalException {
        forward(IPVersion.IPV4);
    }

    @Test
    public void test6Forward() throws CloudException, InternalException {
        forward(IPVersion.IPV6);
    }

    private void stopForwarding(IPVersion version) throws CloudException, InternalException {
        IpAddressSupport support = getSupport();

        if( support.isForwarding(version) ) {
            support.stopForward(testRuleId);
            for( IpForwardingRule rule : support.listRules(testAddress.getProviderIpAddressId()) ) {
                if( testRuleId.equals(rule.getProviderRuleId()) ) {
                    Assert.fail("Target rule still exists among forwarding rules");
                }
            }
            out("Stopped forwarding");
        }
        else {
            out(version + " forwarding is not supported (OK)");
        }
    }

    @Test
    public void test4StopForward() throws CloudException, InternalException {
        stopForwarding(IPVersion.IPV4);
    }

    @Test
    public void test6StopForward() throws CloudException, InternalException {
        stopForwarding(IPVersion.IPV6);
    }
}
