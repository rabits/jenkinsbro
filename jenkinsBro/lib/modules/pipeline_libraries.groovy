import java.lang.System
import hudson.model.*
import org.jenkinsci.plugins.workflow.libs.*

pluginsActive 'workflow-cps-global-lib', 'git', {
  GlobalLibraries gl = GlobalLibraries.get()
  List<LibraryConfiguration> lib_configs = new ArrayList<LibraryConfiguration>()

  MODULE.each { key, value ->
    info "Configuring Pipeline shared groovy library: ${value.name} ..."
    def scm_source = new jenkins.plugins.git.GitSCMSource(
      value.name,
      value.scm_path,
      value.credentials_id ?: null,
      null, null, false
    )
    LibraryConfiguration lib_config = new LibraryConfiguration(
      value.name, new SCMSourceRetriever(scm_source)
    )
    lib_config.setDefaultVersion(value.version)
    lib_config.setImplicit(value.implicitly ?: false)
    lib_config.setAllowVersionOverride(value.allow_overridden ?: true)
    lib_configs.add(lib_config)
  }
  gl.setLibraries(lib_configs)
}
