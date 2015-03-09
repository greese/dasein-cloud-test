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

package org.dasein.cloud.test.network;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.network.DNSRecord;
import org.dasein.cloud.network.DNSRecordType;
import org.dasein.cloud.network.DNSSupport;
import org.dasein.cloud.network.DNSZone;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Tests stateful functionality for DNS support in Dasein Cloud.
 * <p>Created by George Reese: 3/01/13 6:18 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatefulDNSTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulDNSTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private DNSRecord testRecord;
    private String    testZoneId;
    private String    testRecordName;

    public StatefulDNSTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        if( name.getMethodName().equals("removeZone") ) {
            testZoneId = tm.getTestZoneId(DaseinTestManager.REMOVED, true);
        }
        else if( name.getMethodName().equals("addRecord") ) {
            testZoneId = tm.getTestZoneId(DaseinTestManager.STATEFUL, true);
            if( testZoneId != null ) {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services != null ) {
                    DNSSupport support = services.getDnsSupport();

                    if( support != null ) {
                        try {
                            DNSZone zone = support.getDnsZone(testZoneId);

                            if( zone != null ) {
                                testRecordName = "dsn" + System.currentTimeMillis() + "." + zone.getDomainName();
                            }
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
        }
        else if( name.getMethodName().equals("removeRecord") ) {
            testZoneId = tm.getTestZoneId(DaseinTestManager.STATEFUL, true);

            if( testZoneId != null ) {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services == null ) {
                    tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
                    return;
                }
                DNSSupport support = services.getDnsSupport();

                if( support == null ) {
                    tm.ok("DNS is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
                    return;
                }
                try {
                    DNSZone zone = support.getDnsZone(testZoneId);

                    if( zone != null ) {
                        testRecordName = "dsn" + System.currentTimeMillis() + "." + zone.getDomainName();
                    }
                    testRecord = support.addDnsRecord(testZoneId, DNSRecordType.A, testRecordName, 3600, "210.10.10.10");
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void createZone() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        DNSSupport support = services.getDnsSupport();

        if( support == null ) {
            tm.ok("DNS is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        NetworkResources resources = DaseinTestManager.getNetworkResources();

        assertNotNull("Failed to initialize network resources prior to test execution", resources);
        String zoneId = resources.provisionDNSZone(support, DaseinTestManager.STATEFUL, "dasein", "org");

        tm.out("New DNS Zone", zoneId);
        assertNotNull("No zone was provided for the newly created DNS zone", zoneId);

        DNSZone zone = support.getDnsZone(zoneId);

        assertNotNull("No DNS zone exists for the new DNS ID " + zoneId, zone);
    }

    @Test
    public void removeZone() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        DNSSupport support = services.getDnsSupport();

        if( support == null ) {
            tm.ok("DNS is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testZoneId != null ) {
            DNSZone zone = support.getDnsZone(testZoneId);

            tm.out("Before", zone);

            support.deleteDnsZone(testZoneId);
            zone = support.getDnsZone(testZoneId);

            tm.out("After", zone);

            assertNull("DNS zone still exists after the deletion", zone);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Test was not run because this account is not subscribed for DNS support in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("No test DNS zone exists for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void addRecord() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        DNSSupport support = services.getDnsSupport();

        if( support == null ) {
            tm.ok("DNS is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testZoneId != null && testRecordName != null ) {
            DNSRecord record = support.addDnsRecord(testZoneId, DNSRecordType.A, testRecordName, 3600, "210.10.10.10");

            tm.out("New DNS Record", record);
            assertNotNull("No DNS record was created for the test", record);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Test was not run because this account is not subscribed for DNS support in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("No test DNS zone exists for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void removeRecord() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        DNSSupport support = services.getDnsSupport();

        if( support == null ) {
            tm.ok("DNS is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testRecord != null ) {
            support.deleteDnsRecords(testRecord);
            boolean found = false;

            for( DNSRecord record : support.listDnsRecords(testZoneId, DNSRecordType.A, null) ) {
                if( record.getName().equals(testRecord.getName()) ) {
                    String[] testValues = testRecord.getValues();
                    String[] values = record.getValues();

                    if( Arrays.equals(testValues, values) ) {
                        found = true;
                        break;
                    }
                }
            }
            assertFalse("The DNS record still exists in the system", found);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Test was not run because this account is not subscribed for DNS support in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("No test DNS zone exists for the test " + name.getMethodName());
            }
        }
    }
}
