/**
 * 管理员仪表盘JavaScript
 */
document.addEventListener('DOMContentLoaded', function() {
    // 初始化
    checkLoginStatus();
    setupTabSwitching();
    setupLogout();
    
    // 加载仪表盘数据
    loadDashboardData();
    
    // 根据角色显示/隐藏某些菜单项
    updateMenuByRole();
});

/**
 * 检查登录状态
 */
function checkLoginStatus() {
    fetch('/course-selection/api/admin', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => response.json())
    .then(data => {
        if (!data.success) {
            // 未登录，跳转到登录页面
            window.location.href = 'admin_login.html';
            return;
        }
        
        // 更新用户信息
        const admin = data.data;
        document.getElementById('adminName').textContent = admin.name;
        document.getElementById('adminRole').textContent = getRoleName(admin.role);
        
        // 保存管理员信息到全局变量，以便其他函数使用
        window.currentAdmin = admin;
        
        // 根据角色加载不同的内容
        loadContentByRole();
    })
    .catch(error => {
        console.error('检查登录状态失败:', error);
        alert('网络错误，请刷新页面重试');
    });
}

/**
 * 根据角色名称获取中文名称
 */
function getRoleName(role) {
    const roleMap = {
        'SUPER_ADMIN': '超级管理员',
        'COURSE_ADMIN': '课程管理员',
        'STUDENT_ADMIN': '学生管理员'
    };
    
    return roleMap[role] || '管理员';
}

/**
 * 根据管理员角色更新菜单项
 */
function updateMenuByRole() {
    if (!window.currentAdmin) return;
    
    const role = window.currentAdmin.role;
    const menuItems = document.querySelectorAll('.sidebar-menu li');
    
    // 超级管理员可以查看所有菜单
    if (role === 'SUPER_ADMIN') {
        return;
    }
    
    // 课程管理员只能查看首页、课程管理
    if (role === 'COURSE_ADMIN') {
        for (let item of menuItems) {
            const link = item.querySelector('a');
            if (link) {
                const target = link.getAttribute('data-target');
                if (target && !['dashboard', 'courses', 'settings'].includes(target) && !link.id) {
                    item.style.display = 'none';
                }
            }
        }
    }
    
    // 学生管理员只能查看首页、学生管理、交易记录
    if (role === 'STUDENT_ADMIN') {
        for (let item of menuItems) {
            const link = item.querySelector('a');
            if (link) {
                const target = link.getAttribute('data-target');
                if (target && !['dashboard', 'students', 'transactions', 'settings'].includes(target) && !link.id) {
                    item.style.display = 'none';
                }
            }
        }
    }
}

/**
 * 根据管理员角色加载不同的内容
 */
function loadContentByRole() {
    if (!window.currentAdmin) return;
    
    // 加载不同模块
    loadCoursesModule();
    loadStudentsModule();
    loadTransactionsModule();
    loadAdminsModule();
    loadSettingsModule();
}

/**
 * 设置标签页切换功能
 */
function setupTabSwitching() {
    const menuLinks = document.querySelectorAll('.sidebar-menu a[data-target]');
    const tabContents = document.querySelectorAll('.tab-content');
    
    menuLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            // 更新菜单激活状态
            menuLinks.forEach(item => item.classList.remove('active'));
            this.classList.add('active');
            
            // 更新标签页显示
            const targetId = this.getAttribute('data-target');
            tabContents.forEach(tab => {
                tab.classList.remove('active');
                if (tab.id === targetId) {
                    tab.classList.add('active');
                }
            });
            
            // 更新标题
            document.querySelector('.header-title').textContent = this.textContent.trim();
            
            // 更新URL哈希，不刷新页面
            window.location.hash = targetId;
        });
    });
    
    // 初始化：根据URL哈希显示对应标签页
    const hash = window.location.hash.substring(1);
    if (hash) {
        const targetLink = document.querySelector(`.sidebar-menu a[data-target="${hash}"]`);
        if (targetLink) {
            targetLink.click();
        }
    }
}

/**
 * 设置退出登录功能
 */
