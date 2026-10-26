# Antes de começar

Este tutorial constrói, do zero, uma versão reduzida do **Robocode Championship Manager**: um programa que organiza um campeonato de [Robocode](https://robocode.sourceforge.io/), roda as batalhas em segundo plano e depois as reproduz em 3D, com tiros, explosões e fogo nos tanques machucados.

Você vai escrever cada classe, ver cada resultado na tela e, no último capítulo, ter o programa inteiro funcionando.

## O que vamos construir

Ao final você terá um programa que:

- roda cada batalha do Robocode escondida, sem abrir a janela do Robocode, e grava tudo o que acontece, turno a turno;
- reproduz essa gravação em 3D: tanques que se movem, tiros, explosões, barras de energia e um placar;
- põe fogo e fumaça nos tanques que perderam metade da energia;
- monta a chave do campeonato, com eliminatória e repescagem, e decide quem enfrenta quem;
- mostra o campeão no fim.

O programa completo deste repositório tem muito mais (cinco arenas, apresentadores, música, inscrições pela web, pódio). O tutorial constrói o esqueleto que faz tudo isso funcionar, e o último capítulo mostra onde cada pedaço vive no código completo.

## O que você precisa saber

Você precisa ter escrito alguns programas, em qualquer linguagem: saber o que são variável, condição (`if`), repetição (`for`, `while`) e função. O resto é explicado quando aparece:

- **Java** é apresentado aos poucos, no momento em que cada recurso é usado. O [Apêndice A](90-apendice-java.md) reúne tudo numa página, para consulta.
- **3D** não é pré-requisito. Os capítulos 2 e 3 explicam o que é uma cena, uma câmera, uma luz e uma malha de triângulos.
- **Matemática** é a do ensino médio: ângulos, seno e cosseno, e um pouco de interpolação, que será explicada no caso.
- **Robocode** também não é pré-requisito. Você escreve um robô simples no capítulo 4.

Cada capítulo termina com algo funcionando na tela. Dá para parar no fim de qualquer um deles e continuar depois.

## Como o tutorial funciona

### A estrutura de cada capítulo

Todo capítulo segue a mesma ordem:

1. **Objetivo**: uma frase que diz o que o capítulo faz e por quê.
2. **Passos**: o código é construído em pequenos pedaços. Depois de cada pedaço você salva, compila ou executa e vê o resultado antes de continuar.
3. **Exercícios**, em três níveis (fácil, médio e difícil). Todos mexem no mesmo projeto que você está construindo.
4. **Perguntas para fixação**: perguntas que só se respondem entendendo o *porquê*, não relendo o código.

### Como os trechos de código aparecem

O texto sempre diz o que fazer com o código que vem logo abaixo. São quatro situações:

| O texto diz | O que você faz |
|---|---|
| **Crie o arquivo** `caminho/Arquivo.java` | Crie o arquivo com exatamente esse conteúdo. |
| **Acrescente ao final da classe** | Cole o código dentro da classe, depois do último método e antes da última chave `}` do arquivo. |
| **No topo do arquivo, junto dos outros `import`** | Acrescente as linhas `import` ao grupo que já existe no início do arquivo. O editor costuma sugerir o `import` sozinho quando um nome fica vermelho. |
| **Substitua o método** `nome` | Apague o método inteiro, da primeira linha da assinatura até a chave que o fecha, e cole o novo no lugar. |

Os caminhos são sempre relativos à pasta do projeto, `mini-campeonato`, que você cria mais adiante neste capítulo.

Você pode copiar e colar. Mesmo assim, digitar os trechos curtos ajuda a notar detalhes que passam batido na leitura, como um ponto e vírgula ou uma letra maiúscula.

### Como os nomes são escolhidos

Todo nome que **você** inventa (classes, métodos, variáveis) está em português e sem acento: `Tanque`, `posicionar`, `quantidadeDeRodadas`. Todo nome que pertence a uma biblioteca (JavaFX, Robocode, a biblioteca padrão do Java) aparece no original em inglês: `Group`, `Rotate`, `getChildren`, `AnimationTimer`.

Isso funciona como um aviso permanente: um nome em português pode ser trocado por outro à vontade; um nome em inglês tem de ser copiado exatamente, porque quem o definiu foi outra pessoa.

### Quando algo der errado

1. Leia a **primeira** mensagem de erro, a que está mais acima. As seguintes costumam ser consequência dela.
2. A mensagem diz o arquivo e a linha (`Arena3D.java:42`). Vá até lá e compare com o texto do tutorial, letra por letra.
3. Erros de `cannot find symbol` quase sempre são um `import` que falta ou uma letra maiúscula trocada.
4. Se nada resolver, o [Apêndice B](91-apendice-problemas.md) lista os problemas mais comuns.

## Instalando as ferramentas

Você vai instalar quatro coisas: o **JDK**, o **Maven**, um **editor** e o **Robocode**.

### 1. O JDK 21

O Java tem dois jeitos de existir no computador. O **JRE** só *executa* programas Java. O **JDK** (*Java Development Kit*) executa e também traz o **compilador** `javac`, que transforma o código que você escreve (arquivos `.java`) em instruções que a máquina virtual do Java (a JVM) entende (arquivos `.class`). Para programar, você precisa do JDK.

Este tutorial usa o **JDK 21**, uma versão de suporte longo (LTS). Instale o **Eclipse Temurin 21**, que é gratuito:

- **Windows**, no Prompt de Comando ou no PowerShell:

  ```text
  winget install EclipseAdoptium.Temurin.21.JDK
  ```

- **macOS**, com o [Homebrew](https://brew.sh):

  ```text
  brew install --cask temurin@21
  ```

- **Linux**: use o gerenciador de pacotes da sua distribuição. No Ubuntu 24.04, por exemplo:

  ```text
  sudo apt install openjdk-21-jdk
  ```

Se preferir um instalador com janelas, baixe o Temurin 21 em [adoptium.net](https://adoptium.net/installation).

Feche e abra o terminal de novo e confira:

<!-- executar: | espera: openjdk version "21 -->
```bash
java -version
```

A primeira linha da resposta deve conter `21`. Confira também o compilador:

<!-- executar: | espera: javac 21 -->
```bash
javac -version
```

Se o `javac` não for encontrado, você instalou só o JRE, ou o `PATH` não inclui a pasta `bin` do JDK. Reinstale seguindo um dos caminhos acima.

### 2. O Maven

O **Maven** é a ferramenta que cuida do projeto: baixa as bibliotecas de que ele depende, compila, roda os testes e empacota. Você descreve o projeto num arquivo `pom.xml` e o Maven faz o resto. Sem ele, você teria de baixar o JavaFX à mão e montar comandos enormes de compilação.

A [documentação do Maven](https://maven.apache.org/install.html) pede um JDK instalado (a versão atual, 3.10.0, funciona com JDK 8 ou mais novo) e dá este caminho para qualquer sistema: baixar o arquivo binário, extraí-lo e acrescentar a pasta `bin` dele ao `PATH`. Se o seu sistema tem um gerenciador de pacotes, é mais simples:

| Sistema | Comando |
|---|---|
| macOS (Homebrew) | `brew install maven` |
| Ubuntu e Debian | `sudo apt install maven` |
| Fedora | `sudo dnf install maven` |
| Windows (Chocolatey) | `choco install maven` |
| Windows (Scoop) | `scoop install main/maven` |

Abra um terminal **novo** e confira:

<!-- executar: | espera: Apache Maven 3 -->
```bash
mvn -v
```

A resposta começa com `Apache Maven 3` e mostra, mais abaixo, a versão do Java que o Maven encontrou. Ela precisa ser a 21.

### 3. Um editor

Qualquer editor serve, mas um que entenda Java ajuda muito: ele sublinha erros enquanto você digita, completa nomes e sugere os `import`. Três opções comuns são o IntelliJ IDEA, o Eclipse e o Visual Studio Code com o pacote *Extension Pack for Java*. Os três abrem uma pasta com um `pom.xml` como projeto Maven.

### 4. O Robocode

O **Robocode** é o jogo em que cada jogador programa um tanque. Ele é quem simula as batalhas; o nosso programa só o controla e mostra o resultado.

1. Baixe o instalador em [robocode.sourceforge.io](https://robocode.sourceforge.io/). O arquivo tem um nome parecido com `robocode-1.11.1-setup.jar`. O tutorial foi testado com a versão **1.11.1**.
2. Execute o instalador. Num terminal, na pasta onde o arquivo foi baixado:

   ```text
   java -jar robocode-1.11.1-setup.jar
   ```

   Ajuste o nome do arquivo para o da versão que você baixou. Em muitos sistemas, dois cliques no arquivo também funcionam.
3. **Anote a pasta** onde o Robocode foi instalado. A janela do instalador mostra a sugerida. Neste tutorial vamos chamá-la de `ROBOCODE`; nos exemplos para Linux e macOS ela é `~/robocode`.

Dentro dessa pasta existem duas que nos interessam: `libs`, com os arquivos `.jar` do Robocode, e `robots`, onde ficam os robôs. Confira:

<!-- executar: | espera: ok -->
```bash
test -f ~/robocode/libs/robocode.jar && test -f ~/robocode/robots/sample/Walls.class && echo ok
```

No Windows, confira com `dir C:\robocode\libs\robocode.jar` e `dir C:\robocode\robots\sample\Walls.class`, trocando `C:\robocode` pela pasta que você anotou.

Agora veja uma batalha do jeito comum, para saber o que vamos automatizar. Na pasta do Robocode, execute o script de abertura (`robocode.sh` no Linux e no macOS, `robocode.bat` no Windows). No menu **Battle**, escolha **New**, selecione os robôs `sample.Walls` e `sample.SpinBot`, clique em **Add** e depois em **Start Battle**. Os tanques lutam numa janela 2D. É essa batalha que o nosso programa vai rodar escondida e reproduzir em 3D.

## Preparando a pasta do projeto

Crie uma pasta vazia chamada `mini-campeonato`, em qualquer lugar do computador. Tudo o que o tutorial manda criar vai dentro dela. Abra essa pasta no editor.

No fim do tutorial ela terá esta forma (cada arquivo aparece no capítulo que o cria):

```text
mini-campeonato/
├── pom.xml
├── iniciador/
│   └── IniciadorDeBatalha.java
└── src/
    ├── main/java/campeonato/
    │   ├── cena/          (tudo o que é 3D)
    │   ├── robocode/      (conversa com o Robocode)
    │   └── torneio/       (a chave do campeonato)
    └── test/java/campeonato/
```

## Conferência

Antes de seguir, confirme:

- `java -version` mostra a versão 21 e `javac -version` responde;
- `mvn -v` responde e usa o Java 21;
- a pasta do Robocode tem `libs/robocode.jar` e `robots/sample/Walls.class`;
- você sabe o caminho completo da pasta do Robocode.

No próximo capítulo você cria o projeto e abre a primeira janela.
