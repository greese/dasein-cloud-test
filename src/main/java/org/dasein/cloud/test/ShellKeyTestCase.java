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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.identity.SSHKeypair;
import org.dasein.cloud.util.APITrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ShellKeyTestCase extends BaseTestCase {
    private CloudProvider cloud        = null;
    private String        keyToDelete  = null;
    
    public ShellKeyTestCase(String name) { super(name); }
    
    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        String name = getName();
        
        cloud = getProvider();
        cloud.connect(getTestContext());
        if( name.equals("testDeleteKey") || name.equals("testKeyContent") ) {
            keyToDelete = "dsn" + System.currentTimeMillis();            
            cloud.getIdentityServices().getShellKeySupport().createKeypair(keyToDelete);
        }
    }
    
    @After
    @Override
    public void tearDown() {
        try {
            if( keyToDelete != null ) {
                cloud.getIdentityServices().getShellKeySupport().deleteKeypair(keyToDelete);
                keyToDelete = null;
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
    public void testCreateKey() throws InternalException, CloudException {
        begin();
        keyToDelete = "dsncreate" + System.currentTimeMillis();
        SSHKeypair pk = cloud.getIdentityServices().getShellKeySupport().createKeypair(keyToDelete);
        assertNotNull("No private key for new key", pk);
        out("ID:          " + pk.getProviderKeypairId());
        out("Owner ID:    " + pk.getProviderOwnerId());
        out("Region ID:   " + pk.getProviderRegionId());
        out("Fingerprint: " + pk.getFingerprint());
        out("Name:        " + pk.getName());
        out("Private: \n" + pk.getPrivateKey());
        assertNotNull("No ID for new key", pk.getProviderKeypairId());
        assertNotNull("Must have a private key value on create", pk.getPrivateKey());
        assertNotNull("Must have an owner ID", pk.getProviderOwnerId());
        assertNotNull("Must have a region ID", pk.getProviderRegionId());
        assertNotNull("Must have a fingerprint", pk.getFingerprint());
        assertNotNull("Must have a name", pk.getName());
        end();
    }
    
    @Test
    public void testDeleteKey() throws InternalException, CloudException {
        begin();
        cloud.getIdentityServices().getShellKeySupport().deleteKeypair(keyToDelete);
        assertNull("Keypair still exists", cloud.getIdentityServices().getShellKeySupport().getKeypair(keyToDelete));
        keyToDelete = null;
        end();
    }
    
    @Test
    public void testKeyContent() throws InternalException, CloudException {
        begin();
        String fingerprint = cloud.getIdentityServices().getShellKeySupport().getFingerprint(keyToDelete);

        assertNotNull("No key fingerprint was found", fingerprint);
        end();
    }
    
    @Test
    public void testListKeys() throws InternalException, CloudException {
        begin();
        Iterable<SSHKeypair> keys = cloud.getIdentityServices().getShellKeySupport().list();
        
        assertNotNull("Keys must not be null, but can be empty", keys);
        try {
            for( SSHKeypair key : keys ) {
                out("Key: " + key);
            }
        }
        catch( Throwable notPartOfTest ) {
            // ignore
        }
        end();
    }
    
    @Test
    public void testMetaData() {
        begin();
        end();
    }
    
    @Test
    public void testSubscription() {
        begin();
        end();
    }
}
