# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application (HTTPS on port 8443)
mvnw.cmd spring-boot:run

# Build
mvnw.cmd clean package

# Compile only (fastest check for errors)
mvnw.cmd compile

# Run tests
mvnw.cmd test
```

On Unix use `./mvnw` instead of `mvnw.cmd`.

## Architecture

**SkillSwap** is a Spring Boot 3.4.3 REST API (Java 17) for a skill-sharing/mentorship platform. The frontend is plain static HTML served directly by Spring Boot from `src/main/resources/static/` — no build step, no bundler, no framework. Every page is a self-contained HTML file that uses `fetch()` to call the REST API and writes results into the DOM.

### Package structure

The application entry point is `com.capstone.demo.SkillSwapApplication`. All other classes use **flat, top-level packages** with no `com.example` prefix:

| Package | Contents |
|---|---|
| `controller` | 21 `@RestController` classes, all under `/api/**` |
| `service` | 21 service classes |
| `repository` | 20 `JpaRepository` extensions |
| `model` | JPA entities |
| `dto`, `payload` | Request/response POJOs (no MapStruct) |
| `enums` | Enum types |
| `config` | Spring config + startup seeders |
| `security`, `security.jwt` | JWT filter, auth entry point |
| `exception` | `ResourceNotFoundException`, `GlobalExceptionHandler`, etc. |
| `validator` | OWASP HTML sanitizer wiring |

### Core data model

| Entity | Key fields / notes |
|---|---|
| `User` | `roles` (LEARNER/MENTOR/SPONSOR), `credits`, `reputation`, `mfaEnabled`/`mfaSecret`, `resetCode` |
| `Session` | `mentor`, `learner`, `skill`, `status` (OPEN→PENDING→SCHEDULED→COMPLETED/CANCELLED), `aiSummary`, `pendingAssessmentId` (Long — pre-generated assessment ID for post-session quiz redirect) |
| `UserSkill` | bridges `User↔Skill` with `type` (LEARN/MENTOR) and `level` (BEGINNER→EXPERT) |
| `Assessment` | `skill`, `questions` (TEXT — entire 5-question MCQ array stored as JSON blob), `passingScore` (default 70) |
| `AssessmentAttempt` | `user`, `assessment`, `score`, `passed`, `completedAt` |
| `Badge` | `name`, `iconName`, `criteriaType` (SESSION_COUNT / RATING_THRESHOLD / CUSTOM), `thresholdValue`, `published` |
| `UserBadge` | links `User↔Badge`; CUSTOM badges created on-the-fly when assessment is passed |
| `CreditTransaction` | `user`, `amount` (positive=earned, negative=spent), `reason`, `sessionId` |
| `SponsorProgram` | `sponsor` (User), `title`, `description`, `status`, `fundingAmount`, `paymentStatus`, `stripeSessionId` |
| `SponsorCoupon` | `sponsor`, `code`, `discount`, `expiryDate`, `usedCount`, `maxUses`, `active` |
| `MentorAvailability` | weekly recurring or specific-date slots |
| `OtpRequest` | email OTP for signup and password reset |
| `Review` | session reviews |
| `AuditLog` | security audit trail |
| `PasswordHistory` | BCrypt hashes of past passwords for reuse prevention |

### Security & Auth

- **JWT** (HMAC-SHA256) validated in `AuthTokenFilter extends OncePerRequestFilter`. The filter runs on every request but **skips** (calls `filterChain.doFilter` and returns) for: URIs matching `.*\.(css|js|png|jpg|jpeg|gif|svg|ico)$`, paths starting with `/css/`, `/js/`, `/images/`, `/static/`, `/webjars/`, `/api/auth/`, root `/`, and any `.html` page. No `shouldNotFilter()` override exists; exclusion is done inside `doFilterInternal`.
- **SecurityConfig** `permitAll()` list covers `/*.html`, `/*.css`, `/js/**`, `/css/**`, `/images/**`, `/static/**`, `/webjars/**`, public `/api/auth/**` endpoints, `/api/skills/**`, and `/api/stripe/webhook`. The catch-all is `.anyRequest().authenticated()`.
- **HTTPS only** — port 8443, PKCS12 keystore at `src/main/resources/keystore.p12`.
- **MFA** — TOTP via `dev.samstevens.totp`; fields on `User`.
- **Rate limiting** — `RateLimitFilter` runs before `AuthTokenFilter`.
- **CORS** — `https://localhost:*` only.
- **Input sanitization** — `InputSanitizer.sanitize()` (OWASP HTML Sanitizer) called explicitly in controllers before persisting user-supplied strings.
- Auth in frontend pages: JWT stored in `localStorage.getItem('jwtToken')`, sent as `Authorization: Bearer <token>` header on every API fetch.

### Frontend pattern

All working pages follow this pattern — **all CSS is inlined in a `<style>` block** within `<head>` (no external CSS file dependency). JS at the bottom calls the REST API with the JWT, then writes to the DOM. There is no shared layout or templating.

The sponsor pages (`sponsor-*.html`) deviate: they reference an external `/sponsor.css` file and contain Thymeleaf `th:` attributes (which are ignored by browsers when served as static files, causing placeholder content instead of real data).

### Session post-completion flow

1. Mentor calls `PUT /api/sessions/{id}/complete`
2. `completeSession()` generates AI summary (Gemini), updates status, transfers credits (1 credit/hour between mentor and learner)
3. Learner visits `sessions.html` → `GET /api/sessions/me/pending-assessment` fires in `init()`
4. If the learner has completed sessions with skills not yet assessed, redirects to `assessment.html?skillId=X&skillName=Y&fromSession=true`
5. Assessment questions generated by Gemini (`POST /api/assessments/generate/{skillId}`); questions stored as JSON in `Assessment.questions`
6. On submit (`POST /api/assessments/{id}/submit`): auto-graded, `AssessmentAttempt` recorded; if score ≥ 70% on first pass → `"Skill Validated: <SkillName>"` CUSTOM badge awarded

### AI integrations (all use Gemini Flash Lite)

All three AI features use the same Gemini endpoint (`gemini-2.0-flash-lite`) and the same key property:

```
gemini.api.key=${GEMINI_API_KEY:}   # empty string default = fallback mode
```

| Service | Fallback when key missing |
|---|---|
| `AiSummaryService` | Returns `"Summary unavailable — please check back later."` |
| `AssessmentService` | Returns 5 placeholder MCQ questions with `"Option A/B/C/D"` |
| `GeminiMatchingService` | Used by `RecommendationController` for AI mentor matching |

All three catch all exceptions internally and always return a non-null result — they never propagate exceptions to callers.

### Configuration

All runtime config is in `src/main/resources/application.properties`:
- **Database:** MySQL at `localhost:3306/skillswapuae`, user `capstone_user`, `ddl-auto=update` (schema auto-migrated, no migration files)
- **Thymeleaf:** `spring.thymeleaf.prefix=classpath:static/` — Thymeleaf is configured but sponsor pages in `static/` are served as raw HTML without processing
- **Mail:** Gmail SMTP, credentials in properties file
- **Stripe:** test keys hardcoded in properties file; `StripeWebhookController` handles events with idempotency via `ProcessedStripeEvent`
- **Seeders** run at startup: `AdminSeeder`, `DataSeeder`, `DataLoader`, `BadgeSeeder` (seeds 10 default SESSION_COUNT and RATING_THRESHOLD badges if table is empty)

### Key patterns

- `@Transactional` on service methods; repositories are plain `JpaRepository` extensions
- `@PrePersist`/`@PreUpdate` on entities for `createdAt`/`updatedAt` timestamps
- DTOs are plain POJOs; no MapStruct
- `ResourceNotFoundException` is the primary domain exception; `GlobalExceptionHandler` maps it to HTTP 404
- Meeting links for online sessions: any valid `https://` URL accepted; platform badge (Zoom/Teams/Meet/Online) derived from URL content at render time, not stored
