package org.dasein.cloud.test.platform;

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

import static junit.framework.Assert.*;
import static org.junit.Assert.fail;

/**
 * Verifies the stateless elements of cloud monitoring solutions like AWS CloudWatch.
 * <p>Created by Cameron Stokes: 2/19/13 2:38 PM</p>
 *
 * @author Cameron Stokes
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatelessMonitoringTests {
  // every test has a test manager class that stores the test context
  // and provides access to shared resources (minimizing the number of cloud resources that must be provisioned)
  static private DaseinTestManager tm;

  @BeforeClass
  static public void configure() {
    // I always call this method configure; doesn't matter what it is called as long as it has the BeforeClass
    // annotation. It's role is to initialize the test manager
    tm = new DaseinTestManager( StatelessMonitoringTests.class );
  }

  @AfterClass
  static public void cleanUp() {
    // Same as for configure, except this is an AfterClass for clean up. It should close the test manager
    if ( tm != null ) {
      tm.close();
    }
  }

  // Provides access to the currently executing test name
  @Rule
  public final TestName name = new TestName();


  public StatelessMonitoringTests() {
  }

  @Before
  public void before() {
    // I always call this before, but as per the Before annotation contract, whatever this is called, it will
    // be called before each test within this class
    // It should minimally tell the test manager that a test is beginning
    // Any further initialization should happen AFTER this tm.begin() call
    tm.begin( name.getMethodName() );

    // stateless tests by design should be doing basically nothing here because they should not care
    // about the state of the system
    // I might have a private String for a test VM I am looking for:
    // testVMId = tm.getTestVMId(true, null);
    // that method will provide me with a shared (true) VM
    // shared means it is an existing VM and I should not screw with it
    // the second argument should be null in stateless tests
    // it described the desired state for the VM (e.g. RUNNING)
    // if the VM is not RUNNING right now and the test is stateful, it will make it RUNNING
  }

  @After
  public void after() {
    try {
      // This is called AFTER each test is executed. Any further closing should happen BEFORE you call tm.end()
    }
    finally {
      tm.end();
    }
  }

  @Test
  public void checkMetaData() throws CloudException, InternalException {
    if ( !tm.isTestSkipped() ) {
      PlatformServices services = getServices();

      if ( services != null ) {
        //MonitoringSupport support = services.getMonitoringSupport(); or whatever you call it

        //if( support != null ) {
        //    tm.out("Subscribed", support.isSubscribed());
        //    tm.out("Term for Monitoring Product", support.getProviderTermForMonitoring(Locale.getDefault()));
        //    TODO: print all meta-data elements out, but assume the values may be null
        //    assertNotNull("Monitoring term may not be null", support.getProviderTermForMonitoring(Locale.getDefault()));
        //    TODO: after all values are printed out, do assertions to validate the values
        //else {
        //    tm.ok(tm.getProvider().getCloudName() + " does not support monitoring");
        //}
      }
      else {
        // No compute services, so pass the test without output that says all is OK
        tm.ok( tm.getProvider().getCloudName() + " does not support any compute services" );
      }
    }
    else {
      tm.skip();
    }
  }

  @Test
  public void listMetrics() throws CloudException, InternalException {
    assertTrue( !tm.isTestSkipped() );
    MonitoringSupport support = getSupport();
    if ( support != null ) {
      Collection<Metric> metrics = support.listMetrics( MetricFilterOptions.getInstance() );
      for ( Metric metric : metrics ) {
        assertMetric( metric );
      }
    }
    else {
      tm.ok( "No MonitoringSupport in this cloud" );
    }
  }

  @Test
  public void listMetricsWithFilter() throws CloudException, InternalException {
    assertTrue( !tm.isTestSkipped() );
    String metricNamespace = "AWS/EC2";
    String metricName = "CPUUtilization";

    MonitoringSupport support = getSupport();
    if ( support != null ) {
      Collection<Metric> metrics = support.listMetrics( MetricFilterOptions
          .getInstance()
          .withMetricNamespace( metricNamespace )
          .withMetricName( metricName )
      );
      for ( Metric metric : metrics ) {
        assertMetric( metric );
        assertEquals( metricName, metric.getName() );
        assertEquals( metricNamespace, metric.getMetadata().get( "Namespace" ) );
      }
    }
    else {
      tm.ok( "No MonitoringSupport in this cloud" );
    }
  }

  @Test
  public void listAlarms() throws CloudException, InternalException {
    assertTrue( !tm.isTestSkipped() );
    MonitoringSupport support = getSupport();
    if ( support != null ) {
      Collection<Alarm> alarms = support.listAlarms( AlarmFilterOptions.getInstance() );
      for ( Alarm alarm : alarms ) {
        assertAlarm( alarm );
      }
    }
    else {
      tm.ok( "No MonitoringSupport in this cloud" );
    }
  }

  @Test
  public void enableAlarmActions() throws CloudException, InternalException {
    assertTrue( !tm.isTestSkipped() );
    MonitoringSupport support = getSupport();
    if ( support != null ) {
      try {
        support.enableAlarmActions( new String[] {} );
        fail( "enableAlarmActions is not supported, but the request appears to have completed" );
      }
      catch ( OperationNotSupportedException expected ) {
        tm.ok( "Caught operation not supported exception as expected" );
      }
    }
    else {
      tm.ok( "No MonitoringSupport in this cloud" );
    }
  }

  @Test
  public void disableAlarmActions() throws CloudException, InternalException {
    assertTrue( !tm.isTestSkipped() );
    MonitoringSupport support = getSupport();
    if ( support != null ) {
      try {
        support.disableAlarmActions( new String[] {} );
        fail( "disableAlarmActions is not supported, but the request appears to have completed" );
      }
      catch ( OperationNotSupportedException expected ) {
        tm.ok( "Caught operation not supported exception as expected" );
      }
    }
    else {
      tm.ok( "No MonitoringSupport in this cloud" );
    }
  }

  private void assertMetric( Metric metric ) {
    assertNotNull( metric );
    assertNotNull( metric.getName() );

    if ( metric.getMetadata() != null ) {
      for ( Map.Entry<String, String> entry : metric.getMetadata().entrySet() ) {
        assertNotNull( entry.getKey() );
      }
    }
  }

  private void assertAlarm( Alarm alarm ) {
    assertNotNull( alarm );
    assertNotNull( alarm.getName() );
    assertNotNull( alarm.getMetric() );
    if ( !alarm.isFunction() ) {
      assertNotNull( alarm.getStatistic() );
      assertNotNull( alarm.getComparisonOperator() );
      assertNotNull( alarm.getThreshold() );
    }
    assertNotNull( alarm.getProviderAlarmId() );
    if ( alarm.getProviderOKActionIds() != null ) {
      for ( String id : alarm.getProviderOKActionIds() ) {
        assertNotNull( id );
      }
    }
    if ( alarm.getProviderAlarmActionIds() != null ) {
      for ( String id : alarm.getProviderAlarmActionIds() ) {
        assertNotNull( id );
      }
    }
    if ( alarm.getProviderInsufficentDataActionIds() != null ) {
      for ( String id : alarm.getProviderInsufficentDataActionIds() ) {
        assertNotNull( id );
      }
    }
    if ( alarm.getMetadata() != null ) {
      for ( Map.Entry<String, String> entry : alarm.getMetadata().entrySet() ) {
        assertNotNull( entry.getKey() );
      }
    }
  }

  private MonitoringSupport getSupport() {
    PlatformServices services = getServices();
    return services.getMonitoringSupport();
  }

  private PlatformServices getServices() {
    CloudProvider provider = tm.getProvider();
    return provider.getPlatformServices();
  }

  // tests I like to see in general in the stateless tests...
  // 1. getXXX() using invalid values
  // 2. getXXX() using valid values
  // 3. listXXX() making sure the result is non-null, even when the underlying services is not subscribed/supported
  // 4. searchXXX() if any, same as with list
  // 5. execute everything, even if the cloud does not support it; expect OperationNotSupportedException when not supported
}
