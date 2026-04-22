# Changelog — AsteraComm

> Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).
> Itens implementados e aprovados ficam em `[Unreleased]` até a criação de uma versão.

---

## [Unreleased]

### Bug Fixes
- FIX-076: Calls sem circuito associado para ligações entrantes
- FIX-078: Corrige testes de CircuitServiceTest com comportamento alterado

### Refactoring
- RF-094: Relatório de auditoria com direção de chamada (INBOUND/OUTBOUND) e filtro de ligações efetuadas
- RF-095: Script de reconciliação de ligações sem circuito via canal CDR
- RF-098: Refatorar relatório de chamadas órfãs com filtro por período e card no dashboard

---

## [1.0.0] - 2026-04-02

### Features
- US-001: Cadastro de provedores VoIP para troncos de saída com autenticação usuário/senha
- US-002: Pesquisa e visualização de ligações realizadas
- US-003: Cadastro de planos de cobrança com franquia e tarifação por categoria
- US-005: Refatoração — Modelo de Circuito SIP como entidade central do domínio
- US-006: Cadastro de DID (pool de números)
- US-007: Vinculação DID-Circuito com provisionamento automático de Extensions
- US-008: Refatoração — EndpointStatusService delegar conexão AMI ao AmiService
- US-009: Vinculação Circuito-Tronco com provisionamento automático de contextos
- US-070: Cadastro de clientes e vínculo obrigatório com circuito
- US-010: Geração dinâmica de `extensions_trunks.conf` para contextos de tronco
- US-071: Custeio de ligações por circuito com franquia e tarifação
- US-014: Dashboard inicial com visão geral operacional e financeira
- US-015: Relatório de custo de ligações por circuito no período
- US-016: Entidade Call — mapeamento e processamento das ligações do CDR
- US-072: Ferramenta de auditoria de custo de ligações por circuito
- US-017: Enriquecimento de ligações com circuito via channel
- US-018: Vínculo de plano de cobrança ao circuito e seleção de cliente no frontend
- US-019: Migrações de schema com Flyway
- US-020: Ajuste do consumo de pacote de minutagem para frações de 30 segundos
- US-021: Refatoração UX — página de detalhe do circuito e ID no domínio
- US-022: Campo `active` no circuito e regra de exclusão com fallback para desativação
- US-023: Reordenar e ajustar colunas da listagem de circuitos
- US-024: Padronizar estilo dos botões de ação em todo o sistema
- US-027: Refatoração — DID referencia circuito por ID em vez de número
- US-028: Reorganizar colunas e padronizar status na listagem de clientes
- US-029: Padronizar largura de colunas nas listagens do sistema
- US-030: Página de detalhe do cliente
- US-031: Componente de seleção com pesquisa integrada (SearchSelect)
- US-032: Navegação contextual — voltar para cliente ao acessar circuito via página de cliente
- US-033: Botão "Adicionar circuito" na página de detalhe do cliente
- US-034: Ajustar colunas da tabela de DIDs vinculados no circuito
- US-035: Travar seleção e exibir ações ao selecionar item no SearchSelect
- US-036: Redesenhar layout da página de detalhe do circuito
- US-039: Sistema de modais empilhados para edição de registros (Circuito e Cliente)
- US-041: Reestruturação da página de listagem de Circuitos
- US-042: Reestruturação da página de listagem de Clientes
- US-043: Reestruturação da página de listagem de DIDs
- US-044: Reestruturação da página de listagem de Troncos
- US-045: Reestruturação da página de listagem de Ligações (CDR)
- US-046: Reestruturação da página de listagem de Planos
- US-047: Reestruturação do sistema de Relatórios
- US-048: Reestruturação da página de listagem de Usuários
- US-049: Reestruturação da página de Auditoria
- US-050: Reestruturação do sidebar de navegação
- US-051: Reestruturação da tela de Login
- US-052: Migrar frontend para 100% Tailwind CSS
- US-053: Botão "Novo circuito" abre modal de criação
- US-055: Excluir DID livre pela página de listagem de DIDs
- US-056: Contador de registros no rodapé das listagens de Circuitos, Clientes e DIDs
- US-057: Campo `active` no Plano com filtro na listagem
- US-058: Ajustar cor do texto dos IDs nas páginas de Clientes e DIDs
- US-059: Adicionar coluna ID na listagem de Troncos
- US-060: Excluir usuário pelo modal de edição
- US-061: Simplificação de roles — manter apenas ADMIN
- US-062: Refatoração — organizar pacote `report` com sub-pacotes por relatório
- US-063: Modal de detalhes do circuito a partir da página de Auditoria
- US-064: Modais de perfil e senha no padrão do sistema
- US-067: Exibir versão da aplicação abaixo da logo no menu lateral
- US-068: Tronco SIP com autenticação por IP (IP Auth)
- US-069: Refatoração — remover métricas de faturamento do dashboard

### Bug Fixes
- FIX: Auditoria — circuito não encontrado ao processar (seletor usava `id` em vez de `number`)
- FIX-001: Ativação/desativação de usuário via edição não persistia alterações
- FIX-001: Carregamento de DIDs livres no modal de vínculo do circuito
- FIX-002: Modais fora do tamanho correto nas páginas de Ligações e Planos
- FIX-003: Botões do modal desalinhados no modo de criação
- FIX-005: Fetches com limite hardcoded no frontend substituídos por endpoints `summary`
- FIX-007: btn-prev habilitado na primeira página da listagem de Circuitos
- FIX-008: Código gerado automaticamente ao criar circuito usava número de telefone em vez de sequência
- FIX-009: Permitir criação de cliente sem nome
- FIX-010: Planos e clientes inativos aparecendo nos seletores do modal de Circuito
- FIX-011: Gráfico de consumo próximo ao limite sem suporte a pacotes por categoria
- FIX-012: Top 10 circuitos por consumo ignorava planos por categoria
- FIX-068: Cálculo de `minutes_from_quota` armazenava frações em vez de minutos reais
