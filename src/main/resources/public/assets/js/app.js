const state = {
    token: localStorage.getItem("pm_token") || "",
    user: JSON.parse(localStorage.getItem("pm_user") || "null"),
    tasks: [],
    dashboard: null,
    filters: {
        status: "",
        priority: "",
        fromDate: "",
        toDate: "",
        q: ""
    },
    deletingId: null,
    section: "dashboard"
};

let API_BASE = resolveApiBase();

const el = {
    authView: document.getElementById("auth-view"),
    workspaceView: document.getElementById("workspace-view"),
    loginForm: document.getElementById("login-form"),
    signupForm: document.getElementById("signup-form"),
    showSignup: document.getElementById("show-signup"),
    showLogin: document.getElementById("show-login"),
    authError: document.getElementById("auth-error"),

    sidebarMenu: document.getElementById("sidebar-menu"),
    viewTitle: document.getElementById("view-title"),
    viewSubtitle: document.getElementById("view-subtitle"),
    userChip: document.getElementById("user-chip"),
    profileName: document.getElementById("profile-name"),
    profileEmail: document.getElementById("profile-email"),
    notifCount: document.getElementById("notif-count"),
    search: document.getElementById("global-search"),

    dashboardSection: document.getElementById("dashboard-section"),
    tasksSection: document.getElementById("tasks-section"),
    profileSection: document.getElementById("profile-section"),
    statsGrid: document.getElementById("stats-grid"),
    trend: document.getElementById("revenue-trend"),
    completionRate: document.getElementById("completion-rate"),
    statusBars: document.getElementById("status-bars"),
    dueSoonMetric: document.getElementById("due-soon-metric"),
    revenueMetric: document.getElementById("revenue-metric"),

    filterStatus: document.getElementById("filter-status"),
    filterPriority: document.getElementById("filter-priority"),
    filterFrom: document.getElementById("filter-from"),
    filterTo: document.getElementById("filter-to"),
    clearFilters: document.getElementById("clear-filters"),
    newTaskBtn: document.getElementById("new-task-btn"),
    taskInsights: document.getElementById("task-insights"),
    tasksEmpty: document.getElementById("tasks-empty"),
    tasksError: document.getElementById("tasks-error"),

    colTodo: document.getElementById("col-todo"),
    colProgress: document.getElementById("col-in_progress"),
    colCompleted: document.getElementById("col-completed"),

    taskModal: document.getElementById("task-modal"),
    taskModalTitle: document.getElementById("task-modal-title"),
    taskForm: document.getElementById("task-form"),
    closeTaskModal: document.getElementById("close-task-modal"),

    deleteModal: document.getElementById("delete-modal"),
    cancelDelete: document.getElementById("cancel-delete"),
    confirmDelete: document.getElementById("confirm-delete"),

    loading: document.getElementById("loading-overlay"),
    toastHost: document.getElementById("toast-host")
};

boot();

function boot() {
    bindEvents();
    if (state.token) {
        initializeWorkspace().catch(() => showAuth());
    } else {
        showAuth();
    }
}

