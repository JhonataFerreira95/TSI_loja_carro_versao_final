package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.CarroException;
import br.org.edu.ifrn.LojaCarro.model.Carro;
import br.org.edu.ifrn.LojaCarro.model.PapelUsuario;
import br.org.edu.ifrn.LojaCarro.model.StatusCarro;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.CarroRepository;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CarroService {

    private static final String ENTIDADE = "Carro";

    @Autowired
    public CarroRepository carroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LogService logService;

    @Autowired
    private SessaoService sessaoService;

    @Autowired
    private ImagemCarroService imagemCarroService;

    public Carro save(Carro c) {
        return save(c, null);
    }

    public Carro save(Carro c, MultipartFile imagem) {
        return save(c, imagem, null);
    }

    public Carro save(Carro c, MultipartFile imagem, Long usuarioId) {
        validarModelo(c.getModelo());  // Valida o modelo antes de salvar
        validarPreco(c.getPreco());
        Usuario usuarioLogado = sessaoService.usuarioLogado().orElse(null);
        boolean administrador = usuarioLogado != null
                && usuarioLogado.getPapel() == PapelUsuario.ADMIN;
        if (!administrador && usuarioId != null
                && (usuarioLogado == null || !usuarioId.equals(usuarioLogado.getId()))) {
            throw new CarroException("Usuários comuns só podem cadastrar carros na própria conta.");
        }
        Usuario responsavel = administrador && usuarioId != null ? buscarUsuario(usuarioId) : usuarioLogado;
        salvarImagemSeInformada(c, imagem);
        c.setCadastradoPor(responsavel);
        Carro salvo = carroRepository.save(c);
        logService.registrar("CRIAR", ENTIDADE, "Carro criado: ID " + salvo.getId() + ", modelo " + salvo.getModelo());
        return salvo;
    }

    // Novo método para deletar por ID
    public void deleteById(Long id) {
        if(id <= 0){
            throw new CarroException("O ID do carro não pode ser negativo. ID fornecido: " + id);
        }
        Carro carro = carroRepository.findById(id)
                .orElseThrow(() -> new CarroException("Carro com ID " + id + " não encontrado."));
        imagemCarroService.remover(carro.getImagem());
        carroRepository.delete(carro);
        logService.registrar("EXCLUIR", ENTIDADE, "Carro excluído: ID " + id);
    }

    // Novo método para pesquisar por ID
    public Optional<Carro> findById(Long id) {
        if(id <= 0){
            throw new CarroException("O ID do carro não pode ser negativo. ID fornecido: " + id);
        }
        logService.registrar("CONSULTAR", ENTIDADE, "Consulta do carro ID " + id);
        return carroRepository.findById(id);
    }

    // Novo método para listar todos os carros
    public List<Carro> findAll() {
        logService.registrar("CONSULTAR", ENTIDADE, "Listagem de todos os carros");
        return carroRepository.findAll();
    }

    public List<Carro> findMeusCarros() {
        Long usuarioId = sessaoService.usuarioLogado()
                .orElseThrow(() -> new IllegalStateException("Não há usuário autenticado."))
                .getId();
        logService.registrar("CONSULTAR", ENTIDADE, "Listagem dos carros do usuário logado");
        return carroRepository.findAllByCadastradoPor_Id(usuarioId);
    }

    public Carro atualizarImagem(Long id, MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new CarroException("Selecione uma imagem para enviar.");
        }
        Carro carro = carroRepository.findById(id)
                .orElseThrow(() -> new CarroException("Carro com ID " + id + " não encontrado."));
        String imagemAnterior = carro.getImagem();
        carro.setImagem(imagemCarroService.salvar(imagem));
        Carro atualizado = carroRepository.save(carro);
        if (!Objects.equals(imagemAnterior, atualizado.getImagem())) {
            imagemCarroService.remover(imagemAnterior);
        }
        logService.registrar("ATUALIZAR", ENTIDADE, "Imagem atualizada para o carro ID " + id);
        return atualizado;
    }

    public void removerImagem(Long id) {
        Carro carro = carroRepository.findById(id)
                .orElseThrow(() -> new CarroException("Carro com ID " + id + " não encontrado."));
        imagemCarroService.remover(carro.getImagem());
        carro.setImagem(null);
        carroRepository.save(carro);
        logService.registrar("EXCLUIR", ENTIDADE, "Imagem removida do carro ID " + id);
    }

    public Optional<Carro> findByModelo(String modelo) {
        validarModelo(modelo);
        logService.registrar("CONSULTAR", ENTIDADE, "Consulta do carro modelo " + modelo);
        return carroRepository.findFirstByModelo(modelo);
    }

    public Carro saveFromLegacy(String modelo, double preco) {
        Carro carro = new Carro(modelo, LocalDate.now().getYear(), preco);
        return save(carro);
    }

    public Carro updateByModelo(String modelo, double preco) {
        Carro carro = localizarCarroPorModelo(modelo);
        validarPreco(preco);
        carro.setPreco(preco);
        Carro atualizado = carroRepository.save(carro);
        logService.registrar("ATUALIZAR", ENTIDADE, "Preço do carro modelo " + modelo + " atualizado para " + preco);
        return atualizado;
    }

    public Carro deleteByModelo(String modelo) {
        Carro carro = localizarCarroPorModelo(modelo);
        imagemCarroService.remover(carro.getImagem());
        carroRepository.delete(carro);
        logService.registrar("EXCLUIR", ENTIDADE, "Carro excluído: ID " + carro.getId() + ", modelo " + modelo);
        return carro;
    }

    // Método para atualizar (usa o save existente, mas pode ser renomeado se preferir)
    public Carro update(Carro c) {
        return update(c, null);
    }

    public Carro update(Carro c, MultipartFile imagem) {
        return update(c, imagem, null);
    }

    public Carro update(Carro c, MultipartFile imagem, Long usuarioId) {
        if (c.getId() == null) {
            throw new CarroException("O ID do carro para atualização não pode ser nulo.");
        }
        Carro existente = carroRepository.findById(c.getId())
                .orElseThrow(() -> new CarroException("Carro com ID " + c.getId() + " não encontrado para atualização."));
        validarModelo(c.getModelo());  // Valida o modelo antes de atualizar
        validarPreco(c.getPreco());
        Usuario responsavel = usuarioId == null ? existente.getCadastradoPor() : buscarUsuario(usuarioId);
        String imagemAnterior = existente.getImagem();
        c.setImagem(existente.getImagem());
        salvarImagemSeInformada(c, imagem);
        c.setCadastradoPor(responsavel);
        c.setStatus(existente.getStatus());
        Carro atualizado = carroRepository.save(c);  // Retorna o carro salvo para feedback
        if (!Objects.equals(imagemAnterior, atualizado.getImagem())) {
            imagemCarroService.remover(imagemAnterior);
        }
        logService.registrar("ATUALIZAR", ENTIDADE, "Carro atualizado: ID " + atualizado.getId() + ", modelo " + atualizado.getModelo());
        return atualizado;
    }

    @Transactional
    public Carro alterarStatus(Long id, StatusCarro status) {
        if (status == null) {
            throw new CarroException("Informe uma situação válida para o veículo.");
        }
        Carro carro = carroRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new CarroException("Carro com ID " + id + " não encontrado."));
        carro.setStatus(status);
        Carro atualizado = carroRepository.save(carro);
        logService.registrar("ATUALIZAR", ENTIDADE,
                "Situação do carro ID " + id + " alterada para " + status);
        return atualizado;
    }

    private void salvarImagemSeInformada(Carro carro, MultipartFile imagem) {
        if (imagem != null && !imagem.isEmpty()) {
            carro.setImagem(imagemCarroService.salvar(imagem));
        }
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new CarroException("Usuário com ID " + id + " não encontrado."));
    }

    // Validação do modelo
    private void validarModelo(String modelo) {
        if (modelo == null || modelo.trim().isEmpty()) {
            throw new CarroException("O modelo do carro não pode estar vazio.");
        }
    }

    private void validarPreco(double preco) {
        if (preco < 0) {
            throw new CarroException("O preço do carro não pode ser negativo. Valor fornecido: " + preco);
        }
    }

    private Carro localizarCarroPorModelo(String modelo) {
        validarModelo(modelo);
        return carroRepository.findFirstByModelo(modelo)
                .orElseThrow(() -> new CarroException("Carro com modelo " + modelo + " não encontrado."));
    }
}
