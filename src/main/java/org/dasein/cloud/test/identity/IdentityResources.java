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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
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

    public void init(boolean stateful) {
        IdentityServices identityServices = provider.getIdentityServices();

        if( identityServices != null ) {
            ShellKeySupport keySupport = identityServices.getShellKeySupport();

            try {
                if( keySupport != null && keySupport.isSubscribed() ) {
                    if( stateful ) {
                        testKeyId = provision(keySupport, "dsnkp");
                    }
                    else {
                        Iterator<SSHKeypair> keypairs = keySupport.list().iterator();

                        if( keypairs.hasNext() ) {
                            testKeyId = keypairs.next().getProviderKeypairId();
                        }
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

            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

                generator.initialize(2048);
                publicKey = new String(encodePublicKey((RSAPublicKey)generator.genKeyPair().getPublic()), "utf-8");
            }
            catch( Throwable ignore ) {
                // ignore
            }
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

    // found this bit on StackOverflow: http://stackoverflow.com/questions/3706177/how-to-generate-ssh-compatible-id-rsa-pub-from-java
    public byte[] encodePublicKey(RSAPublicKey key) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        /* encode the "ssh-rsa" string */
        byte[] sshrsa = new byte[] {0, 0, 0, 7, 's', 's', 'h', '-', 'r', 's', 'a'};
        out.write(sshrsa);
        /* Encode the public exponent */
        BigInteger e = key.getPublicExponent();
        byte[] data = e.toByteArray();
        encodeUInt32(data.length, out);
        out.write(data);
        /* Encode the modulus */
        BigInteger m = key.getModulus();
        data = m.toByteArray();
        encodeUInt32(data.length, out);
        out.write(data);
        return out.toByteArray();
    }

    public void encodeUInt32(int value, OutputStream out) throws IOException  {
        byte[] tmp = new byte[4];
        tmp[0] = (byte)((value >>> 24) & 0xff);
        tmp[1] = (byte)((value >>> 16) & 0xff);
        tmp[2] = (byte)((value >>> 8) & 0xff);
        tmp[3] = (byte)(value & 0xff);
        out.write(tmp);
    }
}
