# Regras de Negócio e Features - TrinketOS

Este documento detalha as funcionalidades implementadas e as regras de negócio vigentes no backend do TrinketOS.

---

## 1. Multi-tenancy (Gestão de Organizações)

O sistema utiliza uma estratégia de **isolamento lógico** de dados.

- **Regra Geral**: Todo `User` e `Ticket` pertence obrigatoriamente a uma `Organization`.
- **Isolamento**: As consultas ao banco (principalmente tickets) devem sempre filtrar pelo `organizationId` do usuário autenticado. Um usuário da Empresa A jamais pode ver dados da Empresa B.
- **Criação de Tenant**: A porta de entrada para uma nova empresa no sistema é o endpoint de "Registro de Tenant", que cria a Organização e o primeiro usuário Administrador simultaneamente.

---

## 2. Autenticação e Segurança (RBAC)

A segurança é gerida via **JWT (JSON Web Token)** e RBAC (Role-Based Access Control).

### Papéis (Roles)
| Role | Descrição | Permissões |
|---|---|---|
| **ROLE_ADMIN** | Administrador da Organização | Pode criar outros usuários (Agentes/Clientes). Tem acesso total aos tickets da organização. Vê Analytics Global. |
| **ROLE_AGENT** | Agente de Suporte | Atende os tickets. Vê Analytics Pessoal e seus Tickets. |
| **ROLE_CUSTOMER** | Cliente Final | Abre tickets para solicitar suporte. Vê apenas seus próprios Tickets. |

### Fluxos de Autenticação
1.  **Registro de Tenant (Público)**:
    -   **Endpoint**: `POST /api/v1/auth/register-tenant`
    -   **Regra**: Cria uma nova `Organization` e um usuário `ROLE_ADMIN` vinculado a ela. Retorna o Token JWT.
2.  **Login (Público)**:
    -   **Endpoint**: `POST /api/v1/auth/login`
    -   **Regra**: Valida email e senha (BCrypt). Retorna o Token JWT contendo `sub` (email), `role` e `organizationId`.
3.  **Registro de Usuários (Protegido - Apenas ADMIN)**:
    -   **Endpoint**: `POST /api/v1/auth/register-user`
    -   **Regra**: Apenas um usuário com token `ROLE_ADMIN` pode acessar.
    -   **Lógica**: O novo usuário é criado automaticamente na **mesma organização** do Administrador que está fazendo a requisição. O Admin define se o novo usuário será `AGENT` ou `CUSTOMER`.

---

## 3. Gestão de Tickets

O core do sistema é o gerenciamento de solicitações de suporte.

### Criação de Ticket
-   **Endpoint**: `POST /api/v1/tickets`
-   **Identificador (Code)**: Gerado automaticamente no formato `TKT-XXXXXXXX` (Único).
-   **Quem pode criar**: Usuários autenticados (Geralmente Clientes, mas Agentes/Admins também podem).
-   **Campos Obrigatórios**: `title`, `description`. `customerId` (opcional, se não informado pode ser inferido ou tratado depois).
-   **Regra de Associação**: O ticket é salvo com o `organizationId` do usuário criador.
-   **Gatilho de IA**: Ao criar um ticket com sucesso, um processo **Assíncrono** é disparado (ver seção IA).

### Listagem de Tickets
-   **Endpoint**: `GET /api/v1/tickets`
-   **Regra**: Retorna todos os tickets da organização do usuário logado.

---

## 4. Inteligência Artificial (TrinketOS AI)

O sistema integra com o **Google Gemini** para automação e auxílio na produtividade.

### Feature: Processamento de Texto (Polimórfico)
-   **Endpoint**: `POST /api/v1/ai/process`
-   **Objetivo**: Ferramenta auxiliar Síncrona para Cliente e Agente.
-   **Modos (Instruction Type)**:
    1.  **REFINE** (Padrão): Reescreve o texto do cliente seguindo o template `Contexto > Problema > Impacto`.
    2.  **SUMMARIZE**: Resume textos longos em um parágrafo conciso para o Agente.
-   **Temperature**: Fixada em `0.1` para garantir assertividade e consistência.

