package module

import (
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/gomodule/redigo/redis"
	logger "github.com/sirupsen/logrus"
	"net"
	"strconv"
	"time"
	"timerwheel/common/config"
	"timerwheel/common/error"
	"timerwheel/common/model"
	r "timerwheel/common/redis"
	"timerwheel/common/zookeeper"
	"timerwheel/core/g"
)

//向时间轮中发送定时任务的缓冲队列
var taskList = NewSafeTaskList()

var addTaskChannel = make(chan bool)
var stopChannel = make(chan bool)

type TimerWheel struct {
	TimeWheelId int                 // 时间轮id
	Addr        string              // 时间轮地址
	Interval    time.Duration       // 指针每隔多久往前移动一格
	Ticker      *time.Ticker        // clock组件
	Slots       []*Slot             // 时间轮槽
	SlotNum     int                 // 槽数量
	Metadata    model.ConcurrentMap // key: 定时任务唯一标识id   value: 定时任务所在的槽
	CurrentPos  int                 // 当前指针指向哪一个槽
}

// 创建一个时间轮
func New(timeWheelId int, interval time.Duration, slotNum int) *TimerWheel {
	if interval <= 0 || slotNum <= 0 {
		return nil
	}
	addr := config.Config().Rpc.Listen
	tcpAddr, err := net.ResolveTCPAddr("tcp", addr)
	if err != nil {
		logger.Fatalf("net.ResolveTCPAddr fail: %s", err)
	}
	tw := &TimerWheel{
		TimeWheelId: timeWheelId,
		Addr:        tcpAddr.String(),
		Interval:    interval,
		Slots:       make([]*Slot, slotNum),
		Metadata:    model.NewConcurrentMap(),
		CurrentPos:  0,
		SlotNum:     slotNum,
	}
	tw.initSlots()
	logger.Info("init timerWheel success...")
	return tw
}

// 初始化槽，每个槽指向一个redis的map结构
func (tw *TimerWheel) initSlots() {
	for i := 0; i < tw.SlotNum; i++ {
		tw.Slots[i] = &Slot{Key: "tw-" + strconv.Itoa(tw.TimeWheelId) + "-slot-" + strconv.Itoa(i)}
	}
}

// Start 启动时间轮
func (tw *TimerWheel) Start(zkClient *zookeeper.ZkClient) {
	tw.Ticker = time.NewTicker(tw.Interval)
	go tw.start(zkClient)
}

func (tw *TimerWheel) start(zkClient *zookeeper.ZkClient) {
	//注册zookeeper服务节点
	nodeName := fmt.Sprintf("timerWheel-%d", tw.TimeWheelId)
	node := &zookeeper.ServiceNode{
		Id:   tw.TimeWheelId,
		Name: nodeName,
		Addr: tw.Addr,
	}
	if err := zkClient.Register(node); err != nil {
		panic(err)
	}
	for {
		select {
		case <-addTaskChannel:
			tasks := taskList.PopAll()
			tw.addTask(tasks)
		default:
		}

		select {
		case <-tw.Ticker.C:
			tw.tickHandler()
		case <-stopChannel:
			tw.Ticker.Stop()
			return
		}
	}
}

// Stop 停止时间轮
func Stop() {
	stopChannel <- true
}

// AddTimerBatch 批量添加定时器 key为定时器唯一标识
func AddTimerBatch(tasks []*model.DelayTask) {
	if len(tasks) != 0 {
		taskList.PushAll(tasks)
		addTaskChannel <- true
	}
}

func (tw *TimerWheel) tickHandler() {
	//时间轮
	if tw.CurrentPos == tw.SlotNum-1 {
		tw.CurrentPos = 0
	} else {
		tw.CurrentPos++
	}
	//在redis上注册metadata数据
	tw.registerMetaData()
	slot := tw.Slots[tw.CurrentPos]
	//扫描Slot中的超时任务
	slot.scanTask(tw)
}

// 新增任务到slot中的集合
func (tw *TimerWheel) addTask(tasks []*model.DelayTask) {
	tw.addTaskBatch(tasks)
}

