# AGENTS.md

## Project overview
This repository is a monorepo for a **web digital signature application**.

It contains two independent applications:

- **frontend**: `FrontEnd/assinatura_front`
- **backend**: `BackEnd/assinatura`

Always identify which application is the target before making changes.

---

## Business context
This project is a web digital signature system with these main flows:

### Public flow
- Anyone can access a public verification page
- A signature can be verified by ID
- The verification result should show whether the signature is valid or invalid
- When available, show signer, algorithm, and timestamp

### Authenticated flow
- A user can register
- A user can log in
- An authenticated user can type text into a textarea and sign it
- The system stores the signature and its metadata

### Backend persistence requirements
The backend persists:
- users
- user key pairs
- signatures
- verification logs

Current core backend domain model:
- `User`
- `UserKey`
- `Signature`
- `VerificationLog`

---

## Monorepo rules
- Do not modify both frontend and backend unless explicitly requested
- Focus only on the relevant application for the current task
- Before making changes, inspect the existing folder structure and current implementation
- Prefer small, maintainable, reusable code
- Do not invent unnecessary features
- Do not add libraries unless necessary
- Preserve existing project structure where possible
- Avoid broad unrelated refactors
- Keep code compiling/runnable after changes

---

## Scope rules

### When the task is about frontend
Work only inside:
- `FrontEnd/assinatura_front`

Do not modify backend files unless explicitly requested.

Typical frontend tasks:
- routing
- pages
- React components
- forms
- Material UI
- frontend API client integration
- frontend types
- authenticated/public page structure

### When the task is about backend
Work only inside:
- `BackEnd/assinatura`

Do not modify frontend files unless explicitly requested.

Typical backend tasks:
- entities
- repositories
- services
- controllers
- DTOs
- security
- database persistence
- migrations
- tests

---

## Validation expectations
- If frontend code changes, validate using the frontend project's own scripts if available
- If backend code changes, validate using Maven Wrapper from the backend directory
- Do not claim work is complete if validation fails

---

## Delivery expectations
At the end of each task, always summarize:
1. files created
2. files changed
3. assumptions made
4. validation performed
5. whether validation succeeded or failed