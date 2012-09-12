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
import org.dasein.cloud.platform.CDNSupport;
import org.dasein.cloud.platform.Distribution;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CDNTestCase extends BaseTestCase {
    private String        distributionToDelete = null;
    private CloudProvider provider             = null;
    private String        testDirectory        = null;
    private String        testDistributionId   = null;

    public CDNTestCase(String name) { super(name); } 
    
    @Before
    public void setUp() throws InternalException, CloudException, InstantiationException, IllegalAccessException {
        String name = getName();
        
        provider = getProvider();
        provider.connect(getTestContext());
        if( name.equals("testCreateDistribution") ) {
            testDirectory = provider.getStorageServices().getBlobStoreSupport().createBucket("dsncdn" + System.currentTimeMillis(), true).getBucketName();
        }
        else if( name.equals("testGetDistribution") ) {
            for( Distribution d : provider.getPlatformServices().getCDNSupport().list() ) {
                if( d.isActive() ) {
                    testDistributionId = d.getProviderDistributionId();
                }
            }
            if( testDistributionId == null ) {
                String dir = provider.getStorageServices().getBlobStoreSupport().createBucket("dsnbk" + System.currentTimeMillis(), true).getBucketName();
                
                testDistributionId = provider.getPlatformServices().getCDNSupport().create(dir, "Get CDN Test", true, System.currentTimeMillis() + "a.dasein.org");
                distributionToDelete = testDistributionId;
            }
        }
        else if( name.equals("testDistributionContent") ) {
            for( Distribution d : provider.getPlatformServices().getCDNSupport().list() ) {
                if( d.isActive() ) {
                    testDistributionId = d.getProviderDistributionId();
                }
            }
            if( testDistributionId == null ) {
                String dir = provider.getStorageServices().getBlobStoreSupport().createBucket("dsnbk" + System.currentTimeMillis(), true).getBucketName();
                
                testDistributionId = provider.getPlatformServices().getCDNSupport().create(dir, "CDN Test Content", true, System.currentTimeMillis() + "b.dasein.org");
                distributionToDelete = testDistributionId;
            }            
        }
        else if( name.equals("testUpdateDistribution") ) {
            String dir = provider.getStorageServices().getBlobStoreSupport().createBucket("dsnbk" + System.currentTimeMillis(), true).getBucketName();
            
            testDistributionId = provider.getPlatformServices().getCDNSupport().create(dir, "CDN Test Update", true, System.currentTimeMillis() + "c.dasein.org");
            distributionToDelete = testDistributionId;   
        }
        else if( name.equals("testDeleteDistribution") ) {
            String dir = provider.getStorageServices().getBlobStoreSupport().createBucket("dsnbk" + System.currentTimeMillis(), true).getBucketName();
            
            testDistributionId = provider.getPlatformServices().getCDNSupport().create(dir, "CDN Test Delete", true, System.currentTimeMillis() + "d.dasein.org");
            distributionToDelete = testDistributionId;   
        }   
    }
    
    @After
    public void tearDown() {
        try {
            if( distributionToDelete != null ) {
                provider.getPlatformServices().getCDNSupport().delete(distributionToDelete);
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
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
    public void testSubscription() throws CloudException, InternalException {
        begin();
        CDNSupport services = provider.getPlatformServices().getCDNSupport();

        assertTrue("Account must be subscribed in order to run unit tests", services.isSubscribed());
        end();
    }

    @Test
    public void testMetaData() throws CloudException, InternalException {
        begin();
        CDNSupport services = provider.getPlatformServices().getCDNSupport();

        assertNotNull("Must provider a provider name", services.getProviderTermForDistribution(Locale.getDefault()));
        end();
    }
    
    @Test
    public void testCreateDistribution() throws InternalException, CloudException {
        begin();
        String id = provider.getPlatformServices().getCDNSupport().create(testDirectory, "CDN Test", true, System.currentTimeMillis() + ".dasein.org");
        
        assertNotNull("No distribution was created", id);
        assertNotNull("No distribution was created", provider.getPlatformServices().getCDNSupport().getDistribution(id));
        end();
    }
    
    @Test
    public void testDistributionContent() throws CloudException, InternalException {
        begin();
        Distribution d = provider.getPlatformServices().getCDNSupport().getDistribution(testDistributionId);
        
        assertNotNull("No matching distribution", d);
        assertNotNull("No name exists for distribution", d.getName());
        assertEquals("Distribution ID does not match", testDistributionId, d.getProviderDistributionId());
        end();
    }
    
    @Test
    public void testGetBogusDistribution() throws CloudException, InternalException {
        begin();
        Distribution d = provider.getPlatformServices().getCDNSupport().getDistribution(UUID.randomUUID().toString());
        
        assertNull("Distribution was incorrectly found", d);
        end();
    }
    
    @Test
    public void testGetDistribution() throws CloudException, InternalException {
        begin();
        Distribution d = provider.getPlatformServices().getCDNSupport().getDistribution(testDistributionId);
        
        assertNotNull("Distribution was not found", d);
        end();
    }
    
    @Test
    public void testListDistributions() throws CloudException, InternalException {
        begin();
        assertNotNull("No distributions were provided", provider.getPlatformServices().getCDNSupport().list());
        end();
    }
}
