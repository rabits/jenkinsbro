/**
 * Example to test the pipeline_libraries module
 */

import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.junit.Test

@Module('pipeline_libraries')
class PipelineLibrariesTest extends JenkinsTest {
  @Test
  void testPipelineLibrariesCount() {
    GlobalLibraries gl = GlobalLibraries.get()
    assert gl.getLibraries().size() == MODULE.size()
  }

  @Test
  void testPipelineLibrariesSettings() {
    GlobalLibraries gl = GlobalLibraries.get()
    gl.getLibraries().each { library ->
      def config_library = MODULE.find { it.value.name == library.name }
      assert library.name == config_library?.value?.name
      assert library.defaultVersion == config_library?.value?.version
    }
  }
}
