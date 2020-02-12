package main

import (
	"encoding/base64"
	"fmt"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
)

const ctxKeyNameCredential = "Credential"

func getCredential(c *gin.Context) credential {
	cred, _ := c.Get(ctxKeyNameCredential)
	result, _ := cred.(credential)
	return result
}

func authHandler(c *gin.Context) {
	cred := parseCredential(c.Request.Header.Get("authorization"))
	c.Set(ctxKeyNameCredential, cred)
	r, _ := http.NewRequest("GET", "http://localhost:8123/gsql/simpleauth", nil)
	r.Header.Set("authorization", c.Request.Header.Get("authorization"))
	client := &http.Client{}
	resp, err := client.Do(r)
	if err != nil {
		c.AbortWithStatusJSON(http.StatusInternalServerError, response{
			Error:   true,
			Message: err.Error(),
		})
		return
	}
	if resp.StatusCode == http.StatusUnauthorized {
		c.AbortWithStatusJSON(http.StatusBadRequest, response{
			Error:   true,
			Message: "invalid username/password combination",
		})
		return
	}
}

type credential struct {
	Username *string `json:"username"`
	Password *string `json:"password"`
}

func (r *credential) validate() error {
	if r.Username == nil {
		return fmt.Errorf("username field required")
	} else if r.Password == nil {
		return fmt.Errorf("password field required")
	}
	return nil
}

func parseCredential(authInfo string) credential {
	auth := strings.SplitN(authInfo, " ", 2)
	var result credential
	if len(auth) != 2 || auth[0] != "Basic" {
		return result
	}

	payload, _ := base64.StdEncoding.DecodeString(auth[1])
	pair := strings.SplitN(string(payload), ":", 2)

	if len(pair) == 2 {
		result.Username = &pair[0]
		result.Password = &pair[1]
	}
	return result
}
