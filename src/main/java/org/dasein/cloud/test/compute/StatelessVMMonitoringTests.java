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

package org.dasein.cloud.test.compute;

import junit.framework.Assert;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmStatistics;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.CalendarWrapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Tests on {@link VirtualMachineSupport} that do not involve making any changes to the system.
 * <p>Created by George Reese: 2/17/13 3:22 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatelessVMMonitoringTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessVMMonitoringTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testVMId;

    public StatelessVMMonitoringTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testVMId = tm.getTestVMId(DaseinTestManager.STATELESS, null, false, null);
    }

    @After
    public void after() {
        testVMId = null;
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                tm.out("Basic Analytics", support.getCapabilities().isBasicAnalyticsSupported());
                tm.out("Extended Analytics", support.getCapabilities().isExtendedAnalyticsSupported());
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
    public void getConsoleOutput() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVMId != null ) {
                    String output = support.getConsoleOutput(testVMId);

                    tm.out("Console", output);
                    assertNotNull("Console output may be empty, but it cannot be null", output);
                }
                else if( support.isSubscribed() ) {
                    fail("No test virtual machine exists and thus no test could be run for "+name.getMethodName());
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
    public void getStatisticsForLastHour() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVMId != null ) {
                    VmStatistics stats = support.getVMStatistics(testVMId, System.currentTimeMillis() - CalendarWrapper.HOUR, System.currentTimeMillis());

                    tm.out("Statistics", stats);
                    assertNotNull("Statistics object may be empty, but it cannot be null", stats);
                    tm.out("Sample Count", stats.getSamples());
                    tm.out("Sample Start", new Date(stats.getStartTimestamp()));
                    tm.out("Sample End", new Date(stats.getEndTimestamp()));
                    tm.out("Min CPU", stats.getMinimumCpuUtilization());
                    tm.out("Max CPU", stats.getMaximumCpuUtilization());
                    tm.out("Avg CPU", stats.getAverageCpuUtilization());
                    tm.out("Min Disk Read Bytes", stats.getMinimumDiskReadBytes());
                    tm.out("Max Disk Read Bytes", stats.getMaximumDiskReadBytes());
                    tm.out("Avg Disk Read Bytes", stats.getAverageDiskReadBytes());
                    tm.out("Min Disk Read Ops", stats.getMinimumDiskReadOperations());
                    tm.out("Max Disk Read Ops", stats.getMaximumDiskReadOperations());
                    tm.out("Avg Disk Read Ops", stats.getAverageDiskReadOperations());
                    tm.out("Min Disk Write Bytes", stats.getMinimumDiskWriteBytes());
                    tm.out("Max Disk Write Bytes", stats.getMaximumDiskWriteBytes());
                    tm.out("Avg Disk Write Bytes", stats.getAverageDiskWriteBytes());
                    tm.out("Min Disk Write Ops", stats.getMinimumDiskWriteOperations());
                    tm.out("Max Disk Write Ops", stats.getMaximumDiskWriteOperations());
                    tm.out("Avg Disk Write Ops", stats.getAverageDiskWriteOperations());
                    tm.out("Min Network In", stats.getMinimumNetworkIn());
                    tm.out("Max Network In", stats.getMaximumNetworkIn());
                    tm.out("Avg Network In", stats.getAverageNetworkIn());
                    tm.out("Min Network Out", stats.getMinimumNetworkOut());
                    tm.out("Max Network Out", stats.getMaximumNetworkOut());
                    tm.out("Avg Network Out", stats.getAverageNetworkOut());
                    if( stats.getSamples() > 0 ) {
                        assertTrue("Sample start must be an hour ago", stats.getStartTimestamp() >= ( System.currentTimeMillis() - ( CalendarWrapper.MINUTE + CalendarWrapper.HOUR ) ));
                        assertTrue("Sample end must be greater than the sample start", stats.getEndTimestamp() >= stats.getStartTimestamp());
                    }
                }
                else if( support.isSubscribed() ) {
                    fail("No test virtual machine exists and thus no test could be run for "+name.getMethodName());
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
    public void getSamplesForLastHour() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVMId != null ) {
                    Iterable<VmStatistics> samples = support.getVMStatisticsForPeriod(testVMId, System.currentTimeMillis() - CalendarWrapper.HOUR, System.currentTimeMillis());

                    //noinspection ConstantConditions
                    tm.out("Has Samples", samples != null);
                    assertNotNull("Samples may be empty, but they may not be null", samples);

                    VmStatistics lastSample = null;

                    for( VmStatistics sample : samples ) {
                        tm.out(( new Date(sample.getStartTimestamp()) ).toString(), sample);
                        if( lastSample != null ) {
                            assertTrue("Samples must be ordered from oldest to newest", lastSample.getStartTimestamp() < sample.getStartTimestamp());
                        }
                        lastSample = sample;
                    }
                }
                else if( support.isSubscribed() ) {
                    fail("No test virtual machine exists and thus no test could be run for "+name.getMethodName());
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
