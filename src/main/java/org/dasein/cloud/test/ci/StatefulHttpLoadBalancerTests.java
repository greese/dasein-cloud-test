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

package org.dasein.cloud.test.ci;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ci.CIServices;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancer;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancer.BackendService;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancer.ForwardingRule;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancer.HealthCheck;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancer.TargetHttpProxy;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancer.UrlSet;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancerFilterOptions;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancerSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Tests support for Dasein Cloud Replicapools which represent complex, multi-resource groups.
 */
public class StatefulHttpLoadBalancerTests {
    static private DaseinTestManager tm;
    private String testTopologyId;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulHttpLoadBalancerTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    public StatefulHttpLoadBalancerTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testTopologyId = tm.getTestTopologyId(DaseinTestManager.STATELESS, false);
    }

    @After
    public void after() {
        tm.end();
    }

    private void verifyConvergedHttpLoadBalancerNameDescription(ConvergedHttpLoadBalancer createdHttpLoadBalancer, String name, String description, String convergedHttpLoadBalancerSelfUrl) {
        assertTrue("ConvergedHttpLoadBalancer name does not match expected.", createdHttpLoadBalancer.getName().equals(name));
        assertTrue("ConvergedHttpLoadBalancer description does not match expected.", createdHttpLoadBalancer.getDescription().equals(description));
        assertTrue("ConvergedHttpLoadBalancer selfUrl does not match expected.", createdHttpLoadBalancer.getSelfLink().equals(convergedHttpLoadBalancerSelfUrl));
    }

    private void verifyConvergedHttpLoadBalancerUrlSetPresent(ConvergedHttpLoadBalancer createdHttpLoadBalancer, String name, String description, String hostMatchPatterns, Map<String, String> pathMap) {
        boolean found = false;
        List<UrlSet> urlSets = createdHttpLoadBalancer.getUrlSets();
        if (null != urlSets) {
            for (UrlSet urlSet : urlSets) {
                if (urlSet.getName().equals(name)) {
                    found = true;
                    //assertTrue("urlSet description does not match.", urlSet.getDescription().equals(description)); //GCE does not support
                    assertTrue("urlSet hostMatchPatterns does not match.", urlSet.getHostMatchPatterns().equals(hostMatchPatterns));
                    for (String key : pathMap.keySet()) {
                        assertTrue(key + " not present in urlSet pathMap.", (urlSet.getPathMap().get(key).equals(pathMap.get(key))));
                    }
                }
            }
        }
        assertTrue("urlSet not found.", found);
    }

    private void verifyConvergedHttpLoadBalancerBackendServicePresent(ConvergedHttpLoadBalancer createdHttpLoadBalancer, String name, String description, String selfUrl) {
        boolean found = false;
        List<BackendService> backendServices = createdHttpLoadBalancer.getBackendServices();
        if (null != backendServices) {
            for (BackendService backendService : backendServices) {
                if (backendService.getName().equals(name)) {
                    found = true;
                    //assertTrue("backendService description does not match.", backendService.getDescription().equals(description));
                    assertTrue("backendService selfLink does not match.", backendService.getSelfLink().equals(selfUrl));
                }
            }
        }
        assertTrue("BackendService not found.", found);
    }

    private void verifyConvergedHttpLoadBalancerTargetHttpProxyPresent(ConvergedHttpLoadBalancer createdHttpLoadBalancer, String name, String description) {
        boolean found = false;
        List<TargetHttpProxy> targetHttpProxies = createdHttpLoadBalancer.getTargetHttpProxies();
        if (null != targetHttpProxies) {
            for (TargetHttpProxy targetHttpProxy : targetHttpProxies) {
                found = true;
                assertTrue("targetHttpProxy name does not match.", targetHttpProxy.getName().equals(name));
                assertTrue("targetHttpProxy description does not match.", targetHttpProxy.getDescription().equals(description));
            }
        }
        assertTrue("TargetHttpProxy not found.", found);
    }

    private void verifyConvergedHttpLoadBalancerForwardingRulePresent(ConvergedHttpLoadBalancer createdHttpLoadBalancer, String name, String description, String ipAddress, String ipProtocol, String portRange, String target) {
        boolean found = false;
        List<ForwardingRule> forwardingRules = createdHttpLoadBalancer.getForwardingRules();
        if (null != forwardingRules) {
            for (ForwardingRule forwardingRule : forwardingRules) {
                found = true;
                assertTrue("forwardingRule name does not match.", forwardingRule.getName().equals(name));
                assertTrue("forwardingRule description does not match.", forwardingRule.getDescription().equals(description));
                //assertTrue("forwardingRule ipAddress does not match.", forwardingRule.getIpAddress().equals(ipAddress));
                assertTrue("forwardingRule ipProtocol does not match.", forwardingRule.getIpProtocol().equals(ipProtocol));
                assertTrue("forwardingRule portRange does not match.", forwardingRule.getPortRange().equals(portRange));
                assertTrue("forwardingRule target does not match.", forwardingRule.getTarget().equals(target));
            }
        }
        assertTrue("forwardingRule not found.", found);
    }

    @Test
    public void listHttpLoadBalancers() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if (services != null) {
            if (services.hasConvergedHttpLoadBalancerSupport()) {
                ConvergedHttpLoadBalancerSupport support = services.getConvergedHttpLoadBalancerSupport();
                if (support != null) {
                    ConvergedHttpLoadBalancerFilterOptions options = new ConvergedHttpLoadBalancerFilterOptions();

                    Iterable<String> result = support.listConvergedHttpLoadBalancers();

                    // ConvergedHttpLoadBalancer likely will need to be populated with data from other calls...

                    tm.out("Subscribed", support.isSubscribed());
                    //tm.out("Public Library", support.supportsPublicLibrary());
                } else {
                    tm.ok(tm.getProvider().getCloudName() + " does not support topologies");
                }
            }
        } else {
            tm.ok(tm.getProvider().getCloudName() + " does not support compute services");
        }
    }

    @Test
    public void getConvergedHttpLoadBalancer() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if (services != null) {
            if (services.hasConvergedHttpLoadBalancerSupport()) {
                ConvergedHttpLoadBalancerSupport support = services.getConvergedHttpLoadBalancerSupport();
                if (support != null) {

                    ConvergedHttpLoadBalancer result = support.getConvergedHttpLoadBalancer("test-http-load-balancer");
                }
            }
        }
    }

    @Test
    public void removeHttpLoadBalancers() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if (services != null) {
            if (services.hasConvergedHttpLoadBalancerSupport()) {
                ConvergedHttpLoadBalancerSupport support = services.getConvergedHttpLoadBalancerSupport();
                if (support != null) {

                    support.removeConvergedHttpLoadBalancers("test-http-load-balancer");
                }
            }
        }
    }


    // withExistingXXXXX()
    @Test
    public void createHttpLoadBalancer() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if (services != null) {
            if (services.hasConvergedHttpLoadBalancerSupport()) {
                ConvergedHttpLoadBalancerSupport support = services.getConvergedHttpLoadBalancerSupport();
                if (support != null) {
                    String instanceGroup1 = "https://www.googleapis.com/resourceviews/v1beta2/projects/qa-project-2/zones/europe-west1-b/resourceViews/instance-group-1";
                    String instanceGroup2 = "https://www.googleapis.com/resourceviews/v1beta2/projects/qa-project-2/zones/us-central1-f/resourceViews/instance-group-2";
                    Map<String, String> pathMap = new HashMap<String, String>();
                    String defaultBackend = "test-backend-1";
                    String backend2 = "test-backend-2";
                    String backend3 = "test-backend-3";
                    pathMap.put("/*", defaultBackend);
                    pathMap.put("/video, /video/*", backend2);
                    pathMap.put("/audio, /audio/*", backend3);
                    String healthCheck1 = "test-health-check";
                    String targetProxy1 = "target-proxy-1";
                    String targetProxy2 = "target-proxy-2";
                    ConvergedHttpLoadBalancer withExperimentalConvergedHttpLoadbalancerOptions = ConvergedHttpLoadBalancer
                            .getInstance("test-http-load-balancer", "test-http-load-balancer-description", defaultBackend)
                            .withHealthCheck(healthCheck1, healthCheck1 + "-description", null, 80, "/", 5, 5, 2, 2) //ONLY ONE ALLOWED
                            .withBackendService(defaultBackend, defaultBackend + "-description", 80, "http", "HTTP", new String[] {healthCheck1}, new String[] {instanceGroup1}, 30)
                            .withBackendService(backend2, backend2 + "-description", 80, "http", "HTTP", new String[] {healthCheck1}, new String[] {instanceGroup2}, 30)
                            .withBackendService(backend3, backend3 + "-description", 80, "http", "HTTP", new String[] {healthCheck1}, new String[] {instanceGroup1, instanceGroup2}, 30)
                            .withUrlSet("url-map-1", "url-map-description", "*", pathMap)
                            .withUrlSet("url-map-2", "url-map-2-description", "*.net", pathMap)
                            .withTargetHttpProxy(targetProxy1, targetProxy1 + "-description")
                            .withTargetHttpProxy(targetProxy2, targetProxy2 + "-description")
                            .withForwardingRule(targetProxy1 + "-fr", targetProxy1 + "-fr-description", null, "TCP", "80", targetProxy1)
                            .withForwardingRule(targetProxy2 + "-fr", targetProxy2 + "-fr-description", null, "TCP", "8080", targetProxy2);

                    String convergedHttpLoadBalancerSelfUrl = support.createConvergedHttpLoadBalancer(withExperimentalConvergedHttpLoadbalancerOptions);

                    tm.out("Subscribed", support.isSubscribed());
                } else {
                    tm.ok(tm.getProvider().getCloudName() + " does not support topologies");
                }
            }
        } else {
            tm.ok(tm.getProvider().getCloudName() + " does not support compute services");
        }
    }

    @Test
    public void createHttpLoadBalancerUsingExistingHealthCheck() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if (services != null) {
            if (services.hasConvergedHttpLoadBalancerSupport()) {
                ConvergedHttpLoadBalancerSupport support = services.getConvergedHttpLoadBalancerSupport();
                if (support != null) {
                    Map<String, String> pathMap = new HashMap<String, String>();
                    String instanceGroup1 = "https://www.googleapis.com/resourceviews/v1beta2/projects/qa-project-2/zones/europe-west1-b/resourceViews/instance-group-1";
                    String defaultBackend = "test-backend-4";
                    pathMap.put("/*", defaultBackend);
                    String targetProxy = "target-proxy-3";
                    ConvergedHttpLoadBalancer withExperimentalConvergedHttpLoadbalancerOptions = ConvergedHttpLoadBalancer
                            .getInstance("test-http-load-balancer-with-existing-health-check", "test-http-load-balancer-description", defaultBackend)
                            .withUrlSet("url-map-1", "url-map-description", "*", pathMap)
                            .withExistingHealthCheck("https://www.googleapis.com/compute/v1/projects/qa-project-2/global/httpHealthChecks/test-health-check")
                            .withBackendService(defaultBackend, defaultBackend + "-description", 80, "http", "HTTP", new String[] {"test-health-check"}, new String[] {instanceGroup1}, 30)
                            .withTargetHttpProxy(targetProxy, targetProxy + "-description")
                            .withForwardingRule(targetProxy + "-fr", targetProxy + "-fr-description", null, "TCP", "80", targetProxy);

                    String convergedHttpLoadBalancerSelfUrl = support.createConvergedHttpLoadBalancer(withExperimentalConvergedHttpLoadbalancerOptions);

                    tm.out("Subscribed", support.isSubscribed());
                } else {
                    tm.ok(tm.getProvider().getCloudName() + " does not support topologies");
                }
            }
        } else {
            tm.ok(tm.getProvider().getCloudName() + " does not support compute services");
        }
    }

    @Test
    public void createHttpLoadBalancerUsingExistingBackendService() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if (services != null) {
            if (services.hasConvergedHttpLoadBalancerSupport()) {
                ConvergedHttpLoadBalancerSupport support = services.getConvergedHttpLoadBalancerSupport();
                if (support != null) {
                    if (support.getCapabilities().supportsHealthChecks() &&
                        support.getCapabilities().supportsBackendServices() &&
                        support.getCapabilities().supportsUrlSets() &&
                        support.getCapabilities().supportsTargetHttpProxies() &&
                        support.getCapabilities().supportsForwardingRules() && 
                        support.getCapabilities().supportsUsingExistingBackendService()) {
                            Map<String, String> pathMap = new HashMap<String, String>();
                            String defaultBackend = "test-backend-1";
                            pathMap.put("/*", defaultBackend);
                            String targetProxy = "target-proxy-4";
                            ConvergedHttpLoadBalancer withExperimentalConvergedHttpLoadbalancerOptions = ConvergedHttpLoadBalancer
                                    .getInstance("test-http-load-balancer-with-existing-backend-service", "test-http-load-balancer-description", defaultBackend)
                                    .withUrlSet("url-map-1", "url-map-description", "*", pathMap)
                                    .withExistingBackendService("https://www.googleapis.com/compute/v1/projects/qa-project-2/global/backendServices/test-backend-1")
                                    .withTargetHttpProxy(targetProxy, targetProxy + "-description")
                                    .withForwardingRule(targetProxy + "-fr", targetProxy + "-fr-description", null, "TCP", "80", targetProxy);

                            String convergedHttpLoadBalancerSelfUrl = support.createConvergedHttpLoadBalancer(withExperimentalConvergedHttpLoadbalancerOptions);
                            // VERIFY IT WAS CREATED CORRECTLY

                            ConvergedHttpLoadBalancer createdHttpLoadBalancer = support.getConvergedHttpLoadBalancer("test-http-load-balancer-with-existing-backend-service");

                            verifyConvergedHttpLoadBalancerNameDescription(createdHttpLoadBalancer, "test-http-load-balancer-with-existing-backend-service", "test-http-load-balancer-description", convergedHttpLoadBalancerSelfUrl);
                            verifyConvergedHttpLoadBalancerUrlSetPresent(createdHttpLoadBalancer, "url-map-1", "url-map-description", "*", pathMap);  // why no description
                            verifyConvergedHttpLoadBalancerBackendServicePresent(createdHttpLoadBalancer, "test-backend-1", "", "https://www.googleapis.com/compute/v1/projects/qa-project-2/global/backendServices/test-backend-1");
                            verifyConvergedHttpLoadBalancerTargetHttpProxyPresent(createdHttpLoadBalancer, targetProxy, targetProxy + "-description");
                            verifyConvergedHttpLoadBalancerForwardingRulePresent(createdHttpLoadBalancer, targetProxy + "-fr", targetProxy + "-fr-description", null, "TCP", "80-80", targetProxy);

                            tm.ok("createHttpLoadBalancerUsingExistingBackendService");
                    } else {
                        tm.ok(tm.getProvider().getCloudName() + " does not support required HttpLoadBalancer methods");
                    }
                    tm.out("Subscribed", support.isSubscribed());
                } else {
                    fail(tm.getProvider().getCloudName() + " returned null for getConvergedHttpLoadBalancerSupport");
                }
            } else {
                tm.ok(tm.getProvider().getCloudName() + " does not have ConvergedHttpLoadBalancerSupport");
            }
        } else {
            tm.ok(tm.getProvider().getCloudName() + " does not support combined infrastructure");
        }
    }

}