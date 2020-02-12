package cn.spawn.timerwheel.http;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

public class ApiServer {

    private final static Logger log = Logger.getLogger(ApiServer.class) ;
    private final static String SERVLET_PATH = "/timerWheel/*" ;

    public void start(int port) throws Exception{
        Server server = new Server(port);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        handler.addServletWithMapping(TimerWheelAdminService.class, SERVLET_PATH);

        server.start();
        log.info("jetty server started at port:" + port);
    }
}
