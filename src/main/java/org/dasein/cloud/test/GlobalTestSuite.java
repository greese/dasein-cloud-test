package org.dasein.cloud.test;

import org.dasein.cloud.test.cloud.StatelessDCTests;
import org.dasein.cloud.test.compute.StatefulVMTests;
import org.dasein.cloud.test.compute.StatelessVMTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/17/13 7:38 PM</p>
 *
 * @author George Reese
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ StatelessDCTests.class, StatefulVMTests.class, StatelessVMTests.class })
public class GlobalTestSuite extends AbstractStatefulTestSuite {

}
