# Sistema Web de Assinatura Digital

## 1) Visão Geral do Projeto
Este projeto implementa um **sistema web de assinatura digital** com autenticação por sessão (cookie), geração de assinatura de textos e verificação pública.

Em alto nível, o usuário:
- cria conta
- faz login
- assina um texto
- compartilha/consulta um identificador público para verificação

A verificação pode ser feita publicamente por `publicId` ou manualmente com `publicId + texto + assinatura`.

## 2) Tecnologias Utilizadas
### Frontend
- Next.js (App Router)
- React
- TypeScript
- Material UI (MUI)
- pnpm

### Backend
- Spring Boot
- Java 21
- Maven Wrapper
- Spring Security (sessão/cookie)
- Spring Data JPA / Hibernate

### Banco de dados
- PostgreSQL

### Infraestrutura e banco
- Docker Compose (orquestração completa na raiz)
- Flyway (migrações versionadas)

### Testes
- JUnit 5
- Spring Boot Test
- MockMvc
- Testes de integração no backend

## 3) Estrutura do Repositório
Monorepo com frontend e backend separados:

```text
.
├─ FrontEnd/
│  └─ assinatura_front/                 # App web (Next.js)
├─ BackEnd/
│  └─ assinatura/
│     ├─ src/main/java/...              # API Spring Boot
│     ├─ src/main/resources/
│     │  └─ db/migration/
│     │     └─ V1__init_schema.sql      # Migração Flyway inicial
│     └─ src/test/java/...              # Testes automatizados
└─ docker-compose.yml                   # Stack completa (db + backend + frontend)
```

## 4) Como Rodar o Projeto

### 4.1 Pré-requisitos
- Docker + Docker Compose

### 4.2 Subir tudo com um comando (recomendado)
Na raiz do repositório:

```powershell
docker compose up --build
```

### 4.3 URLs finais
- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html

