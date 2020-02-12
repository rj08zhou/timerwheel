package rpc

import (
	"context"
	"timerwheel/common/model"
	"timerwheel/core/module"
)

type TWheel struct{}

func (server *TWheel) Send(ctx context.Context, request *model.AddTaskRequest) (*model.AddTaskResponse, error) {
	module.AddTimerBatch(transferData(request.DelayTasks))
	response := &model.AddTaskResponse{
		Code: 1,
	}
	return response, nil
}

func transferData(tasks []*model.DelayTaskModel) []*model.DelayTask {
	var delayTasks []*model.DelayTask
	for _, task := range tasks {
		delayTask := &model.DelayTask{
			Key:         task.Key,
			Delay:       task.Delay,
			Circle:      task.Circle,
			MonitorRule: task.MonitorRule,
			ProcessTime: task.ProcessTime,
			Timestamp:   task.Timestamp,
			ExpiredTime: task.ExpiredTime,
		}
		delayTasks = append(delayTasks, delayTask)
	}
	return delayTasks
}
