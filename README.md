# TideFlow

## Sobre o Projeto

O TideFlow é uma plataforma B2B de saúde mental corporativa que utiliza inteligência artificial para oferecer suporte terapêutico individualizado aos colaboradores e, simultaneamente, fornecer insights agregados em tempo real para gestão de recursos humanos. A solução combina um aplicativo de chat terapêutico com IA para os funcionários e um dashboard analítico para empresas, permitindo monitoramento proativo do bem-estar organizacional sem violar a privacidade individual.

### Proposta de Valor

O sistema resolve o problema das pesquisas de clima organizacional tradicionais, que são realizadas anualmente e frequentemente respondidas com receio e falta de sinceridade. O TideFlow oferece uma alternativa contínua e proativa, utilizando análise de sentimento e processamento de linguagem natural para identificar padrões de estresse, burnout e risco de turnover antes que se tornem problemas críticos.

### Funcionalidades Principais

**Para Colaboradores (App "My Tide")**
- Chat seguro com IA terapeuta para desabafos e suporte emocional
- Garantia de privacidade: dados individuais não são compartilhados com a empresa
- Coach de carreira com IA para desenvolvimento de soft skills
- Análise emocional contínua e personalizada

**Para Empresas (Dashboard "The Corporate Map")**
- Sismógrafo de Stress: gráficos de linha mostrando níveis de estresse organizacional ao longo do tempo
- Mapa de Calor por Setor: visualização comparativa do bem-estar entre departamentos
- Predição de Turnover: alertas sobre risco de perda de talentos baseados em análise de sentimento
- Análise de Impacto de Decisões: feedback em tempo real sobre como mudanças organizacionais afetam a moral dos colaboradores

## Arquitetura

O TideFlow é construído seguindo uma arquitetura de microserviços, onde cada serviço possui responsabilidades bem definidas e pode ser desenvolvido, testado e implantado independentemente.

### Microserviços

**User Service** (Porta 8080)
- Gerenciamento de usuários, empresas e autenticação
- Controle de acesso e autorização baseado em JWT
- Integração com serviços de pagamento (Stripe)
- Multi-tenancy para suporte a múltiplas empresas

**AI Service** (Porta 8082)
- Processamento de conversas com modelos de linguagem (LLM)
- Análise de sentimento e detecção de riscos emocionais
- Geração de relatórios corporativos agregados
- Integração com múltiplos provedores de IA (Ollama, Gemini, OpenRouter)
- Processamento de áudio com Whisper para transcrição

**Notification Service** (Porta 8083)
- Envio de notificações por email
- Gerenciamento de filas de mensagens via RabbitMQ
- Histórico de notificações enviadas

### Frontend

**UI Web** (Next.js)
- Dashboard corporativo para gestores e RH
- Visualizações de dados e relatórios analíticos
- Interface administrativa para configuração de empresas

**UI Mobile** (React Native / Expo)
- Aplicativo móvel para colaboradores
- Interface de chat com IA
- Autenticação e gerenciamento de perfil

### Infraestrutura e Serviços de Apoio

- **PostgreSQL**: Três instâncias separadas, uma para cada microserviço (portas 5432, 5433, 5434)
- **Redis**: Cache distribuído e armazenamento de sessões (porta 6379)
- **RabbitMQ**: Message broker para comunicação assíncrona entre serviços (portas 5672, 15672)
- **Ollama**: Servidor local de modelos de linguagem (porta 11434)
- **Faster Whisper**: Serviço de transcrição de áudio (porta 8001)
- **PgAdmin**: Interface web para gerenciamento de bancos de dados (porta 5050)

## Tecnologias

### Backend

- **Java 17**: Linguagem principal para os microserviços
- **Spring Boot 3.5.7**: Framework para desenvolvimento de aplicações Java
- **Spring Data JPA**: Abstração para acesso a dados
- **Spring Security**: Autenticação e autorização
- **Spring Cloud OpenFeign**: Cliente HTTP para comunicação entre serviços
- **JWT (Auth0)**: Tokens para autenticação stateless
- **RabbitMQ**: Mensageria assíncrona
- **Redis**: Cache e armazenamento de sessões
- **PostgreSQL 16**: Banco de dados relacional

### Frontend

