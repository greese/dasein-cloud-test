/**
 * Copyright (C) 2009-2015 Dell, Inc.
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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.*;
import org.junit.rules.TestName;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.junit.matchers.JUnitMatchers.both;

/**
 * Tests on {@link org.dasein.cloud.compute.VirtualMachineSupport} that do not involve making any changes to the system.
 * <p>Created by George Reese: 2/17/13 3:22 PM</p>
 *
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatelessVMTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessVMTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testVMId;
    private String testImageId;
    private String testProductId;
    private String testDataCenterId = null;

    public StatelessVMTests() {
    }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        testDataCenterId = DaseinTestManager.getDefaultDataCenterId(true);

        testVMId = tm.getTestVMId(DaseinTestManager.STATELESS, null, true, testDataCenterId);
        testImageId = DaseinTestManager.getSystemProperty("test.machineImage");
        if( testImageId == null ) {
            testImageId = tm.getTestImageId(DaseinTestManager.STATELESS, false);
        }
        testProductId = tm.getTestVMProductId();
    }

    @After
    public void after() {
        testVMId = null;
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                Map<String, Requirement> requirements = new HashMap<String, Requirement>();
                Requirement r;

                tm.out("Subscribed", support.isSubscribed());

                String termForServer = support.getCapabilities().getProviderTermForVirtualMachine(Locale.getDefault());

                tm.out("Term for VM", termForServer);

                int maxVmCount = support.getCapabilities().getMaximumVirtualMachineCount();

                if( maxVmCount == -2 ) {
                    tm.out("Max VM Count", "Unknown");
                }
                else if( maxVmCount == -1 ) {
                    tm.out("Max VM Count", "Unlimited");
                }
                else {
                    tm.out("Max VM Count", maxVmCount);
                }

                Iterable<Architecture> architectures = support.getCapabilities().listSupportedArchitectures();

                if( architectures == null ) {
                    tm.out("Supported Architectures", null);
                }
                else {
                    List<Architecture> tmp = new ArrayList<Architecture>();

                    for( Architecture a : architectures ) {
                        tmp.add(a);
                    }
                    tm.out("Supported Architectures", tmp);
                }
                for( ImageClass cls : ImageClass.values() ) {
                    r = support.getCapabilities().identifyImageRequirement(cls);
                    tm.out("Image Class Req [" + cls.name() + "]", r);
                    requirements.put("Launch with Image Class " + cls.name() + " Requirement", r);
                }
                for( Platform platform : Platform.values() ) {
                    r = support.getCapabilities().identifyPasswordRequirement(platform);
                    tm.out("Password Req [" + platform.name() + "]", r);
                    requirements.put("Password for Platform " + platform.name() + " Requirement", r);
                }
                for( Platform platform : Platform.values() ) {
                    r = support.getCapabilities().identifyShellKeyRequirement(platform);
                    tm.out("Shell Key Req [" + platform.name() + "]", r);
                    requirements.put("Shell Key for Platform " + platform.name() + " Requirement", r);
                }
                r = support.getCapabilities().identifyRootVolumeRequirement();
                tm.out("Root Volume Req", r);
                requirements.put("Root Volume Requirement", r);
                r = support.getCapabilities().identifyStaticIPRequirement();
                tm.out("Static IP Req", r);
                r = support.getCapabilities().identifyVlanRequirement();
                requirements.put("Static IP Requirement", r);
                tm.out("VLAN Req", r);
                requirements.put("VLAN Requirement", r);
                tm.out("Prevent API Termination", support.getCapabilities().isAPITerminationPreventable());
                tm.out("User Data", support.getCapabilities().isUserDataSupported());

                VMScalingCapabilities capabilities = support.getCapabilities().getVerticalScalingCapabilities();

                if( capabilities == null ) {
                    tm.out("VM Scaling Capabilities", "None");
                }
                else {
                    tm.out("SCALE [Creates New VM]", capabilities.isCreatesNewVirtualMachine());
                    tm.out("SCALE [Product Changes]", capabilities.isSupportsProductChanges());
                    tm.out("SCALE [Product Size Changes]", capabilities.isSupportsProductSizeChanges());
                }
                Map<VmState, Float> costFactors = new HashMap<VmState, Float>();

                for( VmState state : VmState.values() ) {
                    float costFactor = support.getCapabilities().getCostFactor(state);

                    tm.out("Cost Factor [" + state.name() + "]", costFactor);
                    costFactors.put(state, costFactor);
                }

                assertTrue("Max VM count must be -2, -1, or non-negative", maxVmCount >= -2);
                assertNotNull("Supported architectures may not be null", architectures);
                assertNotNull("Term for VM may not be null for any locale (Used " + Locale.getDefault() + ")", termForServer);
                for( Map.Entry<String, Requirement> entry : requirements.entrySet() ) {
                    assertNotNull(entry.getKey() + " may not be null", entry.getValue());
                }
                for( Map.Entry<VmState, Float> entry : costFactors.entrySet() ) {
                    assertTrue("Cost factor for " + entry.getKey().name() + " must be at least 0.0f", entry.getValue() >= 0.0f);
                    assertTrue("Cost factor for " + entry.getKey().name() + " must be no more than 100.0f", entry.getValue() <= 100.0f);
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void deprecatedCapabilities() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                VMScalingCapabilities capabilities = support.getCapabilities().getVerticalScalingCapabilities();
                if( capabilities != null ) {
                    boolean oldCapabilitiesEnabled = !capabilities.getAlterVmForNewVolume().equals(Requirement.NONE) || !capabilities.getAlterVmForVolumeChange().equals(Requirement.NONE);

                    assertFalse("The capabilities indicate that the deprecated volume scaling is supported, however the new product scaling capabilities are not supported, please update your driver", ( !capabilities.isSupportsProductSizeChanges() && !capabilities.isSupportsProductChanges() ) && oldCapabilitiesEnabled);
                }
                else {
                    tm.ok("No VM vertical scaling capabilities in this cloud");
                }
            }
            else {
                tm.ok("No VM support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void getBogusVMProduct() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                VirtualMachineProduct prd = support.getProduct(UUID.randomUUID().toString());

                tm.out("Bogus Product", prd);
                assertNull("Bogus product was supposed to be none, but got a valid product.", prd);
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void getVMProduct() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                assertNotNull("No test product was setup during configuration and so there's no test product to fetch", testProductId);

                VirtualMachineProduct prd = support.getProduct(testProductId);

                tm.out("Product", prd);
                assertNotNull("Test product was not found in the request", prd);
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void productContent() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                assertNotNull("No test product was setup during configuration and so there's no test product to fetch", testProductId);

                VirtualMachineProduct prd = support.getProduct(testProductId);

                assertNotNull("Test product was not found in the request", prd);
                tm.out("Product ID", prd.getProviderProductId());
                tm.out("Name", prd.getName());
                tm.out("CPU Count", prd.getCpuCount());
                tm.out("RAM", prd.getRamSize());
                tm.out("Root Volume", prd.getRootVolumeSize());
                tm.out("Hourly Rate", prd.getStandardHourlyRate());
                tm.out("Description", prd.getDescription());

                assertNotNull("Product ID must not be null", prd.getProviderProductId());
                assertNotNull("Product name may not be null", prd.getName());
                assertNotNull("Product description may not be null", prd.getDescription());
                assertTrue("Product CPU count must be greater than 0", prd.getCpuCount() > 0);
                assertNotNull("Product RAM size must not be null", prd.getRamSize());
                assertTrue("Product RAM size must be greater than 0", prd.getRamSize().intValue() > 0);
                assertNotNull("Product root volume size may not be null", prd.getRootVolumeSize());
                assertTrue("Product root volume size must be greater than 0", prd.getRootVolumeSize().intValue() > 0);
                assertTrue("Product hourly rate may not be negative", prd.getStandardHourlyRate() >= 0.00f);
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void listVMProducts() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                boolean found = false;
                int total = 0;

                for( Architecture architecture : support.getCapabilities().listSupportedArchitectures() ) {
                    Iterable<VirtualMachineProduct> products = support.listProducts(architecture);
                    int count = 0;

                    assertNotNull("listProducts() must return at least an empty collections and may not be null", products);
                    for( VirtualMachineProduct product : products ) {
                        count++;
                        total++;
                        tm.out("VM Product [" + architecture.name() + "]", product);
                        if( testProductId != null && testProductId.equals(product.getProviderProductId()) ) {
                            found = true;
                        }
                    }
                    tm.out("Total " + architecture.name() + " Product Count", count);
                }
                if( total < 1 && support.isSubscribed() ) {
                    if( testProductId == null ) {
                        tm.warn("No products were listed and thus the test may be in error");
                    }
                    else {
                        fail("Should have found test product " + testProductId + ", but none were found");
                    }
                }
                else if( testProductId != null ) {
                    assertTrue("Failed to find test product " + testProductId + " among the listed products", found);
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    // TODO(stas): this is a bare-bones test that doesn't test anything specific to
    // listProducts(String imageId) so it will need further work.
    @Test
    public void listVMProductsForImage() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services == null ) {
            tm.ok("No compute services in this cloud");
            return;
        }
        VirtualMachineSupport support = services.getVirtualMachineSupport();

        if( support == null ) {
            tm.ok("No virtual machine support in this cloud");
            return;
        }
        MachineImageSupport imageSupport = services.getImageSupport();
        if( imageSupport == null ) {
            tm.ok("No machine image support in this cloud");
            return;
        }
        assertNotNull("No test imageId for the test", testImageId);

        boolean found = false;

        MachineImage image = imageSupport.getImage(testImageId);
        Iterator<VirtualMachineProduct> productsByArch = support.listProducts(VirtualMachineProductFilterOptions.getInstance().withArchitecture(image.getArchitecture())).iterator();
        int totalByArch = 0;
        for( ; productsByArch.hasNext(); productsByArch.next() ) {
            totalByArch++;
        }
        tm.out("Total Product Count Filtered by Architecture", totalByArch);

        Iterable<VirtualMachineProduct> products = support.listProducts(testImageId);
        int totalByImage = 0;

        assertNotNull("listProducts() must return at least an empty collections and may not be null", products);
        for( VirtualMachineProduct product : products ) {
            totalByImage++;
            // TODO: check that the product is the same architecture as the image at the very least
        }
        tm.out("Total Product Count Filtered by Image", totalByImage);
        assertThat("Number of products filtered by image should be lesser than or equal to the number of products filtered by this image's architecture alone", totalByImage, lessThanOrEqualTo(totalByArch));
    }

    @Test
    public void getBogusVM() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                VirtualMachine vm = support.getVirtualMachine(UUID.randomUUID().toString());

                tm.out("Bogus VM", vm);
                assertNull("Bogus VM was supposed to be none, but got a valid virtual machine.", vm);
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    /**
     * This test is likely to fail most of the times, as password is not always available (e.g. in AWS
     * it's only available on clean Amazon images), and requires for instance to be up for some time for
     * password to be ready.
     *
     * <p>TODO: This test needs more attention.</p>
     *
     * @throws CloudException
     * @throws InternalException
     */
    @Test
    public void getVMPassword() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVMId != null ) {
                    String pass = support.getPassword(testVMId);

                    tm.out("Password for vm: ", pass);
                    assertNotNull("Did not find the password for test virtual machine " + testVMId, pass);
                }
                else if( support.isSubscribed() ) {
                    fail("No test virtual machine exists and thus no test could be run for getVMPassword");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    /**
     * This test should performed outside virtualMachineContent() test as getUserData()
     * is a separate call.
     *
     * @throws CloudException
     * @throws InternalException
     */
    @Test
    public void getVMUserData() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVMId != null ) {
                    String data = support.getUserData(testVMId);

                    tm.out("User data for vm: ", data);
                    assertNotNull("Did not find the user-data for test virtual machine " + testVMId, data);
                    assertTrue("User-data must contain 'echo \"dasein\"'", data.contains("echo \"dasein\""));
                }
                else if( support.isSubscribed() ) {
                    fail("No test virtual machine exists and thus no test could be run for getVMUserData");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void getVirtualMachine() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVMId != null ) {
                    VirtualMachine vm = support.getVirtualMachine(testVMId);

                    tm.out("Test Virtual Machine", vm);
                    assertNotNull("Did not find the test virtual machine " + testVMId, vm);
                }
                else if( support.isSubscribed() ) {
                    fail("No test virtual machine exists and thus no test could be run for getVirtualMachine");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void virtualMachineContent() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVMId != null ) {
                    VirtualMachine vm = support.getVirtualMachine(testVMId);

                    assertNotNull("Did not find the test virtual machine " + testVMId, vm);

                    tm.out("Virtual Machine ID", vm.getProviderVirtualMachineId());
                    tm.out("Current State", vm.getCurrentState());
                    tm.out("Name", vm.getName());
                    tm.out("Created", ( new Date(vm.getCreationTimestamp()) ));
                    tm.out("Owner Account", vm.getProviderOwnerId());
                    tm.out("Region ID", vm.getProviderRegionId());
                    tm.out("Data Center ID", vm.getProviderDataCenterId());
                    tm.out("VLAN ID", vm.getProviderVlanId());
                    tm.out("Subnet ID", vm.getProviderSubnetId());
                    tm.out("Product ID", vm.getProductId());
                    tm.out("Architecture", vm.getArchitecture());
                    tm.out("Platform", vm.getPlatform());
                    tm.out("Machine Image ID", vm.getProviderMachineImageId());
                    tm.out("Kernel Image ID", vm.getProviderKernelImageId());
                    tm.out("Ramdisk Image ID", vm.getProviderRamdiskImageId());
                    tm.out("Assigned IP ID", vm.getProviderAssignedIpAddressId());
                    tm.out("Public IPs", Arrays.toString(vm.getPublicAddresses()));
                    tm.out("Public DNS", vm.getPublicDnsAddress());
                    tm.out("Private IPs", Arrays.toString(vm.getPrivateAddresses()));
                    tm.out("Private DNS", vm.getPrivateDnsAddress());
                    tm.out("Shell Key IDs", Arrays.toString(vm.getProviderShellKeyIds()));
                    tm.out("Firewall IDs", Arrays.toString(vm.getProviderFirewallIds()));
                    tm.out("Root User", vm.getRootUser());
                    tm.out("Root Password", vm.getRootPassword());
                    tm.out("Pause", support.getCapabilities().canPause(vm.getCurrentState()));
                    tm.out("Unpause", support.getCapabilities().canUnpause(vm.getCurrentState()));
                    tm.out("Start", support.getCapabilities().canStart(vm.getCurrentState()));
                    tm.out("Stop", support.getCapabilities().canStop(vm.getCurrentState()));
                    tm.out("Suspend", support.getCapabilities().canSuspend(vm.getCurrentState()));
                    tm.out("Resume", support.getCapabilities().canResume(vm.getCurrentState()));
                    tm.out("Clonable", vm.isClonable());
                    tm.out("Imageable", vm.isImagable());
                    tm.out("Rebootable", vm.isRebootable());
                    tm.out("Description", vm.getDescription());

                    Map<String, String> tags = vm.getTags();
                    assertNotNull("Tags may not be null", vm.getTags());

                    for( Map.Entry<String, String> entry : tags.entrySet() ) {
                        tm.out("Tag " + entry.getKey(), entry.getValue());
                    }

                    assertNotNull("VM ID may not be null", vm.getProviderVirtualMachineId());
                    assertNotNull("VM state may not be null", vm.getCurrentState());
                    assertNotNull("VM name may not be null", vm.getName());
                    assertNotNull("VM description may not be null", vm.getDescription());
                    assertTrue("VM creation may not be negative", vm.getCreationTimestamp() >= 0L);
                    assertNotNull("Owning account may not be null", vm.getProviderOwnerId());
                    assertNotNull("VM region may not null", vm.getProviderRegionId());
                    assertEquals("VM region must match current search region", tm.getContext().getRegionId(), vm.getProviderRegionId());
                    assertNotNull("VM data center ID may not be null", vm.getProviderDataCenterId());
                    assertNotNull("VM product ID may not be null", vm.getProductId());
                    assertNotNull("VM architecture may not be null", vm.getArchitecture());
                    assertNotNull("VM platform may not be null", vm.getPlatform());
                    assertNotNull("Public IP addresses must not be null", vm.getPublicAddresses());
                    assertNotNull("Private IP addresses must not be null", vm.getPrivateAddresses());
                    assertNotNull("Shell key ID list may not be null", vm.getProviderShellKeyIds());
                    assertNotNull("Firewall ID list may not be null", vm.getProviderFirewallIds());
                }
                else if( support.isSubscribed() ) {
                    fail("No test virtual machine exists and thus no test could be run for getVirtualMachine");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void listVMs() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                Iterable<VirtualMachine> vms = support.listVirtualMachines();
                boolean found = false;
                int count = 0;

                assertNotNull("listVirtualMachines() must return at least an empty collections and may not be null", vms);
                for( VirtualMachine vm : vms ) {
                    count++;
                    tm.out("VM", vm);
                    if( testVMId != null && testVMId.equals(vm.getProviderVirtualMachineId()) ) {
                        found = true;
                    }
                }
                tm.out("Total VM Count", count);
                if( count < 1 && support.isSubscribed() ) {
                    if( testVMId == null ) {
                        tm.warn("No virtual machines were listed and thus the test may be in error");
                    }
                    else {
                        fail("Should have found test virtual machine " + testVMId + ", but none were found");
                    }
                }
                else if( testVMId != null ) {
                    assertTrue("Failed to find test VM " + testVMId + " among the listed virtual machines", found);
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void listSpotPriceHistories() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services == null ) {
            tm.ok("No compute services in this cloud");
            return;
        }

        VirtualMachineSupport support = services.getVirtualMachineSupport();

        if( support == null ) {
            tm.ok("No virtual machine support in this cloud");
            return;
        }
        if( !support.getCapabilities().supportsSpotVirtualMachines() ) {
            tm.ok("Spot Virtual Machines are not supported in this cloud");
            return;
        }

        long timeEnd = System.currentTimeMillis();
        long timeStart = System.currentTimeMillis() - 2 * 60 * 60 * 1000; // half an hour back
        SpotPriceHistoryFilterOptions options = SpotPriceHistoryFilterOptions.getInstance().matchingAny().matchingInterval(timeStart, timeEnd);
        Iterable<SpotPriceHistory> histories = support.listSpotPriceHistories(options);
        int count = 0;
        int prices = 0;
        assertNotNull("listSpotPriceHistories() must return at least an empty collection and never a null", histories);
        for( SpotPriceHistory h : histories ) {
            count++;
            tm.out("SpotPriceHistory", h);
            for( SpotPrice price : h.getPriceHistory() ) {
                assertThat("SpotPrice timestamp outside requested bounds", price.getTimestamp(), is(both(greaterThan(timeStart)).and(lessThan(timeEnd))));
                prices++;
            }
        }
        tm.out("Total SpotPriceHistory Count", count);
        tm.out("Total SpotPrice Count", prices);

        if( count > 0 ) {
            Iterator<SpotPriceHistory> it = histories.iterator();
            VirtualMachineProduct prod = null;
            Map<String, String> triedProducts = new HashMap<String, String>();
            while( it.hasNext() ) {
                SpotPriceHistory sph = it.next();
                if( triedProducts.get(sph.getProductId()) != null ) {
                    continue;
                }
                prod = support.getProduct(sph.getProductId());
                if( prod != null ) {
                    break;
                }
                else {
                    triedProducts.put(sph.getProductId(), sph.getProductId());
                }
            }
            assertNotNull(String.format("All %d SpotPriceHistory items were for an unknown product %s", count, Arrays.toString(triedProducts.keySet().toArray(new String[triedProducts.size()]))), prod);
        }
        else {
            tm.warn("SpotPriceHistory is empty, the test is likely INVALID");
        }
    }

    @Test
    public void listVMStatus() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                Iterable<ResourceStatus> vms = support.listVirtualMachineStatus();
                boolean found = false;
                int count = 0;

                assertNotNull("listVirtualMachineStatus() must return at least an empty collections and may not be null", vms);
                for( ResourceStatus vm : vms ) {
                    count++;
                    tm.out("VM Status", vm);
                    if( testVMId != null && testVMId.equals(vm.getProviderResourceId()) ) {
                        found = true;
                    }
                }
                tm.out("Total VM Count", count);
                if( count < 1 && support.isSubscribed() ) {
                    if( testVMId == null ) {
                        tm.warn("No virtual machines were listed and thus the test may be in error");
                    }
                    else {
                        fail("Should have found test virtual machine " + testVMId + ", but none were found");
                    }
                }
                else if( testVMId != null ) {
                    assertTrue("Failed to find test VM " + testVMId + " in the listed virtual machine status", found);
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void compareVMListAndStatus() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                Map<String, Map<String, Boolean>> map = new HashMap<String, Map<String, Boolean>>();
                Iterable<VirtualMachine> vms = support.listVirtualMachines();
                Iterable<ResourceStatus> status = support.listVirtualMachineStatus();

                assertNotNull("listVirtualMachines() must return at least an empty collections and may not be null", vms);
                assertNotNull("listVirtualMachineStatus() must return at least an empty collections and may not be null", status);
                for( ResourceStatus s : status ) {
                    Map<String, Boolean> current = map.get(s.getProviderResourceId());

                    if( current == null ) {
                        current = new HashMap<String, Boolean>();
                        map.put(s.getProviderResourceId(), current);
                    }
                    current.put("status", true);
                }
                for( VirtualMachine vm : vms ) {
                    Map<String, Boolean> current = map.get(vm.getProviderVirtualMachineId());

                    if( current == null ) {
                        current = new HashMap<String, Boolean>();
                        map.put(vm.getProviderVirtualMachineId(), current);
                    }
                    current.put("vm", true);
                }
                for( Map.Entry<String, Map<String, Boolean>> entry : map.entrySet() ) {
                    Boolean s = entry.getValue().get("status");
                    Boolean v = entry.getValue().get("vm");

                    assertTrue("Status and VM lists do not match for " + entry.getKey(), s != null && v != null && s && v);
                }
                tm.out("Matches");
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void requestSpotVm() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());

        ComputeServices computeServices = tm.getProvider().getComputeServices();
        if( computeServices == null ) {
            tm.ok("No compute services in this cloud");
            return;
        }

        VirtualMachineSupport vmSupport = computeServices.getVirtualMachineSupport();

        if( vmSupport == null ) {
            tm.ok("No virtual machine support in this cloud");
            return;
        }

        if( !vmSupport.getCapabilities().supportsSpotVirtualMachines() ) {
            tm.ok("Spot Virtual Machines are not supported in this cloud");
            return;
        }
        // we need to make sure the image has been created
        assertNotNull("Test imageId is null", testImageId);
        assertNotNull("Test productId is null", testProductId);
        List<VirtualMachine> before = new ArrayList<VirtualMachine>();
        for( VirtualMachine vm : vmSupport.listVirtualMachines(VMFilterOptions.getInstance().withLifecycles(VirtualMachineLifecycle.SPOT)) ) {
            before.add(vm);
        }
        if( before.size() > 0 ) {
            tm.warn("There are " + before.size() + " spot VMs already running, test may not be clean or you may have leaking resources.");
        }
        long validFrom = ( System.currentTimeMillis() / 1000 ) * 1000 + ( 15 * 1000 ); // now + 15 seconds
        long validUntil = validFrom + ( 10 * 60 * 1000 ); // +5 minutes
        float price = 0.05f;
        SpotVirtualMachineRequest request = vmSupport.createSpotVirtualMachineRequest(SpotVirtualMachineRequestCreateOptions.getInstance(testProductId, testImageId, price).validFrom(validFrom).validUntil(validUntil).ofType(SpotVirtualMachineRequestType.ONE_TIME));
        assertNotNull(request);
        assertEquals("Created Spot VM request has an invalid product", testProductId, request.getProductId());
        assertEquals("Created Spot VM request has an invalid image", testImageId, request.getProviderMachineImageId());
        assertEquals("Created Spot VM request has an invalid price", price, request.getSpotPrice(), 0.01);
        assertEquals("Created Spot VM request has an incorrect valid-from time", validFrom, request.getValidFromTimestamp());
        assertEquals("Created Spot VM request has an incorrect valid-until time", validUntil, request.getValidUntilTimestamp());
        assertEquals("Created Spot VM request has an incorrect type", SpotVirtualMachineRequestType.ONE_TIME, request.getType());
        if( System.currentTimeMillis() < validFrom ) {
            assertNull("Created Spot VM request indicates a fulfilled DC but it is too soon", request.getFulfillmentDataCenterId());
            assertEquals("Created Spot VM request indicates a fulfillment time but it is too soon", 0, request.getFulfillmentTimestamp());
        }

        VirtualMachine launchedVm = null;
        while( launchedVm == null ) {
            Iterator<VirtualMachine> vms = vmSupport.listVirtualMachines(VMFilterOptions.getInstance().withSpotRequestId(request.getProviderSpotVmRequestId())).iterator();
            if( vms.hasNext() ) {
                launchedVm = vms.next();
            }
            try {
                Thread.sleep(60000);
            } catch( InterruptedException e ) {
            }
            if( System.currentTimeMillis() > validUntil ) {
                break;
            }
        }
        if( launchedVm != null ) {
            assertEquals("Found Spot VM is an incorrect product", testProductId, launchedVm.getProductId());
            assertEquals("Found Spot VM is launched from incorrect image", testImageId, launchedVm.getProviderMachineImageId());
        }
        else {
            tm.warn("Spot VM has failed to run within the allotted time, perhaps price is too low?");
        }

        Iterator<SpotVirtualMachineRequest> requests = vmSupport.listSpotVirtualMachineRequests(SpotVirtualMachineRequestFilterOptions.getInstance().withSpotRequestIds(request.getProviderSpotVmRequestId())).iterator();
        SpotVirtualMachineRequest verifyRequest = null;
        if( requests.hasNext() ) {
            verifyRequest = requests.next();
        }
        assertNotNull("Expected to be able to list the known spot request, but failed", verifyRequest);
        assertEquals("Found Spot VM request indicates an incorrect image", testImageId, verifyRequest.getProviderMachineImageId());
        assertEquals("Found Spot VM request indicates an incorrect product", testProductId, verifyRequest.getProductId());
        assertEquals("Found Spot VM request indicates incorrect valid-from time", validFrom, verifyRequest.getValidFromTimestamp());
        assertEquals("Found Spot VM request indicates incorrect valid-until time", validUntil, verifyRequest.getValidUntilTimestamp());
        // only verify the fulfillment if the vm has indeed been fulfilled
        if( launchedVm != null ) {
            assertThat("Found fulfilled Spot VM request indicates an incorrect fulfillment time", verifyRequest.getFulfillmentTimestamp(), greaterThan(0L));
            assertEquals("Found Spot VM request indicates an incorrect datacenter", launchedVm.getProviderDataCenterId(), verifyRequest.getFulfillmentDataCenterId());
        } else {
            assertThat("Found unfulfilled Spot VM request indicates an incorrect fulfillment time", verifyRequest.getFulfillmentTimestamp(), equalTo(0L));

        }

        vmSupport.cancelSpotVirtualMachineRequest(request.getProviderSpotVmRequestId());
        if( launchedVm != null ) {
            // TODO: add vm instance to TestManager so it would clean up instead?
            vmSupport.terminate(launchedVm.getProviderVirtualMachineId());
        }
    }

}
