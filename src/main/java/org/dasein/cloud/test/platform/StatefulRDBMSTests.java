/**
 * Copyright (C) 2009-2014 Dell, Inc.
 * See annotations for authorship information
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.test.platform;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.platform.Database;
import org.dasein.cloud.platform.DatabaseState;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.platform.RelationalDatabaseSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.CalendarWrapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nullable;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Tests for validating the stateful features of the RDBMS support in Dasein Cloud.
 * <p>Created by George Reese: 2/27/13 9:29 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
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
        if( name.getMethodName().equals("removeDatabase") ) {
            testDatabaseId = tm.getTestRDBMSId(DaseinTestManager.REMOVED, true, null);
        }
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
            String id = p.provisionRDBMS(support, "provisionKeypair", "dsnrdbms", null);

            tm.out("New Database", id);
            assertNotNull("No database was created by this test", id);
        }
        else {
            fail("No platform resources were initialized for the test run");
        }
    }

    @Test
    public void removeDatabase() throws CloudException, InternalException {
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
        if( testDatabaseId != null ) {
            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*20L);
            Database db = support.getDatabase(testDatabaseId);

            while( timeout > System.currentTimeMillis() ) {
                if( canRemove(db) ) {
                    break;
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
                try { db = support.getDatabase(db.getProviderDatabaseId()); }
                catch( Throwable ignore ) { }
            }
            assertNotNull("The test database is not found", db);
            tm.out("Before", db.getCurrentState());

            support.removeDatabase(testDatabaseId);
            db = support.getDatabase(testDatabaseId);
            DatabaseState s = (db == null ? DatabaseState.DELETED : db.getCurrentState());
            tm.out("After", s);
            assertTrue("Database state must be one of DELETING or DELETED (or no database found)", s.equals(DatabaseState.DELETED) || s.equals(DatabaseState.DELETING));
        }
        else {
            if( support.isSubscribed() ) {
                fail("No test database for " + name.getMethodName());
            }
            else {
                tm.ok("RDBMS support is not subscribed so this test is not entirely valid");
            }
        }
    }

    private boolean canRemove(@Nullable Database db) {
        if( db == null ) {
            return true;
        }
        switch( db.getCurrentState() ) {
            case DELETING: case DELETED: case AVAILABLE: case STORAGE_FULL: case FAILED: return true;
            default: return false;
        }
    }
}
