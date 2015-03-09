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

package org.dasein.cloud.test.cloud;

import junit.framework.Assert;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.*;
import org.junit.rules.TestName;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

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
    private String initialAccount;

    public StatelessAuthenticationTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        initialAccount = System.getProperty("accountNumber");

        if( name.getMethodName().equals("invalidPassword") ) {
            provider = DaseinTestManager.constructProvider(null, null, "ThisCannotPossiblyBeASecretKey");
        }
        else if( name.getMethodName().equals("invalidAccount") ) {
            provider = DaseinTestManager.constructProvider("MyWibblesAreTribbles", null, null);
        }
        else if( name.getMethodName().equals("invalidSharedKey") ) {
            provider = DaseinTestManager.constructProvider(null, "MyWibblesAreTribbles", null);
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
    public void invalidPassword() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        assertNull("Connection succeeded with bad API secret", provider.testContext());
    }

    @Test
    public void invalidSharedKey() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        assertNull("Connection succeeded with fake account", provider.testContext());
    }

    @Test
    public void invalidAccount() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        String accountNumber = provider.testContext();

        // Null may indicate that unlike in AWS in this particular provider
        // the account number must be correct to authenticate a call, so we
        // can not test any further.
        assumeNotNull(accountNumber);
        assertEquals("Returned account number is not the same as the configured one", accountNumber, initialAccount);
    }
}
