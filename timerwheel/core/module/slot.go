package module

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/gomodule/redigo/redis"
	logger "github.com/sirupsen/logrus"
	"io/ioutil"
	"net/http"
	"strconv"
	"strings"
	cfg "timerwheel/common/config"
	"timerwheel/common/model"
	r "timerwheel/common/redis"
)

/**
Slot存储了当前时间下超时的任务、
任务以哈希表的形式存储在集中存储redis中
*/
type Slot struct {
	Key string //redis 集合的key
}

//在slot上新增超时任务
func (slot *Slot) AddTask(tasks []*model.DelayTask) {
	storage4Redis := make(map[string]string)
	if len(tasks) == 0 {
		return
	}
	for _, task := range tasks {
		bit, err := json.Marshal(task)
		if err != nil {
			logger.Error("json marshal delayTask fail, error is", err.Error())
			return
		}
		//base64编码减少redis中value的大小
		base64Task := base64.StdEncoding.EncodeToString(bit)
		storage4Redis[task.Key] = base64Task
	}
	rc := r.RedisPool.Get()
	defer rc.Close()
	key := slot.Key
	if _, errP := rc.Do("HMSET", redis.Args{}.Add(key).AddFlat(storage4Redis)...); errP != nil {
		logger.Errorf("Fail to AddTask at redis, redis key is %s", key)
	} else {
		logger.Debugf("Success to AddTask at redis, redis key is %s", key)
	}

}

func (slot *Slot) scanTask(tw *TimerWheel) {
	rc := r.RedisPool.Get()
	defer rc.Close()
	//hscan增量式迭代器
	cursor := 0
	for {
		reply, err := redis.Values(rc.Do("HSCAN", slot.Key, cursor))
		if err != nil {
			logger.Errorf("redis HSCAN slot error..., %s", err.Error())
			return
		}
		cursor, err = redis.Int(reply[0], nil)
		taskMap, err := redis.StringMap(reply[1], nil)
		if err != nil {
			logger.Error("redis stringMap error... ", err.Error())
			return
		}
		if taskMap == nil || len(taskMap) == 0 {
			break
		}
		//启动一个协程来处理
		go slot.handleTask(taskMap, tw)
		if cursor == 0 {
			break
		}
	}

}

func (slot *Slot) handleTask(taskMap map[string]string, tw *TimerWheel) {
	rc := r.RedisPool.Get()
	defer rc.Close()
	//需要删除的数据
	var key2delete []string
	//需要批量发送到新网关的数据
	falconData := make(map[string]map[string]interface{})
	for key, value := range taskMap {
		delayTask := model.DelayTask{}
		//value需要base64反编码
		decodeBytes, err := base64.StdEncoding.DecodeString(value)
		if err != nil {
			logger.Errorf("decode base64 value error, value is %s", value)
			continue
		}
		json.Unmarshal(decodeBytes, &delayTask)
		if delayTask.Circle > 0 { //如果大于一圈时间轮
			delayTask.Circle--
			bit, err := json.Marshal(delayTask)
			if err != nil {
				logger.Error("json unmarshal delayTask fail, error is", err.Error())
				return
			}
			base64Task := base64.StdEncoding.EncodeToString(bit)
			_, errP := rc.Do("HSET", slot.Key, key, base64Task)
			if errP != nil {
				logger.Errorf("Fail to write at redis,key is %s,delayTask.key=%s,delayTask=%s", slot.Key, key, base64Task)
			}
		} else { //判断超时
			reply, err := rc.Do("GET", key)
			if err != nil {
				logger.Errorf("Fail to get key,key is %s, err is %v", key, err)
				continue
			}
			if reply != nil {
				storageValue, err := redis.String(reply, err)
				if err != nil {
					logger.Errorf("Fail to stringfy the reply, reply is %s, err is %v", key, err)
					continue
				}
				if key != "" && storageValue == value { //超时条件
					key2delete = append(key2delete, key)
					valueMap := make(map[string]interface{})
					valueMap["monitorRule"] = delayTask.MonitorRule
					valueMap["expiredTime"] = delayTask.ExpiredTime
					valueMap["timestamp"] = delayTask.Timestamp
					valueMap["processTime"] = delayTask.ProcessTime
					orignKey := strings.TrimPrefix(key, "timerwheel-")
					falconData[orignKey] = valueMap
					//删除内存中metadata的数据
					tw.Metadata.Delete(key)
				} else {
					//没超时的这个slot中的数据也要删除
					key2delete = append(key2delete, key)
				}
			} else {
				logger.Infof("the key[%s] in slot[%s] has expired by redis ....", key, slot.Key)
				//处理redis中删除的数据
				if key != "" {
					key2delete = append(key2delete, key)
					//删除内存中metadata的数据
					tw.Metadata.Delete(key)
				}
			}
		}
	}
	if len(key2delete) != 0 {
		//删除slot中的数据
		if _, errP := rc.Do("HDEL", redis.Args{}.Add(slot.Key).AddFlat(key2delete)...); errP != nil {
			logger.Errorf("Fail to del at redis,key is %s, err is %s", slot.Key, errP.Error())
			return
		}
		//删除metadata中的数据
		metakey := "tw-" + strconv.Itoa(tw.TimeWheelId) + "-metadata"
		if _, errP := rc.Do("HDEL", redis.Args{}.Add(metakey).AddFlat(key2delete)...); errP != nil {
			logger.Errorf("Fail to deleteMetaData at redis ..., key2delete length is %d, err is %s", len(key2delete), errP.Error())
			return
		}
	}
	if len(falconData) != 0 {
		logger.Infof("====>sendToFalcon, success, total: %d", len(falconData))
		go DelayCallback(falconData)
		for key, value := range falconData {
			logger.Infof("delay task key is %s, task info is %v", key, value)
		}
	}

}

//sendToFalcon 超时数据发送至open-falcon进行处理
func DelayCallback(falconData map[string]map[string]interface{}) {
	defer panicHandler()
	//最大可重试
	maxRetry := 3
	ok := false
	taskStr, err := json.Marshal(falconData)
	if err == nil {
		url := cfg.Config().ScheduleUrl
		reqPost, err := http.NewRequest("POST", url, bytes.NewBuffer([]byte(taskStr)))
		if err != nil {
			logger.Error("Error =", err.Error())
			return
		}
		reqPost.Header.Set("Content-Type", "application/json")
		client := &http.Client{}
		for tryCount := 0; tryCount < maxRetry && !ok; tryCount++ {
			resp, err := client.Do(reqPost)
			if err != nil {
				logger.Error("Error =", err.Error())
			} else {
				body, _ := ioutil.ReadAll(resp.Body)
				responseData := make(map[string]interface{})
				json.Unmarshal(body, &responseData)
				if responseData["success"] == true {
					ok = true
					break
				}
			}
			resp.Body.Close()
		}
		if !ok {
			logger.Error("====>fail to send falcon after " + strconv.Itoa(maxRetry) + " retry")
		} else {
			logger.Infof("====>sendToFalcon, success, total: %d", len(falconData))
		}
	} else {
		logger.Error("marshal falconData failed ", err.Error())
	}
}

func panicHandler() {
	if err := recover(); err != nil {
		logger.Error(fmt.Sprintf("%v\r\n", err)) //输出panic信息
	}
}
