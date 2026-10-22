#!/usr/bin/env python3
"""
Seed script to register demo/test robots into the championship SQLite database.
Reads real packaged robots from ~/robocode/robots/*.jar and registers them
with their compiled JAR binaries, Java source code, logo, sound, warcry, and author info.
A jar that is missing is built from the sample robot of the same name that comes
with Robocode, with its package renamed to the team.
"""

import sqlite3
import subprocess
import tempfile
import zipfile
import os
import hashlib
import base64

def hash_pw(password: str) -> str:
    """A PBKDF2-SHA256 hash of the password, with a random salt, in the format stored in the database"""
    salt = os.urandom(16)
    digest = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, 210000, 32)
    return "pbkdf2$210000$" + base64.b64encode(salt).decode() + "$" + base64.b64encode(digest).decode()

def read_file(path):
    """The bytes of the file, or None if it does not exist"""
    if not os.path.exists(path):
        return None
    with open(path, 'rb') as handle:
        return handle.read()

def read_source(jar_file_path, src_file):
    """The Java source inside the jar, or None if it can not be read"""
    try:
        with zipfile.ZipFile(jar_file_path) as archive:
            return archive.read(src_file).decode('utf-8', errors='replace')
    except Exception as error:
        print(f"Could not extract source code from {jar_file_path}: {error}")
        return None

def build_jar(robot, robocode_dir):
    """Builds team.Name.jar from Robocode's sample robot, in the package of the team; nothing when the jar exists"""
    robots_dir = os.path.join(robocode_dir, 'robots')
    jar_path = os.path.join(robots_dir, robot["jar"])
    if os.path.exists(jar_path):
        return
    team, name = robot["team"], robot["name"]
    with open(os.path.join(robots_dir, 'sample', name + '.java'), encoding='utf-8') as handle:
        source = handle.read().replace('package sample;', f'package {team};', 1)

    with tempfile.TemporaryDirectory() as work:
        package_dir = os.path.join(work, team)
        os.makedirs(package_dir)
        with open(os.path.join(package_dir, name + '.java'), 'w', encoding='utf-8') as handle:
            handle.write(source)
        subprocess.run(['javac', '-encoding', 'UTF-8', '-cp', os.path.join(robocode_dir, 'libs', 'robocode.jar'),
                        '-d', work, os.path.join(package_dir, name + '.java')], check=True)
        properties = (f"robot.description={robot['warcry']}\nrobocode.version=1.11.1\nrobot.java.source.included=true\n"
                      f"robot.author.name={robot['author_name']}\nrobot.classname={team}.{name}\nrobot.name={name}\nrobot.version=1.0\n")
        with zipfile.ZipFile(jar_path, 'w', zipfile.ZIP_DEFLATED) as archive:
            archive.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\r\n\r\n')
            # The class, its inner classes and the source
            for entry in sorted(os.listdir(package_dir)):
                archive.write(os.path.join(package_dir, entry), f'{team}/{entry}')
            archive.writestr(f'{team}/{name}.properties', properties)
    print(f"Built {jar_path} from the sample robot {name}")

