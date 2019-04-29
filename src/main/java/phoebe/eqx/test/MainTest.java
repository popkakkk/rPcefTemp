package phoebe.eqx.test;


import ec02.tools.dev.AFTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MainTest {
    public static void main(String[] args) {
        clearTheFile();
        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/scene_test.xml", "", "");
    }

    public static void clearTheFile() {
        String fileLogPath = "./log/rPCEF.EC02LIB.0.0.log";
        File fileLog = new File(fileLogPath);
        if (fileLog.exists()) {
            System.out.println("---- Clear file log ----");
            try {
                FileWriter fwOb = new FileWriter(fileLogPath, false);
                PrintWriter pwOb = new PrintWriter(fwOb, false);
                pwOb.flush();
                pwOb.close();
                fwOb.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}