function setupLogout() {
    document.getElementById('logout').addEventListener('click', function(e) {
        e.preventDefault();
        
        // 确认是否退出
        if (confirm('确定要退出登录吗？')) {
            fetch('/course-selection/api/admin/logout', {
                method: 'GET',
                credentials: 'same-origin'
            })
            .then(response => response.json())
            .then(data => {
                // 无论成功失败，都跳转到登录页面
                window.location.href = 'admin_login.html';
            })
            .catch(error => {
                console.error('退出登录失败:', error);
                // 出错时也跳转到登录页面
                window.location.href = 'admin_login.html';
            });
        }
    });
}

/**
 * 加载仪表盘数据
 */
function loadDashboardData() {
    // 加载学生总数
    fetch('/course-selection/api/admin/stats/students', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            document.getElementById('studentCount').textContent = data.data || '0';
        } else {
            document.getElementById('studentCount').textContent = '加载失败';
        }
    })
    .catch(error => {
        console.error('加载学生总数失败:', error);
        document.getElementById('studentCount').textContent = '网络错误';
    });
    
    // 加载课程总数
    fetch('/course-selection/api/admin/stats/courses', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            document.getElementById('courseCount').textContent = data.data || '0';
        } else {
            document.getElementById('courseCount').textContent = '加载失败';
        }
    })
    .catch(error => {
        console.error('加载课程总数失败:', error);
        document.getElementById('courseCount').textContent = '网络错误';
    });
    
    // 加载今日活跃用户数
    fetch('/course-selection/api/admin/stats/active-users', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            document.getElementById('activeUsers').textContent = data.data || '0';
        } else {
            document.getElementById('activeUsers').textContent = '加载失败';
        }
    })
    .catch(error => {
        console.error('加载活跃用户数失败:', error);
        document.getElementById('activeUsers').textContent = '网络错误';
    });
    
    // 加载最近交易记录
    fetch('/course-selection/api/admin/transactions/recent', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success && data.data) {
            renderRecentTransactions(data.data);
        } else {
            document.getElementById('recentTransactions').innerHTML = '<tr><td colspan="6">没有最近交易记录</td></tr>';
        }
    })
    .catch(error => {
        console.error('加载最近交易记录失败:', error);
        document.getElementById('recentTransactions').innerHTML = '<tr><td colspan="6">加载失败，网络错误</td></tr>';
    });
}

/**
 * 渲染最近交易记录
 */
function renderRecentTransactions(transactions) {
    if (!transactions || transactions.length === 0) {
        document.getElementById('recentTransactions').innerHTML = '<tr><td colspan="6">没有最近交易记录</td></tr>';
        return;
    }
    
    let html = '';
    
    transactions.forEach(transaction => {
        html += `
            <tr>
                <td>${transaction.transactionId}</td>
                <td>${transaction.studentId}</td>
                <td>${getTransactionTypeName(transaction.type)}</td>
                <td>${transaction.amount.toFixed(2)}</td>
                <td>${new Date(transaction.transactionDate).toLocaleString()}</td>
                <td>${transaction.status ? '成功' : '失败'}</td>
            </tr>
        `;
    });
    
    document.getElementById('recentTransactions').innerHTML = html;
}

/**
 * 获取交易类型的中文名称
 */
function getTransactionTypeName(type) {
    const typeMap = {
        'DEPOSIT': '充值',
        'WITHDRAW': '提现',
        'PAYMENT': '支付',
        'REFUND': '退款',
        'TRANSFER': '转账'
    };
    
    return typeMap[type] || type;
}

/**
 * 加载课程管理模块
 */
