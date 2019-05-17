package phoebe.eqx.pcef.core.model;


import phoebe.eqx.pcef.core.data.QuotaByKey;
import phoebe.eqx.pcef.core.data.RateLimitByKey;
import phoebe.eqx.pcef.core.data.ResourceQuota;

import java.util.ArrayList;
import java.util.Date;

public class Quota {

    private String _id;
    private String userType;
    private String userValue;
    private Integer processing;
    private Date expireDate;
    private String monitoringKey;
    private String counterId;
    private QuotaByKey quotaByKey;
    private RateLimitByKey rateLimitByKey;
    private ArrayList<ResourceQuota> resources = new ArrayList<>();


    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getUserValue() {
        return userValue;
    }

    public void setUserValue(String userValue) {
        this.userValue = userValue;
    }

    public Integer getProcessing() {
        return processing;
    }

    public void setProcessing(Integer processing) {
        this.processing = processing;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public String getMonitoringKey() {
        return monitoringKey;
    }

    public void setMonitoringKey(String monitoringKey) {
        this.monitoringKey = monitoringKey;
    }

    public String getCounterId() {
        return counterId;
    }

    public void setCounterId(String counterId) {
        this.counterId = counterId;
    }

    public QuotaByKey getQuotaByKey() {
        return quotaByKey;
    }

    public void setQuotaByKey(QuotaByKey quotaByKey) {
        this.quotaByKey = quotaByKey;
    }

    public RateLimitByKey getRateLimitByKey() {
        return rateLimitByKey;
    }

    public void setRateLimitByKey(RateLimitByKey rateLimitByKey) {
        this.rateLimitByKey = rateLimitByKey;
    }

    public ArrayList<ResourceQuota> getResources() {
        return resources;
    }

    public void setResources(ArrayList<ResourceQuota> resources) {
        this.resources = resources;
    }
}
