pipeline {
    agent any

    stages {

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
                        cp /home/ubuntu/workspace/job2/terraform.tfstate .

                        terraform init
                        terraform destroy -auto-approve
                    '''
                }
            }
        }
    }
}