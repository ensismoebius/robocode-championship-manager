# Apêndice A — O Java do tutorial numa página

Os capítulos explicam cada recurso do Java no momento em que ele é usado. Este apêndice reúne os recursos por assunto, com um exemplo curto de cada um e o capítulo em que ele aparece pela primeira vez, para consulta rápida. Os exemplos são independentes do projeto, e cada um mostra, num comentário, o que o código escreve ou o valor que calcula.

## Tipos e contas

O Java exige que toda variável tenha um **tipo**, escolhido ao declarar e conferido pelo compilador antes de o programa rodar (capítulo 1).

| Tipo | Guarda | Exemplo |
| --- | --- | --- |
| `int` | número inteiro | `int rodadas = 3;` |
| `long` | inteiro muito grande, como o tempo em nanossegundos | `long agora = 1_000_000_000L;` |
| `double` | número com vírgula | `double energia = 87.5;` |
| `boolean` | verdadeiro ou falso | `boolean vivo = true;` |
| `char` | uma letra, entre aspas simples | `char tipo = 'F';` |
| `String` | um texto, entre aspas duplas | `String nome = "Vigia";` |

O ponto decimal do Java é o **ponto**: `87.5`, e não `87,5`. Um `final` na declaração faz a variável valer uma vez só: `final int TAMANHO = 96;`.

A divisão entre dois `int` dá um `int`, **sem a parte decimal**. Se um dos lados for `double`, o resultado é `double`. O `%` é o **resto** da divisão, e, em Java, o resto de um número negativo é negativo (capítulos 10 e 11). Um **cast** entre parênteses converte um tipo em outro, e o de `double` para `int` corta a parte decimal, sem arredondar (capítulo 8).

<!-- ilustrativo -->
```java
System.out.println(7 / 2);        // 3
System.out.println(7 / 2.0);      // 3.5
System.out.println(7 % 3);        // 1
System.out.println(-1 % 96);      // -1, e não 95
System.out.println((-1 + 96) % 96); // 95: o jeito de dar a volta num vetor circular
System.out.println((int) 3.99);   // 3
System.out.println((double) 7 / 2); // 3.5: o cast vale antes da divisão
```

O operador **condicional** `condição ? a : b` vale `a` se a condição é verdadeira, e `b` se é falsa (capítulo 1). Os operadores lógicos são `&&` (e), `||` (ou) e `!` (não), e os dois primeiros **param assim que o resultado é decidido**: em `texto != null && texto.isEmpty()`, o `isEmpty()` nunca é chamado se `texto` for `null`, e é por isso que essa ordem é segura (capítulo 12).

<!-- ilustrativo -->
```java
int energia = 30;
String situacao = energia > 50 ? "forte" : "fraco";
System.out.println(situacao);     // fraco

String texto = null;
System.out.println(texto != null && texto.isEmpty()); // false, sem erro
```

Os operadores de **bits** mexem nos 32 bits de um `int`: `>> 16` desloca os bits 16 casas para a direita, e `& 0xff` mantém só os 8 últimos (capítulo 6, no `Cores.deArgb`).

<!-- ilustrativo -->
```java
int cor = 0xff2ec4ff;
System.out.println((cor >> 16) & 0xff); // 46: o byte do vermelho
System.out.println(cor & 0xff);         // 255: o byte do azul
```

## Textos

Dois textos se comparam com `equals`, e não com `==`, que compara se são o mesmo objeto (capítulo 12). O `String.format` e o `printf` montam um texto a partir de um modelo, em que `%d` é um inteiro, `%s`, um texto, `%.1f`, um decimal com uma casa, e `%n`, uma quebra de linha (capítulos 1 e 5). O `Locale` decide se o separador decimal é ponto ou vírgula.

<!-- ilustrativo -->
```java
String a = new String("robo");
String b = "ro" + "bo";
System.out.println(a == b);       // false: objetos diferentes
System.out.println(a.equals(b));  // true: as mesmas letras
System.out.println(String.format(java.util.Locale.ROOT, "%s tem %d de energia (%.1f%%)", "Vigia", 87, 87.0));
// Vigia tem 87 de energia (87.0%)
```

Os métodos de texto mais usados são o `length()`, o `charAt(i)`, o `substring(i, j)`, o `indexOf(x)`, o `split(separador)`, o `trim()`, o `isBlank()`, o `startsWith(x)` e o `String.join(separador, partes)` (capítulos 4, 5 e 15).

<!-- ilustrativo -->
```java
String linha = "resultado=V1-2;sample.Walls;536;16";
int igual = linha.indexOf('=');
System.out.println(linha.substring(0, igual));      // resultado
String[] partes = linha.substring(igual + 1).split(";");
System.out.println(partes.length + " " + partes[1]); // 4 sample.Walls
System.out.println(String.join(" x ", "robo1", "robo2")); // robo1 x robo2
```

Um **bloco de texto** (`"""`) guarda várias linhas, e o Java tira o recuo comum a todas elas (capítulo 5, nos testes da gravação).

## Decisões e repetições

