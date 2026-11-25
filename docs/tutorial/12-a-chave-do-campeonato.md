# Capítulo 12 — A chave do campeonato

## Objetivo

Decidir quem enfrenta quem. Neste capítulo você escreve a chave de **eliminação simples**: quem perde uma partida está fora, e o último que sobra é o campeão. A chave funciona para qualquer número de robôs, e **todo robô joga em toda rodada**: quando o número não é uma potência de dois, o robô que fica sem par joga um **desafio**, em vez de passar de rodada sem lutar. Ela tem tudo o que o programa precisa para conduzir o campeonato: qual é a próxima partida, o registro de cada resultado e o campeão. No fim, uma simulação joga um campeonato inteiro, com vencedores sorteados, e mostra o resultado no terminal. A repescagem vem no capítulo seguinte.

Este capítulo não usa JavaFX nem o Robocode: é só lógica, em Java puro, e por isso pode ser testada de cima a baixo com testes automáticos.

## Passo 1 — Como funciona uma chave

Num campeonato de eliminação simples, os robôs jogam em **rodadas**. Em cada partida, um robô vence e segue para a rodada seguinte, e o outro está eliminado. Com 8 robôs, a primeira rodada tem 4 partidas, a segunda tem 2 e a terceira, a final, tem 1: ao todo são 7 partidas, uma a menos que o número de robôs, porque cada partida elimina exatamente um robô e só o campeão não é eliminado.

Cada partida recebe um **código**: `V` (de "vencedores", a chave principal), o número da rodada, um hífen e a posição na rodada. A segunda partida da primeira rodada é a `V1-2`, e a final de uma chave de 8 é a `V3-1`.

Para 8 robôs, numerados de 1 a 8 pela ordem de força, a chave é esta:

```text
Rodada 1                  Rodada 2                  Rodada 3

robo1 ─┐
       ├─ V1-1 ─┐
robo8 ─┘        │
                ├─ V2-1 ─┐
robo4 ─┐        │        │
       ├─ V1-2 ─┘        │
robo5 ─┘                 ├─ V3-1 ─ campeão
robo2 ─┐                 │
       ├─ V1-3 ─┐        │
robo7 ─┘        │        │
                ├─ V2-2 ─┘
robo3 ─┐        │
       ├─ V1-4 ─┘
robo6 ─┘
```

O número de cada robô é a sua **semente**: a posição dele na lista de participantes, com a semente 1 sendo a melhor. Repare em quem enfrenta quem na primeira rodada: 1 contra 8, 4 contra 5, 2 contra 7 e 3 contra 6. A ordem espalha as sementes para os melhores só se encontrarem no fim: o 1 e o 2 só se enfrentam na final, e o 1 só encontra o 3 ou o 4 na semifinal. Os campeonatos de tênis e de futebol, com as suas chaves de sorteio, usam a mesma ideia.

E se o número de robôs não for 2, 4, 8, 16, e assim por diante? A regra da chave é que **todo robô joga em toda rodada**, e que **ninguém avança sem derrotar alguém**: não existe "folga", o robô que passa de rodada sem lutar. Em vez disso, quando o número de robôs de uma rodada é ímpar, sobra um robô sem par, e ele joga um **desafio**: enfrenta o **perdedor da primeira partida da rodada**, que já tinha lutado e perdido. Quem vence o desafio avança, e quem perde o desafio está fora.

Com 5 robôs, a primeira rodada tem duas partidas normais, `V1-1` (1 contra 4) e `V1-2` (5 contra 2), e o robô 3 fica sem par: ele joga a `V1-3`, o desafio, contra o perdedor da `V1-1`. Os três vencedores (da `V1-1`, da `V1-2` e do desafio) seguem, e como três também é ímpar, a segunda rodada repete a ideia: a `V2-1` é uma partida normal, e a `V2-2` é o desafio entre o terceiro robô e o perdedor da `V2-1`. A final, a `V3-1`, tem dois robôs. A chave de 5 joga 6 partidas, e não 4: cada desafio é uma partida a mais, que dá uma segunda chance a quem perdeu a primeira partida da rodada.

Um último ponto, de projeto. A chave guardará o menos possível: só quem são os participantes, em que ordem, e os resultados das partidas já jogadas. Tudo o mais (quem enfrenta quem, qual é a próxima partida, quem é o campeão) é **calculado** a partir disso. Para guardar um campeonato em andamento, bastará guardar a lista de participantes e a lista de resultados, e para continuar depois, montar a chave de novo e repetir os resultados. O capítulo 15 vai usar essa vantagem.

## Passo 2 — A situação de uma partida

Uma partida passa por três situações:

- `AGUARDANDO`: espera o resultado de partidas anteriores, e então não se sabe ainda quem joga.
- `PRONTA`: os dois participantes já são conhecidos, e a partida pode ser jogada.
- `FINALIZADA`: já foi jogada.

Em Java, um conjunto fixo de valores como esse é um `enum`, o mesmo tipo do `Tipo` das partículas do capítulo 9. Um `enum` também pode guardar dados e ter métodos. Aqui cada situação tem o seu `texto`, para escrever na tela, passado ao **construtor** do `enum` entre parênteses (`AGUARDANDO("aguardando")`), e um método `decidida`, que diz se a partida já está resolvida: só a `FINALIZADA` está. **Crie o arquivo** `src/main/java/campeonato/torneio/Situacao.java`:

<!-- arquivo: src/main/java/campeonato/torneio/Situacao.java | criar -->
```java
package campeonato.torneio;

/** Em que pé está uma partida da chave. */
public enum Situacao {

    /** Espera o resultado de partidas anteriores. */
    AGUARDANDO("aguardando"),
    /** Os dois participantes são conhecidos: a partida pode ser jogada. */
    PRONTA("pronta"),
    /** Já foi jogada. */
    FINALIZADA("finalizada");

    private final String texto;

    Situacao(String texto) {
        this.texto = texto;
    }

    /** O nome da situação, para escrever na tela. */
    public String texto() {
        return texto;
    }

    /** Diz se a situação é final: a partida já foi jogada. */
    public boolean decidida() {
        return this == FINALIZADA;
    }
}
```

Nos `enum`, compara-se com `==`, e não com `equals`: só existe um objeto para cada valor, então `this == FINALIZADA` basta.

A chave completa, do capítulo 13, terá três partes: a chave principal, a repescagem e a grande final. Cada partida precisa saber a qual parte pertence, e o `enum` `Lado` diz. Neste capítulo só existe o `PRINCIPAL`. **Crie o arquivo** `src/main/java/campeonato/torneio/Lado.java`:

<!-- arquivo: src/main/java/campeonato/torneio/Lado.java | criar -->
```java
package campeonato.torneio;

/** A parte da chave a que uma partida pertence: a chave principal, a repescagem ou a grande final. */
public enum Lado {

    /** A chave principal. */
    PRINCIPAL,
    /** A repescagem, onde quem perdeu na principal ainda pode continuar. */
    REPESCAGEM,
    /** A grande final, entre o campeão da principal e o da repescagem. */
    FINAL
}
```

