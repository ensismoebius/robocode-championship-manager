# Apêndice B — Problemas comuns e como resolvê-los

Este apêndice reúne os problemas que mais atrapalham quem segue o tutorial. Cada um aparece com a **mensagem que o computador mostra**, para você achar o problema pelo texto, a **causa**, e o que fazer. As mensagens foram provocadas de propósito, uma a uma, durante a revisão do tutorial, num computador com Linux. As poucas que não foram reproduzidas, por dependerem de outro sistema ou de outra versão do Java, estão marcadas no texto. Algumas mensagens aparecem em português e outras em inglês, conforme o programa que as escreve e o idioma do seu sistema, e por isso a entrada traz as duas formas quando elas existem.

Antes de procurar aqui, vale repetir o que o capítulo 0 diz: **leia a primeira mensagem de erro**, a que está mais acima, e olhe o arquivo e a linha que ela cita. As mensagens seguintes costumam ser consequência da primeira.

## Parte 1 — As ferramentas

### O terminal não encontra o `mvn`, o `java` ou o `javac`

```text
bash: linha 1: mvn: comando não encontrado
```

No Windows, a mensagem diz que o comando não é reconhecido como um comando interno ou externo (essa não foi reproduzida, pois o teste foi no Linux).

**Causa.** O programa não está instalado, ou está, mas a pasta `bin` dele não está no `PATH`, a lista de pastas em que o terminal procura os comandos. Outra causa comum é instalar o programa com o terminal já aberto: o terminal só lê o `PATH` quando abre.

**O que fazer.** Feche o terminal e abra outro. Se a mensagem continuar, instale a ferramenta de novo, seguindo o capítulo 0. Quando o `java -version` responde e o `javac -version` não, você instalou só o JRE, e falta o JDK.

### O Maven usa um Java que não é o 21

O `mvn -v` mostra, na linha `Java version`, qual Java o Maven encontrou:

```text
Apache Maven 3.10.0 (c43a36b8d67be7e0805a411bc0898af1a51f5472)
Maven home: /usr/share/java/maven
Java version: 1.8.0_504, vendor: Arch Linux, runtime: /usr/lib/jvm/java-8-openjdk/jre
```

Com um Java mais antigo, a compilação falha. Com um JDK entre o 9 e o 20, o compilador diz `release version 21 not supported`, a mensagem que o capítulo 1 cita (ela não foi reproduzida, porque a máquina de teste só tinha os JDKs 8 e 21). Com o Java 8, o resultado é uma enxurrada de erros de sintaxe em arquivos que estão certos:

```text
[ERROR] COMPILATION ERROR :
[ERROR] src/main/java/campeonato/FluxoDoCampeonato.java:[127,42] : expected
[ERROR] src/main/java/campeonato/FluxoDoCampeonato.java:[132,10] : expected
```

**Causa.** O projeto pede Java 21 (`maven.compiler.release` no `pom.xml`), e o Maven está usando outro JDK: o que a variável `JAVA_HOME` indica ou, se ela não existe, o `java` que vem primeiro no `PATH`. Isso acontece quando há mais de um JDK instalado.

**O que fazer.** Aponte o Maven para o JDK 21 com a variável `JAVA_HOME`, que guarda a pasta do JDK. No Linux e no macOS:

```bash
export JAVA_HOME=/caminho/do/jdk-21
```

No Prompt de Comando do Windows:

```text
set JAVA_HOME=C:\caminho\do\jdk-21
```

Rode o `mvn -v` de novo: a linha `Java version` deve começar com `21`. Como acontece com a `ROBOCODE` (veja a Parte 3), a variável vale só para o terminal onde você a definiu.

### O Maven não acha o projeto

```text
[ERROR] The goal you specified requires a project to execute but there is no POM in this directory (...). Please verify you invoked Maven from the correct directory.
```

Com o `mvn javafx:run`, a mesma causa produz outra mensagem:

```text
[ERROR] No plugin found for prefix 'javafx' in the current project and in the plugin groups [org.apache.maven.plugins, org.codehaus.mojo] ...
```