O `if`, o `for` e o `while` são os de outras linguagens. O `for (Tipo x : lista)` percorre todos os elementos de uma lista ou de um vetor, e o `break` e o `continue` saem do laço e passam à próxima volta (capítulos 3 a 5).

O `switch` do tutorial usa a seta `->`: cada caso tem o seu resultado, não precisa de `break`, e vários valores se juntam num caso só. Quando o `switch` **devolve um valor**, o compilador exige que todos os casos estejam cobertos, e para um `enum` isso quer dizer que acrescentar um valor novo ao `enum` faz o código que não o trata parar de compilar (capítulos 5 e 9).

<!-- ilustrativo -->
```java
String nome = switch (3) {
case 1 -> "Quartas";
case 2 -> "Semifinal";
case 3, 4 -> "Final";
default -> "outra";
};
System.out.println(nome);          // Final
```

## Vetores e matrizes

Um **vetor** tem tamanho fixo, definido ao criá-lo, e se acessa por índices a partir de **zero**. Uma **matriz** é um vetor de vetores, com um índice para cada dimensão, e as linhas podem ter tamanhos diferentes (capítulo 3, nos contornos do tanque).

<!-- ilustrativo -->
```java
double[] pontos = { 1.5, 2.5, 4.0 };
System.out.println(pontos.length + " " + pontos[2]);  // 3 4.0

double[][] contorno = { { 0, 0 }, { 10, 0 }, { 10, 5 } };
System.out.println(contorno[1][0]);                   // 10.0
```

## Coleções

Uma lista cresce e encolhe, o que o vetor não faz. O `List<String>` é a **interface**, o tipo que descreve o que uma lista faz, e o `ArrayList` é a implementação mais comum. O `List.of(...)` cria uma lista pronta que **não pode ser alterada**, e o `Collections.unmodifiableList` devolve uma visão só de leitura de outra (capítulos 5 e 12). Um `Map<Chave, Valor>` guarda pares, um `Set` guarda valores **sem repetição**, e o `LinkedHashMap` lembra a ordem em que as entradas foram postas, ao contrário do `HashMap` (capítulos 6, 12 e 13).

<!-- ilustrativo -->
```java
java.util.List<String> robos = new java.util.ArrayList<>(java.util.List.of("Walls", "SpinBot"));
robos.add("Tracker");
System.out.println(robos + " " + robos.size() + " " + robos.get(0)); // [Walls, SpinBot, Tracker] 3 Walls

java.util.Map<String, Integer> derrotas = new java.util.HashMap<>();
derrotas.merge("Walls", 1, Integer::sum);
derrotas.merge("Walls", 1, Integer::sum);
System.out.println(derrotas);       // {Walls=2}

System.out.println(java.util.Set.copyOf(java.util.List.of("a", "b", "a")).size()); // 2
```

Os genéricos entre `<` e `>` dizem o tipo dos elementos, e o `<>` sozinho, no lado direito, deixa o Java deduzir o tipo (capítulo 5).

## Classes, records, enums e interfaces

Uma **classe** reúne dados (campos) e comportamento (métodos). O **construtor** tem o nome da classe, e uma classe pode ter vários, com parâmetros diferentes (**sobrecarga**), e um pode chamar outro com `this(...)` (capítulo 13). O `private` limita o acesso à própria classe, o `public` abre para todos, e a ausência de um dos dois deixa o acesso **dentro do pacote** (capítulos 9 e 12). O `static` pertence à classe, e não a um objeto, e o `final` num campo diz que ele recebe valor uma vez só. O `@Override` avisa o compilador de que o método substitui o de uma classe de cima (capítulo 12). Um **pacote** (`package campeonato.torneio;`) agrupa classes, e o `import` traz uma classe de outro pacote.

Um **record** é uma classe que só guarda dados: o Java cria o construtor, os métodos de leitura, o `equals` e o `toString`, e os valores não mudam depois de criado (capítulo 5). Um **enum** é um conjunto fixo de valores, que também pode guardar dados e ter métodos, e compara-se com `==` (capítulos 9 e 12). Uma **interface** diz o que um tipo sabe fazer, sem dizer como (capítulos 5 e 6).

<!-- ilustrativo -->
```java
record Ponto(double x, double y) {
}

enum Situacao {
    AGUARDANDO("aguardando"), PRONTA("pronta");

    private final String texto;

    Situacao(String texto) {
        this.texto = texto;
    }

    String texto() {
        return texto;
    }
}
```

<!-- ilustrativo -->
```java
Ponto p = new Ponto(3, 4);
System.out.println(p + " " + p.x() + " " + p.equals(new Ponto(3, 4))); // Ponto[x=3.0, y=4.0] 3.0 true
System.out.println(Situacao.PRONTA.texto() + " " + (Situacao.PRONTA == Situacao.valueOf("PRONTA"))); // pronta true
```

## Funções como valores

Uma **lambda** é uma função escrita no lugar em que é usada: `(parâmetros) -> corpo`. Ela serve onde o Java espera uma interface de um método só, como o `Runnable` (sem parâmetros e sem resultado), o `Consumer<T>` (recebe um `T`), o `Function<T, R>` e o `Comparator<T>`. Uma **referência a método** é um atalho para uma lambda que só chama um método: `Partida::codigo` é o mesmo que `partida -> partida.codigo()` (capítulos 1, 2 e 12).

