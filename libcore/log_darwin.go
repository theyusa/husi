package libcore

import (
	"errors"

	"golang.org/x/sys/unix"
)

func dup(oldFd, newFd, flags int) error {
	if flags != 0 {
		return errors.New("dup flags are unsupported on this platform")
	}
	return unix.Dup2(oldFd, newFd)
}

func flock(fd int) (unlock func() error) {
	_ = unix.Flock(fd, unix.LOCK_EX)
	return func() error {
		return unix.Flock(fd, unix.LOCK_UN)
	}
}
