// Optional CI step. Assumes the Users API and MySQL are already running on the Jenkins agent.
// Use `bat` instead of `sh` if the agent is Windows.
pipeline {
  agent any
  stages {
    stage('Test') {
      steps {
        sh 'mvn -B clean test'
      }
    }
  }
  post {
    always {
      junit 'target/surefire-reports/*.xml'
      archiveArtifacts artifacts: 'target/extent-report/**', allowEmptyArchive: true
    }
  }
}
