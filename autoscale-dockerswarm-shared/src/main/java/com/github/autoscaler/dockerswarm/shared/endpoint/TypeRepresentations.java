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
package com.github.autoscaler.dockerswarm.shared.endpoint;

import java.util.ArrayList;

/**
 * list of types used to represent the objects returned by our HTTP endpoint queries. Most of these will be representations of objects in
 * a generic fashion to be queried using JSONPath instead of using a fixed object model. They are grouped and defined here to allow an
 * object representation to be easily changed in lots of places easily.
 */
public class TypeRepresentations
{
    /**
     * List of Objects, where the caller isn't aware of the type, or doesn't care at this point.
     */
    public static class ObjectList extends ArrayList<Object>
    {
    }

}
