package Request;

public class leechRequest implements Request {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String requestType = "LEECH";
    private String merkleRoot = null;
    private Integer portNo = null;
    private String ipAddress = null;
    private String fileName = null;

    public leechRequest(String merkleRoot, String ipAddress, String fileName, Integer portNo) {

        this.merkleRoot = merkleRoot;
        this.portNo = portNo;
        this.ipAddress = ipAddress;
        this.fileName = fileName;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getMerkleRoot() {
        return this.merkleRoot;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public Integer getPortNo() {
        return this.portNo;
    }

    @Override
    public String getRequestType() {

        return this.requestType;
    }

}
