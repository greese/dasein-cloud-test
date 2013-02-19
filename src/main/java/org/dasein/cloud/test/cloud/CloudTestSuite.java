package org.dasein.cloud.test.cloud;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/18/13 6:40 PM</p>
 *
 * @author George Reese
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ StatelessAuthenticationTests.class, StatelessDCTests.class })
public class CloudTestSuite {
}
