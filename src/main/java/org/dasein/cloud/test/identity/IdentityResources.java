package org.dasein.cloud.test.identity;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.identity.SSHKeypair;
import org.dasein.cloud.identity.ShellKeySupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/18/13 10:16 AM</p>
 *
 * @author George Reese
 */
public class IdentityResources {
    static private final Logger logger = Logger.getLogger(IdentityResources.class);

    private CloudProvider   provider;
    private TreeSet<String> provisionedKeys = new TreeSet<String>();
    private String          testKeyId;

    public IdentityResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    public void close() {
        try {
            IdentityServices identityServices = provider.getIdentityServices();

            if( identityServices != null ) {
                ShellKeySupport keySupport = identityServices.getShellKeySupport();

                if( keySupport != null ) {
                    for( String id : provisionedKeys ) {
                        try {
                            keySupport.deleteKeypair(id);
                        }
                        catch( Throwable ignore ) {
                            // ignore
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

    public @Nullable String getTestKeypairId() {
        return testKeyId;
    }

    public void init() {
        IdentityServices identityServices = provider.getIdentityServices();

        if( identityServices != null ) {
            ShellKeySupport keySupport = identityServices.getShellKeySupport();

            try {
                if( keySupport != null && keySupport.isSubscribed() ) {
                    Iterator<SSHKeypair> keypairs = keySupport.list().iterator();

                    if( keypairs.hasNext() ) {
                        testKeyId = keypairs.next().getProviderKeypairId();
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
    }

    public @Nonnull String provision(@Nonnull ShellKeySupport support, @Nonnull String namePrefix) throws CloudException, InternalException {
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
        provisionedKeys.add(id);
        return id;
    }
}
