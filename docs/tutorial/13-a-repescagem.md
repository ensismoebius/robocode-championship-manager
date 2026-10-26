# Capítulo 13 — A repescagem

## Objetivo

Dar uma segunda chance a quem perde. Com a **repescagem**, quem perde uma partida na chave principal não está eliminado: cai para uma segunda chave, a repescagem, e só sai do campeonato na segunda derrota. No fim, o campeão da chave principal e o da repescagem se enfrentam numa **grande final**.

A mesma `Chave` do capítulo anterior passa a aceitar a repescagem, sem perder o que já funciona. Ela ganha também o que o programa precisa para o campeonato inteiro: o nome de cada rodada ("Quartas de final", "Fim da repescagem"), a **importância** de cada partida (só as semifinais e as finais merecem aviso na arena), as colocações e o pódio. Como no capítulo 12, tudo é lógica em Java puro, testada de cima a baixo, e a `Simulacao` joga um campeonato inteiro no terminal.

## Passo 1 — Duas vidas

Na eliminação simples, uma derrota tira o robô do campeonato, e o robô mais forte pode perder numa partida ruim e ir embora. Na chave com repescagem, cada robô tem **duas vidas**: perder uma vez manda o robô para a repescagem, e perder duas vezes o elimina.

A chave principal continua a mesma do capítulo 12, com os códigos que começam com `V`. A novidade são as partidas da repescagem, com os códigos que começam com `R`, e a grande final, o `F1`. Para 8 robôs, o caminho de cada perdedor é este:

```text
chave principal    V1 ─────────► V2 ───────────────────► V3 ─────────┐
                   │             │                       │           │
                   │ perdem      │ perdem                │ perde     │ vence
                   ▼             ▼                       ▼           ▼
repescagem         R1 ─────────► R2 ───────► R3 ───────► R4 ───────► F1
```

Os perdedores de cada rodada da chave principal caem na repescagem, e o campeão da chave principal espera na grande final. Em cada rodada da repescagem, joga quem sobrou da anterior:

| Rodada | Quem joga |
| --- | --- |
| `R1` | os perdedores da `V1`, dois a dois |
| `R2` | os vencedores da `R1` contra os perdedores da `V2` |
| `R3` | os vencedores da `R2`, dois a dois |
| `R4` | o vencedor da `R3` contra o perdedor da `V3` |
| `F1` | o vencedor da `V3-1` contra o vencedor da `R4-1` |

A repescagem alterna duas espécies de rodada: uma em que os **sobreviventes** jogam entre si, e outra em que eles encontram quem acabou de **cair** da chave principal. Uma chave principal de `k` rodadas tem `2 × (k − 1)` rodadas de repescagem: a de 8 robôs tem 3 rodadas na principal e 4 na repescagem, e a de 16 tem 4 e 6.

Uma conta mostra quantas partidas a chave tem. Cada partida tem um perdedor, e cada robô perde, no máximo, duas vezes. Quem não chega à grande final perde exatamente duas, e dos dois finalistas, o campeão perdeu no máximo uma vez e o vice perdeu uma ou duas. O resultado, com `n` robôs, é sempre de `2n − 2` partidas: 6 com 4 robôs, 14 com 8 e 30 com 16. Esse número é um bom teste da chave.

A grande final é **uma partida só**, sem revanche. Ela é diferente da eliminação dupla clássica, onde o campeão da chave principal, por ainda não ter perdido nenhuma vez, teria direito a uma segunda final. Aqui, quem perder a `F1` perde o campeonato.

## Passo 2 — Uma chave, dois modos

A `Chave` precisa saber se tem repescagem. Há um detalhe de projeto: os testes e o código do capítulo 12 chamam `new Chave(robos(4))`, só com a lista, e queremos que isso continue valendo. O Java deixa uma classe ter **vários construtores**, desde que os parâmetros sejam diferentes, o que se chama **sobrecarga**. Então a chave terá dois:

- `Chave(sementes)`, sem repescagem, que não faz nada além de chamar o outro;
- `Chave(sementes, repescagem)`, que faz o trabalho.

O primeiro chama o segundo com `this(sementes, false)`. Dentro de um construtor, `this(...)` chama **outro construtor da mesma classe**, e precisa ser a primeira linha. Assim o código de conferir os participantes e montar a chave fica num lugar só. **Acrescente os campos** à `Chave`, depois do `participantes`:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | inserir-depois private final List<String> participantes; -->
```java
    private final boolean repescagem;
```

E depois do `rodadasPrincipais`, a lista com as rodadas da repescagem, do mesmo tipo:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | inserir-depois private final List<List<Partida>> rodadasPrincipais = new ArrayList<>(); -->
```java
    private final List<List<Partida>> rodadasDaRepescagem = new ArrayList<>();
```

**Substitua o construtor** da `Chave` pelos dois construtores:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | substituir-metodo Chave -->
```java
    /** Uma chave de eliminação simples: quem perde uma vez está fora. */
    public Chave(List<String> sementes) {
        this(sementes, false);
    }

    /**
     * @param sementes os participantes na ordem das sementes, a melhor primeiro; pelo menos dois, todos diferentes
     * @param repescagem se verdadeiro, quem perde na chave principal ainda joga na repescagem e só é eliminado na segunda derrota
     */
    public Chave(List<String> sementes, boolean repescagem) {
        if (sementes.size() < 2) {
            throw new IllegalArgumentException("São necessários ao menos dois participantes");
        }
        if (sementes.stream().distinct().count() != sementes.size() || sementes.stream().anyMatch(participante -> participante == null)) {
            throw new IllegalArgumentException("Participantes repetidos ou nulos");
        }
        this.participantes = List.copyOf(sementes);
        this.repescagem = repescagem;
        construir();
        atualizar();
    }
```

O comentário que descreve a classe também precisa falar da repescagem. **Acrescente as linhas** abaixo no comentário da `Chave`, logo depois da linha que termina em "decidida sem jogar.":

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | inserir-depois a partida de quem tem folga é decidida sem jogar. -->
```java
 *
 * <p>Com repescagem, quem perde na chave principal ainda joga na repescagem e só sai na segunda derrota. O campeão da
 * chave principal e o da repescagem decidem o campeonato numa grande final, de uma partida só.
```

<!-- compilar -->

## Passo 3 — As rodadas da repescagem

