package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.model.Agendamento;
import br.org.edu.ifrn.LojaCarro.model.StatusAgendamento;
import br.org.edu.ifrn.LojaCarro.services.AgendamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agendamento")
public class AgendamentoController {
    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @PostMapping
    public ResponseEntity<Agendamento> criar(@RequestBody Map<String, Object> dados) {
        if (dados.get("carroId") == null || dados.get("dataHora") == null) {
            throw new IllegalArgumentException("Informe o veículo, a data e o horário do test drive.");
        }
        Long carroId;
        LocalDateTime dataHora;
        try {
            carroId = Long.valueOf(dados.get("carroId").toString());
            dataHora = LocalDateTime.parse(dados.get("dataHora").toString());
        } catch (NumberFormatException | java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("Veículo ou data/hora inválidos.");
        }
        String observacao = dados.get("observacao") == null ? null : dados.get("observacao").toString();
        return ResponseEntity.ok(agendamentoService.criar(carroId, dataHora, observacao));
    }

    @GetMapping("/meus")
    public ResponseEntity<List<Agendamento>> meus() {
        return ResponseEntity.ok(agendamentoService.meus());
    }

    @GetMapping("/disponibilidade")
    public ResponseEntity<List<LocalTime>> horariosDisponiveis(@RequestParam Long carroId,
                                                                @RequestParam LocalDate data) {
        return ResponseEntity.ok(agendamentoService.horariosDisponiveis(carroId, data));
    }

    @GetMapping
    public ResponseEntity<List<Agendamento>> listarTodos(
            @RequestParam(required = false) LocalDate data,
            @RequestParam(required = false) Long carroId) {
        return ResponseEntity.ok(agendamentoService.listarTodos(data, carroId));
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Agendamento> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.cancelar(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Agendamento> alterarStatus(@PathVariable Long id,
                                                      @RequestParam StatusAgendamento status) {
        return ResponseEntity.ok(agendamentoService.alterarStatus(id, status));
    }
}
