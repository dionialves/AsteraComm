# AsteraComm Backend

AsteraComm Backend é a parte do sistema responsável pela comunicação com o Asterisk. Atualmente, o backend fornece uma API simples que consulta e exibe os endpoints registrados no Asterisk. A arquitetura foi projetada para ser expandida futuramente, com a adição de funcionalidades como gerenciamento de chamadas, filas e outros recursos do Asterisk. O objetivo é fornecer uma plataforma escalável e centralizada para monitorar e administrar o Asterisk de maneira eficiente.

O backend é a espinha dorsal do AsteraComm, e pode ser integrado com o frontend para oferecer uma solução completa de monitoramento e gerenciamento do Asterisk.
<br>
<br>
## 🚀 Tecnologias

- **Java 21**: A principal linguagem utilizada no desenvolvimento do backend.
- **Spring Boot**: Framework para a construção de APIs RESTful.
- **PostgreSQL**: Banco de dados utilizado para armazenar informações sobre o Asterisk e os dados do sistema.
- **JPA/Hibernate**: Para a interação com o banco de dados.
- **Maven**: Gerenciador de dependências e construção do projeto.
<br>

## 🌐 Repositórios

- **Projeto Principal**: [AsteraComm](https://github.com/dionialves/AsteraComm)
  - O repositório principal inclui o frontend e a documentação completa do projeto.
  
- **Backend**: [AsteraComm-backend](https://github.com/dionialves/AsteraComm-backend)
  - Este repositório contém apenas o backend, a API que interage com o Asterisk.
  
- **Frontend**: [AsteraComm-frontend](https://github.com/dionialves/AsteraComm-frontend)
  - Repositório do frontend, onde o gerenciamento visual do Asterisk acontece.
<br>

## ⚙️ Como Rodar o Backend Localmente

### ✅ Pré-requisitos para execução do AsteraComm-backend

Para executar este projeto, você precisará ter os seguintes itens instalados e funcionando:

- Java 21
- PostgreSQL (acessível e em funcionamento)
- Arquivo `.env` na raiz do projeto com as variáveis:

```env
DB_URL=localhost
DB_USER=seu_usuario
DB_PASSWORD=sua_senha
```

### Passos para rodar

1. Clone o repositório:
    ```bash
    git clone https://github.com/dionialves/AsteraComm-backend.git
    cd AsteraComm-backend
    ```

2. Construa o projeto:
    ```bash
    mvn clean install
    ```

3. Inicie o backend:
    ```bash
    mvn spring-boot:run
    ```

4. A API estará disponível em `http://localhost:8090`.
<br>

## 📦 Endpoints Disponíveis

Atualmente, o único endpoint disponível é:

### /api/endpoints

- **GET**: Lista todos os endpoints registrados no Asterisk.

Demais endpoints, como gerenciamento de chamadas, filas e outros, serão implementados em versões futuras.
<br>

## 🔗 Links Úteis

- [Documentação do Asterisk](https://wiki.asterisk.org/)
- [AsteraComm Projeto Principal](https://github.com/dionialves/AsteraComm)
- [AsteraComm Frontend](https://github.com/dionialves/AsteraComm-frontend)
- [AsteraComm Backend](https://github.com/dionialves/AsteraComm-backend)

## 📄 Licença

Distribuído sob a licença MIT. Veja `LICENSE` para mais informações.
