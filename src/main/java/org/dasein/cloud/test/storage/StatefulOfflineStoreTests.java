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

package org.dasein.cloud.test.storage;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.storage.Blob;
import org.dasein.cloud.storage.OfflineStoreRequest;
import org.dasein.cloud.storage.OfflineStoreRequestAction;
import org.dasein.cloud.storage.OfflineStoreRequestStatus;
import org.dasein.cloud.storage.OfflineStoreSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.dasein.cloud.storage.StorageServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.rules.TestName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class StatefulOfflineStoreTests {

    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulOfflineStoreTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void buckets() throws Exception {
        OfflineStoreSupport offlineStore = getSupportOrBail();
        if (offlineStore == null) {
            return;
        }

        List<String> createdBuckets = new ArrayList<String>();

        String bucketPrefix = "testbkt" + UUID.randomUUID().toString();

        try {
            String bucket1 = bucketPrefix + "_1";
            createdBuckets.add(bucket1);
            Blob bucketBlob1 = offlineStore.createBucket(bucket1, false);

            String bucket2 = bucketPrefix + "_2";
            createdBuckets.add(bucket2);
            Blob bucketBlob2 = offlineStore.createBucket(bucket2, false);

            String bucket3 = bucketPrefix + "_3";
            createdBuckets.add(bucket3);
            Blob bucketBlob3 = offlineStore.createBucket(bucket3, false);

            assertTrue(offlineStore.exists(bucket1));
            assertFalse(offlineStore.exists(bucketPrefix + "NOTTHISONE"));

            assertBlobEquals(bucketBlob1, offlineStore.getBucket(bucket1));
            assertBlobEquals(bucketBlob2, offlineStore.getBucket(bucket2));
            assertBlobEquals(bucketBlob3, offlineStore.getBucket(bucket3));

            Map<String, Blob> blobs = new HashMap<String, Blob>();
            for (Blob blob : offlineStore.list(null)) {
                assertNotNull(blob.getBucketName());
                if (blob.getBucketName().startsWith(bucketPrefix)) {
                    assertFalse(blobs.containsKey(blob.getBucketName()));
                    blobs.put(blob.getBucketName(), blob);
                }
            }
            assertEquals(3, blobs.size());
            assertBlobEquals(bucketBlob1, blobs.get(bucket1));
            assertBlobEquals(bucketBlob2, blobs.get(bucket2));
            assertBlobEquals(bucketBlob3, blobs.get(bucket3));

        } finally {
            for (String bucket : createdBuckets) {
                offlineStore.removeBucket(bucket);
            }
        }
    }

    @Test
    public void requests() throws Exception {
        OfflineStoreSupport offlineStore = getSupportOrBail();
        if (offlineStore == null) {
            return;
        }

        /* Glacier does not allow listing (inventorying) recently-created vaults.
           They cannot be listed for up to 24 hours after they are created. So in this
           test we cheat and look for any existing vaults that we can list.
         */
        for (Blob bucketBlob : offlineStore.list(null)) {
            String bucket = bucketBlob.getBucketName();
            try {
                OfflineStoreRequest listRequest = offlineStore.createListRequest(bucket);
                assertListRequest(bucket, listRequest);

                OfflineStoreRequest gotRequest = offlineStore.getRequest(bucket, listRequest.getRequestId());
                assertListRequest(bucket, gotRequest);

                // since we know this bucket is inventoried, we can take this opportunity
                // to assert that it has an object size.
                assertNotNull(bucketBlob.getSize());
                long size = bucketBlob.getSize().longValue();
                assertTrue(size >= 0);

                tm.out("Successfully made list request for bucket " + bucket + " (size: " + size + ")");

            }
            catch (CloudException e) {
                assertEquals(404, e.getHttpCode());
                tm.out("Couldn't make list request for bucket (too new): " + bucket);
            }
        }

        // also try making a bucket and listing it, we should get a recognizable error
        String newBucket = "testbkt" + UUID.randomUUID().toString();
        boolean failed = false;
        try {
            offlineStore.createBucket(newBucket, false);

            // should fail
            offlineStore.createListRequest(newBucket);
        } catch (CloudException e) {
            if (e.getHttpCode() == 404) {
                failed = true;
            }
        } finally {
            offlineStore.removeBucket(newBucket);
        }
        assertTrue("Expected list request creation to fail on a new bucket", failed);
    }

    private static void assertListRequest(String bucket, OfflineStoreRequest listRequest) {
        assertEquals(bucket, listRequest.getBucketName());
        assertNull(listRequest.getObjectName());
        assertNotNull(listRequest.getRequestId());
        assertEquals(OfflineStoreRequestStatus.IN_PROGRESS, listRequest.getStatus());
        assertEquals(OfflineStoreRequestAction.LIST, listRequest.getAction());
    }

    private static void assertBlobEquals(Blob expected, Blob actual) {
        assertEquals(expected.getBucketName(), actual.getBucketName());
        assertEquals(expected.getLocation(), actual.getLocation());
    }

    private OfflineStoreSupport getSupportOrBail() {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return null;
        }

        OfflineStoreSupport support = services.getOfflineStorageSupport();

        if( support == null ) {
            tm.ok("No offline storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return null;
        }
        return support;
    }
}
