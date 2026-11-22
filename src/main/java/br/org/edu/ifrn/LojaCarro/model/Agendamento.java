package br.org.edu.ifrn.LojaCarro.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Agendamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Carro carro;

    @ManyToOne(optional = false)
    private Usuario cliente;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(updatable = false)
    private LocalDateTime dataCriacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAgendamento status = StatusAgendamento.CONFIRMADO;

    @Column(length = 500)
    private String observacao;

    public Agendamento() {}

    @PrePersist
    void registrarCriacao() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Carro getCarro() { return carro; }
    public void setCarro(Carro carro) { this.carro = carro; }
    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario cliente) { this.cliente = cliente; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
