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

                    Map<String, String> pathMap = new HashMap<String, String>();
                    pathMap.put("/*", "roger-bes-name");
                    pathMap.put("/video, /video/*", "roger-bes2-name");
                    pathMap.put("/audio, /audio/*", "roger-bes3-name");
                    ConvergedHttpLoadBalancer withExperimentalConvergedHttpLoadbalancerOptions = ConvergedHttpLoadBalancer
                            .getInstance("roger-name", "roger-description", "roger-bes-name")
                            .withHealthCheck("roger-hc-1", "roger-hc-1", null, 80, "/", 5, 5, 2, 2) //ONLY ONE ALLOWED
                            .withBackendService("roger-bes-name", "roger-bes-description", 80, "http", "HTTP", new String[] {"roger-hc-1"}, new String[] {"https://www.googleapis.com/resourceviews/v1beta2/projects/qa-project-2/zones/europe-west1-b/resourceViews/instance-group-1"}, 30)
                            .withBackendService("roger-bes2-name", "roger-bes2-description", 80, "http", "HTTP", new String[] {"roger-hc-1"}, new String[] {"https://www.googleapis.com/resourceviews/v1beta2/projects/qa-project-2/zones/us-central1-f/resourceViews/instance-group-2"}, 30)
                            .withBackendService("roger-bes3-name", "roger-bes3-description", 80, "http", "HTTP", new String[] {"roger-hc-1"}, new String[] {"https://www.googleapis.com/resourceviews/v1beta2/projects/qa-project-2/zones/europe-west1-b/resourceViews/instance-group-1", "https://www.googleapis.com/resourceviews/v1beta2/projects/qa-project-2/zones/us-central1-f/resourceViews/instance-group-2"}, 30)
                            .withUrlSet("roger-url-map", "roger-url-map", "*", pathMap)
                            .withTargetHttpProxy("bob", "bob")
                            .withTargetHttpProxy("fred", "fred")
                            .withForwardingRule("bobfr", "bobfr", null, "TCP", "80", "bob")
                            .withForwardingRule("fredfr", "fredfr", null, "TCP", "8080", "fred");

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
