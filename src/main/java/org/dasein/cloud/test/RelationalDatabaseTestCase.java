/**
 * Copyright (C) 2009-2012 enStratus Networks Inc
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

package org.dasein.cloud.test;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.platform.Database;
import org.dasein.cloud.platform.DatabaseEngine;
import org.dasein.cloud.platform.DatabaseProduct;
import org.dasein.cloud.platform.DatabaseState;
import org.dasein.cloud.util.APITrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Random;

public class RelationalDatabaseTestCase extends BaseTestCase {
    static private Iterable<DatabaseEngine>   testEngines = null;
    static private String                     testDatabaseId = null;
    
    private CloudProvider                     provider    = null;

    public RelationalDatabaseTestCase(String name) { super(name); }
    
    private String getRandomPassword() {
        StringBuilder str = new StringBuilder();
        Random random = new Random();
        int len = 10 + random.nextInt(5);
        
        while( str.length() < len ) {
            int c = random.nextInt()%255;
            
            if( c >= 'a' && c <= 'z' ) {
                str.append((char)c);
            }
            else if( c >= 'A' && c <= 'Z' ) {
                str.append((char)c);
            }
            else {
                str.append(c%10);
            }
        }
        return str.toString();
    }
    
    private void createTestDatabase() throws CloudException, InternalException {
        DatabaseProduct product = provider.getPlatformServices().getRelationalDatabaseSupport().getDatabaseProducts(testEngines.iterator().next()).iterator().next();
        
        testDatabaseId = provider.getPlatformServices().getRelationalDatabaseSupport().createFromScratch("dsn" + System.currentTimeMillis(), product, product.getEngine().getVersion(), "dasein", getRandomPassword(), 3306);
    }
    
    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        provider = getProvider();
        provider.connect(getTestContext());
        
        String name = getName();
        
        if( testEngines == null ) {
            testEngines = provider.getPlatformServices().getRelationalDatabaseSupport().getDatabaseEngines();
        }
        if( name.equals("testListDatabases") || name.equals("testGetDatabase") || name.equals("testDatabaseContent") || name.equals("testRemoveDatabase") ) {
            createTestDatabase();
        }
    }
    
    @After
    @Override
    public void tearDown() {
        if( testDatabaseId != null ) {
            try {
                Database db = provider.getPlatformServices().getRelationalDatabaseSupport().getDatabase(testDatabaseId);
                
                while( db != null && !DatabaseState.AVAILABLE.equals(db.getCurrentState()) && !DatabaseState.DELETED.equals(db.getCurrentState()) ) {
                    try { Thread.sleep(20000L); }
                    catch( InterruptedException e ) { }
                    db = provider.getPlatformServices().getRelationalDatabaseSupport().getDatabase(testDatabaseId);
                }
                if( db != null ) {
                    provider.getPlatformServices().getRelationalDatabaseSupport().removeDatabase(testDatabaseId);
                }
            }
            catch( Throwable t ) {
                t.printStackTrace();
            }
        }
        APITrace.report(getName());
        APITrace.reset();
        try {
            if( provider != null ) {
                provider.close();
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
    }

    @Test
    public void testCreateDatabase() throws InternalException, CloudException {
        begin();
        DatabaseProduct product = provider.getPlatformServices().getRelationalDatabaseSupport().getDatabaseProducts(testEngines.iterator().next()).iterator().next();

        testDatabaseId = provider.getPlatformServices().getRelationalDatabaseSupport().createFromScratch("dsn" + System.currentTimeMillis(), product, product.getEngine().getVersion(), "dasein", getRandomPassword(), 3306);

        assertNotNull("Database was not created", testDatabaseId);
        assertNotNull("Unable to find new database in the cloud", provider.getPlatformServices().getRelationalDatabaseSupport().getDatabase(testDatabaseId));
        end();
    }

    @Test
    public void testRemoveDatabase() throws InternalException, CloudException {
        begin();

        Database db = provider.getPlatformServices().getRelationalDatabaseSupport().getDatabase(testDatabaseId);

        while( db != null && !DatabaseState.AVAILABLE.equals(db.getCurrentState()) && !DatabaseState.DELETED.equals(db.getCurrentState()) ) {
            try { Thread.sleep(20000L); }
            catch( InterruptedException e ) { }
            db = provider.getPlatformServices().getRelationalDatabaseSupport().getDatabase(testDatabaseId);
        }
        if( db == null ) {
            fail("No test database exists");
        }
        provider.getPlatformServices().getRelationalDatabaseSupport().removeDatabase(testDatabaseId);
        db = provider.getPlatformServices().getRelationalDatabaseSupport().getDatabase(testDatabaseId);
        if( db != null ) {
            assertTrue("Database is still running", !DatabaseState.AVAILABLE.equals(db.getCurrentState()));
        }
        testDatabaseId = null;
        end();
    }
}
