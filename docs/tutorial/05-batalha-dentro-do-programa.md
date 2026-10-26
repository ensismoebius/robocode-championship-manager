# Capítulo 5 — Rodando a batalha de dentro do programa

## Objetivo

Fazer o nosso programa iniciar o `IniciadorDeBatalha`, ler o que ele escreve e transformar esse texto em objetos Java: a classificação final, a gravação turno a turno e uma medida de quanto da batalha já foi simulada. No fim, o programa roda uma batalha de verdade sozinho e relata o resultado.

## Passo 1 — Dados que não mudam: records

A gravação da batalha é, no fundo, uma enorme lista de números: posições, ângulos, energias. Para não carregar tudo em vetores de `double`, vamos dar a cada coisa uma forma com nome: um robô, o estado de um robô num turno, um tiro, um turno.

O Java tem um recurso feito para isso, o **record**. Um record é uma classe que só guarda dados. Você escreve os campos entre parênteses e o Java cria sozinho o construtor, os métodos de leitura e os métodos `equals`, `hashCode` e `toString`. Um record é **imutável**: depois de criado, nenhum valor dele muda. Veja um exemplo que não faz parte do projeto:

<!-- ilustrativo -->
```java
record Ponto(double x, double y) {
}

Ponto p = new Ponto(3, 4);
System.out.println(p.x());   // 3.0: o método de leitura tem o nome do campo
System.out.println(p);       // Ponto[x=3.0, y=4.0]
```

Compare com uma classe comum: sem o record, seriam uns vinte linhas de código para obter o mesmo. Como cada turno da gravação é criado uma vez e nunca mais alterado, o record encaixa bem.

**Crie o arquivo** `src/main/java/campeonato/robocode/Gravacao.java`. Ele começa só com os records e os campos que vão guardar a batalha:

<!-- arquivo: src/main/java/campeonato/robocode/Gravacao.java | criar -->
```java
package campeonato.robocode;

import java.util.ArrayList;
import java.util.List;

/**
 * Todos os turnos de uma batalha, lidos do texto que o IniciadorDeBatalha escreve.
 * As coordenadas são as do Robocode: a origem fica no canto inferior esquerdo e os
 * ângulos estão em radianos, com 0 no norte, crescendo no sentido horário.
 */
public final class Gravacao {

    /** O estado de um robô que o Robocode chama de "morto". */
    public static final int ROBO_MORTO = 3;

    /** Os estados de um tiro que dizem que ele acertou alguma coisa. */
    public static final int TIRO_ACERTOU_ROBO = 2;
    public static final int TIRO_ACERTOU_TIRO = 3;
    public static final int TIRO_ACERTOU_PAREDE = 4;

    /** Um robô da batalha: o índice, o nome e as cores (ARGB) do corpo, do canhão e do radar. */
    public record Robo(int indice, String nome, int corCorpo, int corCanhao, int corRadar) {
    }

    /** O que um robô está fazendo em um turno: onde está, para onde aponta cada parte e quanta energia tem. */
    public record EstadoDoRobo(int indice, double x, double y, double anguloDoCorpo, double anguloDoCanhao, double anguloDoRadar, double energia, int estado) {

        public boolean morto() {
            return estado == ROBO_MORTO;
        }
    }

    /** Um tiro em um turno: quem atirou, onde está, a potência, o estado e o índice da vítima (-1 se ninguém). */
    public record Tiro(int id, int dono, double x, double y, double potencia, int estado, int vitima) {

        /** O tiro acertou um robô, outro tiro ou a parede. */
        public boolean acertou() {
            return estado == TIRO_ACERTOU_ROBO || estado == TIRO_ACERTOU_TIRO || estado == TIRO_ACERTOU_PAREDE;
        }
    }

    /** Os robôs e os tiros de um turno de uma rodada. */
    public record Turno(int rodada, int numero, List<EstadoDoRobo> robos, List<Tiro> tiros) {
    }

    private int largura = 800;
    private int altura = 600;
    private final List<Robo> robos = new ArrayList<>();
    private final List<List<Turno>> rodadas = new ArrayList<>();

    public int largura() {
        return largura;
    }

    public int altura() {
        return altura;
    }

    public List<Robo> robos() {
        return robos;
    }

    /** Os turnos de cada rodada: {@code rodadas().get(2)} é a lista de turnos da terceira rodada. */
    public List<List<Turno>> rodadas() {
        return rodadas;
    }
}
```

Alguns pontos:

- `List<Turno>` lê-se "lista de turnos". O que fica entre `<` e `>` diz o tipo dos elementos. `List<List<Turno>>` é uma lista de listas: uma lista para cada rodada, com os turnos dentro.
- `ROBO_MORTO` é uma **constante**: `static` (pertence à classe, não a um objeto) e `final` (não muda). Dar nome ao número 3 evita, mais adiante, escrever `estado == 3` sem que ninguém lembre o que o 3 quer dizer. Os estados são os da tabela do capítulo 4.
- Um record pode ter métodos próprios, como o `morto()` e o `acertou()`. O `acertou()` diz que o tiro acertou alguma coisa nos três estados de impacto da tabela do capítulo 4: um robô, outro tiro ou a parede.
- A classe é `final`: ninguém pode herdar dela.

## Passo 2 — Ler um turno

Agora a parte de ler. Começamos por uma linha `#T`, a mais importante. Lembre o formato: `#T rodada turno|robô;robô|tiro;tiro`, e cada robô e cada tiro com seus campos separados por espaço. **Acrescente ao final da classe** `Gravacao`:

