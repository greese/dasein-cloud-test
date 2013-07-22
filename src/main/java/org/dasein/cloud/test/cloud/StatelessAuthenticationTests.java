/**
 * Copyright (C) 2009-2013 Dell, Inc.
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

package org.dasein.cloud.test.cloud;

import junit.framework.Assert;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * General authentication tests to verify the ability of a Dasein Cloud implementation to authenticate with the cloud
 * provider.
 * <p>Created by George Reese: 2/18/13 6:35 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
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

    private CloudProvider provider;

    public StatelessAuthenticationTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        if( name.getMethodName().equals("invalidPassword") ) {
            provider = DaseinTestManager.constructProvider(null, null, "ThisCannotPossiblyBeASecretKey");
        }
        else if( name.getMethodName().equals("invalidAccount") ) {
            provider = DaseinTestManager.constructProvider("MyWibblesAreTribbles", "MyWibblesAreTribbles", null);
        }
        else if( name.getMethodName().equals("reconnect") ) {
            provider = DaseinTestManager.constructProvider();
            ProviderContext ctx = provider.getContext();

            Assert.assertNotNull("Context cannot be null at this point", ctx);
            String id = provider.testContext();

            ctx.setAccountNumber(id);
        }
        else {
            provider = DaseinTestManager.constructProvider();
        }
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void authenticate() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        String id = tm.getProvider().testContext();

        tm.out("Account", id);
        assertNotNull("Connection test failed", id);
    }

    @Test
    public void reconnect() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        String id = provider.testContext();

        //noinspection ConstantConditions
        assertEquals("New account number fails connection", id, provider.getContext().getAccountNumber());
    }

    @Test
    public void invalidPassword() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        assertNull("Connection succeeded with bad API secret", provider.testContext());
    }

    @Test
    public void invalidAccount() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        assertNull("Connection succeeded with fake account", provider.testContext());
    }
}
