package org.dasein.cloud.test.network;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.network.VPN;
import org.dasein.cloud.network.VPNCapabilities;
import org.dasein.cloud.network.VPNConnection;
import org.dasein.cloud.network.VPNGateway;
import org.dasein.cloud.network.VPNGatewayCreateOptions;
import org.dasein.cloud.network.VPNGatewayState;
import org.dasein.cloud.network.VpnCreateOptions;
import org.dasein.cloud.network.VPNProtocol;
import org.dasein.cloud.network.VPNState;
import org.dasein.cloud.network.VPNSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class StatefulVPNTests {
    private VLAN testVlan1 = null;
    private VLAN testVlan2 = null;
    /*
     * vpn1 10.240.0.0/16   10.240.0.1  0
     * vpn2    192.168.1.0/24  192.168.1.1 0
     * vpn-1   vpn1    us-central1 146.148.35.118       130.211.188.232
     * vpn-2   vpn2    us-central1 130.211.188.232      146.148.35.118
     */
    private static DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulVPNTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if (tm != null) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    public StatefulVPNTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        NetworkServices network = tm.getProvider().getNetworkServices();

        createNetworks();
        //if (name.getMethodName().equals("createFirewall")) {
        //    testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
        //}
    }

    public void createNetworks() {
        NetworkServices networkServices = tm.getProvider().getNetworkServices();

        String testVpnId1 = "vpn1-network";
        String testVpnId2 = "vpn2-network";
        VLANSupport vlanSupport = networkServices.getVlanSupport();

        try {
            testVlan1 = vlanSupport.createVlan("10.240.0.0/16", testVpnId1, testVpnId1, null, null, null);
            testVlan2 = vlanSupport.createVlan("192.168.1.0/24", testVpnId2, testVpnId2, null, null, null);
        } catch( Throwable t ) {
            tm.warn("Failed to provision vlans " + t.getMessage());
        }
    }

    @After
    public void after() {
        try {
            removeNetworks();
            testVlan1 = null;
            testVlan2 = null;
            //testVLANId = null;
            //testFirewallId = null;
            //testRuleId = null;
        }
        finally {
            tm.end();
        }
    }

    private void removeNetworks() {
        NetworkServices networkServices = tm.getProvider().getNetworkServices();

        VLANSupport vlanSupport = networkServices.getVlanSupport();
        try {
            vlanSupport.removeVlan(testVlan1.getName());
            vlanSupport.removeVlan(testVlan2.getName());
        } catch( Throwable t ) {
            tm.warn("Failed to remove VLANS " + t.getMessage());
        }
    }

    @Test
    public void createInternalVPN() throws CloudException, InternalException {
        CloudProvider provider = tm.getProvider();
        VPNSupport vpnSupport = provider.getNetworkServices().getVpnSupport();
        VPNCapabilities vpnCapabilities = vpnSupport.getCapabilities();

        if (null != vpnCapabilities) {
            Iterable<VPNProtocol> supportedProtocols = vpnCapabilities.listSupportedVPNProtocols();

            VPNProtocol protocol = supportedProtocols.iterator().next();
            tm.out("Testing VPN protocol: " + protocol);
            VpnCreateOptions vpnLaunchOptions1 = VpnCreateOptions.getInstance("vpn1", "vpn1", protocol);
            if (vpnCapabilities.getVPNVLANConstraint() == Requirement.REQUIRED) {
                vpnLaunchOptions1 = vpnLaunchOptions1.withProviderVlanId("vpn1-network");
            }
            VPN vpn1 = vpnSupport.createVPN(vpnLaunchOptions1);

            VpnCreateOptions vpnLaunchOptions2 = VpnCreateOptions.getInstance("vpn2", "vpn2", protocol);
            if (vpnCapabilities.getVPNVLANConstraint() == Requirement.REQUIRED) {
                vpnLaunchOptions2 = vpnLaunchOptions2.withProviderVlanId("vpn2-network");
            }
            VPN vpn2 = vpnSupport.createVPN(vpnLaunchOptions2);
            
            VPNGateway result1 = vpnSupport.createVPNGateway(VPNGatewayCreateOptions.getInstance("vpn1-tunnel", vpn1.getDescription(), vpn1.getProtocol(), vpn2.getProviderVpnIp()).withCidr("192.168.1.0/24").withSharedSecret("googtest").withVlanName("vpn1-network").withVpnName("vpn1"));
            VPNGateway result2 = vpnSupport.createVPNGateway(VPNGatewayCreateOptions.getInstance("vpn2-tunnel", vpn2.getDescription(), vpn2.getProtocol(), vpn1.getProviderVpnIp()).withCidr("10.240.0.0/16").withSharedSecret("googtest").withVlanName("vpn2-network").withVpnName("vpn2"));
            //VPNGateway result1 = vpnSupport.connectToVPNGateway(vpnLaunchOptions1.getProviderVlanId(), vpn2.getProviderVpnIp(), "vpn1-tunnel", vpn1.getDescription(), vpn2.getProtocol(), "googtest", "192.168.1.0/24");
            //VPNGateway result2 = vpnSupport.connectToVPNGateway(vpnLaunchOptions2.getProviderVlanId(), vpn1.getProviderVpnIp(), "vpn2-tunnel", vpn2.getDescription(), vpn1.getProtocol(), "googtest", "10.240.0.0/16");

            int vpnCount = 0;
            Iterable<VPN> vpns = vpnSupport.listVPNs();
            if (null != vpns) {
                for (VPN vpn : vpns) {
                    vpnCount++;
                }
            }
            assertTrue("listVPNs() should return > 0 result", (vpnCount >0));

            int vpnConnectionsCount = 0;
            Iterable<VPNConnection> vpnConnections = vpnSupport.listVPNConnections(vpn1.getName());
            if (null != vpnConnections) {
                for (VPNConnection vpnConnection : vpnConnections) {
                    vpnConnectionsCount++;
                }
            }
            assertTrue("listVPNConnections() should return > 0 result", (vpnConnectionsCount >0));

            Iterable<ResourceStatus> allVpnStatus = vpnSupport.listVPNStatus();
            if (null != allVpnStatus) {
                for (ResourceStatus vpnStatus : allVpnStatus) {
                    tm.out("VPN STATUS = " + vpnStatus.getProviderResourceId() + " STATUS:" + vpnStatus.getResourceStatus());
                    assertConnected(vpnStatus, Arrays.asList(VPNState.PENDING, VPNState.AVAILABLE));
                }
            }

            tm.out("SLEEPING 60seconds....");
            try {
                Thread.sleep(60000L);
            } catch ( InterruptedException e ) { }

            allVpnStatus = vpnSupport.listVPNStatus();
            if (null != allVpnStatus) {
                for (ResourceStatus vpnStatus : allVpnStatus) {
                    tm.out("VPN STATUS = " + vpnStatus.getProviderResourceId() + " STATUS:" + vpnStatus.getResourceStatus());
                    assertConnected(vpnStatus, Arrays.asList(VPNState.AVAILABLE));
                }
            }

            VPNGateway vpnGateway1 = vpnSupport.getGateway(result1.getName());
            assertVpnGateway(vpnGateway1);
            VPNGateway vpnGateway2 = vpnSupport.getGateway(result2.getName());
            assertVpnGateway(vpnGateway2);

            vpnSupport.deleteVPNGateway(result1.getName());
            vpnSupport.deleteVPNGateway(result2.getName());

            vpnSupport.deleteVPN(vpn1.getName());
            vpnSupport.deleteVPN(vpn2.getName());
        }
    }

    private void assertVpnGateway(VPNGateway vpnGateway) {
        assertTrue("Name should not be null", null != vpnGateway.getName());
        assertTrue("Description should not be null", null != vpnGateway.getDescription());
        assertTrue("State not in set of possible states.", Arrays.asList(VPNGatewayState.values()).contains(vpnGateway.getCurrentState()));
        assertTrue("Endpoint should not be null", null != vpnGateway.getEndpoint());
        assertTrue("Protocol not in set of possible protocols.", Arrays.asList(VPNProtocol.values()).contains(vpnGateway.getProtocol()));

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

    private void assertConnected(ResourceStatus vpnStatus, List<VPNState> expectedStates) {
        assertTrue("VPN state should be in the set of " + expectedStates + " but was " + vpnStatus.getResourceStatus(), expectedStates.contains(vpnStatus.getResourceStatus()));
    }
}
