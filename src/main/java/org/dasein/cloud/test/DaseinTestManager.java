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

package org.dasein.cloud.test;

import org.apache.log4j.Logger;
import org.dasein.cloud.*;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.network.*;
import org.dasein.cloud.platform.DatabaseEngine;
import org.dasein.cloud.storage.Blob;
import org.dasein.cloud.test.ci.CIResources;
import org.dasein.cloud.test.compute.ComputeResources;
import org.dasein.cloud.test.identity.IdentityResources;
import org.dasein.cloud.test.network.NetworkResources;
import org.dasein.cloud.test.platform.PlatformResources;
import org.dasein.cloud.test.storage.StorageResources;
import org.dasein.cloud.util.APITrace;
import org.dasein.util.CalendarWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.security.Provider;
import java.util.*;

/**
 * Consolidates and manages cloud resources shared across many different tests.
 * <p>Created by George Reese: 2/17/13 3:23 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @version 2013.07 Added MQ cloud services support (issue #6)
 * @since 2013.04
 */
public class DaseinTestManager {
    static public final String STATEFUL  = "stateful";
    static public final String STATELESS = "stateless";
    static public final String REMOVED   = "removed";

    static private HashMap<String,Integer> apiAudit = new HashMap<String, Integer>();

    static private CIResources       ciResources;
    static private ComputeResources  computeResources;
    static private TreeSet<String>   exclusions;
    static private IdentityResources identityResources;
    static private NetworkResources  networkResources;
    static private PlatformResources platformResources;
    static private StorageResources  storageResources;

    static private TreeSet<String>   inclusions;

    static private int  skipCount;
    static private int  testCount;
    static private long testStart;

    static public @Nonnull CloudProvider constructProvider() {
        return constructProvider(null, null, null);
    }