### Feature: Análise Automática de Tickets (Background)
-   **Gatilho**: Ocorre automaticamente após a persistência de um novo Ticket (`TicketAIService.analyzeTicket`).
-   **Processamento Assíncrono (`@Async`)**: Não bloqueia a resposta de criação do ticket para o usuário.
-   **Capabilities**:
    1.  **Sensibilidade (Sentiment Analysis)**: Define como Positivo, Negativo ou Neutro.
    2.  **Prioridade Sugerida**: Define `LOW`, `MEDIUM`, `HIGH` ou `CRITICAL` baseado na urgência.
    3.  **Categorização**: Busca categorias existentes no banco e associa a mais adequada. Se nenhuma servir, pode sugerir nova.
    4.  **Título Refinado**: Reescreve o título para ser mais técnico e conciso.
    5.  **Diagnóstico Técnico**: Breve resumo da provável causa raiz.
    6.  **Solução Sugerida**: Passo a passo de resolução para o agente.
-   **Resultado**: O ticket é atualizado no banco de dados com essas informações.

---

## 5. Analytics e Métricas

Endpoints dedicados à visualização de performance (Dashboards).

### Dashboard Geral
-   **Endpoint**: `GET /api/v1/analytics/dashboard`
-   **Parâmetos**: `range` (WEEK, MONTH, QUARTER, YEAR).
-   **Regra de Visibilidade**:
    -   **ADMIN**: Vê métricas consolidadas de **toda a organização**.
    -   **AGENT**: Vê apenas suas **próprias métricas**.

### Analytics Avançado
-   **Endpoint**: `GET /api/v1/analytics/advanced`
-   **Objetivo**: Dados detalhados para gráficos de distribuição e insights.
-   **Filtro de Agente**:
    -   **ADMIN**: Pode passar `?agentId=UUID` para ver a performance de um agente específico.
    -   **AGENT**: O filtro é ignorado/bloqueado; vê sempre apenas os seus dados.

### Métricas Calculadas
-   **ART (Average Resolution Time)**: Tempo médio entre `createdAt` e `resolvedAt`.
-   **Resolved Count**: Total de tickets fechados no período.
-   **Critical Issues**: Tickets abertos com prioridade CRITICAL.
-   **Distribuições**: Mapas de contagem por Status, Prioridade e Sentimento.

---

---
 
 ## 6. Gestão de Categorias
 
 *   **Padrão**: Criar Categoria `POST /api/v1/categories`.
 *   **Padrão**: Listar `GET /api/v1/categories` (Paginado, Busca por nome/descrição, Ordenação).
 *   **Padrão**: Detalhes `GET /api/v1/categories/{id}`.
 *   **Padrão**: Atualizar `PUT /api/v1/categories/{id}` (Nome e Descrição).
 *   **Padrão**: Remover `DELETE /api/v1/categories/{id}`.
 *   **Padrão**: Contar `GET /api/v1/categories/count`.
 *   **Regra**: Apenas **Admin** ou **Manager** pode criar/atualizar/deletar.
 *   **Regra**: Categorias são isoladas por Organização.
 *   **Uso**: O sistema de IA utiliza essas categorias cadastradas para classificar automaticamente os tickets.
 
 ---
 
 ### 7. Gestão de Times

*   **Padrão**: Criar Time `POST /api/v1/teams` (Gera slug automático).
*   **Padrão**: Listar Times `GET /api/v1/teams` (Paginado, Busca).
*   **Padrão**: Detalhes `GET /api/v1/teams/{id}`.
*   **Padrão**: Atualizar `PUT /api/v1/teams/{id}`.
*   **Padrão**: Remover `DELETE /api/v1/teams/{id}`.
*   **Padrão**: Contar `GET /api/v1/teams/count`.
*   **Regra**: Apenas Admin pode criar/editar/deletar times.
*   **Regra**: Slug é único por organização.
*   **Regra**: Validar nomes (apenas letras, números e espaços).

### 8. Regras de Negócio de Atores
*   **Agente**:
    *   Vinculado diretamente à Organização do Admin que o criou.
    *   Pode ser cadastrado sem time.
    *   **Restrição**: Não pode visualizar ou atuar em tickets se não pertencer a um time (Acesso de Tickets bloqueado).
