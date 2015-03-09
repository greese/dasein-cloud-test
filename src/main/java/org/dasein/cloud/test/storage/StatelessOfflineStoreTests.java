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
import org.dasein.cloud.InternalException;
import org.dasein.cloud.storage.Blob;
import org.dasein.cloud.storage.OfflineStoreRequest;
import org.dasein.cloud.storage.OfflineStoreRequestAction;
import org.dasein.cloud.storage.OfflineStoreRequestStatus;
import org.dasein.cloud.storage.OfflineStoreSupport;
import org.dasein.cloud.storage.StorageServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Locale;

import static junit.framework.Assert.*;
import static org.junit.Assume.assumeTrue;

public class StatelessOfflineStoreTests {

    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessOfflineStoreTests.class);
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
    public void checkMetaData() throws CloudException, InternalException {
        OfflineStoreSupport support = getSupportOrBail();
        if (support == null) {
            return;
        }

        tm.out("Subscribed", support.isSubscribed());
        tm.out("Term for Bucket", support.getProviderTermForBucket(Locale.getDefault()));
        tm.out("Term for Object", support.getProviderTermForObject(Locale.getDefault()));
        tm.out("Bucket Naming Rules", support.getBucketNameRules());
        tm.out("Object Naming Rules", support.getObjectNameRules());
        tm.out("Public Bucket Sharing", support.allowsPublicSharing());
        tm.out("Root Objects", support.allowsRootObjects());
        tm.out("Nested Buckets", support.allowsNestedBuckets());
        tm.out("Max Buckets", support.getMaxBuckets());
        tm.out("Max Object Size", support.getMaxObjectSize());
        tm.out("Max Objects/Bucket", support.getMaxObjectsPerBucket());

        assertNotNull("The provider term for a bucket may not be null in any locale", support.getProviderTermForBucket(Locale.getDefault()));
        assertNotNull("The provider term for an object may not be null in any locale", support.getProviderTermForObject(Locale.getDefault()));
        assertNotNull("The bucket naming rules may not be null", support.getBucketNameRules());
        assertNotNull("The object naming rules may not be null", support.getObjectNameRules());
        assertTrue("The maximum number of buckets must be -2, -1, or non-negative", support.getMaxBuckets() >= -2);
        assertTrue("The maximum number of objects per bucket must be -2, -1, or non-negative", support.getMaxObjectsPerBucket() >= -2);
    }

    @Test
    public void listBuckets() throws Exception {
        OfflineStoreSupport offlineStore = getSupportOrBail();
        if (offlineStore == null) {
            return;
        }

        Iterable<Blob> buckets = offlineStore.list(null);
        assertNotNull(buckets);

        for (Blob bucket : buckets) {
            assertNotNull(bucket);
            assertNotNull(bucket.getBucketName());
            assertNotNull(bucket.getLocation());
            assertNull(bucket.getObjectName());
        }
    }

    @Test
    public void listRequests()  throws Exception {
        OfflineStoreSupport offlineStore = getSupportOrBail();
        if (offlineStore == null) {
            return;
        }

        boolean listedSomething = false;
        // look for any existing list requests that are complete. we can piggyback
        // on these and test the result parsing
        for (Blob bucketBlob : offlineStore.list(null)) {
            String bucket = bucketBlob.getBucketName();

            for (OfflineStoreRequest request : offlineStore.listRequests(bucket)) {

                assertEquals(bucket, request.getBucketName());
                assertNotNull(request.getRequestId());
                assertNotNull(request.getStatus());

                if (request.getAction() == OfflineStoreRequestAction.LIST) {
                    if (request.getStatus() == OfflineStoreRequestStatus.SUCCEEDED) {
                        Iterable<Blob> listRequestResult = offlineStore.getListRequestResult(
                                bucket, request.getRequestId());
                        tm.out("Got results of list request " + request.getRequestId());
                        for (Blob blob : listRequestResult) {
                            if (blob != null) {
                                tm.out("Found blob in offline storage list for bucket " +
                                        bucket + ": " + blob + " (size: " + blob.getSize() + ")");
                                // since we don't know anything about what we've just listed,
                                // all we can do is assert that the resulting Blob object is
                                // well-formed
                                assertNotNull(blob.getObjectName());
                                assertNotNull(blob.getBucketName());
                                assertNotNull(blob.getSize());
                            }
                        }
                        // no guarantee there are any blobs in the archive..
                        assertNotNull(listRequestResult);
                        listedSomething = true;
                    }
                }
            }
        }
        if (!listedSomething) {
            tm.warn("There were no existing list requests to read output from!");
        }
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
