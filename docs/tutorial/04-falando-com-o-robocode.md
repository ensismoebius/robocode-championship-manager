# Capítulo 4 — Falando com o Robocode

## Objetivo

Fazer o Robocode rodar uma batalha **escondido**, sem janela, a partir de um programa Java nosso, e escrever em texto tudo o que acontece nela, turno a turno. Esse texto é a "gravação" da batalha. No capítulo 6 vamos transformá-la em 3D.

## Passo 1 — Dois programas conversando

O Robocode não é só uma janela. Por baixo dela existe um **motor** que simula a batalha: move os tanques, dispara os tiros, calcula os danos. A janela apenas desenha o que o motor calcula. O Robocode traz uma biblioteca, a **API de controle** (pacote `robocode.control`), feita para que outro programa use o motor sem a janela: escolher os robôs, rodar a batalha e receber os resultados e os "retratos" (*snapshots*) de cada turno, com a posição, a direção e a energia de cada robô e de cada tiro.

Vamos escrever um pequeno programa, o **iniciador**, que usa essa API. O programa principal (o JavaFX) não vai chamar o motor diretamente: ele vai **iniciar o iniciador como um programa à parte** e ler o que ele escrever na saída:

```text
┌─────────────────────┐   inicia    ┌──────────────────────────┐
│  Mini Campeonato    │ ──────────→ │  IniciadorDeBatalha      │
│  (JavaFX, Maven)    │             │  (usa o motor Robocode)  │
│                     │ ←────────── │                          │
└─────────────────────┘ linhas de   └──────────────────────────┘
                        texto
```

Por que separar em dois programas, em vez de chamar o motor de dentro do JavaFX?

- O motor do Robocode precisa de opções especiais para rodar no Java 21 e de bastante memória. Isolado, ele recebe só as opções de que precisa e, se faltar memória ou algo quebrar, o programa principal continua de pé.
- O projeto Maven não precisa conhecer os arquivos do Robocode na hora de compilar.
- O jeito de conversar é o mais simples que existe: **texto**. Qualquer programa lê texto, e podemos até guardar a saída num arquivo para reproduzir a batalha depois.

## Passo 2 — Guardar o caminho do Robocode

Os comandos deste capítulo precisam do caminho da pasta onde o Robocode foi instalado, aquela que você anotou no capítulo 0. Para não repeti-lo em todo comando, guarde-o numa **variável do terminal**. No Linux e no macOS (ajuste o caminho se o seu for outro):

```bash
export ROBOCODE="$HOME/robocode"
```

No Prompt de Comando (`cmd`) do Windows:

```text
set ROBOCODE=C:\robocode
```

A variável vale só para o terminal onde você a definiu: ao abrir um terminal novo, defina-a de novo. Nos comandos, o Linux e o macOS leem a variável como `$ROBOCODE`, e o Windows como `%ROBOCODE%`.

Todos os comandos deste capítulo são digitados num terminal aberto na pasta `mini-campeonato`.

## Passo 3 — Seu primeiro robô

Um robô do Robocode é uma classe Java que herda de `Robot`. O Robocode chama o método `run()` e o robô fica num laço. Quando algo acontece, o Robocode chama o método do evento correspondente: `onScannedRobot` quando o radar vê um inimigo, `onHitByBullet` quando o robô leva um tiro.

Os robôs do campeonato ficam numa pasta própria do projeto, `robos`, separada do código do programa. **Crie o arquivo** `robos/aluno/Vigia.java`:

<!-- arquivo: robos/aluno/Vigia.java | criar -->
```java
package aluno;

import java.awt.Color;
import robocode.HitByBulletEvent;
import robocode.Robot;
import robocode.ScannedRobotEvent;

/** Um robô simples: anda, gira o canhão olhando em volta e atira em quem encontrar. */
public class Vigia extends Robot {

    @Override
    public void run() {
        setColors(Color.ORANGE, Color.BLACK, Color.RED);
        while (true) {
            ahead(150);
            turnGunRight(360);
            back(100);
            turnGunRight(360);
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent evento) {
        fire(2);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent evento) {
        turnLeft(90 - evento.getBearing());
    }
}
```

O que cada método de ação faz (todos vêm da classe `Robot`; as distâncias são em unidades da arena e os ângulos, em graus):

| Método | Efeito |
|---|---|
| `setColors(corpo, canhão, radar)` | Pinta as três partes do robô. |
| `ahead(d)` e `back(d)` | Anda `d` para a frente ou para trás. |
| `turnGunRight(graus)` | Gira o canhão (e o radar, que vai junto) para a direita. |
| `turnLeft(graus)` | Gira o corpo para a esquerda. |
| `fire(potência)` | Atira. A potência vai de 0,1 a 3: tiro forte machuca mais, mas gasta mais energia. |

O `@Override` avisa que o método substitui um da classe `Robot`; se você errar o nome, o compilador reclama. No `onHitByBullet`, `evento.getBearing()` é o ângulo, em graus, entre a frente do nosso robô e a direção de onde veio o tiro. Girar `90 - ângulo` coloca o robô de lado para o atirador, que é um alvo mais difícil de acertar.

O Robocode precisa de dois arquivos para reconhecer um robô: o compilado (`.class`) e, ao lado dele, um arquivo `.properties` que diz o nome e a classe do robô. Os robôs que vêm com o Robocode têm um (veja `robots/sample/Walls.properties`). O do `Vigia` vamos escrever à mão. **Crie o arquivo** `robos/aluno/Vigia.properties`:

