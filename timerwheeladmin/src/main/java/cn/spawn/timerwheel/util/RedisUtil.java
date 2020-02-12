package cn.spawn.timerwheel.util;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import cn.spawn.timerwheel.core.RedisDataCallBack;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.log4j.Logger;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;

public class RedisUtil {

    private static final Logger log = Logger.getLogger(RedisUtil.class) ;
    private static GenericObjectPool<StatefulRedisConnection<String, String>> redisPool ;

    static {
        redisPool = getRedisSentinelPool() ;
    }

//	private static GenericObjectPool<StatefulRedisConnection<String, String>> getRedisStandAlonePool(){
//		RedisClient client = RedisClient.create(RedisURI.create("127.0.0.1", 6379)) ;
//		client.setDefaultTimeout(Duration.ofSeconds(20));
//		GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
//		poolConfig.setMaxIdle(20);
//		poolConfig.setMaxTotal(100);
//		redisPool = ConnectionPoolSupport.createGenericObjectPool(()->client.connect(), poolConfig) ;
//		return redisPool ;
//	}

    private static GenericObjectPool<StatefulRedisConnection<String, String>> getRedisSentinelPool(){
        String masterName = PropertiesUtil.getProperty("redis.master.name") ;
        String password = PropertiesUtil.getProperty("redis.master.pwd") ;
        String host = PropertiesUtil.getProperty("redis.sentine.host") ;
        int port = Integer.parseInt(PropertiesUtil.getProperty("redis.sentine.port")) ;
        RedisURI redisUri = RedisURI.builder().withPassword(password).withSentinel(host, port).withSentinelMasterId(masterName).build() ;
        RedisClient client = RedisClient.create(redisUri) ;
        client.setDefaultTimeout(Duration.ofSeconds(20));
        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<StatefulRedisConnection<String, String>>();
        poolConfig.setMaxIdle(20);
        poolConfig.setMaxTotal(100);
        redisPool = ConnectionPoolSupport.createGenericObjectPool(()->client.connect(), poolConfig) ;
        return redisPool ;
    }

    public static void mset(Map<String, String> dataMap) {
        StatefulRedisConnection<String, String> connection = null ;
        try {
            connection  = redisPool.borrowObject() ;
            RedisCommands<String, String> commands = connection.sync();
            commands.mset(dataMap) ;
        } catch (Exception ex) {
            log.error("mset redis failed, nested exception is ", ex);
        } finally {
            connection.close();
        }
    }

    public static void set(String key, String value) {
        StatefulRedisConnection<String, String> connection = null ;
        try {
            connection  = redisPool.borrowObject() ;
            RedisCommands<String, String> commands = connection.sync();
            commands.set(key, value) ;
        } catch (Exception ex) {
            log.error("setex redis failed, nested exception is ", ex);
        } finally {
            connection.close();
        }
    }

    public static void setEx(String key, String value, long seconds) {
        StatefulRedisConnection<String, String> connection = null ;
        try {
            connection  = redisPool.borrowObject() ;
            RedisCommands<String, String> commands = connection.sync();
            commands.setex(key, seconds, value) ;
        } catch (Exception ex) {
            log.error("setex redis failed, nested exception is ", ex);
        } finally {
            connection.close();
        }
    }

    public static void del(String key) {
        StatefulRedisConnection<String, String> connection = null ;
        try {
            connection  = redisPool.borrowObject() ;
            RedisCommands<String, String> commands = connection.sync();
            commands.del(key) ;
        } catch (Exception ex) {
            log.error("del redis failed, nested exception is ", ex);
        } finally {
            connection.close();
        }
    }

    public static void hscan(String key, RedisDataCallBack callback) {
        StatefulRedisConnection<String, String> connection = null ;
        try {
            connection  = redisPool.borrowObject() ;
            RedisCommands<String, String> commands = connection.sync();
            ScanCursor cursor = ScanCursor.INITIAL ;
            ScanArgs scanArgs = ScanArgs.Builder.limit(100);
            do {
                MapScanCursor<String, String> result = commands.hscan(key, cursor, scanArgs);
                //重置游标
                cursor = ScanCursor.of(result.getCursor());
                cursor.setFinished(result.isFinished());
                Map<String, String> dataMap = result.getMap() ;
                if (null != callback) {
                    callback.handleRedisData(dataMap) ;
                }
            } while(!(ScanCursor.FINISHED.getCursor().equals(cursor.getCursor()) && ScanCursor.FINISHED.isFinished() == cursor.isFinished())) ;
        } catch (Exception ex) {
            log.error("hscan redis failed, nested exception is ", ex);
            ex.printStackTrace();
        } finally {
            connection.close();
        }
    }

    public static String get(String key) {
        StatefulRedisConnection<String, String> connection = null ;
        try {
            connection  = redisPool.borrowObject() ;
            RedisAsyncCommands<String, String> commands = connection.async();
            RedisFuture<String> result = commands.get(key) ;
            return result.get() ;
        } catch (Exception ex) {
            log.error("get redis failed, nested exception is ", ex);
        } finally {
            connection.close();
        }
        return null ;
    }

    /**
     * 根据pattern来删除key
     * @param pattern
     * @return
     */
    public static String delKeysByPattern(String pattern) {
        StatefulRedisConnection<String, String> connection = null ;
        try {
            connection  = redisPool.borrowObject() ;
            RedisCommands<String, String> commands = connection.sync();
            ScanCursor cursor = ScanCursor.INITIAL ;
            ScanArgs scanArgs = ScanArgs.Builder.matches(pattern).limit(100) ;
            do {
                KeyScanCursor<String> result = commands.scan(cursor, scanArgs);
                cursor = ScanCursor.of(result.getCursor());
                cursor.setFinished(result.isFinished());
                List<String> keyList = result.getKeys() ;
                if (keyList != null && keyList.size() > 0) {
                    String[] keys = keyList.toArray(new String[keyList.size()]) ;
                    commands.del(keys) ;
                }
            } while(!(ScanCursor.FINISHED.getCursor().equals(cursor.getCursor()) && ScanCursor.FINISHED.isFinished() == cursor.isFinished())) ;
        } catch (Exception ex) {
            log.error("delKeysByPattern redis failed, nested exception is ", ex);
            ex.printStackTrace();
        } finally {
            connection.close();
        }
        return null ;
    }

}
