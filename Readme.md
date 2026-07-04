## Repository Structure

helixrail_project/
│
├── RailControlSystem.java # Core Java Backend (Synchronized Monitors, Embedded Server & JDBC)
├── ConcurrentSimulation.py# Simulation Pipeline (Volumetric Load Tester)
├── App.jsx                # Frontend Module (React, Tailwind CSS, Train Grid Dashboard)
├── ConcurrencyTest.java   # Verification Testing Suite (Thread-Safety Assertions)
└── README.md              # Integration & System Operations Manual

# HelixRail: Synchronized Resource Access Architecture Protocol

HelixRail is a standalone concurrency exploration sandbox designed to demonstrate monitor patterns, guarded execution loops, wait/notify routines, and database thread safety.

---

## 🚀 Execution Instructions

### 1. Link Database Dependencies
Download the standard SQLite JDBC file driver `.jar` asset collection into your working directory path.

### 2. Compile Application Engine


javac RailControlSystem.java