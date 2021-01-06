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
package com.hpe.caf.autoscale.reflection;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 *
 * A utility class to provide reflection assistance
 */
public class ReflectionAssistance
{

    /**
     * Assists in finding out the method information of the calling object for use with reflection.
     * This works out the calling method information, equivilent to object.getClass, but there is no object.getMethod. 
     *
     * @param classToSearch
     * @param parameterTypes
     * @return
     */
    public static Method getCallingMethod(final Class<?> classToSearch, Class<?>... parameterTypes)
    {
        // we want the method name of the caller of this method
        final String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();

        Method methodToSearch = null;
        try {
            methodToSearch = classToSearch.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {

            // just before we give up, I would love for this to be easier to code up, i.e. without have
            // to define the params in the method signature and again in the getcallingmethod call, so fallback
            // to a unique method name search, of course if there are 2 methods with same name, but with different signatures then
            // they should have passed the correct param types!
            Method[] methods = classToSearch.getMethods();
            ArrayList<Method> listOfFoundsMethods = new ArrayList<>();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    // found a match, add and continue on.
                    listOfFoundsMethods.add(method);
                }
            }

            if (listOfFoundsMethods.size() == 1) {
                // found a single match, so great use this.
                return listOfFoundsMethods.get(0);
            }

            if (listOfFoundsMethods.size() > 1) {
                throw new RuntimeException(
                    "Found more than one method that matches the specified name: " + methodName + ", \r\n"
                    + "ensure you have supplied the correct parameter types for your method signature to keep it unique.",
                    ex);
            }

            throw new RuntimeException(
                "No such method found with name: " + methodName + ", \r\n"
                + "ensure you have supplied the correct parameter types for your method signature.", ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }

        return methodToSearch;
    }
}
