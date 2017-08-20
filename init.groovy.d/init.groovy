/**
 * Simple script to load jenkinsBro from the init.groovy.d
**/
evaluate(new File("${System.getenv("JENKINS_HOME")}/jenkinsBro/jenkinsBro.groovy"))
