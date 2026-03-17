
# AGENTS.md

## Backend overview
This is the backend of a **web digital signature application** built with:

- Spring Boot
- Java 21
- Maven Wrapper
- Spring Data JPA / Hibernate
- Lombok

Backend root:
- `BackEnd/assinatura`

Do not modify frontend files from this context.

---

## Backend business context
This backend supports a web digital signature system with these main flows:

### Authenticated flow
- a user registers
- a key pair is generated for the user
- an authenticated user signs a text
- the system stores the signature and related metadata

### Public flow
- anyone can verify a signature by ID
- the system returns whether the signature is valid or invalid
- verification attempts are persisted

### Core persisted entities
- `User`
- `UserKey`
- `Signature`
- `VerificationLog`

Entity intent:
- `User`: application user
- `UserKey`: public/private key pair associated with a user
- `Signature`: signed text and signature metadata
- `VerificationLog`: verification attempt log

---

## Backend architecture conventions
- Use Java 21
- Use Spring Boot conventions
- Use Spring Data JPA / Hibernate conventions
- Use Maven Wrapper commands from the backend root
- Keep code explicit, maintainable, and idiomatic

Preferred package organization:
- `domain/entity`
- `repository`
- `service`
- `controller`
- `dto`
- `config`

If an existing package structure is already present, follow it consistently.

---

## JPA conventions
- Prefer explicit, maintainable JPA mappings
- Use `LocalDateTime` for timestamps unless the project already uses another pattern
- Prefer `GenerationType.IDENTITY` unless the project already follows another ID strategy
- Use explicit table names where useful, such as:
  - `users`
  - `user_keys`
  - `signatures`
  - `verification_logs`
- Use DTOs for request/response layers when implementing controllers
- Do not create controllers, services, repositories, or DTOs unless requested

---

## Lombok conventions
- Lombok is allowed and preferred in the backend
- For JPA entities, prefer:
  - `@Getter`
  - `@Setter`
  - `@NoArgsConstructor`
- Use `@AllArgsConstructor` only when actually useful
- Avoid `@Data` on JPA entities unless explicitly requested
- Avoid unnecessarily complex `equals()` and `hashCode()` implementations for JPA entities

---

## Java source file encoding rules
- All `.java` files must be saved as **UTF-8 WITHOUT BOM**
- Never insert or preserve a BOM at the beginning of Java files
- The first character of every Java source file must be the `p` in the `package` declaration
- Do not include invisible characters before `package`
- If recreating or rewriting files, ensure they remain UTF-8 without BOM

Important:
Previous compilation failures in this project were caused by BOM characters in Java files.
Do not repeat this mistake.

---

## Backend quality bar
- Do not leave broken imports, invalid annotations, or unused classes
- Keep Spring code minimal and idiomatic
- Do not create unnecessary abstractions
- Do not create unrelated files
- Keep changes scoped to the request

---

## Backend validation
When changing backend code, always validate from:
- `BackEnd/assinatura`

Use:
```powershell
.\mvnw.cmd clean compile