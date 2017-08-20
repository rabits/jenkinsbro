import hudson.model.*
import jenkins.model.*

def isPluginActive(shortname) {
  def pm = Jenkins.getInstance().instance.pluginManager
  return pm.activePlugins.find { it.shortName == shortname } != null
}
