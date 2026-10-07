@Library('Cumulus@1.3-stable') _

pipeline {

  agent {
    kubernetes {
      inheritFrom 'jenkins-jenkins-agent'
      yaml podBuilder.from([maven.podSpec(25), dind.podSpec(), sonar, trivy])
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
                container('maven') {
                  // The pod has no route to the public PyPI, so pip goes through the
                  // Artifactory PyPI remote with the read-only credentials of the maven
                  // container. Python runs in a separate image via the dind daemon.
                  sh '''
                    set -e
                    ROOT="$(pwd)"

                    PYPI_REPO="pypi"
                    for repo in pypi pypi-virtual pypi-local; do
                      if curl -s --netrc -o /dev/null -w "%{http_code}" --max-time 20 \
                          "https://repo.omgeving.vlaanderen.be/artifactory/api/pypi/$repo/simple/mkdocs/" \
                          | grep -q "200"; then
                        PYPI_REPO="$repo"
                        break
                      fi
                    done
                    echo "Using Artifactory PyPI repository: $PYPI_REPO"

                    # Credentials go into a file rather than the command line, so they are never logged.
                    PIP_CONF="$ROOT/.pip-artifactory.conf"
                    trap 'rm -f "$PIP_CONF"' EXIT
                    printf '[global]\nindex-url = https://%s:%s@repo.omgeving.vlaanderen.be/artifactory/api/pypi/%s/simple\n' \
                        "$bamboo_artifactory_ro_user" "$bamboo_artifactory_ro_password" "$PYPI_REPO" > "$PIP_CONF"

                    docker run --rm \
                        -v "$ROOT":/workspace \
                        -w /workspace \
                        -v "$PIP_CONF":/tmp/pip.conf:ro \
                        -e PIP_CONFIG_FILE=/tmp/pip.conf \
                        -e PIP_TRUSTED_HOST=repo.omgeving.vlaanderen.be \
                        -e PIP_DISABLE_PIP_VERSION_CHECK=1 \
                        -e PIP_ROOT_USER_ACTION=ignore \
                        acd-docker.repository.milieuinfo.be/library/python:3.12-alpine \
                        sh -c 'pip install --quiet -r requirements-docs.txt && mkdocs build --strict'

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
