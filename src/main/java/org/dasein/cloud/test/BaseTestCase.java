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
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.network.AddressType;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.LbAlgorithm;
import org.dasein.cloud.network.LbListener;
import org.dasein.cloud.network.LbProtocol;
import org.dasein.cloud.network.LoadBalancerSupport;
import org.dasein.util.CalendarWrapper;

public class BaseTestCase extends TestCase {
    
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
    
    public BaseTestCase(String name) { 
        super(name);
        verbose = System.getProperty("test.verbose", "true").equalsIgnoreCase("true");
        logger = Logger.getLogger(getClass());
    }
    
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
    
    protected Properties getProperties() {
        return System.getProperties();
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
    
    protected CloudProvider getProvider() throws InstantiationException, IllegalAccessException {
        return ComprehensiveTestSuite.providerClass.newInstance();
    }
    
    protected String launch(CloudProvider provider) throws InternalException, CloudException {
        return launch(provider, false);
    }
    
    protected String launch(CloudProvider provider, boolean forImaging) throws InternalException, CloudException {
        VirtualMachineSupport support = provider.getComputeServices().getVirtualMachineSupport();

        if( support != null ) {
            String[] firewalls = ((getTestFirewallId() == null) ? new String[0] : new String[] { getTestFirewallId() });
        
            return support.launch(getTestMachineImageId(), support.getProduct(getTestProduct()), getTestDataCenterId(), getTestHostname(), getTestHostname(), null, null, false, forImaging, firewalls).getProviderVirtualMachineId();
        }
        return null;
    }

    protected boolean pause(VirtualMachineSupport support, String vmId) throws InternalException, CloudException {
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 15L);
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
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 15L);
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
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 15L);
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
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 15L);
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
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 15L);
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
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 15L);
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

    protected String lbIpToRelease      = null;
    protected String lbVmToKill         = null;
    
    protected String makeTestLoadBalancer(CloudProvider provider) throws CloudException, InternalException {
        LoadBalancerSupport support = provider.getNetworkServices().getLoadBalancerSupport();
        String[] dcIds = new String[0];
        String address = null;
        
        if( !support.isAddressAssignedByProvider() ) {
            for( IpAddress ip : provider.getNetworkServices().getIpAddressSupport().listPublicIpPool(true) ) {
                address = ip.getProviderIpAddressId();
                break;
            }
            if( address == null ) {
                if( provider.getNetworkServices().getIpAddressSupport().isRequestable(AddressType.PUBLIC) ) {
                    lbIpToRelease = provider.getNetworkServices().getIpAddressSupport().request(AddressType.PUBLIC);
                    address = lbIpToRelease;
                }
                else {
                    throw new CloudException("No addresses available for load balancer");
                }
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
