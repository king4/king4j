/*
 * Copyright (c) 2012-2013 King4j Team. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.king4j.cache;

/**
 * An object that can receive a notification when an entry is removed from a cache. The removal
 * resulting in notification could have occured to an entry being manually removed or replaced, or
 * due to eviction resulting from timed expiration, exceeding a maximum size, or garbage
 * collection.
 *
 * <p>An instance may be called concurrently by multiple threads to process different entries.
 * Implementations of this interface should avoid performing blocking calls or synchronizing on
 * shared resources.
 *
 * @param <K> the most general type of keys this listener can listen for; for
 *     example {@code Object} if any key is acceptable
 * @param <V> the most general type of values this listener can listen for; for
 *     example {@code Object} if any key is acceptable
 *
 * @author Fuchun
 * @since 1.0
 */
public interface RemovalListener<K, V> {

    /**
     * 缓存条目被移除时的通知方法。
     *
     * @param notification 移除时的通知对象。
     */
    public void onRemoval(RemovalNotification<K, V> notification);
}
