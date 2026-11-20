# TSI_loja_carro_versao_final

## Executar localmente

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

## Imagens dos carros

No cadastro, informe uma URL HTTP ou HTTPS pública para a imagem. O servidor baixa
JPEG, PNG, GIF ou WebP de até 5 MB e salva o arquivo em `uploads/carros/`; o banco
guarda apenas o caminho da imagem. A pasta pode ser alterada pela variável
`UPLOADS_DIR`. Em produção ou com Docker, mantenha essa pasta em um volume persistente.