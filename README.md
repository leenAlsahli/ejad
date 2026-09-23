<p align="center">
  <img src="assets/logoo.png" alt="Ejad Logo" width="240">
</p>


A multi-threaded client-server desktop application built in Java, designed to solve linear equations over TCP with live network monitoring and round-trip-time (RTT) tracking.

## Overview
Rather than settling for a typical networking course project built around backend logic alone, Ejad extends the assignment into a fully interactive system. It pairs a multi-threaded TCP server with a graphical dashboard for live monitoring, alongside a client interface for submitting equations, viewing results, and tracking network performance in real time.

## Features
- **Server Dashboard:** Live logs displaying incoming connections, requests, and processing status in real time
- **Client Interface:** Simple GUI for entering linear equations and viewing computed results instantly
- **RTT Tracking:** Real-time round-trip-time measurement for every client-server exchange
- **Multi-threaded Backend:** Handles multiple client connections concurrently over TCP without blocking
- **Live Network Logging:** Server-side event logging for transparency into system behavior

## Tech Stack
- **Language:** Java
- **GUI Framework:** Java Swing
- **Networking:** Java Sockets (`java.net`), TCP
- **Concurrency:** Multi-threading (`AtomicInteger` for thread-safe counters)
- **Architecture:** Client-Server, request-response over TCP

## How It Works
1. `EjadServer` starts a multi-threaded TCP server, spawning a new thread for each incoming client connection so multiple clients can be served concurrently.
2. `EjadClient` connects to the server, sends a linear equation for processing, and displays the returned solution.
3. The server dashboard logs every connection and request live, while the client tracks and displays RTT for each exchange.

## Running Locally
1. Open the project folder in your preferred Java-supported IDE.
2. Run `EjadServer.java` first to start the server.
3. Then run `EjadClient.java` in a separate run configuration to connect as a client.

## What I Learned
Building Ejad went beyond implementing socket communication — it required designing a system that stays responsive under concurrent connections, while surfacing that concurrency visually through live logs and performance metrics like RTT, bridging backend network programming with practical, user-facing monitoring tools.

## Author
Leen Alsahli — [LinkedIn](https://linkedin.com/in/leen-alsahli-1064a6305) | [Portfolio](https://leen-portfolio-inky.vercel.app)
