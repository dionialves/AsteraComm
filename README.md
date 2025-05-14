# AsteraComm

**AsteraComm** é uma plataforma de gerenciamento e monitoramento do [Asterisk](https://www.asterisk.org/), desenvolvida como parte do meu portfólio. O objetivo é fornecer uma interface moderna e intuitiva para administrar sistemas de telefonia, facilitando a integração e o controle de operações.
<br>

## 🚀 Tecnologias Utilizadas

* **Backend:** Java
* **Frontend:** React
* **Servidor Web:** NGINX
* **Banco de Dados:** PostgreSQL
* **Containerização:** Docker
<br>

## 🐳 Arquitetura Baseada em Docker

Todo o projeto é containerizado utilizando o Docker, o que simplifica a configuração e a implantação. Os serviços estão organizados da seguinte forma:

* `asteracomm/`: Contém o backend em Java.
* `nginx/`: Configurações do servidor NGINX para servir o frontend.
* `postgres/`: Configurações do banco de dados PostgreSQL.
* `docker-compose.yml`: Orquestra todos os serviços para facilitar o desenvolvimento e a produção.
<br>

### 🚀 Instalação automática

Siga os comandos abaixo para instalar automaticamenter

1. Executar script:

   ```bash
   bash <(curl -s https://raw.githubusercontent.com/dionialves/AsteraComm/main/install.sh)
   ```

2. Acesse a aplicação:

   * Frontend: [http://localhost](http://localhost)
   * Backend: [http://localhost:8090](http://localhost:8090)  
<br>

#### ⚠️ Considerações para ambientes de produção

Para ambientes de **produção**, é altamente recomendável seguir as boas práticas de segurança:

-  **Altere a senha padrão do PostgreSQL** definida no arquivo `docker-compose.yml`.
-  **Ajuste a configuração do Asterisk** para se conectar com as novas credenciais do PostgreSQL.
-  **Atualize o backend Java** para refletir as novas credenciais e host do banco, editando o arquivo `.env`.
-  **Nunca exponha variáveis sensíveis em repositórios públicos**, como senhas, tokens ou strings de conexão.
<br>

## 📁 Estrutura do Projeto

```
AsteraComm/
├── asteracomm/         # Backend em Java
├── nginx/              # Configurações do NGINX
├── postgres/           # Configurações do PostgreSQL
├── docker-compose.yml  # Orquestração dos containers
└── README.md           # Documentação do projeto
```

## 🚧 Roadmap

Você pode acompanhar o progresso e o planejamento completo do projeto no arquivo [ROADMAP.md](./ROADMAP.md).

Principais etapas:

- [x] **Consulta de endpoints registrados** (v0.1.0)
- [ ] **Dashboard inicial + autenticação** (v0.2.0)
- [ ] **Criação e gerenciamento de endpoints** (v0.3.0)
- [ ] **Gerenciamento de contextos e dialplans** (v0.4.0)
- [ ] **Canais e chamadas em tempo real** (v0.5.0)
- [ ] **Gravações e CDRs** (v0.6.0)
- [ ] **Gerenciamento de filas (queues)** (v0.7.0)
- [ ] **Conferências** (v0.8.0)
- [ ] **Ferramentas administrativas** (v0.9.0)
- [ ] **Primeira versão estável** (v1.0.0)
- [ ] **Futuro** (WebRTC, multi-tenant, CRM, IA, etc)

## 📌 Observações

* O projeto está em desenvolvimento e pode sofrer alterações.
* Contribuições são bem-vindas! Sinta-se à vontade para abrir issues ou pull requests.

## 📄 Licença

Este projeto está licenciado sob a [MIT License](LICENSE).
