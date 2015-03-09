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

package org.dasein.cloud.test.platform;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.platform.*;
import org.dasein.cloud.storage.Blob;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.storage.StorageResources;
import org.dasein.util.CalendarWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Manages all identity resources for automated provisioning and de-provisioning during integration tests.
 * <p>Created by George Reese: 2/18/13 10:16 AM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @version 2013.07 updated for message queue services support (issue #6)
 * @since 2013.04
 */
public class PlatformResources {
    static private final Logger logger = Logger.getLogger(PlatformResources.class);

    static private final Random random = new Random();

    private final HashMap<String,String> testCDNs   = new HashMap<String, String>();
    private final HashMap<String,String> testQueues = new HashMap<String, String>();
    private final HashMap<String,String> testRDBMS  = new HashMap<String, String>();
    private final HashMap<String,String> testTopics = new HashMap<String, String>();

    private CloudProvider   provider;

    public PlatformResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    private boolean canRemove(@Nullable Database db) {
        if( db == null ) {
            return true;
        }
        switch( db.getCurrentState() ) {
            case DELETING: case DELETED: case AVAILABLE: case STORAGE_FULL: case FAILED: return true;
            default: return false;
        }
    }

    public int close() {
        int count = 0;

        try {
            PlatformServices services = provider.getPlatformServices();

            if( services != null ) {
                // start CDN termination first, wait later
                ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

                CDNSupport cdnSupport = services.getCDNSupport();

                if( cdnSupport != null ) {
                    for( Map.Entry<String,String> entry : testCDNs.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                Distribution d = cdnSupport.getDistribution(entry.getValue());

                                if( d != null ) {
                                    results.add(cleanCDN(cdnSupport, entry.getValue()));
                                    count++;
                                }
                                else {
                                    count++;
                                }
                            }
                            catch( Throwable t ) {
                                logger.warn("Failed to de-provision test CDN " + entry.getValue() + ": " + t.getMessage());
                            }
                        }
                    }
                }

                MQSupport mqSupport = services.getMessageQueueSupport();

                if( mqSupport != null ) {
                    for( Map.Entry<String,String> entry : testQueues.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                MessageQueue mq = mqSupport.getMessageQueue(entry.getValue());

                                if( mq != null ) {
                                    mqSupport.removeMessageQueue(mq.getProviderMessageQueueId(), "Dasein Cloud test clean-up");
                                    count++;
                                }
                                else {
                                    count++;
                                }
                            }
                            catch( Throwable t ) {
                                logger.warn("Failed to de-provision test message queue " + entry.getValue() + ": " + t.getMessage());
                            }
                        }
                    }
                }

                PushNotificationSupport pushSupport = services.getPushNotificationSupport();

                if( pushSupport != null ) {
                    for( Map.Entry<String,String> entry : testTopics.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                Topic topic = pushSupport.getTopic(entry.getValue());

                                if( topic != null ) {
                                    pushSupport.removeTopic(entry.getValue());
                                    count++;
                                }
                                else {
                                    count++;
                                }
                            }
                            catch( Throwable t ) {
                                logger.warn("Failed to de-provision test notification topic " + entry.getValue() + ": " + t.getMessage());
                            }
                        }
                    }
                }

                RelationalDatabaseSupport rdbmsSupport = services.getRelationalDatabaseSupport();

                if( rdbmsSupport != null ) {
                    for( Map.Entry<String,String> entry : testRDBMS.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*20L);
                                Database db = rdbmsSupport.getDatabase(entry.getValue());

                                while( timeout > System.currentTimeMillis() ) {
                                    if( canRemove(db) ) {
                                        break;
                                    }
                                    try { Thread.sleep(15000L); }
                                    catch( InterruptedException ignore ) { }
                                    try { db = rdbmsSupport.getDatabase(db.getProviderDatabaseId()); }
                                    catch( Throwable ignore ) { }
                                }
                                if( db != null && !db.getCurrentState().equals(DatabaseState.DELETED) && !db.getCurrentState().equals(DatabaseState.DELETING) ) {
                                    rdbmsSupport.removeDatabase(entry.getValue());
                                    count++;
                                }
                                else {
                                    count++;
                                }
                            }
                            catch( Throwable t ) {
                                logger.warn("Failed to de-provision test relational database " + entry.getValue() + ": " + t.getMessage());
                            }
                        }
                    }
                }

                // no wait for CDN stuff
                boolean done;

                do {
                    done = true;
                    try { Thread.sleep(15000L); }
                    catch( InterruptedException ignore ) { }
                    for( Future<Boolean> result : results ) {
                        if( !result.isDone() ) {
                            done = false;
                            break;
                        }
                    }
                } while( !done );
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        return count;
    }

    ExecutorService service = Executors.newCachedThreadPool();

    private Future<Boolean> cleanCDN(final @Nonnull CDNSupport support, final @Nonnull String distributionId) {
        return service.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                support.delete(distributionId);
                return true;
            }
        });
    }

    public int report() {
        boolean header = false;
        int count = 0;

        testRDBMS.remove(DaseinTestManager.STATELESS);
        if( !testRDBMS.isEmpty() ) {
            logger.info("Provisioned Platform Resources:");
            header = true;
            count += testRDBMS.size();
            DaseinTestManager.out(logger, null, "---> RDBMS Instances", testRDBMS.size() + " " + testRDBMS);
        }
        testCDNs.remove(DaseinTestManager.STATELESS);
        if( !testCDNs.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Platform Resources:");
                header = true;
            }
            count += testCDNs.size();
            DaseinTestManager.out(logger, null, "---> CDN Distributions", testCDNs.size() + " " + testCDNs);
        }
        testQueues.remove(DaseinTestManager.STATELESS);
        if( !testQueues.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Platform Resources:");
                header = true;
            }
            count += testQueues.size();
            DaseinTestManager.out(logger, null, "---> Message Queues", testQueues.size() + " " + testQueues);
        }
        testTopics.remove(DaseinTestManager.STATELESS);
        if( !testTopics.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Platform Resources:");
            }
            count += testTopics.size();
            DaseinTestManager.out(logger, null, "---> Notification Topics", testTopics.size() + " " + testTopics);
        }
        return count;
    }

    public @Nullable String getTestDistributionId(@Nonnull String label, boolean provisionIfNull, @Nullable String origin) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testCDNs.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessDistribution();
        }
        String id = testCDNs.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            PlatformServices services = provider.getPlatformServices();

            if( services != null ) {
                CDNSupport support = services.getCDNSupport();

                if( support != null ) {
                    try {
                        return provisionDistribution(support, label, "Dasein CDN", origin);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }


    public @Nullable String getTestQueueId(@Nonnull String label, boolean provisionIfNull) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testQueues.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessMQ();
        }
        String id = testQueues.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            PlatformServices services = provider.getPlatformServices();

            if( services != null ) {
                MQSupport mqSupport = services.getMessageQueueSupport();

                if( mqSupport != null ) {
                    try {
                        return provisionMQ(mqSupport, label, "dsnmq");
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestRDBMSId(@Nonnull String label, boolean provisionIfNull, @Nullable DatabaseEngine engine) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testRDBMS.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessRDBMS();
        }
        String id = testRDBMS.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            PlatformServices services = provider.getPlatformServices();

            if( services != null ) {
                RelationalDatabaseSupport rdbmsSupport = services.getRelationalDatabaseSupport();

                if( rdbmsSupport != null ) {
                    try {
                        return provisionRDBMS(rdbmsSupport, label, "dsnrdbms", engine);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestTopicId(@Nonnull String label, boolean provisionIfNull) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testTopics.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessTopic();
        }
        String id = testTopics.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            PlatformServices services = provider.getPlatformServices();

            if( services != null ) {
                PushNotificationSupport support = services.getPushNotificationSupport();

                if( support != null ) {
                    try {
                        return provisionTopic(support, label, "dsntopic");
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String findStatelessDistribution() {
        PlatformServices services = provider.getPlatformServices();

        if( services != null ) {
            CDNSupport support = services.getCDNSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Iterator<Distribution> dists = support.list().iterator();

                    if( dists.hasNext() ) {
                        String id = dists.next().getProviderDistributionId();

                        testCDNs.put(DaseinTestManager.STATELESS, id);
                        return id;
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nullable String findStatelessMQ() {
        PlatformServices services = provider.getPlatformServices();

        if( services != null ) {
            MQSupport mqSupport = services.getMessageQueueSupport();

            try {
                if( mqSupport != null && mqSupport.isSubscribed() ) {
                    Iterator<MessageQueue> queues = mqSupport.listMessageQueues().iterator();

                    if( queues.hasNext() ) {
                        String id = queues.next().getProviderMessageQueueId();

                        testQueues.put(DaseinTestManager.STATELESS, id);
                        return id;
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nullable String findStatelessRDBMS() {
        PlatformServices services = provider.getPlatformServices();

        if( services != null ) {
            RelationalDatabaseSupport rdbmsSupport = services.getRelationalDatabaseSupport();

            try {
                if( rdbmsSupport != null && rdbmsSupport.isSubscribed() ) {
                    Iterator<Database> databases = rdbmsSupport.listDatabases().iterator();

                    if( databases.hasNext() ) {
                        String id = databases.next().getProviderDatabaseId();

                        testRDBMS.put(DaseinTestManager.STATELESS, id);
                        return id;
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nullable String findStatelessTopic() {
        PlatformServices services = provider.getPlatformServices();

        if( services != null ) {
            PushNotificationSupport support = services.getPushNotificationSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Iterator<Topic> topics = support.listTopics().iterator();

                    if( topics.hasNext() ) {
                        String id = topics.next().getProviderTopicId();

                        testTopics.put(DaseinTestManager.STATELESS, id);
                        return id;
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nonnull String provisionDistribution(@Nonnull CDNSupport support, @Nonnull String label, @Nonnull String namePrefix, @Nullable String origin) throws CloudException, InternalException {
        if( origin == null ) {
            StorageResources r = DaseinTestManager.getStorageResources();

            if( r != null ) {
                Blob bucket = r.getTestRootBucket(label, true, null);

                if( bucket != null ) {
                    origin = bucket.getBucketName();
                }
            }
        }
        if( origin == null ) {
            origin = "http://localhost";
        }
        String id = support.create(origin, namePrefix + random.nextInt(10000),  true, "dsncdn" + random.nextInt(10000) + ".dasein.org");

        synchronized( testCDNs ) {
            while( testCDNs.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testCDNs.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionMQ(@Nonnull MQSupport support, @Nonnull String label, @Nonnull String namePrefix) throws CloudException, InternalException {
        MQCreateOptions options = MQCreateOptions.getInstance(namePrefix + (System.currentTimeMillis()%10000), "Test MQ auto-provisioned by Dasein Cloud integration tests");
        String id = support.createMessageQueue(options);
        synchronized( testQueues ) {
            while( testQueues.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testQueues.put(label, id);
        }
        return id;
    }

    public static @Nonnull String randomPassword() {
        String password = "a" + random.nextInt(100000000);

        while( password.length() < 20 ) {
            password = password + random.nextInt(10);
        }
        return password;
    }

    public static @Nonnull DatabaseProduct getCheapestProduct(@Nonnull RelationalDatabaseSupport support, @Nullable DatabaseEngine engine, @Nullable DatabaseProduct afterThis) throws CloudException, InternalException {
        if( engine == null ) {
            for( DatabaseEngine e : support.getDatabaseEngines() ) {
                if( DatabaseEngine.MYSQL.equals(e) ) {
                    engine = e;
                    break;
                }
            }
        }
        if( engine == null ) {
            throw new InternalException("No engine was specified, and the cloud doesn't seem to support MySQL. Getting outta here.");
        }

        DatabaseProduct product = getNextCheapestProduct(support.listDatabaseProducts(engine), afterThis );

        if( product == null ) {
            throw new CloudException("No database product could be identified");
        }

        return product;
    }

    private static @Nonnull DatabaseProduct getNextCheapestProduct(@Nonnull Iterable<DatabaseProduct> fromList, @Nullable DatabaseProduct afterThis) {
        DatabaseProduct minimal = null;
        for( DatabaseProduct product : fromList ) {
            if( product.getLicenseModel() == DatabaseLicenseModel.BRING_YOUR_OWN_LICENSE) {
                // can't use in tests
                continue;
            }
            if( minimal == null ) {
                if( afterThis == null ) {
                    minimal = product;
                }
                else if( product.getStandardHourlyRate() > afterThis.getStandardHourlyRate() ) {
                    minimal = product;
                }
            }
            else {
                if( afterThis == null ) {
                    if( product.getStandardHourlyRate() < minimal.getStandardHourlyRate() ) {
                        minimal = product;
                    }
                }
                else if( product.getStandardHourlyRate() > afterThis.getStandardHourlyRate()
                        && product.getStandardHourlyRate() < minimal.getStandardHourlyRate() ) {
                    minimal = product;
                }
            }
        }
        return minimal;
    }

    public @Nonnull String provisionRDBMS(@Nonnull RelationalDatabaseSupport support, @Nonnull String label, @Nonnull String namePrefix, @Nullable DatabaseEngine engine) throws CloudException, InternalException {
        String version = support.getDefaultVersion(engine);

        String id = null;
        DatabaseProduct databaseProduct = getCheapestProduct(support, engine, null);
        do {
            try {
                id = support.createFromScratch(namePrefix + ( System.currentTimeMillis() % 10000 ), databaseProduct, version, "dasein", randomPassword(), 3000);
            } catch (CloudException e) {
                if( CloudErrorType.CAPACITY.equals(e.getErrorType()) ) {
                    databaseProduct = getCheapestProduct(support, engine, databaseProduct);
                }
                else {
                    throw e;
                }
            }
        } while( id == null );

        if( id == null ) {
            throw new CloudException("No database was generated");
        }

        synchronized( testRDBMS ) {
            while( testRDBMS.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testRDBMS.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionTopic(@Nonnull PushNotificationSupport support, @Nonnull String label, @Nonnull String namePrefix) throws CloudException, InternalException {
        String id = support.createTopic(namePrefix + random.nextInt(10000)).getProviderTopicId();

        synchronized( testTopics ) {
            while( testTopics.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testTopics.put(label, id);
        }
        return id;
    }

}
