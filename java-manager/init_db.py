import sqlite3

DB = r'E:\Trae\Project\Netizen-Simulator-project\Netizen-Simulator\java-manager\data\social_persona.db'
SQL = r'E:\Trae\Project\Netizen-Simulator-project\Netizen-Simulator\java-manager\src\main\resources\schema.sql'

conn = sqlite3.connect(DB)
with open(SQL, encoding='utf-8') as f:
    script = f.read()

conn.executescript(script)

tables = conn.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()
print('Tables created:', [t[0] for t in tables])
conn.close()
