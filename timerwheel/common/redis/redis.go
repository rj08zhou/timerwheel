package redis

import (
	"errors"
	"strings"
	"time"

	"github.com/FZambia/sentinel"
	"github.com/gomodule/redigo/redis"
	logger "github.com/sirupsen/logrus"
	cfg "timerwheel/common/config"
)

var RedisPool *redis.Pool

func InitRedisPool() error {

	dsn := cfg.Config().Redis.Dsn
	maxIdle := cfg.Config().Redis.MaxIdle
	idleTimeout := 240 * time.Second

	connTimeout := time.Duration(cfg.Config().Redis.ConnTimeout) * time.Millisecond
	readTimeout := time.Duration(cfg.Config().Redis.ReadTimeout) * time.Millisecond
	writeTimeout := time.Duration(cfg.Config().Redis.WriteTimeout) * time.Millisecond

	Type := cfg.Config().Redis.Type

	if Type == "sentinel" {
		masterName := cfg.Config().Redis.MasterName
		masterPwd := cfg.Config().Redis.MasterPwd
		dsnArr := strings.Split(dsn, ",")
		if masterName == "" {
			return errors.New("masterName or masterPwd is not input")
		}

		sntnl := &sentinel.Sentinel{
			Addrs:      dsnArr,
			MasterName: masterName,
			Dial: func(addr string) (redis.Conn, error) {
				c, err := redis.Dial("tcp", addr, redis.DialConnectTimeout(connTimeout),
					redis.DialReadTimeout(readTimeout), redis.DialWriteTimeout(writeTimeout))
				if err != nil {
					return nil, err
				}
				return c, nil
			},
		}

		RedisPool = &redis.Pool{
			MaxIdle:     cfg.Config().Redis.MaxIdle,
			MaxActive:   cfg.Config().Redis.MaxActive,
			Wait:        true,
			IdleTimeout: time.Duration(cfg.Config().Redis.IdleTimeout) * time.Second,
			Dial: func() (redis.Conn, error) {
				masterAddr, err := sntnl.MasterAddr()
				if err != nil {
					return nil, err
				}
				c, err := redis.Dial("tcp", masterAddr)
				if err != nil {
					return nil, err
				}

				if masterPwd != "" {
					if _, err := c.Do("AUTH", masterPwd); err != nil {
						c.Close()
						return nil, err
					}
				}

				return c, nil
			},
			TestOnBorrow: func(c redis.Conn, t time.Time) error {
				if !sentinel.TestRole(c, "master") {
					return errors.New("Role check failed")
				} else {
					return nil
				}
			},
		}

	} else {
		RedisPool = &redis.Pool{
			MaxIdle:     maxIdle,
			IdleTimeout: idleTimeout,
			Dial: func() (redis.Conn, error) {
				c, err := redis.Dial("tcp", dsn, redis.DialConnectTimeout(connTimeout),
					redis.DialReadTimeout(readTimeout), redis.DialWriteTimeout(writeTimeout))
				if err != nil {
					return nil, err
				}
				return c, err
			},
			TestOnBorrow: PingRedis,
		}
	}
	return nil
}

func PingRedis(c redis.Conn, t time.Time) error {
	_, err := c.Do("ping")
	if err != nil {
		logger.Println("[ERROR] ping redis fail", err)
	}
	return err
}
