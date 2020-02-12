package cmd

import (
	"fmt"
	"github.com/spf13/cobra"
	"os"
	"os/exec"
	"path/filepath"
)

var Monitor = &cobra.Command{
	Use:           "monitor timerWheel",
	Short:         "monitor timerWheel",
	RunE:          monitor,
	SilenceUsage:  true,
	SilenceErrors: true,
}

func monitor(c *cobra.Command, args []string) error {
	if len(args) < 1 {
		return c.Usage()
	}
	//检查参数数量
	args, err := CheckArgs(args)
	if err != nil {
		return err
	}
	//检查参数是否合法
	argument := args[0]
	if err := checkMonReq(argument); err != nil {
		return err
	}
	var tailArgs = []string{"-f", "./logs/timerWheel.log"}
	cmd := exec.Command("tail", tailArgs...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func checkMonReq(name string) error {
	if name != "timerWheel" {
		return fmt.Errorf("%s command doesn't exist", name)
	}

	logPath, _ := filepath.Abs("./logs/timerWheel.log")
	if !HasLogfile(logPath) {
		cfgName := "./config/cfg.json"
		r := Rel(cfgName)
		return fmt.Errorf("expect logfile: %s", r)
	}

	return nil
}
