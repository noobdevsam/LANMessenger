Project Title: Java LAN Messenger (Without Framework)


Project Goal:
Create a LAN-based messenger application where multiple users connected on the same WiFi/LAN network can communicate in real-time.

==================================================
STEP 1 — PROJECT STRUCTURE
==========================

Create a professional Java project structure like this:
```
src/
├── client/
├── server/
├── ui/
├── database/
├── model/
├── utils/
```

Explain the responsibility of each package.

==================================================
STEP 2 — DATABASE DESIGN
========================

Use MySQL database.

Create database:

* lan_messenger

Create tables:

1. users
2. messages

users table fields:

* id
* username
* password
* status
* created_at

messages table fields:

* id
* sender
* receiver
* message
* time

Generate:

* Full SQL queries
* Sample data
* JDBC connection class

==================================================
STEP 3 — SERVER APPLICATION
===========================

Build the server-side application.

Requirements:

* Server runs on a specific port
* Multiple clients can connect simultaneously
* Use ServerSocket
* Each client handled using separate Thread
* Broadcast online users list
* Handle client disconnect properly
* Show server logs in console

Generate:

* ServerMain.java
* ClientHandler.java
* Online user management logic

Explain every important part clearly.

==================================================
STEP 4 — CLIENT LOGIN & REGISTRATION GUI
========================================

Create Java Swing GUI:

1. Login Form
2. Registration Form

Features:

* Username
* Password
* Login button
* Register button

Validation:

* Empty field validation
* Duplicate username check
* Wrong password handling

Design:

* Modern dark/light UI
* Clean layout
* Professional button styling

Generate complete Swing code.

==================================================
STEP 5 — MAIN CHAT WINDOW GUI
=============================

Create a modern messenger UI.

Features:

* Left panel:

  * Online users list
* Right panel:

  * Chat area
  * Message input field
  * Send button

Additional features:

* Auto scroll chat
* Timestamp
* Different colors for sender/receiver messages

Use:

* JTextArea / JTextPane
* JList
* JScrollPane
* JPanel layouts

Generate complete GUI code.

==================================================
STEP 6 — SOCKET COMMUNICATION
=============================

Implement real-time messaging.

Requirements:

* Send messages instantly
* Receive messages instantly
* Use DataInputStream / DataOutputStream
* Background thread for receiving messages
* Handle connection errors properly

Generate:

* Message sending logic
* Message receiving logic
* Thread management

==================================================
STEP 7 — PRIVATE CHAT FEATURE
=============================

Add private messaging.

Requirements:

* Click user from online list
* Open private chat
* Store messages in database
* Load previous chat history

Generate:

* Private chat logic
* Database retrieval code
* UI update code

==================================================
STEP 8 — EXTRA FEATURES
=======================

Add these advanced features one by one:

1. Emoji support
2. File sharing
3. User online/offline status
4. Chat notifications
5. Sound notification
6. Profile picture support
7. Message search
8. Delete chat option

Explain implementation clearly.

==================================================
STEP 9 — SECURITY IMPROVEMENTS
==============================

Implement:

* Password hashing
* Input sanitization
* SQL injection prevention using PreparedStatement

Explain best practices.

==================================================
STEP 10 — FINAL POLISH
======================

Improve:

* UI design
* Exception handling
* Code optimization
* Comments/documentation

Generate:

* Final project cleanup checklist
* Complete folder structure
* How to run project
* Common bug fixes

