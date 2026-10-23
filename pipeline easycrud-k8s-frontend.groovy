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
                    docker build -t matin1402/f-b74:v1 ./frontend
                '''
            }
        }

        stage('Docker Hub Login & Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    sh '''
                        echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                        docker push matin1402/f-b74:v1
                    '''
                }
            }
        }

        stage('Deploy & Check Kubernetes') {
            steps {
                withCredentials([
                    aws(
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        credentialsId: 'aws-creds',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        aws eks update-kubeconfig --region ap-south-1 --name cluster
                        
                        kubectl apply -f frontend/frontend-deploy.yml
                        kubectl apply -f frontend/frontend-svc.yml
                        
                        kubectl get deployments
                        kubectl get pods
                        kubectl get svc frontend-svc
                    '''
                }
            }
        }
    }
}