<!-- arquivo: robos/aluno/Vigia.properties | criar -->
```text
robot.classname=aluno.Vigia
robot.name=Vigia
robot.author.name=Seu nome
robot.description=Anda, gira o canhao e atira em quem encontra
robocode.version=1.11.1
```

Agora compile o robô. O `javac` precisa do arquivo `robocode.jar`, que traz as classes `Robot`, `ScannedRobotEvent` e as outras da API. A opção `-d` diz **onde** gravar o resultado: direto na pasta `robots` do Robocode. No Linux e no macOS:

<!-- executar: | espera: Vigia.class -->
```bash
javac -cp "$ROBOCODE/libs/robocode.jar" -d "$ROBOCODE/robots" robos/aluno/Vigia.java
cp robos/aluno/Vigia.properties "$ROBOCODE/robots/aluno/"
ls "$ROBOCODE/robots/aluno"
```

No Windows:

```text
javac -cp "%ROBOCODE%\libs\robocode.jar" -d "%ROBOCODE%\robots" robos\aluno\Vigia.java
copy robos\aluno\Vigia.properties "%ROBOCODE%\robots\aluno\"
```

Se o Robocode estiver aberto, feche e abra de novo para ele reconhecer o robô novo. Na pasta `javadoc` do Robocode fica a documentação de todos os métodos da API de robôs: abra `javadoc/index.html` no navegador quando quiser saber o que um deles faz.

## Passo 4 — O iniciador, versão 1: só o resultado

O iniciador é um programa Java comum, **fora** do projeto Maven. Ele fica na pasta `iniciador`, sem pacote, e é compilado à parte, com as bibliotecas do Robocode. **Crie o arquivo** `iniciador/IniciadorDeBatalha.java`:

<!-- arquivo: iniciador/IniciadorDeBatalha.java | criar -->
```java
import java.io.File;
import java.util.Arrays;

import robocode.BattleResults;
import robocode.control.BattleSpecification;
import robocode.control.BattlefieldSpecification;
import robocode.control.RobocodeEngine;
import robocode.control.RobotSpecification;
import robocode.control.events.BattleAdaptor;
import robocode.control.events.BattleCompletedEvent;
import robocode.control.events.BattleErrorEvent;

/** Roda uma batalha do Robocode sem janela e escreve o resultado. */
public class IniciadorDeBatalha {

    public static void main(String[] args) {
        String pastaDoRobocode = args[0];
        int rodadas = Integer.parseInt(args[1]);
        String robos = String.join(",", Arrays.copyOfRange(args, 2, args.length));

        RobocodeEngine.setLogMessagesEnabled(false);
        RobocodeEngine motor = new RobocodeEngine(new File(pastaDoRobocode));
        motor.addBattleListener(new Observador());
        motor.setVisible(false);

        RobotSpecification[] escolhidos = motor.getLocalRepository(robos);
        motor.runBattle(new BattleSpecification(rodadas, new BattlefieldSpecification(800, 600), escolhidos), true);
        motor.close();
        System.exit(0);
    }

    static class Observador extends BattleAdaptor {

        @Override
        public void onBattleCompleted(BattleCompletedEvent evento) {
            for (BattleResults resultado : evento.getSortedResults()) {
                System.out.println(resultado.getRank() + "\t" + resultado.getTeamLeaderName() + "\t" + resultado.getScore() + "\t" + resultado.getFirsts());
            }
        }

        @Override
        public void onBattleError(BattleErrorEvent evento) {
            System.out.println("Err> " + evento.getError());
        }
    }
}
```

Por partes:

- Os argumentos do programa vêm no vetor `args`: o caminho do Robocode, quantas **rodadas** a batalha tem e, depois, os nomes dos robôs. Cada rodada termina quando só resta um robô vivo, e a batalha é a soma de todas as rodadas.
- `RobocodeEngine` é o **motor**. `setVisible(false)` diz que não queremos a janela. `setLogMessagesEnabled(false)` cala as mensagens do próprio Robocode, para que a saída do programa só tenha o que nós escrevermos.
- `getLocalRepository(robos)` consulta o repositório de robôs instalados e devolve a descrição (`RobotSpecification`) dos que pedimos. Os nomes vêm numa só lista separada por vírgulas, e é o `String.join(",", ...)` que a monta.
- A `BattleSpecification` junta tudo: as rodadas, o campo (`BattlefieldSpecification(800, 600)`, o tamanho padrão do Robocode) e os robôs. `runBattle(..., true)` roda a batalha, e o `true` manda **esperar** até ela acabar.
- `BattleAdaptor` é uma classe com métodos vazios, um para cada coisa que pode acontecer na batalha. A nossa classe `Observador` **herda** dela e sobrescreve só os métodos de que precisamos; o motor os chama quando os eventos ocorrem. Esse jeito de "ficar de olho" em algo é chamado de **observador**, e você vai vê-lo de novo no JavaFX.
- Quando a batalha termina, `onBattleCompleted` recebe os resultados ordenados. Escrevemos uma linha por robô, com quatro campos separados por tabulação (`\t`): a colocação (*rank*), o nome, a pontuação total e quantas rodadas o robô ganhou.
- O `System.exit(0)` no fim encerra o programa de qualquer maneira. Nos nossos testes ele terminava sozinho sem essa linha, mas o motor do Robocode cria linhas de execução (*threads*) próprias, e uma que sobrasse viva manteria o programa preso, esperando para sempre.

Compile o iniciador. A opção `-cp` (*classpath*) diz onde o Java procura classes. O `*` quer dizer "todos os `.jar` desta pasta". No Linux e no macOS:

<!-- executar: | espera: ok -->
```bash
javac -cp "$ROBOCODE/libs/*" -d iniciador iniciador/IniciadorDeBatalha.java && echo ok
```

No Windows:

```text
javac -cp "%ROBOCODE%\libs\*" -d iniciador iniciador\IniciadorDeBatalha.java
```

## Passo 5 — Rodando uma batalha

Ao rodar o iniciador, o Java precisa de algumas **opções da máquina virtual**, que vêm antes do nome da classe:

- `-Xmx2048M` permite ao programa usar até 2 GB de memória. Batalhas longas guardam muita coisa.
- Cada `--add-opens` libera ao Robocode o acesso a uma parte interna do Java, que a partir do Java 9 é protegida. São as mesmas quatro que o script de abertura do Robocode usa; para conferir, abra o `robocode.sh` na pasta do Robocode. Sem elas, o programa para logo no começo, com uma mensagem que começa em `java.lang.NullPointerException: Cannot invoke "java.util.HashMap.keySet()" because "net.sf.robocode.io.URLJarCollector.fileCache" is null`.

Escrever tudo isso a cada execução cansa. O Java aceita um **arquivo de opções**: um arquivo de texto com uma opção por linha, que se usa escrevendo `@` e o nome do arquivo no comando. **Crie o arquivo** `iniciador/opcoes.txt`:

<!-- arquivo: iniciador/opcoes.txt | criar -->
```text
-Xmx2048M
--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.desktop/javax.swing.text=ALL-UNNAMED
--add-opens=java.desktop/sun.awt=ALL-UNNAMED
```

Agora rode uma batalha de 3 rodadas entre dois robôs que já vêm com o Robocode. Depois do `-cp`, o `iniciador` no fim do classpath é a pasta onde ficou o nosso `.class`:

<!-- executar: | espera: sample.Walls | tempo: 120 -->
```bash
java @iniciador/opcoes.txt -cp "$ROBOCODE/libs/*:iniciador" IniciadorDeBatalha "$ROBOCODE" 3 sample.Walls sample.SpinBot
```

No Prompt de Comando do Windows, o separador do classpath é `;` e não `:`:

```text
java @iniciador\opcoes.txt -cp "%ROBOCODE%\libs\*;iniciador" IniciadorDeBatalha "%ROBOCODE%" 3 sample.Walls sample.SpinBot
```

Depois de alguns segundos (neste computador, entre oito e quinze), o terminal mostra algo como:

```text
1	sample.Walls	385	3
2	sample.SpinBot	17	0
```

Os números mudam a cada batalha, porque as posições iniciais e os disparos são sorteados. Neste exemplo, o `Walls` ganhou as três rodadas e somou 385 pontos.

Agora o seu robô. Ele é um robô "solto" na pasta `robots`, e o Robocode marca os robôs assim com um **asterisco** no nome: `aluno.Vigia*`. É assim que precisamos escrevê-lo, **entre aspas**, para o terminal não tratar o `*` como curinga de nomes de arquivo:

<!-- executar: | espera: aluno.Vigia | tempo: 120 -->
```bash
java @iniciador/opcoes.txt -cp "$ROBOCODE/libs/*:iniciador" IniciadorDeBatalha "$ROBOCODE" 3 "aluno.Vigia*" sample.SpinBot
```

No Windows:

```text
java @iniciador\opcoes.txt -cp "%ROBOCODE%\libs\*;iniciador" IniciadorDeBatalha "%ROBOCODE%" 3 "aluno.Vigia*" sample.SpinBot
```

Um resultado possível:

```text
2	sample.SpinBot	370	2
1	aluno.Vigia*	192	1
```

Se aparecer `Can't find 'aluno.Vigia*'`, o robô não foi encontrado: confira se a compilação do passo 3 gerou `robots/aluno/Vigia.class` e se o `.properties` foi copiado para a mesma pasta.

Olhe com atenção para esse resultado. A lista vem ordenada por pontos, e o `SpinBot` fez 370 pontos e ganhou 2 rodadas. Mesmo assim, o primeiro campo, a colocação (*rank*) que o Robocode calcula, deu **1** para o `Vigia`, que ficou com 192 pontos e 1 rodada. Nas batalhas de dois robôs que rodamos para escrever este capítulo, a colocação 1 foi sempre do robô **listado primeiro** no comando, ganhasse ele ou perdesse: com o `Vigia` na frente, ele recebeu o 1 até nas batalhas que perdeu, e com o `SpinBot` na frente aconteceu o mesmo com o `SpinBot`. Não sabemos a causa, e não precisamos saber: o nosso programa vai ignorar a colocação e decidir o vencedor pelas **rodadas ganhas** (o quarto campo), usando a pontuação para desempatar.

## Passo 6 — Medir, suspeitar, corrigir

Mais de oito segundos para simular três rodadas parece muito. Antes de achar que é assim mesmo, vamos **medir** o tempo da batalha dentro do próprio programa. **Substitua o método** `main`:

<!-- arquivo: iniciador/IniciadorDeBatalha.java | substituir-metodo main -->
```java
    public static void main(String[] args) {
        String pastaDoRobocode = args[0];
        int rodadas = Integer.parseInt(args[1]);
        String robos = String.join(",", Arrays.copyOfRange(args, 2, args.length));

        RobocodeEngine.setLogMessagesEnabled(false);
        RobocodeEngine motor = new RobocodeEngine(new File(pastaDoRobocode));
        motor.addBattleListener(new Observador());
        motor.setVisible(false);

        RobotSpecification[] escolhidos = motor.getLocalRepository(robos);
        long inicio = System.nanoTime();
        motor.runBattle(new BattleSpecification(rodadas, new BattlefieldSpecification(800, 600), escolhidos), true);
        System.err.println("batalha simulada em " + (System.nanoTime() - inicio) / 1_000_000 + " ms");
        motor.close();
        System.exit(0);
    }
```