Uma lambda só pode usar variáveis locais que **nunca mudam** depois de definidas. Para guardar um valor que muda, usa-se um campo da classe, ou um vetor de um elemento só, cujo **conteúdo** pode mudar (capítulo 2).

<!-- ilustrativo -->
```java
java.util.function.Consumer<String> avisar = texto -> System.out.println("Aviso: " + texto);
avisar.accept("semifinal");           // Aviso: semifinal

java.util.List<String> nomes = java.util.List.of("Walls", "Tracker", "Fire");
System.out.println(nomes.stream().map(String::length).toList()); // [5, 7, 4]

int[] contador = { 0 };               // um vetor, para a lambda poder mudar o conteúdo
nomes.forEach(nome -> contador[0]++);
System.out.println(contador[0]);      // 3
```

## Fluxos, Optional e Comparator

Um **fluxo** (`stream`) descreve uma sequência de passos sobre uma coleção: `filter` fica com os que passam num teste, `map` transforma cada um, e o resultado sai por `toList`, `count`, `anyMatch`, `min`, `max` ou `average` (capítulos 5, 12 e 14). Um `Optional<T>` é uma caixa que pode estar vazia, e é a resposta honesta de um método que às vezes não tem o que devolver: o `isPresent()` e o `isEmpty()` perguntam, o `orElse(valor)` dá uma reserva, e o `orElseThrow()` lança uma exceção se estiver vazia. O `OptionalDouble` é o mesmo para `double` (capítulos 12 e 14). Um **`Comparator`** diz como ordenar, e se compõe: `comparingInt(...)` ordena por um número, e o `thenComparing(...)` desempata (capítulos 5 e 12).

<!-- ilustrativo -->
```java
java.util.List<String> robos = java.util.List.of("Walls", "SpinBot", "Fire", "Tracker");
System.out.println(robos.stream().filter(r -> r.length() > 4).count());        // 3
System.out.println(robos.stream().mapToInt(String::length).max().getAsInt());  // 7

java.util.Optional<String> primeiro = robos.stream().filter(r -> r.startsWith("F")).findFirst();
System.out.println(primeiro.orElse("nenhum"));                                 // Fire

java.util.List<String> ordenados = new java.util.ArrayList<>(robos);
ordenados.sort(java.util.Comparator.comparingInt(String::length).thenComparing(java.util.Comparator.naturalOrder()));
System.out.println(ordenados);      // [Fire, Walls, SpinBot, Tracker]
```

## Exceções

Uma **exceção** é o jeito de o Java avisar que algo impediu o código de continuar. O `throw new ...` a lança, e o `try { ... } catch (Tipo erro) { ... }` a captura. Um `catch` com `|` captura mais de um tipo de uma vez. As exceções de `RuntimeException`, como a `IllegalArgumentException` (argumento errado) e a `IllegalStateException` (momento errado), são livres. As **verificadas**, como a `IOException`, o compilador obriga a tratar: ou se captura, ou se declara no método com `throws` (capítulos 12 e 15). O `try (recurso) { ... }`, o *try-with-resources*, fecha o recurso sozinho ao sair do bloco, por qualquer caminho (capítulo 5).

<!-- ilustrativo -->
```java
try {
    Integer.parseInt("muitos");
} catch (NumberFormatException erro) {
    System.out.println("não é número: " + erro.getMessage()); // não é número: For input string: "muitos"
}

try {
    java.util.List.of(1, 2).add(3);
} catch (UnsupportedOperationException erro) {
    System.out.println("a lista não muda");                   // a lista não muda
}
```

## Arquivos

A classe `Path` é o caminho de um arquivo, e a `Files` lê e grava. Sempre se diz o **UTF-8** ao ler e gravar texto, para as letras acentuadas sobreviverem em qualquer computador (capítulo 15).

<!-- ilustrativo -->
```java
java.nio.file.Path arquivo = java.nio.file.Files.createTempFile("apendice", ".txt");
java.nio.file.Files.write(arquivo, java.util.List.of("repescagem=true", "robo=ação"), java.nio.charset.StandardCharsets.UTF_8);
System.out.println(java.nio.file.Files.readAllLines(arquivo, java.nio.charset.StandardCharsets.UTF_8)); // [repescagem=true, robo=ação]
java.nio.file.Files.delete(arquivo);
```

## Linhas de execução

Um programa pode fazer várias coisas ao mesmo tempo, cada uma numa **linha de execução** (*thread*). O JavaFX desenha a janela numa só linha, e o trabalho demorado vai para outra, para a janela não travar; **só a linha do JavaFX mexe nos componentes da tela** (capítulo 6).

## Comentários de documentação

Um comentário `/** ... */` logo acima de uma classe ou de um método é um comentário de **Javadoc**, que o editor mostra quando se passa o mouse sobre o nome. As linhas `@param`, `@return` e `@throws` documentam os parâmetros, o resultado e as exceções (capítulo 12).
