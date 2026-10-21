<p align="center"><a href="README.md">Versão em português</a></p>

<div align="center">

# 🤖 Robocode Championship Manager

**Run [Robocode](https://robocode.sourceforge.io/) championships with an arcade look:
web registration, automatic brackets, 3D battles and a podium ceremony.**

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![JavaFX 21](https://img.shields.io/badge/JavaFX-21-blue)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven)
![License GPLv3](https://img.shields.io/badge/license-GPLv3-green)

</div>

---

## Contents

- [What it does](#-what-it-does)
- [The arenas](#-the-arenas)
- [Quick start](#-quick-start)
- [Tutorial: running a championship](#-tutorial-running-a-championship)
  1. [Configure](#1-configure)
  2. [Open registration](#2-open-registration)
  3. [Draw the bracket](#3-draw-the-bracket)
  4. [Run the battles](#4-run-the-battles)
  5. [The podium ceremony](#5-the-podium-ceremony)
- [Tutorial: registering your team's robot](#-tutorial-registering-your-teams-robot)
- [All controls](#-all-controls)
- [Championship rules](#-championship-rules)
- [Installers](#-installers)
- [For developers](#-for-developers)
- [License](#-license)

---

## ✨ What it does

| | |
|---|---|
| 🌐 **Web registration** | Teams register their robot, source code, logo, motto and theme song on a website served by the program itself. |
| 🏆 **Automatic brackets** | Single or double elimination (with repechage), with any number of robots, and an optional rematch in the grand final. |
| 🎮 **3D battles** | Robocode runs hidden; the battle is recorded and replayed in 3D with a fighting-game HUD, a cinematic camera and a slow-motion replay of the killing shot. |
| 🌋 **Four arenas** | Neon city, volcanic forge, glacial tundra and tropical forest, each with its own critters, who run away when the battle heats up. |
| 🎵 **Music and sound** | Each team comes in with its own music; shots, explosions, tracks and turrets have their own sounds. |
| 🥇 **Podium** | At the end the champion appears, then third, second and first place are revealed one by one, with bronze, silver and gold trophies. |
| ↩️ **Undo** | Entered a wrong result? `Ctrl + Z` undoes the last one. |

Everything runs full screen, with menus and dialogs in the style of an arcade game.

![Start screen](docs/images/start.jpg)

---

## 🌍 The arenas

Each battle takes place in an arena drawn at random (or a fixed one, if you prefer).

<table>
<tr>
<td width="50%"><img src="docs/images/batalha-neon.jpg" alt="Neon Grid"><br><b>Neon Grid</b>: a neon city over a cyan grid.</td>
<td width="50%"><img src="docs/images/batalha-vulcao.jpg" alt="Volcanic Forge"><br><b>Volcanic Forge</b>: lava, basalt and volcanoes.</td>
</tr>
<tr>
<td><img src="docs/images/batalha-gelo.jpg" alt="Glacial Tundra"><br><b>Glacial Tundra</b>: ice crystals, falling snow and a glacial sea with penguins.</td>
<td><img src="docs/images/batalha-selva.jpg" alt="Tropical Forest"><br><b>Tropical Forest</b>: dense jungle, rivers and waterfalls.</td>
</tr>
</table>

Before the first round the camera flies over the arena, starting with the critters that live there.
They never enter the battle area, but they get startled by shots and explosions.

![A salamander from the Volcanic Forge](docs/images/bicho-vulcao.jpg)

---

## 🚀 Quick start

**You need:**

- **Java 21** or newer (the JDK, to build) and **Maven**. JavaFX, Jetty, Hibernate, SQLite and the
  MP3 decoder are downloaded by Maven; no system codecs are needed.
- A **Robocode** installation to run the battles (tested with Robocode 1.11.1).

**To run:**

```bash
./run.sh            # builds when needed and opens the program
./run.sh --build    # forces a build
./run.sh --test     # runs the tests
```

or, without the script:

```bash
mvn package                                  # builds and runs the tests
java -jar target/RobocodeChampManager-5-all.jar
# or
mvn javafx:run
```

> [!IMPORTANT]
> Run the program from this folder: the battles are started by
> `RobocodeStarter/start.sh` (or `start.bat` on Windows).

Prefer an installer? See [Installers](#-installers).

---

## 📖 Tutorial: running a championship

### 1. Configure

The first time the program opens, it asks for the configuration. After that, press **`C`** at any time to
change it. Changes apply right away, with no restart: the web server moves to the new port and the
new database file is opened immediately.

![The four configuration tabs](docs/images/config.jpg)

| Tab | What's there |
|---|---|
| **Start screen** | Start screen title and subtitle (an empty subtitle is not shown), whether registrations are open, and the registration web server port (8080 by default). |
| **Battle** | Rounds per battle, battle winner rule, display (3D in the manager or the classic Robocode window), starting speed and the round start text (`FIGHT!`). |
| **Arena and sound** | Arena and music (random or fixed), "use the arena's own music", volume and anti-aliasing (turn it off if battles run slowly). |
| **System** | The SQLite database file and the Robocode folder (the one that has the `robots` folder). |

The configuration is stored in `~/robocodeChampManager.config` and the data in
`~/RobocodeChampManager/robochamp.db` (tables are created automatically).

### 2. Open registration

With registrations open, the start screen shows the address where teams register their robots.
Put that screen up where everyone can see it! **`Ctrl + S`** opens or closes registrations.

You don't need to memorize the shortcuts: **hold `Alt`** on the start screen and they appear.

![Holding Alt on the start screen](docs/images/keys.jpg)

The steps for teams are in
[Tutorial: registering your team's robot](#-tutorial-registering-your-teams-robot).

### 3. Draw the bracket

Once everyone has registered, press **`Ctrl + G`** to draw a championship with all registered robots,
then **`R`** to see the bracket.

![The bracket](docs/images/bracket.jpg)

- The main bracket is on top, the repechage below, and the finals and standings on the side.
- Click a match to select it; after each result the next match is selected automatically.
- **`+`**, **`-`** and **`0`** zoom in, zoom out and show the whole bracket (or `Ctrl` + mouse wheel);
  drag, use the wheel or the arrows to move around.

After a result, the bracket shows what happened like a game: the camera goes to the match, the winner
and loser are revealed, the robots walk to their next matches, and the camera zooms in on the next
battle. Any key or click skips the animation.

![The whole bracket](docs/images/bracket-wide.jpg)

### 4. Run the battles

**`N`** selects the next match and **`B`** shows the versus screen:

![Versus screen](docs/images/versus.jpg)

**`M`** presents the robots of the match, with team, motto and developers:

![Robot presentation](docs/images/robot.jpg)

**`S`** starts the battle. First each robot enters the scene to its team's music...

![Robot presented in the arena](docs/images/apresentacao-3d.jpg)

...then the two face each other, the camera flies over the arena, and each round starts with a 3-second countdown.

![The two robots face to face](docs/images/versus-3d.jpg)

During the fight, each robot's energy bar sits at the top of the screen. The tanks use the colors the
robot chose in its code, and shots come out in the color of whoever fired them. When a robot is
destroyed, the moments before its death are replayed in slow motion, following the fatal shot.

![Battle in the Glacial Tundra](docs/images/batalha-gelo.jpg)

| During the 3D battle | |
|---|---|
| `V` | Changes the camera: **CINEMATIC** (follows the action by itself), **FREE**, **ISOMETRIC** and **TOP VIEW** |
| `Space` | Pause |
| `←` `→` (or `-` `+`) | Slower / faster |
| `Enter` | Skips what is playing (presentation, fly-over or replay); otherwise goes to the result |
| `Esc` | The same, and skips the presentation of all robots |
| Drag | Orbits the camera (dragging in the cinematic camera switches to the free one) |
| Mouse wheel | Zoom |

The winner is recorded automatically. If you need to declare the result by hand, **`1`** and **`2`** give
the win to the first or second robot; **`Ctrl + Z`** undoes the last result.

### 5. The podium ceremony

When the last battle ends, the screen fades to black, the music swells and the champion appears:

![The champion](docs/images/campeao.jpg)

Then **`Space`**, **`Enter`** or **`→`** reveal third, second and first place, one at a time, each with
their robot, team, motto and trophy.

<table>
<tr>
<td width="50%"><img src="docs/images/podio-terceiro.jpg" alt="Third place revealed"></td>
<td width="50%"><img src="docs/images/podio.jpg" alt="The full podium"></td>
</tr>
</table>

**`←`** goes back one place and **`R`** opens the bracket. Once the championship is over, `S`, `1`, `2`, `B`
and `M` bring the podium back.

---

## 📝 Tutorial: registering your team's robot

> This part is for the teams. The address is shown on the championship's start screen, something like
> `http://192.168.0.10:8080`.

**1. Open the address in your browser.** If your team has no login yet, click **Register your team**.
If it already does, sign in with your login and password to change your registration.

![Entry page](docs/images/web-login.jpg)

**2. Fill in the registration form.**

<img src="docs/images/web-cadastro.jpg" alt="Registration form" width="480" align="right">

- **Login details**: a login and a password that the whole team can use.
- **Team details**:
  - the **motto**, the battle cry shown during presentations and on the podium;
  - the **theme song**, an `.mp3` of up to 1 MB that plays when the robot enters the arena;
  - the **logo**, a `.gif`, `.jpg` or `.png` of up to 640 x 480 pixels and 1 MB.
- **Developers**: name and email of at least one person. Use **+ Add person** for the others.
- **Robot data**:
  - the **compiled robot**, the `.jar` produced by the Robocode packager
    (*Robot → Package robot for upload*);
  - the **source code**, the `.java` file.

**3. Click Save.** Done! Until registrations close, you can sign in again and change your robot as
many times as you like.

<br clear="right">

> [!TIP]
> Choose your tank's colors in the robot's code, with `setColors(...)`: the 3D arena uses those colors.

---

## ⌨️ All controls

Hold **`Alt`** on the start screen to see this list.

| Key | Action |
|---|---|
| `I` | Start screen |
| `Ctrl + G` | Draws a new championship |
| `Ctrl + S` | Opens or closes registrations |
| `R` | Bracket (click a match to select it) |
| `N` | Selects the next match (and flies to it) |
| `B` | Versus screen of the selected match |
| `S` | Runs the selected match and records the winner |
| `1` / `2` | Declares the first / second robot the winner |
| `Ctrl + Z` | Undoes the last result |
| `M` | Shows the robots of the match (at the end, the champion) |
| `X` | Exports the robots' source code to `<temp folder>/robochamp-export` |
| `C` | Configuration |
| `Ctrl + Q` | Quits (the window is always full screen, with no close button) |
| `+` / `-` / `0` | In the bracket: zoom in, zoom out, show the whole bracket |

The [3D battle](#4-run-the-battles) and [podium](#5-the-podium-ceremony) controls are in their
tutorials above.

---

## 📜 Championship rules

- **Any number of robots** (two or more). When it isn't a power of two, the first robots drawn go
  straight to the second round.
- **With repechage** (double elimination, the default): whoever loses in the main bracket goes to the
  repechage and is only out after a second loss. The main bracket champion and the repechage champion
  play the grand final.
- **Rematch final**: if the repechage champion wins the grand final, it is played again, so the
  champion never has two losses.
- **Without repechage**: single elimination.
- **Who wins a battle** is configurable: most rounds won (total score breaks ties, the default), or
  total score (rounds won break ties). The two can disagree: a robot may win more rounds with fewer
  points. The ranking Robocode reports itself is not used, because it can be wrong (it is computed
  before the last round counts).

Robots are copied to `<robocode>/robots` as `team.Robot.jar` before each battle.

---

## 📦 Installers

The installers bring their own Java, but **not** Robocode: on first start, the program asks for the
folder where Robocode is installed.

| System | Command | Result |
|---|---|---|
| Linux (needs Docker) | `packaging/build-linux-packages.sh` | `dist/linux/ubuntu/*.deb` (Ubuntu 24.04), `dist/linux/debian/*.deb` (Debian 13), `dist/linux/fedora/*.rpm`, `dist/linux/arch/*.pkg.tar.zst` |
| Current system | `packaging/build-installer.sh` | The package for the system it runs on |
| macOS | `packaging/build-installer.sh` | `dist/*.dmg` |
| Windows (needs WiX Toolset 3) | `packaging\build-installer.ps1` | `dist\*.exe` |

Each system builds its own installer; jpackage can't cross-build. The
`.github/workflows/installers.yml` workflow builds all three when a `v*` tag is pushed (or by hand)
and keeps them as artifacts.

> [!NOTE]
> The macOS installer is not signed: the first time, open the program with **Open** from the
> right-click menu.

---

## 🛠️ For developers

### Source organization

| Where | What |
|---|---|
| `src/model` | The JPA entities |
| `src/tournament` | The bracket rules (`Bracket`) and championship storage |
| `src/jetty` | The web server and the registration pages |
| `src/utils` | Utilities: configuration, database, battles... |
| `src/widgets` | Controls, windows and scenes (the 3D arenas are in `widgets/scenes`) |
| `RobocodeStarter/` | Pre-compiled program that controls the Robocode battles (source in the `robocodebattlestarter` submodule) |
| `test/` | Automated tests, run with `mvn test` |

### The battle starter

`RobocodeStarter/BattleStarter.java` is the program that runs the battles. After changing it,
compile it with:

```bash
javac --release 11 -cp "<robocode folder>/libs/*" -d RobocodeStarter RobocodeStarter/BattleStarter.java
```

### Upgrading from version 3

Version 3 stored data in H2; this version uses SQLite, and the old "levels" ranking was replaced by
the brackets. Robots have to be registered again (or the data copied by hand). Old plain-text
passwords keep working if copied.

---

## ⚖️ License

Distributed under the [GNU General Public License v3](LICENSE).

Made by André Furlan ([ensismoebius@gmail.com](mailto:ensismoebius@gmail.com)).
