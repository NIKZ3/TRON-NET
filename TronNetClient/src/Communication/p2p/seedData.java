package Communication.p2p;

import java.io.Serializable;
import Communication.p2p.*;

public class seedData implements p2p {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String messageType = "SEED";
    private byte[] content = null;
    private String contentHash = null;
    private Integer distributionIndex = 0;

    public seedData(byte[] content, Integer distributionIndex, String contentHash) {
        this.content = content;
        this.distributionIndex = distributionIndex;
        this.contentHash = contentHash;
    }

    public Integer getDistributionIndex() {
        return this.distributionIndex;
    }

    public String getContentHash() {
        return this.contentHash;
    }

    public seedData(byte[] content) {
        this.content = content;
    }

    public byte[] getContent() {
        return this.content;
    }

    public String getMsgType() {
        return this.messageType;
    }

}
