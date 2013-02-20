package org.dasein.cloud.test.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assume.assumeTrue;

/**
 * Verifies the stateful functionality of cloud monitoring solutions like AWS CloudWatch.
 * <p>Created by Cameron Stokes: 2/19/13 2:38 PM</p>
 * @author Cameron Stokes
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatefulMonitoringTests {
    // every test has a test manager class that stores the test context
    // and provides access to shared resources (minimizing the number of cloud resources that must be provisioned)
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        // I always call this method configure; doesn't matter what it is called as long as it has the BeforeClass
        // annotation. It's role is to initialize the test manager
        tm = new DaseinTestManager(StatelessImageTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        // Same as for configure, except this is an AfterClass for clean up. It should close the test manager
        // and de-provision any specially provisioned resources
        if( tm != null ) {
            tm.close();
        }
    }

    // Provides access to the currently executing test name
    @Rule
    public final TestName name = new TestName();

    // The ID for the VM to be used in a given test
    private String testVMId;

    private String testVmYouLikelyDontNeed;

    public StatefulMonitoringTests() { }

    @Before
    public void before() {
        // I always call this before, but as per the Before annotation contract, whatever this is called, it will
        // be called before each test within this class
        // It should minimally tell the test manager that a test is beginning
        // Any further initialization should happen AFTER this tm.begin() call
        tm.begin(name.getMethodName());

        // Here is where you do any provisioning specific to a test and setup test resource references
        // NOTE: You rarely need to provision your own resources except when you are doing something
        // destructive like a termination test
        // FOR EXAMPLE:
        testVMId = tm.getTestVMId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, null);

        // the above call fetched a pre-provisioned VM that I can feel comfortable doing horrible things to
        // the "false" value says that the reference is not stateless (if I had passed in true, I would
        // have potentially gotten a reference to a VM that pre-existed the test suite execution).
        // By specifying RUNNING as the second argument, I am telling the test manager to make sure
        // the VM is in a running state before the call returns
        // it will automatically figure out how to get the VM from it's current state to the desired state
        // if I pass null OR if the VM is stateless (arg 1 == true), then no change will be attempted

        ComputeResources compute = DaseinTestManager.getComputeResources();

        if( compute != null ) {
            ComputeServices services = tm.getProvider().getComputeServices();

            if( services != null ) {
                VirtualMachineSupport support = services.getVirtualMachineSupport();

                if( support != null ) {
                    try {
                        // Dasein Cloud Test Manager will automatically terminate this once all tests complete
                        // Always prefix resource names with dsn so they can be readily identified as things that
                        // can be killed should the clean up fail for whatever reason
                        testVmYouLikelyDontNeed = compute.provisionVM(support, "My Special Dasein Test", "dsnmytest", null);
                    }
                    catch( Throwable ignore ) {
                        // deal with the lack of VM later, because there may be good reasons for this to fail
                        // that don't involve test failure
                    }
                }
            }
        }
    }

    @After
    public void after() {
        // NOTE: the best way to provision resources is through the test manager state methods
        // those provisioning methods will clean up even if you fail to
        try {
            // This is called AFTER each test is executed. Any further closing should happen BEFORE you call tm.end()
            // the first line nulls out our test VM reference
            testVMId = null;
            // if you did any special provisioning (in particular, if you provisioned something without using the
            // test manager), you should clean it up here
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void whatever() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            //MonitoringSupport support = services.getMonitoringSupport(); or whatever you call it

            //if( support != null ) {
            // whatever
            //else {
            //    tm.ok(tm.getProvider().getCloudName() + " does not support monitoring");
            //}
        }
        else {
            // No compute services, so pass the test without output that says all is OK
            tm.ok(tm.getProvider().getCloudName() + " does not support any compute services");
        }
    }
}
