# ProManage Pro 🚀

ProManage Pro is a high-performance, full-stack task and project management ecosystem. It blends a robust **Java 21 / SparkJava** backend with a stunning, modern **Glassmorphism** web dashboard.

## 🌟 Key Features

- **🔐 Secure Authentication**: Full signup and login flow with salted `jBCrypt` password hashing.
- **📊 Real-time Dashboard**: Interactive statistics showing Total Revenue, Task States, and priority distribution.
- **📋 Kanban Workflow**: Dynamic drag-and-drop task management across 'Todo', 'In Progress', and 'Completed' states.
- **💎 Premium UI**: Modern aesthetics featuring Glassmorphism, smooth animations, and a responsive Tailwind CSS design.
- **🛡️ Resilience Engine**: Intelligent dual-mode operation—automatically falls back to **In-Memory Storage** if the PostgreSQL database is unreachable, ensuring zero downtime for demos.
- **📦 RESTful API**: Clean API architecture for tasks, authentication, and dashboard analytics.

## 🛠️ Technology Stack

- **Backend**: Java 21, SparkJava, Gson, jBCrypt.
- **Frontend**: HTML5, Vanilla JavaScript, Tailwind CSS.
- **Database**: PostgreSQL (with automatic schema bootstrapping and in-memory fallback).
- **Build System**: Maven.

## 🚀 Quick Start

### 1. Prerequisites
- **Java 21+** installed.
- **Maven** installed.
- (Optional) **PostgreSQL** running on localhost:5432.

### 2. Run the Application
The project includes a convenient bootstrap script:

```powershell
.\start_backend.bat
```

The application will be available at:
👉 **[http://localhost:8080](http://localhost:8080)**

### 3. Default Account (Seeded)
If the server is running, you can log in with:
- **Email**: `abhishekk19445@gmail.com`
- **Password**: `123456`

## 📡 API Reference

| Endpoint | Method | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `/api/auth/signup` | POST | Register a new user | No |
| `/api/auth/login` | POST | Log in and receive a Bearer token | No |
| `/api/auth/me` | GET | Get current user profile | Yes |
| `/api/tasks` | GET | List all tasks | Yes |
| `/api/tasks` | POST | Create a new task | Yes |
| `/api/tasks/:id` | PUT | Update task details | Yes |
| `/api/tasks/update-status` | PUT | Quick update for Kanban drag-and-drop | Yes |
| `/api/dashboard` | GET | Get aggregate stats for charts | Yes |

## 🏗️ Project Architecture

```text
src/main/java/
├── WebMain.java        # Main server entry & API Route definitions
├── DBConnection.java   # JDBC Connection management
├── DBConfig.java       # Environment-aware configuration
└── ...                 # Business logic and DTOs

src/main/resources/public/
├── index.html          # Smart redirector
├── login.html          # Premium Auth interface
└── dashboard.html      # Main Kanban & Analytics dashboard
```

## 📝 License
Proprietary / Development Demo

---
**Author**: Abhishek Kumar
**Last Updated**: April 2026