## Passo 3 — Vagas e partidas

Uma partida tem duas **vagas**, e cada vaga é ocupada de um de dois jeitos:

- por uma **semente**, na primeira rodada: o participante já é conhecido;
- pelo **vencedor** (ou, no desafio, pelo **perdedor**) de outra partida: o participante só será conhecido quando essa partida terminar.

Em vez de guardar um nome que ainda não existe, a vaga guarda **de onde** o participante vai vir: a partida de `origem`, e se ela fica com o vencedor (`doVencedor` verdadeiro) ou com o perdedor. O método `resolver` olha a origem e, se ela já está decidida, preenche a vaga. Enquanto o `participante` é `null`, ainda não se sabe quem é. O `descrever` devolve o texto para a tela: o nome do participante ou, enquanto não se sabe, de onde ele virá, como `Vencedor de V1-2`.

As vagas do perdedor servem ao desafio, já neste capítulo, e ao capítulo 13, para a repescagem; por isso `doVencedor` existe desde já.

A `Partida` guarda o código, o lado, a rodada, a posição, as duas vagas, se é um `desafio` e o resultado. Há uma conta nova, a `profundidade`: **quantas partidas, em sequência, precisam ser jogadas antes desta**. Na primeira rodada é 1; uma partida cujas vagas vêm de partidas de profundidade 1 tem profundidade 2, e assim por diante. É ela que decide a ordem em que as partidas são jogadas. A conta é `1 + max(profundidade de a, profundidade de b)`, e a profundidade de uma vaga sem origem é 0.

O método `atualizar` da partida faz a transição de situação: se ela ainda aguarda, pede às duas vagas que se resolvam. Se as duas ficarem com participante, a partida está `PRONTA`.

Os campos não são `private`, para a `Vaga`, a `Partida` e a `Chave`, do mesmo pacote, mexerem neles direto, como no `Particula` do capítulo 9. Fora do pacote, só se lê, pelos métodos de mesmo nome dos campos. As duas classes se referem uma à outra (uma vaga conhece a partida de origem, uma partida tem duas vagas), então o projeto só compila depois de as duas existirem. **Crie o arquivo** `src/main/java/campeonato/torneio/Vaga.java`:

<!-- arquivo: src/main/java/campeonato/torneio/Vaga.java | criar -->
```java
package campeonato.torneio;

/** Um dos dois lugares de uma partida: ocupado por uma semente do campeonato ou pelo vencedor (ou perdedor) de outra partida. */
public final class Vaga {

    /** De onde o participante vem: a partida anterior, ou null na primeira rodada. */
    final Partida origem;
    /** Verdadeiro se a vaga fica com o vencedor da partida de origem, falso se fica com o perdedor. */
    final boolean doVencedor;

    String participante;

    /** Uma vaga ocupada por uma semente do campeonato. */
    Vaga(String semente) {
        this.origem = null;
        this.doVencedor = false;
        this.participante = semente;
    }

    /** Uma vaga que fica com o vencedor, ou com o perdedor, de outra partida. */
    Vaga(Partida origem, boolean doVencedor) {
        this.origem = origem;
        this.doVencedor = doVencedor;
    }

    /** O participante, ou null se ainda não se sabe quem é. */
    public String participante() {
        return participante;
    }

    /** A partida de onde o participante vem, ou null na primeira rodada. */
    public Partida origem() {
        return origem;
    }

    /** Verdadeiro se a vaga fica com o vencedor da partida de origem, e não com o perdedor. */
    public boolean doVencedor() {
        return doVencedor;
    }

    /** O que escrever na tela: o participante, ou de onde ele vai vir, como "Vencedor de V1-2". */
    public String descrever() {
        if (participante != null) {
            return participante;
        }
        return (doVencedor ? "Vencedor de " : "Perdedor de ") + origem.codigo();
    }

    /** Preenche a vaga com o vencedor, ou o perdedor, da partida de origem, quando essa partida já estiver decidida. */
    void resolver() {
        if (participante == null && origem != null && origem.situacao.decidida()) {
            participante = doVencedor ? origem.vencedor : origem.perdedor;
        }
    }
}
```

A `Vaga` tem **dois construtores**, com parâmetros diferentes: o Java escolhe o certo pelo que é passado. `new Vaga("robo1")` é uma semente, e `new Vaga(partida, true)` pega o vencedor de uma partida. **Crie o arquivo** `src/main/java/campeonato/torneio/Partida.java`:

<!-- arquivo: src/main/java/campeonato/torneio/Partida.java | criar -->
```java
package campeonato.torneio;

/** Uma partida da chave: as duas vagas, a rodada, o lugar na chave e o resultado. */
public final class Partida {

    final String codigo;
    final Lado lado;
    final int rodada;
    final int posicao;
    final Vaga a;
    final Vaga b;
    final int profundidade;

    /** Verdadeiro no desafio: a partida de quem ficou sem par, contra o perdedor da primeira partida da rodada. */
    boolean desafio;
    Situacao situacao = Situacao.AGUARDANDO;
    String vencedor;
    String perdedor;
    int pontosDoVencedor;
    int pontosDoPerdedor;

    /** Uma partida entre duas vagas, na rodada e na posição dadas do seu lado da chave. */
    Partida(String codigo, Lado lado, int rodada, int posicao, Vaga a, Vaga b) {
        this.codigo = codigo;
        this.lado = lado;
        this.rodada = rodada;
        this.posicao = posicao;
        this.a = a;
        this.b = b;
        this.profundidade = 1 + Math.max(profundidadeDe(a), profundidadeDe(b));
    }

    /** Quantas partidas vêm antes de uma vaga, em sequência: zero na primeira rodada. */
    private static int profundidadeDe(Vaga vaga) {
        return vaga.origem == null ? 0 : vaga.origem.profundidade;
    }

    /** O código da partida: V (chave principal) ou R (repescagem), seguido de rodada-posição, ou F1 para a grande final. */
    public String codigo() {
        return codigo;
    }

    public Lado lado() {
        return lado;
    }

    /** A rodada, contada de 1, dentro do lado. */
    public int rodada() {
        return rodada;
    }

    /** A posição, contada de 1, dentro da rodada. */
    public int posicao() {
        return posicao;
    }

    public Vaga a() {
        return a;
    }

    public Vaga b() {
        return b;
    }

    /** Diz se a partida é um desafio. */
    public boolean desafio() {
        return desafio;
    }

    /** Quantas partidas, no máximo, precisam ser jogadas antes desta: serve para decidir a ordem em que se joga. */
    public int profundidade() {
        return profundidade;
    }

    public Situacao situacao() {
        return situacao;
    }

    public String vencedor() {
        return vencedor;
    }

    public String perdedor() {
        return perdedor;
    }

    public int pontosDoVencedor() {
        return pontosDoVencedor;
    }

    public int pontosDoPerdedor() {
        return pontosDoPerdedor;
    }

    /** Diz se um participante joga esta partida. */
    public boolean envolve(String participante) {
        return participante != null && (participante.equals(a.participante) || participante.equals(b.participante));
    }

    /** Preenche as vagas quando as origens estão decididas e deixa a partida pronta quando os dois participantes são conhecidos. */
    void atualizar() {
        if (situacao != Situacao.AGUARDANDO) {
            return;
        }
        a.resolver();
        b.resolver();
        if (a.participante != null && b.participante != null) {
            situacao = Situacao.PRONTA;
        }
    }

    /** A partida escrita em uma linha: o código, as duas vagas e a situação. */
    public String descrever() {
        return String.format("%-5s %s x %s  (%s)", codigo, a.descrever(), b.descrever(), situacao.texto());
    }

    @Override
    public String toString() {
        return descrever();
    }
}
```

