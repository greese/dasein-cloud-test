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

import java.io.IOException;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.util.APITrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ContextTestCase extends BaseTestCase {
    
    private CloudProvider provider         = null;
    
    public ContextTestCase(String name) { super(name); } 
    
    @Before
    @Override
    public void setUp() throws CloudException, InternalException, InstantiationException, IllegalAccessException, IOException {
        provider = getProvider();
        ProviderContext ctx = getTestContext();
        
        if( getName().equals("testBadSecret") ) {
            ctx.setAccessPrivate("ThisCannotPossiblyBeASecretKey".getBytes());
        }
        else if( getName().equals("testFakeAccount") ) {
            ctx.setAccountNumber("MyWibblesAreTribbles");
            ctx.setAccessPublic("MyWibblesAreTribbles".getBytes());
        }
        else if( getName().equals("testContextReconnect") ) {
            provider.connect(ctx);
            
            String id = provider.testContext();
            
            ctx.setAccountNumber(id);
            return;
        }
        provider.connect(ctx);
    }
    
    @After
    @Override
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
    public void testContext() throws CloudException, InternalException {
        begin();
        String id = provider.testContext();
        
        assertNotNull("Connection test failed", id);
        out("Account: " + id);
        end();
    }
    
    @Test 
    public void testContextReconnect() throws CloudException, InternalException {
        begin();
        String id = provider.testContext();
        
        assertEquals("New account number fails connection", id, provider.getContext().getAccountNumber());
        end();
    }
    
    @Test
    public void testBadSecret() throws CloudException, InternalException {
        begin();
        assertNull("Connection succeeded with bad API secret", provider.testContext());
        end();
    }
    
    @Test
    public void testFakeAccount() throws CloudException, InternalException {
        begin();
        assertNull("Connection succeeded with fake account", provider.testContext());
        end();
    }
}
