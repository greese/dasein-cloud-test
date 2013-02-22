package org.dasein.cloud.test.network;

import org.dasein.cloud.CloudException;
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
 * [Class Documentation]
 * <p>Created by George Reese: 2/22/13 10:43 AM</p>
 *
 * @author George Reese
 */
public class StatelessFirewallTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessFirewallTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    public StatelessFirewallTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalError {

    }
}