<!-- arquivo: src/main/java/campeonato/robocode/Gravacao.java | acrescentar -->
```java
    /** Lê o que vem depois de "#T ": "rodada turno|robôs|tiros". */
    private void lerTurno(String corpo) {
        String[] partes = corpo.split("\\|", -1);
        String[] cabecalho = partes[0].split(" ");
        int rodada = Integer.parseInt(cabecalho[0]);
        int numero = Integer.parseInt(cabecalho[1]);

        List<EstadoDoRobo> estados = new ArrayList<>();
        if (partes.length > 1 && !partes[1].isEmpty()) {
            for (String robo : partes[1].split(";")) {
                String[] campos = robo.split(" ");
                estados.add(new EstadoDoRobo(Integer.parseInt(campos[0]), Double.parseDouble(campos[1]), Double.parseDouble(campos[2]),
                        Double.parseDouble(campos[3]), Double.parseDouble(campos[4]), Double.parseDouble(campos[5]),
                        Double.parseDouble(campos[6]), Integer.parseInt(campos[7])));
            }
        }

        List<Tiro> tiros = new ArrayList<>();
        if (partes.length > 2 && !partes[2].isEmpty()) {
            for (String tiro : partes[2].split(";")) {
                String[] campos = tiro.split(" ");
                tiros.add(new Tiro(Integer.parseInt(campos[0]), Integer.parseInt(campos[1]), Double.parseDouble(campos[2]), Double.parseDouble(campos[3]),
                        Double.parseDouble(campos[4]), Integer.parseInt(campos[5]), Integer.parseInt(campos[6])));
            }
        }

        garantirRodada(rodada);
        rodadas.get(rodada).add(new Turno(rodada, numero, estados, tiros));
    }

    /** Cria as listas de turnos das rodadas que ainda não existem, até a rodada pedida. */
    private void garantirRodada(int rodada) {
        while (rodadas.size() <= rodada) {
            rodadas.add(new ArrayList<>());
        }
    }
```

O que o método faz, em ordem:

- `corpo.split("\\|", -1)` corta o texto onde houver um `|`. O `split` recebe uma **expressão regular**, e nela o `|` tem significado especial ("ou"), por isso ele vem escrito `\\|`: a barra invertida tira o significado especial, e como em Java a própria barra invertida precisa ser dobrada dentro de um texto, ficam duas. O `-1` manda manter os pedaços vazios no fim. Sem ele, um turno sem tiros, que termina em `|`, perderia a terceira parte.
- `Integer.parseInt` e `Double.parseDouble` transformam texto em número. O `Double.parseDouble` só entende o **ponto** como separador decimal, e é por isso que, no capítulo 4, escrevemos os números com `Locale.ROOT`.
- Uma lista vazia (`partes[1].isEmpty()`) é tratada à parte, porque `"".split(";")` devolveria um vetor com um texto vazio, e `Integer.parseInt("")` daria erro.
- `garantirRodada` cria as listas de turnos que faltam. Se o primeiro turno que chegar for da rodada 2, as rodadas 0 e 1 passam a existir, vazias.

## Passo 3 — Ler a gravação inteira

Falta cuidar das outras linhas (`#F`, `#N`, `#C`, `#R`) e de percorrer o texto todo. **Acrescente ao final da classe** `Gravacao`:

<!-- arquivo: src/main/java/campeonato/robocode/Gravacao.java | acrescentar -->
```java
    /**
     * Lê as linhas que começam com {@code #} e ignora as outras (o resultado, as mensagens de erro, a medida de tempo).
     */
    public static Gravacao ler(List<String> linhas) {
        Gravacao gravacao = new Gravacao();
        for (String linha : linhas) {
            if (linha.length() < 3 || linha.charAt(0) != '#') {
                continue;
            }
            try {
                gravacao.lerLinha(linha);
            } catch (RuntimeException defeito) {
                // uma linha estragada custa um turno, não a batalha
                System.err.println("Linha da batalha ignorada: " + linha);
            }
        }
        return gravacao;
    }

    /** Lê uma linha da gravação, escolhendo o que fazer pela letra que vem depois do #. */
    private void lerLinha(String linha) {
        String corpo = linha.substring(3);
        switch (linha.charAt(1)) {
        case 'F' -> {
            String[] campos = corpo.split(" ");
            largura = Integer.parseInt(campos[0]);
            altura = Integer.parseInt(campos[1]);
        }
        case 'N' -> {
            int espaco = corpo.indexOf(' ');
            robos.add(new Robo(Integer.parseInt(corpo.substring(0, espaco)), corpo.substring(espaco + 1), COR_PADRAO_CORPO, COR_PADRAO_CORPO, COR_PADRAO_RADAR));
        }
        case 'C' -> lerCores(corpo);
        case 'R' -> garantirRodada(Integer.parseInt(corpo.trim()));
        case 'T' -> lerTurno(corpo);
        default -> {
        }
        }
    }

    /** Lê "índice corpo canhão radar". Só as primeiras cores definem o modelo do robô. */
    private void lerCores(String corpo) {
        String[] campos = corpo.split(" ");
        int indice = Integer.parseInt(campos[0]);
        if (!coloridos.add(indice)) {
            return;
        }
        for (int i = 0; i < robos.size(); i++) {
            if (robos.get(i).indice() == indice) {
                robos.set(i, new Robo(indice, robos.get(i).nome(), Integer.parseInt(campos[1]), Integer.parseInt(campos[2]), Integer.parseInt(campos[3])));
            }
        }
    }
```

Estes métodos usam três nomes que ainda não existem: `COR_PADRAO_CORPO`, `COR_PADRAO_RADAR` e `coloridos`. **Acrescente** as constantes logo abaixo de `ROBO_MORTO` e o conjunto junto dos outros campos. Primeiro, as constantes:

<!-- arquivo: src/main/java/campeonato/robocode/Gravacao.java | inserir-depois public static final int ROBO_MORTO = 3; -->
```java

    /** As cores de um robô que nunca disse quais são as suas: azul no corpo e no canhão, amarelo no radar. */
    private static final int COR_PADRAO_CORPO = 0xff2ec4ff;
    private static final int COR_PADRAO_RADAR = 0xffffd400;
```

