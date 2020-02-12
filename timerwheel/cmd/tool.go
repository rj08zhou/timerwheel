package cmd

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
)

func CheckArgs(args []string) ([]string, error) {
	length := len(args)
	if length == 1 {
		return args, nil
	}
	return nil, fmt.Errorf(" Args length [%d] is inValid ", length)
}

func HasCfg(name string) bool {
	cfgPath, _ := filepath.Abs(name)
	if _, err := os.Stat(cfgPath); err != nil {
		return false
	}
	return true
}

func HasLogfile(name string) bool {
	if _, err := os.Stat(name); err != nil {
		return false
	}
	return true
}

func Rel(p string) string {
	wd, err := os.Getwd()
	if err != nil {
		return ""
	}
	abs, _ := filepath.Abs(p)
	r, err := filepath.Rel(wd, abs)
	if err != nil {
		return ""
	}
	return r
}

func IsRunning(name string) bool {
	return Pid(name) != ""
}

func Pid(name string) string {
	output, _ := exec.Command("pgrep", "-f", name).Output()
	pidStr := strings.TrimSpace(string(output))
	return pidStr
}
