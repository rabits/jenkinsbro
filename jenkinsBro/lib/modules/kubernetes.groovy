/**
 * Kubernetes cloud module
 *
 * Parameters:
 *   kubernetes {
 *     [cluster_name] {
 *       server_url ('') - if you using standalone jenkins outside the cluster
 *       server_certificate ('') - remote cluster certificate
 *       credentials_id ('') - if you want to use remote cluster
 *       skip_tls_verify (false) - do not verify tls connection
 *       namespace ('') - if you have special place where to run the containers
 *       jenkins_url - this url will be used to connect jnlp agent to jenkins
 *       connect_timeout (5) - to Kube API in seconds
 *       read_timeout (15) - from Kube API in seconds
 *       concurrency_limit (10) - maximum containers per cluster
 *       retention_timeout (5) - in minutes to rerun the agent pod
 *       max_requests_per_host (32) - max concurrent equests to Kube API
 *       wait_for_pod_sec (600) - how much time to wait the agent connection
 *       default_template ('') - template to use by default
 *       templates {
 *         [pod_template_name] {
 *           namespace ('') - namespace where to run this template
 *           usage_mode ('EXCLUSIVE') - use for specified labels only or as much as possible ('NORMAL')
 *           inherit_from ('') - override some parameters of the parent template
 *           label ('') - what the labels is provided by the template
 *           active_deadline_seconds (28800) - timeout to kill the pod
 *           dump_raw_yaml (false) - dump the raw yaml pod specification to the build console
 *           env { - environment to set for pod
 *             [key] = [value]
 *           }
 *           containers {
 *             [container_name] {
 *               image - docker image path to use for the container
 *               working_dir ('/home/jenkins') - where the pod volume will be mounted and where the workspace dir will be created
 *               command ('') - what to run in the container (`cat` to run forever)
 *               args ('') - arguments to the command
 *               tty_enabled (false) - if you need tty for the command
 *               request_cpu ('') - how much cpu is needed to the container
 *               request_memory ('') - how much memory is needed to the container
 *               limit_cpu ('') - what the limits of cpu for the container
 *               limit_memory ('') - what the limits of memory for the container
 *               env { - environment variables for the container
 *                 [key] = [value]
 *               }
 *             }
 *           }
 *         }
 *       }
 *     }
 *   }
 */

import jenkins.model.Jenkins
import jenkins.model.JenkinsLocationConfiguration

pluginsActive 'kubernetes', {
  def jenkins = Jenkins.getInstance()
  // Remove all not defined clusters
  jenkins.clouds.getAll(org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud).findAll {
    !MODULE.keySet().contains(it.name)
  }.each {
    info "Cluster ${it.name} not found in the configuration, removing..."
    jenkins.clouds.remove(it)
  }

  // Create or replace clusters
  MODULE.each { cloud_name, cloud_conf ->
    def templates = cloud_conf.templates.collect { temp_name, temp_conf ->
      def containers = temp_conf.containers.collect { name, conf ->
        def container = org.csanchez.jenkins.plugins.kubernetes.ContainerTemplate.newInstance(name, conf.image)
        container.setWorkingDir(conf.working_dir ?: '/home/jenkins')
        container.setCommand(conf.command ?: '')
        container.setArgs(conf.args ?: '')
        container.setTtyEnabled(conf.tty_enabled ?: false)
        container.setEnvVars(conf.env.collect { key, value ->
          org.csanchez.jenkins.plugins.kubernetes.model.KeyValueEnvVar.newInstance(key, value)
        })
        container.setResourceRequestCpu((conf.request_cpu ?: '').toString())
        container.setResourceRequestMemory((conf.request_memory ?: '').toString())
        container.setResourceLimitCpu((conf.limit_cpu ?: '').toString())
        container.setResourceLimitMemory((conf.limit_memory ?: '').toString())

        info "Prepared container ${name} template ${temp_name} cloud ${name}"
        return container
      }

      def temp = org.csanchez.jenkins.plugins.kubernetes.PodTemplate.newInstance()
      temp.setName(temp_name)
      temp.setNamespace(temp_conf.namespace ?: '')
      temp.setNodeUsageMode(temp_conf.usage_mode ?: 'EXCLUSIVE')
      temp.setInheritFrom(temp_conf.inherit_from ?: '')
      temp.setLabel(temp_conf.label ?: '')
      temp.setActiveDeadlineSeconds(temp_conf.active_deadline_seconds ?: 28800)
      temp.setShowRawYaml(temp_conf.dump_raw_yaml ?: false)
      temp.setEnvVars(temp_conf.env.collect { key, value ->
        org.csanchez.jenkins.plugins.kubernetes.model.KeyValueEnvVar.newInstance(key, value)
      })
      temp.setContainers(containers)

      info "Prepared template ${temp_name} cloud ${cloud_name}"
      return temp
    }

    // Base settings
    def cloud = org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud.newInstance(
      cloud_name,
      templates,
      cloud_conf.server_url ?: '',
      cloud_conf.namespace ?: '',
      cloud_conf.jenkins_url,
      ("${cloud_conf.container_cap}".isInteger() ? cloud_conf.container_cap : 10).toString(),
      cloud_conf.connect_timeout ?: 5,
      cloud_conf.read_timeout ?: 15,
      "${cloud_conf.retention_timeout}".isInteger() ? cloud_conf.retention_timeout : 5
    )

    // Additional settings
    cloud.setServerCertificate(cloud_conf.server_certificate ?: '')
    cloud.setCredentialsId(cloud_conf.credentials_id ?: '')
    cloud.setSkipTlsVerify(cloud_conf.skip_tls_verify ?: false)
    cloud.setMaxRequestsPerHostStr((cloud_conf.max_requests_per_host ?: 32).toString())
    cloud.setWaitForPodSec(cloud_conf.wait_for_pod_sec ?: 600)
    cloud.setDefaultsProviderTemplate(cloud_conf.default_template ?: '')

    info "Apply configuration for cloud: ${cloud_name}"
    jenkins.clouds.replace(cloud)
  }
}
