package cn.spawn.timerwheel.core;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cn.spawn.timerwheel.common.Constants;
import cn.spawn.timerwheel.common.DelayTask;
import cn.spawn.timerwheel.common.ServiceNode;
import cn.spawn.timerwheel.proto.DelayTaskModel;
import cn.spawn.timerwheel.rpc.RpcClient;
import cn.spawn.timerwheel.util.HttpUtil;
import cn.spawn.timerwheel.util.PropertiesUtil;
import cn.spawn.timerwheel.util.RedisUtil;
import cn.spawn.timerwheel.util.ZookeeperUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;

public class DelayTaskSender {

    private final static Logger log = Logger.getLogger(DelayTaskSender.class) ;
    private static final String ZKROOT = "zookeeper.root" ;
    private static final String ZKQUORUM = "zookeeper.zkQuorum" ;
    private static final String CONNECTIONTIMEOUT = "zookeeper.connectionTimeoutMs" ;
    private static final String SESSIONTIMEOUT = "zookeeper.sessionTimeoutMs" ;
    private static final String SENDBATCH = "send.batch" ;
    private static final String LOCKPATH = "zookeeper.lock.path" ;
    private static final String DATAPATH = "zookeeper.data.path" ;

    //队列最大长度
    public static final int DefaultSendQueueMaxSize = 102400 ;
    //时间轮服务池
    private ConcurrentMap<String, ServiceNode> serviceNodePool = new ConcurrentHashMap<String, ServiceNode>() ;
    //发送缓存队列
    private ConcurrentMap<String, LinkedBlockingQueue<DelayTask>> tWheelAddQueues = new ConcurrentHashMap<String, LinkedBlockingQueue<DelayTask>>();
    private ConcurrentMap<String, Forward2AddTimerWheelThread> sendTaskThreadPool = new ConcurrentHashMap<String, Forward2AddTimerWheelThread>() ;
    //zk客户端
    protected CuratorFramework curatorFramework;
    private PathChildrenCache zkRootPathChildrenCache ;
    private String distributeLock = PropertiesUtil.getProperty(LOCKPATH) ;
    private String dataNodes = PropertiesUtil.getProperty(DATAPATH) ;
    //round robin发送到不同时间轮服务的计数器
    private final AtomicInteger next = new AtomicInteger(0);

    private DelayTaskSender(){}

    private static DelayTaskSender instance = new DelayTaskSender() ;

    public static DelayTaskSender getDelayTaskSender(){
        return instance ;
    }

    public void startSender() {
        try {
            //初始化zookeeper
            initZookeeper() ;
            //初始化缓存队列
            initSendQueue() ;
        } catch (Exception ex) {
            log.error("starting task sender failed..., nested exception is ", ex);
        }
    }

    //将数据推入某个发送缓存队列
    public void push2AddTWheelQueue(List<DelayTask> delayTasks) throws InterruptedException {
        for (DelayTask task : delayTasks) {
            log.info("receive task from sf-falcon, key is"  + task.getKey());
            task.setKey(Constants.PREFIX + task.getKey());
            if (task.getDelay() == 0) {
                int delay = (int) ( (task.getExpiredTime() - System.currentTimeMillis())/(1000*60) ) ;
                task.setDelay(delay);
            }
            String key = robinRoundKey() ;
            if (key != null) {
                LinkedBlockingQueue<DelayTask> queue = tWheelAddQueues.get(key) ;
                queue.put(task);
//                String jsonTask = JSONObject.toJSONString(task) ;
//                String base64Task = Base64.getEncoder().encodeToString(jsonTask.getBytes()) ;
//                RedisUtil.setEx(task.getKey(), base64Task, (task.getDelay() + 10) * 60);
            } else {
                log.error("tWheelAddQueues.size == 0 !!!");
            }
        }
    }

