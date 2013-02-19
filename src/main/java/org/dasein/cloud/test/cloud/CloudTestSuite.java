package org.dasein.cloud.test.cloud;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Basic test suite for general cloud operations.
 * <p>Created by George Reese: 2/18/13 6:40 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ StatelessAuthenticationTests.class, StatelessDCTests.class })
public class CloudTestSuite {
}
