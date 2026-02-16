# Thunder API

This is a HTTP API server build with Spring Boot. It provides data and features for Thunder Flutter App.


## Table of Contents
- [Architecture](#architecture)
- [Installation](#installation)


## Architecture

```mermaid
flowchart TB
    subgraph ROOT["thunder-api (Gradle Multi-module)"]
        API["core:core-api\nExecutable Spring Boot API"]
        DOMAIN["core:core-domain\nDomain models + ports (Adapter interfaces)"]
        DB["storage:db-core\nPersistence adapters (JPA/JDBC)"]
    end

    API -->|implementation| DOMAIN
    DB -->|implementation| DOMAIN
    API -.->|compileOnly| DB

    CLIENT["Thunder Flutter App"] -->|HTTP| API
    API -->|Flyway/JPA| PG[(PostgreSQL)]
    API -->|SDK| S3[(AWS S3)]
    API -->|Admin SDK| FCM[(Firebase FCM)]
    API -->|HTTP Client| SMS[(Aligo SMS)]
```


### Installation
- need JDK 21 or higher
- environment variables file (provided by Thunder App Team)
