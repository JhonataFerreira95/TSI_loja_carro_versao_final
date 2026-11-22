package br.org.edu.ifrn.LojaCarro.repository;

import br.org.edu.ifrn.LojaCarro.model.Agendamento;
import br.org.edu.ifrn.LojaCarro.model.StatusAgendamento;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    List<Agendamento> findByClienteOrderByDataHoraDesc(Usuario cliente);

    List<Agendamento> findByCarro_IdAndDataHoraLessThanAndDataHoraGreaterThanAndStatusIn(
            Long carroId, LocalDateTime fim, LocalDateTime inicio, Collection<StatusAgendamento> status);

    List<Agendamento> findByCarro_IdAndDataHoraLessThanAndDataHoraGreaterThanAndStatusInAndIdNot(
            Long carroId, LocalDateTime fim, LocalDateTime inicio,
            Collection<StatusAgendamento> status, Long id);

    List<Agendamento> findByDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAsc(
            LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByCarro_IdAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAsc(
            Long carroId, LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByCarro_IdOrderByDataHoraAsc(Long carroId);
}
