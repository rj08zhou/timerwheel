package main

import (
	"fmt"
	"github.com/spf13/cobra"
	"os"
	"timerwheel/cmd"
)

var RootCmd = &cobra.Command{
	Use: "twAdmin",
	RunE: func(c *cobra.Command, args []string) error {
		return c.Usage()
	},
}

func init() {
	RootCmd.AddCommand(cmd.Start)
	RootCmd.AddCommand(cmd.Stop)
	RootCmd.AddCommand(cmd.Monitor)
	RootCmd.AddCommand(cmd.Check)
}

func main() {
	if err := RootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
