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

package org.dasein.cloud.test.identity;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.identity.CloudGroup;
import org.dasein.cloud.identity.CloudUser;
import org.dasein.cloud.identity.IdentityAndAccessSupport;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.identity.SSHKeypair;
import org.dasein.cloud.identity.ShellKeySupport;
import org.dasein.cloud.test.DaseinTestManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
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

    private final HashMap<String,String> testGroups = new HashMap<String, String>();
    private final HashMap<String,String> testKeys   = new HashMap<String, String>();
    private final HashMap<String,String> testUsers  = new HashMap<String, String>();
    private CloudProvider   provider;

    public IdentityResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    private void write(byte[] str, OutputStream os) throws IOException {
        for (int shift = 24; shift >= 0; shift -= 8)
            os.write((str.length >>> shift) & 0xFF);
        os.write(str);
    }

    /**
     * @link http://stackoverflow.com/a/14582408/211197
     * @return Encoded generated public key
     */
    private @Nullable String generateKey() {
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.genKeyPair();
            RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
            ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byteOs);
            dos.writeInt("ssh-rsa".getBytes().length);
            dos.write("ssh-rsa".getBytes());
            dos.writeInt(rsaPublicKey.getPublicExponent().toByteArray().length);
            dos.write(rsaPublicKey.getPublicExponent().toByteArray());
            dos.writeInt(rsaPublicKey.getModulus().toByteArray().length);
            dos.write(rsaPublicKey.getModulus().toByteArray());
            String publicKeyEncoded = new String(
                    Base64.encodeBase64(byteOs.toByteArray()));
            return "ssh-rsa " + publicKeyEncoded + " dasein";
        }
        catch( Throwable e ) {
            return null;
        }
    }

    public int close() {
        int count = 0;

        try {
            IdentityServices identityServices = provider.getIdentityServices();

            if( identityServices != null ) {
                ShellKeySupport keySupport = identityServices.getShellKeySupport();

                if( keySupport != null ) {
                    for( Map.Entry<String,String> entry : testKeys.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                keySupport.deleteKeypair(entry.getValue());
                                count++;
                            }
                            catch( Throwable t ) {
                                logger.warn("Failed to de-provision test keypair " + entry.getValue() + ": " + t.getMessage());
                            }
                        }
                    }
                }
                IdentityAndAccessSupport iamSupport = identityServices.getIdentityAndAccessSupport();

                if( iamSupport != null ) {
                    for( Map.Entry<String,String> entry : testUsers.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                iamSupport.removeUser(entry.getValue());
                                count++;
                            }
                            catch( Throwable t ) {
                                logger.warn("Failed to de-provision test user " + entry.getValue() + ": " + t.getMessage());
                            }
                        }
                    }
                    for( Map.Entry<String,String> entry : testGroups.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                            try {
                                iamSupport.removeGroup(entry.getValue());
                                count++;
                            }
                            catch( Throwable t ) {
                                logger.warn("Failed to de-provision test group " + entry.getValue() + ": " + t.getMessage());
                            }
                        }
                    }
                }
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        return count;
    }

    public int report() {
        boolean header = false;
        int count = 0;

        testKeys.remove(DaseinTestManager.STATELESS);
        if( !testKeys.isEmpty() ) {
            logger.info("Provisioned Identity Resources:");
            header = true;
            count += testKeys.size();
            DaseinTestManager.out(logger, null, "---> SSH Keypairs", testKeys.size() + " " + testKeys);
        }
        testGroups.remove(DaseinTestManager.STATELESS);
        if( !testGroups.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Identity Resources:");
                header = true;
            }
            count += testGroups.size();
            DaseinTestManager.out(logger, null, "---> Groups", testGroups.size() + " " + testGroups);
        }
        testUsers.remove(DaseinTestManager.STATELESS);
        if( !testUsers.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Identity Resources:");
            }
            count+= testUsers.size();
            DaseinTestManager.out(logger, null, "---> Users", testUsers.size() + " " + testUsers);
        }
        return count;
    }

    public @Nullable String getTestGroupId(@Nonnull String label, boolean provisionIfNull) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testGroups.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessGroup();
        }
        String id = testGroups.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            IdentityServices services = provider.getIdentityServices();

            if( services != null ) {
                IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

                if( support != null ) {
                    try {
                        return provisionGroup(support, label, "dsngroup");
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestKeypairId(@Nonnull String label, boolean provisionIfNull) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testKeys.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
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
                        return provisionKeypair(support, label, "dsnkp");
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestUserId(@Nonnull String label, boolean provisionIfNull, @Nullable String groupToJoin) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testUsers.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessUser(groupToJoin);
        }
        String id = testUsers.get(label);

        if( id != null ) {

            if( groupToJoin != null ) {
                IdentityServices services = provider.getIdentityServices();

                if( services != null ) {
                    IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

                    if( support != null ) {
                        try { support.addUserToGroups(id, groupToJoin); }
                        catch( Throwable ignore ) { }
                    }
                }
            }
            return id;
        }
        if( provisionIfNull ) {
            IdentityServices services = provider.getIdentityServices();

            if( services != null ) {
                IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

                if( support != null ) {
                    try {
                        return provisionUser(support, label, "dsnuser", groupToJoin);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String findStatelessGroup() {
        IdentityServices services = provider.getIdentityServices();

        if( services != null ) {
            IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Iterator<CloudGroup> groups = support.listGroups(null).iterator();

                    if( groups.hasNext() ) {
                        String id = groups.next().getProviderGroupId();

                        testGroups.put(DaseinTestManager.STATELESS, id);
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

    public @Nullable String findStatelessUser(@Nullable String preferredGroupId) {
        IdentityServices services = provider.getIdentityServices();

        if( services != null ) {
            IdentityAndAccessSupport support = services.getIdentityAndAccessSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Iterator<CloudUser> users;

                    if( preferredGroupId != null ) {
                        users = support.listUsersInGroup(preferredGroupId).iterator();
                        if( users.hasNext() ) {
                            String id = users.next().getProviderUserId();

                            testUsers.put(DaseinTestManager.STATELESS, id);
                            return id;
                        }
                    }
                    users = support.listUsersInPath(null).iterator();
                    if( users.hasNext() ) {
                        String id = users.next().getProviderUserId();

                        testUsers.put(DaseinTestManager.STATELESS, id);
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

    public @Nonnull String provisionGroup(@Nonnull IdentityAndAccessSupport support, @Nonnull String label, @Nonnull String namePrefix) throws CloudException, InternalException {
        String id = support.createGroup(namePrefix + " " + System.currentTimeMillis(), "/dsntest", false).getProviderGroupId();

        if( id == null ) {
            throw new CloudException("No group was created");
        }
        synchronized( testGroups ) {
            while( testGroups.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testGroups.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionKeypair(@Nonnull ShellKeySupport support, @Nonnull String label, @Nonnull String namePrefix) throws CloudException, InternalException {
        String id = null;

        if( support.getCapabilities().identifyKeyImportRequirement().equals(Requirement.REQUIRED) ) {
            String publicKey = generateKey();
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

    public @Nonnull String provisionUser(@Nonnull IdentityAndAccessSupport support, @Nonnull String label, @Nonnull String namePrefix, @Nullable String ... preferredGroups) throws CloudException, InternalException {
        String id = support.createUser(namePrefix + (System.currentTimeMillis()%10000), "/dsntest", preferredGroups == null ? new String[0] : preferredGroups).getProviderUserId();

        if( id == null ) {
            throw new CloudException("No user was created");
        }
        synchronized( testUsers ) {
            while( testUsers.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testUsers.put(label, id);
        }
        return id;
    }
}