O `System.nanoTime()` devolve um instante em nanossegundos (bilionésimos de segundo), contado a partir de um ponto qualquer. Só a **diferença** entre duas leituras tem sentido: ela é o tempo que passou, e dividir por um milhão converte para milissegundos. A mensagem vai para o `System.err`, a **saída de erro**: um segundo canal de texto, separado do `System.out`. No terminal os dois aparecem juntos, mas, quando redirecionarmos a saída para um arquivo (passo 8), só o `System.out` irá para ele, e a medida de tempo continuará aparecendo na tela, sem sujar a gravação.

Compile e rode de novo o duelo do passo 5:

<!-- executar: | espera: ok -->
```bash
javac -cp "$ROBOCODE/libs/*" -d iniciador iniciador/IniciadorDeBatalha.java && echo ok
```

<!-- executar: | espera: batalha simulada em | tempo: 120 -->
```bash
java @iniciador/opcoes.txt -cp "$ROBOCODE/libs/*:iniciador" IniciadorDeBatalha "$ROBOCODE" 3 sample.Walls sample.SpinBot
```

No computador em que escrevemos este capítulo, a medida ficou assim, e variou entre 9 e 13 segundos nas várias vezes que repetimos:

```text
batalha simulada em 13403 ms
```

Cada uma dessas batalhas tem uns quatro mil turnos. O motor deveria dar conta disso muito mais depressa. Uma suspeita: o Robocode **toca sons** (tiros, batidas, explosões) durante a batalha, e o som faz parte do motor, não da janela, de modo que esconder a janela não o desliga. Se tocar o som custa tempo, desligá-lo deve encurtar a batalha. A melhor forma de saber é desligar o som **e medir de novo**, sem mudar mais nada.

Desligamos o som **só nesta execução**, sem mexer nas configurações salvas do Robocode. O motor guarda os gerenciadores de som e de configurações dentro dele, em classes que não aparecem na API pública. Para chegar nelas usamos **reflexão** (*reflection*): procurar uma classe e chamar um método dela **pelo nome**, enquanto o programa roda, em vez de escrevê-los diretamente no código. Se o Robocode mudar essas classes numa versão futura, a busca falha sem derrubar o programa: a batalha só fica mais lenta e barulhenta.

**No topo do arquivo**, junto dos outros `import`:

<!-- arquivo: iniciador/IniciadorDeBatalha.java | importar -->
```java
import java.lang.reflect.Method;
```

**Acrescente ao final da classe** `IniciadorDeBatalha`, depois da classe `Observador` e antes da última chave do arquivo:

<!-- arquivo: iniciador/IniciadorDeBatalha.java | acrescentar -->
```java
    /** Desliga os sons do Robocode só nesta execução, em memória: as configurações do usuário não são salvas. */
    private static void silenciarSons() {
        try {
            // os componentes do motor moram no carregador de classes dele, que é o desta linha de execução
            ClassLoader motor = Thread.currentThread().getContextClassLoader();
            Class<?> recipiente = Class.forName("net.sf.robocode.core.Container", true, motor);
            Method componente = recipiente.getMethod("getComponent", Class.class);

            Class<?> configuracoes = Class.forName("net.sf.robocode.settings.ISettingsManager", true, motor);
            configuracoes.getMethod("setOptionsSoundEnableSound", boolean.class).invoke(componente.invoke(null, configuracoes), false);

            Class<?> sons = Class.forName("net.sf.robocode.sound.ISoundManager", true, motor);
            sons.getMethod("setEnableSound", boolean.class).invoke(componente.invoke(null, sons), false);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException falha) {
            // a batalha é a mesma, só mais lenta e barulhenta
        }
    }
```

O `catch` aceita três tipos de falha de uma vez (`|`): a reflexão lança `ReflectiveOperationException` quando não acha a classe ou o método, `LinkageError` quando a classe existe mas não pode ser carregada, e `RuntimeException` para o resto. Em todos os casos o programa segue em frente.

Agora chame o método no `main`, logo depois de esconder a janela. **Substitua o método** `main`:

<!-- arquivo: iniciador/IniciadorDeBatalha.java | substituir-metodo main -->
```java
    public static void main(String[] args) {
        String pastaDoRobocode = args[0];
        int rodadas = Integer.parseInt(args[1]);
        String robos = String.join(",", Arrays.copyOfRange(args, 2, args.length));

        RobocodeEngine.setLogMessagesEnabled(false);
        RobocodeEngine motor = new RobocodeEngine(new File(pastaDoRobocode));
        motor.addBattleListener(new Observador());
        motor.setVisible(false);
        silenciarSons();

        RobotSpecification[] escolhidos = motor.getLocalRepository(robos);
        long inicio = System.nanoTime();
        motor.runBattle(new BattleSpecification(rodadas, new BattlefieldSpecification(800, 600), escolhidos), true);
        System.err.println("batalha simulada em " + (System.nanoTime() - inicio) / 1_000_000 + " ms");
        motor.close();
        System.exit(0);
    }
```

Compile e rode o mesmo duelo outra vez:

<!-- executar: | espera: ok -->
```bash
javac -cp "$ROBOCODE/libs/*" -d iniciador iniciador/IniciadorDeBatalha.java && echo ok
```

<!-- executar: | espera: batalha simulada em | tempo: 120 -->
```bash
java @iniciador/opcoes.txt -cp "$ROBOCODE/libs/*:iniciador" IniciadorDeBatalha "$ROBOCODE" 3 sample.Walls sample.SpinBot
```

No nosso computador, a medida passou a ser:

```text
batalha simulada em 507 ms
```

Nas medidas que fizemos, a batalha levou de 8,9 a 13,4 segundos com o som ligado e de 0,5 a 0,9 segundo com ele desligado: em média, mais de dez vezes menos. Mudamos uma coisa só e a hipótese se confirmou. Também testamos cada uma das duas chamadas de `silenciarSons` sozinha, e qualquer uma já bastou para a batalha encurtar. Mantemos as duas porque uma mexe na opção de configuração e a outra no gerenciador de som que já está funcionando, e não sabemos se outras versões do Robocode consultam só uma delas. O exercício 5 propõe repetir essa comparação.

Os tempos dependem do computador, e em alguns a diferença pode ser pequena. Por isso a medida vem primeiro: ela é que diz se o conserto serve.

## Passo 7 — Contando as rodadas

O resultado final diz quem ganhou, mas não *como*. Queremos acompanhar a batalha **enquanto** ela acontece. O motor avisa cada passo ao nosso `Observador`:

| Evento | Quando acontece |
|---|---|
| `onBattleStarted` | A batalha começa. Traz as regras (tamanho do campo, número de rodadas). |
| `onRoundStarted` | Uma rodada começa. |
| `onTurnEnded` | Um **turno** termina. |
| `onRoundEnded` | Uma rodada termina. |
| `onBattleCompleted` | A batalha acaba. |

O Robocode divide o tempo em **turnos**: a cada turno, cada robô faz um passo. Num turno, o tanque anda no máximo 8 unidades, e o tiro anda entre 11 (o mais forte) e 19,7 (o mais fraco): quanto mais potente, mais devagar. Uma rodada tem centenas ou milhares de turnos.

Vamos fazer o `Observador` escrever uma linha no começo e outra no fim de cada rodada. Cada linha começa com `#` e uma letra que diz o tipo da linha. Primeiro, os `import` novos. **No topo do arquivo** `IniciadorDeBatalha.java`, junto dos outros `import`:

<!-- arquivo: iniciador/IniciadorDeBatalha.java | importar -->
```java
import robocode.BattleRules;
import robocode.control.events.BattleStartedEvent;
import robocode.control.events.RoundEndedEvent;
import robocode.control.events.RoundStartedEvent;
```

**Substitua a classe** `Observador` por esta:

<!-- arquivo: iniciador/IniciadorDeBatalha.java | substituir-classe Observador -->
```java
    static class Observador extends BattleAdaptor {

        @Override
        public void onBattleStarted(BattleStartedEvent evento) {
            BattleRules regras = evento.getBattleRules();
            System.out.println("#F " + regras.getBattlefieldWidth() + " " + regras.getBattlefieldHeight() + " " + regras.getNumRounds());
        }

        @Override
        public void onRoundStarted(RoundStartedEvent evento) {
            System.out.println("#R " + evento.getRound());
        }

        @Override
        public void onRoundEnded(RoundEndedEvent evento) {
            System.out.println("#E " + evento.getRound() + " " + evento.getTurns());
        }

        @Override
        public void onBattleCompleted(BattleCompletedEvent evento) {
            for (BattleResults resultado : evento.getSortedResults()) {
                System.out.println(resultado.getRank() + "\t" + resultado.getTeamLeaderName() + "\t" + resultado.getScore() + "\t" + resultado.getFirsts());
            }
        }

        @Override
        public void onBattleError(BattleErrorEvent evento) {
            System.out.println("Err> " + evento.getError());
        }
    }
```

Compile e rode de novo, com os mesmos comandos do passo 5:

<!-- executar: | espera: ok -->
```bash
javac -cp "$ROBOCODE/libs/*" -d iniciador iniciador/IniciadorDeBatalha.java && echo ok
```

<!-- executar: | espera: #E 2 | tempo: 120 -->
```bash
java @iniciador/opcoes.txt -cp "$ROBOCODE/libs/*:iniciador" IniciadorDeBatalha "$ROBOCODE" 3 sample.Walls sample.SpinBot
```

A saída agora é como esta:

```text
#F 800 600 3
#R 0
#E 0 1310
#R 1
#E 1 1286
#R 2
#E 2 1516
1	sample.Walls	341	3
2	sample.SpinBot	18	0
```

A primeira rodada teve 1310 turnos. A 30 turnos por segundo, que é o valor padrão do Robocode, uma janela levaria uns 44 segundos só para ela, e as três rodadas (4112 turnos) passariam de dois minutos. O iniciador simulou tudo em menos de um segundo. É por isso que, no nosso programa, a batalha será **simulada primeiro e reproduzida depois**: a simulação é rápida, e a reprodução, que nós controlamos, pode ser lenta, rápida ou pausada.

## Passo 8 — Um retrato de cada turno

Falta o principal: o que cada robô e cada tiro estavam fazendo em cada turno. A cada turno, o motor entrega um **retrato** (`ITurnSnapshot`) com a lista de robôs (`IRobotSnapshot`) e de tiros (`IBulletSnapshot`). Vamos escrever uma linha `#T` por turno, uma linha `#N` com o nome de cada robô e uma linha `#C` com as cores. Os `import` primeiro, **no topo do arquivo**:

