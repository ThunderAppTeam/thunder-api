# Thunder API

This is a HTTP API server build with Spring Boot. It provides data and features for Thunder Flutter App.


## Table of Contents
- [Architecture](#architecture)
- [Installation](#installation)


## Architecture

```mermaid
flowchart LR
    API["core:core-api"]
    DOMAIN["core:core-domain"]
    ERRORS["shared:errors"]
    DB["infrastructure:db-core"]
    AWS["infrastructure:aws"]
    FB["infrastructure:firebase"]
    CLIENT["infrastructure:client"]

    API --> DOMAIN
    API --> ERRORS
    API --> DB
    API --> AWS
    API --> FB
    API --> CLIENT
    DB --> DOMAIN
    FB --> DOMAIN
    CLIENT --> ERRORS
```


### Installation
- need JDK 21 or higher
- environment variables file (provided by Thunder App Team)
