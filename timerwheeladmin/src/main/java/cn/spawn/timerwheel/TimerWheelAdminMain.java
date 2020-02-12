package cn.spawn.timerwheel;

import cn.spawn.timerwheel.core.DelayTaskSender;
import cn.spawn.timerwheel.http.ApiServer;
import cn.spawn.timerwheel.util.PropertiesUtil;

public class TimerWheelAdminMain {

    public static void main(String[] args) {
        ApiServer jettyServer = new ApiServer();
        try {
            jettyServer.start(Integer.parseInt(PropertiesUtil.getProperty("jetty.port")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        DelayTaskSender sender = DelayTaskSender.getDelayTaskSender();
        sender.startSender();
        for(;;){}
    }
}
