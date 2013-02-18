/**
 * Copyright (C) 2009-2012 enStratus Networks Inc
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.cloud.identity.ShellKeySupport;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.LbAlgorithm;
import org.dasein.cloud.network.LbListener;
import org.dasein.cloud.network.LbProtocol;
import org.dasein.cloud.network.LoadBalancerSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.SubnetState;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BaseTestCase extends TestCase {
    static private int actualFirewallReuses   = 0;
    static private int actualImageReuses      = 0;
    static private int actualVlanReuses       = 0;
    static private int actualVmReuses         = 0;
    static private int actualVolumeReuses     = 0;
    static private int expectedFirewallReuses = 0;
    static private int expectedImageReuses    = 0;
    static private int expectedVlanReuses     = 0;
    static private int expectedVmReuses       = 0;
    static private int expectedVolumeReuses   = 0;

    static public void addExpectedFirewallReuses(int count) {
        expectedFirewallReuses += count;
    }

    static public void addExpectedImageReuses(int count) {
        expectedImageReuses += count;
    }

    static public void addExpectedVlanReuses(int count) {
        expectedVlanReuses += count;
    }

    static public void addExpectedVolumeReuses(int count) {
        expectedVolumeReuses += count;
    }

    static public void addExpectedVmReuses(int count) {
        expectedVmReuses += count;
    }

    static public ProviderContext getTestContext(Class<? extends CloudProvider> providerClass) {
        ProviderContext ctx = new ProviderContext();
        Properties props = System.getProperties();
        String publicKey, privateKey;
        String apiVersion;

        apiVersion = props.getProperty("apiVersion");
        if( apiVersion != null ) {
            Properties p = new Properties();

            p.setProperty("apiVersion", apiVersion);
            ctx.setCustomProperties(p);
        }
        publicKey = props.getProperty("apiSharedKey");
        privateKey = props.getProperty("apiSecretKey");  
        if( publicKey != null && privateKey != null ) {
            ctx.setAccessKeys(publicKey.getBytes(), privateKey.getBytes());
        }
        ctx.setAccountNumber(props.getProperty("accountNumber"));
        ctx.setCloudName(props.getProperty("cloudName"));
        ctx.setEndpoint(props.getProperty("endpoint"));
        ctx.setProviderName(props.getProperty("providerName"));
        ctx.setRegionId(props.getProperty("regionId"));

        publicKey = props.getProperty("x509Cert");
        privateKey = props.getProperty("x509Key");
        if( publicKey != null && publicKey.length() < 1 ) {
            publicKey = null;
        }
        if( privateKey != null && privateKey.length() < 1 ) {
            privateKey = null;
        }
        if( publicKey != null && privateKey != null ) {
            try {
                BufferedInputStream input = new BufferedInputStream(new FileInputStream(publicKey));
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] tmp = new byte[10240];
                int count;
                
                while( (count = input.read(tmp, 0, 10240)) > -1 ) {
                    output.write(tmp, 0, count);
                    output.flush();
                }
                ctx.setX509Cert(output.toByteArray());
                input.close();
                output.close();
                input = new BufferedInputStream(new FileInputStream(privateKey));
                output = new ByteArrayOutputStream();
                while( (count = input.read(tmp, 0, 10240)) > -1 ) {
                    output.write(tmp, 0, count);
                    output.flush();
                }
                ctx.setX509Key(output.toByteArray());
                input.close();
                output.close();
            }
            catch( IOException e ) {
                fail(e.getMessage());
            }
        }
        Properties custom = new Properties();
        Enumeration<?> names = props.propertyNames();
        
        while( names.hasMoreElements() ) {
            String name = (String)names.nextElement();
            
            if( name.startsWith("test.") ) {
                custom.setProperty(name, props.getProperty(name));
            }
        }
        ctx.setCustomProperties(custom);
        return ctx;
    }
    
    private boolean verbose = true;
    private Logger  logger;

    private long launchWindow;
    private long stateChangeWindow;

    public BaseTestCase(String name) { 
        super(name);
        verbose = System.getProperty("test.verbose", "true").equalsIgnoreCase("true");
        logger = Logger.getLogger(getClass());

        try {
            launchWindow = (CalendarWrapper.MINUTE * Long.parseLong(System.getProperty("vm.maxLaunchPeriod", "15")));
        }
        catch( NumberFormatException e ) {
            launchWindow = CalendarWrapper.MINUTE * 15L;
        }
        try {
            stateChangeWindow = (CalendarWrapper.MINUTE * Long.parseLong(System.getProperty("vm.maxChangePeriod", "15")));
        }
        catch( NumberFormatException e ) {
            stateChangeWindow = CalendarWrapper.MINUTE * 10L;
        }
    }

    /*
    protected String allocateVolume(CloudProvider cloud) throws InternalException, CloudException {
        String id = cloud.getComputeServices().getVolumeSupport().create(null, 5, getTestDataCenterId());
        long timeout = (System.currentTimeMillis() + CalendarWrapper.MINUTE*5L);
        
        while( System.currentTimeMillis() < timeout ) {
            Volume volume = cloud.getComputeServices().getVolumeSupport().getVolume(id);
            
            if( volume != null && volume.getCurrentState().equals(VolumeState.AVAILABLE) ) {
                return id;
            }
            try { Thread.sleep(15000L); }
            catch( InterruptedException e ) { }
        }
        return id;
    }
    */

    protected void assertDirectoryExists(String errorMessage, CloudProvider provider, String directory) throws InternalException, CloudException {
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 2L);
        
        while( System.currentTimeMillis() < timeout ) {
            try { Thread.sleep(1000L); }
            catch( InterruptedException e ) { }
            if( provider.getStorageServices().getBlobStoreSupport().exists(directory) ) {
                return;
            }
        }
        assertTrue(errorMessage, false);
    }

    /*
    protected void assertNotDirectoryExists(String errorMessage, CloudProvider provider, String directory) throws InternalException, CloudException {
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 2L);
        
        while( System.currentTimeMillis() < timeout ) {
            try { Thread.sleep(1000L); }
            catch( InterruptedException e ) { }
            if( !provider.getStorageServices().getBlobStoreSupport().exists(directory) ) {
                return;
            }
        }
        assertTrue(errorMessage, false);
    }
    
    protected void assertObjectExists(String errorMessage, CloudProvider provider, String directory, String object, boolean multipart) throws InternalException, CloudException {
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 2L);
        
        while( System.currentTimeMillis() < timeout ) {
            try { Thread.sleep(1000L); }
            catch( InterruptedException e ) { }
            if( provider.getStorageServices().getBlobStoreSupport().getObjectSize(directory, object) != null ) {
                return;
            }
        }
        assertTrue(errorMessage, false);
    }
    
    protected void assertNotObjectExists(String errorMessage, CloudProvider provider, String directory, String object, boolean multipart) throws InternalException, CloudException {
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 2L);
        
        while( System.currentTimeMillis() < timeout ) {
            try { Thread.sleep(1000L); }
            catch( InterruptedException e ) { }
            if( provider.getStorageServices().getBlobStoreSupport().getObjectSize(directory, object) != null ) {
                return;
            }
        }
        assertTrue(errorMessage, false);
    }
    */

    long start;
    
    protected void begin() {
        start = System.currentTimeMillis();
        out("BEGIN");
    }
    
    static private final String LINE_ONE = "1: Test";
    static private final String LINE_TWO = "2: Done.";
    
    protected boolean checkTestFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        
        try {
            String line;
            
            line = reader.readLine();
            if( line == null || !line.equals(LINE_ONE) ) {
                return false;
            }
            line = reader.readLine();
            if( line == null || !line.equals(LINE_TWO) ) {
                return false;
            }
            return (reader.readLine() == null);
        }
        finally {
            reader.close();
        }
    }

    static private String         firewallToDelete = null;
    static private String         imageToDelete    = null;
    static private String         ipToRelease      = null;
    static private String         vlanToKill       = null;
    static private String         vmToKill         = null;
    static private String         volumeToKill     = null;

    protected void cleanFirewall(@Nonnull FirewallSupport support, @Nonnull String firewallId) {
        try {
            Firewall fw = support.getFirewall(firewallId);

            if( fw != null ) {
                support.delete(firewallId);
            }
        }
        catch( Throwable t ) {
            out("WARNING: Failed to clean up after test, the firewall " + firewallId + " was not removed cleanly");
        }
    }

    protected void cleanImage(@Nonnull MachineImageSupport support, @Nonnull String imageId) {
        try {
            long timeout = System.currentTimeMillis() + getStateChangeWindow();
            MachineImage img = null;

            while( timeout > System.currentTimeMillis() ) {
                try {
                    img = support.getImage(imageId);
                    if( img == null || MachineImageState.DELETED.equals(img.getCurrentState()) ) {
                        return;
                    }
                    if( !MachineImageState.PENDING.equals(img.getCurrentState()) ) {
                        break;
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
            }
            if( img != null && !MachineImageState.DELETED.equals(img.getCurrentState()) ) {
                support.remove(imageId);
            }
        }
        catch( Throwable t ) {
            out("WARNING: Failed to clean up after test, the image " + imageId + " was not removed cleanly");
        }
    }

    protected void cleanVlan(@Nonnull VLANSupport support, @Nonnull String vlanId) {
        try {
            long timeout = System.currentTimeMillis() + getStateChangeWindow();
            VLAN vlan = null;

            while( timeout > System.currentTimeMillis() ) {
                try {
                    vlan = support.getVlan(vlanId);
                    if( vlan == null ) {
                        return;
                    }
                    if( !VLANState.PENDING.equals(vlan.getCurrentState()) ) {
                        break;
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
            }
            if( vlan != null ) {
                support.removeVlan(vlanId);
            }
        }
        catch( Throwable t ) {
            out("WARNING: Failed to clean up after test, the VLAN " + vlanId + " was not removed cleanly");
        }
    }

    protected void cleanVolume(@Nonnull VolumeSupport support, @Nonnull String volumeId) {
        try {
            Volume volume = support.getVolume(volumeId);

            if( volume == null || VolumeState.DELETED.equals(volume.getCurrentState()) ) {
                return;
            }
            if( volume.getProviderVirtualMachineId() != null ) {
                try {
                    support.detach(volumeId, true);

                    long timeout = System.currentTimeMillis() + getLaunchWindow();

                    while( timeout > System.currentTimeMillis() ) {
                        try { volume = support.getVolume(volumeId); }
                        catch( Throwable ignore ) { }
                        if( volume == null || volume.getProviderVirtualMachineId() == null ) {
                            break;
                        }
                        try { Thread.sleep(15000L); }
                        catch( InterruptedException ignore ) { }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
            if( volume != null ) {
                //noinspection ConstantConditions
                support.remove(volumeId);
            }
        }
        catch( Throwable t ) {
            out("warning: Failed to clean up after test, the volume " + volumeId + " was not removed cleanly");
        }
    }

    protected void cleanUp(@Nonnull CloudProvider provider) {
        killTestVolume(provider);
        killTestVm(provider);
        killTestAddress(provider);
        killTestImage(provider);
        killTestFirewall(provider);
        killTestVlan(provider);
    }

    protected File createTestFile() {
        String fileName = "dsnupltest" + System.currentTimeMillis() + ".txt";
        
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName))));
            
            writer.println(LINE_ONE);
            writer.println(LINE_TWO);
            writer.flush();
            writer.close();
            return new File(fileName);
        }
        catch( Throwable t ) {
            return null;
        }
    }
    
    protected void end() {
        out("END (" + (System.currentTimeMillis() - start) + " millis)");
    }

    protected @Nonnull Firewall findTestFirewall(@Nonnull CloudProvider provider, @Nonnull FirewallSupport support, boolean useConfigured, boolean findFree, boolean createNew) throws CloudException, InternalException {
        Firewall firewall = null;

        if( createNew ) {
            actualFirewallReuses++;
        }
        if( firewallToDelete != null ) {
            firewall = support.getFirewall(firewallToDelete);
        }
        if( firewall == null && useConfigured ) {
            String id = getTestFirewallId();

            if( id != null ) {
                firewall = support.getFirewall(id);
            }
        }
        if( firewall == null && findFree ) {
            Iterator<Firewall> firewalls = support.list().iterator();

            if( firewalls.hasNext() ) {
                firewall = firewalls.next();
            }
        }
        if( firewall == null && createNew ) {
            firewallToDelete = support.create(getName() + (System.currentTimeMillis()%10000), "Reusable test firewall starting with " + getName());
            firewall = support.getFirewall(firewallToDelete);
        }
        if( firewall == null ) {
            Assert.fail("Unable to identify a test firewall");
        }
        return firewall;
    }

    protected @Nonnull MachineImage findTestImage(@Nonnull CloudProvider provider, @Nonnull MachineImageSupport support, boolean useConfigured, boolean findFree, boolean createNew) throws CloudException, InternalException {
        MachineImage image = null;

        if( createNew ) {
            actualImageReuses++;
        }
        if( imageToDelete != null ) {
            image = support.getImage(imageToDelete);
        }
        if( image == null && useConfigured ) {
            String id = getTestMachineImageId();

            if( id != null ) {
                image = support.getImage(id);
            }
        }
        if( image == null && findFree ) {
            Iterator<MachineImage> images = support.listImages(ImageClass.MACHINE).iterator();

            if( images.hasNext() ) {
                image = images.next();
            }
            else {
                for( Platform platform : new Platform[] { Platform.UBUNTU, Platform.WINDOWS, Platform.RHEL, Platform.CENT_OS, Platform.SOLARIS } ) {
                    images = support.searchPublicImages(null, platform, null).iterator();
                    if( images.hasNext() ) {
                        image = images.next();
                        break;
                    }
                }
            }
        }
        if( image == null && createNew ) {
            VirtualMachine vm = findTestVirtualMachine(provider, provider.getComputeServices().getVirtualMachineSupport(), false, true);
            String name = getClass().getName().substring(0, 3).toLowerCase() + "img-" + getName() + (System.currentTimeMillis()%10000);
            ImageCreateOptions options = ImageCreateOptions.getInstance(vm, name, getName() + " test case execution");

            image = support.captureImage(options);
            imageToDelete = image.getProviderMachineImageId();
        }
        if( image == null ) {
            Assert.fail("No test image could be found or created to support this test case");
        }
        return image;
    }

    protected String findTestProduct(@Nonnull CloudProvider provider, @Nonnull VirtualMachineSupport support, @Nullable Architecture architecture, boolean findFree) throws CloudException, InternalException {
        String productId = getTestProduct();

        if( productId == null && findFree ) {
            VirtualMachineProduct cheapo = null;

            for( VirtualMachineProduct p : support.listProducts(architecture) ) {
                if( cheapo == null || (p.getRamSize() != null && (cheapo.getRamSize() == null || p.getRamSize().intValue() < cheapo.getRamSize().intValue())) ) {
                    cheapo = p;
                }
            }
            if( cheapo != null ) {
                productId = cheapo.getProviderProductId();
            }
        }
        if( productId == null ) {
            Assert.fail("No test virtual machine product could be found for this test case");
        }
        return productId;
    }

    protected @Nullable VLAN findTestVLAN(@Nonnull CloudProvider provider, @Nullable VLANSupport support, boolean findFree, boolean createNew) throws CloudException, InternalException {
        VLAN vlan = null;

        if( createNew ) {
            actualVlanReuses++;
        }
        if( support == null || !support.isSubscribed() ) {
            return null;
        }
        if( vlanToKill != null ) {
            vlan = support.getVlan(vlanToKill);
        }
        if( vlan == null && findFree ) {
            Iterator<VLAN> vlans = support.listVlans().iterator();

            if( vlans.hasNext() ) {
                vlan = vlans.next();
            }
        }
        if( vlan == null && createNew && support.allowsNewVlanCreation() ) {
            vlan = support.createVlan("192.168.104.0/24", getName() + (System.currentTimeMillis()%10000), "VLAN for Dasein Cloud Integration Tests", "example.com", new String[0], new String[0]);
        }
        return vlan;
    }

    protected VirtualMachine findTestVirtualMachine(@Nonnull CloudProvider provider, @Nonnull VirtualMachineSupport support, boolean findFree, boolean createNew) throws CloudException, InternalException {
        VirtualMachine vm = null;

        if( createNew ) {
            actualVmReuses++;
        }
        if( vmToKill != null ) {
            vm = support.getVirtualMachine(vmToKill);
            if( vm != null && VmState.TERMINATED.equals(vm.getCurrentState()) ) {
                vm = null;
            }
        }
        if( vm == null && findFree ) {
            Iterable<VirtualMachine> servers = support.listVirtualMachines();

            for( VirtualMachine server : servers ) {
                if( !VmState.TERMINATED.equals(server.getCurrentState()) ) {
                    vm = server;
                    break;
                }
            }
        }
        if( vm == null && createNew ) {
            MachineImage img = findTestImage(provider, provider.getComputeServices().getImageSupport(), true, true, false);
            String name = getClass().getSimpleName().substring(0, 3).toLowerCase() + "vm-" + getName() + (System.currentTimeMillis()%10000);
            String productId = findTestProduct(provider, support, img.getArchitecture(), true);
            VMLaunchOptions options = VMLaunchOptions.getInstance(productId, img.getProviderMachineImageId(), name, getName() + " test case execution");

            options.inDataCenter(getTestDataCenterId());
            if( support.identifyPasswordRequirement(img.getPlatform()).equals(Requirement.REQUIRED) ) {
                options.withBootstrapUser("dasein", "x" + System.currentTimeMillis());
            }
            if( support.identifyStaticIPRequirement().equals(Requirement.REQUIRED) ) {
                NetworkServices services = provider.getNetworkServices();

                if( services == null ) {
                    throw new CloudException("A static IP is required to launch a virtual machine, but no network services exist.");
                }
                IpAddressSupport ips = services.getIpAddressSupport();

                if( support == null ) {
                    throw new CloudException("A static IP is required to launch a virtual machine, but no IP address support exists.");
                }
                for( IPVersion version : ips.listSupportedIPVersions() ) {
                    try {
                        options.withStaticIps(identifyTestIPAddress(provider, version));
                    }
                    catch( CloudException ignore ) {
                        // try again, maybe
                    }
                }
                if( options.getStaticIpIds().length < 1 ) {
                    throw new CloudException("Unable to provisionVM the required IP address for this test");
                }
            }
            if( support.identifyRootVolumeRequirement().equals(Requirement.REQUIRED) ) {
                String vp = null;

                for( VolumeProduct p : provider.getComputeServices().getVolumeSupport().listVolumeProducts() ) {
                    vp = p.getProviderProductId();
                }
                assertNotNull("Cannot identify a volume product for the root volume.", productId);
                options.withRootVolumeProduct(vp);
            }
            if( support.identifyShellKeyRequirement(img.getPlatform()).equals(Requirement.REQUIRED) ) {
                ShellKeySupport sks = provider.getIdentityServices().getShellKeySupport();
                String keyId = null;

                if( sks.getKeyImportSupport().equals(Requirement.REQUIRED) ) {
                    fail("Import not yet supported in test cases.");
                }
                else {
                    keyId = sks.createKeypair(name).getProviderKeypairId();
                }
                //noinspection ConstantConditions
                options.withBoostrapKey(keyId);
            }
            if( support.identifyVlanRequirement().equals(Requirement.REQUIRED) ) {
                VLANSupport vs = provider.getNetworkServices().getVlanSupport();

                assertNotNull("No VLAN support but a vlan is required.", vs);
                String vlanId = null;

                VLAN testVlan = null;

                for( VLAN vlan : vs.listVlans() ) {
                    if( vlan.getCurrentState().equals(VLANState.AVAILABLE) && (!vs.isVlanDataCenterConstrained() || vlan.getProviderDataCenterId().equals(getTestDataCenterId())) ) {
                        testVlan = vlan;
                    }
                }
                assertNotNull("Test VLAN could not be found.", testVlan);
                if( vs.getSubnetSupport().equals(Requirement.NONE) ) {
                    vlanId = testVlan.getProviderVlanId();
                }
                else {
                    for( Subnet subnet : vs.listSubnets(testVlan.getProviderVlanId()) ) {
                        if( subnet.getCurrentState().equals(SubnetState.AVAILABLE) && (!vs.isSubnetDataCenterConstrained() || subnet.getProviderDataCenterId().equals(getTestDataCenterId())) ) {
                            vlanId = subnet.getProviderSubnetId();
                        }
                    }
                }
                assertNotNull("No test VLAN/subnet was identified.", vlanId);
                options.inVlan(null, getTestDataCenterId(), testVlan.getProviderVlanId());
            }
            if( provider.hasNetworkServices() && provider.getNetworkServices().hasFirewallSupport() ) {
                String id = getTestFirewallId();

                if( id != null ) {
                    options.behindFirewalls(id);
                }
            }
            vm = support.launch(options);
            vmToKill = vm.getProviderVirtualMachineId();
            waitForState(support, vm.getProviderVirtualMachineId(), VmState.RUNNING, getLaunchWindow());
            vm = support.getVirtualMachine(vm.getProviderVirtualMachineId());
        }
        if( vm == null ) {
            Assert.fail("No test virtual machine could be found or created to support this test case");
        }
        return vm;
    }

    protected Volume findTestVolume(@Nonnull CloudProvider provider, @Nonnull VolumeSupport support, boolean findFree, boolean createNew) throws CloudException, InternalException {
        Volume volume = null;

        if( createNew ) {
            actualVolumeReuses++;
        }
        if( volumeToKill != null ) {
            volume = support.getVolume(volumeToKill);
            if( volumeToKill != null && VolumeState.DELETED.equals(volume.getCurrentState()) ) {
                volume = null;
            }
        }
        if( volume == null && findFree ) {
            Iterable<Volume> volumes = support.listVolumes();

            for( Volume v : volumes ) {
                if( VolumeState.AVAILABLE.equals(v.getCurrentState()) ) {
                    volume = v;
                    break;
                }
                if( !VolumeState.DELETED.equals(v.getCurrentState()) ) {
                    volume = v;
                }
            }
        }
        if( volume == null && createNew ) {
            VolumeCreateOptions options;
            boolean network = true;

            for( VolumeFormat fmt : support.listSupportedFormats() ) {
                if( fmt.equals(VolumeFormat.BLOCK) ) {
                    network = false;
                    break;
                }
            }
            if( network ) {
                throw new CloudException("Test volumes not supported");
            }
            else {
                String name = "dsnvol-" + getName() + (System.currentTimeMillis() % 10000);

                if( support.getVolumeProductRequirement().equals(Requirement.REQUIRED) ) {
                    VolumeProduct product = null;

                    for( VolumeProduct prd : support.listVolumeProducts() ) {
                        if( product == null ) {
                            product = prd;
                        }
                        else {
                            Float thisCost = prd.getMonthlyGigabyteCost();
                            Float currentCost = product.getMonthlyGigabyteCost();

                            if( currentCost == null || currentCost < 0.001f ) {
                                Storage<Gigabyte> thisSize = prd.getVolumeSize();
                                Storage<Gigabyte> currentSize = product.getVolumeSize();

                                if( currentSize == null || (thisSize != null && thisSize.intValue() < currentSize.intValue()) ) {
                                    product = prd;
                                }
                            }
                            else if( thisCost != null && thisCost > 0.0f && thisCost < currentCost ) {
                                product = prd;
                            }
                        }
                    }
                    if( product == null ) {
                        options = VolumeCreateOptions.getInstance(support.getMinimumVolumeSize(), name, name);
                    }
                    else {
                        Storage<Gigabyte> size = null;

                        if( support.isVolumeSizeDeterminedByProduct() ) {
                            size = product.getVolumeSize();
                        }
                        if( size == null || size.intValue() < 1 ) {
                            size = support.getMinimumVolumeSize();
                        }
                        options = VolumeCreateOptions.getInstance(product.getProviderProductId(), size, name, name, 0);
                    }
                }
                else {
                    options = VolumeCreateOptions.getInstance(support.getMinimumVolumeSize(), name, name);
                }
            }
            volume = support.getVolume(support.createVolume(options));
            if( volume != null ) {
                volumeToKill = volume.getProviderVolumeId();
            }
        }
        if( volume == null ) {
            Assert.fail("No test volume could be found or created to support this test case");
        }
        return volume;
    }

    public int getImageReuseCount() {
        return 0;
    }

    protected long getLaunchWindow() {
        return launchWindow;
    }

    protected Properties getProperties() {
        return System.getProperties();
    }

    protected long getStateChangeWindow() {
        return stateChangeWindow;
    }

    protected ProviderContext getTestContext() {
        return getTestContext(ComprehensiveTestSuite.providerClass);
    }
    
    protected String getTestDataCenterId() {
        return getProperties().getProperty("test.dataCenter");
    }
    
    protected String getTestFirewallId() {
        return getProperties().getProperty("test.firewall");
    }
    
    protected String getTestMachineImageId() {
        return getProperties().getProperty("test.machineImage");
    }
    
    protected String getTestProduct() {
        return getProperties().getProperty("test.product");
    }

    protected String getTestHostname() {
        return "dsn" + (System.currentTimeMillis()%10000);
    }

    protected String getTestShareAccount() {
        return System.getProperty("test.shareAccount");
    }

    protected CloudProvider getProvider() throws InstantiationException, IllegalAccessException {
        return ComprehensiveTestSuite.providerClass.newInstance();
    }

    public int getFirewallReuseCount() {
        return 0;
    }

    public int getVlanReuseCount() {
        return 0;
    }

    public int getVolumeReuseCount() {
        return 0;
    }

    public int getVmReuseCount() {
        return 0;
    }

    protected void killTestAddress(@Nonnull CloudProvider provider) {
        try {
            if( ipToRelease != null ) {
                //noinspection ConstantConditions
                provider.getNetworkServices().getIpAddressSupport().releaseFromPool(ipToRelease);
                ipToRelease = null;
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
    }

    protected void killTestFirewall(@Nonnull CloudProvider provider) {
        if( firewallToDelete == null || actualFirewallReuses < expectedFirewallReuses ) {
            return;
        }
        try {
            @SuppressWarnings("ConstantConditions") FirewallSupport support = provider.getNetworkServices().getFirewallSupport();

            cleanFirewall(support, firewallToDelete);
            firewallToDelete = null;
        }
        catch( Throwable t ) {
            out("WARNING: Failed to delete temporary firewall " + firewallToDelete + " used during tests: " + t.getMessage());
        }
    }

    protected void killTestImage(CloudProvider provider) {
        if( imageToDelete != null && actualImageReuses >= expectedImageReuses ) {
            try {
                cleanImage(provider.getComputeServices().getImageSupport(), imageToDelete);
                imageToDelete = null;
            }
            catch( Throwable t ) {
                out("WARNING: Failed to kill test image " + imageToDelete + " during clean up: " + t.getMessage());
            }
        }
    }

    protected void killTestVlan(CloudProvider provider) {
        if( vlanToKill != null && actualVlanReuses >= expectedVlanReuses ) {
            try {
                @SuppressWarnings("ConstantConditions") VLANSupport support = provider.getNetworkServices().getVlanSupport();

                cleanVlan(support, vlanToKill);
                vlanToKill = null;
            }
            catch( Throwable t ) {
                out("WARNING: Failed to clean up after test, the VLAN " + vlanToKill + " was not cleanly removed");
            }
        }
    }
    protected void killTestVm(@Nonnull CloudProvider provider) {
        if( vmToKill != null && actualVmReuses >= expectedVmReuses ) {
            try {
                long timeout = System.currentTimeMillis() + getLaunchWindow();
                VirtualMachine vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(vmToKill);
                boolean stopped = false;

                if( vm == null ) {
                    return;
                }
                if( !VmState.STOPPED.equals(vm.getCurrentState()) && provider.getComputeServices().getVirtualMachineSupport().supportsStartStop(vm) ) {
                    try {
                        provider.getComputeServices().getVirtualMachineSupport().stop(vmToKill);
                        stopped = true;
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
                while( timeout > System.currentTimeMillis() ) {
                    try {
                        vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(vmToKill);
                        if( vm == null || VmState.TERMINATED.equals(vm.getCurrentState()) ) {
                            return;
                        }
                        if( VmState.STOPPED.equals(vm.getCurrentState()) ) {
                            break;
                        }
                        if( !stopped && !VmState.STOPPING.equals(vm.getCurrentState()) && !VmState.PENDING.equals(vm.getCurrentState()) ) {
                            break;
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                    try { Thread.sleep(15000L); }
                    catch( InterruptedException ignore ) { }
                }
                if( vm != null ) {
                    //noinspection ConstantConditions
                    provider.getComputeServices().getVirtualMachineSupport().terminate(vmToKill);
                }
            }
            catch( Throwable t ) {
                out("WARNING: Failed to clean up after test, the VM " + vmToKill + " was not cleanly removed");
            }
        }
    }

    protected void killTestVolume(@Nonnull CloudProvider provider) {
        if( volumeToKill != null && actualVolumeReuses >= expectedVolumeReuses ) {
            try {
                ComputeServices services = provider.getComputeServices();

                if( services == null ) {
                    return;
                }
                VolumeSupport support = provider.getComputeServices().getVolumeSupport();

                if( support == null ) {
                    return;
                }
                cleanVolume(support, volumeToKill);
                volumeToKill = null;
            }
            catch( Throwable t ) {
                out("WARNING: Failed to clean up after test, the volume " + volumeToKill + " was not cleanly removed");
            }
        }
    }

    protected String launch(CloudProvider provider) throws InternalException, CloudException {
        return launch(provider, false);
    }
    
    protected String launch(CloudProvider provider, boolean forImaging) throws InternalException, CloudException {
        return launch(provider, "dsn" + getName(), forImaging);
    }

    protected String launch(@Nonnull CloudProvider provider, @Nonnull String namePrefix, boolean forImaging) throws InternalException, CloudException {
        VirtualMachineSupport support = provider.getComputeServices().getVirtualMachineSupport();

        if( support != null ) {
            String hostName = namePrefix + (System.currentTimeMillis()%10000);

            VMLaunchOptions testLaunchOptions = VMLaunchOptions.getInstance(getTestProduct(), getTestMachineImageId(), hostName, hostName, "DSN Test Host - " + getName());

            testLaunchOptions.inDataCenter(getTestDataCenterId());
            if( support.identifyPasswordRequirement().equals(Requirement.REQUIRED) ) {
                testLaunchOptions.withBootstrapUser("dasein", "x" + System.currentTimeMillis());
            }
            if( support.identifyStaticIPRequirement().equals(Requirement.REQUIRED) ) {
                NetworkServices services = provider.getNetworkServices();

                if( services == null ) {
                    throw new CloudException("A static IP is required to launch a virtual machine, but no network services exist.");
                }
                IpAddressSupport ips = services.getIpAddressSupport();

                if( support == null ) {
                    throw new CloudException("A static IP is required to launch a virtual machine, but no IP address support exists.");
                }
                for( IPVersion version : ips.listSupportedIPVersions() ) {
                    try {
                        testLaunchOptions.withStaticIps(identifyTestIPAddress(provider, version));
                    }
                    catch( CloudException ignore ) {
                        // try again, maybe
                    }
                }
                if( testLaunchOptions.getStaticIpIds().length < 1 ) {
                    throw new CloudException("Unable to provisionVM the required IP address for this test");
                }
            }
            if( support.identifyRootVolumeRequirement().equals(Requirement.REQUIRED) ) {
                String productId = null;

                for( VolumeProduct p : provider.getComputeServices().getVolumeSupport().listVolumeProducts() ) {
                    productId = p.getProviderProductId();
                }
                assertNotNull("Cannot identify a volume product for the root volume.", productId);
                testLaunchOptions.withRootVolumeProduct(productId);
            }
            if( support.identifyShellKeyRequirement().equals(Requirement.REQUIRED) ) {
                ShellKeySupport sks = provider.getIdentityServices().getShellKeySupport();
                String keyId = null;

                if( sks.getKeyImportSupport().equals(Requirement.REQUIRED) ) {
                    fail("Import not yet supported in test cases.");
                }
                else {
                    keyId = sks.createKeypair(hostName).getProviderKeypairId();
                }
                //noinspection ConstantConditions
                testLaunchOptions.withBoostrapKey(keyId);
            }
            if( support.identifyVlanRequirement().equals(Requirement.REQUIRED) ) {
                VLANSupport vs = provider.getNetworkServices().getVlanSupport();

                assertNotNull("No VLAN support but a vlan is required.", vs);
                String vlanId = null;

                VLAN testVlan = null;

                for( VLAN vlan : vs.listVlans() ) {
                    if( vlan.getCurrentState().equals(VLANState.AVAILABLE) && (!vs.isVlanDataCenterConstrained() || vlan.getProviderDataCenterId().equals(getTestDataCenterId())) ) {
                        testVlan = vlan;
                    }
                }
                assertNotNull("Test VLAN could not be found.", testVlan);
                if( vs.getSubnetSupport().equals(Requirement.NONE) ) {
                    vlanId = testVlan.getProviderVlanId();
                }
                else {
                    for( Subnet subnet : vs.listSubnets(testVlan.getProviderVlanId()) ) {
                        if( subnet.getCurrentState().equals(SubnetState.AVAILABLE) && (!vs.isSubnetDataCenterConstrained() || subnet.getProviderDataCenterId().equals(getTestDataCenterId())) ) {
                            vlanId = subnet.getProviderSubnetId();
                        }
                    }
                }
                assertNotNull("No test VLAN/subnet was identified.", vlanId);
                testLaunchOptions.inVlan(null, getTestDataCenterId(), testVlan.getProviderVlanId());
            }
            if( provider.hasNetworkServices() && provider.getNetworkServices().hasFirewallSupport() ) {
                String id = getTestFirewallId();

                if( id != null ) {
                    testLaunchOptions.behindFirewalls(id);
                }
            }
            return support.launch(testLaunchOptions).getProviderVirtualMachineId();
        }
        return null;
    }

    protected boolean pause(VirtualMachineSupport support, String vmId) throws InternalException, CloudException {
        long timeout = System.currentTimeMillis() + stateChangeWindow;
        VirtualMachine vm = support.getVirtualMachine(vmId);

        // make sure it is running before pausing
        assertNotNull("Target virtual machine does not exist and cannot be paused: " + vmId, vm);
        if( vm.getCurrentState().equals(VmState.STOPPED) ) {
            support.start(vmId);
        }
        assertTrue("Timed out waiting for VM to be running.", waitForState(support, vmId, VmState.RUNNING, timeout));
        vm = support.getVirtualMachine(vmId);
        assertNotNull("Target virtual machine has ceased to exist: " + vmId, vm);
        if( support.supportsPauseUnpause(vm) ) {
            support.pause(vmId);
            assertTrue("Timed out waiting for VM to be paused.", waitForState(support, vmId, VmState.PAUSED, timeout));
            return true;
        }
        else {
            return false;
        }
    }

    protected boolean resume(VirtualMachineSupport support, String vmId) throws InternalException, CloudException {
        long timeout = System.currentTimeMillis() + stateChangeWindow;
        VirtualMachine vm = support.getVirtualMachine(vmId);

        assertNotNull("Target virtual machine does not exist and cannot be resumed: " + vmId, vm);
        if( !vm.getCurrentState().equals(VmState.SUSPENDED) ) {
            // make sure it is suspended before resuming
            if( vm.getCurrentState().equals(VmState.STOPPED) ) {
                support.start(vmId);
            }
            assertTrue("Timed out waiting for VM to be suspendable.", waitForState(support, vmId, VmState.RUNNING, timeout));
            vm = support.getVirtualMachine(vmId);
            assertNotNull("Target virtual machine has ceased to exist: " + vmId, vm);
            if( support.supportsSuspendResume(vm) ) {
                support.suspend(vmId);
                assertTrue("Timed out waiting for VM to be resumable.", waitForState(support, vmId, VmState.SUSPENDED, timeout));
            }
        }
        if( support.supportsSuspendResume(vm) ) {
            support.resume(vmId);
            assertTrue("Timed out waiting for VM to be running.", waitForState(support, vmId, VmState.RUNNING, timeout));
            return true;
        }
        else {
            return false;
        }
    }

    protected boolean start(VirtualMachineSupport support, String vmId) throws InternalException, CloudException {
        long timeout = System.currentTimeMillis() + launchWindow;
        VirtualMachine vm = support.getVirtualMachine(vmId);

        assertNotNull("Target virtual machine does not exist and cannot be started: " + vmId, vm);
        if( !vm.getCurrentState().equals(VmState.STOPPED) ) {
            assertTrue("Timed out waiting for VM to be stoppable.", waitForState(support, vmId, VmState.RUNNING, timeout));
            vm = support.getVirtualMachine(vmId);
            assertNotNull("Target virtual machine has ceased to exist: " + vmId, vm);
            if( support.supportsStartStop(vm) ) {
                support.stop(vmId);
                assertTrue("Timed out waiting for VM to be startable.", waitForState(support, vmId, VmState.STOPPED, timeout));
            }
        }
        if( support.supportsStartStop(vm) ) {
            support.start(vmId);
            assertTrue("Timed out waiting for VM to be running.", waitForState(support, vmId, VmState.RUNNING, timeout));
            return true;
        }
        else {
            return false;
        }
    }

    protected boolean stop(VirtualMachineSupport support, String vmId) throws InternalException, CloudException {
        long timeout = System.currentTimeMillis() + stateChangeWindow;
        VirtualMachine vm = support.getVirtualMachine(vmId);

        // make sure it is running before stopping
        assertNotNull("Target virtual machine does not exist and cannot be stopped: " + vmId, vm);
        if( vm.getCurrentState().equals(VmState.STOPPED) ) {
            support.start(vmId);
        }
        assertTrue("Timed out waiting for VM to be running.", waitForState(support, vmId, VmState.RUNNING, timeout));
        vm = support.getVirtualMachine(vmId);
        assertNotNull("Target virtual machine has ceased to exist: " + vmId, vm);
        if( support.supportsStartStop(vm) ) {
            support.stop(vmId);
            assertTrue("Timed out waiting for VM to be stopped.", waitForState(support, vmId, VmState.STOPPED, timeout));
            return true;
        }
        else {
            return false;
        }
    }

    protected boolean suspend(VirtualMachineSupport support, String vmId) throws InternalException, CloudException {
        long timeout = System.currentTimeMillis() + stateChangeWindow;
        VirtualMachine vm = support.getVirtualMachine(vmId);

        // make sure it is running before pausing
        assertNotNull("Target virtual machine does not exist and cannot be suspended: " + vmId, vm);
        if( vm.getCurrentState().equals(VmState.STOPPED) ) {
            support.start(vmId);
        }
        assertTrue("Timed out waiting for VM to be running.", waitForState(support, vmId, VmState.RUNNING, timeout));
        vm = support.getVirtualMachine(vmId);
        assertNotNull("Target virtual machine has ceased to exist: " + vmId, vm);
        if( support.supportsSuspendResume(vm) ) {
            support.suspend(vmId);
            assertTrue("Timed out waiting for VM to be suspended.", waitForState(support, vmId, VmState.SUSPENDED, timeout));
            return true;
        }
        else {
            return false;
        }
    }

    protected boolean waitForState(VirtualMachineSupport support, String vmId, VmState state, long timeout) throws InternalException, CloudException {
        VirtualMachine vm = support.getVirtualMachine(vmId);

        while( timeout > System.currentTimeMillis() ) {
            vm = support.getVirtualMachine(vmId);
            if( vm == null && state.equals(VmState.TERMINATED) ) {
                return true;
            }
            assertNotNull("Target virtual machine has ceased to exist: " + vmId, vm);
            if( vm.getCurrentState().equals(state) ) {
                return true;
            }
            try { Thread.sleep(5000L); }
            catch( InterruptedException ignore ) { }
        }
        return false;
    }

    protected boolean unpause(VirtualMachineSupport support, String vmId) throws InternalException, CloudException {
        long timeout = System.currentTimeMillis() + stateChangeWindow;
        VirtualMachine vm = support.getVirtualMachine(vmId);

        assertNotNull("Target virtual machine does not exist and cannot be unpaused: " + vmId, vm);
        if( !vm.getCurrentState().equals(VmState.PAUSED) ) {
            // make sure it is paused before unpausing
            if( vm.getCurrentState().equals(VmState.STOPPED) ) {
                support.start(vmId);
            }
            assertTrue("Timed out waiting for VM to be pausable.", waitForState(support, vmId, VmState.RUNNING, timeout));
            vm = support.getVirtualMachine(vmId);
            assertNotNull("Target virtual machine has ceased to exist: " + vmId, vm);
            if( support.supportsPauseUnpause(vm) ) {
                support.pause(vmId);
                assertTrue("Timed out waiting for VM to be unpausable.", waitForState(support, vmId, VmState.PAUSED, timeout));
            }
        }
        if( support.supportsPauseUnpause(vm) ) {
            support.unpause(vmId);
            assertTrue("Timed out waiting for VM to be running.", waitForState(support, vmId, VmState.RUNNING, timeout));
            return true;
        }
        else {
            return false;
        }
    }

    protected String lbVmToKill         = null;

    protected @Nonnull String identifyTestIPAddress(@Nonnull CloudProvider provider, @Nonnull IPVersion version) throws CloudException, InternalException {
        NetworkServices services = provider.getNetworkServices();

        if( services == null ) {
            throw new CloudException("IP addresses are not supported in " + provider.getCloudName());
        }
        IpAddressSupport support = services.getIpAddressSupport();

        if( support == null ) {
            throw new CloudException("IP addresses are not supported in " + provider.getCloudName());
        }
        Iterator<IpAddress> it = support.listIpPool(version, true).iterator();
        String address = null;

        if( it.hasNext() ) {
            address = it.next().getProviderIpAddressId();
        }
        else {
            if( support.isRequestable(version) ) {
                ipToRelease = support.request(version);
                address = ipToRelease;
            }
        }
        if( address == null ) {
            throw new CloudException("No addresses available for this test");
        }
        return address;
    }

    protected String makeTestLoadBalancer(CloudProvider provider) throws CloudException, InternalException {
        LoadBalancerSupport support = provider.getNetworkServices().getLoadBalancerSupport();
        String[] dcIds = new String[0];
        String address = null;
        
        if( !support.isAddressAssignedByProvider() ) {
            for( IPVersion version : support.listSupportedIPVersions() ) {
                try {
                    address = identifyTestIPAddress(provider, version);
                }
                catch( CloudException ignore ) {
                    // try again, maybe?
                }
            }
            if( address == null ) {
                throw new CloudException("Unable to provisionVM an IP address to test load balancers");
            }
        }
        if( support.isDataCenterLimited() ) {
            dcIds = new String[] { getTestDataCenterId() };
        }
        
        LbListener listener = new LbListener();
        
        for( LbAlgorithm algorithm : support.listSupportedAlgorithms() ) {
            listener.setAlgorithm(algorithm);
            break;
        }
        for( LbProtocol protocol : support.listSupportedProtocols() ) {
            listener.setNetworkProtocol(protocol);
            break;
        }
        listener.setPrivatePort(2000);
        listener.setPublicPort(2000);

        String[] serverIds = new String[0];
        
        if( support.requiresServerOnCreate() ) {
            lbVmToKill = launch(provider);
            serverIds = new String[] { lbVmToKill };
            while( true ) {
                try { Thread.sleep(10000L); }
                catch( InterruptedException e ) { }
                VirtualMachine vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(lbVmToKill);
                
                if( !vm.getCurrentState().equals(VmState.PENDING) ) {
                    break;
                }
            }
        }
        return support.create("dsn" + System.currentTimeMillis(), "Test LB", address, dcIds, new LbListener[] { listener }, serverIds);
    }
    
    protected void out(String msg) {
        if( verbose ) {
            String prefix = " --> " + getName() + "> ";
        
            logger.info(prefix + " " + msg);
        }
    }
    
    protected void out(AsynchronousTask<?> task) throws Exception {
        if( verbose ) {
            String prefix = " --> " + getName() + "> Begin";
            
            logger.info(prefix);
            while( !task.isComplete() ) {
                try { Thread.sleep(15000L); }
                catch( InterruptedException e ) { }
                logger.info(" --> " + getName() + "> ...");
            }
            logger.info(" --> " + getName() + "> Done");
        }
    }
}
