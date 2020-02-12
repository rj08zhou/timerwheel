package model

import (
	"container/list"
	"sync"
)

type SafeLinkedList struct {
	sync.RWMutex
	L *list.List
}

func NewSafeLinkedList() *SafeLinkedList {
	return &SafeLinkedList{L: list.New()}
}

func (this *SafeLinkedList) PushFront(v interface{}) *list.Element {
	this.Lock()
	defer this.Unlock()
	return this.L.PushFront(v)
}

func (this *SafeLinkedList) Front() *list.Element {
	this.RLock()
	defer this.RUnlock()
	return this.L.Front()
}

func (this *SafeLinkedList) PopBack() *list.Element {
	this.Lock()
	defer this.Unlock()

	back := this.L.Back()
	if back != nil {
		this.L.Remove(back)
	}

	return back
}

func (this *SafeLinkedList) Back() *list.Element {
	this.Lock()
	defer this.Unlock()

	return this.L.Back()
}

func (this *SafeLinkedList) Len() int {
	this.RLock()
	defer this.RUnlock()
	return this.L.Len()
}
