package Communication.p2p;

import Communication.p2p.*;

public class distributionMessage implements p2p {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String msgType = "DISTRIBUTION";
    private Integer distributionIndex = 0;
    private String contentHash = null;

    public distributionMessage(Integer distributionIndex, String contentHash) {
        this.distributionIndex = distributionIndex;
        this.contentHash = contentHash;
    }

    public String getContentHash() {
        return this.contentHash;
    }

    public Integer getDistributionIndex() {
        return this.distributionIndex;
    }

    @Override
    public String getMsgType() {
        return this.msgType;
    }

}
