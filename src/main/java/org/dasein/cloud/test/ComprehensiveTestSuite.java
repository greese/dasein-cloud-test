/**
 * Copyright (C) 2009-2012 enStratus Networks Inc
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

package org.dasein.cloud.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import junit.framework.TestSuite;

import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.identity.IdentityServices;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.platform.PlatformServices;
import org.dasein.cloud.storage.StorageServices;
import org.junit.Test;

public class ComprehensiveTestSuite extends TestSuite {
    static protected Class<? extends CloudProvider> providerClass;
    
    String[] exclusions = null, inclusions = null;
    
    public ComprehensiveTestSuite(Class<? extends CloudProvider> classOfProvider) throws TestConfigurationException {
        super();
        try {
            String exc = System.getProperty("dasein.exclusions");
            String inc = System.getProperty("dasein.inclusions");
            
            if( exc != null ) {
                if( exc.contentEquals(",") ) {
                    exclusions = exc.split(",");
                }
                else {
                    exclusions = new String[] { exc };
                }
            }
            if( inc != null ) {
                if( inc.contentEquals(",") ) {
                    inclusions = inc.split(",");
                }
                else {
                    inclusions = new String[] { inc };
                }
            }
            System.out.println("Inclusions=" + Arrays.toString(inclusions));
            System.out.println("Exclusions=" + Arrays.toString(exclusions));
            providerClass = classOfProvider;
            addTests(MinimumFunctionalityTestCase.class);
                        
            CloudProvider provider = providerClass.newInstance();
            
            provider.connect(BaseTestCase.getTestContext(providerClass));

            if( provider.hasIdentityServices() ) {
                IdentityServices identity = provider.getIdentityServices();
                
                if( identity.hasIdentityAndAccessSupport() ) {
                    addTests(IAMTestCase.class);
                }
            }
            if( provider.hasPlatformServices() ) {
                PlatformServices platform = provider.getPlatformServices();
                
                if( platform.hasCDNSupport() ) {
                    addTests(CDNTestCase.class);
                }
                if( platform.hasRelationalDatabaseSupport() ) {
                    addTests(RelationalDatabaseTestCase.class);
                }
            }
            if( provider.hasStorageServices() ) {
                StorageServices storage = provider.getStorageServices();
                
                if( storage.hasBlobStoreSupport() ) {
                    addTests(BlobStoreTestCase.class);
                }
            }
            if( provider.hasNetworkServices() ) {
                NetworkServices network = provider.getNetworkServices();
                
                if( network.hasFirewallSupport() ) {
                    addTests(FirewallTestCase.class);
                }
                if( network.hasLoadBalancerSupport() ) {
                    addTests(LoadBalancerTestCase.class);
                }
                if( network.hasDnsSupport() ) {
                    addTests(DNSTestCase.class);
                }
                if( network.hasIpAddressSupport() ) {
                    addTests(IpAddressTestCase.class);
                }
            }
            if( provider.hasComputeServices() ) {
                ComputeServices compute = provider.getComputeServices();

                if( compute.hasSnapshotSupport() ) {
                    addTests(SnapshotTestCase.class);
                }                
            }
        }
        catch( Throwable t ) {
            throw new TestConfigurationException(t);
        }
    }
    
    private void addTests(Class<? extends BaseTestCase> testClass) throws SecurityException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InstantiationException, InvocationTargetException {
        String cname = testClass.getSimpleName();
        
        if( inclusions != null ) {
            boolean included = false;
            
            for( String inc : inclusions ) {
                if( inc.equalsIgnoreCase(cname) ) {
                    included = true;
                }
            }
            if( !included ) {
                return;
            }
        }
        else if( exclusions != null ) {
            for( String exc : exclusions ) {
                if( exc.equalsIgnoreCase(cname) ) {
                    return;
                }
            }
        }
        System.out.println("TEST " + testClass.getName());
        Constructor<? extends BaseTestCase> c = testClass.getConstructor(String.class);
        
        if( c == null ) {
            throw new IllegalAccessException("No single argument string constructor for test class");
        }
        for( Method method : testClass.getDeclaredMethods() ) {
            for( Annotation annotation : method.getAnnotations() ) {
                if( annotation.annotationType().equals(Test.class) ) {
                    BaseTestCase t = c.newInstance(method.getName());

                    BaseTestCase.addExpectedVmReuses(t.getVmReuseCount());
                    BaseTestCase.addExpectedImageReuses(t.getImageReuseCount());
                    BaseTestCase.addExpectedFirewallReuses(t.getFirewallReuseCount());
                    BaseTestCase.addExpectedVolumeReuses(t.getVolumeReuseCount());
                    BaseTestCase.addExpectedVlanReuses(t.getVlanReuseCount());
                    addTest(t);
                }
            }
        }
    }
}
