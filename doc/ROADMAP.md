# 🗺️ Roadmap — AsteraComm

Este roadmap descreve o plano de desenvolvimento do AsteraComm, um sistema completo de gerenciamento de servidores Asterisk via interface web.

---

## 🔹 v0.1.0 — Leitura básica de endpoints ✅
- Consultar endpoints registrados via `pjsip show contacts`
- Exibir RTT dos endpoints
- Página simples de visualização

---

## 🔹 v0.2.0 — Dashboard inicial + autenticação
- Autenticação com JWT (usuário e senha)
- Tela de login
- Dashboard com resumo de endpoints ativos/inativos
- Organização inicial do frontend

---

## 🔹 v0.3.0 — Criação e gerenciamento de endpoints
- Criar endpoints (salvar no banco e aplicar no Asterisk)
- Editar e remover endpoints existentes
- Aplicar `reload` automaticamente após mudanças
- Validação de parâmetros essenciais

---

## 🔹 v0.4.0 — Gerenciamento de contextos e dialplans
- Criar, editar e excluir contextos
- Montagem visual de dialplans
- Visualização das extensões (exten => ...)
- Aplicar reload de dialplan

---

## 🔹 v0.5.0 — Canais e chamadas em tempo real
- Listar chamadas ativas em tempo real
- Detalhes por canal (src, dst, duração, contexto)
- Encerrar chamada via interface
- Monitoramento básico de volume de chamadas

---

## 🔹 v0.6.0 — Gravações e CDRs
- Listar gravações e permitir download
- Listar CDRs com filtros por data, ramal, duração
- Exportar relatório de chamadas
- Detalhamento de cada ligação

---

## 🔹 v0.7.0 — Gerenciamento de filas (queues)
- Criar e editar filas
- Adicionar e remover agentes
- Visualizar chamadas em espera e agentes disponíveis
- Exibir métricas de desempenho da fila

---

## 🔹 v0.8.0 — Conferências
- Criar salas de conferência
- Adicionar participantes manualmente
- Controles de moderação (mute, kick)
- Monitorar conferências ativas

---

## 🔹 v0.9.0 — Ferramentas administrativas
- Executar comandos Asterisk via interface
- Acessar logs do sistema
- Editar arquivos de configuração via web
- Backup e restore das configurações

---

## 🔹 v1.0.0 — Primeira versão estável
- Gerenciamento completo de ramais, filas, chamadas e contextos
- Deploy via Docker + Docker Compose
- Interface amigável e responsiva
- Documentação completa no GitHub

---

## 🔮 Futuro
- Integração com WebRTC
- Suporte a multi-tenant
- Integração com CRMs e WhatsApp
- Agendamento de tarefas
- Transcrição de chamadas e análise com IA
