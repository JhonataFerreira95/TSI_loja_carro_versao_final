package br.org.edu.ifrn.LojaCarro.repository;

import br.org.edu.ifrn.LojaCarro.model.Carro;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarroRepository extends JpaRepository<Carro, Long> {

    Optional<Carro> findFirstByModelo(String modelo);

    Optional<Carro> findFirstByImagem(String imagem);

    List<Carro> findAllByCadastradoPor_Id(Long usuarioId);

    boolean existsByCadastradoPor(Usuario usuario);
}
