package org.dasein.cloud.test.network;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.dc.DataCenter;
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
import java.util.TreeSet;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/18/13 10:48 AM</p>
 *
 * @author George Reese
 */
public class NetworkResources {
    static private final Logger logger = Logger.getLogger(NetworkResources.class);

    private CloudProvider   provider;
    private TreeSet<String> provisionedAddresses = new TreeSet<String>();
    private TreeSet<String> provisionedSubnets = new TreeSet<String>();
    private TreeSet<String> provisionedVLANs = new TreeSet<String>();
    private String          testStaticIpIdCustom;
    private String          testStaticIpIdShared;
    private String          testSubnetIdCustom;
    private String          testSubnetIdShared;
    private String          testVLANIdCustom;
    private String          testVLANIdShared;

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
                        for( String id : provisionedAddresses ) {
                            try {
                                ipSupport.releaseFromServer(id);
                                try { Thread.sleep(3000L); }
                                catch( InterruptedException ignore ) { }
                            }
                            catch( Throwable ignore ) {
                                // ignore
                            }
                            try {
                                ipSupport.releaseFromPool(id);
                            }
                            catch( Throwable ignore ) {
                                // ignore
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
                        for( String id : provisionedSubnets ) {
                            try {
                                vlanSupport.removeSubnet(id);
                            }
                            catch( Throwable ignore ) {
                                // ignore
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                    try {
                        for( String id : provisionedVLANs ) {
                            try {
                                vlanSupport.removeVlan(id);
                            }
                            catch( Throwable ignore ) {
                                // ignore
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

    public @Nullable String getTestStaticIpId(boolean shared) {
        if( testStaticIpIdCustom != null ) {
            return testStaticIpIdCustom;
        }
        if( shared ) {
            return testStaticIpIdShared;
        }
        return testStaticIpIdCustom;
    }

    public @Nullable String getTestSubnetId(boolean shared) {
        if( testSubnetIdCustom != null ) {
            return testSubnetIdCustom;
        }
        if( shared ) {
            return testSubnetIdShared;
        }
        return testSubnetIdCustom;
    }

    public @Nullable String getTestVLANId(boolean shared) {
        if( testVLANIdCustom != null ) {
            return testVLANIdCustom;
        }
        if( shared ) {
            return testVLANIdShared;
        }
        return testVLANIdCustom;
    }

    public void init() {
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
                        testStaticIpIdShared = defaultAddress.getProviderIpAddressId();
                    }
                    /*
                    if( stateful ) {
                        IPVersion version = null;

                        for( IPVersion v : ipSupport.listSupportedIPVersions() ) {
                            if( ipSupport.isRequestable(v) ) {
                                version = v;
                                break;
                            }
                        }
                        if( version != null ) {
                            testStaticIpIdCustom = provisionAddress(ipSupport, version);
                        }
                    }
                    */
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
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
                                            break;
                                        }
                                    }
                                }
                            }
                            if( defaultVlan == null || VLANState.AVAILABLE.equals(vlan.getCurrentState()) ) {
                                defaultVlan = vlan;
                                defaultSubnet = foundSubnet;
                                if( VLANState.AVAILABLE.equals(vlan.getCurrentState()) && ((foundSubnet != null && SubnetState.AVAILABLE.equals(foundSubnet.getCurrentState())) || vlanSupport.getSubnetSupport().equals(Requirement.NONE)) ) {
                                    break;
                                }
                            }
                        }
                    }
                    if( defaultVlan != null ) {
                        testVLANIdShared = defaultVlan.getProviderVlanId();
                    }
                    if( defaultSubnet != null ) {
                        testSubnetIdShared = defaultSubnet.getProviderSubnetId();
                    }
                    /*
                    if( stateful && vlanSupport.allowsNewVlanCreation() ) {
                        testVLANIdCustom = provisionVLAN(vlanSupport, "dsnvlan", null);
                        if( stateful && vlanSupport.allowsNewSubnetCreation() ) {
                            testSubnetIdCustom = provisionSubnet(vlanSupport, testVLANIdCustom, "dsnsub", null);
                        }
                    }
                    */
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
    }

    public @Nonnull String provisionAddress(@Nonnull IpAddressSupport support, @Nonnull IPVersion version) throws CloudException, InternalException {
        String id = support.request(version);

        provisionedAddresses.add(id);
        return id;
    }

    public @Nonnull String provisionSubnet(@Nonnull VLANSupport support, @Nonnull String vlanId, @Nonnull String namePrefix, @Nullable String preferredDataCenterId) throws CloudException, InternalException {
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
            options = SubnetCreateOptions.getInstance(vlanId, "192.168.1.0/27", namePrefix + (System.currentTimeMillis()%10000), "Dasein Cloud Integration test subnet");
        }
        else {
            options = SubnetCreateOptions.getInstance(vlanId, preferredDataCenterId, "192.168.1.0/27", namePrefix + (System.currentTimeMillis()%10000), "Dasein Cloud Integration test subnet");
        }
        HashMap<String,Object> tags = new HashMap<String, Object>();

        tags.put("dsntestcase", "true");
        options.withMetaData(tags);
        String id;

        options.withSupportedTraffic(support.listSupportedIPVersions().iterator().next());
        id = options.build(provider);
        provisionedSubnets.add(id);
        return id;
    }

    public @Nonnull String provisionVLAN(@Nonnull VLANSupport support, @Nonnull String namePrefix, @Nullable String preferredDataCenterId) throws CloudException, InternalException {
        String id;

        if( support.isVlanDataCenterConstrained() && preferredDataCenterId == null ) {
            // TODO: when Dasein Cloud can properly constrain a VLAN
        }
        id = support.createVlan("192.168.1.0/24", namePrefix + (System.currentTimeMillis()%10000), "Test VLAN for the Dasein Cloud Integration tests", "example.com", new String[] { "192.168.1.1"}, new String[] { "192.168.1.1" }).getProviderVlanId();
        if( id == null ) {
            throw new CloudException("No VLAN was created");
        }
        provisionedVLANs.add(id);
        return id;
    }
}
