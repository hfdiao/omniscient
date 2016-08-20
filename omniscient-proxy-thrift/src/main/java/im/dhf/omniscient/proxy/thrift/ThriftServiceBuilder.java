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

package im.dhf.omniscient.proxy.thrift;

import com.facebook.nifty.client.FramedClientChannel;
import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.nifty.client.NiftyClientChannel;
import com.facebook.swift.service.ThriftClientManager;
import com.google.common.util.concurrent.ListenableFuture;
import im.dhf.omniscient.OmniscientException;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author dhf
 */
public class ThriftServiceBuilder {
    private ThriftClientManager clientManager;
    private Class<?> type;
    private InetSocketAddress address;
    private long connectTimeoutMills = -1;

    public ThriftClientManager clientManager() {
        return clientManager;
    }

    public ThriftServiceBuilder clientManager(ThriftClientManager clientManager) {
        this.clientManager = clientManager;
        return this;
    }

    public Class<?> type() {
        return type;
    }

    public ThriftServiceBuilder type(Class<?> type) {
        this.type = type;
        return this;
    }

    public InetSocketAddress address() {
        return address;
    }

    public ThriftServiceBuilder address(InetSocketAddress address) {
        this.address = address;
        return this;
    }

    public long connectTimeoutMills() {
        return connectTimeoutMills;
    }

    public ThriftServiceBuilder connectTimeoutMills(long connectTimeoutMills) {
        this.connectTimeoutMills = connectTimeoutMills;
        return this;
    }

    public Object build() {
        Objects.requireNonNull(type);
        Objects.requireNonNull(address);
        Objects.requireNonNull(clientManager);
        return createService();
    }

    private Object createService() {
        NiftyClientChannel channel = connect();
        Object service = clientManager.createClient(channel, type);
        return service;
    }

    private NiftyClientChannel connect() {
        try {
            FramedClientConnector connector = new FramedClientConnector(address);
            ListenableFuture<FramedClientChannel> channelFuture = clientManager.createChannel(connector);
            FramedClientChannel framedClientChannel = null;
            if (connectTimeoutMills <= 0) {
                framedClientChannel = channelFuture.get();
            } else {
                framedClientChannel = channelFuture.get(connectTimeoutMills, TimeUnit.MILLISECONDS);
            }
            return framedClientChannel;
        } catch (Exception e) {
            String errMsg = "connect to address failed: " + address;
            throw new OmniscientException(errMsg, e);
        }
    }
}
