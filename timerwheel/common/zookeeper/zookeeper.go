package zookeeper

import (
	"encoding/json"
	"github.com/samuel/go-zookeeper/zk"
	logger "github.com/sirupsen/logrus"
	"strings"
	"sync"
	"time"
	cfg "timerwheel/common/config"
)

//timerWheel注册到zookeeper上的节点
type ServiceNode struct {
	Id   int    `json:"id"`
	Name string `json:"name"`
	Addr string `json:"addr"`
}

//zookeeper客户端
type ZkClient struct {
	ZkServers []string
	ZkRoot    string
	Conn      *zk.Conn
}

type ServiceNodePool struct {
	NodeMap map[string]*ServiceNode
	sync.RWMutex
}

func (pool *ServiceNodePool) AddServiceNode(key string, node *ServiceNode) {
	pool.Lock()
	defer pool.Unlock()
	pool.NodeMap[key] = node
}

func (pool *ServiceNodePool) RemoveServiceNode(key string) *ServiceNode {
	pool.Lock()
	defer pool.Unlock()
	node := pool.NodeMap[key]
	delete(pool.NodeMap, key)
	return node
}

func (pool *ServiceNodePool) DiscoverServiceNodes() map[string]*ServiceNode {
	pool.RLock()
	defer pool.RUnlock()
	return pool.NodeMap
}

//新建zookeeper客户端
func newZkClient(zkServers []string, zkRoot string, timeout int) (*ZkClient, error) {
	client := new(ZkClient)
	client.ZkServers = zkServers
	client.ZkRoot = zkRoot
	conn, _, err := zk.Connect(zkServers, time.Duration(timeout)*time.Second)
	if err != nil {
		return nil, err
	}
	client.Conn = conn
	if err := client.ensureRoot(); err != nil {
		client.Close()
		return nil, err
	}
	return client, nil
}

func (client *ZkClient) ensureRoot() error {
	exists, _, err := client.Conn.Exists(client.ZkRoot)
	if err != nil {
		return err
	}
	if !exists {
		_, err := client.Conn.Create(client.ZkRoot, []byte(""), 0, zk.WorldACL(zk.PermAll))
		if err != nil && err != zk.ErrNodeExists {
			return err
		}
	}
	return nil
}

func (client *ZkClient) ExistsPath(path string) (bool, error) {
	exists, _, err := client.Conn.Exists(path)
	if err != nil {
		return false, err
	}
	return exists, nil
}

func (client *ZkClient) Close() {
	client.Conn.Close()
}

func (client *ZkClient) Register(node *ServiceNode) error {
	exists, _, err := client.Conn.Exists(client.ZkRoot + "/data")
	if err != nil {
		return err
	}
	if !exists {
		_, err := client.Conn.Create(client.ZkRoot+"/data", []byte(""), 0, zk.WorldACL(zk.PermAll))
		if err != nil && err != zk.ErrNodeExists {
			return err
		}
	}
	path := client.ZkRoot + "/data/" + node.Name
	data, err := json.Marshal(node)
	if err != nil {
		return err
	}
	_, err = client.Conn.Create(path, data, zk.FlagEphemeral, zk.WorldACL(zk.PermAll))
	if err != nil {
		return err
	}
	logger.Infof("register a node, path is [%s], data is [%s]", path, data)

	return nil
}

//初始化zookeeper连接
func InitZookeeper() *ZkClient {
	zkServers := cfg.Config().Zookeeper.ZkServers
	var zkServerArr = strings.Split(zkServers, ",")
	var zkRoot = cfg.Config().Zookeeper.ZkRoot
	timeout := cfg.Config().Zookeeper.Timeout
	zkClient, err := newZkClient(zkServerArr, zkRoot, timeout)
	if err != nil {
		logger.Fatalf("connection to zookeeper failed ..., err is", err)
	}
	return zkClient
}