Agora o método que cria as partidas da repescagem. Ele repete, para cada rodada, uma de três receitas, e a rodada decide qual:

1. **A primeira rodada** (`R1`). Os perdedores da `V1` jogam entre si, dois a dois: o perdedor da `V1-1` contra o da `V1-2`, o da `V1-3` contra o da `V1-4`, e assim por diante. São `tamanho / 4` partidas, metade das partidas da `V1`. As vagas são `new Vaga(partida, false)`: o `false` quer dizer que a vaga fica com o **perdedor** da partida de origem, e é o motivo de a `Vaga` ter o campo `doVencedor` desde o capítulo 12.
2. **As rodadas pares** (`R2`, `R4`, `R6`...). Quem sobrou da rodada anterior da repescagem enfrenta quem acabou de cair da chave principal: a `R2` recebe os perdedores da `V2`, a `R4` os da `V3`, e a conta é `rodada / 2`, como índice da lista de rodadas (a `V2` é o índice 1, e a `V3` é o índice 2, pois a lista começa em 0).
3. **As rodadas ímpares** (`R3`, `R5`...). Quem sobrou joga contra outro sobrevivente, dois a dois, sem ninguém novo.

Na rodada par há um cuidado. Se o vencedor da `R1-1` enfrentasse sempre o perdedor da `V2-1`, a revanche seria provável: o vencedor da `R1-1` é um dos perdedores da `V1-1` e da `V1-2`, e o perdedor da `V2-1` é um dos **vencedores** dessas mesmas duas partidas, então os dois podem ter se enfrentado pouco antes. A saída é **inverter a ordem** de quem cai, de modo que o vencedor da `R1-1` encontre o perdedor da `V2-2`, que veio da outra metade da chave. Na `R2`, `invertida` é verdadeiro e o índice vira `caindo.size() - 1 - indice`, o espelho. Na rodada par seguinte (a `R4`), a ordem volta ao normal, e assim se alterna: `(rodada / 2) % 2 == 1`.

O código segue a ordem de criação do capítulo 12: as partidas da `R2` são criadas depois das da `R1` e da `V2`, de que dependem, e por isso o `atualizar` continua precisando de uma passada só. **Acrescente o método** antes do `adicionar`, o que começa com "Cria uma partida":

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | inserir-antes Cria uma partida, guarda na lista -->
```java
    /** As rodadas da repescagem: quem perde na chave principal joga aqui até sobrar um só. */
    private void construirRepescagem(int tamanho, int rodadas) {
        for (int rodada = 1; rodada <= 2 * (rodadas - 1); rodada++) {
            List<Partida> daRodada = new ArrayList<>();
            if (rodada == 1) {
                // os perdedores da primeira rodada da principal jogam entre si, dois a dois
                List<Partida> primeira = rodadasPrincipais.get(0);
                for (int indice = 0; indice < tamanho / 4; indice++) {
                    daRodada.add(adicionar("R1-" + (indice + 1), Lado.REPESCAGEM, 1, indice + 1,
                            new Vaga(primeira.get(2 * indice), false), new Vaga(primeira.get(2 * indice + 1), false)));
                }
            } else if (rodada % 2 == 0) {
                // rodada par: quem sobrou da repescagem enfrenta quem acabou de cair da chave principal
                List<Partida> anterior = rodadasDaRepescagem.get(rodada - 2);
                List<Partida> caindo = rodadasPrincipais.get(rodada / 2);
                // trocar a ordem de quem cai, de uma rodada para outra, evita repetir confrontos já jogados
                boolean invertida = (rodada / 2) % 2 == 1;
                for (int indice = 0; indice < anterior.size(); indice++) {
                    Partida daPrincipal = caindo.get(invertida ? caindo.size() - 1 - indice : indice);
                    daRodada.add(adicionar("R" + rodada + "-" + (indice + 1), Lado.REPESCAGEM, rodada, indice + 1,
                            new Vaga(anterior.get(indice), true), new Vaga(daPrincipal, false)));
                }
            } else {
                // rodada ímpar: os sobreviventes da repescagem jogam entre si, dois a dois
                List<Partida> anterior = rodadasDaRepescagem.get(rodada - 2);
                for (int indice = 0; indice < anterior.size() / 2; indice++) {
                    daRodada.add(adicionar("R" + rodada + "-" + (indice + 1), Lado.REPESCAGEM, rodada, indice + 1,
                            new Vaga(anterior.get(2 * indice), true), new Vaga(anterior.get(2 * indice + 1), true)));
                }
            }
            rodadasDaRepescagem.add(daRodada);
        }
    }

```

O laço vai até `2 × (rodadas − 1)`. Numa chave de 2 participantes, `rodadas` vale 1, o laço não roda nenhuma vez e a repescagem fica vazia. Veremos já a seguir o que acontece nesse caso.

O índice `rodada - 2` das listas pode confundir: as rodadas são contadas a partir de 1, e as listas, a partir de 0. A rodada `R2` está no índice 1 de `rodadasDaRepescagem`, e a rodada anterior, a `R1`, no índice 0, que é `rodada - 2` quando `rodada` vale 2. O mesmo vale para a lista das rodadas da chave principal, de onde se tira o `rodada / 2`.

## Passo 4 — A grande final

Falta ligar tudo no `construir`: depois da chave principal e da repescagem, a grande final, a `F1`, com duas vagas:

- o **vencedor** da final da chave principal (a última partida de `rodadasPrincipais`);
- o vencedor da última partida da repescagem.

A partida `F1` é a nova `decisiva`: quem vence a `F1` é o campeão, e o `campeao()` do capítulo 12, que olha a `decisiva`, continua certo sem mudar nada. Sem repescagem, a `decisiva` é a final da chave principal, como antes, e o método termina cedo, com um `return`.

Com **dois** participantes não há rodadas de repescagem, como vimos. O perdedor da única partida da chave principal ainda tem direito à segunda vida, e vai direto à grande final: nesse caso, a segunda vaga da `F1` é o **perdedor** da final da chave principal. A chave de 2 com repescagem joga, então, duas partidas (`V1-1` e `F1`), as `2n − 2` do passo 1.

