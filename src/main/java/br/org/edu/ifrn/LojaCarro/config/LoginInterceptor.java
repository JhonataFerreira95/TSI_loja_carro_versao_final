package br.org.edu.ifrn.LojaCarro.config;

import br.org.edu.ifrn.LojaCarro.model.Carro;
import br.org.edu.ifrn.LojaCarro.model.PapelUsuario;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.CarroRepository;
import br.org.edu.ifrn.LojaCarro.services.LogService;
import br.org.edu.ifrn.LojaCarro.services.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Bloqueia as rotas do sistema para quem não fez login
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final Pattern ROTA_IMAGEM_CARRO = Pattern.compile("^/carro/(\\d+)/imagem$");

    @Autowired
    private SessaoService sessaoService;

    @Autowired
    private CarroRepository carroRepository;

    @Autowired
    private LogService logService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String contextPath = request.getContextPath();
        String caminho = request.getRequestURI().substring(contextPath.length());
        Optional<Usuario> usuarioLogado = sessaoService.usuarioLogado();
        if (usuarioLogado.isEmpty()) {
            if ("/usuario/salvar".equals(caminho) && "POST".equals(request.getMethod())) {
                return true;
            }
            if ("GET".equals(request.getMethod()) && ("/carro/listarCarros".equals(caminho)
                    || caminho.matches("/carro/\\d+")
                    || caminho.startsWith("/uploads/"))) {
                return true;
            }
            registrarAcessoNegado(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "É necessário fazer login.");
            return false;
        }

        if (temPermissao(usuarioLogado.get(), caminho, request.getMethod())) {
            return true;
        }

        registrarAcessoNegado(request, response, HttpServletResponse.SC_FORBIDDEN,
                "Você não tem permissão para acessar este recurso.");
        return false;
    }

    private boolean temPermissao(Usuario usuario, String caminho, String metodo) {
        boolean administrador = usuario.getPapel() == PapelUsuario.ADMIN;

        if ("GET".equals(metodo) && ("/carro/listarCarros".equals(caminho)
                || caminho.matches("/carro/\\d+")
                || caminho.startsWith("/uploads/"))) {
            return true;
        }

        if ("/usuario/salvar".equals(caminho) && "POST".equals(metodo)) {
            return administrador;
        }

        if ("/agendamento/meus".equals(caminho) && "GET".equals(metodo)) {
            return true;
        }

        if ("/agendamento".equals(caminho) && "POST".equals(metodo)) {
            return true;
        }

        if (caminho.startsWith("/agendamento")) {
            return administrador;
        }

        if ("/carro/meusCarros".equals(caminho) && "GET".equals(metodo)) {
            return true;
        }

        if ("/carro/salvar".equals(caminho) && "POST".equals(metodo)) {
            return true;
        }

        Matcher rotaImagem = ROTA_IMAGEM_CARRO.matcher(caminho);
        if (rotaImagem.matches()) {
            if (administrador) {
                return true;
            }
            if (!"PUT".equals(metodo) && !"DELETE".equals(metodo)) {
                return false;
            }
            return buscarCarro(rotaImagem.group(1)).map(carro -> pertenceAoUsuario(carro, usuario)).orElse(false);
        }

        if (caminho.startsWith("/uploads/")) {
            return administrador || carroRepository.findFirstByImagem(caminho)
                    .map(carro -> pertenceAoUsuario(carro, usuario)).orElse(false);
        }

        if (caminho.equals("/carro") || caminho.startsWith("/carro/")
                || caminho.startsWith("/log/") || caminho.equals("/log")
                || caminho.equals("/usuario/listarUsuarios")) {
            return administrador;
        }

        if (caminho.matches("/usuario/\\d+")) {
            if (administrador) {
                return true;
            }
            return caminho.equals("/usuario/" + usuario.getId())
                    && ("GET".equals(metodo) || "PUT".equals(metodo));
        }

        return false;
    }

    private Optional<Carro> buscarCarro(String id) {
        try {
            return carroRepository.findById(Long.valueOf(id));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private boolean pertenceAoUsuario(Carro carro, Usuario usuario) {
        return carro.getCadastradoPor() != null
                && carro.getCadastradoPor().getId().equals(usuario.getId());
    }

    private void registrarAcessoNegado(HttpServletRequest request, HttpServletResponse response,
                                       int status, String mensagem) throws Exception {
        logService.registrar("ACESSO_NEGADO", "Sistema",
                "Tentativa de acesso sem permissão: " + request.getMethod() + " " + request.getRequestURI());
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }
}
