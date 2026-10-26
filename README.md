## Running the backend

Requirements: Java 25, Maven, Docker (for the Postgres container and for tests).

```powershell
cd backend
mvn spring-boot:run
```

If `spring-boot:run` is unavailable, build and run the jar instead:

```powershell
mvn clean package -DskipTests
java -jar target\gym-backend-0.1.0-SNAPSHOT.jar
```

Run tests (needs Docker): `mvn clean test`

### Environment variables

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Database connection | local `gymdb` |
| `JWT_SECRET` | JWT signing key (change in real use) | placeholder |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` | Creates the first admin, only if none exists | empty |
| `EMAIL_ENABLED` | Turns email sending on | `false` |
| `MAIL_HOST`, `MAIL_PORT` | SMTP server (e.g. Brevo: `smtp-relay.brevo.com`, `587`) | Gmail, `587` |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP login and verified sender address | empty |

Set secrets as environment variables on the machine or server. Never commit them to git.
Email is off unless `EMAIL_ENABLED=true`.