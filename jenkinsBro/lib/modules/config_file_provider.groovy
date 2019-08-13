/**
 * Config provider module
 *
 * Parameters:
 *   config_file_provider {
 *     [id] {
 *       name - name of the config file
 *       type - right now supported only "maven_settings"
 *       comment - just a commentary
 *       content - config file content
 *     }
 *   }
 */
import jenkins.model.Jenkins

pluginsActive 'config-file-provider', {
  def jenkins_global_configs = []
  MODULE.each { config_id, config ->
    switch( config.type ) {
      case 'maven_settings':
        jenkins_global_configs << org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig.newInstance(
          config_id,
          config.name,
          config.comment,
          config.content,
          true,
          null // Do not support credentials for now
        )
        info "Config file added ${config_id}:${config.type}"
        break

      default:
        error "Unsupported config type: ${config.type}!"
    }
  }
  def plugin = Jenkins.instance.getDescriptor('org.jenkinsci.plugins.configfiles.GlobalConfigFiles')
  plugin.setConfigs(jenkins_global_configs)
  plugin.save()
}
