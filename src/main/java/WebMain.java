import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.mindrot.jbcrypt.BCrypt;
import spark.Request;
import spark.Response;

import static spark.Spark.*;

/**
 * DTOs moved to top-level package classes for better accessibility and serialization.
 */
class SignupRequest { public String name, email, password; }
class LoginRequest { public String email, password; }
class TaskPayload { public String title, description, status, priority, dueDate; public Integer revenue; }
record AuthUser(String name, String email) {}

public class WebMain {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, AuthUser> TOKEN_STORE = new ConcurrentHashMap<>();
    
    // In-memory fallbacks
    private static final Map<String, AuthUser> IN_MEMORY_USERS = new ConcurrentHashMap<>();
    private static final Map<String, String> IN_MEMORY_PASSWORDS = new ConcurrentHashMap<>();
    private static final List<Map<String, Object>> IN_MEMORY_TASKS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger TASK_ID_GEN = new AtomicInteger(1001);

    private static boolean DATABASE_READY = true;
    private static String DATABASE_ISSUE = null;

    public static void main(String[] args) {
        port(resolvePort());
        staticFiles.location("/public");

        // DB Bootstrap
        try {
            bootstrapSchema();
        } catch (Exception e) {
            DATABASE_READY = false;
            DATABASE_ISSUE = e.getMessage();
            System.err.println("WARN: Database inaccessible. Using in-memory fallback. Reason: " + e.getMessage());
        }

        // Global CORS & JSON
        before((req, res) -> {
            applyCorsHeaders(res);
            if (res.type() == null) res.type("application/json");
        });

        // Global Error Handling
        exception(Exception.class, (e, req, res) -> {
            e.printStackTrace();
            res.status(500);
            res.body(GSON.toJson(mapOf("success", false, "message", "Internal Server Error: " + e.getMessage())));
        });

        path("/api", () -> {
            
            // Public Routes
            path("/auth", () -> {
                post("/signup", WebMain::handleSignup);
                post("/register", WebMain::handleSignup);
                post("/login", WebMain::handleLogin);
            });

            // Protected Routes Filter
            before("/*", (req, res) -> {
                if ("OPTIONS".equalsIgnoreCase(req.requestMethod())) return;
                if (req.pathInfo().startsWith("/api/auth/")) return;

                AuthUser user = authenticate(req);
                if (user == null) {
                    halt(401, GSON.toJson(mapOf("success", false, "message", "Please log in.")));
                }
                req.attribute("user", user);
            });

            // Authenticated API
            get("/auth/me", (req, res) -> writeSuccess(res, req.attribute("user")));
            get("/dashboard", (req, res) -> handleDashboard(res));
            
            path("/tasks", () -> {
                get("", WebMain::handleListTasks);
                post("", WebMain::handleCreateTask);
                put("/update-status", WebMain::handleUpdateTaskStatus);
                put("/:id", WebMain::handleUpdateTask);
                delete("/:id", WebMain::handleDeleteTask);
            });
        });

        get("/health", (req, res) -> "OK");
        System.out.println("ProManage Web Server running on port " + port());
    }

    private static int resolvePort() {
        String p = System.getenv("PORT");
        return (p != null) ? Integer.parseInt(p.trim()) : 8080;
    }