**Substitua o método** `construir`:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | substituir-metodo construir -->
```java
    /** Cria as partidas da chave, rodada por rodada. */
    private void construir() {
        // o tamanho da chave é a menor potência de dois que comporta todos; o que sobra são folgas
        int tamanho = 2;
        int rodadas = 1;
        while (tamanho < participantes.size()) {
            tamanho *= 2;
            rodadas++;
        }
        construirPrincipal(tamanho, rodadas);

        Partida finalDaPrincipal = rodadasPrincipais.get(rodadas - 1).get(0);
        decisiva = finalDaPrincipal;
        if (!repescagem) {
            return;
        }

        construirRepescagem(tamanho, rodadas);

        // com dois participantes não há repescagem: o perdedor da única partida vai direto para a grande final
        Vaga daRepescagem = rodadasDaRepescagem.isEmpty()
                ? new Vaga(finalDaPrincipal, false)
                : new Vaga(rodadasDaRepescagem.get(rodadasDaRepescagem.size() - 1).get(0), true);
        decisiva = adicionar("F1", Lado.FINAL, 1, 1, new Vaga(finalDaPrincipal, true), daRepescagem);
    }
```

O `? :` de duas linhas é o **operador condicional** do capítulo 1, que escolhe entre dois valores conforme uma condição.

A ordem em que as partidas são jogadas vem da `profundidade` do capítulo 12, sem nenhuma mudança. A `R1` vem depois da `V1`, de que depende, e tem profundidade 2, igual à da `V2`, e no empate a chave principal sai primeiro: `V2-1`, `V2-2`, `R1-1`, `R1-2`. A final da chave principal, a `V3-1`, tem profundidade 3 e vem depois da `R1`. O `Comparator` do `proximaPartida` já mandava a principal antes da repescagem e a repescagem antes da final, pela ordem de declaração do `enum` `Lado`.

Por fim, os métodos que mostram a repescagem. **Acrescente ao final da classe** `Chave`:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | acrescentar -->
```java

    /** As rodadas da repescagem; vazia se a chave não tem repescagem ou se só tem dois participantes. */
    public List<List<Partida>> rodadasDaRepescagem() {
        return Collections.unmodifiableList(rodadasDaRepescagem);
    }

    /** Diz se a chave tem repescagem. */
    public boolean temRepescagem() {
        return repescagem;
    }
```

<!-- compilar -->

Os testes novos conferem a chave de 4 participantes com repescagem, com as 6 partidas e as suas vagas; a passagem dos perdedores da primeira rodada para a `R1-1`; e a chave de 2, em que a `F1` fica pronta depois da `V1-1`. O teste mais forte joga campeonatos inteiros, de 2 a 40 participantes, 5 vezes cada, e confere o que o passo 1 prometeu:

- foram jogadas exatamente `2n − 2` partidas;
- o campeão perdeu no máximo uma vez;
- o vice perdeu uma ou duas vezes, e **todos os outros perderam exatamente duas**.

O vice é quem perde a `F1`, o `chave.partida("F1").perdedor()`. Há também um teste com folgas, que mostra que a repescagem trata uma vaga vazia como a chave principal trata: com 5 participantes, a `R1-2` reúne duas folgas, e a `R1-1` tem uma folga e só espera o perdedor da `V1-2`, que passa direto. **Acrescente ao final da classe** `ChaveTest`:

<!-- arquivo: src/test/java/campeonato/torneio/ChaveTest.java | acrescentar -->
```java

    @Test
    void comRepescagemUmaChaveDeQuatroTemSeisPartidas() {
        Chave chave = new Chave(robos(4), true);
        List<String> codigos = chave.partidas().stream().map(Partida::codigo).toList();
        assertEquals(List.of("V1-1", "V1-2", "V2-1", "R1-1", "R2-1", "F1"), codigos);

        // os perdedores da primeira rodada jogam entre si
        Partida r1 = chave.partida("R1-1");
        assertEquals("Perdedor de V1-1", r1.a().descrever());
        assertEquals("Perdedor de V1-2", r1.b().descrever());
        // o vencedor da R1 encontra quem cai da final da principal, e a grande final reúne os dois campeões
        assertEquals("Vencedor de R1-1", chave.partida("R2-1").a().descrever());
        assertEquals("Perdedor de V2-1", chave.partida("R2-1").b().descrever());
        assertEquals("Vencedor de V2-1", chave.partida("F1").a().descrever());
        assertEquals("Vencedor de R2-1", chave.partida("F1").b().descrever());
    }

    @Test
    void quemPerdeNaPrincipalVaiParaARepescagem() {
        Chave chave = new Chave(robos(4), true);
        chave.registrar("V1-1", "robo1", 1, 0);
        chave.registrar("V1-2", "robo2", 1, 0);
        // robo4 e robo3 perderam, e agora jogam entre si
        Partida r1 = chave.partida("R1-1");
        assertEquals(Situacao.PRONTA, r1.situacao());
        assertEquals(Set.of("robo4", "robo3"), Set.of(r1.a().participante(), r1.b().participante()));
    }

    @Test
    void comDoisParticipantesOPerdedorVaiDiretoParaAGrandeFinal() {
        Chave chave = new Chave(robos(2), true);
        assertTrue(chave.rodadasDaRepescagem().isEmpty());
        chave.registrar("V1-1", "robo1", 1, 0);
        assertEquals(Situacao.PRONTA, chave.partida("F1").situacao());
        chave.registrar("F1", "robo2", 1, 0);
        assertTrue(chave.terminou());
        assertEquals("robo2", chave.campeao());
    }

    @Test
    void comFolgasAPartidaSemParticipanteSeDecideSozinha() {
        // 5 participantes: a R1-2 reúne duas folgas, e a R1-1 só espera quem perder a V1-2
        Chave chave = new Chave(robos(5), true);
        assertEquals(Situacao.FOLGA, chave.partida("R1-2").situacao());
        assertEquals(Situacao.AGUARDANDO, chave.partida("R1-1").situacao());

        chave.registrar("V1-2", "robo4", 1, 0);
        // o perdedor da V1-2 segue na repescagem sem jogar, porque a R1-1 tem uma folga
        assertEquals(Situacao.FOLGA, chave.partida("R1-1").situacao());
        assertEquals("robo5", chave.partida("R1-1").vencedor());
    }

    @Test
    void aRepescagemDaUmaSegundaChanceATodos() {
        Random sorteio = new Random(2);
        for (int quantidade = 2; quantidade <= 40; quantidade++) {
            for (int repeticao = 0; repeticao < 5; repeticao++) {
                Chave chave = new Chave(robos(quantidade), true);
                Map<String, Integer> derrotas = jogarTudo(chave, sorteio);

                assertTrue(chave.terminou(), "n=" + quantidade);
                String campeao = chave.campeao();
                String vice = chave.partida("F1").perdedor();

                // todos, menos os finalistas, saem com exatamente duas derrotas; na grande final não há revanche, então o
                // campeão perdeu no máximo uma vez, e o vice, uma (se veio da principal) ou duas (se veio da repescagem)
                for (String robo : robos(quantidade)) {
                    if (robo.equals(campeao)) {
                        assertTrue(derrotas.getOrDefault(robo, 0) <= 1, "n=" + quantidade);
                    } else if (robo.equals(vice)) {
                        assertTrue(derrotas.get(robo) == 1 || derrotas.get(robo) == 2, "n=" + quantidade);
                    } else {
                        assertEquals(2, derrotas.get(robo), "n=" + quantidade + " " + robo);
                    }
                }

                // cada partida tira uma das duas vidas de quem perde, e só os finalistas sobram com vida
                long jogadas = chave.partidas().stream().filter(partida -> partida.situacao() == Situacao.FINALIZADA).count();
                assertEquals(2L * quantidade - 2, jogadas, "n=" + quantidade);
            }
        }
    }
```

