package main

import (
	"errors"
	"net/http"

	"github.com/gin-gonic/gin"
)

type dropUserReq struct {
	Username *string `json:"username"`
}

func (r *dropUserReq) validate() error {
	if r.Username == nil {
		return errors.New(`username field is required`)
	}
	return nil
}

func dropUserHandler(c *gin.Context) {
	req := dropUserReq{}
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
		"drop user "+*req.Username,
		"")

	_, err = processOutput(result, err)
	c.JSON(200, response{
		Error:   err != nil,
		Message: result,
	})
}
