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
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.IpForwardingRule;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.util.APITrace;
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
    static public final String T4_REQUEST_VLAN     = "test4RequestVLANAddress";
    static public final String T6_REQUEST_VLAN     = "test6RequestVLANAddress";
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

    static private final String[] NEEDS_VLANS = { T4_REQUEST_VLAN, T6_REQUEST_VLAN };

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
    private String         testVlan         = null;

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

    @Override
    public int getVlanReuseCount() {
        return NEEDS_VLANS.length;
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

        for( String test : NEEDS_VLANS ) {
            if( getName().equals(test) ) {
                VLAN v = findTestVLAN(provider, provider.getNetworkServices().getVlanSupport(), true, true);

                testVlan = (v == null ? null : v.getProviderVlanId());
                if( testVlan == null ) {
                    boolean required = false;

                    for( IPVersion version : IPVersion.values() ) {
                        if( getSupport().supportsVLANAddresses(version) ) {
                            required = true;
                            break;
                        }
                    }
                    if( required ) {
                        Assert.fail("Did not find or provisionVM a test VLAN as required for test");
                    }
                }
            }
        }
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
        else if( getName().equals(T4_FORWARD) || getName().equals(T6_FORWARD) || getName().equals(T4_LIST_RULES) || getName().equals(T6_LIST_RULES)) {
            createTestVm();

            IPVersion version = ((getName().equals(T4_FORWARD) ||getName().equals(T4_LIST_RULES)) ? IPVersion.IPV4 : IPVersion.IPV6);

            testAddress = requestTestAddress(support, version);
            if( testAddress == null && support.isForwarding(version) ) {
                Assert.fail("Cannot test IP forwarding for " + version + " due to lack of test address");
            }
            if( getName().equals(T4_LIST_RULES) || getName().equals(T6_LIST_RULES) ) {
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
            cleanUp(provider);
            testVlan = null;
            APITrace.report(getName());
            APITrace.reset();
            try {
                if( provider != null ) {
                    provider.close();
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
