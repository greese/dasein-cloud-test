package org.dasein.cloud.test.compute;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.util.CalendarWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeSet;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/17/13 8:35 PM</p>
 *
 * @author George Reese
 */
public class ComputeResources {
    static private final Logger logger = Logger.getLogger(ComputeResources.class);
    static private final Random random = new Random();

    private DataCenter      defaultDataCenter;
    private CloudProvider   provider;
    private TreeSet<String> provisioned = new TreeSet<String>();

    private String        testImageId;
    private Platform      testImagePlatform;
    private String        testVMIdCustom;
    private String        testVMIdShared;
    private String        testVMProductId;

    public ComputeResources(@Nonnull CloudProvider provider, @Nullable DataCenter defaultDataCenter) {
        this.provider = provider;
        this.defaultDataCenter = defaultDataCenter;
    }

    public void close() {
        for( String id : provisioned ) {
            ComputeServices computeServices = provider.getComputeServices();

            if( computeServices != null ) {
                VirtualMachineSupport vmSupport = computeServices.getVirtualMachineSupport();

                if( vmSupport != null ) {
                    try {
                        vmSupport.terminate(id);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        provider.close();
    }

    public @Nullable String getTestImageId() {
        return testImageId;
    }

    public @Nullable String getTestVMId(boolean shared, @Nullable VmState desiredState) {
        if( testVMIdCustom != null ) {
            ComputeServices computeServices = provider.getComputeServices();

            if( computeServices != null ) {
                VirtualMachineSupport vmSupport = computeServices.getVirtualMachineSupport();

                if( vmSupport != null ) {
                    try {
                        VirtualMachine vm = vmSupport.getVirtualMachine(testVMIdCustom);

                        if( vm != null && (desiredState == null || desiredState.equals(vm.getCurrentState())) ) {
                            return testVMIdCustom;
                        }
                        else if( vm != null && !shared) {
                            if( setState(vmSupport, vm, desiredState) ) {
                                return testVMIdCustom;
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        if( shared ) {
            return testVMIdShared;
        }
        return testVMIdCustom;
    }

    public @Nullable String getTestVMProductId() {
        return testVMProductId;
    }

    public void init(boolean stateful) {
        ComputeServices computeServices = provider.getComputeServices();

        if( computeServices != null ) {
            HashMap<Architecture,VirtualMachineProduct> productMap = new HashMap<Architecture, VirtualMachineProduct>();
            VirtualMachineSupport vmSupport = computeServices.getVirtualMachineSupport();

            if( vmSupport != null ) {
                try {
                    for( Architecture architecture : Architecture.values() ) {
                        VirtualMachineProduct defaultProduct = null;

                        try {
                            for( VirtualMachineProduct product : vmSupport.listProducts(architecture) ) {
                                if( defaultProduct == null ) {
                                    defaultProduct = product;
                                }
                                else if( defaultProduct.getRamSize().intValue() > product.getRamSize().intValue() ) {
                                    if( product.getRamSize().intValue() > 1000 ) {
                                        defaultProduct = product;
                                    }
                                }
                                else {
                                    if( defaultProduct.getRamSize().intValue() < 1024 && product.getRamSize().intValue() < 2200 ) {
                                        defaultProduct = product;
                                    }
                                    else if( defaultProduct.getCpuCount() > product.getCpuCount() ) {
                                        if( (defaultProduct.getRamSize().intValue()*2) > product.getRamSize().intValue() ) {
                                            defaultProduct = product;
                                        }
                                    }
                                }
                            }
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                        productMap.put(architecture, defaultProduct);
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }

            MachineImageSupport imageSupport = computeServices.getImageSupport();

            if( imageSupport != null ) {
                for( Architecture architecture : new Architecture[] { Architecture.I64, Architecture.POWER, Architecture.I32, Architecture.SPARC } ) {
                    VirtualMachineProduct currentProduct = productMap.get(architecture);

                    if( currentProduct != null ) {
                        for( Platform platform : new Platform[] { Platform.UBUNTU, Platform.CENT_OS, Platform.WINDOWS } ) {
                            ImageFilterOptions options = ImageFilterOptions.getInstance(ImageClass.MACHINE).withArchitecture(architecture).onPlatform(platform);

                            try {
                                for( MachineImage image : imageSupport.listImages(options) ) {
                                    if( MachineImageState.ACTIVE.equals(image.getCurrentState()) && "".equals(image.getSoftware()) ) {
                                        testVMProductId = currentProduct.getProviderProductId();
                                        testImageId = image.getProviderMachineImageId();
                                        testImagePlatform = image.getPlatform();
                                        break;
                                    }
                                }
                            }
                            catch( Throwable ignore ) {
                                // ignore
                            }
                            if( testVMProductId != null ) {
                                break;
                            }
                        }
                        if( testVMProductId != null ) {
                            break;
                        }
                    }
                }
            }
            if( vmSupport != null ) {
                if( stateful ) {
                    try {
                        testVMIdCustom = provision(vmSupport, "Dasein Test VM", "dsnint");
                    }
                    catch( Throwable t ) {
                        logger.warn("Error provisioning test virtual machine: " + t.getMessage());
                    }
                }
                try {
                    for( VirtualMachine vm : vmSupport.listVirtualMachines() ) {
                        if( VmState.RUNNING.equals(vm.getCurrentState()) ) {
                            testVMIdShared = vm.getProviderVirtualMachineId();
                            break;
                        }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
    }

    public @Nonnull String provision(@Nonnull VirtualMachineSupport support, @Nonnull String namePrefix, @Nonnull String hostPrefix) throws CloudException, InternalException {
        long now = System.currentTimeMillis();
        String name = namePrefix + " " + now;
        String host = hostPrefix + (now%10000);

        VMLaunchOptions options = VMLaunchOptions.getInstance(testVMProductId, testImageId, name, host, "Test VM for stateful integration tests for Dasein Cloud");

        if( defaultDataCenter != null ) {
            options.inDataCenter(defaultDataCenter.getProviderDataCenterId());
        }
        if( Requirement.REQUIRED.equals(support.identifyPasswordRequirement(testImagePlatform)) ) {
            options.withBootstrapUser("dasein", "x" + random.nextInt(100000) + System.currentTimeMillis());
        }
        if( Requirement.REQUIRED.equals(support.identifyShellKeyRequirement(testImagePlatform)) ) {
            // TODO: set shell key
        }
        if( Requirement.REQUIRED.equals(support.identifyStaticIPRequirement()) ) {
            // TODO: static IP
        }
        if( Requirement.REQUIRED.equals(support.identifyRootVolumeRequirement()) ) {
            // TODO: root volume product
        }
        if( Requirement.REQUIRED.equals(support.identifyVlanRequirement()) ) {
            // TODO: VLAN
        }
        options.withMetaData("dsntestcase", "true");

        String id = support.launch(options).getProviderVirtualMachineId();

        provisioned.add(id);
        return id;
    }

    private boolean setState(@Nonnull VirtualMachineSupport support, @Nonnull VirtualMachine vm, @Nonnull VmState state) {
        VmState currentState = vm.getCurrentState();

        if( state.equals(currentState) ) {
            return true;
        }
        if( state.equals(VmState.TERMINATED) ) {
            return false;
        }
        String id = vm.getProviderVirtualMachineId();

        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*20L);


        while( timeout > System.currentTimeMillis() ) {
            if( !currentState.equals(VmState.PENDING) && !currentState.equals(VmState.PAUSING) && !currentState.equals(VmState.REBOOTING) && !currentState.equals(VmState.STOPPING) && !currentState.equals(VmState.SUSPENDING) ) {
                break;
            }
            try { Thread.sleep(15000L); }
            catch( InterruptedException ignore ) { }
            try {
                VirtualMachine v = support.getVirtualMachine(id);

                if( v == null ) {
                    return state.equals(VmState.TERMINATED);
                }
                vm = v;
                currentState = vm.getCurrentState();
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        try {
            if( state.equals(VmState.RUNNING) ) {
                if( currentState.equals(VmState.PAUSED) ) {
                    support.unpause(id);
                }
                else if( currentState.equals(VmState.STOPPED) ) {
                    support.start(id);
                }
                else if( currentState.equals(VmState.SUSPENDED) ) {
                    support.resume(id);
                }
            }
            else if( state.equals(VmState.PAUSED) ) {
                if( currentState.equals(VmState.RUNNING) || setState(support, vm, VmState.RUNNING) ) {
                    support.pause(id);
                }
                else {
                    return false;
                }
            }
            else if( state.equals(VmState.STOPPED) ) {
                if( currentState.equals(VmState.RUNNING) || setState(support, vm, VmState.RUNNING)) {
                    support.stop(id);
                }
                else {
                    return false;
                }
            }
            else if( state.equals(VmState.SUSPENDED) ) {
                if( currentState.equals(VmState.RUNNING) || setState(support, vm, VmState.RUNNING)) {
                    support.suspend(id);
                }
                else {
                    return false;
                }
            }
        }
        catch( Throwable ignore ) {
            return false;
        }
        timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*20L);


        while( timeout > System.currentTimeMillis() ) {
            if( state.equals(currentState) ) {
                return true;
            }
            try { Thread.sleep(15000L); }
            catch( InterruptedException ignore ) { }
            try {
                VirtualMachine v = support.getVirtualMachine(id);

                if( v == null ) {
                    return state.equals(VmState.TERMINATED);
                }
                vm = v;
                currentState = vm.getCurrentState();
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return false;
    }
}
