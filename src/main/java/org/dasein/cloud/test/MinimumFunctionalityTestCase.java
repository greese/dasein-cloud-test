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

import java.io.IOException;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MinimumFunctionalityTestCase extends BaseTestCase {
    private CloudProvider provider = null;
    
    public MinimumFunctionalityTestCase(String name) { super(name); }
    
    @Before
    @Override
    public void setUp() throws CloudException, InternalException, InstantiationException, IllegalAccessException, IOException {
        provider = getProvider();
        provider.connect(getTestContext());
    }
    
    @After
    @Override
    public void tearDown() {
        if( provider != null ) {
            provider.close();
        }
    }

    @Test
    public void testConfiguration() {
        assertNotNull("Cloud name must be specified", provider.getCloudName());
        assertNotNull("Provider name must be specified", provider.getProviderName());
    }
    
    @Test
    public void testDataCenterService() {
        assertNotNull("All Dasein Cloud implementations must support data center services", provider.getDataCenterServices());
    }
}
