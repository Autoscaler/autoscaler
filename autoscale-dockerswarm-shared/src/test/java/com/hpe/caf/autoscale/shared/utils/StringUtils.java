/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.autoscale.shared.utils;

/**
 * simple string utils 
 */
public class StringUtils
{
    private final static int INDEX_NOT_FOUND = -1;

    public static String difference(String str1, String str2)
    {
        if (str1 == null) {
            return str2;
        }
        if (str2 == null) {
            return str1;
        }

        int at = indexOfDifference(str1, str2);
        if (at == INDEX_NOT_FOUND) {
            return "";
        }
        return str2.substring(at);
    }

    public static int indexOfDifference(CharSequence cs1, CharSequence cs2)
    {
        if (cs1 == cs2) {
            return INDEX_NOT_FOUND;
        }
        if (cs1 == null || cs2 == null) {
            return 0;
        }
        int i;
        for (i = 0; i < cs1.length() && i < cs2.length(); ++i) {
            if (cs1.charAt(i) != cs2.charAt(i)) {
                break;
            }
        }
        if (i < cs2.length() || i < cs1.length()) {
            return i;
        }
        return INDEX_NOT_FOUND;
    }
}
