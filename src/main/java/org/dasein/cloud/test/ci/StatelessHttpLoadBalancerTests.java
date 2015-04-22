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

import static org.junit.Assume.assumeTrue;

import java.util.HashMap;
import java.util.Map;

import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ci.CIServices;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancer;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancerFilterOptions;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancerSupport;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Tests support for Dasein Cloud topologies which represent complex, multi-resource templates that can be provisioned into
 * running resources.
 * <p>Created by George Reese: 5/31/13 10:57 AM</p>
 * @author George Reese
 * @version 2013.07 initial version
 * @since 2013.07
 */
public class StatelessHttpLoadBalancerTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessHttpLoadBalancerTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testTopologyId;

    public StatelessHttpLoadBalancerTests() { }

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

                    ConvergedHttpLoadBalancer result = support.getConvergedHttpLoadBalancer("roger-name");
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

                    support.removeConvergedHttpLoadBalancers("roger-name");
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
                    String defaultBackend = "roger-bes-name";
                    String backend2 = "roger-bes2-name";
                    String backend3 = "roger-bes3-name";
                    pathMap.put("/*", defaultBackend);
                    pathMap.put("/video, /video/*", backend2);
                    pathMap.put("/audio, /audio/*", backend3);
                    String healthCheck1 = "roger-hc-1";
                    String targetProxy1 = "bob";
                    String targetProxy2 = "fred";
                    ConvergedHttpLoadBalancer withExperimentalConvergedHttpLoadbalancerOptions = ConvergedHttpLoadBalancer
                            .getInstance("roger-name", "roger-description", defaultBackend)
                            .withHealthCheck(healthCheck1, healthCheck1 + "-description", null, 80, "/", 5, 5, 2, 2) //ONLY ONE ALLOWED
                            .withBackendService(defaultBackend, defaultBackend + "-description", 80, "http", "HTTP", new String[] {healthCheck1}, new String[] {instanceGroup1}, 30)
                            .withBackendService(backend2, backend2 + "-description", 80, "http", "HTTP", new String[] {healthCheck1}, new String[] {instanceGroup2}, 30)
                            .withBackendService(backend3, backend3 + "-description", 80, "http", "HTTP", new String[] {healthCheck1}, new String[] {instanceGroup1, instanceGroup2}, 30)
                            .withUrlSet("roger-url-map", "roger-url-map", "*", pathMap)
                            .withUrlSet("roger-url-map2", "roger-url-map2", "*.net", pathMap)
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
}
