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

import java.util.Locale;
import java.util.UUID;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/18/13 7:34 PM</p>
 *
 * @author George Reese
 */
public class StatelessKeypairTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessKeypairTests.class);
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

    public StatelessKeypairTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testKeyId = tm.getTestKeypairId(DaseinTestManager.STATELESS, false);
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services != null ) {
            ShellKeySupport support = services.getShellKeySupport();

            if( support != null ) {
                tm.out("Subscribed", support.isSubscribed());
                tm.out("Term for Keypair", support.getProviderTermForKeypair(Locale.getDefault()));
                tm.out("Key Import Req", support.getKeyImportSupport());
            }
            else {
                tm.ok("No shell key support in this cloud");
            }
        }
        else {
            tm.ok("No identity services in this cloud");
        }
    }

    @Test
    public void getBogusKeypair() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services != null ) {
            ShellKeySupport support = services.getShellKeySupport();

            if( support != null ) {
                SSHKeypair keypair = support.getKeypair(UUID.randomUUID().toString());

                tm.out("Bogus Keypair", keypair);
                assertNull("Found a bogus keypair when none should exist", keypair);
            }
            else {
                tm.ok("No shell key support in this cloud");
            }
        }
        else {
            tm.ok("No identity services in this cloud");
        }
    }

    @Test
    public void getKeypair() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services != null ) {
            ShellKeySupport support = services.getShellKeySupport();

            if( support != null ) {
                if( testKeyId != null ) {
                    SSHKeypair keypair = support.getKeypair(testKeyId);

                    tm.out("Keypair", keypair);
                    assertNotNull("Failed to find the test keypair", keypair);
                }
                else {
                    tm.warn("No test key was specified, this test is probably invalid");
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

    @Test
    public void keypairContent() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services != null ) {
            ShellKeySupport support = services.getShellKeySupport();

            if( support != null ) {
                if( testKeyId != null ) {
                    SSHKeypair keypair = support.getKeypair(testKeyId);

                    assertNotNull("Failed to find the test keypair", keypair);
                    tm.out("Keypair ID", keypair.getProviderKeypairId());
                    tm.out("Name", keypair.getName());
                    tm.out("Owner Account", keypair.getProviderOwnerId());
                    tm.out("Region ID", keypair.getProviderRegionId());
                    tm.out("Fingerprint", keypair.getFingerprint());
                    tm.out("Public Key", keypair.getPublicKey());
                    tm.out("PrivateKey", keypair.getPrivateKey());

                    assertNotNull("Keypair ID may not be null", keypair.getProviderKeypairId());
                    assertNotNull("Keypair name may not be null", keypair.getName());
                    assertNotNull("Keypair owning account may not be null", keypair.getProviderOwnerId());
                    assertEquals("Keypair region must match current region context", tm.getContext().getRegionId(), keypair.getProviderRegionId());
                    if( keypair.getFingerprint() == null ) {
                        tm.warn("A keypair really, really should have a fingerprint if supported by the cloud");
                    }
                }
                else {
                    tm.warn("No test key was specified, this test is probably invalid");
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

    @Test
    public void listKeypairs() throws CloudException, InternalException {
        IdentityServices services = tm.getProvider().getIdentityServices();

        if( services != null ) {
            ShellKeySupport support = services.getShellKeySupport();

            if( support != null ) {
                Iterable<SSHKeypair> keypairs = support.list();
                boolean found = false;
                int count = 0;

                assertNotNull("The list of keypairs may not be null", keypairs);
                for( SSHKeypair keypair : keypairs ) {
                    count++;
                    tm.out("Keypair", keypair);
                    if( testKeyId != null && testKeyId.equals(keypair.getProviderKeypairId()) ) {
                        found = true;
                    }
                }
                tm.out("Total Keypair Count", count);
                if( count < 1 ) {
                    tm.warn("No keypairs were found, so this test may have been invalid");
                }
                if( testKeyId != null ) {
                    assertNotNull("Unable to find the test keypair among the listed keypairs", found);
                }
                else {
                    tm.warn("No test key exists, so unable to validate key listing");
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
}