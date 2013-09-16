/**
 * Copyright (C) 2009-2013 Dell, Inc.
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

package org.dasein.cloud.test.compute;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.*;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.identity.IdentityResources;
import org.dasein.cloud.test.network.NetworkResources;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Handles the shared compute resources for executing various tests.
 * <p>Created by George Reese: 2/17/13 8:35 PM</p>
 * @author George Reese
 * @version 2013.04
 * @since 2013.02
 */
public class ComputeResources {
    static private final Logger logger = Logger.getLogger(ComputeResources.class);

    static private final Random random = new Random();

    private CloudProvider   provider;

    private final HashMap<String,String> testMachineImages = new HashMap<String,String>();
    private final HashMap<String,String> testSnapshots     = new HashMap<String, String>();
    private final HashMap<String,String> testVMs           = new HashMap<String, String>();
    private final HashMap<String,String> testVolumes       = new HashMap<String, String>();

    private String        testDataCenterId;
    private Platform      testImagePlatform;
    private String        testVMProductId;
    private String        testVolumeProductId;

    public ComputeResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    public int report() {
        boolean header = false;
        int count = 0;

        testMachineImages.remove(DaseinTestManager.STATELESS);
        if( !testMachineImages.isEmpty() ) {
            logger.info("Provisioned Compute Resources:");
            header = true;
            count += testMachineImages.size();
            DaseinTestManager.out(logger, null, "---> Machine Images", testMachineImages.size() + " " + testMachineImages);
        }
        testSnapshots.remove(DaseinTestManager.STATELESS);
        if( !testSnapshots.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Compute Resources:");
                header = true;
            }
            count += testSnapshots.size();
            DaseinTestManager.out(logger, null, "---> Snapshots", testSnapshots.size() + " " + testSnapshots);
        }
        testVMs.remove(DaseinTestManager.STATELESS);
        if( !testVMs.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Compute Resources:");
                header = true;
            }
            count += testVMs.size();
            DaseinTestManager.out(logger, null, "---> Virtual Machines", testVMs.size() + " " + testVMs);
        }
        testVolumes.remove(DaseinTestManager.STATELESS);
        if( !testVolumes.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Compute Resources:");
            }
            count+= testVolumes.size();
            DaseinTestManager.out(logger, null, "---> Volumes", testVolumes.size() + " " + testVolumes);
        }
        return count;
    }

    public int close() {
        ComputeServices computeServices = provider.getComputeServices();
        int count = 0;

        if( computeServices != null ) {
            VirtualMachineSupport vmSupport = computeServices.getVirtualMachineSupport();

            if( vmSupport != null ) {
                for( Map.Entry<String,String> entry : testVMs.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            VirtualMachine vm = vmSupport.getVirtualMachine(entry.getValue());

                            if( vm != null ) {
                                vmSupport.terminate(entry.getValue());
                                count++;
                            }
                            else {
                                count++;
                            }
                        }
                        catch( Throwable t ) {
                            logger.warn("Failed to de-provision test VM " + entry.getValue() + ": " + t.getMessage());
                        }
                    }
                }
            }

            MachineImageSupport imageSupport = computeServices.getImageSupport();

            if( imageSupport != null ) {
                for( Map.Entry<String,String> entry : testMachineImages.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            MachineImage img = imageSupport.getImage(entry.getValue());

                            if( img != null ) {
                                imageSupport.remove(entry.getValue());
                                count++;
                            }
                            else {
                                count++;
                            }
                        }
                        catch( Throwable t ) {
                            logger.warn("Failed to de-provision test image " + entry.getValue() + ": " + t.getMessage());
                        }
                    }
                }
            }

            SnapshotSupport snapshotSupport = computeServices.getSnapshotSupport();

            if( snapshotSupport != null ) {
                for( Map.Entry<String,String> entry : testSnapshots.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            Snapshot snapshot = snapshotSupport.getSnapshot(entry.getValue());

                            if( snapshot != null ) {
                                snapshotSupport.remove(entry.getValue());
                                count++;
                            }
                            else {
                                count++;
                            }
                        }
                        catch( Throwable t ) {
                            logger.warn("Failed to de-provision test snapshot " + entry.getValue() + " post-test: " + t.getMessage());
                        }
                    }
                }
            }
            VolumeSupport volumeSupport = computeServices.getVolumeSupport();

            if( volumeSupport != null ) {
                for( Map.Entry<String,String> entry : testVolumes.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            Volume volume = volumeSupport.getVolume(entry.getValue());

                            if( volume != null ) {
                                volumeSupport.detach(entry.getValue(), true);
                            }
                        }
                        catch( Throwable ignore ) {
                            // IGNORE
                        }
                    }
                }
                try { Thread.sleep(60000L); }
                catch( InterruptedException ignore ) { }
                for( Map.Entry<String,String> entry : testVolumes.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            Volume volume = volumeSupport.getVolume(entry.getValue());

                            if( volume != null ) {
                                volumeSupport.remove(entry.getValue());
                                count++;
                            }
                            else {
                                count++;
                            }
                        }
                        catch( Throwable t ) {
                            logger.warn("Failed to de-provision test volume " + entry.getValue() + ": " + t.getMessage());
                        }
                    }
                }
            }
        }
        provider.close();
        return count;
    }

    private @Nullable String findStatelessSnapshot() {
        ComputeServices computeServices = provider.getComputeServices();

        if( computeServices != null ) {
            SnapshotSupport support = computeServices.getSnapshotSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Snapshot defaultSnapshot = null;

                    for( Snapshot snapshot : support.listSnapshots() ) {
                        if( snapshot.getCurrentState().equals(SnapshotState.AVAILABLE) ) {
                            defaultSnapshot = snapshot;
                            break;
                        }
                        if( defaultSnapshot == null ) {
                            defaultSnapshot = snapshot;
                        }
                    }
                    if( defaultSnapshot != null ) {
                        String id = defaultSnapshot.getProviderSnapshotId();

                        if( id != null ) {
                            testSnapshots.put(DaseinTestManager.STATELESS, id);
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
                            testDataCenterId = vm.getProviderDataCenterId();
                            return testDataCenterId;
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
                    if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                        id = entry.getValue();
                        if( id != null ) {
                            return id;
                        }
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

    public @Nullable String getTestSnapshotId(@Nonnull String label, boolean provisionIfNull) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testSnapshots.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessSnapshot();
        }
        String id = testSnapshots.get(label);

        if( id != null ) {
            return id;
        }
        if( !provisionIfNull ) {
            return null;
        }
        ComputeServices services = provider.getComputeServices();

        if( services != null ) {
            SnapshotSupport support = services.getSnapshotSupport();

            if( support != null ) {
                try {
                    return provisionSnapshot(support, label, "dsnsnap" + (System.currentTimeMillis()%10000), null);
                }
                catch( Throwable ignore ) {
                    return null;
                }
            }
        }
        return null;
    }

    public @Nullable String getTestVmId(@Nonnull String label, @Nullable VmState desiredState, boolean provisionIfNull, @Nullable String preferredDataCenterId) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testVMs.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        try {
                            @SuppressWarnings("ConstantConditions") VirtualMachine vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(id);

                            if( vm != null && !VmState.TERMINATED.equals(vm.getCurrentState()) ) {
                                return id;
                            }
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
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

                    if( (vm == null || VmState.TERMINATED.equals(vm.getCurrentState())) && provisionIfNull ) {
                        id = provisionVM(support, label, "Dasein Test " + label, "dsnvm", preferredDataCenterId);
                        vm = support.getVirtualMachine(id);
                    }
                    if( vm != null && desiredState != null ) {
                        setState(support, vm, desiredState);
                    }
                    return id;
                }
                catch( Throwable t ) {
                    try {
                        if( support.isSubscribed() ) {
                            logger.warn("Unable to provision test virtual machine under label " + label + ": " + t.getMessage());
                        }
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }

    public @Nullable String getTestVLANVmId(@Nonnull String label, @Nullable VmState desiredState, @Nullable String vlanId, boolean provisionIfNull, @Nullable String preferredDataCenterId) {
      if( label.equals(DaseinTestManager.STATELESS) ) {
        for( Map.Entry<String,String> entry : testVMs.entrySet() ) {
          if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
            String id = entry.getValue();

            if( id != null ) {
              try {
                @SuppressWarnings("ConstantConditions") VirtualMachine vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(id);

                if( vm != null && !VmState.TERMINATED.equals(vm.getCurrentState()) && vm.getProviderVlanId() != null ) {
                  if( vlanId == null ) {
                    return id;
                  }
                  else if( vm.getProviderVlanId().equalsIgnoreCase(vlanId) ) {
                    return id;
                  }
                }
              }
              catch( Throwable ignore ) {
                // ignore
              }
            }
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
            if( (vm == null || VmState.TERMINATED.equals(vm.getCurrentState()) || vm.getProviderVlanId() == null || !vm.getProviderVlanId().equalsIgnoreCase(vlanId)) && provisionIfNull ) {
              String testImageId = getTestImageId(DaseinTestManager.STATELESS, false);
              if( testImageId == null ) {
                throw new CloudException("No test image exists for provisioning a virtual machine");
              }
              long now = System.currentTimeMillis();
              String name = "Dasein Test " + label + " " + now;
              String host = "dsnvm" + (now%10000);
              VMLaunchOptions vmOpts = VMLaunchOptions.getInstance(testVMProductId, testImageId, name, host, "Test VM for stateful integration tests for Dasein Cloud").withExtendedAnalytics();
              NetworkResources network = DaseinTestManager.getNetworkResources();
              if( vlanId != null ) {
                NetworkServices ns = provider.getNetworkServices();
                VLANSupport vs = ns.getVlanSupport();
                VLAN v = vs.getVlan( vlanId );
                Iterable<Subnet> subnets = vs.listSubnets( vlanId );
                if( subnets.iterator().hasNext() ) {
                  Subnet sub = subnets.iterator().next();
                  vmOpts.inVlan( null, v.getProviderDataCenterId(), sub.getProviderSubnetId() );
                } else {
                  Subnet sub = vs.createSubnet(SubnetCreateOptions.getInstance(vlanId, "192.168.50.0/24", "dsnsub", "dasein test create vm for vlan"));
                  vmOpts.inVlan( null, v.getProviderDataCenterId(), sub.getProviderSubnetId() );
                }
              } else {
                if( network != null ) {
                  String networkId = network.getTestVLANId(DaseinTestManager.STATEFUL, true, preferredDataCenterId);

                  if( networkId == null ) {
                    networkId = network.getTestVLANId(DaseinTestManager.STATELESS, false, preferredDataCenterId);
                  }

                  // wait for network to be ready
                  try { Thread.sleep(10000L); }
                  catch( InterruptedException ignore ) { }

                  if( networkId != null ) {

                    String subnetId = network.getTestSubnetId(DaseinTestManager.STATEFUL, true, networkId, preferredDataCenterId);

                    if( subnetId == null ) {
                      subnetId = network.getTestSubnetId(DaseinTestManager.STATELESS, true, networkId, preferredDataCenterId);
                    }
                    if( subnetId != null ) {

                      // wait for subnet to be ready
                      try { Thread.sleep(10000L); }
                      catch( InterruptedException ignore ) { }

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
                        vmOpts.inVlan(null, dcId, subnetId);
                      }
                    }
                  }
                }
              }
              id = provisionVM(support, label, vmOpts, preferredDataCenterId);
              vm = support.getVirtualMachine(id);
            }
            if( vm != null && desiredState != null ) {
              setState(support, vm, desiredState);
            }
            if( vlanId != null && vm.getProviderVlanId().equalsIgnoreCase( vlanId ) && id != null ) {
              return id;
            }
            else if( vlanId == null && id != null ) {
              return id;
            }
            else {
              return null;
            }
          }
          catch( Throwable t ) {
            try {
              if( support.isSubscribed() ) {
                logger.warn("Unable to provision test virtual machine under label " + label + ": " + t.getMessage());
              }
            }
            catch( Throwable ignore ) {
              // ignore
            }
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
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return null;
        }
        String id = testVolumes.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            ComputeServices services = provider.getComputeServices();

            if( services != null ) {
                VolumeSupport support = services.getVolumeSupport();

                if( support != null ) {
                    try {
                        return provisionVolume(support, label, "dsnvol" + (System.currentTimeMillis()%10000), desiredFormat, preferredDataCenterId);
                    }
                    catch( Throwable ignore ) {
                        return null;
                    }
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
                        for( Platform platform : new Platform[] { Platform.UBUNTU, Platform.CENT_OS, Platform.WINDOWS, Platform.RHEL } ) {
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
                            options = ImageFilterOptions.getInstance(ImageClass.MACHINE).withArchitecture(architecture).onPlatform(platform);
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
            throw new CloudException("Unable to provisionKeypair a machine image because Dasein Cloud is showing no VM support");
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

    public @Nonnull String provisionSnapshot(@SuppressWarnings("UnusedParameters") @Nonnull SnapshotSupport support, @Nonnull String label, @Nonnull String namePrefix, @Nullable String volumeId) throws CloudException, InternalException {
        SnapshotCreateOptions options;

        if( volumeId == null ) {
            volumeId = getTestVolumeId(DaseinTestManager.STATEFUL + (System.currentTimeMillis()%1000), true, null, null);
            if( volumeId == null ) {
                throw new CloudException("No volume from which to create a snapshot");
            }
        }
        @SuppressWarnings("ConstantConditions") VolumeSupport vs = provider.getComputeServices().getVolumeSupport();

        if( vs != null ) {
            Volume volume = vs.getVolume(volumeId);

            if( volume != null ) {
                long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*20L);

                while( timeout > System.currentTimeMillis() ) {
                    try { Thread.sleep(15000L); }
                    catch( InterruptedException ignore ) { }
                    try { volume = vs.getVolume(volumeId); }
                    catch( Throwable ignore ) { }
                    if( volume == null || volume.getCurrentState().equals(VolumeState.AVAILABLE) || volume.getCurrentState().equals(VolumeState.DELETED) ) {
                        break;
                    }
                }
            }
            if( volume != null && volume.getProviderVirtualMachineId() == null && support.identifyAttachmentRequirement().equals(Requirement.REQUIRED) ) {
                String vmId = getTestVmId(DaseinTestManager.STATEFUL, VmState.RUNNING, true, volume.getProviderDataCenterId());

                if( vmId != null ) {
                    @SuppressWarnings("ConstantConditions") VirtualMachine vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(vmId);

                    if( vm != null ) {
                        for( String deviceId : vs.listPossibleDeviceIds(vm.getPlatform()) ) {
                            try {
                                vs.attach(volumeId, vmId, deviceId);
                                break;
                            }
                            catch( Throwable ignore ) {
                                // ignore
                            }
                        }
                    }
                }
            }
        }
        options = SnapshotCreateOptions.getInstanceForCreate(volumeId, namePrefix + (System.currentTimeMillis()%10000), "Dasein Snapshot Test " + label);
        String id = options.build(provider);

        if( id == null ) {
            throw new CloudException("Unable to create a snapshot");
        }
        synchronized( testSnapshots ) {
            while( testSnapshots.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testSnapshots.put(label, id);
        }
        return id;

    }

    public @Nonnull String provisionVM(@Nonnull VirtualMachineSupport support, @Nonnull String label, @Nonnull VMLaunchOptions options, @Nullable String preferredDataCenter) throws CloudException, InternalException {

        if( preferredDataCenter != null ) {
            options.inDataCenter(preferredDataCenter);
        }
        if( options.getBootstrapUser() == null && Requirement.REQUIRED.equals(support.identifyPasswordRequirement(testImagePlatform)) ) {
            options.withBootstrapUser("dasein", "x" + random.nextInt(100000) + System.currentTimeMillis());
        }
        if( options.getBootstrapKey() == null && Requirement.REQUIRED.equals(support.identifyShellKeyRequirement(testImagePlatform)) ) {
            IdentityResources identity = DaseinTestManager.getIdentityResources();

            if( identity != null ) {
                String keypairId = identity.getTestKeypairId(DaseinTestManager.STATEFUL, true);

                if( keypairId != null ) {
                    options.withBoostrapKey(keypairId);
                }
            }
        }
        if( options.getVlanId() == null && Requirement.REQUIRED.equals(support.identifyVlanRequirement()) ) {
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
        if( options.getStaticIpIds().length < 1 && Requirement.REQUIRED.equals(support.identifyStaticIPRequirement()) ) {
            NetworkResources network = DaseinTestManager.getNetworkResources();

            if( network != null ) {
                String ipId;

                if( options.getVlanId() != null ) {
                    ipId = network.getTestStaticIpId(label, true, null, true, options.getVlanId());
                }
                else {
                    ipId = network.getTestStaticIpId(label, true, null, false, null);
                }
                if( ipId != null ) {
                    options.withStaticIps(ipId);
                }
            }
        }
        if( options.getRootVolumeProductId() == null && Requirement.REQUIRED.equals(support.identifyRootVolumeRequirement()) && testVolumeProductId != null ) {
            options.withRootVolumeProduct(testVolumeProductId);
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

    /**
     * Provisions a virtual machine and returns the ID of the new virtual machine. This method tracks the newly provisioned
     * virtual machine and will tear it down at the end of the test suite.
     * @param support the virtual machine support object used to provisionKeypair the VM
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

        return provisionVM(support, label, VMLaunchOptions.getInstance(testVMProductId, testImageId, name, host, "Test VM for stateful integration tests for Dasein Cloud").withExtendedAnalytics(), preferredDataCenter);
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
                    support.stop(id, true);
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