**Causa.** O terminal não está na pasta `mini-campeonato`. O Maven lê o `pom.xml` da pasta em que é executado, e, sem ele, não sabe nem que existe o plugin do JavaFX.

**O que fazer.** Entre na pasta do projeto (`cd mini-campeonato`) e confirme com `ls` (ou `dir`, no Windows) que o `pom.xml` aparece na lista.

### O Maven não consegue baixar as bibliotecas

```text
[ERROR] Plugin org.apache.maven.plugins:maven-resources-plugin:3.5.0 or one of its dependencies could not be resolved:
[ERROR] The following artifacts could not be resolved: ...: Could not transfer artifact ... from/to central (https://repo.maven.apache.org/maven2): transfer failed for ...
```

**Causa.** Na primeira vez que roda, o Maven baixa da internet o JavaFX, o JUnit e os plugins que o projeto usa, e guarda tudo no repositório local, a pasta `.m2/repository`, dentro da sua pasta pessoal. Sem conexão (ou atrás de um proxy que não foi configurado), o download falha.

**O que fazer.** Confira a conexão e rode o comando de novo. Depois que tudo foi baixado uma vez, o Maven funciona sem rede se você acrescentar `-o` (de *offline*) ao comando: `mvn -o test`.

## Parte 2 — Compilação e execução

### `cannot find symbol`

```text
[ERROR] src/main/java/campeonato/tela/VisaoDaChave.java:[26,13] cannot find symbol
  symbol:   class Chave
  location: class campeonato.tela.VisaoDaChave
```

A mensagem diz o arquivo, a linha e a coluna entre colchetes (`[26,13]`), o nome que o compilador não conhece (`symbol`) e onde ele o procurou (`location`).

**Causa.** Em quase todos os casos é uma entre três: falta o `import` da classe (no exemplo, o `import campeonato.torneio.Chave;` foi apagado de propósito); o nome foi escrito com uma letra diferente, e o Java distingue maiúsculas de minúsculas (`chave` e `Chave` são nomes diferentes); ou a classe ainda não existe, porque o arquivo que a cria está num passo que você não fez.

**O que fazer.** Vá à linha indicada e compare com o texto do tutorial, letra por letra. Se o nome é de uma classe do tutorial, confira se o arquivo dela foi criado e se o `import` está no topo. O editor costuma oferecer o `import` quando o nome fica sublinhado.

### `';' expected`

```text
[ERROR] src/main/java/campeonato/torneio/Lado.java:[1,27] ';' expected
```

**Causa.** Falta um ponto e vírgula, e o compilador só percebe quando chega ao que vem depois. Por isso a linha apontada é a do ponto onde o `;` deveria estar, **ou a linha logo depois**. Aqui, o `;` da linha `package campeonato.torneio;` foi apagado, e a coluna 27 é logo depois do último caractere dela.

**O que fazer.** Olhe o fim da linha indicada e o fim da anterior. O mesmo vale para chaves e parênteses que ficaram abertos: a mensagem aponta onde o compilador **desistiu de entender**, que pode ser depois do erro de verdade.

### `Erro: os componentes de runtime do JavaFX não foram encontrados`

```text
Error: JavaFX runtime components are missing, and are required to run this application
```

Em português, no Java 21: `Erro: os componentes de runtime do JavaFX não foram encontrados. Eles são obrigatórios para executar este aplicativo`.

**Causa.** Você abriu o programa de um jeito que não é o do tutorial, por exemplo com o botão de executar do editor, ou com `java -cp ... campeonato.Principal`. A classe `Principal` herda de `Application`, e o Java só a inicia se o JavaFX estiver no lugar que ele espera (o *module path*). O `mvn javafx:run` monta isso sozinho, com as bibliotecas do `pom.xml`.

**O que fazer.** Abra o programa com `mvn javafx:run`, como no capítulo 1, de dentro da pasta do projeto. Para abrir outra classe, use `mvn javafx:run -Dclasse.principal=campeonato.tela.DemonstracaoDaChave`, por exemplo.

## Parte 3 — O Robocode e as batalhas

### A janela pede a variável `ROBOCODE`

```text
Defina a variável ROBOCODE com a pasta do Robocode (capítulo 4, passo 2).
```

