package org.dasein.cloud.test.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VMFilterOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.CalendarWrapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

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
        tm = new DaseinTestManager(StatelessVMTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testVmId = null;

    public StatefulVMTests() { }

    private void assertVMState(@Nonnull VirtualMachine vm, @Nonnull VmState targetState, @Nonnegative long timeout) {
        VmState currentState = vm.getCurrentState();

        while( System.currentTimeMillis() < timeout ) {
            if( targetState.equals(vm.getCurrentState()) ) {
                return;
            }
            try { Thread.sleep(15000L); }
            catch( InterruptedException ignore ) { }
            try {
                @SuppressWarnings("ConstantConditions") VirtualMachine v = tm.getProvider().getComputeServices().getVirtualMachineSupport().getVirtualMachine(vm.getProviderVirtualMachineId());

                if( v == null && !targetState.equals(VmState.TERMINATED) ) {
                    fail("Virtual machine went away before entering target state " + targetState.name());
                }
                else if( v == null ) {
                    return;
                }
                currentState = v.getCurrentState();
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        fail("VM " + vm.getProviderVirtualMachineId() + " failed to reach " + targetState + " prior to timeout (last known state: " + currentState + ")");
    }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        if( name.getMethodName().equals("filterVMs") ) {
            ComputeServices services = tm.getProvider().getComputeServices();

            if( services != null ) {
                VirtualMachineSupport support = services.getVirtualMachineSupport();

                if( support != null ) {
                    try {
                        //noinspection ConstantConditions
                        testVmId = DaseinTestManager.getComputeResources().provisionVM(support, "Dasein Filter Test", "dsnfilter", null);
                    }
                    catch( Throwable t ) {
                        tm.warn("Failed to provisionVM VM for filter test: " + t.getMessage());
                    }
                }
            }
        }
        else if( name.getMethodName().equals("start") ) {
            testVmId = tm.getTestVMId(false, VmState.STOPPED);
        }
        else if( name.getMethodName().equals("stop") ) {
            testVmId = tm.getTestVMId(false, VmState.RUNNING);
        }
        else {
            testVmId = tm.getTestVMId(false, null);
        }
    }

    @After
    public void after() {
        testVmId = null;
        tm.end();
    }

    @Test
    public void disableAnalytics() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    support.disableAnalytics(testVmId);
                }
                else {
                    tm.warn("No test virtual machine was found for testing enabling analytics");
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

    @Test
    public void enableAnalytics() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    support.enableAnalytics(testVmId);
                }
                else {
                    tm.warn("No test virtual machine was found for testing enabling analytics");
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

    @Test
    public void launch() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( support.isSubscribed() ) {
                    @SuppressWarnings("ConstantConditions") String id = DaseinTestManager.getComputeResources().provisionVM(support, "Dasein Test Launch", "dsnlaunch", null);

                    tm.out("Launched", id);
                    assertNotNull("Attempts to provisionVM a virtual machine MUST return a valid ID", id);
                    assertNotNull("Could not find the newly created virtual machine", support.getVirtualMachine(id));
                }
                else {
                    try {
                        //noinspection ConstantConditions
                        DaseinTestManager.getComputeResources().provisionVM(support, "Should Fail", "failure", null);
                        fail("Attempt to launch VM should not succeed when the account is not subscribed to virtual machine services");
                    }
                    catch( CloudException ok ) {
                        tm.ok("Got exception when not subscribed: " + ok.getMessage());
                    }
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
                    if( vm.getProviderVirtualMachineId().equals(testVmId) ) {
                        found = true;
                    }
                    tm.out("VM", vm);
                }
                tm.out("Total VM Count", count);
                if( count < 1 && support.isSubscribed() ) {
                    if( testVmId == null ) {
                        tm.warn("No virtual machines were listed and thus the test may be in error");
                    }
                    else {
                        fail("Should have found test virtual machine " + testVmId + ", but none were found");
                    }
                }
                if( testVmId != null ) {
                    assertTrue("Did not find the test filter VM " + testVmId + " among the filtered VMs", found);
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

    @Test
    public void stop() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    VirtualMachine vm = support.getVirtualMachine(testVmId);

                    if( vm != null ) {
                        if( support.supportsStartStop(vm) ) {
                            support.stop(testVmId);
                            assertVMState(vm, VmState.STOPPED, CalendarWrapper.MINUTE * 20L);
                        }
                        else {
                            try {
                                support.stop(testVmId);
                                fail("Start/stop is unsupported, yet the method completed without an error");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("STOP -> Operation not supported exception");
                            }
                        }
                    }
                    else {
                        tm.warn("Test virtual machine " + testVmId + " no longer exists");
                    }
                }
                else {
                    tm.warn("No test virtual machine was found for testing enabling analytics");
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

    @Test
    public void start() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    VirtualMachine vm = support.getVirtualMachine(testVmId);

                    if( vm != null ) {
                        if( support.supportsStartStop(vm) ) {
                            support.start(testVmId);
                            assertVMState(vm, VmState.RUNNING, CalendarWrapper.MINUTE * 20L);
                        }
                        else {
                            try {
                                support.start(testVmId);
                                fail("Start/stop is unsupported, yet the method completed without an error");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("START -> Operation not supported exception");
                            }
                        }
                    }
                    else {
                        tm.warn("Test virtual machine " + testVmId + " no longer exists");
                    }
                }
                else {
                    tm.warn("No test virtual machine was found for testing enabling analytics");
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
