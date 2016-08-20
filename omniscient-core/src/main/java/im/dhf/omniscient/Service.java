/*
 * Copyright (c) 2016, dhf
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package im.dhf.omniscient;

import java.util.Objects;
import java.util.UUID;

/**
 *
 * @author dhf
 */
public class Service {
    public static final String DEFAULT_VERSION = "1.0.0";

    private final String id;
    private final String name;
    private final String version;

    /**
     * @param targetClass target class
     */
    public Service(Class<?> targetClass) {
        this(targetClass.getCanonicalName());
    }

    public Service(Class<?> targetClass, String version) {
        this(targetClass.getCanonicalName(), version);
    }

    public Service(Class<?> targetClass, String version, String id) {
        this(targetClass.getCanonicalName(), version, id);
    }

    /**
     * @param name service name
     */
    public Service(String name) {
        this(name, null, null);
    }

    /**
     * @param name    service name
     * @param version service version
     */
    public Service(String name, String version) {
        this(name, version, null);
    }

    /**
     * @param name    service name
     * @param version service version
     * @param id      service id
     */
    public Service(String name, String version, String id) {
        name = trimToNull(name);
        version = trimToNull(version);
        id = trimToNull(id);

        Objects.requireNonNull(name, "service name should not be blank");
        if (null == version) {
            version = DEFAULT_VERSION;
        }
        if (null == id) {
            id = UUID.randomUUID().toString();
        }

        this.name = name.trim();
        this.version = version.trim();
        this.id = id.trim();
    }

    private static String trimToNull(String str) {
        if (null == str) {
            return null;
        }
        str = str.trim();
        if (str.isEmpty()) {
            return null;
        }
        return str;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return String.format("{id: %s, name: %s, version: %s}", id, name, version);
    }
}
