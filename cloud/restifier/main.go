package main

import (
	"github.com/Jeffail/gabs"
	"github.com/gin-gonic/gin"
)

type response struct {
	Error   bool        `json:"error"`
	Message string      `json:"message"`
	Result  interface{} `json:"result"`
}

func toJSON(stdout string) interface{} {
	result, err := gabs.ParseJSON([]byte(stdout))
	if err != nil {
		return nil
	}
	return result.Data()
}

func main() {
	r := gin.Default()
	r.Use(authHandler)

	api := r.Group("/api")
	{
		api.POST("/gsqlcmd", gsqlcmdHandler)
	}

	user := api.Group("/user")
	{
		user.GET("/profile", profileHandler)
		user.POST("/create", createUserHandler)
		user.POST("/drop", dropUserHandler)
		user.POST("/grant", grantUserHandler)
		user.POST("/revoke", revokeUserHandler)
	}
	r.Run() // listen and serve on 0.0.0.0:8080 (for windows "localhost:8080")
}
