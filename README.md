# TrinketOS - Backend

Sistema SaaS de tickets multi-tenant com Inteligência Artificial.

## 🚀 Como Executar

### 1. Pré-requisitos
- Java 21+
- Docker & Docker Compose
- API Key do Google Gemini

### 2. Configuração
1. Copie o arquivo de exemplo `.env.example` para `.env`:
   ```bash
   cp .env.example .env
   ```
2. Edite o arquivo `.env` e adicione sua **GOOGLE_GEMINI_API_KEY**.

### 3. Banco de Dados
Inicie o banco PostgreSQL via Docker:
```bash
docker-compose up -d
```

### 4. Executando a Aplicação
Execute o projeto via Maven Wrapper:
```bash
./mvnw spring-boot:run
```

A aplicação iniciará na porta **8080**.

---

## 🔗 Endpoints e Documentação

A documentação interativa (Swagger UI) está disponível em:
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

### Principais Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/v1/auth/register-tenant` | Registra nova Organização e Admin |
| POST | `/api/v1/auth/login` | Login (Retorna JWT) |
| POST | `/api/v1/tickets` | Cria ticket (+ Análise IA automática) |
| GET | `/api/v1/tickets` | Lista tickets da organização |
| POST | `/api/v1/ai/refine` | Refina texto de descrição com IA |

> **Nota:** Para os endpoints protegidos, copie o token JWT retornado no login e use o botão "Authorize" no Swagger.

---

## 🛠️ Testando o Fluxo

1. **Registrar Tenant**: Crie uma organização e um usuário admin.
2. **Login**: Faça login com esse usuário para obter o Token.
3. **Criar Ticket**: Use o token para criar um ticket. O campo `priority`, `sentiment` e `category` serão preenchidos automaticamente pela IA em background.
4. **Verificar IA**: Consulte o ticket criado e veja os campos preenchidos.
