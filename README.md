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

| Método | Endpoint | Proteção | Descrição |
|---|---|---|---|
| **POST** | `/api/v1/auth/register-tenant` | **Público** | Cria Empresa + Admin. |
| **POST** | `/api/v1/auth/login` | **Público** | Autentica e gera Token. |
| **POST** | `/api/v1/auth/register-user` | **Admin** | Admin cria Agentes/Clientes. |
| **POST** | `/api/v1/teams` | **Admin** | Cria Time (Auto Slug). |
| **GET** | `/api/v1/teams` | Autenticado | Lista (Pag/Busca). |
| **GET** | `/api/v1/teams/{id}` | Autenticado | Detalhes. |
| **PUT** | `/api/v1/teams/{id}` | **Admin** | Atualiza. |
| **DELETE**| `/api/v1/teams/{id}` | **Admin** | Remove. |
| **POST** | `/api/v1/categories` | **Admin/Manager** | Cria Categoria. |
| **GET** | `/api/v1/categories` | Autenticado | Lista (Pag/Busca/Sort). |
| **GET** | `/api/v1/categories/{id}` | Autenticado | Detalhes Categoria. |
| **PUT** | `/api/v1/categories/{id}` | **Admin/Manager** | Atualiza (Name/Desc). |
| **DELETE**| `/api/v1/categories/{id}` | **Admin/Manager** | Remove Categoria. |
| **GET** | `/api/v1/categories/count` | Autenticado | Conta Categorias. |
| **POST** | `/api/v1/tickets` | Autenticado | Cria ticket + IA. |
| **GET** | `/api/v1/tickets` | Autenticado | Lista (Busca: Título/Desc/Code `TKT-`). |
| **GET** | `/api/v1/tickets/{id}` | Autenticado | Detalhes. |
| **PUT** | `/api/v1/tickets/{id}` | Autenticado | Atualiza. |
| **DELETE**| `/api/v1/tickets/{id}` | **Admin** | Remove. |
| **POST** | `/api/v1/ai/process` | Autenticado | IA (Refina/Resume). |
| **GET** | `/api/v1/analytics/dashboard` | Autenticado | Resumo de métricas. |
| **GET** | `/api/v1/analytics/dashboard` | Autenticado | Resumo de métricas. |
| **GET** | `/api/v1/analytics/advanced` | Autenticado | Métricas detalhadas. |
| **GET** | `/api/v1/users` | Autenticado | Lista Usuários. |
| **GET** | `/api/v1/users/{id}` | Autenticado | Detalhes Usuários. |
| **PUT** | `/api/v1/users/{id}` | **Admin** | Atualiza Usuários. |
| **DELETE**| `/api/v1/users/{id}` | **Admin** | Remove Usuários. |
| **GET** | `/api/v1/users/count` | Autenticado | Conta Usuários (Filtro). |

> **Nota:** Para os endpoints protegidos, copie o token JWT retornado no login e use o botão "Authorize" no Swagger.

---

## 🛠️ Testando o Fluxo

1. **Registrar Tenant**: Crie uma organização e um usuário admin.
2. **Login**: Faça login com esse usuário para obter o Token.
3. **Criar Ticket**: Use o token para criar um ticket. O campo `title` (refinado), `priority`, `sentiment` e `category` serão preenchidos automaticamente pela IA em background.
4. **Verificar IA**: Consulte o ticket criado e veja os campos preenchidos.
