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
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Fuchun
 * @since 1.0
 */
public class DefaultCache<K, V> implements Cache<K, V> {

    /**
     * 日志记录器。
     */
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    /**
     * 本地缓存可存入的对象的最大数量的默认值。
     */
    public static final int DEFAULT_MAXIMUM_SIZE = 10000;
    /**
     * 本地缓存自对象写入缓存后的默认的过期时间（秒）。
     */
    public static final int DEFAULT_EXPIRE_AFTER_WRITE = 300;
    /**
     * 本地缓存自对象被访问后的默认的过期时间（秒）。
     */
    public static final int DEFAULT_EXPIRE_AFTER_ACCESS = -1;
    /**
     * 本地缓存的默认初始容量。
     */
    public static final int DEFAULT_INITIAL_CAPACITY = 100;

    private final List<RemovalListener<K, V>> removalListeners = Lists.newArrayList();
    private final Monitor cacheMonitor = new Monitor();
    private DelayQueue<DelayElement<Entry<K, V>>> queue = new DelayQueue<DelayElement<Entry<K, V>>>();

    private ConcurrentMap<K, V> cacheMap;

    /**
     * 本地缓存可容纳对象的最大数量。
     */
    private int maximumSize = DEFAULT_MAXIMUM_SIZE;
    /**
     * 本地缓存自写入后的过期时间（秒）。
     */
    private int expireAfterWrite = DEFAULT_EXPIRE_AFTER_WRITE;
    /**
     * 本地缓存自访问后的过期时间（秒）。
     */
    private int expireAfterAccess = DEFAULT_EXPIRE_AFTER_ACCESS;
    /**
     * 本地缓存的初始容量。
     */
    private int initialCapacity = DEFAULT_INITIAL_CAPACITY;

    private long expireAfterAccessNanos;
    private long expireAfterWriteNanos;

    private final String cacheName;
    private DaemonRunnable daemonRunnable;
    private String threadName;
    private boolean isRunning = false;

    public DefaultCache(String cacheName) {
        this.cacheName = cacheName;
    }

    public DefaultCache(CacheBuilder<K, V> builder) {
        cacheName = builder.getCacheName();
        initialCapacity = builder.getInitialCapacity();
        if (CacheBuilder.NOSET_VAL != builder.getMaximumSize()) {
            maximumSize = builder.getMaximumSize();
        }
        expireAfterAccessNanos = builder.getExpireAfterAccessNanos();
        expireAfterWriteNanos = builder.getExpireAfterWriteNanos();

        if (builder.getRemovalListeners() != null) {
            for (RemovalListener<K, V> listener : builder.getRemovalListeners()) {
                removalListeners.add(listener);
            }
        }
    }

