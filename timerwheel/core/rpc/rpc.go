package rpc

import (
	logger "github.com/sirupsen/logrus"
	"google.golang.org/grpc"
	"net"
	"timerwheel/common/config"
	"timerwheel/common/model"
)

func Start() {
	if !config.Config().Rpc.Enabled {
		return
	}
	addr := config.Config().Rpc.Listen
	tcpAddr, err := net.ResolveTCPAddr("tcp", addr)
	if err != nil {
		logger.Fatalf("net.ResolveTCPAddr fail: %s", err)
	}

	listener, err := net.ListenTCP("tcp", tcpAddr)
	if err != nil {
		logger.Fatalf("listen %s fail: %s", addr, err)
	} else {
		logger.Println("rpc listening", addr)
	}

	s := grpc.NewServer()
	model.RegisterTimerWheelServiceServer(s, &TWheel{})

	s.Serve(listener)
}
