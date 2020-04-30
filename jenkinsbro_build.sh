#!/bin/sh
#
# Script to simplify build of the lts/latest versions
#
# Usage:
#   $ ./jenkinsbro_build.sh [VERSION [TAG [FAIL_NOT_FOUND]]]
#
# Parameters:
#   VERSION - allows to check the version to use, values:
#     * ""        - empty means use default Dockerfile version
#     * "2.176."  - prefix of the jenkins version, will choose the latest available LTS 2.176.*
#     * "2.176.2" - exact version you want
#     * "lts"     - latest stable version
#     * "latest"  - just latest version
#   TAG - what the tag to use for the build
#   FAIL_NOT_FOUND - if specific version is not found - don't use lts/latest and fail
#

VAR_VERSION=$1

TAG=jenkinsbro-master
[ "x$2" = 'x' ] || TAG=$2
[ "x$3" = 'x' ] || FAIL_NOT_FOUND=True

if [ "${VAR_VERSION}" ]; then
    ver_url=http://mirrors.jenkins.io/war
    ver="[0-9]"
    if [ "${VAR_VERSION}" = 'lts' ]; then
        ver_url=${ver_url}-stable
    elif [ "${VAR_VERSION}" != 'latest' ]; then
        ver=$(echo "${VAR_VERSION}" | tr -dc '0-9.')
        echo "Using specified version '${ver}'"
    fi

    jenkins_version=$(curl -s "${ver_url}/" | grep -oE 'href="'${ver}'[^/]*' | tail -1 | tr -dc '0-9.')
    if [ "${FAIL_NOT_FOUND}" = "True" ]; then
        echo "Unable to find version for: ${VAR_VERSION} '${jenkins_version}'"
        [ "${jenkins_version}" ] || exit 1
    else
        # Use latest (for lts - stable latest) if no version is determined
        [ "${jenkins_version}" ] || jenkins_version=latest
    fi
    echo "Using jenkins version: ${ver_url}/${jenkins_version}"

    jenkins_sha=$(curl -s "${ver_url}/${jenkins_version}/jenkins.war.sha256" | cut -d" " -f 1)

    build_args="--build-arg JENKINS_VERSION=${jenkins_version} --build-arg JENKINS_SHA=${jenkins_sha}"
fi

docker build ${build_args} -t "${TAG}" "$(dirname $0)"
