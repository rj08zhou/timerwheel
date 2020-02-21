# timerwheel
A distributed timerwheel system used for monitoring scheduled tasks...

## 分布式调度任务超时监控系统

### 时间轮介绍

时间轮方案中将现实中的时间概念引入，定义一个时钟周期(比如时钟的60分钟)和步长(比如时钟指针一秒走一次)，需要计时的任务根据超时的时间分布在不同的时钟刻度上。当时钟指针每走一步时，获取当前时钟刻度上挂在的任务并执行。时间轮的结构一般如下图所示:

![timerWheel](https://img-blog.csdnimg.cn/20200221151549634.jpg)

从上图看到，对于时间的计算交给类似时钟的组件做，而任务则是通过一个指针或者引用去关联某个刻度上到期的定时任务，这样就能将定时任务的存储和时间进行解耦。

### 数据存储

时间轮方案中任务数据的存储是关键。可以依托专门的存储服务如HBase,Redis来进行存储。这里为了不过多地引入其它组件增加系统系统复杂性，采用了redis来存储
数据。如下图所示:

<a href="url"><img src="https://img-blog.csdnimg.cn/2020022115153236.jpg" align="center" height="350" width="550" ></a>

定时任务存储在redis的hash结构中，存储数据的命令可为 hmset key <dataKey, dateValue>。这里的key指的是时间轮槽位(刻度)的值，即每个时间轮的槽位在
redis中都有一个对应的hash结构存储。datakey和dataValue可以是落位在这个槽中的定时任务的存储数据。

### 时间轮可靠性

在数据过多的情况下，只是使用单个时间轮可能导致时间轮某些刻度上存储的任务过多，造成定时任务处理延时较大。同时，如果时间轮本身出现意外无法提供服务后，重启
后改时间轮的数据也需要恢复，否则会造成定时任务处理遗漏或者重复处理。
这里采用了时间轮的集群来实现可靠性。每个时间轮维护自己一个元数据(metadata),里面可以记录当前指针刻度，当前任务key值等用于恢复时间轮意外down掉的信息。
同时利用zookeeper分布式协调服务实现一个服务注册发现的过程，通过zookeeper的watcher机制来控制时间轮本身的自动扩容缩容以及意外恢复。如下图所示:

![discovery](https://img-blog.csdnimg.cn/20200221151519541.jpg)

具体流程如下：

*  启用一个时间轮网关timerwheel-gateway(时间轮网关)模块，模块中维护一个时间轮的可用服务对象池。
*  时间轮上线启动时向zookeeper注册服务，同时zookeeper对注册的时间轮节点进行监控watch node。同时上面的服务对象池中记录该上线时间轮服务的地址。当任务
   生产者发送数据时遍历服务池采用相应的对策(如round robin)进行发送。
*  时间轮管理模块会根据zookeepeer watch node的事件对时间轮进行可用性管理。


### 总体架构图

![whole_structure](https://img-blog.csdnimg.cn/2020022115161437.jpg)


### 部署和使用

*  timerwheel-gateway(已改名timerwheelAdmin) <br/>
   打包：maven clean package -Pdev  <br/>
   解压缩后，bin目录中直接执行相应启动停止脚本即可。

*  timerwheel <br/>
   /timerwheel/core 下 go build -o timerWheel-core main.go  ===> timerWheel-core <br/>
   /timerwheel 下      go build -o twAdmin main.go          ===> twAdmin
   
*  timerwheel目录结构  <br/>
   |---timerWheel-service <br/>
     &emsp; |---bin<br/>
       &emsp; &emsp; |---timerWheel-core<br/>
     &emsp; |---config<br/>
       &emsp; &emsp; |---cfg.json<br/>
     &emsp; |---logs<br/>
       &emsp; &emsp; |---timerWheel.log<br/>
     &emsp; wAdmin

*  timerwheel使用    <br/>
   ./twAdmin start   timerWheel  <br/>
   ./twAdmin stop    timerWheel  <br/>
   ./twAdmin monitor timerWheel  <br/>
   ./twAdmin check   timerWheel  <br/>