function loadCoursesModule() {
    const coursesContent = document.getElementById('coursesContent');
    
    // 检查权限
    if (window.currentAdmin && !window.currentAdmin.isCourseAdmin && window.currentAdmin.role !== 'SUPER_ADMIN') {
        coursesContent.innerHTML = '<div class="alert alert-warning">您没有权限访问此模块</div>';
        return;
    }
    
    // 构建课程管理界面
    let html = `
        <div class="module-actions">
            <button class="btn btn-primary" id="addCourseBtn"><i class="bi bi-plus"></i> 添加课程</button>
            <div class="search-box">
                <input type="text" class="form-control" id="courseSearch" placeholder="搜索课程...">
                <button class="btn btn-secondary" id="searchCourseBtn"><i class="bi bi-search"></i></button>
            </div>
        </div>
        
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>课程编号</th>
                        <th>课程名称</th>
                        <th>所属院系</th>
                        <th>学分</th>
                        <th>已选人数/容量</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody id="coursesList">
                    <tr>
                        <td colspan="6" class="loading">加载中...</td>
                    </tr>
                </tbody>
            </table>
        </div>
        
        <!-- 分页 -->
        <div class="pagination" id="coursesPagination">
            <!-- 分页内容将通过JavaScript动态生成 -->
        </div>
    `;
    
    coursesContent.innerHTML = html;
    
    // 绑定事件
    document.getElementById('addCourseBtn').addEventListener('click', showAddCourseModal);
    document.getElementById('searchCourseBtn').addEventListener('click', searchCourses);
    document.getElementById('courseSearch').addEventListener('keyup', function(e) {
        if (e.key === 'Enter') {
            searchCourses();
        }
    });
    
    // 加载课程列表
    loadCoursesList();
}

/**
 * 加载学生管理模块
 */
function loadStudentsModule() {
    const studentsContent = document.getElementById('studentsContent');
    
    // 检查权限
    if (window.currentAdmin && !window.currentAdmin.isStudentAdmin && window.currentAdmin.role !== 'SUPER_ADMIN') {
        studentsContent.innerHTML = '<div class="alert alert-warning">您没有权限访问此模块</div>';
        return;
    }
    
    // 构建学生管理界面
    let html = `
        <div class="module-actions">
            <div class="search-box">
                <input type="text" class="form-control" id="studentSearch" placeholder="搜索学生...">
                <button class="btn btn-secondary" id="searchStudentBtn"><i class="bi bi-search"></i></button>
            </div>
        </div>
        
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>学号</th>
                        <th>姓名</th>
                        <th>注册时间</th>
                        <th>余额</th>
                        <th>状态</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody id="studentsList">
                    <tr>
                        <td colspan="6" class="loading">加载中...</td>
                    </tr>
                </tbody>
            </table>
        </div>
        
        <!-- 分页 -->
        <div class="pagination" id="studentsPagination">
            <!-- 分页内容将通过JavaScript动态生成 -->
        </div>
    `;
    
    studentsContent.innerHTML = html;
    
    // 绑定事件
    document.getElementById('searchStudentBtn').addEventListener('click', searchStudents);
    document.getElementById('studentSearch').addEventListener('keyup', function(e) {
        if (e.key === 'Enter') {
            searchStudents();
        }
    });
    
    // 加载学生列表
    loadStudentsList();
}

/**
 * 加载交易记录模块
 */
function loadTransactionsModule() {
    const transactionsContent = document.getElementById('transactionsContent');
    
    // 检查权限
    if (window.currentAdmin && !window.currentAdmin.isStudentAdmin && window.currentAdmin.role !== 'SUPER_ADMIN') {
        transactionsContent.innerHTML = '<div class="alert alert-warning">您没有权限访问此模块</div>';
        return;
    }
    
    // 构建交易记录界面
    let html = `
        <div class="module-actions">
            <div class="filter-container">
                <div class="form-group">
                    <label>交易类型</label>
                    <select class="form-control" id="transactionTypeFilter">
                        <option value="">全部</option>
                        <option value="DEPOSIT">充值</option>
                        <option value="WITHDRAW">提现</option>
                        <option value="PAYMENT">支付</option>
                        <option value="REFUND">退款</option>
                        <option value="TRANSFER">转账</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>日期范围</label>
                    <div class="date-range">
                        <input type="date" class="form-control" id="startDate">
                        <span>至</span>
                        <input type="date" class="form-control" id="endDate">
                    </div>
                </div>
                <button class="btn btn-secondary" id="filterTransactionsBtn">筛选</button>
            </div>
            <div class="search-box">
                <input type="text" class="form-control" id="transactionSearch" placeholder="学生ID...">
                <button class="btn btn-secondary" id="searchTransactionBtn"><i class="bi bi-search"></i></button>
            </div>
        </div>
        
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>交易ID</th>
                        <th>学生ID</th>
                        <th>类型</th>
                        <th>金额</th>
                        <th>时间</th>
                        <th>状态</th>
                        <th>相关用户</th>
                        <th>详情</th>
                    </tr>
                </thead>
                <tbody id="transactionsList">
                    <tr>
                        <td colspan="8" class="loading">加载中...</td>
                    </tr>
                </tbody>
            </table>
        </div>
        
        <!-- 分页 -->
        <div class="pagination" id="transactionsPagination">
            <!-- 分页内容将通过JavaScript动态生成 -->
        </div>
    `;
    
    transactionsContent.innerHTML = html;
    
    // 绑定事件
    document.getElementById('filterTransactionsBtn').addEventListener('click', filterTransactions);
    document.getElementById('searchTransactionBtn').addEventListener('click', searchTransactions);
    document.getElementById('transactionSearch').addEventListener('keyup', function(e) {
        if (e.key === 'Enter') {
            searchTransactions();
        }
    });
    
    // 设置日期选择器的默认值
    const today = new Date();
    const lastMonth = new Date();
    lastMonth.setMonth(lastMonth.getMonth() - 1);
    
    document.getElementById('startDate').valueAsDate = lastMonth;
    document.getElementById('endDate').valueAsDate = today;
    
    // 加载交易记录列表
    loadTransactionsList();
}