E o conjunto:

<!-- arquivo: src/main/java/campeonato/robocode/Gravacao.java | inserir-depois private final List<List<Turno>> rodadas = new ArrayList<>(); -->
```java
    /** Os índices dos robôs cujas cores já foram lidas. */
    private final Set<Integer> coloridos = new HashSet<>();
```

E o `import` dos dois tipos novos, **no topo do arquivo**:

<!-- arquivo: src/main/java/campeonato/robocode/Gravacao.java | importar -->
```java
import java.util.HashSet;
import java.util.Set;
```

Notas sobre o código:

- O `continue` pula para a próxima volta do laço. Linhas que não começam com `#` (a classificação, a medida de tempo) são ignoradas aqui.
- O `try`/`catch` protege cada linha: se uma linha vier estragada e der erro, ela é registrada e o programa segue. Sem isso, uma só linha ruim derrubaria a leitura de uma batalha de milhares de turnos.
- O `switch` com setas (`case 'F' -> ...`) escolhe o que fazer pelo caractere que vem depois do `#`. Cada `case` com chaves roda o bloco e termina; não é preciso `break`. O `default` vazio cobre as letras que não nos interessam, como o `#E`.
- Um `Set` é uma coleção sem repetições. `coloridos.add(indice)` devolve `true` se o índice entrou agora e `false` se já estava lá. Por isso o `if (!coloridos.add(indice)) return;` quer dizer: "se as cores deste robô já foram lidas, não faça nada". Um robô pode mudar de cor no meio da batalha, mas o modelo dele é definido pelas primeiras cores.
- Como o `Robo` é imutável, para mudar as cores não dá para alterar o objeto: criamos um novo `Robo` e o colocamos no lugar do antigo com `robos.set(i, ...)`.

Compile:

<!-- compilar -->

## Passo 4 — Testar sem o Robocode

Como saber se a leitura está certa sem rodar uma batalha inteira a cada mudança? Escrevendo um **teste automático**: um programa pequeno que alimenta o código com uma entrada conhecida e confere se a saída é a esperada. Usaremos o **JUnit**, a biblioteca de testes mais usada em Java, e o Maven roda os testes para nós.

Duas mudanças no `pom.xml`. A primeira, a dependência do JUnit. Ela vale só para os testes (`<scope>test</scope>`), então não vai para o programa final. **Acrescente**, dentro de `<dependencies>`, depois da dependência do JavaFX:

<!-- arquivo: pom.xml | inserir-antes </dependencies> -->
```xml
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>6.1.3</version>
            <scope>test</scope>
        </dependency>
```

A segunda, o plugin que executa os testes. **Acrescente**, dentro de `<plugins>`, depois do plugin do compilador:

<!-- arquivo: pom.xml | inserir-antes </plugins> -->
```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.6</version>
            </plugin>
```

Agora o teste. As classes de teste ficam em `src/test/java`, na mesma estrutura de pastas da classe testada. Uma linha de cada tipo, escrita à mão, alimenta a leitura. **Crie o arquivo** `src/test/java/campeonato/robocode/GravacaoTest.java`:

<!-- arquivo: src/test/java/campeonato/robocode/GravacaoTest.java | criar -->
```java
package campeonato.robocode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class GravacaoTest {

    private static final String BATALHA = """
            #F 800 600 1
            #N 0 aluno.Vigia*
            #N 1 sample.SpinBot
            #C 0 -14336 -16777216 -65536
            #R 0
            #T 0 0|0 100.000 200.000 0.000 0.000 0.000 100.000 0;1 700.000 400.000 3.142 3.142 3.142 100.000 0|
            #T 0 1|0 101.000 200.000 0.000 0.500 0.500 98.000 0;1 699.000 400.000 3.142 3.142 3.142 0.000 3|7 0 150.000 220.000 2.000 1 -1
            #E 0 2
            1\taluno.Vigia*\t98\t1
            """;

    private final Gravacao gravacao = Gravacao.ler(BATALHA.lines().toList());

    @Test
    void leOCampoEOsNomes() {
        assertEquals(800, gravacao.largura());
        assertEquals(600, gravacao.altura());
        assertEquals(2, gravacao.robos().size());
        assertEquals("aluno.Vigia*", gravacao.robos().get(0).nome());
        assertEquals("sample.SpinBot", gravacao.robos().get(1).nome());
    }

    @Test
    void asPrimeirasCoresDefinemOModeloDoRobo() {
        Gravacao.Robo vigia = gravacao.robos().get(0);
        assertEquals(-14336, vigia.corCorpo());
        assertEquals(-16777216, vigia.corCanhao());
        assertEquals(-65536, vigia.corRadar());
    }

    @Test
    void leOsTurnosDeCadaRodada() {
        assertEquals(1, gravacao.rodadas().size());
        List<Gravacao.Turno> turnos = gravacao.rodadas().get(0);
        assertEquals(2, turnos.size());
        assertEquals(1, turnos.get(1).numero());
    }

    @Test
    void leOEstadoDosRobos() {
        Gravacao.EstadoDoRobo vigia = gravacao.rodadas().get(0).get(1).robos().get(0);
        assertEquals(101.0, vigia.x(), 0.001);
        assertEquals(0.5, vigia.anguloDoCanhao(), 0.001);
        assertEquals(98.0, vigia.energia(), 0.001);
        assertFalse(vigia.morto());
        assertTrue(gravacao.rodadas().get(0).get(1).robos().get(1).morto());
    }

    @Test
    void leOsTirosEUmTurnoSemTiros() {
        assertTrue(gravacao.rodadas().get(0).get(0).tiros().isEmpty());
        Gravacao.Tiro tiro = gravacao.rodadas().get(0).get(1).tiros().get(0);
        assertEquals(7, tiro.id());
        assertEquals(2.0, tiro.potencia(), 0.001);
        assertEquals(-1, tiro.vitima());
        assertFalse(tiro.acertou());
    }

    @Test
    void umTiroNoEstadoDeParedeJaAcertouAlgo() {
        Gravacao.Tiro naParede = new Gravacao.Tiro(1, 0, -3.5, 569.2, 2.0, 4, -1);
        assertTrue(naParede.acertou());
    }

    @Test
    void umaLinhaEstragadaNaoDerrubaALeitura() {
        Gravacao lida = Gravacao.ler(List.of("#N 0 aluno.Vigia*", "#T isto nao e um turno", "#R 0"));
        assertEquals(1, lida.robos().size());
        assertEquals(1, lida.rodadas().size());
        assertTrue(lida.rodadas().get(0).isEmpty());
    }
}
```

