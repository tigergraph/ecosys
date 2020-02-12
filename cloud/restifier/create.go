package main

import (
	"errors"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
)

type createUserReq struct {
	Username *string `json:"username"`
	Password *string `json:"password"`
}

func (r *createUserReq) validate() error {
	if r.Username == nil {
		return errors.New(`username field is required`)
	} else if r.Password == nil {
		return errors.New("password field is required")
	}
	return nil
}

func cleanCreateUserOutput(stdout string) string {
	lines := strings.Split(stdout, "\n")
	if len(lines) < 4 {
		return stdout
	}
	return strings.Join(lines[3:], "\n")
}

func createUserHandler(c *gin.Context) {
	req := createUserReq{}
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
	result, err := execGSQL(
		*cred.Username,
		*cred.Password,
		nil,
		"create user",
		strings.Join([]string{
			*req.Username,
			*req.Password,
			*req.Password,
		}, "\n")+"\n")
	result = cleanCreateUserOutput(result)

	_, err = processOutput(result, err)
	c.JSON(200, response{
		Error:   err != nil,
		Message: result,
	})
}