<!-- arquivo: iniciador/IniciadorDeBatalha.java | importar -->
```java
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import robocode.control.events.TurnEndedEvent;
import robocode.control.snapshot.IBulletSnapshot;
import robocode.control.snapshot.IRobotSnapshot;
import robocode.control.snapshot.ITurnSnapshot;
```

**Substitua a classe** `Observador` pela versão final:

<!-- arquivo: iniciador/IniciadorDeBatalha.java | substituir-classe Observador -->
```java
    static class Observador extends BattleAdaptor {

        private final Map<Integer, String> coresVistas = new HashMap<>();
        private boolean nomesImpressos = false;

        @Override
        public void onBattleStarted(BattleStartedEvent evento) {
            BattleRules regras = evento.getBattleRules();
            System.out.println("#F " + regras.getBattlefieldWidth() + " " + regras.getBattlefieldHeight() + " " + regras.getNumRounds());
        }

        @Override
        public void onRoundStarted(RoundStartedEvent evento) {
            ITurnSnapshot inicio = evento.getStartSnapshot();
            if (!nomesImpressos) {
                for (IRobotSnapshot robo : inicio.getRobots()) {
                    System.out.println("#N " + robo.getContestantIndex() + " " + robo.getName());
                }
                nomesImpressos = true;
            }
            System.out.println("#R " + evento.getRound());
            imprimirTurno(inicio);
        }

        @Override
        public void onTurnEnded(TurnEndedEvent evento) {
            imprimirTurno(evento.getTurnSnapshot());
        }

        @Override
        public void onRoundEnded(RoundEndedEvent evento) {
            System.out.println("#E " + evento.getRound() + " " + evento.getTurns());
        }

        private void imprimirTurno(ITurnSnapshot turno) {
            for (IRobotSnapshot robo : turno.getRobots()) {
                String cores = robo.getBodyColor() + " " + robo.getGunColor() + " " + robo.getRadarColor();
                if (!cores.equals(coresVistas.get(robo.getContestantIndex()))) {
                    coresVistas.put(robo.getContestantIndex(), cores);
                    System.out.println("#C " + robo.getContestantIndex() + " " + cores);
                }
            }

            StringBuilder linha = new StringBuilder("#T ").append(turno.getRound()).append(' ').append(turno.getTurn()).append('|');
            IRobotSnapshot[] robos = turno.getRobots();
            for (int i = 0; i < robos.length; i++) {
                IRobotSnapshot robo = robos[i];
                if (i > 0) {
                    linha.append(';');
                }
                linha.append(robo.getContestantIndex()).append(' ')
                        .append(numero(robo.getX())).append(' ')
                        .append(numero(robo.getY())).append(' ')
                        .append(numero(robo.getBodyHeading())).append(' ')
                        .append(numero(robo.getGunHeading())).append(' ')
                        .append(numero(robo.getRadarHeading())).append(' ')
                        .append(numero(robo.getEnergy())).append(' ')
                        .append(robo.getState().getValue());
            }

            linha.append('|');
            boolean primeiro = true;
            for (IBulletSnapshot tiro : turno.getBullets()) {
                // as explosões de robôs destruídos são desenhadas pelo visualizador
                if (tiro.isExplosion()) {
                    continue;
                }
                if (!primeiro) {
                    linha.append(';');
                }
                primeiro = false;
                linha.append(tiro.getBulletId()).append(' ').append(tiro.getOwnerIndex()).append(' ')
                        .append(numero(tiro.getPaintX())).append(' ')
                        .append(numero(tiro.getPaintY())).append(' ')
                        .append(numero(tiro.getPower())).append(' ')
                        .append(tiro.getState().getValue()).append(' ')
                        .append(tiro.getVictimIndex());
            }
            System.out.println(linha);
        }

        /** Três casas decimais e sempre com ponto, não importa o idioma do computador. */
        private static String numero(double valor) {
            return String.format(Locale.ROOT, "%.3f", valor);
        }

        @Override
        public void onBattleCompleted(BattleCompletedEvent evento) {
            for (BattleResults resultado : evento.getSortedResults()) {
                System.out.println(resultado.getRank() + "\t" + resultado.getTeamLeaderName() + "\t" + resultado.getScore() + "\t" + resultado.getFirsts());
            }
        }

        @Override
        public void onBattleError(BattleErrorEvent evento) {
            System.out.println("Err> " + evento.getError());
        }
    }
```

Pontos a notar:

- O `StringBuilder` monta uma linha de texto aos poucos, sem criar um texto novo a cada `append`. É a forma indicada quando o texto é montado em muitos pedaços.
- **Por que o `Locale.ROOT`?** Num computador configurado em português, `String.format("%.3f", 300.5)` escreve `300,500`, com **vírgula**. O leitor do próximo capítulo só entende o ponto. O `Locale.ROOT` é o "idioma neutro" e sempre escreve com ponto. Sem ele, o mesmo programa escreveria números diferentes em dois computadores com idiomas diferentes.
- As cores (`#C`) só são escritas quando **mudam**: um robô pode trocar de cor durante a batalha, e escrever as cores a cada turno encheria o arquivo de repetição. O `Map` guarda as últimas cores vistas de cada robô para comparar.
- Os ângulos dos robôs vêm em **radianos**: 0 é o norte e os valores crescem no sentido horário.

