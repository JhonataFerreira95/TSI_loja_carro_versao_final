package br.org.edu.ifrn.LojaCarro.exception;

import br.org.edu.ifrn.LojaCarro.CarroException;
import br.org.edu.ifrn.LojaCarro.AgendamentoConflitoException;
import br.org.edu.ifrn.LojaCarro.UsuarioException;
import br.org.edu.ifrn.LojaCarro.services.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private LogService logService;

    @ExceptionHandler(CarroException.class)
    public ResponseEntity<Map<String, String>> handleCarroException(CarroException ex) {
        return erro("Carro", ex.getMessage());
    }

    @ExceptionHandler(UsuarioException.class)
    public ResponseEntity<Map<String, String>> handleUsuarioException(UsuarioException ex) {
        return erro("Usuario", ex.getMessage());
    }

    @ExceptionHandler(AgendamentoConflitoException.class)
    public ResponseEntity<Map<String, String>> handleAgendamentoConflito(AgendamentoConflitoException ex) {
        logService.registrar("CONFLITO", "Agendamento", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("erro", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleRequestException(RuntimeException ex) {
        return erro("Sistema", ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> erro(String entidade, String mensagem) {
        logService.registrar("ERRO", entidade, mensagem);
        Map<String, String> body = new HashMap<>();
        body.put("erro", mensagem);
        return ResponseEntity.badRequest().body(body);
    }
}
