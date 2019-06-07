package phoebe.eqx.pcef.instance;

import phoebe.eqx.pcef.core.data.QuotaByKey;

import java.util.Date;
import java.util.List;

public class CommitData {


    private ID _id;
    private int count;
    private Date expireDate;

    private List<String> transactionIds;
    private List<String> lastRtid;
    private QuotaByKey quotaByKey;

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public ID get_id() {
        return _id;
    }

    public void set_id(ID _id) {
        this._id = _id;
    }

    public List<String> getTransactionIds() {
        return transactionIds;
    }

    public void setTransactionIds(List<String> transactionIds) {
        this.transactionIds = transactionIds;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public QuotaByKey getQuotaByKey() {
        return quotaByKey;
    }

    public void setQuotaByKey(QuotaByKey quotaByKey) {
        this.quotaByKey = quotaByKey;
    }

    public String getLastRtid() {
        if (lastRtid != null) {
            if (lastRtid.size() == 1) {
                return lastRtid.get(0);
            }

        }
        return "UNKNOWN";
    }
}



