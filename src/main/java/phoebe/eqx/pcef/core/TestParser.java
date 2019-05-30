package phoebe.eqx.pcef.core;

public class TestParser {


    public static String ocfUsageMonitoring = "{  \n" +
            "   \"command\":\"usageMonitoringStart\",\n" +
            "   \"devMessage\":\"SUCCESS\",\n" +
            "   \"requestNumber\":\"1\",\n" +
            "   \"resources\":[  \n" +
            "      {  \n" +
            "         \"counterId\":\"4001000001\",\n" +
            "         \"monitoringKey\":\"1005176999_4001000001_20251117:174659\",\n" +
            "         \"quotaByKey\":{  \n" +
            "            \"unit\":\"5\",\n" +
            "            \"unitType\":\"unit\",\n" +
            "            \"validityTime\":\"360\"\n" +
            "         },\n" +

            "         \"rateLimitByKey\":{  \n" +
            "            \"transactionPerTime\":\"100|60\"\n" +
            "         },\n" +
            "         \"resourceId\":\"PD-20148\",\n" +
            "         \"resourceName\":\"GSSO/sendOneTimePwd\",\n" +
            "         \"resultCode\":\"20000\",\n" +
            "         \"resultDesc\":\"SUCCESS\",\n" +
            "         \"rtid\":\"R-TRAN-004\"\n" +
            "      }\n" +
            "   ],\n" +
            "   \"sessionId\":\"sessionIdFromPCEF\",\n" +
            "   \"status\":\"200\",\n" +
            "   \"tid\":\"transactionId1\",\n" +
            "   \"userType\":\"privateId\",\n" +
            "   \"userValue\":\"0X9XGyxtxkMzjhNPXRl7PR9F4qOojLtA1548757885384\"\n" +
            "}";

    public static void main(String[] args) {
        PCEFParser ocfUsageMonitoringParser = new PCEFParser(ocfUsageMonitoring);
//        ocfUsageMonitoringParser.translateUsageMonitoringResponse();

    }
}
