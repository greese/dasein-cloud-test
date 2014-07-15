/**
 * Copyright (C) 2009-2014 Dell, Inc.
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.HealthCheckOptions;
import org.dasein.cloud.network.LbEndpointType;
import org.dasein.cloud.network.LoadBalancer;
import org.dasein.cloud.network.LoadBalancerEndpoint;
import org.dasein.cloud.network.LoadBalancerHealthCheck;
import org.dasein.cloud.network.LoadBalancerState;
import org.dasein.cloud.network.LoadBalancerSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.SSLCertificate;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Implements test cases against stateful load balancer functions.
 * <p>Created by George Reese: 3/8/13 4:34 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatefulLoadBalancerTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulLoadBalancerTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testDataCenterId;
    private String testLoadBalancerId;
    private String testVirtualMachineId;
    private String testSSLCertificateName;

    public StatefulLoadBalancerTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        try {
            testDataCenterId = System.getProperty("test.dataCenter");
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( testDataCenterId == null )
                testDataCenterId = tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()).iterator().next().getProviderDataCenterId();
        }
        catch( Throwable ignore ) {
            // ignore
        }
        if( name.getMethodName().equals("removeLoadBalancer") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.REMOVED, tm.getUserName() + "-dsnlb", true, false);
        }
        else if( name.getMethodName().equals("addIP") || name.getMethodName().equals("createLoadBalancerHealthCheck")) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, tm.getUserName() + "-dsnlb", true);
        }
        else if( name.getMethodName().equals("createLoadBalancerWithHealthCheck")) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, tm.getUserName() + "-dsnlb", true, true);
        }
        else if( name.getMethodName().equals("removeIP") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, tm.getUserName() + "-dsnlb", true);
            NetworkServices services = tm.getProvider().getNetworkServices();

            if( services != null ) {
                LoadBalancerSupport support = services.getLoadBalancerSupport();

                if( support != null ) {
                    try {
                        support.removeIPEndpoints(testLoadBalancerId, "197.41.20.2");
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        else if( name.getMethodName().equals("addServer") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, tm.getUserName() + "-dsnlb", true, true);
            LoadBalancer lb = null;

            NetworkServices net = tm.getProvider().getNetworkServices();

            try {
                if( net != null ) {
                    LoadBalancerSupport support = net.getLoadBalancerSupport();

                    if( support != null ) {
                        lb = support.getLoadBalancer(testLoadBalancerId);
                    }
                }
                if( lb != null ) {
                    String[] ids = lb.getProviderDataCenterIds();

                    boolean found = false;
                    for( String dataCenterId : ids )
                        if( testDataCenterId.equals(dataCenterId) )
                            found = true;
                    if( !found )
                        fail("Failed to find testDataCenterId in the results of lb.getProviderDataCenterIds()");
                    testVirtualMachineId = tm.getTestVMId(DaseinTestManager.STATEFUL + "-" + testLoadBalancerId + (System.currentTimeMillis()%10000), VmState.RUNNING,true, testDataCenterId);
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        else if( name.getMethodName().equals("addDataCenter") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, tm.getUserName() + "-dsnlb", true);
            if( testLoadBalancerId != null ) {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services != null ) {
                    LoadBalancerSupport support = services.getLoadBalancerSupport();

                    if( support != null ) {
                        try {
                            if( support.getCapabilities().isDataCenterLimited() ) {
                                LoadBalancer lb = support.getLoadBalancer(testLoadBalancerId);

                                if( lb != null ) {
                                    ArrayList<DataCenter> regionDataCenters = new ArrayList<DataCenter>();
                                    String[] dcs = lb.getProviderDataCenterIds();

                                    regionDataCenters.addAll(tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()));

                                    if( dcs.length >= regionDataCenters.size() ) {
                                        support.removeDataCenters(testLoadBalancerId, dcs[0]);
                                        testDataCenterId = dcs[0];
                                    }
                                    else {
                                        for( DataCenter dc : regionDataCenters ) {
                                            boolean included = false;

                                            for( String id : dcs ) {
                                                if( id.equals(dc.getProviderDataCenterId()) ) {
                                                    included = true;
                                                    break;
                                                }
                                            }
                                            if( !included ) {
                                                testDataCenterId = dc.getProviderDataCenterId();
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            else {
                                try {
                                    testDataCenterId = System.getProperty("test.dataCenter");
                                } catch (Throwable ignore) {
                                    // ignore
                                }
                                try {
                                    if (testDataCenterId == null)
                                        testDataCenterId = tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()).iterator().next().getProviderDataCenterId();
                                } catch (Throwable ignore) {
                                    // ignore
                                }
                            }
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }

                    }
                }
            }
        }
        else if( name.getMethodName().equals("removeDataCenter") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, tm.getUserName() + "-dsnlb", true);
            if( testLoadBalancerId != null ) {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services != null ) {
                    LoadBalancerSupport support = services.getLoadBalancerSupport();

                    if( support != null ) {
                        try {
                            if( support.getCapabilities().isDataCenterLimited() ) {
                                LoadBalancer lb = support.getLoadBalancer(testLoadBalancerId);

                                if( lb != null ) {
                                    Iterator<DataCenter> it = tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()).iterator();
                                    String[] dcs = lb.getProviderDataCenterIds();

                                    if( dcs.length >= 2 ) {
                                        testDataCenterId = dcs[0];
                                    }
                                    else {
                                        while( dcs.length < 2 ) {
                                            testDataCenterId = it.next().getProviderDataCenterId();
                                            support.addDataCenters(testLoadBalancerId, testDataCenterId);
                                            if( dcs.length < 1 && it.hasNext() ) {
                                                support.addDataCenters(testLoadBalancerId, it.next().getProviderDataCenterId());
                                            }
                                        }
                                    }
                                }
                            }
                            else {
                                testDataCenterId = tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()).iterator().next().getProviderDataCenterId();
                            }
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }

                    }
                }
            }
        }
        else if( name.getMethodName().equals("removeServer") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, tm.getUserName() + "-dsnlb", true, true);
            NetworkServices net = tm.getProvider().getNetworkServices();

            try {
                if( net != null ) {
                    LoadBalancerSupport support = net.getLoadBalancerSupport();

                    if( support != null ) {
                        ArrayList<String> ids = new ArrayList<String>();

                        for( LoadBalancerEndpoint endpoint : support.listEndpoints(testLoadBalancerId) ) {
                            if( endpoint.getEndpointType().equals(LbEndpointType.VM) ) {
                                ids.add(endpoint.getEndpointValue());
                            }
                        }
                        if( ids.size() > 0 ) {
                            testVirtualMachineId = ids.iterator().next();
                        }
                        else {
                            testVirtualMachineId = tm.getTestVMId(DaseinTestManager.STATEFUL + "_" + testLoadBalancerId + (System.currentTimeMillis()%10000), VmState.RUNNING,  true, testDataCenterId);
                            if( testVirtualMachineId != null ) {
                                support.addServers(testLoadBalancerId, testVirtualMachineId);
                            }
                        }
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        else if( name.getMethodName().equals("removeSSLCertificate") ) {
            testSSLCertificateName = tm.getTestSSLCertificateName(DaseinTestManager.REMOVED, true);
        }
    }

    @After
    public void after() {
        try {
            testLoadBalancerId = null;
            testDataCenterId = null;
            testVirtualMachineId = null;
            testSSLCertificateName = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void createLoadBalancer() throws CloudException, InternalException {
        createLoadBalancer(false);
    }

    @Test
    public void createLoadBalancerWithHttpsListener() throws CloudException, InternalException {
        createLoadBalancer(true);
    }

    private void createLoadBalancer(boolean withHttpsListener) throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        NetworkResources network = DaseinTestManager.getNetworkResources();

        if( network == null ) {
            fail("Failed to initialize network capabilities for tests");
        }
        String id = network.provisionLoadBalancer("provision", tm.getUserName() + "-dsncrlbtest", false, withHttpsListener, false);

        tm.out("New Load Balancer", id);
        assertNotNull("The newly created load balancer ID was null", id);

        LoadBalancer lb = support.getLoadBalancer(id);

        assertNotNull("The newly created load balancer is null", lb);
    }

    @Test
    public void createLoadBalancerWithHealthCheck() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        NetworkResources network = DaseinTestManager.getNetworkResources();

        if( network == null ) {
            fail("Failed to initialize network capabilities for tests");
        }
        
        // Need to see whats the health check NEEDS to be created in here...
        String id = network.provisionLoadBalancer("provision", tm.getUserName() + "-dsncrlbtest", false, false, true);

        tm.out("New Load Balancer", id);
        assertNotNull("The newly created load balancer ID was null", id);

        LoadBalancer lb = support.getLoadBalancer(id);
        assertNotNull(String.format("Load Balancer %s failed to create.", id));

        // lb.getProviderLBHealthCheckId() is null. why?
        LoadBalancerHealthCheck lbhc = support.getLoadBalancerHealthCheck(lb.getProviderLBHealthCheckId(), id);
        assertHealthCheck(id, support, lbhc); 
    }

    /**
     * @see org.dasein.cloud.network.LoadBalancerSupport#modifyHealthCheck(String, org.dasein.cloud.network.HealthCheckOptions)
     * @throws CloudException
     * @throws InternalException
     */
    @Test
    public void modifyHealthCheck() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        NetworkResources network = DaseinTestManager.getNetworkResources();

        if( network == null ) {
            fail("Failed to initialize network capabilities for tests");
        }

        HealthCheckOptions hcOpt = HealthCheckOptions.getInstance(null, null, null, null, LoadBalancerHealthCheck.HCProtocol.TCP, 9876, null, 10, 9, 5, 5);

        String lbId = null;
        String lbhcId = null;
        if( support.getCapabilities().healthCheckRequiresLoadBalancer() ) {
            lbId = network.provisionLoadBalancer("provision", "dsnmodhctest", false, false, true);

            tm.out("New Load Balancer", lbId);
            assertNotNull("The newly created load balancer ID was null", lbId);

            LoadBalancer lb = support.getLoadBalancer(lbId);
            assertNotNull(String.format("Load Balancer %s failed to create.", lbId));
            hcOpt.withProviderLoadBalancerId(lbId);
            lbhcId = lb.getProviderLBHealthCheckId();
        } else {
            LoadBalancerHealthCheck lbhc = support.createLoadBalancerHealthCheck(null, null, null, LoadBalancerHealthCheck.HCProtocol.HTTP, 8090, null, 20, 15, 2, 2);
            lbhcId = lbhc.getProviderLBHealthCheckId();
        }

        LoadBalancerHealthCheck lbhc = support.getLoadBalancerHealthCheck(lbhcId, lbId);
        assertHealthCheck(lbId, support, lbhc);
        LoadBalancerHealthCheck lbhcModified = support.modifyHealthCheck(lbhcId, hcOpt);

        // check correct values are returned - modified as requested
        assertCompareOptionsWithLBHC(hcOpt, lbhcModified);

        // get it again to make sure there was no cheating
        lbhcModified = support.getLoadBalancerHealthCheck(lbhcId, lbId);
        assertCompareOptionsWithLBHC(hcOpt, lbhcModified);
    }

    private void assertCompareOptionsWithLBHC( HealthCheckOptions requested, LoadBalancerHealthCheck actual) {
        assertNotNull("Health check may not be null", actual);
        assertEquals("Failed to modify health check 'path'", requested.getPath(), actual.getPath());
        assertEquals("Failed to modify health check 'protocol'", requested.getProtocol(), actual.getProtocol());
        assertEquals("Failed to modify health check 'port'", requested.getPort(), actual.getPort());
        assertEquals("Failed to modify health check 'interval'", requested.getInterval(), actual.getInterval());
        assertEquals("Failed to modify health check 'timeout'", requested.getTimeout(), actual.getTimeout());
        assertEquals("Failed to modify health check 'healthyCount'", requested.getHealthyCount(), actual.getHealthyCount());
        assertEquals("Failed to modify health check 'unhealthyCount'", requested.getUnhealthyCount(), actual.getUnhealthyCount());
    }

    static void assertHealthCheck( String testLBId, LoadBalancerSupport support, LoadBalancerHealthCheck lbhc ) throws CloudException, InternalException {
        assertNotNull("The LB health check 'ID' may not be null", lbhc.getProviderLBHealthCheckId());
        if( lbhc.getPath() != null ) {
            assertEquals("The LB health check 'path' is incorrect", NetworkResources.TEST_HC_PATH, lbhc.getPath());
        }
        if( lbhc.getHost() != null ) {
            assertEquals("The LB health check 'host' is incorrect", NetworkResources.TEST_HC_HOST, lbhc.getHost());
        }
        assertThat("The LB health check 'healthyCount' should be greater than zero", lbhc.getHealthyCount(), greaterThan(0));
        assertThat("The LB health check 'unhealthyCount' should be greater than zero", lbhc.getUnhealthyCount(), greaterThan(0));
        assertThat("The LB health check 'port' should be greater than zero", lbhc.getPort(), equalTo(NetworkResources.TEST_HC_PORT));
        assertNotNull("The LB health check 'protocol' may not be null", lbhc.getProtocol());
        // assertEquals("The LB health check 'protocol' is incorrect", NetworkResources.TEST_HC_PROTOCOL, lbhc.getProtocol());
        assertNotNull("The LB health check 'providerLoadBalancerIds' may not be null", lbhc.getProviderLoadBalancerIds());
        if( support.getCapabilities().healthCheckRequiresLoadBalancer() ) {
            assertThat("The LB health check 'providerLoadBalancerIds' should have at least one element",
                    lbhc.getProviderLoadBalancerIds().size(), greaterThan(0));
        } else {
            assertEquals("The LB health check 'providerLoadBalancerIds' should have 0 elements",
                    lbhc.getProviderLoadBalancerIds().size(), 0);
        }
    }

    @Test
    public void createInternalLoadBalancer() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        NetworkResources network = DaseinTestManager.getNetworkResources();

        if( network == null ) {
            fail("Failed to initialize network capabilities for tests");
        }
        String id = network.provisionLoadBalancer("provision", tm.getUserName() + "-dsncrintlbtest", true);

        tm.out("New Internal Load Balancer", id);
        assertNotNull("The newly created load balancer ID was null", id);

        LoadBalancer lb = support.getLoadBalancer(id);

        assertNotNull("The newly created load balancer is null", lb);
    }

    @Test
    public void addDataCenter() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testLoadBalancerId != null && testDataCenterId != null ) {
            if( support.getCapabilities().isDataCenterLimited() ) {
                LoadBalancer lb = support.getLoadBalancer(testLoadBalancerId);

                assertNotNull("The test load balancer disappeared prior to the test", lb);

                tm.out("Before", Arrays.toString(lb.getProviderDataCenterIds()));

                support.addDataCenters(testLoadBalancerId, testDataCenterId);

                lb = support.getLoadBalancer(testLoadBalancerId);
                assertNotNull("The test load balancer no longer exists", lb);

                String[] ids = lb.getProviderDataCenterIds();

                tm.out("After", Arrays.toString(ids));

                boolean ok = false;

                for( String dc : ids ) {
                    if( dc.equals(testDataCenterId) ) {
                        ok = true;
                    }
                }
                assertTrue("Failed to find the new data center among the load balancer data centers", ok);
            }
            else {
                try {
                    support.addDataCenters(testLoadBalancerId, testDataCenterId);
                    fail("Should not be able to add data centers in a cloud that is not data center limited");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud is not data center limited");
                }
            }
        }
        else {
            if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void addIP() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        String testIpAddress = "196.91.70.2"; // "162.222.179.154;" // for GCE
        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        boolean ips = false;

        for( LbEndpointType t :support.getCapabilities().listSupportedEndpointTypes() ) {
            if( t.equals(LbEndpointType.IP) ) {
                ips = true;
                break;
            }
        }
        if( testLoadBalancerId != null ) {
            if( support.getCapabilities().supportsAddingEndpoints() && ips ) {
                tm.out("Before", support.listEndpoints(testLoadBalancerId));
                support.addIPEndpoints(testLoadBalancerId, testIpAddress);

                Iterable<LoadBalancerEndpoint> endpoints = support.listEndpoints(testLoadBalancerId);

                tm.out("After", endpoints);
                boolean ok = false;

                for( LoadBalancerEndpoint endpoint : endpoints ) {
                    if( endpoint.getEndpointType().equals(LbEndpointType.IP) && endpoint.getEndpointValue().equals(testIpAddress) ) {
                        ok = true;
                    }
                }
                assertTrue("Failed to find the new IP address among the load balancer endpoints", ok);
            }
            else {
                try {
                    support.addIPEndpoints(testLoadBalancerId, testIpAddress);
                    fail("Should not be able to add endpoints in this cloud, but the operation completed");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud does not support adding IP addresses post-create");
                }
            }
        }
        else {
            if( !ips ) {
                tm.ok("Load balancers in this cloud do not support IP endpoints");
            }
            else if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void addServer() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        boolean vms = false;

        for( LbEndpointType t :support.getCapabilities().listSupportedEndpointTypes() ) {
            if( t.equals(LbEndpointType.VM) ) {
                vms = true;
                break;
            }
        }
        if( testLoadBalancerId != null && testVirtualMachineId != null ) {
            if( support.getCapabilities().supportsAddingEndpoints() && vms ) {
                tm.out("Before", support.listEndpoints(testLoadBalancerId));
                support.addServers(testLoadBalancerId, testVirtualMachineId);

                Iterable<LoadBalancerEndpoint> endpoints = support.listEndpoints(testLoadBalancerId);

                tm.out("After", endpoints);
                boolean ok = false;

                for( LoadBalancerEndpoint endpoint : endpoints ) {
                    if( endpoint.getEndpointType().equals(LbEndpointType.VM) && endpoint.getEndpointValue().equals(testVirtualMachineId) ) {
                        ok = true;
                    }
                }
                assertTrue("Failed to find the new server among the load balancer endpoints", ok);
            }
            else {
                try {
                    support.addServers(testLoadBalancerId, testVirtualMachineId);
                    fail("Should not be able to add endpoints in this cloud, but the operation completed");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud does not support adding virtual machines post-create");
                }
            }
        }
        else {
            if( !vms ) {
                tm.ok("Load balancers in this cloud do not support virtual machine endpoints");
            }
            else if( support.isSubscribed() ) {
                StringBuilder sb = new StringBuilder();
                if( testLoadBalancerId == null ) {
                    sb.append("No test load balancer for " + name.getMethodName()).append(". ");
                }
                if( testVirtualMachineId == null ) {
                    sb.append("No test VM for " + name.getMethodName()).append(".");
                }
                fail(sb.toString());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void removeDataCenter() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testLoadBalancerId != null && testDataCenterId != null ) {
            if( support.getCapabilities().isDataCenterLimited() ) {
                LoadBalancer lb = support.getLoadBalancer(testLoadBalancerId);

                assertNotNull("The test load balancer disappeared during the test", lb);

                tm.out("Before", Arrays.toString(lb.getProviderDataCenterIds()));
                support.removeDataCenters(testLoadBalancerId, testDataCenterId);

                lb = support.getLoadBalancer(testLoadBalancerId);

                assertNotNull("The test load balancer no longer exists", lb);

                String[] ids = lb.getProviderDataCenterIds();
                tm.out("After", Arrays.toString(ids));

                boolean ok = false;

                for( String dc : ids ) {
                    if( dc.equals(testDataCenterId) ) {
                        ok = true;
                    }
                }
                assertFalse("The test data center is still present among the load balancer data centers", ok);
            }
            else {
                try {
                    support.removeDataCenters(testLoadBalancerId, testDataCenterId);
                    fail("Should not be able to remove data centers in a cloud that is not data center limited");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud is not data center limited");
                }
            }
        }
        else {
            if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void removeIP() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        boolean ips = false;

        for( LbEndpointType t :support.getCapabilities().listSupportedEndpointTypes() ) {
            if( t.equals(LbEndpointType.IP) ) {
                ips = true;
                break;
            }
        }
        if( testLoadBalancerId != null ) {
            if( support.getCapabilities().supportsAddingEndpoints() && ips ) {
                tm.out("Before", support.listEndpoints(testLoadBalancerId));

                support.removeIPEndpoints(testLoadBalancerId, "197.41.20.2");

                Iterable<LoadBalancerEndpoint> endpoints = support.listEndpoints(testLoadBalancerId);

                tm.out("After", endpoints);

                boolean ok = false;

                for( LoadBalancerEndpoint endpoint : endpoints ) {
                    if( endpoint.getEndpointType().equals(LbEndpointType.IP) && endpoint.getEndpointValue().equals("197.41.20.2") ) {
                        ok = true;
                    }
                }
                assertFalse("The test IP endpoint still exists among the load balancer endpoints", ok);
            }
            else {
                try {
                    support.removeIPEndpoints(testLoadBalancerId, "197.41.20.2");
                    fail("Should not be able to remove endpoints in this cloud, but the operation completed");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud does not support removing IP addresses post-create");
                }
            }
        }
        else {
            if( !ips ) {
                tm.ok("Load balancers in this cloud do not support IP endpoints");
            }
            else if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void removeServer() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        boolean vms = false;

        for( LbEndpointType t :support.getCapabilities().listSupportedEndpointTypes() ) {
            if( t.equals(LbEndpointType.VM) ) {
                vms = true;
                break;
            }
        }
        if( testLoadBalancerId != null && testVirtualMachineId != null ) {
            if( support.getCapabilities().supportsAddingEndpoints() && vms ) {
                tm.out("Before", support.listEndpoints(testLoadBalancerId));

                support.removeServers(testLoadBalancerId, testVirtualMachineId);

                Iterable<LoadBalancerEndpoint> endpoints = support.listEndpoints(testLoadBalancerId);

                tm.out("After", endpoints);

                boolean ok = false;

                for( LoadBalancerEndpoint endpoint : support.listEndpoints(testLoadBalancerId) ) {
                    if( endpoint.getEndpointType().equals(LbEndpointType.VM) && endpoint.getEndpointValue().equals(testVirtualMachineId) ) {
                        ok = true;
                    }
                }
                assertFalse("The test virtual machine is still present among the load balancer endpoints", ok);
            }
            else {
                try {
                    support.removeServers(testLoadBalancerId, testVirtualMachineId);
                    fail("Should not be able to remove endpoints in this cloud, but the operation completed");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud does not support adding/removing virtual machines post-create");
                }
            }
        }
        else {
            if( !vms ) {
                tm.ok("Load balancers in this cloud do not support virtual machine endpoints");
            }
            else if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void removeLoadBalancer() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testLoadBalancerId != null ) {
            LoadBalancer lb = support.getLoadBalancer(testLoadBalancerId);

            tm.out("Before", (lb == null) ? LoadBalancerState.TERMINATED : lb.getCurrentState());
            assertNotNull("The load balancer is null prior to the test", lb);
            support.removeLoadBalancer(testLoadBalancerId);
            lb = support.getLoadBalancer(testLoadBalancerId);
            LoadBalancerState s = (lb == null) ? LoadBalancerState.TERMINATED : lb.getCurrentState();

            tm.out("After", s);
            assertEquals("The load balancer still exists after the test", LoadBalancerState.TERMINATED, s);
        }
        else {
            if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void createLoadBalancerHealthCheck() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        //TODO: Clean these values up
        LoadBalancerHealthCheck lbhc = support.createLoadBalancerHealthCheck(HealthCheckOptions.getInstance("foobar", "foobardesc", testLoadBalancerId, "www.mydomain.com", LoadBalancerHealthCheck.HCProtocol.HTTP, 80, "/ping", 30, 3, 2, 2));

        if( support.getCapabilities().healthCheckRequiresLoadBalancer() ){
            if( testLoadBalancerId != null ){
                assertNotNull("Could not create a healthcheck for loadbalancer", lbhc);
            }
            else {
                if( support.isSubscribed() ) {
                    fail("No test load balancer for " + name.getMethodName());
                }
                else {
                    tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
                }
            }
        }
        else {
            assertNotNull("Could not create a standalone healthcheck", lbhc);
        }
        support.removeLoadBalancerHealthCheck("foobar");
    }

    @Test
    public void createSSLCertificate() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("SSL certificates are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        NetworkResources network = DaseinTestManager.getNetworkResources();

        if( network == null ) {
            fail("Failed to initialize network capabilities for tests");
        }
        String id = network.provisionSSLCertificate("provision", tm.getUserName() + "-dsnssltest");

        tm.out("New SSL certificate", id);
        assertNotNull("The newly created SSL certificate ID was null", id);

        SSLCertificate certificate = support.getSSLCertificate(id);

        assertNotNull("The newly created SSL certificate is null", certificate);
    }

    @Test
    public void removeSSLCertificate() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("SSL certificates are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testSSLCertificateName != null ) {
            SSLCertificate certificate = support.getSSLCertificate(testSSLCertificateName);

            assertNotNull("The SSL certificate is null prior to the test", certificate);
            support.removeSSLCertificate(testSSLCertificateName);
            certificate = support.getSSLCertificate(testSSLCertificateName);
            assertNull("The SSL certificate still exists after removing", certificate);
        }
        else {
            if( support.isSubscribed() ) {
                fail("No test SSL certificate for " + name.getMethodName());
            }
            else {
                tm.ok("SSL certificate support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void listLoadBalancers() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        NetworkResources network = DaseinTestManager.getNetworkResources();

        if( network == null ) {
            fail("Failed to initialize network capabilities for tests");
        }

        Iterable<LoadBalancer> loadBalancerList = support.listLoadBalancers();
        int lbCount1 = 0;
        int lbCount2 = 0;
        for (LoadBalancer lb : loadBalancerList) {
            lbCount1++;
        }

        String id1 = network.provisionLoadBalancer("provision", tm.getUserName() + "-dsncrlbtest1", false, false, true);
        tm.out("New Load Balancer", id1);
        assertNotNull("The newly created load balancer ID was null", id1);
        String id2 = network.provisionLoadBalancer("provision", tm.getUserName() + "-dsncrlbtest1", false, false, true);
        tm.out("New Load Balancer", id2);
        assertNotNull("The newly created load balancer ID was null", id2);
        boolean lb1_found = false;
        boolean lb2_found = false;
        loadBalancerList = support.listLoadBalancers();
        for (LoadBalancer lb : loadBalancerList) {
            lbCount2++;
            if (lb.getName().equals(id1)) {
                LoadBalancerHealthCheck lbhc = support.getLoadBalancerHealthCheck(lb.getProviderLBHealthCheckId(), lb.getName());
                assertHealthCheck(id1, support, lbhc); 
                lb1_found = true;
            } else if (lb.getName().equals(id2)) {
                lb2_found = true;
            }
        }
        assertEquals("Failed to find LoadBalancer 1", true, lb1_found);
        assertEquals("Failed to find LoadBalancer 2", true, lb2_found);
        assertEquals("Second count of load balancers should have been 2 more than first", lbCount1 + 2, lbCount2);
    }
}
