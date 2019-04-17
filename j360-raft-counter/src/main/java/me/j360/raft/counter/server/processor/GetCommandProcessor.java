/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.j360.raft.counter.server.processor;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import me.j360.raft.counter.KeyNotFoundException;
import me.j360.raft.counter.command.GetCommand;
import me.j360.raft.counter.command.ValueCommand;
import me.j360.raft.counter.server.AtomicRangeGroup;
import me.j360.raft.counter.server.AtomicServer;
import me.j360.raft.counter.server.CommandType;

/**
 * Get command processor
 * @author dennis
 *
 */
public class GetCommandProcessor extends BaseAsyncUserProcessor<GetCommand> {

    @Override
    protected CommandType getCmdType() {
        return CommandType.GET;
    }

    public GetCommandProcessor(AtomicServer server) {
        super(server);
    }

    @Override
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, GetCommand request) {
        if (request.isReadByStateMachine()) {
            super.handleRequest(bizCtx, asyncCtx, request);
        } else {
            try {
                final AtomicRangeGroup group = server.getGroupBykey(request.getKey());
                if (!request.isReadFromQuorum()) {
                    asyncCtx.sendResponse(new ValueCommand(group.getFsm().getValue(request.getKey())));
                } else {
                    group.readFromQuorum(request.getKey(), asyncCtx);
                }
            } catch (final KeyNotFoundException e) {
                asyncCtx.sendResponse(createKeyNotFoundResponse());
            }
        }
    }

    public static ValueCommand createKeyNotFoundResponse() {
        return new ValueCommand(false, "key not found");
    }

    @Override
    public String interest() {
        return GetCommand.class.getName();
    }

}
