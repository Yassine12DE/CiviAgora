pipeline {
    agent any

    stages {

        stage('Testing Maven') {
            steps {
                sh 'mvn -version'
            }
        }

        stage('Testing JDK') {
            steps {
                sh 'java -version'
            }
        }
    }
}