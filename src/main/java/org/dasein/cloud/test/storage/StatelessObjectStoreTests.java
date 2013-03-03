package org.dasein.cloud.test.storage;

import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import static org.junit.Assume.assumeTrue;

/**
 * Tests that validate the stateless functionality of a Dasein Cloud implementation against an object storage system.
 * <p>Created by George Reese: 3/3/13 4:57 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatelessObjectStoreTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessObjectStoreTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    public StatelessObjectStoreTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
    }

    @After
    public void after() {
        tm.end();
    }
}
