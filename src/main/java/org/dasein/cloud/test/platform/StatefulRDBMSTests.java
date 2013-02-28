package org.dasein.cloud.test.platform;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.platform.RelationalDatabaseSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/27/13 9:29 PM</p>
 *
 * @author George Reese
 */
public class StatefulRDBMSTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulRDBMSTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testDatabaseId;

    public StatefulRDBMSTests() { }

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
    public void createDatabase() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        RelationalDatabaseSupport support = services.getRelationalDatabaseSupport();

        if( support == null ) {
            tm.ok("Relational database support is not implemented for " + tm.getContext().getRegionId() + " in " + tm.getProvider().getCloudName());
            return;
        }
        PlatformResources p = DaseinTestManager.getPlatformResources();

        if( p != null ) {
            String id = p.provisionRDBMS(support, "provision", "dsnrdbms", null);

            tm.out("New Database", id);
            assertNotNull("No database was created by this test", id);
        }
        else {
            fail("No platform resources were initialized for the test run");
        }
    }
}
