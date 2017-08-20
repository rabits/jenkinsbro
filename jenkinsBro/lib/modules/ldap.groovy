import java.lang.System
import jenkins.model.*
import hudson.security.*

pluginsActive 'ldap', {
  info "Configuring LDAP..."
  Jenkins.instance.setSecurityRealm(new LDAPSecurityRealm(
    MODULE.server,
    MODULE.root_dn,
    MODULE.user_search_base,
    MODULE.user_search,
    MODULE.group_search_base,
    MODULE.manager_dn,
    MODULE.manager_password,
    MODULE.inhibit_infer_root_dn)
  )
  Jenkins.instance.save()
}