// 获取定时任务在槽中的位置, 时间轮需要转动的圈数
func (tw *TimerWheel) getPositionAndCircle(d time.Duration) (pos int, circle int) {
	delayTime := int(d.Seconds())
	intervalTime := int(tw.Interval.Seconds())
	circle = int(delayTime / intervalTime / tw.SlotNum)
	pos = int(tw.CurrentPos+delayTime/intervalTime) % tw.SlotNum
	return pos, circle
}

func (tw *TimerWheel) addTaskBatch(tasks []*model.DelayTask) {

	//可能传入的数据有错误，为了不让错误的数据down掉程序
	defer func() {
		if err := recover(); err != nil {
			if indexOutOfRangeError, ok := err.(*error.IndexOutOfRangeError); ok {
				logger.Error(fmt.Sprintf("%s\r\n", indexOutOfRangeError.Error()))
			} else {
				logger.Error(fmt.Sprintf("%v\r\n", err)) //输出panic信息
			}
		}
	}()

	rc := r.RedisPool.Get()
	defer rc.Close()

	pNewMap := make(map[int][]*model.DelayTask)
	for _, task := range tasks {
		task.ExpiredTime = time.Now().Unix() + int64(task.Delay*60*100)
		//写入redis的总存储中
		bit, err := json.Marshal(task)
		if err != nil {
			logger.Error("json marshal delayTask fail, error is", err.Error())
			return
		}
		base64Task := base64.StdEncoding.EncodeToString(bit)
		if _, errP := rc.Do("SET", task.Key, base64Task, (task.Delay+10)*60); errP != nil {
			logger.Errorf("Fail to AddTask at redis, redis key is %s", task.Key)
			return
		}
		//写入slot中
		var keyArray []*model.DelayTask
		pos, circle := tw.getPositionAndCircle(time.Duration(task.Delay) * time.Second * 60)
		if pos < 0 || pos > g.SLOT_NUM-1 {
			indexOutOfRangeError := &error.IndexOutOfRangeError{
				Num: pos,
				Err: errors.New(fmt.Sprintf("IndexOutOfRangeError--pos [%d] is invalid, please check the task[key=%s] ", pos, task.Key)),
			}
			panic(indexOutOfRangeError)
		}
		if pNewMap[pos] == nil {
			keyArray = append(keyArray, task)
			pNewMap[pos] = keyArray
		} else {
			pNewMap[pos] = append(pNewMap[pos], task)
		}
		task.Circle = int32(circle)
		if task.Key != "" {
			tw.Metadata.Set(task.Key, pos)
			logger.Infof("add task to timerwheel, key is %s, position in timerwheel-%d is %d", task.Key, tw.TimeWheelId, pos)
		}
	}
	for pos, tempTasks := range pNewMap {
		if len(tempTasks) != 0 {
			tw.Slots[pos].AddTask(tempTasks)
		}
	}
}

func (tw *TimerWheel) registerMetaData() {
	tw.Metadata.Set("currentPos", tw.CurrentPos)
	strInt64 := strconv.FormatInt(time.Now().Unix(), 10)
	nowTime, err := strconv.Atoi(strInt64)
	if err != nil {
		logger.Fatal("now time transfer from int64 error ....")
	}
	tw.Metadata.Set("tickTime", nowTime)
	rc := r.RedisPool.Get()
	defer rc.Close()
	key := "tw-" + strconv.Itoa(tw.TimeWheelId) + "-metadata"
	if _, err := rc.Do("HMSET", redis.Args{}.Add(key).AddFlat(tw.Metadata.Items())...); err != nil {
		logger.Error("Fail to registerMetaData at redis ...")
	}
}

func (tw *TimerWheel) deleteMetaData(key string) {
	rc := r.RedisPool.Get()
	defer rc.Close()
	_, errP := rc.Do("HDEL", "tw-"+strconv.Itoa(tw.TimeWheelId)+"-metadata", key)
	if errP != nil {
		logger.Error("Fail to deleteMetaData at redis ...")
	}
}
