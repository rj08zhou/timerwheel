package model

import "sync"

//concurrentMap 采用分段锁解决map在分布式环境下的读写冲突问题

//concurrentMap分区数
var SHARD_COUNT int = 32

//concurrentMap由不同分区下的concurrentMapShard数组组成
//存储的数据根据key值分配到不同的concurrentMapShard上去
type ConcurrentMap []*ConcurrentMapShard

type ConcurrentMapShard struct {
	item map[string]interface{}
	sync.RWMutex
}

func NewConcurrentMap() ConcurrentMap {
	m := make(ConcurrentMap, SHARD_COUNT)
	for i := 0; i < SHARD_COUNT; i++ {
		m[i] = &ConcurrentMapShard{
			item: make(map[string]interface{}),
		}
	}
	return m
}

//根据key值分配该key在哪个shard上
func (m ConcurrentMap) GetShard(key string) *ConcurrentMapShard {
	mask := SHARD_COUNT - 1
	return m[uint(fnv32(key))&uint(mask)]
}

func (m ConcurrentMap) Set(key string, value interface{}) {
	shard := m.GetShard(key)
	shard.Lock()
	defer shard.Unlock()
	shard.item[key] = value
}

func (m ConcurrentMap) Get(key string) (interface{}, bool) {
	shard := m.GetShard(key)
	shard.RLock()
	defer shard.RUnlock()
	value, ok := shard.item[key]
	return value, ok
}

func (m ConcurrentMap) Len() int {
	count := 0
	for i := 0; i <= SHARD_COUNT; i++ {
		shard := m[i]
		shard.RLock()
		count += len(shard.item)
		shard.RUnlock()
	}
	return count
}

func (m ConcurrentMap) Delete(key string) {
	shard := m.GetShard(key)
	shard.Lock()
	defer shard.Unlock()
	delete(shard.item, key)
}

type Tuple struct {
	key   string
	value interface{}
}

func (m ConcurrentMap) Iterator() <-chan Tuple {
	chans := snapshot(m)
	total := 0
	for _, c := range chans {
		total += cap(c)
	}
	ch := make(chan Tuple, total)
	go flatPeek(chans, ch)
	return ch
}

func (m ConcurrentMap) Items() map[string]interface{} {
	tmp := make(map[string]interface{})
	for item := range m.Iterator() {
		tmp[item.key] = item.value
	}
	return tmp
}

func flatPeek(chans []chan Tuple, out chan Tuple) {
	wg := sync.WaitGroup{}
	wg.Add(len(chans))
	for _, ch := range chans {
		go func(ch chan Tuple) {
			for t := range ch {
				out <- t
			}
			wg.Done()
		}(ch)
	}
	wg.Wait()
	close(out)
}

func snapshot(m ConcurrentMap) (chans []chan Tuple) {
	chans = make([]chan Tuple, SHARD_COUNT)
	wg := sync.WaitGroup{}
	wg.Add(SHARD_COUNT)
	for index, shard := range m {
		go func(index int, shard *ConcurrentMapShard) {
			shard.RLock()
			chans[index] = make(chan Tuple, len(shard.item))
			wg.Done()
			for key, value := range shard.item {
				chans[index] <- Tuple{key, value}
			}
			shard.RUnlock()
			close(chans[index])
		}(index, shard)
	}
	wg.Wait()
	return chans
}

func fnv32(key string) uint32 {
	hash := uint32(2166136261)
	const prime32 = uint32(16777619)
	for i := 0; i < len(key); i++ {
		hash *= prime32
		hash ^= uint32(key[i])
	}
	return hash
}