    static public @Nonnull CloudProvider constructProvider(@Nullable String overrideAccount, @Nullable String overrideShared, @Nullable String overrideSecret) {
        String cname = getSystemProperty("providerClass");
        if( cname == null ) {
            throw new RuntimeException("Provider class name (env.providerClass) is not set, make sure to specify it manually or via a Maven profile");
        }

        CloudProvider provider = null;

        try{
            String prop, account = "", cloudName = "", endpoint = "", regionId = "", providerName = "", userName = "";

            prop = overrideAccount == null ? System.getProperty("accountNumber") : overrideAccount;
            if( prop != null ) {
                account = prop;
            }
            prop = System.getProperty("cloudName");
            if( prop != null ) {
                cloudName = prop;
            }
            prop = System.getProperty("endpoint");
            if( prop != null ) {
                endpoint = prop;
            }
            prop = System.getProperty("providerName");
            if( prop != null ) {
                providerName = prop;
            }
            prop = System.getProperty("regionId");
            if( prop != null ) {
                regionId = prop;
            }

            Cloud cloud = Cloud.register(providerName, cloudName, endpoint, (Class<? extends CloudProvider>) Class.forName(cname));

            ContextRequirements requirements = cloud.buildProvider().getContextRequirements();
            List<ContextRequirements.Field> fields = requirements.getConfigurableValues();
            List<ProviderContext.Value> values = new ArrayList<ProviderContext.Value>(fields.size());

            for(ContextRequirements.Field f : fields ) {
                if( f.type.equals(ContextRequirements.FieldType.TOKEN) ) {
                    String token = System.getProperty(f.name);
                    // use either shared or secret override, doesn't really matter
                    if( overrideShared != null || overrideSecret != null ) {
                        token = overrideShared == null ? overrideSecret : overrideShared;
                    }
                    values.add(ProviderContext.Value.parseValue(f, token));
                }
                else if( f.type.equals(ContextRequirements.FieldType.KEYPAIR) ) {
                    String shared = overrideShared == null ? System.getProperty(f.name + "Shared") : overrideShared;
                    String secret = overrideSecret == null ? System.getProperty(f.name + "Secret") : overrideSecret;
                    if( shared != null || secret != null ) {

                        //I would rather not have this but its the only way to pass in the binary file from a path
                        boolean p12 = false;
                        byte[] p12Bytes = null;
                        if(f.name.contains("p12")){
                            String p12Path = shared;
                            File file = new File(p12Path);
                            p12Bytes = new byte[(int) file.length()];
                            InputStream ios = null;
                            try {
                                ios = new FileInputStream(file);
                                if ( ios.read(p12Bytes) == -1 ) {
                                    throw new IOException("EOF reached while trying to read p12 certificate");
                                }
                                p12 = true;
                            }
                            catch(IOException ex){
                                //Bummer
                            }
                            finally {
                                try {
                                    if ( ios != null )
                                        ios.close();
                                } catch ( IOException e) {}
                            }
                        }
                        if(p12) values.add(new ProviderContext.Value<byte[][]>("p12Certificate", new byte[][] { p12Bytes, secret.getBytes() }));
                        else values.add(ProviderContext.Value.parseValue(f, shared, secret));
                    } else {
                        String error = String.format("Keypair fields are not set up correctly: " +
                                "%sShared = %s, %sSecret = %s. Check the Maven profile and pom.xml.",
                                f.name, shared, f.name, secret);
                        Logger logger = Logger.getLogger(DaseinTestManager.class);
                        logger.fatal(error);
                        throw new RuntimeException(error);
                    }
                }
                else {
                    String value = System.getProperty(f.name);
                    if( value != null && value.trim().length() > 0 ) {
                        values.add(ProviderContext.Value.parseValue(f, value));
                    } else if( f.required ) {
                        String error = String.format("%s field is missing, but declared as REQUIRED. " +
                                        "Check the Maven profile and pom.xml.",
                                f.name);
                        Logger logger = Logger.getLogger(DaseinTestManager.class);
                        logger.fatal(error);
                        throw new RuntimeException(error);
                    }
                }
            }

            ProviderContext ctx = cloud.createContext(account, regionId, values.toArray(new ProviderContext.Value[0]));
            provider = ctx.connect();
        }
        catch( ClassNotFoundException e ) {
            throw new RuntimeException("No such class: " + e.getMessage());
        }
        catch( IllegalAccessException e ) {

        }
        catch( InstantiationException e) {

        }
        catch( UnsupportedEncodingException e ) {

        }
        catch( InternalException e ) {

        }
        catch( CloudException e ) {

        }
        return provider;

        /*
        ProviderContext ctx = new ProviderContext();

        try {
            String prop;

            prop = overrideAccount == null ? System.getProperty("accountNumber") : overrideAccount;
            if( prop != null ) {
                ctx.setAccountNumber(prop);
            }
            prop = overrideShared == null ? System.getProperty("accessPublic") : overrideShared;
            if( prop != null ) {
                ctx.setAccessPublic(prop.getBytes("utf-8"));
            }
            prop = overrideSecret == null ? System.getProperty("accessPrivate") : overrideSecret;
            if( prop != null ) {
                ctx.setAccessPrivate(prop.getBytes("utf-8"));
            }
            prop = System.getProperty("x509CertFile");
            if( prop != null ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(prop)));
                StringBuilder str = new StringBuilder();
                String line;

                while( (line = reader.readLine()) != null ) {
                    str.append(line);
                    str.append("\n");
                }
                ctx.setX509Cert(str.toString().getBytes("utf-8"));
            }
            prop = System.getProperty("x509KeyFile");
            if( prop != null ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(prop)));
                StringBuilder str = new StringBuilder();
                String line;

                while( (line = reader.readLine()) != null ) {
                    str.append(line);
                    str.append("\n");
                }
                ctx.setX509Key(str.toString().getBytes("utf-8"));
            }
            prop = System.getProperty("endpoint");
            if( prop != null ) {
                ctx.setEndpoint(prop);
            }
            prop= System.getProperty("cloudName");
            if( prop != null ) {
                ctx.setCloudName(prop);
            }
            prop = System.getProperty("providerName");
            if( prop != null ) {
                ctx.setProviderName(prop);
            }
            prop = System.getProperty("regionId");
            if( prop != null ) {
                ctx.setRegionId(prop);
            }
            prop = System.getProperty("p12Certificate");
            if(prop != null){
                InputStream inputStream = new FileInputStream(prop);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                byte[] data = new byte[4096];
                int count = inputStream.read(data);
                while(count != -1) {
                    dos.write(data, 0, count);
                    count = inputStream.read(data);
                }
                ctx.setAccessPrivate(baos.toByteArray());
            }
            prop = System.getProperty("customProperties");
            if( prop != null ) {
                JSONObject json = new JSONObject(prop);
                String[] names = JSONObject.getNames(json);

                if( names != null ) {
                    Properties properties = new Properties();

                    for( String name : names ) {
                        properties.put(name, json.getString(name));
                    }
                    ctx.setCustomProperties(properties);
                }
            }
        }
        catch( UnsupportedEncodingException e ) {
            throw new RuntimeException("UTF-8 unsupported: " + e.getMessage());
        }
        catch( FileNotFoundException e ) {
            throw new RuntimeException("No such file: " + e.getMessage());
        }
        catch( IOException e ) {
            throw new RuntimeException("Failed to read file: " + e.getMessage());
        }
        catch( JSONException e ) {
            throw new RuntimeException("Failed to understand custom properties JSON: " + e.getMessage());
        }
        provider.connect(ctx);
        return provider;*/
    }