Alguns pontos sobre o teste:

- Os `"""` abrem e fecham um **bloco de texto** (*text block*): um texto de várias linhas, escrito como ele é. A indentação comum a todas as linhas é descartada. `\t` dentro do bloco vale uma tabulação, como em qualquer texto Java.
- `BATALHA.lines().toList()` quebra o texto em uma lista de linhas, que é o que `Gravacao.ler` espera.
- `@Test` marca um método como teste. `assertEquals(esperado, obtido)` falha o teste se os dois valores forem diferentes. Para números com casas decimais, a comparação exata é arriscada (`0.1 + 0.2` não é exatamente `0.3` no computador), então damos uma tolerância como terceiro argumento: `assertEquals(101.0, vigia.x(), 0.001)`.
- Cada teste conta uma história: o nome diz o que deve ser verdade (`leOEstadoDosRobos`). Quando um teste falha, o nome já indica onde procurar.
- O último teste cobre o caso ruim: uma linha de turno que não é um turno. A leitura deve seguir adiante com as outras linhas.

Execute os testes:

<!-- testar -->

No terminal, o mesmo comando é `mvn test`. A resposta termina com `Tests run: 6, Failures: 0, Errors: 0` e `BUILD SUCCESS`. A mensagem `Linha da batalha ignorada: #T isto nao e um turno`, que aparece no meio, é esperada: é o último teste, que estraga uma linha de propósito.

## Passo 5 — Quem ganhou

Agora o resultado final. As linhas de resultado têm quatro campos separados por tabulação (colocação, nome, pontos, rodadas ganhas), e o capítulo 4 mostrou que a colocação não é confiável. Vamos decidir pelas rodadas ganhas e usar os pontos para desempatar.

Dois detalhes pedem cuidado. O primeiro: o Robocode escreve o nome do robô à sua maneira, às vezes com um `*` no fim (robôs "soltos"), às vezes com a versão (`team.Robot 1.0`) ou com um número de instância (`team.Robot (1)`). Precisamos reduzir tudo ao nome da classe. O segundo: o nosso programa conhece os robôs pelo nome que passou ao iniciador, e a resposta tem de voltar com esse mesmo nome.

**Crie o arquivo** `src/main/java/campeonato/robocode/ControleDeBatalha.java`:

<!-- arquivo: src/main/java/campeonato/robocode/ControleDeBatalha.java | criar -->
```java
package campeonato.robocode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Roda uma batalha do Robocode e entende o que ela escreve. */
public final class ControleDeBatalha {

    /** O resultado de um robô: o nome com que foi pedido, a colocação que o Robocode deu, os pontos e as rodadas ganhas. */
    public record Resultado(String robo, int colocacao, int pontos, int rodadasGanhas) {
    }

    /** O que uma batalha produz: a classificação, com o vencedor primeiro, e a gravação dos turnos. */
    public record Desfecho(List<Resultado> classificacao, Gravacao gravacao) {
    }

    /**
     * A ordem da classificação: quem ganhou mais rodadas fica na frente, e os pontos desempatam.
     * A colocação que o Robocode informa não é usada, porque ela nem sempre combina com o placar.
     */
    static final Comparator<Resultado> POR_RODADAS_E_PONTOS = Comparator.comparingInt(Resultado::rodadasGanhas).reversed()
            .thenComparing(Comparator.comparingInt(Resultado::pontos).reversed());

    private ControleDeBatalha() {
    }

    /**
     * O nome da classe de um robô, sem o que o Robocode acrescenta: o asterisco dos robôs soltos,
     * a versão ("equipe.Robo 1.0") ou o número da instância ("equipe.Robo (1)").
     */
    static String nomeDaClasse(String nomeRelatado) {
        return nomeRelatado.trim().split("[ (]")[0].replaceAll("\\*$", "");
    }

    /**
     * Lê as linhas de resultado, "colocação, nome, pontos e rodadas ganhas" separados por tabulação, e devolve a
     * classificação com o vencedor primeiro. As outras linhas são ignoradas.
     *
     * @param robos os nomes dos robôs como foram passados ao iniciador
     */
    static List<Resultado> lerClassificacao(List<String> saida, List<String> robos) {
        List<Resultado> resultados = new ArrayList<>();
        for (String linha : saida) {
            String[] campos = linha.split("\t");
            if (campos.length < 4 || !campos[0].trim().matches("[0-9]+")) {
                continue;
            }
            String relatado = nomeDaClasse(campos[1]);
            for (String robo : robos) {
                boolean ehEsteRobo = nomeDaClasse(robo).equals(relatado);
                boolean jaTemResultado = resultados.stream().anyMatch(resultado -> resultado.robo().equals(robo));
                if (ehEsteRobo && !jaTemResultado) {
                    resultados.add(new Resultado(robo, Integer.parseInt(campos[0].trim()), Integer.parseInt(campos[2].trim()), Integer.parseInt(campos[3].trim())));
                    break;
                }
            }
        }
        resultados.sort(POR_RODADAS_E_PONTOS);
        return resultados;
    }
}
```

