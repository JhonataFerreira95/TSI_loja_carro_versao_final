# TSI Loja de Carro

Projeto de loja de carros desenvolvido com Spring Boot, MySQL e Angular.

## Requisitos

- Windows
- Java 25
- Maven 3.9+ ou o Maven Wrapper incluido no projeto
- Node.js 20+ e npm
- MySQL 8 executando localmente

Este projeto pode ser executado sem Docker.

## Banco de dados MySQL local

Crie o banco utilizado pelo perfil de desenvolvimento:

```sql
CREATE DATABASE carros CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

O projeto espera estas configuracoes no perfil `dev`:

- Host: `localhost`
- Porta: `3306`
- Banco: `carros`
- Usuario: `root`
- Senha: `root`

As configuracoes podem ser alteradas em:

[application-dev.properties](src/main/resources/application-dev.properties)

As tabelas sao criadas ou atualizadas automaticamente pelo Hibernate.

## Executar o backend

Na raiz do projeto, execute no PowerShell:

```powershell
$env:SERVER = "dev"
.\mvnw.cmd spring-boot:run
```

O backend sera iniciado em:

```text
http://localhost:8080
```

Para encerrar o backend, use `Ctrl+C` no terminal.

## Executar o front-end Angular integrado

A maneira mais simples e executar o backend e usar o build Angular integrado ao Spring:

```powershell
cd frontend
npm install
npm run build
cd ..

$env:SERVER = "dev"
.\mvnw.cmd spring-boot:run
```

Depois, acesse:

```text
http://localhost:8080
```

## Executar Angular e Spring separadamente

Use esta opcao para editar o front-end com recarregamento automatico.

### Terminal 1 - backend

Na raiz do projeto:

```powershell
$env:SERVER = "dev"
.\mvnw.cmd spring-boot:run
```

### Terminal 2 - front-end Angular

```powershell
cd frontend
npm install
npm start
```

O Angular informara o endereco local, normalmente:

```text
http://localhost:4200
```

As chamadas `/usuario`, `/carro`, `/log` e `/uploads` serao encaminhadas automaticamente para o Spring em `http://localhost:8080`.

## Gerar somente o front-end de producao

```powershell
cd frontend
npm run build
```

Os arquivos serao gerados em:

[src/main/resources/static](src/main/resources/static)

## Papéis e permissões

Cada usuário tem um papel persistido na coluna `papel`: `USER` ou `ADMIN`.
Novas contas públicas sempre começam como `USER`; somente um administrador pode
escolher o papel ao criar ou editar contas. Usuários comuns podem consultar e
atualizar a própria conta, cadastrar carros associados à própria conta e enviar
ou remover fotos desses carros. Administradores podem gerenciar todos os carros,
contas e logs e associar carros a qualquer conta.

Para conceder o primeiro papel de administrador, crie a conta pela tela de
cadastro e atualize o papel diretamente no MySQL:

```sql
UPDATE usuario SET papel = 'ADMIN' WHERE email = 'admin@exemplo.com';
```

Depois de promover a conta, faça login como esse usuário e recarregue a página.

## Imagens dos carros

No cadastro, selecione uma imagem JPEG, PNG, GIF ou WebP de até 5 MB do computador.
O arquivo é salvo localmente em `uploads/carros/`; o banco guarda apenas o caminho
da imagem. A pasta pode ser alterada pela variável `UPLOADS_DIR`. Em produção ou
ao executar em outro ambiente, mantenha essa pasta em armazenamento persistente.