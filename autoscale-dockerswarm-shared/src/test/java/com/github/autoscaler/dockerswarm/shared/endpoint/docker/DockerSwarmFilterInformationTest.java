/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.autoscaler.dockerswarm.shared.endpoint.docker;

import static com.github.autoscaler.dockerswarm.shared.endpoint.docker.DockerSwarmFilters.buildServiceFilter;
import com.github.autoscaler.dockerswarm.shared.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * Unit testing for dockerswarmfilterinformation and dockerswarmfilters classes.
 */
public class DockerSwarmFilterInformationTest
{

    @Test
    public void testBuildServiceFilter()
    {
        final String myServiceStackName = "testMeStack";
        
        final String filterByLabel = buildServiceFilter(DockerSwarmFilters.ServiceFilterByType.LABEL,
                                                        DockerSwarmFilters.FilterLabelKeys.DOCKER_STACK, myServiceStackName);
        
        final String expectedString = "{\"label\":{\"com.docker.stack.namespace=" + myServiceStackName + "\":true}}";
        
        compareStrings(expectedString, filterByLabel, "Should match already built filter");
    }
    
    @Test
    public void testBuildTaskFilters()
    {
        final String myServiceStackName = "testMeStack";
        final String myServiceId = "testMyServiceID1";
        List<DockerSwarmFilterInformation> filterInfoList = new ArrayList<>();
        filterInfoList.add(new DockerSwarmFilterInformation( DockerSwarmFilters.TaskFilters.LABEL,
                                                        DockerSwarmFilters.FilterLabelKeys.DOCKER_STACK, myServiceStackName));
        filterInfoList.add(new DockerSwarmFilterInformation( DockerSwarmFilters.TaskFilters.DESIRED_STATE, DockerSwarmFilters.TaskState.RUNNING.toString()));
        filterInfoList.add(new DockerSwarmFilterInformation( DockerSwarmFilters.TaskFilters.SERVICE, myServiceId));
        
        final String filterByLabel = DockerSwarmFilters.buildFilterInformation(filterInfoList);
        
        final String expectedString = "{\"label\":{\"com.docker.stack.namespace=" + myServiceStackName + "\":true}," + 
            "\"desired-state\":{\"running\":true}," + 
            "\"service\":{\"" + myServiceId + "\":true}" + 
            "}";
        
        compareStrings(expectedString, filterByLabel, "Should match already built filter");
    }

    private void compareStrings(final String expectedString, final String actualString, final String assertMessage)
    {
        if ( !expectedString.equals(actualString) )
        {
            String difference =  StringUtils.difference( expectedString, actualString );
            System.out.println("Difference in strings is: " + difference);
        }
        
        Assert.assertEquals(assertMessage, expectedString, actualString);
    }

}
