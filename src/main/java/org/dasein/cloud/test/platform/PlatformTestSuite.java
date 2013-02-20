package org.dasein.cloud.test.platform;

import org.dasein.cloud.test.AbstractStatefulTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Cameron Stokes (http://github.com/clstokes)
 * @since 2013-02-19
 */
@RunWith( Suite.class )
@Suite.SuiteClasses( {StatelessMonitoringTests.class, StatefulMonitoringTests.class} )
public class PlatformTestSuite extends AbstractStatefulTestSuite {
}
