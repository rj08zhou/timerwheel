package zookeeper

import (
	"github.com/samuel/go-zookeeper/zk"
	logger "github.com/sirupsen/logrus"
)

type Watch struct{}

func (this *Watch) ZkNodeWatch(c *zk.Conn, path string) {
	for {
		_, _, getCh, err := c.ExistsW(path)
		if err != nil {
			logger.Info(err)
		}
		for {
			select {
			case chEvent := <-getCh:
				{
					if chEvent.Type == zk.EventNodeCreated {
						logger.Infof("has new node[%s] create\n", chEvent.Path)
					} else if chEvent.Type == zk.EventNodeDeleted {
						logger.Infof("has node[%s] detete\n", chEvent.Path)
					} else if chEvent.Type == zk.EventNodeChildrenChanged {
						logger.Infof("has node[%s] changed\n", chEvent.Path)
					}
				}
			}
			break
		}
	}
}

func (this *Watch) ZkChildrenWatch(c *zk.Conn, path string) {
	for {
		_, _, getCh, err := c.ChildrenW(path)
		if err != nil {
			logger.Errorf("children watch failed, error is %s", err)
		}
		for {
			select {
			case chEvent := <-getCh:
				{
					if chEvent.Type == zk.EventNodeCreated {
						logger.Infof("has new node[%s] create\n", chEvent.Path)
					} else if chEvent.Type == zk.EventNodeDeleted {
						logger.Infof("has node[%s] detete\n", chEvent.Path)
					} else if chEvent.Type == zk.EventNodeChildrenChanged {
						logger.Infof("has node[%s] changed\n", chEvent.Path)
					}
				}
			}
			break
		}
	}
}