**Causa.** O `Principal` lê a variável com `System.getenv("ROBOCODE")`, e ela não existe **no terminal de onde o programa foi aberto**. Uma variável definida num terminal vale só para ele e para os programas que ele abre: um terminal novo, ou o botão de executar do editor, não a enxergam.

**O que fazer.** No mesmo terminal em que você roda o `mvn javafx:run`, defina a variável de novo, como no capítulo 4: `export ROBOCODE="$HOME/robocode"` no Linux e no macOS, `set ROBOCODE=C:\robocode` no Prompt do Windows. Confira com `echo $ROBOCODE` (`echo %ROBOCODE%` no Windows).

### `A batalha não retornou o resultado esperado`

Esta é a mensagem geral para qualquer batalha que não deu certo, e **o que importa são as linhas que vêm depois dela**: são a saída do processo do Robocode, copiada tal e qual. Na tela do programa, a mensagem aparece na arena, com `Não foi possível rodar a batalha:` na frente e `ESC: voltar para a chave` no fim. No terminal, ela sai da `IOException` do `ControleDeBatalha`. Estas são as quatro mais comuns, cada uma provocada de propósito:

| A linha que vem depois | O que significa | O que fazer |
| --- | --- | --- |
| ``Error: could not open `iniciador/opcoes.txt'`` | O processo não achou o arquivo de opções, porque o programa não foi aberto na pasta `mini-campeonato`: o caminho `iniciador/opcoes.txt` é relativo à pasta onde o programa roda. | Abra o terminal na pasta do projeto (veja a Parte 1). |
| `Error: Could not find or load main class IniciadorDeBatalha` (em português, `Erro: Não foi possível localizar nem carregar a classe principal IniciadorDeBatalha`) | O `IniciadorDeBatalha.class` não existe: o iniciador nunca foi compilado, ou você apagou o `.class`. | Compile o iniciador de novo, com o `javac` do passo 4 do capítulo 4. |
| `Error: Unable to initialize main class IniciadorDeBatalha`, seguida de `Caused by: java.lang.NoClassDefFoundError: robocode/control/events/IBattleListener` (em português, `Erro: Não é possível inicializar a classe principal IniciadorDeBatalha`) | O iniciador existe, mas não achou as bibliotecas do Robocode: a variável `ROBOCODE` aponta para uma pasta que não é a do Robocode. | A pasta certa é a que tem `libs/robocode.jar` (capítulo 0). Corrija a variável. |
| `Can't find 'aluno.Vigia'` | O Robocode não achou o robô. | Veja o próximo problema. |

As mensagens que começam com `Error:` ou `Erro:` vêm do próprio `java` e dependem do idioma do sistema. Se aparecer outra mensagem, ela é a pista: procure-a na internet com as aspas, e confira se o `ControleDeBatalha` está igual ao do capítulo 5.

### `Can't find 'aluno.Vigia'`

```text
Can't find 'aluno.Vigia'
```

**Causa.** Há duas possíveis. Na primeira, o robô não foi compilado para dentro da pasta do Robocode: o Robocode só enxerga os robôs que estão na pasta `robots` dele, e o capítulo 4 compila o `Vigia` para lá (`javac ... -d "$ROBOCODE/robots"`) e copia o `.properties` junto. Na segunda, o nome está sem o asterisco. O `Vigia` é um robô "solto" na pasta `robots`, e o Robocode marca os robôs assim com um `*` depois do nome: o nome certo é `aluno.Vigia*`, escrito entre aspas no terminal.

**O que fazer.** Confira se existe o arquivo `robots/aluno/Vigia.class` dentro da pasta do Robocode, e se o nome da lista de robôs do programa tem o asterisco (`ListaDeRobos.PADRAO` tem). Se o arquivo não existe, repita o passo 3 do capítulo 4.

## Parte 4 — Os testes

### `Toolkit not initialized`

```text
java.lang.IllegalStateException: Toolkit not initialized
```

**Causa.** O teste criou um componente do JavaFX, como uma `Label`. Os componentes só podem ser criados depois que o **toolkit**, o motor gráfico do JavaFX, está de pé, o que acontece quando uma janela abre, e um teste comum não abre janela.

