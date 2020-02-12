package cmd

import (
	"fmt"
	"github.com/spf13/cobra"
	"os"
	"os/exec"
	"path/filepath"
	"time"
)

var Start = &cobra.Command{
	Use:           "start timerWheel",
	Short:         "start timerWheel",
	RunE:          start,
	SilenceUsage:  true,
	SilenceErrors: true,
}

func start(c *cobra.Command, args []string) error {
	//检查参数数量
	fmt.Println("start checking args...")
	args, err := CheckArgs(args)
	if err != nil {
		return err
	}
	//检查参数是否合法
	argument := args[0]
	if err := checkStartReq(argument); err != nil {
		return err
	}
	//检查是否已经启动
	fmt.Println("start checking progress status...")
	progressName := "timerWheel-core"
	if IsRunning(progressName) {
		fmt.Print("[", progressName, "] ", Pid(progressName), "\n")
		return nil
	}
	//启动
	fmt.Println("start starting progress...")
	if err := execProgram(); err != nil {
		return err
	}
	fmt.Println("start checking if start...")
	//检查是否启动
	if isStarted(progressName) {
		fmt.Print("[", progressName, "] ", Pid(progressName), "\n")
		return nil
	}
	return fmt.Errorf("[%s] failed to start", argument)
}

func checkStartReq(name string) error {
	if name != "timerWheel" {
		return fmt.Errorf("%s command doesn't exist", name)
	}
	cfgName := "./config/cfg.json"
	if !HasCfg(cfgName) {
		r := Rel(cfgName)
		return fmt.Errorf("expect config file: %s", r)
	}
	return nil
}

func execProgram() error {
	cfgPath, _ := filepath.Abs("./config/cfg.json")
	cmd := exec.Command("./bin/timerWheel-core", "-c", cfgPath)
	logOutput, err := openLogFile()
	if err != nil {
		return err
	}
	defer logOutput.Close()
	cmd.Stdout = logOutput
	cmd.Stderr = logOutput
	return cmd.Start()
}

func openLogFile() (*os.File, error) {
	logDir, _ := filepath.Abs(filepath.Dir("./logs/timerWheel.log"))
	if err := os.MkdirAll(logDir, 0755); err != nil {
		return nil, err
	}
	logPath, _ := filepath.Abs("./logs/timerWheel.log")
	logOutput, err := os.OpenFile(logPath, os.O_CREATE|os.O_RDWR|os.O_APPEND, 0666)
	if err != nil {
		return nil, err
	}
	return logOutput, nil
}

func isStarted(name string) bool {
	ticker := time.NewTicker(time.Millisecond * 100)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			if IsRunning(name) {
				return true
			}
		case <-time.After(time.Second):
			return false
		}
	}
}
