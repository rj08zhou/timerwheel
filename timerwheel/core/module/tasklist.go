package module

import (
	"container/list"
	"timerwheel/common/model"
)

type TaskList model.SafeLinkedList

func NewSafeTaskList() *TaskList {
	return &TaskList{L: list.New()}
}

func (this *TaskList) PopAll() []*model.DelayTask {
	this.Lock()
	defer this.Unlock()

	size := this.L.Len()
	if size == 0 {
		return []*model.DelayTask{}
	}
	ret := make([]*model.DelayTask, 0, size)
	for i := 0; i < size; i++ {
		item := this.L.Back()
		ret = append(ret, item.Value.(*model.DelayTask))
		this.L.Remove(item)
	}
	return ret
}

// PushAll
func (this *TaskList) PushAll(items []*model.DelayTask) {
	this.Lock()
	defer this.Unlock()

	size := len(items)
	if size > 0 {
		for i := size - 1; i >= 0; i-- {
			this.L.PushBack(items[i])
		}
	}
}

func (this *TaskList) FetchAll() []*model.DelayTask {
	this.Lock()
	defer this.Unlock()
	count := this.L.Len()
	ret := make([]*model.DelayTask, 0, count)
	p := this.L.Back()
	for p != nil {
		ret = append(ret, p.Value.(*model.DelayTask))
		p = p.Prev()
	}

	return ret
}
