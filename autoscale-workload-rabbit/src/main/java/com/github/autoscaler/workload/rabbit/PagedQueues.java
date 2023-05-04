/*
 * Copyright 2015-2023 Open Text.
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
package com.github.autoscaler.workload.rabbit;

import com.google.common.base.MoreObjects;

public class PagedQueues
{
    private int page;
    private int page_count;
    private Item[] items;

    public int getPage()
    {
        return page;
    }

    public int getPageCount()
    {
        return page_count;
    }

    public Item[] getItems()
    {
        return items;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("page", page)
                .add("page_count", page_count)
                .add("items", items)
                .toString();
    }

    public static class Item
    {
        private String name;
        private int messages_ready;

        public String getName()
        {
            return name;
        }

        public int getMessagesReady()
        {
            return messages_ready;
        }

        @Override
        public String toString()
        {
            return MoreObjects.toStringHelper(this)
                    .add("name", name)
                    .add("messages_ready", messages_ready)
                    .toString();
        }
    }
}
