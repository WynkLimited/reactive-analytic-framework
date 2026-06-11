node('disco-prd-slave') {
    env.deployment_environment="pre-prod"
    env.service_name="reactive-analytic"
    env.docker_registry='asia-south1-docker.pkg.dev'
    env.imagename="pre-disco-reactive-analytic"


    def TIME_OF_BUILD = Calendar.getInstance().getTime().format('YYYY_MM_dd-HH_mm_ss', TimeZone.getTimeZone('IST'))
    def BUILD_SUCCESS = true
    try {
        stage('GitClone') {
            git branch: "$BRANCH_NAME", credentialsId: 'wynkdeployment', url: 'https://github.com/WynkLimited/reactive-analytic-framework.git'
        }
        stage('Build Binary') {
        
            sh """
                
                export PATH=$PATH:/usr/local/go/bin
                export GOPATH=/mnt/jenkins/go
                export CGO_ENABLED=1
                echo "Setting up Go environment..."

                gcloud auth configure-docker ${GCP_REGION}
                go build -o bin/alerter ./cmd/alerter
            """
            
        }

        stage('Build') {
            sh """
            
                docker build -t "$docker_registry"/"${GCP_PROJECT}"/"$imagename"/$TIME_OF_BUILD:${TIME_OF_BUILD} .
            """
        }

        stage('Docker Push') {
            sh """
                gcloud auth configure-docker ${GCP_REGION}
                docker login "$docker_registry"/"${GCP_PROJECT}"/"$imagename"
                docker push "$docker_registry"/"${GCP_PROJECT}"/"$imagename"/$TIME_OF_BUILD:${TIME_OF_BUILD}
            """
        }
        
        stage('Deployment Pipeline') {
            build job: "$CD_DEPLOYMENT_PIPELINE",
                    parameters: [[$class: 'StringParameterValue', name: 'APP_ENV', value: "${deployment_environment}"],
                                 [$class: 'StringParameterValue', name: 'SERVICE_NAME', value: "${service_name}"],
                                 [$class: 'StringParameterValue', name: 'IMAGE_REPO', value: "${docker_registry}\\/${GCP_PROJECT}\\/${imagename}\\/${TIME_OF_BUILD}"],
                                 [$class: 'StringParameterValue', name: 'IMAGE_TAG', value: "${TIME_OF_BUILD}"]],
                    wait: true
        }

        stage('Clean Up') {
            sh """
                rm -rf *
            """
        }
    }
    catch (error) {
        BUILD_SUCCESS = false
        currentBuild.result = 'FAILURE'
    }
    finally {
        if (BUILD_SUCCESS) {
            slackSend channel: '#discovery-gcp-jenkins-build', botUser: true, color: 'good', message: "[SUCCESS] ${env.BUILD_USER} triggered ${env.JOB_NAME}-${env.BUILD_NUMBER} \nURL: ${env.BUILD_URL} \nParameters: ${params}"
        }
        else {
            slackSend channel: '#discovery-gcp-jenkins-build', botUser: true, color: 'danger', message: "[FAILURE] ${env.BUILD_USER} triggered ${env.JOB_NAME}-${env.BUILD_NUMBER} \nURL: ${env.BUILD_URL} \nParameters: ${params}"
        }
    }
}
