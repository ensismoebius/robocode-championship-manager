# Tutorial: um campeonato de Robocode, passo a passo

Este tutorial constrói, do zero, uma versão reduzida do **Robocode Championship Manager**: um programa em Java que sorteia os robôs de um campeonato, monta a chave com repescagem, roda cada batalha do [Robocode](https://robocode.sourceforge.io/) em segundo plano e a reproduz em 3D, com tiros, explosões e fogo nos tanques machucados.

![A chave do campeonato, no meio do caminho](imagens/15-meio.jpg)

![Um tanque machucado, com fogo e fumaça](imagens/11-movimento.jpg)

Quem seguir todos os capítulos escreve cada classe, vê cada resultado na tela ou no terminal e termina com o programa inteiro funcionando. O projeto é **um só**, o `mini-campeonato`, e cada capítulo acrescenta uma peça a ele.

## Para quem é

Para quem já escreveu alguns programas, em qualquer linguagem, e sabe o que são variável, condição, repetição e função. Java, 3D, Robocode e a matemática necessária (ângulos, seno e cosseno, um pouco de interpolação) são explicados quando aparecem. A matemática é a do ensino médio.

## O que você precisa

- um computador com **Linux**, **macOS** ou **Windows**, com internet na primeira vez que o Maven rodar;
- o **JDK 21**, o **Maven**, um editor de código e o **Robocode 1.11.1**, todos instalados no [capítulo 0](00-antes-de-comecar.md);
- paciência para digitar e conferir: cada capítulo é uma sessão de trabalho, e os capítulos de 8 a 11, sobre efeitos visuais, são os mais longos.

## Como usar

1. Leia o [capítulo 0](00-antes-de-comecar.md): ele instala as ferramentas e explica como os trechos de código aparecem (criar um arquivo, acrescentar ao final de uma classe, substituir um método).
2. Siga os capítulos **em ordem**. Cada um parte do ponto em que o anterior terminou, e termina com algo funcionando. Dá para parar no fim de qualquer um.
3. Em cada capítulo, escreva o código aos poucos: depois de cada pedaço você salva, compila ou executa e vê o resultado antes de seguir.
4. Faça os exercícios (fácil, médio e difícil) e responda às perguntas para fixação, com as respostas escondidas para você conferir depois.
5. Quando algo der errado, leia a **primeira** mensagem de erro e procure-a no [Apêndice B](91-apendice-problemas.md).

## Os capítulos

| | Capítulo | O que fica pronto |
| --- | --- | --- |
| 0 | [Antes de começar](00-antes-de-comecar.md) | As ferramentas instaladas e a pasta do projeto. |
| 1 | [A primeira janela](01-primeira-janela.md) | O projeto Maven e uma janela com o relógio do programa. |
| 2 | [O mundo em 3D](02-mundo-3d.md) | A cena 3D, com câmera, luz, chão e cerca. |
| 3 | [O tanque](03-tanque.md) | O tanque, com torre e radar independentes do casco. |
| 4 | [Falando com o Robocode](04-falando-com-o-robocode.md) | Uma batalha rodando escondida, gravada em texto. |
| 5 | [Rodando a batalha de dentro do programa](05-batalha-dentro-do-programa.md) | A classificação e a gravação lidas por objetos Java. |
| 6 | [A batalha em movimento](06-batalha-em-movimento.md) | O replay em 3D, com barra de progresso. |
| 7 | [Tiros, explosões e destroços](07-tiros-e-explosoes.md) | Tiros, explosões e a física dos pedaços. |
| 8 | [O placar](08-placar-e-energia.md) | Nomes, barras de energia, rodadas e K.O. |
| 9 | [Fumaça, brasas e faíscas](09-fumaca-brasas-e-faiscas.md) | O sistema de partículas. |
| 10 | [A imagem da chama](10-a-imagem-da-chama.md) | A chama desenhada pixel a pixel. |
| 11 | [O corpo da chama](11-o-corpo-da-chama.md) | O fogo preso ao robô, que se arrasta quando ele anda. |
| 12 | [A chave do campeonato](12-a-chave-do-campeonato.md) | A chave de eliminação simples, com folgas. |
| 13 | [A repescagem](13-a-repescagem.md) | A segunda chance, a grande final, as colocações e o pódio. |
| 14 | [A chave na tela](14-a-chave-na-tela.md) | A chave numa janela, com cartões e linhas. |
| 15 | [O campeonato completo](15-o-campeonato-completo.md) | O programa inteiro, que desfaz e continua de onde parou. |
| A | [Apêndice A: recursos do Java](90-apendice-java.md) | Os recursos do Java usados, numa página, para consulta. |
| B | [Apêndice B: problemas comuns](91-apendice-problemas.md) | Mensagens de erro reais, a causa e o que fazer. |

Os capítulos 12 e 13 são lógica em Java puro, sem janela e sem Robocode, e por isso são conferidos só com testes automáticos.

## Convenções

- Todo nome que **você** inventa (classes, métodos, variáveis) está em português e sem acento, como `Tanque` e `posicionar`. Todo nome que pertence a uma biblioteca (JavaFX, Robocode, a biblioteca padrão do Java) aparece no original, em inglês, como `Group` e `getChildren`. Um nome em português pode ser trocado por outro à vontade; um em inglês tem de ser copiado exatamente.
- Os caminhos dos arquivos são relativos à pasta do projeto, `mini-campeonato`.
- Os comandos são mostrados para Linux e macOS e, quando mudam, para o Prompt de Comando do Windows.

## Como o tutorial foi conferido

Todo trecho de código do tutorial foi montado, na ordem em que aparece, num projeto novo, e compilado e executado: os testes automáticos rodam e passam, e as telas foram abertas e fotografadas, de onde saem as imagens da pasta [imagens](imagens/). O programa do capítulo 15 foi também executado de ponta a ponta com batalhas reais do Robocode, num campeonato completo, incluindo desfazer e continuar a partir do arquivo. Os comandos de instalação e os do Windows e do macOS não foram executados (veja o fim do capítulo 0).

As versões usadas são: Java 21, JavaFX 21.0.12, JUnit 6.1.3 e Robocode 1.11.1.

## O programa completo

Este tutorial imita o programa que está no resto do repositório. O fim do [capítulo 15](15-o-campeonato-completo.md) mostra, numa tabela, onde cada peça do tutorial está no programa completo (`Chave` é `Bracket`, `Arena3D` é `BattleArena3D`, e assim por diante) e o que o programa completo tem a mais.
