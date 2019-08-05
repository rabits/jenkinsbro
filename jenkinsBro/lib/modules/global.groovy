/**
 * Global jenkins configuration
 *
 * Contains small parameters not worth to have a separated module
 */

def I = jenkins.model.Jenkins.getInstance()

info "Set number of executors on master"
I.setNumExecutors(MODULE.num_executors_on_master ?: 2)

info "Set quiet period"
I.setQuietPeriod(MODULE.quiet_period ?: 3)

info "Set checkout retry"
I.setScmCheckoutRetryCount(MODULE.scm_checkout_retry_count ?: 2)

info 'Disable Jenkins CLI'
def cli = I.getDescriptor('jenkins.CLI')
if( cli ) {
  cli.setEnabled(MODULE.enable_cli ?: false)
  cli.save()
}

info 'Enable jenkins CSRF protection'
if( MODULE.enable_csrf_protection ?: true )
  I.setCrumbIssuer(hudson.security.csrf.DefaultCrumbIssuer.newInstance(false))
else
  I.setCrumbIssuer(null)

info 'Configure Slave to Master access control'
I.getInjector().getInstance(jenkins.security.s2m.AdminWhitelistRule.class).
  setMasterKillSwitch(MODULE.disable_agent_master_security ?: false)

info 'Set list JNLP protocols'
I.setAgentProtocols(MODULE.get('jnlp_agent_protocols', ['JNLP4-connect', 'Ping']).toSet())

def jenkins_url = MODULE.jenkins_root_url ?: "http://${java.net.InetAddress.localHost.getHostAddress()}:8080/"
info "Set jenkins root url to ${jenkins_url}"
def jlc = jenkins.model.JenkinsLocationConfiguration.get()
jlc.setUrl(jenkins_url)
jlc.save()

// Set Admin Email as a string "Name <email>"
if( MODULE.jenkins_admin_email ) {
  info "Set admin e-mail address to ${MODULE.jenkins_admin_email}"
  jlc.setAdminAddress(MODULE.jenkins_admin_email)
  jlc.save()
}

def desc = I.getDescriptor('hudson.plugins.git.GitSCM')
if( desc && MODULE.git ) {
  info "Set Global GIT configuration name to ${MODULE.git.name} and email address to ${MODULE.git.email}"
  desc.setGlobalConfigName(MODULE.git.name ?: '')
  desc.setGlobalConfigEmail(MODULE.git.email ?: '')
}

if( MODULE.smtp ) {
  info 'Setting E-mail configuration...'
  def email_desc = I.getDescriptor(hudson.tasks.Mailer)
  email_desc.setSmtpHost(MODULE.smtp.host)
  email_desc.setSmtpPort(MODULE.smtp.port as String)
  email_desc.setReplyToAddress(MODULE.smtp.reply_to_address)
  if( MODULE.smtp.auth )
    email_desc.setSmtpAuth(MODULE.smtp.auth.login, MODULE.smtp.auth.password)
} else
  info "Skipping E-mail configuration..."

info "Setting system message..."
def env = System.getenv()
if( env.containsKey('JENKINS_BUILD_VERSION') ) {
  helpers.addGlobalEnvVariable('JENKINS_BUILD_VERSION', env['JENKINS_BUILD_VERSION'])

  def date = new Date()
  sdf = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
  system_message = "This Jenkins instance configured by JenkinsBRO\n\n" +
                   "Avoid any manual changes since they will be discarded with next deployment.\n" +
                   "Jenkins docker image version: ${env['JENKINS_BUILD_VERSION']}\n" +
                   "Deployment date: ${sdf.format(date)}\n\n"
  info "Set system message to:\n${system_message}"
  I.setSystemMessage(system_message)
} else
  warn 'Cant set system message - missing env variable JENKINS_IMAGE_VERSION'

info 'Setting global env variables...'
MODULE.variables.each { key, value ->
  helpers.addGlobalEnvVariable(key, value)
}

I.save()
