# TigerGraph 2.x Management API Server


## Introduction

This tool is used to provide restful API for TigerGraph system management, including create/drop user, grant/revoke user role, create/update/drop vertex/edge/graph.

This is a reference implementation, and it is not guaranteed to be up-to-dated or production-ready.

## Usage
### Build

Go 1.13 is required.

Run `go build -o server` to build a server executable

### Run

run `PORT=<port> ./server` to serve at given port on **m1** (the first node in the cluster).


### API


#### Authentication

The API server uses [basic authentication](https://en.wikipedia.org/wiki/Basic_access_authentication).

Refer to `example.sh` for more details.


#### Endpoints

* `POST /api/gsqlcmd`: execute gsql commands
* `GET /api/user/profile`: get user profile, `isSuperUser`, roles on graphs, etc
* `POST /api/user/create`: create user
* `POST /api/user/drop`: drop user
* `POST /api/user/grant`: grant role to users
* `POST /api/user/revoke`: revoke role from users

For usage, please refer to `example.sh`


## Copyright

Every file in the same directory of this README.md for this specific tool is in public domain.