- **Next.js 16**: Framework React para aplicações web
- **React 19**: Biblioteca para construção de interfaces
- **TypeScript 5**: Tipagem estática para JavaScript
- **Tailwind CSS 4**: Framework CSS utilitário
- **Radix UI**: Componentes acessíveis e não estilizados
- **Framer Motion**: Biblioteca de animações
- **Recharts**: Biblioteca de gráficos
- **Zod**: Validação de esquemas TypeScript

### Mobile

- **React Native 0.81**: Framework para desenvolvimento mobile
- **Expo 54**: Plataforma e ferramentas para React Native
- **TypeScript 5**: Tipagem estática

### IA e Processamento

- **Ollama**: Execução local de modelos de linguagem
- **Faster Whisper**: Transcrição de áudio otimizada
- **OpenRouter**: Gateway para múltiplos modelos de IA
- **Google Gemini**: Modelo de linguagem da Google

### DevOps e Infraestrutura

- **Docker**: Containerização de serviços
- **Docker Compose**: Orquestração de containers locais
- **Maven**: Gerenciamento de dependências Java
- **GitHub Actions**: CI/CD automatizado

## Pré-requisitos

Antes de executar o projeto, certifique-se de ter instalado:

- Docker (versão 20.10 ou superior)
- Docker Compose (versão 2.0 ou superior)
- Java 17 (para desenvolvimento local dos microserviços)
- Node.js 20 ou superior (para desenvolvimento do frontend)
- Maven 3.8 ou superior (para build dos serviços Java)

## Como Executar o Projeto

### Executando a Infraestrutura com Docker Compose

O projeto utiliza Docker Compose para orquestrar todos os serviços de infraestrutura necessários. Para iniciar os serviços de apoio (bancos de dados, cache, message broker, etc.), execute:

```bash
docker-compose up -d
```

Este comando irá iniciar os seguintes serviços:
- Três instâncias do PostgreSQL (portas 5432, 5433, 5434)
- Redis (porta 6379)
- RabbitMQ com interface de gerenciamento (portas 5672, 15672)
- Ollama (porta 11434)
- Faster Whisper (porta 8001)
- PgAdmin (porta 5050)

Para verificar o status dos containers:

```bash
docker-compose ps
```

Para visualizar os logs de todos os serviços:

```bash
docker-compose logs -f
```

Para parar todos os serviços:

```bash
docker-compose down
```

Para parar e remover volumes (apaga dados persistentes):

```bash
docker-compose down -v
```

### Configuração de Variáveis de Ambiente

Alguns serviços requerem variáveis de ambiente para funcionamento completo. Crie um arquivo `.env` na raiz do projeto (veja `.env.example` para referência):

```bash
cp .env.example .env
```

Edite o arquivo `.env` e configure as variáveis necessárias:
- `JWT_SECRET`: Chave secreta para JWT (deve ser a mesma em todos os serviços)
- `OPENROUTER_API_KEY`: Chave da API OpenRouter (opcional)
- `GEMINI_API_KEY`: Chave da API Gemini (opcional)
- `RESEND_API_KEY`: Chave da API Resend para envio de emails (opcional)
- `STRIPE_SECRET_KEY`: Chave secreta do Stripe (opcional)
- Outras variáveis conforme necessário

### Populando o Banco de Dados com Dados de Exemplo

Para popular o banco de dados com dados de exemplo (usuário root, empresa moredevs, funcionários, conversas e relatórios), execute:

**Opção 1: Script Automatizado (Recomendado)**
```bash
./scripts/seed-database.sh
```

**Opção 2: Manualmente**

1. **User Service** (cria usuário root, empresa, departamentos e funcionários):
```bash
cd user-service
./mvnw spring-boot:run -Dspring-boot.run.arguments=seed
```

2. **AI Service** (cria conversas e relatórios):
```bash
cd ai-service
./mvnw spring-boot:run -Dspring-boot.run.arguments=seed
```

**O que é criado:**
- ✅ Usuário root: `root@tideflow.com` / `root123`
- ✅ Empresa: `moredevs` com 6 departamentos
- ✅ 15 funcionários (email: `[nome]@moredevs.com` / senha: `senha123`)
- ✅ ~45-60 conversas simuladas
- ✅ 5 relatórios corporativos

**Nota:** O script verifica se os dados já existem antes de criar, evitando duplicações.

Para mais detalhes, consulte [README-SEED.md](./README-SEED.md).

### Executando os Microserviços

#### User Service

```bash
cd user-service
./mvnw spring-boot:run
```

