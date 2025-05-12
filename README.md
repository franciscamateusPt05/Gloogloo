# Gloogloo – Distributed Search Engine

Gloogloo is a distributed search engine inspired by the architecture and functionalities of Google. Developed as a final project for a Distributed System course, it combines Java RMI for distributed communication and Spring Boot for the frontend. The system is modular, fault-tolerant, and supports dynamic updates, making it both scalable and resilient.

---

## 📑 Table of Contents

- [Introduction](#introduction)
- [System Architecture](#system-architecture)
  - [Gateway](#gateway)
  - [Barrels](#barrels)
  - [Downloaders](#downloaders)
  - [Queue](#queue)
- [Functionalities](#functionalities)
- [Requirements](#requirements)
- [Execution Instructions](#execution-instructions)
  - [1. Configure IPs and Ports](#1-configure-ips-and-ports)
  - [2. Build the Project](#2-build-the-project)
  - [3. Executing the Files](#3-executing-the-files)
  - [4. Running the Frontend](#4-running-the-frontend)

---

## Introduction

The goal of Gloogloo is to simulate the behavior of a distributed search engine by building a backend that handles indexing, search, and statistics and a frontend that serves as the user interface. Communication is achieved using Java RMI and REST APIs (HackerNews, OpenRoute).

---

## System Architecture

### Gateway

- Acts as the central controller and entry point.
- Manages Barrel registration, search routing, and queue operations.
- Maintains statistics (top searches, response times, barrel sizes).
- Offers administrative insights in real-time.

### Barrels

- Serve as independent SQLite-backed storage nodes.
- Store indexed content, title, snippet, and links.
- Handle search queries and word frequency statistics.
- Do not communicate directly — coordination via Gateway.

### Downloaders

- Retrieve URLs from Queue, download and parse web pages using **Jsoup**.
- Normalize and index content.
- Push indexed data to all Barrels via a reliable multicast approach.
- Sync stopwords dynamically and retry failed operations.

### Queue

- Manages a URL buffer and stopword list.
- Acts as the dispatch center for URLs.
- Ensures load balancing between Downloaders.
- Provides persistent in-memory TXT-based storage.

---

## Functionalities

- **Insert & Index URL**  
  Submit a URL via frontend → sent to Gateway → added to Queue → indexed by Downloaders → stored in all Barrels.

- **Keyword Search**  
  Search for one or more terms → routed to Gateway → queries a random Barrel → returns relevant URLs/snippets.

- **Related URL Search**  
  Submit a URL → system returns semantically related URLs using indexed link data.

- **Admin Interface**  
  Real-time stats including:
  - Top search terms
  - Average Barrel response times
  - Barrel index sizes

---

## Requirements

Ensure the following dependencies and tools are installed:

- Java JDK 22
- Maven
- SQLite
- Spring Boot
- Libraries:
  - Gson
  - Unirest
  - Jsoup
  - org.json

All `.properties` files must be correctly configured for host/IP and ports.

---

## Execution Instructions

### 1. Configure IPs and Ports

Set correct host and port values in:

- `gateway.properties`
- `queue.properties`
- `barrel.properties`

Ensure all components are referencing consistent IPs/ports to allow successful communication.

---

### 2. Build the Project

Open your terminal and run the following commands to build all components:

```bash
cd Gloogloo/common
mvn clean install -DskipTests

cd Gloogloo/backend
mvn clean install -DskipTests

cd Gloogloo/frontend
mvn clean install -DskipTests

cd Gloogloo
mvn clean install -DskipTests
```
### 3. Executing the files 
Now the user should proceed to run the following files in seperate terminals
 ```bash
 QueueServer.java 
 Gateway.java 
 BarrelServer.java 
 Barrel2Server.java 
 Downloader.java
```
### 4. Running the frontend
For the final step the user has to run the following command in the given directory of "cd Gloogloo/frontend"
```bash
mvn spring-boot:run -DskipTests
```
