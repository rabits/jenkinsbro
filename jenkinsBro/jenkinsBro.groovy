/**
 * Script will help with config loading, some helpful functions and modules execution
**/

import jenkins.model.Jenkins
import groovy.json.JsonSlurper

// Default functions
binding.setVariable('info', { Object... params ->
  println "--> INFO: " + params.join(' ').replace('\n', '\n    ')
})
binding.setVariable('warn', { Object... params ->
  println "--> WARN: " + params.join(' ').replace('\n', '\n    ')
})
binding.setVariable('error', { Object... params ->
  println "--> ERROR: " + params.join(' ').replace('\n', '\n    ')
})

info 'JenkinsBRO running'

def home_dir = System.getenv('JENKINS_HOME')

def configs_file = new File(System.getenv('JENKINSBRO_CONFIGS_PATH') ?: "${home_dir}/config/jenkins.conf")
if( !(configs_file.exists() || configs_file.canRead()) )
  return warn('  configuration not found, skipping JenkinsBRO run')

def config_parser
if( !configs_file.getName().endsWith('.json') ) {
  config_parser = new ConfigSlurper()
  def config_binds = [env: System.getenv()]

  info 'Reading configuration secrets...'
  def secrets_file = new File(System.getenv('JENKINSBRO_SECRETS_PATH') ?: "${home_dir}/config/secrets.conf")
  if( secrets_file.exists() || secrets_file.canRead() ) {
    def secrets = config_parser.parse(secrets_file.toURI().toURL())
    if( secrets.get('delete_secrets_file', true) )
      secrets_file.delete()
    info "  binding next secrets: ${secrets.keySet().join(', ')}"
    config_binds['secrets'] = secrets
  } else
    warn '  secrets not found, skipping'

  config_parser.setBinding(config_binds)
} else
  config_parser = new JsonSlurper()

info 'Reading the jenkins configuration...'
binding.setVariable('CONFIG', config_parser.parse(configs_file.toURI().toURL()))

if( CONFIG.get('delete_configs_file', true) )
  configs_file.delete()

GroovyShell shell = new GroovyShell(Jenkins.instance.getPluginManager().uberClassLoader, binding)

binding.setVariable('JENKINSBRO_DIR', CONFIG.jenkinsbro_dir ?: "${home_dir}/jenkinsBro")

info 'Loading helpers...'
def helpers = [:]
new File("${JENKINSBRO_DIR}/lib/helpers").listFiles({ file -> file.name.endsWith('.groovy') } as FileFilter).each { file ->
  def helper = shell.parse(file)
  def name = file.name[0..-8].replaceAll(/[^A-Za-z0-9]/, '')
  if( helpers.containsKey(name) )
    warn "  skipping ${name}: helpers already contains this method"
  else
    helpers[name] = helper.&"${name}"
}

info "  binding next helpers: ${helpers.keySet().join(', ')}"
binding.setVariable('helpers', helpers)

info 'Loading interfaces...'
new File("${JENKINSBRO_DIR}/lib/interfaces").listFiles({ file -> file.name.endsWith('.groovy') } as FileFilter).each { file ->
  def module = shell.parse(file)
  def name = file.name[0..-8].replaceAll(/[^A-Za-z0-9]/, '')
  if( binding.variables.containsKey(name) )
    warn "  skipping ${name}: binding already contains this variable"
  else
    binding.setVariable(name, module.&"${name}")
}

info "  next bindings are available for modules: ${binding.variables.keySet().join(', ')}"

info 'Processing modules...'
def available_modules = new File("${JENKINSBRO_DIR}/lib/modules").listFiles({ f -> f.name.endsWith('.groovy') } as FileFilter)
def not_configured_modules = available_modules.collect({ file -> file.name[0..-8] }) - CONFIG.modules.keySet()
if( not_configured_modules )
  info "Found not configured modules: ${not_configured_modules.join(', ')}"

CONFIG.modules.each { name, module ->
  if( ! available_modules*.name.contains(name+'.groovy') )
    return warn("Skipping ${name} configuration: no required module found")

  info "Processing module ${name} ..."
  try {
    binding.setVariable('MODULE', module)
    evaluate(available_modules.find { file -> file.name[0..-8] == name })
  } catch( Exception e ) {
    def st = e.getStackTrace().findAll { it.getFileName()?.endsWith('.groovy') }
    error "Exception while processing module ${name}: ${e.toString()}\n${st.join('\n')}"
  }
}

info 'JenkinsBRO finished his work'
