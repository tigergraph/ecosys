package main

import (
	"io/ioutil"
	"net/http"

	"github.com/Jeffail/gabs"
	"github.com/gin-gonic/gin"
)

func profileHandler(c *gin.Context) {
	r, _ := http.NewRequest("GET", "http://localhost:8123/gsql/simpleauth", nil)
	r.Header.Set("authorization", c.Request.Header.Get("authorization"))
	client := &http.Client{}
	resp, err := client.Do(r)
	if err != nil {
		c.AbortWithStatusJSON(http.StatusBadRequest, response{
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
	buf, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		c.AbortWithStatusJSON(http.StatusBadRequest, response{
			Error:   true,
			Message: err.Error(),
		})
		return
	}
	jsonResp, err := gabs.ParseJSON(buf)
	if err != nil {
		c.AbortWithStatusJSON(http.StatusBadRequest, response{
			Error:   true,
			Message: err.Error(),
		})
		return
	}
	c.JSON(200, response{
		Error:  err != nil,
		Result: jsonResp.Data(),
	})
}