<!-- testar -->

O `jogarTudo` do capítulo 12 continua conferindo, antes de cada partida, que **ninguém está em duas partidas prontas ao mesmo tempo**, e agora a regra vale também entre a chave principal e a repescagem.

## Passo 5 — O nome e a importância de cada rodada

O programa vai mostrar, na tela da chave e na arena, em que rodada está cada partida: "Quartas de final", "Semifinal", "Repescagem 2". O nome depende do tamanho da chave: a rodada que vem imediatamente antes da final é sempre a "Semifinal", e a anterior a ela, as "Quartas de final", e a anterior, as "Oitavas de final". Os nomes são dados **a partir da final, para trás**. Nas chaves grandes, as primeiras rodadas não têm nome tradicional, e o programa as numera: "1ª rodada".

Com repescagem, as duas finais ganham nomes próprios: a final da chave principal é o **"Fim da chave principal"** (e não "Final", porque a decisão do campeonato fica para a grande final), e a última rodada da repescagem é o **"Fim da repescagem"**. As outras rodadas da repescagem são "Repescagem 1", "Repescagem 2" e assim por diante. A `F1` é a "Grande final".

Os nomes são calculados com o `switch` com `->` do capítulo 9. A expressão `rodadasPrincipais.size() - rodada` diz **quantas rodadas ainda vêm depois desta**: 0 é a última, 1 é a penúltima, e assim por diante. Cada `case` devolve um texto, e o `default` cobre as rodadas mais cedo, com `rodada + "ª rodada"`. O `switch` do `nomeDaRodada` é sobre o `enum` `Lado`, e por isso o compilador confere que as três partes da chave estão tratadas.

Há ainda a **importância** da partida. O programa avisa na arena, com um letreiro, antes de uma partida, mas **só as semifinais e as finais** (a da chave principal, a da repescagem e a grande final) merecem esse aviso. Seria possível decidir isso olhando o texto do nome ("o nome é Semifinal?"), mas comparar textos é frágil: um erro de digitação ou uma mudança de nome desligaria o aviso sem ninguém notar. Por isso a decisão é outro valor, um `enum` próprio, a `Importancia`, e o nome serve só para mostrar. **Crie o arquivo** `src/main/java/campeonato/torneio/Importancia.java`:

<!-- arquivo: src/main/java/campeonato/torneio/Importancia.java | criar -->
```java
package campeonato.torneio;

/** O quanto uma partida importa, para o programa avisar sobre as grandes. */
public enum Importancia {

    /** Uma partida como as outras. */
    COMUM,
    /** Uma semifinal da chave principal. */
    SEMIFINAL,
    /** Uma final: a da chave principal, a da repescagem ou a grande final. */
    FINAL
}
```

**Acrescente ao final da classe** `Chave` os métodos do nome e da importância:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | acrescentar -->
```java

    /** O nome da rodada de uma partida, como "Quartas de final" ou "Repescagem 2". */
    public String nomeDaRodada(Partida partida) {
        return switch (partida.lado()) {
        case PRINCIPAL -> nomeDaRodadaPrincipal(partida.rodada());
        case REPESCAGEM -> nomeDaRodadaDaRepescagem(partida.rodada());
        case FINAL -> "Grande final";
        };
    }

    /** O nome de uma rodada da chave principal, contada de 1: os nomes são dados a partir da final, para trás. */
    public String nomeDaRodadaPrincipal(int rodada) {
        return switch (rodadasPrincipais.size() - rodada) {
        case 0 -> repescagem ? "Fim da chave principal" : "Final";
        case 1 -> "Semifinal";
        case 2 -> "Quartas de final";
        case 3 -> "Oitavas de final";
        default -> rodada + "ª rodada";
        };
    }

    /** O nome de uma rodada da repescagem, contada de 1. */
    public String nomeDaRodadaDaRepescagem(int rodada) {
        return rodada == rodadasDaRepescagem.size() ? "Fim da repescagem" : "Repescagem " + rodada;
    }

    /** Se uma partida é uma semifinal ou uma final (da principal, da repescagem ou a grande final), ou uma comum. */
    public Importancia importancia(Partida partida) {
        return switch (partida.lado()) {
        case PRINCIPAL -> switch (rodadasPrincipais.size() - partida.rodada()) {
        case 0 -> Importancia.FINAL;
        case 1 -> Importancia.SEMIFINAL;
        default -> Importancia.COMUM;
        };
        case REPESCAGEM -> partida.rodada() == rodadasDaRepescagem.size() ? Importancia.FINAL : Importancia.COMUM;
        case FINAL -> Importancia.FINAL;
        };
    }
```

O `importancia` tem um `switch` dentro de outro: o de fora escolhe a parte da chave, e o de dentro, na parte principal, olha quantas rodadas faltam. Numa chave de 2 participantes, a única rodada é ao mesmo tempo a última e a primeira, e vale `FINAL`.

<!-- compilar -->

Os testes conferem os nomes das rodadas de uma chave de 16 (com e sem repescagem) e de 32, e a importância: sem repescagem, só as semifinais e a final; com repescagem, as três finais. O `switch` do teste, com `->` sobre textos, calcula a importância esperada a partir do nome, e assim o teste confere os dois métodos entre si. **Acrescente ao final da classe** `ChaveTest`:

