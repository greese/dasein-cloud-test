package org.dasein.cloud.test.identity;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.identity.SSHKeypair;
import org.dasein.cloud.identity.ShellKeySupport;
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
public class IdentityResources {
    static private final Logger logger = Logger.getLogger(IdentityResources.class);

    static private final Random random = new Random();

    private final HashMap<String,String> testKeys = new HashMap<String, String>();
    private CloudProvider   provider;

    public IdentityResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    public void close() {
        try {
            IdentityServices identityServices = provider.getIdentityServices();

            if( identityServices != null ) {
                ShellKeySupport keySupport = identityServices.getShellKeySupport();

                if( keySupport != null ) {
                    for( Map.Entry<String,String> entry : testKeys.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                keySupport.deleteKeypair(entry.getValue());
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

        testKeys.remove(DaseinTestManager.STATELESS);
        if( !testKeys.isEmpty() ) {
            logger.info("Provisioned Identity Resources:");
            //header = true;
            DaseinTestManager.out(logger, null, "---> SSH Keypairs", testKeys.size() + " " + testKeys);
        }
    }

    public @Nullable String getTestKeypairId(@Nonnull String label, boolean provisionIfNull) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testKeys.entrySet() ) {
                String id = entry.getValue();

                if( id != null ) {
                    return id;
                }
            }
            return findStatelessKeypair();
        }
        String id = testKeys.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            IdentityServices services = provider.getIdentityServices();

            if( services != null ) {
                ShellKeySupport support = services.getShellKeySupport();

                if( support != null ) {
                    try {
                        return provision(support, label, "dsnkp");
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String findStatelessKeypair() {
        IdentityServices identityServices = provider.getIdentityServices();

        if( identityServices != null ) {
            ShellKeySupport keySupport = identityServices.getShellKeySupport();

            try {
                if( keySupport != null && keySupport.isSubscribed() ) {
                    Iterator<SSHKeypair> keypairs = keySupport.list().iterator();

                    if( keypairs.hasNext() ) {
                        String id = keypairs.next().getProviderKeypairId();

                        testKeys.put(DaseinTestManager.STATELESS, id);
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

    public @Nonnull String provision(@Nonnull ShellKeySupport support, @Nonnull String label, @Nonnull String namePrefix) throws CloudException, InternalException {
        String id = null;

        if( support.getKeyImportSupport().equals(Requirement.REQUIRED) ) {
            String publicKey = null;

            // TODO: generate key for import
            if( publicKey != null ) {
                id = support.importKeypair(namePrefix+ (System.currentTimeMillis()%10000), publicKey).getProviderKeypairId();
            }
        }
        else {
            id = support.createKeypair(namePrefix + (System.currentTimeMillis()%10000)).getProviderKeypairId();
        }
        if( id == null ) {
            throw new CloudException("No keypair was generated");
        }
        synchronized( testKeys ) {
            while( testKeys.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testKeys.put(label, id);
        }
        return id;
    }
}
