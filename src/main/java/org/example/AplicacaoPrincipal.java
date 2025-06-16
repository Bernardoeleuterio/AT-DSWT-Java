package org.example;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AplicacaoPrincipal {

    public static final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create()
                .start(7000);


        app.get("/hello", ctx -> {
            ctx.result("Hello, Javalin!");
        });

        app.get("/status", ctx -> {
            Map<String, String> status = new HashMap<>();
            status.put("status", "ok");
            status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            ctx.json(status);
        });

        app.post("/echo", ctx -> {
            Map<String, Object> corpoRequisicao = ctx.bodyAsClass(Map.class);
            String mensagemRecebida = (String) corpoRequisicao.get("mensagem");
            Map<String, String> resposta = new HashMap<>();
            resposta.put("mensagem", mensagemRecebida);
            ctx.json(resposta);
        });

        app.get("/saudacao/{nome}", ctx -> {
            String nome = ctx.pathParam("nome");
            Map<String, String> saudacao = new HashMap<>();
            saudacao.put("mensagem", "Olá, " + nome + "!");
            ctx.json(saudacao);
        });


        app.post("/usuarios", ctx -> {
            Usuario novoUsuario = ctx.bodyAsClass(Usuario.class);

            if (usuarios.containsKey(novoUsuario.getEmail())) {
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

            usuarios.put(novoUsuario.getEmail(), novoUsuario);
            ctx.status(HttpStatus.CREATED);
            ctx.json(novoUsuario);
            System.out.println("Usuário cadastrado: " + novoUsuario.getEmail());
        });

        app.get("/usuarios", ctx -> {
            ctx.json(new ArrayList<>(usuarios.values()));
        });

        app.get("/usuarios/{email}", ctx -> {
            String emailBusca = ctx.pathParam("email");
            Usuario usuarioEncontrado = usuarios.get(emailBusca);

            if (usuarioEncontrado != null) {
                ctx.json(usuarioEncontrado);
            } else {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(Map.of("erro", "Usuário não encontrado com o email: " + emailBusca));
            }
        });


        System.out.println("--------------------------------------------------");
        System.out.println("Servidor Javalin do AT-DSWT-Java iniciado na porta 7000.");
        System.out.println("  GET  -> http://localhost:7000/hello");
        System.out.println("  GET  -> http://localhost:7000/status");
        System.out.println("  GET  -> http://localhost:7000/saudacao/SeuNome");
        System.out.println("  POST -> http://localhost:7000/echo (JSON: {\"mensagem\": \"Sua Mensagem\"})");
        System.out.println("");
        System.out.println("  --- Endpoints de Usuários ---");
        System.out.println("  POST -> http://localhost:7000/usuarios (Cria um usuário - Body JSON: {\"nome\":\"Bernardo\",\"email\":\"bernardo@gmail.com\",\"idade\":30})");
        System.out.println("  GET  -> http://localhost:7000/usuarios (Lista todos os usuários)");
        System.out.println("  GET  -> http://localhost:7000/usuarios/{email} (Busca por e-mail, ex: /usuarios/bernardo@gmail.com)");
        System.out.println("--------------------------------------------------");
    }
}