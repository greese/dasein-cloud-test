/**
 * Copyright (C) 2009-2015 Dell, Inc.
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
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.platform.Database;
import org.dasein.cloud.platform.DatabaseBackup;
import org.dasein.cloud.platform.DatabaseBackupState;
import org.dasein.cloud.platform.DatabaseEngine;
import org.dasein.cloud.platform.DatabaseProduct;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.platform.RelationalDatabaseCapabilities;
import org.dasein.cloud.platform.RelationalDatabaseSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests for validating stateless functionality in support of relational databases in Dasein Cloud.
 * <p>Created by George Reese: 2/27/13 7:15 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatelessRDBMSTests {
    static private DaseinTestManager tm;
    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessRDBMSTests.class);
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
    private String testDataCenterId;
    private final static String statelessTestDatabase = "stateless-test-database-4";

    public StatelessRDBMSTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        testDataCenterId = DaseinTestManager.getDefaultDataCenterId(true);

        testDatabaseId = tm.getTestRDBMSId(DaseinTestManager.STATELESS, false, null);
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
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

        RelationalDatabaseCapabilities capabilities = support.getCapabilities();
        assertNotNull("Relational database support for "  + tm.getProvider().getCloudName() + " does not implement getCapabilities() ", capabilities);

        tm.out("Subscribed", support.isSubscribed());
        tm.out("Term for Database", capabilities.getProviderTermForDatabase(Locale.getDefault()));
        tm.out("Term for Database Snapshot", capabilities.getProviderTermForSnapshot(Locale.getDefault()));
        tm.out("Supports Firewall Rules", capabilities.supportsFirewallRules());
        tm.out("High Availability Support", capabilities.supportsHighAvailability());
        tm.out("Low Availability Support", capabilities.supportsLowAvailability());
        tm.out("Maintenance Window Support", capabilities.supportsMaintenanceWindows());
        tm.out("Supports Snapshots", capabilities.supportsSnapshots());

        Iterable<DatabaseEngine> engines = support.getDatabaseEngines();

        if( engines != null ) {
            for( DatabaseEngine engine : engines ) {
                tm.out("Default Version [" + engine + "]", support.getDefaultVersion(engine));
            }
            for( DatabaseEngine engine : engines ) {
                tm.out("Supported Versions [" + engine + "]", support.getSupportedVersions(engine));
            }
        }
        assertNotNull("The provider term for a database may not be null", capabilities.getProviderTermForDatabase(Locale.getDefault()));
        assertNotNull("The provider term for a database snapshot may not be null", capabilities.getProviderTermForSnapshot(Locale.getDefault()));
        for( DatabaseEngine engine : support.getDatabaseEngines() ) {
            Iterable<DatabaseProduct> products = support.listDatabaseProducts(engine);
            Iterable<String> versions = support.getSupportedVersions(engine);

            assertNotNull("The list of database products for " + engine + " may not be null, even if not supported", products);
            assertNotNull("The list of supported database versions for " + engine + " may not be null, even if not supported", versions);
            if( support.isSubscribed() && engines != null ) {
                for( DatabaseEngine supported : engines ) {
                    if( supported.equals(engine) ) {
                        assertTrue("There must be at least one supported version for every supported database engine (" + engine +  " missing)", versions.iterator().hasNext());
                    }
                }
            }
        }
        if( engines != null ) {
            for( DatabaseEngine engine : engines ) {
                assertNotNull("The default version for a supported database engine (" + engine + ") cannot be null", support.getDefaultVersion(engine));
            }
        }
    }

    @Test
    public void listDatabaseEngines() throws CloudException, InternalException {
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
        int count = 0;

        assertNotNull("The list of database engines may not be null", engines);
        for( DatabaseEngine engine : engines ) {
            count++;
            tm.out("RDBMS Engine", engine);
        }
        tm.out("Total Database Engine Count", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
                tm.ok("This account is not subscribed to RDBMS support in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("There must be at least one supported database engine");
            }
        }
    }

    @Test
    public void listDatabaseProducts() throws CloudException, InternalException {
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

        for( DatabaseEngine engine : DatabaseEngine.values() ) {
            Iterable<DatabaseProduct> products = support.listDatabaseProducts(engine);
            int count = 0;

            assertNotNull("The list of database products may not be null, even if the engine is not supported", products);
            for( DatabaseProduct product : products ) {
                count++;
                tm.out("RDBMS Product [" + engine + "]", product.getName());
            }
            tm.out("Total " + engine + " Database Product Count " + count);

            boolean supported = false;
            for( DatabaseEngine dbe : engines ) {
                if( dbe.equals(engine) ) {
                    supported = true;
                    break;
                }
            }

            if (supported) {
                if( count < 1 ) {
                    if (support.isSubscribed()) {
                        fail("There must be at least one product for each supported database engine (missing one for " + engine + ")");
                    } else
                        tm.ok("This account is not subscribed to RDBMS support in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
                }
            } else
                tm.ok("RDBMS does not support " + engine + " as expected.");
        }
    }

    @Test
    public void getBogusDatabase() throws CloudException, InternalException {
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
        Database database = support.getDatabase(UUID.randomUUID().toString());

        tm.out("Bogus Database", database);
        assertNull("The random UUID resulted in a database being returned, should be null", database);
    }

    @Test
    public void getDatabase() throws CloudException, InternalException {
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
            Database database = support.getDatabase(testDatabaseId);

            tm.out("Database", database);
            assertNotNull("The test database returned null", database);
        }
        else if( !support.isSubscribed() ) {
            tm.ok("This account is not subscribed to relational database support in " + tm.getContext().getRegionId() + " in " + tm.getProvider().getCloudName());
        }
        else {
            fail("No test database has been identified for this test");
        }
    }

    private void assertDatabase(@Nonnull Database db) {
        assertNotNull("Database ID is null", db.getProviderDatabaseId());
        assertNotNull("Status is null", db.getCurrentState());
        assertNotNull("Name is null", db.getName());
        assertNotNull("Product is null", db.getProductSize());
        assertNotNull("Region is null", db.getProviderRegionId());
        assertNotNull("Engine is null", db.getEngine());
        assertTrue("Region must match the current region", tm.getContext().getRegionId().startsWith(db.getProviderRegionId()));
        assertNotNull("Engine version is null", db.getEngineVersion());
    }

    @Test
    public void databaseContent() throws CloudException, InternalException {
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
            Database db = support.getDatabase(testDatabaseId);

            assertNotNull("The test database returned null", db);

            tm.out("RDBMS ID", db.getProviderDatabaseId());
            tm.out("Current State", db.getCurrentState());
            tm.out("Name", db.getName());
            tm.out("Created", (new Date(db.getCreationTimestamp())));
            tm.out("Owner Account", db.getProviderOwnerId());
            tm.out("Region ID", db.getProviderRegionId());
            tm.out("Data Center ID", db.getProviderDataCenterId());
            tm.out("Product", db.getProductSize());
            tm.out("Engine", db.getEngine());
            tm.out("High Availability", db.isHighAvailability());
            tm.out("Location", db.getHostName() + ":" + db.getHostPort());
            tm.out("Storage", db.getAllocatedStorageInGb() + " GB");
            tm.out("Recovery Point", (new Date(db.getRecoveryPointTimestamp())));
            tm.out("Snapshot Window", db.getSnapshotWindow());
            tm.out("Snapshot Retention", new TimePeriod<Day>(db.getSnapshotRetentionInDays(), TimePeriod.DAY));
            tm.out("Maintenance Window", db.getMaintenanceWindow());
            tm.out("Admin User", db.getAdminUser());
            tm.out("Configuration", db.getConfiguration());
            tm.out("Engine Version", db.getEngineVersion());

            assertDatabase(db);
        }
        else if( !support.isSubscribed() ) {
            tm.ok("This account is not subscribed to relational database support in " + tm.getContext().getRegionId() + " in " + tm.getProvider().getCloudName());
        }
        else {
            fail("No test database has been identified for this test");
        }
    }

    @Test
    public void listDatabases() throws CloudException, InternalException {
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
        Iterable<Database> databases = support.listDatabases();
        int count = 0;

        assertNotNull("The list of databases may not be null, even if not subscribed", databases);
        for( Database db : databases ) {
            count++;
            tm.out("Database", db);
        }
        tm.out("Total Database Count", count);
        if( !support.isSubscribed() ) {
            assertEquals("The database count must be zero since the account is not subscribed", 0, count);
        }
        else if( count < 1 ) {
            tm.warn("This test is likely invalid as no databases were provided in the results for validation");
        }
        for( Database db : databases ) {
            assertDatabase(db);
        }
    }

    @Test
    public void listDatabaseStatus() throws CloudException, InternalException {
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
        Iterable<ResourceStatus> databases = support.listDatabaseStatus();
        int count = 0;

        assertNotNull("The list of databases may not be null, even if not subscribed", databases);
        for( ResourceStatus db : databases ) {
            count++;
            tm.out("Database Status", db);
        }
        tm.out("Total Database Status Count", count);
        if( !support.isSubscribed() ) {
            assertEquals("The database status count must be zero since the account is not subscribed", 0, count);
        }
        else if( count < 1 ) {
            tm.warn("This test is likely invalid as no database status items were provided in the results for validation");
        }
    }

    @Test
    public void compareDatabaseListAndStatus() throws CloudException, InternalException {
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
        HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
        Iterable<Database> databases = support.listDatabases();
        Iterable<ResourceStatus> status = support.listDatabaseStatus();

        assertNotNull("listDatabases() must return at least an empty collections and may not be null", databases);
        assertNotNull("listDatabaseStatus() must return at least an empty collection and may not be null", status);
        for( ResourceStatus s : status ) {
            Map<String,Boolean> current = map.get(s.getProviderResourceId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(s.getProviderResourceId(), current);
            }
            current.put("status", true);
        }
        for( Database db : databases ) {
            Map<String,Boolean> current = map.get(db.getProviderDatabaseId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(db.getProviderDatabaseId(), current);
            }
            current.put("database", true);
        }
        for( Map.Entry<String,Map<String,Boolean>> entry : map.entrySet() ) {
            Boolean s = entry.getValue().get("status");
            Boolean d = entry.getValue().get("database");

            assertTrue("Status and database lists do not match for " + entry.getKey(), s != null && d != null && s && d);
        }
        tm.out("Matches");
    }

    @Test 
    public void restoreBackup() throws CloudException, InternalException {
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

        if (support.getCapabilities().supportsDatabaseBackups()) {
            Iterable<DatabaseBackup> backupList = support.listBackups(statelessTestDatabase);
            for (DatabaseBackup backup : backupList) {
                if (support.getCapabilities().supportsDatabaseBackups()) {
                    if (DatabaseBackupState.AVAILABLE == backup.getCurrentState()) {
                        try {
                            support.restoreBackup(backup);
                        } catch (Exception e) {
                            fail(e.getMessage());
                        }
                        tm.ok("Database backup restored");
                        break;
                    }
                }
            }
        } else
            tm.ok("Database does not support backups.");
    }

    /**
     * TEST: createFromLatest
     * TODO: The tests below appear to have preconditions: it only ever supposed to work with Cloud SQL, and relies on a certain named db instance being present. This needs to be changed so it works with other drivers or that other drivers can bail out successfully.
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
//    @Test DISABLED
    public void createFromLatest() throws CloudException, InternalException {
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

        String copyName = null;
        try {
            Random random = new Random();
            copyName = "stateless-test-database-clone-";
            for (int x = 0; x< 7; x++)
                copyName = copyName + random.nextInt(9);
            support.createFromLatest(statelessTestDatabase, copyName, "D1", testDataCenterId, 999);
            Database db = support.getDatabase(copyName);
            assertTrue(db.getProviderDatabaseId().equals(copyName));
            assertTrue(db.getProductSize().equals("D1"));
            if (!tm.getProvider().getProviderName().equals("GCE"))
                assertTrue(testDataCenterId.startsWith(db.getProviderRegionId())); // testDataCenterId
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            support.removeDatabase(copyName);
        }
    }

    /** 
     * TEST: listBackups
     * @author Roger Unwin
     * 
     * NOTE: requires a database be present named statelessTestDatabase
     * 
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    // @Test DISABLED
    public void listBackups() throws CloudException, InternalException {
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

        if (support.getCapabilities().supportsDatabaseBackups()) {
            Iterable<DatabaseBackup> backupList = support.listBackups(statelessTestDatabase);
            for (DatabaseBackup backup : backupList) {
                assertTrue("DatabaseBackup returned did not match database id requested ", backup.getProviderDatabaseId().equals(statelessTestDatabase));
            }

            backupList = support.listBackups(null);
            for (DatabaseBackup backup : backupList) {

                assertTrue("DatabaseBackup returned did not match database id requested ", backup.getProviderDatabaseId().equals(statelessTestDatabase));

                if (support.getCapabilities().supportsDeleteBackup())
                    support.removeBackup(backup); // GCE does not support.
            }
        } else
            tm.ok("Database does not support backups.");

    }

    /** 
     * TEST: restartDatabase
     * @author Roger Unwin
     * 
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    // @Test DISABLED
    public void restartDatabase() throws CloudException, InternalException {
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
        try {
            support.restart(statelessTestDatabase, true);
            tm.ok("restart passed");
        } catch (Exception e) {
            fail("restartDatabase failed.");
        }
    }
}
