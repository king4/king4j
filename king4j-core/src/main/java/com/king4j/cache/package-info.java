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

/**
 * 提供缓存所需的类和接口。
 * <p />
 * 该缓存组件中的接口设计参考了 <a href="http://code.google.com/p/guava-libraries/">Google-Guava library</a>。
 * 但缓存的存储实现与 {@code guava} 完全不同，该缓存使用了 {@link java.util.concurrent.Delayed} 接口作为缓存对象的载体，
 * 实现在指定过期时间后自动执行 {@code remove} 操作。
 */
package com.king4j.cache;