/**
 * Generic Global Tool Configuration provider
 *
 * - Type of the tool is the same as on Global Tool Configuration page
 * - Installer name is existing on the Global Tool Configuration page when install automatically checkbox is selected
 */

import java.lang.reflect.ParameterizedType

// Get superclass type of existing object
def getSuperclassType(obj) {
  ((ParameterizedType) obj.getClass().getGenericSuperclass()).getActualTypeArguments()[0]
}

// Check that the constructor starts with the required var types and returns a number of arguments to provide
// most probably they are taken from the base class `ToolInstallation` : name, home, properties
def getConstructorAdditionalParams(Class clazz, List params) {
  clazz.getConstructors().findAll { it.getParameterCount() >= params.size() }.findAll {
    // Check all the provided parameters have similar classes for the available ones
    it.getParameterTypes()[0..(params.size()-1)].withIndex().each { e,i -> e.isInstance(params[i]) }
  }.collect{ it.getParameterCount()-params.size() }.sort()[0]
}

def tools = hudson.tools.ToolInstallation.all().collectEntries { [(it.getDisplayName()): it] }
def installers = hudson.tools.ToolInstallerDescriptor.all().collectEntries { [(it.getDisplayName()): it] }

MODULE.each { type, list ->
  info "Processing tool type ${type}..."
  def tool_desc = tools.get(type)
  if( ! tool_desc )
    return error("Unable to find tool type in the list ${tools.keySet()}")

  // Getting tool installer class from the descriptor installed tools array class type:
  def tool_class = tool_desc.getInstallations().getClass().getComponentType()

  // Recreating the list of tools from scratch - no way to modify existing one
  tool_desc.setInstallations(*list.collect { name, config ->
    // Constructing installers list
    config.installers ?: warn("Unable to find installers for tool ${type}->${name}")
    tool_insts = config.get('installers', []).collect {
      info "Processing installer for ${type}->${name}: ${it}"
      def inst_desc = installers.get(it.name)
      if( ! inst_desc )
        return error("Unable to find installer '${it.name}' in the list of available ones: ${installers.keySet()}")

      // Each installer descriptor has superclass with specific installer class type
      def inst_class = getSuperclassType(inst_desc)
      // Now we can create the installer object with provided parameters
      it.params ?: warn("Unable to find params for the installation ${it.name}")
      try {
        inst_class.newInstance(it.params as Object[])
      } catch(Exception ex) {
        return error("Exception occured during installation object creation: ${ex}")
      }
    }.findAll()

    // Packing installers into the install property
    if( tool_insts )
      tool_insts = [hudson.tools.InstallSourceProperty.newInstance(tool_insts)]
    def tool_params = [name, config.get('home', ''), tool_insts]

    // Checking that constructor is valid and getting number of null params if constructor is taking more params
    def add_params = getConstructorAdditionalParams(tool_class, tool_params)
    if( add_params == null )
      return error("Unsupported tool class ${tool_class.getName()} - please create a special module to configure it")

    // And creating the tool
    try {
      tool_class.newInstance((tool_params + [null]*add_params) as Object[])
    } catch(Exception ex) {
      return error("Exception occured during tool object creation: ${ex}")
    }
  })

  tool_desc.save()
}
