![Build status](https://github.com/navikt/syfojanitor-backend/workflows/main/badge.svg?branch=main)

# syfojanitor-backend

Se hvordan man bruker `syfojanitor` ved å lese README i `syfojanitor-frontend` ([repository](https://github.com/navikt/syfojanitor-frontend))

## Technologies used

* Docker
* Gradle
* Kafka
* Kotlin
* Ktor
* Postgres

##### Test Libraries:

* Kluent
* Mockk
* Spek

#### Requirements

* JDK 17

### Build

Run `./gradlew clean shadowJar`

### Lint (Ktlint)

##### Command line

Run checking: `./gradlew --continue ktlintCheck`

Run formatting: `./gradlew ktlintFormat`

##### Git Hooks

Apply checking: `./gradlew addKtlintCheckGitPreCommitHook`

Apply formatting: `./gradlew addKtlintFormatGitPreCommitHook`

## Contact

### For NAV employees

We are available at the Slack channel `#isyfo`.