    static public @Nullable ComputeResources getComputeResources() {
        return computeResources;
    }

    static public @Nullable String getDefaultDataCenterId(boolean stateless) {
        return (computeResources == null ? null : computeResources.getTestDataCenterId(stateless));
    }

    static public @Nullable IdentityResources getIdentityResources() {
        return identityResources;
    }

    static public @Nullable NetworkResources getNetworkResources() {
        return networkResources;
    }

    static public @Nullable PlatformResources getPlatformResources() {
        return platformResources;
    }

    static public @Nullable StorageResources getStorageResources() {
        return storageResources;
    }

    static public void init() {
        Logger logger = Logger.getLogger(DaseinTestManager.class);

        logger.info("BEGIN Test Initialization ------------------------------------------------------------------------------");
        try {
            testStart = System.currentTimeMillis();

            CloudProvider cloudProvider = constructProvider();
            storageResources = new StorageResources(cloudProvider);
            platformResources = new PlatformResources(cloudProvider);
            networkResources = new NetworkResources(cloudProvider);
            identityResources = new IdentityResources(cloudProvider);
            ciResources = new CIResources(cloudProvider);
            computeResources = new ComputeResources(cloudProvider);

            computeResources.init();

            String prop = System.getProperty("dasein.inclusions");

            if( prop != null && !prop.equals("") ) {
                inclusions = new TreeSet<String>();
                if( prop.contains(",") ) {
                    for( String which : prop.split(",") ) {
                        inclusions.add(which.toLowerCase());
                    }
                }
                else {
                    inclusions.add(prop.toLowerCase());
                }
            }
            prop = System.getProperty("dasein.exclusions");

            if( prop != null && !prop.equals("") ) {
                exclusions = new TreeSet<String>();
                if( prop.contains(",") ) {
                    for( String which : prop.split(",") ) {
                        exclusions.add(which.toLowerCase());
                    }
                }
                else {
                    exclusions.add(prop.toLowerCase());
                }
            }
            out(logger, null, "Included", (inclusions == null ? null : inclusions.toString()));
            out(logger, null, "Excluded", (exclusions == null ? null : exclusions.toString()));

            APITrace.report("Init");
            APITrace.reset();
        }
        finally {
            logger.info("END Test Initialization ------------------------------------------------------------------------------");
            logger.info("");
        }
    }

