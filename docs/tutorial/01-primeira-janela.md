# Capítulo 1 — A primeira janela

## Objetivo

Criar o projeto Maven e abrir uma janela JavaFX que já tem o **coração** do programa: um relógio que chama o método `atualizar` a cada quadro desenhado. É esse relógio que, nos próximos capítulos, vai mover os tanques, os tiros e o fogo.

## Passo 1 — O arquivo `pom.xml`

O Maven lê um arquivo chamado `pom.xml` (*Project Object Model*) para saber o que o projeto é e do que ele precisa. Dentro da pasta `mini-campeonato`, **crie o arquivo** `pom.xml`:

<!-- arquivo: pom.xml | criar -->
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>campeonato</groupId>
    <artifactId>mini-campeonato</artifactId>
    <version>1.0</version>
    <name>Mini Campeonato de Robocode</name>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <javafx.version>21.0.12</javafx.version>
        <classe.principal>campeonato.Principal</classe.principal>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>${javafx.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.15.0</version>
            </plugin>
            <plugin>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <version>0.0.8</version>
                <configuration>
                    <mainClass>${classe.principal}</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Cada parte tem um papel:

| Trecho | Para que serve |
|---|---|
| `groupId`, `artifactId`, `version` | O "endereço" do projeto: quem o fez, como se chama e em que versão está. |
| `maven.compiler.release` | Compila para o Java 21. |
| `project.build.sourceEncoding` | Os arquivos de texto são lidos como UTF-8. Sem isso, letras com acento podem sair trocadas no Windows. |
| `javafx.version` | A versão do JavaFX, guardada num lugar só para ser trocada com facilidade. |
| `classe.principal` | A classe que tem o `main`, guardada num lugar só. É o programa que o `mvn javafx:run` abre. Mais adiante, um `-Dclasse.principal=...` na linha de comando abre outra classe no lugar dela. |
| `javafx-controls` | A biblioteca de janelas e botões do JavaFX. Ela traz de carona o `javafx-graphics`, que tem o 3D. |
| `maven-compiler-plugin` | O plugin que chama o `javac`. Fixamos a versão para que o projeto compile igual em todos os computadores. |
| `javafx-maven-plugin` | O plugin que abre o programa com `mvn javafx:run`. O `mainClass` diz qual classe tem o `main`; o `${classe.principal}` é o valor da propriedade de mesmo nome. |

O **JavaFX** é a biblioteca que desenha janelas, botões e cenas 3D. Ela deixou de vir dentro do JDK no Java 11, por isso entra como dependência. O Maven baixa o JavaFX da internet na primeira vez, junto com a versão certa para o seu sistema operacional.

## Passo 2 — A primeira janela

O Maven espera encontrar o código em `src/main/java`, dentro de pastas que repetem o nome do pacote. O nosso pacote principal chama-se `campeonato`. **Crie o arquivo** `src/main/java/campeonato/Principal.java`:

<!-- arquivo: src/main/java/campeonato/Principal.java | criar -->
```java
package campeonato;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/** O programa: abre a janela. */
public class Principal extends Application {

    @Override
    public void start(Stage palco) {
        Label titulo = new Label("Mini Campeonato de Robocode");
        Scene cena = new Scene(titulo, 800, 600);
        palco.setTitle("Mini Campeonato de Robocode");
        palco.setScene(cena);
        palco.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

Linha a linha:

- `package campeonato;` diz a que pacote a classe pertence. Pacotes são pastas de classes; o nome tem de bater com a pasta onde o arquivo está.
- `extends Application` faz da nossa classe um programa JavaFX. O JavaFX cuida de abrir a janela e de chamar o método `start`, onde dizemos o que mostrar.
- `main` é onde todo programa Java começa. Ele só chama `launch`, que prepara o JavaFX e depois chama o `start`.
- Um programa JavaFX tem **três camadas**: o `Stage` (o *palco*, que é a janela do sistema), a `Scene` (a *cena*, o conteúdo da janela) e os **nós**, as peças dentro da cena. Aqui o único nó é um `Label`, um texto.

## Passo 3 — Rodar

Num terminal, dentro da pasta `mini-campeonato`:

<!-- executar: | rodar: 20 | proibe: Exception, ERROR -->
```bash
mvn javafx:run
```

Na primeira vez o Maven baixa o JavaFX e os plugins, o que leva alguns minutos e enche o terminal de linhas `Downloading`. Nas próximas, é rápido.

Uma janela de 800 por 600 pixels deve abrir, com o texto "Mini Campeonato de Robocode" no canto de cima à esquerda. Feche a janela para o comando terminar.

![A primeira janela](imagens/01-primeira-janela.png)

Se o terminal mostrar `release version 21 not supported`, o Maven está usando um JDK mais antigo que o 21. Confira o `mvn -v` do capítulo anterior.

## Passo 4 — Uma arena para pôr coisas dentro

Um `Label` solto não serve para uma batalha. Vamos criar uma classe só para a **arena**, que será o conteúdo da janela, e que crescerá a cada capítulo. Ela mora no pacote `campeonato.cena`, onde ficará tudo o que for visual. **Crie o arquivo** `src/main/java/campeonato/cena/Arena3D.java`:

<!-- arquivo: src/main/java/campeonato/cena/Arena3D.java | criar -->
```java
package campeonato.cena;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/** A tela onde a batalha acontece. Por enquanto só mostra o relógio do programa. */
public final class Arena3D extends StackPane {

    private final Label medidor = new Label();

    public Arena3D() {
        setStyle("-fx-background-color: #101820;");
        medidor.setTextFill(Color.WHITE);
        StackPane.setAlignment(medidor, Pos.TOP_LEFT);
        StackPane.setMargin(medidor, new Insets(10));
        getChildren().add(medidor);
    }
}
```

O `extends StackPane` diz que a `Arena3D` **é** um `StackPane`: ela herda tudo o que um painel desse tipo sabe fazer, e acrescenta o que é dela. O `final` antes de `class` diz o contrário, que ninguém vai herdar da `Arena3D`, o que o compilador confere. `StackPane` é um painel que empilha os filhos um sobre o outro. Mais tarde, a cena 3D ficará embaixo e o placar em cima, e o `StackPane` faz exatamente isso. `getChildren()` devolve a lista de filhos do painel, e `add` põe o `medidor` nela. O `setStyle` usa a linguagem CSS do JavaFX para pintar o fundo.

## Passo 5 — O relógio do programa

Um jogo, ou uma reprodução de batalha, é um laço que se repete umas 60 vezes por segundo: **atualiza** o estado das coisas e **desenha**. No JavaFX, o desenho é automático; o que escrevemos é a atualização. A classe `AnimationTimer` chama um método a cada quadro.

Primeiro o `import`. **No topo do arquivo** `Arena3D.java`, junto dos outros:

<!-- arquivo: src/main/java/campeonato/cena/Arena3D.java | importar -->
```java
import javafx.animation.AnimationTimer;
```

Agora o relógio e o método `atualizar`. **Acrescente ao final da classe**:

<!-- arquivo: src/main/java/campeonato/cena/Arena3D.java | acrescentar -->
```java
    private double tempoTotal = 0;
    private double tempoDoPeriodo = 0;
    private int quadrosDoPeriodo = 0;

    /** Chama atualizar() a cada quadro desenhado pelo JavaFX. */
    private final AnimationTimer relogio = new AnimationTimer() {
        private long ultimo = -1;

        @Override
        public void handle(long agora) {
            // 'agora' vem em nanossegundos; o primeiro quadro não tem um anterior
            double delta = ultimo < 0 ? 0 : Math.min(0.1, (agora - ultimo) / 1e9);
            ultimo = agora;
            atualizar(delta);
        }
    };

    /** Avança o programa em 'delta' segundos. Tudo o que se move passa por aqui. */
    void atualizar(double delta) {
        tempoTotal += delta;
        tempoDoPeriodo += delta;
        quadrosDoPeriodo++;
        if (tempoDoPeriodo >= 0.5) {
            medidor.setText(String.format("%.0f quadros por segundo  |  %.1f s", quadrosDoPeriodo / tempoDoPeriodo, tempoTotal));
            tempoDoPeriodo = 0;
            quadrosDoPeriodo = 0;
        }
    }
```

O que há de importante aqui:

- O `handle` recebe o instante atual em **nanossegundos** (um bilionésimo de segundo). Subtraindo o instante do quadro anterior e dividindo por um bilhão, temos o **delta**: quantos segundos se passaram entre dois quadros.
- Tudo o que se mover no programa vai ser multiplicado por esse delta. Um tanque que anda 100 unidades por segundo anda `100 * delta` a cada quadro, e por isso cruza a arena em um tempo certo, tanto num computador rápido quanto num lento. Se movêssemos "5 unidades por quadro", o tanque andaria mais depressa em quem tem mais quadros por segundo.
- O `ultimo < 0 ? 0 : ...` é o **operador condicional**: `condição ? a : b` vale `a` se a condição for verdadeira e `b` se for falsa. Aqui, no primeiro quadro, em que `ultimo` ainda vale `-1`, o delta é 0; nos outros quadros é o tempo medido.
- O `Math.min(0.1, ...)` limita o delta a um décimo de segundo. Se a janela ficar travada por um instante (você arrasta a janela, ou o computador engasga), o quadro seguinte não dá um salto enorme.
- O `medidor` só é atualizado a cada meio segundo. Escrever um número novo 60 vezes por segundo seria ilegível.

O relógio ainda não foi ligado. Falta o construtor dizer **quando** ele roda. **Substitua o método** `Arena3D` (o construtor) por esta versão:

<!-- arquivo: src/main/java/campeonato/cena/Arena3D.java | substituir-metodo Arena3D -->
```java
    public Arena3D() {
        setStyle("-fx-background-color: #101820;");
        medidor.setTextFill(Color.WHITE);
        StackPane.setAlignment(medidor, Pos.TOP_LEFT);
        StackPane.setMargin(medidor, new Insets(10));
        getChildren().add(medidor);

        // O relógio só roda enquanto a arena está dentro de uma janela
        sceneProperty().addListener((observavel, antiga, nova) -> {
            if (nova == null) {
                relogio.stop();
            } else {
                relogio.start();
            }
        });
    }
```

`sceneProperty()` é uma **propriedade observável**: um valor que avisa quando muda. Aqui ele diz em qual `Scene` a arena está; `null` significa "em nenhuma". A setinha `->` cria uma **expressão lambda**, um pedaço de código passado como argumento: "quando a cena mudar, faça isto". Sem esse cuidado, o relógio continuaria rodando para sempre, mesmo com a arena fora da janela.

<!-- compilar -->

## Passo 6 — Ligar a arena à janela

Falta a `Principal` mostrar a arena no lugar do `Label`. Como o arquivo é pequeno, **substitua o conteúdo inteiro** de `src/main/java/campeonato/Principal.java` por:

<!-- arquivo: src/main/java/campeonato/Principal.java | criar -->
```java
package campeonato;

import campeonato.cena.Arena3D;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** O programa: abre a janela com a arena. */
public class Principal extends Application {

    @Override
    public void start(Stage palco) {
        Arena3D arena = new Arena3D();
        palco.setTitle("Mini Campeonato de Robocode");
        palco.setScene(new Scene(arena, 1100, 700));
        palco.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

A arena está em outro pacote, `campeonato.cena`, por isso a classe `Principal` precisa do `import campeonato.cena.Arena3D;`. O `Label` saiu: quem mostra texto agora é a arena.

<!-- compilar -->

Rode de novo:

<!-- executar: | rodar: 20 | proibe: Exception, ERROR -->
```bash
mvn javafx:run
```

A janela agora é escura e, no canto, o medidor mostra algo como `60 quadros por segundo  |  3,5 s`. O segundo número cresce de meio em meio segundo (o separador decimal, vírgula ou ponto, segue o idioma do seu sistema). O número de quadros por segundo depende da sua tela: a maioria dos monitores atualiza 60 vezes por segundo, e o JavaFX acompanha essa taxa.

![O relógio da arena funcionando](imagens/01-relogio.png)

## O que construímos

```text
mini-campeonato/
├── pom.xml
└── src/main/java/campeonato/
    ├── Principal.java          abre a janela
    └── cena/
        └── Arena3D.java        a arena, com o relógio do programa
```

## Exercícios

**Fácil**

1. Troque o título da janela e a cor de fundo da arena (`#101820` é uma cor em hexadecimal: dois dígitos para vermelho, dois para verde e dois para azul).
2. Mostre o tempo total com duas casas decimais em vez de uma.

**Médio**

3. Faça a tecla **Espaço** pausar o relógio: o tempo total para de crescer e volta a crescer quando você aperta de novo. Dica: o `Scene` tem `setOnKeyPressed`, que recebe um `KeyEvent`; guarde um `boolean pausado` e ignore o `delta` quando ele for verdadeiro.
4. Mostre também o **menor** número de quadros por segundo já medido.

**Difícil**

5. Pinte o texto do medidor de verde quando houver mais de 50 quadros por segundo, de amarelo entre 30 e 50, e de vermelho abaixo disso.

## Perguntas para fixação

1. Por que o código de movimento usa o `delta` em vez de somar um valor fixo a cada quadro? O que aconteceria num computador que desenha 30 quadros por segundo se o tanque andasse "5 unidades por quadro"?
2. O que aconteceria se o `AnimationTimer` nunca fosse parado quando a arena sai da janela?

<details>
<summary>Respostas</summary>

1. Com um valor fixo por quadro, a velocidade do tanque dependeria da velocidade do computador: em 30 quadros por segundo ele andaria 150 unidades por segundo, em 60 andaria 300. Com `velocidade * delta`, a distância percorrida por segundo é sempre a mesma.
2. O relógio continuaria chamando `atualizar` para uma arena que ninguém vê, gastando processador, e o objeto da arena nunca poderia ser descartado pela memória, porque o relógio o mantém vivo.

</details>

No próximo capítulo a arena ganha uma terceira dimensão.
