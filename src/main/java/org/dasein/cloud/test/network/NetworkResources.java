/**
 * Copyright (C) 2009-2013 enstratius, Inc.
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

package org.dasein.cloud.test.network;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.DNSRecord;
import org.dasein.cloud.network.DNSRecordType;
import org.dasein.cloud.network.DNSSupport;
import org.dasein.cloud.network.DNSZone;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallCreateOptions;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.LbEndpointType;
import org.dasein.cloud.network.LbListener;
import org.dasein.cloud.network.LoadBalancer;
import org.dasein.cloud.network.LoadBalancerAddressType;
import org.dasein.cloud.network.LoadBalancerCreateOptions;
import org.dasein.cloud.network.LoadBalancerState;
import org.dasein.cloud.network.LoadBalancerSupport;
import org.dasein.cloud.network.NetworkFirewallSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.SubnetCreateOptions;
import org.dasein.cloud.network.SubnetState;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.compute.ComputeResources;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Caching of and access to network resources used in the various test cases.
 * <p>Created by George Reese: 2/18/13 10:48 AM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class NetworkResources {
    static private final Logger logger = Logger.getLogger(NetworkResources.class);

    static private final Random random = new Random();

    private CloudProvider   provider;

    private final HashMap<String,String> testGeneralFirewalls = new HashMap<String, String>();
    private final HashMap<String,String> testIps4Free         = new HashMap<String, String>();
    private final HashMap<String,String> testIps6Free         = new HashMap<String, String>();
    private final HashMap<String,String> testIps4VLAN         = new HashMap<String, String>();
    private final HashMap<String,String> testIps6VLAN         = new HashMap<String, String>();
    private final HashMap<String,String> testLBs              = new HashMap<String, String>();
    private final HashMap<String,String> testNetworkFirewalls = new HashMap<String,String>();
    private final HashMap<String,String> testSubnets          = new HashMap<String, String>();
    private final HashMap<String,String> testVLANs            = new HashMap<String, String>();
    private final HashMap<String,String> testVLANFirewalls    = new HashMap<String, String>();
    private final HashMap<String,String> testZones            = new HashMap<String, String>();

    public NetworkResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    public int report() {
        boolean header = false;
        int count = 0;

        testGeneralFirewalls.remove(DaseinTestManager.STATELESS);
        if( !testGeneralFirewalls.isEmpty() ) {
            logger.info("Provisioned Network Resources:");
            header = true;
            count += testGeneralFirewalls.size();
            DaseinTestManager.out(logger, null, "---> Firewalls (Standard)", testGeneralFirewalls.size() + " " + testGeneralFirewalls);
        }
        testVLANFirewalls.remove(DaseinTestManager.STATELESS);
        if( !testVLANFirewalls.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Network Resources:");
                header = true;
            }
            count += testVLANFirewalls.size();
            DaseinTestManager.out(logger, null, "---> Firewalls (VLAN)", testVLANFirewalls.size() + " " + testVLANFirewalls);
        }
        testNetworkFirewalls.remove(DaseinTestManager.STATELESS);
        if( !testNetworkFirewalls.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Network Resources:");
                header = true;
            }
            count += testNetworkFirewalls.size();
            DaseinTestManager.out(logger, null, "---> Network Firewalls", testNetworkFirewalls.size() + " " + testNetworkFirewalls);
        }
        testIps4Free.remove(DaseinTestManager.STATELESS);
        if( !testIps4Free.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Network Resources:");
                header = true;
            }
            count += testIps4Free.size();
            DaseinTestManager.out(logger, null, "---> Static IPs (Standard/IPv4)", testIps4Free.size() + " " + testIps4Free);
        }
        testIps6Free.remove(DaseinTestManager.STATELESS);
        if( !testIps6Free.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Network Resources:");
                header = true;
            }
            count += testIps6Free.size();
            DaseinTestManager.out(logger, null, "---> Static IPs (Standard/IPv6)", testIps6Free.size() + " " + testIps6Free);
        }
        testIps4VLAN.remove(DaseinTestManager.STATELESS);
        if( !testIps4VLAN.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Network Resources:");
                header = true;
            }
            count += testIps4VLAN.size();
            DaseinTestManager.out(logger, null, "---> Static IPs (VLAN/IPv4)", testIps4VLAN.size() + " " + testIps4VLAN);
        }
        testIps6VLAN.remove(DaseinTestManager.STATELESS);
        if( !testIps6VLAN.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Network Resources:");
                header = true;
            }
            count += testIps6VLAN.size();
            DaseinTestManager.out(logger, null, "---> Static IPs (VLAN/IPv6)", testIps6VLAN.size() + " " + testIps6VLAN);
        }
        testSubnets.remove(DaseinTestManager.STATELESS);
        if( !testSubnets.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Network Resources:");
                header = true;
            }
            count += testSubnets.size();
            DaseinTestManager.out(logger, null, "---> Subnets", testSubnets.size() + " " + testSubnets);
        }
        testVLANs.remove(DaseinTestManager.STATELESS);
        if( !testVLANs.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Network Resources:");
                header = true;
            }
            count += testVLANs.size();
            DaseinTestManager.out(logger, null, "---> VLANs", testVLANs.size() + " " + testVLANs);
        }
        testZones.remove(DaseinTestManager.STATELESS);
        if( !testZones.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Network Resources:");
                header = true;
            }
            count += testZones.size();
            DaseinTestManager.out(logger, null, "---> DNS Zones", testZones.size() + " " + testZones);
        }
        testLBs.remove(DaseinTestManager.STATELESS);
        if( !testLBs.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Network Resources:");
            }
            count += testLBs.size();
            DaseinTestManager.out(logger, null, "--> Load Balancers", testLBs.size() + " " + testLBs);
        }
        return count;
    }

    public int close() {
        int count = 0;

        try {
            try { Thread.sleep(10000L); }
            catch( InterruptedException ignore ) { }
            NetworkServices networkServices = provider.getNetworkServices();

            if( networkServices != null ) {
                DNSSupport dnsSupport = networkServices.getDnsSupport();

                if( dnsSupport != null ) {
                    try {
                        for( Map.Entry<String,String> entry : testZones.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                DNSZone zone = dnsSupport.getDnsZone(entry.getValue());

                                try {
                                    if( zone != null ) {
                                        dnsSupport.deleteDnsZone(zone.getProviderDnsZoneId());
                                        count++;
                                    }
                                    else {
                                        count++;
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision test DNS zone " + entry.getValue() + ":" + t.getMessage());
                                }
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }

                LoadBalancerSupport lbSupport = networkServices.getLoadBalancerSupport();

                if( lbSupport != null ) {
                    try {
                        for( Map.Entry<String,String> entry : testLBs.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                LoadBalancer lb = lbSupport.getLoadBalancer(entry.getValue());

                                try {
                                    if( lb != null ) {
                                        lbSupport.removeLoadBalancer(lb.getProviderLoadBalancerId());
                                        count++;
                                    }
                                    else {
                                        count++;
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision test load balancer " + entry.getValue() + ":" + t.getMessage());
                                }
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }

                IpAddressSupport ipSupport = networkServices.getIpAddressSupport();

                if( ipSupport != null ) {
                    try {
                        for( Map.Entry<String,String> entry : testIps4Free.entrySet() ) {
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
                                        count++;
                                    }
                                    else {
                                        count++;
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision static IP " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                    try {
                        for( Map.Entry<String,String> entry : testIps6Free.entrySet() ) {
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
                                        count++;
                                    }
                                    else {
                                        count++;
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision static IP " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                    try {
                        for( Map.Entry<String,String> entry : testIps4VLAN.entrySet() ) {
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
                                        count++;
                                    }
                                    else {
                                        count++;
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision static IP " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                    try {
                        for( Map.Entry<String,String> entry : testIps6VLAN.entrySet() ) {
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
                                        count++;
                                    }
                                    else {
                                        count++;
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision static IP " + entry.getValue() + " post-test: " + t.getMessage());
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
                        for( Map.Entry<String,String> entry : testVLANs.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                VLAN v = vlanSupport.getVlan(entry.getValue());

                                if( v != null ) {
                                    try {
                                        if( vlanSupport.isConnectedViaInternetGateway(v.getProviderVlanId()) ) {
                                            vlanSupport.removeInternetGateway(v.getProviderVlanId());
                                        }
                                    }
                                    catch( Throwable t ) {
                                        logger.warn("Failed to remove internet gateway for test VLAN " + v + ":" + t.getMessage());
                                    }
                                }
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }

                NetworkFirewallSupport nfSupport = networkServices.getNetworkFirewallSupport();

                if( nfSupport != null ) {
                    try {
                        for( Map.Entry<String,String> entry : testNetworkFirewalls.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                try {
                                    Firewall f = nfSupport.getFirewall(entry.getValue());

                                    if( f != null ) {
                                        nfSupport.removeFirewall(entry.getValue());
                                        count++;
                                    }
                                    else {
                                        count++;
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision network firewall " + entry.getValue() + " post-test: " + t.getMessage());
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
                                        count++;
                                    }
                                    else {
                                        count++;
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision standard firewall " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }

                        for( Map.Entry<String,String> entry : testVLANFirewalls.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                try {
                                    Firewall f = firewallSupport.getFirewall(entry.getValue());

                                    if( f != null ) {
                                        firewallSupport.delete(entry.getValue());
                                        count++;
                                    }
                                    else {
                                        count++;
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision VLAN firewall " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }

                if( vlanSupport != null ) {
                    try {
                        for( Map.Entry<String,String> entry : testSubnets.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                Subnet s = vlanSupport.getSubnet(entry.getValue());

                                if( s != null ) {
                                    try {
                                        vlanSupport.removeSubnet(entry.getValue());
                                        count++;
                                    }
                                    catch( Throwable t ) {
                                        logger.warn("Failed to de-provision subnet (1) " + entry.getValue() + " post-test: " + t.getMessage());
                                        try { Thread.sleep(30000L); }
                                        catch( InterruptedException ignore ) { }
                                        try {
                                            vlanSupport.removeSubnet(entry.getValue());
                                            count++;
                                        }
                                        catch( Throwable t2 ) {
                                            logger.warn("Failed to de-provision subnet (final) " + entry.getValue() + " post-test: " + t2.getMessage());
                                        }
                                    }
                                }
                                else {
                                    count++;
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
                                VLAN v = vlanSupport.getVlan(entry.getValue());

                                if( v != null ) {
                                    try {
                                        if( vlanSupport.isConnectedViaInternetGateway(v.getProviderVlanId()) ) {
                                            vlanSupport.removeInternetGateway(v.getProviderVlanId());
                                        }
                                    }
                                    catch( Throwable t ) {
                                        logger.warn("Failed to remove internet gateway for test VLAN " + v + ":" + t.getMessage());
                                    }
                                }
                                else {
                                    count++;
                                    continue;
                                }
                                for( Firewall fw : nfSupport.listFirewalls() ) {
                                    if( fw.getProviderVlanId().equals(entry.getValue()) ) {
                                        try {
                                            nfSupport.removeFirewall(fw.getProviderFirewallId());
                                        }
                                        catch( Throwable t ) {
                                            logger.warn("Failed to remove network firewall for test VLAN " + v + ": " + t.getMessage());
                                        }
                                    }
                                }
                                for( Firewall fw : firewallSupport.list() ) {
                                    if( entry.getValue().equals(fw.getProviderFirewallId()) ) {
                                        try {
                                            firewallSupport.delete(fw.getProviderFirewallId());
                                        }
                                        catch( Throwable t ) {
                                            logger.warn("Failed to remove test VLAN firewall for VLAN " + v + ": " + t.getMessage());
                                        }
                                    }
                                }
                                try {
                                    for( Subnet subnet : vlanSupport.listSubnets(entry.getValue()) ) {
                                        try {
                                            vlanSupport.removeSubnet(subnet.getProviderSubnetId());
                                        }
                                        catch( Throwable t ) {
                                            logger.warn("Failed to de-provision subnet " + subnet.getProviderSubnetId() + " for test VLAN " + entry.getValue() + " post-test: " + t.getMessage());
                                        }
                                    }
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision subnets for test VLAN " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                                try {
                                    vlanSupport.removeVlan(entry.getValue());
                                    count++;
                                }
                                catch( Throwable t ) {
                                    logger.warn("Failed to de-provision test VLAN " + entry.getValue() + " post-test: " + t.getMessage());
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
        return count;
    }

    private @Nullable String findStatelessDNSZone() {
        NetworkServices networkServices = provider.getNetworkServices();

        if( networkServices != null ) {
            DNSSupport support = networkServices.getDnsSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    DNSZone defaultZone = null;

                    for( DNSZone zone : support.listDnsZones() ) {
                        boolean hasRecord = false;

                        for( DNSRecordType type : DNSRecordType.values() ) {
                            Iterable<DNSRecord> records = support.listDnsRecords(zone.getProviderDnsZoneId(), type, null);

                            if( records.iterator().hasNext() ) {
                                hasRecord = true;
                                break;
                            }
                        }
                        defaultZone = zone;
                        if( hasRecord ) {
                            break;
                        }
                    }
                    if( defaultZone != null ) {
                        String id = defaultZone.getProviderDnsZoneId();

                        if( id != null ) {
                            testZones.put(DaseinTestManager.STATELESS, id);
                        }
                        return id;
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
                ignore.printStackTrace();
            }
        }
        return null;
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

    private @Nullable String findStatelessLoadBalancer() {
        NetworkServices networkServices = provider.getNetworkServices();

        if( networkServices != null ) {
            LoadBalancerSupport support = networkServices.getLoadBalancerSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    LoadBalancer defaultLB = null;

                    for( LoadBalancer lb : support.listLoadBalancers() ) {
                        LoadBalancerState s = lb.getCurrentState();
                        boolean hasEndpoints = false;

                        if( s.equals(LoadBalancerState.ACTIVE) ) {
                            defaultLB = lb;
                            hasEndpoints = support.listEndpoints(lb.getProviderLoadBalancerId()).iterator().hasNext();
                        }
                        else if( defaultLB == null ) {
                            defaultLB = lb;
                        }
                        if( hasEndpoints ) {
                            break;
                        }
                    }
                    if( defaultLB != null ) {
                        testLBs.put(DaseinTestManager.STATELESS, defaultLB.getProviderLoadBalancerId());
                        return defaultLB.getProviderLoadBalancerId();
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
                ignore.printStackTrace();
            }
        }
        return null;
    }

    private @Nullable String findStatelessNetworkFirewall() {
        NetworkServices networkServices = provider.getNetworkServices();

        if( networkServices != null ) {
            NetworkFirewallSupport support = networkServices.getNetworkFirewallSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Firewall defaultFirewall = null;

                    for( Firewall firewall : support.listFirewalls() ) {
                        if( firewall.isActive() && firewall.isAvailable() ) {
                            String id = firewall.getProviderFirewallId();

                            if( id != null && support.listRules(id).iterator().hasNext() ) {
                                testNetworkFirewalls.put(DaseinTestManager.STATELESS, id);
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
                    if( defaultFirewall != null ) {
                        String id = defaultFirewall.getProviderFirewallId();

                        if( id != null ) {
                            testNetworkFirewalls.put(DaseinTestManager.STATELESS, id);
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

    private @Nullable String findStatelessIP(IPVersion version, boolean vlan) {
        NetworkServices networkServices = provider.getNetworkServices();

        if( networkServices != null ) {
            IpAddressSupport ipSupport = networkServices.getIpAddressSupport();

            try {
                if( ipSupport != null && ipSupport.isSubscribed() ) {
                    IpAddress defaultAddress = null;

                    for( IpAddress address : ipSupport.listIpPool(version, false) ) {
                        if( (address.isAssigned() || defaultAddress == null) && (vlan == (address.getProviderVlanId() != null)) ) {
                            defaultAddress = address;
                            if( defaultAddress.isAssigned() ) {
                                break;
                            }
                        }
                    }
                    if( defaultAddress != null ) {
                        String id = defaultAddress.getProviderIpAddressId();

                        if( vlan ) {
                            if( version.equals(IPVersion.IPV4) ) {
                                testIps4VLAN.put(DaseinTestManager.STATELESS, id);
                            }
                            else {
                                testIps6VLAN.put(DaseinTestManager.STATELESS, id);
                            }
                        }
                        else {
                            if( version.equals(IPVersion.IPV4) ) {
                                testIps4Free.put(DaseinTestManager.STATELESS, id);
                            }
                            else {
                                testIps6Free.put(DaseinTestManager.STATELESS, id);
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

    public @Nullable String getTestLoadBalancerId(@Nonnull String label, boolean provisionIfNull) {
        if( label.equalsIgnoreCase(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testLBs.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessLoadBalancer();
        }
        String id = testLBs.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            NetworkServices services = provider.getNetworkServices();

            if( services != null ) {
                try {
                    return provisionLoadBalancer(label, null);
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        return null;
    }

    public @Nullable String getTestNetworkFirewallId(@Nonnull String label, boolean provisionIfNull, @Nullable String vlanId) {
        if( label.equalsIgnoreCase(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testNetworkFirewalls.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessNetworkFirewall();
        }
        String id = testNetworkFirewalls.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            NetworkServices services = provider.getNetworkServices();

            if( services != null ) {
                try {
                    return provisionNetworkFirewall(label, vlanId);
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        return null;
    }

    public @Nullable String getTestStaticIpId(@Nonnull String label, boolean provisionIfNull, @Nullable IPVersion version, boolean inVlan, @Nullable String vlanId) {
        if( version == null ) {
            NetworkServices services = provider.getNetworkServices();
            IpAddressSupport support = (services == null ? null : services.getIpAddressSupport());

            if( support == null ) {
                return null;
            }
            try {
                for( IPVersion v : support.listSupportedIPVersions() ) {
                    String id = getTestStaticIpId(label, provisionIfNull, v, inVlan, vlanId);

                    if( id != null ) {
                        return id;
                    }
                }
                return null;
            }
            catch( Throwable ignore ) {
                return null;
            }
        }
        Map<String,String> map;

        if( inVlan ) {
            map = (version.equals(IPVersion.IPV4) ? testIps4VLAN : testIps6VLAN);
        }
        else {
            map = (version.equals(IPVersion.IPV4) ? testIps4Free : testIps6Free);
        }
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : map.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessIP(version, inVlan);
        }
        String id = map.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            NetworkServices services = provider.getNetworkServices();

            if( services != null ) {
                IpAddressSupport support = services.getIpAddressSupport();

                if( support != null ) {
                    try {
                        if( inVlan ) {
                            if( vlanId == null ) {
                                vlanId = getTestVLANId(DaseinTestManager.STATEFUL, true, null);
                            }
                            return provisionAddress(support, label, version, vlanId);
                        }
                        else {
                            return provisionAddress(support, label, version, null);
                        }
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
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
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
                    catch( Throwable t ) {
                        logger.warn("Failed to provider test subnet for " + vlanId + ": " + t.getMessage());
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

    public @Nullable String getTestZoneId(@Nonnull String label, boolean provisionIfNull) {
        if( label.equalsIgnoreCase(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testZones.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessDNSZone();
        }
        String id = testZones.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            NetworkServices services = provider.getNetworkServices();

            if( services != null ) {
                DNSSupport support = services.getDnsSupport();

                if( support != null ) {
                    try {
                        return provisionDNSZone(support, label, "dasein", "org");
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nonnull String provisionAddress(@Nonnull IpAddressSupport support, @Nonnull String label, @Nullable IPVersion version, @Nullable String vlanId) throws CloudException, InternalException {
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
        Map<String,String> map;

        if( vlanId == null ) {
            map = (version.equals(IPVersion.IPV4) ? testIps4Free : testIps6Free);
        }
        else {
            map = (version.equals(IPVersion.IPV4) ? testIps4VLAN : testIps6VLAN);
        }
        String id;

        if( vlanId == null ) {
            id = support.request(version);
        }
        else {
            if( support.identifyVlanForVlanIPRequirement().equals(Requirement.NONE) ) {
                id = support.requestForVLAN(version);
            }
            else {
                id = support.requestForVLAN(version, vlanId);
            }
        }
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized( map ) {
            while( map.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            map.put(label, id);
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

    public @Nonnull String provisionLoadBalancer(@Nonnull String label, @Nullable String namePrefix) throws CloudException, InternalException {
        NetworkServices services = provider.getNetworkServices();

        if( services == null ) {
            throw new CloudException("This cloud does not support load balancers");
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            throw new CloudException("This cloud does not support load balancers");
        }

        String name = (namePrefix == null ? "dsnlb" + random.nextInt(10000) : namePrefix + random.nextInt(10000));
        String description = "Dasein Cloud LB Test";
        LoadBalancerCreateOptions options;

        if( !support.isAddressAssignedByProvider() && support.getAddressType().equals(LoadBalancerAddressType.IP) ) {
            IpAddressSupport ipSupport = services.getIpAddressSupport();

            if( ipSupport == null ) {
                options = LoadBalancerCreateOptions.getInstance(name, description);
            }
            else {
                IpAddress address = null;

                for( IPVersion version : ipSupport.listSupportedIPVersions() ) {
                    Iterator<IpAddress> addrs = ipSupport.listIpPool(version, true).iterator();

                    if( addrs.hasNext() ) {
                        address = addrs.next();
                        break;
                    }
                }
                if( address == null ) {
                    for( IPVersion version : ipSupport.listSupportedIPVersions() ) {
                        if( ipSupport.isRequestable(version) ) {
                            address = ipSupport.getIpAddress(ipSupport.request(version));
                            if( address != null ) {
                                break;
                            }
                        }
                    }
                }
                if( address == null ) {
                    options = LoadBalancerCreateOptions.getInstance(name, description);
                }
                else {
                    options = LoadBalancerCreateOptions.getInstance(name, description, address.getProviderIpAddressId());
                }
            }
        }
        else {
            options = LoadBalancerCreateOptions.getInstance(name, description);
        }

        if( support.identifyListenersOnCreateRequirement().equals(Requirement.REQUIRED) ) {
            options.havingListeners(LbListener.getInstance(1000 + random.nextInt(10000), 1000 + random.nextInt(10000)));
        }
        String[] dcIds = new String[2];

        if( support.identifyEndpointsOnCreateRequirement().equals(Requirement.REQUIRED) ) {
            Iterable<LbEndpointType> types = support.listSupportedEndpointTypes();
            boolean vmBased = false;

            for( LbEndpointType t : types ) {
                if( t.equals(LbEndpointType.VM) ) {
                    vmBased = true;
                    break;
                }
            }
            if( vmBased ) {
                ComputeResources c = DaseinTestManager.getComputeResources();

                if( c != null ) {
                    String server1 = c.getTestVmId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, null);

                    if( server1 != null ) {
                        @SuppressWarnings("ConstantConditions") VirtualMachine vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(server1);

                        if( vm != null ) {
                            dcIds[0] = vm.getProviderDataCenterId();
                        }
                    }
                    @SuppressWarnings("ConstantConditions") Iterator<DataCenter> it = provider.getDataCenterServices().listDataCenters(provider.getContext().getRegionId()).iterator();
                    String targetDC = dcIds[0];

                    if( it.hasNext() ) {
                        String dcId = it.next().getProviderDataCenterId();

                        if( !dcId.equals(dcIds[0]) ) {
                            dcIds[1] = dcId;
                            targetDC = dcId;
                        }
                    }
                    String server2 = c.getTestVmId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, targetDC);

                    if( server1 != null && server2 != null ) {
                        options.withVirtualMachines(server1, server2);
                    }
                    else if( server1 != null ) {
                        options.withVirtualMachines(server1);
                    }
                }
            }
            else {
                options.withIpAddresses("207.32.82.72");
            }
        }
        if( support.isDataCenterLimited() ) {
            if( dcIds[0] != null && dcIds[1] != null ) {
                options.limitedTo(dcIds);
            }
            else if( dcIds[0] != null ) {
                options.limitedTo(dcIds[0]);
            }
            else if( dcIds[1] != null ) {
                options.limitedTo(dcIds[1]);
            }
            else {
                @SuppressWarnings("ConstantConditions") Iterator<DataCenter> it = provider.getDataCenterServices().listDataCenters(provider.getContext().getRegionId()).iterator();

                if( it.hasNext() ) {
                    options.limitedTo(it.next().getProviderDataCenterId());
                }
            }
        }

        String id = options.build(provider);

        synchronized( testLBs ) {
            while( testLBs.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testLBs.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionNetworkFirewall(@Nonnull String label, @Nullable String vlanId) throws CloudException, InternalException {
        NetworkServices services = provider.getNetworkServices();

        if( services == null ) {
            throw new CloudException("This cloud does not support network firewalls");
        }
        NetworkFirewallSupport support = services.getNetworkFirewallSupport();

        if( support == null ) {
            throw new CloudException("This cloud does not support network firewalls");
        }

        if( vlanId == null ) {
            vlanId = getTestVLANId(DaseinTestManager.STATEFUL, true, null);
            if( vlanId == null ) {
                throw new CloudException("No VLAN ID could be found");
            }
        }

        String tmp = String.valueOf(random.nextInt(10000));
        String name = "dsnnetfw" + tmp;
        String description = "Dasein Cloud Integration Test NetworkFirewall";

        FirewallCreateOptions options = FirewallCreateOptions.getInstance(vlanId, name, description);

        String id = support.createFirewall(options);

        synchronized( testNetworkFirewalls ) {
            while( testNetworkFirewalls.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testNetworkFirewalls.put(label, id);
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
        try {
            id = options.build(provider);
        }
        catch( CloudException e ) {
            if( e.getMessage().contains("conflicts with another") ) {
                return provisionSubnet(support, label, vlanId, namePrefix, preferredDataCenterId);
            }
            throw e;
        }
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

    public @Nonnull String provisionDNSZone(@Nonnull DNSSupport support, @Nonnull String label, @Nonnull String domainPrefix, @Nonnull String tld) throws CloudException, InternalException {
        String name = domainPrefix + (System.currentTimeMillis()%10000) + "." + tld;
        String id = support.createDnsZone(name, name, "Dasein Cloud Test Zone");

        synchronized( testZones ) {
            while( testZones.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testZones.put(label, id);
        }
        return id;
    }
}
