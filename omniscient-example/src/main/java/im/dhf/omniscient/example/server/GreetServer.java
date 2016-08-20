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

package im.dhf.omniscient.example.server;

import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.ThriftServerConfig;
import com.facebook.swift.service.ThriftServiceProcessor;
import im.dhf.omniscient.Service;
import im.dhf.omniscient.ServiceProvider;
import im.dhf.omniscient.example.idl.GreetService;
import im.dhf.omniscient.registry.ServiceRegister;
import im.dhf.omniscient.registry.consul.ConsulRegistry;
import im.dhf.omniscient.registry.util.ServiceProviderHeartbeat;

import java.util.ArrayList;

/**
 *
 * @author dhf
 */
public class GreetServer {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 12345;
        GreetService service = new GreetServiceImpl();

        // start thrift server
        ThriftServiceProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), new ArrayList<>(), service);
        ThriftServerConfig serverConfig = new ThriftServerConfig();
        serverConfig.setBindAddress(host);
        serverConfig.setPort(port);
        new ThriftServer(processor, serverConfig).start();

        // register to service registry
        ServiceProvider provider = new ServiceProvider(new Service(GreetService.class), host, port);
        String consulHost = "127.0.0.1";
        int consulPort = 8500;
        long heartbeatInterval = 5000L;
        ServiceRegister register = new ConsulRegistry(consulHost, consulPort);
        Thread heartbeatThread = new Thread(new ServiceProviderHeartbeat(register, provider, heartbeatInterval));
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }
}
