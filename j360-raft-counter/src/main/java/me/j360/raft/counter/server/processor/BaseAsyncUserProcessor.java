package me.j360.raft.counter.server.processor;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AsyncUserProcessor;
import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.entity.Task;
import me.j360.raft.counter.command.BaseRequestCommand;
import me.j360.raft.counter.command.BooleanCommand;
import me.j360.raft.counter.command.CommandCodec;
import me.j360.raft.counter.server.AtomicRangeGroup;
import me.j360.raft.counter.server.AtomicServer;
import me.j360.raft.counter.server.CommandType;
import me.j360.raft.counter.server.LeaderTaskClosure;

import java.nio.ByteBuffer;

/**
 * @author: min_xu
 */
public abstract class BaseAsyncUserProcessor<T extends BaseRequestCommand> extends AsyncUserProcessor<T> {

    protected AtomicServer server;

    public BaseAsyncUserProcessor(AtomicServer server) {
        super();
        this.server = server;
    }

    @Override
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request) {
        final AtomicRangeGroup group = server.getGroupBykey(request.getKey());
        if (!group.getFsm().isLeader()) {
            asyncCtx.sendResponse(group.redirect());
            return;
        }

        final CommandType cmdType = getCmdType();
        final Task task = createTask(asyncCtx, request, cmdType);
        group.getNode().apply(task);
    }

    protected abstract CommandType getCmdType();

    private Task createTask(AsyncContext asyncCtx, T request, CommandType cmdType) {
        final LeaderTaskClosure closure = new LeaderTaskClosure();
        closure.setCmd(request);
        closure.setCmdType(cmdType);
        closure.setDone(new Closure() {

            @Override
            public void run(Status status) {
                if (status.isOk()) {
                    asyncCtx.sendResponse(closure.getResponse());
                } else {
                    asyncCtx.sendResponse(new BooleanCommand(false, status.getErrorMsg()));
                }
            }
        });
        final byte[] cmdBytes = CommandCodec.encodeCommand(request);
        final ByteBuffer data = ByteBuffer.allocate(cmdBytes.length + 1);
        data.put(cmdType.toByte());
        data.put(cmdBytes);
        data.flip();
        final Task task = new Task(data, closure);
        return task;
    }

}

