package me.j360.raft.counter.command;

/**
 * @author: min_xu
 */
public class ValueCommand extends BooleanCommand {

    private static final long serialVersionUID = -4313480716428249772L;
    private long              vlaue;

    public ValueCommand() {
        super();
    }

    public ValueCommand(boolean result, String errorMsg) {
        super(result, errorMsg);
    }

    public ValueCommand(boolean result) {
        super(result);
    }

    public ValueCommand(long vlaue) {
        super();
        this.vlaue = vlaue;
        this.setSuccess(true);
    }

    public long getVlaue() {
        return this.vlaue;
    }

    public void setVlaue(long vlaue) {
        this.vlaue = vlaue;
    }

}