**O que fazer.** Não teste o componente: teste a conta. É a razão de existirem a `Aparencia` (capítulo 9) e a `Disposicao` (capítulo 14), classes sem nenhum componente do JavaFX, e é por isso que os testes do tutorial passam sem abrir janela. Se um teste seu precisa de um componente, a conta que ele quer conferir provavelmente deve morar numa classe à parte.

## Parte 5 — O campeonato

### Troquei a lista de robôs e a chave não mudou

**Causa.** O campeonato guarda as **sementes**, a lista de robôs já na ordem sorteada, no arquivo `campeonato.txt` (capítulo 15). Ao abrir, o programa lê o arquivo e continua o campeonato dele: a lista do `ListaDeRobos.PADRAO` só vale quando o arquivo não existe. No terminal, a mensagem confirma qual caminho foi seguido:

```text
Campeonato retomado de campeonato.txt: 3 partidas já jogadas.
```

contra

```text
Campeonato novo, com as sementes [sample.Walls, aluno.Vigia*, ...]
```

**O que fazer.** Apague o `campeonato.txt` (ele fica na pasta de onde o programa foi aberto) e abra o programa de novo. Quando um campeonato termina, a tecla N sorteia outro com os mesmos robôs, mas a lista só muda mesmo apagando o arquivo.

### `Não consegui ler campeonato.txt`

```text
Não consegui ler campeonato.txt (Linha que não se entende: lixo): começando um campeonato novo.
```

**Causa.** O arquivo existe, mas não é o de um campeonato válido, porque foi editado à mão, está vazio ou veio de uma versão diferente do programa. O `Campeonato.carregar` recusa o arquivo com uma `IOException`, e o `Principal` avisa e começa um campeonato novo. Estas são as mensagens que o `carregar` produz, cada uma provocada com um arquivo ruim de propósito:

| O arquivo tem | A mensagem |
| --- | --- |
| uma linha que não é `nome=valor`, como `lixo` | `Linha que não se entende: lixo` |
| menos de dois robôs, ou nenhum (arquivo vazio) | `O arquivo não combina com a chave: São necessários ao menos dois participantes` |
| um vencedor que não joga a partida | `O arquivo não combina com a chave: O vencedor não joga a partida V1-1` |
| uma partida jogada antes de poder ser jogada | `O arquivo não combina com a chave: A partida V2-1 não pode ser jogada agora (aguardando)` |
| pontos que não são números | `Pontos que não são números: V1-1;a;x;0` |

**O que fazer.** Se o arquivo importa, abra-o num editor de texto e corrija a linha: a ordem das linhas `resultado` importa (a `V2-1` não pode vir antes das partidas da primeira rodada). Se não importa, apague-o. **Atenção:** o programa não mexe no arquivo ruim ao abrir, mas o **primeiro resultado** guardado depois o sobrescreve com o campeonato novo. Guarde uma cópia antes, se for o caso.

### O ENTER ou as outras teclas não fazem nada

A tecla depende de **onde o programa está** (capítulo 15):

- Na **chave**, o ENTER joga a partida escolhida. Se o campeonato já terminou, não há partida a jogar: use N para sortear outro ou U para desfazer o último resultado. O N só funciona depois do fim.
- Em **"Preparando a batalha"**, **todas as teclas são ignoradas** de propósito: a batalha roda em segundo plano e vai terminar sozinha. Se a preparação parece demorar, olhe o terminal: uma batalha que falha mostra a mensagem do problema na tela (veja a Parte 3).
- No **replay**, só o ESC faz algo: ele volta para a chave.

Se nenhuma tecla funciona, confira se a janela do programa está em primeiro plano (clique no fundo dela): uma janela sem o foco não recebe as teclas.

### Fechei a janela no meio de uma partida

O resultado é guardado **assim que a batalha termina**, antes do replay (capítulo 15). Fechar a janela durante o replay não perde nada: ao abrir de novo, a partida aparece como jogada. Fechar durante a **preparação** perde a batalha, que ainda não tinha resultado, e a partida volta a esperar a vez.
