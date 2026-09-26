# Todo Management API

Spring Boot REST API for the [Todo Management frontend](https://github.com/vijayveldurai2/todo-management-frontend). The backend and frontend are maintained in separate repositories.

## Technology

The current `pom.xml` targets Java 17 and Spring Boot 4.0.7. The backend uses Spring Data JPA, Spring Security, JWT, MySQL, Flyway database migrations, and Redis for authentication sessions.

## Prerequisites

- JDK 17 or later, with `JAVA_HOME` configured.
- Docker Desktop with Docker Compose, running before you start Redis.
- A reachable MySQL database and credentials with permission to apply migrations.

The Maven wrapper is included; a separate Maven installation is not required. Run the commands below in PowerShell from the backend repository root.

## Configure the environment

If you do not already have a `.env` file, create one from the example:

```powershell
Copy-Item .env.example .env
```

Edit `.env` with your own settings. Preserve an existing `.env` instead of overwriting it.

| Variable | Purpose |
| --- | --- |
| `DB_URL` | MySQL JDBC URL, for example `jdbc:mysql://127.0.0.1:3306/todo_management` |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | A random secret of at least 32 characters |
| `REDIS_HOST` | Redis hostname; `localhost` for the supplied Compose setup |
| `REDIS_PORT` | Redis port; `6379` for the supplied Compose setup |
| `MAIL_ENABLED` | `false` for local development; verification links are logged to the console |
| `ATTACHMENT_DIRECTORY` | Optional attachment storage location; defaults to `./data/attachments` |

For email delivery, set `MAIL_ENABLED=true` and configure the `MAIL_*` SMTP settings shown in [.env.example](.env.example). The application loads `.env` through its dotenv dependency. The file is ignored by Git; keep credentials out of commits.

Create the database named in `DB_URL` before starting the backend. Flyway manages schema migrations, and Hibernate validates the resulting schema at startup.

## Start locally

### 1. Start Redis with Docker

```powershell
docker compose up -d
```

The supplied [docker-compose.yml](docker-compose.yml) starts **Redis only**, on port `6379`, with a persistent Docker volume. MySQL and the Spring Boot application run separately.

Check Redis or inspect its logs:

```powershell
docker compose ps
docker compose logs redis
```

### 2. Start the backend

With MySQL available and `.env` configured:

```powershell
.\mvnw.cmd spring-boot:run
```

The default backend address is `http://localhost:8080`. Keep this terminal open while using the app. The companion frontend runs separately at `http://localhost:5173`; follow its repository instructions to start it.

### 3. Stop the application

Press `Ctrl+C` in the backend terminal. To stop Redis:

```powershell
docker compose down
```

This retains the Redis data volume.

## Build and test

Run automated tests:

```powershell
.\mvnw.cmd test
```

Build the application, including tests:

```powershell
.\mvnw.cmd clean package
```

Tests that load the application context may require the configured database and Redis. A successful package build produces a JAR in `target/`, which you can run from the repository root:

```powershell
java -jar target/todo-management-0.0.1-SNAPSHOT.jar
```

## Project layout and documentation

- `src/main/java/com/vijay/todo_management/`: application code.
- [application.properties](src/main/resources/application.properties): application configuration and environment variable defaults.
- `src/main/resources/db/migration/`: Flyway database migrations.
- `src/test/`: automated tests.
- [API reference](api_list.md).
- [Routing documentation](docs/URL_ROUTING.md).
- [Product backlog](docs/BACKLOG.md).
- [Comments](docs/todo_comments.md), [attachments](docs/todo_attachments.md), and [subtasks](docs/todo_subtasks.md).

## Troubleshooting

- **Database connection failure:** verify MySQL is running, the database exists, and the `DB_*` values are correct.
- **Redis connection failure:** check `docker compose ps` and confirm `REDIS_HOST` and `REDIS_PORT` match the Compose service.
- **Port already in use:** check for another process using backend port `8080` or Redis port `6379`.
- **No verification email in local development:** with `MAIL_ENABLED=false`, look for the verification link in the backend console.

## Contribution workflow

Keep changes on dedicated feature branches. Changes to `dev`, `stage`, `staging`, `main`, or `master` must go through user-created pull requests. Keep frontend changes in the separate frontend repository. See [AGENTS.md](AGENTS.md) for repository guidelines.
