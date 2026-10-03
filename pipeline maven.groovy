pipeline {
    agent any

    stages {
        stage('PULL') {
            steps {
                git branch: 'main', url: 'https://github.com/Rohit-1920/EasyCRUD-Updated.git'
            }
        }
        stage('Build') {
            steps {
                sh '''cd backend
                    mvn clean package -DskipTests'''
            }
        }   
    }
}


# DskipTests is for skipping the test , bcz we have not done integration between backend and database in easy crud project 
