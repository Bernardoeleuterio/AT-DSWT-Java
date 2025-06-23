package org.example;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AplicacaoPrincipalTest {

    private Javalin app;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    void setup() {
        app = Javalin.create()
                .start(7001);

        app.get("/hello", ctx -> ctx.result("Hello, Javalin!"));

        app.get("/status", ctx -> {
            ctx.json(Map.of("status", "ok", "timestamp", "2025-01-01T00:00:00"));
        });

        app.post("/echo", ctx -> {
            Map<String, Object> corpoRequisicao = ctx.bodyAsClass(Map.class);
            String mensagemRecebida = (String) corpoRequisicao.get("mensagem");
            ctx.json(Map.of("mensagem", mensagemRecebida));
        });

        app.get("/saudacao/{nome}", ctx -> {
            String nome = ctx.pathParam("nome");
            ctx.json(Map.of("mensagem", "Olá, " + nome + "!"));
        });

        app.post("/tarefas", ctx -> {
            Tarefa novaTarefa = ctx.bodyAsClass(Tarefa.class);

            if (novaTarefa.getTitulo() == null || novaTarefa.getTitulo().trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("erro", "O título da tarefa é obrigatório."));
                return;
            }

            AplicacaoPrincipal.tarefas.put(novaTarefa.getId(), novaTarefa);
            ctx.status(HttpStatus.CREATED);
            ctx.json(novaTarefa);
        });

        app.get("/tarefas", ctx -> {
            ctx.json(new ArrayList<>(AplicacaoPrincipal.tarefas.values()));
        });

        app.get("/tarefas/{id}", ctx -> {
            String idBusca = ctx.pathParam("id");
            Tarefa tarefaEncontrada = null;
            try {
                UUID uuidIdBusca = UUID.fromString(idBusca);
                tarefaEncontrada = AplicacaoPrincipal.tarefas.get(uuidIdBusca);
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("erro", "O ID fornecido não é um formato UUID válido."));
                return;
            }

            if (tarefaEncontrada != null) {
                ctx.json(tarefaEncontrada);
            } else {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(Map.of("erro", "Tarefa não encontrada com o ID: " + idBusca));
            }
        });

        System.out.println("Servidor Javalin de TESTE iniciado na porta 7001 para JUnit.");
    }

    @AfterAll
    void tearDown() {
        app.stop();
        AplicacaoPrincipal.tarefas.clear();
        System.out.println("Servidor Javalin de TESTE parado.");
    }


    private TestResponse enviaRequisicao(String metodo, String path, String body) throws Exception {
        URL url = new URI("http://localhost:7001" + path).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(metodo);
        connection.setDoOutput(metodo.equals("POST") || metodo.equals("PUT"));
        connection.setRequestProperty("Content-Type", "application/json");

        if (body != null && !body.isEmpty()) {
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        int status = connection.getResponseCode();
        String responseBody = "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                status >= 300 ? connection.getErrorStream() : connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            responseBody = response.toString();
        } finally {
            connection.disconnect();
        }
        return new TestResponse(status, responseBody);
    }

    private static class TestResponse {
        public final int status;
        public final String body;

        public TestResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }


    @Test
    @Order(1)
    @DisplayName("Teste /hello: verifica status 200 e resposta 'Hello, Javalin!'")
    void testeHelloEndpoint() throws Exception {
        TestResponse resposta = enviaRequisicao("GET", "/hello", null);
        assertEquals(200, resposta.status);
        assertEquals("Hello, Javalin!", resposta.body);
    }

    @Test
    @Order(2)
    @DisplayName("Teste /status: verifica status 200 e JSON de status")
    void testeStatusEndpoint() throws Exception {
        TestResponse resposta = enviaRequisicao("GET", "/status", null);
        assertEquals(200, resposta.status);
        Map<String, String> jsonResponse = objectMapper.readValue(resposta.body, Map.class);
        assertEquals("ok", jsonResponse.get("status"));
        assertTrue(jsonResponse.containsKey("timestamp"));
    }

    @Test
    @Order(3)
    @DisplayName("Teste /echo (POST): verifica se o JSON é retornado corretamente")
    void testeEchoEndpoint() throws Exception {
        String jsonEnvio = "{\"mensagem\":\"Mensagem de teste para echo!\"}";
        TestResponse resposta = enviaRequisicao("POST", "/echo", jsonEnvio);
        assertEquals(200, resposta.status);
        assertEquals(jsonEnvio, resposta.body);
    }

    @Test
    @Order(4)
    @DisplayName("Teste /saudacao/{nome} (GET): verifica saudação para um nome")
    void testeSaudacaoEndpoint() throws Exception {
        TestResponse resposta = enviaRequisicao("GET", "/saudacao/Testador", null);
        assertEquals(200, resposta.status);
        Map<String, String> jsonResponse = objectMapper.readValue(resposta.body, Map.class);
        assertEquals("Olá, Testador!", jsonResponse.get("mensagem"));
    }

    private static String taskIdParaBusca = null;

    @Test
    @Order(5)
    @DisplayName("Teste POST /tarefas: cria uma nova tarefa e verifica status 201")
    void testeCriaTarefaEndpoint() throws Exception {
        String jsonTarefa = "{\"titulo\":\"Comprar Leite\",\"descricao\":\"No supermercado\"}";
        TestResponse resposta = enviaRequisicao("POST", "/tarefas", jsonTarefa);
        assertEquals(201, resposta.status);

        Map<String, Object> tarefaCriada = objectMapper.readValue(resposta.body, Map.class);
        assertNotNull(tarefaCriada.get("id"));
        assertEquals("Comprar Leite", tarefaCriada.get("titulo"));
        assertEquals(false, tarefaCriada.get("concluida"));

        taskIdParaBusca = (String) tarefaCriada.get("id");
        System.out.println("Tarefa de teste criada com ID: " + taskIdParaBusca);
    }

    @Test
    @Order(6)
    @DisplayName("Teste POST /tarefas: tenta criar tarefa sem título, espera 400")
    void testeCriaTarefaSemTituloEndpoint() throws Exception {
        String jsonTarefaInvalida = "{\"titulo\":\"   \",\"descricao\":\"Esta tarefa não deveria ser criada\"}";
        TestResponse resposta = enviaRequisicao("POST", "/tarefas", jsonTarefaInvalida);
        assertEquals(400, resposta.status);
        Map<String, String> erroResponse = objectMapper.readValue(resposta.body, Map.class);
        assertEquals("O título da tarefa é obrigatório.", erroResponse.get("erro"));
    }


    @Test
    @Order(7)
    @DisplayName("Teste GET /tarefas: verifica listagem de tarefas (não vazia)")
    void testeListaTarefasEndpoint() throws Exception {
        if (taskIdParaBusca == null) {
            testeCriaTarefaEndpoint();
        }

        TestResponse resposta = enviaRequisicao("GET", "/tarefas", null);
        assertEquals(200, resposta.status);
        List<Map<String, Object>> tarefasListadas = objectMapper.readValue(resposta.body, List.class);
        assertFalse(tarefasListadas.isEmpty());

        assertTrue(tarefasListadas.stream().anyMatch(t -> taskIdParaBusca.equals(t.get("id"))));
    }

    @Test
    @Order(8)
    @DisplayName("Teste GET /tarefas/{id}: busca tarefa existente e verifica dados")
    void testeBuscaTarefaExistenteEndpoint() throws Exception {
        assertNotNull(taskIdParaBusca, "ID da tarefa não deveria ser nulo para este teste.");

        TestResponse resposta = enviaRequisicao("GET", "/tarefas/" + taskIdParaBusca, null);
        assertEquals(200, resposta.status);
        Map<String, Object> tarefaEncontrada = objectMapper.readValue(resposta.body, Map.class);
        assertEquals(taskIdParaBusca, tarefaEncontrada.get("id"));
        assertEquals("Comprar Leite", tarefaEncontrada.get("titulo"));
    }

    @Test
    @Order(9)
    @DisplayName("Teste GET /tarefas/{id}: busca tarefa inexistente e verifica status 404")
    void testeBuscaTarefaInexistenteEndpoint() throws Exception {
        String idInexistente = "11111111-2222-3333-4444-555555555555";
        TestResponse resposta = enviaRequisicao("GET", "/tarefas/" + idInexistente, null);
        assertEquals(404, resposta.status);
        Map<String, String> erroResponse = objectMapper.readValue(resposta.body, Map.class);
        assertEquals("Tarefa não encontrada com o ID: " + idInexistente, erroResponse.get("erro"));
    }

    @Test
    @Order(10)
    @DisplayName("Teste GET /tarefas/{id}: busca com ID inválido e verifica status 400")
    void testeBuscaTarefaIdInvalidoEndpoint() throws Exception {
        String idInvalido = "NAO-EH-UM-UUID-VALIDO";
        TestResponse resposta = enviaRequisicao("GET", "/tarefas/" + idInvalido, null);
        assertEquals(400, resposta.status);
        Map<String, String> erroResponse = objectMapper.readValue(resposta.body, Map.class);
        assertEquals("O ID fornecido não é um formato UUID válido.", erroResponse.get("erro"));
    }
}
