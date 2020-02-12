package config

import (
	"encoding/json"
	"sync"

	logger "github.com/sirupsen/logrus"
	"github.com/toolkits/file"
)

type GlobalConfig struct {
	TimerWheelId int              `json:"timeWheelId"`
	ScheduleUrl  string           `json:"scheduleUrl"`
	Redis        *RedisConfig     `json:"redis"`
	Rpc          *RpcConfig       `json:"rpc"`
	Zookeeper    *ZookeeperConfig `json:"zookeeper"`
	LogFile      *LogFileConfig   `json:"log_file"`
}

type RedisConfig struct {
	Dsn          string `json:"dsn"`
	Type         string `json:"type"`
	MasterName   string `json:"masterName"`
	MasterPwd    string `json:"masterPwd"`
	MaxIdle      int    `json:"maxIdle"`
	MaxActive    int    `json:"maxActive"`
	IdleTimeout  int    `json:"idleTimeout"`
	ConnTimeout  int    `json:"connTimeout"`
	ReadTimeout  int    `json:"readTimeout"`
	WriteTimeout int    `json:"writeTimeout"`
}

type RpcConfig struct {
	Enabled bool   `json:"enabled"`
	Listen  string `json:"listen"`
}

type ZookeeperConfig struct {
	ZkServers string `json:"zkServers"`
	ZkRoot    string `json:"zkRoot"`
	Timeout   int    `json:"timeout"`
}

type LogFileConfig struct {
	LogPath      string `json:"log_path"`
	LogFileName  string `json:"log_file_name"`
	MaxAge       int    `json:"max_age"`
	RotationTime int    `json:"rotation_time"`
}

var (
	ConfigFile string
	config     *GlobalConfig
	configLock = new(sync.RWMutex)
)

func Config() *GlobalConfig {
	configLock.RLock()
	defer configLock.RUnlock()
	return config
}

func ParseConfig(cfg string) {

	if cfg == "" {
		logger.Fatalln("use -c to specify configuration file")
	}

	if !file.IsExist(cfg) {
		logger.Fatalln("config file:", cfg, "is not existent")
	}

	ConfigFile = cfg

	configContent, err := file.ToTrimString(cfg)
	if err != nil {
		logger.Fatalln("read config file:", cfg, "fail:", err)
	}

	var c GlobalConfig
	err = json.Unmarshal([]byte(configContent), &c)
	if err != nil {
		logger.Fatalln("parse config file:", cfg, "fail:", err)
	}

	configLock.Lock()
	defer configLock.Unlock()

	config = &c

	logger.Println("read config file:", cfg, "successfully")
}