    /**
     * 启动本地缓存。
     */
    public void start() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        cacheMap = new ConcurrentHashMap<K, V>(initialCapacity);
        daemonRunnable = new DaemonRunnable();
        Thread cacheThread = new Thread(daemonRunnable);
        threadName = cacheName == null ? "LocalCache" : String.format("%s_LocalCache", cacheName);
        cacheThread.setName(threadName);
        cacheThread.setDaemon(true);
        cacheThread.start();
        LOGGER.info(String.format("%s started.", threadName));
    }

    /**
     * 停止本地缓存。
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        queue.clear();
        cacheMap.clear();
        isRunning = false;
        LOGGER.info(String.format("%s stopped.", threadName));
    }

    protected void checkRunning() {
        if (!isRunning) {
            throw new IllegalStateException(String.format("LocalCache not start yet."));
        }
    }

    /**
     * @see com.king4j.cache.Cache#getIfPresent(java.lang.Object)
     */
    @Override
    public V getIfPresent(K key) {
        checkRunning();
        V value = cacheMap.get(key);
        if (value != null && getExpireAfterAccess() > 0) {
            putInner(key, value, getExpireAfterAccess());
        }
        return value;
    }

    /**
     * @see com.king4j.cache.Cache#get(java.lang.Object, java.util.concurrent.Callable)
     */
    @Override
    public V get(K key, Callable<? extends V> valueLoader) throws ExecutionException {
        return get(key, getExpireAfterWrite(), valueLoader);
    }

    /**
     * @see com.king4j.cache.Cache#get(java.lang.Object, int,
     *      java.util.concurrent.Callable)
     */
    @Override
    public V get(K key, int seconds, Callable<? extends V> valueLoader) throws ExecutionException {
        checkRunning();
        if (seconds <= 0) {
            seconds = getExpireAfterWrite();
        }
        cacheMonitor.enter();
        V value = null;
        try {
            value = cacheMap.get(key);
            if (value != null) {
                if (getExpireAfterAccess() > 0) {
                    putInner(key, value, getExpireAfterAccess());
                }
                return value;
            }
            if ((value = valueLoader.call()) == null) {
                throw new IllegalArgumentException("The valueLoader returned value must not be null.");
            }
            removeCacheIfMaxsize();
            putInner(key, value, seconds);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        } finally {
            cacheMonitor.leave();
        }
        return value;
    }

    /**
     * @see com.king4j.cache.Cache#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public void put(K key, V value) {
        put(key, value, getExpireAfterWrite());
    }

    /**
     * @see com.king4j.cache.Cache#put(java.lang.Object, java.lang.Object, int)
     */
    @Override
    public void put(K key, V value, int seconds) {
        checkRunning();
        cacheMonitor.enter();
        try {
            removeCacheIfMaxsize();
            putInner(key, value, seconds);
        } finally {
            cacheMonitor.leave();
        }
    }

    /**
     * @see com.king4j.cache.Cache#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    @Override
    public V putIfAbsent(K key, V value) {
        checkRunning();
        cacheMonitor.enter();
        try {
            V oldVal = cacheMap.get(key);
            if (oldVal != null) {
                return oldVal;
            }
            removeCacheIfMaxsize();
            putInner(key, value, getExpireAfterWrite());
        } finally {
            cacheMonitor.leave();
        }
        return null;
    }

    /**
     * @see com.king4j.cache.Cache#asMap()
     */
    @Override
    public ConcurrentMap<K, V> asMap() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see com.king4j.cache.Cache#remove(java.lang.Object)
     */
    @Override
    public void remove(K key) {
        checkRunning();
        cacheMonitor.enter();
        try {
            V oldValue = cacheMap.remove(key);
            if (oldValue != null) {
                removeQueueElement(key, RemovalCause.EXPLICIT);
            }
        } finally {
            cacheMonitor.leave();
        }
    }

    /**
     * @see com.king4j.cache.Cache#removeAll(K[])
     */
    @Override
    public void removeAll(K... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        cacheMonitor.enter();
        try {
            removeQueueElements(keys, RemovalCause.EXPLICIT);

            for (K key : keys) {
                cacheMap.remove(key);
            }
        } finally {
            cacheMonitor.leave();
        }
    }

    /**
     * @see com.king4j.cache.Cache#clear()
     */
    @Override
    public void clear() {
        checkRunning();
        cacheMonitor.enter();
        try {
            cacheMap.clear();
            queue.clear();
        } finally {
            cacheMonitor.leave();
        }
    }

    /**
     * 返回本地缓存的名称。
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * @see com.king4j.cache.Cache#size()
     */
    @Override
    public int size() {
        return queue.size();
    }

    /**
     * @see com.king4j.cache.Cache#shutdown()
     */
    @Override
    public void shutdown() {
        stop();
    }

    protected void removeQueueElement(final K key, final RemovalCause cause) {
        for (DelayElement<Entry<K, V>> de : queue) {
            if (Objects.equal(de.getElement().getKey(), key)) {
                if (queue.remove(de)) {
                    fireRemovalEvent(de.getElement(), cause);
                }
                break;
            }
        }
    }

    protected void removeQueueElements(final K[] keys, final RemovalCause cause) {
        List<DelayElement<Entry<K, V>>> deList = Lists.newArrayListWithCapacity(keys.length);
        for (DelayElement<Entry<K, V>> de : queue) {
            for (K key : keys) {
                if (Objects.equal(de.getElement().getKey(), key)) {
                    deList.add(de);
                    break;
                }
            }
        }
        if (queue.removeAll(deList)) {
            for (DelayElement<Entry<K, V>> de : deList) {
                fireRemovalEvent(de.getElement(), cause);
            }
        }
    }

    /**
     * 由于缓存达到最大上限而自动移除即将过期的缓存元素（默认移除5个）。
     */
    protected void removeCacheIfMaxsize() {
        if (queue.size() < getMaximumSize()) {
            return;
        }
        int factor = 5;
        List<Entry<K, V>> removedEntries = Lists.newArrayList();
        for (int i = 0; i < factor; i++) {
            DelayElement<Entry<K, V>> de = queue.peek();
            if (de != null) {
                removedEntries.add(de.getElement());
                queue.remove(de);
            }
        }
        if (removedEntries.size() > 0) {
            for (Entry<K, V> entry : removedEntries) {
                cacheMap.remove(entry.getKey());
                fireRemovalEvent(entry, RemovalCause.SIZE);
            }
        }
    }

    protected void putInner(K key, V value, int seconds) {
        assert isRunning == true;
        V oldVal = cacheMap.put(key, value);
        if (oldVal != null) {
            removeQueueElement(key, RemovalCause.REPLACED);
        }

        long nanoTime = TimeUnit.NANOSECONDS.convert(seconds, TimeUnit.SECONDS);
        Entry<K, V> entry = new LocalCacheEntry<K, V>(key, value);
        queue.put(DelayElement.create(entry, nanoTime));
    }

    protected void fireRemovalEvent(final Entry<K, V> entry, final RemovalCause cause) {
        if (entry == null || cause == null || removalListeners.isEmpty()) {
            return;
        }
        List<RemovalListener<K, V>> listeners = null;
        synchronized (removalListeners) {
            listeners = Lists.newArrayList(removalListeners);
        }
        RemovalNotification<K, V> notification = new RemovalNotification<K, V>(entry.getKey(),
                entry.getValue(), cause);
        for (RemovalListener<K, V> listener : listeners) {
            listener.onRemoval(notification);
        }
    }

    /**
     * 返回本地缓存可容纳对象的最大数量。
     */
    public int getMaximumSize() {
        return maximumSize;
    }

    /**
     * 设置本地缓存可容纳对象的最大数量。
     */
    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    /**
     * 返回本地缓存自定入缓存后的过期时间（秒）。
     */
    public int getExpireAfterWrite() {
        return expireAfterWrite;
    }

    /**
     * 设置本地缓存自定入缓存后的过期时间（秒）。
     */
    public void setExpireAfterWrite(int expireAfterWrite) {
        this.expireAfterWrite = expireAfterWrite;
    }

    /**
     * 返回本地缓存自访问后的过期时间（秒）。
     */
    public int getExpireAfterAccess() {
        return expireAfterAccess;
    }

    /**
     * 设置本地缓存自访问后的过期时间（秒）。
     */
    public void setExpireAfterAccess(int expireAfterAccess) {
        this.expireAfterAccess = expireAfterAccess;
    }

    /**
     * 返回本地缓存初始容量。
     */
    public int getInitialCapacity() {
        return initialCapacity;
    }

    /**
     * 设置本地缓存初始容量。
     */
    public void setInitialCapacity(int initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    /**
     * 添加本地缓存移除事件监听。
     */
    public void addCacheRemovalListener(RemovalListener<K, V> listener) {
        if (listener != null) {
            removalListeners.add(listener);
        }
    }

    /**
     * 删除指定的本地缓存的移除事件监听。
     */
    public void removeCacheRemovalListener(RemovalListener<K, V> listener) {
        if (listener != null) {
            removalListeners.remove(listener);
        }
    }

    private static class LocalCacheEntry<K, V> implements Entry<K, V>, Serializable {

        private static final long serialVersionUID = 1L;

        final K key;
        final V value;

        LocalCacheEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            K k = getKey();
            V v = getValue();
            return ((k == null) ? 0 : k.hashCode()) ^ ((v == null) ? 0 : v.hashCode());
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof Entry) {
                Entry<?, ?> that = (Entry<?, ?>) object;
                return Objects.equal(this.getKey(), that.getKey())
                        && Objects.equal(this.getValue(), that.getValue());
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("%s=%s", getKey(), getValue());
        }
    }

    private class DaemonRunnable implements Runnable {

        @Override
        public void run() {
            while (isRunning) {
                try {
                    DelayElement<Entry<K, V>> element = queue.take();
                    if (element != null) {
                        Entry<K, V> entry = element.getElement();
                        cacheMap.remove(entry.getKey(), entry.getValue());
                        fireRemovalEvent(entry, RemovalCause.EXPIRED);
                    }
                } catch (InterruptedException ex) {
                    LOGGER.error("本地缓存守护线程被中断：", ex);
                }
            }
        }
    }
}
