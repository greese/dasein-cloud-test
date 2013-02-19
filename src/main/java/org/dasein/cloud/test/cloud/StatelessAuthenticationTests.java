package org.dasein.cloud.test.cloud;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static junit.framework.Assert.*;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/18/13 6:35 PM</p>
 *
 * @author George Reese
 */
public class StatelessAuthenticationTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessAuthenticationTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    public StatelessAuthenticationTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void testContext() throws CloudException, InternalException {
        String id = tm.getProvider().testContext();

        tm.out("Account" + id);
        assertNotNull("Connection test failed", id);
    }

    @Test
    public void testContextReconnect() throws CloudException, InternalException {
        String id = tm.getProvider().testContext();

        assertEquals("New account number fails connection", id, tm.getContext().getAccountNumber());
    }

    @Test
    public void testBadSecret() throws CloudException, InternalException {
        assertNull("Connection succeeded with bad API secret", tm.getProvider().testContext());
    }

    @Test
    public void testFakeAccount() throws CloudException, InternalException {
        assertNull("Connection succeeded with fake account", tm.getProvider().testContext());
    }
}
