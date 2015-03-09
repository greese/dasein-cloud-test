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

package org.dasein.cloud.test.ci;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ci.CIProvisionOptions;
import org.dasein.cloud.ci.CIServices;
import org.dasein.cloud.ci.ConvergedInfrastructure;
import org.dasein.cloud.ci.ConvergedInfrastructureSupport;
import org.dasein.cloud.ci.Topology;
import org.dasein.cloud.ci.TopologyProvisionOptions;
import org.dasein.cloud.ci.TopologyProvisionOptions.AccessConfig;
import org.dasein.cloud.ci.TopologyProvisionOptions.DiskType;
import org.dasein.cloud.ci.TopologyProvisionOptions.MaintenenceOption;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Tests support for Dasein Cloud Replicapools which represent complex, multi-resource groups.
 */
public class StatefulReplicapoolTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulReplicapoolTests.class);
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

    public StatefulReplicapoolTests() { }

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

    /*
     * create new replicapool and verify it.
     */
    @Test
    public void createReplicapoolFromTopolology() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if( services != null ) {
            TopologySupport topologySupport = services.getTopologySupport();
            ConvergedInfrastructureSupport replicapoolSupport = services.getConvergedInfrastructureSupport();
            if ((null != topologySupport) && (null != replicapoolSupport)) {
                String id = "instance-template-2";
                String name = "instance-template-2";
                String description = "foo";
                String zone = "us-central1-f";
                String instanceTemplate = "https://www.googleapis.com/compute/v1/projects/qa-project-2/global/instanceTemplates/instance-template-2";
                CIProvisionOptions options = CIProvisionOptions.getInstance(id, name, description , zone , 2, instanceTemplate );  // is testTopologyId the url?
                ConvergedInfrastructure result = replicapoolSupport.provision(options);
                
                System.out.println("INSPECT");
            } else {
                tm.ok("No topology support in this cloud");
            }
        } else {
            tm.ok("No compute services in this cloud");
        }
    }

    /*
     * delete a replicapool
     */
    @Test
    public void deleteReplicapoolFromTopolology() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if( services != null ) {
            TopologySupport support = services.getTopologySupport();

            if( support != null ) {

                
                
                
                
            } else {
                tm.ok("No topology support in this cloud");
            }
        } else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void listConvergedInfrastructures() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if( services != null ) {
            TopologySupport topologySupport = services.getTopologySupport();
            ConvergedInfrastructureSupport replicapoolSupport = services.getConvergedInfrastructureSupport();

            if( replicapoolSupport != null ) {

                replicapoolSupport.listConvergedInfrastructures(null);


            } else {
                tm.ok("No topology support in this cloud");
            }
        } else {
            tm.ok("No compute services in this cloud");
        }
    }
}
