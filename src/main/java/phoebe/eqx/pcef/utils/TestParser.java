package phoebe.eqx.pcef.utils;

import com.google.gson.Gson;

public class TestParser {
    public static void main(String[] args) {

        String str = "{" +
                "\"a\":\"test\"," +
                "\"b\":\"test\"" +
                "" +
                "}";
        Test test = new Gson().fromJson(str,Test.class);

    }







}

class Test{

    String a;

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }
}
