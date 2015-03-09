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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/28/13 2:57 PM</p>
 *
 * @author George Reese
 */
public class StatelessDNSTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessDNSTests.class);
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

    public StatelessDNSTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testZoneId = tm.getTestZoneId(DaseinTestManager.STATELESS, false);
        if( testZoneId != null ) {
            if( name.getMethodName().equals("recordContent") ) {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services != null ) {
                    DNSSupport support = services.getDnsSupport();

                    if( support != null ) {
                        for( DNSRecordType type : DNSRecordType.values() ) {
                            try {
                                Iterator<DNSRecord> records = support.listDnsRecords(testZoneId, type, null).iterator();

                                if( records.hasNext() ) {
                                    testRecord = records.next();
                                    break;
                                }
                            }
                            catch( Throwable ignore ) {
                                // ignore
                            }
                        }
                    }
                }
            }
        }
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
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
        tm.out("Subscribed", support.isSubscribed());
        tm.out("Term for DNS Record", support.getProviderTermForRecord(Locale.getDefault()));
        tm.out("Term for DNS Zone", support.getProviderTermForZone(Locale.getDefault()));

        assertNotNull("The provider term for a DNS record may not be null", support.getProviderTermForRecord(Locale.getDefault()));
        assertNotNull("The provider term for a DNS zone may not be null", support.getProviderTermForZone(Locale.getDefault()));
    }

    @Test
    public void getBogusZone() throws CloudException, InternalException {
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
        DNSZone zone = support.getDnsZone(UUID.randomUUID().toString());

        tm.out("Bogus DNS Zone", zone);
        assertNull("Got a valid result back for the bogus zone ID", zone);
    }

    @Test
    public void getZone() throws CloudException, InternalException {
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

            tm.out("DNS Zone", zone);
            assertNotNull("No DNS zone was found matching the test DNS zone ID " + testZoneId, zone);
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

    private void assertDNSZone(@Nonnull DNSZone zone) {
        assertNotNull("The DNS zone ID may not be null", zone.getProviderDnsZoneId());
        assertNotNull("The DNS zone name may not be null", zone.getName());
        assertNotNull("The DNS zone description may not be null", zone.getDescription());
        assertNotNull("The owner account may not be null", zone.getProviderOwnerId());
        assertNotNull("The domain name may not be null", zone.getDomainName());
        assertNotNull("The nameservers list may be empty, but it may not be null", zone.getNameservers());
    }

    @Test
    public void zoneContent() throws CloudException, InternalException {
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

            assertNotNull("No DNS zone was found matching the test DNS zone ID " + testZoneId, zone);

            tm.out("DNS Zone ID", zone.getProviderDnsZoneId());
            tm.out("Name", zone.getName());
            tm.out("Owner Account", zone.getProviderOwnerId());
            tm.out("Domain Name", zone.getDomainName());
            tm.out("Nameservers", Arrays.toString(zone.getNameservers()));
            tm.out("Description", zone.getDescription());

            assertDNSZone(zone);
            assertEquals("The DNS zone provided did not match the requested ID", testZoneId, zone.getProviderDnsZoneId());
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
    public void listZones() throws CloudException, InternalException {
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
        Iterable<DNSZone> zones = support.listDnsZones();
        int count = 0;

        assertNotNull("The list of DNS zones may not be null even if the account is not subscribed", zones);
        for( DNSZone zone : zones ) {
            count++;
            tm.out("DNS Zone", zone);
        }
        tm.out("Total DNS Zone Count", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
                tm.ok("The account is not subscribed to DNS zones, so this test is invalid");
            }
            else {
                tm.warn("This test is likely invalid as no network firewalls were provided in the results for validation");
            }
        }
        for( DNSZone zone : zones ) {
            assertDNSZone(zone);
        }
    }

    private void assertRecord(@Nonnull DNSRecord record) {
        assertNotNull("The DNS record name may not be null", record.getName());
        assertNotNull("The zone ID may not be null", record.getProviderZoneId());
        assertNotNull("The DNS record type may not be null", record.getType());
        assertNotNull("The record values may not be null", record.getValues());
        assertTrue("There must be at least one value", record.getValues().length > 0);
        assertTrue("The DNS record TTL must be greater than 0", record.getTtl() > 0);
    }

    @Test
    public void recordContent() throws CloudException, InternalException {
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
            tm.out("Zone ID", testRecord.getProviderZoneId());
            tm.out("Name", testRecord.getName());
            tm.out("Type", testRecord.getType());
            tm.out("TTL", testRecord.getTtl());
            tm.out("Values", Arrays.toString(testRecord.getValues()));
        }
        else if( !support.isSubscribed() ) {
            tm.ok("Not subscribed to DNS services in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
        }
        else {
            fail("No test record was in place");
        }
    }

    @Test
    public void listRecords() throws CloudException, InternalException {
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
            int total = 0;

            for( DNSRecordType type : DNSRecordType.values() ) {
                Iterable<DNSRecord> records = support.listDnsRecords(testZoneId, type, null);
                int count = 0;

                assertNotNull("DNS records for any type may not be null", records);
                tm.out(type.name(), "");
                for( DNSRecord record : records ) {
                    count++;
                    tm.out(type.name(), record);
                }
                total += count;
                tm.out("Total " + type + " Records", count);
                for( DNSRecord record : records ) {
                    assertRecord(record);
                }
            }
            tm.out("Total DNS Records", total);
            if( total < 1 ) {
                tm.warn("No DNS records were present in the test zone, so this test may not be valid");
            }
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
