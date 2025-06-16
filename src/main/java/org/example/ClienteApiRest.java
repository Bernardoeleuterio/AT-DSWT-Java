package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ClienteApiRest {

    private static final String BASE_URL = "http://localhost:7000";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        System.out.println("Iniciando o cliente Java para consumo da API REST...");

        try {
            System.out.println("\n--- 1. Criando um novo usuário (POST /usuarios) ---");
            String novoUsuarioJson = "{\"nome\":\"Roberto Carlos\",\"email\":\"roberto.carlos@musica.com\",\"idade\":83}";
            fazerRequisicao("POST", "/usuarios", novoUsuarioJson);

            String segundoUsuarioJson = "{\"nome\":\"Rita Lee\",\"email\":\"rita.lee@musica.com\",\"idade\":75}";
            fazerRequisicao("POST", "/usuarios", segundoUsuarioJson);

            System.out.println("\n--- Tentando criar usuário com email duplicado (POST /usuarios) ---");
            String usuarioDuplicadoJson = "{\"nome\":\"Roberto Carlos 2\",\"email\":\"roberto.carlos@musica.com\",\"idade\":50}";
            fazerRequisicao("POST", "/usuarios", usuarioDuplicadoJson);


            System.out.println("\n--- 2. Listando todos os usuários (GET /usuarios) ---");
            fazerRequisicao("GET", "/usuarios", null);


            System.out.println("\n--- 3. Buscando um usuário por email (GET /usuarios/{email}) ---");
            String emailParaBuscar = "rita.lee@musica.com";
            fazerRequisicao("GET", "/usuarios/" + emailParaBuscar, null);

            System.out.println("\n--- Tentando buscar usuário inexistente (GET /usuarios/{email}) ---");
            String emailInexistente = "naoexiste@musica.com";
            fazerRequisicao("GET", "/usuarios/" + emailInexistente, null);


            System.out.println("\n--- 4. Obtendo o status da API (GET /status) ---");
            fazerRequisicao("GET", "/status", null);


        } catch (Exception e) {
            System.err.println("Ocorreu um erro ao consumir a API: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("\nCliente Java finalizado.");
    }

    private static void fazerRequisicao(String metodo, String path, String body) throws Exception {
        URL url = new URL(BASE_URL + path);
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
    }
}
