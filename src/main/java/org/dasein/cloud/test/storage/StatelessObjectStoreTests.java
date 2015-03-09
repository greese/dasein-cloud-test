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
import org.dasein.cloud.storage.BlobStoreSupport;
import org.dasein.cloud.storage.StorageServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests that validate the stateless functionality of a Dasein Cloud implementation against an object storage system.
 * <p>Created by George Reese: 3/3/13 4:57 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatelessObjectStoreTests {
    static private final Random random = new Random();

    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessObjectStoreTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private Blob testChildBucket;
    private Blob testChildObject;
    private Blob testRootBucket;
    private Blob testRootObject;

    public StatelessObjectStoreTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testRootBucket = tm.getTestBucket(DaseinTestManager.STATELESS, true, false);
        testChildBucket = tm.getTestBucket(DaseinTestManager.STATELESS, false, false);
        testRootObject = tm.getTestObject(DaseinTestManager.STATELESS, true, false);
        testChildObject = tm.getTestObject(DaseinTestManager.STATELESS, false, false);
        if( testChildObject != null && name.getMethodName().equals("childObjectContent") ) {
            StorageServices services = tm.getProvider().getStorageServices();

            if( services != null ) {
                BlobStoreSupport support = services.getOnlineStorageSupport();

                if( support != null ) {
                    try {
                        //noinspection ConstantConditions
                        testRootBucket = support.getBucket(testChildObject.getBucketName());
                    }
                    catch( Throwable ignore ) {
                        // ignore
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
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
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

    private void assertBlob(@Nonnull BlobStoreSupport support, @Nonnull Blob item, @Nullable Blob parent, @Nullable Boolean container) throws CloudException, InternalException {
        if( container != null ) {
            assertEquals("The value for isContainer() does not match", container, item.isContainer());
        }
        if( item.isContainer() ) {
            assertNull("Object name must be null for buckets", item.getObjectName());
            assertNotNull("The bucket name may not be null for buckets", item.getBucketName());
        }
        else {
            assertNotNull("The object name may not be null for objects", item.getObjectName());
        }
        if( parent == null ) {
            if( !item.isContainer() ) {
                assertTrue("This cloud does not support root objects, but a root object was found", support.allowsRootObjects());
                assertNull("The parent bucket must be null for root objects", item.getBucketName());
            }
        }
        else if( item.isContainer() ) {
            assertTrue("This cloud does not support nested buckets, but a nested bucket was found in " + parent.getBucketName(), support.allowsNestedBuckets());
        }
        assertTrue("The object creation date must be non-negative", item.getCreationTimestamp() >= 0L);
        assertNotNull("The region ID may not be null", item.getProviderRegionId());
        assertEquals("The region ID must match the current context", tm.getContext().getRegionId(), item.getProviderRegionId());
        assertNotNull("The object/bucket location may not be null", item.getLocation());
        if( container != null && container ) {
            assertNull("Containers do not have a size", item.getSize());
        }
        else if( container != null ) {
            assertNotNull("Objects must have a size", item.getSize());
        }
    }

    private void out(Blob blob) {
        tm.out("Bucket", blob.getBucketName());
        tm.out("Object", blob.getObjectName());
        tm.out("Created", new Date(blob.getCreationTimestamp()));
        tm.out("Region ID", blob.getProviderRegionId());
        tm.out("Container", blob.isContainer());
        tm.out("Location", blob.getLocation());
        tm.out("Size", blob.getSize());
    }

    @Test
    public void listItemsUnderRoot() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Iterable<Blob> items = support.list(null);
        int count = 0;

        assertNotNull("Items returned may not be null", items);
        for( Blob item : items ) {
            count++;
            tm.out("Item", item);
        }
        tm.out("Total Root Items", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
                tm.ok("Account is not subscribed for object storage, so this test may not be not valid");
            }
            else {
                tm.warn("No items were returned, so it is impossible to verify the validity of this test");
            }
        }
        else {
            for( Blob item : items ) {
                assertBlob(support, item, null, null);
            }
        }
    }

    @Test
    public void getBogusRootBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Blob bucket = support.getBucket(UUID.randomUUID().toString().replaceAll("-", String.valueOf(random.nextInt(9))).substring(0, 15));

        tm.out("Bogus Bucket", bucket);
        assertNull("Fetched an actual bucket for the bogus ID requested", bucket);
    }

    @Test
    public void getRootBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testRootBucket != null ) {
            @SuppressWarnings("ConstantConditions") Blob bucket = support.getBucket(testRootBucket.getBucketName());

            tm.out("Bucket", bucket);
            assertNotNull("The test bucket does not exist in the target cloud", bucket);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void rootBucketContent() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testRootBucket != null ) {
            @SuppressWarnings("ConstantConditions") Blob bucket = support.getBucket(testRootBucket.getBucketName());

            assertNotNull("The test bucket does not exist in the target cloud", bucket);

            out(bucket);
            assertBlob(support, bucket, null, true);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void existsBogusBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testRootBucket != null ) {
            @SuppressWarnings("ConstantConditions") boolean exists = support.exists(UUID.randomUUID().toString());

            tm.out("Exists", exists);
            assertFalse("The bucket does exists even though it does not exist", exists);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void existsBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testRootBucket != null ) {
            @SuppressWarnings("ConstantConditions") boolean exists = support.exists(testRootBucket.getBucketName());

            tm.out("Exists", exists);
            assertTrue("The bucket does not exist even though it exists", exists);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void listItemsUnderRootBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testRootBucket != null ) {
            Iterable<Blob> items = support.list(testRootBucket.getBucketName());
            int count = 0;

            assertNotNull("Items returned may not be null", items);
            for( Blob item : items ) {
                count++;
                tm.out("Item", item);
            }
            tm.out("Total Items in " + testRootBucket.getBucketName(), count);
            if( count < 1 ) {
                if( !support.isSubscribed() ) {
                    tm.ok("Account is not subscribed for object storage, so this test may not be not valid");
                }
                else {
                    tm.warn("No items were returned, so it is impossible to verify the validity of this test");
                }
            }
            else {
                for( Blob item : items ) {
                    assertBlob(support, item, testRootBucket, null);
                }
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void getBogusRootObject() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if (!support.allowsRootObjects()) {
            tm.ok("Root objects are not allowed in "+tm.getProvider().getCloudName());
            return;
        }
        Blob object = support.getObject(null, UUID.randomUUID().toString());

        tm.out("Bogus Object", object);
        assertNull("Fetched an actual object for the bogus ID requested", object);
    }

    @Test
    public void getRootObject() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testRootObject != null ) {
            @SuppressWarnings("ConstantConditions") Blob object = support.getObject(null, testRootObject.getObjectName());

            tm.out("Object", object);
            assertNotNull("The test object does not exist in the target cloud", object);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else if( !support.allowsRootObjects() ) {
                tm.ok("Root objects are not allowed in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test object for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void rootObjectContent() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testRootObject != null ) {
            @SuppressWarnings("ConstantConditions") Blob object = support.getObject(null, testRootObject.getObjectName());

            assertNotNull("The test object does not exist in the target cloud", object);

            out(object);
            assertBlob(support, object, null, false);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else if( !support.allowsRootObjects() ) {
                tm.ok("Root objects are not allowed in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test object for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void getBogusChildBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testRootBucket != null ) {
            Blob bucket = support.getBucket(testRootBucket + "/" + UUID.randomUUID().toString().replaceAll("-", String.valueOf(random.nextInt(9))).substring(0, 15));

            tm.out("Bogus Bucket", bucket);
            assertNull("Fetched an actual bucket for the bogus ID requested", bucket);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void getChildBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testChildBucket != null ) {
            @SuppressWarnings("ConstantConditions") Blob bucket = support.getBucket(testChildBucket.getBucketName());

            tm.out("Bucket", bucket);
            assertNotNull("The test bucket does not exist in the target cloud", bucket);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else if( !support.allowsNestedBuckets() ) {
                tm.ok("Nested buckets are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void childBucketContent() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testChildBucket != null ) {
            @SuppressWarnings("ConstantConditions") Blob bucket = support.getBucket(testChildBucket.getBucketName());

            assertNotNull("The test bucket does not exist in the target cloud", bucket);

            out(bucket);
            assertBlob(support, bucket, null, true);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else if( !support.allowsNestedBuckets() ) {
                tm.ok("Nested buckets are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void listItemsUnderChildBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testChildBucket != null ) {
            Iterable<Blob> items = support.list(testChildBucket.getBucketName());
            int count = 0;

            assertNotNull("Items returned may not be null", items);
            for( Blob item : items ) {
                count++;
                tm.out("Item", item);
            }
            tm.out("Total Items in " + testChildBucket.getBucketName(), count);
            if( count < 1 ) {
                if( !support.isSubscribed() ) {
                    tm.ok("Account is not subscribed for object storage, so this test may not be not valid");
                }
                else {
                    tm.warn("No items were returned, so it is impossible to verify the validity of this test");
                }
            }
            else {
                for( Blob item : items ) {
                    assertBlob(support, item, testChildBucket, null);
                }
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else if( !support.allowsNestedBuckets() ) {
                tm.ok("Nested buckets are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void getBogusChildObject() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Blob object = support.getObject(testRootBucket.getBucketName(), UUID.randomUUID().toString());

        tm.out("Bogus Object", object);
        assertNull("Fetched an actual object for the bogus ID requested", object);
    }

    @Test
    public void getChildObject() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testChildObject != null ) {
            @SuppressWarnings("ConstantConditions") Blob object = support.getObject(testChildObject.getBucketName(), testChildObject.getObjectName());

            tm.out("Object", object);
            assertNotNull("The test object does not exist in the target cloud", object);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void childObjectContent() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getOnlineStorageSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testChildObject != null && testRootBucket != null ) {
            @SuppressWarnings("ConstantConditions") Blob object = support.getObject(testChildObject.getBucketName(), testChildObject.getObjectName());

            assertNotNull("The test object does not exist in the target cloud", object);

            out(object);
            assertBlob(support, object, testRootBucket, false);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket or object for the test " + name.getMethodName());
            }
        }
    }


}