    protected void push2AddTWheelQueueByOne(DelayTask task) throws InterruptedException {
        //随机取出一个队列用于发送
        String key = robinRoundKey() ;
        LinkedBlockingQueue<DelayTask> queue = tWheelAddQueues.get(key) ;
        //如果不能取出queue,意味着所有的时间轮服务都down了
        if (queue != null) {
            queue.put(task);
            String jsonTask = JSONObject.toJSONString(task) ;
            String base64Task = Base64.getEncoder().encodeToString(jsonTask.getBytes()) ;
            RedisUtil.setEx(task.getKey(), base64Task, (task.getDelay() + 10) * 60);
        } else {
            log.error("All timerwheel services have been stopped...");
        }
    }

    private String robinRoundKey() {
        Object[] keys = tWheelAddQueues.keySet().toArray() ;
        if (keys.length == 0) {
            return null ;
        }
        return keys[next.getAndIncrement() % keys.length].toString() ;
    }

    public void deleteTask(String key) {
        RedisUtil.del(key);
    }

    private void initZookeeper() throws Exception {
        String zkRootPath = PropertiesUtil.getProperty(ZKROOT) ;
        String zkQuorum = PropertiesUtil.getProperty(ZKQUORUM) ;
        String connectionTimeoutMs = PropertiesUtil.getProperty(CONNECTIONTIMEOUT) ;
        String sessionTimeoutMs = PropertiesUtil.getProperty(SESSIONTIMEOUT) ;
        //创建zk客户端
        curatorFramework = ZookeeperUtil.initCuratorFramework(zkQuorum, Long.valueOf(connectionTimeoutMs), Long.valueOf(sessionTimeoutMs)) ;
        //创建父节点
        if (curatorFramework.checkExists().forPath(zkRootPath)== null){
            curatorFramework.create().creatingParentsIfNeeded().forPath(zkRootPath) ;
        }
        //创建默认节点
        initDefaultNode() ;
        //初始化已有节点
        initServiceNode();
        //创建watch监控zkRootPath下是否子节点变化
        watchZkRootNode() ;

        log.info("init zookeeper completed...");
    }

    private void initDefaultNode() {
        try {
            Stat stat = curatorFramework.checkExists().forPath(distributeLock);
            if (stat == null) {
                //创建锁节点
                curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(distributeLock);
            }
            stat = curatorFramework.checkExists().forPath(dataNodes) ;
            if (stat == null) {
                //创建数据节点
                curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(dataNodes);
            }
            log.info("init locknode and datanode completed...");
        } catch (Exception ex) {
            log.error("init locknode or datanode failed..., nested exception is ", ex);
        }
    }

    private void initServiceNode() throws Exception{
        List<String> children = curatorFramework.getChildren().forPath(dataNodes) ;
        for (String child : children) {
            String childPath = dataNodes + "/" + child ;
            byte[] data = curatorFramework.getData().forPath(childPath) ;
            ServiceNode serviceNode = JSONObject.parseObject(new String(data), ServiceNode.class) ;
            serviceNodePool.put(childPath, serviceNode) ;
        }
        log.info("init " + children.size() + " service node completed...");
    }

