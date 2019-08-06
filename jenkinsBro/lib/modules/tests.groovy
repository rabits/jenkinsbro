/**
 * Testing module
 * Allows to run unit and integration tests on the working jenkins
 * You can test how configuration was applied, run jobs/pipelines or UI tests
 *
 * Configuration:
 *   tests.tests_dir    - directory contains the test classes (def: `$JENKINSBRO_DIR/tests`)
 *   tests.status_file  - path to the file with tests status ("Started", "Finished") (def: `$JENKINSBRO_DIR/tests_status`)
 *   tests.report_dir   - dir to place the test junit xml reports (def: `$JENKINSBRO_DIR/junit_report`)
 *   tests.wait_sec     - how much time (sec) wait for Jenkins to be ready to use after the configuration
 *   tests.modules      - map with override configuration for modules to execute tests that requires configuration
 *   tests.exit_on_finished - when all the tests are completed - automatically shutdown jenkins with exit code = number of failed tests
 *
 * Global:
 *   CONFIG.modules     - will be used if to find module if `tests.modules.{module}` is not defined
 *   JENKINSBRO_DIR     - used to locate tests and test helpers
 */

import hudson.init.InitMilestone
import jenkins.model.Jenkins

/**
 * Test class Module annotation
 *
 * Prepend your test class with Module('name') annotation to get the current configuration
 * If it will not be provided - test class will be started without access to the MODULE configs
 */
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
@interface Module {
  String value()
}

/**
 * Common test class
 * You can use it in your tests
 */
class JenkinsTest {
  def jenkins

  @org.junit.Before
  void setUp() {
    jenkins = Jenkins.getInstance()
  }
}

// lazyMap (used by JsonSlurper) is not thread safe, need to provide copy for module
def MODULE = MODULE instanceof groovy.json.internal.LazyMap ? MODULE.clearAndCopy() : MODULE

Thread.start('JenkinsBro tests') {
  def tests_dir = MODULE.tests_dir ?: "${JENKINSBRO_DIR}/tests"
  def status_file = new File(MODULE.status_file ?: "${JENKINSBRO_DIR}/tests_status")
  status_file.write('Started')
  def junit_report_dir = new File(MODULE.report_dir ?: "${JENKINSBRO_DIR}/junit_report")
  junit_report_dir.deleteDir()
  junit_report_dir.mkdir()

  def failed_tests = 0

  info "JenkinsBro tests started, status file: `${status_file}`, reports dir: `${junit_report_dir}`"

  try {
    info 'JenkinsBro tests: waiting for Jenkins initialization...'
    def retries = MODULE.wait_sec ?: 5 * 60 // 5 minutes by default
    (1..retries).each {
      if( Jenkins.getInstance().getInitLevel() == InitMilestone.COMPLETED )
        return
      sleep(1000)
    }

    if( Jenkins.getInstance().getInitLevel() != InitMilestone.COMPLETED )
      return error("JenkinsBro tests, Jenkins initialization not completed in ${retries} seconds, exiting...")

    info 'JenkinsBro tests: loading required classes'
    def cl = new GroovyClassLoader(this.class.getClassLoader())
    cl.setClassCacheEntry(JenkinsTest)
    cl.setClassCacheEntry(Module)

    info 'JenkinsBro tests: preparing junit'
    def core = org.junit.runner.JUnitCore.newInstance()
    def listener = new org.junit.internal.TextListener(System.out)
    core.addListener(listener)

    cl.addClasspath("${JENKINSBRO_DIR}/lib/test_helpers")
    Class formatterClazz = cl.loadClass('SimpleJUnitResultFormatterAsRunListener')
    core.addListener(formatterClazz.newInstance(junit_report_dir))

    new File(tests_dir).eachFileRecurse(groovy.io.FileType.FILES) {
      if( !it.name.endsWith('Test.groovy') )
        return

      info "Init test suite: ${it.getName()}..."
      Class clazz
      try {
        clazz = cl.parseClass(it)
      } catch( Exception ex ) {
        return error("Exception while loading test suite ${it}: ${ex}")
      }

      def annotation = clazz.getAnnotation(Module)
      if( annotation ) {
        def module = annotation.value()
        if( MODULE.modules.get(module, false) )
          clazz.metaClass.MODULE = MODULE.modules.get(module)
        else if( CONFIG.modules.get(module, false) )
          clazz.metaClass.MODULE = CONFIG.modules.get(module)
        else
          return info("Module ${module} is not configured, skiping test suite ${clazz.getName()}")
      } else
        info "Test suite ${clazz.getName()} doesn't prepended with annotation: @Module('xxx')"

      info "Running test suite: ${it.getName()}..."
      def result = core.run(clazz)
      failed_tests += result.getFailureCount()
    }
  } catch( Exception ex ) {
    warn "Exception while executing tests: ${ex}"
  }

  status_file.write('Finished')

  info 'JenkinsBro tests: All tests are finished'
  if( MODULE.exit_on_finished ?: false ) {
    info 'JenkinsBro tests: Shutdown Jenkins'
    System.exit(failed_tests)
  }
}
