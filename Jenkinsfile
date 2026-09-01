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

        stage('Build & SonarQube Analysis') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'sonar-token',
                        variable: 'SONAR_TOKEN'
                    )
                ]) {
                    sh '''
                        mvn clean verify \
                            org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                            -Dsonar.projectKey=CIVOX-backend \
                            -Dsonar.host.url=http://192.168.221.133:9000 \
                            -Dsonar.token=$SONAR_TOKEN
                    '''
                }
            }
        }
    }
}