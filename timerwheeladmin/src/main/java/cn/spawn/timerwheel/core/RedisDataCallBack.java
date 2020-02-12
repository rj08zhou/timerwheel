package cn.spawn.timerwheel.core ;

import java.util.Map;

public interface RedisDataCallBack {

    public void handleRedisData(Map<String, String> dataMap) ;

}
