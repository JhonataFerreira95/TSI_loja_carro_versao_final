package br.org.edu.ifrn.LojaCarro.model;

public enum StatusAgendamento {
    CONFIRMADO,
    CANCELADO,
    REALIZADO,
    // Mantidos para que registros criados pela versão anterior continuem legíveis.
    PENDENTE,
    CONCLUIDO
}