Vale explicar três coisas:

- O `Comparator` diz como comparar dois resultados para ordenar uma lista. `Comparator.comparingInt(Resultado::rodadasGanhas)` compara pelo número de rodadas; `.reversed()` inverte a ordem para que o **maior** venha primeiro; `.thenComparing(...)` só é consultado quando o primeiro critério empata. `Resultado::rodadasGanhas` é uma **referência a método**: um jeito curto de escrever "a função que, dado um `Resultado`, devolve o seu `rodadasGanhas()`".
- A expressão regular `[ (]` casa com "um espaço ou um parêntese", e o `split` corta o nome no primeiro deles, ficando só com o que vem antes. O `\\*$` casa com "um asterisco no fim do texto", que o `replaceAll` apaga. Assim `aluno.Vigia*`, `team.Robot 1.0` e `team.Robot (1)` viram `aluno.Vigia` e `team.Robot`.
- `resultados.stream().anyMatch(...)` pergunta se **algum** resultado da lista já atende à condição. A seta `resultado -> resultado.robo().equals(robo)` é uma **expressão lambda**: uma função escrita no próprio lugar onde é usada, com o parâmetro à esquerda da seta e o corpo à direita. O uso do `jaTemResultado` evita que dois robôs com o mesmo nome de classe ganhem o mesmo resultado duas vezes.

Os métodos `nomeDaClasse` e `lerClassificacao` não têm `public`: são visíveis apenas para as classes do mesmo pacote, o que basta para os testes e esconde detalhes do resto do programa.

Teste-os. **Crie o arquivo** `src/test/java/campeonato/robocode/ControleDeBatalhaTest.java`:

<!-- arquivo: src/test/java/campeonato/robocode/ControleDeBatalhaTest.java | criar -->
```java
package campeonato.robocode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ControleDeBatalhaTest {

    @Test
    void reduzONomeAoNomeDaClasse() {
        assertEquals("aluno.Vigia", ControleDeBatalha.nomeDaClasse("aluno.Vigia*"));
        assertEquals("equipe.Robo", ControleDeBatalha.nomeDaClasse("equipe.Robo 1.0"));
        assertEquals("equipe.Robo", ControleDeBatalha.nomeDaClasse("equipe.Robo (1)"));
        assertEquals("sample.Walls", ControleDeBatalha.nomeDaClasse(" sample.Walls "));
    }

    @Test
    void quemGanhouMaisRodadasFicaNaFrenteMesmoComColocacaoTrocada() {
        List<String> saida = List.of(
                "#F 800 600 3",
                "2\tsample.SpinBot\t370\t2",
                "1\taluno.Vigia*\t192\t1",
                "batalha simulada em 507 ms");
        List<ControleDeBatalha.Resultado> classificacao = ControleDeBatalha.lerClassificacao(saida, List.of("aluno.Vigia*", "sample.SpinBot"));
        assertEquals(2, classificacao.size());
        assertEquals("sample.SpinBot", classificacao.get(0).robo());
        assertEquals(2, classificacao.get(0).rodadasGanhas());
        assertEquals("aluno.Vigia*", classificacao.get(1).robo());
    }

    @Test
    void osPontosDesempatamRodadasIguais() {
        List<String> saida = List.of("1\tsample.Walls\t100\t1", "2\tsample.Crazy\t250\t1");
        List<ControleDeBatalha.Resultado> classificacao = ControleDeBatalha.lerClassificacao(saida, List.of("sample.Walls", "sample.Crazy"));
        assertEquals("sample.Crazy", classificacao.get(0).robo());
    }

    @Test
    void ignoraRobosQueNaoForamPedidos() {
        List<String> saida = List.of("1\tsample.Walls\t100\t3", "2\tsample.Intruso\t10\t0");
        List<ControleDeBatalha.Resultado> classificacao = ControleDeBatalha.lerClassificacao(saida, List.of("sample.Walls"));
        assertEquals(1, classificacao.size());
    }
}
```

<!-- testar -->

## Passo 6 — Quanto da batalha já passou

O programa vai mostrar uma barra de progresso enquanto o iniciador simula. Mas o iniciador não diz "estou em 40%": só escreve linhas. Precisamos **deduzir** o progresso do que ele escreve.

Uma rodada acaba quando um robô perde toda a energia. Logo, o progresso de uma rodada é o quanto o robô **mais fraco** já perdeu: com 100 de energia, 0%; com 25, 75%; com 0, 100%. O progresso da batalha é o número de rodadas completas somado ao da rodada em andamento, dividido pelo total de rodadas. Dois cuidados: o progresso **nunca volta** (um robô pode ganhar energia ao acertar um tiro, e a barra não deve recuar), e o total de rodadas vem na linha `#F`.

**Crie o arquivo** `src/main/java/campeonato/robocode/ProgressoDaBatalha.java`:

