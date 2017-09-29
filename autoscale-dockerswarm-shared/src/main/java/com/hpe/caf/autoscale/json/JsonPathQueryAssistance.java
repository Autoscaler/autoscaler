/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.autoscale.json;

import com.hpe.caf.autoscale.endpoint.docker.DockerSwarmService;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Group common and useful JsonPath query methods together here.
 */
public class JsonPathQueryAssistance
{
    /**
     *
     * @param serviceItem
     * @param requiredItem = 1 is first item, do not confuse this with index of item.
     * @param jsonQuery
     * @return
     * @throws Exception
     */
    public static String queryForValueAsString(DockerSwarmService serviceItem, final int requiredItem,
                                               final String jsonQuery) throws Exception
    {
        if (requiredItem < 1) {
            throw new InvalidParameterException("Required item is a 1 based index.  First item = 1.");
        }
        // now get the requestedString information
        LinkedList<Object> stringList = serviceItem.getDocumentContext().read(jsonQuery);

        Objects.requireNonNull(stringList, "No item found to match the query: " + jsonQuery);

        if (stringList.isEmpty() || (stringList.size() < requiredItem)) {
            // failed to get the requestedString information for this service, so we can't update it.
            throw new Exception(
                "Failed to find the requested element(s) using the json supplied, requestedItem: " + requiredItem
                + " items in list: " + stringList.size());
        }

        final String requestedValue = stringList.get(requiredItem - 1).toString();
        return requestedValue;
    }

    /**
     *
     * @param serviceItem
     * @param requiredItem = 1 is first item, do not confuse this with index of item.
     * @param jsonQuery
     * @return Integer representing the requested value.
     * @throws Exception
     */
    public static Integer queryForValueAsInteger(DockerSwarmService serviceItem, final int requiredItem,
                                               final String jsonQuery) throws Exception
    {
        if (requiredItem < 1) {
            throw new InvalidParameterException("Required item is a 1 based index.  First item = 1.");
        }
        // now get the requestedString information
        LinkedList<Integer> stringList = serviceItem.getDocumentContext().read(jsonQuery);

        Objects.requireNonNull(stringList, "No item found to match the query: " + jsonQuery);

        if (stringList.isEmpty() || (stringList.size() < requiredItem)) {
            // failed to get the requestedString information for this service, so we can't update it.
            throw new Exception(
                "Failed to find the requested element(s) using the json supplied, requestedItem: " + requiredItem
                + " items in list: " + stringList.size());
        }

        final Integer requestedValue = stringList.get(requiredItem - 1);
        return requestedValue;
    }
}
