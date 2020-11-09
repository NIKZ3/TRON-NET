package Communication.Request;

public class leechRequest implements Request {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String requestType = "LEECH";
    private String merkleRoot = null;
    private Integer portNo = null;
    private String ipAddress = null;

    public leechRequest(String merkleRoot, String ipAddress, Integer portNo) {

        this.merkleRoot = merkleRoot;
        this.portNo = portNo;
        this.ipAddress = ipAddress;
    }

    @Override
    public String getRequestType() {

        return this.requestType;
    }

}
