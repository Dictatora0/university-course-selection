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
    .then(response => {
        // 检查响应状态
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        // 检查内容类型是否为JSON
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new Error(`非预期的响应格式：${contentType}，请检查API实现`);
        }
        
        return response.json();
    })
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
        
        // 给管理员对象添加辅助属性
        admin.isSuperAdmin = admin.role === 'SUPER_ADMIN';
        admin.isCourseAdmin = admin.role === 'COURSE_ADMIN' || admin.role === 'SUPER_ADMIN';
        admin.isStudentAdmin = admin.role === 'STUDENT_ADMIN' || admin.role === 'SUPER_ADMIN';
        
        // 保存管理员信息到全局变量，以便其他函数使用
        window.currentAdmin = admin;
        
        // 根据角色加载不同的内容
        loadContentByRole();
    })
    .catch(error => {
        console.error('检查登录状态失败:', error);
        alert('网络错误，请刷新页面重试: ' + error.message);
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
    fetch('/course-selection/api/admin/stats/activeUsers', {
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
    loadRecentTransactions();
}

/**
 * 加载最近交易记录
 */
function loadRecentTransactions() {
    document.getElementById('recentTransactions').innerHTML = '<tr><td colspan="6" class="loading">加载中...</td></tr>';
    
    fetch('/course-selection/api/admin/stats/recentTransactions', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => {
        // 检查响应状态
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        // 检查内容类型是否为JSON
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new Error(`非预期的响应格式：${contentType}，请检查API实现`);
        }
        
        return response.json();
    })
    .then(data => {
        if (data.success && data.data) {
            renderRecentTransactions(data.data);
        } else {
            document.getElementById('recentTransactions').innerHTML = '<tr><td colspan="6">没有最近交易记录</td></tr>';
        }
    })
    .catch(error => {
        console.error('加载最近交易记录失败:', error);
        document.getElementById('recentTransactions').innerHTML = 
            `<tr><td colspan="6">加载失败: ${error.message || '网络错误'}</td></tr>`;
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

/**
 * 显示添加课程模态框
 */
function showAddCourseModal() {
    // 创建模态框
    let modalHtml = `
        <div class="modal" id="addCourseModal">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">添加课程</h5>
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close" id="closeAddCourseModalBtn">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <form id="addCourseForm">
                            <div class="form-group">
                                <label for="courseId">课程编号</label>
                                <input type="text" class="form-control" id="courseId" required>
                            </div>
                            <div class="form-group">
                                <label for="courseName">课程名称</label>
                                <input type="text" class="form-control" id="courseName" required>
                            </div>
                            <div class="form-group">
                                <label for="deptId">所属院系</label>
                                <select class="form-control" id="deptId" required>
                                    <!-- 院系列表将动态加载 -->
                                    <option value="">-- 请选择院系 --</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="credit">学分</label>
                                <input type="number" class="form-control" id="credit" min="0" step="0.5" required>
                            </div>
                            <div class="form-group">
                                <label for="capacity">容量</label>
                                <input type="number" class="form-control" id="capacity" min="1" required>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal" id="cancelAddCourseBtn">取消</button>
                        <button type="button" class="btn btn-primary" id="saveAddCourseBtn">保存</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    // 添加到页面
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    // 加载院系列表
    loadDepartments();

    // 显示模态框
    const modal = document.getElementById('addCourseModal');
    modal.style.display = 'block';
    modal.style.backgroundColor = 'rgba(0,0,0,0.5)';
    modal.style.position = 'fixed';
    modal.style.zIndex = '1000';
    modal.style.top = '0';
    modal.style.left = '0';
    modal.style.width = '100%';
    modal.style.height = '100%';
    modal.style.overflow = 'auto';
    modal.style.paddingTop = '50px';

    // 设置模态框内容样式
    const modalDialog = modal.querySelector('.modal-dialog');
    modalDialog.style.margin = '10px auto';
    modalDialog.style.maxWidth = '500px';
    modalDialog.style.backgroundColor = '#fff';
    modalDialog.style.borderRadius = '5px';
    modalDialog.style.overflow = 'hidden';

    // 事件监听
    document.getElementById('closeAddCourseModalBtn').addEventListener('click', closeAddCourseModal);
    document.getElementById('cancelAddCourseBtn').addEventListener('click', closeAddCourseModal);
    document.getElementById('saveAddCourseBtn').addEventListener('click', saveAddCourse);
}

/**
 * 关闭添加课程模态框
 */
function closeAddCourseModal() {
    const modal = document.getElementById('addCourseModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 保存添加的课程
 */
function saveAddCourse() {
    const courseId = document.getElementById('courseId').value.trim();
    const courseName = document.getElementById('courseName').value.trim();
    const deptId = document.getElementById('deptId').value.trim();
    const credit = document.getElementById('credit').value;
    const capacity = document.getElementById('capacity').value;

    if (!courseId || !courseName || !deptId || !credit || !capacity) {
        alert('请填写所有必填字段');
        return;
    }

    const newCourse = {
        courseId: courseId,
        courseName: courseName,
        deptId: deptId,
        credit: parseFloat(credit),
        capacity: parseInt(capacity, 10)
    };

    fetch('/course-selection/api/course/add', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(newCourse),
        credentials: 'same-origin'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert('课程添加成功');
            closeAddCourseModal();
            loadCoursesList(); // 重新加载课程列表
        } else {
            alert('课程添加失败: ' + (data.message || '未知错误'));
        }
    })
    .catch(error => {
        console.error('添加课程失败:', error);
        alert('添加课程失败，请检查网络连接');
    });
}

/**
 * 加载院系列表
 */
function loadDepartments() {
    fetch('/course-selection/api/department/list', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success && data.data) {
            const deptSelect = document.getElementById('deptId');
            if (deptSelect) {
                // 保留第一个选项
                const firstOption = deptSelect.options[0];
                deptSelect.innerHTML = '';
                deptSelect.appendChild(firstOption);
                
                // 添加院系选项
                data.data.forEach(dept => {
                    const option = document.createElement('option');
                    option.value = dept.deptId;
                    option.textContent = dept.deptName;
                    deptSelect.appendChild(option);
                });
            }
        }
    })
    .catch(error => {
        console.error('加载院系列表失败:', error);
    });
}

/**
 * 加载课程列表
 */
function loadCoursesList() {
    document.getElementById('coursesList').innerHTML = '<tr><td colspan="6" class="loading">加载中...</td></tr>';
    
    fetch('/course-selection/api/course/list', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => {
        // 检查响应状态
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        // 检查内容类型是否为JSON
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new Error(`非预期的响应格式：${contentType}，请检查API实现`);
        }
        
        return response.json();
    })
    .then(data => {
        if (data.success && data.data && data.data.length > 0) {
            let html = '';
            
            data.data.forEach(course => {
                html += `
                    <tr>
                        <td>${course.courseId}</td>
                        <td>${course.courseName}</td>
                        <td>${course.deptName || '未知'}</td>
                        <td>${course.credit}</td>
                        <td>${course.enrollmentCount || 0}/${course.capacity}</td>
                        <td>
                            <button class="btn btn-secondary btn-sm edit-course-btn" data-id="${course.courseId}">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-danger btn-sm delete-course-btn" data-id="${course.courseId}">
                                <i class="bi bi-trash"></i>
                            </button>
                        </td>
                    </tr>
                `;
            });
            
            document.getElementById('coursesList').innerHTML = html;
            
            // 添加事件监听
            document.querySelectorAll('.edit-course-btn').forEach(btn => {
                btn.addEventListener('click', function() {
                    const courseId = this.getAttribute('data-id');
                    editCourse(courseId);
                });
            });
            
            document.querySelectorAll('.delete-course-btn').forEach(btn => {
                btn.addEventListener('click', function() {
                    const courseId = this.getAttribute('data-id');
                    deleteCourse(courseId);
                });
            });
        } else {
            document.getElementById('coursesList').innerHTML = '<tr><td colspan="6">暂无课程数据</td></tr>';
        }
    })
    .catch(error => {
        console.error('加载课程列表失败:', error);
        document.getElementById('coursesList').innerHTML = `<tr><td colspan="6">加载失败: ${error.message}</td></tr>`;
    });
}

/**
 * 编辑课程
 */
function editCourse(courseId) {
    alert(`编辑课程: ${courseId} 功能尚未实现`);
}

/**
 * 删除课程
 */
function deleteCourse(courseId) {
    if (confirm(`确定要删除课程 ${courseId} 吗？此操作不可恢复！`)) {
        // 发送删除请求
        fetch(`/course-selection/api/course/${courseId}`, {
            method: 'DELETE',
            credentials: 'same-origin'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                alert('课程删除成功');
                loadCoursesList(); // 重新加载课程列表
            } else {
                alert('课程删除失败: ' + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            console.error('删除课程失败:', error);
            alert('删除课程失败，请检查网络连接或服务器日志');
        });
    }
}

/**
 * 搜索课程
 */
function searchCourses() {
    const searchText = document.getElementById('courseSearch').value.trim();
    console.log('搜索课程:', searchText);
    // 这里应该实现搜索课程的功能
}

/**
 * 加载学生列表
 */
function loadStudentsList() {
    document.getElementById('studentsList').innerHTML = '<tr><td colspan="6" class="loading">加载中...</td></tr>';
    
    fetch('/course-selection/api/admin/students', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => {
        // 检查响应状态
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        // 检查内容类型是否为JSON
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new Error(`非预期的响应格式：${contentType}，请检查API实现`);
        }
        
        return response.json();
    })
    .then(data => {
        if (data.success && data.data && data.data.length > 0) {
            let html = '';
            
            data.data.forEach(student => {
                // 使用下划线格式的字段名，这是后端API返回的格式
                const studentId = student.student_id;
                const createdAt = student.created_at;
                const accountStatus = student.account_status;
                
                // 格式化日期（避免Invalid Date）
                let dateStr = '';
                try {
                    if (createdAt) {
                        dateStr = new Date(createdAt).toLocaleString();
                    }
                } catch (e) {
                    console.error('日期格式化失败:', e);
                    dateStr = createdAt || '';
                }
                
                html += `
                    <tr>
                        <td>${studentId || ''}</td>
                        <td>${student.name || ''}</td>
                        <td>${dateStr}</td>
                        <td>${student.balance ? student.balance.toFixed(2) : '0.00'}</td>
                        <td>${accountStatus ? '正常' : '禁用'}</td>
                        <td>
                            <button class="btn btn-secondary btn-sm view-student-btn" data-id="${studentId}">
                                <i class="bi bi-eye"></i>
                            </button>
                            <button class="btn btn-warning btn-sm toggle-status-btn" data-id="${studentId}" data-status="${accountStatus}">
                                ${accountStatus ? '<i class="bi bi-lock"></i>' : '<i class="bi bi-unlock"></i>'}
                            </button>
                        </td>
                    </tr>
                `;
            });
            
            document.getElementById('studentsList').innerHTML = html;
            
            // 添加事件监听
            document.querySelectorAll('.view-student-btn').forEach(btn => {
                btn.addEventListener('click', function() {
                    const studentId = this.getAttribute('data-id');
                    if (studentId) {
                        viewStudentDetail(studentId);
                    } else {
                        alert('获取学生ID失败');
                    }
                });
            });
            
            document.querySelectorAll('.toggle-status-btn').forEach(btn => {
                btn.addEventListener('click', function() {
                    const studentId = this.getAttribute('data-id');
                    if (!studentId) {
                        alert('获取学生ID失败');
                        return;
                    }
                    
                    const currentStatus = this.getAttribute('data-status') === 'true';
                    toggleStudentStatus(studentId, currentStatus);
                });
            });
        } else {
            document.getElementById('studentsList').innerHTML = '<tr><td colspan="6">暂无学生数据</td></tr>';
        }
    })
    .catch(error => {
        console.error('加载学生列表失败:', error);
        document.getElementById('studentsList').innerHTML = `<tr><td colspan="6">加载失败: ${error.message}</td></tr>`;
    });
}

/**
 * 查看学生详情
 */
function viewStudentDetail(studentId) {
    if (!studentId) {
        alert('学生ID无效');
        return;
    }
    
    // 创建模态框
    let modalHtml = `
        <div class="modal" id="studentDetailModal">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">学生详情</h5>
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close" id="closeStudentDetailModalBtn">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body" id="studentDetailContent">
                        <div class="loading-spinner">加载中...</div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal" id="closeStudentDetailBtn">关闭</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    // 添加到页面
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    // 显示模态框
    const modal = document.getElementById('studentDetailModal');
    modal.style.display = 'block';
    modal.style.backgroundColor = 'rgba(0,0,0,0.5)';
    modal.style.position = 'fixed';
    modal.style.zIndex = '1000';
    modal.style.top = '0';
    modal.style.left = '0';
    modal.style.width = '100%';
    modal.style.height = '100%';
    modal.style.overflow = 'auto';
    modal.style.paddingTop = '50px';

    // 设置模态框内容样式
    const modalDialog = modal.querySelector('.modal-dialog');
    modalDialog.style.margin = '10px auto';
    modalDialog.style.maxWidth = '600px';
    modalDialog.style.backgroundColor = '#fff';
    modalDialog.style.borderRadius = '5px';
    modalDialog.style.overflow = 'hidden';

    // 绑定关闭事件
    document.getElementById('closeStudentDetailModalBtn').addEventListener('click', closeStudentDetailModal);
    document.getElementById('closeStudentDetailBtn').addEventListener('click', closeStudentDetailModal);
    
    // 获取学生详情
    fetch(`/course-selection/api/admin/students/${studentId}`, {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        if (data.success && data.data) {
            const student = data.data;
            
            // 格式化日期
            let createdAtStr = '';
            let birthDateStr = '';
            try {
                if (student.created_at || student.createdAt) {
                    createdAtStr = new Date(student.created_at || student.createdAt).toLocaleString();
                }
                if (student.birth_date || student.birthDate) {
                    birthDateStr = new Date(student.birth_date || student.birthDate).toLocaleDateString();
                }
            } catch (e) {
                console.error('日期格式化失败:', e);
            }
            
            let html = `
                <div class="student-detail">
                    <div class="detail-group">
                        <label>学号：</label>
                        <span>${student.student_id || student.studentId || ''}</span>
                    </div>
                    <div class="detail-group">
                        <label>姓名：</label>
                        <span>${student.name || ''}</span>
                    </div>
                    <div class="detail-group">
                        <label>院系：</label>
                        <span>${student.dept_name || student.deptName || '未设置'}</span>
                    </div>
                    <div class="detail-group">
                        <label>出生日期：</label>
                        <span>${birthDateStr || '未设置'}</span>
                    </div>
                    <div class="detail-group">
                        <label>身份证号：</label>
                        <span>${student.id_card || student.idCard || '未设置'}</span>
                    </div>
                    <div class="detail-group">
                        <label>地址：</label>
                        <span>${student.address || '未设置'}</span>
                    </div>
                    <div class="detail-group">
                        <label>账户余额：</label>
                        <span>${student.balance ? student.balance.toFixed(2) : '0.00'} 元</span>
                    </div>
                    <div class="detail-group">
                        <label>账户状态：</label>
                        <span class="${(student.account_status || student.accountStatus) ? 'text-success' : 'text-danger'}">
                            ${(student.account_status || student.accountStatus) ? '正常' : '禁用'}
                        </span>
                    </div>
                    <div class="detail-group">
                        <label>注册时间：</label>
                        <span>${createdAtStr || '未知'}</span>
                    </div>
                </div>
            `;
            
            document.getElementById('studentDetailContent').innerHTML = html;
            
            // 添加样式
            const detailGroups = document.querySelectorAll('.detail-group');
            detailGroups.forEach(group => {
                group.style.marginBottom = '12px';
                group.style.display = 'flex';
            });
            
            const labels = document.querySelectorAll('.detail-group label');
            labels.forEach(label => {
                label.style.fontWeight = 'bold';
                label.style.minWidth = '80px';
            });
            
        } else {
            document.getElementById('studentDetailContent').innerHTML = `
                <div class="alert alert-warning">获取学生信息失败：${data.message || '未知错误'}</div>
            `;
        }
    })
    .catch(error => {
        console.error('获取学生详情失败:', error);
        document.getElementById('studentDetailContent').innerHTML = `
            <div class="alert alert-danger">获取学生详情失败：${error.message || '网络错误'}</div>
        `;
    });
}

/**
 * 关闭学生详情模态框
 */
function closeStudentDetailModal() {
    const modal = document.getElementById('studentDetailModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 切换学生状态（启用/禁用）
 */
function toggleStudentStatus(studentId, currentStatus) {
    const newStatus = !currentStatus;
    const action = newStatus ? '启用' : '禁用';
    
    if (confirm(`确定要${action}学生 ${studentId} 吗？`)) {
        console.log(`正在${action}学生: ${studentId}, 新状态: ${newStatus}`);
        
        fetch(`/course-selection/api/admin/students/${studentId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ status: newStatus }),
            credentials: 'same-origin'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                // 无论响应中的status是什么，我们都使用请求中设置的newStatus
                console.log('状态更新成功，重新加载学生列表');
                alert(`学生${action}成功`);
                loadStudentsList(); // 重新加载学生列表来显示最新的状态
            } else {
                console.error('状态更新失败', data);
                alert(`学生${action}失败: ` + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            console.error(`${action}学生失败:`, error);
            alert(`${action}学生失败，请检查网络连接或服务器日志`);
        });
    }
}

/**
 * 搜索学生
 */
function searchStudents() {
    const searchText = document.getElementById('studentSearch').value.trim();
    console.log('搜索学生:', searchText);
    // 这里应该实现搜索学生的功能
}

/**
 * 筛选交易记录
 */
function filterTransactions() {
    const typeFilter = document.getElementById('transactionTypeFilter').value;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    
    // 构建查询参数
    let queryParams = [];
    if (typeFilter) {
        queryParams.push(`type=${encodeURIComponent(typeFilter)}`);
    }
    if (startDate) {
        queryParams.push(`startDate=${encodeURIComponent(startDate)}`);
    }
    if (endDate) {
        queryParams.push(`endDate=${encodeURIComponent(endDate)}`);
    }
    
    const queryString = queryParams.length ? `?${queryParams.join('&')}` : '';
    
    // 显示加载中状态
    document.getElementById('transactionsList').innerHTML = '<tr><td colspan="8" class="loading">加载中...</td></tr>';
    
    // 调用API获取筛选后的交易记录
    fetch(`/course-selection/api/admin/transactions${queryString}`, {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new Error(`非预期的响应格式：${contentType}`);
        }
        
        return response.json();
    })
    .then(data => {
        if (data.success && data.data && data.data.length > 0) {
            renderTransactionsList(data.data);
        } else {
            document.getElementById('transactionsList').innerHTML = '<tr><td colspan="8">没有符合条件的交易记录</td></tr>';
        }
    })
    .catch(error => {
        console.error('筛选交易记录失败:', error);
        document.getElementById('transactionsList').innerHTML = 
            `<tr><td colspan="8">加载失败: ${error.message || '网络错误'}</td></tr>`;
    });
}

/**
 * 搜索交易记录
 */
function searchTransactions() {
    const searchText = document.getElementById('transactionSearch').value.trim();
    if (!searchText) {
        alert('请输入学生ID');
        return;
    }
    
    // 显示加载中状态
    document.getElementById('transactionsList').innerHTML = '<tr><td colspan="8" class="loading">加载中...</td></tr>';
    
    // 调用API搜索交易记录
    fetch(`/course-selection/api/admin/transactions?studentId=${encodeURIComponent(searchText)}`, {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new Error(`非预期的响应格式：${contentType}`);
        }
        
        return response.json();
    })
    .then(data => {
        if (data.success && data.data && data.data.length > 0) {
            renderTransactionsList(data.data);
        } else {
            document.getElementById('transactionsList').innerHTML = '<tr><td colspan="8">没有找到该学生的交易记录</td></tr>';
        }
    })
    .catch(error => {
        console.error('搜索交易记录失败:', error);
        document.getElementById('transactionsList').innerHTML = 
            `<tr><td colspan="8">加载失败: ${error.message || '网络错误'}</td></tr>`;
    });
}

/**
 * 加载交易记录列表
 */
function loadTransactionsList() {
    document.getElementById('transactionsList').innerHTML = '<tr><td colspan="8" class="loading">加载中...</td></tr>';
    
    // 获取当前筛选条件
    const typeFilter = document.getElementById('transactionTypeFilter').value;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    
    // 构建查询参数
    let queryParams = [];
    if (typeFilter) {
        queryParams.push(`type=${encodeURIComponent(typeFilter)}`);
    }
    if (startDate) {
        queryParams.push(`startDate=${encodeURIComponent(startDate)}`);
    }
    if (endDate) {
        queryParams.push(`endDate=${encodeURIComponent(endDate)}`);
    }
    
    const queryString = queryParams.length ? `?${queryParams.join('&')}` : '';
    
    // 调用API获取交易记录列表
    fetch(`/course-selection/api/admin/transactions${queryString}`, {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new Error(`非预期的响应格式：${contentType}`);
        }
        
        return response.json();
    })
    .then(data => {
        if (data.success && data.data && data.data.length > 0) {
            renderTransactionsList(data.data);
        } else {
            document.getElementById('transactionsList').innerHTML = '<tr><td colspan="8">暂无交易记录</td></tr>';
        }
    })
    .catch(error => {
        console.error('加载交易记录列表失败:', error);
        document.getElementById('transactionsList').innerHTML = 
            `<tr><td colspan="8">加载失败: ${error.message || '网络错误'}</td></tr>`;
    });
}

/**
 * 渲染交易记录列表
 */
function renderTransactionsList(transactions) {
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
                <td>${transaction.relatedStudentId || '-'}</td>
                <td>${transaction.description || '-'}</td>
            </tr>
        `;
    });
    
    document.getElementById('transactionsList').innerHTML = html;
}

/**
 * 保存管理员个人信息
 */
function saveProfile() {
    const adminName = document.getElementById('currentAdminName').value.trim();
    const password = document.getElementById('currentAdminPassword').value.trim();
    const confirmPassword = document.getElementById('confirmPassword').value.trim();
    
    if (!adminName) {
        alert('姓名不能为空');
        return;
    }
    
    // 如果输入了密码，需要检查确认密码是否匹配
    if (password && password !== confirmPassword) {
        alert('两次输入的密码不一致');
        return;
    }
    
    // 构建更新数据
    const updateData = {
        name: adminName
    };
    
    // 如果有设置密码，则添加到更新数据中
    if (password) {
        updateData.password = password;
    }
    
    // 发送更新请求
    fetch(`/course-selection/api/admin/${window.currentAdmin.adminId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(updateData),
        credentials: 'same-origin'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert('个人信息更新成功');
            // 更新全局管理员对象
            if (window.currentAdmin) {
                window.currentAdmin.name = adminName;
                document.getElementById('adminName').textContent = adminName;
            }
            // 清空密码字段
            document.getElementById('currentAdminPassword').value = '';
            document.getElementById('confirmPassword').value = '';
        } else {
            alert('个人信息更新失败: ' + (data.message || '未知错误'));
        }
    })
    .catch(error => {
        console.error('更新个人信息失败:', error);
        alert('更新个人信息失败，请检查网络连接');
    });
}

/**
 * 显示添加管理员模态框
 */
function showAddAdminModal() {
    // 创建模态框
    let modalHtml = `
        <div class="modal" id="addAdminModal">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">添加管理员</h5>
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close" id="closeAddAdminModalBtn">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <form id="addAdminForm">
                            <div class="form-group">
                                <label for="newAdminId">管理员ID</label>
                                <input type="text" class="form-control" id="newAdminId" required>
                            </div>
                            <div class="form-group">
                                <label for="newAdminName">姓名</label>
                                <input type="text" class="form-control" id="newAdminName" required>
                            </div>
                            <div class="form-group">
                                <label for="newAdminPassword">密码</label>
                                <input type="password" class="form-control" id="newAdminPassword" required>
                            </div>
                            <div class="form-group">
                                <label for="newAdminRole">角色</label>
                                <select class="form-control" id="newAdminRole" required>
                                    <option value="">-- 请选择角色 --</option>
                                    <option value="SUPER_ADMIN">超级管理员</option>
                                    <option value="COURSE_ADMIN">课程管理员</option>
                                    <option value="STUDENT_ADMIN">学生管理员</option>
                                </select>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal" id="cancelAddAdminBtn">取消</button>
                        <button type="button" class="btn btn-primary" id="saveAddAdminBtn">保存</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    // 添加到页面
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    // 显示模态框
    const modal = document.getElementById('addAdminModal');
    modal.style.display = 'block';
    modal.style.backgroundColor = 'rgba(0,0,0,0.5)';
    modal.style.position = 'fixed';
    modal.style.zIndex = '1000';
    modal.style.top = '0';
    modal.style.left = '0';
    modal.style.width = '100%';
    modal.style.height = '100%';
    modal.style.overflow = 'auto';
    modal.style.paddingTop = '50px';

    // 设置模态框内容样式
    const modalDialog = modal.querySelector('.modal-dialog');
    modalDialog.style.margin = '10px auto';
    modalDialog.style.maxWidth = '500px';
    modalDialog.style.backgroundColor = '#fff';
    modalDialog.style.borderRadius = '5px';
    modalDialog.style.overflow = 'hidden';

    // 事件监听
    document.getElementById('closeAddAdminModalBtn').addEventListener('click', closeAddAdminModal);
    document.getElementById('cancelAddAdminBtn').addEventListener('click', closeAddAdminModal);
    document.getElementById('saveAddAdminBtn').addEventListener('click', saveAddAdmin);
}

/**
 * 关闭添加管理员模态框
 */
function closeAddAdminModal() {
    const modal = document.getElementById('addAdminModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 保存添加的管理员
 */
function saveAddAdmin() {
    const adminId = document.getElementById('newAdminId').value.trim();
    const name = document.getElementById('newAdminName').value.trim();
    const password = document.getElementById('newAdminPassword').value.trim();
    const role = document.getElementById('newAdminRole').value.trim();

    if (!adminId || !name || !password || !role) {
        alert('请填写所有必填字段');
        return;
    }

    const newAdmin = {
        adminId: adminId,
        name: name,
        password: password,
        role: role
    };

    fetch('/course-selection/api/admin', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(newAdmin),
        credentials: 'same-origin'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert('管理员添加成功');
            closeAddAdminModal();
            loadAdminsList(); // 重新加载管理员列表
        } else {
            alert('管理员添加失败: ' + (data.message || '未知错误'));
        }
    })
    .catch(error => {
        console.error('添加管理员失败:', error);
        alert('添加管理员失败，请检查网络连接');
    });
}

/**
 * 加载管理员列表
 */
function loadAdminsList() {
    document.getElementById('adminsList').innerHTML = '<tr><td colspan="6" class="loading">加载中...</td></tr>';
    
    fetch('/course-selection/api/admin/all', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => {
        // 检查响应状态
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        // 检查内容类型是否为JSON
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new Error(`非预期的响应格式：${contentType}，请检查API实现`);
        }
        
        return response.json();
    })
    .then(data => {
        if (data.success && data.data && data.data.length > 0) {
            let html = '';
            
            data.data.forEach(admin => {
                html += `
                    <tr>
                        <td>${admin.adminId}</td>
                        <td>${admin.name}</td>
                        <td>${getRoleName(admin.role)}</td>
                        <td>${admin.createdAt ? new Date(admin.createdAt).toLocaleString() : '-'}</td>
                        <td>${admin.lastLogin ? new Date(admin.lastLogin).toLocaleString() : '-'}</td>
                        <td>
                            <button class="btn btn-secondary btn-sm edit-admin-btn" data-id="${admin.adminId}">
                                <i class="bi bi-pencil"></i>
                            </button>
                            ${admin.adminId !== window.currentAdmin.adminId ? 
                                `<button class="btn btn-danger btn-sm delete-admin-btn" data-id="${admin.adminId}">
                                    <i class="bi bi-trash"></i>
                                </button>` : ''}
                        </td>
                    </tr>
                `;
            });
            
            document.getElementById('adminsList').innerHTML = html;
            
            // 添加事件监听
            document.querySelectorAll('.edit-admin-btn').forEach(btn => {
                btn.addEventListener('click', function() {
                    const adminId = this.getAttribute('data-id');
                    editAdmin(adminId);
                });
            });
            
            document.querySelectorAll('.delete-admin-btn').forEach(btn => {
                btn.addEventListener('click', function() {
                    const adminId = this.getAttribute('data-id');
                    deleteAdmin(adminId);
                });
            });
        } else {
            document.getElementById('adminsList').innerHTML = '<tr><td colspan="6">暂无管理员数据</td></tr>';
        }
    })
    .catch(error => {
        console.error('加载管理员列表失败:', error);
        document.getElementById('adminsList').innerHTML = `<tr><td colspan="6">加载失败: ${error.message}</td></tr>`;
    });
}

/**
 * 编辑管理员
 */
function editAdmin(adminId) {
    alert(`编辑管理员: ${adminId} 功能尚未实现`);
}

/**
 * 删除管理员
 */
function deleteAdmin(adminId) {
    if (confirm(`确定要删除管理员 ${adminId} 吗？此操作不可恢复！`)) {
        fetch(`/course-selection/api/admin/${adminId}`, {
            method: 'DELETE',
            credentials: 'same-origin'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('管理员删除成功');
                loadAdminsList(); // 重新加载管理员列表
            } else {
                alert('管理员删除失败: ' + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            console.error('删除管理员失败:', error);
            alert('删除管理员失败，请检查网络连接');
        });
    }
}

// 其他函数将根据需要实现
// 包括加载各种列表的函数、显示模态框的函数、处理表单提交的函数等 