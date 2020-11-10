package Communication.Request;

public class serverSeedMsg implements Request {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String requestType = "SERVERSEED";
    private String leecherIP = "localhost";
    private Integer leecherPort = 3000;
    private String merkleRoot = null;
    private String fileName = null;

    public serverSeedMsg(String merkleRoot, String leecherIP, Integer leecherPort, String fileName) {
        this.merkleRoot = merkleRoot;
        this.leecherIP = leecherIP;
        this.leecherPort = leecherPort;
        this.fileName = fileName;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getLeecherIP() {
        return this.leecherIP;
    }

    public Integer getLeecherPort() {
        return this.leecherPort;
    }

    public String getMerkleRoot() {
        return this.merkleRoot;
    }

    @Override
    public String getRequestType() {
        return this.requestType;
    }

}
