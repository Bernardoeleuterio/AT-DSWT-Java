Projeto AT-DSWT-Java
API REST em Java (Javalin) para gerenciamento de Tarefas. Inclui testes JUnit e cliente Java para consumo (HttpURLConnection).

Estrutura
src/main/java/org/example/: Código da API (AplicacaoPrincipal.java, Tarefa.java, ClienteApiRest.java).

src/test/java/org/example/: Testes unitários (AplicacaoPrincipalTest.java).

build.gradle: Arquivo de configuração do Gradle.

Como Executar
1. Build do Projeto
   Para compilar e empacotar o projeto, execute o seguinte comando no terminal, na pasta raiz do projeto:

./gradlew clean build

Para rodar os testes durante o build, você pode usar: ./gradlew clean test

2. Iniciar a Aplicação (Servidor REST)
   Abra a classe AplicacaoPrincipal.java.

Execute o AplicacaoPrincipal.java

O servidor iniciará na porta 7000. Mantenha o console aberto.

3. Rodar os Testes Unitários (JUnit)
   PARE a AplicacaoPrincipal (servidor).

Abra AplicacaoPrincipalTest.java.

Execute AplicacaoPrincipalTest.

Verifique se todos os testes passaram (✅ verde no "Test Results").

4. Executar o Cliente REST
   Inicie a AplicacaoPrincipal (servidor) novamente.

Abra ClienteApiRest.java.

Execute ClienteApiRest.

Observe a saída no console com os resultados das requisições.

Endpoints da API
Base URL: http://localhost:7000

Genéricos
GET /hello: Retorna "Hello, Javalin!"

GET /status: Retorna {"timestamp":"" ,"status":"ok"}

POST /echo:

Requisição: {"mensagem": "Sua mensagem aqui"}

Retorna: O mesmo JSON enviado.

GET /saudacao/{nome}:

Exemplo: GET http://localhost:7000/saudacao/Bernardo

Retorna: {"mensagem":"Olá, Bernardo!"}

Tarefas
POST /tarefas (Cria uma nova tarefa)

Requisição:

{
"titulo": "Organizar documentos",
"descricao": "Separar contas pagas e arquivar",
"concluida": false
}

Observação: O campo "concluida" (tipo boolean) pode ser enviado na requisição (true ou false). Se omitido, a tarefa será criada como não concluída (false).

Retorna: 201 Created e o JSON da tarefa criada (incluindo o id gerado e concluida).

Cenários de Erro:

400 Bad Request se o título for vazio.

GET /tarefas (Lista todas as tarefas)

Retorna: 200 OK e um array JSON com todas as tarefas.

GET /tarefas/{id} (Busca uma tarefa por ID)

Exemplo: GET http://localhost:7000/tarefas/a1b2c3d4-e5f6-... (substitua pelo ID real)

Retorna: 200 OK e o JSON da tarefa encontrada.

Cenários de Erro:

404 Not Found se não existir.

400 Bad Request se o ID for inválido.