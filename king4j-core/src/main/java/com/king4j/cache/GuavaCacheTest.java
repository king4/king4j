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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * @author Fuchun
 * @version $Id$
 * @since 1.0 13-3-9
 */
public class GuavaCacheTest {

    public static void main(String[] args) {
        Cache<String, String> cache = CacheBuilder.newBuilder().initialCapacity(20)
                .removalListener(new RemovalListener<String, String>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, String> notification) {
                        System.out.println(String.format("The cache key(%s) value(%s) is removed, cause %s",
                                notification.getKey(), notification.getValue(), notification.getCause()));
                    }
                }).expireAfterWrite(10, TimeUnit.SECONDS)
                .expireAfterAccess(10, TimeUnit.SECONDS)
                .maximumSize(10000).build();

        cache.put("key1", "value1");
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // ignore
            }
            cache.put("key2", "value2");
        }

        Scanner scanner = new Scanner(System.in);
        String line;
        while (!"exit".equalsIgnoreCase((line = scanner.nextLine()))) {
            System.out.println(String.format("You enter the \"%s\"", line));

            if (line.startsWith("get")) {
                String key = line.substring(4);
                System.out.println(String.format("Get value (%s) for key (%s) from cache.",
                        cache.getIfPresent(key), key));
            }
        }
        System.out.println("Goodbye!!!");
    }
}