    static public void cleanUp() {
        Logger logger = Logger.getLogger(DaseinTestManager.class);
        int provisioned = 0;
        int cleaned = 0;

        logger.info("");
        logger.info("BEGIN Test Clean Up ------------------------------------------------------------------------------");
        try {
            APITrace.report("Clean Up");
            if( ciResources != null ) {
                int count = ciResources.close();

                out(logger, null, "CI Resources", String.valueOf(count));
                cleaned += count;
            }
            if( computeResources != null ) {
                int count = computeResources.close();

                out(logger, null, "Compute Resources", String.valueOf(count));
                cleaned += count;
            }
            if( networkResources != null ) {
                int count = networkResources.close();

                out(logger, null, "Network Resources", String.valueOf(count));
                cleaned += count;
            }
            if( identityResources != null ) {
                int count = identityResources.close();

                out(logger, null, "Identity Resources", String.valueOf(count));
                cleaned += count;
            }
            if( platformResources != null ) {
                int count = platformResources.close();

                out(logger, null, "Platform Resources", String.valueOf(count));
                cleaned += count;
            }
            if( storageResources != null ) {
                int count = storageResources.close();

                out(logger, null, "Storage Resources", String.valueOf(count));
                cleaned += count;
            }
        }
        finally {
            logger.info("END Test Clean Up ------------------------------------------------------------------------------");
            logger.info("");
        }
        long duration = System.currentTimeMillis() - testStart;
        int minutes = (int)(duration/ CalendarWrapper.MINUTE);
        float seconds = ((float)(duration%CalendarWrapper.MINUTE))/1000f;
        logger.info("");
        logger.info("All Tests Complete ------------------------------------------------------------------------------");
        logger.info("--------------- API Log ---------------");
        int total = 0;

        for( Map.Entry<String,Integer> entry : apiAudit.entrySet() ) {
            out(logger, null, "---> " + entry.getKey(), String.valueOf(entry.getValue()));
            total += entry.getValue();
        }
        out(logger, null, "---> Total Calls", String.valueOf(total));
        logger.info("");

        logger.info("----------- Provisioning Log ----------");
        if( computeResources != null ) {
            provisioned += computeResources.report();
        }
        if( ciResources != null ) {
            provisioned += ciResources.report();
        }
        if( identityResources != null ) {
            provisioned += identityResources.report();
        }
        if( networkResources != null ) {
            provisioned += networkResources.report();
        }
        if( platformResources != null ) {
            provisioned += platformResources.report();
        }
        if( storageResources != null ) {
            provisioned += storageResources.report();
        }
        logger.info("");
        logger.info("--------------- Results ---------------");
        out(logger, null, "Tests", String.valueOf(testCount));
        out(logger, null, "Skipped", String.valueOf(skipCount));
        out(logger, null, "Run", String.valueOf(testCount - skipCount));
        out(logger, null, "Resources Provisioned", String.valueOf(provisioned));
        out(logger, null, "Resources De-provisioned", String.valueOf(cleaned));
        out(logger, null, "Duration", minutes + " minutes " + seconds + " seconds");
        logger.info("-------------------------------------------------------------------------------------------------");
    }

    static public void out(@Nonnull Logger logger, @Nullable String prefix, @Nonnull String key, @Nullable String value) {
        StringBuilder str = new StringBuilder();

        if( key.length() > 36 ) {
            str.append(key.substring(0, 36)).append(": ");
        }
        else {
            str.append(key).append(": ");
            while( str.length() < 38 ) {
                str.append(" ");
            }
        }
        if( prefix == null ) {
            logger.info(str.toString() + value);
        }
        else {
            logger.info( prefix + str.toString() + value);
        }
    }


    private Logger                  logger;
    private String                  name;
    private String                  prefix;
    private CloudProvider           provider;
    private long                    startTimestamp;
    private String                  suite;
    private String                  userName = "";

    public DaseinTestManager(@Nonnull Class<?> testClass) {
        logger = Logger.getLogger(testClass);
        suite = testClass.getSimpleName();
        provider = constructProvider();
        changePrefix();

        String prop = System.getProperty("user.name");
        if( prop != null ) {
            userName = prop;
        }
    }

