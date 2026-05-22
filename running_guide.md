I'll show concise steps to build the fat JAR, run server/client from the JAR, supply MySQL credentials, and how to point clients to a remote server (env or JVM props). Also include the minimal code snippet to let the client read host/port at runtime.

Build fat JAR
- From project root:
```bash
mvn clean package
```
- The shade plugin produces the runnable JAR in target (artifactId/version). Run with `java -jar target/lanmessenger-1.0.0-SNAPSHOT.jar`.

Run server vs client from the JAR
- Run the bundled app (starts server + UI on same machine):
```bash
java -jar target/lanmessenger-1.0.0-SNAPSHOT.jar
```
- Run *server only* (headless) using the server main class:
```bash
java -cp target/lanmessenger-1.0.0-SNAPSHOT.jar server.ServerMain
```
- Run *client only* (UI) on a client machine:
```bash
java -cp target/lanmessenger-1.0.0-SNAPSHOT.jar ui.LoginFrame
```

Use MySQL with the JAR
- Export DB connection environment variables before running any JAR process:
```bash
export LANM_DB_URL='jdbc:mysql://dbhost:3306/lan_messenger?useSSL=false&serverTimezone=UTC'
export LANM_DB_USER='lanuser'
export LANM_DB_PASS='secret'
```
- Then run server or client JAR as above; `src/database/DBConnection` will detect `LANM_DB_URL` and use MySQL (and create schema from schema.sql on first connect).

Point clients to a remote server (two options)
- Option 1 — Environment variables (quick):
```bash
export LANM_SERVER_HOST='your.server.ip'
export LANM_SERVER_PORT='8085'
java -cp target/lanmessenger-1.0.0-SNAPSHOT.jar ui.LoginFrame
```
- Option 2 — JVM system properties (per-run):
```bash
java -Dlanm.server.host=your.server.ip -Dlanm.server.port=8085 -cp target/lanmessenger-1.0.0-SNAPSHOT.jar ui.LoginFrame
```

Minimal code to read host/port at runtime (add to `ChatFrame.establishNetworkPipe()`; I can apply this change if you want)
- Replace the hardcoded host/port with:
```java
String host = System.getProperty("lanm.server.host",
               System.getenv().getOrDefault("LANM_SERVER_HOST", "127.0.0.1"));
int port = Integer.parseInt(System.getProperty("lanm.server.port",
               System.getenv().getOrDefault("LANM_SERVER_PORT", "8085")));
this.networkManager = new NetworkManager(host, port, this);
```
- This lets you control the client server address without editing code (via env or `-D`).

Migrating existing SQLite data to MySQL (summary)
- Dump SQLite:
```bash
sqlite3 lan_messenger.db .dump > dump.sql
```
- Convert types / AUTOINCREMENT differences or use a migration tool; if you want I can generate a migration script that maps your SQLite `INSERT` statements to the MySQL schema.

Firewall / network notes
- Ensure the server machine allows inbound TCP on the chosen port (default `8085`) and clients can reach it across the LAN.
- If you run server on a cloud host, use secure networking (VPN/SSH tunnel) or add authentication/encryption layers.
