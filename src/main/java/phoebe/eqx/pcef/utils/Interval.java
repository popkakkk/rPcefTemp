package phoebe.eqx.pcef.utils;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.services.IMyFuncInterval;

public class Interval {

    private int retryConfig;
    private int interval;
    private int retry = 0;

    public Interval(int retryConfig, int interval) {
        this.retryConfig = retryConfig;
        this.interval = interval;
    }


    public void waitInterval() throws TimeoutIntervalException {
        retry++;
        if (retry > retryConfig) {
            AFLog.d("Interval Timeout!!!");
            throw new TimeoutIntervalException();
        }
        try {
            AFLog.d("WAIT INTERVAL :" + retry + " TIME!!!");
            Thread.sleep(interval * 1000);
        } catch (InterruptedException e) {
            AFLog.d("Thread has interrupt!!");
        }
    }

    public void waitIntervalAndRetry(IMyFuncInterval func) throws TimeoutIntervalException {
        boolean canProcess = false;
        while (!canProcess) {
            waitInterval();
            canProcess = func.execute();
        }
    }

    public void reset() {
        retry = 0;
    }


}
