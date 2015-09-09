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

import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.DayOfWeek;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.TimeWindow;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.platform.Database;
import org.dasein.cloud.platform.DatabaseEngine;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.*;
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
        if ( name.getMethodName().equals("listAccess") ||
                name.getMethodName().equals("alterDatabase") ||
                name.getMethodName().equals("checkAccess")) {
            testDatabaseId = tm.getTestRDBMSId(DaseinTestManager.STATEFUL, true, DatabaseEngine.MYSQL);
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

            assertTrue("At least one version of a database engine is required, yet none were found." , versions.iterator().hasNext());
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
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
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

        if( support.getCapabilities().supportsDeleteBackup() ) {
            fail("Please implement deleteBackup test.");
        }
        else {
            tm.ok("Platform does not support deleting of individual database backups.");
        }
    }

    /** 
     * TEST: createBackup
     * Created by Roger Unwin: Wed Oct  1 17:08
     * @author Roger Unwin
     * 
     * Notes: GCE does not support manually creating backups. 
     * Left that for the first person to run this whose database supports backups...
     * 
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
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

        if( support.getCapabilities().supportsDemandBackups() ) {
            fail("Please implement createBackup test.");
        }
        else {
            tm.ok("Platform does not support manually creating of individual database backups.");
        }
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
            String id = p.provisionRDBMS(support, "provisionRdbms", "dsnrdbms", null);

            tm.out("New Database", id);
            assertNotNull("No database was created by this test", id);
            Assert.assertNotNull("database has not been created", support.getDatabase(id));
        }
        else {
            fail("No platform resources were initialized for the test run");
        }
    }

    private @Nullable Database waitForDatabaseState(@Nonnull String dbId, int waitMinutes, @Nonnull DatabaseState state) throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();
        if( services == null ) {
            return null;
        }
        RelationalDatabaseSupport support = services.getRelationalDatabaseSupport();
        if( support == null ) {
            return null;
        }
        long timeout = System.currentTimeMillis() + waitMinutes*60*1000;
        while( timeout > System.currentTimeMillis() ) {
            Database instance = support.getDatabase(dbId);
            if( instance != null && instance.getCurrentState().equals(state) ) {
                return instance;
            }
            try {
                Thread.sleep(20000);
            }
            catch( InterruptedException ignore ) { }
        }
        return null;
    }

    /**
     * Authorize our IP and verify we can connect
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
    @Test
    public void checkAccess() throws CloudException, InternalException {
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

        String ourIp = getOurIp();
        if( ourIp == null ) {
            tm.warn("Unable to find out our IP, test can't continue");
            return;
        }

        Database instance = waitForDatabaseState(testDatabaseId, 15, DatabaseState.AVAILABLE);
        if( instance == null ) {
            fail("The database instance is not in available state to run this test");
        }
        assertFalse("Was able to connect to the database server before access was granted, something is really really wrong", checkConnection(instance.getHostName(), instance.getHostPort()));

        support.addAccess(testDatabaseId, ourIp + "/32");
        // let the cloud settle in
        try { Thread.sleep(60000L); } catch( InterruptedException e ) {}
        assertTrue("Was unable to connect to the database server after access was granted", checkConnection(instance.getHostName(), instance.getHostPort()));

        support.revokeAccess(testDatabaseId, ourIp + "/32");
        // let the cloud settle in
        try { Thread.sleep(60000L); } catch( InterruptedException e ) {}

        assertFalse("Was able to connect to the database server after access was revoked", checkConnection(instance.getHostName(), instance.getHostPort()));
    }

    /**
     * Check a TCP connection can be established to given host:port
     * @param host
     * @param port
     * @return true if can connect
     */
    // TODO(stas): move out to a util class
    private boolean checkConnection(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(2000);
            final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final int data = in.read(); // -1 no data to read.
            return (data != -1);
        }
        catch( IOException e ) {
        }
        finally {
            if( socket != null ) {
                try { socket.close(); } catch( IOException e ) {}
            }

        }
        return false;
    }

    /**
     * Get our external IP address
     * @return null if there was a problem
     */
    // TODO(stas): move out to a util class
    private @Nullable String getOurIp() {
        final HttpClient client = new DefaultHttpClient();
        HttpResponse response = null;
        try {
            response = client.execute(new HttpGet("http://ipinfo.io/ip"));
            if( response.getStatusLine().getStatusCode() == 200 ) {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                return reader.readLine();
            }
        }
        catch( IOException e ) {
        }
        finally {
            if( response != null ) {
                try { response.getEntity().getContent().close(); } catch( IOException ignore ) {}
            }
        }
        return null;
    }
    /*
     * Test if all CIDR ranges are set and revoked correctly
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

        Database instance = waitForDatabaseState(testDatabaseId, 15, DatabaseState.AVAILABLE);
        if( instance == null ) {
            fail("The database instance is not in available state to run this test");
        }

        // Let's see how many ranges did the database have by default
        Iterable<String> ranges = support.listAccess(testDatabaseId);
        int originalRanges = 0;
        for (String element: ranges) {
            originalRanges++;
        }
        List<String> testRanges = new ArrayList<String>();

        // First check if we can find any subnets, so we can authorize against their CIDRs
        NetworkServices networkServices = tm.getProvider().getNetworkServices();
        if( networkServices != null && networkServices.hasVlanSupport() &&
                !Requirement.NONE.equals(
                        networkServices.getVlanSupport().getCapabilities().getSubnetSupport())) {
            for( VLAN vlan : tm.getProvider().getNetworkServices().getVlanSupport().listVlans() ) {
                for( Subnet subnet : tm.getProvider().getNetworkServices().getVlanSupport().listSubnets(vlan.getProviderVlanId()) ) {
                    testRanges.add(subnet.getCidr());
                }
            }
        }
        // If there's no subnet support (hello, GCE!), try a hardcoded range
        if( testRanges.isEmpty() ) {
            tm.warn("Found no subnet ranges to authorize, adding a hardcoded range.");
            testRanges.add("72.197.190.0/24");
        }
        // Authorize our ranges
        for( String range : testRanges ) {
            support.addAccess(testDatabaseId, range);
        }

        // Check if those ranges have been added
        int count = 0;
        ranges = support.listAccess(testDatabaseId);
        for (String element: ranges) {
            count++;
        }
        assertEquals("Resulting CIDR number is incorrect after granting access", originalRanges + testRanges.size(), count);

        // Revoke access for all of our ranges now
        for( String range : testRanges) {
            support.revokeAccess(testDatabaseId, range);
        }

        // Check if all ranges were revoked
        count = 0;
        ranges = support.listAccess(testDatabaseId);
        for (String element: ranges)
            count++;
        assertEquals("Resulting CIDR number is incorrect after revoking access", originalRanges, count);
    }

    /**
     * This test verifies database modification, however is quite limited in scope as some things are not
     * feasible to change in an integration testing context (product size, storage size).
     * We will add configuration modification testing later on as better support for database configuration
     * is added to core.
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
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

        assertNotNull("Test database instance is not available", testDatabaseId);

        Database instance = waitForDatabaseState(testDatabaseId, 12, DatabaseState.AVAILABLE);

        // TODO: ideally we want to find the next cheap product and try to change to that, but unfortunately it
        // fails for AWS (no capacity in availability zone, which is totally weird but nothing we can do)
//        DatabaseEngine engine = instance.getEngine();
//        DatabaseProduct currentProduct = null;
//        Iterable<DatabaseProduct> products = support.listDatabaseProducts(engine);
//        // TODO: we shouldn't be doing this, a Database instance should really include a DatabaseProduct, not a size string
//        for( DatabaseProduct product : products ) {
//            if( product.getProductSize().equals(instance.getProductSize()) ) {
//                currentProduct = product;
//                break;
//            }
//        }
//        DatabaseProduct nextCheapestProduct = getNextCheapestProduct(products, currentProduct);
//
        String productSize = null;
//        if( nextCheapestProduct == null ) {
//            productSize = instance.getProductSize();
//            tm.warn("No other product sizes found, not changing the product size ("+productSize+")");
//        }
//        else {
//            productSize = nextCheapestProduct.getProductSize();
//        }

        // int storageInGigabytes = (int) (instance.getAllocatedStorageInGb() * 1.5); // must be at least 10% higher for AWS
        boolean applyImmediately = true;
        String configurationId = null; // TODO: can't pass arbitrary ids here // "new-configuration-id");
        String newAdminUser = "dasein";
        String newAdminPassword = "notasecret";
        int newPort = instance.getHostPort() + 1;
        int snapshotRetentionInDays = 5;
        TimeWindow preferredMaintenanceWindow = new TimeWindow();
        preferredMaintenanceWindow.setStartDayOfWeek(DayOfWeek.MONDAY);
        preferredMaintenanceWindow.setStartHour(3);
        preferredMaintenanceWindow.setStartMinute(0);
        preferredMaintenanceWindow.setEndDayOfWeek(DayOfWeek.SUNDAY);
        preferredMaintenanceWindow.setEndHour(5);
        preferredMaintenanceWindow.setEndMinute(0);
        TimeWindow preferredBackupWindow = new TimeWindow();
        preferredBackupWindow.setStartHour(1);
        preferredBackupWindow.setStartMinute(0);
        preferredBackupWindow.setEndHour(2);
        preferredBackupWindow.setEndMinute(45);
        support.alterDatabase(testDatabaseId, applyImmediately, productSize, 0, configurationId, newAdminUser, newAdminPassword, newPort, snapshotRetentionInDays, preferredMaintenanceWindow, preferredBackupWindow);

        Database database = support.getDatabase(testDatabaseId);
//        Assert.assertEquals(productSize.toLowerCase(), database.getProductSize().toLowerCase());
//        Assert.assertEquals("Allocated storage has not been updated", storageInGigabytes, database.getAllocatedStorageInGb());
//        Assert.assertEquals(configurationId, database.getConfiguration());
        if (support.getCapabilities().supportsMaintenanceWindows()) {
            // TODO: TimeWindow should implement #equals, so for now we'll use strings :-(
            Assert.assertEquals("Preferred maintenance window has not been updated", preferredMaintenanceWindow.toString(), database.getMaintenanceWindow().toString());
        }
        // TODO: TimeWindow should implement #equals, so for now we'll use strings :-(
        assertEquals("Preferred backup window has not been updated", preferredBackupWindow.toString(), database.getBackupWindow().toString());

        //Assert.assertEquals(newAdminUser, database.getAdminUser());
        //Assert.assertEquals(newPort, database.getHostPort());

        tm.ok("alterDatabase appears fine");
    }

    /**
     * FIXME: Stas commented this test out as it's unreliable in AWS. Not all products of all versions can be created
     * successfully in a test context, which doesn't mean the implementation is wrong. We should think how to improve
     * this test. Also we don't need to test against all combinations of all engines: we are testing the API, not the
     * cloud itself.
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
//    @Test DISABLED
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
                String id = p.provisionRDBMS(support, "provisionRdbms", "dsnrdbms", dbEngine);

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


    /**
     * This test will only execute where Oracle SE1 is available
     *
     * @throws CloudException an error occurred in the cloud provider
     * @throws InternalException an error occurred within the Dasein Cloud API implementation
     */
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
        String dbName = "dsnora" + ( System.currentTimeMillis() % 10000 );
        String expectedDbName = dbName.toUpperCase().substring(0, 8);
        String id = support.createFromScratch(dbName, PlatformResources.getCheapestProduct(support, oracleEngine, null), null, "dasein", PlatformResources.randomPassword(), 3000);
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

    private void removeDatabase( String id ) throws CloudException, InternalException {
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
            long timeout = System.currentTimeMillis() + ( CalendarWrapper.MINUTE * 20L );
            Database db = support.getDatabase(id);

            while( timeout > System.currentTimeMillis() ) {
                if( canRemove(db) ) {
                    break;
                }
                try {
                    Thread.sleep(15000L);
                }
                catch( InterruptedException ignore ) {
                }
                try {
                    db = support.getDatabase(db.getProviderDatabaseId());
                }
                catch( Throwable ignore ) {
                }
            }
            assertNotNull("The test database is not found", db);
            tm.out("Before", db.getCurrentState());

            support.removeDatabase(id);
            db = support.getDatabase(id);
            DatabaseState s = ( db == null ? DatabaseState.DELETED : db.getCurrentState() );
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

    private boolean canRemove( @Nullable Database db ) {
        if( db == null ) {
            return true;
        }
        switch( db.getCurrentState() ) {
            case DELETING:
            case DELETED:
            case AVAILABLE:
            case STORAGE_FULL:
            case FAILED:
                return true;
            default:
                return false;
        }
    }
}
