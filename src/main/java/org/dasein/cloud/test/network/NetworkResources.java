/**
 * Copyright (C) 2009-2014 Dell, Inc.
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

package org.dasein.cloud.test.network;

import org.apache.log4j.Logger;
import org.dasein.cloud.*;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.*;
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
 *
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class NetworkResources {
    static private final Logger logger = Logger.getLogger(NetworkResources.class);

    static private final Random random = new Random();

    static public final String TEST_CIDR = "209.98.98.98/32";
    static public final String TEST_HC_PATH = "/index.htm";
    static public final LoadBalancerHealthCheck.HCProtocol TEST_HC_PROTOCOL = LoadBalancerHealthCheck.HCProtocol.HTTP;
    static public final String TEST_HC_HOST = "localhost";
    static public final int TEST_HC_PORT = 8080;

    private CloudProvider provider;

    private final HashMap<String, String> testGeneralFirewalls = new HashMap<String, String>();
    private final HashMap<String, String> testIps4Free = new HashMap<String, String>();
    private final HashMap<String, String> testIps6Free = new HashMap<String, String>();
    private final HashMap<String, String> testIps4VLAN = new HashMap<String, String>();
    private final HashMap<String, String> testIps6VLAN = new HashMap<String, String>();
    private final HashMap<String, String> testLBs = new HashMap<String, String>();
    private final HashMap<String, String> testSSLCertificates = new HashMap<String, String>();
    private final HashMap<String, String> testNetworkFirewalls = new HashMap<String, String>();
    private final HashMap<String, String> testSubnets = new HashMap<String, String>();
    private final HashMap<String, String> testInternetGateways = new HashMap<String, String>();
    private final HashMap<String, String> testVLANs = new HashMap<String, String>();
    private final HashMap<String, String> testRouteTables = new HashMap<String, String>();
    private final HashMap<String, String> testVLANFirewalls = new HashMap<String, String>();
    private final HashMap<String, String> testZones = new HashMap<String, String>();
    // make subnet creation more predicatable
    private final String[] cidrs = new String[]{"192.168.1.0/28", "192.168.1.20/28", "192.168.1.40/28", "192.168.1.60/28", "192.168.1.80/28",
            "192.168.1.100/28", "192.168.1.120/28", "192.168.1.140/28", "192.168.1.160/28", "192.168.1.180/28", "192.168.1.200/28",
            "192.168.1.220/28", "192.168.1.240/28"};
    private Integer cidrCount = 0;

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
                header = true;
            }
            count += testLBs.size();
            DaseinTestManager.out(logger, null, "---> Load Balancers", testLBs.size() + " " + testLBs);
        }
        testSSLCertificates.remove(DaseinTestManager.STATELESS);
        if ( !testSSLCertificates.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Network Resources:");
            }
            count += testSSLCertificates.size();
            DaseinTestManager.out(logger, null, "---> SSL Certificates", testSSLCertificates.size() + " " +
                    testSSLCertificates);
        }
        return count;
    }

    public int close() {
        int count = 0;

        try {
            try {
                Thread.sleep(10000L);
            } catch( InterruptedException ignore ) {
            }
            NetworkServices networkServices = provider.getNetworkServices();

            if( networkServices != null ) {
                DNSSupport dnsSupport = networkServices.getDnsSupport();

                if( dnsSupport != null ) {
                    try {
                        for( Map.Entry<String, String> entry : testZones.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                DNSZone zone = dnsSupport.getDnsZone(entry.getValue());

                                try {
                                    if( zone != null ) {
                                        try {
                                            for( DNSRecord record : dnsSupport.listDnsRecords(zone.getProviderDnsZoneId(), DNSRecordType.A, null) ) {
                                                try {
                                                    dnsSupport.deleteDnsRecords(record);
                                                } catch( Throwable ignore ) {
                                                    // ignore
                                                }
                                            }
                                        } catch( Throwable ignore ) {
                                            // ignore
                                        }
                                        dnsSupport.deleteDnsZone(zone.getProviderDnsZoneId());
                                        count++;
                                    } else {
                                        count++;
                                    }
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision test DNS zone " + entry.getValue() + ":" + t.getMessage());
                                }
                            }
                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                }

                LoadBalancerSupport lbSupport = networkServices.getLoadBalancerSupport();

                if( lbSupport != null ) {
                    try {
                        for( Map.Entry<String, String> entry : testLBs.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                LoadBalancer lb = lbSupport.getLoadBalancer(entry.getValue());

                                try {
                                    if( lb != null ) {
                                        lbSupport.removeLoadBalancer(lb.getProviderLoadBalancerId());
                                        
                                        try {
                                        	lbSupport.removeLoadBalancerHealthCheck(lb.getProviderLoadBalancerId()); // named LBHC same as LB for convienence.
                                        } catch (Throwable t ) { /* ignore if not supported */ }
                                        
                                        count++;
                                    } else {
                                        count++;
                                    }
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision test load balancer " + entry.getValue() + ":" + t.getMessage());
                                }
                            }
                        }

                        for ( Map.Entry<String, String> entry : testSSLCertificates.entrySet() ) {
                            if ( !DaseinTestManager.STATELESS.equals(entry.getKey()) ) {
                                SSLCertificate sslCertificate = lbSupport.getSSLCertificate(entry.getValue());

                                try {
                                    if ( sslCertificate != null ) {
                                        lbSupport.removeSSLCertificate( entry.getValue() );
                                    }
                                    count++;
                                } catch ( Throwable t ) {
                                    logger.warn("Failed to de-provision test SSL certificate " + entry.getValue() +
                                            ":" + t.getMessage(), t);
                                }
                            }
                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                }

                IpAddressSupport ipSupport = networkServices.getIpAddressSupport();

                if( ipSupport != null ) {
                    try {
                        for( Map.Entry<String, String> entry : testIps4Free.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                IpAddress addr = ipSupport.getIpAddress(entry.getValue());

                                try {
                                    if( addr != null ) {
                                        ipSupport.releaseFromServer(entry.getValue());
                                    }
                                    try {
                                        Thread.sleep(3000L);
                                    } catch( InterruptedException ignore ) {
                                    }
                                } catch( Throwable ignore ) {
                                    // ignore
                                }
                                try {
                                    if( addr != null ) {
                                        ipSupport.releaseFromPool(entry.getValue());
                                        count++;
                                    } else {
                                        count++;
                                    }
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision static IP " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                    try {
                        for( Map.Entry<String, String> entry : testIps6Free.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                IpAddress addr = ipSupport.getIpAddress(entry.getValue());

                                try {
                                    if( addr != null ) {
                                        ipSupport.releaseFromServer(entry.getValue());
                                    }
                                    try {
                                        Thread.sleep(3000L);
                                    } catch( InterruptedException ignore ) {
                                    }
                                } catch( Throwable ignore ) {
                                    // ignore
                                }
                                try {
                                    if( addr != null ) {
                                        ipSupport.releaseFromPool(entry.getValue());
                                        count++;
                                    } else {
                                        count++;
                                    }
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision static IP " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                    try {
                        for( Map.Entry<String, String> entry : testIps4VLAN.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                IpAddress addr = ipSupport.getIpAddress(entry.getValue());

                                try {
                                    if( addr != null ) {
                                        ipSupport.releaseFromServer(entry.getValue());
                                    }
                                    try {
                                        Thread.sleep(3000L);
                                    } catch( InterruptedException ignore ) {
                                    }
                                } catch( Throwable ignore ) {
                                    // ignore
                                }
                                try {
                                    if( addr != null ) {
                                        ipSupport.releaseFromPool(entry.getValue());
                                        count++;
                                    } else {
                                        count++;
                                    }
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision static IP " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                    try {
                        for( Map.Entry<String, String> entry : testIps6VLAN.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                IpAddress addr = ipSupport.getIpAddress(entry.getValue());

                                try {
                                    if( addr != null ) {
                                        ipSupport.releaseFromServer(entry.getValue());
                                    }
                                    try {
                                        Thread.sleep(3000L);
                                    } catch( InterruptedException ignore ) {
                                    }
                                } catch( Throwable ignore ) {
                                    // ignore
                                }
                                try {
                                    if( addr != null ) {
                                        ipSupport.releaseFromPool(entry.getValue());
                                        count++;
                                    } else {
                                        count++;
                                    }
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision static IP " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                }

                VLANSupport vlanSupport = networkServices.getVlanSupport();

                if( vlanSupport != null ) {
                    try {
                        for( Map.Entry<String, String> entry : testVLANs.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                VLAN v = vlanSupport.getVlan(entry.getValue());

                                if( v != null ) {
                                    try {
                                        if( vlanSupport.isConnectedViaInternetGateway(v.getProviderVlanId()) ) {
                                            vlanSupport.removeInternetGateway(v.getProviderVlanId());
                                        }
                                    } catch( Throwable t ) {
                                        logger.warn("Failed to remove internet gateway for test VLAN " + v + ":" + t.getMessage());
                                    }
                                }
                            }
                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                }

                NetworkFirewallSupport nfSupport = networkServices.getNetworkFirewallSupport();

                if( nfSupport != null ) {
                    try {
                        for( Map.Entry<String, String> entry : testNetworkFirewalls.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                try {
                                    Firewall f = nfSupport.getFirewall(entry.getValue());

                                    if( f != null ) {
                                        nfSupport.removeFirewall(entry.getValue());
                                        count++;
                                    } else {
                                        count++;
                                    }
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision network firewall " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                }

                FirewallSupport firewallSupport = networkServices.getFirewallSupport();

                if( firewallSupport != null ) {
                    try {
                        for( Map.Entry<String, String> entry : testGeneralFirewalls.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                try {
                                    Firewall f = firewallSupport.getFirewall(entry.getValue());

                                    if( f != null ) {
                                        firewallSupport.delete(entry.getValue());
                                        count++;
                                    } else {
                                        count++;
                                    }
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision standard firewall " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }

                        for( Map.Entry<String, String> entry : testVLANFirewalls.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                try {
                                    Firewall f = firewallSupport.getFirewall(entry.getValue());

                                    if( f != null ) {
                                        firewallSupport.delete(entry.getValue());
                                        count++;
                                    } else {
                                        count++;
                                    }
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision VLAN firewall " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                }

                if( vlanSupport != null ) {
                    try {
                        for( Map.Entry<String, String> entry : testInternetGateways.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                InternetGateway ig = vlanSupport.getInternetGatewayById(entry.getValue());

                                if( ig != null ) {
                                    try {
                                        vlanSupport.removeInternetGatewayById(entry.getValue());
                                        count++;
                                    } catch( Throwable t ) {
                                        logger.warn("Failed to de-provision internetgateway (1) " + entry.getValue() + " post-test: " + t.getMessage());
                                        try {
                                            Thread.sleep(30000L);
                                        } catch( InterruptedException ignore ) {
                                        }
                                        try {
                                            vlanSupport.removeInternetGatewayById(entry.getValue());
                                            count++;
                                        } catch( Throwable t2 ) {
                                            logger.warn("Failed to de-provision internetgateway (final) " + entry.getValue() + " post-test: " + t2.getMessage());
                                        }
                                    }
                                } else {
                                    count++;
                                }
                            }

                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                    try {
                        for( Map.Entry<String, String> entry : testSubnets.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                Subnet s = vlanSupport.getSubnet(entry.getValue());

                                if( s != null ) {
                                    try {
                                        vlanSupport.removeSubnet(entry.getValue());
                                        count++;
                                    } catch( Throwable t ) {
                                        logger.warn("Failed to de-provision subnet (1) " + entry.getValue() + " post-test: " + t.getMessage());
                                        try {
                                            Thread.sleep(30000L);
                                        } catch( InterruptedException ignore ) {
                                        }
                                        try {
                                            vlanSupport.removeSubnet(entry.getValue());
                                            count++;
                                        } catch( Throwable t2 ) {
                                            logger.warn("Failed to de-provision subnet (final) " + entry.getValue() + " post-test: " + t2.getMessage());
                                        }
                                    }
                                } else {
                                    count++;
                                }
                            }

                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                    try {
                        for( Map.Entry<String, String> entry : testRouteTables.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                RoutingTable rtb = vlanSupport.getRoutingTable(entry.getValue());

                                if( rtb != null ) {
                                    try {
                                        vlanSupport.removeRoutingTable(entry.getValue());
                                        count++;
                                    } catch( Throwable t ) {
                                        logger.warn("Failed to de-provision routetable (1) " + entry.getValue() + " post-test: " + t.getMessage());
                                        try {
                                            Thread.sleep(30000L);
                                        } catch( InterruptedException ignore ) {
                                        }
                                        try {
                                            vlanSupport.removeRoutingTable(entry.getValue());
                                            count++;
                                        } catch( Throwable t2 ) {
                                            logger.warn("Failed to de-provision routetable (final) " + entry.getValue() + " post-test: " + t2.getMessage());
                                        }
                                    }
                                } else {
                                    count++;
                                }
                            }

                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                    try {
                        for( Map.Entry<String, String> entry : testVLANs.entrySet() ) {
                            if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                                VLAN v = vlanSupport.getVlan(entry.getValue());

                                if( v != null ) {
                                    try {
                                        if( vlanSupport.isConnectedViaInternetGateway(v.getProviderVlanId()) ) {
                                            vlanSupport.removeInternetGateway(v.getProviderVlanId());
                                        }
                                    } catch( Throwable t ) {
                                        logger.warn("Failed to remove internet gateway for test VLAN " + v + ":" + t.getMessage());
                                    }
                                } else {
                                    count++;
                                    continue;
                                }
                                if( nfSupport != null ) {
                                    for( Firewall fw : nfSupport.listFirewalls() ) {
                                        if( fw.getProviderVlanId().equals(entry.getValue()) ) {
                                            try {
                                                nfSupport.removeFirewall(fw.getProviderFirewallId());
                                            } catch( Throwable t ) {
                                                logger.warn("Failed to remove network firewall for test VLAN " + v + ": " + t.getMessage());
                                            }
                                        }
                                    }
                                }
                                if( firewallSupport != null ) {
                                    for( Firewall fw : firewallSupport.list() ) {
                                        if( entry.getValue().equals(fw.getProviderFirewallId()) ) {
                                            try {
                                                firewallSupport.delete(fw.getProviderFirewallId());
                                            } catch( Throwable t ) {
                                                logger.warn("Failed to remove test VLAN firewall for VLAN " + v + ": " + t.getMessage());
                                            }
                                        }
                                    }
                                }
                                try {
                                    for( Subnet subnet : vlanSupport.listSubnets(entry.getValue()) ) {
                                        try {
                                            vlanSupport.removeSubnet(subnet.getProviderSubnetId());
                                        } catch( Throwable t ) {
                                            logger.warn("Failed to de-provision subnet " + subnet.getProviderSubnetId() + " for test VLAN " + entry.getValue() + " post-test: " + t.getMessage());
                                        }
                                    }
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision subnets for test VLAN " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                                try {
                                    for( RoutingTable routeTable : vlanSupport.listRoutingTablesForVlan(entry.getValue()) ) {
                                        try {
                                            vlanSupport.removeRoutingTable(routeTable.getProviderRoutingTableId());
                                        } catch( Throwable t ) {
                                            logger.warn("Failed to de-provision route table " + routeTable.getProviderRoutingTableId() + " for test VLAN " + entry.getValue() + " post-test: " + t.getMessage());
                                        }
                                    }
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision route tables for test VLAN " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                                try {
                                    vlanSupport.removeVlan(entry.getValue());
                                    count++;
                                } catch( Throwable t ) {
                                    logger.warn("Failed to de-provision test VLAN " + entry.getValue() + " post-test: " + t.getMessage());
                                }
                            }
                        }
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        } catch( Throwable ignore ) {
            // ignore
        }
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
            } catch( Throwable ignore ) {
                // ignore
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
                        if( ( firewall.getProviderVlanId() != null ) == inVlan || !support.getCapabilities().requiresVLAN().equals(Requirement.NONE)) {
                            if( firewall.isActive() && firewall.isAvailable() ) {
                                String id = firewall.getProviderFirewallId();

                                if( id != null && support.getRules(id).iterator().hasNext() ) {
                                    if( inVlan ) {
                                        testVLANFirewalls.put(DaseinTestManager.STATELESS, id);
                                    } else {
                                        testGeneralFirewalls.put(DaseinTestManager.STATELESS, id);
                                    }
                                    return firewall.getProviderFirewallId();
                                }
                            }
                            if( defaultFirewall == null ) {
                                defaultFirewall = firewall;
                            } else if( ( firewall.isActive() && firewall.isAvailable() ) && ( !defaultFirewall.isActive() || !defaultFirewall.isAvailable() ) ) {
                                defaultFirewall = firewall;
                            }
                        }
                    }
                    if( defaultFirewall != null ) {
                        String id = defaultFirewall.getProviderFirewallId();

                        if( id != null ) {
                            if( inVlan ) {
                                testVLANFirewalls.put(DaseinTestManager.STATELESS, id);
                            } else {
                                testGeneralFirewalls.put(DaseinTestManager.STATELESS, id);
                            }
                        }
                        return id;
                    }
                }
            } catch( Throwable ignore ) {
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
                        } else if( defaultLB == null ) {
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
            } catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    private @Nullable String findStatelessSSLCertificate() {
        NetworkServices networkServices = provider.getNetworkServices();

        if( networkServices != null ) {
            LoadBalancerSupport support = networkServices.getLoadBalancerSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Iterator<SSLCertificate> certificates = support.listSSLCertificates().iterator();
                    if ( certificates.hasNext() ) {
                        SSLCertificate certificate = certificates.next();
                        testSSLCertificates.put(DaseinTestManager.STATELESS, certificate.getCertificateName());
                        return certificate.getCertificateName();
                    }
                }
            } catch( Throwable ignore ) {
                // ignore
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
                        } else if( ( firewall.isActive() && firewall.isAvailable() ) && ( !defaultFirewall.isActive() || !defaultFirewall.isAvailable() ) ) {
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
            } catch( Throwable ignore ) {
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
                        if( ( address.isAssigned() || defaultAddress == null ) && ( vlan == ( address.getProviderVlanId() != null ) ) ) {
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
                            } else {
                                testIps6VLAN.put(DaseinTestManager.STATELESS, id);
                            }
                        } else {
                            if( version.equals(IPVersion.IPV4) ) {
                                testIps4Free.put(DaseinTestManager.STATELESS, id);
                            } else {
                                testIps6Free.put(DaseinTestManager.STATELESS, id);
                            }
                        }
                        return id;
                    }
                }
            } catch( Throwable ignore ) {
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
                    VLAN firstVlan = null; //will only be used if we can't satisfy all conditions below
                    Subnet defaultSubnet = null;
                    InternetGateway defaultInternetGateway = null;
                    RoutingTable defaultRouteTable = null;
                    for( VLAN vlan : vlanSupport.listVlans() ) {
                        if( defaultVlan == null || VLANState.AVAILABLE.equals(vlan.getCurrentState()) ) {
                            Subnet foundSubnet = null;
                            // other tests depend on this being correct
                            if( vlan.getCidr().contains("192.168.1.") ) {
                                if( !vlanSupport.getCapabilities().getSubnetSupport().equals(Requirement.NONE) ) {
                                    for( Subnet subnet : vlanSupport.listSubnets(vlan.getProviderVlanId()) ) {
                                        if( foundSubnet == null || SubnetState.AVAILABLE.equals(subnet.getCurrentState()) ) {
                                            foundSubnet = subnet;
                                            if( SubnetState.AVAILABLE.equals(subnet.getCurrentState()) ) {
                                                InternetGateway foundInternetGateway = null;
                                                if( vlanSupport.isConnectedViaInternetGateway(vlan.getProviderVlanId()) ) {
                                                    for( InternetGateway igateway : vlanSupport.listInternetGateways(vlan.getProviderVlanId()) ) {
                                                        if( foundInternetGateway == null ) {
                                                            foundInternetGateway = vlanSupport.getInternetGatewayById(igateway.getProviderInternetGatewayId());
                                                            if( foundInternetGateway != null ) {
                                                                if( defaultRouteTable == null ) {
                                                                    for( RoutingTable rtb : vlanSupport.listRoutingTablesForVlan(vlan.getProviderVlanId()) ) {
                                                                        defaultRouteTable = vlanSupport.getRoutingTable(rtb.getProviderRoutingTableId());
                                                                        defaultVlan = vlan;
                                                                        defaultSubnet = foundSubnet;
                                                                        defaultInternetGateway = foundInternetGateway;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if( defaultVlan == null ) {
                                // other tests demend on this being correct
                                if( vlan.getCidr().contains("192.168.1.") ) {
                                    defaultVlan = vlan;
                                }
                            }
                            if (firstVlan == null) {
                                firstVlan = vlan;
                            }
                            if( VLANState.AVAILABLE.equals(vlan.getCurrentState()) && defaultInternetGateway != null && defaultRouteTable != null && ( ( foundSubnet != null && SubnetState.AVAILABLE.equals(foundSubnet.getCurrentState()) ) || vlanSupport.getCapabilities().getSubnetSupport().equals(Requirement.NONE) ) ) {
                                break;
                            }
                        }
                    }
                    String id = null;

                    if( defaultVlan != null ) {
                        id = defaultVlan.getProviderVlanId();
                        testVLANs.put(DaseinTestManager.STATELESS, id);
                    }
                    else {
                        // couldn't find a vlan satisfying all requirements so use the first one if found
                        if (firstVlan != null) {
                            id = firstVlan.getProviderVlanId();
                            testVLANs.put(DaseinTestManager.STATELESS, id);
                        }
                    }
                    if( defaultSubnet != null ) {
                        testSubnets.put(DaseinTestManager.STATELESS, defaultSubnet.getProviderSubnetId());
                    }
                    if( defaultInternetGateway != null ) {
                        testInternetGateways.put(DaseinTestManager.STATELESS, defaultInternetGateway.getProviderInternetGatewayId());
                    }
                    if( defaultRouteTable != null ) {
                        testRouteTables.put(DaseinTestManager.STATELESS, defaultRouteTable.getProviderRoutingTableId());
                    }
                    return id;
                }
            } catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nullable String getTestFirewallId(@Nonnull String label, boolean provisionIfNull, @Nullable String vlanId) {
        HashMap<String, String> map = ( vlanId == null ? testGeneralFirewalls : testVLANFirewalls );
        if( label.equalsIgnoreCase(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String, String> entry : map.entrySet() ) {
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
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestLoadBalancerId(@Nonnull String label, @Nonnull String lbNamePrefix, boolean provisionIfNull, boolean withHealthCheck) {
        if( label.equalsIgnoreCase(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String, String> entry : testLBs.entrySet() ) {
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
                    return provisionLoadBalancer(label, lbNamePrefix, false, false, withHealthCheck);
                } catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        return null;
    }

    public @Nullable String getTestSSLCertificateName(@Nonnull String label, boolean provisionIfNull) {
        if( label.equalsIgnoreCase(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String, String> entry : testSSLCertificates.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if (id != null) {
                        return id;
                    }
                }
            }
            return findStatelessSSLCertificate();
        }
        String id = testSSLCertificates.get(label);

        if ( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            NetworkServices services = provider.getNetworkServices();

            if( services != null ) {
                try {
                    return provisionSSLCertificate(label, null);
                } catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        return null;
    }

    public @Nullable String getTestNetworkFirewallId(@Nonnull String label, boolean provisionIfNull, @Nullable String vlanId) {
        if( label.equalsIgnoreCase(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String, String> entry : testNetworkFirewalls.entrySet() ) {
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
                } catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
        return null;
    }

    public @Nullable String getTestStaticIpId(@Nonnull String label, boolean provisionIfNull, @Nullable IPVersion version, boolean inVlan, @Nullable String vlanId) {
        if( version == null ) {
            NetworkServices services = provider.getNetworkServices();
            IpAddressSupport support = ( services == null ? null : services.getIpAddressSupport() );

            if( support == null ) {
                return null;
            }
            try {
                for( IPVersion v : support.getCapabilities().listSupportedIPVersions() ) {
                    String id = getTestStaticIpId(label, provisionIfNull, v, inVlan, vlanId);

                    if( id != null ) {
                        return id;
                    }
                }
                return null;
            } catch( Throwable ignore ) {
                return null;
            }
        }
        Map<String, String> map;

        if( inVlan ) {
            map = ( version.equals(IPVersion.IPV4) ? testIps4VLAN : testIps6VLAN );
        } else {
            map = ( version.equals(IPVersion.IPV4) ? testIps4Free : testIps6Free );
        }
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String, String> entry : map.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        try {
                            @SuppressWarnings("ConstantConditions") IpAddress addr = provider.getNetworkServices().getIpAddressSupport().getIpAddress(id);

                            if( addr != null ) {
                                return id;
                            }
                        } catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
            return findStatelessIP(version, inVlan);
        }
        String id = map.get(label);

        if( id != null ) {
            try {
                @SuppressWarnings("ConstantConditions") IpAddress addr = provider.getNetworkServices().getIpAddressSupport().getIpAddress(id);

                if( addr != null ) {
                    return id;
                }
            } catch( Throwable ignore ) {
                // ignore
            }
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
                        } else {
                            return provisionAddress(support, label, version, null);
                        }
                    } catch( Throwable t ) {
                        try {
                            if( support.isSubscribed() ) {
                                logger.warn("Failed to provision test IP address under label " + label + ": " + t.getMessage());
                            }
                        } catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
        }
        return null;
    }
    public @Nullable String getTestSubnetId(@Nonnull String label, boolean provisionIfNull, @Nullable String vlanId, @Nullable String preferredDataCenterId) {
    	return getTestSubnetId(label, "dsnlb", provisionIfNull, vlanId, preferredDataCenterId);
    }
    
    public @Nullable String getTestSubnetId(@Nonnull String label, @Nonnull String lbName, boolean provisionIfNull, @Nullable String vlanId, @Nullable String preferredDataCenterId) {
        String id;
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String, String> entry : testSubnets.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            findStatelessVLAN();
        }
        id = testSubnets.get(label);

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
                        id = provisionSubnet(support, label, vlanId, "dsnsub", preferredDataCenterId);
                        // wait for subnet to be ready for describe
                        try {
                            Thread.sleep(1000L);
                        } catch( InterruptedException ignore ) {
                        }
                        return id;
                    } catch( Throwable t ) {
                        logger.warn("Failed to provision test subnet for " + vlanId + ": " + t.getMessage());
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestInternetGatewayId(@Nonnull String label, boolean provisionIfNull, @Nullable String vlanId, @Nullable String preferredDataCenterId) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String, String> entry : testInternetGateways.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
        }
        String id = testInternetGateways.get(label);

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
                        } else {
                            String internetGatewayId = support.getAttachedInternetGatewayId(vlanId);
                            if( internetGatewayId != null ) {
                                return internetGatewayId;
                            }
                        }
                        return provisionInternetGateway(support, label, vlanId);
                    } catch( Throwable t ) {
                        logger.warn("Failed to provision test internet gateway for " + vlanId + ": " + t.getMessage());
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestVLANId(@Nonnull String label, boolean provisionIfNull, @Nullable String preferredDataCenterId) {
        String id = null;
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String, String> entry : testVLANs.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    id = entry.getValue();
                    if( id != null ) {
                        return id;
                    }
                }
            }
            id = findStatelessVLAN();
        }
        if( id != null ) {
            return id;
        }
        id = testVLANs.get(label);
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
                    } catch( Throwable ignore ) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestRoutingTableId(@Nonnull String label, boolean provisionIfNull, @Nullable String vlanId, @Nullable String preferredDataCenterId) {
        NetworkServices services = provider.getNetworkServices();
        String id;
        if( services != null ) {
            VLANSupport support = services.getVlanSupport();
            if( support != null ) {
                if( label.equals(DaseinTestManager.STATELESS) ) {
                    for( Map.Entry<String, String> entry : testRouteTables.entrySet() ) {
                        if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                            id = entry.getValue();
                            try {
                                RoutingTable rtb = support.getRoutingTable(id);
                                if( rtb != null ) {
                                    if( vlanId != null ) {
                                        if( rtb.getProviderVlanId().equalsIgnoreCase(vlanId) ) {
                                            return id;
                                        }
                                    } else {
                                        return id;
                                    }
                                }
                            } catch( Exception e ) {
                                // ignore
                            }
                        }
                    }
                }
                id = testRouteTables.get(label);
                try {
                    RoutingTable rtb = support.getRoutingTable(id);
                    if( rtb != null ) {
                        if( vlanId != null ) {
                            if( rtb.getProviderVlanId().equalsIgnoreCase(vlanId) ) {
                                return id;
                            }
                        } else {
                            return id;
                        }
                    }
                } catch( Exception e ) {
                    // ignore
                }
                if( provisionIfNull ) {
                    try {
                        if( vlanId == null ) {
                            String vId = getTestVLANId(label, true, preferredDataCenterId);
                            try {
                                Thread.sleep(15000L);
                            } catch( InterruptedException ignore ) {
                            }
                            if( vId != null ) {
                                return provisionRoutingTable(support, vId, label, "dsnrtb");
                            }
                        } else {
                            return provisionRoutingTable(support, vlanId, label, "dsnrtb");
                        }
                    } catch( Throwable ignore ) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestZoneId(@Nonnull String label, boolean provisionIfNull) {
        if( label.equalsIgnoreCase(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String, String> entry : testZones.entrySet() ) {
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
                    } catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nonnull String provisionAddress(@Nonnull IpAddressSupport support, @Nonnull String label, @Nullable IPVersion version, @Nullable String vlanId) throws CloudException, InternalException {
        if( version == null ) {
            for( IPVersion v : support.getCapabilities().listSupportedIPVersions() ) {
                if( support.getCapabilities().isRequestable(v) ) {
                    version = v;
                    break;
                }
            }
        }
        if( version == null ) {
            throw new CloudException("No IP version is requestable");
        }
        Map<String, String> map;

        if( vlanId == null ) {
            map = ( version.equals(IPVersion.IPV4) ? testIps4Free : testIps6Free );
        } else {
            map = ( version.equals(IPVersion.IPV4) ? testIps4VLAN : testIps6VLAN );
        }
        String id = null;

        if( vlanId == null ) {
            id = support.request(version);
        }
        else {
            if( support.getCapabilities().identifyVlanForVlanIPRequirement().equals(Requirement.NONE) ) {
                id = support.requestForVLAN(version);
            } else {
                id = support.requestForVLAN(version, vlanId);
            }
        }
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized ( map ) {
            while( map.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            map.put(label, id);
        }
        return id;
    }

    public @Nonnull FirewallRuleCreateOptions constructRuleCreateOptions(int port, Direction direction, Permission permission) throws CloudException, InternalException {
        NetworkServices services = provider.getNetworkServices();

        if( services == null ) {
            throw new OperationNotSupportedException("No network services in cloud");
        }

        FirewallSupport support = services.getFirewallSupport();

        if( support == null ) {
            throw new OperationNotSupportedException("No firewall support in cloud");
        }
        if( !support.getCapabilities().supportsRules(direction, permission, false) ) {
            throw new OperationNotSupportedException("Firewall rules are not supported for " + direction + "/" + permission);
        }
        RuleTarget sourceEndpoint, destinationEndpoint;

        if( direction.equals(Direction.INGRESS) ) {
            sourceEndpoint = RuleTarget.getCIDR(TEST_CIDR);
            destinationEndpoint = null;
        } else {
            destinationEndpoint = RuleTarget.getCIDR(TEST_CIDR);
            sourceEndpoint = null;
        }
        return FirewallRuleCreateOptions.getInstance(direction, permission, sourceEndpoint, Protocol.TCP, destinationEndpoint, port, port);
    }

    public @Nonnull String provisionFirewall(@Nonnull String label, @Nullable String vlanId) throws CloudException, InternalException {
        return provisionFirewall(label, vlanId, null);
    }

    public @Nonnull String provisionFirewall(@Nonnull String label, @Nullable String vlanId, @Nullable FirewallRuleCreateOptions firstRule) throws CloudException, InternalException {
        String tmp = String.valueOf(random.nextInt(10000));
        String name = "dsnfw" + tmp;
        String description = "Dasein Cloud Integration Test Firewall";
        FirewallCreateOptions options;

        if( vlanId == null ) {
            if( firstRule == null ) {
                options = FirewallCreateOptions.getInstance(name, description);
            } else {
                options = FirewallCreateOptions.getInstance(name, description, firstRule);
            }
        } else {
            if( firstRule == null ) {
                options = FirewallCreateOptions.getInstance(vlanId, name, description);
            } else {
                options = FirewallCreateOptions.getInstance(vlanId, name, description, firstRule);
            }
        }
        String id = options.build(provider, false);

        if( vlanId == null ) {
            synchronized ( testGeneralFirewalls ) {
                while( testGeneralFirewalls.containsKey(label) ) {
                    label = label + random.nextInt(9);
                }
                testGeneralFirewalls.put(label, id);
            }
        } else {
            synchronized ( testVLANFirewalls ) {
                while( testVLANFirewalls.containsKey(label) ) {
                    label = label + random.nextInt(9);
                }
                testVLANFirewalls.put(label, id);
            }
        }
        return id;
    }

    public @Nonnull String provisionLoadBalancer(@Nonnull String label, @Nullable String namePrefix, boolean internal) throws CloudException, InternalException {
        return provisionLoadBalancer(label, namePrefix, internal, false, false);
    }

    public @Nonnull String provisionLoadBalancer(@Nonnull String label, @Nullable String namePrefix,
                                 boolean internal, boolean withHttps, boolean withHealthCheck) throws CloudException, InternalException {
    	NetworkServices services = provider.getNetworkServices();

        if( services == null ) {
            throw new CloudException("This cloud does not support load balancers");
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            throw new CloudException("This cloud does not support load balancers");
        }

        String name = ( namePrefix == null ? "dsnlb" + random.nextInt(10000) : namePrefix + random.nextInt(10000) );
        String description = "Dasein Cloud LB Test";
        LoadBalancerCreateOptions options;

        if( !support.getCapabilities().isAddressAssignedByProvider() && support.getCapabilities().getAddressType().equals(LoadBalancerAddressType.IP) ) {
            IpAddressSupport ipSupport = services.getIpAddressSupport();

            if( ipSupport == null ) {
                options = LoadBalancerCreateOptions.getInstance(name, description);
            } else {
                IpAddress address = null;

                for( IPVersion version : ipSupport.getCapabilities().listSupportedIPVersions() ) {
                    Iterator<IpAddress> addrs = ipSupport.listIpPool(version, true).iterator();

                    if( addrs.hasNext() ) {
                        address = addrs.next();
                        break;
                    }
                }
                if( address == null ) {
                    for( IPVersion version : ipSupport.getCapabilities().listSupportedIPVersions() ) {
                        if( ipSupport.getCapabilities().isRequestable(version) ) {
                            address = ipSupport.getIpAddress(ipSupport.request(version));
                            if( address != null ) {
                                break;
                            }
                        }
                    }
                }
                if( address == null ) {
                    options = LoadBalancerCreateOptions.getInstance(name, description);
                } else {
                    options = LoadBalancerCreateOptions.getInstance(name, description, address.getProviderIpAddressId());
                }
            }
        } else {
        	// not sure if options need tweeking
            options = LoadBalancerCreateOptions.getInstance(name, description);
        }

        if( support.getCapabilities().identifyListenersOnCreateRequirement().equals(Requirement.REQUIRED) ) {
            final int publicPort = 1024 + random.nextInt(10000);
            final int privatePort = 1024 + random.nextInt(10000);
            if ( !withHttps ) {
                options.havingListeners(LbListener.getInstance(publicPort, privatePort));
            } else {
                String certificateName = provisionSSLCertificate("provision", "dsnssltest");
                try {
                    // Wait as in some clouds it takes time before SSL certificate can be linked to a listener
                    Thread.sleep(5000L);
                } catch( InterruptedException ignore ) {
                }
                options.havingListeners(LbListener.getInstance(LbProtocol.HTTPS, publicPort, privatePort,
                        certificateName));
            }
        }
        String[] dcIds = new String[2];
        String testSubnetId = null;

        if( support.getCapabilities().identifyEndpointsOnCreateRequirement().equals(Requirement.REQUIRED) ) {
            Iterable<LbEndpointType> types = support.getCapabilities().listSupportedEndpointTypes();
            boolean vmBased = false;

            for( LbEndpointType t : types ) {
                if( t.equals(LbEndpointType.VM) ) {
                    vmBased = true;
                    break;
                }
            }
            if( !internal ) {
                options.asType(LbType.EXTERNAL);
            } else {
                options.asType(LbType.INTERNAL);
            }
            if( vmBased ) {
                ComputeResources c = DaseinTestManager.getComputeResources();

                if( c != null ) {
                    String server1 = null;
                    if( !internal ) {
                        server1 = c.getTestVmId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, null);
                    } else {
                        String vlanId = getTestVLANId(DaseinTestManager.STATEFUL, true, null);
                        try {
                            Thread.sleep(750L);
                        } catch( InterruptedException ignore ) {
                        }
                        VLANSupport vlanSupport = services.getVlanSupport();
                        if( vlanSupport == null ) {
                            throw new InternalException("No VLAN support");
                        }
                        VLAN vlan = vlanSupport.getVlan(vlanId);
                        if( vlan == null ) {
                            throw new CloudException("No such VLAN: " + vlanId);
                        }
                        String subnetId = getTestSubnetId(DaseinTestManager.STATEFUL, true, vlanId, vlan.getProviderDataCenterId());
                        try {
                            Thread.sleep(750L);
                        } catch( InterruptedException ignore ) {
                        }
                        Subnet subnet = services.getVlanSupport().getSubnet(subnetId);
                        if( subnet == null ) {
                            throw new CloudException("No such Subnet: " + subnetId);
                        }
                        testSubnetId = subnetId;
                        String productId = c.getTestVMProductId();
                        String imageId = c.getTestImageId(DaseinTestManager.STATELESS, false);
                        VMLaunchOptions vmOptions = VMLaunchOptions.getInstance(productId, imageId, "dsnnetl" + ( System.currentTimeMillis() % 10000 ), "Dasein Network Launch " + System.currentTimeMillis(), "Test launch for a VM in a network");
                        vmOptions.inVlan(null, vlan.getProviderDataCenterId(), subnet.getProviderSubnetId());
                        server1 = c.provisionVM(provider.getComputeServices().getVirtualMachineSupport(), "internalLbLaunch", vmOptions, vlan.getProviderDataCenterId());
                    }

                    if( server1 != null ) {
                        @SuppressWarnings("ConstantConditions") VirtualMachine vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(server1);

                        if( vm != null ) {
                            dcIds[0] = vm.getProviderDataCenterId();
                        }
                    }
                    String server2 = null;
                    if( !internal ) {
                        @SuppressWarnings("ConstantConditions") Iterator<DataCenter> it = provider.getDataCenterServices().listDataCenters(provider.getContext().getRegionId()).iterator();
                        String targetDC = dcIds[0];

                        if( it.hasNext() ) {
                            String dcId = it.next().getProviderDataCenterId();

                            if( !dcId.equals(dcIds[0]) ) {
                                dcIds[1] = dcId;
                                targetDC = dcId;
                            }
                        }
                        server2 = c.getTestVmId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, targetDC);
                    }

                    if( server1 != null && server2 != null ) {
                        options.withVirtualMachines(server1, server2);
                    } else if( server1 != null ) {
                        options.withVirtualMachines(server1);
                    }
                }
            } else {
                options.withIpAddresses("207.32.82.72");
            }
        }
        if( support.getCapabilities().isDataCenterLimited() ) {
            if( dcIds[0] != null && dcIds[1] != null ) {
                options.limitedTo(dcIds);
            } else if( dcIds[0] != null ) {
                options.limitedTo(dcIds[0]);
            } else if( dcIds[1] != null ) {
                options.limitedTo(dcIds[1]);
            } else {
                @SuppressWarnings("ConstantConditions") Iterator<DataCenter> it = provider.getDataCenterServices().listDataCenters(provider.getContext().getRegionId()).iterator();

                if( it.hasNext() ) {
                    options.limitedTo(it.next().getProviderDataCenterId());
                }
            }
        }
        if( internal && testSubnetId != null ) {
            options.withProviderSubnetIds(testSubnetId);
        }

        if( withHealthCheck ) {
            options.withHealthCheckOptions(HealthCheckOptions.getInstance(
            		name, "lb desc", name, TEST_HC_HOST, TEST_HC_PROTOCOL, TEST_HC_PORT, TEST_HC_PATH, 60, 100, 3, 10));
        }

        String id = options.build(provider);

        synchronized ( testLBs ) {
            while( testLBs.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testLBs.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionSSLCertificate(@Nonnull String label, @Nullable String namePrefix) throws CloudException, InternalException {
        NetworkServices services = provider.getNetworkServices();

        if( services == null ) {
            throw new CloudException("This cloud does not support load balancers");
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            throw new CloudException("This cloud does not support load balancers");
        }

        String name = ( namePrefix == null ? "dsnssl" + random.nextInt(10000) : namePrefix + random.nextInt(10000) );

        final String testSslCertificateBody = "-----BEGIN CERTIFICATE-----\n" +
                "MIIDdTCCAl2gAwIBAgIJAJ0yH+H1fw8nMA0GCSqGSIb3DQEBBQUAMFExCzAJBgNV\n" +
                "BAYTAlVTMRUwEwYDVQQHDAxEZWZhdWx0IENpdHkxHDAaBgNVBAoME1RoZSBXZWF0\n" +
                "aGVyIENoYW5uZWwxDTALBgNVBAMMBHRlc3QwHhcNMTQwNDE4MTUyMzI2WhcNMjQw\n" +
                "NDE1MTUyMzI2WjBRMQswCQYDVQQGEwJVUzEVMBMGA1UEBwwMRGVmYXVsdCBDaXR5\n" +
                "MRwwGgYDVQQKDBNUaGUgV2VhdGhlciBDaGFubmVsMQ0wCwYDVQQDDAR0ZXN0MIIB\n" +
                "IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyZc/utwao8R+vfCkTx50vNHl\n" +
                "2aL7L4Gpm4bdHGU4LSmwaS0Q6eRob+bZEKb5Wfb0xmkx0Rl+VEkoxyVTxg8JtrIr\n" +
                "8m63ohr24FH8fQpx2jZTyJKYLO94Ls2sTY4IJiYX6tnHOSQEnmk+BHwk7uTAI4Il\n" +
                "TiY70tRCbUuIgei18jAR2FsevYoereotkukA0aD3n50JCpBNC4rgiCFZD4dP6tFx\n" +
                "GUSKpCoxjH+pcW3txCV3op1JIK7nQ5L21vw4phSlOznoxO9QY/phqGQGorY7KxRi\n" +
                "gvX6OjD6suRyrqKmOulBdTJP5Sqgrl/yET2Yhocs/rSGnaLA3Y7b6DkKts5O6wID\n" +
                "AQABo1AwTjAdBgNVHQ4EFgQUVfdyopff30iphehCoyXsk1FJ85IwHwYDVR0jBBgw\n" +
                "FoAUVfdyopff30iphehCoyXsk1FJ85IwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0B\n" +
                "AQUFAAOCAQEANQBsY17sSckU+LRDwi3KdaCljkgVaCeRBxvu29jB4Vzhbagwya1u\n" +
                "PhO2avn2VErsv8wj56qY68SM+GAb7Jl7ZY5V1us+YEy5zAoLj23bCMMjBUOOdNNx\n" +
                "BeQx32D5lB3qKMiTNSJO6N3OsAp+gcAcwb63S4bpaGQlfX4gy4nh9H1kY3X0ZAty\n" +
                "Hip4sKXZgj9nJKtmIH7jP91NODfcwvRSdrElGU7rOiIy4f228tQhPMPkEB0CYlhT\n" +
                "pZUfKI90+uyjEQEc9Af11VNP3+g6CB3dgLkYOFStxG2eueTyz1V70b94dBGeMBYW\n" +
                "Zp1tE1iQ+jfkGFJuEiqsIMF6NAcjXFe/YA==\n" +
                "-----END CERTIFICATE-----\n";
        final String testSslCertificatePrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIIEowIBAAKCAQEAyZc/utwao8R+vfCkTx50vNHl2aL7L4Gpm4bdHGU4LSmwaS0Q\n" +
                "6eRob+bZEKb5Wfb0xmkx0Rl+VEkoxyVTxg8JtrIr8m63ohr24FH8fQpx2jZTyJKY\n" +
                "LO94Ls2sTY4IJiYX6tnHOSQEnmk+BHwk7uTAI4IlTiY70tRCbUuIgei18jAR2Fse\n" +
                "vYoereotkukA0aD3n50JCpBNC4rgiCFZD4dP6tFxGUSKpCoxjH+pcW3txCV3op1J\n" +
                "IK7nQ5L21vw4phSlOznoxO9QY/phqGQGorY7KxRigvX6OjD6suRyrqKmOulBdTJP\n" +
                "5Sqgrl/yET2Yhocs/rSGnaLA3Y7b6DkKts5O6wIDAQABAoIBACOO8kbbnDdW6aRH\n" +
                "VjQ+gwjrXUfOX9A5ZtlwKIBhuk79E4j50gnvqBxU8+TkDwe3b+WvmIHxpT7oyLCX\n" +
                "/PbqoCQBuY7ByNJnPzTCQW8s8Hg1LQIsGXuTofdfgA0OCJHyFjXuxB1oJQhsN+xC\n" +
                "maEp6FpbEol+ZP8DQdRVhnajvbRCRgWyO7sEYGRDvuYVo4Pn4kXFsIXajzA5ZnvJ\n" +
                "U/VeicaPaj4T1uI2EDFDAZYLVd3x9vKop+8FKqhuZMhrSNbBoD+qucOcD7fZ3jjT\n" +
                "ZS8m4R3TkYLx0myLB0zOu0zARg0GUbMmIXpBk5IxwqIp6iZfXz8U0DKU//2OKiFC\n" +
                "TmJoNwECgYEA9M4rfvBugItZzTQ+woPx84+MSirwJUU5ToklSpVSuQZE3nuucRV7\n" +
                "bdQp0MGMYIFUvGVdwH9a/MUCYLtfCvsYbk0leTu6Ycb6h74TsIptIcGEZplyMOM3\n" +
                "1Yt5F5VLPAmKBHRXXRrxQa2OWCGKuywsrz0HHAD8BGzNtyUka+dWoBsCgYEA0s8v\n" +
                "alvmCgNKrnU9yM939WkAfAE330Dm8m9PVCGRnmXZ+ZpDQUjyveN9/9EUIjn2AR24\n" +
                "1Svu5WjtezSXqJLWJYoYH9mCdKOE8WsTQl+Xk4zkJqQrniX0oYaZam9dBZ/TIYK4\n" +
                "Hz/K3qdpJUbDEW6xh+OaTIH2OaNJY32I2NC9GXECgYAb3awN0wiBEVuzhBLwyVwt\n" +
                "QVXSy3hyhaK0UeAw0TaNYS1Ntf5xWOSn59KqtJ1qDs66cz9svhJ5W2Od5zY2Zcau\n" +
                "J5HwbuAUaTXzZauQGPG7Oe/8TdM1xWeBo1KxYIkj2GIhh6y6KGr18u+VEJxeGfUs\n" +
                "LWI1ydbmGgyAoHW44qh1qQKBgCMJJAw9McJAQc001wvkzz8OMHJrkWmdU8S/EyQc\n" +
                "YCM/Mjb1mG/lO9KrWGmHyhzWHTiaQ/nJz255Pd7YIsx1evnKNbA1aiUQeCvXa+AA\n" +
                "GyT+qXxylH04OawOvridwYwJwAE1xHwNEh5nHGaBmDHxf7fh7+b/QnjZ1nyehHvk\n" +
                "VUlBAoGBAPBNvBRDMAPEAaxl6QSxZD2O3dG51S+JEy0Xw7egiQlFEQTPiazkGqzc\n" +
                "pWusEXgU82S4R2DYhcCk3dJ1AsE1hexDXTvrBIK1q9yAUUEw9X9YPhfzAadULNcx\n" +
                "SoUufwzG5X1lxMIzP95FAvFgOwGY1yqoFGLZrp5Gm37RBNqoAp7a\n" +
                "-----END RSA PRIVATE KEY-----";

        SSLCertificateCreateOptions options = SSLCertificateCreateOptions.getInstance(testSslCertificateBody, null,
                testSslCertificatePrivateKey, name, null);

        final SSLCertificate sslCertificate = support.createSSLCertificate(options);
        final String certificateName = sslCertificate.getCertificateName();

        synchronized ( testSSLCertificates ) {
            while( testSSLCertificates.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testSSLCertificates.put(label, certificateName);
        }
        return certificateName;
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

        synchronized ( testNetworkFirewalls ) {
            while( testNetworkFirewalls.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testNetworkFirewalls.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionSubnet(@Nonnull VLANSupport support, @Nonnull String label, @Nonnull String vlanId, @Nonnull String namePrefix, @Nullable String preferredDataCenterId) throws CloudException, InternalException {
        if( preferredDataCenterId == null && support.getCapabilities().isSubnetDataCenterConstrained() ) {
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
            options = SubnetCreateOptions.getInstance(vlanId, cidrs[cidrCount], namePrefix + ( System.currentTimeMillis() % 10000 ), "Dasein Cloud Integration test subnet");
        } else {
            options = SubnetCreateOptions.getInstance(vlanId, preferredDataCenterId, cidrs[cidrCount], namePrefix + ( System.currentTimeMillis() % 10000 ), "Dasein Cloud Integration test subnet");
        }
        cidrCount++;
        HashMap<String, Object> tags = new HashMap<String, Object>();

        tags.put("dsntestcase", "true");
        options.withMetaData(tags);
        String id;

        options.withSupportedTraffic(support.getCapabilities().listSupportedIPVersions().iterator().next());
        try {
            id = options.build(provider);
        } catch( CloudException e ) {
            if( e.getMessage().contains("conflicts with another") ) {
                return provisionSubnet(support, label, vlanId, namePrefix, preferredDataCenterId);
            }
            throw e;
        }
        synchronized ( testSubnets ) {
            while( testSubnets.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testSubnets.put(label, id);
        }
        return id;
    }

    public @Nullable String provisionInternetGateway(@Nonnull VLANSupport support, @Nonnull String label, @Nonnull String vlanId) throws CloudException, InternalException {
        if( support.getCapabilities().isSubnetDataCenterConstrained() ) {
            VLAN vlan = support.getVlan(vlanId);
            if( vlan == null ) {
                throw new CloudException("No such VLAN: " + vlanId);
            }
        }
        String id = support.createInternetGateway(vlanId);
        synchronized ( testInternetGateways ) {
            while( testInternetGateways.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testInternetGateways.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionVLAN(@Nonnull VLANSupport support, @Nonnull String label, @Nonnull String namePrefix, @Nullable String preferredDataCenterId) throws CloudException, InternalException {
        String id;

        //if( support.isVlanDataCenterConstrained() && preferredDataCenterId == null ) {
        //preferredDataCenterId = DaseinTestManager.getDefaultDataCenterId(true);
        //}
        id = support.createVlan("192.168.1.0/24", namePrefix + ( System.currentTimeMillis() % 10000 ), "Test VLAN for the Dasein Cloud Integration tests", "example.com", new String[]{"192.168.1.1"}, new String[]{"192.168.1.1"}).getProviderVlanId();
        if( id == null ) {
            throw new CloudException("No VLAN was created");
        }
        synchronized ( testVLANs ) {
            while( testVLANs.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testVLANs.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionRoutingTable(@Nonnull VLANSupport support, @Nonnull String vlanId, @Nonnull String label, @Nonnull String namePrefix) throws CloudException, InternalException {
        String id = support.createRoutingTable(vlanId, namePrefix + ( System.currentTimeMillis() % 10000 ), "Test Routing Table for the Dasein Cloud Integration tests");
        if( id == null ) {
            throw new CloudException("No Routing Table was created");
        }
        synchronized ( testRouteTables ) {
            while( testRouteTables.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testRouteTables.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionDNSZone(@Nonnull DNSSupport support, @Nonnull String label, @Nonnull String domainPrefix, @Nonnull String tld) throws CloudException, InternalException {
        String name = domainPrefix + ( System.currentTimeMillis() % 10000 ) + "." + tld;
        String id = support.createDnsZone(name, name, "Dasein Cloud Test Zone");

        synchronized ( testZones ) {
            while( testZones.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testZones.put(label, id);
        }
        return id;
    }
}
