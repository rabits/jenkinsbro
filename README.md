Jenkins Master Automation
=========================

CentOS-7 jenkins master and JenkinsBRO configuration framework based on init.groovy.d hooks.

Inspired by: https://github.com/Praqma/JenkinsAsCodeReference

Configuration as a code
-----------------------

Main goal is getting a better way to configure jenkins master and make
possible to test configuration without touching production jenkins servers.

Each user should be able to deploy prod-like jenkins and simple test their
changes. Also configuration CI should be able to do the same with running
test cases.

Jenkins Master
--------------

Dockerfile is based on centos-7 and provides next functionality:
* Choose jenkins version
* Install described plugins with required dependencies (plugins.txt file)
* Place JenkinsBRO framework and connect it by init.groovy
* Set build version for CI or local testing

JenkinsBRO
----------

Simple scripts that unify configuration and makes possible to divide
configuration logic into helpers, interfaces and modules. It uses simple
config files based on groovy ConfigSlurper.

## Configs

Right now configs contains 2 conf files: secrets.conf & jenkins.conf.
Secrets should be separated from the main configuration, that's why it's a
separated file. You could use secrets in the jenkins.conf by `secrets.<prop.path>`.

You can set 2 docker arguments / env variables to change default config paths:
* JENKINSBRO_SECRETS_PATH = $JENKINS_HOME/config/secrets.conf
* JENKINSBRO_CONFIGS_PATH = $JENKINS_HOME/config/jenkins.conf

Config is used by modules to get values - so get the available values you need to
go inside the module and check it, or use templates from the /jenkinsBro/config dir.

Modules are executed in the configuration order - so make sure, that configuration
is consistent and dependencies are in place. If there is no module with required name
It will be skipped with warning message. Not configured modules also will be skipped
with info message.

## Helpers & interfaces

Common rules for each helper/interface:

* One file - one helper, flat parsing (without subdirs)
* File names allowed: "[A-Za-z0-9]+.groovy"
* Name of the file is the name of the main function
* Could use global CONFIG while execution

### Helpers

Helpers placed in the /jenkinsBro/lib/helpers directory.
JenkinsBRO parsed helpers first and attach main functions to the helpers
binding to use it later in interfaces & modules via `helpers.<functionName>`.

### Interfaces

Interfaces placed in the /jenkinsBro/lib/interfaces directory.
They could use helpers to simplify an interface logic. Will be binded
to the execution shell with names of functions - so in the modules you can
use interfaces just by calling `<functionName>`.

## Modules

Modules is slightly different - it's not a functions inside the file like
helpers or interfaces. Modules actually executed as groovy scripts and can use
helpers & interfaces binded methods to simplify inside logic. While executing
module jenkins changes his configuration or doing other amazing things.

If module will fail with exception - you will see errors in the jenkins log
and failed module execution will be aborted.

Module could use self configuration via MODULE binding and global configuration
by CONFIG binding.

Testing
-------

To test this project you need to setup docker on your local. After that
copy templates /jenkinsBro/config to /config and modify it as you wish.

Now you should be ready to run your local jenkins master:
```
$ docker build -t jenkins-master .
$ docker run --name jenkins --rm -it -p 8085:8080 -p 50005:50000 jenkins-master
```
After that you can connect to the http://localhost:8085/ and check the configuration

If you would like to mount your local directory to save the jenkins data:
```
$ docker build -t jenkins-master .
$ mkdir ~/.jenkins; chown 1000 ~/.jenkins ; cp -a config ~/.jenkins
$ docker run --name jenkins --rm -it -v ~/.jenkins:/var/jenkins_home:z -p 8085:8080 -p 50005:50000 jenkins-master
```
