# Changelog

Todas as alterações relevantes do projeto AsteraComm serão documentadas neste arquivo.

## [0.2.0] - 2026-02-08

### Backend

- **Role SUPER_ADMIN**: Nova role `SUPER_ADMIN` no enum `UserRole`, com authorities `ROLE_SUPER_ADMIN`, `ROLE_ADMIN` e `ROLE_USER`
- **Endpoints de self-update**: `PUT /api/auth/me` (atualizar nome) e `PATCH /api/auth/me/password` (alterar senha com validação da senha atual) no `AuthenticationController`
- **DTOs de perfil**: `ProfileUpdateDTO` e `ProfilePasswordUpdateDTO` para os endpoints de self-update
- **SecurityConfigurations**: Endpoints `/api/users/**` e `/api/auth/register` agora exigem `ROLE_SUPER_ADMIN` (antes era `ROLE_ADMIN`)
- **UserController**: Corrigido parsing do parâmetro `sort` para separar campo e direção (`name,asc` → `Sort.by(ASC, "name")`)
- **CRUD de Endpoint (Circuito)**: Novo `EndpointController` com operações completas em `/api/circuits` — listagem paginada com busca (`GET`), consulta por ID (`GET /{id}`), criação (`POST`), atualização (`PUT /{id}`) e exclusão (`DELETE /{id}`)
- **EndpointService transacional**: Criação de endpoint registra automaticamente Aors, Auth, Endpoint e Extensions (dialplan) em uma única transação. Exclusão remove em cascata extensions, status, endpoint, auth e aors
- **EndpointCreateDTO**: Novo record com campos `number` e `password` para criação/atualização de circuitos
- **Entidade Extension**: Nova entidade mapeando a tabela `extensions` do Asterisk (context, exten, priority, app, appdata) com `ExtensionRepository`
- **Reorganização de pacotes**: Entidades Asterisk movidas de `com.dionialves.AsteraComm.endpoint` para subpacotes em `com.dionialves.AsteraComm.asterisk` (`aors`, `auth`, `endpoint`, `extension`)
- **DevDataSeeder**: Atualizado para criar Extensions nos dados de teste e simplificado com `@RequiredArgsConstructor`
- **SecurityConfigurations**: Adicionadas rotas `/api/user` e `/api/user/**` com restrição `ROLE_SUPER_ADMIN`

### Frontend

- **Menu do usuário no sidebar**: Menu expansível no canto inferior esquerdo com nome do usuário, link "Dados do usuário" e botão "Sair"
- **Página de perfil** (`/user`): Permite ao usuário editar seu nome e alterar sua senha
- **Menu Usuários no sidebar**: Link "Usuários" visível apenas para usuários com role `SUPER_ADMIN`
- **Página de gerenciamento de usuários** (`/users`): Listagem com tabela (ID, Nome, Username, Nível de Acesso, Data de Criação, Data de Atualização), pesquisa, paginação e ordenação por colunas
- **Modal de criação/edição de usuário**: Formulário modal sobre a listagem com campos Nome, Username, Senha e Nível de Acesso. Na edição, a senha só é alterada se o campo for preenchido
- **API routes de proxy**: `/api/auth/me`, `/api/auth/me/password`, `/api/user/users`, `/api/user/[id]` e `/api/user/[id]/password`
- **Layout.astro**: Botão "Sair" substituído pelo menu expansível do usuário com carregamento dinâmico dos dados via `/api/auth/me`
- **Perfil via modais no Layout**: Página `/user` removida; dados cadastrais e troca de senha agora são modais no `Layout.astro` com botões "Dados cadastrais" e "Alterar Senha" no menu do usuário
- **Sistema de Toast**: Componente global `showToast` no Layout para notificações de sucesso/erro com animação slide-in e auto-dismiss
- **CRUD de Circuitos**: Página `/circuits` agora possui modal para criar, editar e excluir circuitos. Campo número com máscara `(49) xxxx-xxxx` e validação de senha (10 caracteres alfanuméricos). Clique na linha da tabela abre o modal de edição
- **API routes de circuitos**: `POST /api/circuit/circuits` para criação, `PUT /api/circuit/[id]` para atualização e `DELETE /api/circuit/[id]` para exclusão de circuitos
