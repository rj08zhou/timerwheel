package cron

import (
	logger "github.com/sirupsen/logrus"
	"strconv"
	"time"
	"timerwheel/common/zookeeper"
	"timerwheel/core/module"
)

func CheckZkConn(tw *module.TimerWheel, zkClient *zookeeper.ZkClient) {
	for {
		time.Sleep(time.Minute * 1)
		checkZkConn(tw, zkClient)
	}
}

func checkZkConn(tw *module.TimerWheel, zkClient *zookeeper.ZkClient) {
	nodeName := "timerWheel-" + strconv.Itoa(tw.TimeWheelId)
	path := zkClient.ZkRoot + "/data/" + nodeName
	logger.Infof("start checking timerWheel-[%d] registration status... , path is [%s]", tw.TimeWheelId, path)
	exists, err := zkClient.ExistsPath(path)
	if !exists && err == nil {
		node := &zookeeper.ServiceNode{
			Id:   tw.TimeWheelId,
			Name: nodeName,
			Addr: tw.Addr,
		}
		if err := zkClient.Register(node); err != nil {
			panic(err)
		}
		logger.Infof("timerWheel-[%d] has been reRegistered... , path is [%s]", tw.TimeWheelId, path)
	}

}
