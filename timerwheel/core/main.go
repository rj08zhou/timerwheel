package main

import (
	"flag"
	"fmt"
	_ "github.com/mkevac/debugcharts"
	"net/http"
	_ "net/http/pprof"
	"os"
	"os/signal"
	"syscall"
	"time"
	"timerwheel/common/config"
	logger "timerwheel/common/log"
	"timerwheel/common/redis"
	zk "timerwheel/common/zookeeper"
	"timerwheel/core/cron"
	"timerwheel/core/g"
	"timerwheel/core/module"
	"timerwheel/core/rpc"
)

func main() {

	cfg := flag.String("c", "cfg.json", "configuration file")
	flag.Parse()
	//加载配置文件
	config.ParseConfig(*cfg)
	//config.ParseConfig("E:\\go-code\\src\\timerwheel\\modules\\core\\cfg.json")
	logger.InitLogConfig()
	//初始化redis连接池
	redis.InitRedisPool()

	//初始化zookeeper连接
	zkClient := zk.InitZookeeper()
	defer zkClient.Close()

	//创建时间轮
	timerWheelId := config.Config().TimerWheelId
	tw := module.New(timerWheelId, 1*time.Second, g.SLOT_NUM)
	tw.Start(zkClient)

	go rpc.Start()
	go cron.CheckZkConn(tw, zkClient)

	//开启性能监控展示界面
	go func() {
		ip := "0.0.0.0:6060"
		if err := http.ListenAndServe(ip, nil); err != nil {
			fmt.Printf("start pprof failed on %s\n", ip)
		}
	}()

	//优雅地shutdown
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, syscall.SIGINT, syscall.SIGTERM)
	go func() {
		<-signals
		//关闭时间轮
		redis.RedisPool.Close()
		zkClient.Close()
		module.Stop()
		os.Exit(0)
	}()

	select {}
}