Alguns detalhes:

- O `participante.equals(a.participante)` compara textos. Em Java, `==` entre dois textos compara se são **o mesmo objeto**, e não se têm as mesmas letras. Para comparar o conteúdo, sempre `equals`. O `participante != null &&` vem antes porque chamar `equals` em `null` dá erro.
- `Math.max(a, b)` devolve o maior dos dois, e já apareceu no capítulo 8.
- O `String.format("%-5s %s x %s  (%s)", ...)` é o `String.format` do capítulo 1, agora com texto: cada `%s` é trocado por um texto, e o `%-5s` ocupa **5 colunas**, com o texto encostado à esquerda (o `-`). Assim os códigos `V1-1` e `V10-1` ficam alinhados quando as partidas são escritas uma embaixo da outra.
- O `@Override` em `toString` avisa o compilador de que este método **substitui** um que toda classe herda de `Object`. É o `toString` que o Java chama quando se escreve um objeto com `System.out.println` ou se junta com um texto.

<!-- compilar -->

## Passo 4 — Quem enfrenta quem

Primeiro o difícil: a ordem das sementes. Dado o tamanho da chave (uma potência de dois), `ordemDasSementes` devolve a lista das sementes na ordem em que ocupam a primeira rodada, de duas em duas: as duas primeiras se enfrentam, as duas seguintes se enfrentam, e assim por diante. A receita é construir a lista dobrando o tamanho:

1. Comece com `[1, 2]`: para uma chave de 2, a semente 1 enfrenta a 2.
2. Para dobrar a chave, some um à nova quantidade de sementes: com a chave indo de 2 para 4, o **total** é `2 × 2 + 1 = 5`. Cada semente `s` da lista antiga vira o par `s` e `total − s`, nessa ordem: o 1 vira `1, 4`, e o 2 vira `2, 3`. A lista nova é `[1, 4, 2, 3]`, e o 1 enfrenta o 4, e o 2 enfrenta o 3.
3. Para dobrar de novo, de 4 para 8, o total é `2 × 4 + 1 = 9`, e a lista vira `[1, 8, 4, 5, 2, 7, 3, 6]`.

Cada semente passa a ter como adversário o número que **soma o total** com ela, o que mantém as melhores sementes longe uma da outra. O código usa uma `List<Integer>` e só no fim copia para um vetor `int[]`, porque uma lista cresce e um vetor não. O `for (int semente : ordem)` percorre a lista, e o `List.of(1, 2)` é uma lista pronta, que não pode ser mudada: o `new ArrayList<>(List.of(1, 2))` copia para uma lista que pode.

**Crie o arquivo** `src/main/java/campeonato/torneio/Chave.java`. Por enquanto ele só tem o comentário que descreve a chave toda e a conta das sementes:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | criar -->
```java
package campeonato.torneio;

import java.util.ArrayList;
import java.util.List;

/**
 * A chave de um campeonato, para qualquer número de participantes.
 *
 * <p>Na eliminação simples, quem perde uma partida está fora. Todos jogam em toda rodada, e ninguém avança sem vencer alguém: quando o número
 * de participantes de uma rodada é ímpar, quem ficou sem par joga um desafio contra o perdedor da primeira partida da rodada.
 *
 * <p>A chave não guarda nada além dos resultados: ela é montada de novo a partir das sementes e dos resultados.
 */
public final class Chave {

    /** A ordem das sementes na primeira rodada, para as melhores só se encontrarem no fim: para 8, 1 8 4 5 2 7 3 6. */
    static int[] ordemDasSementes(int tamanho) {
        List<Integer> ordem = new ArrayList<>(List.of(1, 2));
        while (ordem.size() < tamanho) {
            // cada semente é emparelhada com a que soma, com ela, o total
            int total = ordem.size() * 2 + 1;
            List<Integer> proxima = new ArrayList<>();
            for (int semente : ordem) {
                proxima.add(semente);
                proxima.add(total - semente);
            }
            ordem = proxima;
        }
        int[] resultado = new int[tamanho];
        for (int i = 0; i < tamanho; i++) {
            resultado[i] = ordem.get(i);
        }
        return resultado;
    }
}
```

Os testes conferem as ordens de 2, 4 e 8 sementes, escritas à mão, e uma propriedade que vale para qualquer tamanho: todas as sementes aparecem uma vez, e em cada par, as duas somam `tamanho + 1`. O `assertArrayEquals` compara dois vetores elemento a elemento (o `assertEquals` compararia se são o mesmo objeto). **Crie o arquivo** `src/test/java/campeonato/torneio/ChaveTest.java`:

<!-- arquivo: src/test/java/campeonato/torneio/ChaveTest.java | criar -->
```java
package campeonato.torneio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ChaveTest {

    @Test
    void asSementesSeEspalham() {
        assertArrayEquals(new int[] { 1, 2 }, Chave.ordemDasSementes(2));
        assertArrayEquals(new int[] { 1, 4, 2, 3 }, Chave.ordemDasSementes(4));
        assertArrayEquals(new int[] { 1, 8, 4, 5, 2, 7, 3, 6 }, Chave.ordemDasSementes(8));
    }

    @Test
    void emQualquerTamanhoCadaParSomaOMesmoTotal() {
        for (int tamanho = 2; tamanho <= 64; tamanho *= 2) {
            int[] ordem = Chave.ordemDasSementes(tamanho);
            // todas as sementes aparecem, cada uma uma vez
            assertEquals(tamanho, Arrays.stream(ordem).distinct().count());
            for (int i = 0; i < tamanho; i += 2) {
                assertEquals(tamanho + 1, ordem[i] + ordem[i + 1]);
            }
        }
    }
}
```

<!-- testar -->

## Passo 5 — Montando as rodadas

