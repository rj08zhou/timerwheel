package cmd

import (
	"fmt"
	"github.com/spf13/cobra"
	"os"
	"os/exec"
)

var Stop = &cobra.Command{
	Use:           "stop timerWheel",
	Short:         "stop timerWheel",
	RunE:          stop,
	SilenceUsage:  true,
	SilenceErrors: true,
}

func stop(c *cobra.Command, args []string) error {
	//检查参数数量
	args, err := CheckArgs(args)
	if err != nil {
		return err
	}
	//检查参数是否合法
	argument := args[0]
	if argument != "timerWheel" {
		return fmt.Errorf("%s command doesn't exist", argument)
	}
	//kill -15
	progressName := "timerWheel-core"
	cmd := exec.Command("kill", "-TERM", Pid(progressName))
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err = cmd.Run()
	if err == nil {
		fmt.Print("[", argument, "] down\n")
		return err
	}
	return nil
}
