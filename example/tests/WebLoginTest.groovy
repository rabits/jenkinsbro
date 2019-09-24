/**
 * Jenkins UI login test
 * Test will make sure the user will be added to a special role to login properly
 *
 * Parameters:
 *   web_login_test.url - URL to jenkins login page
 *   web_login_test.username - user name to test (some existing user, from ldap for example)
 *   web_login_test.password - user password
 */

@Grab(group='net.sourceforge.htmlunit', module='htmlunit', version='2.35.0')

import com.gargoylesoftware.htmlunit.WebClient

import org.junit.Test
import org.junit.Before
import org.junit.After

@Module('web_login_test')
class WebLoginTest extends JenkinsTest {
  def type
  def role

  @Before
  void setUp() {
    super.setUp()

    // Make sure the user is exist in the required role to login
    role = com.michelin.cio.hudson.plugins.rolestrategy.Role.newInstance('web_login_test_role', [
      'hudson.model.Hudson.Read',
      'hudson.model.Item.Read',
      'hudson.model.Item.Discover',
      'hudson.model.View.Read',
    ].collect { hudson.security.Permission.fromId(it) }.toSet())
    type = com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType.fromString('globalRoles')

    jenkins.getAuthorizationStrategy().addRole(type, role)
    jenkins.getAuthorizationStrategy().assignRole(type, role, MODULE.username )
  }

  @Test
  void testWebLogin() {
    def webClient = new WebClient()
    webClient.getOptions().setJavaScriptEnabled(false)
    def page = webClient.getPage(MODULE.url)

    def form = page.getFormByName('login')
    form.getInputByName('j_username').setValueAttribute(MODULE.username)
    form.getInputByName('j_password').setValueAttribute(MODULE.password)
    form.getInputByName('remember_me').setChecked(true)

    def button = form.getInputByName('Submit')

    def page2 = button.click()

    assert page2.getTitleText().contains('Jenkins')
  }

  @After
  void tearDown() {
    // Cleaning the test role
    jenkins.getAuthorizationStrategy().getRoleMap(type).removeRole(role)
  }
}