Agora o construtor. A `Chave` recebe a lista de participantes **na ordem das sementes**, e faz:

1. **Confere** os participantes: pelo menos dois, todos diferentes e nenhum nulo. Se não, lança uma `IllegalArgumentException`, a exceção do Java para "o argumento que você passou não serve". O `throw new` interrompe o construtor, e quem o chamou recebe o erro. O `List.copyOf` guarda uma cópia que não pode ser mudada, e a chave não muda se quem a criou mexer na lista depois.
2. **Monta a primeira rodada** com as sementes, na ordem do passo 4. O tamanho da chave é a menor potência de dois que comporta todos os participantes (com 5 participantes, 8), e a ordem das sementes é a de uma chave desse tamanho, mas **sem as sementes que não existem**: com 5 participantes, a ordem `1 8 4 5 2 7 3 6` vira `1 4 5 2 3`.
3. **Emparelha** as vagas, rodada por rodada, com o método `emparelhar`: as duas primeiras jogam entre si, as duas seguintes jogam entre si, e assim por diante. Se sobra uma, ela joga o **desafio** contra o perdedor da primeira partida. Os vencedores de cada rodada são as vagas da rodada seguinte, e isso se repete até sobrar uma vaga só.

O `emparelhar` devolve três coisas de uma vez, e por isso há um **`record`** (o do capítulo 14 é igual): `Rodada` guarda as `partidas` da rodada, as vagas de quem `avancam` e as vagas de quem `caem`, os perdedores. Quem cai só é usado no capítulo 13, onde a repescagem os recebe: no desafio, o perdedor da primeira partida já voltou a jogar, e então a vaga de quem cai, nessa posição, passa a ser a do perdedor **do desafio**. O desafio tem a vaga de um perdedor: `new Vaga(primeira, false)`.

O código das partidas é `"V" + rodada + "-" + (indice + 1)`, e o desafio vem depois das partidas normais, com o próximo número da rodada. Cada partida é guardada em dois lugares: na lista `partidas`, em ordem de criação, e no `porCodigo`, um `Map` (o `HashMap` do capítulo 6) que acha uma partida pelo código.

A ordem de criação importa: uma partida é sempre criada **depois** das que alimentam as suas vagas, e o desafio, depois da primeira partida da sua rodada. Por isso o `atualizar` precisa de uma passada só pela lista: quando chega a uma partida, as anteriores já foram resolvidas.

A `decisiva` é a partida que decide o campeão: nesta chave, a única da última rodada. **Acrescente o import** no topo de `Chave.java`:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | importar -->
```java
import java.util.HashMap;
import java.util.Map;
```

**Acrescente as linhas** abaixo antes do comentário de `ordemDasSementes` (o que começa com "A ordem das sementes"):

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | inserir-antes A ordem das sementes na primeira rodada -->
```java
    private final List<String> participantes;
    private final List<Partida> partidas = new ArrayList<>();
    private final Map<String, Partida> porCodigo = new HashMap<>();
    private final List<List<Partida>> rodadasPrincipais = new ArrayList<>();
    /** Quem perde cada rodada da chave principal: a repescagem do capítulo 13 os recebe. */
    private final List<List<Vaga>> perdedoresDasRodadas = new ArrayList<>();

    /** A partida que decide o campeão. */
    private Partida decisiva;

    /** Uma rodada: as suas partidas, as vagas de quem avança e as de quem cai. */
    private record Rodada(List<Partida> partidas, List<Vaga> avancam, List<Vaga> caem) {
    }

    /**
     * @param sementes os participantes na ordem das sementes, a melhor primeiro; pelo menos dois, todos diferentes
     */
    public Chave(List<String> sementes) {
        if (sementes.size() < 2) {
            throw new IllegalArgumentException("São necessários ao menos dois participantes");
        }
        if (sementes.stream().distinct().count() != sementes.size() || sementes.stream().anyMatch(participante -> participante == null)) {
            throw new IllegalArgumentException("Participantes repetidos ou nulos");
        }
        this.participantes = List.copyOf(sementes);
        construir();
        atualizar();
    }

    /** Cria as partidas da chave, rodada por rodada. */
    private void construir() {
        construirPrincipal();
        decisiva = rodadasPrincipais.get(rodadasPrincipais.size() - 1).get(0);
    }

    /** As rodadas da chave principal: a primeira recebe as sementes, as outras recebem os vencedores da anterior. */
    private void construirPrincipal() {
        // o tamanho da chave é a menor potência de dois que comporta todos; as sementes que não existem não entram
        int tamanho = 2;
        while (tamanho < participantes.size()) {
            tamanho *= 2;
        }
        List<Vaga> vagas = new ArrayList<>();
        for (int semente : ordemDasSementes(tamanho)) {
            if (semente <= participantes.size()) {
                vagas.add(new Vaga(participantes.get(semente - 1)));
            }
        }
        for (int rodada = 1; vagas.size() > 1; rodada++) {
            Rodada atual = emparelhar(Lado.PRINCIPAL, rodada, vagas);
            rodadasPrincipais.add(atual.partidas());
            perdedoresDasRodadas.add(atual.caem());
            vagas = atual.avancam();
        }
    }

    /**
     * Emparelha as vagas, duas a duas, na ordem. Todos jogam em toda rodada, e quem avança venceu alguém: se o número de vagas é
     * ímpar, a que sobra joga um desafio contra o perdedor da primeira partida da rodada. Quem vence o desafio avança, e o perdedor
     * do desafio ocupa, entre os que caem, o lugar do perdedor da primeira partida, que já voltou a jogar.
     */
    private Rodada emparelhar(Lado lado, int rodada, List<Vaga> vagas) {
        String prefixo = lado == Lado.PRINCIPAL ? "V" : "R";
        List<Partida> daRodada = new ArrayList<>();
        List<Vaga> avancam = new ArrayList<>();
        List<Vaga> caem = new ArrayList<>();
        int pares = vagas.size() / 2;
        for (int indice = 0; indice < pares; indice++) {
            Partida partida = adicionar(prefixo + rodada + "-" + (indice + 1), lado, rodada, indice + 1, vagas.get(2 * indice), vagas.get(2 * indice + 1));
            daRodada.add(partida);
            avancam.add(new Vaga(partida, true));
            caem.add(new Vaga(partida, false));
        }
        if (vagas.size() % 2 == 1) {
            Partida desafio = adicionar(prefixo + rodada + "-" + (pares + 1), lado, rodada, pares + 1, vagas.get(vagas.size() - 1), new Vaga(daRodada.get(0), false));
            desafio.desafio = true;
            daRodada.add(desafio);
            avancam.add(new Vaga(desafio, true));
            caem.set(0, new Vaga(desafio, false));
        }
        return new Rodada(daRodada, avancam, caem);
    }

    /** Cria uma partida, guarda na lista e no índice por código, e a devolve. */
    private Partida adicionar(String codigo, Lado lado, int rodada, int posicao, Vaga a, Vaga b) {
        Partida partida = new Partida(codigo, lado, rodada, posicao, a, b);
        partidas.add(partida);
        porCodigo.put(codigo, partida);
        return partida;
    }

    /** As partidas são criadas depois das que dependem delas, e uma passada só já basta. */
    private void atualizar() {
        for (Partida partida : partidas) {
            partida.atualizar();
        }
    }

```

