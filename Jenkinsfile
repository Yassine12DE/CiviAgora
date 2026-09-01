pipeline {
    agent any

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"

        SONAR_HOST_URL = 'http://192.168.221.133:9000'
        SONAR_PROJECT_KEY = 'CIVOX-backend'
    }

    stages {

        stage('Environment Check') {
            steps {
                sh '''
                    echo "============================"
                    echo "      ENVIRONMENT CHECK"
                    echo "============================"

                    echo "--- Java ---"
                    java -version

                    echo "--- Javac ---"
                    javac -version

                    echo "--- Maven ---"
                    mvn -version

                    echo "--- JAVA_HOME ---"
                    echo $JAVA_HOME
                '''
            }
        }

        stage('Build Backend') {
            steps {
                dir('CiviAgora-Backend') {
                    sh '''
                        echo "============================"
                        echo "       BUILD BACKEND"
                        echo "============================"

                        mvn clean package -DskipTests
                    '''
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('CiviAgora-Backend') {

                    withCredentials([
                        string(
                            credentialsId: 'sonar-token',
                            variable: 'SONAR_TOKEN'
                        )
                    ]) {

                        sh '''
                            echo "============================"
                            echo "     SONARQUBE ANALYSIS"
                            echo "============================"

                            mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                -Dsonar.projectKey=$SONAR_PROJECT_KEY \
                                -Dsonar.host.url=$SONAR_HOST_URL \
                                -Dsonar.token=$SONAR_TOKEN
                        '''
                    }
                }
            }
        }
    }

    post {

        success {
            echo '======================================'
            echo '✅ CIVOX BACKEND PIPELINE SUCCESS'
            echo '✅ Maven build completed'
            echo '✅ SonarQube analysis completed'
            echo '======================================'
        }

        failure {
            echo '======================================'
            echo '❌ CIVOX BACKEND PIPELINE FAILED'
            echo 'Check the failed stage logs.'
            echo '======================================'
        }
    }
}