    private void watchZkRootNode() throws Exception {
        zkRootPathChildrenCache = new PathChildrenCache(curatorFramework, dataNodes, true) ;
        zkRootPathChildrenCache.start();
        PathChildrenCacheListener cacheListener = new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                //处理节点上线事件
                if (event.getType().toString().equals(PathChildrenCacheEvent.Type.CHILD_ADDED.toString())) {
                    log.info("A new node " + event.getData().getPath() + " has been created ...");
                    addCallBack(event) ;
                } else if (event.getType().toString().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED.toString())) {
                    delCallBack(event) ;
                    log.info("A node " + event.getData().getPath() + " has been deleted...");
                }
            }
        } ;
        zkRootPathChildrenCache.getListenable().addListener(cacheListener);
    }

    private void initSendQueue() {
        for (String key : serviceNodePool.keySet()) {
            LinkedBlockingQueue<DelayTask> addQ = new LinkedBlockingQueue<>(DefaultSendQueueMaxSize) ;
            tWheelAddQueues.put(key, addQ) ;
        }
    }

    class Forward2AddTimerWheelThread extends Thread {

        private LinkedBlockingQueue<DelayTask> addQ ;
        private ServiceNode node ;
        private volatile boolean TERMINATION = false ;

        Forward2AddTimerWheelThread (LinkedBlockingQueue<DelayTask> addQ, ServiceNode node, String name) {
            super(name) ;
            this.addQ = addQ ;
            this.node = node ;
        }

        @Override
        public void run() {
            int batch = Integer.parseInt(PropertiesUtil.getProperty(SENDBATCH)) ;
            String addr = node.getAddr() ;
            String host = addr.split(":")[0] ;
            int port = Integer.parseInt(addr.split(":")[1]) ;
            RpcClient rpcClient = new RpcClient(host, port) ;
            while (!TERMINATION) {
                List<DelayTask> delayTasks = popBackBy(addQ, batch) ;
                if (delayTasks.size() == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        log.error("A error occured in method forward2AddTWheelTask... , nested excption is ", ex);
                    }
                } else {
                    try {
                        rpcClient.send(transferData(delayTasks)) ;
                    } catch (Exception ex) {
                        log.error("sending delayTasks to timerwheel failed... , nested exception is ", ex);
                        new Thread(()-> pushFailedDelayTask2Queue(addQ, delayTasks)).start();
                    } finally{
                        delayTasks.clear();
                    }
                }
            }
            if (TERMINATION) {
                try {
                    rpcClient.shutdown();
                    log.info("rpc client shutdown...");
                } catch (InterruptedException ex) {
                    log.info("rpc client shutdown failed...");
                }
            }
        }

        public void cancel() {
            TERMINATION = true ;
        }
    }

    private List<DelayTaskModel> transferData(List<DelayTask> DelayTasks) {
        List<DelayTaskModel> delayTaskModels = new ArrayList<>() ;
        for (DelayTask model : DelayTasks) {
            DelayTaskModel task = DelayTaskModel.newBuilder()
                    .setKey(model.getKey()).setDelay(model.getDelay())
                    .setCircle(model.getCircle()).setMonitorRule(model.getMonitorRule())
                    .setProcessTime(model.getProcessTime()).setTimestamp(model.getTimestamp())
                    .setExpiredTime(model.getExpiredTime()).build() ;
            delayTaskModels.add(task) ;
        }
        return delayTaskModels ;
    }

    private void pushFailedDelayTask2Queue(LinkedBlockingQueue<DelayTask> addQ, List<DelayTask> tasks) {
        for (DelayTask task : tasks) {
            try {
                addQ.put(task);
            } catch (InterruptedException ex) {
                log.error("pushFailedDelayTask2Queue failed... , nested excption is ", ex);
            }
        }
    }

    private synchronized void addCallBack(PathChildrenCacheEvent event) {
        String path = event.getData().getPath() ;
        ServiceNode node = JSONObject.parseObject(event.getData().getData(), ServiceNode.class, Feature.OrderedField) ;
        //发送池中增加服务节点
        serviceNodePool.put(path, node) ;
        //增加新增服务节点对应的缓冲队列
        LinkedBlockingQueue<DelayTask> addQueue = new LinkedBlockingQueue<>() ;
        tWheelAddQueues.put(path, addQueue) ;
        //启动一个线程用于拉取数据到时间轮服务节点
        Forward2AddTimerWheelThread sendThread = new Forward2AddTimerWheelThread(addQueue, node, "sendThread-"+ path) ;
        sendThread.setDaemon(false);
        sendTaskThreadPool.put(path, sendThread) ;
        sendThread.start();
    }

    private synchronized void delCallBack(PathChildrenCacheEvent event) {
        String path = event.getData().getPath() ;
        //发送池中移除服务节点
        ServiceNode node = serviceNodePool.remove(path) ;
        //迁移这个服务节点缓存队列中的数据
        LinkedBlockingQueue<DelayTask> queue = tWheelAddQueues.remove(path) ;
        List<DelayTask> tasks = frontAll(queue) ;
        for (DelayTask task : tasks) {
            for (LinkedBlockingQueue<DelayTask> queueAfterDel : tWheelAddQueues.values()) {
                try {
                    queueAfterDel.put(task);
                } catch (InterruptedException ex) {
                    log.error("transfer data from sending queue failed... , nested excption is ", ex);
                }
            }
        }
        //中断发送任务的线程
        Forward2AddTimerWheelThread sendThread = sendTaskThreadPool.get(path) ;
        sendThread.cancel();
        log.info("send Thread " + sendThread.getName() + " has been stopped ....");
        //迁移这个时间轮服务节点存储在redis中的数据
        InterProcessMutex lock = null ;
        try {
            lock = new InterProcessMutex(curatorFramework, distributeLock);
            lock.acquire();
            transferRedisData(node.getName()) ;
        } catch (Exception ex) {
            log.error("transfer data from redis failed... , nested excption is ", ex);
        } finally {
            try {
                //释放锁
                if (lock.isAcquiredInThisProcess()) {
                    lock.release();
                }
            } catch (Exception ex) {
                log.error("A error occured in method delCallBack, nested exception is ", ex);
            }
        }
    }

    private void transferRedisData(String name) {
        //获取timerWheelId
        String timerWheelId = name.split("-")[1] ;
        String metaKey = "tw-" + timerWheelId + "-metadata" ;
        RedisUtil.hscan(metaKey, new RedisDataCallBackImpl());
        //删除这个时间轮中redis存储的数据
        String pattern = "tw-" + timerWheelId + "*" ;
        RedisUtil.delKeysByPattern(pattern);
    }

    private class RedisDataCallBackImpl implements RedisDataCallBack {
        @Override
        public void handleRedisData(Map<String, String> dataMap) {
            Map<String, Map<String, Object>> falconData = new HashMap<>() ;
            for (String taskKey : dataMap.keySet()) {
                if (taskKey.indexOf("timerwheel-") != -1) {
                    String taskData = RedisUtil.get(taskKey) ;
                    if (taskData == null) {
                        continue ;
                    }
                    byte[] decodeBytes = Base64.getDecoder().decode(taskData) ;
                    DelayTask task = JSONObject.parseObject(decodeBytes, DelayTask.class, Feature.OrderedField) ;
                    long lastTime = task.getExpiredTime() - System.currentTimeMillis()/1000 ;
                    //如果任务没有超时,重新计算dalayTime并发送到timerWheel
                    if (lastTime > 0) {
                        int delayTime = (int) (lastTime/1000/60) ;
                        task.setDelay(delayTime) ;
                        try {
                            push2AddTWheelQueueByOne(task);
                        } catch (InterruptedException ex) {
                            log.error("A error occured in method push2AddTWheelQueueByOne, nested exception is ", ex);
                        }
                    } else {
                        //可能时间轮服务down的时候有任务超时了
                        Map<String, Object> valueMap = new HashMap<>() ;
                        valueMap.put("monitorRule", task.getMonitorRule()) ;
                        valueMap.put("expiredTime", task.getExpiredTime()) ;
                        valueMap.put("timestamp", task.getTimestamp()) ;
                        valueMap.put("processTime", task.getProcessTime()) ;
                        falconData.put(taskKey, valueMap) ;
                    }
                }
            }
            if (falconData.size() > 0) {
                sendExpireTaskToFalcon(falconData);
            }
        }
    }

    private void sendExpireTaskToFalcon(Map<String, Map<String, Object>> falconData) {
        String jsonString = JSONObject.toJSONString(falconData) ;
        String url = PropertiesUtil.getProperty("falcon.url") ;
        String response = HttpUtil.httpPost(url, jsonString) ;
        log.info(response);
    }

    private List<DelayTask> popBackBy(LinkedBlockingQueue<DelayTask> queue, int max) {
        int count = queue.size() ;
        if (count == 0) {
            return new ArrayList<>() ;
        }
        if (count > max) {
            count = max ;
        }
        List<DelayTask> tasks = new ArrayList<>(count) ;
        for (int i = 0; i< count; i++) {
            tasks.add(queue.poll()) ;
        }
        return tasks ;
    }

    private List<DelayTask> frontAll(LinkedBlockingQueue<DelayTask> queue) {
        int count = queue.size() ;
        if (count == 0) {
            return new ArrayList<>() ;
        }
        List<DelayTask> tasks = new ArrayList<>(count) ;
        for (int i = 0; i< count; i++) {
            tasks.add(queue.poll()) ;
        }
        return tasks ;
    }
}