def register_robot(cur, robot, robots_dir, logo_data, music_data):
    """Inserts a robot and its author, unless a robot with the same team and name exists"""
    cur.execute("SELECT id FROM Opponent WHERE team = ? AND name = ?", (robot["team"], robot["name"]))
    existing = cur.fetchone()
    if existing:
        print(f"Robot {robot['team']}.{robot['name']} already registered with ID {existing[0]}. Skipping.")
        return

    jar_file_path = os.path.join(robots_dir, robot["jar"])
    jar_data = read_file(jar_file_path)
    source_code = read_source(jar_file_path, robot["src_file"]) if jar_data is not None else None

    cur.execute("""
        INSERT INTO Opponent (name, team, battleCry, compiledRobot, sourceCode, image, music, musicStart)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, (robot["name"], robot["team"], robot["warcry"], jar_data, source_code, logo_data, music_data, 0.0))

    opp_id = cur.lastrowid

    cur.execute("""
        INSERT INTO Author (login, mail, name, observations, password, opponentId)
        VALUES (?, ?, ?, ?, ?, ?)
    """, (robot["author_login"], robot["author_mail"], robot["author_name"], "Developer", hash_pw(robot["author_pw"]), opp_id))

    print(f"Registered robot {robot['team']}.{robot['name']} with ID {opp_id}")

DEMO_ROBOTS = [
    {
        "team": "alpha",
        "name": "Corners",
        "jar": "alpha.Corners.jar",
        "src_file": "alpha/Corners.java",
        "warcry": "Alpha Strike! Dominando os quatro cantos da arena!",
        "author_name": "Alan Turing",
        "author_mail": "alan@alpha.org",
        "author_login": "alpha",
        "author_pw": "123456"
    },
    {
        "team": "bravo",
        "name": "Crazy",
        "jar": "bravo.Crazy.jar",
        "src_file": "bravo/Crazy.java",
        "warcry": "Crazy moves, crazy wins! Ninguém prevê nossa loucura!",
        "author_name": "Grace Hopper",
        "author_mail": "grace@bravo.org",
        "author_login": "bravo",
        "author_pw": "123456"
    },
    {
        "team": "charlie",
        "name": "Walls",
        "jar": "charlie.Walls.jar",
        "src_file": "charlie/Walls.java",
        "warcry": "As paredes são nossos escudos e nossas armas!",
        "author_name": "Ada Lovelace",
        "author_mail": "ada@charlie.org",
        "author_login": "charlie",
        "author_pw": "123456"
    },
    {
        "team": "delta",
        "name": "SpinBot",
        "jar": "delta.SpinBot.jar",
        "src_file": "delta/SpinBot.java",
        "warcry": "Gira, esquiva e destrói! O furacão está solto!",
        "author_name": "Margaret Hamilton",
        "author_mail": "margaret@delta.org",
        "author_login": "delta",
        "author_pw": "123456"
    },
    {
        "team": "echo",
        "name": "Tracker",
        "jar": "echo.Tracker.jar",
        "src_file": "echo/Tracker.java",
        "warcry": "Rastreado e destruído! A mira nunca falha!",
        "author_name": "Claude Shannon",
        "author_mail": "claude@echo.org",
        "author_login": "echo",
        "author_pw": "123456"
    },
    {
        "team": "foxtrot",
        "name": "RamFire",
        "jar": "foxtrot.RamFire.jar",
        "src_file": "foxtrot/RamFire.java",
        "warcry": "Ataque frontal em alta velocidade! Bate e atropela!",
        "author_name": "John von Neumann",
        "author_mail": "john@foxtrot.org",
        "author_login": "foxtrot",
        "author_pw": "123456"
    },
    {
        "team": "golf",
        "name": "Fire",
        "jar": "golf.Fire.jar",
        "src_file": "golf/Fire.java",
        "warcry": "Parado e mirando! Quem se aproxima vira cinza!",
        "author_name": "Katherine Johnson",
        "author_mail": "katherine@golf.org",
        "author_login": "golf",
        "author_pw": "123456"
    },
    {
        "team": "hotel",
        "name": "TrackFire",
        "jar": "hotel.TrackFire.jar",
        "src_file": "hotel/TrackFire.java",
        "warcry": "Te achei, te segui, te derrubei!",
        "author_name": "Dennis Ritchie",
        "author_mail": "dennis@hotel.org",
        "author_login": "hotel",
        "author_pw": "123456"
    },
    {
        "team": "india",
        "name": "VelociRobot",
        "jar": "india.VelociRobot.jar",
        "src_file": "india/VelociRobot.java",
        "warcry": "Velocidade é tudo! Ninguém acerta o que não alcança!",
        "author_name": "Barbara Liskov",
        "author_mail": "barbara@india.org",
        "author_login": "india",
        "author_pw": "123456"
    },
    {
        "team": "juliett",
        "name": "Target",
        "jar": "juliett.Target.jar",
        "src_file": "juliett/Target.java",
        "warcry": "Pode mirar, o alvo aqui sabe se defender!",
        "author_name": "Edsger Dijkstra",
        "author_mail": "edsger@juliett.org",
        "author_login": "juliett",
        "author_pw": "123456"
    },
    {
        "team": "kilo",
        "name": "PaintingRobot",
        "jar": "kilo.PaintingRobot.jar",
        "src_file": "kilo/PaintingRobot.java",
        "warcry": "Pintamos a arena com a cor da vitória!",
        "author_name": "Frances Allen",
        "author_mail": "frances@kilo.org",
        "author_login": "kilo",
        "author_pw": "123456"
    },
    {
        "team": "lima",
        "name": "MyFirstRobot",
        "jar": "lima.MyFirstRobot.jar",
        "src_file": "lima/MyFirstRobot.java",
        "warcry": "Primeiro robô, primeira vitória!",
        "author_name": "Donald Knuth",
        "author_mail": "donald@lima.org",
        "author_login": "lima",
        "author_pw": "123456"
    },
    {
        "team": "mike",
        "name": "MyFirstJuniorRobot",
        "jar": "mike.MyFirstJuniorRobot.jar",
        "src_file": "mike/MyFirstJuniorRobot.java",
        "warcry": "Pequeno no tamanho, gigante na arena!",
        "author_name": "Radia Perlman",
        "author_mail": "radia@mike.org",
        "author_login": "mike",
        "author_pw": "123456"
    },
    {
        "team": "november",
        "name": "SittingDuck",
        "jar": "november.SittingDuck.jar",
        "src_file": "november/SittingDuck.java",
        "warcry": "Pato parado também é pato perigoso!",
        "author_name": "Ken Thompson",
        "author_mail": "ken@november.org",
        "author_login": "november",
        "author_pw": "123456"
    }
]

def robots():
    """The demo robots, as they are in ~/robocode/robots: a fresh copy of DEMO_ROBOTS"""
    return [dict(robot) for robot in DEMO_ROBOTS]


def main():
    """Registers the demo robots in the championship database, skipping the ones already there"""
    repo_dir = os.path.dirname(os.path.abspath(__file__))
    db_path = os.path.join(repo_dir, 'database', 'robochamp.db')
    robocode_dir = os.path.expanduser('~/robocode')
    robots_dir = os.path.join(robocode_dir, 'robots')
    logo_path = os.path.join(repo_dir, 'src', 'assets', 'robocodeLogo.png')
    music_path = os.path.join(repo_dir, 'test', 'fixtures', 'tone.mp3')

    logo_data = read_file(logo_path)
    music_data = read_file(music_path)

    os.makedirs(os.path.dirname(db_path), exist_ok=True)
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    cur.execute("SELECT count(*) FROM Opponent")
    initial_count = cur.fetchone()[0]
    print(f"Current opponents in database: {initial_count}")

    for robot in robots():
        build_jar(robot, robocode_dir)
        register_robot(cur, robot, robots_dir, logo_data, music_data)

    conn.commit()
    conn.close()
    print("Database seeding completed.")

if __name__ == "__main__":
    main()

