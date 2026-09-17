# Operating Systems — Processes and Threads

A process is an executing program with its own address space and operating-system resources.

A thread is an execution path within a process. Multiple threads in the same process share memory and resources.

## Context switching

The operating system scheduler switches between runnable tasks. A context switch saves the state of one task and restores another.

## Synchronization

Threads that share mutable data require synchronization to avoid race conditions.

Common synchronization mechanisms include:

- mutexes
- semaphores
- condition variables
- read-write locks

Concurrency can improve responsiveness and throughput but increases reasoning complexity.
