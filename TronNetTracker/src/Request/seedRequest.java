package Request;

public class seedRequest implements Request {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String requestType = "SEED";

    @Override
    public String getRequestType() {
        return this.requestType;
    }

}
