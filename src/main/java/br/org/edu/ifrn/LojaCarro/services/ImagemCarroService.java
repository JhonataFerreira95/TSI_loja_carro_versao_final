package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.CarroException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class ImagemCarroService {

    private static final int TAMANHO_MAXIMO = 5 * 1024 * 1024;
    private static final String PREFIXO_CAMINHO = "/uploads/carros/";
    private final Path diretorioImagens;

    public ImagemCarroService(@Value("${app.uploads.directory:uploads}") String diretorioUploads) {
        this.diretorioImagens = Path.of(diretorioUploads).toAbsolutePath().normalize().resolve("carros");
    }

    public String salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            return null;
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new CarroException("A imagem deve ter no máximo 5 MB.");
        }

        try {
            byte[] conteudo;
            try (InputStream entrada = arquivo.getInputStream()) {
                conteudo = entrada.readNBytes(TAMANHO_MAXIMO + 1);
            }
            if (conteudo.length > TAMANHO_MAXIMO) {
                throw new CarroException("A imagem deve ter no máximo 5 MB.");
            }

            String extensao = identificarExtensao(conteudo);
            String nomeArquivo = UUID.randomUUID() + extensao;
            Files.createDirectories(diretorioImagens);
            Files.write(diretorioImagens.resolve(nomeArquivo), conteudo);
            return "/uploads/carros/" + nomeArquivo;
        } catch (IOException ex) {
            throw new CarroException("Não foi possível salvar a imagem enviada.");
        }
    }

    public void remover(String caminhoImagem) {
        if (caminhoImagem == null) {
            return;
        }
        if (!caminhoImagem.startsWith(PREFIXO_CAMINHO)) {
            throw new CarroException("A imagem não aponta para um arquivo local gerenciado pela aplicação.");
        }

        String nomeArquivo = caminhoImagem.substring(PREFIXO_CAMINHO.length());
        if (nomeArquivo.isBlank() || nomeArquivo.contains("/") || nomeArquivo.contains("\\")) {
            throw new CarroException("O caminho da imagem armazenado é inválido.");
        }

        Path arquivo = diretorioImagens.resolve(nomeArquivo).normalize();
        if (!arquivo.startsWith(diretorioImagens)) {
            throw new CarroException("O caminho da imagem armazenado é inválido.");
        }

        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException ex) {
            throw new CarroException("Não foi possível remover a imagem do armazenamento local.");
        }
    }

    private String identificarExtensao(byte[] conteudo) {
        if (conteudo.length >= 3 && (conteudo[0] & 0xff) == 0xff
                && (conteudo[1] & 0xff) == 0xd8 && (conteudo[2] & 0xff) == 0xff) {
            return ".jpg";
        }
        if (conteudo.length >= 8 && (conteudo[0] & 0xff) == 0x89
                && conteudo[1] == 'P' && conteudo[2] == 'N' && conteudo[3] == 'G'
                && (conteudo[4] & 0xff) == 0x0d && (conteudo[5] & 0xff) == 0x0a
                && (conteudo[6] & 0xff) == 0x1a && (conteudo[7] & 0xff) == 0x0a) {
            return ".png";
        }
        if (conteudo.length >= 6) {
            String assinaturaGif = new String(conteudo, 0, 6, StandardCharsets.US_ASCII);
            if (assinaturaGif.equals("GIF87a") || assinaturaGif.equals("GIF89a")) {
                return ".gif";
            }
        }
        if (conteudo.length >= 12
                && new String(conteudo, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(conteudo, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return ".webp";
        }
        throw new CarroException("Envie uma imagem JPEG, PNG, GIF ou WebP válida.");
    }
}
