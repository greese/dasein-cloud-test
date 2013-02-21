package org.dasein.cloud.test.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/20/13 6:10 PM</p>
 *
 * @author George Reese
 */
public class StatefulVolumeTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulVolumeTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String provisionedVolume;
    private String testVLANId;

    public StatefulVolumeTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testVLANId = tm.getTestVLANId(DaseinTestManager.STATELESS, false, null);
        if( testVLANId == null ) {
            testVLANId = tm.getTestVLANId(DaseinTestManager.STATEFUL, true, null);
        }
    }

    @After
    public void after() {
        try {
            ComputeServices services = tm.getProvider().getComputeServices();

            if( services != null ) {
                VolumeSupport support = services.getVolumeSupport();

                if( support != null ) {
                    if( provisionedVolume != null ) {
                        try {
                            support.detach(provisionedVolume, true);
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                        try { Thread.sleep(10000L); }
                        catch( InterruptedException ignore ) { }
                        try {
                            support.remove(provisionedVolume);
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }
                    }
                }
            }
            provisionedVolume = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void createBlockVolume() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                boolean supported = support.isSubscribed();

                if( supported ) {
                    supported = false;
                    for( VolumeFormat fmt : support.listSupportedFormats() ) {
                        if( fmt.equals(VolumeFormat.BLOCK) ) {
                            supported = true;
                        }
                    }
                }

                String productId = tm.getTestVolumeProductId();
                VolumeCreateOptions options = null;

                if( productId != null ) {
                    Storage<Gigabyte> size = null;

                    if( support.isVolumeSizeDeterminedByProduct() ) {
                        VolumeProduct product = null;

                        for( VolumeProduct prd : support.listVolumeProducts() ) {
                            if( prd.getProviderProductId().equals(productId) ) {
                                product = prd;
                                break;
                            }
                        }
                        if( product != null ) {
                            size = product.getVolumeSize();
                        }
                    }
                    if( size == null ) {
                        size = support.getMinimumVolumeSize();
                    }
                    options = VolumeCreateOptions.getInstance(productId, size, "dsnvolprv" + (System.currentTimeMillis()%10000), "Volume Provisioning Test", 0);
                }
                if( options == null ) {
                    options = VolumeCreateOptions.getInstance(support.getMinimumVolumeSize(), "dsnvolprv" + (System.currentTimeMillis()%10000), "Volume Provisioning Test");
                }

                if( supported ) {
                    provisionedVolume = options.build(tm.getProvider());

                    tm.out("New Block Volume", provisionedVolume);
                }
                else {
                    try {
                        provisionedVolume = options.build(tm.getProvider());
                        fail("Block volumes are either not subscribed or supported, yet the operation completed");
                    }
                    catch( OperationNotSupportedException expected ) {
                        tm.ok("Got an OperationNotSupportedException from " + name.getMethodName() + " as expected");
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void createNFSVolume() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VolumeSupport support = services.getVolumeSupport();

            if( support != null ) {
                boolean supported = support.isSubscribed();

                if( supported ) {
                    supported = false;
                    for( VolumeFormat fmt : support.listSupportedFormats() ) {
                        if( fmt.equals(VolumeFormat.NFS) ) {
                            supported = true;
                        }
                    }
                }
                if( testVLANId != null ) {
                    String productId = tm.getTestVolumeProductId();
                    VolumeCreateOptions options = null;

                    if( productId != null ) {
                        Storage<Gigabyte> size = null;

                        if( support.isVolumeSizeDeterminedByProduct() ) {
                            VolumeProduct product = null;

                            for( VolumeProduct prd : support.listVolumeProducts() ) {
                                if( prd.getProviderProductId().equals(productId) ) {
                                    product = prd;
                                    break;
                                }
                            }
                            if( product != null ) {
                                size = product.getVolumeSize();
                            }
                        }
                        if( size == null ) {
                            size = support.getMinimumVolumeSize();
                        }
                        options = VolumeCreateOptions.getNetworkInstance(productId, testVLANId, size, "dsnnfsvol" + (System.currentTimeMillis()%10000), "Dasein NFS volume test");
                    }
                    if( options == null ) {
                        options = VolumeCreateOptions.getNetworkInstance(testVLANId, support.getMinimumVolumeSize(), "dsnvolprv" + (System.currentTimeMillis()%10000), "Volume Provisioning Test");
                    }

                    if( supported ) {
                        provisionedVolume = options.build(tm.getProvider());

                        tm.out("New NFS Volume", provisionedVolume);
                    }
                    else {
                        try {
                            provisionedVolume = options.build(tm.getProvider());
                            fail("NFS volumes are either not subscribed or supported, yet the operation completed");
                        }
                        catch( OperationNotSupportedException expected ) {
                            tm.ok("Got an OperationNotSupportedException from " + name.getMethodName() + " as expected");
                        }
                    }
                }
                else {
                    if( !supported ) {
                        tm.ok("Either network volumes are not supported or volumes are not subscribed");
                    }
                    else {
                        fail("Unable to test network volume provisioning due to a lack of a network in which to test");
                    }
                }
            }
            else {
                tm.ok("No volume support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }
}