<!-- arquivo: src/test/java/campeonato/torneio/ChaveTest.java | acrescentar -->
```java

    @Test
    void osNomesDasRodadasVemDaFinalParaTras() {
        Chave dezesseis = new Chave(robos(16));
        assertEquals("Oitavas de final", dezesseis.nomeDaRodadaPrincipal(1));
        assertEquals("Quartas de final", dezesseis.nomeDaRodadaPrincipal(2));
        assertEquals("Semifinal", dezesseis.nomeDaRodadaPrincipal(3));
        assertEquals("Final", dezesseis.nomeDaRodadaPrincipal(4));
        // com repescagem, a principal e a repescagem terminam em finais próprias
        Chave comRepescagem = new Chave(robos(16), true);
        assertEquals("Fim da chave principal", comRepescagem.nomeDaRodadaPrincipal(4));
        assertEquals("Fim da repescagem", comRepescagem.nomeDaRodadaDaRepescagem(comRepescagem.rodadasDaRepescagem().size()));
        assertEquals("Repescagem 1", comRepescagem.nomeDaRodadaDaRepescagem(1));
        assertEquals("Grande final", comRepescagem.nomeDaRodada(comRepescagem.partida("F1")));
        // numa chave grande, as primeiras rodadas são contadas pelo número
        assertEquals("1ª rodada", new Chave(robos(32)).nomeDaRodadaPrincipal(1));
    }

    @Test
    void soAsSemifinaisEAsFinaisSaoImportantes() {
        Chave simples = new Chave(robos(8));
        for (Partida partida : simples.partidas()) {
            Importancia esperada = switch (simples.nomeDaRodada(partida)) {
            case "Semifinal" -> Importancia.SEMIFINAL;
            case "Final" -> Importancia.FINAL;
            default -> Importancia.COMUM;
            };
            assertEquals(esperada, simples.importancia(partida), partida.codigo());
        }
        assertEquals(2, simples.partidas().stream().filter(partida -> simples.importancia(partida) == Importancia.SEMIFINAL).count());

        // com repescagem, são finais a da principal, a da repescagem e a grande final
        Chave dupla = new Chave(robos(8), true);
        for (Partida partida : dupla.partidas()) {
            String nome = dupla.nomeDaRodada(partida);
            boolean grande = nome.startsWith("Fim da") || nome.startsWith("Grande");
            assertEquals(grande, dupla.importancia(partida) == Importancia.FINAL, nome);
        }
    }
```

<!-- testar -->

## Passo 6 — Colocações e pódio

No fim do campeonato, o programa mostra quem ficou em que lugar. A `colocacoes()` devolve um `Map` do participante para a colocação, que também funciona **durante** o campeonato, para quem já foi eliminado: quem ainda joga não aparece. O campeão é o 1º, e os outros são ordenados por **até onde chegaram** antes de serem eliminados. Quem foi eliminado na mesma rodada empata e divide a colocação.

Para isso, é preciso saber **onde cada um foi eliminado**. Aqui a repescagem muda a conta: na chave principal, perder uma partida não elimina ninguém, porque o perdedor ainda tem uma vaga na repescagem. Uma derrota elimina quando **nenhuma outra partida fica com o perdedor** da partida. O método `eliminaOPerdedor` procura, em todas as partidas, uma vaga que tenha esta partida como origem e fique com o perdedor (`!doVencedor`): se achar, o perdedor segue em frente, e se não achar, está fora. Na eliminação simples, toda derrota elimina, e na repescagem, só as derrotas nas partidas `R` e na `F1`.

O resto é aritmética, em três etapas:

1. Para cada partida finalizada que elimina o perdedor, guarda-se a **profundidade** dela, no mapa `eliminadoEm`. A partida de profundidade maior é mais longe do começo, então quem foi eliminado nela chegou mais longe.
2. A lista dos eliminados é ordenada da profundidade maior para a menor, com `Comparator.comparing(eliminadoEm::get).reversed()`: o `eliminadoEm::get` é uma referência a método, que extrai a profundidade de cada participante, e o `reversed()` inverte a ordem, do maior para o menor.
3. A colocação de um eliminado é `1 + aindaJogando + (1 se já há campeão) + chegaramMaisLonge`. Ou seja: quem ainda joga e o campeão estão na frente dele, e mais os que foram eliminados mais longe. Os eliminados na mesma profundidade têm o mesmo `chegaramMaisLonge`, e dividem a colocação; a seguinte, então, é pulada.

Um exemplo, na chave de 4 sem repescagem, depois de `V1-1` e `V1-2`: os dois perdedores foram eliminados na profundidade 1, os dois vencedores ainda jogam e não há campeão. A colocação de cada perdedor é `1 + 2 + 0 + 0 = 3`, e os dois dividem o 3º lugar.

O mapa devolvido é um `LinkedHashMap`, que **guarda a ordem em que as entradas foram postas**, diferente do `HashMap`, que não garante ordem nenhuma. Assim, o campeão vem primeiro, e a ordem do mapa é a da colocação. O `(int)` na contagem é uma conversão: o `count()` devolve um `long`, e o campo é `int`.

O `podio()` pega o campeão e, para o 2º e o 3º lugar, o **primeiro** participante do mapa com aquela colocação. Se houver empate no 3º lugar, como na eliminação simples, em que os dois perdedores das semifinais dividem o posto, o pódio mostra um deles, o que vier antes no mapa (o exercício 6 dá um critério melhor). O `entrada.getValue() == colocacao` compara um `Integer` com um `int`: o Java converte o `Integer` em número antes de comparar, e é uma comparação de números. Comparar dois `Integer` com `==` seria outra história: compararia objetos, e o resultado dependeria do tamanho do número.

**Acrescente os imports** no topo de `Chave.java`:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | importar -->
```java
import java.util.LinkedHashMap;
```

**Acrescente ao final da classe** `Chave`:

