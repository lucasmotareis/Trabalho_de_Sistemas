# AGENTS.md

## Frontend overview
This is the frontend of a **web digital signature application** built with:

- Next.js
- React
- TypeScript
- Material UI
- App Router

Frontend root:
- `FrontEnd/assinatura_front`

Do not modify backend files from this context.

---

## Frontend business context
This frontend represents a digital signature app with these main flows:

### Public pages
- login
- signup
- verification page
- verification by signature ID

### Authenticated pages
- sign text page

Important:
- the main protected page is the text signing page
- a Material UI dashboard template may be reused only as a **visual/layout reference**
- do not create an unnecessary business dashboard unless explicitly requested

Expected routes:
- `/login`
- `/signup`
- `/verify`
- `/verify/[id]`
- `/sign`

---

## Frontend architecture conventions
- Use Next.js App Router under `src/app`
- Keep `page.tsx` files thin
- Move reusable UI into `src/components`
- Keep API calls in `src/services`
- Keep shared utilities in `src/lib`
- Keep interfaces/types in `src/types`
- Keep Material UI theme files in `src/theme`
- Prefer reusable components over large route files
- Keep imports clean and consistent
- Do not introduce unnecessary global state libraries unless already used

Suggested structure:
- `src/app/(public)/login/page.tsx`
- `src/app/(public)/signup/page.tsx`
- `src/app/(public)/verify/page.tsx`
- `src/app/(public)/verify/[id]/page.tsx`
- `src/app/(auth)/sign/page.tsx`
- `src/components/auth`
- `src/components/layout`
- `src/components/signature`
- `src/services`
- `src/types`
- `src/lib`
- `src/theme`

---

## UI conventions
- Reuse existing Material UI SignIn and SignUp templates if they exist
- Reuse a Material UI dashboard template only for styling/layout if useful
- Do not force unnecessary abstractions
- Preserve existing styling/configuration unless a change is requested
- Prefer simple, maintainable UI composition

---

## Service layer conventions
- Use the existing HTTP client if one already exists
- If there is no API client yet, create a small and typed service layer
- If backend endpoints are not implemented yet, create typed placeholders instead of fake business logic
- Keep request/response models explicit

---

## Frontend quality bar
- TypeScript must stay clean
- Avoid dead code
- Avoid oversized components
- Avoid unnecessary dependencies
- Keep route components focused and readable

---

## Frontend validation
Run frontend validation only from:
- `FrontEnd/assinatura_front`

Use only scripts that already exist in `package.json`.

Typical examples:
```powershell
pnpm install
pnpm lint
pnpm build