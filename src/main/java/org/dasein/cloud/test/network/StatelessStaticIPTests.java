package org.dasein.cloud.test.network;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Locale;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/24/13 5:10 PM</p>
 *
 * @author George Reese
 */
public class StatelessStaticIPTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessStaticIPTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    public StatelessStaticIPTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        IpAddressSupport support = services.getIpAddressSupport();

        if( support == null ) {
            tm.ok("Static IP addresses are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        tm.out("Subscribed", support.isSubscribed());
        tm.out("Term for Static IP", support.getProviderTermForIpAddress(Locale.getDefault()));
        tm.out("Specify VLAN for VLAN IP", support.identifyVlanForVlanIPRequirement());

        Iterable<IPVersion> versions = support.listSupportedIPVersions();

        tm.out("IP Versions", versions);

        for( IPVersion v : IPVersion.values() ) {
            tm.out("Requestable [" + v + "]", support.isRequestable(v));
            tm.out("Assignable [" + v + "]", support.isAssigned(v));
            tm.out("Assignable Post-launch [" + v + "]", support.isAssignablePostLaunch(v));
            tm.out("Forwarding [" + v + "]", support.isForwarding(v));
            tm.out("VLAN Addresses [" + v + "]", support.supportsVLANAddresses(v));
        }
        assertNotNull("IP versions may not be null", versions);
        assertTrue("Static IP address support must provide support for at least one IP version", versions.iterator().hasNext());
    }

    @Test
    public void getBogusAddress() throws CloudException, InternalException {

    }

    @Test
    public void getAddress() throws CloudException, InternalException {

    }

    @Test
    public void ipv4Content() throws CloudException, InternalException {

    }

    @Test
    public void ipv6Content() throws CloudException, InternalException {

    }

    @Test
    public void listAllIPv4() throws CloudException, InternalException {

    }

    @Test
    public void listAllIPv6() throws CloudException, InternalException {

    }

    @Test
    public void listAvailableIPv4() throws CloudException, InternalException {

    }

    @Test
    public void listAvailableIPv6() throws CloudException, InternalException {

    }

    @Test
    public void listIPv4Status() throws CloudException, InternalException {

    }

    @Test
    public void listIPv6Status() throws CloudException, InternalException {

    }

    @Test
    public void compareIPv4ListAndStatus() throws CloudException, InternalException {

    }

    @Test
    public void compareIPv6ListAndStatus() throws CloudException, InternalException {

    }
}
