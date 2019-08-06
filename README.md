Jenkins Master Automation
=========================

Alpine jenkins master and JenkinsBRO configuration framework based on init.groovy.d hooks.

Inspired by: https://github.com/Praqma/JenkinsAsCodeReference

## Configuration as a code

Main goal is getting a better way to configure jenkins master and make
possible to test configuration without touching production jenkins servers.

Each user should be able to deploy prod-like jenkins and simple test its
changes. Also configuration CI should be able to do the same with running
test cases.

## Jenkins Master

Dockerfile is based on alpine linux and provides next functionality:
* Choose jenkins version
* Install described plugins with required dependencies (plugins.txt file)
* Place JenkinsBRO framework and connect it via init.groovy
* Set build version for CI or local testing

## JenkinsBRO

Framework to unify the configuration and make possible to separate configs into
helpers, interfaces and modules. It supports ConfigSlurper and JSON config files.

### Configuration

Config is used by modules to get values - to get the available values you need to
go inside the module and check it, or use templates from the `examples` dir.

Modules are executed in the configuration order - so make sure, that configuration
is consistent and dependencies are in place. If there is no module with required name
It will be skipped with warning message. Not configured modules also will be skipped
with info message.

By default the jenkinsbro configs will be removed right after parsing (to prevent
reapply on restart and some security issues). You can control the behavior by
`delete_configs_file=false` and `delete_secrets_file=false`.

#### ConfigSlurper config

ConfigSlurper format supports an additional secrets.conf where you can put the creds
that you don't want to store with the other public available configuration. It will be
available in the jenkins.conf as a binding, which will be accessible as `secrets.<path>`.

Another advantage of the conf files - they can use the environment variables by `env.VAR`
right in the configuration - it's also binded by jenkinsBro.

You can set 2 docker arguments / env variables to change default config paths:
* JENKINSBRO_SECRETS_PATH = $JENKINS_HOME/config/secrets.conf
* JENKINSBRO_CONFIGS_PATH = $JENKINS_HOME/config/jenkins.conf

#### JSON config

JSON config must have extention `.json`. It could be generated by your automation - so
it's just a simple file with all the values (no templating).
* JENKINSBRO_CONFIGS_PATH = $JENKINS_HOME/config/jenkins.json

### Helpers & interfaces

Common rules for each helper/interface:

* One file - one helper, flat parsing (without subdirs)
* File names allowed: "[A-Za-z0-9]+.groovy"
* Name of the file is the name of the main function
* Could use global CONFIG while execution

#### Helpers

Helpers placed in the `jenkinsBro/lib/helpers` directory.
JenkinsBRO parsed helpers first and attach main functions to the helpers
binding to use it later in interfaces & modules via `helpers.<functionName>`.

#### Interfaces

Interfaces placed in the `jenkinsBro/lib/interfaces` directory.
They could use helpers to simplify an interface logic. Will be binded
to the execution shell with names of functions - so in the modules you can
use interfaces just by calling `<functionName>`.

### Modules

Modules is slightly different - it's not a functions inside the file like
helpers or interfaces. Modules actually executed as groovy scripts and can use
helpers & interfaces binded methods to simplify inside logic. While executing
module jenkins changes his configuration or doing other amazing things.

If module will fail with exception - you will see errors in the jenkins log
and failed module execution will be aborted.

Module could use self configuration via MODULE binding and global configuration
by CONFIG binding.

#### Unit/Integration Tests

Special module `tests` could be used for the unit or integration testing of your
jenkins configuration, jobs or jenkins web interface. Tests will be started
when jenkins configuration will be completed and it will be ready to use, so you
will be able to execute anything you want.

You can use this module for both - unit testing and integration testing of the
existing instances to make sure they are working properly. But please be carefull:
`power -> responsibility`...

Tests supports 2 modes:
* Configured - test class with `@Module('name')` annotation will be started with
the existing module configuration or skipped if no module configuration was found.
You can override configs in the `tests.modules` config section.
* Standalone - if test class doesn't have `@Module('name')` annotation will be
started as a usual static test.

Tests will generate junit xml reports - so you will be able to convert them to a
nice reports.

Test examples you can find in directory `example/tests` - you just need to put
them (or yours) into `jenkinsBro/tests` directory.

## Advanced usage

### Grape to get dependencies

Grape is available in tests and modules - so you can use it to get your requirements:
```
@Grab(group='org.yaml', module='snakeyaml', version='1.24')

import org.yaml.snakeyaml.Yaml

def yaml = new Yaml();
println(yaml.dump([asd: 3, bbb: 5]))
```

Check `example/tests/WebLoginTest.groovy` to get some clue how to use grape for testing.
Also you can check the documentation: http://docs.groovy-lang.org/latest/html/documentation/grape.html

## How to use

To test this project you need to setup docker on your local. After that
place config exapmples `example/config/*.conf` to `config` dir in the repo root
and modify them as you wish.

Now you should be ready to run your local jenkins master:
```
$ docker build -t jenkinsbro-master .
$ docker run --name jenkins --rm -it -p 8085:8080 -p 50005:50000 jenkinsbro-master
```
After that you can connect to the http://localhost:8085/ and check the configuration

If you would like to mount your local directory to save the jenkins data:
```
$ docker build -t jenkinsbro-master .
$ mkdir ~/.jenkins; chown 1000 ~/.jenkins ; cp -a config ~/.jenkins
$ docker run --name jenkins --rm -it -v ~/.jenkins:/var/jenkins_home:z -p 8085:8080 -p 50005:50000 jenkinsbro-master
```
