Here’s how to run your SQLite-based app as a single server and multiple clients from the same JAR package:

### 1. Build the JAR
From your project root:
```bash
mvn clean package
```
This creates lanmessenger-1.0.0-SNAPSHOT.jar.

---

### 2. Start the Server (on one machine)
On the server machine, run:
```bash
java -cp target/lanmessenger-1.0.0-SNAPSHOT.jar server.ServerMain
```
- This starts the server only (no UI).
- The SQLite database file (lan_messenger.db) will be created/used in the server’s working directory.

---

### 3. Start Clients (on any machine in the LAN)
On each client machine, run:
```bash
java -cp target/lanmessenger-1.0.0-SNAPSHOT.jar ui.LoginFrame
```
- This starts the login/registration UI only.
- By default, clients try to connect to `127.0.0.1`.  
  To connect to the server over the network, set the server’s IP address and port using environment variables or JVM properties:

#### Example (Linux/macOS/Windows PowerShell):
```bash
export LANM_SERVER_HOST=192.168.1.100   # Replace with your server's LAN IP
export LANM_SERVER_PORT=8085
java -cp target/lanmessenger-1.0.0-SNAPSHOT.jar ui.LoginFrame
```
Or with JVM properties:
```bash
java -Dlanm.server.host=192.168.1.100 -Dlanm.server.port=8085 -cp target/lanmessenger-1.0.0-SNAPSHOT.jar ui.LoginFrame
```

---

### 4. How it works
- The server manages all messaging and the SQLite DB.
- All clients connect to the server’s IP/port and share the same chat environment.
- Only the server machine needs to access the SQLite file.

---

