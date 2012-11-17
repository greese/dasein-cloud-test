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

import java.util.Locale;
import java.util.UUID;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.network.DNSRecord;
import org.dasein.cloud.network.DNSRecordType;
import org.dasein.cloud.network.DNSSupport;
import org.dasein.cloud.network.DNSZone;
import org.dasein.cloud.util.APITrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DNSTestCase extends BaseTestCase {
    private CloudProvider cloud          = null;
    private DNSRecord     recordToRemove = null;
    private DNSRecord     testRecord     = null;
    //private DNSZone       testZone       = null;
    private String        zoneToRemove   = null;
    
    public DNSTestCase(String name) { super(name); }

    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        String name = getName();
        
        cloud = getProvider();
        cloud.connect(getTestContext());
        if( !name.equals("testCreateZone") && !name.equals("testSubscription") && !name.equals("testMetaData") && !name.equals("testGetBogusZone") ) {
            zoneToRemove = cloud.getNetworkServices().getDnsSupport().createDnsZone("daseintest.org", "Dasein Test Domain", "Dasein Test Domain");
        }
        if( name.equals("testRemoveRecord") || name.equals("testRecordContent") ) {
            testRecord = cloud.getNetworkServices().getDnsSupport().addDnsRecord(zoneToRemove, DNSRecordType.A, "dnstest.daseintest.org.", 600, "174.129.196.233");
            recordToRemove = testRecord;
        }
    }
    
    @After
    @Override
    public void tearDown() {
        try {
            if( recordToRemove != null ) {
                cloud.getNetworkServices().getDnsSupport().deleteDnsRecords(recordToRemove);
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( zoneToRemove != null ) {
                cloud.getNetworkServices().getDnsSupport().deleteDnsZone(zoneToRemove);
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        APITrace.report(getName());
        APITrace.reset();
        try {
            if( cloud != null ) {
                cloud.close();
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
    }

    @Test
    public void testSubscription() throws CloudException, InternalException {
        begin();
        assertTrue("Account is not subscribed, tests not possible", cloud.getNetworkServices().getDnsSupport().isSubscribed());
        end();
    }
    
    @Test
    public void testMetaData() throws CloudException, InternalException {
        begin();
        DNSSupport support = cloud.getNetworkServices().getDnsSupport();

        assertNotNull("Term for zone is null", support.getProviderTermForZone(Locale.getDefault()));
        assertNotNull("Term for record is null", support.getProviderTermForRecord(Locale.getDefault()));
        out("Zone:           " + support.getProviderTermForZone(Locale.getDefault()));
        out("Record:         " + support.getProviderTermForRecord(Locale.getDefault()));
        end();
    }
    
    @Test
    public void testListZones() throws CloudException, InternalException {
        begin();
        DNSSupport support = cloud.getNetworkServices().getDnsSupport();
        Iterable<DNSZone> zones = support.listDnsZones();

        assertNotNull("Zone list cannot be null", zones);
        try {
            for( DNSZone zone : zones ) {
                out("DNS Zone: " + zone);
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        end();
    }

    @Test
    public void testGetBogusZone() throws CloudException, InternalException {
        begin();
        DNSSupport support = cloud.getNetworkServices().getDnsSupport();
        String zoneId = UUID.randomUUID().toString();

        if( zoneId.length() > 24 ) {
            zoneId = zoneId.substring(0, 24);
        }
        assertNull("Found the bogus zone", support.getDnsZone(zoneId));
        end();
    }

    @Test
    public void testGetZone() throws CloudException, InternalException {
        begin();
        DNSSupport support = cloud.getNetworkServices().getDnsSupport();

        DNSZone zone = support.getDnsZone(zoneToRemove);

        assertNotNull("No zone was found", zone);
        assertEquals("Zone IDs do not match", zoneToRemove, zone.getProviderDnsZoneId());
        out(zone.toString());
        end();
    }

    @Test
    public void testZoneContent() throws CloudException, InternalException {
        begin();
        DNSSupport support = cloud.getNetworkServices().getDnsSupport();

        DNSZone zone = support.getDnsZone(zoneToRemove);

        assertNotNull("No zone was found", zone);
        assertNotNull("Provider zone ID may not null", zone.getProviderDnsZoneId());
        out("ID:             " + zone.getProviderDnsZoneId());
        assertEquals("Zone account numbers do not match", cloud.getContext().getAccountNumber(), zone.getProviderOwnerId());
        out("Owner:          " + zone.getProviderOwnerId());
        assertNotNull("Zone domain cannot be null", zone.getDomainName());
        out("Domain:         " + zone.getDomainName());
        assertNotNull("Zone name cannot be null", zone.getName());
        out("Name:           " + zone.getName());
        assertNotNull("Zone description cannot be null", zone.getDescription());
        out("Description:    " + zone.getDescription());
        assertNotNull("Nameservers must be given", zone.getNameservers());
        assertTrue("You must have at least one nameserver", zone.getNameservers().length > 0);
        StringBuilder str = new StringBuilder();
        for( String ns : zone.getNameservers() ) {
            str.append(ns);
            str.append(",");
        }
        out("Nameservers:    " + str.toString());
        end();
    }
    
    @Test
    public void testRecordContent() throws CloudException, InternalException {
        begin();
        DNSSupport support = cloud.getNetworkServices().getDnsSupport();
        DNSRecord record = null;

        for( DNSRecord r : support.listDnsRecords(testRecord.getProviderZoneId(), testRecord.getType(), testRecord.getName()) ) {
            record = r;
        }
        assertNotNull("No record was found", record);
        assertNotNull("Provider zone ID may not null", record.getProviderZoneId());
        out("Zone:      " + record.getProviderZoneId());
        assertNotNull("Name must be non-null", record.getName());
        out("Name:      " + record.getName());
        assertNotNull("Record type cannot be null", record.getType());
        out("Type:      " + record.getType());
        assertTrue("TTL must be non-zero", record.getTtl() > 0);
        out("TTL:       " + record.getTtl());
        if( record.getValues() != null ) {
            StringBuilder str = new StringBuilder();
            for( String val : record.getValues() ) {
                str.append(val);
                str.append(",");
            }
            out("Values:    " + str.toString());
        }
        end();
    }
    
    @Test
    public void testCreateZone() throws CloudException, InternalException {
        begin();
        DNSSupport support = cloud.getNetworkServices().getDnsSupport();

        zoneToRemove = support.createDnsZone("daseintest.org", "Dasein Test Domain", "Dasein Test Domain");
        assertNotNull("No zone was created", zoneToRemove);
        out(zoneToRemove);
        end();
    }
    
    @Test
    public void testAddRecord() throws CloudException, InternalException {
        begin();
        DNSSupport support = cloud.getNetworkServices().getDnsSupport();
        
        support.addDnsRecord(zoneToRemove, DNSRecordType.A, "dnstest.daseintest.org.", 600, "174.129.196.233");
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        try {
            for( DNSRecord record : support.listDnsRecords(zoneToRemove, DNSRecordType.A, "dnstest.daseintest.org.") ) {
                String[] values = record.getValues();
                    
                if( values != null && values.length == 1 && values[0].equals("174.129.196.233") ) {
                    recordToRemove = record;
                    break;
                }
            }
                
        }
        catch( Throwable ignore ) {
            // ignore
        }
        assertNotNull("Could not find newly created record", recordToRemove);
        end();
    }
    
    @Test
    public void testRemoveRecord() throws CloudException, InternalException {
        begin();
        DNSSupport support = cloud.getNetworkServices().getDnsSupport();
        
        support.deleteDnsRecords(testRecord);
        recordToRemove = null;
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        try {
            for( DNSRecord record : support.listDnsRecords(zoneToRemove, DNSRecordType.A, "dnstest.daseintest.org.") ) {
                String[] values = record.getValues();
                    
                if( values != null && values.length == 1 && values[0].equals("174.129.196.233") ) {
                    recordToRemove = record;
                    break;
                }
            }
                
        }
        catch( Throwable ignore ) {
            // ignore
        }
        assertNull("Record still exists in zone", recordToRemove);
        end();
    }
    
    @Test
    public void testRemoveZone() throws CloudException, InternalException {
        begin();
        DNSSupport support = cloud.getNetworkServices().getDnsSupport();

        support.deleteDnsZone(zoneToRemove);
        try { Thread.sleep(5000L); }
        catch( InterruptedException e ) { }
        DNSZone z = support.getDnsZone(zoneToRemove);
        
        assertNull("The zone in " + zoneToRemove + " still exists.", z);
        zoneToRemove = null;
        end();
    }
}
