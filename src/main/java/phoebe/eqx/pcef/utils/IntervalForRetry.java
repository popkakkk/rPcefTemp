package phoebe.eqx.pcef.utils;

import phoebe.eqx.pcef.services.MongoDBService;

public class IntervalForRetry {

    private int interval;// sec
    private int retryConfig;// sec

    public IntervalForRetry(int interval, int retryConfig, MongoDBService mgService) {
        this.interval = interval;
        this.retryConfig = retryConfig;
    }


    public void processInterval() {




    }


    public int getInterval() {
        return interval;
    }

    public int getRetryConfig() {
        return retryConfig;
    }
}
