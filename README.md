# SynQ Backend

SynQ 백엔드 서버입니다. 회의 음성을 실시간으로 전사(STT)하고, AI 요약·채팅·참고자료 기반 RAG 검색 등을 제공합니다.

## 팀원

| 이름 | 닉네임 | GitHub |
| --- | --- | --- |
| 인석진 | 제이스 | [sjinssun](https://github.com/sjinssun) |
| 이민규 | 도미닉 | [MinGyuLee2](https://github.com/MinGyuLee2) |
| 이중희 | 조자 | [jungee123213](https://github.com/jungee123213) |
| 박서은 | 써니 | [1PSE](https://github.com/1PSE) |
| 한다경 | 데이 | [handagyeong](https://github.com/handagyeong) |
| 문서찬 | 요한 | [dev-moonsc](https://github.com/dev-moonsc) |

## 기술 스택

- **Language/Framework:** Java 17, Spring Boot 3.5
- **DB/Storage:** PostgreSQL (pgvector), Redis, AWS S3
- **Auth:** Spring Security, JWT, OAuth2 (Kakao/Google/Naver)
- **Migration:** Flyway
- **AI/STT:** OpenAI, Gemini, Soniox(WebSocket 실시간 STT)
- **Docs:** springdoc-openapi(Swagger UI)

## 프로젝트 구조

```
src/main/java/com/synq/backend
├── domain
│   ├── ai            # AI 채팅/요약/RAG
│   ├── auth           # 인증/인가, OAuth
│   ├── meeting        # 회의
│   ├── project        # 프로젝트
│   ├── record         # 녹음/전사 원본
│   ├── reference       # 참고자료 (링크/파일)
│   ├── transcript      # 실시간 전사
│   └── user           # 사용자
└── global            # 공통 설정, 예외 처리 등
```

## 로컬 개발 환경 설정

### 요구사항

- JDK 17 (`mise`로 관리)
- Docker (OrbStack 등)

### 1. 환경변수 설정

```bash
cp .env.example .env
```

`.env` 파일을 열어 필요한 값을 채워넣습니다. (JWT, OAuth 클라이언트, S3, AI API 키 등)

### 2. 인프라 기동 (PostgreSQL, Redis)

```bash
docker compose up -d
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 프로파일은 `local`이며, 서버는 `http://localhost:8080`에서 기동됩니다.

### API 문서

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/v3/api-docs`

### 테스트

```bash
./gradlew test
```

## 기여

작업 워크플로우 및 커밋 컨벤션은 [`docs/convention`](./docs/convention) 문서를 따릅니다.

- [Git 워크플로우](./docs/convention/git-workflow.md)
- [커밋 메시지 컨벤션](./docs/convention/commit-message.md)