function bindEvents() {
    el.showSignup.addEventListener("click", () => toggleAuthForm("signup"));
    el.showLogin.addEventListener("click", () => toggleAuthForm("login"));

    [
        ...el.loginForm.querySelectorAll("input"),
        ...el.signupForm.querySelectorAll("input")
    ].forEach((input) => {
        input.addEventListener("input", () => {
            el.authError.classList.add("hidden");
        });
    });

    el.loginForm.addEventListener("submit", (event) => {
        event.preventDefault();
        const formData = new FormData(el.loginForm);
        login(Object.fromEntries(formData.entries()));
    });

    el.signupForm.addEventListener("submit", (event) => {
        event.preventDefault();
        const formData = new FormData(el.signupForm);
        signup(Object.fromEntries(formData.entries()));
    });

    el.sidebarMenu.addEventListener("click", (event) => {
        const target = event.target;
        if (!(target instanceof HTMLButtonElement)) {
            return;
        }

        const action = target.dataset.action;
        if (action === "logout") {
            logout();
            return;
        }

        const section = target.dataset.section;
        if (section) {
            switchSection(section);
        }
    });

    el.search.addEventListener("input", debounce((event) => {
        state.filters.q = event.target.value.trim();
        loadTasks();
    }, 280));

    [
        [el.filterStatus, "status"],
        [el.filterPriority, "priority"],
        [el.filterFrom, "fromDate"],
        [el.filterTo, "toDate"]
    ].forEach(([node, key]) => {
        node.addEventListener("change", () => {
            state.filters[key] = node.value;
            loadTasks();
        });
    });

    el.clearFilters.addEventListener("click", () => {
        state.filters = {status: "", priority: "", fromDate: "", toDate: "", q: ""};
        el.filterStatus.value = "";
        el.filterPriority.value = "";
        el.filterFrom.value = "";
        el.filterTo.value = "";
        el.search.value = "";
        loadTasks();
    });

    el.newTaskBtn.addEventListener("click", () => openTaskModal());
    el.closeTaskModal.addEventListener("click", closeTaskModal);

    el.taskForm.addEventListener("submit", (event) => {
        event.preventDefault();
        saveTask();
    });

    el.cancelDelete.addEventListener("click", closeDeleteModal);
    el.confirmDelete.addEventListener("click", confirmDeleteTask);

    [el.colTodo, el.colProgress, el.colCompleted].forEach((zone) => {
        zone.addEventListener("dragover", (event) => {
            event.preventDefault();
            zone.classList.add("drag-over");
        });

        zone.addEventListener("dragleave", () => zone.classList.remove("drag-over"));

        zone.addEventListener("drop", async (event) => {
            event.preventDefault();
            zone.classList.remove("drag-over");

            const taskId = Number(event.dataTransfer.getData("taskId"));
            if (!taskId) {
                return;
            }

            const targetStatus = zone.id.replace("col-", "");
            await moveTask(taskId, targetStatus);
        });
    });

    document.addEventListener("click", (event) => {
        const target = event.target;
        if (!(target instanceof HTMLElement)) {
            return;
        }

        const editBtn = target.closest("[data-edit-id]");
        if (editBtn) {
            const taskId = Number(editBtn.getAttribute("data-edit-id"));
            const task = state.tasks.find((item) => item.id === taskId);
            if (task) {
                openTaskModal(task);
            }
            return;
        }

        const deleteBtn = target.closest("[data-delete-id]");
        if (deleteBtn) {
            const taskId = Number(deleteBtn.getAttribute("data-delete-id"));
            promptDeleteTask(taskId);
        }
    });
}

async function initializeWorkspace() {
    showWorkspace();
    await Promise.all([loadProfile(), loadDashboard(), loadTasks()]);
}

function showAuth() {
    el.authView.classList.remove("hidden");
    el.workspaceView.classList.add("hidden");
    toggleAuthForm("login");
}

function showWorkspace() {
    el.authView.classList.add("hidden");
    el.workspaceView.classList.remove("hidden");
    switchSection(state.section);
}

function toggleAuthForm(mode) {
    el.authError.classList.add("hidden");
    if (mode === "signup") {
        el.signupForm.classList.remove("hidden");
        el.loginForm.classList.add("hidden");
    } else {
        el.signupForm.classList.add("hidden");
        el.loginForm.classList.remove("hidden");
    }
}

async function signup(payload) {
    const normalized = normalizeSignupPayload(payload);
    if (!normalized.ok) {
        showAuthError(normalized.message);
        return;
    }

    setAuthPending("signup", true);
    withLoading(true);
    try {
        const result = await apiRequest("/api/auth/signup", {
            method: "POST",
            body: normalized.data,
            auth: false
        });

        if (!result.success) {
            throw new Error(result.message || "Signup failed.");
        }

        if (result.data?.mode === "in-memory") {
            pushToast("Account created in temporary local mode. Please login.", "success");
        } else {
            pushToast("Account created. Please login.", "success");
        }

        toggleAuthForm("login");
        el.loginForm.reset();
        el.loginForm.elements.email.value = normalized.data.email;
        el.loginForm.elements.password.value = "";
        el.signupForm.reset();
    } catch (error) {
        showAuthError(error.message);
    } finally {
        setAuthPending("signup", false);
        withLoading(false);
    }
}

