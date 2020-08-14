/**
 * Global Pipeline libraries module
 *
 * Parameters:
 *   pipeline_libraries {
 *     key {
 *       name           - name of the library to use as id
 *       scm_path       - git repo url or local directory path
 *       refspec        - refspec mapping
 *       version        - branch/commit/tag in the repository
 *       credentials_id - credentials to clone the git repo
 *       implicitly     - load library by default for each pipeline job
 *       allow_override - Jenkinsfiles/pipeline scripts will be able to use a different version of the lib
 *     }
 *   }
 */

pluginsActive 'workflow-cps-global-lib', 'git', {
  def gl = org.jenkinsci.plugins.workflow.libs.GlobalLibraries.get()
  def lib_configs = MODULE.collect { key, data ->
    info "Configuring Pipeline shared groovy library: ${data.name} ..."
    def scm_source = new jenkins.plugins.git.GitSCMSource(
      data.name,
      data.scm_path,
      data.credentials_id ?: null,
      null, data.refspec ?: null, false
    )
    def lib_config = org.jenkinsci.plugins.workflow.libs.LibraryConfiguration.newInstance(
      data.name, org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever.newInstance(scm_source)
    )
    lib_config.setDefaultVersion(data.version ?: '')
    lib_config.setImplicit(data.implicitly ?: false)
    lib_config.setAllowVersionOverride(data.allow_override ?: true)
    return lib_config
  }
  gl.setLibraries(lib_configs)
}
