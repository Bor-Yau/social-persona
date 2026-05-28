"""
SQLite CHECK 约束迁移脚本

问题: AIStatus 枚举值 (active/sleeping/archived/request_pending) 
      与 schema 的 CHECK (active/paused/archived) 不匹配，
      导致 AIStateMachine 设置 status='sleeping' 时报 SQLITE_CONSTRAINT_CHECK。

修复: 重建 personas 表使 CHECK 约束对齐 AIStatus 枚举。
"""

import sqlite3
import shutil
import os
import re

DB_PATH = os.path.abspath(os.path.join(
    os.path.dirname(__file__), "..", "..", "java-manager", "data", "social_persona.db"))
BACKUP_PATH = DB_PATH + ".bak_before_status_migration"


def main():
    if not os.path.exists(DB_PATH):
        print(f"[跳过] 数据库不存在: {DB_PATH}")
        return

    print(f"数据库路径: {DB_PATH}")
    print(f"备份到: {BACKUP_PATH}")
    shutil.copy2(DB_PATH, BACKUP_PATH)
    print("[备份完成]")

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # 先清理可能的残留
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='personas_old'")
    if cursor.fetchone():
        cursor.execute("DROP TABLE personas_old")
        print("[清理] 删除残留的 personas_old")

    cursor.execute("PRAGMA table_info(personas)")
    columns_info = cursor.fetchall()
    col_names = [c[1] for c in columns_info]
    col_defs = ", ".join([f'"{c[1]}"' for c in columns_info])
    print(f"\n[扫描] personas 表 {len(columns_info)} 列: {', '.join(col_names)}")

    cursor.execute("SELECT * FROM personas")
    rows = cursor.fetchall()
    print(f"[扫描] personas 共 {len(rows)} 行")

    cursor.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name='personas'")
    old_sql = cursor.fetchone()[0]
    print(f"[扫描] 当前 CHECK: {re.findall(r'CHECK\s*\(.*?\)', old_sql)[-1] if re.findall(r'CHECK\s*\(.*?\)', old_sql) else '(无)'}")

    cursor.execute("ALTER TABLE personas RENAME TO personas_old")
    print("[迁移] 重命名 personas → personas_old")

    new_check = "CHECK (status IN ('active', 'sleeping', 'archived', 'request_pending'))"
    new_sql = re.sub(
        r"CHECK\s*\(\s*status\s+IN\s*\([^)]+\)\s*\)",
        new_check,
        old_sql
    )
    cursor.execute(new_sql)
    print(f"[迁移] 创建新 CHECK 约束: {new_check}")

    cursor.execute(f"INSERT INTO personas ({col_defs}) SELECT {col_defs} FROM personas_old")
    print(f"[迁移] 迁移 {cursor.rowcount} 行数据")

    cursor.execute("DROP TABLE personas_old")
    print("[清理] 删除 personas_old")

    # 追加：确保 life_stage 等新列存在
    for col, col_type in [
        ("life_stage", "TEXT"),
        ("life_stage_detail", "TEXT"),
        ("current_location", "TEXT"),
    ]:
        try:
            cursor.execute(f"ALTER TABLE personas ADD COLUMN {col} {col_type}")
            print(f"[新增列] {col} ({col_type})")
        except sqlite3.OperationalError:
            print(f"[已存在] {col}")

    conn.commit()
    conn.close()
    print("\n[完成] 迁移成功！")


if __name__ == "__main__":
    main()