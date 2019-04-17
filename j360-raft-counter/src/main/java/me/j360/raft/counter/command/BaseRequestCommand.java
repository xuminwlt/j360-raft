package me.j360.raft.counter.command;

/**
 * @author: min_xu
 */
public abstract class BaseRequestCommand {

    private String key;

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
