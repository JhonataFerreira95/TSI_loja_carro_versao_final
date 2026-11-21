package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.model.Agendamento;
import br.org.edu.ifrn.LojaCarro.model.StatusAgendamento;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.AgendamentoRepository;
import br.org.edu.ifrn.LojaCarro.repository.CarroRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgendamentoService {
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

    public Agendamento criar(Long carroId, LocalDateTime dataHora, String observacao) {
        Usuario cliente = sessaoService.usuarioLogado()
                .orElseThrow(() -> new IllegalStateException("É necessário fazer login."));
        if (dataHora == null || dataHora.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new IllegalArgumentException("Informe uma data futura para o test drive.");
        }
        Agendamento agendamento = new Agendamento();
        agendamento.setCarro(carroRepository.findById(carroId)
                .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado.")));
        agendamento.setCliente(cliente);
        agendamento.setDataHora(dataHora);
        agendamento.setObservacao(observacao);
        agendamento.setStatus(StatusAgendamento.PENDENTE);
        return agendamentoRepository.save(agendamento);
    }

    public List<Agendamento> meus() {
        Usuario cliente = sessaoService.usuarioLogado()
                .orElseThrow(() -> new IllegalStateException("É necessário fazer login."));
        return agendamentoRepository.findByClienteOrderByDataHoraDesc(cliente);
    }

    public List<Agendamento> listarTodos() {
        return agendamentoRepository.findAll();
    }

    public Agendamento alterarStatus(Long id, StatusAgendamento status) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));
        agendamento.setStatus(status);
        return agendamentoRepository.save(agendamento);
    }
}