    public void begin(@Nonnull String name) {
        this.name = name;
        APITrace.report("Setup");
        APITrace.reset();
        changePrefix();
        startTimestamp = System.currentTimeMillis();
        testCount++;
        out("");
        out(">>> BEGIN ---------------------------------------------------------------------------------------------->>>");
    }

    private void changePrefix() {
        StringBuilder str = new StringBuilder();
        String s;

        if( suite.endsWith("Test") ) {
            s = suite.substring(0, suite.length()-4);
        }
        else if( suite.endsWith("Tests") ) {
            s = suite.substring(0, suite.length()-5);
        }
        else {
            s = suite;
        }
        str.append(provider.getProviderName()).append("/").append(provider.getCloudName()).append(".").append(s);
        if( name != null ) {
            str.append(".").append(name);
        }
        if( str.length() > 44 ) {
            prefix = str.substring(str.length()-44) + "> ";
        }
        else {
            str.append("> ");
            while( str.length() < 46 ) {
                str.append(" ");
            }
            prefix = str.toString();
        }
    }

    public void close() {
        getProvider().close();
    }

    public void end() {
        String[] calls = APITrace.listApis(provider.getProviderName(), provider.getCloudName());

        if( calls.length > 0 ) {
            out("---------- API Log ----------");
            int total = 0;

            for( String call : calls ) {
                int count = (int)APITrace.getAPICountAcrossAccounts(provider.getProviderName(), provider.getCloudName(), call);

                if( apiAudit.containsKey(call) ) {
                    apiAudit.put(call, count + apiAudit.get(call));
                }
                else {
                    apiAudit.put(call, count);
                }
                out("---> " + call, count);
                total += count;
            }
            out("---> Total Calls", total);
        }
        out("Duration", (((float) (System.currentTimeMillis() - startTimestamp)) / 1000f) + " seconds");
        out("<<< END   ----------------------------------------------------------------------------------------------<<<");
        out("");
        APITrace.report(prefix);
        APITrace.reset();
        name = null;
        changePrefix();
    }

