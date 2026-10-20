pipeline {
    agent any

    stages {

        stage('PULL') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/Matintamboli/mt-donald_trumph.git'
            }
        }

        stage('Terraform Installation') {
            steps {
                sh '''
                    wget -O - https://apt.releases.hashicorp.com/gpg | \
                    sudo gpg --batch --yes --dearmor \
                    -o /usr/share/keyrings/hashicorp-archive-keyring.gpg

                    echo "deb [arch=$(dpkg --print-architecture) \
                    signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] \
                    https://apt.releases.hashicorp.com \
                    $(grep -oP '(?<=UBUNTU_CODENAME=).*' /etc/os-release || lsb_release -cs) \
                    main" | \
                    sudo tee /etc/apt/sources.list.d/hashicorp.list

                    sudo apt update
                    sudo apt install -y terraform

                    terraform version
                '''
            }
        }

        stage('AWS CLI Installation') {
            steps {
                sh '''
                    sudo snap install aws-cli --classic
                    aws --version
                '''
            }
        }

        stage('kubectl Installation') {
            steps {
                sh '''
                    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"

                    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"

                    echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check

                    sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

                    rm -f kubectl kubectl.sha256

                    kubectl version --client
                '''
            }
        }

        stage('AWS Credentials') {
            steps {
                withCredentials([
                    aws(
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        credentialsId: 'aws-creds',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        aws sts get-caller-identity
                    '''
                }
            }
        }

        stage('Terraform Init & Apply') {
            steps {
                withCredentials([
                    aws(
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        credentialsId: 'aws-creds',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        terraform init
                        terraform apply -auto-approve
                    '''
                }
            }
        }

        stage('EKS Cluster Login') {
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

        stage('Check EKS Nodes') {
            steps {
                withCredentials([
                    aws(
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        credentialsId: 'aws-creds',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        kubectl get nodes
                    '''
                }
            }
        }
    }
}