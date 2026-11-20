package br.org.edu.ifrn.LojaCarro.config;

import br.org.edu.ifrn.LojaCarro.model.PapelUsuario;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.services.LogService;
import br.org.edu.ifrn.LojaCarro.services.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

// Bloqueia as rotas do sistema para quem não fez login
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private SessaoService sessaoService;

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

        if ("/usuario/salvar".equals(caminho) && "POST".equals(metodo)) {
            return administrador;
        }

        if (caminho.equals("/carro") || caminho.startsWith("/carro/")
                || caminho.startsWith("/log/") || caminho.equals("/log")
                || caminho.startsWith("/uploads/") || caminho.equals("/usuario/listarUsuarios")) {
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

    private void registrarAcessoNegado(HttpServletRequest request, HttpServletResponse response,
                                       int status, String mensagem) throws Exception {
        logService.registrar("ACESSO_NEGADO", "Sistema",
                "Tentativa de acesso sem permissão: " + request.getMethod() + " " + request.getRequestURI());
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }
}
