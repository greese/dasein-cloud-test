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

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.storage.Blob;
import org.dasein.cloud.storage.BlobStoreSupport;
import org.dasein.cloud.storage.StorageServices;
import org.dasein.cloud.test.DaseinTestManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 3/3/13 4:55 PM</p>
 *
 * @author George Reese
 */
public class StorageResources {
    static private final Logger logger = Logger.getLogger(StorageResources.class);

    static private final Random random = new Random();

    private final HashMap<String,Blob> testChildBuckets = new HashMap<String, Blob>();
    private final HashMap<String,Blob> testChildObjects = new HashMap<String, Blob>();
    private final HashMap<String,Blob> testRootBuckets = new HashMap<String, Blob>();
    private final HashMap<String,Blob> testRootObjects = new HashMap<String, Blob>();

    private CloudProvider provider;

    public StorageResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    public int close() {
        int count = 0;

        try {
            StorageServices services = provider.getStorageServices();

            if( services != null ) {
                BlobStoreSupport support = services.getBlobStoreSupport();

                if( support != null ) {
                    for( Map.Entry<String,Blob> entry : testRootObjects.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                String bucket = entry.getValue().getBucketName();
                                String object = entry.getValue().getObjectName();

                                if( object == null ) {
                                    continue; // not possible
                                }
                                Blob blob = support.getObject(bucket, object);

                                if( blob != null ) {
                                    support.removeObject(bucket, object);
                                    count++;
                                }
                                else {
                                    count++;
                                }
                            }
                            catch( Throwable t ) {
                                logger.warn("Failed to remove test root object " + entry.getValue() + ": " + t.getMessage());
                            }
                        }
                    }

                    for( Map.Entry<String,Blob> entry : testChildObjects.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                String bucket = entry.getValue().getBucketName();
                                String object = entry.getValue().getObjectName();

                                if( object == null ) {
                                    continue; // not possible
                                }
                                Blob blob = support.getObject(bucket, object);

                                if( blob != null ) {
                                    support.removeObject(bucket, object);
                                    count++;
                                }
                                else {
                                    count++;
                                }
                            }
                            catch( Throwable t ) {
                                logger.warn("Failed to remove test child object " + entry.getValue() + ": " + t.getMessage());
                            }
                        }
                    }
                    for( Map.Entry<String,Blob> entry : testChildBuckets.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                String bucket = entry.getValue().getBucketName();

                                if( bucket == null ) {
                                    bucket = entry.getValue().getObjectName();
                                    if( bucket == null ) {
                                        continue; // not possible
                                    }
                                }
                                else {
                                    bucket = bucket + "/" + entry.getValue().getObjectName();
                                }
                                Blob blob = support.getBucket(bucket);

                                if( blob != null ) {
                                    support.removeBucket(bucket);
                                    count++;
                                }
                                else {
                                    count++;
                                }
                            }
                            catch( Throwable t ) {
                                logger.warn("Failed to remove test child bucket " + entry.getValue() + ": " + t.getMessage());
                            }
                        }
                    }
                    for( Map.Entry<String,Blob> entry : testRootBuckets.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                Blob blob = support.getBucket(entry.getValue().getBucketName());

                                if( blob != null ) {
                                    support.removeBucket(entry.getValue().getBucketName());
                                    count++;
                                }
                                else {
                                    count++;
                                }
                            }
                            catch( Throwable t ) {
                                logger.warn("Failed to remove test root bucket " + entry.getValue() + ": " + t.getMessage());
                            }
                        }
                    }
                }
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        provider.close();
        return count;
    }

    public int report() {
        boolean header = false;
        int count = 0;

        testRootBuckets.remove(DaseinTestManager.STATELESS);
        if( !testRootBuckets.isEmpty() ) {
            logger.info("Provisioned Storage Resources:");
            header = true;
            count += testRootBuckets.size();
            DaseinTestManager.out(logger, null, "---> Root Buckets", testRootBuckets.size() + " " + testRootBuckets);
        }
        testRootObjects.remove(DaseinTestManager.STATELESS);
        if( !testRootObjects.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Storage Resources:");
                header = true;
            }
            count += testRootObjects.size();
            DaseinTestManager.out(logger, null, "---> Root Objects", testRootObjects.size() + " " + testRootObjects);
        }
        testChildBuckets.remove(DaseinTestManager.STATELESS);
        if( !testChildBuckets.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Storage Resources:");
                header = true;
            }
            count += testChildBuckets.size();
            DaseinTestManager.out(logger, null, "---> Child Buckets", testChildBuckets.size() + " " + testChildBuckets);
        }
        testChildObjects.remove(DaseinTestManager.STATELESS);
        if( !testChildObjects.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Storage Resources:");
            }
            count += testChildObjects.size();
            DaseinTestManager.out(logger, null, "---> Child Objects", testChildObjects.size() + " " + testChildObjects);
        }
        return count;
    }

    public @Nullable Blob getTestRootBucket(@Nonnull String label, boolean provisionIfNull, @Nullable String namePrefix) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,Blob> entry : testRootBuckets.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    Blob bucket = entry.getValue();

                    if( bucket != null ) {
                        return bucket;
                    }
                }
            }
            return findStatelessRootBucket();
        }
        Blob bucket = testRootBuckets.get(label);

        if( bucket != null ) {
            return bucket;
        }
        if( provisionIfNull ) {
            StorageServices services = provider.getStorageServices();

            if( services != null ) {
                BlobStoreSupport support = services.getBlobStoreSupport();

                if( support != null ) {
                    try {
                        return provisionRootBucket(support, label, namePrefix == null ? "dsnbkt" : namePrefix, false, true);
                    }
                    catch( Throwable t ) {
                        logger.warn("Unable to provision root test bucket: " + t.getMessage());
                    }
                }
            }
        }
        return null;
    }

    public @Nullable Blob getTestChildBucket(@Nonnull String label, boolean provisionIfNull, @Nullable String parentBucket, @Nullable String namePrefix) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,Blob> entry : testChildBuckets.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    Blob bucket = entry.getValue();

                    if( bucket != null ) {
                        return bucket;
                    }
                }
            }
            return findStatelessChildBucket();
        }
        Blob bucket = testChildBuckets.get(label);

        if( bucket != null ) {
            return bucket;
        }
        if( provisionIfNull ) {
            StorageServices services = provider.getStorageServices();

            if( services != null ) {
                BlobStoreSupport support = services.getBlobStoreSupport();

                if( support != null ) {
                    try {
                        if( parentBucket == null ) {
                            Blob parent = getTestRootBucket(label, true, null);

                            if( parent == null ) {
                                return null;
                            }
                            parentBucket = parent.getBucketName();
                            if( parentBucket == null ) {
                                return null;
                            }
                        }
                        return provisionChildBucket(support, label, parentBucket, namePrefix == null ? "dsnbkt" : namePrefix, false, true);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable Blob getTestRootObject(@Nonnull String label, boolean provisionIfNull, @Nullable String namePrefix) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,Blob> entry : testRootObjects.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    Blob bucket = entry.getValue();

                    if( bucket != null ) {
                        return bucket;
                    }
                }
            }
            return findStatelessRootObject();
        }
        Blob object = testRootObjects.get(label);

        if( object != null ) {
            return object;
        }
        if( provisionIfNull ) {
            StorageServices services = provider.getStorageServices();

            if( services != null ) {
                BlobStoreSupport support = services.getBlobStoreSupport();

                if( support != null ) {
                    try {
                        return provisionRootObject(support, label, namePrefix == null ? "dsnobj" : namePrefix);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable Blob getTestChildObject(@Nonnull String label, boolean provisionIfNull, @Nullable String parentBucket, @Nullable String namePrefix) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,Blob> entry : testChildObjects.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    Blob bucket = entry.getValue();

                    if( bucket != null ) {
                        return bucket;
                    }
                }
            }
            return findStatelessChildObject();
        }
        Blob object = testChildObjects.get(label);

        if( object != null ) {
            return object;
        }
        if( provisionIfNull ) {
            StorageServices services = provider.getStorageServices();

            if( services != null ) {
                BlobStoreSupport support = services.getOnlineStorageSupport();

                if( support != null ) {
                    try {
                        if( parentBucket == null ) {
                            Blob parent = getTestRootBucket(DaseinTestManager.STATEFUL, true, null);

                            if( parent == null ) {
                                return null;
                            }
                            parentBucket = parent.getBucketName();
                            if( parentBucket == null ) {
                                return null;
                            }
                        }
                        return provisionChildObject(support, label, parentBucket, namePrefix == null ? "dsnobj" : namePrefix);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable Blob findStatelessChildBucket() {
        StorageServices services = provider.getStorageServices();

        if( services != null ) {
            BlobStoreSupport support = services.getOnlineStorageSupport();

            try {
                if( support != null && support.allowsNestedBuckets() && support.isSubscribed() ) {
                    Iterable<Blob> roots = support.list(null);

                    for( Blob root : roots ) {
                        if( root.isContainer() ) {
                            Iterable<Blob> options = support.list(root.getBucketName());

                            for( Blob option : options ) {
                                if( option.isContainer() ) {
                                    testChildBuckets.put(DaseinTestManager.STATELESS, option);
                                    return option;
                                }
                            }
                        }
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nullable Blob findStatelessRootBucket() {
        StorageServices services = provider.getStorageServices();

        if( services != null ) {
            BlobStoreSupport support = services.getBlobStoreSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Iterable<Blob> options = support.list(null);
                    Blob defaultBlob = null;

                    for( Blob option : options ) {
                        if( option.isContainer() ) {
                            Iterable<Blob> children = support.list(option.getBucketName());

                            if( children.iterator().hasNext() ) {
                                testRootBuckets.put(DaseinTestManager.STATELESS, option);
                                return option;
                            }
                            if( defaultBlob == null ) {
                                defaultBlob = option;
                            }
                        }
                    }
                    if( defaultBlob != null ) {
                        testRootBuckets.put(DaseinTestManager.STATELESS, defaultBlob);
                        return defaultBlob;
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nullable Blob findStatelessChildObject() {
        StorageServices services = provider.getStorageServices();

        if( services != null ) {
            BlobStoreSupport support = services.getBlobStoreSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Iterable<Blob> roots = support.list(null);

                    for( Blob root : roots ) {
                        if( root.isContainer() ) {
                            Iterable<Blob> options = support.list(root.getBucketName());

                            for( Blob option : options ) {
                                if( !option.isContainer() ) {
                                    testChildObjects.put(DaseinTestManager.STATELESS, option);
                                    return option;
                                }
                            }
                        }
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nullable Blob findStatelessRootObject() {
        StorageServices services = provider.getStorageServices();

        if( services != null ) {
            BlobStoreSupport support = services.getBlobStoreSupport();

            try {
                if( support != null && support.allowsRootObjects() && support.isSubscribed() ) {
                    Iterable<Blob> options = support.list(null);

                    for( Blob option : options ) {
                        if( !option.isContainer() ) {
                            testRootObjects.put(DaseinTestManager.STATELESS, option);
                            return option;
                        }
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nonnull Blob provisionRootBucket(@Nonnull BlobStoreSupport support, @Nonnull String label, @Nonnull String namePrefix, boolean useName, boolean findFreeName) throws CloudException, InternalException {
        String name = (useName ? namePrefix : (namePrefix + random.nextInt(10000)));
        Blob blob = support.createBucket(name, findFreeName);

        synchronized( testRootBuckets ) {
            while( testRootBuckets.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testRootBuckets.put(label, blob);
        }
        return blob;
    }

    public @Nonnull Blob provisionChildBucket(@Nonnull BlobStoreSupport support, @Nonnull String label, @Nonnull String parentBucket, @Nonnull String namePrefix, boolean useName, boolean findFreeName) throws CloudException, InternalException {
        String name = (useName ? namePrefix : (namePrefix + random.nextInt(10000)));

        Blob blob = support.createBucket(parentBucket + "/" + name, findFreeName);

        synchronized( testChildBuckets ) {
            while( testChildBuckets.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testChildBuckets.put(label, blob);
        }
        return blob;
    }

    public @Nonnull Blob provisionRootObject(@Nonnull BlobStoreSupport support, @Nonnull String label, @Nonnull String namePrefix, @Nullable String ... lines) throws CloudException, InternalException {
        try {
            File file = File.createTempFile("dsntst", "txt");

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));

            if( lines == null ) {
                writer.write("This is a test of Dasein Cloud");
                writer.newLine();
            }
            else {
                for( String line : lines ) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            writer.flush();
            writer.close();

            Blob blob = support.upload(file, null, namePrefix + random.nextInt(10000) + ".txt");

            synchronized( testChildObjects ) {
                while( testChildObjects.containsKey(label) ) {
                    label = label + random.nextInt(9);
                }
                testChildObjects.put(label, blob);
            }
            return blob;
        }
        catch( IOException e ) {
            throw new InternalException(e);
        }
    }

    public @Nonnull Blob provisionChildObject(@Nonnull BlobStoreSupport support, @Nonnull String label, @Nonnull String parentBucket, @Nonnull String namePrefix, @Nullable String ... lines) throws CloudException, InternalException {
        try {
            File file = File.createTempFile("dsntst", "txt");

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));

            if( lines == null ) {
                writer.write("This is a test of Dasein Cloud");
                writer.newLine();
            }
            else {
                for( String line : lines ) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            writer.flush();
            writer.close();

            Blob blob = support.upload(file, parentBucket, namePrefix + random.nextInt(10000) + ".txt");

            synchronized( testChildObjects ) {
                while( testChildObjects.containsKey(label) ) {
                    label = label + random.nextInt(9);
                }
                testChildObjects.put(label, blob);
            }
            return blob;
        }
        catch( IOException e ) {
            throw new InternalException(e);
        }
    }
}
