package libcore

import (
	"net"
	"path/filepath"
	"time"

	"libcore/vario"

	"github.com/sagernet/sing/common"
	E "github.com/sagernet/sing/common/exceptions"
)

type Client struct {
	conn *net.UnixConn
}

func NewClient() (*Client, error) {
	var (
		conn *net.UnixConn
		err  error
	)
	for i := range 10 {
		conn, err = dialUnix(apiPath())
		if err == nil {
			break
		}
		time.Sleep(time.Duration(100+i*50) * time.Millisecond)
	}
	if err != nil {
		return nil, E.Cause(err, "dial unix")
	}
	return &Client{conn: conn}, nil
}

func (c *Client) Close() error {
	return common.Close(c.conn)
}

func HasAPIInstance(internalAssets string) string {
	path := filepath.Join(internalAssets, Socket)
	exist := hasAPIInstance(path)
	if !exist {
		return ""
	}
	return path
}

func hasAPIInstance(path string) bool {
	conn, err := dialUnix(path)
	if err != nil {
		return false
	}
	defer conn.Close()
	err = vario.WriteUint8(conn, commandPing)
	if err != nil {
		return false
	}
	result, err := vario.ReadUint8(conn)
	if err != nil {
		return false
	}
	return result == resultNoError
}

func dialUnix(path string) (*net.UnixConn, error) {
	return net.DialUnix("unix", nil, &net.UnixAddr{Name: path, Net: "unix"})
}
