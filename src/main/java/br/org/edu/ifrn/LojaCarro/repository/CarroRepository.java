package br.org.edu.ifrn.LojaCarro.repository;

import br.org.edu.ifrn.LojaCarro.model.Carro;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarroRepository extends JpaRepository<Carro, Long> {

    Optional<Carro> findFirstByModelo(String modelo);

    Optional<Carro> findFirstByImagem(String imagem);

    List<Carro> findAllByCadastradoPor_Id(Long usuarioId);

    boolean existsByCadastradoPor(Usuario usuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select carro from Carro carro where carro.id = :id")
    Optional<Carro> findByIdForUpdate(@Param("id") Long id);
}
