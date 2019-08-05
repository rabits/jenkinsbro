/**
 * Example to test the global module
 */

import org.junit.Test

@Module('global')
class GlobalTest extends JenkinsTest {
  @Test
  void testGlobalSettings() {
    assert jenkins.getNumExecutors() == MODULE.num_executors_on_master
    assert jenkins.getRootUrl() == MODULE.jenkins_root_url
  }
}
