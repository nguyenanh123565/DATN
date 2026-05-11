/**
 * SmartHome Admin - Main Scripts
 * Xử lý xác thực Token (Role Admin), Cấu hình Axios, và Sidebar Menu Toggle
 */

const API_BASE_URL = 'http://localhost:8080/api/v1';

// Cấu hình Axios
const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
});

// Kiểm tra quyền truy cập: Giả lập decode JWT để check Role = ADMIN
function checkAdminAccess() {
    const token = localStorage.getItem('token');
    if (!token) {
        forcePublicLogin();
        return false;
    }
    
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const roles = payload.roles || [];
        // Spring Security ghi nhận role theo set. Nếu danh sách role chứa ROLE_ADMIN
        if (!roles.includes('ROLE_ADMIN') && !roles.includes('ADMIN')) {
            // Cản lại
            alert('Bạn không có quyền truy cập trang Quản Trị Viên!');
            window.location.href = '../index.html';
            return false;
        }
        
        // Hiển thị tên Admin
        const adminNameEl = document.getElementById('admin-name');
        if(adminNameEl) adminNameEl.innerText = payload.sub || 'Quản trị viên';
        
        return true;
    } catch (e) {
        forcePublicLogin();
        return false;
    }
}

function forcePublicLogin() {
    localStorage.removeItem('token');
    alert('Phiên đăng nhập không hợp lệ hoặc hết hạn. Vui lòng đăng nhập lại.');
    window.location.href = '../auth.html';
}

function logoutAdmin() {
    localStorage.removeItem('token');
    window.location.href = '../auth.html';
}

// Interceptor nạp Token
apiClient.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
}, error => Promise.reject(error));

// SweetAlert2 Toast configuration
function showToast(icon, message) {
    const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true,
        background: '#1e293b',
        color: '#fff',
    });
    Toast.fire({ icon: icon, title: message });
}

// Format Tiền tệ
function formatCurrency(amount) {
    if(!amount) return '0 đ';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

// --- DOM Ready ---
document.addEventListener('DOMContentLoaded', () => {
    // 1. Kiểm tra Quyền
    if (!checkAdminAccess()) return;

    // 2. Xử lý UI Toggle Sidebar
    const toggleBtn = document.getElementById('toggle-btn');
    const sidebar = document.getElementById('sidebar');
    const mainContent = document.getElementById('main-content');

    if (toggleBtn && sidebar && mainContent) {
        toggleBtn.addEventListener('click', () => {
            if (window.innerWidth <= 991) {
                sidebar.classList.toggle('mobile-open');
            } else {
                sidebar.classList.toggle('collapsed');
                mainContent.classList.toggle('expanded');
            }
        });
    }

    // 3. Xử lý Nút Đăng xuất
    const logoutBtn = document.getElementById('logout-btn-action');
    if(logoutBtn) logoutBtn.addEventListener('click', logoutAdmin);
    
    // 4. Đánh dấu menu active
    const links = document.querySelectorAll('.menu-item');
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    links.forEach(link => {
        if(link.getAttribute('href') === currentPage) {
            link.classList.add('active');
        }
    });

    // 5. Xử lý Báo cáo (Report Page)
    const btnPreview = document.getElementById('btn-preview-report');
    const btnExportExcel = document.getElementById('btn-export-excel');
    const btnPrint = document.getElementById('btn-print-report');
    const previewSection = document.getElementById('preview-section');
    const previewBody = document.getElementById('report-preview-body');

    // Hàm lấy tham số ngày tháng và trạng thái
    const getFilterParams = () => {
        const fromDate = document.getElementById('filter-from-date').value;
        const toDate = document.getElementById('filter-to-date').value;
        const statusEl = document.getElementById('filter-status');
        const status = statusEl ? statusEl.value : '';
        
        let params = [];
        if (fromDate) params.push(`from=${fromDate}T00:00:00`);
        if (toDate) params.push(`to=${toDate}T23:59:59`);
        if (status) params.push(`status=${status}`);
        return params.length > 0 ? '?' + params.join('&') : '';
    };

    // Nút Xem trước (Preview)
    if (btnPreview) {
        btnPreview.addEventListener('click', async () => {
            try {
                showToast('info', 'Đang tải dữ liệu...');
                const params = getFilterParams();
                const response = await apiClient.get(`/reports/preview${params}`);
                
                const orders = response.data.data || [];
                renderReportPreview(orders);
                
                previewSection.style.display = 'block';
                showToast('success', `Đã tìm thấy ${orders.length} đơn hàng.`);
            } catch (error) {
                console.error(error);
                showToast('error', 'Lỗi khi tải dữ liệu xem trước.');
            }
        });
    }

    function renderReportPreview(orders) {
        if (!previewBody) return;
        previewBody.innerHTML = '';
        let total = 0;

        orders.forEach(order => {
            total += order.finalAmount || 0;
            const row = `
                <tr>
                    <td>#${order.id}</td>
                    <td>${order.fullName || 'N/A'}</td>
                    <td>${order.phone || 'N/A'}</td>
                    <td>${order.shippingAddress || 'N/A'}</td>
                    <td class="fw-bold text-primary">${formatCurrency(order.finalAmount)}</td>
                    <td><span class="badge ${getStatusBadge(order.status)}">${order.status}</span></td>
                    <td>${new Date(order.createdAt).toLocaleDateString('vi-VN')}</td>
                </tr>
            `;
            previewBody.insertAdjacentHTML('beforeend', row);
        });

        document.getElementById('report-summary-count').innerText = `${orders.length} đơn hàng`;
        document.getElementById('report-total-revenue').innerText = formatCurrency(total);
    }

    function getStatusBadge(status) {
        switch (status) {
            case 'DELIVERED': return 'bg-success';
            case 'PENDING': return 'bg-warning';
            case 'SHIPPING': return 'bg-primary';
            case 'CANCELLED': return 'bg-danger';
            default: return 'bg-secondary';
        }
    }

    // Nút Xuất Excel
    if (btnExportExcel) {
        btnExportExcel.addEventListener('click', async () => {
            try {
                showToast('info', 'Đang khởi tạo báo cáo Excel...');
                const params = getFilterParams();
                
                const response = await fetch(`${API_BASE_URL}/reports/export/excel${params}`, {
                    headers: {
                        'Authorization': `Bearer ${localStorage.getItem('token')}`
                    }
                });

                if (!response.ok) throw new Error('Không thể tải báo cáo');

                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `bao_cao_doanh_thu_${new Date().getTime()}.xlsx`;
                document.body.appendChild(a);
                a.click();
                a.remove();
                window.URL.revokeObjectURL(url);
                
                showToast('success', 'Đã tải báo cáo Excel thành công!');
            } catch (error) {
                console.error(error);
                showToast('error', 'Lỗi khi xuất báo cáo: ' + error.message);
            }
        });
    }

    // Nút In
    if (btnPrint) {
        btnPrint.addEventListener('click', () => {
            if (previewSection.style.display === 'none') {
                showToast('warning', 'Vui lòng bấm "Xem trước" để nạp dữ liệu trước khi in!');
                return;
            }
            window.print();
        });
    }
});