async function login(payload) {
    const normalized = normalizeLoginPayload(payload);
    if (!normalized.ok) {
        showAuthError(normalized.message);
        return;
    }

    setAuthPending("login", true);
    withLoading(true);
    try {
        const result = await apiRequest("/api/auth/login", {
            method: "POST",
            body: normalized.data,
            auth: false
        });

        if (!result.success) {
            throw new Error(result.message || "Login failed.");
        }

        state.token = result.data.token;
        state.user = result.data.user;
        localStorage.setItem("pm_token", state.token);
        localStorage.setItem("pm_user", JSON.stringify(state.user));

        await initializeWorkspace();

        if (result.data?.mode === "in-memory") {
            pushToast("Welcome back. Running in temporary local mode.", "success");
        } else {
            pushToast("Welcome back.", "success");
        }
    } catch (error) {
        clearSession();
        showAuth();
        showAuthError(error.message);
    } finally {
        setAuthPending("login", false);
        withLoading(false);
    }
}

function clearSession() {
    state.token = "";
    state.user = null;
    state.tasks = [];
    localStorage.removeItem("pm_token");
    localStorage.removeItem("pm_user");
}

function logout() {
    clearSession();
    showAuth();
    pushToast("Session ended.", "success");
}

function showAuthError(message) {
    el.authError.textContent = message;
    el.authError.classList.remove("hidden");
}

function switchSection(section) {
    state.section = section;

    const sections = {
        dashboard: el.dashboardSection,
        tasks: el.tasksSection,
        profile: el.profileSection
    };

    Object.entries(sections).forEach(([name, node]) => {
        node.classList.toggle("hidden", name !== section);
    });

    const labelMap = {
        dashboard: ["Dashboard", "Realtime project pulse and priorities"],
        tasks: ["Tasks", "Create, update, filter, and organize execution"],
        profile: ["Profile", "Workspace identity and account details"]
    };

    const active = labelMap[section] || labelMap.dashboard;
    el.viewTitle.textContent = active[0];
    el.viewSubtitle.textContent = active[1];

    [...el.sidebarMenu.querySelectorAll(".menu-item[data-section]")].forEach((button) => {
        button.classList.toggle("active", button.dataset.section === section);
    });
}

async function loadProfile() {
    if (!state.token) {
        return;
    }

    const result = await apiRequest("/api/auth/me", {method: "GET"});
    if (!result.success) {
        throw new Error(result.message || "Unable to load profile.");
    }

    state.user = result.data;
    localStorage.setItem("pm_user", JSON.stringify(state.user));
    renderUser();
}

function renderUser() {
    const name = state.user?.name || "Guest";
    const email = state.user?.email || "-";
    el.userChip.textContent = name;
    el.profileName.textContent = `Name: ${name}`;
    el.profileEmail.textContent = `Email: ${email}`;
}

async function loadDashboard() {
    try {
        const result = await apiRequest("/api/dashboard", {method: "GET"});
        if (!result.success) {
            throw new Error(result.message || "Dashboard fetch failed.");
        }

        state.dashboard = result.data;
        renderDashboard();
    } catch (error) {
        pushToast(error.message, "error");
    }
}