Falta dizer à chave como mostrar o que ela construiu, e os testes dependem disso. **Acrescente o import** no topo de `Chave.java`:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | importar -->
```java
import java.util.Collections;
```

**Acrescente ao final da classe** `Chave` os métodos que devolvem as partidas:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | acrescentar -->
```java

    public List<String> participantes() {
        return participantes;
    }

    /** Todas as partidas; as que dependem de outras vêm sempre depois delas. */
    public List<Partida> partidas() {
        return Collections.unmodifiableList(partidas);
    }

    /** A partida com o código dado, ou null se não existe. */
    public Partida partida(String codigo) {
        return porCodigo.get(codigo);
    }

    /** As rodadas da chave principal, da primeira à final. */
    public List<List<Partida>> rodadasPrincipais() {
        return Collections.unmodifiableList(rodadasPrincipais);
    }
```

O `Collections.unmodifiableList` devolve a mesma lista, mas **só para leitura**: quem receber a lista e tentar acrescentar ou tirar uma partida leva uma exceção. Sem isso, qualquer classe de fora poderia estragar a chave por engano.

<!-- compilar -->

Os testes novos usam uma função de apoio, `robos(n)`, que faz a lista `robo1`, `robo2` e assim por diante, e conferem a chave de 4 participantes (3 partidas, e quem enfrenta quem), a de 2 (uma partida só), a de 5, com os desafios, e as recusas de listas ruins. Na chave de 5, a primeira rodada tem duas partidas prontas (1 contra 4 e 5 contra 2) e o desafio da semente 3, que espera o perdedor da primeira; a segunda rodada tem a `V2-1` e outro desafio; e as 4 partidas normais mais os 2 desafios dão 6 partidas na chave, mais a final. O `assertThrows` confere que um trecho de código lança a exceção esperada: o `() -> new Chave(...)` é uma **função sem parâmetros** que o `assertThrows` executa por conta própria, para pegar a exceção. **Acrescente os imports** no topo de `ChaveTest.java`:

<!-- arquivo: src/test/java/campeonato/torneio/ChaveTest.java | importar -->
```java
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
```

**Acrescente ao final da classe** `ChaveTest`:

<!-- arquivo: src/test/java/campeonato/torneio/ChaveTest.java | acrescentar -->
```java

    /** Os participantes robo1, robo2... na ordem das sementes. */
    private static List<String> robos(int quantidade) {
        List<String> robos = new ArrayList<>();
        for (int numero = 1; numero <= quantidade; numero++) {
            robos.add("robo" + numero);
        }
        return robos;
    }

    @Test
    void quatroParticipantesFazemTresPartidas() {
        Chave chave = new Chave(robos(4));
        assertEquals(3, chave.partidas().size());

        Partida primeira = chave.partida("V1-1");
        assertEquals("robo1", primeira.a().participante());
        assertEquals("robo4", primeira.b().participante());
        assertEquals(Situacao.PRONTA, primeira.situacao());
        assertEquals("robo2", chave.partida("V1-2").a().participante());
        assertEquals("robo3", chave.partida("V1-2").b().participante());

        Partida decisiva = chave.partida("V2-1");
        assertEquals(Situacao.AGUARDANDO, decisiva.situacao());
        assertEquals("Vencedor de V1-1", decisiva.a().descrever());
        assertEquals("Vencedor de V1-2", decisiva.b().descrever());
    }

    @Test
    void doisParticipantesFazemUmaPartidaSo() {
        Chave chave = new Chave(robos(2));
        assertEquals(1, chave.partidas().size());
        assertEquals(Situacao.PRONTA, chave.partida("V1-1").situacao());
    }

    @Test
    void quemFicaSemParJogaUmDesafioContraOPerdedorDaPrimeiraPartida() {
        // 5 participantes: duas partidas, e a semente 3 fica sem par
        Chave chave = new Chave(robos(5));

        List<Partida> primeiraRodada = chave.rodadasPrincipais().get(0);
        assertEquals(3, primeiraRodada.size());
        assertEquals(Situacao.PRONTA, chave.partida("V1-1").situacao());
        assertEquals(Situacao.PRONTA, chave.partida("V1-2").situacao());
        Partida desafio = chave.partida("V1-3");
        assertEquals(true, desafio.desafio());
        assertEquals("robo3", desafio.a().participante());
        assertEquals("Perdedor de V1-1", desafio.b().descrever());
        assertEquals(Situacao.AGUARDANDO, desafio.situacao());
    }

    @Test
    void recusaChavesSemParticipantesSuficientesOuRepetidos() {
        assertThrows(IllegalArgumentException.class, () -> new Chave(robos(1)));
        assertThrows(IllegalArgumentException.class, () -> new Chave(List.of("robo1", "robo1")));
        assertThrows(IllegalArgumentException.class, () -> new Chave(Arrays.asList("robo1", null)));
    }
```

<!-- testar -->

## Passo 6 — Registrando um resultado

Quando uma partida é jogada, a chave recebe o resultado com `registrar(codigo, vencedor, pontosDoVencedor, pontosDoPerdedor)`. Os pontos só servem para mostrar o placar depois, e não entram na decisão: quem passa é o vencedor. O método confere três coisas, e cada uma tem a sua exceção:

1. A partida precisa **existir**: se não, `IllegalArgumentException`.
2. A partida precisa estar **pronta**: não dá para registrar o resultado de uma que ainda aguarda os participantes, nem de uma já jogada. Aqui o problema não é o argumento, e sim o **momento**, e o Java tem outra exceção para isso: `IllegalStateException`, "o estado do objeto não permite".
3. O vencedor precisa **jogar** a partida: se não, `IllegalArgumentException`.

