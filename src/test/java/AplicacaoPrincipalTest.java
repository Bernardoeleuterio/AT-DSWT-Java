package org.example;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        app.post("/usuarios", ctx -> {
            Usuario novoUsuario = ctx.bodyAsClass(Usuario.class);

            if (AplicacaoPrincipal.usuarios.containsKey(novoUsuario.getEmail())) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(Map.of("erro", "Email já cadastrado!"));
                return;
            }
            if (novoUsuario.getNome() == null || novoUsuario.getNome().isEmpty() ||
                    novoUsuario.getEmail() == null || novoUsuario.getEmail().isEmpty() ||
                    novoUsuario.getIdade() <= 0) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("erro", "Nome, email e idade são obrigatórios e idade deve ser positiva."));
                return;
            }

            AplicacaoPrincipal.usuarios.put(novoUsuario.getEmail(), novoUsuario);
            ctx.status(HttpStatus.CREATED);
            ctx.json(novoUsuario);
        });

        app.get("/usuarios", ctx -> {
            ctx.json(new ArrayList<>(AplicacaoPrincipal.usuarios.values()));
        });

        app.get("/usuarios/{email}", ctx -> {
            String emailBusca = ctx.pathParam("email");
            Usuario usuarioEncontrado = AplicacaoPrincipal.usuarios.get(emailBusca);
            if (usuarioEncontrado != null) {
                ctx.json(usuarioEncontrado);
            } else {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(Map.of("erro", "Usuário não encontrado com o email: " + emailBusca));
            }
        });

        System.out.println("Servidor Javalin de TESTE iniciado na porta 7001 para JUnit.");
    }

    @AfterAll
    void tearDown() {
        app.stop();
        AplicacaoPrincipal.usuarios.clear();
        System.out.println("Servidor Javalin de TESTE parado.");
    }

    private TestResponse enviaRequisicao(String metodo, String path, String body) throws Exception {
        URL url = new URL("http://localhost:7001" + path);
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

    @Test
    @Order(5)
    @DisplayName("Teste POST /usuarios: cria um novo usuário e verifica status 201")
    void testeCriaUsuarioEndpoint() throws Exception {
        String jsonUsuario = "{\"nome\":\"Usuario Teste\",\"email\":\"teste.usuario@teste.com\",\"idade\":25}";
        TestResponse resposta = enviaRequisicao("POST", "/usuarios", jsonUsuario);
        assertEquals(201, resposta.status);
        Map<String, Object> usuarioCriado = objectMapper.readValue(resposta.body, Map.class);
        assertEquals("teste.usuario@teste.com", usuarioCriado.get("email"));
    }

    @Test
    @Order(6)
    @DisplayName("Teste POST /usuarios: tenta criar usuário com email duplicado, espera 409")
    void testeCriaUsuarioEmailDuplicadoEndpoint() throws Exception {
        String jsonUsuarioDuplicado = "{\"nome\":\"Usuario Teste Duplicado\",\"email\":\"teste.usuario@teste.com\",\"idade\":30}";
        TestResponse resposta = enviaRequisicao("POST", "/usuarios", jsonUsuarioDuplicado);
        assertEquals(409, resposta.status);
        Map<String, String> erroResponse = objectMapper.readValue(resposta.body, Map.class);
        assertEquals("Email já cadastrado!", erroResponse.get("erro"));
    }

    @Test
    @Order(7)
    @DisplayName("Teste GET /usuarios: verifica listagem de usuários (não vazio)")
    void testeListaUsuariosEndpoint() throws Exception {
        TestResponse resposta = enviaRequisicao("GET", "/usuarios", null);
        assertEquals(200, resposta.status);
        List<Map<String, Object>> usuariosListados = objectMapper.readValue(resposta.body, List.class);
        assertFalse(usuariosListados.isEmpty());
        assertTrue(usuariosListados.stream().anyMatch(u -> "teste.usuario@teste.com".equals(u.get("email"))));
    }

    @Test
    @Order(8)
    @DisplayName("Teste GET /usuarios/{email}: busca usuário existente e verifica dados")
    void testeBuscaUsuarioExistenteEndpoint() throws Exception {
        String emailBusca = "teste.usuario@teste.com";
        TestResponse resposta = enviaRequisicao("GET", "/usuarios/" + emailBusca, null);
        assertEquals(200, resposta.status);
        Map<String, Object> usuarioEncontrado = objectMapper.readValue(resposta.body, Map.class);
        assertEquals("Usuario Teste", usuarioEncontrado.get("nome"));
        assertEquals(emailBusca, usuarioEncontrado.get("email"));
        assertEquals(25, usuarioEncontrado.get("idade"));
    }

    @Test
    @Order(9)
    @DisplayName("Teste GET /usuarios/{email}: busca usuário inexistente e verifica status 404")
    void testeBuscaUsuarioInexistenteEndpoint() throws Exception {
        String emailBusca = "naoexiste@teste.com";
        TestResponse resposta = enviaRequisicao("GET", "/usuarios/" + emailBusca, null);
        assertEquals(404, resposta.status);
        Map<String, String> erroResponse = objectMapper.readValue(resposta.body, Map.class);
        assertEquals("Usuário não encontrado com o email: " + emailBusca, erroResponse.get("erro"));
    }
}
