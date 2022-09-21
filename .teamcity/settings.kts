package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Build : BuildType({
    name = "Build"

    params {
        param("env.GH_TOKEN", "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")
        param("env.GH_USERNAME", "TFK70")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        gradle {
            name = "Gradle build (1)"
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
            name = "Login docker"
            scriptContent = "echo %env.GH_TOKEN% | docker login ghcr.io -u %env.GH_USERNAME% --password-stdin"
        }
        dockerCommand {
            name = "Push"
            commandType = push {
                namesAndTags = "ghcr.io/tfk70/testtodo:1.0"
            }
        }
        script {
            name = "Connect to cluster"
            scriptContent = """
                kubectl config set-credentials kubeuser/minikube --username=kubeuser --password=kubepassword
                kubectl config set-cluster minikube --insecure-skip-tls-verify=true --server=http://127.0.0.1:7070
                kubectl config set-context default/minikube/kubeuser --user=kubeuser/minikube --namespace=default --cluster=minikube
                kubectl config use-context default/minikube/kubeuser
            """.trimIndent()
        }
        script {
            name = "Apply all specs"
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
