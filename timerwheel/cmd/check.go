package cmd

import (
	"fmt"
	"github.com/spf13/cobra"
)

var Check = &cobra.Command{
	Use:           "check timerWheel",
	Short:         "check timerWheel",
	RunE:          check,
	SilenceUsage:  true,
	SilenceErrors: true,
}

func check(c *cobra.Command, args []string) error {
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
	progressName := "timerWheel-core"
	if IsRunning(progressName) {
		fmt.Printf("%20s %10s %15s \n", argument, "UP", Pid(progressName))
	} else {
		fmt.Printf("%20s %10s %15s \n", argument, "DOWN", "-")
	}
	return nil
}
