package phoebe.eqx.pcef.enums.stats;

public enum EStatCmd {

    //Usage Monitoring
    Receive_Usage_Monitoring_Request("receive Usage Monitoring request"),
    Sent_Usage_Monitoring_Response("sent Usage Monitoring response"),

    //Refund Management
    Receive_Refund_Management_Request("receive Refund Management request"),
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
    receive_Get_Product_response("receive Get Product response"),


    //transaction
    sent_insert_Transaction_request("sent insert Transaction request"),
    receive_insert_Transaction_response("receive insert Transaction response"),
    sent_read_Transaction_request("sent read Transaction request"),
    receive_read_Transaction_response("receive read Transaction response"),
    sent_update_Transaction_request("sent update Transaction request"),
    receive_update_Transaction_response("receive update Transaction response"),
    sent_delete_Transaction_request("sent delete Transaction request"),
    receive_delete_Transaction_response("receive delete Transaction response"),

    //profile
    sent_insert_Profile_request("sent insert Profile request"),
    receive_insert_Profile_response("receive insert Profile response"),
    sent_read_Profile_request("sent read Profile request"),
    receive_read_Profile_response("receive read Profile response"),
    sent_update_Profile_request("sent update Profile request"),
    receive_update_Profile_response("receive update Profile response"),
    sent_delete_Profile_request("sent delete Profile request"),
    receive_delete_Profile_response("receive delete Profile response"),

    //Quota
    sent_insert_Quota_request("sent insert Quota request"),
    receive_insert_Quota_response("receive insert Quota response"),
    sent_read_Quota_request("sent read Quota request"),
    receive_read_Quota_response("receive read Quota response"),
    sent_update_Quota_request("sent update Quota request"),
    receive_update_Quota_response("receive update Quota response"),
    sent_delete_Quota_request("sent delete Quota request"),
    receive_delete_Quota_response("receive delete Quota response");


    String cmd;

    EStatCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getCmd() {
        return cmd;
    }
}
