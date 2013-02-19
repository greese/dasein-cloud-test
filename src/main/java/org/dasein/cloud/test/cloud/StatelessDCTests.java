package org.dasein.cloud.test.cloud;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static junit.framework.Assert.*;

import java.util.Locale;
import java.util.UUID;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/18/13 5:51 PM</p>
 *
 * @author George Reese
 */
public class StatelessDCTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessDCTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testDataCenterId;

    public StatelessDCTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        try {
            DataCenterServices services = tm.getProvider().getDataCenterServices();

            for( DataCenter dc : services.listDataCenters(tm.getContext().getRegionId()) ) {
                if( testDataCenterId == null || dc.isActive() ) {
                    testDataCenterId = dc.getProviderDataCenterId();
                }
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void configuration() throws CloudException, InternalException {
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        tm.out("DC Services", services);
        assertNotNull("Data center services must be implemented for all clouds", services);
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        tm.out("Term for Region", services.getProviderTermForRegion(Locale.getDefault()));
        tm.out("Term for DataCenter", services.getProviderTermForDataCenter(Locale.getDefault()));
        assertNotNull("The provider term for region may not be null", services.getProviderTermForRegion(Locale.getDefault()));
        assertNotNull("The provider term for data center may not be null", services.getProviderTermForDataCenter(Locale.getDefault()));
    }

    @Test
    public void getBogusRegion() throws CloudException, InternalException {
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        Region region = services.getRegion(UUID.randomUUID().toString());

        tm.out("Bogus Region", region);
        assertNull("Dummy region must be null, but one was found", region);
    }

    @Test
    public void getRegion() throws CloudException, InternalException {
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        Region region = services.getRegion(tm.getContext().getRegionId());

        tm.out("Region", region);
        assertNotNull("Failed to find the region associated with the current operational context", region);
    }

    @Test
    public void regionContent() throws CloudException, InternalException {
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        Region region = services.getRegion(tm.getContext().getRegionId());

        tm.out("Region ID", region.getProviderRegionId());
        tm.out("Active", region.isActive());
        tm.out("Available", region.isAvailable());
        tm.out("Name", region.getName());
        tm.out("Jurisdiction", region.getJurisdiction());
        assertNotNull("Failed to find the region associated with the current operational context", region);
    }

    @Test
    public void listRegions() throws CloudException, InternalException {
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        Iterable<Region> regions = services.listRegions();
        boolean found = false;
        int count = 0;

        assertNotNull("Null set of regions returned from listRegions()", regions);
        for( Region region : regions ) {
            count++;
            tm.out("Region", region);
            if( region.getProviderRegionId().equals(tm.getContext().getRegionId()) ) {
                found = true;
            }
        }
        tm.out("Total Region Count", count);
        assertTrue("There must be at least one region", count > 0);
        assertTrue("Did not find the context region ID among returned regions", found);
    }

    @Test
    public void getBogusDataCenter() throws CloudException, InternalException {
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        DataCenter dc = services.getDataCenter(UUID.randomUUID().toString());

        tm.out("Bogus Data Center", dc);
        assertNull("Dummy data center must be null, but one was found", dc);
    }

    @Test
    public void getDataCenter() throws CloudException, InternalException {
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        DataCenter dc = services.getDataCenter(testDataCenterId);

        tm.out("Data Center", dc);
        assertNotNull("Failed to find the test data center", dc);
    }

    @Test
    public void dataCenterContent() throws CloudException, InternalException {
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        DataCenter dc = services.getDataCenter(testDataCenterId);

        tm.out("Data Center ID", dc.getProviderDataCenterId());
        tm.out("Active", dc.isActive());
        tm.out("Available", dc.isAvailable());
        tm.out("Name", dc.getName());
        tm.out("Region ID", dc.getRegionId());
        assertNotNull("Failed to find the test data center", dc);
    }

    @Test
    public void listDataCenters() throws CloudException, InternalException {
        DataCenterServices services = tm.getProvider().getDataCenterServices();
        Iterable<DataCenter> dataCenters = services.listDataCenters(tm.getContext().getRegionId());
        boolean found = false;
        int count = 0;

        assertNotNull("Null set of data centers returned from listDataCenters()", dataCenters);
        for( DataCenter dc : dataCenters ) {
            count++;
            tm.out("Data Center", dc);
            if( dc.getProviderDataCenterId().equals(testDataCenterId) ) {
                found = true;
            }
        }
        tm.out("Total Data Center Count", count);
        assertTrue("There must be at least one data center in this region", count > 0);
        assertTrue("Did not find the test data center ID among returned data centers", found);
    }

    @Test
    public void regionIntegrity() throws CloudException, InternalException {
        DataCenterServices services = tm.getProvider().getDataCenterServices();

        for( Region region : services.listRegions() ) {
            if( region.isActive() ) {
                int count = 0;

                for( DataCenter dc : services.listDataCenters(region.getProviderRegionId()) ) {
                    if( dc.isActive() ) {
                        count++;
                    }
                }
                tm.out("Data Centers in " + region, count);
                assertTrue("An active region must have at least one active data center; " + region.getProviderRegionId() + " has none", count > 0);
            }
        }
    }
}
