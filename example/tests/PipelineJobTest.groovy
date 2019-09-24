/**
 * Example to test the pipeline job with auth
 */

import org.junit.After
import org.junit.Before
import org.junit.Test

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.plugins.git.GitSCM
import org.jenkinsci.plugins.workflow.job.WorkflowJob

@Module('pipeline_job_test')
class PipelineJobTest extends JenkinsTest {
  def role
  def type

  @Before
  void setUp() {
    super.setUp()

    // Giving access to be able to run the job
    def perms = [ hudson.security.Permission.fromId('hudson.model.Hudson.Administer') ]
    type = com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType.fromString('globalRoles')
    role = com.michelin.cio.hudson.plugins.rolestrategy.Role.newInstance('pipeline_test_role', perms.toSet())
    jenkins.getAuthorizationStrategy().addRole(type, role)
    jenkins.getAuthorizationStrategy().assignRole(type, role, MODULE.username)

    hudson.security.ACL.impersonate(hudson.model.User.get(MODULE.username).impersonate())
  }

  @Test
  void testPipeline() {
    def existing = jenkins.getItems().find {it.name == 'Jenkins-Test-Pipeline'}
    def folder = existing ?: jenkins.createProject(Folder.class, 'Jenkins-Test-Pipeline')
    folder.save()

    def job_name = "test_pipeline-${folder.getItems().size()}"
    def scm = new GitSCM(MODULE.clone_url)
    scm.branches = [new hudson.plugins.git.BranchSpec(MODULE.branch_spec)]
    scm.userRemoteConfigs = GitSCM.createRepoList(MODULE.clone_url, MODULE.credentials_id)

    def wfjob = folder.createProject(WorkflowJob.class, job_name)
    def flow_def = new CpsScmFlowDefinition(scm, 'Jenkinsfile')
    wfjob.setDefinition(flow_def)

    // Running the job and wait for complete
    def task_future = wfjob.scheduleBuild2(0)
    def build = task_future.waitForStart()
    task_future.get() // Waiting

    build.getLogText().writeLogTo(0, System.out)

    assert build.getResult().toString() == 'SUCCESS'
  }

  @After
  void tearDown() {
    jenkins.getAuthorizationStrategy().getRoleMap(type).removeRole(role)
  }
}
