package me.j360.raft.counter.command;

import java.io.Serializable;

/**
 * @author: min_xu
 */
public class BooleanCommand implements Serializable {

    private static final long serialVersionUID = 2776110757482798187L;
    private boolean           success;
    private String            errorMsg;
    private String            redirect;

    public String getRedirect() {
        return this.redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public BooleanCommand() {
        super();
    }

    public BooleanCommand(boolean result) {
        this(result, null);
    }

    public BooleanCommand(boolean result, String errorMsg) {
        super();
        this.success = result;
        this.errorMsg = errorMsg;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean result) {
        this.success = result;
    }
}
