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

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Fuchun
 * @since 1.0
 */
public class CacheBuilder<K, V> {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 4;
    private static final int DEFAULT_EXPIRATION_NANOS = 0;
    private static final int DEFAULT_REFRESH_NANOS = 0;
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheBuilder.class.getName());
    public static final int NOSET_VAL = -1;

    enum NullListener implements RemovalListener<Object, Object> {
        INSTANCE;

        @Override
        public void onRemoval(RemovalNotification<Object, Object> notification) {
        }
    }

    private final String cacheName;
    int maximumSize = NOSET_VAL;
    int initialCapacity = NOSET_VAL;
    long expireAfterWriteNanos = NOSET_VAL;
    long expireAfterAccessNanos = NOSET_VAL;
    long refreshNanos = NOSET_VAL;

    List<RemovalListener> removalListeners = Lists.newArrayList();

    private CacheBuilder(String name) {
        this.cacheName = name;
    }

    String getCacheName() {
        return cacheName;
    }

    /**
     * 使用指定的缓存名称和默认的设置构造一个新的 {@link CacheBuilder} 实例。
     *
     * @param name 缓存名称。
     */
    public static CacheBuilder<Object, Object> newBuilder(String name) {
        return new CacheBuilder<Object, Object>(name);
    }

    /**
     * 设置内部 {@code hash} 存储的初始容量大小。
     *
     * @param initialCapacity 初始容量。
     * @return 当前缓存构建器实例。
     * @throws IllegalStateException 如果 {@code initial capacity} 已经被设置。
     * @throws IllegalArgumentException 如果 {@code initialCapacity <= 0}。
     */
    public CacheBuilder<K, V> initialCapacity(int initialCapacity) {
        checkState(this.initialCapacity == NOSET_VAL, "initial capacity was already set to %s",
                this.initialCapacity);
        checkArgument(initialCapacity >= 0);
        this.initialCapacity = initialCapacity;
        return this;
    }

    int getInitialCapacity() {
        return initialCapacity == NOSET_VAL ? DEFAULT_INITIAL_CAPACITY : initialCapacity;
    }

    int getMaximumSize() {
        return 0;
    }

    /**
     * 缓存条目被新创建，或者被其他的新值替换后，持续的时间{@code duration}之后，缓存条目将自动被移除。
     *
     * @param duration 自条目被写入缓存后的过期的时间值。
     * @param unit 自条目写入缓存后的过期的时间单位。
     * @return 当前缓存构建器实例。
     * @throws IllegalStateException 如果 {@code expireAfterWrite} 已经被设置。
     * @throws IllegalArgumentException 如果指定的时间值 {@code duration <= 0}。
     */
    public CacheBuilder<K, V> expireAfterWrite(long duration, TimeUnit unit) {
        checkState(expireAfterWriteNanos == NOSET_VAL, "expireAfterWrite was already set %s ns",
                expireAfterWriteNanos);
        checkArgument(duration >= 0, "The duration must not be negative: %s %s", duration, unit);
        this.expireAfterWriteNanos = unit.toNanos(duration);
        return this;
    }

    long getExpireAfterWriteNanos() {
        return expireAfterWriteNanos == NOSET_VAL ? DEFAULT_EXPIRATION_NANOS : expireAfterWriteNanos;
    }

    /**
     * 缓存条目被新创建，或者被其他的新值替换后，或者最后访问时间被更新，持续的时间{@code duration}之后，缓存条目将自动被移除。
     * <p/>
     * 操作缓存的所有读写操作，将更新缓存条目的最后访问时间。
     *
     * @param duration 自条目的访问时间被更新后的过期的时间值。
     * @param unit 自条目的访问时间被更新后的过期的时间单位。
     * @return 当前缓存构建器实例。
     * @throws IllegalStateException 如果 {@code expireAfterAccess} 已经被设置。
     * @throws IllegalArgumentException 如果指定的时间值 {@code duration <= 0}。
     */
    public CacheBuilder<K, V> expireAfterAccess(long duration, TimeUnit unit) {
        checkState(expireAfterAccessNanos == NOSET_VAL, "expireAfterAccess was already set %s ns",
                expireAfterAccessNanos);
        checkArgument(duration >= 0, "The duration must not be negative: %s %s", duration, unit);
        this.expireAfterAccessNanos = unit.toNanos(duration);
        return this;
    }

    long getExpireAfterAccessNanos() {
        return expireAfterAccessNanos == NOSET_VAL ? DEFAULT_EXPIRATION_NANOS : expireAfterAccessNanos;
    }

    long getRefreshNanos() {
        return refreshNanos == NOSET_VAL ? DEFAULT_REFRESH_NANOS : refreshNanos;
    }

    /**
     * 添加移除缓存动作监听器。
     *
     * @param listener 移除缓存监听器。
     * @param <K1> 缓存的键类型。
     * @param <V1> 缓存的值类型。
     * @return 当前缓存构建器实例。
     */
    public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> addRemovalListener(
            RemovalListener<? super K1, ? super V1> listener) {
        checkNotNull(listener, "Added RemovalListener instance must not be null.");
        CacheBuilder<K1, V1> me = (CacheBuilder<K1, V1>) this;
        if (me.removalListeners.contains(listener)) {
            LOGGER.debug("The RemovalListener {} already exists. Do nothing.", listener);
            return me;
        }
        me.removalListeners.add(listener);
        return me;
    }

    List<RemovalListener> getRemovalListeners() {
        return removalListeners;
    }
}