<!-- arquivo: src/main/java/campeonato/robocode/ProgressoDaBatalha.java | criar -->
```java
package campeonato.robocode;

/**
 * Quanto de uma batalha já foi simulada, deduzido das linhas que o iniciador escreve. Uma rodada acaba
 * quando um robô perde toda a energia, então o progresso da rodada é o quanto o robô mais fraco perdeu;
 * o da batalha são as rodadas completas mais a que está rodando, sobre o total. Nunca diminui.
 */
public final class ProgressoDaBatalha {

    /** A energia com que todo robô começa uma rodada. */
    static final double ENERGIA_CHEIA = 100;

    private int rodadas = 1;
    private int rodadaAtual;
    private double naRodada;
    private double feito;

    /**
     * Recebe uma linha da saída: "#F" informa o total de rodadas, "#T" a energia dos robôs em um turno,
     * e qualquer outra coisa é ignorada.
     *
     * @return o progresso agora, de 0 a 1
     */
    public double aceitar(String linha) {
        if (linha.startsWith("#F ")) {
            rodadas = Math.max(1, lerRodadas(linha.substring(3).split(" ")));
        } else if (linha.startsWith("#T ")) {
            lerTurno(linha.substring(3));
        }
        return feito;
    }

    /** A batalha acabou: tudo está feito. */
    public double concluir() {
        feito = 1;
        return feito;
    }

    /** As rodadas de "largura altura rodadas", ou as que já eram conhecidas se a linha não for assim. */
    private int lerRodadas(String[] campos) {
        try {
            return campos.length > 2 ? Integer.parseInt(campos[2].trim()) : rodadas;
        } catch (NumberFormatException naoEhNumero) {
            return rodadas;
        }
    }

    /** Lê "rodada turno|robô;robô|tiros", sendo cada robô "índice x y corpo canhão radar energia estado". */
    private void lerTurno(String corpo) {
        String[] partes = corpo.split("\\|", -1);
        if (partes.length < 2 || partes[1].isEmpty()) {
            return;
        }
        try {
            int atual = Integer.parseInt(partes[0].split(" ")[0]);
            if (atual != rodadaAtual) {
                rodadaAtual = atual;
                naRodada = 0;
            }
            double maisFraco = ENERGIA_CHEIA;
            for (String robo : partes[1].split(";")) {
                maisFraco = Math.min(maisFraco, Double.parseDouble(robo.split(" ")[6]));
            }
            naRodada = Math.max(naRodada, 1 - Math.max(0, maisFraco) / ENERGIA_CHEIA);
            feito = Math.max(feito, Math.min(1, (rodadaAtual + naRodada) / rodadas));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException naoEhUmTurno) {
            // uma linha que não é um turno não diz nada sobre o progresso
        }
    }
}
```

O `Math.max(feito, ...)` é o que impede a barra de recuar: o novo valor só vale se for maior que o anterior. O `catch` com `|` captura dois tipos de erro de uma vez.

Teste. **Crie o arquivo** `src/test/java/campeonato/robocode/ProgressoDaBatalhaTest.java`:

<!-- arquivo: src/test/java/campeonato/robocode/ProgressoDaBatalhaTest.java | criar -->
```java
package campeonato.robocode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProgressoDaBatalhaTest {

    private static String turno(int rodada, double energiaDoPrimeiro, double energiaDoSegundo) {
        return "#T " + rodada + " 5|0 100.000 100.000 0.000 0.000 0.000 " + energiaDoPrimeiro + " 0;1 200.000 200.000 0.000 0.000 0.000 " + energiaDoSegundo + " 0|";
    }

    @Test
    void comecaEmZero() {
        ProgressoDaBatalha progresso = new ProgressoDaBatalha();
        assertEquals(0.0, progresso.aceitar("#F 800 600 2"), 0.0001);
    }

    @Test
    void oRoboMaisFracoMedeARodada() {
        ProgressoDaBatalha progresso = new ProgressoDaBatalha();
        progresso.aceitar("#F 800 600 2");
        // numa batalha de 2 rodadas, metade da energia perdida na primeira vale 25%
        assertEquals(0.25, progresso.aceitar(turno(0, 100, 50)), 0.0001);
    }

    @Test
    void asRodadasCompletasContam() {
        ProgressoDaBatalha progresso = new ProgressoDaBatalha();
        progresso.aceitar("#F 800 600 2");
        progresso.aceitar(turno(0, 100, 0));
        assertEquals(0.5, progresso.aceitar(turno(1, 100, 100)), 0.0001);
    }

    @Test
    void nuncaVolta() {
        ProgressoDaBatalha progresso = new ProgressoDaBatalha();
        progresso.aceitar("#F 800 600 1");
        progresso.aceitar(turno(0, 20, 100));
        // o robô se curou: a barra não recua
        assertEquals(0.8, progresso.aceitar(turno(0, 90, 100)), 0.0001);
    }

    @Test
    void concluirCompletaEOutrasLinhasSaoIgnoradas() {
        ProgressoDaBatalha progresso = new ProgressoDaBatalha();
        assertEquals(0.0, progresso.aceitar("1\tsample.Walls\t100\t3"), 0.0001);
        assertEquals(0.0, progresso.aceitar("#T isto nao e um turno"), 0.0001);
        assertEquals(1.0, progresso.concluir(), 0.0001);
    }
}
```

<!-- testar -->

## Passo 7 — Iniciar o iniciador

Agora a peça central: montar o comando que usamos no capítulo 4 e executá-lo de dentro do Java. A classe `ProcessBuilder` faz isso: recebe a lista de partes do comando, inicia o programa e dá acesso à sua saída.

O comando é o mesmo de antes:

```text
java @iniciador/opcoes.txt -cp "$ROBOCODE/libs/*:iniciador" IniciadorDeBatalha $ROBOCODE 3 robô robô
```

Em Java cada parte vira um item da lista, **sem aspas**: quem separa as partes é a lista, não o espaço. Duas escolhas: o `java` que executa o nosso programa é o mesmo que executa o iniciador (`System.getProperty("java.home")` diz onde ele está instalado), assim não dependemos de existir um `java` no `PATH`; e o separador do classpath muda conforme o sistema (`:` ou `;`), então usamos `File.pathSeparator`.

**Acrescente ao final da classe** `ControleDeBatalha`:

