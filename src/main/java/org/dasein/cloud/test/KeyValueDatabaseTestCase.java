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
import org.dasein.cloud.util.APITrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KeyValueDatabaseTestCase extends BaseTestCase {
    private CloudProvider provider;

    public KeyValueDatabaseTestCase(String name) { super(name); }
    
    @Before
    public void setUp() throws CloudException, InternalException, InstantiationException, IllegalAccessException {
        begin();
        provider = getProvider();
        provider.connect(getTestContext());
    }
    
    @After
    public void tearDown() {
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
    public void testAddKeyValuePair() {
        
    }
    
    @Test
    public void testAddMultipleKeyValuePairs() {
        
    }
    
    @Test
    public void testCreateKeyValueDatabase() {
        
    }
    
    @Test
    public void testGetBogusKeyValueDatabase() {
        
    }
    
    @Test
    public void testGetKeyValueDatabase() {
        
    }
    
    @Test
    public void testKeyValueDatabaseContent() {
        
    }
    
    @Test
    public void testListKeyValueDatabases() {
        
    }
    
    @Test
    public void testMetaData() {
        
    }
    
    @Test
    public void testQuery() {
        
    }
    
    @Test
    public void testRemoveKeyValueDatabase() {
        
    }
    
    @Test
    public void testReplaceKeyValuePair() {
        
    }
    
    @Test
    public void testRemoveMultipleKeyValuePairs() {
        
    }
    
    @Test
    public void testSubscription() {
        
    }
}