function renderDashboard() {
    const data = state.dashboard || {
        total: 0,
        todo: 0,
        inProgress: 0,
        completed: 0,
        dueSoon: 0,
        totalRevenue: 0,
        revenueTrend: []
    };

    const cards = [
        ["Total Tasks", data.total],
        ["To-Do", data.todo],
        ["In Progress", data.inProgress],
        ["Completed", data.completed],
        ["Due Soon", data.dueSoon],
        ["Revenue", data.totalRevenue]
    ];

    el.statsGrid.innerHTML = cards
        .map(([label, value]) => `<article class="stat-card"><p>${label}</p><h3>${escapeHtml(formatMetricValue(label, value))}</h3></article>`)
        .join("");

    const total = Number(data.total || 0);
    const completed = Number(data.completed || 0);
    const inProgress = Number(data.inProgress || 0);
    const todo = Number(data.todo || 0);
    const completion = total > 0 ? Math.round((completed / total) * 100) : 0;
    const statusItems = [
        ["To-Do", todo],
        ["Active", inProgress],
        ["Done", completed]
    ];

    el.completionRate.textContent = `${completion}%`;
    el.statusBars.innerHTML = statusItems
        .map(([label, count]) => {
            const width = total > 0 ? Math.max(6, Math.round((count / total) * 100)) : 0;
            return `
                <div class="status-row">
                    <span class="status-label">${escapeHtml(label)}</span>
                    <div class="status-track"><div class="status-fill" style="width:${width}%"></div></div>
                    <span class="status-value">${escapeHtml(String(count))}</span>
                </div>
            `;
        })
        .join("");

    el.dueSoonMetric.textContent = escapeHtml(String(data.dueSoon || 0));
    el.revenueMetric.textContent = escapeHtml(formatNumber(data.totalRevenue || 0));

    const trend = Array.isArray(data.revenueTrend) ? data.revenueTrend : [];
    if (trend.length === 0) {
        el.trend.innerHTML = "<p class=\"muted\">No historical revenue data yet.</p>";
    } else {
        el.trend.innerHTML = trend
            .map((item) => `
                <article class="trend-item">
                    <p>Week ${escapeHtml(String(item.weekNo))}</p>
                    <h4>${escapeHtml(String(item.totalRevenue))}</h4>
                    <small>${escapeHtml(new Date(item.createdAt).toLocaleDateString())}</small>
                </article>
            `)
            .join("");
    }

    el.notifCount.textContent = String(data.dueSoon || 0);
}

async function loadTasks() {
    el.tasksError.classList.add("hidden");
    try {
        const query = new URLSearchParams();
        Object.entries(state.filters).forEach(([key, value]) => {
            if (value) {
                query.set(key, value);
            }
        });

        const path = query.toString() ? `/api/tasks?${query.toString()}` : "/api/tasks";
        const result = await apiRequest(path, {method: "GET"});

        if (!result.success) {
            throw new Error(result.message || "Unable to load tasks.");
        }

        state.tasks = result.data || [];
        renderTaskBoard();
        await loadDashboard();
    } catch (error) {
        el.tasksError.textContent = error.message;
        el.tasksError.classList.remove("hidden");
    }
}

function renderTaskBoard() {
    const byStatus = {
        todo: [],
        in_progress: [],
        completed: []
    };

    state.tasks.forEach((task) => {
        if (task.status === "in_progress") {
            byStatus.in_progress.push(task);
        } else if (task.status === "completed") {
            byStatus.completed.push(task);
        } else {
            byStatus.todo.push(task);
        }
    });

    el.colTodo.innerHTML = byStatus.todo.map(renderTaskCard).join("");
    el.colProgress.innerHTML = byStatus.in_progress.map(renderTaskCard).join("");
    el.colCompleted.innerHTML = byStatus.completed.map(renderTaskCard).join("");

    const all = byStatus.todo.length + byStatus.in_progress.length + byStatus.completed.length;
    el.tasksEmpty.classList.toggle("hidden", all > 0);

    renderTaskInsights(byStatus);

    el.colTodo.querySelectorAll(".task-card").forEach(bindDragStart);
    el.colProgress.querySelectorAll(".task-card").forEach(bindDragStart);
    el.colCompleted.querySelectorAll(".task-card").forEach(bindDragStart);
}

