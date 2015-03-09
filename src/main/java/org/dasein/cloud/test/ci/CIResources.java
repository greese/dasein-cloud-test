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

package org.dasein.cloud.test.ci;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.ci.CIServices;
import org.dasein.cloud.ci.ConvergedInfrastructure;
import org.dasein.cloud.ci.ConvergedInfrastructureSupport;
import org.dasein.cloud.ci.Topology;
import org.dasein.cloud.ci.TopologyState;
import org.dasein.cloud.ci.TopologySupport;
import org.dasein.cloud.test.DaseinTestManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 6/3/13 4:50 PM</p>
 *
 * @author George Reese
 */
public class CIResources {
    static private final Logger logger = Logger.getLogger(CIResources.class);

    private CloudProvider   provider;

    private final HashMap<String,String> testInfrastructures = new HashMap<String, String>();
    private final HashMap<String,String> testTopologies      = new HashMap<String, String>();

    public CIResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    public int close() {
        CIServices ciServices = provider.getCIServices();
        int count = 0;

        if( ciServices != null ) {
            ConvergedInfrastructureSupport ciSupport = ciServices.getConvergedInfrastructureSupport();

            if( ciSupport != null ) {
                for( Map.Entry<String,String> entry : testInfrastructures.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            ConvergedInfrastructure ci = ciSupport.getConvergedInfrastructure(entry.getValue());

                            if( ci != null ) {
                                ciSupport.terminate(entry.getValue(), null);
                                count++;
                            }
                            else {
                                count++;
                            }
                        }
                        catch( Throwable t ) {
                            logger.warn("Failed to de-provision test CI " + entry.getValue() + ": " + t.getMessage());
                        }
                    }
                }
            }

            TopologySupport tSupport = ciServices.getTopologySupport();

            if( tSupport != null ) {
                for( Map.Entry<String,String> entry : testTopologies.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            Topology t = tSupport.getTopology(entry.getValue());

                            if( t != null ) {
                                // TODO: implement this
                                // tSupport.remove(entry.getKey());
                                count++;
                            }
                            else {
                                count++;
                            }
                        }
                        catch( Throwable t ) {
                            logger.warn("Failed to de-provision test topology " + entry.getValue() + ": " + t.getMessage());
                        }
                    }
                }
            }
        }
        return count;
    }

    public @Nullable String getTestTopologyId(@Nonnull String label, boolean provisionIfNull) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testTopologies.entrySet() ) {
                if( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessTopology();
        }
        String id = testTopologies.get(label);

        if( id != null ) {
            return id;
        }
        if( !provisionIfNull ) {
            return null;
        }
        CIServices services = provider.getCIServices();

        if( services != null ) {
            TopologySupport support = services.getTopologySupport();

            if( support != null ) {
                try {
                    // TODO: when support for creating topologies is implemented, use this
                    return null;
                }
                catch( Throwable ignore ) {
                    return null;
                }
            }
        }
        return null;
    }

    private @Nullable String findStatelessTopology() {
        CIServices services = provider.getCIServices();

        if( services != null ) {
            TopologySupport support = services.getTopologySupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Topology defaultTopology = null;

                    for( Topology t : support.listTopologies(null) ) {
                        if( t.getCurrentState().equals(TopologyState.ACTIVE) ) {
                            defaultTopology = t;
                            break;
                        }
                        if( defaultTopology == null ) {
                            defaultTopology = t;
                        }
                    }
                    if( defaultTopology != null ) {
                        String id = defaultTopology.getProviderTopologyId();

                        testTopologies.put(DaseinTestManager.STATELESS, id);
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

    public int report() {
        boolean header = false;
        int count = 0;

        testInfrastructures.remove(DaseinTestManager.STATELESS);
        if( !testInfrastructures.isEmpty() ) {
            logger.info("Provisioned CI Resources:");
            header = true;
            count += testInfrastructures.size();
            DaseinTestManager.out(logger, null, "---> Infrastructures", testInfrastructures.size() + " " + testInfrastructures);
        }
        testTopologies.remove(DaseinTestManager.STATELESS);
        if( !testTopologies.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned CI Resources:");
            }
            count += testTopologies.size();
            DaseinTestManager.out(logger, null, "---> Topologies", testTopologies.size() + " " + testTopologies);
        }
        return count;
    }
}