    private static void bootstrapSchema() throws SQLException {
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, name VARCHAR(100), email VARCHAR(255) UNIQUE, password_hash TEXT, created_at TIMESTAMP DEFAULT NOW())");
            st.execute("CREATE TABLE IF NOT EXISTS projects (id SERIAL PRIMARY KEY, title TEXT, description TEXT, status TEXT, priority TEXT, revenue INTEGER, due_date DATE)");
        }
    }

    // --- HANDLERS ---

    private static Object handleSignup(Request request, Response response) {
        SignupRequest p = GSON.fromJson(request.body(), SignupRequest.class);
        if (p == null || p.email == null || p.password == null) return writeError(response, 400, "Invalid data.");

        String hash = BCrypt.hashpw(p.password, BCrypt.gensalt(12));

        if (!DATABASE_READY) {
            if (IN_MEMORY_USERS.containsKey(p.email)) return writeError(response, 409, "User exists.");
            IN_MEMORY_USERS.put(p.email, new AuthUser(p.name, p.email));
            IN_MEMORY_PASSWORDS.put(p.email, hash);
            return writeSuccess(response, mapOf("message", "User registered (in-memory)."));
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO users(name, email, password_hash) VALUES(?,?,?)")) {
            ps.setString(1, p.name);
            ps.setString(2, p.email.toLowerCase());
            ps.setString(3, hash);
            ps.executeUpdate();
            return writeSuccess(response, mapOf("message", "User created."));
        } catch (SQLException e) {
            if (e.getMessage().contains("unique")) return writeError(response, 409, "Email taken.");
            return writeError(response, 500, e.getMessage());
        }
    }

    private static Object handleLogin(Request request, Response response) {
        LoginRequest p = GSON.fromJson(request.body(), LoginRequest.class);
        AuthUser user = null;
        String hash = null;

        if (!DATABASE_READY) {
            user = IN_MEMORY_USERS.get(p.email);
            hash = IN_MEMORY_PASSWORDS.get(p.email);
        } else {
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT name, email, password_hash FROM users WHERE email=?")) {
                ps.setString(1, p.email.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        user = new AuthUser(rs.getString("name"), rs.getString("email"));
                        hash = rs.getString("password_hash");
                    }
                }
            } catch (SQLException e) { return writeError(response, 500, e.getMessage()); }
        }

        if (user == null || hash == null || !BCrypt.checkpw(p.password, hash)) {
            return writeError(response, 401, "Invalid credentials.");
        }

        String token = UUID.randomUUID().toString();
        TOKEN_STORE.put(token, user);
        return writeSuccess(response, mapOf("token", token, "user", user));
    }

    private static Object handleListTasks(Request req, Response res) {
        if (!DATABASE_READY) return writeSuccess(res, IN_MEMORY_TASKS);
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM projects ORDER BY id DESC")) {
            while (rs.next()) list.add(mapTask(rs));
            return writeSuccess(res, list);
        } catch (SQLException e) { return writeError(res, 500, e.getMessage()); }
    }

    private static Object handleCreateTask(Request req, Response res) {
        TaskPayload p = GSON.fromJson(req.body(), TaskPayload.class);
        if (!DATABASE_READY) {
            Map<String, Object> t = new HashMap<>();
            t.put("id", TASK_ID_GEN.incrementAndGet());
            t.put("title", p.title);
            t.put("description", p.description);
            t.put("status", p.status != null ? p.status : "todo");
            t.put("priority", p.priority);
            t.put("revenue", p.revenue);
            t.put("dueDate", p.dueDate);
            IN_MEMORY_TASKS.add(0, t);
            return writeSuccess(res, t);
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO projects(title, description, status, priority, revenue, due_date) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, p.title);
            ps.setString(2, p.description);
            ps.setString(3, p.status != null ? p.status : "todo");
            ps.setString(4, p.priority);
            ps.setObject(5, p.revenue);
            ps.setObject(6, (p.dueDate != null && !p.dueDate.isEmpty()) ? java.sql.Date.valueOf(p.dueDate) : null);
            ps.executeUpdate();
            return writeSuccess(res, mapOf("message", "Created."));
        } catch (SQLException e) { return writeError(res, 500, e.getMessage()); }
    }

    private static Object handleUpdateTaskStatus(Request req, Response res) {
        Map<String, Object> p = GSON.fromJson(req.body(), Map.class);
        int id = ((Double) p.get("id")).intValue();
        String st = (String) p.get("status");
        if (!DATABASE_READY) {
            for (Map<String, Object> t : IN_MEMORY_TASKS) if ((int) t.get("id") == id) { t.put("status", st); return writeSuccess(res, t); }
            return writeError(res, 404, "Not found.");
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE projects SET status=? WHERE id=?")) {
            ps.setString(1, st); ps.setInt(2, id); ps.executeUpdate();
            return writeSuccess(res, mapOf("status", "updated"));
        } catch (SQLException e) { return writeError(res, 500, e.getMessage()); }
    }

    private static Object handleUpdateTask(Request req, Response res) {
        int id = Integer.parseInt(req.params(":id"));
        TaskPayload p = GSON.fromJson(req.body(), TaskPayload.class);
        if (!DATABASE_READY) {
            for (Map<String, Object> t : IN_MEMORY_TASKS) if ((int) t.get("id") == id) {
                t.put("title", p.title); t.put("description", p.description); t.put("status", p.status);
                t.put("priority", p.priority); t.put("revenue", p.revenue); t.put("dueDate", p.dueDate);
                return writeSuccess(res, t);
            }
            return writeError(res, 404, "Not found.");
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE projects SET title=?, description=?, status=?, priority=?, revenue=?, due_date=? WHERE id=?")) {
            ps.setString(1, p.title); ps.setString(2, p.description); ps.setString(3, p.status);
            ps.setString(4, p.priority); ps.setObject(5, p.revenue);
            ps.setObject(6, (p.dueDate != null && !p.dueDate.isEmpty()) ? java.sql.Date.valueOf(p.dueDate) : null);
            ps.setInt(7, id); ps.executeUpdate();
            return writeSuccess(res, mapOf("message", "Updated."));
        } catch (SQLException e) { return writeError(res, 500, e.getMessage()); }
    }

    private static Object handleDeleteTask(Request req, Response res) {
        int id = Integer.parseInt(req.params(":id"));
        if (!DATABASE_READY) { IN_MEMORY_TASKS.removeIf(t -> (int) t.get("id") == id); return writeSuccess(res, "deleted"); }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM projects WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate(); return writeSuccess(res, "deleted");
        } catch (SQLException e) { return writeError(res, 500, e.getMessage()); }
    }

    private static Object handleDashboard(Response res) {
        Map<String, Object> stats = new HashMap<>();
        if (!DATABASE_READY) {
            stats.put("totalRevenue", IN_MEMORY_TASKS.stream().mapToInt(t -> (Integer) t.getOrDefault("revenue", 0)).sum());
            stats.put("todo", (int) IN_MEMORY_TASKS.stream().filter(t -> "todo".equals(t.get("status"))).count());
            stats.put("inProgress", (int) IN_MEMORY_TASKS.stream().filter(t -> "in_progress".equals(t.get("status"))).count());
            stats.put("completed", (int) IN_MEMORY_TASKS.stream().filter(t -> "completed".equals(t.get("status"))).count());
            return writeSuccess(res, stats);
        }
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT SUM(revenue) as total, COUNT(*) FILTER (WHERE status='todo') as td, COUNT(*) FILTER (WHERE status='in_progress') as ip, COUNT(*) FILTER (WHERE status='completed') as cp FROM projects")) {
            if (rs.next()) {
                stats.put("totalRevenue", rs.getInt("total"));
                stats.put("todo", rs.getInt("td"));
                stats.put("inProgress", rs.getInt("ip"));
                stats.put("completed", rs.getInt("cp"));
            }
            return writeSuccess(res, stats);
        } catch (SQLException e) { return writeError(res, 500, e.getMessage()); }
    }

    private static Map<String, Object> mapTask(ResultSet rs) throws SQLException {
        Map<String, Object> t = new HashMap<>();
        t.put("id", rs.getInt("id")); t.put("title", rs.getString("title")); t.put("description", rs.getString("description"));
        t.put("status", rs.getString("status")); t.put("priority", rs.getString("priority"));
        t.put("revenue", rs.getObject("revenue")); t.put("dueDate", rs.getString("due_date"));
        return t;
    }

    private static AuthUser authenticate(Request request) {
        String auth = request.headers("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return TOKEN_STORE.get(auth.substring(7));
    }

    private static void applyCorsHeaders(Response response) {
        response.header("Access-Control-Allow-Origin", "*");
        response.header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        response.header("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With");
    }

    private static String writeSuccess(Response res, Object data) { res.status(200); return GSON.toJson(mapOf("success", true, "data", data)); }
    private static String writeError(Response res, int s, String m) { res.status(s); return GSON.toJson(mapOf("success", false, "message", m)); }
    private static Map<String, Object> mapOf(Object... kvs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) m.put(kvs[i].toString(), kvs[i + 1]);
        return m;
    }
}