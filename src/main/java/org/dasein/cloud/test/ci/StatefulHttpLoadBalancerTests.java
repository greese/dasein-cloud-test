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

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.ci.CIProvisionOptions;
import org.dasein.cloud.ci.CIServices;
import org.dasein.cloud.ci.ConvergedInfrastructure;
import org.dasein.cloud.ci.ConvergedInfrastructureSupport;
import org.dasein.cloud.ci.TopologySupport;
import org.dasein.cloud.compute.ComputeServices;
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

    private String testTopologyId;

    public StatefulHttpLoadBalancerTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        //testTopologyId = tm.getTestTopologyId(DaseinTestManager.STATELESS, false);

        if (name.getMethodName().startsWith("listConvergedInfrastructures") ||
            name.getMethodName().startsWith("listVLANs") ||
            name.getMethodName().startsWith("listVirtualMachines") ||
            name.getMethodName().startsWith("listConvergedInfrastructureStatus") ||
            name.getMethodName().startsWith("deleteReplicapoolFromTopolology")) {
            //tm.getProvider().getComputeServices().getVirtualMachineSupport().getVirtualMachine()
            try {
                CIProvisionOptions options = CIProvisionOptions.getInstance(name.getMethodName().toLowerCase(), "test-description", "us-central1-f", 1, "instance-template-2");
                ConvergedInfrastructure ci = tm.getProvider().getCIServices().getConvergedInfrastructureSupport().provision(options);
                testTopologyId = ci.getName();
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
    }

    @After
    public void after() {
        tm.end();
        try {
            if (name.getMethodName().startsWith("listConvergedInfrastructures") ||
                name.getMethodName().startsWith("listVLANs") ||
                name.getMethodName().startsWith("listConvergedInfrastructureStatus") ||
                name.getMethodName().startsWith("listVirtualMachines")) {
                tm.getProvider().getCIServices().getConvergedInfrastructureSupport().terminate(name.getMethodName().toLowerCase(), "test over");
            }
            if (name.getMethodName().startsWith("createReplicapoolFromTopolology")) {
                tm.getProvider().getCIServices().getConvergedInfrastructureSupport().terminate(name.getMethodName().toLowerCase(), "test over");
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @Test
    public void createReplicapoolFromTopolology() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();
/*
        if( services != null ) {
            HttpLoadBalancerSupport support = services.getHttpLoadBalancerSupport();
            services.hasHttpLoadBalancerSuppot
            if ((null != topologySupport) && (null != replicapoolSupport)) {
                String description = "create-test";
                String zone = "us-central1-f";
                CIProvisionOptions options = CIProvisionOptions.getInstance(name.getMethodName().toLowerCase(), description , zone , 2, "instance-template-2" );  // is testTopologyId the url?
                ConvergedInfrastructure result = replicapoolSupport.provision(options);

            } else {
                tm.ok("No topology support in this cloud");
            }
        } else {
            tm.ok("No compute services in this cloud");
        }
        */
    }

    /*
     * delete a replicapool
     */
    @Test
    public void deleteReplicapoolFromTopolology() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if ( services != null) {
            TopologySupport support = services.getTopologySupport();

            if (support != null) {
                ConvergedInfrastructureSupport replicapoolSupport = services.getConvergedInfrastructureSupport();
                replicapoolSupport.terminate(testTopologyId, "die");

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

        if (services != null) {
            ConvergedInfrastructureSupport replicapoolSupport = services.getConvergedInfrastructureSupport();

            if( replicapoolSupport != null ) {
                int count = 0;
                Iterable<ConvergedInfrastructure> convergedInfrastructures = replicapoolSupport.listConvergedInfrastructures(null);
                for (ConvergedInfrastructure ci : convergedInfrastructures) {
                    count++;
                    // testTopologyId
                }
                assertTrue("listConvergedInfrastructures must return more than one result.", count > 0);
            } else {
                tm.ok("No replicapool support in this cloud");
            }
        } else {
            tm.ok("No Converged Infrastructure services in this cloud");
        }
    }

    @Test
    public void listVirtualMachines() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if (services != null) {
            ConvergedInfrastructureSupport replicapoolSupport = services.getConvergedInfrastructureSupport();

            if( replicapoolSupport != null ) {
                int count = 0;

                Iterable<String> virtualMachines = replicapoolSupport.listVirtualMachines(testTopologyId);
                for (String vm : virtualMachines) {
                    count++;
                }
                assertTrue("listVirtualMachines must return more than one result.", count > 0);
            } else {
                tm.ok("No replicapool support in this cloud");
            }
        } else {
            tm.ok("No Converged Infrastructure services in this cloud");
        }
    }

    @Test
    public void listVLANs() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if (services != null) {
            ConvergedInfrastructureSupport replicapoolSupport = services.getConvergedInfrastructureSupport();

            if( replicapoolSupport != null ) {
                int count = 0;

                Iterable<String> virtualMachines = replicapoolSupport.listVLANs(testTopologyId);
                for (String vlan : virtualMachines) {
                    count++;
                }
                assertTrue("listVLANs must return more than one result.", count > 0);
            } else {
                tm.ok("No replicapool support in this cloud");
            }
        } else {
            tm.ok("No Converged Infrastructure services in this cloud");
        }
    }

    @Test
    public void listConvergedInfrastructureStatus() throws CloudException, InternalException {
        CIServices services = tm.getProvider().getCIServices();

        if (services != null) {
            ConvergedInfrastructureSupport replicapoolSupport = services.getConvergedInfrastructureSupport();

            if( replicapoolSupport != null ) {
                int count = 0;

                Iterable<ResourceStatus> ciStatus = replicapoolSupport.listConvergedInfrastructureStatus();
                for (ResourceStatus status : ciStatus) {
                    count++;
                }
                assertTrue("listConvergedInfrastructureStatus must return more than one result.", count > 0);
            } else {
                tm.ok("No replicapool support in this cloud");
            }
        } else {
            tm.ok("No Converged Infrastructure services in this cloud");
        }
    }
}