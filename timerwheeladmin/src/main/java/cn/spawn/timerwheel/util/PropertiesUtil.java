package cn.spawn.timerwheel.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesUtil {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);
    private static Properties props;
    static{
        loadProps();
    }

    synchronized static private void loadProps(){
        props = new Properties();
        InputStream in = null;
        BufferedReader bufferedReader = null;
        try {
            in = PropertiesUtil.class.getClassLoader().getResourceAsStream("service.properties");
            bufferedReader = new BufferedReader(new InputStreamReader(in,"utf-8"));
            props.load(bufferedReader);
        } catch (FileNotFoundException e) {
            logger.error("service.properties文件未找到");
        } catch (IOException e) {
            logger.error("出现IOException");
        } finally {
            try {
                if(null != bufferedReader){
                    bufferedReader.close();
                }
                if(null != in) {
                    in.close();
                }
            } catch (IOException e) {
                logger.error("service.properties文件流关闭出现异常");
            }
        }
    }

    public static String getProperty(String key){
        if(null == props) {
            loadProps();
        }
        return props.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        if(null == props) {
            loadProps();
        }
        return props.getProperty(key, defaultValue);
    }

}