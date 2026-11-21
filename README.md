# TSI_loja_carro_versao_final

## Executar localmente

Requisitos: Java 25 LTS e Maven 3.9.15+ (ou use `mvnw`/`mvnw.cmd`).

Inicie o MySQL de desenvolvimento (Docker Compose cria o banco `carros`):

```bash
docker compose up -d --wait mysql
```

Inicie o backend usando o perfil `dev`:

```bash
SERVER=dev mvn spring-boot:run
```

O perfil local usa `root/root`; essas credenciais são somente para desenvolvimento.
Para parar o MySQL sem apagar os dados:

```bash
docker compose down
```

## Front-end Vue

O front-end está em [frontend/](frontend/) e usa Vue 3 com Vue Router.

Para alterar e executar a interface em modo de desenvolvimento:

```bash
cd frontend
npm install
npm run dev
```

Para gerar a versão de produção dentro de `src/main/resources/static`:

```bash
cd frontend
npm run build
```

O build gera uma SPA única consumida pelo Spring Boot, mantendo os serviços REST
sob os mesmos caminhos já utilizados pelo projeto.

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
com Docker, mantenha essa pasta em um volume persistente.