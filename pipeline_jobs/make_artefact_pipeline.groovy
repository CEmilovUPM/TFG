pipeline {
    agent any

    stages {
        stage('Diagnostics'){
            steps {
                sh 'java -version'
            }
        }

        stage('Checkout') {
            steps {
                git 'https://github.com/CEmilovUPM/TFG.git'
            }
        }

        stage('Build with Maven Wrapper') {
            steps {
                sh 'chmod +x ./mvnw'
                sh './mvnw -Dmaven.test.failure.ignore=true clean package'
            }
        }
        // stage('Build Docker Image') {
        //     steps {
        //         sh 'docker build -t whatevernameigivethis .'
        //     }
        // }
    }

    post {
        success {
            echo 'Build completed successfully!'
        }
        failure {
            echo 'Build failed.'
        }
    }
}