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
 * 定义缓存被移除的原因枚举。
 *
 * @author Fuchun
 * @since 1.0
 */
public enum RemovalCause {

    /**
     * 表示缓存中的实体是被用户手动调用以下方法移除的： {@link Cache#remove(Object)},
     * {@link Cache#removeAll(Object...)}, {@link Cache#clear()}。
     */
    EXPLICIT {
        @Override
        public boolean wasEvicted() {
            return false;
        }
    },

    /**
     * 表示缓存中的实体是被用户手动调用以下方法移除的： {@link Cache#put(Object, Object)}、
     * {@link Cache#put(Object, Object, int)} 或 {@link Cache#putIfAbsent(Object, Object)}。
     */
    REPLACED {
        @Override
        public boolean wasEvicted() {
            return false;
        }
    },

    /**
     * The entry's expiration timestamp has passed. This can occur when using
     * {@link CacheBuilder#expireAfterWrite} or {@link CacheBuilder#expireAfterAccess}.
     */
    EXPIRED {
        @Override
        public boolean wasEvicted() {
            return true;
        }
    },

    /**
     * The entry was evicted due to size constraints. This can occur when using
     * {@link CacheBuilder#maximumSize} or {@link CacheBuilder#maximumWeight}.
     */
    SIZE {
        @Override
        public boolean wasEvicted() {
            return true;
        }
    };

    /**
     * Returns {@code true} if there was an automatic removal due to eviction (the cause
     * is neither {@link #EXPLICIT} nor {@link #REPLACED}).
     */
    public abstract boolean wasEvicted();
}
