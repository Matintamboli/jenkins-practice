pipeline {
    agent any

    stages {

        stage('PULL') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/Matintamboli/EasyCRUD-Updated.git'
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build -t matin1402/b-b74:v1 ./backend
                '''
            }
        }

        stage('Docker Hub Login') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    sh '''
                        echo "$DOCKER_PASSWORD" | \
                        docker login -u "$DOCKER_USERNAME" --password-stdin
                    '''
                }
            }
        }

        stage('Docker Push') {
            steps {
                sh '''
                    docker push matin1402/b-b74:v1
                '''
            }
        }

        stage('EKS Login') {
            steps {
                withCredentials([
                    aws(
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        credentialsId: 'aws-creds',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        aws eks update-kubeconfig \
                        --region ap-south-1 \
                        --name cluster
                    '''
                }
            }
        }

        stage('Deploy Backend') {
            steps {
                withCredentials([
                    aws(
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        credentialsId: 'aws-creds',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        kubectl apply -f backend/backend-pod.yaml
                        kubectl apply -f backend/backend-svc.yaml
                    '''
                }
            }
        }

        stage('Check Backend') {
            steps {
                withCredentials([
                    aws(
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        credentialsId: 'aws-creds',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        kubectl get pods
                        kubectl get svc backend-svc
                    '''
                }
            }
        }
    }
}