Se tudo está certo, o método grava o vencedor, o perdedor (o outro dos dois) e os pontos, marca a partida como `FINALIZADA` e chama o `atualizar`, para o vencedor subir para a próxima partida. **Acrescente ao final da classe** `Chave`:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | acrescentar -->
```java

    /**
     * Registra o resultado de uma partida.
     *
     * @throws IllegalArgumentException se a partida não existe ou o vencedor não joga nela
     * @throws IllegalStateException se a partida não pode ser jogada agora
     */
    public void registrar(String codigo, String vencedor, int pontosDoVencedor, int pontosDoPerdedor) {
        Partida partida = porCodigo.get(codigo);
        if (partida == null) {
            throw new IllegalArgumentException("Partida inexistente: " + codigo);
        }
        if (partida.situacao != Situacao.PRONTA) {
            throw new IllegalStateException("A partida " + codigo + " não pode ser jogada agora (" + partida.situacao.texto() + ")");
        }
        if (!partida.envolve(vencedor)) {
            throw new IllegalArgumentException("O vencedor não joga a partida " + codigo);
        }
        partida.vencedor = vencedor;
        partida.perdedor = vencedor.equals(partida.a.participante) ? partida.b.participante : partida.a.participante;
        partida.pontosDoVencedor = pontosDoVencedor;
        partida.pontosDoPerdedor = pontosDoPerdedor;
        partida.situacao = Situacao.FINALIZADA;
        atualizar();
    }
```

<!-- compilar -->

O primeiro teste segue o caminho de um vencedor numa chave de 4: depois de `V1-1`, a partida fica `FINALIZADA` com os pontos gravados, e a vaga da decisiva já tem o vencedor, mas ela continua aguardando até a outra semifinal acabar. O segundo mostra o desafio: ele fica pronto quando a primeira partida da rodada termina, e quem o vence continua na chave. O terceiro confere as quatro recusas. **Acrescente ao final da classe** `ChaveTest`:

<!-- arquivo: src/test/java/campeonato/torneio/ChaveTest.java | acrescentar -->
```java

    @Test
    void oVencedorSobeParaAProximaPartida() {
        Chave chave = new Chave(robos(4));
        chave.registrar("V1-1", "robo4", 80, 60);

        Partida primeira = chave.partida("V1-1");
        assertEquals(Situacao.FINALIZADA, primeira.situacao());
        assertEquals("robo4", primeira.vencedor());
        assertEquals("robo1", primeira.perdedor());
        assertEquals(80, primeira.pontosDoVencedor());
        assertEquals(60, primeira.pontosDoPerdedor());

        // a vaga já foi preenchida, mas a outra ainda espera: a decisiva continua aguardando
        Partida decisiva = chave.partida("V2-1");
        assertEquals("robo4", decisiva.a().participante());
        assertEquals(Situacao.AGUARDANDO, decisiva.situacao());

        chave.registrar("V1-2", "robo2", 90, 10);
        assertEquals(Situacao.PRONTA, decisiva.situacao());
    }

    @Test
    void oDesafioFicaProntoDepoisDaPrimeiraPartidaEQuemVenceAvanca() {
        Chave chave = new Chave(robos(5));
        Partida desafio = chave.partida("V1-3");
        assertEquals(Situacao.AGUARDANDO, desafio.situacao());

        // o perdedor da primeira partida (o robo4) enfrenta o robo3, que ficou sem par
        chave.registrar("V1-1", "robo1", 1, 0);
        assertEquals(Situacao.PRONTA, desafio.situacao());
        assertEquals(Set.of("robo3", "robo4"), Set.of(desafio.a().participante(), desafio.b().participante()));

        // o robo4 perdeu a primeira partida, mas venceu o desafio: está na segunda rodada, e o robo3 saiu
        chave.registrar("V1-3", "robo4", 1, 0);
        assertEquals("robo4", chave.partida("V2-2").a().participante());
    }

    @Test
    void recusaResultadosInvalidos() {
        Chave chave = new Chave(robos(4));
        assertThrows(IllegalArgumentException.class, () -> chave.registrar("X9", "robo1", 1, 0));
        assertThrows(IllegalStateException.class, () -> chave.registrar("V2-1", "robo1", 1, 0));
        assertThrows(IllegalArgumentException.class, () -> chave.registrar("V1-1", "robo2", 1, 0));
        chave.registrar("V1-1", "robo1", 1, 0);
        assertThrows(IllegalStateException.class, () -> chave.registrar("V1-1", "robo1", 1, 0));
    }
```

<!-- testar -->

## Passo 7 — A próxima partida e o campeão

Falta o que o programa precisa para conduzir o campeonato: **qual é a próxima partida**, e **quem é o campeão**.

A próxima partida é escolhida entre as `PRONTA`. Quando há mais de uma (na primeira rodada, quase todas), vale a que tem menos partidas antes dela, ou seja, a de menor `profundidade`, e, para desempatar, a da chave principal antes das outras partes (a ordem de declaração do `enum` `Lado`), depois a de menor rodada e a de menor posição. É um `Comparator` composto, como o `FUMACA_PRIMEIRO` do capítulo 11. O `Partida::profundidade` é uma **referência a método**: um jeito curto de escrever `partida -> partida.profundidade()`. O `thenComparing(Partida::lado)` compara os `enum` pela ordem em que foram declarados.

O resultado vem num `Optional<Partida>`, que é uma **caixa que pode estar vazia**. Se não há nenhuma partida pronta, ou porque o campeonato acabou ou porque falta resultado, a caixa vem vazia. Isso é mais honesto que devolver `null`: quem recebe o `Optional` vê, pelo tipo, que a resposta pode não existir, e precisa tratá-la. Os métodos que mais vamos usar são `isPresent()` (tem algo?), `isEmpty()` (está vazia?) e `get()` (pega o conteúdo, e só deve ser chamado depois de verificar), e o `orElseThrow()` (pega o conteúdo, e lança uma exceção se estiver vazia, o que é prático nos testes).

O campeão é o vencedor da partida `decisiva`, se ela já estiver decidida, ou `null`. A decisiva sempre é jogada: a chave tem pelo menos dois participantes, e o construtor recusa menos que isso. **Acrescente os imports** no topo de `Chave.java`:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | importar -->
```java
import java.util.Comparator;
import java.util.Optional;
```

**Acrescente ao final da classe** `Chave`:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | acrescentar -->
```java

    /** A próxima partida a jogar: a que tem menos partidas antes dela, e a da chave principal primeiro. */
    public Optional<Partida> proximaPartida() {
        return partidas.stream()
                .filter(partida -> partida.situacao == Situacao.PRONTA)
                .min(Comparator.comparingInt(Partida::profundidade)
                        .thenComparing(Partida::lado)
                        .thenComparingInt(Partida::rodada)
                        .thenComparingInt(Partida::posicao));
    }

    /** Diz se a chave já tem campeão. */
    public boolean terminou() {
        return campeao() != null;
    }

    /** O campeão, ou null se o campeonato não acabou. */
    public String campeao() {
        return decisiva.situacao.decidida() ? decisiva.vencedor : null;
    }
```

<!-- compilar -->

O teste mais importante do capítulo joga **campeonatos inteiros**, de 2 a 40 participantes, sorteando o vencedor de cada partida, e confere as regras da chave em todos eles:

- Antes de cada partida, **ninguém está em duas partidas prontas ao mesmo tempo**. Se estivesse, o robô teria de lutar em dois lugares.
- Ao fim, a chave tem campeão, e foram jogadas `n − 1` partidas **mais os desafios**: cada partida elimina um robô, menos o desafio, que dá a quem perdeu a primeira partida da rodada uma segunda chance. Com uma quantidade de robôs que é potência de dois, não há desafio. O campeão perdeu no máximo uma vez, e **todos os outros perderam pelo menos uma**.

A função `jogarTudo` faz o jogo. O `Random(1)`, com semente fixa, repete o mesmo sorteio sempre, e o teste fica igual em todos os computadores. O `derrotas.merge(perdedor, 1, Integer::sum)` soma 1 às derrotas do perdedor (como no `Vitorias` do capítulo 8). Há ainda dois testes menores: a ordem da `proximaPartida` e o que acontece quando não há nenhuma. **Acrescente os imports** no topo de `ChaveTest.java`:

<!-- arquivo: src/test/java/campeonato/torneio/ChaveTest.java | importar -->
```java
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
```

**Acrescente ao final da classe** `ChaveTest`:

<!-- arquivo: src/test/java/campeonato/torneio/ChaveTest.java | acrescentar -->
```java

    /**
     * Joga a chave inteira sorteando os vencedores e confere as regras no caminho.
     *
     * @return quantas partidas cada participante perdeu
     */
    private static Map<String, Integer> jogarTudo(Chave chave, Random sorteio) {
        Map<String, Integer> derrotas = new HashMap<>();
        int guarda = 0;
        while (chave.proximaPartida().isPresent()) {
            assertTrue(guarda++ < 1000, "O campeonato nunca termina");

            // ninguém está em duas partidas prontas ao mesmo tempo
            Set<String> ocupados = new HashSet<>();
            for (Partida partida : chave.partidas()) {
                if (partida.situacao() == Situacao.PRONTA) {
                    assertTrue(ocupados.add(partida.a().participante()), "Em duas partidas: " + partida);
                    assertTrue(ocupados.add(partida.b().participante()), "Em duas partidas: " + partida);
                }
            }

            Partida proxima = chave.proximaPartida().get();
            String vencedor = sorteio.nextBoolean() ? proxima.a().participante() : proxima.b().participante();
            chave.registrar(proxima.codigo(), vencedor, 100, 50);
            derrotas.merge(proxima.perdedor(), 1, Integer::sum);
        }
        return derrotas;
    }

    @Test
    void aProximaPartidaEAQueTemMenosPartidasAntes() {
        Chave chave = new Chave(robos(8));
        // na primeira rodada, as de código menor primeiro
        assertEquals("V1-1", chave.proximaPartida().orElseThrow().codigo());
        chave.registrar("V1-1", "robo1", 1, 0);
        assertEquals("V1-2", chave.proximaPartida().orElseThrow().codigo());
    }

    @Test
    void semPartidasProntasNaoHaProxima() {
        Chave chave = new Chave(robos(2));
        chave.registrar("V1-1", "robo2", 1, 0);
        assertTrue(chave.proximaPartida().isEmpty());
    }

    @Test
    void qualquerNumeroDeParticipantesTermina() {
        Random sorteio = new Random(1);
        for (int quantidade = 2; quantidade <= 40; quantidade++) {
            Chave chave = new Chave(robos(quantidade));
            assertNull(chave.campeao());
            assertFalse(chave.terminou());

            Map<String, Integer> derrotas = jogarTudo(chave, sorteio);

            assertTrue(chave.terminou(), "n=" + quantidade);
            // cada partida elimina um robô, menos o desafio: o perdedor da primeira partida da rodada tem uma segunda chance
            long jogadas = chave.partidas().stream().filter(partida -> partida.situacao() == Situacao.FINALIZADA).count();
            long desafios = chave.partidas().stream().filter(Partida::desafio).count();
            assertEquals(quantidade - 1 + desafios, jogadas, "n=" + quantidade);
            // só as quantidades que são potência de dois não têm desafio
            assertEquals(quantidade == Integer.highestOneBit(quantidade), desafios == 0, "n=" + quantidade);
            String campeao = chave.campeao();
            assertTrue(derrotas.getOrDefault(campeao, 0) <= 1, "n=" + quantidade);
            for (String robo : robos(quantidade)) {
                if (!robo.equals(campeao)) {
                    assertTrue(derrotas.get(robo) >= 1, "n=" + quantidade + " " + robo);
                }
            }
        }
    }
```

<!-- testar -->

## Passo 8 — Jogando uma chave de mentira

Falta ver a chave funcionando. A classe `Simulacao` monta uma chave com tantos robôs quantos forem pedidos (6, se nada for dito), escreve as partidas como estão antes de qualquer jogo, e depois joga uma a uma, sorteando o vencedor, na ordem que a `proximaPartida` manda. O `args` do `main` são os argumentos da linha de comando: `args[0]`, se existir, é o número de robôs. O `Integer.parseInt` converte o texto em número. **Crie o arquivo** `src/main/java/campeonato/torneio/Simulacao.java`:

<!-- arquivo: src/main/java/campeonato/torneio/Simulacao.java | criar -->
```java
package campeonato.torneio;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Joga uma chave inteira com vencedores sorteados, para ver a chave funcionando. */
public final class Simulacao {

    private Simulacao() {
    }

    /** Uso: Simulacao [quantidade de robôs]. */
    public static void main(String[] args) {
        int quantidade = args.length > 0 ? Integer.parseInt(args[0]) : 6;
        List<String> robos = new ArrayList<>();
        for (int numero = 1; numero <= quantidade; numero++) {
            robos.add("robo" + numero);
        }

        Chave chave = new Chave(robos);
        System.out.println("A chave, antes de qualquer partida:");
        for (Partida partida : chave.partidas()) {
            System.out.println("  " + partida.descrever());
        }

        System.out.println("As partidas, na ordem em que são jogadas:");
        Random sorteio = new Random(1);
        while (chave.proximaPartida().isPresent()) {
            Partida partida = chave.proximaPartida().get();
            String a = partida.a().participante();
            String b = partida.b().participante();
            String vencedor = sorteio.nextBoolean() ? a : b;
            chave.registrar(partida.codigo(), vencedor, 100, 50);
            System.out.println("  " + partida.codigo() + ": " + a + " x " + b + " -> venceu " + vencedor);
        }
        System.out.println("Campeão: " + chave.campeao());
    }
}
```

<!-- compilar -->

Compile e rode, de dentro da pasta `mini-campeonato`, como no capítulo 5:

<!-- executar: | espera: Campeão: robo1 | proibe: Exception -->
```bash
mvn -q compile
java -cp target/classes campeonato.torneio.Simulacao 6
```

A saída, com 6 robôs, é esta:

