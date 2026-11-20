package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.CarroException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Service
public class ImagemCarroService {

    private static final int TAMANHO_MAXIMO = 5 * 1024 * 1024;
    private static final int TIMEOUT_MS = (int) Duration.ofSeconds(10).toMillis();

    private final Path diretorioImagens;

    public ImagemCarroService(@Value("${app.uploads.directory:uploads}") String diretorioUploads) {
        this.diretorioImagens = Path.of(diretorioUploads).toAbsolutePath().normalize().resolve("carros");
    }

    public String salvarDaUrl(String urlImagem) {
        URI uri = validarUrl(urlImagem);
        HttpURLConnection conexao = null;
        try {
            URL url = uri.toURL();
            conexao = (HttpURLConnection) url.openConnection();
            conexao.setConnectTimeout(TIMEOUT_MS);
            conexao.setReadTimeout(TIMEOUT_MS);
            conexao.setInstanceFollowRedirects(false);
            conexao.setRequestProperty("Accept", "image/jpeg,image/png,image/gif,image/webp");

            int status = conexao.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new CarroException("Não foi possível baixar a imagem. Verifique a URL informada.");
            }

            String tipoConteudo = conexao.getContentType();
            byte[] conteudo;
            try (InputStream entrada = conexao.getInputStream()) {
                conteudo = entrada.readNBytes(TAMANHO_MAXIMO + 1);
            }
            if (conteudo.length > TAMANHO_MAXIMO) {
                throw new CarroException("A imagem deve ter no máximo 5 MB.");
            }

            String extensao = identificarExtensao(conteudo, tipoConteudo);
            String nomeArquivo = UUID.randomUUID() + extensao;
            Files.createDirectories(diretorioImagens);
            Files.write(diretorioImagens.resolve(nomeArquivo), conteudo);
            return "/uploads/carros/" + nomeArquivo;
        } catch (IOException ex) {
            throw new CarroException("Não foi possível baixar ou salvar a imagem informada.");
        } finally {
            if (conexao != null) {
                conexao.disconnect();
            }
        }
    }

    private URI validarUrl(String valor) {
        final URI uri;
        try {
            uri = URI.create(valor.trim());
        } catch (IllegalArgumentException ex) {
            throw new CarroException("URL da imagem inválida.");
        }

        String esquema = uri.getScheme();
        String host = uri.getHost();
        if (esquema == null || (!esquema.equalsIgnoreCase("http") && !esquema.equalsIgnoreCase("https"))
                || host == null || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443)) {
            throw new CarroException("Informe uma URL HTTP ou HTTPS pública e válida para a imagem.");
        }

        try {
            for (InetAddress endereco : InetAddress.getAllByName(host)) {
                if (!enderecoPublico(endereco)) {
                    throw new CarroException("A URL da imagem não pode apontar para um endereço local ou privado.");
                }
            }
        } catch (IOException ex) {
            throw new CarroException("Não foi possível localizar o servidor da imagem.");
        }
        return uri;
    }

    private boolean enderecoPublico(InetAddress endereco) {
        if (endereco.isAnyLocalAddress() || endereco.isLoopbackAddress() || endereco.isLinkLocalAddress()
                || endereco.isSiteLocalAddress() || endereco.isMulticastAddress()) {
            return false;
        }

        byte[] bytes = endereco.getAddress();
        if (endereco instanceof Inet4Address) {
            int primeiro = Byte.toUnsignedInt(bytes[0]);
            int segundo = Byte.toUnsignedInt(bytes[1]);
            int terceiro = Byte.toUnsignedInt(bytes[2]);
            return primeiro != 0 && primeiro < 224
                    && !(primeiro == 100 && segundo >= 64 && segundo <= 127)
                    && !(primeiro == 192 && (segundo == 0 || (segundo == 88 && terceiro == 99)))
                    && !(primeiro == 198 && (segundo == 18 || segundo == 19 || (segundo == 51 && terceiro == 100)))
                    && !(primeiro == 203 && segundo == 0 && terceiro == 113);
        }

        if (endereco instanceof Inet6Address) {
            int primeiro = Byte.toUnsignedInt(bytes[0]);
            int segundo = Byte.toUnsignedInt(bytes[1]);
            return (primeiro & 0xe0) == 0x20
                    && !(primeiro == 0x20 && segundo == 0x01 && (Byte.toUnsignedInt(bytes[2]) & 0xfe) == 0)
                    && !(primeiro == 0x20 && segundo == 0x02);
        }
        return false;
    }

    private String identificarExtensao(byte[] conteudo, String tipoConteudo) {
        String tipo = tipoConteudo == null ? "" : tipoConteudo.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (tipo.equals("image/jpeg") && conteudo.length >= 3
                && (conteudo[0] & 0xff) == 0xff && (conteudo[1] & 0xff) == 0xd8 && (conteudo[2] & 0xff) == 0xff) {
            return ".jpg";
        }
        if (tipo.equals("image/png") && conteudo.length >= 8
                && (conteudo[0] & 0xff) == 0x89 && conteudo[1] == 'P' && conteudo[2] == 'N' && conteudo[3] == 'G'
                && (conteudo[4] & 0xff) == 0x0d && (conteudo[5] & 0xff) == 0x0a
                && (conteudo[6] & 0xff) == 0x1a && (conteudo[7] & 0xff) == 0x0a) {
            return ".png";
        }
        if (tipo.equals("image/gif") && conteudo.length >= 6
                && (new String(conteudo, 0, 6, StandardCharsets.US_ASCII).equals("GIF87a")
                || new String(conteudo, 0, 6, StandardCharsets.US_ASCII).equals("GIF89a"))) {
            return ".gif";
        }
        if (tipo.equals("image/webp") && conteudo.length >= 12
                && new String(conteudo, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(conteudo, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return ".webp";
        }
        throw new CarroException("O endereço informado não retornou uma imagem JPEG, PNG, GIF ou WebP válida.");
    }
}
