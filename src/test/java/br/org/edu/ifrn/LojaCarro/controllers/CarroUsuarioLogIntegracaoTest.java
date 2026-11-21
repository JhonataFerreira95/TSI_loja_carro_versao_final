package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.model.PapelUsuario;
import br.org.edu.ifrn.LojaCarro.repository.CarroRepository;
import br.org.edu.ifrn.LojaCarro.repository.LogRepository;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Verifica a integração entre Carro, Usuário (login) e Logs
@SpringBootTest
@AutoConfigureMockMvc
class CarroUsuarioLogIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarroRepository carroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.uploads.directory}")
    private String diretorioUploads;

    private MockHttpSession sessao;
    private long idUsuario;

    @BeforeEach
    void prepararUsuarioLogado() throws Exception {
        carroRepository.deleteAll();
        usuarioRepository.deleteAll();
        logRepository.deleteAll();

        String resposta = mockMvc.perform(post("/usuario/salvar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"email\":\"maria@email.com\",\"senha\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString();
        idUsuario = objectMapper.readTree(resposta).get("id").asLong();
        var maria = usuarioRepository.findById(idUsuario).orElseThrow();
        maria.setPapel(PapelUsuario.ADMIN);
        usuarioRepository.save(maria);

        sessao = new MockHttpSession();
        mockMvc.perform(post("/usuario/login").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@email.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    private long salvarCarro(String modelo) throws Exception {
        String resposta = mockMvc.perform(multipart("/carro/salvar").session(sessao)
                        .param("modelo", modelo).param("ano", "2020").param("preco", "50000"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).get("id").asLong();
    }

    @Test
    void carroGuardaOUsuarioQueCadastrou() throws Exception {
        long idCarro = salvarCarro("Gol");

        mockMvc.perform(get("/carro/" + idCarro).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastradoPor.email").value("maria@email.com"))
                .andExpect(jsonPath("$.cadastradoPor.senha").doesNotExist());

        // Atualizar o carro não perde quem o cadastrou
        mockMvc.perform(multipart("/carro/" + idCarro).session(sessao)
                        .param("modelo", "Uno").param("ano", "2021").param("preco", "40000")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastradoPor.email").value("maria@email.com"));
    }

    @Test
    void aceitaNomeDeModeloComMaisDeCincoCaracteres() throws Exception {
        mockMvc.perform(multipart("/carro/salvar").session(sessao)
                        .param("modelo", "Corolla Cross").param("ano", "2024").param("preco", "150000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value("Corolla Cross"));
    }

    @Test
    void uploadSalvaArquivoLocalEArmazenaApenasOCaminhoNoCarro() throws Exception {
        byte[] conteudo = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII=");
        MockMultipartFile imagem = new MockMultipartFile("imagem", "carro.png", "image/png", conteudo);

        String corpo = mockMvc.perform(multipart("/carro/salvar").file(imagem).session(sessao)
                        .param("modelo", "Gol").param("ano", "2020").param("preco", "50000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagem", startsWith("/uploads/carros/")))
                .andReturn().getResponse().getContentAsString();

        String caminhoImagem = objectMapper.readTree(corpo).get("imagem").asText();
        Path arquivoSalvo = Path.of(diretorioUploads, "carros",
                caminhoImagem.substring(caminhoImagem.lastIndexOf('/') + 1));
        try {
            assertThat(Files.readAllBytes(arquivoSalvo)).containsExactly(conteudo);
            assertThat(carroRepository.findAll()).singleElement()
                    .extracting(carro -> carro.getImagem())
                    .isEqualTo(caminhoImagem);
            mockMvc.perform(multipart("/carro/" + objectMapper.readTree(corpo).get("id").asLong())
                            .session(sessao).param("modelo", "Gol").param("ano", "2021")
                            .param("preco", "51000")
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.imagem").value(caminhoImagem));
        } finally {
            Files.deleteIfExists(arquivoSalvo);
        }
    }

    @Test
    void rejeitaArquivoQueNaoEImagem() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("imagem", "arquivo.txt",
                "text/plain", "nao e uma imagem".getBytes());

        mockMvc.perform(multipart("/carro/salvar").file(arquivo).session(sessao)
                        .param("modelo", "Gol").param("ano", "2020").param("preco", "50000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("imagem JPEG, PNG, GIF ou WebP")));

        assertThat(carroRepository.findAll()).isEmpty();
    }

    @Test
    void usuarioSoPodeEnviarELimparFotosDosPropriosCarrosEAdminPodeDeTodos() throws Exception {
        String respostaUsuario = mockMvc.perform(post("/usuario/salvar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Joao\",\"email\":\"joao@email.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var usuario = usuarioRepository.findById(objectMapper.readTree(respostaUsuario).get("id").asLong())
                .orElseThrow();

        String respostaCarroUsuario = mockMvc.perform(multipart("/carro/salvar").session(sessao)
                        .param("modelo", "Uno").param("ano", "2021").param("preco", "20000")
                        .param("usuarioId", usuario.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastradoPor.email").value("joao@email.com"))
                .andReturn().getResponse().getContentAsString();
        long idCarroDoUsuario = objectMapper.readTree(respostaCarroUsuario).get("id").asLong();
        long idCarroDoAdmin = salvarCarro("Gol");
        String respostaImagemAdmin = mockMvc.perform(multipart("/carro/" + idCarroDoAdmin + "/imagem")
                        .file(imagemValida()).session(sessao)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String caminhoImagemAdmin = objectMapper.readTree(respostaImagemAdmin).get("imagem").asText();

        MockHttpSession sessaoUsuario = new MockHttpSession();
        mockMvc.perform(post("/usuario/login").session(sessaoUsuario).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"joao@email.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/carro/meusCarros").session(sessaoUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(idCarroDoUsuario));

        MockMultipartFile imagemUsuario = imagemValida();
        String respostaImagem = mockMvc.perform(multipart("/carro/" + idCarroDoUsuario + "/imagem")
                        .file(imagemUsuario).session(sessaoUsuario)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagem", startsWith("/uploads/carros/")))
                .andReturn().getResponse().getContentAsString();
        String caminhoImagemUsuario = objectMapper.readTree(respostaImagem).get("imagem").asText();

        mockMvc.perform(get(caminhoImagemUsuario).session(sessaoUsuario))
                .andExpect(status().isOk());
        mockMvc.perform(get(caminhoImagemAdmin).session(sessaoUsuario))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/carro/" + idCarroDoAdmin + "/imagem").file(imagemValida())
                        .session(sessaoUsuario)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/carro/" + idCarroDoAdmin + "/imagem").session(sessaoUsuario))
                        .andExpect(status().isForbidden());
        mockMvc.perform(delete("/carro/" + idCarroDoUsuario).session(sessaoUsuario))
                        .andExpect(status().isForbidden());
        mockMvc.perform(get("/carro/listarCarros").session(sessaoUsuario))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/carro/" + idCarroDoUsuario + "/imagem").session(sessaoUsuario))
                .andExpect(status().isNoContent());
        assertThat(carroRepository.findById(idCarroDoUsuario).orElseThrow().getImagem()).isNull();
        assertThat(Files.exists(Path.of(diretorioUploads, "carros",
                caminhoImagemUsuario.substring(caminhoImagemUsuario.lastIndexOf('/') + 1)))).isFalse();

        mockMvc.perform(delete("/carro/" + idCarroDoAdmin + "/imagem").session(sessao))
                .andExpect(status().isNoContent());
        assertThat(carroRepository.findById(idCarroDoAdmin).orElseThrow().getImagem()).isNull();
        assertThat(Files.exists(Path.of(diretorioUploads, "carros",
                caminhoImagemAdmin.substring(caminhoImagemAdmin.lastIndexOf('/') + 1)))).isFalse();
    }

    @Test
    void usuarioPodeCadastrarCarroComFotoNaPropriaConta() throws Exception {
        String respostaUsuario = mockMvc.perform(post("/usuario/salvar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Joao\",\"email\":\"joao@email.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long idJoao = objectMapper.readTree(respostaUsuario).get("id").asLong();
        MockHttpSession sessaoJoao = new MockHttpSession();
        mockMvc.perform(post("/usuario/login").session(sessaoJoao).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"joao@email.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/carro/salvar").session(sessaoJoao)
                        .param("modelo", "Gol").param("ano", "2022").param("preco", "25000")
                        .param("usuarioId", String.valueOf(idUsuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("própria conta")));

        String respostaCarro = mockMvc.perform(multipart("/carro/salvar").file(imagemValida()).session(sessaoJoao)
                        .param("modelo", "Uno").param("ano", "2022").param("preco", "25000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastradoPor.email").value("joao@email.com"))
                .andExpect(jsonPath("$.imagem", startsWith("/uploads/carros/")))
                .andReturn().getResponse().getContentAsString();
        var carro = objectMapper.readTree(respostaCarro);
        long idCarro = carro.get("id").asLong();
        String caminhoImagem = carro.get("imagem").asText();
        Path arquivoImagem = Path.of(diretorioUploads, "carros",
                caminhoImagem.substring(caminhoImagem.lastIndexOf('/') + 1));

        try {
            assertThat(Files.exists(arquivoImagem)).isTrue();
            mockMvc.perform(get("/carro/meusCarros").session(sessaoJoao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(idCarro))
                    .andExpect(jsonPath("$[0].imagem").value(caminhoImagem));
            mockMvc.perform(get("/carro/listarCarros").session(sessaoJoao))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/carro/" + idCarro).session(sessaoJoao))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/carro/" + idCarro + "/imagem").session(sessaoJoao))
                    .andExpect(status().isNoContent());
            assertThat(Files.exists(arquivoImagem)).isFalse();
        } finally {
            Files.deleteIfExists(arquivoImagem);
        }
    }

    @Test
    void naoExcluiUsuarioComCarrosCadastrados() throws Exception {
        salvarCarro("Gol");

        // Outro usuário tenta excluir a Maria, que tem um carro
        String respostaJoao = mockMvc.perform(post("/usuario/salvar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Joao\",\"email\":\"joao@email.com\",\"senha\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString();
        long idJoao = objectMapper.readTree(respostaJoao).get("id").asLong();
        var joao = usuarioRepository.findById(idJoao).orElseThrow();
        joao.setPapel(PapelUsuario.ADMIN);
        usuarioRepository.save(joao);
        MockHttpSession sessaoJoao = new MockHttpSession();
        mockMvc.perform(post("/usuario/login").session(sessaoJoao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"joao@email.com\",\"senha\":\"123456\"}"));

        mockMvc.perform(delete("/usuario/" + idUsuario).session(sessaoJoao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("possui carros cadastrados")));
    }

    @Test
    void logsRegistramQuemFezCadaAcao() throws Exception {
        salvarCarro("Gol");
        mockMvc.perform(multipart("/carro/salvar").session(sessao)
                        .param("modelo", "Modelo Grande").param("ano", "2020").param("preco", "-1"))
                .andExpect(status().isBadRequest());

        // Logs vêm do mais recente para o mais antigo
        mockMvc.perform(get("/log").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].acao").value("ERRO"))
                .andExpect(jsonPath("$[0].entidade").value("Carro"))
                .andExpect(jsonPath("$[0].usuario").value("Maria (maria@email.com)"))
                .andExpect(jsonPath("$[1].acao").value("CRIAR"))
                .andExpect(jsonPath("$[1].entidade").value("Carro"))
                .andExpect(jsonPath("$[1].usuario").value("Maria (maria@email.com)"))
                .andExpect(jsonPath("$[2].acao").value("LOGIN"))
                .andExpect(jsonPath("$[3].acao").value("CRIAR"))
                .andExpect(jsonPath("$[3].usuario").value("anônimo"));
    }

    @Test
    void acessoSemLoginFicaRegistradoNoLog() throws Exception {
        mockMvc.perform(get("/carro/listarCarros")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/log").session(sessao))
                .andExpect(jsonPath("$[0].acao").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$[0].descricao").value(containsString("/carro/listarCarros")));
    }

    @Test
    void usuarioComumNaoAcessaCarrosNemLogs() throws Exception {
        mockMvc.perform(post("/usuario/salvar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Joao\",\"email\":\"joao@email.com\",\"senha\":\"123456\"}"));
        MockHttpSession sessaoJoao = new MockHttpSession();
        mockMvc.perform(post("/usuario/login").session(sessaoJoao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"joao@email.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/carro/listarCarros").session(sessaoJoao))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/log").session(sessaoJoao))
                .andExpect(status().isForbidden());
    }

    private MockMultipartFile imagemValida() {
        byte[] conteudo = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII=");
        return new MockMultipartFile("imagem", "carro.png", "image/png", conteudo);
    }
}
