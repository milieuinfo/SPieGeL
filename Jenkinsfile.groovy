@Library('Cumulus@1.3-stable') _

def pythonPodSpec = '''
spec:
  containers:
    - name: python
      image: acd-docker.repository.milieuinfo.be/library/python:3.12-alpine
      command:
        - cat
      tty: true
      resources:
        requests:
          memory: "256Mi"
          cpu: "250m"
        limits:
          memory: "1Gi"
'''

pipeline {

  agent {
    kubernetes {
      inheritFrom 'jenkins-jenkins-agent'
      yaml podBuilder.from([maven.podSpec(25), pythonPodSpec, sonar, trivy])
    }
  }

  options {
    disableConcurrentBuilds()
  }

  environment {
    GH_PAGES_BRANCH   = 'gh-pages'
    GITHUB_REPO       = 'milieuinfo/SPieGeL'
    SONAR_PROJECT_KEY = 'be.vlaanderen.omgeving.spiegel:spiegel-parent'
  }

  stages {

    stage('Setup') {
      steps {
        script {
          // The Maven stages only run once the Maven skeleton exists.
          env.HAS_POM = fileExists('pom.xml') ? 'true' : 'false'
          if (env.BRANCH_IS_PRIMARY && env.HAS_POM == 'true') {
            properties([versions.releaseParameters()])
            if (versions.isRelease()) {
              def currentVersion = maven.version()
              def version = versions.bump(currentVersion)
              git.validateTag(version)
              maven.validateVersion(version)
              env.VERSION = version
            }
          }
        }
      }
    }

    stage('CI') {
      when {
        expression { git.notSkipCi() }
      }

      stages {

        stage('Build') {
          parallel {

            stage('Docs (MkDocs)') {
              steps {
                container('python') {
                  sh '''
                    set -e
                    python -m venv .venv
                    . .venv/bin/activate
                    pip install --quiet -r requirements-docs.txt
                    mkdocs build --strict
                    touch site/.nojekyll
                  '''
                }
              }
              post {
                always {
                  archiveArtifacts artifacts: 'site/**', allowEmptyArchive: true, fingerprint: true
                }
              }
            }

            stage('Maven verify') {
              when {
                expression { env.HAS_POM == 'true' && !env.BRANCH_IS_PRIMARY }
              }
              steps {
                script {
                  maven.goal([goal: 'verify'])
                }
              }
            }

            stage('Trivy scan') {
              when {
                expression { env.HAS_POM == 'true' }
              }
              steps {
                script {
                  trivy.scanFilesystem([targetPath: 'pom.xml'])
                }
              }
            }
          }
        }

        stage('Deploy docs to GitHub Pages') {
          when {
            branch 'main'
          }
          steps {
            container('jnlp') {
              script {
                git.withGitAuth {
                  sh '''
                    set -e
                    REPO_URL=$(git config --get remote.origin.url)
                    rm -rf .gh-pages-deploy
                    git clone --depth 1 --branch "$GH_PAGES_BRANCH" "$REPO_URL" .gh-pages-deploy \
                        || git clone --depth 1 "$REPO_URL" .gh-pages-deploy

                    cd .gh-pages-deploy
                    git checkout -B "$GH_PAGES_BRANCH"
                    # Remove top-level entries only, and never .git itself.
                    find . -mindepth 1 -maxdepth 1 ! -name '.git' -exec rm -rf {} +
                    cp -R ../site/. .

                    git config user.email "$GIT_USER_EMAIL"
                    git config user.name "$GIT_USER_NAME"
                    git add -A
                    if ! git diff --cached --quiet; then
                      git commit -m "docs: deploy from ${BUILD_TAG}"
                      git push origin "$GH_PAGES_BRANCH"
                    else
                      echo "No changes to deploy"
                    fi
                  '''
                }
              }
            }
          }
        }
      }
    }

    stage('Primary branch') {
      when {
        allOf {
          expression { env.BRANCH_IS_PRIMARY }
          expression { env.HAS_POM == 'true' }
          expression { git.notSkipCi() }
        }
      }

      stages {

        stage('Maven prepare') {
          when {
            expression { versions.isRelease() }
          }
          steps {
            script {
              maven.goal([
                goal     : 'release:clean release:prepare',
                version  : env.VERSION,
                skipTests: true
              ])
            }
          }
        }

        stage('Maven deploy') {
          steps {
            script {
              maven.goal([goal: 'deploy'])
            }
          }
        }

        stage('Sonar scan') {
          steps {
            script {
              sonar.scanMaven([
                projectKey        : env.SONAR_PROJECT_KEY,
                tolerateBadQuality: true
              ])
            }
          }
        }

        stage('Maven release') {
          when {
            expression { versions.isRelease() }
          }
          steps {
            script {
              maven.goal([
                goal     : 'release:perform',
                version  : env.VERSION,
                skipTests: true
              ])
            }
          }
        }
      }
    }
  }
}