function renderTaskInsights(byStatus) {
    const allTasks = [...byStatus.todo, ...byStatus.in_progress, ...byStatus.completed];
    if (allTasks.length === 0) {
        el.taskInsights.innerHTML = "";
        return;
    }

    const highPriority = allTasks.filter((task) => task.priority === "high").length;
    const nextDue = allTasks
        .filter((task) => task.dueDate)
        .sort((a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime())[0];
    const activeRevenue = byStatus.in_progress.reduce((sum, task) => sum + Number(task.revenue || 0), 0);

    const cards = [
        {
            label: "Critical Load",
            value: `${highPriority} high-priority task${highPriority === 1 ? "" : "s"}`,
            note: highPriority > 0 ? "Consider assigning owners today" : "No urgent task pressure"
        },
        {
            label: "Nearest Deadline",
            value: nextDue ? nextDue.title : "No due dates set",
            note: nextDue ? `Due ${nextDue.dueDate}` : "Add due dates for better planning"
        },
        {
            label: "Revenue in Motion",
            value: formatNumber(activeRevenue),
            note: `${byStatus.in_progress.length} task${byStatus.in_progress.length === 1 ? "" : "s"} in progress`
        }
    ];

    el.taskInsights.innerHTML = cards
        .map((item) => `
            <article class="insight-card">
                <p>${escapeHtml(item.label)}</p>
                <h4>${escapeHtml(item.value)}</h4>
                <small>${escapeHtml(item.note)}</small>
            </article>
        `)
        .join("");
}

function renderTaskCard(task) {
    return `
        <article class="task-card" draggable="true" data-task-id="${task.id}">
            <h4>${escapeHtml(task.title)}</h4>
            <p>${escapeHtml(task.description || "No description")}</p>
            <div class="task-meta">
                <span class="pill priority-${escapeHtml(task.priority)}">${escapeHtml(task.priority)}</span>
                <span class="pill">Due ${escapeHtml(task.dueDate)}</span>
                <span class="pill">Revenue ${escapeHtml(String(task.revenue))}</span>
            </div>
            <div class="task-actions">
                <small>${escapeHtml(task.projectCode || "")}</small>
                <div>
                    <button data-edit-id="${task.id}">Edit</button>
                    <button class="danger" data-delete-id="${task.id}">Delete</button>
                </div>
            </div>
        </article>
    `;
}

function bindDragStart(card) {
    card.addEventListener("dragstart", (event) => {
        event.dataTransfer.setData("taskId", card.dataset.taskId || "");
    });
}

async function moveTask(taskId, nextStatus) {
    const task = state.tasks.find((item) => item.id === taskId);
    if (!task || task.status === nextStatus) {
        return;
    }

    try {
        await apiRequest(`/api/tasks/${taskId}`, {
            method: "PUT",
            body: {
                title: task.title,
                description: task.description,
                status: nextStatus,
                priority: task.priority,
                dueDate: task.dueDate,
                revenue: task.revenue
            }
        });

        task.status = nextStatus;
        renderTaskBoard();
        await loadDashboard();
        pushToast("Task moved.", "success");
    } catch (error) {
        pushToast(error.message, "error");
    }
}

function openTaskModal(task) {
    el.taskForm.reset();
    if (task) {
        el.taskModalTitle.textContent = "Edit Task";
        el.taskForm.elements.id.value = String(task.id);
        el.taskForm.elements.title.value = task.title;
        el.taskForm.elements.description.value = task.description || "";
        el.taskForm.elements.status.value = task.status;
        el.taskForm.elements.priority.value = task.priority;
        el.taskForm.elements.dueDate.value = task.dueDate;
        el.taskForm.elements.revenue.value = task.revenue;
    } else {
        el.taskModalTitle.textContent = "Create Task";
        el.taskForm.elements.id.value = "";
        const nextWeek = new Date();
        nextWeek.setDate(nextWeek.getDate() + 7);
        el.taskForm.elements.dueDate.value = nextWeek.toISOString().slice(0, 10);
        el.taskForm.elements.revenue.value = "40000";
    }

    el.taskModal.classList.remove("hidden");
}

function closeTaskModal() {
    el.taskModal.classList.add("hidden");
}

async function saveTask() {
    const payload = {
        title: el.taskForm.elements.title.value.trim(),
        description: el.taskForm.elements.description.value.trim(),
        status: el.taskForm.elements.status.value,
        priority: el.taskForm.elements.priority.value,
        dueDate: el.taskForm.elements.dueDate.value,
        revenue: Number(el.taskForm.elements.revenue.value)
    };

    const id = el.taskForm.elements.id.value;

    if (!payload.title || !payload.dueDate || !payload.revenue) {
        pushToast("Title, due date, and revenue are required.", "error");
        return;
    }

    withLoading(true);
    try {
        const endpoint = id ? `/api/tasks/${id}` : "/api/tasks";
        const method = id ? "PUT" : "POST";

        const result = await apiRequest(endpoint, {method, body: payload});
        if (!result.success) {
            throw new Error(result.message || "Task save failed.");
        }

        closeTaskModal();
        await loadTasks();
        pushToast(id ? "Task updated." : "Task created.", "success");
    } catch (error) {
        pushToast(error.message, "error");
    } finally {
        withLoading(false);
    }
}

function promptDeleteTask(taskId) {
    state.deletingId = taskId;
    el.deleteModal.classList.remove("hidden");
}

function closeDeleteModal() {
    state.deletingId = null;
    el.deleteModal.classList.add("hidden");
}

async function confirmDeleteTask() {
    if (!state.deletingId) {
        return;
    }

    withLoading(true);
    try {
        const result = await apiRequest(`/api/tasks/${state.deletingId}`, {method: "DELETE"});
        if (!result.success) {
            throw new Error(result.message || "Delete failed.");
        }

        closeDeleteModal();
        await loadTasks();
        pushToast("Task deleted.", "success");
    } catch (error) {
        pushToast(error.message, "error");
    } finally {
        withLoading(false);
    }
}

async function apiRequest(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };

    if (options.auth !== false && state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }

    const candidateBases = buildApiBaseCandidates(API_BASE);
    let lastNetworkError = null;

    for (const base of candidateBases) {
        const url = toApiUrl(path, base);

        try {
            const response = await fetch(url, {
                method: options.method || "GET",
                headers,
                body: options.body ? JSON.stringify(options.body) : undefined
            });

            const rawBody = await response.text();
            const payload = tryParseJson(rawBody) || {
                success: false,
                message: buildNonJsonMessage(rawBody, url)
            };

            if (response.status === 401) {
                if (options.auth !== false) {
                    clearSession();
                    showAuth();
                    throw new Error("Session expired. Login again.");
                }

                throw new Error(payload.message || "Unauthorized.");
            }

            if (!response.ok) {
                throw new Error(payload.message || `Request failed with status ${response.status}.`);
            }

            if (base !== API_BASE) {
                API_BASE = base;
                localStorage.setItem("pm_api_base", API_BASE);
            }

            return payload;
        } catch (error) {
            if (error instanceof TypeError) {
                lastNetworkError = error;
                continue;
            }
            throw error;
        }
    }

    if (lastNetworkError) {
        const tried = candidateBases.join(" or ");
        throw new Error(`Cannot reach API server at ${tried}. Start backend with 'mvn exec:java' and try again.`);
    }

    throw new Error("API request failed.");
}

