package org.dasein.cloud.test.platform;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.platform.Database;
import org.dasein.cloud.platform.DatabaseEngine;
import org.dasein.cloud.platform.DatabaseProduct;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.platform.RelationalDatabaseSupport;
import org.dasein.cloud.test.DaseinTestManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Manages all identity resources for automated provisioning and de-provisioning during integration tests.
 * <p>Created by George Reese: 2/18/13 10:16 AM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class PlatformResources {
    static private final Logger logger = Logger.getLogger(PlatformResources.class);

    static private final Random random = new Random();

    private final HashMap<String,String> testRDBMS = new HashMap<String, String>();

    private CloudProvider   provider;

    public PlatformResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    public void close() {
        try {
            PlatformServices services = provider.getPlatformServices();

            if( services != null ) {
                RelationalDatabaseSupport rdbmsSupport = services.getRelationalDatabaseSupport();

                if( rdbmsSupport != null ) {
                    for( Map.Entry<String,String> entry : testRDBMS.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                rdbmsSupport.removeDatabase(entry.getValue());
                            }
                            catch( Throwable ignore ) {
                                // ignore
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
    }

    public void report() {
        //boolean header = false;

        testRDBMS.remove(DaseinTestManager.STATELESS);
        if( !testRDBMS.isEmpty() ) {
            logger.info("Provisioned Platform Resources:");
            //header = true;
            DaseinTestManager.out(logger, null, "---> RDBMS Instances", testRDBMS.size() + " " + testRDBMS);
        }
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

    public @Nonnull String provisionRDBMS(@Nonnull RelationalDatabaseSupport support, @Nonnull String label, @Nonnull String namePrefix, @Nullable DatabaseEngine engine) throws CloudException, InternalException {
        String password = "a" + random.nextInt(100000000);
        String id;

        while( password.length() < 20 ) {
            password = password + random.nextInt(10);
        }
        DatabaseProduct product = null;

        if( engine != null ) {
            for( DatabaseProduct p : support.getDatabaseProducts(engine) ) {
                if( product == null || product.getStandardHourlyRate() > p.getStandardHourlyRate() ) {
                    product = p;
                }
            }
        }
        else {
            for( DatabaseEngine e : support.getDatabaseEngines() ) {
                for( DatabaseProduct p : support.getDatabaseProducts(e) ) {
                    if( product == null || product.getStandardHourlyRate() > p.getStandardHourlyRate() ) {
                        product = p;
                    }
                }
            }
        }
        if( product == null ) {
            throw new CloudException("No database product could be identified");
        }
        String version = support.getDefaultVersion(product.getEngine());

        id = support.createFromScratch(namePrefix + (System.currentTimeMillis()%10000), product, version, "dasein", password, 3000);
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
}
