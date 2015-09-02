package org.dasein.cloud.test.network;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.List;

import co.freeside.betamax.Betamax;
import co.freeside.betamax.Recorder;
import co.freeside.betamax.httpclient.BetamaxHttpsSupport;
import co.freeside.betamax.httpclient.BetamaxRoutePlanner;
import org.apache.http.impl.client.AbstractHttpClient;
import org.dasein.cloud.*;
import org.dasein.cloud.aws.AWSCloud;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.network.*;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class StatefulVPNTests {
    private String testVlanId1;
    private String testVlanId2;
    private String testDataCenterId;
    private String testVpnId;
    private NetworkServices networkServices;
    private VPNSupport vpnSupport;
    private NetworkResources networkResources;

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
        if( name.getMethodName().equalsIgnoreCase("createInternalVPN") ) {

            testDataCenterId = DaseinTestManager.getDefaultDataCenterId(false);
            testVlanId1 = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, testDataCenterId);
            NetworkResources networkResources = DaseinTestManager.getNetworkResources();
            if( networkResources != null ) {
                try {
                    testVlanId2 = networkResources.provisionVLAN(networkServices.getVlanSupport(), DaseinTestManager.STATEFUL, "vpn", testDataCenterId);
                } catch (CloudException e) {
                } catch (InternalException e) {
                }
            }
        }
        else if ( name.getMethodName().equalsIgnoreCase("removeVpn") ) {
            testVpnId = tm.getTestVpnId(DaseinTestManager.REMOVED, true, testDataCenterId);
            // wait...
            try {
                Thread.sleep(5000L);
            } catch( InterruptedException ignore ) {
            }
            if( testVpnId == null ) {
                testVpnId = tm.getTestVpnId(DaseinTestManager.STATELESS, false, testDataCenterId);
            }
            if( testVpnId == null ) {
                testVpnId = tm.getTestVpnId(DaseinTestManager.STATEFUL, false, testDataCenterId);
            }
            if( testVpnId == null ) {
                testVpnId = tm.getTestVpnId(DaseinTestManager.STATEFUL, true, testDataCenterId);
                // wait...
                try {
                    Thread.sleep(5000L);
                } catch( InterruptedException ignore ) {
                }
            }

        }
    }

//    public void createNetworks() {
//        NetworkServices networkServices = tm.getProvider().getNetworkServices();
//
//        String testVpnId1 = "vpn1";
//        String testVpnId2 = "vpn2";
//        VLANSupport vlanSupport = networkServices.getVlanSupport();
//
//        try {
//            testVlanId1 = vlanSupport.createVlan("10.240.0.0/16", testVpnId1, testVpnId1, null, null, null);
//            testVlanId2 = vlanSupport.createVlan("192.168.1.0/24", testVpnId2, testVpnId2, null, null, null);
//        } catch( Throwable t ) {
//            tm.warn("Failed to provision vlans " + t.getMessage());
//        }
//    }

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

    @Rule public Recorder recorder = new Recorder();
    @Betamax(tape="vpn")
    @Test
    public void createVpn() throws InternalException, CloudException {
        if( tm.getProvider() instanceof AWSCloud ) {
            BetamaxRoutePlanner.configure((AbstractHttpClient) ((AWSCloud) tm.getProvider()).getClient());
            BetamaxHttpsSupport.configure(((AWSCloud) tm.getProvider()).getClient());
            recorder.setSslSupport(true);
        }
        assumeNotNull(networkServices);
        assumeNotNull(networkResources);
        assumeNotNull(vpnSupport);
        String id = networkResources.provisionVpn(DaseinTestManager.STATEFUL, "crt", testDataCenterId);
        assertNotNull("Unable to create VPN, VPN ID cannot be null", id);
        VPN vpn = networkServices.getVpnSupport().getVPN(id);
        assertNotNull("Unable find newly created VPN "+id, vpn);
    }

    @Test
    public void removeVpn() throws InternalException, CloudException {
        assumeNotNull(networkServices);
        assumeNotNull(vpnSupport);
        VPN vpn = vpnSupport.getVPN(testVpnId);

        tm.out("Before", vpn);
        assertNotNull("Test VPN no longer exists, cannot test removing it", vpn);
        tm.out("State", vpn.getCurrentState());
        vpnSupport.deleteVPN(testVpnId); // TODO: core - rename to removeVpn in line with removeSubnet etc
        int tries = 0;
        do {
            try {
                Thread.sleep(5000L);
                tries++;
            } catch (InterruptedException ignore) {
            }
            vpn = vpnSupport.getVPN(testVpnId);
        } while( tries < 10 && vpn != null && VPNState.DELETING.equals(vpn.getCurrentState()) );

        tm.out("After", vpn);
        tm.out("State", (vpn == null || VPNState.DELETED.equals(vpn.getCurrentState()) ? "DELETED" : vpn.getCurrentState()));
        assertTrue("The VPN remains available", vpn == null || VPNState.DELETED.equals(vpn.getCurrentState()));

    }

//    private void removeNetworks() {
//        NetworkServices networkServices = tm.getProvider().getNetworkServices();
//
//        VLANSupport vlanSupport = networkServices.getVlanSupport();
//        try {
//            vlanSupport.removeVlan(testVlanId1.getName());
//            vlanSupport.removeVlan(testVlanId2.getName());
//        } catch( Throwable t ) {
//            tm.warn("Failed to remove VLANS " + t.getMessage());
//        }
//    }

    @Test
    public void createInternalVPN() throws CloudException, InternalException {
        CloudProvider provider = tm.getProvider();
        VPNSupport vpnSupport = provider.getNetworkServices().getVpnSupport();
        VPNCapabilities vpnCapabilities = vpnSupport.getCapabilities();

        if (null != vpnCapabilities) {
            Iterable<VPNProtocol> supportedProtocols = vpnCapabilities.listSupportedVPNProtocols();

            for (VPNProtocol protocol : supportedProtocols) {
                tm.out("Testing VPN protocol: " + protocol);
                VpnLaunchOptions vpnLaunchOptions1 = VpnLaunchOptions.getInstance("vpn1", "vpn1", protocol);
                if (vpnCapabilities.getVPNVLANConstraint() == Requirement.REQUIRED) {
                    vpnLaunchOptions1 = vpnLaunchOptions1.withProviderVlanId("vpn1");
                }
                VPN vpn1 = vpnSupport.createVPN(vpnLaunchOptions1);

                VpnLaunchOptions vpnLaunchOptions2 = VpnLaunchOptions.getInstance("vpn2", "vpn2", protocol);
                if (vpnCapabilities.getVPNVLANConstraint() == Requirement.REQUIRED) {
                    vpnLaunchOptions2 = vpnLaunchOptions2.withProviderVlanId("vpn2");
                }
                VPN vpn2 = vpnSupport.createVPN(vpnLaunchOptions2);

                VPNGateway result1 = vpnSupport.connectToVPNGateway(vpn1.getName(), vpn2.getProviderVpnIp(), "vpn1-tunnel", vpn1.getDescription(), vpn2.getProtocol(), "googtest", "192.168.1.0/24");
                VPNGateway result2 = vpnSupport.connectToVPNGateway(vpn2.getName(), vpn1.getProviderVpnIp(), "vpn2-tunnel", vpn2.getDescription(), vpn1.getProtocol(), "googtest", "10.240.0.0/16");

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
