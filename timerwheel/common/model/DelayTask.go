package model

type DelayTask struct {
	Key         string
	Delay       int32
	Circle      int32
	MonitorRule string
	ProcessTime int64
	Timestamp   int64
	ExpiredTime int64
}
