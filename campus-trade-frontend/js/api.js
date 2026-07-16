/**
 * campus-trade-frontend/js/api.js
 * 统一封装对网关 (localhost:8080) 的 REST 调用
 */
const API_BASE = ['localhost', '127.0.0.1'].includes(location.hostname)
    ? 'http://localhost:8080'
    : `${location.protocol}//${location.hostname}:8080`;

// ── Token / 用户信息 本地存储 ───────────────────────────────
const auth = {
    getToken:    () => localStorage.getItem('ct_token'),
    setToken:    (t) => localStorage.setItem('ct_token', t),
    removeToken: () => localStorage.removeItem('ct_token'),
    getUser:     () => JSON.parse(localStorage.getItem('ct_user') || 'null'),
    setUser:     (u) => localStorage.setItem('ct_user', JSON.stringify(u)),
    removeUser:  () => localStorage.removeItem('ct_user'),
    isLoggedIn:  () => !!localStorage.getItem('ct_token'),
    isAdmin:     () => {
        const user = JSON.parse(localStorage.getItem('ct_user') || 'null');
        return !!user && user.username === 'admin';
    },
    logout() {
        this.removeToken();
        this.removeUser();
        window.location.href = '/login.html';
    }
};

// ── 基础请求 ──────────────────────────────────────────────
async function request(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    const token = auth.getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch(API_BASE + path, { ...options, headers, cache: 'no-store' });
    const json = await res.json().catch(() => ({}));

    if (json.code === 401 || res.status === 401) {
        auth.logout();
        return;
    }
    if (!res.ok || (json.code && json.code !== 200)) {
        throw new Error(json.message || '请求失败');
    }
    return json.data;
}

const get  = (path)       => request(path, { method: 'GET' });
const post = (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) });
const put  = (path)       => request(path, { method: 'PUT' });
const del  = (path)       => request(path, { method: 'DELETE' });

// ── 用户 / 认证 ───────────────────────────────────────────
const userApi = {
    async login(username, password) {
        const data = await post('/api/auth/login', { username, password });
        auth.setToken(data.token);
        auth.setUser({ userId: data.userId, username: data.username, nickname: data.nickname });
        return data;
    },
    async register(form) {
        return post('/api/auth/register', form);
    },
    async logout() {
        try { await post('/api/auth/logout', {}); } catch {}
        auth.logout();
    },
    async me() { return get('/api/users/me'); }
};

const adminApi = {
    users() {
        return get('/api/users/admin/all');
    },
    products() {
        return get('/api/products/admin/all');
    },
    offlineProduct(id) {
        return put(`/api/products/admin/${id}/offline`);
    },
    orders() {
        return get('/api/orders/admin/all');
    },
    messages() {
        return get('/api/products/admin/messages');
    },
    deleteMessage(id) {
        return del(`/api/products/admin/messages/${id}`);
    }
};

// ── 商品 ──────────────────────────────────────────────────
const productApi = {
    list(keyword = '', page = 1, size = 12) {
        const q = new URLSearchParams({ page, size });
        if (keyword) q.set('keyword', keyword);
        return get('/api/products?' + q);
    },
    detail(id)         { return get(`/api/products/${id}`); },
    create(data)       { return post('/api/products', data); },
    offline(id)        { return put(`/api/products/${id}/offline`); },
    toggleFavorite(id) { return post(`/api/products/${id}/favorite`, {}); },
    addMessage(id, content) {
        return request(`/api/products/${id}/messages`, {
            method: 'POST',
            headers: { 'Content-Type': 'text/plain' },
            body: content
        });
    },
    messages(id)       { return get(`/api/products/${id}/messages`); },
    myProducts()       { return get('/api/products/my'); },
    myFavorites()      { return get('/api/products/favorites'); }
};

// ── 订单 ──────────────────────────────────────────────────
const orderApi = {
    place(productId, remark = '') {
        const q = new URLSearchParams({ productId });
        if (remark) q.set('remark', remark);
        return post('/api/orders?' + q, {});
    },
    cancel(id)          { return put(`/api/orders/${id}/cancel`); },
    myBuyOrders()       { return get('/api/orders/buy'); },
    mySellOrders()      { return get('/api/orders/sell'); }
};

// ── 支付 ──────────────────────────────────────────────────
const paymentApi = {
    async pay(orderNo) {
        const token = auth.getToken();
        const res = await fetch(`${API_BASE}/api/payment/pay?orderNo=${orderNo}`, {
            headers: { 'Authorization': 'Bearer ' + token },
            cache: 'no-store'
        });
        const html = await res.text();
        // 将支付宝返回的自提交表单写入页面并自动提交
        const div = document.createElement('div');
        div.innerHTML = html;
        document.body.appendChild(div);
        div.querySelector('form').submit();
    },
    sync(orderNo) {
        return post('/api/payment/sync?orderNo=' + encodeURIComponent(orderNo), {});
    }
};

// ── 工具函数 ──────────────────────────────────────────────
function requireLogin() {
    if (!auth.isLoggedIn()) {
        window.location.href = '/login.html?redirect=' + encodeURIComponent(location.href);
        return false;
    }
    return true;
}

function showToast(msg, type = 'success') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.style.cssText = 'position:fixed;top:20px;right:20px;z-index:9999;display:flex;flex-direction:column;gap:8px;';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `alert alert-${type === 'error' ? 'danger' : type} alert-dismissible shadow`;
    toast.style.cssText = 'min-width:220px;animation:fadeIn .3s';
    toast.innerHTML = `${msg}<button type="button" class="btn-close" onclick="this.parentElement.remove()"></button>`;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

function productImageUrl(src) {
    const fallback = '/assets/products/placeholder.svg';
    if (!src) return fallback;
    return normalizeImagePath(src) || fallback;
}

function normalizeImagePath(src) {
    let value = String(src).trim().replace(/\\/g, '/');
    if (!value) return undefined;
    if (/^(https?:|data:|blob:)/i.test(value)) return value;

    const assetsIndex = value.indexOf('/assets/');
    if (assetsIndex >= 0) return value.slice(assetsIndex);
    if (value.startsWith('assets/')) return '/' + value;
    if (value.startsWith('/assets/')) return value;

    return value.startsWith('/') ? value : '/' + value;
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('zh-CN', { hour12: false });
}

function statusLabel(status) {
    const map = {
        ON_SALE: '<span class="badge bg-success">在售</span>',
        LOCKED:  '<span class="badge bg-warning text-dark">交易中</span>',
        SOLD:    '<span class="badge bg-secondary">已售出</span>',
        OFFLINE: '<span class="badge bg-dark">已下架</span>',
        CREATED:    '<span class="badge bg-primary">待支付</span>',
        PAID:       '<span class="badge bg-success">已支付</span>',
        COMPLETED:  '<span class="badge bg-secondary">已完成</span>',
        CANCELED:   '<span class="badge bg-danger">已取消</span>'
    };
    return map[status] || '<span class="badge bg-light text-dark">未知状态</span>';
}
