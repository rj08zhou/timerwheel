package log

import (
	"github.com/lestrrat/go-file-rotatelogs"
	"github.com/pkg/errors"
	"github.com/rifflock/lfshook"
	logger "github.com/sirupsen/logrus"
	"os"
	"path"
	"time"
	"timerwheel/common/config"
)

func InitLogConfig() {
	logger.SetFormatter(&logger.TextFormatter{
		ForceColors:               true,
		EnvironmentOverrideColors: true,
		TimestampFormat:           "2006-01-02 15:04:05",
	})
	logger.SetReportCaller(true)
	logger.SetOutput(os.Stdout)
	logger.SetLevel(logger.InfoLevel)
	logPath := config.Config().LogFile.LogPath
	logFileName := config.Config().LogFile.LogFileName
	maxAge := config.Config().LogFile.MaxAge
	rotationTime := config.Config().LogFile.RotationTime
	registerLfsLoggerHook(logPath, logFileName, time.Duration(maxAge)*time.Hour*24, time.Duration(rotationTime)*time.Second)
}

//register a file rotate hoop
func registerLfsLoggerHook(logPath string, logFileName string, maxAge time.Duration, rotationTime time.Duration) {
	baseLogPath := path.Join(logPath, logFileName)
	writer, err := rotatelogs.New(baseLogPath+".%Y%m%d%H%M",
		rotatelogs.WithLinkName(baseLogPath),
		rotatelogs.WithMaxAge(maxAge),
		rotatelogs.WithRotationTime(rotationTime))
	if err != nil {
		logger.Errorf("register local file system logger hook error. %+v", errors.WithStack(err))
	}
	hook := lfshook.NewHook(lfshook.WriterMap{
		logger.DebugLevel: writer,
		logger.InfoLevel:  writer,
		logger.WarnLevel:  writer,
		logger.ErrorLevel: writer,
		logger.FatalLevel: writer,
		logger.PanicLevel: writer,
	}, &logger.TextFormatter{DisableColors: true})

	logger.AddHook(hook)
}
