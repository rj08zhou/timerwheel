package cn.spawn.timerwheel.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.spawn.timerwheel.common.Constants;
import cn.spawn.timerwheel.common.DelayTask;
import cn.spawn.timerwheel.core.DelayTaskSender;
import cn.spawn.timerwheel.util.HttpRequestReader;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class TimerWheelAdminService extends HttpServlet {

    private final static Logger log = Logger.getLogger(TimerWheelAdminService.class) ;
    private static final long serialVersionUID = 1L;
    private DelayTaskSender sender = DelayTaskSender.getDelayTaskSender() ;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException  {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST,GET");
        resp.setHeader("Access-Control-Max-Age", "3600");
        resp.setHeader("Access-Control-Allow-Headers", "content-type");
        String url = req.getPathInfo();
        String requestBody = HttpRequestReader.ReadAsChars(req) ;
        Map<String, Object> responseData = new HashMap<>() ;
        switch (url) {
            case "/addTask" :
                if (requestBody != null && !requestBody.isEmpty()) {
                    List<DelayTask> delayTasks = JSONArray.parseArray(requestBody, DelayTask.class) ;
                    try {
                        //log.info("start sending " + delayTasks.size() + " delayTasks");
                        sender.push2AddTWheelQueue(delayTasks);
                        responseData.put("code", HttpStatus.SC_OK) ;
                        responseData.put("message", "add delaytask success ...") ;
                    } catch (Exception ex){
                        log.error("sending delayTasks failed, nested exception is ", ex);
                        responseData.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR) ;
                        responseData.put("message", "sending delayTasks failed ...") ;
                    }
                } else {
                    log.info("request body is empty!");
                    responseData.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR) ;
                    responseData.put("message", "request body is empty ...") ;
                }
                break ;
            case "/deleteTask" :
                JSONObject jsonObject = JSONObject.parseObject(requestBody) ;
                Map<String, String> requestMap = JSONObject.toJavaObject(jsonObject, Map.class) ;
                String key = requestMap.get("key") ;
                key = Constants.PREFIX + key ;
                sender.deleteTask(key);
                log.info("用户主动调用删除接口删除key:" + key);
                responseData.put("code", HttpStatus.SC_OK) ;
                responseData.put("message", "delete delaytask success ...") ;
                break ;

        }
        resp.setCharacterEncoding("UTF-8");
        String result = JSONObject.toJSONString(responseData) ;
        resp.getWriter().write(result);
    }

}