<!-- arquivo: src/main/java/campeonato/robocode/ControleDeBatalha.java | acrescentar -->
```java

    /** O comando que inicia o iniciador: o mesmo do capítulo 4, uma parte por item. */
    static List<String> montarComando(List<String> robos, String pastaDoRobocode, int rodadas) {
        List<String> comando = new ArrayList<>();
        comando.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        comando.add("@" + Path.of("iniciador", "opcoes.txt"));
        comando.add("-cp");
        comando.add(Path.of(pastaDoRobocode, "libs") + File.separator + "*" + File.pathSeparator + "iniciador");
        comando.add("IniciadorDeBatalha");
        comando.add(pastaDoRobocode);
        comando.add(String.valueOf(rodadas));
        comando.addAll(robos);
        return comando;
    }

    /**
     * Roda a batalha e espera ela acabar. Fica parado esse tempo todo, por isso não deve ser chamado
     * pela linha de execução que desenha a janela (veja o capítulo 6).
     *
     * @param aoProgredir chamado a cada centésimo a mais de progresso, de 0 a 1, e com 1 no fim
     * @throws IOException se o iniciador não puder ser iniciado ou a batalha não trouxer o resultado esperado
     */
    public static Desfecho rodar(List<String> robos, String pastaDoRobocode, int rodadas, DoubleConsumer aoProgredir) throws IOException, InterruptedException {
        ProcessBuilder construtor = new ProcessBuilder(montarComando(robos, pastaDoRobocode, rodadas)).redirectErrorStream(true);
        Process processo = construtor.start();

        List<String> saida = new ArrayList<>();
        ProgressoDaBatalha progresso = new ProgressoDaBatalha();
        double avisado = -1;
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                saida.add(linha);
                double agora = progresso.aceitar(linha);
                if (agora - avisado >= 0.01) {
                    avisado = agora;
                    aoProgredir.accept(agora);
                }
            }
        }
        processo.waitFor();
        aoProgredir.accept(progresso.concluir());

        List<Resultado> classificacao = lerClassificacao(saida, robos);
        if (classificacao.size() != robos.size()) {
            List<String> mensagens = saida.stream().filter(linha -> !linha.startsWith("#")).toList();
            throw new IOException("A batalha não retornou o resultado esperado:\n" + String.join("\n", mensagens));
        }
        return new Desfecho(classificacao, Gravacao.ler(saida));
    }
```

E os `import` dos tipos novos, **no topo do arquivo**:

<!-- arquivo: src/main/java/campeonato/robocode/ControleDeBatalha.java | importar -->
```java
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.DoubleConsumer;
```

Pontos importantes:

- `redirectErrorStream(true)` junta a saída de erro do iniciador (a medida de tempo, por exemplo) com a saída normal, num fluxo só. As linhas que não nos interessam são ignoradas pelos leitores.
- `processo.getInputStream()` é a **saída** do iniciador vista pelo nosso programa (o nome parece trocado, mas é o ponto de vista de quem lê). Ela entrega bytes; o `InputStreamReader` os converte em caracteres, e o `BufferedReader` entrega uma linha por vez com `readLine()`, que devolve `null` quando o programa termina.
- O `try (...)` com um recurso entre parênteses é um **try-with-resources**: ao sair do bloco, por qualquer caminho, o leitor é fechado sozinho.
- `DoubleConsumer` é uma interface do Java para "uma função que recebe um `double` e não devolve nada". Quem chama o método passa uma lambda, como `progresso -> System.out.println(progresso)`. É assim que a classe avisa o mundo de fora sem saber quem vai usar a informação: uma barra de progresso, um texto no terminal.
- `InterruptedException` é lançada por `waitFor()` se a linha de execução for interrompida enquanto espera. O Java obriga a declarar (ou tratar) esse tipo de erro: é uma exceção **verificada**. A declaração `throws IOException, InterruptedException` diz a quem chama que ele terá de lidar com isso.

Compile:

<!-- compilar -->

## Passo 8 — Rodar de verdade

Falta ver tudo funcionando, com o iniciador e o Robocode reais. Uma classe pequena de demonstração chama o `rodar` e imprime o que recebeu. **Crie o arquivo** `src/main/java/campeonato/robocode/Demonstracao.java`:

<!-- arquivo: src/main/java/campeonato/robocode/Demonstracao.java | criar -->
```java
package campeonato.robocode;

import java.util.List;
import java.util.Locale;

/** Roda uma batalha de verdade e mostra o que o programa entendeu dela. */
public class Demonstracao {

    public static void main(String[] args) throws Exception {
        String pastaDoRobocode = args[0];
        List<String> robos = List.of("aluno.Vigia*", "sample.SpinBot");

        ControleDeBatalha.Desfecho desfecho = ControleDeBatalha.rodar(robos, pastaDoRobocode, 3,
                progresso -> System.out.printf(Locale.ROOT, "progresso: %3.0f%%%n", progresso * 100));

        System.out.println("Classificação:");
        for (ControleDeBatalha.Resultado resultado : desfecho.classificacao()) {
            System.out.println("  " + resultado.robo() + "   rodadas ganhas: " + resultado.rodadasGanhas() + "   pontos: " + resultado.pontos());
        }

        Gravacao gravacao = desfecho.gravacao();
        System.out.println("Campo: " + gravacao.largura() + " x " + gravacao.altura());
        for (int rodada = 0; rodada < gravacao.rodadas().size(); rodada++) {
            System.out.println("Rodada " + rodada + ": " + gravacao.rodadas().get(rodada).size() + " turnos");
        }
    }
}
```

O `%3.0f%%%n` do `printf` quer dizer: um número decimal com largura 3 e nenhuma casa depois da vírgula (`%3.0f`), um sinal de porcentagem (`%%`, porque o `%` sozinho é especial) e uma quebra de linha (`%n`).

Compile o projeto e rode a demonstração. O `target/classes` é a pasta onde o Maven coloca o resultado da compilação. No Linux e no macOS:

<!-- executar: | espera: Rodada 2 | tempo: 180 -->
```bash
mvn -q compile
java -cp target/classes campeonato.robocode.Demonstracao "$ROBOCODE"
```

