package org.dasein.cloud.test;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.test.compute.ComputeResources;
import org.dasein.cloud.test.identity.IdentityResources;
import org.dasein.cloud.test.network.NetworkResources;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Consolidates and manages cloud resources shared across many different tests.
 * <p>Created by George Reese: 2/17/13 3:23 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class DaseinTestManager {
    static private ComputeResources  computeResources;
    static private String            defaultDataCenterId;
    static private IdentityResources identityResources;
    static private NetworkResources  networkResources;

    static public @Nonnull CloudProvider constructProvider() {
        String cname = System.getProperty("providerClass");
        CloudProvider provider;

        if( cname == null ) {
            throw new RuntimeException("Invalid class name for provider: " + cname);
        }
        try {
            provider = (CloudProvider)Class.forName(cname).newInstance();
        }
        catch( Exception e ) {
            throw new RuntimeException("Invalid class name " + cname + " for provider: " + e.getMessage());
        }
        ProviderContext ctx = new ProviderContext();

        try {
            String prop;

            prop = System.getProperty("accountNumber");
            if( prop != null ) {
                ctx.setAccountNumber(prop);
            }
            prop = System.getProperty("accessPublic");
            if( prop != null ) {
                ctx.setAccessPublic(prop.getBytes("utf-8"));
            }
            prop = System.getProperty("accessPrivate");
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
        return provider;
    }

    static public @Nullable ComputeResources getComputeResources() {
        return computeResources;
    }

    static public @Nullable String getDefaultDataCenterId() {
        return defaultDataCenterId;
    }

    static public @Nullable IdentityResources getIdentityResources() {
        return identityResources;
    }

    static public @Nullable NetworkResources getNetworkResources() {
        return networkResources;
    }

    static public void init(boolean stateful) {
        CloudProvider provider = constructProvider();

        networkResources = new NetworkResources(provider);
        networkResources.init(stateful);
        identityResources = new IdentityResources(provider);
        identityResources.init(stateful);
        computeResources = new ComputeResources(provider);
        defaultDataCenterId = computeResources.init(stateful);
    }

    static public void cleanUp() {
        computeResources.close();
    }

    private Logger logger;
    private String name;
    private String prefix;
    private CloudProvider provider;
    private String suite;

    public DaseinTestManager(@Nonnull Class<?> testClass) {
        logger = Logger.getLogger(testClass);
        suite = testClass.getSimpleName();
        provider = constructProvider();
        changePrefix();
    }

    public void begin(@Nonnull String name) {
        this.name = name;
        changePrefix();
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
        out("<<< END   ----------------------------------------------------------------------------------------------<<<");
        out("");
        name = null;
        changePrefix();
    }

    public void error(@Nonnull String message) {
        logger.error(prefix + " ERROR: " + message);
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

    public @Nonnull String getSuite() {
        return suite;
    }

    public @Nullable String getTestImageId() {
        return (computeResources == null ? null : computeResources.getTestImageId());
    }

    public @Nullable String getTestKeypairId() {
        return (identityResources == null ? null : identityResources.getTestKeypairId());
    }

    public @Nullable String getTestStaticIpId(boolean shared) {
        return networkResources == null ? null : networkResources.getTestStaticIpId(shared);
    }

    public @Nullable String getTestSubnetId(boolean shared) {
        return (networkResources == null ? null : networkResources.getTestSubnetId(shared));
    }

    public @Nullable String getTestVLANId(boolean shared) {
        return (networkResources == null ? null : networkResources.getTestVLANId(shared));
    }

    public @Nullable String getTestVMId(boolean shared, @Nullable VmState desiredState) {
        if( computeResources == null ) {
            return null;
        }
        return computeResources.getTestVMId(shared, desiredState);
    }

    public @Nullable String getTestVMProductId() {
        return (computeResources == null ? null : computeResources.getTestVMProductId());
    }

    public @Nullable String getTestVolumeId(boolean shared) {
        return (computeResources == null ? null : computeResources.getTestVolumeId(shared));
    }

    public @Nullable String getTestVolumeProductId() {
        return (computeResources == null ? null : computeResources.getTestVolumeProductId());
    }

    public @Nonnull CloudProvider getProvider() {
        return provider;
    }

    public boolean isTestSkipped() {
        // TODO: implement me
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
        StringBuilder str = new StringBuilder();

        if( key.length() > 31 ) {
            str.append(key.substring(0, 31)).append(": ");
        }
        else {
            str.append(key).append(": ");
            while( str.length() < 33 ) {
                str.append(" ");
            }
        }
        out( str.toString() + value);
    }

    public void skip() {
        out("SKIPPING");
    }

    public void warn(@Nonnull String message) {
        logger.warn(prefix + "WARNING: " + message);
    }
}
