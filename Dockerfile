FROM openjdk:8-jdk-alpine

# Unzip required to install plugins
RUN apk add --no-cache git openssh-client curl unzip bash ttf-dejavu coreutils tini

ARG JENKINS_VERSION=2.176.2
ARG JENKINS_URL=https://updates.jenkins-ci.org/download/war/${JENKINS_VERSION}/jenkins.war
# Check sha256 here: http://mirrors.jenkins.io/war-stable/
ARG JENKINS_SHA=33a6c3161cf8de9c8729fd83914d781319fd1569acf487c7b1121681dba190a5

ARG JAVA_OPTS

ARG user=jenkins
ARG group=jenkins
ARG uid=1000
ARG gid=1000
ARG http_port=8080
ARG agent_port=50000
ARG plugins_txt_path=plugins.txt
ARG jenkins_home=/var/jenkins_home


ENV JENKINS_HOME=${jenkins_home} \
    JENKINS_VERSION=${JENKINS_VERSION} \
    JENKINS_SLAVE_AGENT_PORT=${agent_port} \
    JENKINS_UC=https://updates.jenkins.io \
    JENKINS_UC_EXPERIMENTAL=https://updates.jenkins.io/experimental \
    JENKINS_INCREMENTALS_REPO_MIRROR=https://repo.jenkins-ci.org/incrementals \
    JAVA_OPTS="-Djenkins.install.runSetupWizard=false ${JAVA_OPTS:-}" \
    COPY_REFERENCE_FILE_LOG=${jenkins_home}/copy_reference_file.log

# Jenkins is run with user `jenkins`, uid = 1000
# If you bind mount a volume from the host or a data container, 
# ensure you use the same uid
RUN addgroup -g ${gid} ${group} \
  && adduser -h "${JENKINS_HOME}" -u ${uid} -G ${group} -D -s /bin/bash ${user}

# Jenkins home directory is a volume, so configuration and build history 
# can be persisted and survive image upgrades
VOLUME ${JENKINS_HOME}

# Precreating jenkins directory
RUN mkdir -p /usr/share/jenkins/ref/init.groovy.d

# could use ADD but this one does not check Last-Modified header neither does it allow to control checksum 
# see https://github.com/docker/docker/issues/8331
RUN echo "${JENKINS_SHA} -" > sum.txt && curl -fLs "${JENKINS_URL}" | tee /usr/share/jenkins/jenkins.war | sha256sum -c sum.txt \
  && rm -f sum.txt

RUN chown -R ${user} "$JENKINS_HOME" /usr/share/jenkins/ref

# for main web interface:
EXPOSE ${http_port}

# will be used by attached slave agents:
EXPOSE ${agent_port}

USER ${user}

COPY jenkins-support /usr/local/bin/jenkins-support
COPY jenkins.sh /usr/local/bin/jenkins.sh

# Installing the required plugins
COPY install-plugins.sh /usr/local/bin/install-plugins.sh
COPY ${plugins_txt_path} /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

# Define build argument and env variable master_image_version
# Used to pass info about Jenkins version so it can be represented
# for the users.
ARG JENKINS_BUILD_VERSION="local build"
ENV JENKINS_BUILD_VERSION ${JENKINS_BUILD_VERSION}

# Where to find the config files for the JenkinsBRO
ARG JENKINSBRO_SECRETS_PATH=${JENKINS_HOME}/config/secrets.conf
ARG JENKINSBRO_CONFIGS_PATH=${JENKINS_HOME}/config/jenkins.conf
ENV JENKINSBRO_SECRETS_PATH ${JENKINSBRO_SECRETS_PATH}
ENV JENKINSBRO_CONFIGS_PATH ${JENKINSBRO_CONFIGS_PATH}

# Copy groovy scripts that will be executed by jenkins during start
COPY jenkinsBro /usr/share/jenkins/ref/jenkinsBro
COPY init.groovy.d /usr/share/jenkins/ref/init.groovy.d

ENTRYPOINT ["/sbin/tini", "--", "/usr/local/bin/jenkins.sh"]
