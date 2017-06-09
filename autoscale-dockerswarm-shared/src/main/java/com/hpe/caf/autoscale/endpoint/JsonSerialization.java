/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.autoscale.endpoint;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Assist with serialization of json objects from and to strings.
 */
class JsonSerialization
{
    private static Configuration conf = null;

    static {
        Configuration.setDefaults(new Configuration.Defaults()
        {
            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider()
            {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider()
            {
                return mappingProvider;
            }

            @Override
            public Set<Option> options()
            {
                return EnumSet.noneOf(Option.class);
            }
        });

        conf = Configuration.defaultConfiguration();

        // suppress exceptions, and return null if no leaf with particular property
        conf.addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
        // suppress exceptions, and return blank list if a definite query node doesn't contain a node.
        conf.addOptions(Option.ALWAYS_RETURN_LIST);
    }

    public static <T> T readValue(byte[] bytes, Class<T> type) throws IOException
    {
        DocumentContext document = JsonPath.using(conf).parse(bytes);
        if (type.isInstance(DocumentContext.class)) {
            return (T) document;
        }

        return document.json();
    }

    public static <T> T readValue(String bytes, Class<T> type) throws IOException
    {
        DocumentContext document = JsonPath.using(conf).parse(bytes);

        // does called want the actual full document contxt for querying.
        if (type.isInstance(document)) {
            return (T) document;
        }

        // otherwise try the json representation is it of this required type.
        if (type.isInstance(document.json())) {
            return (T) document.json();
        }

        // try to coerce the type     
        return (T) document.json();
    }

    public static <T> T readValue(InputStream bytes, Class<T> type) throws IOException
    {
        DocumentContext document = JsonPath.using(conf).parse(bytes);

        if (type.isInstance(DocumentContext.class)) {
            return (T) document;
        }

        return document.json();
    }

}
