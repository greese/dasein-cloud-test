package org.dasein.cloud.test.network;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallCreateOptions;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.SubnetCreateOptions;
import org.dasein.cloud.network.SubnetState;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.test.DaseinTestManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/18/13 10:48 AM</p>
 *
 * @author George Reese
 */
public class NetworkResources {
    static private final Logger logger = Logger.getLogger(NetworkResources.class);

    static private final Random random = new Random();

    private CloudProvider   provider;

    private final HashMap<String,String> testGeneralFirewalls = new HashMap<String, String>();
    private final HashMap<String,String> testStaticIps        = new HashMap<String, String>();
    private final HashMap<String,String> testSubnets          = new HashMap<String, String>();
    private final HashMap<String,String> testVLANs            = new HashMap<String, String>();
    private final HashMap<String,String> testVLANFirewalls    = new HashMap<String, String>();

    public NetworkResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    public void close() {
        try {
            NetworkServices networkServices = provider.getNetworkServices();

            if( networkServices != null ) {
                IpAddressSupport ipSupport = networkServices.getIpAddressSupport();

                if( ipSupport != null ) {
                    try {
                        for( Map.Entry<String,String> entry : testStaticIps.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                IpAddress addr = ipSupport.getIpAddress(entry.getValue());

                                try {
                                    if( addr != null ) {
                                        ipSupport.releaseFromServer(entry.getValue());
                                    }
                                    try { Thread.sleep(3000L); }
                                    catch( InterruptedException ignore ) { }
                                }
                                catch( Throwable ignore ) {
                                    // ignore
                                }
                                try {
                                    if( addr != null ) {
                                        ipSupport.releaseFromPool(entry.getValue());
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to deprovision static IP " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
                VLANSupport vlanSupport = networkServices.getVlanSupport();

                if( vlanSupport != null ) {
                    try {
                        for( Map.Entry<String,String> entry : testSubnets.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                try {
                                    Subnet s = vlanSupport.getSubnet(entry.getValue());

                                    if( s != null ) {
                                        vlanSupport.removeSubnet(entry.getValue());
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision subnet " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                    try {
                        for( Map.Entry<String,String> entry : testVLANs.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                try {
                                    for( Subnet subnet : vlanSupport.listSubnets(entry.getValue()) ) {
                                        try {
                                            vlanSupport.removeSubnet(subnet.getProviderSubnetId());
                                        }
                                        catch( Throwable t ) {
                                            logger.warn("Failed to de-provision VLAN subnet " + subnet.getProviderSubnetId() + " for " + entry.getValue() + " post-test: " + t.getMessage());
                                        }
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision VLAN subnets " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                                try {
                                    VLAN v = vlanSupport.getVlan(entry.getValue());

                                    if( v != null ) {
                                        vlanSupport.removeVlan(entry.getValue());
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision VLAN " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
                FirewallSupport firewallSupport = networkServices.getFirewallSupport();

                if( firewallSupport != null ) {
                    try {
                        for( Map.Entry<String,String> entry : testGeneralFirewalls.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                try {
                                    Firewall f = firewallSupport.getFirewall(entry.getValue());

                                    if( f != null ) {
                                        firewallSupport.delete(entry.getValue());
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision firewall " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }

                        for( Map.Entry<String,String> entry : testVLANFirewalls.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                try {
                                    Firewall f = firewallSupport.getFirewall(entry.getValue());

                                    if( f != null ) {
                                        firewallSupport.delete(entry.getValue());
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision firewall " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        provider.close();
    }

    private @Nullable String findStatelessFirewall(boolean inVlan) {
        NetworkServices networkServices = provider.getNetworkServices();

        if( networkServices != null ) {
            FirewallSupport support = networkServices.getFirewallSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Firewall defaultFirewall = null;

                    for( Firewall firewall : support.list() ) {
                        if( (firewall.getProviderVlanId() != null) == inVlan ) {
                            if( firewall.isActive() && firewall.isAvailable() ) {
                                String id = firewall.getProviderFirewallId();

                                if( id != null && support.getRules(id).iterator().hasNext() ) {
                                    if( inVlan ) {
                                        testVLANFirewalls.put(DaseinTestManager.STATELESS, id);
                                    }
                                    else {
                                        testGeneralFirewalls.put(DaseinTestManager.STATELESS, id);
                                    }
                                    return firewall.getProviderFirewallId();
                                }
                            }
                            if( defaultFirewall == null ) {
                                defaultFirewall = firewall;
                            }
                            else if( (firewall.isActive() && firewall.isAvailable()) && (!defaultFirewall.isActive() || !defaultFirewall.isAvailable()) ) {
                                defaultFirewall = firewall;
                            }
                        }
                    }
                    if( defaultFirewall != null ) {
                        String id = defaultFirewall.getProviderFirewallId();

                        if( id != null ) {
                            if( inVlan ) {
                                testVLANFirewalls.put(DaseinTestManager.STATELESS, id);
                            }
                            else {
                                testGeneralFirewalls.put(DaseinTestManager.STATELESS, id);
                            }
                        }
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

    private @Nullable String findStatelessIP() {
        NetworkServices networkServices = provider.getNetworkServices();

        if( networkServices != null ) {
            IpAddressSupport ipSupport = networkServices.getIpAddressSupport();

            try {
                if( ipSupport != null && ipSupport.isSubscribed() ) {
                    IpAddress defaultAddress = null;

                    for( IPVersion version : ipSupport.listSupportedIPVersions() ) {
                        for( IpAddress address : ipSupport.listIpPool(version, false) ) {
                            if( address.isAssigned() || defaultAddress == null ) {
                                defaultAddress = address;
                                if( defaultAddress.isAssigned() ) {
                                    break;
                                }
                            }
                        }
                        if( defaultAddress != null && defaultAddress.isAssigned() ) {
                            break;
                        }
                    }
                    if( defaultAddress != null ) {
                        String id = defaultAddress.getProviderIpAddressId();

                        testStaticIps.put(DaseinTestManager.STATELESS, id);
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

    private @Nullable String findStatelessVLAN() {
        NetworkServices networkServices = provider.getNetworkServices();

        if( networkServices != null ) {
            VLANSupport vlanSupport = networkServices.getVlanSupport();

            try {
                if( vlanSupport != null && vlanSupport.isSubscribed() ) {
                    VLAN defaultVlan = null;
                    Subnet defaultSubnet = null;

                    for( VLAN vlan : vlanSupport.listVlans() ) {
                        if( defaultVlan == null || VLANState.AVAILABLE.equals(vlan.getCurrentState()) ) {
                            Subnet foundSubnet = null;

                            if( !vlanSupport.getSubnetSupport().equals(Requirement.NONE) ) {
                                for( Subnet subnet : vlanSupport.listSubnets(vlan.getProviderVlanId()) ) {
                                    if( foundSubnet == null || SubnetState.AVAILABLE.equals(subnet.getCurrentState()) ) {
                                        foundSubnet = subnet;
                                        if( SubnetState.AVAILABLE.equals(subnet.getCurrentState()) ) {
                                            defaultVlan = vlan;
                                            defaultSubnet = foundSubnet;
                                            break;
                                        }
                                    }
                                }
                            }
                            if( defaultVlan == null ) {
                                defaultVlan = vlan;
                            }
                            if( VLANState.AVAILABLE.equals(vlan.getCurrentState()) && ((foundSubnet != null && SubnetState.AVAILABLE.equals(foundSubnet.getCurrentState())) || vlanSupport.getSubnetSupport().equals(Requirement.NONE)) ) {
                                break;
                            }
                        }
                    }
                    String id = null;

                    if( defaultVlan != null ) {
                        id =  defaultVlan.getProviderVlanId();
                        testVLANs.put(DaseinTestManager.STATELESS, id);
                    }
                    if( defaultSubnet != null ) {
                        testSubnets.put(DaseinTestManager.STATELESS, defaultSubnet.getProviderSubnetId());
                    }
                    return id;
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nullable String getTestFirewallId(@Nonnull String label, boolean provisionIfNull, @Nullable String vlanId) {
        HashMap<String,String> map = (vlanId == null ? testGeneralFirewalls : testVLANFirewalls);

        if( label.equalsIgnoreCase(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : map.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessFirewall(vlanId != null);
        }
        String id = map.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            NetworkServices services = provider.getNetworkServices();

            if( services != null ) {
                FirewallSupport support = services.getFirewallSupport();

                if( support != null ) {
                    try {
                        return provisionFirewall(label, vlanId);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestStaticIpId(@Nonnull String label, boolean provisionIfNull, @Nullable IPVersion version) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testStaticIps.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessIP();
        }
        String id = testStaticIps.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            NetworkServices services = provider.getNetworkServices();

            if( services != null ) {
                IpAddressSupport support = services.getIpAddressSupport();

                if( support != null ) {
                    try {
                        return provisionAddress(support, label, version);
                    }
                    catch( Throwable ignore ) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestSubnetId(@Nonnull String label, boolean provisionIfNull, @Nullable String vlanId, @Nullable String preferredDataCenterId) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testSubnets.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            findStatelessVLAN();
            return testSubnets.get(DaseinTestManager.STATELESS);
        }
        String id = testSubnets.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            NetworkServices services = provider.getNetworkServices();

            if( services != null ) {
                VLANSupport support = services.getVlanSupport();

                if( support != null ) {
                    try {
                        if( vlanId == null ) {
                            vlanId = getTestVLANId(DaseinTestManager.STATEFUL, true, preferredDataCenterId);
                            if( vlanId == null ) {
                                vlanId = getTestVLANId(DaseinTestManager.STATELESS, false, preferredDataCenterId);
                                if( vlanId == null ) {
                                    return null;
                                }
                            }
                        }
                        return provisionSubnet(support, label, vlanId, "dsnsub", preferredDataCenterId);
                    }
                    catch( Throwable ignore ) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestVLANId(@Nonnull String label, boolean provisionIfNull, @Nullable String preferredDataCenterId) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testVLANs.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessVLAN();
        }
        String id = testVLANs.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            NetworkServices services = provider.getNetworkServices();

            if( services != null ) {
                VLANSupport support = services.getVlanSupport();

                if( support != null ) {
                    try {
                        return provisionVLAN(support, label, "dsnnet", preferredDataCenterId);
                    }
                    catch( Throwable ignore ) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public @Nonnull String provisionAddress(@Nonnull IpAddressSupport support, @Nonnull String label, @Nullable IPVersion version) throws CloudException, InternalException {
        if( version == null ) {
            for( IPVersion v : support.listSupportedIPVersions() ) {
                if( support.isRequestable(v) ) {
                    version = v;
                    break;
                }
            }
        }
        if( version == null ) {
            throw new CloudException("No IP version is requestable");
        }
        String id = support.request(version);

        synchronized( testStaticIps ) {
            while( testStaticIps.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testStaticIps.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionFirewall(@Nonnull String label, @Nullable String vlanId) throws CloudException, InternalException {
        String tmp = String.valueOf(random.nextInt(10000));
        FirewallCreateOptions options;
        String name = "dsnfw" + tmp;
        String description = "Dasein Cloud Integration Test Firewall";

        if( vlanId == null ) {
            options = FirewallCreateOptions.getInstance(name, description);
        }
        else {
            options = FirewallCreateOptions.getInstance(vlanId, name, description);
        }
        String id = options.build(provider, false);

        if( vlanId == null ) {
            synchronized( testGeneralFirewalls ) {
                while( testGeneralFirewalls.containsKey(label) ) {
                    label = label + random.nextInt(9);
                }
                testGeneralFirewalls.put(label, id);
            }
        }
        else {
            synchronized( testVLANFirewalls ) {
                while( testVLANFirewalls.containsKey(label) ) {
                    label = label + random.nextInt(9);
                }
                testVLANFirewalls.put(label, id);
            }
        }
        return id;
    }

    public @Nonnull String provisionSubnet(@Nonnull VLANSupport support, @Nonnull String label, @Nonnull String vlanId, @Nonnull String namePrefix, @Nullable String preferredDataCenterId) throws CloudException, InternalException {
        if( preferredDataCenterId == null && support.isSubnetDataCenterConstrained() ) {
            VLAN vlan = support.getVlan(vlanId);

            if( vlan == null ) {
                throw new CloudException("No such VLAN: " + vlanId);
            }
            preferredDataCenterId = vlan.getProviderDataCenterId();
            if( preferredDataCenterId == null ) {
                preferredDataCenterId = DaseinTestManager.getDefaultDataCenterId(true);
                if( preferredDataCenterId == null ) {
                    //noinspection ConstantConditions
                    for( DataCenter dc : provider.getDataCenterServices().listDataCenters(provider.getContext().getRegionId()) ) {
                        if( dc.isActive() && dc.isAvailable() ) {
                            preferredDataCenterId = dc.getProviderDataCenterId(); // don't break here, long story; just don't
                        }
                    }
                }
            }
        }
        SubnetCreateOptions options;

        if( preferredDataCenterId == null ) {
            options = SubnetCreateOptions.getInstance(vlanId, "192.168.1." + random.nextInt(200) + "/27", namePrefix + (System.currentTimeMillis()%10000), "Dasein Cloud Integration test subnet");
        }
        else {
            options = SubnetCreateOptions.getInstance(vlanId, preferredDataCenterId, "192.168.1." + random.nextInt(200) + "/27", namePrefix + (System.currentTimeMillis()%10000), "Dasein Cloud Integration test subnet");
        }
        HashMap<String,Object> tags = new HashMap<String, Object>();

        tags.put("dsntestcase", "true");
        options.withMetaData(tags);
        String id;

        options.withSupportedTraffic(support.listSupportedIPVersions().iterator().next());
        id = options.build(provider);
        synchronized( testSubnets ) {
            while( testSubnets.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testSubnets.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionVLAN(@Nonnull VLANSupport support, @Nonnull String label, @Nonnull String namePrefix, @Nullable String preferredDataCenterId) throws CloudException, InternalException {
        String id;

        //if( support.isVlanDataCenterConstrained() && preferredDataCenterId == null ) {
            //preferredDataCenterId = DaseinTestManager.getDefaultDataCenterId(true);
        //}
        id = support.createVlan("192.168.1.0/24", namePrefix + (System.currentTimeMillis()%10000), "Test VLAN for the Dasein Cloud Integration tests", "example.com", new String[] { "192.168.1.1"}, new String[] { "192.168.1.1" }).getProviderVlanId();
        if( id == null ) {
            throw new CloudException("No VLAN was created");
        }
        synchronized( testVLANs ) {
            while( testVLANs.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testVLANs.put(label, id);
        }
        return id;
    }
}
