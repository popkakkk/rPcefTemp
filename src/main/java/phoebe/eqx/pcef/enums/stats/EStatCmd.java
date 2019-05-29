package phoebe.eqx.pcef.enums.stats;

public enum EStatCmd {

    //Usage Monitoring
    Receive_Usage_Monitoring_Request("receive Usage Monitoring request"),
    Sent_Usage_Monitoring_Response("sent Usage Monitoring response"),

    //Refund Management
    Receive_Refund_Management_Request("receive Usage Monitoring request"),
    Sent_Refund_Management_Response("sent Refund Management response"),

    //GyRAR
    receive_GyRAR_request("receive GyRAR request "),
    sent_GyRAR_response("sent GyRAR response"),

    //Usage Monitoring Start
    sent_Usage_Monitoring_Start_request("sent Usage Monitoring Start request"),
    receive_Usage_Monitoring_Start_response("receive Usage Monitoring Start response"),

    //Usage Monitoring Update
    sent_Usage_Monitoring_Update_request("sent Usage Monitoring Update request"),
    receive_Usage_Monitoring_Update_response("receive Usage Monitoring Update response"),

    //Usage Monitoring Stop
    sent_Usage_Monitoring_Stop_request("sent Usage Monitoring Stop request"),
    receive_Usage_Monitoring_Stop_response("receive Usage Monitoring Stop response"),

    //Refund Transaction
    receive_Refund_Transaction_response("receive Refund Transaction response"),
    sent_Refund_Transaction_request("sent Refund Transaction request"),

    //Get Product
    sent_Get_Product_request("sent Get Product request"),
    receive_Get_Product_response("receive Get Product response");


    String cmd;

    EStatCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getCmd() {
        return cmd;
    }
}
