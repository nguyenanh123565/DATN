/**
 * SmartHome Frontend - Main JS
 * Chứa cấu hình Axios, Utils và các hàm dùng chung
 */

// Base URL cho API
const API_BASE_URL = 'http://localhost:8080/api/v1';

// Cấu hình Axios
const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
});

// Interceptor nạp Token vào mỗi request
apiClient.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
}, error => {
    return Promise.reject(error);
});

// Format tiền tệ VNĐ
function formatCurrency(amount) {
    if(!amount) return '0 đ';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

// Thông báo chung dùng SweetAlert2 (toast style)
function showToast(icon, message) {
    const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true,
        background: '#16213e',
        color: '#fff',
        didOpen: (toast) => {
            toast.addEventListener('mouseenter', Swal.stopTimer)
            toast.addEventListener('mouseleave', Swal.resumeTimer)
        }
    });

    Toast.fire({
        icon: icon, // 'success', 'error', 'warning', 'info'
        title: message
    });
}

// Kiểm tra đăng nhập và cập nhật UI navbar
function checkAuth() {
    const token = localStorage.getItem('token');
    const fullName = localStorage.getItem('fullName');
    const loginBtn = document.getElementById('nav-login-btn');
    const userMenu = document.getElementById('nav-user-menu');

    if (token) {
        if (loginBtn) loginBtn.style.display = 'none';
        if (userMenu) {
            userMenu.style.display = 'block';
            // Hiển thị tên user nếu có element
            const userNameEl = document.getElementById('nav-user-name');
            if (userNameEl && fullName) {
                userNameEl.textContent = fullName;
            }
        }
        // Load số lượng giỏ hàng
        loadCartCount();
    } else {
        if (loginBtn) loginBtn.style.display = 'block';
        if (userMenu) userMenu.style.display = 'none';
    }
}

// Lấy số lượng sản phẩm trong giỏ
function loadCartCount() {
    const badge = document.getElementById('cart-count');
    if (!badge) return;
    apiClient.get('/cart')
        .then(res => {
            const totalItems = res.data?.data?.totalItems || 0;
            badge.textContent = totalItems;
            badge.style.display = totalItems > 0 ? 'inline' : 'none';
        })
        .catch(() => {
            badge.style.display = 'none';
        });
}

// Đăng xuất
function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('fullName');
    localStorage.removeItem('role');
    localStorage.removeItem('email');
    showToast('success', 'Đã đăng xuất thành công!');
    setTimeout(() => {
        window.location.href = '/index.html';
    }, 1000);
}

// Hàm khởi tạo dùng chung (gọi sau khi load xong DOM)
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();

    // Auth Button event binding
    const logoutBtn = document.getElementById('logout-btn');
    if(logoutBtn) logoutBtn.addEventListener('click', (e) => { e.preventDefault(); logout(); });
});
