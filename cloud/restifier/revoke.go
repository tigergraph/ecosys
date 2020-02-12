package main

import (
	"errors"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
)

type revokeUserReq struct {
	Users []string `json:"users"`
	Graph *string  `json:"graph"`
	Role  *string  `json:"role"`
}

func (r *revokeUserReq) validate() error {
	if len(r.Users) == 0 {
		return errors.New(`at least one user is required`)
	}
	if r.Role == nil {
		return errors.New(`role field is required`)
	}
	if r.Graph == nil && *r.Role != "superuser" {
		return errors.New(`graph field can be empty only if role is "superuser"`)
	}
	return nil
}

func revokeUserHandler(c *gin.Context) {
	req := revokeUserReq{}
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
	cliToks := []string{"revoke", "role", *req.Role}
	if req.Graph != nil {
		cliToks = append(cliToks, "on", "graph", *req.Graph)
	}
	cliToks = append(cliToks, "from")
	cliToks = append(cliToks, strings.Join(req.Users, ","))
	result, err := execGSQL(
		*cred.Username,
		*cred.Password,
		nil,
		strings.Join(cliToks, " "),
		"")

	_, err = processOutput(result, err)
	c.JSON(200, response{
		Error:   err != nil,
		Message: result,
	})
}
