graph TB
    A[Java 플랫폼] --> B[구현체 종류]
    B --> C[Oracle JDK<br/>유료/상용]
    B --> D[OpenJDK<br/>무료/오픈소스]
    D --> E[여러 벤더의 배포판]
    
    E --> F[Amazon Corretto<br/>아마존 배포판]
    E --> G[AdoptOpenJDK<br/>커뮤니티 배포판]
    E --> H[Azul Zulu<br/>Azul 배포판]
    E --> I[Red Hat OpenJDK<br/>레드햇 배포판]

    style D fill:#f9f,stroke:#333,stroke-width:2px
    style F fill:#ff9,stroke:#333,stroke-width:2px