/**
 * 加载管理员管理模块
 */
function loadAdminsModule() {
    const adminsContent = document.getElementById('adminsContent');
    
    // 检查权限：只有超级管理员可以访问
    if (window.currentAdmin && window.currentAdmin.role !== 'SUPER_ADMIN') {
        adminsContent.innerHTML = '<div class="alert alert-warning">您没有权限访问此模块</div>';
        return;
    }
    
    // 构建管理员管理界面
    let html = `
        <div class="module-actions">
            <button class="btn btn-primary" id="addAdminBtn"><i class="bi bi-plus"></i> 添加管理员</button>
        </div>
        
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>管理员ID</th>
                        <th>姓名</th>
                        <th>角色</th>
                        <th>创建时间</th>
                        <th>最后登录</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody id="adminsList">
                    <tr>
                        <td colspan="6" class="loading">加载中...</td>
                    </tr>
                </tbody>
            </table>
        </div>
    `;
    
    adminsContent.innerHTML = html;
    
    // 绑定事件
    document.getElementById('addAdminBtn').addEventListener('click', showAddAdminModal);
    
    // 加载管理员列表
    loadAdminsList();
}

/**
 * 加载系统设置模块
 */
function loadSettingsModule() {
    const settingsContent = document.getElementById('settingsContent');
    
    // 构建系统设置界面
    let html = `
        <div class="card">
            <h3>个人信息</h3>
            <div class="form-group">
                <label>管理员ID</label>
                <input type="text" class="form-control" id="currentAdminId" disabled>
            </div>
            <div class="form-group">
                <label>姓名</label>
                <input type="text" class="form-control" id="currentAdminName">
            </div>
            <div class="form-group">
                <label>角色</label>
                <input type="text" class="form-control" id="currentAdminRole" disabled>
            </div>
            <div class="form-group">
                <label>修改密码</label>
                <input type="password" class="form-control" id="currentAdminPassword" placeholder="输入新密码">
            </div>
            <div class="form-group">
                <label>确认密码</label>
                <input type="password" class="form-control" id="confirmPassword" placeholder="再次输入新密码">
            </div>
            <button class="btn btn-primary" id="saveProfileBtn">保存更改</button>
        </div>
        
        <div class="card">
            <h3>通用设置</h3>
            <!-- 此处可添加其他系统设置项 -->
        </div>
    `;
    
    settingsContent.innerHTML = html;
    
    // 填充当前管理员信息
    if (window.currentAdmin) {
        document.getElementById('currentAdminId').value = window.currentAdmin.adminId || '';
        document.getElementById('currentAdminName').value = window.currentAdmin.name || '';
        document.getElementById('currentAdminRole').value = getRoleName(window.currentAdmin.role) || '';
    }
    
    // 绑定保存按钮事件
    document.getElementById('saveProfileBtn').addEventListener('click', saveProfile);
}

// 其他函数将根据需要实现
// 包括加载各种列表的函数、显示模态框的函数、处理表单提交的函数等 