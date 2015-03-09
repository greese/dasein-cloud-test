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

package org.dasein.cloud.test.identity;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.identity.SSHKeypair;
import org.dasein.cloud.identity.ShellKeySupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/18/13 7:34 PM</p>
 *
 * @author George Reese
 */
public class StatefulKeypairTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulKeypairTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testKeyId;

    public StatefulKeypairTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        if( name.getMethodName().equals("removeKeypair") ) {
            IdentityResources id = DaseinTestManager.getIdentityResources();

            if( id != null ) {
                testKeyId = id.getTestKeypairId(DaseinTestManager.REMOVED, true);
            }
        }
    }

    @After
    public void after() {
        try {
            if( testKeyId != null ) {
                //noinspection ConstantConditions
                tm.getProvider().getIdentityServices().getShellKeySupport().deleteKeypair(testKeyId);
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        testKeyId = null;
        tm.end();
    }

    @Test
    public void generateKeypair() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services != null ) {
            ShellKeySupport support = services.getShellKeySupport();

            if( support != null ) {
                if( Requirement.REQUIRED.equals(support.getKeyImportSupport()) ) {
                    try {
                        SSHKeypair keypair = support.createKeypair("dsnkp" + (System.currentTimeMillis()%10000));

                        testKeyId = keypair.getProviderKeypairId();
                        fail("Keypair generation is not supported, but the request appears to have completed");
                    }
                    catch( OperationNotSupportedException expected ) {
                        tm.ok("Caught operation not supported exception as expected");
                    }
                }
                else {
                    SSHKeypair keypair = support.createKeypair("dsnkp" + (System.currentTimeMillis()%10000));

                    tm.out("Keypair", keypair);
                    assertNotNull("The create option must not return a null value", keypair);
                    testKeyId = keypair.getProviderKeypairId();
                }
            }
            else {
                tm.ok("No shell key support in this cloud");
            }
        }
        else {
            tm.ok("No identity services in this cloud");
        }
    }

    /*
    @Test
    public void importKeypair() throws CloudException, InternalException {
    assumeTrue(!tm.isTestSkipped());
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services != null ) {
            ShellKeySupport support = services.getShellKeySupport();

            if( support != null ) {
                String publicKey = null;

                try {
                    // TODO: import keypair
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                assertNotNull("No keypair was created for import and so the import cannot be tested", publicKey);
                if( Requirement.NONE.equals(support.getKeyImportSupport()) ) {
                    try {
                        SSHKeypair keypair = support.importKeypair("dsnkp" + (System.currentTimeMillis()%10000), publicKey);

                        testKeyId = keypair.getProviderKeypairId();
                        fail("Keypair import is not supported, but the request appears to have completed");
                    }
                    catch( OperationNotSupportedException expected ) {
                        tm.ok("Caught operation not supported exception as expected");
                    }
                }
                else {
                    SSHKeypair keypair = support.importKeypair("dsnkp" + (System.currentTimeMillis()%10000), publicKey);

                    tm.out("Keypair", keypair);
                    assertNotNull("The import option must not return a null value", keypair);
                    testKeyId = keypair.getProviderKeypairId();
                }
            }
            else {
                tm.ok("No shell key support in this cloud");
            }
        }
        else {
            tm.ok("No identity services in this cloud");
        }
    }
    */

    @Test
    public void removeKeypair() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services != null ) {
            ShellKeySupport support = services.getShellKeySupport();

            if( support != null ) {
                if( testKeyId != null ) {
                    SSHKeypair keypair = support.getKeypair(testKeyId);

                    tm.out("Before", keypair != null ? "exists" : "doesn't exist");
                    support.deleteKeypair(testKeyId);
                    try { Thread.sleep(3000L); }
                    catch( InterruptedException ignore ) { }
                    keypair = support.getKeypair(testKeyId);
                    tm.out("After", keypair != null ? "exists" : "doesn't exist");
                    assertNull("Keypair still exists after delete", keypair);
                    testKeyId = null;
                }
                else {
                    tm.warn("No test keypair was found for this test");
                }
            }
            else {
                tm.ok("No keypair support in this cloud");
            }
        }
        else {
            tm.ok("No identity services in this cloud");
        }
    }
}