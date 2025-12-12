---
title: ADR - File System for Work Logs
author: Antigravity
date: 2025-12-12T13:41:40.388000842
---

# Architecture Decision Record: File System Persistence for Work Logs

## Context
To enable future RAG (Retrieval-Augmented Generation) capabilities, we need to accumulate work logs and architectural decisions. We chose to store these as **Markdown files** on the local file system rather than using a Database.

## Deep Dive & Trade-off Analysis

### 1. Atomicity & Concurrency
- **Problem**: File systems lack the ACID properties of databases. Concurrent writes can lead to race conditions or file corruption.
- **Decision**: Accepted risk. 
- **Rationale**: 
  - The system is currently single-user (Personal Engineering Wiki).
  - The operational goal is "Fast Data Collection", not high-concurrency transaction processing.
  - **Mitigation**: If scaling is needed later, the `WorkLogRepository` interface allows swapping to a DB implementation without affecting business logic.

### 2. Performance (I/O & Scalability)
- **Problem**: `findAll` reads and parses all files in the directory o enable future RAG (Retrieval-Augmented Generation) capabilities, we need to accumulate work logsmization.
- **Rationale**: 
  - Current volume is low.
  - Adding caching or indexing now would over-engineer the solution and distract from the core value (accumulation of content).

### 3. Searchability
- **Problem**: Complex queries (e.g., "find logs by date range") are inefficient on raw files.
- **Decision**: Deferred to RAG.
- **Rationale**: The ultimate consumer of this data is an AI/LLM, which handles unstructured retrieval well. We do not need to build a complex search engine within the SpringBoot app.

## Conclusion
We prioritize **Simplicity** and **Data Portability** (Markdown files are universal) over ACID compliance and Query Performance at this stage. This aligns with the Agile principle of "YAGNI (You Arent Gonna Need It)" for the current single-user scope.