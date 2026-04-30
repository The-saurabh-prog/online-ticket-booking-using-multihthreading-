Overview

This project simulates a real-world bus ticket booking platform where multiple users can select, lock, and book seats concurrently.

It demonstrates how modern systems handle:

Multiple users booking simultaneously
Seat locking to prevent conflicts
Timeout-based release of seats
Thread-safe operations using Java
🚀 Key Features
🎯 Core Features
✅ Multithreaded booking system (multiple users)
✅ Thread-safe seat allocation
✅ Seat states: AVAILABLE, LOCKED, BOOKED
✅ Prevention of race conditions
🔴 RedBus-like Features
🪑 Seat selection & deselection
⏳ Seat locking with timeout (auto-release)
📅 Date-based booking
🚍 Multiple buses (e.g., Express-101, NightRider-202)
💳 Booking confirmation flow
🌐 Web UI (http://localhost:8080
)
⚡ Advanced Concurrency
🔹 ExecutorService for thread pool
🔹 ScheduledExecutorService for timeout handling
🔹 Synchronization using synchronized / locks
🔹 Concurrent user simulation (bots)
🧠 Concepts Covered
Concept	Usage
Multithreading	Simulating multiple users
Synchronization	Prevent race conditions
ExecutorService	Manage thread pool
ScheduledExecutorService	Handle seat timeout
Race Condition	Avoid double booking
Thread Safety	Ensure correct seat allocation
🏗️ Project Structure
📦 Ticket-Booking-System
 ┣ 📜 BookingSystem.java
 ┣ 📜 Seat.java
 ┣ 📜 Bus.java
 ┣ 📜 SeatManager.java
 ┣ 📜 UserTask.java
 ┣ 📜 Main.java
 ┣ 📜 index.html
 ┗ 📜 README.md
⚙️ How It Works
🔄 Booking Flow
User selects seat → Seat LOCKED (10 sec)
        ↓
User confirms → Seat BOOKED ✅
        ↓
Timeout → Seat RELEASED ❌
🧵 Multithreading Flow
Each user = separate thread
Threads try to access shared seat data
Synchronization ensures:
Only one thread modifies a seat at a time
▶️ Sample Output
[Time] Bot-1 selected Seat-14 on Express-101 (2026-04-28). Locked for 10 seconds
[Time] Bot-1 processing payment...
[Time] Bot-1 confirmed booking

[Time] Bot-2 selected Seat-8 on NightRider-202 (2026-04-28)
[Time] Seat-8 released due to timeout
🌐 Web Interface
Open in browser:
http://localhost:8080

Features:

Seat layout view
Select / deselect seats
Real-time updates
🛠️ Installation & Run
# Clone repository
git clone https://github.com/your-username/ticket-booking-system.git

# Navigate
cd ticket-booking-system

# Compile
javac *.java

# Run
java Main
⚠️ Problem Without Multithreading Control
Thread-1 → sees seat available
Thread-2 → sees seat available
Both book same seat ❌ (overbooking)
✅ Solution

Using synchronization + locking:

Thread-1 → locks → books
Thread-2 → waits → checks → books/fails
🔥 Advanced Behavior
Seat locking prevents conflicts
Timeout ensures fairness
Multiple thread pools handle:
Users
Background tasks (timeouts)
🚀 Future Enhancements
🎨 GUI (JavaFX / React frontend)
🗄️ Database integration (MySQL)
🌐 REST API (Spring Boot)
💳 Payment gateway simulation
📊 Analytics dashboard
🎯 Learning Outcomes
Real-world concurrency handling
Thread-safe programming
Designing scalable systems
Understanding race conditions deeply
💡 Interview Questions
What is a race condition?
How does synchronization work?
Why use ExecutorService instead of Thread?
How does seat locking prevent conflicts?
How is timeout implemented?
📊 Version
🚀 v1.0 – AdUI, seat locking, timeout, and advanced concurrency
👨‍💻 Author

Saurabh
