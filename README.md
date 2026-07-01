# SkillSwap UAE

**An AI-driven peer-to-peer skill exchange platform**

Graduation Project — Abu Dhabi University, College of Engineering (IT / Cybersecurity Concentration)

---

## Overview

SkillSwap UAE is a web platform that connects people who want to learn new skills with people who can teach them, without money changing hands. Instead of paying for lessons, users trade skills directly — for example, someone who can teach graphic design might exchange sessions with someone who can teach Arabic conversation. The platform uses AI (Groq + Gemini) to recommend relevant skill partners, generate session summaries, and power post-session assessments — across a four-role ecosystem built for real-world community and sponsor involvement.

The project was built as a full capstone deliverable: system design, implementation, testing, and a live committee presentation with an interactive QR code demo for judges to try the platform on their own devices in real time.

---

## Key Features

- **AI-assisted skill matching** — Groq-powered matching (`GroqMatchingService`) recommends skill-exchange partners based on profile, skills, and availability
- **AI-generated session summaries** — `AiSummaryService` summarizes completed sessions
- **AI-powered assessments** — auto-generated post-session MCQ assessments with badge awards
- **Four user roles** — `LEARNER`, `MENTOR`, `SPONSOR`, `ADMIN`
- **Secure authentication** — JWT-based auth (`JwtUtils`, `AuthTokenFilter`) with role-based access control
- **Multi-factor authentication** — TOTP-based MFA plus email OTP verification
- **Rate limiting** — Bucket4j-backed request throttling (`RateLimitFilter`, `RateLimitConfig`)
- **Input sanitization** — OWASP Java HTML Sanitizer integration to prevent XSS
- **Encrypted config values** — Jasypt-based property encryption
- **HTTPS support** — configurable via `HttpsConfig` with a local keystore
- **Audit logging** — `AuditLog` model tracks sensitive account/admin actions
- **Sponsor tooling** — sponsor profiles, programs, coupons, talent browsing, and reporting dashboards
- **Stripe integration** — payment/credit handling with webhook verification (`StripeWebhookController`)
- **Session booking & availability** — mentor availability scheduling and session lifecycle management
- **Reviews & badges** — post-session reviews and a badge/achievement system
- **Email notifications** — session and account emails via Spring Mail
- **QR code interactive access** — live demo access for presentations/events without manual sign-up friction

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring Boot 3 (Web, Security, Data JPA, Validation, WebFlux, Mail) |
| Build Tool | Maven |
| Database | MySQL |
| Frontend | Vanilla JavaScript, HTML, CSS (served as static resources) |
| Auth | JWT (jjwt), TOTP MFA (`totp-spring-boot-starter`, `googleauth`), email OTP |
| AI Integration | Groq API (matching), Gemini API (summaries/assessments) |
| Payments | Stripe (`stripe-java`) |
| Security | OWASP Java HTML Sanitizer, Bucket4j rate limiting, Jasypt encrypted config |
| IDE | Eclipse |
| Documentation | LaTeX (Overleaf) |
| Version Control | Git / GitHub |

---

## Project Structure

```
SkillSwapUAE-2026/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/capstone/demo/    # Spring Boot application entrypoint
│   │   │   ├── config/                # Security, JWT, HTTPS, rate limit, Stripe, seeders
│   │   │   ├── controller/            # REST controllers (Auth, Admin, Session, Sponsor, ...)
│   │   │   ├── dto/                   # Data transfer objects
│   │   │   ├── enums/                 # Role, SessionStatus, SkillCategory, etc.
│   │   │   ├── exception/             # Custom exception handling
│   │   │   ├── model/                 # JPA entities (User, Session, Badge, SponsorProgram, ...)
│   │   │   ├── payload/               # Request/response payloads
│   │   │   ├── repository/            # Spring Data JPA repositories
│   │   │   ├── security/              # JWT filters, auth entry point, user details
│   │   │   │   └── jwt/               # JwtUtils
│   │   │   ├── service/               # Business logic (Auth, AI matching, summaries, badges, ...)
│   │   │   └── validator/             # Input validation
│   │   └── resources/
│   │       ├── application.properties # App/DB/mail/JWT/Stripe/AI config
│   │       └── static/                # Frontend pages (dashboard, browse, sponsor, admin, etc.)
│   └── test/                          # Unit and integration tests
├── pom.xml                            # Maven project configuration (Capstone-Prototype)
└── README.md
```

---

## Getting Started

### Prerequisites

- Java JDK 17+
- Maven 3.8+ (or use the included `mvnw` wrapper)
- MySQL 8+
- Eclipse IDE (or any Spring-compatible IDE)
- Groq API key, Gemini API key
- Stripe account (secret/publishable/webhook keys) for payment features
- Gmail (or other SMTP) credentials for email/OTP delivery

### Installation

1. Clone the repository
   ```bash
   git clone https://github.com/astr0Bits/SkillSwapUAE-2026.git
   cd SkillSwapUAE-2026
   ```

2. Create your local `src/main/resources/application.properties` (do **not** commit this file — see Security note below) based on the required keys:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/skillswapuae?useSSL=false&allowPublicKeyRetrieval=true
   spring.datasource.username=your_username
   spring.datasource.password=your_password

   app.jwt.secret=your_jwt_secret

   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=your-email@gmail.com
   spring.mail.password=your_app_password

   groq.api.key=your_groq_key
   gemini.api.key=your_gemini_key

   stripe.secret.key=your_stripe_secret_key
   stripe.publishable.key=your_stripe_publishable_key
   stripe.webhook.secret=your_stripe_webhook_secret
   ```

3. Build the project
   ```bash
   ./mvnw clean install
   ```

4. Run the application
   ```bash
   ./mvnw spring-boot:run
   ```

5. Access the platform at `http://localhost:8080`

---

## Security Note

⚠️ `application.properties` and any keystore files should **never** be committed with real credentials. Keep them out of version control (`.gitignore`), and rotate any secret that has ever been pushed to a public repo. Use environment variables or a secrets manager for production deployments.

---

## User Roles at a Glance

| Role | Capabilities |
|---|---|
| **Learner** | Register/login (with MFA/OTP), build a skill profile, get AI-recommended mentors, book sessions, receive AI session summaries, take post-session assessments, earn badges, leave reviews |
| **Mentor** | Register/login, list skills offered, set availability, accept/manage sessions, receive reviews |
| **Sponsor** | Manage sponsor profile, run sponsorship programs and coupons, browse talent, view reports/community impact |
| **Admin** | Manage users, moderate content, review audit logs, oversee platform activity and reporting |

---

## Non-Functional Requirements

The system was designed and tested against 8 non-functional requirements:

1. **Security** — JWT auth, MFA/OTP, RBAC, XSS sanitization, rate limiting, encrypted config
2. **Performance** — responsive matching and page load times under expected load
3. **Reliability** — consistent uptime and graceful error handling
4. **Usability** — intuitive UI across all four roles
5. **Availability**
6. **Maintainability**
7. **Scalability**
8. **Portability**

Full testing documentation (functional, non-functional, and UAT test cases) is available in the project report.

---

## Documentation

Full technical documentation — including system design, implementation details (Chapter 7), and testing (Chapter 8) — is maintained separately in the graduation project report (LaTeX/Overleaf).

---

## Team

- Dalal Al-Badwi
- Afrah Noor
- Huda Baig

Institution: Abu Dhabi University
Program: Information Technology — Cybersecurity Concentration
