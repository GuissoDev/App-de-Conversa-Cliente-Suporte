# 🛡️ LordSeg - Comunicação Corporativa e Segurança em Tempo Real

> Um aplicativo Android robusto desenvolvido em Java para comunicação instantânea, gerenciamento de usuários e compartilhamento de mídias, focado em alta disponibilidade e integração contínua com serviços backend.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socket.io&logoColor=white)

## 📌 Sobre o Projeto

O **LordSeg** é um ecossistema mobile projetado para facilitar a comunicação através de chats em tempo real e administração de usuários. Ele demonstra conceitos avançados de desenvolvimento mobile, incluindo o consumo de APIs RESTful, conexões persistentes via WebSockets para troca de mensagens bidirecional, e upload de arquivos multipart (imagens e documentos) para servidores em nuvem.

## 🚀 Principais Funcionalidades

* **Chat em Tempo Real (WebSockets):** Troca de mensagens instantâneas utilizando o protocolo STOMP, com suporte a reconexão resiliente e notificações push nativas.
* **Gestão de Mídias:** Captura de fotos via câmera, seleção na galeria e upload de documentos (PDFs, etc.) com download assíncrono utilizando o `DownloadManager` nativo do Android.
* **Controle de Acesso Baseado em Papéis (RBAC):** UI dinâmica que revela um painel administrativo exclusivo apenas para contas autorizadas (ex: "LordSeg"), permitindo criar e excluir perfis permanentemente.
* **Mecanismo de Pesquisa Direcionado:** Lógica de busca customizada onde a conta matriz tem visão global do sistema, enquanto usuários padrão possuem um escopo de pesquisa restrito.
* **Sessão Segura:** Autenticação via JWT (JSON Web Token) com persistência local de sessão, eliminando a necessidade de logins repetitivos.

## 🛠️ Tecnologias e Arquitetura

O projeto foi construído seguindo boas práticas do ecossistema Android, dividindo as responsabilidades de rede, modelos e UI.

| Categoria | Tecnologias Utilizadas |
| :--- | :--- |
| **Linguagem Base** | Java, Android SDK |
| **Comunicação REST** | Retrofit2, OkHttp3, Gson |
| **Comunicação Real-Time** | STOMP Protocol, RxJava (para observabilidade) |
| **Gerenciamento de Imagens** | Glide |
| **Integração Backend** | Spring Boot, MySQL, AWS (S3 / Elastic Beanstalk) |
