import java.lang.System
import jenkins.*
import hudson.model.*
import jenkins.model.*

// Plugins for credentials
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*

pluginsActive 'credentials', {
  global_domain = Domain.global()
  credentials_store = Jenkins.instance.getExtensionList(SystemCredentialsProvider)[0].getStore()

  MODULE.each { name, cred ->
    info "Creating ${cred.type} credentials ${name}"
    def creds = null
    switch( cred.type ) {
      case 'password':
        creds = new UsernamePasswordCredentialsImpl(
          CredentialsScope.GLOBAL,
          cred.id,
          cred.description,
          cred.user_id,
          cred.password
        )
        break

      case 'string':
        creds = org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl.newInstance(
          CredentialsScope.GLOBAL,
          cred.id,
          cred.description,
          hudson.util.Secret.fromString(cred.string)
        )
        break

      case 'ssh':
        creds = pluginsActive 'ssh-credentials', {
          com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.newInstance(
            CredentialsScope.GLOBAL,
            cred.id,
            cred.user_id,
            com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.DirectEntryPrivateKeySource.newInstance(cred.key),
            cred.get('passphrase'),
            cred.description
          )
        }
        break

      case 'file':
        creds = pluginsActive 'plain-credentials', {
          org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl.newInstance(
            CredentialsScope.GLOBAL,
            cred.id,
            cred.description,
            cred.file_name,
            SecretBytes.fromString(cred.context)
          )
        }
        break

      case 'gitlab_api_token':
        creds = pluginsActive 'gitlab-plugin', {
          com.dabsquared.gitlabjenkins.connection.GitLabApiTokenImpl.newInstance(
            CredentialsScope.GLOBAL,
            cred.id,
            cred.description,
            hudson.util.Secret.fromString(cred.token)
          )
        }
        break

      default:
        throw new UnsupportedOperationException("${cred.type} credentials type is not supported!")
    }

    if( creds == null )
      return warn("Unable to set credentials ${cred.id}")

    def existing = helpers.getCredentials(cred.id)
    if( existing != null ) {
      credentials_store.updateCredentials(
        global_domain,
        existing,
        creds
      )
    } else {
      credentials_store.addCredentials(global_domain, creds)
    }
  }
}