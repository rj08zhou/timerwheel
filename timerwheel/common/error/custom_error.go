package error

type IndexOutOfRangeError struct {
	Num int
	Err error
}

func (err *IndexOutOfRangeError) Error() string {
	return err.Err.Error()
}