function resolveApiBase() {
    const query = new URLSearchParams(window.location.search);
    const fromQuery = normalizeApiBase(query.get("apiBase"));
    if (fromQuery) {
        localStorage.setItem("pm_api_base", fromQuery);
        return fromQuery;
    }

    const fromStorage = normalizeApiBase(localStorage.getItem("pm_api_base"));
    if (fromStorage) {
        return fromStorage;
    }

    if (window.location.protocol === "file:" || !window.location.hostname) {
        return "http://localhost:8080";
    }

    const isLocalHost = window.location.hostname === "127.0.0.1" || window.location.hostname === "localhost";
    if (isLocalHost) {
        return `${window.location.protocol}//${window.location.hostname}:8080`;
    }

    return normalizeApiBase(window.location.origin) || "http://localhost:8080";
}

function toApiUrlWithBase(path, base) {
    if (/^https?:\/\//i.test(path)) {
        return path;
    }

    if (path.startsWith("/")) {
        return `${base}${path}`;
    }

    return `${base}/${path}`;
}

function toApiUrl(path, base) {
    return toApiUrlWithBase(path, base || API_BASE);
}

function buildApiBaseCandidates(primaryBase) {
    const candidates = [];
    const normalizedPrimary = normalizeApiBase(primaryBase);
    if (normalizedPrimary) {
        candidates.push(normalizedPrimary);
    }

    if (window.location.protocol === "file:") {
        candidates.push("http://localhost:8080");
        candidates.push("http://127.0.0.1:8080");
    } else {
        if (window.location.hostname === "127.0.0.1") {
            candidates.push(`${window.location.protocol}//localhost:8080`);
        }

        if (window.location.hostname === "localhost") {
            candidates.push(`${window.location.protocol}//127.0.0.1:8080`);
        }

        candidates.push(`${window.location.protocol}//localhost:8080`);
        candidates.push(`${window.location.protocol}//127.0.0.1:8080`);
    }

    if (candidates.length === 0) {
        candidates.push("http://localhost:8080");
    }

    return [...new Set(candidates.map(normalizeApiBase).filter(Boolean))];
}

