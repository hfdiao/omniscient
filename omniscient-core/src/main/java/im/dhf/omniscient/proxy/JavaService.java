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

package im.dhf.omniscient.proxy;

import im.dhf.omniscient.OmniscientException;
import im.dhf.omniscient.Service;

/**
 *
 * @author dhf
 */
public class JavaService extends Service {
    private final Class<?> targetClass;

    public JavaService(Service service) {
        super(service.getName(), service.getVersion(), service.getId());

        if (service instanceof JavaService) {
            this.targetClass = ((JavaService) service).getTargetClass();
        } else {
            this.targetClass = toClass(service.getName());
        }
    }

    /**
     * @param targetClass target class
     * @param service     service
     */
    public JavaService(Class<?> targetClass, Service service) {
        this(targetClass, service.getName(), service.getVersion(), service.getId());
    }

    /**
     * @param targetClass target class
     */
    public JavaService(Class<?> targetClass) {
        this(targetClass, null, null, null);
    }

    /**
     * @param targetClass target class
     * @param name        service name
     */
    public JavaService(Class<?> targetClass, String name) {
        this(targetClass, name, null, null);
    }

    /**
     * @param targetClass target class
     * @param name        service name
     * @param version     service version
     */
    public JavaService(Class<?> targetClass, String name, String version) {
        this(targetClass, name, version, null);
    }

    /**
     * @param targetClass target class
     * @param name        service name
     * @param version     service version
     * @param id          service id
     */
    public JavaService(Class<?> targetClass, String name, String version, String id) {
        super(name, version, id);

        this.targetClass = targetClass;
    }


    public Class<?> getTargetClass() {
        return targetClass;
    }

    private static Class<?> toClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new OmniscientException(e);
        }
    }
}
