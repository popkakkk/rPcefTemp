package phoebe.eqx.pcef.instance;

import phoebe.eqx.pcef.core.model.Quota;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommitPart {

    private Quota quotaExhaust;
    private List<Quota> quotaExpireList = new ArrayList<>();
    private  Map<String, Integer> countUnitMap;

    public Quota getQuotaExhaust() {
        return quotaExhaust;
    }

    public void setQuotaExhaust(Quota quotaExhaust) {
        this.quotaExhaust = quotaExhaust;
    }

    public List<Quota> getQuotaExpireList() {
        return quotaExpireList;
    }

    public void setQuotaExpireList(List<Quota> quotaExpireList) {
        this.quotaExpireList = quotaExpireList;
    }

    public Map<String, Integer> getCountUnitMap() {
        return countUnitMap;
    }

    public void setCountUnitMap(Map<String, Integer> countUnitMap) {
        this.countUnitMap = countUnitMap;
    }
}