<!-- arquivo: src/main/java/campeonato/torneio/Chave.java | acrescentar -->
```java

    /**
     * As colocações conhecidas até agora, da melhor para a pior. O campeão é o primeiro, e os outros são ordenados por
     * quão longe chegaram antes de serem eliminados; empatados dividem a colocação. Quem ainda joga não aparece.
     */
    public Map<String, Integer> colocacoes() {
        String campeao = campeao();

        // a profundidade da partida em que cada participante foi eliminado: quanto maior, mais longe ele chegou
        Map<String, Integer> eliminadoEm = new HashMap<>();
        for (Partida partida : partidas) {
            if (partida.situacao == Situacao.FINALIZADA && eliminaOPerdedor(partida)) {
                eliminadoEm.put(partida.perdedor, partida.profundidade);
            }
        }

        List<String> eliminados = new ArrayList<>(eliminadoEm.keySet());
        eliminados.sort(Comparator.comparing(eliminadoEm::get).reversed());

        int aindaJogando = (int) participantes.stream()
                .filter(participante -> !eliminadoEm.containsKey(participante) && !participante.equals(campeao))
                .count();

        Map<String, Integer> colocacoes = new LinkedHashMap<>();
        if (campeao != null) {
            colocacoes.put(campeao, 1);
        }
        for (String participante : eliminados) {
            int profundidade = eliminadoEm.get(participante);
            long chegaramMaisLonge = eliminadoEm.values().stream().filter(outra -> outra > profundidade).count();
            colocacoes.put(participante, 1 + aindaJogando + (campeao != null ? 1 : 0) + (int) chegaramMaisLonge);
        }
        return colocacoes;
    }

    /** Os três melhores: o campeão, o segundo e o terceiro, nessa ordem; vazio enquanto o campeonato não acaba. */
    public List<String> podio() {
        List<String> podio = new ArrayList<>();
        if (campeao() == null) {
            return podio;
        }
        Map<String, Integer> colocacoes = colocacoes();
        podio.add(campeao());
        for (int colocacao = 2; colocacao <= 3; colocacao++) {
            for (Map.Entry<String, Integer> entrada : colocacoes.entrySet()) {
                if (entrada.getValue() == colocacao) {
                    podio.add(entrada.getKey());
                    break;
                }
            }
        }
        return podio;
    }

    /** Diz se perder esta partida tira o perdedor do campeonato: nenhuma outra partida fica com o perdedor dela. */
    private boolean eliminaOPerdedor(Partida partida) {
        for (Partida outra : partidas) {
            if ((outra.a.origem == partida && !outra.a.doVencedor) || (outra.b.origem == partida && !outra.b.doVencedor)) {
                return false;
            }
        }
        return true;
    }
```

<!-- compilar -->

Os testes conferem quatro situações. A primeira, o exemplo acima: na chave de 4, depois das duas primeiras partidas, `Map.of("robo4", 3, "robo3", 3)`. A segunda, a chave de 2 com repescagem, em que o campeão é o 1º e o vice, o 2º. A terceira, o campeonato em que **o melhor sempre vence**, e as colocações são as das sementes, com os empates esperados: o 5º lugar dividido por dois, e o 7º também. E a quarta, o pódio, em campeonatos de 3 a 12 robôs com repescagem: sempre três posições diferentes, a primeira é o campeão, e as colocações são as esperadas.

O `Map.of(chave, valor, chave, valor, ...)` é um mapa pronto, e `assertEquals` entre dois mapas compara o conteúdo, sem importar a ordem. **Acrescente ao final da classe** `ChaveTest`:

<!-- arquivo: src/test/java/campeonato/torneio/ChaveTest.java | acrescentar -->
```java

    /** Joga a chave inteira com o melhor sempre ganhando: quem tem a semente menor vence. */
    private static void jogarOsMelhoresGanham(Chave chave) {
        while (chave.proximaPartida().isPresent()) {
            Partida proxima = chave.proximaPartida().get();
            String a = proxima.a().participante();
            String b = proxima.b().participante();
            boolean aEMelhor = chave.participantes().indexOf(a) < chave.participantes().indexOf(b);
            chave.registrar(proxima.codigo(), aEMelhor ? a : b, 1, 0);
        }
    }

    @Test
    void asColocacoesQueSeConhecemDuranteOJogo() {
        Chave chave = new Chave(robos(4));
        chave.registrar("V1-1", "robo1", 1, 0);
        chave.registrar("V1-2", "robo2", 1, 0);
        // os dois eliminados na primeira rodada dividem o terceiro lugar
        assertEquals(Map.of("robo4", 3, "robo3", 3), chave.colocacoes());
    }

    @Test
    void comDoisParticipantesAsColocacoesSaoAsDaGrandeFinal() {
        Chave chave = new Chave(robos(2), true);
        chave.registrar("V1-1", "robo1", 1, 0);
        // quem perdeu a V1-1 ainda joga a grande final, e por isso ninguém foi eliminado
        assertTrue(chave.colocacoes().isEmpty());
        chave.registrar("F1", "robo2", 1, 0);
        assertEquals(Map.of("robo2", 1, "robo1", 2), chave.colocacoes());
    }

    @Test
    void seOMelhorSempreGanhaAsColocacoesSaoAsDasSementes() {
        Chave quatro = new Chave(robos(4), true);
        jogarOsMelhoresGanham(quatro);
        assertEquals(Map.of("robo1", 1, "robo2", 2, "robo3", 3, "robo4", 4), quatro.colocacoes());

        // numa chave de 8, quem cai na mesma rodada da repescagem divide a colocação
        Chave oito = new Chave(robos(8), true);
        jogarOsMelhoresGanham(oito);
        assertEquals(Map.of("robo1", 1, "robo2", 2, "robo3", 3, "robo4", 4, "robo5", 5, "robo6", 5, "robo7", 7, "robo8", 7), oito.colocacoes());
        assertEquals(List.of("robo1", "robo2", "robo3"), oito.podio());
    }

    @Test
    void oPodioTemOCampeaoOSegundoEOTerceiro() {
        for (int quantidade = 3; quantidade <= 12; quantidade++) {
            Chave chave = new Chave(robos(quantidade), true);
            assertTrue(chave.podio().isEmpty());
            jogarTudo(chave, new Random(quantidade));

            List<String> podio = chave.podio();
            assertEquals(3, podio.size(), "robôs: " + quantidade);
            assertEquals(chave.campeao(), podio.get(0));
            assertEquals(3, new HashSet<>(podio).size());
            Map<String, Integer> colocacoes = chave.colocacoes();
            assertEquals(1, colocacoes.get(podio.get(0)));
            assertEquals(2, colocacoes.get(podio.get(1)));
            assertEquals(3, colocacoes.get(podio.get(2)));
        }
    }
```

