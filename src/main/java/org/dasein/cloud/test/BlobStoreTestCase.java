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

import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.storage.Blob;
import org.dasein.cloud.storage.BlobStoreSupport;
import org.dasein.cloud.storage.FileTransfer;
import org.dasein.cloud.storage.StorageServices;
import org.dasein.util.CalendarWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BlobStoreTestCase extends BaseTestCase {
    private CloudProvider cloud             = null;
    private String        bucketToRemove    = null;
    private String        testBucketName    = null;
    private String        testObjectName    = null;
    private File          testLocalFile     = null;
    
    public BlobStoreTestCase(String name) { super(name); }

    private void download(String directory, String object) throws Exception {
        File targetFile = new File("dsn" + System.currentTimeMillis() + ".txt");
            
        if( targetFile.exists() ) {
            targetFile.delete();
        }
        try {
            FileTransfer task;

            task = getSupport().download(directory, object, targetFile);
            while( !task.isComplete() ) {
                try { Thread.sleep(1000L); }
                catch( InterruptedException e ) { }
            }
            if( task.getTransferError() != null ) {
                throw new RuntimeException(task.getTransferError());
            }
            assertTrue("File was corrupted", checkTestFile(targetFile));
        }
        finally {
            if( targetFile.exists() ) {
                targetFile.delete();
            }
        }
    }
    
    private void list(String directory, String ... expected) throws CloudException, InternalException {
        int expectedFound = 0;
        
        if( expected == null ) {
            expected = new String[0];
        }
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 2L);
        
        while( System.currentTimeMillis() < timeout ) {
            for( Blob item : getSupport().list(directory) ) {
                out(" " + item.toString());
                for( String name : expected ) {
                    if( (item.isContainer() && name.equals(item.getBucketName())) || (!item.isContainer() &&name.equals(item.getObjectName())) ) {
                        expectedFound++;
                        break;
                    }
                }
            }
            if( expectedFound == expected.length ) {
                return;
            }
        }
        assertTrue("Expected files were not found (" + expected.length + " vs " + expectedFound + ")", expectedFound == expected.length);
    }
    
    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        File dir = new File("target/tmp");

        if( !dir.exists() ) {
            dir.mkdirs();
        }
        String name = getName();

        cloud = getProvider();
        cloud.connect(getTestContext());
        if( name.equals("testClearBucket") || name.equals("testDownloadObject") ||  name.equals("testListBuckets") || name.equals("testListBucket") || name.equals("testRemoveBucket") || name.equals("testRemoveObject") || name.equals("testUploadObject") || name.equals("testGetBucket") || name.equals("testGetObject") || name.equals("testGetBogusObject") ) {
            bucketToRemove = getSupport().createBucket("dsnccd" + System.currentTimeMillis(), true).getBucketName();
            testBucketName = bucketToRemove;
        }
        if( name.equals("testDownloadObject") || name.equals("testUploadObject") || name.equals("testListBucket") || name.equals("testRemoveObject") || name.equals("testGetObject") ) {
            testObjectName = "dsnroot" + System.currentTimeMillis() + ".txt";
            testLocalFile = createTestFile();
        }
        if( name.equals("testDownloadObject") || name.equals("testListBucket") || name.equals("testRemoveObject") || name.equals("testGetObject") ) {
            getSupport().upload(testLocalFile, testBucketName, testObjectName);
        }
    }
    
    @After
    @Override
    public void tearDown() {
        try {
            if( bucketToRemove != null ) {
                getSupport().clearBucket(bucketToRemove);
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( cloud != null ) {
                cloud.close();
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( testLocalFile != null ) {
                testLocalFile.delete();
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
    }

    private @Nonnull BlobStoreSupport getSupport() throws InternalException {
        StorageServices services = cloud.getStorageServices();

        if( services == null ) {
            throw new InternalException("Cloud has no storage services");
        }
        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            throw new InternalException("Cloud has no blob store support");
        }
        return support;
    }


    @Test
    public void testSubscription() throws CloudException, InternalException {
        begin();
        try {
            out("Subscribed: " + getSupport().isSubscribed());
        }
        finally {
            end();
        }
    }

    @Test
    public void testMetaData() throws CloudException, InternalException {
        begin();
        try {
            BlobStoreSupport support = getSupport();

            out("Max object size:       " + support.getMaxObjectSize());
            out("Max buckets:           " + support.getMaxBuckets());
            out("Max objects/bucket:    " + support.getMaxObjectsPerBucket());
            out("Bucket term:           " + support.getProviderTermForBucket(Locale.getDefault()));
            out("Object term:           " + support.getProviderTermForObject(Locale.getDefault()));
            out("Nested buckets:        " + support.allowsNestedBuckets());
            out("Root objects:          " + support.allowsRootObjects());
            out("Allows public sharing: " + support.allowsPublicSharing());
            out("Name rules:            " + support.getBucketNameRules());
            assertNotNull("Bucket term may not be null", support.getProviderTermForBucket(Locale.getDefault()));
            assertNotNull("Object term may not be null", support.getProviderTermForObject(Locale.getDefault()));
            assertNotNull("File size cannot be null", support.getMaxObjectSize());
            assertTrue("Max file size must be positive", support.getMaxObjectSize().getQuantity().longValue() > 0);
            assertNotNull("Name rule may not be null", support.getBucketNameRules());
        }
        finally {
            end();
        }
    }

    @Test
    public void testClearBucket() throws CloudException, InternalException {
        begin();
        try {
            out("Exists before: " + getSupport().exists(testBucketName));
            getSupport().clearBucket(testBucketName);
            out("Exists after:  " + getSupport().exists(testBucketName));
            assertTrue("Did not clear directory", !getSupport().exists(testBucketName));
        }
        finally {
            end();
        }
    }

    @Test
    public void testListBuckets() throws CloudException, InternalException {
        begin();
        try {
            list(null, testBucketName);
        }
        finally {
            end();
        }
    }

    @Test
    public void testCreateBucket() throws InternalException, CloudException {
        begin();
        try {
            testBucketName = "dsncrd" + System.currentTimeMillis();

            while( getSupport().exists(testBucketName) ) {
                testBucketName = "dsncrd" + System.currentTimeMillis();
                try { Thread.sleep(500L); }
                catch( InterruptedException ignore ) { }
            }
            Blob bucket = getSupport().createBucket(testBucketName, false);

            out("Created: " + bucket);
            assertNotNull("No directory was created", bucket);
            testBucketName = bucket.getBucketName();
            bucketToRemove = testBucketName;
            assertNotNull("Bucket has no name", bucket.getBucketName());
            assertNull("Bucket has an object", bucket.getObjectName());
            assertDirectoryExists("Bucket " + testBucketName + " was not created", cloud, testBucketName);
        }
        finally {
            end();
        }
    }

    @Test
    public void testUploadObject() throws InternalException, CloudException {
        begin();
        try {
            String fileName = "child-test.txt";

            getSupport().upload(testLocalFile, testBucketName, fileName);
            assertNotNull("Object does not exist in cloud", getSupport().getObjectSize(testBucketName, fileName));
        }
        finally {
            end();
        }
    }

    @Test
    public void testDownloadObject() throws Exception {
        begin();
        try {
            download(testBucketName, testObjectName);
        }
        finally {
            end();
        }
    }

    @Test
    public void testGetBogusBucket() throws CloudException, InternalException {
        begin();
        try {
            Blob bucket = getSupport().getBucket(UUID.randomUUID().toString());

            out("Found: " + bucket);
            assertNull("Found a bogus bucket: " + bucket, bucket);
        }
        finally {
            end();
        }
    }

    @Test
    public void testGetBucket() throws CloudException, InternalException {
        begin();
        try {
            Blob bucket = getSupport().getBucket(testBucketName);

            out("Bucket:     " + bucket);
            assertNotNull("Bucket was not found", bucket);
            out("Name:           " + bucket.getBucketName());
            out("Region:         " + bucket.getProviderRegionId());
            out("Object:         " + bucket.getObjectName());
            out("Size:           " + bucket.getSize());
            out("Created:        " + (new Date(bucket.getCreationTimestamp())));
            out("Location:       " + bucket.getLocation());
            out("Container:      " + bucket.isContainer());
            assertNotNull("Bucket name is null", bucket.getBucketName());
            assertNull("Object name should be null", bucket.getObjectName());
            assertNotNull("Region ID was null", bucket.getProviderRegionId());
            assertNotNull("Location was null", bucket.getLocation());
        }
        finally {
            end();
        }
    }

    @Test
    public void testRemoveBucket() throws CloudException, InternalException {
        begin();
        try {
            getSupport().removeBucket(testBucketName);
            assertTrue("Bucket " + testBucketName + " still exists", !getSupport().exists(testBucketName));
            testBucketName = null;
            bucketToRemove = null;
        }
        finally {
            end();
        }
    }

    @Test
    public void testGetBogusObject() throws CloudException, InternalException {
        begin();
        try {
            Blob object = getSupport().getObject(testBucketName, UUID.randomUUID().toString());

            out("Found: " + object);
            assertNull("Found a bogus object: " + object, object);
        }
        finally {
            end();
        }
    }

    @Test
    public void testListBucket() throws CloudException, InternalException {
        begin();
        try {
            list(testBucketName, testObjectName);
        }
        finally {
            end();
        }
    }

    @Test
    public void testGetObject() throws CloudException, InternalException {
        begin();
        try {
            Blob object = getSupport().getObject(testBucketName, testObjectName);

            out("Object:         " + object);
            assertNotNull("Object was not found", object);
            out("Name:           " + object.getObjectName());
            out("Bucket:         " + object.getBucketName());
            out("Region:         " + object.getProviderRegionId());
            out("Size:           " + object.getSize());
            out("Created:        " + (new Date(object.getCreationTimestamp())));
            out("Location:       " + object.getLocation());
            out("Container:      " + object.isContainer());
            assertNotNull("Object name should not be null", object.getObjectName());
            assertNotNull("Region ID was null", object.getProviderRegionId());
            assertNotNull("Location was null", object.getLocation());
            assertNotNull("Object size should be non-null", object.getSize());
            assertEquals("Object names do not match", testObjectName, object.getObjectName());
        }
        finally {
            end();
        }
    }

    @Test
    public void testRemoveObject() throws CloudException, InternalException {
        begin();
        try {
            getSupport().removeObject(testBucketName, testObjectName);
            assertNull("Object /" + testBucketName + "/" + testObjectName + " still exists", getSupport().getObjectSize(testBucketName, testObjectName));
            testObjectName = null;
        }
        finally {
            end();
        }
    }
}
