import jenkins.model.Jenkins

import com.dabsquared.gitlabjenkins.connection.*

pluginsActive 'gitlab-plugin', {
  info "Setting gitlab credentials ${MODULE.credentials_id} for connection ${MODULE.name}"
  GitLabConnectionConfig descriptor = Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class)

  GitLabConnection gitLabConnection = new GitLabConnection(
    MODULE.name,
    MODULE.url,
    MODULE.credentials_id,
    false, 10, 10
  )

  descriptor.getConnections().clear()
  descriptor.addConnection(gitLabConnection)
  descriptor.save()
}
