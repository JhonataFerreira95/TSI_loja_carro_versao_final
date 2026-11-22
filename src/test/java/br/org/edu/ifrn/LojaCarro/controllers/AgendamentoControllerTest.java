package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.model.Agendamento;
import br.org.edu.ifrn.LojaCarro.model.Carro;
import br.org.edu.ifrn.LojaCarro.model.PapelUsuario;
import br.org.edu.ifrn.LojaCarro.model.StatusAgendamento;
import br.org.edu.ifrn.LojaCarro.model.StatusCarro;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.AgendamentoRepository;
import br.org.edu.ifrn.LojaCarro.repository.CarroRepository;
import br.org.edu.ifrn.LojaCarro.repository.LogRepository;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgendamentoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private CarroRepository carroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Usuario cliente;
    private Carro corolla;
    private MockHttpSession sessaoCliente;

    @BeforeEach
    void limparBanco() {
        agendamentoRepository.deleteAll();
        carroRepository.deleteAll();
        usuarioRepository.deleteAll();
        logRepository.deleteAll();

        cliente = usuarioRepository.save(new Usuario("Ana", "ana@test.com", "senha"));
        corolla = carroRepository.save(new Carro("Toyota Corolla", 2025));
        sessaoCliente = sessao(cliente);
    }

    @Test
    void reservaComDuracaoFixaListaHorariosECancelamentoLiberaSlot() throws Exception {
        LocalDate data = LocalDate.now().plusDays(2);
        LocalDateTime inicio = data.atTime(14, 0);

        MvcResult resultado = reservar(corolla.getId(), inicio, sessaoCliente)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.dataCriacao").isNotEmpty())
                .andReturn();
        long agendamentoId = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/agendamento/disponibilidade")
                        .param("carroId", corolla.getId().toString())
                        .param("data", data.toString()).session(sessaoCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("14:00:00"))))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem("14:30:00")));

        mockMvc.perform(put("/agendamento/" + agendamentoId + "/cancelar").session(sessaoCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        mockMvc.perform(get("/agendamento/disponibilidade")
                        .param("carroId", corolla.getId().toString())
                        .param("data", data.toString()).session(sessaoCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem("14:00:00")))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem("14:30:00")));

        mockMvc.perform(get("/agendamento/meus").session(sessaoCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void impedeSobreposicaoMasPermiteHorarioContiguoEOuOutroVeiculo() throws Exception {
        LocalDate data = LocalDate.now().plusDays(2);
        reservar(corolla.getId(), data.atTime(14, 0), sessaoCliente)
                .andExpect(status().isOk());

        reservar(corolla.getId(), data.atTime(14, 15), sessaoCliente)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro", containsString("já possui")));

        reservar(corolla.getId(), data.atTime(14, 30), sessaoCliente)
                .andExpect(status().isOk());

        Carro civic = carroRepository.save(new Carro("Honda Civic", 2025));
        reservar(civic.getId(), data.atTime(14, 0), sessaoCliente)
                .andExpect(status().isOk());
    }

    @Test
    void impedeReservaForaDoExpedienteOuDeVeiculoIndisponivel() throws Exception {
        LocalDate data = LocalDate.now().plusDays(2);
        reservar(corolla.getId(), data.atTime(8, 30), sessaoCliente)
                .andExpect(status().isBadRequest());
        reservar(corolla.getId(), data.atTime(17, 31), sessaoCliente)
                .andExpect(status().isBadRequest());
        reservar(corolla.getId(), LocalDate.now().minusDays(1).atTime(14, 0), sessaoCliente)
                .andExpect(status().isBadRequest());

        corolla.setStatus(StatusCarro.VENDIDO);
        carroRepository.save(corolla);
        reservar(corolla.getId(), data.atTime(14, 0), sessaoCliente)
                .andExpect(status().isBadRequest());
    }

    @Test
    void duasConfirmacoesSimultaneasNaoReservamMesmoVeiculo() throws Exception {
        LocalDateTime inicio = LocalDate.now().plusDays(2).atTime(15, 0);
        Usuario outroCliente = usuarioRepository.save(new Usuario("João", "joao@test.com", "senha"));
        MockHttpSession outraSessao = sessao(outroCliente);
        CountDownLatch inicioSimultaneo = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> primeira = executor.submit(() -> reservarEmParalelo(
                    inicioSimultaneo, corolla.getId(), inicio, sessaoCliente));
            Future<Integer> segunda = executor.submit(() -> reservarEmParalelo(
                    inicioSimultaneo, corolla.getId(), inicio, outraSessao));

            inicioSimultaneo.countDown();
            int statusPrimeira = primeira.get();
            int statusSegunda = segunda.get();

            org.junit.jupiter.api.Assertions.assertTrue(
                    (statusPrimeira == 200 && statusSegunda == 409)
                            || (statusPrimeira == 409 && statusSegunda == 200));
            org.junit.jupiter.api.Assertions.assertEquals(1,
                    agendamentoRepository.findByCarro_IdOrderByDataHoraAsc(corolla.getId()).size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void administradorFiltraPorDataEVeiculoEAtualizaSituacao() throws Exception {
        LocalDate data = LocalDate.now().plusDays(2);
        MvcResult resultado = reservar(corolla.getId(), data.atTime(16, 0), sessaoCliente)
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        long agendamentoId = json.get("id").asLong();

        Usuario admin = new Usuario("Admin", "admin@test.com", "senha");
        admin.setPapel(PapelUsuario.ADMIN);
        admin = usuarioRepository.save(admin);
        MockHttpSession sessaoAdmin = sessao(admin);

        mockMvc.perform(get("/agendamento")
                        .param("data", data.toString())
                        .param("carroId", corolla.getId().toString())
                        .session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cliente.nome").value("Ana"))
                .andExpect(jsonPath("$[0].carro.modelo").value("Toyota Corolla"));

        mockMvc.perform(put("/agendamento/" + agendamentoId + "/status")
                        .param("status", StatusAgendamento.REALIZADO.name())
                        .session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REALIZADO"));
    }

    @Test
    void administradorNaoPodeReativarAgendamentoComDataPassada() throws Exception {
        Usuario admin = new Usuario("Admin", "admin@test.com", "senha");
        admin.setPapel(PapelUsuario.ADMIN);
        MockHttpSession sessaoAdmin = sessao(usuarioRepository.save(admin));

        Agendamento passado = new Agendamento();
        passado.setCarro(corolla);
        passado.setCliente(cliente);
        passado.setDataHora(LocalDate.now().minusDays(1).atTime(14, 0));
        passado.setStatus(StatusAgendamento.CANCELADO);
        passado = agendamentoRepository.save(passado);

        mockMvc.perform(put("/agendamento/" + passado.getId() + "/status")
                        .param("status", StatusAgendamento.CONFIRMADO.name())
                        .session(sessaoAdmin))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions reservar(
            Long carroId, LocalDateTime dataHora, MockHttpSession sessao) throws Exception {
        String conteudo = objectMapper.createObjectNode()
                .put("carroId", carroId)
                .put("dataHora", dataHora.toString())
                .toString();
        return mockMvc.perform(post("/agendamento")
                .session(sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content(conteudo));
    }

    private int reservarEmParalelo(CountDownLatch inicio, Long carroId,
                                   LocalDateTime dataHora, MockHttpSession sessao) throws Exception {
        inicio.await();
        return reservar(carroId, dataHora, sessao).andReturn().getResponse().getStatus();
    }

    private MockHttpSession sessao(Usuario usuario) {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute("usuarioLogadoId", usuario.getId());
        return sessao;
    }
}
