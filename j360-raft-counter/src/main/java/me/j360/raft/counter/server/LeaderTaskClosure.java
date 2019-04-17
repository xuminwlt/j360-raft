package me.j360.raft.counter.server;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;

public class LeaderTaskClosure implements Closure {
    private Object      cmd;
    private CommandType cmdType;
    private Closure done;
    private Object      response;

    @Override
    public void run(Status status) {
        if (this.done != null) {
            done.run(status);
        }
    }

    public Object getResponse() {
        return this.response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public Object getCmd() {
        return this.cmd;
    }

    public void setCmd(Object cmd) {
        this.cmd = cmd;
    }

    public CommandType getCmdType() {
        return this.cmdType;
    }

    public void setCmdType(CommandType cmdType) {
        this.cmdType = cmdType;
    }

    public Closure getDone() {
        return this.done;
    }

    public void setDone(Closure done) {
        this.done = done;
    }

}