O serviço estará disponível em `http://localhost:8080`

#### AI Service

```bash
cd ai-service
./mvnw spring-boot:run
```

O serviço estará disponível em `http://localhost:8082`

#### Notification Service

```bash
cd notification-service
./mvnw spring-boot:run
```

O serviço estará disponível em `http://localhost:8081`

### Executando o Frontend Web

```bash
cd ui-web
npm install
npm run dev
```

A aplicação web estará disponível em `http://localhost:3000`

### Executando o Aplicativo Mobile

```bash
cd ui-mobile
npm install
npm start
```

Siga as instruções do Expo para abrir o aplicativo no simulador iOS, emulador Android ou dispositivo físico.

## Estrutura do Projeto

```
tide-flow/
├── ai-service/              # Microserviço de IA e análise
│   ├── src/main/java/      # Código fonte Java
│   └── pom.xml             # Configuração Maven
├── user-service/           # Microserviço de usuários e autenticação
│   ├── src/main/java/      # Código fonte Java
│   └── pom.xml             # Configuração Maven
├── notification-service/   # Microserviço de notificações
│   ├── src/main/java/      # Código fonte Java
│   └── pom.xml             # Configuração Maven
├── ui-web/                 # Aplicação web Next.js
│   ├── app/                # Páginas e componentes
│   ├── components/         # Componentes React reutilizáveis
│   └── package.json        # Dependências Node.js
├── ui-mobile/              # Aplicativo mobile React Native
│   ├── screens/            # Telas do aplicativo
│   ├── components/         # Componentes React Native
│   └── package.json        # Dependências Node.js
├── whisper-service/        # Serviço de transcrição de áudio
│   └── Dockerfile          # Configuração Docker
├── ollama/                 # Configuração do Ollama
│   └── Dockerfile          # Configuração Docker
├── docker-compose.yml      # Orquestração de containers
└── README.md               # Este arquivo
```

## Acessos e Portas

| Serviço | URL | Porta |
|---------|-----|-------|
| User Service | http://localhost:8080 | 8080 |
| AI Service | http://localhost:8082 | 8082 |
| Notification Service | http://localhost:8081 | 8081 |
| UI Web | http://localhost:3000 | 3000 |
| PostgreSQL (User) | localhost | 5432 |
| PostgreSQL (Notification) | localhost | 5433 |
| PostgreSQL (AI) | localhost | 5434 |
| Redis | localhost | 6379 |
| RabbitMQ | localhost | 5672 |
| RabbitMQ Management | http://localhost:15672 | 15672 |
| PgAdmin | http://localhost:5050 | 5050 |
| Ollama | http://localhost:11434 | 11434 |
| Faster Whisper | http://localhost:8001 | 8001 |

**Credenciais padrão:**
- PostgreSQL: usuário `postgres`, senha `postgres`
- RabbitMQ: usuário `guest`, senha `guest`
- PgAdmin: email `admin@tideflow.com`, senha `admin`

## Desenvolvimento

### Build dos Microserviços

Para compilar os microserviços Java:

```bash
# User Service
cd user-service
./mvnw clean package

# AI Service
cd ai-service
./mvnw clean package

# Notification Service
cd notification-service
./mvnw clean package
```

### Executando Testes

```bash
# User Service
cd user-service
./mvnw test

# AI Service
cd ai-service
./mvnw test

# Notification Service
cd notification-service
./mvnw test
```

### Build do Frontend

```bash
cd ui-web
npm run build
```

## Modelo de Negócio

O TideFlow opera como uma plataforma SaaS B2B com modelo de cobrança por assinatura:

- **Pricing**: Assinatura mensal por empresa
- **Preço**: R$ 199,90 por mês
- **Freemium**: Até 20 funcionários gratuito para empresas testarem o dashboard
- **Plano Enterprise**: Para empresas acima de 20 funcionários, com integrações adicionais (Slack, Teams, etc.)

## Privacidade e Compliance

O sistema foi projetado com foco em privacidade e conformidade com regulamentações:

- **LGPD/GDPR**: Conformidade com leis de proteção de dados
- **Anonimização**: Dados individuais nunca são compartilhados com empresas
- **Agregação**: Apenas dados agregados e anonimizados são disponibilizados no dashboard corporativo
- **Transparência**: Usuários são informados claramente sobre como seus dados são utilizados