### 4.4 Execução separada (opcional)
#### Backend
```powershell
cd BackEnd\assinatura
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

#### Frontend
```powershell
cd FrontEnd\assinatura_front
pnpm install
pnpm build
pnpm start
```

## 5) Fluxos Principais do Sistema
1. **Cadastro de usuário**
- Envia nome, email e senha para `/auth/signup`.
- O backend cria usuário, gera par de chaves (RSA) e armazena chave privada criptografada.

2. **Login (stateful)**
- Envia email e senha para `/auth/login`.
- Em sucesso, o backend cria sessão e retorna cookie `JSESSIONID`.

3. **Assinatura de texto (autenticado)**
- Usuário autenticado envia texto para `/sign`.
- Backend assina com chave privada do usuário e persiste a assinatura.

4. **Verificação pública por identificador**
- Qualquer pessoa consulta `/verify/{publicId}`.
- Sistema valida e retorna status + metadados da assinatura.

5. **Verificação manual (publicId + texto + assinatura)**
- Qualquer pessoa chama `POST /verify` com os 3 campos.
- Sistema retorna se a assinatura é válida para aquele texto.

## 6) Endpoints Principais
- `POST /auth/signup`  
  Cadastra usuário.

- `POST /auth/login`  
  Autentica usuário e cria sessão (`JSESSIONID`).

- `POST /auth/logout`  
  Encerra sessão autenticada.

- `GET /auth/me`  
  Retorna usuário da sessão atual.

- `POST /sign`  
  Assina texto (requer sessão autenticada).

- `GET /verify/{publicId}`  
  Verificação pública por identificador.

- `POST /verify`  
  Verificação manual por `publicId + text + signatureBase64`.

## 7) Exemplos de Requisição e Resposta

### 7.1 Cadastro
**Request**
```http
POST /auth/signup
Content-Type: application/json
```

```json
{
  "nome": "Lucas Silva",
  "email": "lucas@example.com",
  "password": "Senha@123"
}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "nome": "Lucas Silva",
  "email": "lucas@example.com"
}
```

### 7.2 Login
**Request**
```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "email": "lucas@example.com",
  "password": "Senha@123"
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "nome": "Lucas Silva",
  "email": "lucas@example.com"
}
```

Observação: o cookie de sessão `JSESSIONID` é retornado no login e usado nas rotas autenticadas.

### 7.3 Assinatura de texto
**Request**
```http
POST /sign
Content-Type: application/json
Cookie: JSESSIONID=...
```

```json
{
  "text": "Texto que será assinado"
}
```

**Response (201 Created)**
```json
{
  "id": 10,
  "publicId": "0a7f4f8d-62a1-4a31-9109-1c6d42c6378f",
  "signatureBase64": "MEQCIF...",
  "hashAlgorithm": "SHA-256",
  "signatureAlgorithm": "SHA256withRSA",
  "createdAt": "2026-03-17T12:10:00"
}
```

### 7.4 Verificação por publicId
**Request**
```http
GET /verify/0a7f4f8d-62a1-4a31-9109-1c6d42c6378f
```

**Response (200 OK)**
```json
{
  "valid": true,
  "publicId": "0a7f4f8d-62a1-4a31-9109-1c6d42c6378f",
  "signerName": "Lucas Silva",
  "hashAlgorithm": "SHA-256",
  "signatureAlgorithm": "SHA256withRSA",
  "createdAt": "2026-03-17T12:10:00",
  "message": "Signature is valid"
}
```

### 7.5 Verificação manual
**Request**
```http
POST /verify
Content-Type: application/json
```

```json
{
  "publicId": "0a7f4f8d-62a1-4a31-9109-1c6d42c6378f",
  "text": "Texto que será assinado",
  "signatureBase64": "MEQCIF..."
}
```

**Response (200 OK) - válido**
```json
{
  "valid": true,
  "publicId": "0a7f4f8d-62a1-4a31-9109-1c6d42c6378f",
  "signerName": "Lucas Silva",
  "hashAlgorithm": "SHA-256",
  "signatureAlgorithm": "SHA256withRSA",
  "createdAt": "2026-03-17T12:10:00",
  "message": "Signature is valid"
}
```

**Response (200 OK) - inválido (texto/assinatura alterados)**
```json
{
  "valid": false,
  "publicId": "0a7f4f8d-62a1-4a31-9109-1c6d42c6378f",
  "signerName": "Lucas Silva",
  "hashAlgorithm": "SHA-256",
  "signatureAlgorithm": "SHA256withRSA",
  "createdAt": "2026-03-17T12:10:00",
  "message": "Signature is invalid"
}
```

## 8) Banco de Dados e Migrações
O projeto usa **Flyway** para versionar e aplicar o schema do banco.

- Localização das migrações: `BackEnd/assinatura/src/main/resources/db/migration`
- Migração atual: `V1__init_schema.sql`

Ao iniciar o backend com Flyway habilitado (como no `docker-compose.yml` da raiz), as tabelas são criadas automaticamente a partir das migrações.

## 9) Casos de Teste (exigidos)
### Caso 1: validação positiva
- Cenário: assinar um texto autenticado e verificar com os dados corretos.
- Resultado esperado: `valid = true`.
- Referência automatizada: `SignatureVerificationIntegrationTest` (cenários de verificação válida).

### Caso 2: validação negativa
- Cenário: assinar um texto e depois alterar o texto **ou** a assinatura antes de verificar.
- Resultado esperado: `valid = false`.
- Referência automatizada: `SignatureVerificationIntegrationTest` (cenários de texto/assinatura alterados).

Além desses casos, o backend possui testes automatizados para cadastro, login stateful, assinatura e persistência de logs.

## 10) Como Executar os Testes
Na pasta do backend:

```powershell
cd BackEnd\assinatura
.\mvnw.cmd test
```

Com repositório local Maven (opcional):

```powershell
cd BackEnd\assinatura
.\mvnw.cmd "-Dmaven.repo.local=.m2\repository" test
```

## 11) Observações Finais
- O identificador público da assinatura (`publicId`) é **não sequencial** (UUID), reduzindo previsibilidade.
- A verificação pública usa esse `publicId` nas rotas `/verify/{publicId}` e `POST /verify`.
- O backend usa autenticação stateful com sessão/cookie (`JSESSIONID`) para rotas protegidas.
- O conteúdo deste README foi alinhado aos contratos atuais implementados no repositório.