Compile e rode. Desta vez a saída vai para o arquivo `batalha.txt`: o `>` redireciona a saída do programa (o `System.out`) para um arquivo, e o terminal fica quieto até o fim, exceto pela medida de tempo, que sai pelo `System.err`.

<!-- executar: | espera: ok -->
```bash
javac -cp "$ROBOCODE/libs/*" -d iniciador iniciador/IniciadorDeBatalha.java && echo ok
```

<!-- executar: | espera: batalha simulada em | tempo: 120 -->
```bash
java @iniciador/opcoes.txt -cp "$ROBOCODE/libs/*:iniciador" IniciadorDeBatalha "$ROBOCODE" 3 "aluno.Vigia*" sample.SpinBot > batalha.txt
```

No Windows, o comando é o mesmo do passo 5 com `> batalha.txt` no fim. Veja o começo do arquivo (no Windows, abra-o no editor):

<!-- executar: | espera: #T 0 1 -->
```bash
head -n 8 batalha.txt
```

Você deve ver linhas assim (os números do seu arquivo serão outros):

```text
#F 800 600 3
#N 0 aluno.Vigia*
#N 1 sample.SpinBot
#R 0
#C 0 -14336 -16777216 -65536
#C 1 -16776961 -16776961 -16777216
#T 0 0|0 204.204 474.805 1.285 1.285 1.285 100.000 0;1 346.421 542.354 5.444 5.444 5.444 100.000 0|
#T 0 1|0 205.163 475.087 1.285 1.285 1.285 100.000 0;1 345.804 543.141 5.619 5.619 5.619 100.000 0|
```

Conte as linhas do arquivo:

<!-- executar: | espera: [0-9]{4,} batalha.txt -->
```bash
wc -l batalha.txt
```

O resultado é um número de quatro algarismos, como `3225 batalha.txt`: o arquivo tem uma linha por turno, de todas as rodadas, mais as linhas de cabeçalho e de resultado. Confira também que as três rodadas foram gravadas, contando as linhas `#E` (no Windows, use `findstr /b "#E" batalha.txt`):

<!-- executar: | espera: (?m)^3$ -->
```bash
grep -c "^#E" batalha.txt
```

O resultado é `3`.

## Passo 9 — Lendo uma linha

As linhas da gravação têm um formato fixo, que o próximo capítulo vai ler. A tabela diz o que é cada tipo de linha:

| Linha | Significado | Campos |
|---|---|---|
| `#F` | **F**rame: o campo de batalha | `largura altura rodadas` |
| `#N` | **N**ome de um robô | `índice nome` |
| `#C` | **C**ores de um robô, quando mudam | `índice corCorpo corCanhão corRadar` (cada cor é um número ARGB) |
| `#R` | Começo de uma **r**odada | `número` |
| `#T` | Um **t**urno | `rodada turno`, `\|`, os robôs, `\|`, os tiros |
| `#E` | Fim (**e**nd) de uma rodada | `número quantosTurnos` |

Dentro de uma linha `#T`, os três grupos ficam separados por `|`, e os itens de cada grupo por `;`. Cada **robô** traz oito campos, separados por espaço:

```text
índice  x  y  ânguloDoCorpo  ânguloDoCanhão  ânguloDoRadar  energia  estado
```

e cada **tiro** traz sete:

```text
id  dono  x  y  potência  estado  vítima
```

Veja esta linha, tirada de uma batalha de teste:

```text
#T 0 111|0 638.748 299.944 3.549 0.407 0.407 80.000 0;1 360.464 505.726 3.916 3.916 3.916 100.000 0|2 0 -3.577 569.242 2.000 4 -1;10003 1 486.447 465.292 3.000 1 -1;3 0 606.087 326.350 2.000 1 -1
```

- É o turno 111 da rodada 0.
- O robô 0, o `Vigia`, está em (638,7; 299,9). O corpo aponta para 3,549 radiano (cerca de 203 graus), o canhão e o radar para 0,407 (cerca de 23 graus). Ele tem **80** de energia e está no estado 0.
- O robô 1, o `SpinBot`, está em (360,5; 505,7) e tem **100** de energia.
- Há três tiros. O tiro `3`, do robô 0, está em (606,1; 326,4), tem potência 2 e estado 1, ou seja, em voo. O tiro `10003`, do robô 1, também está em voo, com potência 3. O tiro `2`, do robô 0, está em x = −3,6, fora do campo, no estado 4: acertou a parede.
- O último campo de cada tiro, a vítima, é `-1` nos três: nenhum acertou um robô.

Os **estados** são números fixos do Robocode:

| Estado | Robô (`RobotState`) | Tiro (`BulletState`) |
|---|---|---|
| 0 | `ACTIVE`: em campo | `FIRED`: acabou de ser disparado |
| 1 | `HIT_WALL`: bateu na parede | `MOVING`: em voo |
| 2 | `HIT_ROBOT`: bateu em outro robô | `HIT_VICTIM`: acertou um robô |
| 3 | `DEAD`: destruído | `HIT_BULLET`: acertou outro tiro |
| 4 | | `HIT_WALL`: acertou a parede |
| 5 | | `EXPLODED`: explodiu |
| 6 | | `INACTIVE`: acabou, não existe mais |

Os nomes da segunda e da terceira coluna são os que a API do Robocode dá a cada número. O campo `vítima` do tiro é o índice do robô atingido, ou `-1` quando o tiro ainda não acertou ninguém.

