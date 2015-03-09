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
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.network.*;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.*;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/21/13 1:15 PM</p>
 *
 * @author George Reese
 */
public class StatelessVLANTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessVLANTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testSubnetId;
    private String testInternetGatewayId;
    private String testVLANId;
    private String testRoutingTableId;

    public StatelessVLANTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testVLANId = tm.getTestVLANId(DaseinTestManager.STATELESS, false, null);
        if( testVLANId == null ) {
          testVLANId = tm.getTestVLANId(DaseinTestManager.STATELESS, true, null);
          try { Thread.sleep(15000L); }
          catch( InterruptedException ignore ) { }
        }
        testSubnetId = tm.getTestSubnetId(DaseinTestManager.STATELESS, false, null, null);
        if( testSubnetId == null ) {
          testSubnetId = tm.getTestSubnetId(DaseinTestManager.STATELESS, true, null, null);
          try { Thread.sleep(5000L); }
          catch( InterruptedException ignore ) { }
        }
        testInternetGatewayId = tm.getTestInternetGatewayId(DaseinTestManager.STATELESS, false, null, null);
        if( testInternetGatewayId == null ) {
          testInternetGatewayId = tm.getTestInternetGatewayId(DaseinTestManager.STATELESS, true, null, null);
          try { Thread.sleep(5000L); }
          catch( InterruptedException ignore ) { }
        }
        testRoutingTableId = tm.getTestRoutingTableId(DaseinTestManager.STATELESS, false, null, null);
        if( testRoutingTableId == null ) {
          testRoutingTableId = tm.getTestRoutingTableId(DaseinTestManager.STATELESS, true, null, null);
          try { Thread.sleep(5000L); }
          catch( InterruptedException ignore ) { }
        }
    }

    @After
    public void after() {
        tm.end();
    }

    private void assertSubnetContent(@Nonnull VLANSupport support, @Nonnull Subnet subnet, @Nullable String vlanId) throws CloudException, InternalException {
        assertNotNull("The subnet ID may not be null", subnet.getProviderSubnetId());
        assertNotNull("The subnet owner may not be null", subnet.getProviderOwnerId());
        assertNotNull("The subnet region ID may not be null", subnet.getProviderRegionId());
        assertNotNull("The subnet VLAN ID may not be null", subnet.getProviderVlanId());
        if( support.getCapabilities().isSubnetDataCenterConstrained() ) {
            assertNotNull("The subnet data center ID may not be null when subnets are data center constrained", subnet.getProviderDataCenterId());
        }
        assertNotNull("The subnet's current state may not be null", subnet.getCurrentState());
        assertNotNull("The subnet name may not be null", subnet.getName());
        assertNotNull("The subnet description may not be null", subnet.getDescription());
        assertEquals("The subnet in question does not belong in the current region", tm.getContext().getRegionId(), subnet.getProviderRegionId());

        IPVersion[] traffic = subnet.getSupportedTraffic();

        assertNotNull("The list of supported IP traffic may not be null", traffic);
        assertTrue("The subnet must support at least one version of IP traffic", traffic.length > 0);
        if( !support.getCapabilities().allowsMultipleTrafficTypesOverSubnet() ) {
            assertTrue("The subnet can support exactly one version of IP traffic", traffic.length == 1);
        }
        assertTrue("The count of available IP addresses must be -1 for unknown or non-negative", subnet.getAvailableIpAddresses() >= -1);
        assertNotNull("The list of allocation pools must not be null", subnet.getAllocationPools());
        assertNotNull("The subnet CIDR may not be null", subnet.getCidr());
        assertNotNull("The subnet meta-data tags may not be null", subnet.getTags());
        if( vlanId != null ) {
            assertEquals("The test VLAN ID does not match the VLAN of the subnet", vlanId, subnet.getProviderVlanId());
        }
    }

    private void assertInternetGatewayContent(@Nonnull InternetGateway internetGateway, @Nullable String vlanId) throws CloudException, InternalException {
      assertNotNull("The internet gateway ID may not be null", internetGateway.getProviderInternetGatewayId());
      assertNotNull("The internet gateway owner may not be null", internetGateway.getProviderOwnerId());
      assertNotNull("The internet gateway region ID may not be null", internetGateway.getProviderRegionId());
      assertNotNull("The internet gateway VLAN ID may not be null", internetGateway.getProviderVlanId());
      assertEquals("The internet gateway in question does not belong in the current region", tm.getContext().getRegionId(), internetGateway.getProviderRegionId());
      assertNotNull("The internet gateway state cannot be null", internetGateway.getAttachmentState());
      if( vlanId != null ) {
        assertEquals("The test VLAN ID does not match the VLAN of the internet gateway", vlanId, internetGateway.getProviderVlanId());
      }
    }

    private void assertVLANContent(@Nonnull VLANSupport support, @Nonnull VLAN network) throws CloudException, InternalException {
        assertNotNull("VLAN ID may not be null", network.getProviderVlanId());
        assertNotNull("Account owner may not be null", network.getProviderOwnerId());
        assertNotNull("Region ID may not be null", network.getProviderRegionId());
        if( support.getCapabilities().isVlanDataCenterConstrained() ) {
            assertNotNull("Data center ID may not be null when VLANs are data center constrained", network.getProviderDataCenterId());
        }
        assertNotNull("VLAN state may not be null", network.getCurrentState());
        assertNotNull("VLAN name may not be null", network.getName());
        assertNotNull("VLAN description may not be null", network.getDescription());
        assertNotNull("VLAN CIDR may not be null", network.getCidr());
        IPVersion[] traffic = network.getSupportedTraffic();

        assertNotNull("VLAN supported network traffic may not be null", traffic);
        assertTrue("The VLAN must support at least one version of IP traffic", traffic.length > 0);
        if( !support.getCapabilities().allowsMultipleTrafficTypesOverVlan() ) {
            assertTrue("The VLAN can support exactly one version of IP traffic", traffic.length == 1);
        }
        assertNotNull("VLAN DNS servers may not be null", network.getDnsServers());
        assertNotNull("VLAN NTP servers may not be null", network.getNtpServers());
        assertNotNull("VLAN meta-data tags must not be null", network.getTags());
        assertEquals("The VLAN in question does not belong in the current region", tm.getContext().getRegionId(), network.getProviderRegionId());
    }

    private void assertRouteTableContent(@Nonnull RoutingTable rtb) throws CloudException, InternalException {
      assertNotNull("Routing Table ID may not be null", rtb.getProviderRoutingTableId());
      assertNotNull("Account owner may not be null", rtb.getProviderOwnerId());
      assertNotNull("Region ID may not be null", rtb.getProviderRegionId());
      assertNotNull("Routing Table name may not be null", rtb.getName());
      assertNotNull("Routing Table description may not be null", rtb.getDescription());
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                tm.out("Subscribed", support.isSubscribed());
                tm.out("Term for VLAN", support.getCapabilities().getProviderTermForVlan(Locale.getDefault()));
                tm.out("Term for Subnet", support.getCapabilities().getProviderTermForSubnet(Locale.getDefault()));
                tm.out("Max VLAN Count", support.getCapabilities().getMaxVlanCount() == -2 ? "Unknown" : (support.getCapabilities().getMaxVlanCount() == -1 ? "Unlimited" : support.getCapabilities().getMaxVlanCount()));
                tm.out("VLAN DC Constrained", support.getCapabilities().isVlanDataCenterConstrained());
                tm.out("Subnet DC Constrained", support.getCapabilities().isSubnetDataCenterConstrained());
                tm.out("Specify DC for Subnet", support.getCapabilities().identifySubnetDCRequirement());
                tm.out("Supports Subnets", support.getCapabilities().getSubnetSupport());
                tm.out("Allows VLAN Creation", support.getCapabilities().allowsNewVlanCreation());
                tm.out("Allows Subnet Creation", support.getCapabilities().allowsNewSubnetCreation());

                Iterable<IPVersion> versions = support.getCapabilities().listSupportedIPVersions();

                tm.out("Supported Traffic", versions);

                tm.out("Multiple Traffic Types per VLAN", support.getCapabilities().allowsMultipleTrafficTypesOverVlan());
                tm.out("Multiple Traffic Types per Subnet", support.getCapabilities().allowsMultipleTrafficTypesOverSubnet());

                assertNotNull("The term for a VLAN may not be null", support.getCapabilities().getProviderTermForVlan(Locale.getDefault()));
                assertNotNull("The term for a subnet may not be null", support.getCapabilities().getProviderTermForSubnet(Locale.getDefault()));
                assertNotNull("Specify DC for subnet may not be null", support.getCapabilities().identifySubnetDCRequirement());
                assertTrue("isSubnetDataCenterConstrained() value conflicts with DC specification requirement", support.getCapabilities().isSubnetDataCenterConstrained() || support.getCapabilities().identifySubnetDCRequirement().equals(Requirement.NONE));
                assertTrue("The maximum VLAN count must be -2 (Unknown), -1 (Unlimited), or non-negative", support.getCapabilities().getMaxVlanCount() >= -2);
                assertNotNull("Subnet requirement must be non-null", support.getCapabilities().getSubnetSupport());
                if( support.getCapabilities().allowsNewSubnetCreation() ) {
                    assertTrue("The cloud allows new subnet creation, but VLAN subnet requirement is NONE", !support.getCapabilities().getSubnetSupport().equals(Requirement.NONE));
                }
                assertNotNull("The supported IP versions list may not be null", versions);
                assertTrue("There must be at least one IP version with supported traffic", versions.iterator().hasNext());
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
    public void getBogusVLAN() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                VLAN network = support.getVlan(UUID.randomUUID().toString());

                tm.out("Bogus VLAN", network);
                assertNull("Bogus network was supposed to be null, but got a valid VLAN", network);
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
    public void getVLAN() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                if( testVLANId != null ) {
                    VLAN network = support.getVlan(testVLANId);

                    tm.out("VLAN", network);
                    assertNotNull("The test VLAN was not found in the cloud", network);
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
    public void vlanContent() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                if( testVLANId != null ) {
                    VLAN network = support.getVlan(testVLANId);

                    assertNotNull("The test VLAN was not found in the cloud", network);

                    tm.out("VLAN ID", network.getProviderVlanId());
                    tm.out("Current State", network.getCurrentState());
                    tm.out("Name", network.getName());
                    tm.out("Owner Account", network.getProviderOwnerId());
                    tm.out("Region ID", network.getProviderRegionId());
                    tm.out("Data Center ID", network.getProviderDataCenterId());
                    tm.out("CIDR", network.getCidr());
                    tm.out("Network Type", network.getNetworkType());
                    tm.out("Supported Traffic", Arrays.toString(network.getSupportedTraffic()));
                    tm.out("Domain Name", network.getDomainName());
                    tm.out("DNS Servers", Arrays.toString(network.getDnsServers()));
                    tm.out("NTP Servers", Arrays.toString(network.getNtpServers()));
                    tm.out("Internet Gateway", support.isConnectedViaInternetGateway(testVLANId));

                    Map<String,String> tags = network.getTags();

                    //noinspection ConstantConditions
                    if( tags != null ) {
                        for( Map.Entry<String,String> entry : tags.entrySet() ) {
                            tm.out("Tag " + entry.getKey(), entry.getValue());
                        }
                    }
                    tm.out("Description", network.getDescription());

                    assertEquals("The ID requested does not match the ID returned", testVLANId, network.getProviderVlanId());
                    assertVLANContent(support, network);
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
    public void listVLANs() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                Iterable<VLAN> vlans = support.listVlans();
                int count = 0;

                assertNotNull("The list of VLANs may not be null (though it can be empty)", vlans);
                for( VLAN vlan : vlans ) {
                    count++;
                    tm.out("VLAN", vlan);
                }
                tm.out("Total VLAN Count", count);

                if( !support.isSubscribed() ) {
                    assertTrue("The call to list VLANs returned VLANs even though the account is marked as not subscribed", count == 0);
                }
                else if( count == 0 ) {
                    tm.warn("No VLANs appeared in the list and thus the test may not be valid");
                }
                if( count > 0 ) {
                    for( VLAN vlan : support.listVlans() )  {
                        assertVLANContent(support, vlan);
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
    public void listRoutingTablesForVlan() throws CloudException, InternalException {
      NetworkServices services = tm.getProvider().getNetworkServices();

      if( services != null ) {
        VLANSupport support = services.getVlanSupport();

        if( support != null ) {

          if( testVLANId != null ) {
            Iterable<RoutingTable> rtbs = support.listRoutingTablesForVlan(testVLANId);
            int count = 0;

            assertNotNull("The list of Routing Tables may not be null (though it can be empty)", rtbs);
            for( RoutingTable rtb : rtbs ) {
              count++;
              tm.out("Routing Table", rtb);
            }
            tm.out("Total Routing Table Count", count);

            if( !support.isSubscribed() ) {
              assertTrue("The call to list Route Tables returned Route Tables even though the account is marked as not subscribed", count == 0);
            }
            else if( count == 0 ) {
              tm.warn("No Route Tables appeared in the list and thus the test may not be valid");
            }
            if( count > 0 ) {
              for( RoutingTable rtb : support.listRoutingTablesForVlan(testVLANId) )  {
                assertRouteTableContent(rtb);
              }
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
    public void listRoutingTablesForSubnet() throws CloudException, InternalException {
      NetworkServices services = tm.getProvider().getNetworkServices();

      if( services != null ) {
        VLANSupport support = services.getVlanSupport();

        if( support != null ) {

          if( testSubnetId != null ) {
            Iterable<RoutingTable> rtbs = support.listRoutingTablesForSubnet(testSubnetId);
            int count = 0;

            assertNotNull("The list of Routing Tables may not be null (though it can be empty)", rtbs);
            for( RoutingTable rtb : rtbs ) {
              count++;
              tm.out("Routing Table", rtb);
            }
            tm.out("Total Routing Table Count", count);

            if( !support.isSubscribed() ) {
              assertTrue("The call to list Route Tables returned Route Tables even though the account is marked as not subscribed", count == 0);
            }
            else if( count == 0 ) {
              tm.warn("No Route Tables appeared in the list and thus the test may not be valid");
            }
            if( count > 0 ) {
              for( RoutingTable rtb : support.listRoutingTablesForVlan(testVLANId) )  {
                assertRouteTableContent(rtb);
              }
            }
          }
          else {
            if( !support.isSubscribed() ) {
              tm.ok("No test VLAN/Subnet was identified for tests due to a lack of subscription to VLAN support");
            }
            else if (support.getCapabilities().getSubnetSupport().equals(Requirement.NONE)) {
                tm.ok("No test subnet was identified for tests due to a lack of support for subnets");
            }
            else {
              fail("No test Subnet was found for running the stateless test: " + name.getMethodName());
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
    public void listVLANStatus() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                Iterable<ResourceStatus> vlans = support.listVlanStatus();
                int count = 0;

                assertNotNull("The VLAN status list may not be null (though it can be empty)", vlans);
                for( ResourceStatus status : vlans ) {
                    count++;
                    tm.out("VLAN Status", status);
                }
                tm.out("Total VLAN Status Count", count);

                if( !support.isSubscribed() ) {
                    assertTrue("The call to list VLAN status returned VLANs even though the account is marked as not subscribed", count == 0);
                }
                else if( count == 0 ) {
                    tm.warn("No VLANs appeared in the status list and thus the test may not be valid");
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
    public void compareVLANListAndStatus() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
                Iterable<VLAN> vlans = support.listVlans();
                Iterable<ResourceStatus> status = support.listVlanStatus();

                assertNotNull("listVLANs() must return at least an empty collections and may not be null", vlans);
                assertNotNull("listVLANStatus() must return at least an empty collection and may not be null", status);
                for( ResourceStatus s : status ) {
                    Map<String,Boolean> current = map.get(s.getProviderResourceId());

                    if( current == null ) {
                        current = new HashMap<String, Boolean>();
                        map.put(s.getProviderResourceId(), current);
                    }
                    current.put("status", true);
                }
                for( VLAN vlan : vlans ) {
                    Map<String,Boolean> current = map.get(vlan.getProviderVlanId());

                    if( current == null ) {
                        current = new HashMap<String, Boolean>();
                        map.put(vlan.getProviderVlanId(), current);
                    }
                    current.put("vlan", true);
                }
                for( Map.Entry<String,Map<String,Boolean>> entry : map.entrySet() ) {
                    Boolean s = entry.getValue().get("status");
                    Boolean v = entry.getValue().get("vlan");

                    assertTrue("Status and VLAN lists do not match for " + entry.getKey(), s != null && v != null && s && v);
                }
                tm.out("Matches");
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
    public void getBogusSubnet() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                Subnet subnet = support.getSubnet(UUID.randomUUID().toString());

                tm.out("Bogus Subnet", subnet);
                assertNull("Bogus subnet was supposed to be null, but got a valid subnet", subnet);
            }
            else {
                tm.ok("No subnet support in this cloud");
            }
        }
        else {
            tm.ok("No network services in this cloud");
        }
    }

    @Test
    public void getSubnet() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                if( testSubnetId != null ) {
                    Subnet subnet = support.getSubnet(testSubnetId);

                    tm.out("Subnet", subnet);
                    assertNotNull("The test subnet was not found in the cloud", subnet);
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.ok("No test subnet was identified for tests due to a lack of subscription to VLAN support");
                    }
                    else if( support.getCapabilities().getSubnetSupport().equals(Requirement.NONE) ) {
                        tm.ok("Subnets are not supported so there is no test for " + name.getMethodName());
                    }
                    else {
                        fail("No test subnet was found for running the stateless test: " + name.getMethodName());
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
    public void getBogusRouteTable() throws CloudException, InternalException {
      NetworkServices services = tm.getProvider().getNetworkServices();

      if( services != null ) {
        VLANSupport support = services.getVlanSupport();

        if( support != null ) {
          RoutingTable rtb = support.getRoutingTable(UUID.randomUUID().toString());

          tm.out("Bogus Route Table", rtb);
          assertNull("Bogus route table was supposed to be null, but got a valid route table", rtb);
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
    public void getRouteTable() throws CloudException, InternalException {
      NetworkServices services = tm.getProvider().getNetworkServices();

      if( services != null ) {
        VLANSupport support = services.getVlanSupport();

        if( support != null ) {
          if( testRoutingTableId != null ) {
            RoutingTable rtb = support.getRoutingTable(testRoutingTableId);

            tm.out("Route Table", rtb);
            assertNotNull("The test route table was not found in the cloud", rtb);
          }
          else {
            if( !support.isSubscribed() ) {
              tm.ok("No test route table was identified for tests due to a lack of subscription to VLAN support");
            }
            else if( support.getCapabilities().getRoutingTableSupport().equals(Requirement.NONE) ) {
              tm.ok("Route Tables are not supported so there is no test for " + name.getMethodName());
            }
            else {
              fail("No test route table was found for running the stateless test: " + name.getMethodName());
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
    public void getBogusInternetGateway() throws CloudException, InternalException {
      NetworkServices services = tm.getProvider().getNetworkServices();

      if( services != null ) {
        VLANSupport support = services.getVlanSupport();

        if( support != null ) {
          InternetGateway internetGateway = support.getInternetGatewayById(UUID.randomUUID().toString());

          tm.out("Bogus Internet Gateway", internetGateway);
          assertNull("Bogus internet gateway was supposed to be null, but got a valid internet gateway", internetGateway);
        }
        else {
          tm.ok("No internet gateway support in this cloud");
        }
      }
      else {
        tm.ok("No network services in this cloud");
      }
    }

    @Test
    public void getInternetGateway() throws CloudException, InternalException {
      NetworkServices services = tm.getProvider().getNetworkServices();

      if( services != null ) {
        VLANSupport support = services.getVlanSupport();

        if( support != null ) {
          if( testInternetGatewayId != null ) {
            InternetGateway iGateway = support.getInternetGatewayById(testInternetGatewayId);
            tm.out("Internet Gateway", iGateway);
            assertNotNull("The test internet gateway was not found in the cloud", iGateway);
          }
          else {
            if( !support.isSubscribed() ) {
              tm.ok("No test internet gatway was identified for tests due to a lack of subscription to VLAN support");
            }
            else if( !support.getCapabilities().supportsInternetGatewayCreation() ) {
              tm.ok("Internet Gateways are not supported so there is no test for " + name.getMethodName());
            }
            else {
              fail("No test internet gateway was found for running the stateless test: " + name.getMethodName());
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
    public void subnetContent() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                if( testSubnetId != null ) {
                    Subnet subnet = support.getSubnet(testSubnetId);

                    assertNotNull("The test subnet was not found in the cloud", subnet);

                    tm.out("Subnet ID", subnet.getProviderSubnetId());
                    tm.out("Current State", subnet.getCurrentState());
                    tm.out("Name", subnet.getName());
                    tm.out("Owner Account", subnet.getProviderOwnerId());
                    tm.out("Region ID", subnet.getProviderRegionId());
                    tm.out("Data Center ID", subnet.getProviderDataCenterId());
                    tm.out("VLAN ID", subnet.getProviderVlanId());
                    tm.out("CIDR", subnet.getCidr());
                    tm.out("Available IPs", subnet.getAvailableIpAddresses());
                    tm.out("Allocation Pools", Arrays.toString(subnet.getAllocationPools()));
                    tm.out("Gateway", subnet.getGateway());
                    tm.out("Supported Traffic", Arrays.toString(subnet.getSupportedTraffic()));
                    Map<String,String> tags = subnet.getTags();

                    //noinspection ConstantConditions
                    if( tags != null ) {
                        for( Map.Entry<String,String> entry : tags.entrySet() ) {
                            tm.out("Tag " + entry.getKey(), entry.getValue());
                        }
                    }
                    tm.out("Description", subnet.getDescription());
                    assertEquals("The requested subnet ID does not match the ID of the subnet returned", testSubnetId, subnet.getProviderSubnetId());
                    assertSubnetContent(support, subnet, null);
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.ok("No test subnet was identified for tests due to a lack of subscription to VLAN support");
                    }
                    else if( support.getCapabilities().getSubnetSupport().equals(Requirement.NONE) ) {
                        tm.ok("Subnets are not supported so there is no test for " + name.getMethodName());
                    }
                    else {
                        fail("No test subnet was found for running the stateless test: " + name.getMethodName());
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
    public void listSubnets() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                if( testVLANId != null ) {
                    Iterable<Subnet> subnets = support.listSubnets(testVLANId);
                    int count = 0;

                    assertNotNull("The list of subnets may not be null (though it can be empty)", subnets);
                    for( Subnet subnet : subnets ) {
                        count++;
                        tm.out("Subnet", subnet);
                    }
                    tm.out("Total Subnet Count for " + testVLANId, count);

                    if( !support.isSubscribed() ) {
                        assertTrue("The call to list subnets returned subnets even though the account is marked as not subscribed", count == 0);
                    }
                    else if( count == 0 ) {
                        tm.warn("No subnets appeared in the list and thus the test may not be valid");
                    }
                    if( count > 0 ) {
                        for( Subnet subnet : subnets )  {
                            assertSubnetContent(support, subnet, testVLANId);
                        }
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
    public void listInternetGateways() throws CloudException, InternalException {
      NetworkServices services = tm.getProvider().getNetworkServices();

      if( services != null ) {
        VLANSupport support = services.getVlanSupport();

        if( support != null ) {
          if( testVLANId != null ) {
            Iterable<InternetGateway> internetGateways = support.listInternetGateways(testVLANId);
            int count = 0;

            assertNotNull("The list of internet gateways may not be null (though it can be empty)", internetGateways);
            for( InternetGateway internetgateway : internetGateways ) {
              count++;
              tm.out("Internet Gateway", internetgateway);
            }
            tm.out("Total Internet Gateway Count for " + testVLANId, count);

            if( !support.isSubscribed() ) {
              assertTrue("The call to list internet gateways returned internet gateways even though the account is marked as not subscribed", count == 0);
            }
            else if( count == 0 ) {
              tm.warn("No internet gateways appeared in the list and thus the test may not be valid");
            }
            if( count > 0 ) {
              for( InternetGateway internetgateway : internetGateways )  {
                assertInternetGatewayContent(internetgateway, testVLANId);
              }
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
    public void listResources() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services != null ) {
            VLANSupport support = services.getVlanSupport();

            if( support != null ) {
                if( testVLANId != null ) {
                    Iterable<Networkable> resources = support.listResources(testVLANId);
                    int count = 0;

                    assertNotNull("The list of VLAN resources may be empty, but it cannot be null", resources);
                    for( Networkable n : resources ) {
                        count++;
                        tm.out("VLAN Resource", n);
                    }
                    tm.out("Total VLAN Resource Count", count);
                    if( !support.isSubscribed() ) {
                        assertTrue("The call to list VLAN resources returned resources even though the account is marked as not subscribed", count == 0);
                    }
                    else if( count == 0 ) {
                        tm.warn("No resources appeared in the list and thus the test may not be valid");
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
    public void assignRouteTableToSubnet() throws CloudException, InternalException {
      NetworkServices services = tm.getProvider().getNetworkServices();

      if( services != null ) {
        VLANSupport support = services.getVlanSupport();

        if( support != null ) {
          if( testSubnetId != null && testRoutingTableId != null ) {
            RoutingTable rtb = support.getRoutingTable(testRoutingTableId);
            tm.out("Route Table", rtb);
            assertNotNull("The test route table was not found in the cloud", rtb);
            Subnet subnet = support.getSubnet(testSubnetId);
            tm.out("Subnet", subnet);
            assertNotNull("The test subnet was not found in the cloud", subnet);
            support.assignRoutingTableToSubnet(testSubnetId, testRoutingTableId);
            try { Thread.sleep(1000L); }
            catch( InterruptedException ignore ) { }
            rtb = support.getRoutingTable(testRoutingTableId);
            tm.out("Route Table", rtb);
            assertNotNull("The test route table was not found in the cloud", rtb);
            tm.out("Route Table subnets", rtb.getProviderSubnetIds());
            assertTrue("The test route table subnets should not be empty", rtb.getProviderSubnetIds().length > 0);
          }
          else {
            if( !support.isSubscribed() ) {
              tm.ok("No test route table was identified for tests due to a lack of subscription to VLAN support");
            }
            else if( support.getCapabilities().getRoutingTableSupport().equals(Requirement.NONE) ) {
              tm.ok("Route Tables are not supported so there is no test for " + name.getMethodName());
            }
            else {
              if( testSubnetId == null ) {
                fail("No test subnet was found for running the stateful test: " + name.getMethodName());
              }
              else {
                fail("No test route table was found for running the stateful test: " + name.getMethodName());
              }
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
    public void disassociateRouteTableFromSubnet() throws CloudException, InternalException {
      NetworkServices services = tm.getProvider().getNetworkServices();

      if( services != null ) {
        VLANSupport support = services.getVlanSupport();

        if( support != null ) {
          if( testSubnetId != null && testRoutingTableId != null ) {
            RoutingTable rtb = support.getRoutingTable(testRoutingTableId);
            tm.out("Route Table", rtb);
            assertNotNull("The test route table was not found in the cloud", rtb);
            Subnet subnet = support.getSubnet(testSubnetId);
            tm.out("Subnet", subnet);
            assertNotNull("The test subnet was not found in the cloud", subnet);
            support.disassociateRoutingTableFromSubnet(testSubnetId, testRoutingTableId);
            try { Thread.sleep(3000L); }
            catch( InterruptedException ignore ) { }
            rtb = support.getRoutingTable(testRoutingTableId);
            Boolean match = false;
            if( rtb != null ) {
              tm.out("Route Table subnets", rtb.getProviderSubnetIds());
              String[] rtbSubArr = rtb.getProviderSubnetIds();
              if( rtbSubArr != null ) {
                for(String subnetId : rtbSubArr) {
                  if(subnetId.equals(testSubnetId)){
                    match = true;
                  }
                }
              }
            }
            assertFalse("The test route table subnets should not not contain test subnet id", match);
          }
          else {
            if( !support.isSubscribed() ) {
              tm.ok("No test route table was identified for tests due to a lack of subscription to VLAN support");
            }
            else if( support.getCapabilities().getRoutingTableSupport().equals(Requirement.NONE) ) {
              tm.ok("Route Tables are not supported so there is no test for " + name.getMethodName());
            }
            else {
              if( testSubnetId == null ) {
                fail("No test subnet was found for running the stateful test: " + name.getMethodName());
              }
              else {
                fail("No test route table was found for running the stateful test: " + name.getMethodName());
              }
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

}
