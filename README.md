# Thunder API

This is a HTTP API server build with Spring Boot. It provides data and features for Thunder Flutter App.


## Table of Contents
- [Architecture](#architecture)
- [Installation](#installation)


## Architecture

```mermaid
flowchart LR
    API["api-thunder"]
    CORE["core"]
    ERRORS["libraries:errors"]
    DB["infrastructure:postgresql"]
    AWS["infrastructure:aws"]
    FB["infrastructure:firebase"]
    CLIENT["infrastructure:client"]

    API --> CORE
    API --> ERRORS
    API --> DB
    API --> AWS
    API --> FB
    API --> CLIENT
    DB --> CORE
    AWS --> CORE
    FB --> CORE
    CLIENT --> CORE
    CLIENT --> ERRORS
```

Module placement, dependency rules, and naming rules are in [docs/architecture.md](./docs/architecture.md).


### Installation
- need JDK 21 to run Gradle (the Gradle 8.11 wrapper does not run on JDK 25)
- Docker must be running for `./gradlew build` (tests start PostgreSQL with Testcontainers)
- environment variables file (provided by Thunder App Team)
