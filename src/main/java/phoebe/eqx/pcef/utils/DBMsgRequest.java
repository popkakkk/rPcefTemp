package phoebe.eqx.pcef.utils;

public class DBMsgRequest {

    private String database;
    private String colletion;
    private String operation;
    private Object query;


    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getColletion() {
        return colletion;
    }

    public void setColletion(String colletion) {
        this.colletion = colletion;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Object getQuery() {
        return query;
    }

    public void setQuery(Object query) {
        this.query = query;
    }
}
