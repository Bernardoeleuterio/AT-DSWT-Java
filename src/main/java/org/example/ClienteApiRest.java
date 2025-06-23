package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class ClienteApiRest {

    private static final String BASE_URL = "http://localhost:7000";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        System.out.println("Iniciando o cliente Java para consumo da API REST...");

        try {
            System.out.println("\n--- 1. Criando uma nova tarefa (POST /tarefas) ---");
            String novaTarefaJson = "{\"titulo\":\"Estudar para a prova\",\"descricao\":\"Revisar Javalin e JUnit\"}";
            String idTarefaCriada = extraiIdDaTarefa(fazerRequisicao("POST", "/tarefas", novaTarefaJson).body);

            String segundaTarefaJson = "{\"titulo\":\"Fazer compras\",\"descricao\":\"Leite, pão e ovos\"}";
            fazerRequisicao("POST", "/tarefas", segundaTarefaJson);

            System.out.println("\n--- Criando uma tarefa JÁ CONCLUÍDA (POST /tarefas com 'concluida': true) ---");
            String tarefaConcluidaJson = "{\"titulo\":\"Pagar contas\",\"descricao\":\"Luz, água e internet\",\"concluida\":true}";
            fazerRequisicao("POST", "/tarefas", tarefaConcluidaJson);


            System.out.println("\n--- Tentando criar tarefa sem título (POST /tarefas) ---");
            String tarefaSemTituloJson = "{\"titulo\":\"\",\"descricao\":\"Esta não deveria passar\"}";
            fazerRequisicao("POST", "/tarefas", tarefaSemTituloJson);


            System.out.println("\n--- 2. Listando todas as tarefas (GET /tarefas) ---");
            fazerRequisicao("GET", "/tarefas", null);


            System.out.println("\n--- 3. Buscando uma tarefa por ID (GET /tarefas/{id}) ---");
            if (idTarefaCriada != null) {
                fazerRequisicao("GET", "/tarefas/" + idTarefaCriada, null);
            } else {
                System.out.println("  ID da tarefa não foi obtido, impossível testar busca por ID.");
            }

            System.out.println("\n--- Tentando buscar tarefa inexistente (GET /tarefas/{id}) ---");
            String idInexistente = UUID.randomUUID().toString();
            fazerRequisicao("GET", "/tarefas/" + idInexistente, null);

            System.out.println("\n--- Tentando buscar tarefa com ID inválido (GET /tarefas/{id}) ---");
            String idInvalido = "ID_INVALIDO_AQUI";
            fazerRequisicao("GET", "/tarefas/" + idInvalido, null);


            System.out.println("\n--- 4. Obtendo o status da API (GET /status) ---");
            fazerRequisicao("GET", "/status", null);

            System.out.println("\n--- Testando /hello (GET) ---");
            fazerRequisicao("GET", "/hello", null);

            System.out.println("\n--- Testando /saudacao/Mundo (GET) ---");
            fazerRequisicao("GET", "/saudacao/Mundo", null);

            System.out.println("\n--- Testando /echo (POST) ---");
            fazerRequisicao("POST", "/echo", "{\"mensagem\":\"Olá do cliente Javalin!\"}");


        } catch (Exception e) {
            System.err.println("Ocorreu um erro ao consumir a API: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("\nCliente Java finalizado.");
    }

    private static TestResponse fazerRequisicao(String metodo, String path, String body) throws Exception {
        URL url = new URI(BASE_URL + path).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(metodo);

        if (metodo.equals("POST") || metodo.equals("PUT")) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            if (body != null && !body.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
        }

        int statusCode = connection.getResponseCode();
        System.out.println("  URL: " + url);
        System.out.println("  Método: " + metodo);
        if (body != null) {
            System.out.println("  Corpo enviado: " + body);
        }
        System.out.println("  Status HTTP: " + statusCode + " " + connection.getResponseMessage());

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                statusCode >= 300 ? connection.getErrorStream() : connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        String responseBody = response.toString();
        if (!responseBody.isEmpty() && (responseBody.startsWith("{") || responseBody.startsWith("["))) {
            try {
                Object json = objectMapper.readValue(responseBody, Object.class);
                System.out.println("  Resposta JSON formatada:\n" + objectMapper.writeValueAsString(json));
            } catch (Exception e) {
                System.out.println("  Resposta (não JSON ou com erro de parse):\n" + responseBody);
            }
        } else {
            System.out.println("  Resposta:\n" + responseBody);
        }
        connection.disconnect();
        return new TestResponse(statusCode, responseBody);
    }

    private static class TestResponse {
        public final int status;
        public final String body;

        public TestResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private static String extraiIdDaTarefa(String jsonBody) {
        try {
            Map<String, Object> tarefa = objectMapper.readValue(jsonBody, Map.class);
            if (tarefa != null && tarefa.containsKey("id")) {
                return tarefa.get("id").toString();
            }
        } catch (Exception e) {
            System.err.println("Erro ao extrair ID da tarefa do JSON: " + e.getMessage());
        }
        return null;
    }
}
