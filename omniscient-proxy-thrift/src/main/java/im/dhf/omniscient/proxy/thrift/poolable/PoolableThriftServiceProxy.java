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

import im.dhf.omniscient.OmniscientException;
import im.dhf.omniscient.proxy.JavaServiceValidator;
import im.dhf.omniscient.proxy.thrift.ThriftServiceProxy;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.transport.TTransportException;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author dhf
 */
public class PoolableThriftServiceProxy extends ThriftServiceProxy implements Closeable {
    private volatile ConcurrentHashMap<Object, InetSocketAddress> serviceAddressMap = new ConcurrentHashMap<>();

    private GenericKeyedObjectPool<Pair<Class<?>, InetSocketAddress>, Object> servicePool;

    public PoolableThriftServiceProxy() {
        this(null, null);
    }

    public PoolableThriftServiceProxy(GenericKeyedObjectPoolConfig poolConfig) {
        this(poolConfig, null);
    }

    public PoolableThriftServiceProxy(GenericKeyedObjectPoolConfig poolConfig, JavaServiceValidator serviceValidator) {
        if (null == poolConfig) {
            poolConfig = new GenericKeyedObjectPoolConfig();
        }
        ThriftServiceObjectFactory serviceObjectFactory = new ThriftServiceObjectFactory(clientManager, serviceValidator);
        servicePool = new GenericKeyedObjectPool(serviceObjectFactory, poolConfig);
    }

    @Override
    public void close() throws IOException {
        servicePool.close();

        super.close();
    }

    @Override
    public <T> T proxy(Class<T> clazz, InetSocketAddress address) {
        try {
            T service = (T) servicePool.borrowObject(Pair.of(clazz, address));
            serviceAddressMap.put(service, address);
            return service;
        } catch (Exception e) {
            String errMsg = "could not get service object for class: " + clazz.getCanonicalName() + " from pool with address: " + address;
            throw new OmniscientException(errMsg, e);
        }
    }

    @Override
    public void destroy(Class<?> targetClass, Object obj, Throwable cause) throws OmniscientException {
        if (null == obj) {
            return;
        }

        InetSocketAddress address = serviceAddressMap.get(obj);
        if (null != cause && cause instanceof TTransportException) {
            try {
                servicePool.invalidateObject(Pair.of(targetClass, address), obj);
            } catch (Exception e) {
                throw new OmniscientException(e);
            }
        } else {
            servicePool.returnObject(Pair.of(targetClass, address), obj);
        }

        serviceAddressMap.remove(obj);
    }
}
