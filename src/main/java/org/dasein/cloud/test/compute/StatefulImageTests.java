package org.dasein.cloud.test.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/19/13 2:35 PM</p>
 *
 * @author George Reese
 */
public class StatefulImageTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulImageTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testImageId;

    public StatefulImageTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testImageId = tm.getTestImageId(DaseinTestManager.STATEFUL, true);
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void listShares() throws CloudException, InternalException {
        assumeTrue(!tm.isTestSkipped());
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            MachineImageSupport support = services.getImageSupport();

            if( support != null ) {
                if( testImageId != null ) {
                    MachineImage image = support.getImage(testImageId);

                    assertNotNull("Failed to find the test image among possible images", image);
                    Iterable<String> shares = support.listShares(testImageId);

                    tm.out("Image Shares", shares);
                    assertNotNull("Image shares may not be null", shares);
                }
                else {
                    if( !support.isSubscribed() ) {
                        tm.warn("No image ID was identified, so this test is not valid");
                    }
                    else {
                        fail("No test image exists for the getImage test");
                    }
                }
            }
            else {
                tm.ok("No image support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }
}
