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
import org.dasein.cloud.platform.CDNSupport;
import org.dasein.cloud.platform.Distribution;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.storage.Blob;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.CalendarWrapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/28/13 2:57 PM</p>
 *
 * @author George Reese
 */
public class StatefulCDNTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulCDNTests.class);
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
    private Blob   testOrigin;

    public StatefulCDNTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        if( name.getMethodName().equals("removeDistribution") ) {
            testOrigin = tm.getTestBucket(name.getMethodName(), true, true);
            if( testOrigin != null ) {
                testDistributionId = tm.getTestDistributionId(DaseinTestManager.REMOVED, true, testOrigin.getBucketName());
            }
        }
        else if( name.getMethodName().equals("createDistribution") ) {
            testOrigin = tm.getTestBucket(name.getMethodName(), true, true);
        }
        else {
            testOrigin = tm.getTestBucket("statefulCDN", true, true);
            if( testOrigin != null ) {
                testDistributionId = tm.getTestDistributionId(DaseinTestManager.STATEFUL, true, testOrigin.getBucketName());
            }
        }
    }

    @After
    public void after() {
        try {
            testDistributionId = null;
            testOrigin = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void createDistribution() throws CloudException, InternalException {
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
        if( testOrigin != null ) {
            PlatformResources r = DaseinTestManager.getPlatformResources();

            if( r != null ) {
                String id = r.provisionDistribution(support, "provision", "dsncdn", testOrigin.getBucketName());

                tm.out("Created", id);
                assertNotNull("No ID was provided from the creation of a distribution from " + testOrigin, id);

                Distribution d = support.getDistribution(id);
                assertNotNull("No distribution can be found under the new ID " + id, d);
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("CDN support is not subscribed so this test is not valid");
            }
            else {
                fail("No test origin is in place so this test cannot be run");
            }
        }
    }

    @Test
    public void changeName() throws CloudException, InternalException {
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
            String newName = "Dasein New " + System.currentTimeMillis();

            assertNotNull("No test distribution exists for the ID " + testDistributionId, d);
            tm.out("Before", d.getName());
            support.update(testDistributionId, newName, d.isActive(), d.getAliases());

            Distribution updated = support.getDistribution(testDistributionId);

            assertNotNull("The distribution disappeared after update", updated);
            tm.out("After", updated.getName());
            /*
            assertEquals("The new distribution name is not the same as the one requested", newName, updated.getName());
            */
            assertEquals("The distribution active state incorrectly changed after the update", d.isActive(), updated.isActive());
            TreeSet<String> old = new TreeSet<String>();
            TreeSet<String> na = new TreeSet<String>();

            Collections.addAll(old, d.getAliases());
            Collections.addAll(na, updated.getAliases());

            assertEquals("The old aliases and new aliases have differing sizes", old.size(), na.size());
            Iterator<String> oit = old.iterator();
            Iterator<String> nit = na.iterator();

            while( oit.hasNext() ) {
                assertEquals("The old aliases and new aliases no longer match", oit.next(), nit.next());
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("CDN support is not subscribed so this test is not valid");
            }
            else {
                fail("No test distribution is in place so this test cannot be run");
            }
        }
    }

    @Test
    public void changeAliases() throws CloudException, InternalException {
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

            assertNotNull("No test distribution exists for the ID " + testDistributionId, d);
            String[] oldAliases = d.getAliases();

            String[] newAliases = (oldAliases.length < 1 ? new String[] { "sp" + System.currentTimeMillis() + ".dasein.org" } :  new String[] { d.getAliases()[0], "sp" + System.currentTimeMillis() + ".dasein.org" });

            tm.out("Before", Arrays.toString(d.getAliases()));
            //noinspection ConstantConditions
            support.update(testDistributionId, d.getName(), d.isActive(), newAliases);

            Distribution updated = support.getDistribution(testDistributionId);

            assertNotNull("The distribution disappeared after update", updated);
            tm.out("After", Arrays.toString(updated.getAliases()));

            /*
            TreeSet<String> expected = new TreeSet<String>();
            TreeSet<String> actual = new TreeSet<String>();

            Collections.addAll(expected, newAliases);
            Collections.addAll(actual, updated.getAliases());

            assertEquals("The new aliases and expected aliases have differing sizes", expected.size(), actual.size());
            Iterator<String> eit = expected.iterator();
            Iterator<String> ait = actual.iterator();

            while( eit.hasNext() ) {
                assertEquals("The new aliases and the expected aliases do not longer match", eit.next(), ait.next());
            }
            */
            assertEquals("The distribution name changed incorrectly after the update", d.getName(), updated.getName());
            assertEquals("The distribution active state incorrectly changed after the update", d.isActive(), updated.isActive());
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("CDN support is not subscribed so this test is not valid");
            }
            else {
                fail("No test distribution is in place so this test cannot be run");
            }
        }
    }

    @Test
    public void deactivate() throws CloudException, InternalException {
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

            assertNotNull("No test distribution exists for the ID " + testDistributionId, d);
            assertTrue("The test distribution is not active, cannot activate it", d.isActive());

            tm.out("Before", d.isActive());
            //noinspection ConstantConditions
            support.update(testDistributionId, d.getName(), false, d.getAliases());

            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 5L);
            Distribution updated = null;

            while( System.currentTimeMillis() < timeout ) {
                try {
                    updated = support.getDistribution(testDistributionId);
                    if( updated == null || !updated.isActive() ) {
                        break;
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
            }
            assertNotNull("The distribution disappeared after update", updated);
            tm.out("After", updated.isActive());
            assertEquals("The distribution active state failed to change after deactivation", false, updated.isActive());

            assertEquals("The distribution name changed incorrectly after the update", d.getName(), updated.getName());
            TreeSet<String> old = new TreeSet<String>();
            TreeSet<String> na = new TreeSet<String>();

            Collections.addAll(old, d.getAliases());
            Collections.addAll(na, updated.getAliases());

            assertEquals("The old aliases and new aliases have differing sizes", old.size(), na.size());
            Iterator<String> oit = old.iterator();
            Iterator<String> nit = na.iterator();

            while( oit.hasNext() ) {
                assertEquals("The old aliases and new aliases no longer match", oit.next(), nit.next());
            }
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("CDN support is not subscribed so this test is not valid");
            }
            else {
                fail("No test distribution is in place so this test cannot be run");
            }
        }
    }

    @Test
    public void removeDistribution() throws CloudException, InternalException {
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

            tm.out("Before", d);
            assertNotNull("No such test distribution: " + testDistributionId, d);
            support.delete(testDistributionId);
            d = support.getDistribution(testDistributionId);
            tm.out("After", d);
            assertTrue("The test distribution still exists post-deletion", d == null || (!d.isActive() && !d.isDeployed()));
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("CDN support is not subscribed so this test is not valid");
            }
            else {
                fail("No test distribution is in place so this test cannot be run");
            }
        }
    }
}
