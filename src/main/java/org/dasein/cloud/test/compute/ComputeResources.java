package org.dasein.cloud.test.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Handles the shared compute resources for executing various tests.
 * <p>Created by George Reese: 2/17/13 8:35 PM</p>
 * @author George Reese
 * @version 2013.04
 * @since 2013.02
 */
public class ComputeResources {
    static private final Random random = new Random();

    private CloudProvider   provider;

    private final HashMap<String,String> testMachineImages = new HashMap<String,String>();
    private final HashMap<String,String> testVMs           = new HashMap<String, String>();
    private final HashMap<String,String> testVolumes       = new HashMap<String, String>();

    private String        testDataCenterId;
    private Platform      testImagePlatform;
    private String        testVMProductId;
    private String        testVolumeProductId;

    public ComputeResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    public void close() {
        ComputeServices computeServices = provider.getComputeServices();

        if( computeServices != null ) {
            VirtualMachineSupport vmSupport = computeServices.getVirtualMachineSupport();

            if( vmSupport != null ) {
                for( Map.Entry<String,String> entry : testVMs.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            vmSupport.terminate(entry.getValue());
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }

            MachineImageSupport imageSupport = computeServices.getImageSupport();

            if( imageSupport != null ) {
                for( Map.Entry<String,String> entry : testMachineImages.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            imageSupport.remove(entry.getValue());
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
            VolumeSupport volumeSupport = computeServices.getVolumeSupport();

            if( volumeSupport != null ) {
                for( Map.Entry<String,String> entry : testVolumes.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            volumeSupport.detach(entry.getValue(), true);
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
                try { Thread.sleep(60000L); }
                catch( InterruptedException ignore ) { }
                for( Map.Entry<String,String> entry : testVolumes.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            volumeSupport.remove(entry.getValue());
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
        }
        provider.close();
    }

    public @Nullable String getTestDataCenterId(boolean stateless) {
        if( testDataCenterId != null ) {
            return testDataCenterId;
        }
        if( stateless ) {
            try {
                DataCenter defaultDC = null;

                //noinspection ConstantConditions
                for( DataCenter dc : provider.getDataCenterServices().listDataCenters(provider.getContext().getRegionId()) ) {
                    if( defaultDC == null ) {
                        defaultDC = dc;
                    }
                    if( dc.isActive() && dc.isAvailable() ) {
                        return dc.getProviderDataCenterId();
                    }
                }
                if( defaultDC != null ) {
                    return defaultDC.getProviderDataCenterId();
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        else {
            ComputeServices services = provider.getComputeServices();

            if( services != null ) {
                VirtualMachineSupport support = services.getVirtualMachineSupport();

                if( support != null ) {
                    try {
                        String id = provisionVM(support, DaseinTestManager.STATEFUL, "Dasein Stateless VM", "dsnstfvm", null);
                        VirtualMachine vm = support.getVirtualMachine(id);

                        if( vm != null ) {
                            testDataCenterId = id;
                            return id;
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore me
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestImageId(@Nonnull String label, boolean provisionIfNull) {
        String id = testMachineImages.get(label);

        if( id == null ) {
            if( label.equals(DaseinTestManager.STATELESS) ) {
                for( Map.Entry<String,String> entry : testMachineImages.entrySet() ) {
                    id = entry.getValue();
                    if( id != null ) {
                        return id;
                    }
                }
                return null;
            }
            if( provisionIfNull ) {
                ComputeServices services = provider.getComputeServices();

                if( services != null ) {
                    MachineImageSupport support = services.getImageSupport();

                    if( support != null ) {
                        try {
                            return provisionImage(support, label, "dsnimg", null);
                        }
                        catch( Throwable ignore ) {
                            return null;
                        }
                    }
                }
            }
        }
        return id;
    }

    public @Nullable String getTestVmId(@Nonnull String label, @Nullable VmState desiredState, boolean provisionIfNull, @Nullable String preferredDataCenterId) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testVMs.entrySet() ) {
                String id = entry.getValue();

                if( id != null ) {
                    return id;
                }
            }
            return null;
        }
        String id = testVMs.get(label);

        if( id == null && !provisionIfNull ) {
            return null;
        }
        ComputeServices services = provider.getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                try {
                    VirtualMachine vm = (id == null ? null : support.getVirtualMachine(id));

                    if( vm == null && provisionIfNull ) {
                        id = provisionVM(support, label, "Dasein Test " + label, "dsnvm", preferredDataCenterId);
                        vm = support.getVirtualMachine(id);
                    }
                    if( vm != null && desiredState != null ) {
                        setState(support, vm, desiredState);
                    }
                    return id;
                }
                catch( Throwable ignore ) {
                    return null;
                }
            }
        }
        return null;
    }

    public @Nullable String getTestVMProductId() {
        return testVMProductId;
    }

    public @Nullable String getTestVolumeId(@Nonnull String label, boolean provisionIfNull, @Nullable VolumeFormat desiredFormat, @Nullable String preferredDataCenterId) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testVolumes.entrySet() ) {
                String id = entry.getValue();

                if( id != null ) {
                    return id;
                }
            }
            return null;
        }
        String id = testVolumes.get(label);

        if( id != null ) {
            return id;
        }
        if( !provisionIfNull ) {
            return null;
        }
        ComputeServices services = provider.getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                try {
                    provisionVolume(support, label, "dsnvol" + (System.currentTimeMillis()%10000), desiredFormat, preferredDataCenterId);
                }
                catch( Throwable ignore ) {
                    return null;
                }
            }
        }
        return null;
    }

    public @Nullable String getTestVolumeProductId() {
        return testVolumeProductId;
    }

    public void init() {
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
                                        testMachineImages.put(DaseinTestManager.STATELESS, image.getProviderMachineImageId());
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
                                        testMachineImages.put(DaseinTestManager.STATELESS, image.getProviderMachineImageId());
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
                            testVMs.put(DaseinTestManager.STATELESS, vm.getProviderVirtualMachineId());
                            break;
                        }
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
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
                        testVolumes.put(DaseinTestManager.STATELESS, defaultVolume.getProviderVolumeId());
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
            }
        }
    }

    public @Nonnull String provisionImage(@Nonnull MachineImageSupport support, @Nonnull String label, @Nonnull String namePrefix, @Nullable String vmId) throws CloudException, InternalException {
        VirtualMachineSupport vmSupport = null;

        ComputeServices services = provider.getComputeServices();

        if( services != null ) {
            vmSupport = services.getVirtualMachineSupport();
        }
        if( vmSupport == null ) {
            throw new CloudException("Unable to provision a machine image because Dasein Cloud is showing no VM support");
        }
        if( vmId == null ) {
            vmId = getTestVmId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, null);
            if( vmId == null ) {
                throw new CloudException("Could not identify a VM for imaging");
            }
        }
        VirtualMachine vm = vmSupport.getVirtualMachine(vmId);

        if( vm == null ) {
            throw new CloudException("Could not identify a VM for imaging");
        }
        String imageId = vm.getProviderMachineImageId();
        MachineImage image = support.getImage(imageId);

        if( image == null || support.supportsImageCapture(image.getType()) ) {
            String id = ImageCreateOptions.getInstance(vm, namePrefix + (System.currentTimeMillis()%10000), "Test machine image with label " + label).build(provider);

            synchronized( testMachineImages ) {
                while( testMachineImages.containsKey(label) ) {
                    label = label + random.nextInt(9);
                }
                testMachineImages.put(label, id);
            }
            return id;
        }
        else if( !support.identifyLocalBundlingRequirement().equals(Requirement.REQUIRED) ) {
            Iterator<MachineImageFormat> formats = support.listSupportedFormatsForBundling().iterator();
            MachineImageFormat format = (formats.hasNext() ? formats.next() : null);

            if( format != null ) {
                String id = support.bundleVirtualMachine(vmId, format, "dsnimg" + (System.currentTimeMillis()%100000), "dsnimg");

                synchronized( testMachineImages ) {
                    while( testMachineImages.containsKey(label) ) {
                        label = label + random.nextInt(9);
                    }
                    testMachineImages.put(label, id);
                }
                return id;
            }
        }
        throw new CloudException("No mechanism exists for provisioning images from a virtual machine");
    }

    /**
     * Provisions a virtual machine and returns the ID of the new virtual machine. This method tracks the newly provisioned
     * virtual machine and will tear it down at the end of the test suite.
     * @param support the virtual machine support object used to provision the VM
     * @param label the label to store the VM under for re-use
     * @param namePrefix a prefix for the friendly name of the VM
     * @param hostPrefix a prefix for the host name of the VM
     * @param preferredDataCenter the data center, if any is preferred, in which the VM should be provisioned
     * @return the ID for the new VM
     * @throws CloudException an error occurred with the cloud provider in provisioning the VM
     * @throws InternalException an error occurred within Dasein Cloud provisioning the VM
     */
    public @Nonnull String provisionVM(@Nonnull VirtualMachineSupport support, @Nonnull String label, @Nonnull String namePrefix, @Nonnull String hostPrefix, @Nullable String preferredDataCenter) throws CloudException, InternalException {
        String testImageId = getTestImageId(DaseinTestManager.STATELESS, false);

        if( testImageId == null ) {
            throw new CloudException("No test image exists for provisioning a virtual machine");
        }
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
                String keypairId = identity.getTestKeypairId(DaseinTestManager.STATEFUL, true);

                if( keypairId != null ) {
                    options.withBoostrapKey(keypairId);
                }
            }
        }
        if( Requirement.REQUIRED.equals(support.identifyStaticIPRequirement()) ) {
            NetworkResources network = DaseinTestManager.getNetworkResources();

            if( network != null ) {
                String ipId = network.getTestStaticIpId(label, true, null);

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
                String networkId = network.getTestVLANId(DaseinTestManager.STATEFUL, true, preferredDataCenter);

                if( networkId == null ) {
                    networkId = network.getTestVLANId(DaseinTestManager.STATELESS, false, preferredDataCenter);
                }
                String subnetId = network.getTestSubnetId(DaseinTestManager.STATEFUL, true, networkId, preferredDataCenter);

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

        String id = options.build(provider);

        synchronized( testVMs ) {
            while( testVMs.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testVMs.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionVolume(@Nonnull VolumeSupport support, @Nonnull String label, @Nonnull String namePrefix, @Nullable VolumeFormat desiredFormat, @Nullable String preferredDataCenterId) throws CloudException, InternalException {
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
                    testVlanId = network.getTestVLANId(DaseinTestManager.STATELESS, false, preferredDataCenterId);
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
                    testVlanId = network.getTestVLANId(DaseinTestManager.STATELESS, false, preferredDataCenterId);
                }
                if( testVlanId != null ) {
                    options = VolumeCreateOptions.getNetworkInstance(testVlanId, support.getMinimumVolumeSize(), namePrefix + (System.currentTimeMillis() % 10000), "Dasein Cloud Integration Tests Volume Tests");
                }
                else {
                    options = VolumeCreateOptions.getInstance(support.getMinimumVolumeSize(), namePrefix + (System.currentTimeMillis()%1000), "Dasein Cloud Integration Tests Volume Tests");
                }
            }
        }
        if( preferredDataCenterId == null ) {
            preferredDataCenterId = getTestDataCenterId(false);
        }
        if( preferredDataCenterId != null ) {
            options.inDataCenter(preferredDataCenterId);
        }
        options.withMetaData("dsntestcase", "true");
        String id = options.build(provider);

        Volume volume = support.getVolume(id);

        if( volume != null && testDataCenterId == null ) {
            testDataCenterId = volume.getProviderDataCenterId();
        }
        synchronized( testVolumes ) {
            while( testVolumes.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testVolumes.put(label, id);
        }
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