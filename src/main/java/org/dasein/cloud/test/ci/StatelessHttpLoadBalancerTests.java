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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ci.CIServices;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancer;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancerFilterOptions;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancerSupport;
import org.dasein.cloud.ci.ConvergedHttpLoadbalancerOptions;
import org.dasein.cloud.ci.ConvergedInfrastructure;
import org.dasein.cloud.ci.ConvergedInfrastructureSupport;
import org.dasein.cloud.ci.Topology;
import org.dasein.cloud.ci.TopologySupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

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

                    Iterable<ConvergedHttpLoadBalancer> result = support.listConvergedHttpLoadBalancers(options);

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
    public void createHttpLoadBalancer() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if (services != null) {
            if (services.hasConvergedHttpLoadBalancerSupport()) {
                ConvergedHttpLoadBalancerSupport support = services.getConvergedHttpLoadBalancerSupport();
                if (support != null) {


                    ConvergedHttpLoadbalancerOptions withConvergedHttpLoadbalancerOptions = ConvergedHttpLoadbalancerOptions.getInstance("roger", "roger", "https://www.googleapis.com/compute/beta/projects/qa-project-2/global/urlMaps/roger-wizard-http-lb");
                    String result = support.createConvergedHttpLoadBalancer(withConvergedHttpLoadbalancerOptions );

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
}
