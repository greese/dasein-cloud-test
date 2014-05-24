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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.network.*;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.dasein.cloud.test.network.StatefulLoadBalancerTests.assertHealthCheck;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 3/8/13 4:34 PM</p>
 *
 * @author George Reese
 */
public class StatelessLoadBalancerTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessLoadBalancerTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testLoadBalancerId;
    private String testSslCertificateName;

    public StatelessLoadBalancerTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATELESS, tm.getUserName() + "-dsnlb", false);
        testSslCertificateName = tm.getTestSSLCertificateName(DaseinTestManager.STATELESS, false);
    }

    @After
    public void after() {
        tm.end();
    }

    private void assertListener(@Nonnull LoadBalancerSupport support, @Nonnull LbListener listener) throws CloudException, InternalException {
        assertNotNull("The listener algorithm may not be null", listener.getAlgorithm());

        boolean ok = false;

        for( LbAlgorithm algorithm : support.getCapabilities().listSupportedAlgorithms() ) {
            if( algorithm.equals(listener.getAlgorithm()) ) {
                ok = true;
            }
        }
        assertTrue("The algorithm associated with this listener is not a supported algorithm", ok);

        ok = false;
        assertNotNull("The listener network protocol may not be null", listener.getNetworkProtocol());

        for( LbProtocol protocol : support.getCapabilities().listSupportedProtocols() ) {
            if( protocol.equals(listener.getNetworkProtocol()) ) {
                ok = true;
            }
        }
        assertTrue("The network protocol associated with this listener is not a supported protocol", ok);

        ok = false;
        assertNotNull("The listener session persistence may not be null", listener.getPersistence());

        for( LbPersistence p : support.getCapabilities().listSupportedPersistenceOptions() ) {
            if( p.equals(listener.getPersistence()) ) {
                ok = true;
            }
        }
        assertTrue("The session persistence option associated with this listener is not a supported option", ok);

        if( listener.getPersistence().equals(LbPersistence.COOKIE) ) {
            assertNotNull("When the session persistence is set to server cookie, a cookie value must be set", listener.getCookie());
        }

        if( LbProtocol.HTTP.equals( listener.getNetworkProtocol() ) ) {
            assertNull("HTTP listener cannot have an SSL certificate", listener.getSslCertificateName());
        }
        else if( LbProtocol.HTTPS.equals( listener.getNetworkProtocol() ) ) {
            assertNotNull("HTTPS listener must have an SSL certificate", listener.getSslCertificateName());
        }
    }

    private void assertLoadBalancer(@Nonnull LoadBalancerSupport support, @Nonnull LoadBalancer lb) throws CloudException, InternalException {
        assertNotNull("The load balancer ID may not be null", lb.getProviderLoadBalancerId());
        assertNotNull("The load balancer state may not be null", lb.getCurrentState());
        assertNotNull("The load balancer name may not be null", lb.getName());
        assertNotNull("The load balancer description may not be null", lb.getDescription());
        assertNotNull("The load balancer owner may not be null", lb.getProviderOwnerId());
        assertNotNull("The load balancer region may not be null", lb.getProviderRegionId());
        assertEquals("The load balancer region must match the region for this context", tm.getContext().getRegionId(), lb.getProviderRegionId());

        String[] dcs = lb.getProviderDataCenterIds();

        assertNotNull("The list of data centers to which the load balancer is assigned may not be null", dcs);
        if( support.getCapabilities().isDataCenterLimited() ) {
            assertTrue("There must be at least one data center associated with the load balancer", dcs.length > 0);
        }
        assertNotNull("The load balancer address type may not be null", lb.getAddressType());
        assertNotNull("The load balancer address may not be null", lb.getAddress());

        int[] ports = lb.getPublicPorts();

        assertNotNull("The list of public ports may notbe null", ports);

        IPVersion[] v = lb.getSupportedTraffic();

        assertNotNull("The IP traffic associated with the load balancer must not be null", v);
        assertTrue("There must be at least one IP version supported for the load balancer", v.length > 0);

        LbListener[] listeners = lb.getListeners();

        assertNotNull("The list of listeners may not be null", listeners);

        if( support.getCapabilities().identifyListenersOnCreateRequirement().equals(Requirement.REQUIRED) ) {
            assertTrue("There must be at least one public port associated with the load balancer", ports.length > 0);
            assertTrue("There must be at least one listener associated with the load balancer", listeners.length > 0);
        }

        for( LbListener listener : listeners ) {
            assertListener(support, listener);
        }
    }

    private void assertSSLCertificate(@Nonnull SSLCertificate certificate, boolean isBodyRequired)
            throws CloudException, InternalException {
        if (isBodyRequired) {
            assertNotNull("The SSL certificate body may not be null", certificate.getCertificateBody());
        }
        assertNotNull("The SSL certificate ID may not be null", certificate.getCertificateName());
        assertNotNull("The SSL certificate provider ID may not be null", certificate.getProviderCertificateId());
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
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
        tm.out("Subscribed", support.isSubscribed());
        tm.out("Term for Load Balancer", support.getCapabilities().getProviderTermForLoadBalancer(Locale.getDefault()));
        tm.out("Data Center Limited", support.getCapabilities().isDataCenterLimited());
        tm.out("Address Type", support.getCapabilities().getAddressType());
        tm.out("Provider-assigned Address", support.getCapabilities().isAddressAssignedByProvider());
        tm.out("Endpoints on Create", support.getCapabilities().identifyEndpointsOnCreateRequirement());
        tm.out("Listeners on Create", support.getCapabilities().identifyListenersOnCreateRequirement());
        tm.out("Max Public Ports", support.getCapabilities().getMaxPublicPorts() == 0 ? "Unlimited" : String.valueOf(support.getCapabilities().getMaxPublicPorts()));
        tm.out("Endpoint Types", support.getCapabilities().listSupportedEndpointTypes());
        tm.out("Algorithms", support.getCapabilities().listSupportedAlgorithms());
        tm.out("Protocols", support.getCapabilities().listSupportedProtocols());
        tm.out("Persistence Options", support.getCapabilities().listSupportedPersistenceOptions());
        tm.out("Supported Traffic", support.getCapabilities().listSupportedIPVersions());
        tm.out("Supports Monitoring", support.getCapabilities().supportsMonitoring());
        tm.out("Can Add Endpoints", support.getCapabilities().supportsAddingEndpoints());
        tm.out("Supports Multiple IP Versions", support.getCapabilities().supportsMultipleTrafficTypes());

        assertNotNull("The provider term for a load balancer may not be null", support.getCapabilities().getProviderTermForLoadBalancer(Locale.getDefault()));
        assertNotNull("The address type may not be null", support.getCapabilities().getAddressType());
        if( LoadBalancerAddressType.DNS.equals(support.getCapabilities().getAddressType()) ) {
            assertTrue("DNS-based load balancers must have the load balancer address assigned by the cloud provider", support.getCapabilities().isAddressAssignedByProvider());
        }
        else if( !support.getCapabilities().isAddressAssignedByProvider() ) {
            IpAddressSupport ipSupport = services.getIpAddressSupport();

            assertNotNull("If IP addresses are not assigned by a provider, there must be IP address support", ipSupport);

            boolean requestable = false;

            for( IPVersion v : support.getCapabilities().listSupportedIPVersions() ) {
                if( ipSupport.getCapabilities().isRequestable(v) ) {
                    requestable = true;
                    break;
                }
            }
            assertTrue("IP addresses must be requestable when IP addresses for load balancers are not provider assigned", requestable);
        }
        assertNotNull("The requirement level for having endpoints when creating a load balancer cannot be null", support.getCapabilities().identifyEndpointsOnCreateRequirement());
        assertNotNull("The requirement level for having listeners when creating a load balancer cannot be null", support.getCapabilities().identifyListenersOnCreateRequirement());
        assertTrue("The maximum number of public ports must be a positive number or 0 for unlimited", support.getCapabilities().getMaxPublicPorts() >= 0);
        assertTrue("There must be at least one supported endpoint type", support.getCapabilities().listSupportedEndpointTypes().iterator().hasNext());
        assertTrue("There must be at least one supported algorithm", support.getCapabilities().listSupportedAlgorithms().iterator().hasNext());
        assertTrue("There must be at least one supported protocol", support.getCapabilities().listSupportedProtocols().iterator().hasNext());
        assertTrue("There must be at least one supported persistence option", support.getCapabilities().listSupportedPersistenceOptions().iterator().hasNext());
        assertTrue("There must be at least one supported IP version", support.getCapabilities().listSupportedIPVersions().iterator().hasNext());
        assertTrue("If you are not creating endpoints at LB create, you need the ability to add them after load balancer creation", support.getCapabilities().identifyEndpointsOnCreateRequirement().equals(Requirement.REQUIRED) || support.getCapabilities().supportsAddingEndpoints());
    }

    @Test
    public void getBogusLoadBalancer() throws CloudException, InternalException {
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
        LoadBalancer lb = support.getLoadBalancer(UUID.randomUUID().toString());

        tm.out("Bogus Load Balancer", lb);
        assertNull("Found a valid load balancer for a bogus load balancer ID", lb);
    }

    @Test
    public void getLoadBalancer() throws CloudException, InternalException {
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

            tm.out("Load Balancer", lb);
            assertNotNull("No load balancer was found for the test load balancer ID", lb);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Test was not run because this account is not subscribed for load balancers");
            }
            else {
                fail("No test load balancer exists for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void loadBalancerContent() throws CloudException, InternalException {
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

            assertNotNull("No load balancer was found for the test load balancer ID", lb);

            tm.out("Load Balancer ID", lb.getProviderLoadBalancerId());
            tm.out("Current State", lb.getCurrentState());
            tm.out("Name", lb.getName());
            tm.out("Owner Account", lb.getProviderOwnerId());
            tm.out("Region ID", lb.getProviderRegionId());
            tm.out("Data Center IDs", Arrays.toString(lb.getProviderDataCenterIds()));
            tm.out("Created", new Date(lb.getCreationTimestamp()));
            tm.out("Address Type", lb.getAddressType());
            tm.out("Address", lb.getAddress());
            tm.out("Public Ports", Arrays.toString(lb.getPublicPorts()));
            tm.out("IP Versions", Arrays.toString(lb.getSupportedTraffic()));
            tm.out("Listeners", Arrays.toString(lb.getListeners()));
            tm.out("Description", lb.getDescription());

            assertLoadBalancer(support, lb);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Test was not run because this account is not subscribed for load balancers");
            }
            else {
                fail("No test load balancer exists for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void listEndpoints() throws CloudException, InternalException {
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

            assertNotNull("No load balancer was found for the test load balancer ID", lb);

            Iterable<LoadBalancerEndpoint> endpoints = support.listEndpoints(testLoadBalancerId);
            int count = 0;

            assertNotNull("The list of load balancer endpoints may not be null", endpoints);
            if( support.getCapabilities().identifyEndpointsOnCreateRequirement().equals(Requirement.REQUIRED) ) {
                assertTrue("There must be at least one endpoint associated with this load balancer", endpoints.iterator().hasNext());
            }
            for( LoadBalancerEndpoint endpoint : endpoints ) {
                count++;
                tm.out("Endpoint", endpoint);
            }
            tm.out("Endpoint Count", count);
            if( count < 1 ) {
                tm.warn("Unable to test endpoints appropriately due to the lack of endpoints");
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Test was not run because this account is not subscribed for load balancers");
            }
            else {
                fail("No test load balancer exists for the test " + name.getMethodName());
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
        Iterable<LoadBalancer> loadBalancers = support.listLoadBalancers();
        int count = 0;

        assertNotNull("The list of load balancers may not be null", loadBalancers);
        for( LoadBalancer lb : loadBalancers ) {
            count++;
            tm.out("Load Balancer", lb);
        }
        tm.out("Load Balancer Count", count);

        if( !support.isSubscribed() ) {
            assertEquals("The load balancer count should be zero since this account is not subscribed to this service", 0, count);
        }
        else if( count == 0 ) {
            tm.warn("This test is likely invalid as no load balancers were provided in the results for validation");
        }
        if( count > 0 ) {
            for( LoadBalancer lb : loadBalancers ) {
                assertLoadBalancer(support, lb);
            }
        }
    }

    @Test
    public void listLoadBalancerStatus() throws CloudException, InternalException {
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
        Iterable<ResourceStatus> loadBalancers = support.listLoadBalancerStatus();
        int count = 0;

        assertNotNull("The list of load balancers may not be null", loadBalancers);
        for( ResourceStatus lb : loadBalancers ) {
            count++;
            tm.out("Load Balancer Status", lb);
        }
        tm.out("Load Balancer Status Count", count);

        if( !support.isSubscribed() ) {
            assertEquals("The load balancer status count should be zero since this account is not subscribed to this service", 0, count);
        }
        else if( count == 0 ) {
            tm.warn("This test is likely invalid as no load balancer status was provided in the results for validation");
        }
    }

    @Test
    public void compareLoadBalancerListAndStatus() throws CloudException, InternalException {
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
        HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
        Iterable<LoadBalancer> loadBalancers = support.listLoadBalancers();
        Iterable<ResourceStatus> status = support.listLoadBalancerStatus();

        assertNotNull("listLoadBalancers() must return at least an empty collections and may not be null", loadBalancers);
        assertNotNull("listLoadBalancerStatus() must return at least an empty collection and may not be null", status);
        for( ResourceStatus s : status ) {
            Map<String,Boolean> current = map.get(s.getProviderResourceId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(s.getProviderResourceId(), current);
            }
            current.put("status", true);
        }
        for( LoadBalancer lb : loadBalancers ) {
            Map<String,Boolean> current = map.get(lb.getProviderLoadBalancerId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(lb.getProviderLoadBalancerId(), current);
            }
            current.put("lb", true);
        }
        for( Map.Entry<String,Map<String,Boolean>> entry : map.entrySet() ) {
            Boolean s = entry.getValue().get("status");
            Boolean l = entry.getValue().get("lb");

            assertTrue("Status and load balancer lists do not match for " + entry.getKey(), s != null && l != null && s && l);
        }
        tm.out("Matches");
    }

    @Test
    public void getBogusSSLCertificate() throws CloudException, InternalException {
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
        SSLCertificate sslCertificate = support.getSSLCertificate(UUID.randomUUID().toString());

        tm.out("Bogus SSL certificate", sslCertificate);
        assertNull("Found a valid SSL certificate for a bogus ID", sslCertificate);
    }

    @Test
    public void getSSLCertificate() throws CloudException, InternalException {
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
        if( testSslCertificateName != null ) {
            SSLCertificate certificate = support.getSSLCertificate(testSslCertificateName);

            tm.out("SSL Certificate", certificate);
            assertNotNull("No SSL certificate was found for the test ID", certificate);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Test was not run because this account is not subscribed for SSL certificates");
            }
            else {
                fail("No test SSL certificate exists for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void sslCertificateContent() throws CloudException, InternalException {
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
        if( testSslCertificateName != null ) {
            SSLCertificate certificate = support.getSSLCertificate(testSslCertificateName);

            assertNotNull("No SSL certificate was found for the test ID", certificate);

            tm.out("SSL certificate name", certificate.getCertificateName());
            tm.out("SSL certificate provider ID", certificate.getProviderCertificateId());
            tm.out("SSL certificate upload date", certificate.getCreatedTimestamp());
            tm.out("SSL certificate path", certificate.getPath());
            tm.out("SSL certificate chain", certificate.getCertificateChain());
            tm.out("SSL certificate body", certificate.getCertificateBody());

            assertSSLCertificate(certificate, true);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Test was not run because this account is not subscribed for SSL certificates");
            }
            else {
                fail("No test SSL certificate exists for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void listSSLCertificates() throws CloudException, InternalException {
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
        Iterable<SSLCertificate> certificates = support.listSSLCertificates();
        int count = 0;

        assertNotNull("The list of SSL certificates may not be null", certificates);
        for( SSLCertificate certificate : certificates ) {
            count++;
            tm.out("SSL certificate", certificate);
        }
        tm.out("SSL certificates count", count);

        if( !support.isSubscribed() ) {
            assertEquals("The SSL certificate count should be zero since this account is not subscribed to this service", 0, count);
        }
        else if( count == 0 ) {
            tm.warn("This test is likely invalid as no SSL certificates were provided in the results for validation");
        }
        if( count > 0 ) {
            for( SSLCertificate certificate : certificates ) {
                assertSSLCertificate(certificate, false);
            }
        }
    }

    @Test
    public void listLoadBalancerHealthChecks() throws CloudException, InternalException {
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
        Iterable<LoadBalancerHealthCheck> healthChecks = support.listLBHealthChecks(null);
        int count = 0;

        assertNotNull("The list of LB health checks may not be null", healthChecks);
        for( LoadBalancerHealthCheck lbhc : healthChecks ) {
            count++;
            tm.out("LB Health Check", lbhc);
        }
        tm.out("LB Health Check Count", count);

        if( !support.isSubscribed() ) {
            assertEquals("The LB health check count should be zero since this account is not subscribed to this service", 0, count);
        }
        else if( count == 0 ) {
            tm.warn("This test is likely invalid as no LB health checks were provided in the results for validation");
        }
        boolean found = false;
        for( LoadBalancerHealthCheck lbhc : healthChecks ) {
            if( NetworkResources.TEST_HC_PATH.equals(lbhc.getPath())
                    && NetworkResources.TEST_HC_PROTOCOL.equals(lbhc.getProtocol())
                    && NetworkResources.TEST_HC_PORT == lbhc.getPort() ) {
                assertHealthCheck(testLoadBalancerId, support, lbhc);
                found = true;
                break;
            }
        }
        assertTrue("Unable to find the test load balancer in the returned list", found);
    }

}
