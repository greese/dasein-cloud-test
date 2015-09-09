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

package org.dasein.cloud.test.platform;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.platform.*;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.*;
import org.junit.rules.TestName;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Verifies the stateful elements of cloud monitoring solutions like AWS CloudWatch.
 * <p>Created by Cameron Stokes: 2/19/13 2:38 PM</p>
 *
 * @author Cameron Stokes
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatefulMonitoringTests {

    static private final Logger logger = Logger.getLogger(StatefulMonitoringTests.class);

    private static final String DASEIN_PREFIX = "dasein-alarm-";

    private String provisionedAlarmName;

    static private DaseinTestManager tm;

    private String availableMetricName;
    private String availableMetricNamespace;

    @Rule
    public final TestName name = new TestName();

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulMonitoringTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    public StatefulMonitoringTests() {
    }

    @Before
    public void before() {
        String methodName = name.getMethodName();
        tm.begin(methodName);
        assumeTrue(!tm.isTestSkipped());
        setAvailableMetricProperties();
        if( "testListAlarms".equals(methodName) ) {
            addTestAlarm();
        }
        else if( "testListAlarmsWithFilter".equals(methodName) ) {
            addTestAlarm();
        }
        else if( "testRemoveAlarms".equals(methodName) ) {
            addTestAlarm();
        }
        else if( "testEnableAlarmActions".equals(methodName) ) {
            addTestAlarm();
        }
        else if( "testDisableAlarmActions".equals(methodName) ) {
            addTestAlarm();
        }
        else {
            logger.debug("No pre-test work for: " + methodName);
        }
    }

    @After
    public void after() {
        try {

            if( provisionedAlarmName != null ) {
                getSupport().removeAlarms(new String[]{ provisionedAlarmName });
            }
        }
        catch( Throwable ex ) {
            logger.warn(ex);
        }
        provisionedAlarmName = null;
        tm.end();
    }

    /**
     * Requires {@link #setAvailableMetricProperties()} to be run first.
     */
    private void addTestAlarm() {
        MonitoringSupport support = getSupport();
        if( support == null || provisionedAlarmName != null ) {
            return;
        }
        try {
            String alarmName = DASEIN_PREFIX + getRandomId();
            AlarmUpdateOptions options = AlarmUpdateOptions.getInstance(alarmName, availableMetricNamespace, availableMetricName, "SampleCount", "GreaterThanOrEqualToThreshold", 0.0, 60, 1);
            support.updateAlarm(options);
            provisionedAlarmName = alarmName;
        }
        catch( Throwable ex ) {
            logger.warn(ex);
        }
    }

    private void setAvailableMetricProperties() {
        MonitoringSupport support = getSupport();
        if( support == null || availableMetricName != null ) {
            return;
        }
        try {
            Iterable<Metric> metrics = support.listMetrics(MetricFilterOptions.getInstance());
            assertTrue("No metrics available to work with.", metrics.iterator().hasNext());

            Metric firstMetric = metrics.iterator().next();
            availableMetricName = firstMetric.getName();
            availableMetricNamespace = firstMetric.getNamespace();
        }
        catch( Throwable ex ) {
            logger.warn(ex);
        }
    }

    @Test
    public void testListMetrics() throws CloudException, InternalException {
        MonitoringSupport support = getSupport();
        if( support != null ) {
            Iterable<Metric> metrics = support.listMetrics(MetricFilterOptions.getInstance());
            assertNotNull(metrics);
            for( Metric metric : metrics ) {
                assertMetric(metric);
            }
        }
        else {
            tm.ok("No MonitoringSupport in this cloud");
        }
    }

    /**
     * Requires {@link #setAvailableMetricProperties()} to be run first.
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Test
    public void testListMetricsWithFilter() throws CloudException, InternalException {
        MonitoringSupport support = getSupport();
        if( support != null ) {
            Iterable<Metric> metrics = support.listMetrics(MetricFilterOptions.getInstance().withMetricNamespace(availableMetricNamespace).withMetricName(availableMetricName));
            assertNotNull(metrics);
            for( Metric metric : metrics ) {
                assertMetric(metric);
                if( availableMetricNamespace != null ) {
                    assertEquals(availableMetricNamespace, metric.getNamespace());
                }
                assertEquals(availableMetricName, metric.getName());
            }
        }
        else {
            tm.ok("No MonitoringSupport in this cloud");
        }
    }

    @Test
    public void testListMetricsWithBadFilter() throws CloudException, InternalException {
        MonitoringSupport support = getSupport();
        if( support != null ) {
            Iterable<Metric> metrics = support.listMetrics(MetricFilterOptions.getInstance().withMetricName("asdf"));
            assertNotNull(metrics);
            for( Metric metric : metrics ) {
                assertMetric(metric);
                assertEquals(availableMetricName, metric.getName());
            }
        }
        else {
            tm.ok("No MonitoringSupport in this cloud");
        }
    }

    /**
     * Requires {@link #addTestAlarm()} ()} to be run first.
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Test
    public void testListAlarms() throws CloudException, InternalException {
        MonitoringSupport support = getSupport();
        if( support != null ) {
            Iterable<Alarm> alarms = support.listAlarms(AlarmFilterOptions.getInstance());
            assertNotNull(alarms);
            for( Alarm alarm : alarms ) {
                assertAlarm(alarm);
            }
        }
        else {
            tm.ok("No MonitoringSupport in this cloud");
        }
    }

    /**
     * Requires {@link #setAvailableMetricProperties()} to be run first.
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Test
    public void testListAlarmsWithFilter() throws CloudException, InternalException {
        MonitoringSupport support = getSupport();
        if( support != null ) {
            Iterable<Alarm> alarms = support.listAlarms(AlarmFilterOptions.getInstance().withAlarmNames(new String[]{ provisionedAlarmName }));
            assertNotNull(alarms);
            for( Alarm alarm : alarms ) {
                assertAlarm(alarm);
            }
        }
        else {
            tm.ok("No MonitoringSupport in this cloud");
        }
    }

    /**
     * Requires {@link #setAvailableMetricProperties()} to be run first.
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Test
    public void testAddAlarm() throws CloudException, InternalException {
        MonitoringSupport support = getSupport();
        if( support == null ) {
            tm.ok("No MonitoringSupport in this cloud");
            return;
        }
        try {
            String alarmName = DASEIN_PREFIX + getRandomId();
            AlarmUpdateOptions options = AlarmUpdateOptions.getInstance(alarmName, availableMetricNamespace, availableMetricName, "SampleCount", "GreaterThanOrEqualToThreshold", 0.0, 60, 1);
            support.updateAlarm(options);
            provisionedAlarmName = alarmName;
        }
        catch( OperationNotSupportedException expected ) {
            tm.ok("OperationNotSupportedException thrown.");
        }
    }

    /**
     * Requires {@link #addTestAlarm()} to be run first.
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Test
    public void testRemoveAlarms() throws CloudException, InternalException {
        MonitoringSupport support = getSupport();
        if( support == null ) {
            tm.ok("No MonitoringSupport in this cloud");
            return;
        }
        try {
            support.removeAlarms(new String[]{ provisionedAlarmName });
        }
        catch( OperationNotSupportedException expected ) {
            tm.ok("OperationNotSupportedException thrown.");
        }
    }

    /**
     * Requires {@link #addTestAlarm()} to be run first.
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Test
    public void testEnableAlarmActions() throws CloudException, InternalException {
        MonitoringSupport support = getSupport();
        if( support == null ) {
            tm.ok("No MonitoringSupport in this cloud");
            return;
        }
        try {
            support.enableAlarmActions(new String[]{ provisionedAlarmName });
        }
        catch( OperationNotSupportedException expected ) {
            tm.ok("OperationNotSupportedException thrown.");
        }
    }

    /**
     * Requires {@link #addTestAlarm()} to be run first.
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Test
    public void testDisableAlarmActions() throws CloudException, InternalException {
        MonitoringSupport support = getSupport();
        if( support == null ) {
            tm.ok("No MonitoringSupport in this cloud");
            return;
        }
        try {
            support.disableAlarmActions(new String[]{ provisionedAlarmName });
        }
        catch( OperationNotSupportedException expected ) {
            tm.ok("OperationNotSupportedException thrown.");
        }
    }

    private long getRandomId() {
        return System.currentTimeMillis() % 10000;
    }

    private void assertMetric(Metric metric) {
        assertNotNull(metric);
        assertNotNull(metric.getName());

        if( metric.getMetadata() != null ) {
            for( Map.Entry<String, String> entry : metric.getMetadata().entrySet() ) {
                assertNotNull(entry.getKey());
            }
        }
    }

    private void assertAlarm(Alarm alarm) {
        assertNotNull(alarm);
        assertNotNull(alarm.getName());
        assertNotNull(alarm.getMetric());
        if( !alarm.isFunction() ) {
            assertNotNull(alarm.getStatistic());
            assertNotNull(alarm.getComparisonOperator());
            assertNotNull(alarm.getThreshold());
        }
        assertNotNull(alarm.getProviderAlarmId());
        if( alarm.getProviderOKActionIds() != null ) {
            for( String id : alarm.getProviderOKActionIds() ) {
                assertNotNull(id);
            }
        }
        if( alarm.getProviderAlarmActionIds() != null ) {
            for( String id : alarm.getProviderAlarmActionIds() ) {
                assertNotNull(id);
            }
        }
        if( alarm.getProviderInsufficientDataActionIds() != null ) {
            for( String id : alarm.getProviderInsufficientDataActionIds() ) {
                assertNotNull(id);
            }
        }
        if( alarm.getMetricMetadata() != null ) {
            for( Map.Entry<String, String> entry : alarm.getMetricMetadata().entrySet() ) {
                assertNotNull(entry.getKey());
            }
        }
    }

    private MonitoringSupport getSupport() {
        PlatformServices services = getServices();

        if( services != null ) {
            return services.getMonitoringSupport();
        }
        return null;
    }

    private PlatformServices getServices() {
        CloudProvider provider = tm.getProvider();
        return provider.getPlatformServices();
    }

}
