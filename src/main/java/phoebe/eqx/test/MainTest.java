package phoebe.eqx.test;


import ec02.tools.dev.AFTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MainTest {
    public static void main(String[] args) {


        clearTheFile();

//
//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/scene_test.xml", "", "");
//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/scene_test_interval.xml", "", "");
        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/scene_test_update_new_resource.xml", "", "");

//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/scene_test_update_quota_available.xml", "", "");

//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/scene_test_update_new_quota.xml", "", "");
//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/gyrar.xml", "", "");
//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/scene_test_update_quota_exhaust.xml", "", "");
//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/scene_test_quota_exh_update_unit.xml", "", "");
//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/refund_complete.xml", "", "");
//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/refund_done.xml", "", "");

//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/E11_timeout.xml", "", "");
//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/E11_timeout_stop.xml", "", "");
//        AFTest.getInstance().run("rPCEF", "./conf/rPCEF.EC02.0.0", "./scene/test.xml", "", "");
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