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

import org.dasein.cloud.*;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.network.*;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.*;
import org.junit.rules.TestName;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

public class StatefulVpnTests {
    // TODO: does Roger need these two?
    private String testVlanId1;
    private String testVlanId2;
    private String testDataCenterId;
    private String testVpnId;
    private NetworkServices networkServices;
    private VpnSupport vpnSupport;
    private NetworkResources networkResources;

    private static DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulVpnTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if (tm != null) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    public StatefulVpnTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        // initialise interface classes
        networkServices = tm.getProvider().getNetworkServices();
        if( networkServices == null ) {
            return; // no network services, no point continuing
        }
        vpnSupport = networkServices.getVpnSupport();
        if( vpnSupport == null ) {
            return; // no vpn support, no point continuing
        }
        networkResources = DaseinTestManager.getNetworkResources();
        if( networkResources == null ) {
            tm.warn("Failed to initialise network resources");
        }

        switch(name.getMethodName()) {
            case "createInternalVpn":
                testDataCenterId = DaseinTestManager.getDefaultDataCenterId(false);
                testVlanId1 = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, testDataCenterId);
                NetworkResources networkResources = DaseinTestManager.getNetworkResources();
                if( networkResources != null ) {
                    try {
                        testVlanId2 = networkResources.provisionVLAN(networkServices.getVlanSupport(), DaseinTestManager.STATEFUL, "vpn", testDataCenterId);
                    } catch (CloudException|InternalException e) {
                    }
                }
                break;
            case "removeVpn":
                testVpnId = tm.getTestVpnId(DaseinTestManager.REMOVED, true, testDataCenterId);
                // wait...
                try {
                    Thread.sleep(5000L);
                } catch( InterruptedException ignore ) {
                }
                if( testVpnId == null ) testVpnId = tm.getTestVpnId(DaseinTestManager.STATELESS, false, testDataCenterId);
                if( testVpnId == null ) testVpnId = tm.getTestVpnId(DaseinTestManager.STATEFUL, false, testDataCenterId);
                if( testVpnId == null ) {
                    testVpnId = tm.getTestVpnId(DaseinTestManager.STATEFUL, true, testDataCenterId);
                    // wait...
                    try {
                        Thread.sleep(5000L);
                    } catch( InterruptedException ignore ) {
                    }
                }
                break;
        }

    }

    @After
    public void after() {
        try {
            testVlanId1 = null;
            testVlanId2 = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void createVpn() throws InternalException, CloudException {
        assumeNotNull(networkServices);
        assumeNotNull(networkResources);
        assumeNotNull(vpnSupport);
        String id = networkResources.provisionVpn(DaseinTestManager.STATEFUL, "crt", testDataCenterId);
        assertNotNull("Unable to create VPN, VPN ID cannot be null", id);
        Vpn vpn = networkServices.getVpnSupport().getVpn(id);
        assertNotNull("Unable find newly created VPN "+id, vpn);
    }

    @Test
    public void removeVpn() throws InternalException, CloudException {
        assumeNotNull(networkServices);
        assumeNotNull(vpnSupport);
        Vpn vpn = vpnSupport.getVpn(testVpnId);

        tm.out("Before", vpn);
        assertNotNull("Test VPN no longer exists, cannot test removing it", vpn);
        tm.out("State", vpn.getCurrentState());
        vpnSupport.deleteVpn(testVpnId); // TODO: core - rename to removeVpn in line with removeSubnet etc
        int tries = 0;
        do {
            try {
                Thread.sleep(5000L);
                tries++;
            } catch (InterruptedException ignore) {
            }
            vpn = vpnSupport.getVpn(testVpnId);
        } while( tries < 10 && vpn != null && VpnState.DELETING.equals(vpn.getCurrentState()) );

        tm.out("After", vpn);
        tm.out("State", (vpn == null || VpnState.DELETED.equals(vpn.getCurrentState()) ? "DELETED" : vpn.getCurrentState()));
        assertTrue("The VPN remains available", vpn == null || VpnState.DELETED.equals(vpn.getCurrentState()));

    }

    @Test
    public void createInternalVpn() throws CloudException, InternalException {
        CloudProvider provider = tm.getProvider();
        VpnSupport vpnSupport = provider.getNetworkServices().getVpnSupport();
        VpnCapabilities vpnCapabilities = vpnSupport.getCapabilities();

        if (null != vpnCapabilities) {
            Iterable<VpnProtocol> supportedProtocols = vpnCapabilities.listSupportedVpnProtocols();

            VpnProtocol protocol = supportedProtocols.iterator().next();
            tm.out("Testing VPN protocol: " + protocol);
            VpnCreateOptions vpnLaunchOptions1 = VpnCreateOptions.getInstance("vpn1", "vpn1", protocol);
            VpnCreateOptions vpnLaunchOptions2 = VpnCreateOptions.getInstance("vpn2", "vpn2", protocol);

            if (vpnCapabilities.identifyVlanIdRequirement() == Requirement.REQUIRED) {
                vpnLaunchOptions1 = vpnLaunchOptions1.withProviderVlanId("vpn1-network");
                vpnLaunchOptions2 = vpnLaunchOptions2.withProviderVlanId("vpn2-network");
            }

            Vpn vpn1 = vpnSupport.createVpn(vpnLaunchOptions1);
            Vpn vpn2 = vpnSupport.createVpn(vpnLaunchOptions2);

            VpnGatewayCreateOptions vpn1options = VpnGatewayCreateOptions.getInstance("vpn1-tunnel", vpn1.getDescription(), vpn1.getProtocol(), vpn2.getProviderVpnIp());
            VpnGatewayCreateOptions vpn2options = VpnGatewayCreateOptions.getInstance("vpn2-tunnel", vpn2.getDescription(), vpn2.getProtocol(), vpn1.getProviderVpnIp());

            if (vpnCapabilities.identifyGatewaySharedSecretRequirement() == Requirement.REQUIRED) {
                vpn1options = vpn1options.withSharedSecret("googtest");
                vpn2options = vpn2options.withSharedSecret("googtest");
            }

            if (vpnCapabilities.identifyGatewayVlanNameRequirement() == Requirement.REQUIRED) {
                vpn1options = vpn1options.withVlanName("vpn1-network");
                vpn2options = vpn2options.withVlanName("vpn2-network");
            }
            if (vpnCapabilities.identifyGatewayVpnNameRequirement() == Requirement.REQUIRED) {
                vpn1options = vpn1options.withVpnName("vpn1");
                vpn2options = vpn2options.withVpnName("vpn2");
            }

            if (vpnCapabilities.identifyGatewayCidrRequirement() == Requirement.REQUIRED) {
                vpn1options = vpn1options.withCidr("192.168.1.0/24");
                vpn2options = vpn2options.withCidr("10.240.0.0/16");
            }

            VpnGateway result1 = vpnSupport.createVpnGateway(vpn1options);
            VpnGateway result2 = vpnSupport.createVpnGateway(vpn2options);

            int vpnCount = 0;
            Iterable<Vpn> vpns = vpnSupport.listVpns();
            for (Vpn vpn : vpns) {
                vpnCount++;
            }
            assertTrue("listVPNs() should return > 0 result", (vpnCount >0));

            int vpnConnectionsCount = 0;
            Iterable<VpnConnection> vpnConnections = vpnSupport.listVpnConnections(vpn1.getName());
            for (VpnConnection $ : vpnConnections) {
                vpnConnectionsCount++;
            }
            assertTrue("listVPNConnections() should return > 0 result", (vpnConnectionsCount >0));

            Iterable<ResourceStatus> allVpnStatus = vpnSupport.listVpnStatus();
            for (ResourceStatus vpnStatus : allVpnStatus) {
                tm.out("VPN STATUS = " + vpnStatus.getProviderResourceId() + " STATUS:" + vpnStatus.getResourceStatus());
                assertConnected(vpnStatus, Arrays.asList(VpnState.PENDING, VpnState.AVAILABLE));
            }

            tm.out("SLEEPING 60seconds....");
            try {
                Thread.sleep(60000L);
            } catch ( InterruptedException e ) { }

            allVpnStatus = vpnSupport.listVpnStatus();
            for (ResourceStatus vpnStatus : allVpnStatus) {
                tm.out("VPN STATUS = " + vpnStatus.getProviderResourceId() + " STATUS:" + vpnStatus.getResourceStatus());
                assertConnected(vpnStatus, Arrays.asList(VpnState.AVAILABLE));
            }

            VpnGateway vpnGateway1 = vpnSupport.getGateway(result1.getName());
            assertVpnGateway(vpnGateway1);
            VpnGateway vpnGateway2 = vpnSupport.getGateway(result2.getName());
            assertVpnGateway(vpnGateway2);

            vpnSupport.deleteVpnGateway(result1.getName());
            vpnSupport.deleteVpnGateway(result2.getName());

            vpnSupport.deleteVpn(vpn1.getName());
            vpnSupport.deleteVpn(vpn2.getName());
        }
    }

    private void assertVpnGateway(VpnGateway vpnGateway) {
        assertNotNull("Name should not be null", vpnGateway.getName());
        assertNotNull("Description should not be null", vpnGateway.getDescription());
        assertTrue("State not in set of possible states.", Arrays.asList(VpnGatewayState.values()).contains(vpnGateway.getCurrentState()));
        assertNotNull("Endpoint should not be null", vpnGateway.getEndpoint());
        assertTrue("Protocol not in set of possible protocols.", Arrays.asList(VpnProtocol.values()).contains(vpnGateway.getProtocol()));

        Iterable<Region> regions;
        boolean regionFound = false;
        try {
            regions = tm.getProvider().getDataCenterServices().listRegions();
            for (Region region : regions) {
                if (region.getName().equals(vpnGateway.getProviderRegionId())) {
                    regionFound = true;
                }
            }
        } catch ( Exception e ) { }
        assertTrue("ProviderRegionId not in set of possible regions.", regionFound);
        //vpnGateway.getBgpAsn();
        //vpnGateway.getProviderOwnerId()
        //vpnGateway.getProviderVpnGatewayId()
    }

    private void assertConnected(ResourceStatus vpnStatus, List<VpnState> expectedStates) {
        assertTrue("VPN state should be in the set of " + expectedStates + " but was " + vpnStatus.getResourceStatus(), expectedStates.contains(vpnStatus.getResourceStatus()));
    }
}
