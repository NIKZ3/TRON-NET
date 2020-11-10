package Communication.p2p;

import Communication.Request.Request;
import Communication.p2p.*;

public class disconnect implements p2p {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String msgType = "DISCONNECT";

    public disconnect() {
        this.msgType = "DISCONNECT";
    }

    @Override
    public String getMsgType() {
        return this.msgType;
    }

}