```text
A chave, antes de qualquer partida:
  V1-1  robo1 x robo4  (pronta)
  V1-2  robo5 x robo2  (pronta)
  V1-3  robo3 x robo6  (pronta)
  V2-1  Vencedor de V1-1 x Vencedor de V1-2  (aguardando)
  V2-2  Vencedor de V1-3 x Perdedor de V2-1  (aguardando)
  V3-1  Vencedor de V2-1 x Vencedor de V2-2  (aguardando)
As partidas, na ordem em que são jogadas:
  V1-1: robo1 x robo4 -> venceu robo1
  V1-2: robo5 x robo2 -> venceu robo2
  V1-3: robo3 x robo6 -> venceu robo6
  V2-1: robo1 x robo2 -> venceu robo2
  V2-2: robo6 x robo1 -> venceu robo1
  V3-1: robo2 x robo1 -> venceu robo1
Campeão: robo1
```

Com 6 robôs, a primeira rodada tem três partidas, e ninguém fica sem par. Os três vencedores (robo1, robo2 e robo6) formam uma rodada ímpar, e então aparece o **desafio**: a `V2-1` é a partida normal (robo1 contra robo2), e a `V2-2` é o desafio do robo6, que ficou sem par, contra o **perdedor** da `V2-1`, o robo1. O robo1 perdeu a `V2-1`, mas ganhou o desafio, e por isso chegou à final, que ganhou: foi campeão depois de perder uma partida, o que só acontece graças ao desafio. Foram 6 partidas jogadas: as 5 do `n − 1` mais o desafio, como o teste garante. Os campeões sorteados mudam se você trocar a semente do `Random`.

## O que construímos

```text
src/main/java/campeonato/torneio/
├── Situacao.java    em que pé está uma partida: aguardando, pronta ou finalizada
├── Lado.java        a parte da chave: principal, repescagem ou final
├── Vaga.java        um lugar de uma partida: uma semente, ou o vencedor/perdedor de outra partida
├── Partida.java     uma partida: as vagas, o código, a profundidade, se é desafio e o resultado
├── Chave.java       a chave de eliminação: monta as rodadas, registra resultados e diz a próxima partida
└── Simulacao.java   joga uma chave inteira com vencedores sorteados

src/test/java/campeonato/torneio/
└── ChaveTest.java
```

## Exercícios

**Fácil**

1. Rode a `Simulacao` com 3, 7 e 16 robôs. Conte, em cada caso, quantas partidas são jogadas e quantas são `desafio`. A conta `n − 1 + desafios` confere? Por que a chave de 16 não tem nenhum desafio?
2. Troque a semente do `Random` na `Simulacao`, de 1 para 2 e depois para 3. A chave muda? O campeão muda? O que a semente do sorteio muda, e o que ela não muda?
3. Escreva um teste para `Chave.ordemDasSementes(16)`. Prever a resposta à mão, com a receita do passo 4 (o total é 17), dá a sequência `1, 16, 8, 9, 4, 13, 5, 12, 2, 15, 7, 10, 3, 14, 6, 11`. Confira.

**Médio**

4. Acrescente à `Chave` o método `int partidasRestantes()`, que conta as partidas que ainda precisam ser jogadas (as `AGUARDANDO` e as `PRONTA`). Use-o na `Simulacao` para escrever `faltam N` depois de cada resultado, e escreva um teste: numa chave de 8, são 7 no começo e 0 no fim. Os desafios contam como partidas a jogar, mesmo antes de a primeira partida da sua rodada ser jogada?
5. Acrescente o método `List<String> aindaNaDisputa()`, que devolve os participantes que ainda não perderam nenhuma partida. Numa chave de 4, são 4 no começo e, depois de `V1-1` vencida pelo `robo4`, os outros 3. Dica: um participante perdeu uma partida se ele é o `perdedor()` de uma partida `FINALIZADA`.

**Difícil**

6. Acrescente a **disputa de terceiro lugar**: uma partida de código `T1`, no `Lado.FINAL`, entre os perdedores das duas semifinais. Dica: use a vaga do **perdedor**, `new Vaga(semifinal, false)`, e crie a partida no fim do `construirPrincipal`, quando a chave tem pelo menos duas rodadas, com `adicionar("T1", Lado.FINAL, 1, 1, ...)`. Os testes novos devem conferir que, numa chave de 8, a `T1` começa aguardando, fica pronta depois das duas semifinais, e que o campeão continua sendo o vencedor da `V3-1`. Dois testes antigos vão falhar, de propósito, porque a regra mudou: o da chave de 4 participantes agora tem 4 partidas, e o que joga campeonatos inteiros supõe que só se joga a chave principal. Ajuste os dois (o segundo deve contar só as partidas do `Lado.PRINCIPAL`). O que acontece com a `T1` numa chave de 3 participantes?

## Perguntas para fixação

1. Por que uma chave de 8 robôs tem 7 partidas? E a de 5, quantas partidas joga e quantas são desafios?
2. Por que o robô sem par joga contra o perdedor da primeira partida da rodada, e não passa direto? O que isso muda para quem perdeu essa partida?
3. Por que a `Vaga` guarda a partida de **origem**, em vez de guardar o nome do vencedor?
4. Por que o `atualizar` precisa de só uma passada pela lista de partidas?
5. Quando o `registrar` lança uma `IllegalArgumentException` e quando lança uma `IllegalStateException`? Dê um exemplo de cada.

<details>
<summary>Respostas</summary>

1. Cada partida elimina exatamente um robô, e só o campeão não é eliminado, então são `n − 1` partidas: 7, com 8 robôs. Com 5 robôs, são 6 partidas: 4 normais (`n − 1`) e 2 desafios, a `V1-3` e a `V2-2`, que são partidas a mais.
2. Para que ninguém avance sem derrotar alguém: o robô sem par precisa de um adversário, e o que está à mão é o perdedor da primeira partida da rodada, que já lutou. Esse robô ganha uma segunda chance: se vencer o desafio, avança; se perder, está fora. Em contrapartida, o robô sem par arrisca ser eliminado, como todos os outros.
3. Porque, quando a chave é montada, o vencedor ainda não existe. A vaga só sabe de onde ele vai vir, e preenche o `participante` quando a partida de origem é decidida. Assim a chave é montada uma vez, e cada resultado só atualiza o que depende dele. Também é por isso que a chave pode ser montada de novo a partir dos resultados.
4. Porque as partidas são criadas depois das que alimentam as suas vagas. Quando o `atualizar` chega a uma partida, as anteriores já foram resolvidas, e o vencedor de uma partida cedo na lista já preencheu a vaga da partida seguinte antes de esta ser olhada.
5. A `IllegalArgumentException` é para um argumento errado: um código que não existe (`"X9"`), ou um vencedor que não joga a partida. A `IllegalStateException` é para o momento errado: registrar `V2-1` quando os participantes ainda não são conhecidos, ou registrar de novo uma partida já finalizada.

</details>

No próximo capítulo, a chave ganha a repescagem: quem perde uma vez ainda pode ser campeão.
