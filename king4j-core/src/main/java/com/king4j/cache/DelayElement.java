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

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Fuchun
 * @since 1.0
 */
class DelayElement<T> implements Delayed {

    private static final long NANO_ORIGIN = System.nanoTime();

    final static long now() {
        return System.nanoTime() - NANO_ORIGIN;
    }

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    /**
     * 构建一个指定元素和过期时间的 {@code DelayElement} 对象。
     *
     * @param <T>
     * @param element 元素。
     * @param timeout 过期时间，单位：纳秒。
     * @return 新创建的延迟元素对象。
     */
    public static <T> DelayElement<T> create(T element, long timeout) {
        return new DelayElement<T>(element, timeout);
    }

    private final long id;
    private final long time;
    private final T element;

    DelayElement(T element, long timeout) {
        this.element = element;
        this.time = now() + timeout;
        id = SEQUENCE.getAndIncrement();
    }

    /**
     * 返回缓存的元素对象。
     */
    public T getElement() {
        return element;
    }

    /** 返回元素对象保存的时间。 */
    public long getTime() {
        return time;
    }

    /** 返回延迟对象的序列号（唯一Id）。 */
    public long getId() {
        return id;
    }

    @Override
    public long getDelay(TimeUnit timeUnit) {
        return timeUnit.convert(time - now(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o == this) {
            return  0;
        }
        if (o instanceof DelayElement) {
            DelayElement<?> that = (DelayElement<?>) o;
            long diff = time - that.getTime();
            if (diff < 0) {
                return  -1;
            } else if (diff > 0) {
                return  1;
            } else if (id < that.getId()) {
                return -1;
            } else {
                return 1;
            }
        }
        long d = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
        return (d == 0) ? 0 : (d < 0 ? -1 : 1);
    }
}
