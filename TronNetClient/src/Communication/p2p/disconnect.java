package Communication.p2p;

import Communication.Request.Request;
import Communication.p2p.*;

public class disconnect implements Request {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String msgType = "DISCONNECT";

    public disconnect() {
        this.msgType = "DISCONNECT";
    }

    @Override
    public String getRequestType() {
        return this.msgType;

    }

}
