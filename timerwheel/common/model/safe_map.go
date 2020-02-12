package model

import "sync"

type SafeMap struct {
	sync.RWMutex
	M map[string]string
}

func NewSafeMap() *SafeMap {
	return &SafeMap{M: make(map[string]string)}
}

func (this *SafeMap) Get(key string) (string, bool) {
	this.RLock()
	defer this.RUnlock()
	value, ok := this.M[key]
	return value, ok
}

func (this *SafeMap) Set(key string, value string) {
	this.Lock()
	defer this.Unlock()
	this.M[key] = value
}

func (this *SafeMap) Len() int {
	this.RLock()
	defer this.RUnlock()
	return len(this.M)
}

func (this *SafeMap) Delete(key string) {
	this.Lock()
	defer this.Unlock()
	delete(this.M, key)
}

func (this *SafeMap) BatchDelete(keys []string) {
	count := len(keys)
	if count == 0 {
		return
	}
	this.Lock()
	defer this.Unlock()
	for i := 0; i < count; i++ {
		delete(this.M, keys[i])
	}
}