<!-- testar -->

## Passo 7 — Um campeonato inteiro, com repescagem

A `Simulacao` do capítulo 12 passa a aceitar um segundo argumento, `repescagem`, para ligar a repescagem. Ela escreve também o nome da rodada de cada partida, um aviso nas partidas importantes, e no fim o campeão, o pódio e as colocações. O `args[1].equals("repescagem")` usa o `equals` do capítulo 12, e o `args.length > 1 &&` vem antes para o `args[1]` não ser lido quando não existe: o `&&` para na primeira condição falsa, e o Java não chega a avaliar a segunda. **Substitua o método** `main` da `Simulacao`:

<!-- arquivo: src/main/java/campeonato/torneio/Simulacao.java | substituir-metodo main -->
```java
    /** Uso: Simulacao [quantidade de robôs] [repescagem]. */
    public static void main(String[] args) {
        int quantidade = args.length > 0 ? Integer.parseInt(args[0]) : 6;
        boolean repescagem = args.length > 1 && args[1].equals("repescagem");
        List<String> robos = new ArrayList<>();
        for (int numero = 1; numero <= quantidade; numero++) {
            robos.add("robo" + numero);
        }

        Chave chave = new Chave(robos, repescagem);
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

            String aviso = chave.importancia(partida) == Importancia.COMUM ? "" : "   [avisar: " + chave.importancia(partida) + "]";
            System.out.println("  " + partida.codigo() + " " + chave.nomeDaRodada(partida) + ": " + a + " x " + b + " -> venceu " + vencedor + aviso);
        }
        System.out.println("Campeão: " + chave.campeao());
        System.out.println("Pódio: " + chave.podio());
        System.out.println("Colocações: " + chave.colocacoes());
    }
```

<!-- compilar -->

Compile e rode, com 8 robôs e a repescagem ligada:

<!-- executar: | espera: Campeão: robo1 | proibe: Exception -->
```bash
mvn -q compile
java -cp target/classes campeonato.torneio.Simulacao 8 repescagem
```

A saída é esta:

```text
A chave, antes de qualquer partida:
  V1-1  robo1 x robo8  (pronta)
  V1-2  robo4 x robo5  (pronta)
  V1-3  robo2 x robo7  (pronta)
  V1-4  robo3 x robo6  (pronta)
  V2-1  Vencedor de V1-1 x Vencedor de V1-2  (aguardando)
  V2-2  Vencedor de V1-3 x Vencedor de V1-4  (aguardando)
  V3-1  Vencedor de V2-1 x Vencedor de V2-2  (aguardando)
  R1-1  Perdedor de V1-1 x Perdedor de V1-2  (aguardando)
  R1-2  Perdedor de V1-3 x Perdedor de V1-4  (aguardando)
  R2-1  Vencedor de R1-1 x Perdedor de V2-2  (aguardando)
  R2-2  Vencedor de R1-2 x Perdedor de V2-1  (aguardando)
  R3-1  Vencedor de R2-1 x Vencedor de R2-2  (aguardando)
  R4-1  Vencedor de R3-1 x Perdedor de V3-1  (aguardando)
  F1    Vencedor de V3-1 x Vencedor de R4-1  (aguardando)
As partidas, na ordem em que são jogadas:
  V1-1 Quartas de final: robo1 x robo8 -> venceu robo1
  V1-2 Quartas de final: robo4 x robo5 -> venceu robo5
  V1-3 Quartas de final: robo2 x robo7 -> venceu robo7
  V1-4 Quartas de final: robo3 x robo6 -> venceu robo6
  V2-1 Semifinal: robo1 x robo5 -> venceu robo5   [avisar: SEMIFINAL]
  V2-2 Semifinal: robo7 x robo6 -> venceu robo6   [avisar: SEMIFINAL]
  R1-1 Repescagem 1: robo8 x robo4 -> venceu robo4
  R1-2 Repescagem 1: robo2 x robo3 -> venceu robo2
  V3-1 Fim da chave principal: robo5 x robo6 -> venceu robo5   [avisar: FINAL]
  R2-1 Repescagem 2: robo4 x robo7 -> venceu robo4
  R2-2 Repescagem 2: robo2 x robo1 -> venceu robo1
  R3-1 Repescagem 3: robo4 x robo1 -> venceu robo1
  R4-1 Fim da repescagem: robo1 x robo6 -> venceu robo1   [avisar: FINAL]
  F1 Grande final: robo5 x robo1 -> venceu robo1   [avisar: FINAL]
Campeão: robo1
Pódio: [robo1, robo5, robo6]
Colocações: {robo1=1, robo5=2, robo6=3, robo4=4, robo2=5, robo7=5, robo3=7, robo8=7}
```

Algumas coisas valem a leitura. As 14 partidas são as `2 × 8 − 2` do passo 1. O `robo1`, o melhor, perdeu na semifinal da chave principal (`V2-1`), caiu para a `R2-2` e, ainda assim, voltou e ganhou o campeonato: é o que a repescagem permite. As partidas de repescagem aparecem **entre** as da chave principal, na ordem da profundidade: a `R1-1` é jogada antes da final da chave principal, porque já está pronta e tem menos partidas a esperar. Na grande final, o `robo5` e o `robo1` já tinham se enfrentado, e a chave só evita as revanches da `R2`, como mostra o exercício 4.

A coluna de colocações mostra os empates: o `robo2` e o `robo7` perderam na mesma rodada da repescagem e dividem o 5º lugar, e o `robo3` e o `robo8` dividem o 7º. Sem o argumento da repescagem, a simulação joga a chave simples, como antes:

<!-- executar: | espera: Campeão: robo6 | proibe: Exception -->
```bash
java -cp target/classes campeonato.torneio.Simulacao 6
```

## O que construímos

```text
src/main/java/campeonato/torneio/
├── Situacao.java      (cap. 12) em que pé está uma partida
├── Lado.java          (cap. 12) a parte da chave: principal, repescagem ou final
├── Vaga.java          (cap. 12) um lugar de uma partida: uma semente, ou o vencedor/perdedor de outra partida
├── Partida.java       (cap. 12) uma partida: as vagas, o código, a profundidade e o resultado
├── Importancia.java   (cap. 13) comum, semifinal ou final: decide se a partida é avisada
├── Chave.java         (cap. 12, 13) a chave: monta as rodadas, a repescagem e a grande final; registra resultados; nomes, colocações e pódio
└── Simulacao.java     (cap. 12, 13) joga uma chave inteira com vencedores sorteados

src/test/java/campeonato/torneio/
└── ChaveTest.java     (cap. 12, 13)
```

