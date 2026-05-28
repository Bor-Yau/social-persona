# Redis（可选）

Redis 用于多 AI 并发时的缓存加速。未安装时 Java 自动降级为内存缓存，不影响核心功能。

## Windows 安装

从 https://github.com/tporadowski/redis/releases 下载 redis-x64-xxx.zip，
解压到此目录，确保 redis-server.exe 在本目录下。

## 验证

双击 redis-server.exe 或运行：
```
redis-server.exe
```