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



        stage('SonarQube Analysis') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'Admin123****',
                        variable: 'squ_a74e6629969926622d8860453982233f0c92e35d'
                    )
                ]) {
                    sh '''
                        mvn sonar:sonar \
                        -Dsonar.projectKey=CIVOX-backend \
                        -Dsonar.host.url=http://192.168.221.133:9000 \
                        -Dsonar.token=$SONAR_TOKEN
                    '''
                }
            }
        }

    }
}