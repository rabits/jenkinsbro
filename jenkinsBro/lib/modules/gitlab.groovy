/**
 * Simple module to setup gitlab connection
 */

pluginsActive 'gitlab-plugin', {
  info "Setting gitlab credentials ${MODULE.credentials_id} for connection ${MODULE.name}"
  def descriptor = Jenkins.getInstance().getDescriptor(
    com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig.class)

  def gitlab_connection = new com.dabsquared.gitlabjenkins.connection.GitLabConnection(
    MODULE.name,
    MODULE.url,
    MODULE.credentials_id,
    false, 10, 10
  )

  descriptor.getConnections().clear()
  descriptor.addConnection(gitlab_connection)
  descriptor.save()
}
