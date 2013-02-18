package org.dasein.cloud.test.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VMFilterOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/17/13 7:46 PM</p>
 *
 * @author George Reese
 */
public class StatefulVMTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        System.out.println("stateful: configure");
        tm = new DaseinTestManager(StatelessVMTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        System.out.println("stateful: cleanUp");
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String filterVmId = null;

    public StatefulVMTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        if( name.getMethodName().equals("filterVMs") ) {
            ComputeServices services = tm.getProvider().getComputeServices();

            if( services != null ) {
                VirtualMachineSupport support = services.getVirtualMachineSupport();

                if( support != null ) {
                    try {
                        filterVmId = DaseinTestManager.getComputeResources().provision(support, "Dasein Filter Test", "dsnfilter");
                    }
                    catch( Throwable t ) {
                        tm.warn("Failed to provision VM for filter test: " + t.getMessage());
                    }
                }
            }
        }
    }

    @After
    public void after() {
        tm.end();
    }


    @Test
    public void filterVMs() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                Iterable<VirtualMachine> vms = support.listVirtualMachines(VMFilterOptions.getInstance(".*[Ff][Ii][Ll][Tt][Ee][Rr].*"));
                boolean found = false;
                int count = 0;

                assertNotNull("Filtering must return at least an empty collections and may not be null", vms);
                for( VirtualMachine vm : vms ) {
                    count++;
                    if( vm.getProviderVirtualMachineId().equals(filterVmId) ) {
                        found = true;
                    }
                    tm.out("VM", vm);
                }
                tm.out("Total VM Count", count);
                if( count < 1 && support.isSubscribed() ) {
                    if( filterVmId == null ) {
                        tm.warn("No virtual machines were listed and thus the test may be in error");
                    }
                    else {
                        fail("Should have found test virtual machine " + filterVmId + ", but none were found");
                    }
                }
                if( filterVmId != null ) {
                    assertTrue("Did not find the test filter VM " + filterVmId + " among the filtered VMs", found);
                }
                else {
                    tm.warn("No test VM existed for filter test, so results may not be valid");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }
}
