pipeline {
    agent any

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
    }

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

            stage('Environment Check') {
                steps {
                    sh '''
                        echo "===== JAVA ====="
                        java -version

                        echo "===== JAVAC ====="
                        javac -version

                        echo "===== MAVEN ====="
                        mvn -version

                        echo "===== JAVA_HOME ====="
                        echo $JAVA_HOME
                        '''
                    }
                }
            }

            stage('Build & SonarQube Analysis') {
                steps {
                        dir('CiviAgora-Backend') {
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
}