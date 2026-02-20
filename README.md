# Pro-Manager Basic

## 📖 Problem Statement

ProManage Solutions Pvt. Ltd. is a project management company that handles multiple client software projects such as UI design, development, testing, and deployment. Each completed project generates revenue for the company.

To manage workload efficiently, the company follows a fixed weekly schedule. All client projects are received by Saturday, planning is done over the weekend, and work starts from Monday. The company operates only five days a week (Monday to Friday) and can complete a maximum of five projects per week, with only one project completed per day.

Each project has:
- A Title
- A Deadline (in calendar days)
- An Expected Revenue

If a project is not completed before its deadline, the company loses the revenue from that project.

Therefore, selecting the right combination of projects and scheduling them properly is necessary to maximize total weekly profit.

The system must:
- Store project details in a PostgreSQL database
- Generate unique project IDs automatically
- Allow adding and viewing projects
- Generate an optimal weekly schedule
- Ensure only one project per day
- Maximize total revenue
- Respect deadline constraints

---

## 📌 Project Overview
Pro-Manager Basic is a Java-based Project Scheduling System designed to optimize weekly project allocation based on deadlines and revenue.

The system ensures:
- Only 1 project per day
- Maximum 5 projects per week (Monday–Friday)
- Maximum revenue generation
- Deadline constraints respected

The application uses **Dynamic Programming** to determine the optimal project combination.

---

## 🚀 Features

- Add new projects with:
    - Title
    - Deadline (calendar days)
    - Revenue
- Automatic Unique Project ID generation (Proj001, Proj002, ...)
- View all stored projects in table format
- Generate optimal weekly schedule
- Revenue maximization using Dynamic Programming
- PostgreSQL database integration
- Weekend days counted in deadline logic (no work assigned on Sat/Sun)

---

## 🛠 Technologies Used

- Java
- PostgreSQL
- JDBC
- IntelliJ IDEA
- Git & GitHub

---

## 🧠 Algorithm Used

Dynamic Programming is used to:
- Evaluate all valid project combinations
- Respect deadline constraints
- Maximize total weekly revenue
- Avoid greedy local-optimum mistakes

---

## 📅 Scheduling Rules

- Work days: Monday to Friday
- Maximum 1 project per day
- Maximum 5 projects per week
- Deadline counted in calendar days
- No work scheduled on weekends

---

## 📂 Project Structure

src/
├── Main.java
│    → Entry point of the application.
│    → Displays menu and handles user interaction.
│
├── Scheduler.java
│    → Contains Dynamic Programming logic.
│    → Generates optimal weekly schedule.
│
├── ProjectDAO.java
│    → Handles database operations.
│    → Add project, view projects, fetch data.
│
├── DBConnection.java
│    → Establishes connection with PostgreSQL database.
│
└── Project.java
→ Model class representing a project entity.
→ Stores title, deadline, revenue, project ID.