package main

import (
	"errors"
	"fmt"
	"net/http"
	"os"
	"os/exec"
	"path"
	"strings"

	"github.com/gin-gonic/gin"
)

// relative path from $HOME
func gsqlPath() string {
	home, _ := os.UserHomeDir()
	return path.Join(home, ".gium", "gsql")
}

func runGSQL(stdin string, args ...string) (string, error) {
	// add --graphstudio to mute banners, warnings and other additional output
	argList := []string{}
	argList = append(argList, args...)
	cmd := exec.Command(gsqlPath(), argList...)
	pipe, _ := cmd.StdinPipe()
	pipe.Write([]byte(stdin))
	pipe.Close()
	outerr, err := cmd.CombinedOutput()
	return string(outerr), err
}

func execGSQL(username string, password string, graph *string, command string, stdin string) (string, error) {
	args := []string{
		"--graphstudio", // mute the banner and license warning etc
		"-u", username,
		"-p", password,
		"-c", command,
	}
	if graph != nil {
		args = append(args, "-g", *graph)
	}

	return runGSQL(stdin, args...)
}

type gsqlcmdReq struct {
	Graph   *string `json:"graph"`
	Command *string `json:"command"`
}

func (r *gsqlcmdReq) validate() error {
	if r.Command == nil {
		return fmt.Errorf("command field required")
	}
	return nil
}

func processOutput(stdout string, err error) (interface{}, error) {
	if err != nil {
		return stdout, err
	}
	if result := toJSON(stdout); result != nil {
		return result, nil
	}
	succPattern := []string{
		"is created",            // create graph/vertex/edge/user
		"is dropped",            // drop graph/vertex/edge/user
		"update to new version", // update schema
		"is successfully",       // grant/revoke role
		"has been added",        // add query
		"installing queries",    // no success pattern, can't catch failure for query installation
	}
	for _, p := range succPattern {
		if strings.Contains(stdout, p) {
			return stdout, nil
		}
	}
	return stdout, errors.New(stdout)
}

func gsqlcmdHandler(c *gin.Context) {
	req := gsqlcmdReq{}
	if err := c.ShouldBind(&req); err != nil {
		c.AbortWithStatusJSON(http.StatusBadRequest, response{
			Error:   true,
			Message: err.Error(),
		})
		return
	}
	if err := req.validate(); err != nil {
		c.AbortWithStatusJSON(http.StatusBadRequest, response{
			Error:   true,
			Message: err.Error(),
		})
		return
	}
	cred := getCredential(c)
	result, err := execGSQL(*cred.Username, *cred.Password, req.Graph, *req.Command, "")

	// if result is JSON, parse it and put it into `Result` field, otherwise,
	// put the stdout into `Message` field
	r, err := processOutput(result, err)
	msg := result
	if r != nil {
		msg = ""
	}
	c.JSON(200, response{
		Error:   err != nil,
		Message: msg,
		Result:  r,
	})
}
