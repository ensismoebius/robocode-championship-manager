#!/usr/bin/env python3
"""
Seed script to register demo/test robots into the championship SQLite database.
Reads real packaged robots from ~/robocode/robots/*.jar and registers them
with their compiled JAR binaries, Java source code, logo, sound, warcry, and author info.
"""

import sqlite3
import zipfile
import os
import hashlib
import base64

def hash_pw(pw: str) -> str:
    salt = os.urandom(16)
    h = hashlib.pbkdf2_hmac("sha256", pw.encode("utf-8"), salt, 210000, 32)
    return "pbkdf2$210000$" + base64.b64encode(salt).decode() + "$" + base64.b64encode(h).decode()

def read_file(path):
    """The bytes of the file, or None if it does not exist"""
    if not os.path.exists(path):
        return None
    with open(path, 'rb') as f:
        return f.read()

def read_source(jar_file_path, src_file):
    """The Java source inside the jar, or None if it can not be read"""
    try:
        with zipfile.ZipFile(jar_file_path) as z:
            return z.read(src_file).decode('utf-8', errors='replace')
    except Exception as e:
        print(f"Could not extract source code from {jar_file_path}: {e}")
        return None

def register_robot(cur, r, robots_dir, logo_data, music_data):
    """Inserts a robot and its author, unless a robot with the same team and name exists"""
    cur.execute("SELECT id FROM Opponent WHERE team = ? AND name = ?", (r["team"], r["name"]))
    existing = cur.fetchone()
    if existing:
        print(f"Robot {r['team']}.{r['name']} already registered with ID {existing[0]}. Skipping.")
        return

    jar_file_path = os.path.join(robots_dir, r["jar"])
    jar_data = read_file(jar_file_path)
    source_code = read_source(jar_file_path, r["src_file"]) if jar_data is not None else None

    cur.execute("""
        INSERT INTO Opponent (name, team, battleCry, compiledRobot, sourceCode, image, music, musicStart)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, (r["name"], r["team"], r["warcry"], jar_data, source_code, logo_data, music_data, 0.0))

    opp_id = cur.lastrowid

    cur.execute("""
        INSERT INTO Author (login, mail, name, observations, password, opponentId)
        VALUES (?, ?, ?, ?, ?, ?)
    """, (r["author_login"], r["author_mail"], r["author_name"], "Developer", hash_pw(r["author_pw"]), opp_id))

    print(f"Registered robot {r['team']}.{r['name']} with ID {opp_id}")

def robots():
    """The demo robots, as they are in ~/robocode/robots"""
    return [
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
        }
    ]


def main():
    repo_dir = os.path.dirname(os.path.abspath(__file__))
    db_path = os.path.join(repo_dir, 'database', 'robochamp.db')
    robots_dir = os.path.expanduser('~/robocode/robots')
    logo_path = os.path.join(repo_dir, 'src', 'widgets', 'scenes', 'robocodeLogo.png')
    music_path = os.path.join(repo_dir, 'test', 'fixtures', 'tone.mp3')

    logo_data = read_file(logo_path)
    music_data = read_file(music_path)

    os.makedirs(os.path.dirname(db_path), exist_ok=True)
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    cur.execute("SELECT count(*) FROM Opponent")
    initial_count = cur.fetchone()[0]
    print(f"Current opponents in database: {initial_count}")

    for r in robots():
        register_robot(cur, r, robots_dir, logo_data, music_data)

    conn.commit()
    conn.close()
    print("Database seeding completed.")

if __name__ == "__main__":
    main()

