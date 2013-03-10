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

package com.king4j.auth;

/**
 * Provides a way to identify a user across more than one page request or visit to a Web site and
 * to store information about that user.
 *
 * @author Fuchun
 * @since 1.0
 */
public interface Session {

    String SESSION_CREATED_EVENT = "createSession";

    String SESSION_DESTROYED_EVENT = "destroySession";

    String SESSION_ACTIVATED_EVENT = "activatedSession";

    String SESSION_PASSIVATED_EVENT = "passivateSession";

    public String getId();

    public boolean isNew();

    public Object getAttribute(String name);

    public void setAttribute(String name, Object value);

    public void removeAttribute(String name);

    public long getCreationTime();

    public int getMaxInactiveInterval();

    public void setMaxInactiveInterval(int interval);
}
