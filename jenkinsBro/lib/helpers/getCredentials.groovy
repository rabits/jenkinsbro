import hudson.model.*
import jenkins.model.*

def getCredentials(String id) {
  def id_matcher = com.cloudbees.plugins.credentials.CredentialsMatchers.withId(id)
  def available_credentials = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
    com.cloudbees.plugins.credentials.common.StandardCredentials.class,
    Jenkins.getInstance(),
    hudson.security.ACL.SYSTEM
  )
  
  return com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrNull(
    available_credentials,
    id_matcher
  )
}
