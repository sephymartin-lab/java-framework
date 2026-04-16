/*
 * Copyright 2022-2026 sephy.top
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.sephy.infra.utils;

import java.util.Collection;
import java.util.List;

public abstract class CollectionUtils {

    @SafeVarargs
    public static <T> List<T> mergeLists(List<T>... lists) {
        List<T> mergedList = new java.util.ArrayList<>();
        for (List<T> list : lists) {
            if (list != null && !list.isEmpty()) {
                mergedList.addAll(list);
            }
        }
        return mergedList;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return org.apache.commons.collections4.CollectionUtils.isEmpty(collection);
    }
}
