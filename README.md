<p align="center"><a href="README.en.md">English version</a></p>

<div align="center">

# 🤖 Robocode Championship Manager

**Organize campeonatos de [Robocode](https://robocode.sourceforge.io/) com cara de fliperama:
inscrições pela web, chaveamento automático, batalhas em 3D e cerimônia de pódio.**

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![JavaFX 21](https://img.shields.io/badge/JavaFX-21-blue)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven)
![Licença GPLv3](https://img.shields.io/badge/licen%C3%A7a-GPLv3-green)

![Tela inicial](docs/images/start.jpg)

</div>

---

## Sumário

- [O que ele faz](#-o-que-ele-faz)
- [As arenas](#-as-arenas)
- [Começo rápido](#-começo-rápido)
- [Tutorial: organizando um campeonato](#-tutorial-organizando-um-campeonato)
  1. [Configure](#1-configure)
  2. [Abra as inscrições](#2-abra-as-inscrições)
  3. [Sorteie o chaveamento](#3-sorteie-o-chaveamento)
  4. [Rode as batalhas](#4-rode-as-batalhas)
  5. [A cerimônia do pódio](#5-a-cerimônia-do-pódio)
- [Tutorial: inscrevendo o robô da sua equipe](#-tutorial-inscrevendo-o-robô-da-sua-equipe)
- [Todos os controles](#-todos-os-controles)
- [Regras do campeonato](#-regras-do-campeonato)
- [Instaladores](#-instaladores)
- [Para desenvolvedores](#-para-desenvolvedores)
- [Licença](#-licença)

---

## ✨ O que ele faz

| | |
|---|---|
| 🌐 **Inscrições pela web** | As equipes cadastram robô, código fonte, logotipo, lema e música tema num site servido pelo próprio programa. |
| 🏆 **Chaveamento automático** | Eliminatória simples ou dupla (com repescagem), com qualquer número de robôs, e final com revanche opcional. |
| 🎮 **Batalhas em 3D** | O Robocode roda escondido; a batalha é gravada e reproduzida em 3D com HUD de jogo de luta, câmera cinematográfica e replay em câmera lenta do golpe final. |
| 🌋 **Quatro arenas** | Cidade neon, forja vulcânica, tundra glacial e floresta tropical, cada uma com seus bichos, que fogem quando a batalha esquenta. |
| 🎵 **Música e som** | Cada equipe entra com sua música; tiros, explosões, esteiras e torres têm som próprio. |
| 🥇 **Pódio** | No fim, o campeão aparece e o terceiro, o segundo e o primeiro lugares são revelados um a um, com troféus de bronze, prata e ouro. |
| ↩️ **Desfazer** | Errou um resultado? `Ctrl + Z` desfaz o último. |

Tudo roda em tela cheia, com menus e diálogos no estilo de um jogo de fliperama.

---

## 🌍 As arenas

Cada batalha acontece numa arena sorteada (ou numa arena fixa, se você preferir).

<table>
<tr>
<td width="50%"><img src="docs/images/batalha-neon.jpg" alt="Grade Neon"><br><b>Grade Neon</b>: uma cidade neon sobre uma grade ciano.</td>
<td width="50%"><img src="docs/images/batalha-vulcao.jpg" alt="Forja Vulcânica"><br><b>Forja Vulcânica</b>: lava, basalto e vulcões.</td>
</tr>
<tr>
<td><img src="docs/images/batalha-gelo.jpg" alt="Tundra Glacial"><br><b>Tundra Glacial</b>: cristais de gelo, neve caindo e um mar glacial com pinguins.</td>
<td><img src="docs/images/batalha-selva.jpg" alt="Floresta Tropical"><br><b>Floresta Tropical</b>: mata fechada, rios e cachoeiras.</td>
</tr>
</table>

Antes do primeiro round a câmera sobrevoa a arena, começando pelos bichos que vivem nela.
Eles nunca entram na área de batalha, mas se assustam com os tiros e as explosões.

![Uma salamandra da Forja Vulcânica](docs/images/bicho-vulcao.jpg)

---

## 🚀 Começo rápido

**Você precisa de:**

- **Java 21** ou mais novo (o JDK, para compilar) e **Maven**. JavaFX, Jetty, Hibernate, SQLite e o
  decodificador de MP3 são baixados pelo Maven; não é preciso nenhum codec no sistema.
- Uma instalação do **Robocode** para rodar as batalhas (testado com o Robocode 1.11.1).

**Para rodar:**

```bash
./run.sh            # compila quando precisa e abre o programa
./run.sh --build    # força a compilação
./run.sh --test     # roda os testes
```

ou, sem o script:

```bash
mvn package                                  # compila e roda os testes
java -jar target/RobocodeChampManager-5-all.jar
# ou
mvn javafx:run
```

> [!IMPORTANT]
> Rode o programa a partir desta pasta: as batalhas são iniciadas por
> `RobocodeStarter/start.sh` (ou `start.bat` no Windows).

Prefere um instalador? Veja [Instaladores](#-instaladores).

---

## 📖 Tutorial: organizando um campeonato

### 1. Configure

Na primeira vez que o programa abre, ele pede a configuração. Depois, aperte **`C`** a qualquer
momento para mudá-la. As mudanças valem na hora, sem reiniciar: o servidor web muda de porta e
o novo banco de dados é aberto na mesma hora.

![As quatro abas da configuração](docs/images/config.jpg)

| Aba | O que tem lá |
|---|---|
| **Tela inicial** | Título e subtítulo da tela inicial (um subtítulo vazio não aparece), se as inscrições estão abertas e a porta do site de inscrições (8080 por padrão). |
| **Batalha** | Rounds por batalha, critério de vitória, exibição (3D no gerenciador ou a janela clássica do Robocode), velocidade inicial e o texto do início do round (`FIGHT!`). |
| **Arena e som** | Arena e música (sorteadas ou fixas), "usar a mesma música da arena", volume e a suavização de bordas (desligue se a batalha ficar lenta). |
| **Sistema** | O arquivo do banco SQLite e a pasta do Robocode (a que tem a pasta `robots`). |

A configuração fica em `~/robocodeChampManager.config` e os dados em
`~/RobocodeChampManager/robochamp.db` (as tabelas são criadas sozinhas).

### 2. Abra as inscrições

Com as inscrições abertas, a tela inicial mostra o endereço onde as equipes cadastram seus
robôs. Projete essa tela para todo mundo ver! **`Ctrl + S`** abre ou fecha as inscrições.

Não decore os atalhos: **segure `Alt`** na tela inicial e eles aparecem.

![Segurando Alt na tela inicial](docs/images/keys.jpg)

O passo a passo das equipes está em
[Tutorial: inscrevendo o robô da sua equipe](#-tutorial-inscrevendo-o-robô-da-sua-equipe).

### 3. Sorteie o chaveamento

Quando todos tiverem se inscrito, aperte **`Ctrl + G`** para sortear um campeonato com todos os
robôs cadastrados, depois **`R`** para ver o chaveamento.

![O chaveamento](docs/images/bracket.jpg)

- A chave principal fica em cima, a repescagem embaixo, as finais e a classificação ao lado.
- Clique numa partida para selecioná-la; depois de cada resultado a próxima é selecionada sozinha.
- **`+`**, **`-`** e **`0`** aproximam, afastam e mostram a chave inteira (ou `Ctrl` + roda do mouse);
  arraste, use a roda ou as setas para mover.

Depois de um resultado o chaveamento mostra o que aconteceu como num jogo: a câmera vai até a
partida, revela vencedor e perdedor, os robôs andam para as próximas partidas e a câmera
aproxima a próxima batalha. Qualquer tecla ou clique pula a animação.

![A chave inteira](docs/images/bracket-wide.jpg)

### 4. Rode as batalhas

**`N`** seleciona a próxima partida e **`B`** mostra a tela de versus:

![Tela de versus](docs/images/versus.jpg)

**`M`** apresenta os robôs da partida, com equipe, lema e quem os desenvolveu:

![Apresentação do robô](docs/images/robot.jpg)

**`S`** começa a batalha. Primeiro cada robô entra em cena ao som da música da sua equipe...

![Robô apresentado na arena](docs/images/apresentacao-3d.jpg)

...depois os dois se encaram, a câmera sobrevoa a arena e cada round começa com uma contagem de
3 segundos.

![Os dois robôs frente a frente](docs/images/versus-3d.jpg)

Durante a luta, a barra de energia de cada robô fica no alto da tela. Os tanques usam as cores
que o robô escolheu no seu código, e os tiros saem na cor de quem atirou. Quando um robô é
destruído, os momentos antes da sua morte são repetidos em câmera lenta, seguindo o tiro fatal.

![Batalha na Tundra Glacial](docs/images/batalha-gelo.jpg)

| Durante a batalha 3D | |
|---|---|
| `V` | Troca a câmera: **CINEMATOGRÁFICA** (segue a ação sozinha), **LIVRE**, **ISOMÉTRICA** e **VISTA DE CIMA** |
| `Espaço` | Pausa |
| `←` `→` (ou `-` `+`) | Mais devagar / mais rápido |
| `Enter` | Pula o que está passando (apresentação, sobrevoo ou replay); senão vai para o resultado |
| `Esc` | O mesmo, e pula a apresentação de todos os robôs |
| Arrastar | Gira a câmera (arrastar na câmera cinematográfica passa para a livre) |
| Roda do mouse | Zoom |

O vencedor é gravado sozinho. Se precisar declarar o resultado na mão, **`1`** e **`2`** dão a
vitória ao primeiro ou ao segundo robô; **`Ctrl + Z`** desfaz o último resultado.

### 5. A cerimônia do pódio

Quando a última batalha termina, a tela escurece, a música sobe e o campeão aparece:

![O campeão](docs/images/campeao.jpg)

Então **`Espaço`**, **`Enter`** ou **`→`** revelam o terceiro, o segundo e o primeiro lugares, um
de cada vez, cada um com seu robô, equipe, lema e troféu.

<table>
<tr>
<td width="50%"><img src="docs/images/podio-terceiro.jpg" alt="Terceiro lugar revelado"></td>
<td width="50%"><img src="docs/images/podio.jpg" alt="O pódio completo"></td>
</tr>
</table>

**`←`** volta um lugar e **`R`** abre o chaveamento. Com o campeonato encerrado, `S`, `1`, `2`, `B`
e `M` abrem o pódio de novo.

---

## 📝 Tutorial: inscrevendo o robô da sua equipe

> Esta parte é para as equipes. O endereço aparece na tela inicial do campeonato, algo como
> `http://192.168.0.10:8080`.

**1. Abra o endereço no navegador.** Se a sua equipe ainda não tem login, clique em
**Cadastre seu time**. Se já tem, entre com o login e a senha para alterar o cadastro.

![Página de entrada](docs/images/web-login.jpg)

**2. Preencha o cadastro.**

<img src="docs/images/web-cadastro.jpg" alt="Formulário de cadastro" width="480" align="right">

- **Dados de login**: um login e uma senha que toda a equipe pode usar.
- **Dados do time**:
  - o **lema**, o grito de guerra que aparece nas apresentações e no pódio;
  - a **música tema**, um `.mp3` de até 1 MB que toca quando o robô entra na arena;
  - o **logotipo**, um `.gif`, `.jpg` ou `.png` de até 640 x 480 pixels e 1 MB.
- **Quem desenvolveu**: nome e e-mail de pelo menos uma pessoa. Use
  **+ Adicionar pessoa** para as outras.
- **Dados do robô**:
  - o **robô compilado**, o `.jar` gerado pelo empacotador do Robocode
    (*Robot → Package robot for upload*);
  - o **código fonte**, o arquivo `.java`.

**3. Clique em Salvar.** Pronto! Até as inscrições fecharem, você pode entrar de novo e trocar
o robô quantas vezes quiser.

<br clear="right">

> [!TIP]
> Escolha as cores do seu tanque no código do robô, com `setColors(...)`: a arena 3D usa essas cores.

---

## ⌨️ Todos os controles

Segure **`Alt`** na tela inicial para ver esta lista.

| Tecla | Ação |
|---|---|
| `I` | Tela inicial |
| `Ctrl + G` | Sorteia um novo campeonato |
| `Ctrl + S` | Abre ou fecha as inscrições |
| `R` | Chaveamento (clique numa partida para selecioná-la) |
| `N` | Seleciona a próxima partida (e voa até ela) |
| `B` | Tela de versus da partida selecionada |
| `S` | Roda a partida selecionada e grava o vencedor |
| `1` / `2` | Declara o primeiro / segundo robô vencedor |
| `Ctrl + Z` | Desfaz o último resultado |
| `M` | Mostra os robôs da partida (no fim, o campeão) |
| `X` | Exporta o código fonte dos robôs para `<pasta temporária>/robochamp-export` |
| `C` | Configurações |
| `Ctrl + Q` | Sai (a janela fica sempre em tela cheia, sem botão de fechar) |
| `+` / `-` / `0` | No chaveamento: aproxima, afasta e mostra a chave inteira |

Os controles da [batalha 3D](#4-rode-as-batalhas) e do [pódio](#5-a-cerimônia-do-pódio) estão
nos seus tutoriais.

---

## 📜 Regras do campeonato

- **Qualquer número de robôs** (dois ou mais). Quando não é uma potência de dois, os primeiros
  sorteados avançam direto na primeira rodada.
- **Com repescagem** (eliminação dupla, o padrão): quem perde na chave principal vai para a
  repescagem e só sai depois da segunda derrota. O campeão da chave principal e o da repescagem
  fazem a grande final.
- **Final com revanche**: se o campeão da repescagem vencer a grande final, ela é jogada de novo,
  para que o campeão nunca tenha duas derrotas.
- **Sem repescagem**: eliminação simples.
- **Quem vence uma batalha** é configurável: mais rounds vencidos (a pontuação total desempata,
  o padrão) ou maior pontuação total (os rounds desempatam). Os dois podem discordar: um robô
  pode vencer mais rounds com menos pontos. A colocação que o próprio Robocode informa não é
  usada, porque pode estar errada (é calculada antes do último round contar).

Os robôs são copiados para `<robocode>/robots` como `equipe.Robo.jar` antes de cada batalha.

---

## 📦 Instaladores

Os instaladores trazem o próprio Java, mas **não** trazem o Robocode: na primeira vez, o
programa pergunta a pasta onde ele está instalado.

| Sistema | Comando | Resultado |
|---|---|---|
| Linux (precisa de Docker) | `packaging/build-linux-packages.sh` | `dist/linux/ubuntu/*.deb` (Ubuntu 24.04), `dist/linux/debian/*.deb` (Debian 13), `dist/linux/fedora/*.rpm`, `dist/linux/arch/*.pkg.tar.zst` |
| O sistema atual | `packaging/build-installer.sh` | O pacote do sistema onde roda |
| macOS | `packaging/build-installer.sh` | `dist/*.dmg` |
| Windows (precisa do WiX Toolset 3) | `packaging\build-installer.ps1` | `dist\*.exe` |

Cada sistema gera o seu próprio instalador, o `jpackage` não faz compilação cruzada. O
workflow `.github/workflows/installers.yml` gera os três quando uma tag `v*` é enviada (ou na
mão) e os guarda como artefatos.

> [!NOTE]
> O instalador do macOS não é assinado: na primeira vez, abra o programa pelo item **Abrir**
> do menu do botão direito.

---

## 🛠️ Para desenvolvedores

### Organização do código

| Onde | O quê |
|---|---|
| `src/model` | As entidades JPA |
| `src/tournament` | As regras do chaveamento (`Bracket`) e o armazenamento do campeonato |
| `src/jetty` | O servidor web e as páginas de inscrição |
| `src/utils` | Utilitários: configuração, banco de dados, batalhas... |
| `src/widgets` | Controles, janelas e cenas do programa (as arenas 3D estão em `widgets/scenes`) |
| `RobocodeStarter/` | Versão compilada do programa que controla as batalhas do Robocode (fonte no submódulo `robocodebattlestarter`) |
| `test/` | Testes automáticos, rodados com `mvn test` |

### O iniciador de batalhas

`RobocodeStarter/BattleStarter.java` é o programa que roda as batalhas. Depois de alterá-lo,
compile com:

```bash
javac --release 11 -cp "<pasta do robocode>/libs/*" -d RobocodeStarter RobocodeStarter/BattleStarter.java
```

### Atualizando da versão 3

A versão 3 guardava os dados em H2; esta usa SQLite, e o antigo ranking por "níveis" foi
trocado pelo chaveamento. Os robôs precisam ser cadastrados de novo (ou os dados copiados na
mão). Senhas antigas em texto puro continuam funcionando se forem copiadas.

---

## ⚖️ Licença

Distribuído sob a [GNU General Public License v3](LICENSE).

Feito por André Furlan ([ensismoebius@gmail.com](mailto:ensismoebius@gmail.com)).
