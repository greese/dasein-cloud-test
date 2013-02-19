package org.dasein.cloud.test.identity;

import org.dasein.cloud.test.AbstractStatefulTestSuite;
import org.dasein.cloud.test.compute.StatefulVMTests;
import org.dasein.cloud.test.compute.StatelessVMTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/17/13 8:13 PM</p>
 *
 * @author George Reese
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ StatelessKeypairTests.class })
public class IdentityTestSuite extends AbstractStatefulTestSuite {
}
