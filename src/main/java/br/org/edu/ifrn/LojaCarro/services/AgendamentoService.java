package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.AgendamentoConflitoException;
import br.org.edu.ifrn.LojaCarro.model.Agendamento;
import br.org.edu.ifrn.LojaCarro.model.Carro;
import br.org.edu.ifrn.LojaCarro.model.StatusAgendamento;
import br.org.edu.ifrn.LojaCarro.model.StatusCarro;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.AgendamentoRepository;
import br.org.edu.ifrn.LojaCarro.repository.CarroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

@Service
public class AgendamentoService {
    private static final int DURACAO_MINUTOS = 30;
    private static final LocalTime ABERTURA = LocalTime.of(9, 0);
    private static final LocalTime ULTIMO_INICIO = LocalTime.of(17, 30);
    private static final List<StatusAgendamento> STATUS_ATIVOS =
            List.of(StatusAgendamento.CONFIRMADO, StatusAgendamento.PENDENTE);

    private final AgendamentoRepository agendamentoRepository;
    private final CarroRepository carroRepository;
    private final SessaoService sessaoService;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              CarroRepository carroRepository,
                              SessaoService sessaoService) {
        this.agendamentoRepository = agendamentoRepository;
        this.carroRepository = carroRepository;
        this.sessaoService = sessaoService;
    }

    @Transactional
    public Agendamento criar(Long carroId, LocalDateTime dataHora, String observacao) {
        Usuario cliente = usuarioAtual();
        validarDataHora(dataHora);

        Carro carro = carroRepository.findByIdForUpdate(carroId)
                .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado."));
        validarCarroDisponivel(carro);
        validarSemConflito(carroId, dataHora, null);

        Agendamento agendamento = new Agendamento();
        agendamento.setCarro(carro);
        agendamento.setCliente(cliente);
        agendamento.setDataHora(dataHora);
        agendamento.setObservacao(observacao);
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        return agendamentoRepository.save(agendamento);
    }

    public List<Agendamento> meus() {
        return agendamentoRepository.findByClienteOrderByDataHoraDesc(usuarioAtual());
    }

    public List<Agendamento> listarTodos(LocalDate data, Long carroId) {
        if (data != null && carroId != null) {
            LocalDateTime inicio = data.atStartOfDay();
            return agendamentoRepository
                    .findByCarro_IdAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAsc(
                            carroId, inicio, inicio.plusDays(1));
        }
        if (data != null) {
            LocalDateTime inicio = data.atStartOfDay();
            return agendamentoRepository.findByDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAsc(
                    inicio, inicio.plusDays(1));
        }
        if (carroId != null) {
            return agendamentoRepository.findByCarro_IdOrderByDataHoraAsc(carroId);
        }
        return agendamentoRepository.findAll(
                org.springframework.data.domain.Sort.by("dataHora").ascending());
    }

    public List<LocalTime> horariosDisponiveis(Long carroId, LocalDate data) {
        Carro carro = carroRepository.findById(carroId)
                .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado."));
        if (carro.getStatus() != StatusCarro.DISPONIVEL || data.isBefore(LocalDate.now())) {
            return List.of();
        }

        LocalDateTime inicioDia = data.atTime(ABERTURA).minusMinutes(DURACAO_MINUTOS);
        LocalDateTime fimDia = data.atTime(18, 0);
        List<Agendamento> agendamentos = agendamentoRepository
                .findByCarro_IdAndDataHoraLessThanAndDataHoraGreaterThanAndStatusIn(
                        carroId, fimDia, inicioDia, STATUS_ATIVOS);
        LocalDateTime antecedenciaMinima = LocalDateTime.now().plusMinutes(DURACAO_MINUTOS);

        return Stream.iterate(ABERTURA, hora -> !hora.isAfter(ULTIMO_INICIO),
                        hora -> hora.plusMinutes(DURACAO_MINUTOS))
                .filter(hora -> !data.atTime(hora).isBefore(antecedenciaMinima))
                .filter(hora -> agendamentos.stream().noneMatch(agendamento ->
                        conflita(data.atTime(hora), agendamento.getDataHora())))
                .toList();
    }

    @Transactional
    public Agendamento cancelar(Long id) {
        Usuario cliente = usuarioAtual();
        Agendamento agendamento = buscarAgendamento(id);
        if (!agendamento.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Você só pode cancelar seus próprios agendamentos.");
        }
        if (!STATUS_ATIVOS.contains(agendamento.getStatus())
                || !agendamento.getDataHora().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Só é possível cancelar agendamentos futuros confirmados.");
        }
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento alterarStatus(Long id, StatusAgendamento status) {
        if (status == null) {
            throw new IllegalArgumentException("Informe uma situação válida para o agendamento.");
        }
        Agendamento agendamento = buscarAgendamento(id);
        Long carroId = agendamento.getCarro().getId();
        carroRepository.findByIdForUpdate(carroId)
                .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado."));
        if (STATUS_ATIVOS.contains(status)) {
            if (!STATUS_ATIVOS.contains(agendamento.getStatus())) {
                validarDataHora(agendamento.getDataHora());
            }
            validarCarroDisponivel(agendamento.getCarro());
            validarSemConflito(carroId, agendamento.getDataHora(), id);
        }
        agendamento.setStatus(status);
        return agendamentoRepository.save(agendamento);
    }

    private Usuario usuarioAtual() {
        return sessaoService.usuarioLogado()
                .orElseThrow(() -> new IllegalStateException("É necessário fazer login."));
    }

    private Agendamento buscarAgendamento(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));
    }

    private void validarDataHora(LocalDateTime dataHora) {
        if (dataHora == null || dataHora.isBefore(LocalDateTime.now().plusMinutes(DURACAO_MINUTOS))) {
            throw new IllegalArgumentException("Informe uma data e horário com pelo menos 30 minutos de antecedência.");
        }
        LocalTime hora = dataHora.toLocalTime();
        if (hora.isBefore(ABERTURA) || hora.isAfter(ULTIMO_INICIO)) {
            throw new IllegalArgumentException("Os test drives acontecem diariamente das 09:00 às 18:00.");
        }
    }

    private void validarCarroDisponivel(Carro carro) {
        if (carro.getStatus() != StatusCarro.DISPONIVEL) {
            throw new IllegalArgumentException("Este veículo não está disponível para test drive.");
        }
    }

    private void validarSemConflito(Long carroId, LocalDateTime inicio, Long agendamentoIgnorado) {
        LocalDateTime fim = inicio.plusMinutes(DURACAO_MINUTOS);
        LocalDateTime limiteInferior = inicio.minusMinutes(DURACAO_MINUTOS);
        List<Agendamento> conflitos = agendamentoIgnorado == null
                ? agendamentoRepository.findByCarro_IdAndDataHoraLessThanAndDataHoraGreaterThanAndStatusIn(
                        carroId, fim, limiteInferior, STATUS_ATIVOS)
                : agendamentoRepository.findByCarro_IdAndDataHoraLessThanAndDataHoraGreaterThanAndStatusInAndIdNot(
                        carroId, fim, limiteInferior, STATUS_ATIVOS, agendamentoIgnorado);
        if (!conflitos.isEmpty()) {
            throw new AgendamentoConflitoException("Este veículo já possui um test drive nesse horário.");
        }
    }

    private boolean conflita(LocalDateTime inicio, LocalDateTime outroInicio) {
        LocalDateTime fim = inicio.plusMinutes(DURACAO_MINUTOS);
        LocalDateTime outroFim = outroInicio.plusMinutes(DURACAO_MINUTOS);
        return inicio.isBefore(outroFim) && outroInicio.isBefore(fim);
    }
}