    public @Nonnull ProviderContext getContext() {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new RuntimeException("Provider context went away");
        }
        return ctx;
    }

    public @Nullable String getName() {
        return name;
    }

    public @Nullable String getTestDataCenterId(boolean stateless) {
        return (computeResources == null ? null : computeResources.getTestDataCenterId(stateless));
    }

    public @Nullable String getTestAnyFirewallId(@Nonnull String label, boolean provisionIfNull) {
        NetworkServices services = provider.getNetworkServices();

        if( services != null ) {
            FirewallSupport support = services.getFirewallSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    if( support.getCapabilities().supportsFirewallCreation(false) ) {
                        return getTestGeneralFirewallId(label, provisionIfNull);
                    }
                    else if( support.getCapabilities().supportsFirewallCreation(true) ) {
                        return getTestVLANFirewallId(DaseinTestManager.REMOVED, true, null);
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nullable Blob getTestBucket(@Nonnull String label, boolean root, boolean provisionIfNull) {
        if( root ) {
            return (storageResources == null ? null : storageResources.getTestRootBucket(label, provisionIfNull, null));
        }
        else {
            return (storageResources == null ? null : storageResources.getTestChildBucket(label, provisionIfNull, null, null));
        }
    }

    public @Nullable String getTestDistributionId(@Nonnull String label, boolean provisionIfNull, @Nullable String origin) {
        return (platformResources == null ? null : platformResources.getTestDistributionId(label, provisionIfNull, origin));
    }

    public @Nullable String getTestGeneralFirewallId(@Nonnull String label, boolean provisionIfNull) {
        return (networkResources == null ? null : networkResources.getTestFirewallId(label, provisionIfNull, null));
    }

    public @Nullable String getTestGroupId(@Nonnull String label, boolean provisionIfNull) {
        return (identityResources == null ? null : identityResources.getTestGroupId(label, provisionIfNull));
    }

    public @Nullable String getTestImageId(@Nonnull String label, boolean provisionIfNull) {
        return (computeResources == null ? null : computeResources.getTestImageId(label, provisionIfNull));
    }

    public @Nullable String getTestKeypairId(@Nonnull String label, boolean provisionIfNull) {
        return (identityResources == null ? null : identityResources.getTestKeypairId(label, provisionIfNull));
    }

    public @Nullable String getTestLoadBalancerId(@Nonnull String label, @Nonnull String lbNamePrefix, boolean provisionIfNull, boolean withHealthCheck) {
        return (networkResources == null ? null : networkResources.getTestLoadBalancerId(label, lbNamePrefix, provisionIfNull, withHealthCheck));
    }

    public @Nullable String getTestLoadBalancerId(@Nonnull String label, @Nonnull String lbNamePrefix, boolean provisionIfNull) {
    	return (networkResources == null ? null : networkResources.getTestLoadBalancerId(label, lbNamePrefix, provisionIfNull, false));
    }

    public @Nullable String getTestSSLCertificateName(@Nonnull String label, boolean provisionIfNull) {
        return (networkResources == null ? null : networkResources.getTestSSLCertificateName(label, provisionIfNull));
    }

    public @Nullable String getTestNetworkFirewallId(@Nonnull String label, boolean provisionIfNull, @Nullable String inVlanId) {
        return (networkResources == null ? null : networkResources.getTestNetworkFirewallId(label, provisionIfNull, inVlanId));
    }

    public @Nullable Blob getTestObject(@Nonnull String label, boolean root, boolean provisionIfNull) {
        if( root ) {
            return (storageResources == null ? null : storageResources.getTestRootObject(label, provisionIfNull, null));
        }
        else {
            return (storageResources == null ? null : storageResources.getTestChildObject(label, provisionIfNull, null, null));
        }
    }

    public @Nullable String getTestQueueId(@Nonnull String label, boolean provisionIfNull) {
        return (platformResources == null ? null : platformResources.getTestQueueId(label, provisionIfNull));
    }

    public @Nullable String getTestRDBMSId(@Nonnull String label, boolean provisionIfNull, @Nullable DatabaseEngine engine) {
        return (platformResources == null ? null : platformResources.getTestRDBMSId(label, provisionIfNull, engine));
    }

    public @Nullable String getTestSnapshotId(@Nonnull String label, boolean provisionIfNull) {
        return (computeResources == null ? null : computeResources.getTestSnapshotId(label, provisionIfNull));
    }

    public @Nullable String getTestStaticIpId(@Nonnull String label, boolean provisionIfNull, @Nullable IPVersion version, boolean forVLAN, @Nullable String vlanId) {
        return networkResources == null ? null : networkResources.getTestStaticIpId(label, provisionIfNull, version, forVLAN, vlanId);
    }

    public @Nullable String getTestSubnetId(@Nonnull String label, boolean provisionIfNull, @Nullable String vlanId, @Nullable String preferredDataCenterId) {
        return (networkResources == null ? null : networkResources.getTestSubnetId(label, provisionIfNull, vlanId, preferredDataCenterId));
    }

    public @Nullable String getTestInternetGatewayId(@Nonnull String label, boolean provisionIfNull, @Nullable String vlanId, @Nullable String preferredDataCenterId) {
      return (networkResources == null ? null : networkResources.getTestInternetGatewayId(label, provisionIfNull, vlanId, preferredDataCenterId));
    }

    public @Nullable String getTestTopicId(@Nonnull String label, boolean provisionIfNull) {
        return (platformResources == null ? null : platformResources.getTestTopicId(label, provisionIfNull));
    }

    public @Nullable String getTestTopologyId(@Nonnull String label, boolean provisionIfNull) {
        return (ciResources == null ? null : ciResources.getTestTopologyId(label, provisionIfNull));
    }

    public @Nullable String getTestUserId(@Nonnull String label, boolean provisionIfNull, @Nullable String preferredGroupId) {
        return (identityResources == null ? null : identityResources.getTestUserId(label, provisionIfNull, preferredGroupId));
    }

    public @Nullable String getTestVLANFirewallId(@Nonnull String label, boolean provisionIfNull, @Nullable String inVlanId) {
        if( inVlanId == null ) {
            if( label.equals(DaseinTestManager.STATELESS) ) {
                inVlanId = getTestVLANId(DaseinTestManager.STATELESS, false, null);
            }
            else {
                inVlanId = getTestVLANId(DaseinTestManager.STATEFUL, true, null);
            }
            if( inVlanId == null ) {
                return null;
            }
        }
        String id = (networkResources == null ? null : networkResources.getTestFirewallId(label, provisionIfNull, inVlanId));

        if( id != null ) {
            try {
                @SuppressWarnings("ConstantConditions") Firewall firewall = provider.getNetworkServices().getFirewallSupport().getFirewall(id);

                if( firewall == null ) {
                    return null;
                }
                if( !inVlanId.equals(firewall.getProviderVlanId()) ) {
                    return getTestVLANFirewallId(label + "a", provisionIfNull, inVlanId);
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return id;
    }

    public @Nullable String getTestVLANId(@Nonnull String label, boolean provisionIfNull, @Nullable String preferredDataCenterId) {
        return (networkResources == null ? null : networkResources.getTestVLANId(label, provisionIfNull, preferredDataCenterId));
    }

    public @Nullable String getTestRoutingTableId(@Nonnull String label, boolean provisionIfNull, @Nullable String vlanId, @Nullable String preferredDataCenterId) {
      return (networkResources == null ? null : networkResources.getTestRoutingTableId(label, provisionIfNull, vlanId, preferredDataCenterId));
    }

    public @Nullable String getTestVMId(@Nonnull String label, @Nullable VmState desiredState, boolean provisionIfNull, @Nullable String preferredDataCenterId) {
    	return getTestVMId(label, "dsnvm", desiredState, provisionIfNull, preferredDataCenterId);
    }
    
    public @Nullable String getTestVMId(@Nonnull String label, @Nonnull String vmName, @Nullable VmState desiredState, boolean provisionIfNull, @Nullable String preferredDataCenterId) {
        if( computeResources == null ) {
            return null;
        }
        return computeResources.getTestVmId(label, vmName, desiredState, provisionIfNull, preferredDataCenterId);
    }

    public @Nullable String getTestVLANVMId(@Nonnull String label, @Nullable VmState desiredState, @Nullable String vlanId, boolean provisionIfNull, @Nullable String preferredDataCenterId) {
      if( computeResources == null ) {
        return null;
      }
      return computeResources.getTestVLANVmId(label, desiredState, vlanId, provisionIfNull, preferredDataCenterId);
    }

    public @Nullable String getTestVMProductId() {
        return (computeResources == null ? null : computeResources.getTestVMProductId());
    }

    public @Nullable String getTestVolumeId(@Nonnull String label, boolean provisionIfNull, @Nullable VolumeFormat preferredFormat, @Nullable String preferredDataCenterId) {
        if( computeResources == null ) {
            return null;
        }
        return computeResources.getTestVolumeId(label, provisionIfNull, preferredFormat, preferredDataCenterId);
    }

    public @Nullable String getTestVolumeProductId() {
        return (computeResources == null ? null : computeResources.getTestVolumeProductId());
    }

    public @Nullable String getTestZoneId(@Nonnull String label, boolean provisionIfNull) {
        return (networkResources == null ? null : networkResources.getTestZoneId(label, provisionIfNull));
    }

    public @Nullable String getTestVpnId(@Nonnull String label, boolean provisionIfNull, @Nullable String preferredDataCenterId) {
        return (networkResources == null ? null : networkResources.getTestVpnId(label, provisionIfNull, preferredDataCenterId));
    }

    public @Nonnull CloudProvider getProvider() {
        return provider;
    }

    /**
     * Checks to see if the test currently being executed is supposed to be skipped.
     * A test is assumed to be run unless there are a list of inclusions and the test is not
     * in the list or there is a list of exclusions and the test is in the list. If there are
     * inclusions and exclusions, any conflict is resolved in favor of executing the test.
     * Exclusions and inclusions are set as the {@link System} properties dasein.inclusions and
     * dasein.exclusions. You may specify an entire suite (e.g. "StatelessVMTests") or a specific
     * test (e.g. "StatelessVMTests.listVirtualMachines"). You may also specify multiple tests:
     * <pre>
     *     -Ddasein.inclusions=StatelessVMTests.listVirtualMachines,StatelessDCTests
     * </pre>
     * This will execute only the listVirtualMachines test from StatelessVMTests and all StatelessDCTests. All other
     * tests will be skipped.
     * @return true if the current test is to be skipped
     */
    public boolean isTestSkipped() {
        if( inclusions == null && exclusions == null ) {
            return false;
        }
        String s = suite.toLowerCase();
        String t = (name == null ? null : name.toLowerCase());

        Boolean suiteIncluded = null;
        Boolean testIncluded = null;

        if( inclusions != null ) {
            if( inclusions.contains(s) ) {
                suiteIncluded = true;
            }
            if( t != null && inclusions.contains(s + "." + t) ) {
                testIncluded = true;
            }
            if( suiteIncluded == null && testIncluded == null ) {
                skip();
                return true;
            }
        }
        if( exclusions != null ) {
            if( t != null && exclusions.contains(s + "." + t) ) {
                if( testIncluded == null || !testIncluded ) {
                    skip();
                    return true;
                }
                return false; // conflict goes to not skipping
            }
            if( exclusions.contains(s) ) {
                if( testIncluded != null && testIncluded ) {
                    return false; // specific test inclusion overrides suite exclusion
                }
                // suite included must be true to get this far
                if( suiteIncluded != null && suiteIncluded ) {
                    return false; // conflict goes to skipping
                }
            }
        }
        return false;
    }

    public void ok(@Nonnull String message) {
        logger.info(prefix + message + " (OK)");
    }

    public void out(@Nonnull String message) {
        logger.info(prefix + message);
    }

    public void out(@Nonnull String key, boolean value) {
        out(key, String.valueOf(value));
    }

    public void out(@Nonnull String key, int value) {
        out(key, String.valueOf(value));
    }

    public void out(@Nonnull String key, long value) {
        out(key, String.valueOf(value));
    }

    public void out(@Nonnull String key, double value) {
        out(key, String.valueOf(value));
    }

    public void out(@Nonnull String key, float value) {
        out(key, String.valueOf(value));
    }

    public void out(@Nonnull String key, Object value) {
        out(key, value == null ? "null" : value.toString());
    }

    public void out(@Nonnull String key, @Nullable String value) {
        out(logger, prefix, key, value);
    }

    public void skip() {
        skipCount++;
        out("SKIPPING");
    }

    public void warn(@Nonnull String message) {
        logger.warn(prefix + "WARNING: " + message);
    }
    
    public String getUserName() {
    	return userName;
    }

    /**
     * Get environment property
     * @param key the key to regrieve the property for
     * @return environment property, null if missing or empty
     */
    public static @Nullable String getSystemProperty(@Nonnull String key) {
        String value = System.getProperty(key);
        if( value != null && value.trim().isEmpty() ) {
            return null;
        }
        return value;
    }

    public static boolean supportsHttps(@Nonnull LoadBalancerSupport lbs) throws CloudException, InternalException {
        boolean sslSupported = false;
        for( LbProtocol proto : lbs.getCapabilities().listSupportedProtocols() ) {
            if( LbProtocol.HTTPS.equals(proto) ) {
                sslSupported = true;
            }
        }
        return sslSupported;
    }

}