function normalizeApiBase(value) {
    if (!value) {
        return "";
    }

    const normalized = value.trim().replace(/\/$/, "");
    if (!normalized || normalized === "null" || normalized === "undefined") {
        return "";
    }

    return normalized;
}

function normalizeSignupPayload(payload) {
    const name = String(payload.name || "").trim();
    const email = String(payload.email || "").trim().toLowerCase();
    const password = String(payload.password || "");
    const confirmPassword = String(payload.confirmPassword || "");

    if (!name) {
        return {ok: false, message: "Please enter your name."};
    }

    if (!email || !email.includes("@")) {
        return {ok: false, message: "Please enter a valid email address."};
    }

    if (password.length < 6) {
        return {ok: false, message: "Password must be at least 6 characters."};
    }

    if (password !== confirmPassword) {
        return {ok: false, message: "Passwords do not match."};
    }

    return {
        ok: true,
        data: {name, email, password}
    };
}

function normalizeLoginPayload(payload) {
    const email = String(payload.email || "").trim().toLowerCase();
    const password = String(payload.password || "");

    if (!email || !email.includes("@")) {
        return {ok: false, message: "Please enter a valid email address."};
    }

    if (!password) {
        return {ok: false, message: "Please enter your password."};
    }

    return {
        ok: true,
        data: {email, password}
    };
}

function setAuthPending(mode, pending) {
    const form = mode === "signup" ? el.signupForm : el.loginForm;
    const submit = form.querySelector("button[type='submit']");
    if (!(submit instanceof HTMLButtonElement)) {
        return;
    }

    form.querySelectorAll("input,button").forEach((node) => {
        node.disabled = pending;
    });

    if (pending) {
        submit.dataset.label = submit.textContent || "";
        submit.textContent = mode === "signup" ? "Creating account..." : "Signing in...";
        return;
    }

    submit.textContent = submit.dataset.label || (mode === "signup" ? "Sign Up" : "Login");
}

function tryParseJson(rawBody) {
    if (!rawBody || !rawBody.trim()) {
        return null;
    }

    try {
        return JSON.parse(rawBody);
    } catch {
        return null;
    }
}

function buildNonJsonMessage(rawBody, url) {
    const body = (rawBody || "").trim();
    if (!body) {
        return `Empty response from ${url}.`;
    }

    if (/<!doctype|<html/i.test(body)) {
        return `API endpoint not reached at ${url}. Backend should run on ${API_BASE}.`;
    }

    return "Invalid server response.";
}

function withLoading(enabled) {
    el.loading.classList.toggle("hidden", !enabled);
}

function pushToast(message, type = "success") {
    const node = document.createElement("div");
    node.className = `toast ${type === "error" ? "error" : ""}`;
    node.textContent = message;
    el.toastHost.appendChild(node);

    window.setTimeout(() => {
        node.remove();
    }, 2800);
}

function formatMetricValue(label, value) {
    if (label === "Revenue") {
        return formatNumber(Number(value || 0));
    }
    return String(value ?? 0);
}

function formatNumber(value) {
    return new Intl.NumberFormat("en-IN").format(Number(value || 0));
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function debounce(fn, delay) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
}
