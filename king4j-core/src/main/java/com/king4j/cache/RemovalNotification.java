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

import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.Map;

/**
 * 缓存条目被移除时的通知。包含了缓存的键、值和被移除的原因({@link RemovalCause})。
 *
 * @author Fuchun
 * @since 1.0
 */
public class RemovalNotification<K, V> implements Map.Entry<K, V>, Serializable {
    private static final long serialVersionUID = 1L;

    private final K key;
    private final V value;
    private final RemovalCause cause;

    protected RemovalNotification(K key, V value, RemovalCause cause) {
        this.key = key;
        this.value = value;
        this.cause = cause;
    }

    public RemovalCause getCause() {
        return cause;
    }

    /**
     * @see java.util.Map.Entry#getKey()
     */
    @Override
    public K getKey() {
        return key;
    }

    /**
     * @see java.util.Map.Entry#getValue()
     */
    @Override
    public V getValue() {
        return value;
    }

    /**
     * @see java.util.Map.Entry#setValue(java.lang.Object)
     */
    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        K k = getKey();
        V v = getValue();
        return ((k == null) ? 0 : k.hashCode()) ^ ((v == null) ? 0 : v.hashCode());
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof Map.Entry) {
            Map.Entry<?, ?> that = (Map.Entry<?, ?>) object;
            return Objects.equal(this.getKey(), that.getKey())
                    && Objects.equal(this.getValue(), that.getValue());
        }
        return false;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getKey() + "=" + getValue();
    }
}
