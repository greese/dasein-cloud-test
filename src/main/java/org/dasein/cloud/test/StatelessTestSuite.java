package org.dasein.cloud.test;

import org.dasein.cloud.test.compute.StatelessVMTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/17/13 8:12 PM</p>
 *
 * @author George Reese
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ StatelessVMTests.class })
public class StatelessTestSuite extends AbstractStatelessTestSuite {
}
