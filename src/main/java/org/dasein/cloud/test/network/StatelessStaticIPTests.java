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
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.IpForwardingRule;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Implements testing of stateless elements in Dasein Cloud IP address support.
 * <p>Created by George Reese: 2/24/13 5:10 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatelessStaticIPTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessStaticIPTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    static public void assertVersion(IpAddress address, IPVersion version) {
        String ip = address.getRawAddress().getIpAddress();

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

    @Rule
    public final TestName name = new TestName();

    private String testIpAddress;

    public StatelessStaticIPTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        if( name.getMethodName().contains("IPv6") ) {
            testIpAddress = tm.getTestStaticIpId(DaseinTestManager.STATELESS, false, IPVersion.IPV6, false, null);
            if( testIpAddress == null ) {
                testIpAddress = tm.getTestStaticIpId(DaseinTestManager.STATELESS, false, IPVersion.IPV6, true, null);
            }
        }
        else {
            testIpAddress = tm.getTestStaticIpId(DaseinTestManager.STATELESS, false, IPVersion.IPV4, false, null);
            if( testIpAddress == null ) {
                testIpAddress = tm.getTestStaticIpId(DaseinTestManager.STATELESS, false, IPVersion.IPV4, true, null);
            }
        }
    }

    @After
    public void after() {
        tm.end();
    }

    private void assertContent(@Nonnull IpAddress address, @Nonnull IPVersion expectedVersion, boolean unassignedOnly) {
        assertNotNull("The IP address version must not be null", address.getVersion());
        assertEquals("The IP address version does not match the expected version", expectedVersion, address.getVersion());
        assertVersion(address, expectedVersion);
        assertNotNull("The IP address ID may not be null", address.getProviderIpAddressId());
        assertNotNull("The region ID associated with the IP address may not be null", address.getRegionId());
        assertNotNull("Address type may not be null", address.getAddressType());
        if( unassignedOnly ) {
            assertFalse("Expected only available addresses", address.isAssigned());
        }
        if( address.isAssigned() ) {
            assertTrue("Address indicates that it is assigned, but does not reflect what it is assigned to", address.getProviderLoadBalancerId() != null || address.getServerId() != null || address.getProviderNetworkInterfaceId() != null);
        }
        else {
            assertTrue("The address indicates that it is unassigned, but it shows itself as being attached to something", address.getProviderLoadBalancerId() == null && address.getServerId() == null && address.getProviderNetworkInterfaceId() == null);
        }
        assertNotNull("The raw address for the IP address may not be null", address.getRawAddress());
        //noinspection deprecation
        assertEquals("The deprecated getAddress() does not match the new getRawAddress().getIpAddress()", address.getRawAddress().getIpAddress(), address.getAddress());
    }

    private void list(IPVersion version, boolean unassignedOnly) throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();
        IpAddressSupport support = (services == null ? null : services.getIpAddressSupport());
        boolean supported = false;

        if( support == null ) {
            tm.ok("Static IP address services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        for( IPVersion v : support.getCapabilities().listSupportedIPVersions() ) {
            if( v.equals(version) ) {
                supported = true;
                break;
            }
        }
        Iterable<IpAddress> addresses = support.listIpPool(version, unassignedOnly);
        int count = 0;

        for( IpAddress address : addresses ) {
            count++;
            tm.out(version + " Address", address);
        }
        tm.out("Total " + (unassignedOnly ? "Available " : "") + version + " Addresses", count);
        if( !supported ) {
            assertEquals("An unsupported protocol should return an empty list", 0, count);
        }
        else {
            for( IpAddress address : addresses ) {
                assertContent(address, version, unassignedOnly);
            }
            if( count < 1 ) {
                tm.warn("There were no " + version + " addresses in the region; this test may not be valid");
            }
        }
    }

    private void listStatus(IPVersion version) throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();
        IpAddressSupport support = (services == null ? null : services.getIpAddressSupport());
        boolean supported = false;

        if( support == null ) {
            tm.ok("Static IP address services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        for( IPVersion v : support.getCapabilities().listSupportedIPVersions() ) {
            if( v.equals(version) ) {
                supported = true;
                break;
            }
        }
        Iterable<ResourceStatus> addresses = support.listIpPoolStatus(version);
        int count = 0;

        for( ResourceStatus address : addresses ) {
            count++;
            tm.out(version + " Address Status", address);
        }
        tm.out("Total IP Address Status", count);
        if( !supported ) {
            assertEquals("An unsupported protocol should return an empty list", 0, count);
        }
        else {
            if( count < 1 ) {
                tm.warn("There were no " + version + " addresses in the region; this test may not be valid");
            }
        }
    }

    private void compareStatus(IPVersion version) throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            IpAddressSupport support = services.getIpAddressSupport();

            if( support != null ) {
                HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
                Iterable<IpAddress> addresses = support.listIpPool(version, false);
                Iterable<ResourceStatus> status = support.listIpPoolStatus(version);

                assertNotNull("listipPool() must return at least an empty collections and may not be null", addresses);
                assertNotNull("listIpPoolStatus() must return at least an empty collection and may not be null", status);
                for( ResourceStatus s : status ) {
                    Map<String,Boolean> current = map.get(s.getProviderResourceId());

                    if( current == null ) {
                        current = new HashMap<String, Boolean>();
                        map.put(s.getProviderResourceId(), current);
                    }
                    current.put("status", true);
                }
                for( IpAddress address : addresses ) {
                    Map<String,Boolean> current = map.get(address.getProviderIpAddressId());

                    if( current == null ) {
                        current = new HashMap<String, Boolean>();
                        map.put(address.getProviderIpAddressId(), current);
                    }
                    current.put("address", true);
                }
                for( Map.Entry<String,Map<String,Boolean>> entry : map.entrySet() ) {
                    Boolean s = entry.getValue().get("status");
                    Boolean a = entry.getValue().get("address");

                    assertTrue("Status and IP address lists do not match for " + entry.getKey(), s != null && a != null && s && a);
                }
                tm.out("Matches");
            }
            else {
                tm.ok("No IP address support in this cloud");
            }
        }
        else {
            tm.ok("No network services in this cloud");
        }
    }
    @Test
    public void checkMetaData() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IpAddressSupport support = services.getIpAddressSupport();

        if( support == null ) {
            tm.ok("Static IP addresses are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        tm.out("Subscribed", support.isSubscribed());
        tm.out("Term for Static IP", support.getCapabilities().getProviderTermForIpAddress(Locale.getDefault()));
        tm.out("Specify VLAN for VLAN IP", support.getCapabilities().identifyVlanForVlanIPRequirement());

        Iterable<IPVersion> versions = support.getCapabilities().listSupportedIPVersions();

        tm.out("IP Versions", versions);

        for( IPVersion v : IPVersion.values() ) {
            tm.out("Requestable [" + v + "]", support.getCapabilities().isRequestable(v));
            tm.out("Assignable [" + v + "]", support.getCapabilities().isAssigned(v));
            tm.out("Assignable Post-launch [" + v + "]", support.getCapabilities().isAssignablePostLaunch(v));
            tm.out("Forwarding [" + v + "]", support.getCapabilities().isForwarding(v));
            tm.out("VLAN Addresses [" + v + "]", support.getCapabilities().supportsVLANAddresses(v));
        }
        assertNotNull("IP versions may not be null", versions);
        assertTrue("Static IP address support must provide support for at least one IP version", versions.iterator().hasNext());
    }

    @Test
    public void getBogusAddress() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IpAddressSupport support = services.getIpAddressSupport();

        if( support == null ) {
            tm.ok("Static IP addresses are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IpAddress address = support.getIpAddress(UUID.randomUUID().toString());

        tm.out("Bogus Address", address);
        assertNull("Found an address matching the bogus IP address ID", address);
    }

    @Test
    public void getAddress() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IpAddressSupport support = services.getIpAddressSupport();

        if( support == null ) {
            tm.ok("Static IP addresses are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IpAddress address = support.getIpAddress(testIpAddress);

        tm.out("Address", address);
        assertNotNull("No IP address was found matching the test IP address ID " + testIpAddress, address);
    }

    @Test
    public void IPv4Content() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IpAddressSupport support = services.getIpAddressSupport();

        if( support == null ) {
            tm.ok("Static IP addresses are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        boolean supported = false;

        for( IPVersion v : support.getCapabilities().listSupportedIPVersions() ) {
            if( v.equals(IPVersion.IPV4) ) {
                supported = true;
            }
        }
        if( testIpAddress == null ) {
            assertFalse("IPv4 is supported, but there is not test IP address for this test", supported);
            tm.ok("No support for IPv4 in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IpAddress address = support.getIpAddress(testIpAddress);

        assertNotNull("No IP address was found matching the test IP address ID " + testIpAddress, address);

        tm.out("Address ID", address.getProviderIpAddressId());
        tm.out("Assigned", address.isAssigned());
        tm.out("Reserved", address.isReserved());
        tm.out("Region ID", address.getRegionId());
        tm.out("Address", address.getRawAddress());
        tm.out("For VLAN", address.isForVlan());
        tm.out("VLAN", address.getProviderVlanId());
        tm.out("Virtual Machine ID", address.getServerId());
        tm.out("Load Balancer ID", address.getProviderLoadBalancerId());
        tm.out("NIC ID", address.getProviderNetworkInterfaceId());
        tm.out("Address Type", address.getAddressType());
        tm.out("IP Version", address.getVersion());
        assertContent(address, IPVersion.IPV4, false);
    }

    @Test
    public void IPv6Content() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IpAddressSupport support = services.getIpAddressSupport();

        if( support == null ) {
            tm.ok("Static IP addresses are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        boolean supported = false;

        for( IPVersion v : support.getCapabilities().listSupportedIPVersions() ) {
            if( v.equals(IPVersion.IPV6) ) {
                supported = true;
            }
        }
        if( testIpAddress == null ) {
            assertFalse("IPv6 is supported, but there is not test IP address for this test", supported);
            tm.ok("No support for IPv6 in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IpAddress address = support.getIpAddress(testIpAddress);

        assertNotNull("No IP address was found matching the test IP address ID " + testIpAddress, address);

        tm.out("Address ID", address.getProviderIpAddressId());
        tm.out("Assigned", address.isAssigned());
        tm.out("Reserved", address.isReserved());
        tm.out("Region ID", address.getRegionId());
        tm.out("Address", address.getRawAddress());
        tm.out("For VLAN", address.isForVlan());
        tm.out("VLAN", address.getProviderVlanId());
        tm.out("Virtual Machine ID", address.getServerId());
        tm.out("Load Balancer ID", address.getProviderLoadBalancerId());
        tm.out("NIC ID", address.getProviderNetworkInterfaceId());
        tm.out("Address Type", address.getAddressType());
        tm.out("IP Version", address.getVersion());
        assertContent(address, IPVersion.IPV6, false);
    }

    @Test
    public void listAllIPv4() throws CloudException, InternalException {
        list(IPVersion.IPV4, false);
    }

    @Test
    public void listAllIPv6() throws CloudException, InternalException {
        list(IPVersion.IPV6, false);
    }

    @Test
    public void listAvailableIPv4() throws CloudException, InternalException {
        list(IPVersion.IPV4, true);
    }

    @Test
    public void listAvailableIPv6() throws CloudException, InternalException {
        list(IPVersion.IPV6, true);
    }

    @Test
    public void listIPv4Status() throws CloudException, InternalException {
        listStatus(IPVersion.IPV4);
    }

    @Test
    public void listIPv6Status() throws CloudException, InternalException {
        listStatus(IPVersion.IPV6);
    }

    @Test
    public void compareIPv4ListAndStatus() throws CloudException, InternalException {
        compareStatus(IPVersion.IPV4);
    }

    @Test
    public void compareIPv6ListAndStatus() throws CloudException, InternalException {
        compareStatus(IPVersion.IPV6);
    }

    private void listRules(@Nonnull IPVersion version) throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IpAddressSupport support = services.getIpAddressSupport();

        if( support == null ) {
            tm.ok("Static IP addresses are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testIpAddress != null ) {
            Iterable<IpForwardingRule> rules = support.listRules(testIpAddress);
            int count = 0;

            for( IpForwardingRule rule : rules ) {
                count++;
                tm.out("Rule", rule);
            }
            tm.out("Total Rule Count", count);
            if( count < 1 ) {
                if( support.getCapabilities().isForwarding(version) ) {
                    tm.warn("No rules were found for the test address " + testIpAddress + " so this test is likely invalid");
                }
                else {
                    tm.ok("No rules were found in a cloud that doesn't support " + version + " forwarding");
                }
            }
            else if( !support.getCapabilities().isForwarding(version) ) {
                fail("At least one IP forwarding rule was included for a cloud in which " + version + " forwarding is not supported");
            }
        }
        else if( support.getCapabilities().isForwarding(version) ) {
            fail("No test IP address exists for testing forwarding rules for " + version);
        }
        else {
            tm.ok("Forwarding for " + version + " is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
        }
    }

    @Test
    public void listIPv4ForwardingRules() throws CloudException, InternalException {
        listRules(IPVersion.IPV4);
    }

    @Test
    public void listIPv6ForwardingRules() throws CloudException, InternalException {
        listRules(IPVersion.IPV6);
    }
}
