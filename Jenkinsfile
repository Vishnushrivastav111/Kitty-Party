// Builds every MicroVault service, then the Angular client.
// Each stage is the same command as that module's own Jenkinsfile.
pipeline {
  agent any
  options {
    timestamps()
    disableConcurrentBuilds()
  }
  environment {
    CI = 'true'
  }
  stages {
    stage('auth-service') {
      steps { script { runMaven('auth-service') } }
    }
    stage('admin-service') {
      steps { script { runMaven('admin-service') } }
    }
    stage('dashboard-service') {
      steps { script { runMaven('dashboard-service') } }
    }
    stage('finance-service') {
      steps { script { runMaven('finance-service') } }
    }
    stage('api-gateway') {
      steps { script { runMaven('api-gateway') } }
    }
    stage('angular-frontend1') {
      steps {
        dir('angular-frontend1') {
          script {
            if (isUnix()) {
              sh 'npm ci && npm run test:coverage'
            } else {
              bat 'npm ci && npm run test:coverage'
            }
          }
        }
      }
    }
  }
  post {
    always {
      junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
      archiveArtifacts artifacts: '**/target/site/jacoco/**, angular-frontend1/coverage/angular-frontend/**', allowEmptyArchive: true
    }
  }
}

void runMaven(String module) {
  dir(module) {
    if (isUnix()) {
      sh 'mvn -B clean verify'
    } else {
      bat 'mvn -B clean verify'
    }
  }
}
