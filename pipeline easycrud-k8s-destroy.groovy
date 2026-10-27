pipeline {
    agent any

    stages {

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

        stage('Copy Correct Terraform State') {
            steps {
                sh '''
                    cp /var/lib/jenkins/workspace/cluster-pipeline/terraform.tfstate .

                    echo "===== STATE FILE ====="
                    ls -lh terraform.tfstate
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

        stage('Terraform Init') {
            steps {
                sh '''
                    terraform init
                '''
            }
        }

        stage('Check Resources') {
            steps {
                sh '''
                    echo "===== RESOURCES TO DESTROY ====="
                    terraform state list
                '''
            }
        }

        stage('Terraform Destroy') {
            steps {
                withCredentials([
                    aws(
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        credentialsId: 'aws-creds',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        terraform destroy -auto-approve
                    '''
                }
            }
        }
    }
}