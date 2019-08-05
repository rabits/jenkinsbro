/**
 * Create a simple pipeline job, run it and wait for the result
 *
 * Parameters:
 *   simple_pipeline_test.clone_url - where to get the sources
 *   simple_pipeline_test.job_name - job name to create on jenkins
 *   simple_pipeline_test.branch_spec - branch/commit/tag to build
 *   simple_pipeline_test.credentials_id - if the creds are required
 */

import org.junit.Test

import hudson.plugins.git.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob

@Module('simple_pipeline_test')
class SimplePipelineTest extends JenkinsTest {
  @Test
  void testJenkinsfileBuild() {
    def scm = new GitSCM(MODULE.clone_url)
    scm.branches = [new BranchSpec(MODULE.branch_spec ?: 'master')]
    scm.userRemoteConfigs = GitSCM.createRepoList(MODULE.clone_url, MODULE.credentials_id ?: '')

    def wfjob = jenkins.createProject(WorkflowJob.class, MODULE.job_name ?: 'simple-pipeline-test')
    def flow_def = org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition.newInstance(scm, 'Jenkinsfile')
    wfjob.setDefinition(flow_def)

    // Running the job and wait for complete
    def task_future = wfjob.scheduleBuild2(0)
    def build = task_future.waitForStart()
    task_future.get() // Waiting

    build.getLogText().writeLogTo(0, System.out)

    assert build.getResult().toString() == 'SUCCESS'
  }
}