As **cores** são números inteiros no formato **ARGB**: quatro bytes, um para a opacidade (*alpha*) e um para cada cor (vermelho, verde e azul). Como o byte da opacidade vale 255 (`FF` em hexadecimal), o bit mais à esquerda do número fica ligado, e o Java mostra o número como negativo. Não é preciso fazer a conta à mão: o `-16777216` é preto opaco, o `-65536` é vermelho opaco e o `-14336` é laranja, as cores do `Vigia`. No capítulo 6, uma linha de código converte esses números em cores do JavaFX.

## O que construímos

```text
mini-campeonato/
├── iniciador/
│   ├── IniciadorDeBatalha.java     roda uma batalha e escreve a gravação
│   ├── IniciadorDeBatalha.class    gerado pelo javac (e o Observador, em outro .class)
│   └── opcoes.txt                  opções da máquina virtual do iniciador
├── robos/aluno/
│   ├── Vigia.java                  o seu primeiro robô
│   └── Vigia.properties            o cadastro dele no Robocode
└── batalha.txt                     a gravação da última batalha
```

## Exercícios

**Fácil**

1. Rode o iniciador com `sample.Crazy` contra `sample.Corners`, em 5 rodadas. Diga quem ganhou mais rodadas olhando o **quarto** campo, e não o primeiro.
2. Mude as cores do `Vigia` (por exemplo, `Color.GREEN` para o corpo), compile de novo como no passo 3 e rode o iniciador com a saída para `batalha.txt`. Confira os números novos na linha `#C` com `grep "^#C" batalha.txt`: o verde puro aparece como `-16711936`.

**Médio**

3. Melhore o `Vigia`: atire com `fire(3)` quando o inimigo estiver perto (`evento.getDistance()` menor que 200) e com `fire(1)` quando estiver longe. Compile e compare as rodadas ganhas em dez rodadas contra `sample.SpinBot`, antes e depois da mudança. Como a batalha tem sorteio, repita cada medida algumas vezes.
4. Faça o iniciador escrever também, na saída de erro, quantos turnos a batalha teve e quantos turnos por segundo o motor simulou. Dica: some `evento.getTurns()` num campo da classe a cada `onRoundEnded` e divida pelo tempo que você já mede no `main`. Compare o resultado com os 30 turnos por segundo da janela do Robocode. No nosso computador deu algo como 4859 turnos por segundo, mais de cem vezes a velocidade da janela.
5. Repita a comparação do passo 6, tirando do `silenciarSons` primeiro a chamada de `setOptionsSoundEnableSound` e depois a de `setEnableSound`. Meça cada versão três vezes. Qual das duas chamadas basta no seu computador?

**Difícil**

6. Escreva um segundo robô, `aluno.Atirador`, com outra estratégia (por exemplo, parar e girar o radar até achar o inimigo, mirar o canhão e atirar). Faça-o duelar com o `Vigia` em quinze rodadas e decida o melhor pelas rodadas ganhas. Não esqueça do `.properties` e do asterisco no nome.
7. Confira que o `silenciarSons` realmente muda a opção. Dentro dele, a interface `ISettingsManager` tem também o método `getOptionsSoundEnableSound()`, que devolve um `boolean`. Chame-o por reflexão, do mesmo jeito que o `set...` é chamado, antes e depois de silenciar, e escreva os dois valores na saída de erro. Dica: o valor muda de `true` para `false`, e o arquivo `config/robocode.properties` do Robocode continua igual.

## Perguntas para fixação

1. Dê duas razões para o programa principal iniciar o iniciador como um programa separado, em vez de chamar o motor do Robocode de dentro dele.
2. Por que a classe `Observador` herda de `BattleAdaptor` em vez de implementar a interface do zero, e por que ela sobrescreve só alguns métodos?
3. O que aconteceria se o iniciador escrevesse os números com o `Locale` do computador, num computador em português?
4. Por que o nosso programa não vai usar o primeiro campo das linhas de resultado para saber quem venceu a batalha?
5. No passo 6, como sabemos que foi o som, e não outra coisa, que atrasava a batalha?

<details>
<summary>Respostas</summary>

1. Por exemplo: (a) o motor precisa de opções especiais da máquina virtual (`--add-opens`, memória) que não queremos impor ao programa principal; (b) se o motor falhar ou ficar sem memória, só o iniciador cai; (c) o projeto Maven não precisa dos arquivos do Robocode para compilar; (d) a conversa por texto permite guardar a batalha num arquivo e reproduzi-la depois.
2. `BattleAdaptor` já implementa todos os métodos da interface, todos vazios. Herdando dela, escrevemos só os que interessam e ignoramos os outros, em vez de ter de escrever uma dezena de métodos vazios.
3. Os decimais sairiam com vírgula (`300,500`). O leitor do próximo capítulo só entende o ponto, e o `Double.parseDouble("300,500")` lança uma `NumberFormatException`. Com o `Locale.ROOT`, a saída é a mesma em qualquer computador.
4. Porque, nas batalhas de dois robôs que rodamos, esse campo (a colocação) seguiu a ordem em que os robôs foram listados no comando, e não os pontos. O vencedor sai das rodadas ganhas (o quarto campo), com a pontuação (o terceiro) para desempatar.
5. Porque medimos a mesma batalha, no mesmo computador, antes e depois de mudar **uma única coisa**, o som, e o tempo caiu de uns 9 a 13 segundos para menos de 1. Se tivéssemos mudado o som e mais outra coisa ao mesmo tempo, não saberíamos qual das duas foi a responsável.

</details>

No próximo capítulo o programa JavaFX passa a rodar esse iniciador sozinho e a ler essas linhas.
