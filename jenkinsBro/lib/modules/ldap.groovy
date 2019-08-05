/**
 * LDAP configuration module
 */

pluginsActive 'ldap', {
  info 'Configuring LDAP...'
  jenkins.model.Jenkins.instance.setSecurityRealm(
    new hudson.security.LDAPSecurityRealm(
      MODULE.server,
      MODULE.root_dn,
      MODULE.user_search_base,
      MODULE.user_search,
      MODULE.group_search_base,
      MODULE.manager_dn,
      MODULE.manager_password,
      MODULE.inhibit_infer_root_dn
    )
  )
  jenkins.model.Jenkins.instance.save()
}
