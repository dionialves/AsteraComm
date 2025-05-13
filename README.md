# AsteraComm

**AsteraComm** é uma plataforma de gerenciamento e monitoramento do [Asterisk](https://www.asterisk.org/), desenvolvida como parte do meu portfólio. O objetivo é fornecer uma interface moderna e intuitiva para administrar sistemas de telefonia, facilitando a integração e o controle de operações.

## 🚀 Tecnologias Utilizadas

* **Backend:** Java
* **Frontend:** React
* **Servidor Web:** NGINX
* **Banco de Dados:** PostgreSQL
* **Containerização:** Docker

## 🐳 Arquitetura Baseada em Docker

Todo o projeto é containerizado utilizando o Docker, o que simplifica a configuração e a implantação. Os serviços estão organizados da seguinte forma:

* `asteracomm/`: Contém o backend em Java.
* `nginx/`: Configurações do servidor NGINX para servir o frontend.
* `postgres/`: Configurações do banco de dados PostgreSQL.
* `docker-compose.yml`: Orquestra todos os serviços para facilitar o desenvolvimento e a produção.

## ⚙️ Instalação e Execução

> *Nota: Em breve, será disponibilizado um script automatizado para facilitar a instalação e configuração do projeto.*

Enquanto isso, você pode iniciar o projeto manualmente seguindo os passos abaixo:

1. Clone o repositório:

   ```bash
   git clone https://github.com/dionialves/AsteraComm.git
   cd AsteraComm
   ```

2. Construa e inicie os containers:

   ```bash
   docker-compose up --build
   ```

3. Acesse a aplicação:

   * Frontend: [http://localhost](http://localhost)
   * Backend: [http://localhost:8090](http://localhost:8090)

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

---

*Desenvolvido por [Dioni A. Oliveira](https://github.com/dionialves)*
