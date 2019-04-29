package phoebe.eqx.pcef.utils;

public class MessageFlow {

    private String message ;
    private String status;
    private String session;


    public MessageFlow(String message, String status, String session) {
        this.message = message;
        this.status = status;
        this.session = session;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }
}
