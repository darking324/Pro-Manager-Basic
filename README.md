# ProManage – Predictive Project Scheduling System

---

## 📖 Problem Statement

ProManage Solutions Pvt. Ltd. is a project management company that handles multiple client software projects such as UI design, development, testing, and deployment. Each completed project generates revenue for the company.

The company follows a fixed weekly schedule:
- Projects are received by Saturday
- Planning is done over the weekend
- Work starts from Monday
- Only one project can be completed per day
- Maximum five projects per week (Monday–Friday)

Each project has:
- Title
- Deadline (in calendar days)
- Expected Revenue

If a project is not completed before its deadline, the revenue is lost.

Therefore, selecting the right projects each week is essential to maximize total profit while respecting deadline constraints.

---

## 📌 Project Overview

**ProManage** is a Java-based Predictive Project Scheduling System built using Java and PostgreSQL.

Unlike traditional scheduling systems that rely only on greedy or dynamic programming approaches, this system integrates:

- Revenue Forecasting (Weighted Moving Average)
- Deadline-based urgency scoring
- Predictive penalty logic
- Greedy slot allocation

The system simulates real-world weekly business planning.

---

## 🚀 Key Features

- Add new projects:
  - Title
  - Deadline (calendar days)
  - Revenue
- Automatic unique Project Code generation (Proj001, Proj002, ...)
- Store and manage projects in PostgreSQL
- Track project status (PENDING / SCHEDULED)
- Predict expected revenue for next week
- Generate optimized weekly schedule
- Store weekly revenue history for forecasting
- Enforce:
  - Only 1 project per day
  - Maximum 5 projects per week
  - Deadline constraints

---

## 🛠 Technologies Used

- Java
- PostgreSQL
- JDBC
- IntelliJ IDEA
- Git & GitHub

---

## 🧠 Algorithm & Model Explanation

### 1️⃣ Revenue Prediction (Weighted Moving Average)

To predict next week's expected revenue, the system uses the last 3 weeks of revenue:
Expected Revenue =
0.5 × Most Recent Week
0.3 × Second Recent Week
0.2 × Third Recent Week

This gives higher importance to recent performance trends.

---

### 2️⃣ Project Scoring Model

Each pending project is assigned a dynamic score based on:

- Revenue potential
- Deadline urgency
- Future penalty (if revenue is lower than expected revenue and deadline is flexible)

Scoring Formula:
Score =
(Revenue Weight)
(Urgency Weight)
(Future Penalty)

Where:

- Urgency = 1 / Deadline
- Projects with smaller deadlines get higher urgency
- Projects with long deadlines and low revenue may receive penalty

This ensures a balance between profitability and urgency.

---

### 3️⃣ Greedy Deadline-Based Allocation

After scoring:

1. Projects are sorted in descending order of score.
2. Each project is assigned to the latest available slot before its deadline.
3. Maximum 5 projects are selected (Monday–Friday).
4. Selected projects are marked as SCHEDULED.
5. Total weekly revenue is stored in revenue history.

Each execution simulates one business week.

---

## 📅 Scheduling Rules

- Work Days: Monday to Friday
- Maximum 1 project per day
- Maximum 5 projects per week
- Deadline respected strictly
- No work assigned on weekends
- Only PENDING projects are considered

---

## 🗂 Database Structure

### projects Table

| Column        | Description |
|---------------|------------|
| id            | Auto-generated internal ID |
| project_code  | Unique business ID |
| title         | Project title |
| deadline      | Deadline (calendar days) |
| revenue       | Expected revenue |
| status        | PENDING / SCHEDULED |
| created_week  | Week of creation |

---

### weekly_revenue_history Table

| Column        | Description |
|---------------|------------|
| week_no       | Auto-generated week number |
| total_revenue | Revenue earned in that week |
| created_at    | Timestamp |

---

## 📂 Project Structure
src/
├── Main.java
│ → Entry point
│ → Menu-based console UI
│
├── Scheduler.java
│ → Revenue prediction
│ → Scoring model
│ → Greedy deadline allocation
│
├── ProjectDAO.java
│ → Database operations
│ → Add / View projects
│ → View revenue history
│
├── DBConnection.java
│ → Database connectivity
│
└── Project.java
→ Model class
→ Encapsulates project attributes

---

## 🔄 System Behavior

Each time "Generate Weekly Schedule" is executed:

1. Predict expected revenue.
2. Score all PENDING projects.
3. Allocate top projects within constraints.
4. Update project statuses.
5. Store weekly revenue.
6. Prepare for next simulated week.

---

## 🎯 Why This Approach?

Instead of using Dynamic Programming, this system uses:

- Predictive revenue analysis
- Business-driven scoring
- Greedy deadline allocation

This reflects real-world decision-making more accurately.

---

## 📈 Advantages

- Adaptive to revenue trends
- Deadline aware
- Business realistic
- Scalable
- Modular architecture
- Database-backed persistence

---

## 📌 Future Improvements

- Web-based UI
- Machine learning-based forecasting
- Multi-team scheduling
- Resource capacity modeling

---

## 👨‍💻 Author

Abhishek Kumar  
B.Tech – CSE (AI & ML)  
Sharda University