No Prompt de Comando do Windows:

```text
mvn -q compile
java -cp target\classes campeonato.robocode.Demonstracao "%ROBOCODE%"
```

A saída é parecida com esta:

```text
progresso:   0%
progresso:   1%
progresso:   2%
...
progresso: 100%
Classificação:
  aluno.Vigia*   rodadas ganhas: 2   pontos: 277
  sample.SpinBot   rodadas ganhas: 1   pontos: 251
Campo: 800 x 600
Rodada 0: 708 turnos
Rodada 1: 1512 turnos
Rodada 2: 1108 turnos
```

Os números variam a cada batalha. O que importa é a forma: o progresso sobe de 0 a 100, o vencedor aparece na frente pelas rodadas ganhas e a gravação tem milhares de turnos, que o capítulo 6 vai desenhar.

Se em vez disso aparecer `A batalha não retornou o resultado esperado` seguido de `Não foi possível localizar nem carregar a classe principal IniciadorDeBatalha`, o programa não achou o iniciador. Ele procura `iniciador/opcoes.txt` e a pasta `iniciador` a partir da pasta onde o comando foi digitado, então rode sempre de dentro de `mini-campeonato`, e confira se o `IniciadorDeBatalha.class` existe (capítulo 4, passo 4). É a mensagem de erro do método `rodar` fazendo o seu trabalho: ela repete tudo o que o iniciador escreveu e que não era uma linha da gravação.

## O que construímos

```text
src/main/java/campeonato/robocode/
├── Gravacao.java             lê o texto do iniciador e guarda cada turno (records)
├── ControleDeBatalha.java    inicia o iniciador, lê a saída, devolve classificação e gravação
├── ProgressoDaBatalha.java   deduz de 0 a 1 o quanto da batalha já foi simulada
└── Demonstracao.java         roda uma batalha de verdade e imprime o que entendeu

src/test/java/campeonato/robocode/
├── GravacaoTest.java
├── ControleDeBatalhaTest.java
└── ProgressoDaBatalhaTest.java
```

## Exercícios

**Fácil**

1. Mude a `Demonstracao` para rodar `sample.Walls` contra `sample.Crazy` em 5 rodadas. Confira se a soma dos turnos impressos bate com os números que o iniciador escreve nas linhas `#E`.
2. No `GravacaoTest`, acrescente um teste que confira a cor do **segundo** robô, que não teve linha `#C` na gravação de exemplo. Qual cor ele deve ter? Dica: veja as constantes `COR_PADRAO_...`.

**Médio**

3. Acrescente ao `ControleDeBatalha` um método `vencedor(List<Resultado>)` que devolva o nome do primeiro da classificação, e teste-o. Pense: o que ele deve fazer com uma lista vazia?
4. A classificação desempata por pontos. Escreva um teste em que dois robôs têm as mesmas rodadas e os mesmos pontos e veja qual ficou na frente. Isso é bom? O que deveria acontecer num empate completo no campeonato?
5. Faça a `Demonstracao` imprimir, para cada rodada, a energia final de cada robô (o último `Turno` da lista). Dica: `gravacao.rodadas().get(rodada)` é a lista de turnos, e `get(lista.size() - 1)` pega o último.

**Difícil**

6. O `ProgressoDaBatalha` supõe que todo robô começa com 100 de energia. Isso nem sempre vale: segundo a documentação do Robocode (pasta `javadoc`, página da interface `Droid`), um *droid* começa com 20 pontos de energia a mais. Mude a classe para guardar a energia do primeiro turno de cada robô em cada rodada e usá-la no lugar da constante.
7. Faça o `rodar` desistir se o iniciador demorar mais que um limite (por exemplo, 120 segundos): use `processo.waitFor(120, TimeUnit.SECONDS)` e, se ele devolver `false`, chame `processo.destroyForcibly()` e lance uma `IOException`. Atenção: a leitura da saída só termina quando o programa termina, então o limite de tempo não pode ficar depois do laço. Dica: uma segunda linha de execução (`Thread`) que espera o limite e então chama `processo.destroyForcibly()`; quando o iniciador morre, o `readLine` devolve `null` e o laço termina.

## Perguntas para fixação

1. Um record e uma classe comum com campos `final` e métodos de leitura são parecidos. O que o record economiza, e que restrição ele impõe?
2. Por que a leitura da gravação usa `try`/`catch` por linha, e não um só em volta do laço inteiro?
3. Por que o `ControleDeBatalha.rodar` não pode ser chamado pela linha de execução que desenha a janela?
4. O que aconteceria com a barra de progresso se, em vez de `Math.max(feito, ...)`, o código simplesmente guardasse o último valor calculado?

<details>
<summary>Respostas</summary>

1. O record escreve sozinho o construtor, os métodos de leitura, `equals`, `hashCode` e `toString`. A restrição é a imutabilidade: depois de criado, nenhum campo muda, e por isso ele não serve para objetos que precisam mudar de estado.
2. Com o `try`/`catch` por linha, uma linha estragada é ignorada e a leitura continua com as outras. Com um só em volta do laço, o primeiro erro interromperia a leitura inteira e a gravação ficaria incompleta.
3. Porque ele fica parado até a batalha acabar, e a linha de execução do JavaFX é a mesma que trata os cliques e redesenha a tela. Parada, a janela congelaria durante todo esse tempo. O capítulo 6 resolve rodando-o em segundo plano.
4. A barra recuaria sempre que um robô ganhasse energia, por exemplo ao acertar um tiro: o robô mais fraco passaria a ter mais energia que antes e o progresso calculado diminuiria.

</details>

No próximo capítulo, a gravação vira movimento: os tanques do capítulo 3 passam a seguir cada turno dessa batalha.
