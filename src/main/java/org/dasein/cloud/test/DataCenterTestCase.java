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
import java.util.Collection;
import java.util.TreeSet;
import java.util.UUID;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.util.APITrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DataCenterTestCase extends BaseTestCase {
    
    private CloudProvider provider         = null;
    private String        testDataCenterId = null;
    private String        testRegionId     = null;
    
    public DataCenterTestCase(String name) { super(name); } 
    
    @Before
    @Override
    public void setUp() throws CloudException, InternalException, InstantiationException, IllegalAccessException, IOException {
        provider = getProvider();
        provider.connect(getTestContext());
        if( getName().equals("testGetRegion") ) {
            testRegionId = provider.getDataCenterServices().listRegions().iterator().next().getProviderRegionId();
        }
        else if( getName().equals("testGetBogusRegion") ) {
            testRegionId = UUID.randomUUID().toString();
        }
        else if( !getName().equals("testRegionContent") ) {
            testRegionId = provider.getContext().getRegionId();
        }
        if( getName().equals("testGetBogusDataCenter") ) {
            testDataCenterId = UUID.randomUUID().toString();            
        }
        else if( getName().equals("testGetDataCenter") ) {
            testDataCenterId = provider.getDataCenterServices().listDataCenters(provider.getContext().getRegionId()).iterator().next().getProviderDataCenterId();
        }
    }
    
    @After
    @Override
    public void tearDown() {
        APITrace.report(getName());
        APITrace.reset();
        if( provider != null ) {
            provider.close();
        }
    }
    
    @Test 
    public void testGetBogusDataCenter() throws CloudException, InternalException {
        begin();
        DataCenter dc = provider.getDataCenterServices().getDataCenter(testDataCenterId);
        
        assertNull("Found matching data center for fake ID: " + testDataCenterId, dc);
        end();
    }
    
    @Test
    public void testGetBogusRegion() throws CloudException, InternalException {
        begin();
        Region region = provider.getDataCenterServices().getRegion(testRegionId);
        
        assertNull("Found matching region for fake ID: " + testRegionId, region);
        end();
    }
    
    @Test
    public void testGetDataCenter() throws CloudException, InternalException {
        begin();
        DataCenter dc = provider.getDataCenterServices().getDataCenter(testDataCenterId);
        
        assertNotNull("No such data center: " + testDataCenterId, dc);
        assertEquals("Data Center IDs do not match: " + testDataCenterId + " vs. " + dc.getProviderDataCenterId(), testDataCenterId, dc.getProviderDataCenterId());
        end();
    }
    
    @Test
    public void testGetRegion() throws CloudException, InternalException {
        begin();
        Region region = provider.getDataCenterServices().getRegion(testRegionId);
        
        assertNotNull("No such region: " + testRegionId, region);
        assertEquals("Region IDs do not match: " + testRegionId + " vs. " + region.getProviderRegionId(), testRegionId, region.getProviderRegionId());
        end();
    }
    
    @Test 
    public void testListDataCenters() throws CloudException, InternalException {
        begin();
        Collection<DataCenter> dataCenters = provider.getDataCenterServices().listDataCenters(testRegionId);
        
        assertNotNull("listDataCenters() must return a value", dataCenters);
        assertTrue("Each region must have at least one data center", dataCenters.size() > 0);
        try {
            for( DataCenter dc : dataCenters ) {
                out("Data Center: " + dc);
            }
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }
    
    @Test
    public void testListRegions() throws CloudException, InternalException {
        begin();
        Collection<Region> regions = provider.getDataCenterServices().listRegions();

        assertNotNull("listRegions() must return a value", regions);
        assertTrue("No regions identified in target cloud", regions.size() > 0);
        try {
            for( Region region : regions ) {
                out("Region: " + region);
            }
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }
    
    @Test
    public void testDataCenterContent() throws CloudException, InternalException {
        begin();
        TreeSet<String> ids = new TreeSet<String>();
        boolean example = true;
        
        for( DataCenter dc : provider.getDataCenterServices().listDataCenters(testRegionId) ) {
            String dataCenterId = dc.getProviderDataCenterId();
            
            assertNotNull("Provider data center ID must be non-null", dataCenterId);
            assertTrue("Provider data center ID must be unique for a given region", !ids.contains(dataCenterId));
            assertNotNull("Provider data center name must be non-null", dc.getName());
            assertEquals("Provider region should match listing request", testRegionId, dc.getRegionId());
            try {
                if( example ) {
                    out("Data CenterID:     " + dc.getProviderDataCenterId());
                    out("Name:              " + dc.getName());
                    out("Region ID:         " + dc.getRegionId());
                    example = false;
                }
            }
            catch( Throwable notPartOfTest ) {
                // ignore
            }
        }
        end();
    }
    
    @Test
    public void testRegionContent() throws CloudException, InternalException {
        begin();
        TreeSet<String> ids = new TreeSet<String>();
        boolean example = true;
        
        for( Region region : provider.getDataCenterServices().listRegions() ) {
            String providerId = region.getProviderRegionId();
            
            assertNotNull("Provider region ID must be non-null", providerId);
            assertTrue("Provider region ID must be unique for a given account", !ids.contains(providerId));
            assertNotNull("Provider region name must be non-null", region.getName());
            ids.add(providerId);
            try {
                if( example ) {
                    out("Region ID:    " + region.getProviderRegionId());
                    out("Name:         " + region.getName());
                    //out("Jurisdiction: " + region.getJurisdiction());
                    example = false;
                }
            }
            catch( Throwable notPartOfTest ) {
                // ignore
            }
        }
        end();
    }
}
