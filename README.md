# Distributed Real-Time Chat System
A multithreaded client-server chat application built in Java utilizing TCP sockets for real-time communication and bidirectional data exchange across isolated chat rooms.

## Tech Stack and Architecture
* Language: Java
* Network Layer: TCP Sockets (`ServerSocket`, `Socket`)
* Frontend Framework: JavaFX
* Concurrency Framework: Multithreading, ConcurrentHashMap, ReentrantLock, BlockingQueue

## Core Features and Technical Implementation

### 1. Concurrent Client Management
* Implemented a decoupled `ClientHandler` pipeline running on dedicated threads to manage multiple simultaneous socket connections without blocking the main server lifecycle.
* Utilized `ConcurrentHashMap` to guarantee thread-safe operations during client registration, room allocation, and active connection auditing.

### 2. Thread-Safe Message Routing
* Integrated a `LinkedBlockingQueue` inside individual chat rooms to enforce a reliable consumer-producer architecture for incoming message payloads.
* Applied explicit synchronization mechanisms using `ReentrantLock` to completely mitigate race conditions during concurrent user validation and session assignment.

### 3. Real-Time Graphical UI
* Developed a responsive JavaFX graphical user interface that allows users to register custom profiles, generate dynamic chat environments, join distinct messaging rooms, and receive incoming text broadcasts via asynchronous worker loops.
