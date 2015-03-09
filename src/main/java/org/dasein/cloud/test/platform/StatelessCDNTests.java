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

package org.dasein.cloud.test.platform;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.platform.CDNSupport;
import org.dasein.cloud.platform.Distribution;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/28/13 2:57 PM</p>
 *
 * @author George Reese
 */
public class StatelessCDNTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessCDNTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testDistributionId;

    public StatelessCDNTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        testDistributionId = tm.getTestDistributionId(DaseinTestManager.STATELESS, false, null);
    }

    @After
    public void after() {
        tm.end();
    }

    private void assertDistribution(@Nonnull Distribution distribution) throws CloudException, InternalException {
        assertNotNull("The distribution ID may not be null", distribution.getProviderDistributionId());
        assertNotNull("The distribution name may not be null", distribution.getName());
        assertNotNull("The distribution's owner account may not be null", distribution.getProviderOwnerId());
        assertNotNull("The distribution DNS name may not be null", distribution.getDnsName());
        assertNotNull("The distribution location may not be null", distribution.getLocation());
    }

    @Test
    public void checkMetaData() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        CDNSupport support = services.getCDNSupport();

        if( support == null ) {
            tm.ok("CDN is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        tm.out("Subscribed", support.isSubscribed());
        tm.out("Term for CDN Distribution", support.getProviderTermForDistribution(Locale.getDefault()));
    }

    @Test
    public void getBogusDistribution() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        CDNSupport support = services.getCDNSupport();

        if( support == null ) {
            tm.ok("CDN is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Distribution d = support.getDistribution(UUID.randomUUID().toString());

        tm.out("Bogus Distribution", d);
        assertNull("Found a matching distribution for the randomly generated distribution ID", d);
    }

    @Test
    public void getDistribution() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        CDNSupport support = services.getCDNSupport();

        if( support == null ) {
            tm.ok("CDN is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testDistributionId != null ) {
            Distribution d = support.getDistribution(testDistributionId);

            tm.out("Distribution", d);
            assertNotNull("No distribution was found matching the test ID " + testDistributionId, d);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to CDN support so this test is invalid");
            }
            else {
                fail("No test distribution was found to support this stateless test. Please create one and run again.");
            }
        }
    }

    @Test
    public void distributionContent() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        CDNSupport support = services.getCDNSupport();

        if( support == null ) {
            tm.ok("CDN is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testDistributionId != null ) {
            Distribution d = support.getDistribution(testDistributionId);

            assertNotNull("No distribution was found matching the test ID " + testDistributionId, d);
            tm.out("Distribution ID", d.getProviderDistributionId());
            tm.out("Active", d.isActive());
            tm.out("Deployed", d.isDeployed());
            tm.out("Name", d.getName());
            tm.out("Owner Account", d.getProviderOwnerId());
            tm.out("DNS Name", d.getDnsName());
            tm.out("Aliases", Arrays.toString(d.getAliases()));
            tm.out("Location", d.getLocation());
            tm.out("Log Directory", d.getLogDirectory());
            tm.out("Log Name", d.getLogName());
            assertDistribution(d);
            assertEquals("The distribution ID from the result does not match the request", testDistributionId, d.getProviderDistributionId());
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to CDN support so this test is invalid");
            }
            else {
                fail("No test distribution was found to support this stateless test. Please create one and run again.");
            }
        }
    }

    @Test
    public void listDistributions() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        CDNSupport support = services.getCDNSupport();

        if( support == null ) {
            tm.ok("CDN is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Iterable<Distribution> dists = support.list();
        int count = 0;

        assertNotNull("The list of distributions must be non-null, even if not supported or subscribed", dists);
        for( Distribution d : dists ) {
            count++;
            tm.out("Distribution", d);
        }
        tm.out("Total Distribution Count", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
                tm.ok("No distribution subscription, so this test is not valid");
            }
            else {
                tm.warn("No distributions were returned, so this test is not valid");
            }
        }
        else if( !support.isSubscribed() ) {
            fail("Distributions were returned for an account without a distribution subscription");
        }
        for( Distribution d : dists ) {
            assertDistribution(d);
        }
    }

    @Test
    public void listDistributionStatus() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        CDNSupport support = services.getCDNSupport();

        if( support == null ) {
            tm.ok("CDN is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        Iterable<ResourceStatus> dists = support.listDistributionStatus();
        int count = 0;

        assertNotNull("The list of distribution status objects must be non-null, even if not supported or subscribed", dists);
        for( ResourceStatus d : dists ) {
            count++;
            tm.out("Distribution Status", d);
        }
        tm.out("Total Distribution Status Count", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
                tm.ok("No distribution subscription, so this test is not valid");
            }
            else {
                tm.warn("No distribution status was returned, so this test is not valid");
            }
        }
        else if( !support.isSubscribed() ) {
            fail("Distribution status objects were returned for an account without a distribution subscription");
        }
    }

    @Test
    public void compareDistributionListAndStatus() throws CloudException, InternalException {
        PlatformServices services = tm.getProvider().getPlatformServices();

        if( services == null ) {
            tm.ok("Platform services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        CDNSupport support = services.getCDNSupport();

        if( support == null ) {
            tm.ok("CDN is not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        HashMap<String,Map<String,Boolean>> map = new HashMap<String, Map<String, Boolean>>();
        Iterable<Distribution> distributions = support.list();
        Iterable<ResourceStatus> status = support.listDistributionStatus();

        assertNotNull("list() must return at least an empty collections and may not be null", distributions);
        assertNotNull("listDistributionStatus() must return at least an empty collection and may not be null", status);
        for( ResourceStatus s : status ) {
            Map<String,Boolean> current = map.get(s.getProviderResourceId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(s.getProviderResourceId(), current);
            }
            current.put("status", true);
        }
        for( Distribution d : distributions ) {
            Map<String,Boolean> current = map.get(d.getProviderDistributionId());

            if( current == null ) {
                current = new HashMap<String, Boolean>();
                map.put(d.getProviderDistributionId(), current);
            }
            current.put("distribution", true);
        }
        for( Map.Entry<String,Map<String,Boolean>> entry : map.entrySet() ) {
            Boolean s = entry.getValue().get("status");
            Boolean d = entry.getValue().get("distribution");

            assertTrue("Status and distribution lists do not match for " + entry.getKey(), s != null && d != null && s && d);
        }
        tm.out("Matches");
    }
}
