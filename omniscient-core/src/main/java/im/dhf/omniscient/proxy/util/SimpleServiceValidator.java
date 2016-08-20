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

package im.dhf.omniscient.proxy.util;

import im.dhf.omniscient.proxy.JavaServiceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author dhf
 */
public class SimpleServiceValidator implements JavaServiceValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleServiceValidator.class);

    private final String methodName;
    private final Object expectedResult;
    private final Object[] args;

    private volatile long methodTimeout = 5000L;

    public SimpleServiceValidator(String methodName) {
        this(methodName, null, null);
    }

    public SimpleServiceValidator(String methodName, Object expectedResult) {
        this(methodName, null, expectedResult);
    }

    public SimpleServiceValidator(String methodName, Object[] args, Object expectedResult) {
        Objects.requireNonNull(methodName);

        this.methodName = methodName;
        this.args = args;
        this.expectedResult = expectedResult;
    }

    @Override
    public boolean isValid(Class<?> clazz, Object service) {
        if (null == clazz || null == service) {
            return false;
        }

        Method method = null;
        try {
            method = clazz.getMethod(methodName);
        } catch (Throwable e) {
            LOGGER.debug("service not valid, method not found. class: {}, method: {}", clazz, method, e);
            return false;
        }

        try {
            Object ret = null;
            Object result = null;
            if (null == args) {
                ret = method.invoke(service);
            } else {
                ret = method.invoke(service, args);
            }
            if (ret instanceof Future) {
                Future future = (Future) ret;
                result = future.get(methodTimeout, TimeUnit.MILLISECONDS);
            } else {
                result = ret;
            }

            if (null == expectedResult) {
                return (result == null);
            }
            return expectedResult.equals(result);
        } catch (Throwable e) {
            LOGGER.debug("service not valid, invoke failed. class: {}, method: {}", clazz, method, e);
            return false;
        }
    }

    public void setMethodTimeout(long methodTimeout) {
        this.methodTimeout = methodTimeout;
    }

    public long getMethodTimeout() {
        return methodTimeout;
    }
}
