/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.conf;

import java.util.HashMap;

/** Empty Configuration. */
public class Configuration extends HashMap<String, String> {

    private int version = 0;

    public Configuration() {
        // Constructor implementation
    }

    /**
     * Gets the version of the configuration.
     *
     * @return the version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Set the <code>value</code> of the <code>name</code> property. If <code>name</code> is
     * deprecated or there is a deprecated name associated to it, it sets the value to both names.
     * Name will be trimmed before put into configuration.
     *
     * @param name property name.
     * @param value property value.
     */
    public void set(String name, String value) {
        put(name, value);
    }

    /**
     * Get the value of the <code>name</code> property, <code>null</code> if no such property
     * exists. If the key is deprecated, it returns the value of the first key which replaces the
     * deprecated key and is not null.
     *
     * <p>Values are processed for <a href="#VariableExpansion">variable expansion</a> before being
     * returned.
     *
     * <p>As a side effect get loads the properties from the sources if called for the first time as
     * a lazy init.
     *
     * @param name the property name, will be trimmed before get value.
     * @return the value of the <code>name</code> or its replacing property, or null if no such
     *     property exists.
     */
    public String get(String name) {
        return super.get(name);
    }

    /**
     * Get the value of the <code>name</code>. If the key is deprecated, it returns the value of the
     * first key which replaces the deprecated key and is not null. If no such property exists, then
     * <code>defaultValue</code> is returned.
     *
     * @param name property name, will be trimmed before get value.
     * @param defaultValue default value.
     * @return property value, or <code>defaultValue</code> if the property doesn't exist.
     */
    public String get(String name, String defaultValue) {
        return super.getOrDefault(name, defaultValue);
    }
}
