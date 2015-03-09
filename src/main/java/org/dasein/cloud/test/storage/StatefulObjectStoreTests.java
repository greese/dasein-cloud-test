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
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.storage.Blob;
import org.dasein.cloud.storage.BlobStoreSupport;
import org.dasein.cloud.storage.FileTransfer;
import org.dasein.cloud.storage.StorageServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Byte;
import org.dasein.util.uom.storage.Storage;
import org.junit.*;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import java.io.*;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests that validate the stateful functionality of a Dasein Cloud implementation against an object storage system.
 * <p>Created by George Reese: 3/3/13 4:57 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatefulObjectStoreTests {
    static private final String LINE_ONE = "1: Test";
    static private final String LINE_TWO = "2: Done.";

    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulObjectStoreTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private Blob testObject;
    private Blob testBucket;

    public StatefulObjectStoreTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        if( name.getMethodName().equals("removeRootBucket") ) {
            testBucket = tm.getTestBucket(DaseinTestManager.REMOVED, true, true);
        }
        else if( name.getMethodName().equals("removeChildBucket") ) {
            testBucket = tm.getTestBucket(DaseinTestManager.REMOVED + name.getMethodName(), false, true);
        }
        else if( name.getMethodName().equals("createChildBucket") ) {
            testBucket = tm.getTestBucket(DaseinTestManager.STATEFUL, true, true);

        }
        else if( name.getMethodName().equals("downloadRoot") ) {
            StorageResources resources = DaseinTestManager.getStorageResources();

            if( resources != null ) {
                StorageServices services = tm.getProvider().getStorageServices();

                if( services != null ) {
                    BlobStoreSupport support = services.getBlobStoreSupport();

                    if( support != null ) {
                        try {
                            testObject = resources.provisionRootObject(support, "download", "dsndl", LINE_ONE, LINE_TWO);
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
        }
        else if( name.getMethodName().equals("downloadChild") ) {
            testBucket = tm.getTestBucket(DaseinTestManager.STATEFUL, true, true);
            if( testBucket != null ) {
                StorageResources resources = DaseinTestManager.getStorageResources();

                if( resources != null ) {
                    StorageServices services = tm.getProvider().getStorageServices();

                    if( services != null ) {
                        BlobStoreSupport support = services.getBlobStoreSupport();

                        if( support != null ) {
                            try {
                                testObject = resources.provisionChildObject(support, "download", testBucket.getBucketName(), "dsndl", LINE_ONE, LINE_TWO);
                            }
                            catch( Throwable ignore ) {
                                // ignore
                                // debug
                                tm.out("No testObject for downloadChild - exception: " + ignore.getLocalizedMessage());
                            }
                        }
                    }
                }
            }
        }
        else if( name.getMethodName().equals("uploadChild") ) {
            testBucket = tm.getTestBucket(DaseinTestManager.STATEFUL, true, true);
        }
        else if( name.getMethodName().equals("removeRootObject") ) {
            testObject = tm.getTestObject(DaseinTestManager.REMOVED, true, true);
        }
        else if( name.getMethodName().equals("removeChildObject") ) {
            testObject = tm.getTestObject(DaseinTestManager.REMOVED + name.getMethodName(), false, true);
        }
        else if( name.getMethodName().equals("clear") ) {
            StorageResources resources = DaseinTestManager.getStorageResources();

            if( resources != null ) {
                StorageServices services = tm.getProvider().getStorageServices();

                if( services != null ) {
                    BlobStoreSupport support = services.getBlobStoreSupport();

                    if( support != null ) {
                        try {
                            testBucket = resources.provisionRootBucket(support, "removed2", "dsnbkt", false, true);
                            //noinspection ConstantConditions
                            testObject = resources.provisionChildObject(support, "removed2", testBucket.getBucketName(), "dsnobj");
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
        }
        else if( name.getMethodName().equals("objectSize") ) {
            testObject = tm.getTestObject(DaseinTestManager.STATEFUL, false, true);
        }
    }

    @After
    public void after() {
        try {
            testBucket = null;
            testObject = null;
        }
        finally {
            tm.end();
        }
    }

    private void assertFile(@Nonnull File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

        try {
            assertEquals("The first line of the downloaded file does not match", LINE_ONE, reader.readLine());
            assertEquals("The second line of the downloaded file does not match", LINE_TWO, reader.readLine());
            assertEquals("There are extra lines in the downloaded file", null, reader.readLine());
        }
        finally {
            reader.close();
        }
    }

    @Test
    public void createRootBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        StorageResources resources = DaseinTestManager.getStorageResources();

        assertNotNull("Failed to initialize storage resources for all tests", resources);

        Blob bucket = resources.provisionRootBucket(support, "provision", "dsnbkt", false, true);

        tm.out("Created", bucket);
        assertNotNull("The newly created bucket is not supposed to be null", bucket);
    }

    @Test
    public void createChildBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testBucket != null ) {
            StorageResources resources = DaseinTestManager.getStorageResources();

            assertNotNull("Failed to initialize storage resources for all tests", resources);

            if( support.allowsNestedBuckets() ) {
                @SuppressWarnings("ConstantConditions") Blob bucket = resources.provisionChildBucket(support, "provision", testBucket.getBucketName(), "dsnbkt", false, true);

                tm.out("Created", bucket);
                assertNotNull("The newly created bucket is not supposed to be null", bucket);
            }
            else {
                try {
                    resources.provisionRootBucket(support, "provision", testBucket.getBucketName() + "/dsnbkt", false, true);
                    fail("Creating a nested bucket succeeded in a cloud that does not support nesting");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException when trying to nest a bucket in a bucket in a cloud that does not support nesting");
                }
            }
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

    @Test
    public void removeRootBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testBucket != null ) {
            @SuppressWarnings("ConstantConditions") Blob bucket = support.getBucket(testBucket.getBucketName());

            tm.out("Before", bucket);
            assertNotNull("Bucket does not exist so test cannot function properly", bucket);
            //noinspection ConstantConditions
            support.removeBucket(bucket.getBucketName());

            long timeout = System.currentTimeMillis() + CalendarWrapper.MINUTE;

            while( timeout > System.currentTimeMillis() ) {
                try {
                    //noinspection ConstantConditions
                    bucket = support.getBucket(bucket.getBucketName());

                    if( bucket == null ) {
                        break;
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try { Thread.sleep(5000L); }
                catch( InterruptedException ignore ) { }
            }
            tm.out("After", bucket);
            assertNull("The system timed out before the cloud reflected any deletion of the root bucket " + testBucket.getBucketName(), bucket);
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

    @Test
    public void removeChildBucket() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testBucket != null ) {
            @SuppressWarnings("ConstantConditions") Blob bucket = support.getBucket(testBucket.getBucketName());

            tm.out("Before", bucket);
            assertNotNull("Bucket does not exist so test cannot function properly", bucket);
            //noinspection ConstantConditions
            support.removeBucket(bucket.getBucketName());

            long timeout = System.currentTimeMillis() + CalendarWrapper.MINUTE;

            while( timeout > System.currentTimeMillis() ) {
                try {
                    //noinspection ConstantConditions
                    bucket = support.getBucket(bucket.getBucketName());

                    if( bucket == null ) {
                        break;
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try { Thread.sleep(5000L); }
                catch( InterruptedException ignore ) { }
            }
            tm.out("After", bucket);
            assertNull("The system timed out before the cloud reflected any deletion of the child bucket " + testBucket.getBucketName(), bucket);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else if( !support.allowsNestedBuckets() ) {
                tm.ok("Nested buckets are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test bucket or object for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void uploadRoot() throws CloudException, InternalException, IOException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( support.isSubscribed() ) {
            StorageResources resources = DaseinTestManager.getStorageResources();

            if( resources != null ) {
                if( support.allowsRootObjects() ) {
                    Blob blob = resources.provisionRootObject(support, "upload", "dsnobj", LINE_ONE, LINE_TWO);

                    tm.out("Uploaded", blob);
                    assertNotNull("Uploaded blob returned a null object", blob);
                }
                else {
                    try {
                        resources.provisionRootObject(support, "upload", "dsnobj", LINE_ONE, LINE_TWO);
                        fail("Uploading a root object succeeded in a cloud that does not allow root objects");
                    }
                    catch( OperationNotSupportedException expected ) {
                        tm.ok("Caught OperationNotSupportedException attempting to upload a root object in a cloud that does not allow root objects");
                    }
                }
            }
            else {
                fail("Failed to initialize storage resources for all tests");
            }
        }
        else {
            tm.ok("Not subscribed to object storage so this test is not valid");
        }
    }

    @Test
    public void uploadChild() throws CloudException, InternalException, IOException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testBucket != null ) {
            StorageResources resources = DaseinTestManager.getStorageResources();

            if( resources != null ) {
                Blob blob = resources.provisionChildObject(support, "upload", testBucket.getBucketName(), "dsnobj", LINE_ONE, LINE_TWO);

                tm.out("Uploaded", blob);
                assertNotNull("Uploaded blob returned a null object", blob);
            }
            else {
                fail("Failed to initialize storage resources for all tests");
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to object storage so this test is not valid");
            }
            else {
                fail("No test bucket exists for validating uploads");
            }
        }
    }

    @Test
    public void downloadRoot() throws CloudException, InternalException, IOException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testObject != null ) {
            File targetFile = File.createTempFile("dsndl", ".dl");

            try {
                FileTransfer task;

                //noinspection ConstantConditions
                task = support.download(null, testObject.getObjectName(), targetFile);
                while( !task.isComplete() ) {
                    try { Thread.sleep(1000L); }
                    catch( InterruptedException e ) { }
                }
                //noinspection ThrowableResultOfMethodCallIgnored
                if( task.getTransferError() != null ) {
                    throw new CloudException(task.getTransferError());
                }
                tm.out("Downloaded", targetFile.length() + " bytes");
                assertFile(targetFile);
            }
            finally {
                if( targetFile.exists() ) {
                    //noinspection ResultOfMethodCallIgnored
                    targetFile.delete();
                }
            }
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
    public void downloadChild() throws CloudException, InternalException, IOException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testObject != null ) {
            File targetFile = File.createTempFile("dsndl", ".dl");

            try {
                FileTransfer task;

                //noinspection ConstantConditions
                task = support.download(testObject.getBucketName(), testObject.getObjectName(), targetFile);
                while( !task.isComplete() ) {
                    try { Thread.sleep(1000L); }
                    catch( InterruptedException e ) { }
                }
                //noinspection ThrowableResultOfMethodCallIgnored
                if( task.getTransferError() != null ) {
                    throw new CloudException(task.getTransferError());
                }
                tm.out("Downloaded", targetFile.length() + " bytes");
                assertFile(targetFile);
            }
            finally {
                if( targetFile.exists() ) {
                    //noinspection ResultOfMethodCallIgnored
                    targetFile.delete();
                }
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test object for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void removeRootObject() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testObject != null ) {
            @SuppressWarnings("ConstantConditions") Blob object = support.getObject(testObject.getBucketName(), testObject.getObjectName());

            tm.out("Before", object);
            assertNotNull("The test object does not really exist", object);
            //noinspection ConstantConditions
            support.removeObject(object.getBucketName(), object.getObjectName());
            long timeout = System.currentTimeMillis() + CalendarWrapper.MINUTE;

            while( timeout > System.currentTimeMillis() ) {
                try {
                    //noinspection ConstantConditions
                    object = support.getObject(object.getBucketName(), object.getObjectName());

                    if( object == null ) {
                        break;
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try { Thread.sleep(5000L); }
                catch( InterruptedException ignore ) { }
            }
            tm.out("After", object);
            assertNull("The system timed out before the cloud reflected any deletion of the root object " + testObject.getObjectName(), object);

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
    public void removeChildObject() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testObject != null ) {
            @SuppressWarnings("ConstantConditions") Blob object = support.getObject(testObject.getBucketName(), testObject.getObjectName());

            tm.out("Before", object);
            assertNotNull("The test object does not really exist", object);
            //noinspection ConstantConditions
            support.removeObject(object.getBucketName(), object.getObjectName());
            long timeout = System.currentTimeMillis() + CalendarWrapper.MINUTE;

            while( timeout > System.currentTimeMillis() ) {
                try {
                    //noinspection ConstantConditions
                    object = support.getObject(object.getBucketName(), object.getObjectName());

                    if( object == null ) {
                        break;
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try { Thread.sleep(5000L); }
                catch( InterruptedException ignore ) { }
            }
            tm.out("After", object);
            assertNull("The system timed out before the cloud reflected any deletion of the child object " + testObject.getBucketName() + "/" + testObject.getObjectName(), object);

        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test object for the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void clear() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testBucket != null ) {
            @SuppressWarnings("ConstantConditions") Blob bucket = support.getBucket(testBucket.getBucketName());

            tm.out("Before", bucket);
            assertNotNull("Bucket does not exist so test cannot function properly", bucket);
            //noinspection ConstantConditions
            support.clearBucket(bucket.getBucketName());

            long timeout = System.currentTimeMillis() + CalendarWrapper.MINUTE;

            while( timeout > System.currentTimeMillis() ) {
                try {
                    //noinspection ConstantConditions
                    bucket = support.getBucket(bucket.getBucketName());

                    if( bucket == null ) {
                        break;
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try { Thread.sleep(5000L); }
                catch( InterruptedException ignore ) { }
            }
            tm.out("After", bucket);
            assertNull("The system timed out before the cloud reflected any deletion of the root bucket " + testBucket.getBucketName(), bucket);
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

    @Test
    public void objectSize() throws CloudException, InternalException {
        StorageServices services = tm.getProvider().getStorageServices();

        if( services == null ) {
            tm.ok("No storage services are supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }

        BlobStoreSupport support = services.getBlobStoreSupport();

        if( support == null ) {
            tm.ok("No object storage is supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testObject != null ) {
            Storage<Byte> size = support.getObjectSize(testObject.getBucketName(), testObject.getObjectName());

            tm.out("Size", size);
            assertNotNull("The size may not be null", size);
            assertTrue("The object size must be non-negative", size.longValue() > -1);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("No subscription to object storage in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            }
            else {
                fail("Unable to establish a test object for the test " + name.getMethodName());
            }
        }
    }
}
