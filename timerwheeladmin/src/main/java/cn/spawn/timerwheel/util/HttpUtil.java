package cn.spawn.timerwheel.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

public class HttpUtil {

    private final static Logger log = Logger.getLogger(HttpUtil.class) ;

    public static String httpPost(String url, String jsonString) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse httpResponse = null;
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            HttpPost httpPost = new HttpPost(url);
            RequestConfig requestConfig =
                    RequestConfig.custom().
                            setSocketTimeout(6000).
                            setConnectTimeout(6000).
                            build();// 设置请求和传输超时时间
            httpPost.setConfig(requestConfig);
            httpPost.addHeader("Content-Type", "application/json") ;
            StringEntity requestEntity = new StringEntity(jsonString, "utf-8");
            httpPost.setEntity(requestEntity);
            httpResponse = httpClient.execute(httpPost);
            reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (Exception ex) {
            log.error("A error occured in method httpPost, nested exception is ", ex);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (httpResponse != null) {
                    httpResponse.close();
                }
                httpClient.close();
            } catch (Exception ex) {
                log.error("A error occured in method httpPost, nested exception is ", ex);
            }
        }
        return response.toString();

    }

}
