import hudson.model.*
import jenkins.model.*

import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry

def addGlobalEnvVariable(String key, String value) {
  def node_properties = Jenkins.getInstance().getGlobalNodeProperties()
  def env_vars = node_properties.getAll(hudson.slaves.EnvironmentVariablesNodeProperty.class)
  if( env_vars == [] ) {
    def entry = new EnvironmentVariablesNodeProperty.Entry(key, value)
    node_properties.add(new EnvironmentVariablesNodeProperty(entry))
  } else {
    env_vars.get(0).getEnvVars().put(key, value)
  }
  info "added global environment variable ${key}"
  Jenkins.getInstance().save()
}
