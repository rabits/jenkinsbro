FROM centos:centos7

RUN curl -sLo jdk.rpm --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
  "http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-linux-x64.rpm" && \
  rpm -i jdk.rpm && rm -f jdk.rpm

# Unzip required to install plugins
RUN yum install -y unzip && yum clean all

ARG user=jenkins
ARG group=jenkins
ARG uid=1000
ARG gid=1000
ARG http_port=8080
ARG agent_port=50000

ENV JENKINS_HOME /var/jenkins_home
ENV JENKINS_SLAVE_AGENT_PORT ${agent_port}

# Jenkins is run with user `jenkins`, uid = 1000
# If you bind mount a volume from the host or a data container, 
# ensure you use the same uid
RUN groupadd -g ${gid} ${group} \
    && useradd -d "$JENKINS_HOME" -u ${uid} -g ${gid} -m -s /bin/bash ${user}

# Jenkins home directory is a volume, so configuration and build history 
# can be persisted and survive image upgrades
VOLUME /var/jenkins_home

# Precreating jenkins directory
RUN mkdir -p /usr/share/jenkins/ref/init.groovy.d

ENV TINI_VERSION 0.14.0
ENV TINI_SHA 6c41ec7d33e857d4779f14d9c74924cab0c7973485d2972419a3b7c7620ff5fd

# Use tini as subreaper in Docker container to adopt zombie processes 
RUN curl -fsSL https://github.com/krallin/tini/releases/download/v${TINI_VERSION}/tini-static-amd64 -o /bin/tini && chmod +x /bin/tini \
  && echo "$TINI_SHA  /bin/tini" | sha256sum -c -

# jenkins version being bundled in this docker image
ARG JENKINS_VERSION
ENV JENKINS_VERSION ${JENKINS_VERSION:-2.74}

# jenkins.war checksum, download will be validated using it
# Check sha256 here: http://mirrors.jenkins.io/war/
ARG JENKINS_SHA=9f37140d0ccb6a9aa7c3c8a522a79b37bd1123edddca0e7f8f6dd579ffe8cb86

# Can be used to customize where jenkins.war get downloaded from
ARG JENKINS_URL=https://updates.jenkins-ci.org/download/war/${JENKINS_VERSION}/jenkins.war

# could use ADD but this one does not check Last-Modified header neither does it allow to control checksum 
# see https://github.com/docker/docker/issues/8331
RUN curl -fsSL ${JENKINS_URL} -o /usr/share/jenkins/jenkins.war \
  && echo "${JENKINS_SHA}  /usr/share/jenkins/jenkins.war" | sha256sum -c -

ENV JENKINS_UC https://updates.jenkins.io
ENV JENKINS_UC_EXPERIMENTAL=https://updates.jenkins.io/experimental
RUN chown -R ${user} "$JENKINS_HOME" /usr/share/jenkins/ref

# for main web interface:
EXPOSE ${http_port}

# will be used by attached slave agents:
EXPOSE ${agent_port}

ENV COPY_REFERENCE_FILE_LOG $JENKINS_HOME/copy_reference_file.log

USER ${user}

COPY jenkins-support /usr/local/bin/jenkins-support
COPY jenkins.sh /usr/local/bin/jenkins.sh
ENTRYPOINT ["/bin/tini", "--", "/usr/local/bin/jenkins.sh"]

COPY install-plugins.sh /usr/local/bin/install-plugins.sh

# Disabling setup wizard
ARG JAVA_OPTS
ENV JAVA_OPTS "-Djenkins.install.runSetupWizard=false ${JAVA_OPTS:-}"

ARG PLUGINS_TXT_PATH=plugins.txt

# Copy configuration scripts that will be executed by groovy
COPY jenkinsBro /usr/share/jenkins/ref/jenkinsBro
COPY init.groovy.d /usr/share/jenkins/ref/init.groovy.d
COPY config ${JENKINS_HOME}/config

# Installing required plugins
COPY ${PLUGINS_TXT_PATH} /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

# Define build argument and env variable master_image_version
# Used to pass info about Jenkins version so it can be represented
# for the users.
ARG JENKINS_BUILD_VERSION="local build"
ENV JENKINS_BUILD_VERSION ${JENKINS_BUILD_VERSION}

# Where to find the config files for the JenkinsBRO
ARG JENKINSBRO_SECRETS_PATH=${JENKINS_HOME}/config/secrets.conf
ENV JENKINSBRO_SECRETS_PATH ${JENKINSBRO_SECRETS_PATH}
ARG JENKINSBRO_CONFIGS_PATH=${JENKINS_HOME}/config/jenkins.conf
ENV JENKINSBRO_CONFIGS_PATH ${JENKINSBRO_CONFIGS_PATH}
