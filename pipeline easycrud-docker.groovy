pipeline {
    agent any

    stages {
        stage('Clone Repo') {
            steps {
                git url: "https://github.com/Matintamboli/EasyCRUD-Updated.git", branch: "main"
            }
        }
        stage('Remove previous images') {
            steps {
               sh '''docker rm -f $(docker ps -aq) || true
                docker rmi -f $(docker images -aq) || true'''
            }
        }

        stage('Build Backend Image') {
            steps {
                sh '''cd backend
                docker build . -t matin1402/b-b74:v1'''
            }
        }

        stage('Build Frontend Image') {
            steps {
                sh '''cd frontend
                docker build . -t matin1402/f-b74:v1'''
            }
        }

        

        stage('Docker Hub Login') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                sh "echo $PASSWORD | docker login -u $USERNAME --password-stdin"
        }
                }
            }
        

        stage('Push  Image to Docker Hub') {
            steps {
                sh '''docker push matin1402/b-b74:v1
                docker push matin1402/f-b74:v1'''
            }
        }
        stage('Deploy') {
            steps {
                sh '''docker run -d -p  8080:8080 --name backend matin1402/b-b74:v1
                docker run -d -p  80:80 --name frontend matin1402/f-b74:v1'''
            }
        }

    }
}