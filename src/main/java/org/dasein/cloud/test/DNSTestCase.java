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
