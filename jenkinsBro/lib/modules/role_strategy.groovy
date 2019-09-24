/**
 * Role Strategy module
 *
 * Parameters:
 *   role_strategy {
 *     [role_map] { // Could be one of [globalRoles, projectRoles, slaveRoles]
 *       [role_name] {
 *         permissions - list of permissions, list could be found by executing `hudson.security.Permission.getAll()*.getId()` in jenkins script console
 *         assigned_sids - list of user id's or group names
 *       }
 *       ...
 *     }
 *     ...
 *   }
 */
import jenkins.model.Jenkins

pluginsActive 'role-strategy', {
  def jenkins = Jenkins.getInstance()
  def strategy = com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy.newInstance()

  MODULE.each { role_map, roles ->
    roles.each { role_name, role ->
      def perms = role.permissions.collect { hudson.security.Permission.fromId(it) }
      def role_type = com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType.fromString(role_map)
      def role_obj = com.michelin.cio.hudson.plugins.rolestrategy.Role.newInstance(role_name, perms.toSet())
      strategy.addRole(role_type, role_obj)
      info "Created role ${role_obj}"

      role.assigned_sids?.each {
        info "Role ${role_obj} assigned to ${it}"
        strategy.assignRole(role_type, role_obj, it)
      }
    }
  }
  jenkins.setAuthorizationStrategy(strategy)
  jenkins.save()
}
