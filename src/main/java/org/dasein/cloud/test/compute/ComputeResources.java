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
import org.dasein.cloud.compute.MachineImageType;
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
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.identity.IdentityResources;
import org.dasein.cloud.test.network.NetworkResources;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;

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

    private CloudProvider   provider;
    private TreeSet<String> provisionedVMs     = new TreeSet<String>();
    private TreeSet<String> provisionedVolumes = new TreeSet<String>();

    private String        testImageId;
    private Platform      testImagePlatform;
    private String        testVMIdCustom;
    private String        testVMIdShared;
    private String        testVMProductId;
    private String        testVolumeIdCustom;
    private String        testVolumeIdShared;
    private String        testVolumeProductId;

    public ComputeResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    public void close() {
        ComputeServices computeServices = provider.getComputeServices();

        if( computeServices != null ) {
            VirtualMachineSupport vmSupport = computeServices.getVirtualMachineSupport();

            if( vmSupport != null ) {
                for( String id : provisionedVMs ) {
                    try {
                        vmSupport.terminate(id);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }

            VolumeSupport volumeSupport = computeServices.getVolumeSupport();

            if( volumeSupport != null ) {
                for( String id : provisionedVolumes ) {
                    try {
                        volumeSupport.detach(id, true);
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
                try { Thread.sleep(60000L); }
                catch( InterruptedException ignore ) { }
                for( String id : provisionedVolumes ) {
                    try {
                        volumeSupport.remove(id);
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

    public @Nullable String getTestVolumeId(boolean shared) {
        if( testVolumeIdCustom != null ) {
            return testVMIdCustom;
        }
        if( shared ) {
            return testVolumeIdShared;
        }
        return testVolumeIdCustom;
    }

    public @Nullable String getTestVolumeProductId() {
        return testVolumeProductId;
    }

    public @Nullable String init(boolean stateful) {
        ComputeServices computeServices = provider.getComputeServices();
        String dataCenterId = null;

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
                boolean volumeBased = false;

                try {
                    for( MachineImageType type : imageSupport.listSupportedImageTypes() ) {
                        if( type.equals(MachineImageType.VOLUME) ) {
                            volumeBased = true;
                            break;
                        }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
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
                                        if( !volumeBased || image.getType().equals(MachineImageType.VOLUME) ) {
                                            break;
                                        }
                                    }
                                }
                            }
                            catch( Throwable ignore ) {
                                // ignore
                            }
                            if( testVMProductId != null ) {
                                break;
                            }
                            try {
                                for( MachineImage image : imageSupport.searchPublicImages(options) ) {
                                    if( MachineImageState.ACTIVE.equals(image.getCurrentState()) && "".equals(image.getSoftware()) ) {
                                        testVMProductId = currentProduct.getProviderProductId();
                                        testImageId = image.getProviderMachineImageId();
                                        testImagePlatform = image.getPlatform();
                                        if( !volumeBased || image.getType().equals(MachineImageType.VOLUME) ) {
                                            break;
                                        }
                                    }
                                }
                            }
                            catch( Throwable ignore ) {
                                // ignore
                            }
                        }
                        if( testVMProductId != null ) {
                            break;
                        }
                    }
                }
            }

            VolumeSupport volumeSupport = computeServices.getVolumeSupport();

            if( volumeSupport != null ) {
                try {
                    VolumeProduct defaultProduct = null;

                    for( VolumeProduct product : volumeSupport.listVolumeProducts() ) {
                        if( defaultProduct == null ) {
                            defaultProduct = product;
                        }
                        else {
                            if( volumeSupport.isVolumeSizeDeterminedByProduct() ) {
                                if( product.getVolumeSize().intValue() < defaultProduct.getVolumeSize().intValue() && product.getVolumeSize().intValue() >= 20 ) {
                                    defaultProduct = product;
                                }
                            }
                            else {
                                if( product.getMonthlyGigabyteCost() > 0.00 ) {
                                    if( product.getMonthlyGigabyteCost() < defaultProduct.getMonthlyGigabyteCost() ) {
                                        defaultProduct = product;
                                    }
                                }
                            }
                        }
                    }
                    if( defaultProduct != null ) {
                        testVolumeProductId = defaultProduct.getProviderProductId();
                    }
                }
                catch( Throwable ignore ) {
                    // ignore me
                }
            }
            if( vmSupport != null ) {
                try {
                    for( VirtualMachine vm : vmSupport.listVirtualMachines() ) {
                        if( VmState.RUNNING.equals(vm.getCurrentState()) ) {
                            testVMIdShared = vm.getProviderVirtualMachineId();
                            if( !stateful ) {
                                dataCenterId = vm.getProviderDataCenterId();
                            }
                            break;
                        }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                if( stateful ) {
                    try {
                        testVMIdCustom = provisionVM(vmSupport, "Dasein Test VM", "dsnint", null);
                        if( dataCenterId == null && testVMIdCustom != null ) {
                            VirtualMachine vm = vmSupport.getVirtualMachine(testVMIdCustom);

                            if( vm != null ) {
                                dataCenterId = vm.getProviderDataCenterId();
                            }
                        }
                    }
                    catch( Throwable t ) {
                        logger.warn("Error provisioning test virtual machine: " + t.getMessage());
                    }
                }
            }
            if( volumeSupport != null ) {
                try {
                    Volume defaultVolume = null;

                    for( Volume volume : volumeSupport.listVolumes() ) {
                        if( VolumeState.AVAILABLE.equals(volume.getCurrentState()) || defaultVolume == null ) {
                            if( defaultVolume == null || volume.isAttached() ) {
                                defaultVolume = volume;
                            }
                            if( VolumeState.AVAILABLE.equals(defaultVolume.getCurrentState()) && defaultVolume.isAttached() ) {
                                break;
                            }
                        }
                    }
                    if( defaultVolume != null ) {
                        testVolumeIdShared = defaultVolume.getProviderVolumeId();
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                if( stateful ) {
                    try {
                        testVolumeIdCustom = provisionVolume(volumeSupport, "dsnvol", null, dataCenterId);
                        if( dataCenterId == null && testVolumeIdCustom != null ) {
                            Volume v = volumeSupport.getVolume(testVolumeIdCustom);

                            if( v != null ) {
                                dataCenterId = v.getProviderDataCenterId();
                            }
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return dataCenterId;
    }

    /**
     * Provisions a virtual machine and returns the ID of the new virtual machine. This method tracks the newly provisioned
     * virtual machine and will tear it down at the end of the test suite.
     * @param support the virtual machine support object used to provision the VM
     * @param namePrefix a prefix for the friendly name of the VM
     * @param hostPrefix a prefix for the host name of the VM
     * @param preferredDataCenter the data center, if any is preferred, in which the VM should be provisioned
     * @return the ID for the new VM
     * @throws CloudException an error occurred with the cloud provider in provisioning the VM
     * @throws InternalException an error occurred within Dasein Cloud provisioning the VM
     */
    public @Nonnull String provisionVM(@Nonnull VirtualMachineSupport support, @Nonnull String namePrefix, @Nonnull String hostPrefix, @Nullable String preferredDataCenter) throws CloudException, InternalException {
        long now = System.currentTimeMillis();
        String name = namePrefix + " " + now;
        String host = hostPrefix + (now%10000);

        VMLaunchOptions options = VMLaunchOptions.getInstance(testVMProductId, testImageId, name, host, "Test VM for stateful integration tests for Dasein Cloud");

        if( preferredDataCenter != null ) {
            options.inDataCenter(preferredDataCenter);
        }
        if( Requirement.REQUIRED.equals(support.identifyPasswordRequirement(testImagePlatform)) ) {
            options.withBootstrapUser("dasein", "x" + random.nextInt(100000) + System.currentTimeMillis());
        }
        if( Requirement.REQUIRED.equals(support.identifyShellKeyRequirement(testImagePlatform)) ) {
            IdentityResources identity = DaseinTestManager.getIdentityResources();

            if( identity != null ) {
                String keypairId = identity.getTestKeypairId();

                if( keypairId != null ) {
                    options.withBoostrapKey(keypairId);
                }
            }
        }
        if( Requirement.REQUIRED.equals(support.identifyStaticIPRequirement()) ) {
            NetworkResources network = DaseinTestManager.getNetworkResources();

            if( network != null ) {
                String ipId = network.getTestStaticIpId(false);

                if( ipId != null ) {
                    options.withStaticIps(ipId);
                }
            }
        }
        if( Requirement.REQUIRED.equals(support.identifyRootVolumeRequirement()) && testVolumeProductId != null ) {
            options.withRootVolumeProduct(testVolumeProductId);
        }
        if( Requirement.REQUIRED.equals(support.identifyVlanRequirement()) ) {
            NetworkResources network = DaseinTestManager.getNetworkResources();

            if( network != null ) {
                String networkId = network.getTestVLANId(true); // can be shared for this stuff
                String subnetId = network.getTestSubnetId(true); // can be shared for this stuff

                try {
                    if( networkId != null || subnetId != null ) {
                        if( subnetId != null ) {
                            @SuppressWarnings("ConstantConditions") Subnet subnet = provider.getNetworkServices().getVlanSupport().getSubnet(subnetId);

                            if( subnet != null ) {
                                String dcId = subnet.getProviderDataCenterId();

                                if( dcId == null ) {
                                    for( DataCenter dc : provider.getDataCenterServices().listDataCenters(provider.getContext().getRegionId()) ) {
                                        if( (dc.isActive() && dc.isAvailable()) || dcId == null ) {
                                            dcId = dc.getProviderDataCenterId();
                                        }
                                    }
                                }
                                options.inVlan(null, dcId, subnetId);
                            }
                        }
                        else {
                            @SuppressWarnings("ConstantConditions") VLAN vlan = provider.getNetworkServices().getVlanSupport().getVlan(networkId);

                            if( vlan != null ) {
                                String dcId = vlan.getProviderDataCenterId();

                                if( dcId == null ) {
                                    for( DataCenter dc : provider.getDataCenterServices().listDataCenters(provider.getContext().getRegionId()) ) {
                                        if( (dc.isActive() && dc.isAvailable()) || dcId == null ) {
                                            dcId = dc.getProviderDataCenterId();
                                        }
                                    }
                                }
                                options.inVlan(null, dcId, networkId);
                            }
                        }
                    }
                }
                catch( NullPointerException ignore ) {
                    // ignore the fiasco
                }
            }
        }
        options.withMetaData("dsntestcase", "true");

        String id = support.launch(options).getProviderVirtualMachineId();

        provisionedVMs.add(id);
        return id;
    }

    public @Nonnull String provisionVolume(@Nonnull VolumeSupport support, @Nonnull String namePrefix, @Nullable VolumeFormat desiredFormat, @Nullable String preferredDataCenterId) throws CloudException, InternalException {
        VolumeCreateOptions options;

        if( desiredFormat == null ) {
            for( VolumeFormat fmt : support.listSupportedFormats() ) {
                if( fmt.equals(VolumeFormat.BLOCK) ) {
                    desiredFormat = VolumeFormat.BLOCK;
                    break;
                }
            }
            if( desiredFormat == null ) {
                desiredFormat = VolumeFormat.NFS;
            }
        }
        if( support.getVolumeProductRequirement().equals(Requirement.REQUIRED) && testVolumeProductId != null ) {
            Storage<Gigabyte> size;

            if( support.isVolumeSizeDeterminedByProduct() ) {
                VolumeProduct prd = null;

                for( VolumeProduct product : support.listVolumeProducts() ) {
                    if( product.getProviderProductId().equals(testVolumeProductId) ) {
                        prd = product;
                        break;
                    }
                }
                if( prd != null ) {
                    size = prd.getVolumeSize();
                    if( size == null ) {
                        size = support.getMinimumVolumeSize();
                    }
                }
                else {
                    size = support.getMinimumVolumeSize();
                }
            }
            else {
                size = support.getMinimumVolumeSize();
            }
            if( desiredFormat.equals(VolumeFormat.BLOCK) ) {
                options = VolumeCreateOptions.getInstance(testVolumeProductId, size, namePrefix + (System.currentTimeMillis()%1000), "Dasein Cloud Integration Tests Volume Tests", 0);
            }
            else {
                NetworkResources network = DaseinTestManager.getNetworkResources();
                String testVlanId = null;

                if( network != null ) {
                    testVlanId = network.getTestVLANId(true);
                }
                if( testVlanId != null ) {
                    options = VolumeCreateOptions.getNetworkInstance(testVolumeProductId, testVlanId, size, namePrefix + (System.currentTimeMillis()%10000), "Dasein Cloud Integration Tests Volume Tests", 0);
                }
                else {
                    options = VolumeCreateOptions.getInstance(testVolumeProductId, size, namePrefix + (System.currentTimeMillis()%1000), "Dasein Cloud Integration Tests Volume Tests", 0);
                }
            }
        }
        else {
            if( desiredFormat.equals(VolumeFormat.BLOCK) ) {
                options = VolumeCreateOptions.getInstance(support.getMinimumVolumeSize(), namePrefix + (System.currentTimeMillis()%10000), "Dasein Test Integration tests volume");
            }
            else {
                NetworkResources network = DaseinTestManager.getNetworkResources();
                String testVlanId = null;

                if( network != null ) {
                    testVlanId = network.getTestVLANId(true);
                }
                if( testVlanId != null ) {
                    options = VolumeCreateOptions.getNetworkInstance(testVlanId, support.getMinimumVolumeSize(), namePrefix + (System.currentTimeMillis()%10000), "Dasein Cloud Integration Tests Volume Tests");
                }
                else {
                    options = VolumeCreateOptions.getInstance(support.getMinimumVolumeSize(), namePrefix + (System.currentTimeMillis()%1000), "Dasein Cloud Integration Tests Volume Tests");
                }
            }
        }
        if( preferredDataCenterId == null ) {
            String vmId = getTestVMId(true, null);

            if( vmId != null ) {
                @SuppressWarnings("ConstantConditions") VirtualMachine vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(vmId);

                if( vm != null ) {
                    preferredDataCenterId = vm.getProviderDataCenterId();
                }
            }
            if( preferredDataCenterId == null ) {
                //noinspection ConstantConditions
                for( DataCenter dc : provider.getDataCenterServices().listDataCenters(provider.getContext().getRegionId()) ) {
                    if( (dc.isActive() && dc.isAvailable()) || preferredDataCenterId == null ) {
                        preferredDataCenterId = dc.getProviderDataCenterId();
                    }
                }
            }
        }
        if( preferredDataCenterId != null ) {
            options.inDataCenter(preferredDataCenterId);
        }
        options.withMetaData("dsntestcase", "true");
        String id = support.createVolume(options);
        provisionedVolumes.add(id);
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
