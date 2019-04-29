package phoebe.eqx.pcef.enums.config;

public enum EConfigType {
    MANDATORY("Mandatory"),
    OPTIONAL("Optional");

    EConfigType(String name){
        this.name = name;
    }
    private String name;

    public String getName() {
        return name;
    }
}