*   **Cliente (Empresa) / Agente**:
    *   Deve possuir **Documento** (CPF ou CNPJ) cadastrado.
    *   **Lógica**:
        *   11 dígitos -> CPF.
        *   14 dígitos -> CNPJ.
        *   Outros tamanhos -> Erro.
    *   Vinculado à Organização.
*   **Admin**:
    *   Único capaz de cadastrar Agentes e Clientes.

### 9. Gestão de Tickets (CRUD Padrão)

*   **Padrão**: Listar `GET /api/v1/tickets` (Paginado, Filtros).
    *   **Busca (`search`)**: Suporta busca por Título, Descrição e **Código do Ticket**.
    *   **Formatos aceitos**: `TKT-1234ABCD`, `1234ABCD` (Auto-completa prefixo), `TKT-` (Prefixo).
*   **Padrão**: Detalhes `GET /api/v1/tickets/{id}`.
*   **Padrão**: Atualizar `PUT /api/v1/tickets/{id}`.
*   **Padrão**: Remover `DELETE /api/v1/tickets/{id}`.
*   **Padrão**: Contar `GET /api/v1/tickets/count`.

### 10. Gestão de Usuários (CRUD Padrão)

*   **Padrão**: Listar `GET /api/v1/users` (Paginado, Filtros).
*   **Padrão**: Detalhes `GET /api/v1/users/{id}`.
*   **Padrão**: Atualizar `PUT /api/v1/users/{id}` (Admin).
*   **Padrão**: Remover `DELETE /api/v1/users/{id}` (Admin).
*   **Padrão**: Contar `GET /api/v1/users/count` (Opcional: `?role=ROLE_AGENT`).
*   **Padrão**: Criar `POST /api/v1/users` (Admin - via Auth Service).

---

## 9. Resumo Técnico dos Endpoints

| Método | Endpoint | Proteção | Descrição |
|---|---|---|---|
| **POST** | `/api/v1/auth/register-tenant` | **Público** | Cria Empresa + Admin. |
| **POST** | `/api/v1/auth/login` | **Público** | Autentica e gera Token. |
| **POST** | `/api/v1/auth/register-user` | **Admin** | Admin cria Agentes/Clientes. |
| **POST** | `/api/v1/teams` | **Admin** | Cria Time (Auto Slug). |
| **GET** | `/api/v1/teams` | Autenticado | Lista times (Pag/Busca). |
| **GET** | `/api/v1/teams/{id}` | Autenticado | Detalhes do Time. |
| **PUT** | `/api/v1/teams/{id}` | **Admin** | Atualiza Time. |
| **DELETE**| `/api/v1/teams/{id}` | **Admin** | Remove Time. |
| **GET** | `/api/v1/teams/count` | Autenticado | Conta Times. |
| **POST** | `/api/v1/categories` | **Admin/Manager** | Cria Categoria. |
| **GET** | `/api/v1/categories` | Autenticado | Lista (Pag/Busca/Sort). | 
| **GET** | `/api/v1/categories/{id}` | Autenticado | Detalhes Categoria. |
| **PUT** | `/api/v1/categories/{id}` | **Admin/Manager** | Atualiza Categoria. |
| **DELETE**| `/api/v1/categories/{id}` | **Admin/Manager** | Remove Categoria. |
| **GET** | `/api/v1/categories/count` | Autenticado | Conta Categorias. |
| **POST** | `/api/v1/tickets` | Autenticado | Cria ticket + Dispara IA. |
| **GET** | `/api/v1/tickets` | Autenticado | Lista tickets (Pag/Busca). |
| **GET** | `/api/v1/tickets/{id}` | Autenticado | Detalhes do Ticket. |
| **PUT** | `/api/v1/tickets/{id}` | Autenticado | Atualiza Ticket. |
| **DELETE**| `/api/v1/tickets/{id}` | **Admin** | Remove Ticket. |
| **GET** | `/api/v1/tickets/count` | Autenticado | Conta Tickets. |
| **POST** | `/api/v1/ai/process` | Autenticado | Refina ou Resume texto (IA). |
| **GET** | `/api/v1/analytics/dashboard` | Autenticado | Resumo de métricas. |
| **GET** | `/api/v1/analytics/advanced` | Autenticado | Métricas detalhadas. |


