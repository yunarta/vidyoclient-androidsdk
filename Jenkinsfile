import groovy.json.JsonOutput

properties([
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '25')),
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
        try {
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
        } catch (err) {
        }
    }
}

def buildFailed = false
def errorString = ""

try {
    stage('Build & Test') {
        node(selectedNode) {
            def GRADLE_HOME = tool name: 'Gradle', type: 'gradle'
            try {
                try {
                    sh "scripts/android-start-emulator"
                    sh "scripts/android-wait-for-emulator"
                    sh "$GRADLE_HOME/bin/gradle clean createDebugCoverageReport jacocoTestReport"
                } catch (err) {
                    buildFailed = true
                    errorString = err.toString()
                } finally {
                    sh "scripts/android-kill-emulator"
                }

                try {
                    junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'
                } catch (_) {
                }
                try {
                    jacoco(execPattern: '**/build/jacoco/testDebugUnitTest.exec', classPattern: '**/build/tmp/kotlin-classes/debug', sourcePattern: '**/src/main/kotlin')
                } catch (_) {
                }
            } catch (err) {
                buildFailed = true
                errorString = err.toString()
            }
        }
    }

    stage('Static Analyze') {
        node(selectedNode) {
            parallel "Detekt": {
                def GRADLE_HOME = tool name: 'Gradle', type: 'gradle'
                sh "$GRADLE_HOME/bin/gradle detektCheck"
                checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/detekt-report.xml', unHealthy: ''
            }, "Lint": {
                def GRADLE_HOME = tool name: 'Gradle', type: 'gradle'
                sh "$GRADLE_HOME/bin/gradle lintDebug"
                androidLint canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '', unHealthy: ''
            }
        }
    }
    stage('SonarQube') {
        node(selectedNode) {
            if (["develop", "master"].contains("${env.BRANCH_NAME}".toString())) {

                def properties = new Properties()
                properties.load(new StringReader(readFile('project.properties')))
                def version = properties.getOrDefault("library.version", "1.0.0")

                sh "sonar-scanner -Dsonar.projectVersion=$BUILD_NUMBER -Dsonar.branch=${env.BRANCH_NAME} -Dsonar.projectVersion=${version}"
            }
        }
    }
} catch (err) {
    buildFailed = true
    errorString = err.toString()
} finally {
    stage('End-Build') {
        node('messaging') {
            try {
                httpRequest contentType: 'APPLICATION_JSON',
                        customHeaders: [[name: 'Authorization', value: 'Basic KzJp5B7mDpZ7kMHv67GowQRys9W9Hbaa5Rzj4PCoiyXfTk1fGAvH']],
                        httpMode: 'POST',
                        requestBody: JsonOutput.toJson([
                                "key"        : "${buildTag}",
                                "state"      : buildFailed ? "failed" : "success",
                                "url"        : "${env.RUN_DISPLAY_URL}",
                                "name"       : "${env.JOB_NAME}",
                                "description": buildFailed ? "Build failed ${errorString}" : "",
                                "project"    : "vidyoclient-androidsdk",
                                "revision"   : "${gitCommit}",
                        ]),
                        url: 'http://apps.up.dogeza.club:18090/~buildStatus/'
            } catch (err) {
            }
        }
    }
}