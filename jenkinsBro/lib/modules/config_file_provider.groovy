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
 *       creds - key-value map with key some name and value is credential-id (maven_settings and properties)
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
          config.creds?.collect { org.jenkinsci.plugins.configfiles.maven.security.ServerCredentialMapping.newInstance(it.key, it.value) }
        )
        info "Config file added ${config_id}:${config.type}"
        break

      case 'json':
        jenkins_global_configs << org.jenkinsci.plugins.configfiles.json.JsonConfig.newInstance(
          config_id,
          config.name,
          config.comment,
          config.content
        )
        info "Config file added ${config_id}:${config.type}"
        break

      case 'properties':
        jenkins_global_configs << org.jenkinsci.plugins.configfiles.properties.PropertiesConfig.newInstance(
          config_id,
          config.name,
          config.comment,
          config.content,
          true,
          config.creds?.collect { org.jenkinsci.plugins.configfiles.properties.security.PropertiesCredentialMapping.newInstance(it.key, it.value) }
        )
        info "Config file added ${config_id}:${config.type}"
        break

      case 'xml':
        jenkins_global_configs << org.jenkinsci.plugins.configfiles.xml.XmlConfig.newInstance(
          config_id,
          config.name,
          config.comment,
          config.content
        )
        info "Config file added ${config_id}:${config.type}"
        break

      case 'custom':
        jenkins_global_configs << org.jenkinsci.plugins.configfiles.custom.CustomConfig.newInstance(
          config_id,
          config.name,
          config.comment,
          config.content
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
