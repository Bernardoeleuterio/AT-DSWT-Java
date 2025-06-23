package org.example;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AplicacaoPrincipal {

    public static final Map<UUID, Tarefa> tarefas = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create()
                .start(7000);


        app.get("/hello", ctx -> {
            ctx.result("Hello, Javalin!");
        });

        app.get("/status", ctx -> {
            Map<String, String> status = Map.of(
                    "status", "ok",
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            );
            ctx.json(status);
        });

        app.post("/echo", ctx -> {
            Map<String, Object> corpoRequisicao = ctx.bodyAsClass(Map.class);
            String mensagemRecebida = (String) corpoRequisicao.get("mensagem");
            Map<String, String> resposta = Map.of("mensagem", mensagemRecebida);
            ctx.json(resposta);
        });

        app.get("/saudacao/{nome}", ctx -> {
            String nome = ctx.pathParam("nome");
            Map<String, String> saudacao = Map.of("mensagem", "Olá, " + nome + "!");
            ctx.json(saudacao);
        });


        app.post("/tarefas", ctx -> {
            Tarefa novaTarefa = ctx.bodyAsClass(Tarefa.class);

            if (novaTarefa.getTitulo() == null || novaTarefa.getTitulo().trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.json(Map.of("erro", "O título da tarefa é obrigatório."));
                return;
            }

            tarefas.put(novaTarefa.getId(), novaTarefa);
            ctx.status(HttpStatus.CREATED);
            ctx.json(novaTarefa);
            System.out.println("Tarefa cadastrada: " + novaTarefa.getTitulo() + " (ID: " + novaTarefa.getId() + ")");
        });

        app.get("/tarefas", ctx -> {
            ctx.json(new ArrayList<>(tarefas.values()));
        });

        app.get("/tarefas/{id}", ctx -> {
            String idBusca = ctx.pathParam("id");
            Tarefa tarefaEncontrada = null;
            try {
                UUID uuidIdBusca = UUID.fromString(idBusca);
                tarefaEncontrada = tarefas.get(uuidIdBusca);
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

        System.out.println("--------------------------------------------------");
        System.out.println("Servidor Javalin do AT-DSWT-Java iniciado na porta 7000.");
        System.out.println("Endpoints para teste:");
        System.out.println("  GET  -> http://localhost:7000/hello");
        System.out.println("  GET  -> http://localhost:7000/status");
        System.out.println("  GET  -> http://localhost:7000/saudacao/SeuNome");
        System.out.println("  POST -> http://localhost:7000/echo (JSON: {\"mensagem\": \"Sua Mensagem\"})");
        System.out.println("");
        System.out.println("  --- Endpoints de Tarefas ---");
        System.out.println("  POST -> http://localhost:7000/tarefas (Cria uma tarefa - Body JSON: {\"titulo\":\"Comprar Leite\",\"descricao\":\"No mercado da esquina\"})");
        System.out.println("  GET  -> http://localhost:7000/tarefas (Lista todas as tarefas)");
        System.out.println("  GET  -> http://localhost:7000/tarefas/{id} (Busca por ID da tarefa, ex: /tarefas/a1b2c3d4-e5f6-7890-1234-567890abcdef)");
        System.out.println("--------------------------------------------------");
    }
}
