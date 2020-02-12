package cn.spawn.timerwheel.util;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class ZookeeperUtil {

    private static CuratorFramework curatorFramework;

    public static CuratorFramework getCuratorFramework() {
        return curatorFramework;
    }

    public static CuratorFramework initCuratorFramework(String zkQuorum, Long connectionTimeoutMs, Long sessionTimeoutMs) {
        //重试策略，初试时间为1s，重试10次
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
        CuratorFramework cf = CuratorFrameworkFactory.builder()
                //zk地址
                .connectString(zkQuorum)
                //尝试连接，5秒内连接不上就断开
                .connectionTimeoutMs(connectionTimeoutMs.intValue())
                //连接以后，5秒内没有操作，就断开
                .sessionTimeoutMs(sessionTimeoutMs.intValue())
                //重试策略
                .retryPolicy(retryPolicy)
                //.namespace("mon")
                .build();
        cf.start();
        curatorFramework = cf;
        return cf;
    }

}
