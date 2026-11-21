package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.model.Agendamento;
import br.org.edu.ifrn.LojaCarro.model.StatusAgendamento;
import br.org.edu.ifrn.LojaCarro.services.AgendamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
        Long carroId = Long.valueOf(dados.get("carroId").toString());
        LocalDateTime dataHora = LocalDateTime.parse(dados.get("dataHora").toString());
        String observacao = dados.get("observacao") == null ? null : dados.get("observacao").toString();
        return ResponseEntity.ok(agendamentoService.criar(carroId, dataHora, observacao));
    }

    @GetMapping("/meus")
    public ResponseEntity<List<Agendamento>> meus() {
        return ResponseEntity.ok(agendamentoService.meus());
    }

    @GetMapping
    public ResponseEntity<List<Agendamento>> listarTodos() {
        return ResponseEntity.ok(agendamentoService.listarTodos());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Agendamento> alterarStatus(@PathVariable Long id,
                                                      @RequestParam StatusAgendamento status) {
        return ResponseEntity.ok(agendamentoService.alterarStatus(id, status));
    }
}
