package cn.spawn.timerwheel.util;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class HttpRequestReader {

    private final static Logger log = Logger.getLogger(HttpRequestReader.class) ;

    public static String ReadAsChars(HttpServletRequest request) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder("");
        try {
            br = request.getReader();
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
            br.close();
        } catch (IOException ex) {
            log.error("reading http request failed..., nested exception is ", ex);
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (IOException ex) {
                    log.error("reading http request failed..., nested exception is ", ex);
                }
            }
        }
        return sb.toString();
    }
}
