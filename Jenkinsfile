import groovy.json.JsonOutput

properties([
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5')),
        disableConcurrentBuilds()
])

selectedNode = "android"
stage('Checkout') {
    node(selectedNode) {
        selectedNode = env.NODE_NAME
        checkout scm

        gitCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
        shortCommit = gitCommit.take(6)
    }
}

buildTag = "${shortCommit}"
stage('Configure') {
    echo "Build tag = ${buildTag}"
}

stage('Pre-Build') {
    node('messaging') {
        httpRequest contentType: 'APPLICATION_JSON',
                customHeaders: [[name: 'Authorization', value: 'Basic KzJp5B7mDpZ7kMHv67GowQRys9W9Hbaa5Rzj4PCoiyXfTk1fGAvH']],
                httpMode: 'POST',
                requestBody: JsonOutput.toJson([
                        "key"     : "${buildTag}",
                        "state"   : "in_progress",
                        "url"     : "${env.RUN_DISPLAY_URL}",
                        "name"    : "${env.JOB_NAME}",
                        "project" : "vidyoclient-androidsdk",
                        "revision": "${gitCommit}",
                ]),
                url: 'http://apps.up.dogeza.club:18090/~buildStatus/'
    }
}

def buildFailed = false
def errorString = ""

try {
    stage('Build') {
        node(selectedNode) {
            def GRADLE_HOME = tool name: 'Gradle 4.5', type: 'gradle'
            sh "$GRADLE_HOME/bin/gradle build"
        }
    }

    stage('Test') {
        node(selectedNode) {
            def GRADLE_HOME = tool name: 'Gradle 4.5', type: 'gradle'
            sh "$GRADLE_HOME/bin/gradle test"
        }
    }

    stage('Analyze') {
        node(selectedNode) {
            def GRADLE_HOME = tool name: 'Gradle 4.5', type: 'gradle'
            sh "$GRADLE_HOME/bin/gradle detektCheck"
        }
    }

    stage('Report') {
        node(selectedNode) {
            def GRADLE_HOME = tool name: 'Gradle 4.5', type: 'gradle'
            sh "$GRADLE_HOME/bin/gradle detektCheck"
            checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/detekt-report.xml', unHealthy: ''
        }
    }
} catch (err) {

} finally {
    stage('End-Build') {
        node('messaging') {
            httpRequest contentType: 'APPLICATION_JSON',
                    customHeaders: [[name: 'Authorization', value: 'Basic KzJp5B7mDpZ7kMHv67GowQRys9W9Hbaa5Rzj4PCoiyXfTk1fGAvH']],
                    httpMode: 'POST',
                    requestBody: JsonOutput.toJson([
                            "key"     : "${buildTag}",
                            "state"   : buildFailed ? "failed" : "success",
                            "url"     : "${env.RUN_DISPLAY_URL}",
                            "name"    : "${env.JOB_NAME}",
                            "description": buildFailed ? "Build failed ${errorString}" : "",
                            "project" : "vidyoclient-androidsdk",
                            "revision": "${gitCommit}",
                    ]),
                    url: 'http://apps.up.dogeza.club:18090/~buildStatus/'
        }
    }
}