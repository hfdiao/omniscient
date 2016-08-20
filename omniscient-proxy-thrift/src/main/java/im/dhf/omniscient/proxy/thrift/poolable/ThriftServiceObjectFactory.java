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

package im.dhf.omniscient.proxy.thrift.poolable;

import com.facebook.swift.service.ThriftClientManager;
import im.dhf.omniscient.proxy.JavaServiceValidator;
import im.dhf.omniscient.proxy.thrift.ThriftServiceBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author dhf
 */
class ThriftServiceObjectFactory extends BaseKeyedPooledObjectFactory<Pair<Class<?>, InetSocketAddress>, Object> {
    private final ThriftClientManager clientManager;
    private final JavaServiceValidator serviceValidator;

    public ThriftServiceObjectFactory(ThriftClientManager clientManager) {
        this(clientManager, null);
    }

    public ThriftServiceObjectFactory(ThriftClientManager clientManager, JavaServiceValidator serviceValidator) {
        this.clientManager = clientManager;
        this.serviceValidator = serviceValidator;
    }

    @Override
    public Object create(Pair<Class<?>, InetSocketAddress> pair) throws Exception {
        return new ThriftServiceBuilder().clientManager(clientManager).type(pair.getKey()).address(pair.getValue()).build();
    }

    @Override
    public PooledObject<Object> wrap(Object value) {
        return new DefaultPooledObject<>(value);
    }

    @Override
    public void destroyObject(Pair<Class<?>, InetSocketAddress> key, PooledObject<Object> p) throws Exception {
        Object service = p.getObject();
        if (service instanceof Closeable) {
            closeQuietly((Closeable) service);
        }
    }

    @Override
    public boolean validateObject(Pair<Class<?>, InetSocketAddress> key, PooledObject<Object> p) {
        if (null == serviceValidator) {
            return true;
        }
        Object service = p.getObject();
        return serviceValidator.isValid(key.getKey(), service);
    }

    private static void closeQuietly(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }
}
