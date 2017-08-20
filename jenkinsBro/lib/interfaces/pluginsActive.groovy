/**
 * Executing the body only if plugins active or just returning boolean
 * Parameters:
 *   * shortname - plugin short name
 *   * ...
 *   * [body] - closure to execute if plugins are active (optional)
 * Usage:
 *   def is_active = pluginsActive('credentials')
 *   def is_notactive = pluginsActive 'notaplugin', 'someplugin'
 *   def closure_output = pluginsActive('kubernetes', 'credentials') {
 *     println "Previous plugins is active: ${is_notactive}"
 *   }
**/
def pluginsActive(Object... params) {
  if( ! params.last() instanceof Closure )
    return params.every { it -> helpers.isPluginActive(it) }

  if( params.init().every { it -> helpers.isPluginActive(it) } )
    return params.last()()

  warn "a number of required plugins is not active: ${params.init().join(', ')}"
  return null
}