## Exercícios

**Fácil**

1. Rode a `Simulacao` com repescagem para 4, 8 e 16 robôs. Em cada caso, conte as partidas jogadas, quantas são da chave principal (as que começam com `V`), quantas são da repescagem (`R`) e quantas são a `F1`. Confira que o total é `2n − 2`, e descubra a regra para as da repescagem, com `n` potência de dois.
2. Rode com 3 robôs e repescagem, e leia a chave antes do jogo. Duas partidas têm uma `Folga` numa das vagas: uma já aparece como `folga`, e a outra aparece como `aguardando`. Quais são, e por que a segunda só é decidida depois que outra partida termina? Conte as partidas jogadas: continuam sendo `2n − 2`?
3. Escreva um teste que confere os nomes de todas as rodadas de uma chave de 8 robôs com repescagem, na ordem das partidas: `Quartas de final`, `Semifinal`, `Fim da chave principal`, `Repescagem 1`, `Repescagem 2`, `Repescagem 3`, `Fim da repescagem` e `Grande final`. Dica: use a lista `chave.partidas()` e o `nomeDaRodada`, e um `distinct()` para não repetir o nome de rodadas com várias partidas.

**Médio**

4. Confira, com um teste, a vantagem da ordem invertida do passo 3. Jogue 200 campeonatos de 8 participantes com repescagem, vencedores sorteados, e conte as partidas da `R2` em que os dois participantes **já tinham se enfrentado** antes. O resultado esperado é zero. Depois troque `boolean invertida = (rodada / 2) % 2 == 1;` por `boolean invertida = false;` e rode o teste de novo: ele precisa falhar. Dica: guarde, num `Set<String>`, cada par já jogado, escrito sempre na mesma ordem (o menor nome primeiro).
5. Acrescente à `Chave` o método `int derrotasDe(String participante)`, que conta as partidas `FINALIZADA` em que o participante foi o `perdedor()`. Escreva um teste: numa chave de 4 com repescagem, depois de a `V1-1` ser vencida pelo `robo4`, o `robo1` tem 1 derrota e **não** aparece em `colocacoes()` (ainda joga). Jogue a `V1-2`, com a vitória do `robo2`, e a `R1-1`, que o `robo1` perde para o `robo3`: o `robo1` passa a ter 2 derrotas e aparece em `colocacoes()`.

**Difícil**

6. Dê um critério melhor ao 3º lugar da eliminação simples: o 3º do pódio é o que perdeu a semifinal **com mais pontos**, ou seja, com o maior `pontosDoPerdedor()` na partida em que foi eliminado. Escreva um método auxiliar `int pontosNaEliminacao(String participante)` e use-o no `podio()` para escolher, entre os que têm a colocação 3, o de maior pontuação (um `max` com `Comparator.comparingInt`). Escreva um teste: numa chave de 4 sem repescagem, a `V1-1` vencida pelo `robo1` por 100 a 40, a `V1-2` pelo `robo2` por 100 a 70, e a final pelo `robo1`: o pódio é `robo1`, `robo2` e `robo3`.

## Perguntas para fixação

1. Por que uma chave com repescagem e `n` robôs joga sempre `2n − 2` partidas, quem quer que ganhe?
2. Por que, na `R2`, o vencedor da `R1-1` enfrenta o perdedor da `V2-2`, e não o da `V2-1`? O que acontece na `R4`?
3. Com dois participantes e repescagem, a `F1` existe? De onde vêm as suas duas vagas?
4. Por que o `eliminaOPerdedor` é necessário? O que aconteceria nas colocações se toda derrota contasse como eliminação?
5. O nome de uma rodada e a sua importância têm origem na mesma conta. Por que a `Chave` tem um `enum` `Importancia`, em vez de o programa comparar o nome com `"Semifinal"`?

<details>
<summary>Respostas</summary>

1. Cada partida tem um perdedor, então o número de partidas é o número de derrotas. Cada robô, exceto os dois finalistas, é eliminado com exatamente duas derrotas, e isso dá `2 × (n − 2)`. Quanto aos finalistas, há dois casos. Se o campeão veio da chave principal, não perdeu nenhuma vez, e o vice, que veio da repescagem, perdeu duas. Se o campeão veio da repescagem, perdeu uma vez, e o vice, que veio da principal, também perdeu uma. Nos dois casos, as derrotas dos finalistas somam 2, e o total é `2 × (n − 2) + 2 = 2n − 2`.
2. A `R1-1` reúne os perdedores da `V1-1` e da `V1-2`, e a `V2-1`, os vencedores das duas. Os dois lados já se enfrentaram: se o vencedor da `R1-1` encontrasse o perdedor da `V2-1`, haveria revanche de uma partida que acabou de acontecer. Ao inverter, ele encontra o perdedor da `V2-2`, que nunca jogou contra ele. Na `R4` a ordem volta ao normal, pois `(rodada / 2) % 2` alterna entre 1 e 0 a cada rodada par.
3. Existe: o perdedor da `V1-1` ainda tem a segunda vida, e a única maneira de usá-la é uma partida contra o vencedor. As vagas da `F1` são o vencedor e o perdedor da `V1-1`, esta última escolhida no `construir` quando a repescagem está vazia.
4. Porque, na chave principal, perder não elimina: o perdedor ainda joga na repescagem. Sem o `eliminaOPerdedor`, todo perdedor de uma partida `V` apareceria nas colocações como eliminado, na profundidade em que perdeu, e quem ainda joga estaria no meio da lista como se já tivesse saído. O `eliminaOPerdedor` olha se alguma outra partida recebe o perdedor, e só conta como eliminação quando nenhuma recebe.
5. O nome é um texto para mostrar, e pode mudar (uma tradução, uma correção de ortografia, um nome novo). Se a decisão de avisar dependesse do texto, mudá-lo desligaria o aviso sem erro nenhum. O `enum` `Importancia` é um valor da lógica, que o compilador confere: um valor que não existe não compila, e um `switch` sobre ele precisa tratar todos os casos.

</details>

No próximo capítulo, a chave aparece na tela: as rodadas em colunas, as partidas em caixas coloridas e a próxima partida em destaque.
