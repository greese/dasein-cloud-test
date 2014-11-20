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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.DayOfWeek;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.TimeWindow;
import org.dasein.cloud.platform.*;
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

import static junit.framework.Assert.assertEquals;
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
        if ( name.getMethodName().equals("listAccess")) {
            testDatabaseId = tm.getTestRDBMSId(DaseinTestManager.STATEFUL, true, null);
        }
    }

    @After
    public void after() {
        tm.end();
    }

    @Test 
    public void getDefaultVersions() throws CloudException, InternalException {
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

        Iterable<DatabaseEngine> engines = support.getDatabaseEngines();
        Set<DatabaseEngine> engineSet = new HashSet<DatabaseEngine>(Arrays.asList(DatabaseEngine.values()));
        for (DatabaseEngine engine : engines) {
            assertTrue("Database engine " + engine + " is not known.", engineSet.contains(engine));

            Iterable<String> versions = support.getSupportedVersions(engine);
            int versionCount = 0;
            for (String version : versions) 
                versionCount++;

            assertTrue("at least one version of a database engine is required, yet none were found." , (versionCount > 0));
        }

    }

    /** 
     * TEST: deleteBackup
     * Created by Roger Unwin: Wed Oct  1 17:08
     * @author Roger Unwin
     * 
     * Notes: GCE does not support deleting backups. 
     * Left that for the first person to run this whose database supports backups...
     * 
     * @throws CloudException
     * @throws InternalException
     */
    @Test 
    public void deleteBackup() throws CloudException, InternalException {
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

        if (true == support.getCapabilities().supportsDeleteBackup()) {
            fail("Please implement deleteBackup test.");
        } else
            tm.ok("Platform does not support deleting of individual database backups.");
    }

    /** 
     * TEST: createBackup
     * Created by Roger Unwin: Wed Oct  1 17:08
     * @author Roger Unwin
     * 
     * Notes: GCE does not support manually creating backups. 
     * Left that for the first person to run this whose database supports backups...
     * 
     * @throws CloudException
     * @throws InternalException
     */
    @Test 
    public void createBackup() throws CloudException, InternalException {
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

        if (true == support.getCapabilities().supportsDemandBackups()) {
            fail("Please implement createBackup test.");
        } else
            tm.ok("Platform does not support manually creating of individual database backups.");
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
            Assert.assertNotNull("database has not been created", support.getDatabase(id));
        }
        else {
            fail("No platform resources were initialized for the test run");
        }
    }

    /*
     * Test will fail if run with other tests due to removeDatabase nuking the test db before it gets to this test. 
     */
    @Test
    public void listAccess() throws CloudException, InternalException {
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

        Iterable<String> access = support.listAccess(testDatabaseId);
        int count = 0;
        for (String element: access)
            count++;
        assertTrue("Count was not zero", (count == 0));

        support.addAccess(testDatabaseId, "qa-project-2");
        support.addAccess(testDatabaseId, "72.197.190.94");
        support.addAccess(testDatabaseId, "72.197.190.0/24");

        count = 0;
        access = support.listAccess(testDatabaseId);
        for (String element: access)
            count++;
        assertTrue("Count was not three", (count == 3));

        support.revokeAccess(testDatabaseId, "qa-project-2");
        support.revokeAccess(testDatabaseId, "72.197.190.94");
        support.revokeAccess(testDatabaseId, "72.197.190.0/24");

        count = 0;
        access = support.listAccess(testDatabaseId);
        for (String element: access)
            count++;
        assertTrue("Count was not zero", (count == 0));
    }

    @Test
    public void alterDatabase() throws CloudException, InternalException {
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
            Iterable<DatabaseEngine> engines = support.getDatabaseEngines();
            for (DatabaseEngine dbEngine : engines) {
                tm.out("testing " + dbEngine.name());
                String providerDatabaseId = p.provisionRDBMS(support, "provisionKeypair", "dsnrdbms", dbEngine);

                    // this should be updated to exercise all available versions of all available databases.  perhaps even for all available products...


                tm.out("New Database", providerDatabaseId);
                assertNotNull("No database was created by this test", providerDatabaseId);
                String productSize = "d1";
                int storageInGigabytes = 250;
                boolean applyImmediately = true;
                String configurationId = "new-configuration-id";
                String newAdminUser = "dcm";
                String newAdminPassword = "notasecret";
                int newPort = 9876;
                int snapshotRetentionInDays = 5;
                TimeWindow preferredMaintenanceWindow = new TimeWindow();
                preferredMaintenanceWindow.setEndDayOfWeek(DayOfWeek.MONDAY);
                preferredMaintenanceWindow.setEndHour(5);
                preferredMaintenanceWindow.setEndMinute(0);
                preferredMaintenanceWindow.setStartDayOfWeek(DayOfWeek.MONDAY);
                preferredMaintenanceWindow.setStartHour(3);
                preferredMaintenanceWindow.setStartMinute(0);
                TimeWindow preferredBackupWindow = preferredMaintenanceWindow;
                support.alterDatabase(providerDatabaseId, applyImmediately, productSize, storageInGigabytes, configurationId, newAdminUser, newAdminPassword, newPort, snapshotRetentionInDays, preferredMaintenanceWindow, preferredBackupWindow);

                Database database = support.getDatabase(providerDatabaseId);
                Assert.assertEquals(productSize.toLowerCase(), database.getProductSize().toLowerCase());
                Assert.assertEquals(storageInGigabytes, database.getAllocatedStorageInGb());
                Assert.assertEquals(configurationId, database.getConfiguration());
                if (support.getCapabilities().supportsMaintenanceWindows()) {
                    Assert.assertEquals(preferredMaintenanceWindow, database.getMaintenanceWindow());
                }

                //Assert.assertEquals(newAdminUser, database.getAdminUser());
                //Assert.assertEquals(newPort, database.getHostPort());

                tm.ok("alterDatabase appears fine");
            }
        }
        else {
            fail("No platform resources were initialized for the test run");
        }
    }

    @Test
    public void createDatabaseMultiple() throws CloudException, InternalException {
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
            Iterable<DatabaseEngine> engines = support.getDatabaseEngines();
            for (DatabaseEngine dbEngine : engines) {
                tm.out("testing " + dbEngine.name());
                String id = p.provisionRDBMS(support, "provisionKeypair", "dsnrdbms", dbEngine);

                    // this should be updated to exercise all available versions of all available databases.  perhaps even for all available products...

                tm.out("New Database", id);
                assertNotNull("No database was created by this test", id);
                Database database = support.getDatabase(id);
                assertNotNull("database has not been created", database);
            }
        }
        else {
            fail("No platform resources were initialized for the test run");
        }
    }


    @Test
    public void createOracleDatabase() throws CloudException, InternalException {
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
        Iterable<DatabaseEngine> engines = support.getDatabaseEngines();
        DatabaseEngine oracleEngine = null;
        for( DatabaseEngine engine : engines ) {
            // can only use ORACLE_SE1 as it's the only one with the license included
            if( engine == DatabaseEngine.ORACLE_SE1 ) {
                oracleEngine = engine;
                break;
            }
        }
        if( oracleEngine == null ) {
            tm.ok("Oracle doesn't seem to be supported by " + tm.getContext().getRegionId() + " in " + tm.getProvider().getCloudName());
            return;
        }
        String dbName = "dsnora" + (System.currentTimeMillis()%10000);
        String expectedDbName = dbName.toUpperCase().substring(0, 8);
        String id = support.createFromScratch(dbName, PlatformResources.getCheapestProduct(support, oracleEngine), null, "dasein", PlatformResources.randomPassword(), 3000);
        Database database = support.getDatabase(id);
        removeDatabase(id);
        Assert.assertNotNull("Oracle database has not been created", database);
        assertEquals("Oracle instance name is not set/returned correctly", expectedDbName, database.getName());
    }

    @Test
    public void removeDatabase() throws CloudException, InternalException {
        String id = null;
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
            id = p.provisionRDBMS(support, "provisionKeypair", "dsnrdbms-remove-test", null);

            tm.out("New Database", id);
            assertNotNull("No database was created by this test", id);
            Assert.assertNotNull("database has not been created", support.getDatabase(id));
        }
        else {
            fail("No platform resources were initialized for the test run");
        }
        removeDatabase(id);
    }

    private void removeDatabase(String id) throws CloudException, InternalException {
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
        if( id != null ) {
            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*20L);
            Database db = support.getDatabase(id);

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

            support.removeDatabase(id);
            db = support.getDatabase(id);
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
