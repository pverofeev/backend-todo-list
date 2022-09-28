import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2022.04"

project {

    buildType(Build)
}

object Build : BuildType({
    name = "Build 111"

    params {
        param("env.GH_USERNAME", "TFK70")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        gradle {
            name = "Gradle build"
            tasks = "clean build"
            buildFile = "build.gradle"
        }
        dockerCommand {
            name = "Build image"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "ghcr.io/tfk70/testtodo:1.0"
            }
        }
        script {
            name = "Login to ghcr"
            scriptContent = "echo %env.GH_TOKEN% | docker login ghcr.io -u %env.GH_USERNAME% --password-stdin"
        }
        dockerCommand {
            name = "Push image"
            commandType = push {
                namesAndTags = "ghcr.io/tfk70/testtodo:1.0"
            }
        }
        script {
            name = "Connect to kubernetes cluster"
            scriptContent = """
                kubectl config set-credentials kubeuser/minikube --username=kubeuser --password=kubepassword
                kubectl config set-cluster minikube --insecure-skip-tls-verify=true --server=http://127.0.0.1:7070
                kubectl config set-context default/minikube/kubeuser --user=kubeuser/minikube --namespace=default --cluster=minikube
                kubectl config use-context default/minikube/kubeuser
            """.trimIndent()
        }
        script {
            name = "Apply specs"
            scriptContent = """
                kubectl apply -f %teamcity.build.checkoutDir%/deployment/common/namespace.yml
                kubectl apply -f %teamcity.build.checkoutDir%/deployment/postgres/postgres.yml
                kubectl apply -f %teamcity.build.checkoutDir%/deployment/postgres/postgres-service.yml
                kubectl apply -f %teamcity.build.checkoutDir%/deployment/backend/deployment.yml
                kubectl apply -f %teamcity.build.checkoutDir%/deployment/backend/service.yml
                kubectl apply -f %teamcity.build.checkoutDir%/deployment/backend/gateway.yml
            """.trimIndent()
        }
    }

    triggers {
        vcs {
        }
    }
})
