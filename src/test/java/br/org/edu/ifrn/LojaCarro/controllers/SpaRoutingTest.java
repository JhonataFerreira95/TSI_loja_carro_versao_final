package br.org.edu.ifrn.LojaCarro.controllers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SpaRoutingTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/veiculos",
            "/veiculos/123",
            "/test-drive/123",
            "/meus-agendamentos",
            "/admin/veiculos"
    })
    void rotasDoFrontendEncaminhamParaIndex(String rota) throws Exception {
        mockMvc.perform(get(rota))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }
}
