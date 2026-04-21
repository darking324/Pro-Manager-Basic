const API_BASE = '/api';

// --- Auth Bridge ---
const loginForm = document.querySelector('form');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        
        try {
            const res = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            const data = await res.json();
            
            if (data.success) {
                localStorage.setItem('auth_token', data.data.token);
                window.location.href = 'kanban.html';
            } else {
                alert('Login failed: ' + data.message);
            }
        } catch (err) {
            console.error('API Error:', err);
        }
    });
}

// --- Kanban & Dashboard Logic ---
const token = localStorage.getItem('auth_token');

async function authFetch(url, options = {}) {
    return fetch(url, {
        ...options,
        headers: {
            ...options.headers,
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        }
    }).then(res => res.json());
}

async function loadDashboard() {
    const data = await authFetch(`${API_BASE}/dashboard`);
    if (data.success) {
        // Update DOM elements matching the stitch UI placeholders
        // e.g. document.getElementById('total-revenue').innerText = '$' + data.data.totalRevenue.toLocaleString();
        console.log('Dashboard metrics loaded:', data.data);
    }
}

async function loadTasks() {
    const data = await authFetch(`${API_BASE}/tasks`);
    if (data.success) {
        console.log('Tasks loaded:', data.data);
        renderTasks(data.data);
    }
}

function renderTasks(tasks) {
    const containers = {
        'todo': document.getElementById('col-todo'),
        'in_progress': document.getElementById('col-in-progress'),
        'completed': document.getElementById('col-completed')
    };

    // Clear existing
    Object.values(containers).forEach(c => c && (c.innerHTML = ''));

    tasks.forEach(task => {
        const col = containers[task.status];
        if (!col) return;
        
        const card = document.createElement('div');
        card.className = "bg-surface-container-lowest p-6 rounded-lg mb-4 shadow-sm cursor-grab active:cursor-grabbing";
        card.draggable = true;
        card.dataset.id = task.id;
        
        card.innerHTML = `
            <div class="flex justify-between items-start mb-4">
                <h4 class="text-on-surface font-semibold text-lg">${task.title}</h4>
                <span class="text-tertiary-container font-bold">$${task.revenue.toLocaleString()}</span>
            </div>
            <div class="text-sm text-outline mb-2">${task.description || 'No description'}</div>
            <div class="flex justify-between items-center text-xs mt-4">
                <span class="bg-surface-container text-on-surface px-3 py-1 rounded-full">${task.priority}</span>
                <span class="text-error font-medium">Due: ${task.dueDate}</span>
            </div>
        `;
        
        card.addEventListener('dragstart', (e) => {
            e.dataTransfer.setData('text/plain', task.id);
        });

        col.appendChild(card);
    });
}

// Drag functionality setup
document.querySelectorAll('.kanban-column').forEach(column => {
    column.addEventListener('dragover', e => e.preventDefault());
    column.addEventListener('drop', async e => {
        e.preventDefault();
        const taskId = e.dataTransfer.getData('text/plain');
        const newStatus = column.dataset.status; // ensure columns have data-status attribute
        
        // Optimistic UI update
        const card = document.querySelector(`[data-id="${taskId}"]`);
        if(card && column.querySelector('.task-container')) {
             column.querySelector('.task-container').appendChild(card);
        }

        // Call our new WebMain.java endpoint
        const res = await authFetch(`${API_BASE}/tasks/update-status`, {
            method: 'PUT',
            body: JSON.stringify({
                id: parseInt(taskId),
                status: newStatus
            })
        });

        if (!res.success) {
            alert('Failed to update task status in database!');
            loadTasks(); // revert changes
        } else {
            loadDashboard(); // Refresh dynamic revenue metrics
        }
    });
});

// Init
if (window.location.pathname.includes('kanban.html')) {
    if (!token) window.location.href = 'login.html';
    loadDashboard();
    loadTasks();
}
