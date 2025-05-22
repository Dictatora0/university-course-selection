document.addEventListener('DOMContentLoaded', function() {
    console.log("学生仪表盘初始化开始...");
    
    // 添加API对象检测
    if (typeof API === 'undefined') {
        console.error("API对象未定义! 请检查api.js是否正确加载");
        
        // 显示全局错误信息
        const errorDiv = document.createElement('div');
        errorDiv.className = 'alert alert-danger m-3';
        errorDiv.innerHTML = `
            <h4>加载错误</h4>
            <p>API接口未初始化，无法加载数据。可能的原因：</p>
            <ul>
                <li>API脚本(api.js)未正确加载</li>
                <li>网络连接问题</li>
                <li>后端服务未启动</li>
            </ul>
            <p>请尝试刷新页面，或检查控制台错误信息。</p>
            <button class="btn btn-primary" onclick="location.reload()">刷新页面</button>
        `;
        
        document.body.insertBefore(errorDiv, document.body.firstChild);
        return;
    } else {
        console.log("API对象已定义:", API);
        console.log("API baseUrl:", API.baseUrl);
        console.log("API可用方法:", Object.keys(API).join(', '));
    }
    
    // 初始化 - 首先尝试从localStorage获取用户信息
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const userType = localStorage.getItem('userType');
    
    console.log("从localStorage获取的用户信息:", user);
    console.log("从localStorage获取的用户类型:", userType);
    
    // 获取当前网站的协议、主机和端口，构建完整的绝对URL
    const protocol = window.location.protocol;
    const host = window.location.host;
    const baseUrl = protocol + '//' + host;
    
    // 获取当前应用的上下文路径
    const contextPath = '/course-selection'; // 硬编码确保正确
    
    // 构建登录页面的完整URL
    const loginPageUrl = baseUrl + contextPath + '/index.html';
    
    console.log('验证登录状态 - URL配置:');
    console.log('- protocol:', protocol);
    console.log('- host:', host);
    console.log('- baseUrl:', baseUrl);
    console.log('- contextPath:', contextPath);
    console.log('- 登录页面URL:', loginPageUrl);
    
    // 消息轮询相关变量
    let messagePollingInterval = null;
    let lastPollingTime = Date.now();
    
    // 添加检查session的逻辑，如果localStorage中没有用户信息，尝试从服务器获取
    if (!user.studentId) {
        console.log("本地存储无用户信息，尝试从API获取...");
        fetch('/course-selection/api/students/getInfo', {
            method: 'GET',
            credentials: 'include'
        })
        .then(response => {
            console.log("API响应状态:", response.status);
            if (!response.ok) {
                throw new Error('未登录');
            }
            return response.json();
        })
        .then(data => {
            console.log("API返回数据:", data);
            if (data.success) {
                // 如果API返回了用户信息，保存到localStorage
                const studentInfo = data.data;
                const userData = {
                    studentId: studentInfo.student_id,
                    name: studentInfo.name,
                    balance: studentInfo.balance
                };
                localStorage.setItem('user', JSON.stringify(userData));
                localStorage.setItem('userType', 'student');
                console.log("API获取用户信息成功，已保存到localStorage");
                
                // 初始化UI
                initUI();
                setupEventListeners();
                
                // 启动轮询新消息
                startMessagePolling();
                
                // 默认显示主页，并加载主页数据
                showTab('dashboardMain');
            } else {
                // 如果API请求成功但返回失败状态
                console.log("API请求成功但返回失败状态，跳转到登录页面:", loginPageUrl);
                window.location.href = loginPageUrl;
            }
        })
        .catch(err => {
            console.error("获取用户信息失败:", err);
            console.log("跳转到登录页面:", loginPageUrl);
            window.location.href = loginPageUrl;
        });
    } else {
        console.log("本地存储有用户信息，直接初始化UI");
        // 如果localStorage中有用户信息，继续初始化
        initUI();
        setupEventListeners();
        
        // 启动轮询新消息
        startMessagePolling();
        
        // 默认显示主页，并加载主页数据
        showTab('dashboardMain');
    }
    
    // 全局可访问的函数，主要用于HTML内联onclick事件
    window.openChatWindow = openChatWindow;
    window.openTransferModalFromButton = function(buttonElement) {
        const friendId = buttonElement.dataset.friendId;
        const friendName = buttonElement.dataset.friendName;
        openTransferModal(friendId, friendName);
    };
    window.deleteFriend = deleteFriend;
    window.acceptFriendRequest = acceptFriendRequest;
    window.rejectFriendRequest = rejectFriendRequest;
    window.cancelFriendRequest = cancelFriendRequest;
    window.addFriendById = addFriendById;

    function initUI() {
        document.getElementById('studentName').textContent = user.name || '学生';
        document.getElementById('welcomeName').textContent = user.name || '同学';
        const studentDeptEl = document.getElementById('studentDept');
        if(studentDeptEl) studentDeptEl.textContent = user.deptName ? `(${user.deptName})` : '';
        
        const today = new Date();
        const options = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' };
        const currentDateEl = document.getElementById('currentDate');
        if(currentDateEl) currentDateEl.textContent = today.toLocaleDateString('zh-CN', options);

        // 直接调用关键数据加载函数
        setTimeout(() => {
            console.log("直接初始化加载所有关键数据...");
            try {
                // 加载课程数据
                loadAllCourses();
                loadEnrolledCourses();
                loadDepartmentsForFilter();
                
                // 加载钱包数据
                loadBalance();
                loadTransactions();
                
                // 加载好友数据
                loadFriends();
                loadFriendRecommendations();
                
                // 加载个人信息
                loadProfileInfo();
                
                // 加载消息数据
                loadRecentContacts();
                
                console.log("初始化数据加载完成");
                window.showToast("欢迎使用在线选课系统", "success");
            } catch (error) {
                console.error("初始化加载数据时出错:", error);
                window.showToast("初始化数据加载失败，请尝试刷新页面或检查网络连接", "danger");
            }
        }, 500); // 延迟500毫秒以确保DOM完全加载
    }

    function setupEventListeners() {
        // 主导航选项卡点击事件
        document.querySelectorAll('.main-nav-link').forEach(function(navLink) {
            navLink.addEventListener('click', function(e) {
                e.preventDefault();
                const targetTabId = this.getAttribute('data-tab');
                console.log(`点击导航链接，目标Tab: ${targetTabId}`);
                if(targetTabId) {
                    showTab(targetTabId);
                }
            });
        });
        
        // 快捷操作按钮点击事件 (data-tab-target)
        document.querySelectorAll('[data-tab-target]').forEach(function(button) {
            button.addEventListener('click', function(e) {
                e.preventDefault();
                const targetTabId = this.getAttribute('data-tab-target');
                console.log(`点击快捷按钮，目标Tab: ${targetTabId}`);
                if(targetTabId) {
                    showTab(targetTabId);
                }
            });
        });
        
        // 个人信息页面的选项卡切换事件
        document.querySelectorAll('#profileTabs .nav-link').forEach(function(tabLink) {
            tabLink.addEventListener('click', function(e) {
                e.preventDefault();
                // 移除所有选项卡的active类
                document.querySelectorAll('#profileTabs .nav-link').forEach(link => link.classList.remove('active'));
                // 当前选项卡添加active类
                this.classList.add('active');
                
                // 获取目标内容ID
                const targetId = this.getAttribute('href');
                // 隐藏所有内容
                document.querySelectorAll('.tab-pane').forEach(pane => {
                    pane.classList.remove('show', 'active');
                });
                // 显示目标内容
                document.querySelector(targetId).classList.add('show', 'active');
            });
        });
        
        // 直接为编辑个人信息按钮添加事件监听器
        const editProfileBtn = document.getElementById('editProfileBtn');
        if (editProfileBtn) {
            // 清除之前的事件监听器
            const cloneBtn = editProfileBtn.cloneNode(true);
            editProfileBtn.parentNode.replaceChild(cloneBtn, editProfileBtn);
            
            cloneBtn.addEventListener('click', function() {
                console.log("编辑个人信息按钮被点击");
                const profileInfoView = document.getElementById('profileInfoView');
                const profileInfoEdit = document.getElementById('profileInfoEdit');
                
                if(profileInfoView && profileInfoEdit) {
                    profileInfoView.style.display = 'none';
                    profileInfoEdit.style.display = 'block';
                    
                    // 尝试初始化表单数据
                    try {
                        const userData = JSON.parse(localStorage.getItem('user') || '{}');
                        const editNameInput = document.getElementById('editName');
                        if(editNameInput) {
                            editNameInput.value = userData.name || '';
                        }
                        
                        // 其他表单字段会在loadProfileInfo函数中处理
                    } catch (error) {
                        console.error("初始化编辑表单失败:", error);
                    }
                } else {
                    console.error("找不到个人信息视图或编辑表单元素");
                    window.showToast("找不到个人信息编辑表单，请刷新页面重试", "danger");
                }
            });
        }
        
        // 直接为取消编辑按钮添加事件监听器
        const cancelEditBtn = document.getElementById('cancelEditBtn');
        if (cancelEditBtn) {
            // 清除之前的事件监听器
            const cloneBtn = cancelEditBtn.cloneNode(true);
            cancelEditBtn.parentNode.replaceChild(cloneBtn, cancelEditBtn);
            
            cloneBtn.addEventListener('click', function() {
                console.log("取消编辑按钮被点击");
                const profileInfoView = document.getElementById('profileInfoView');
                const profileInfoEdit = document.getElementById('profileInfoEdit');
                
                if(profileInfoView && profileInfoEdit) {
                    profileInfoEdit.style.display = 'none';
                    profileInfoView.style.display = 'block';
                } else {
                    console.error("找不到个人信息视图或编辑表单元素");
                }
            });
        }
        
        // 注销按钮点击事件
        document.getElementById('logoutBtn')?.addEventListener('click', function() {
            logout();
        });
        
        // 查看消息按钮点击事件
        document.getElementById('viewMessagesBtn')?.addEventListener('click', function() {
            showTab('messages');
            loadRecentContacts();
        });
        
        // 课程搜索
        document.getElementById('courseSearchBtn')?.addEventListener('click', function() {
            const searchTerm = document.getElementById('courseSearchInput').value.toLowerCase();
            const selectedDept = document.getElementById('deptFilter').value;
            filterAndRenderAvailableCourses(searchTerm, selectedDept);
        });
        
        // 充值表单相关事件
        const depositBtn = document.getElementById('depositBtn');
        
        if (depositBtn) {
            // 清除之前的事件监听器
            const cloneBtn = depositBtn.cloneNode(true);
            depositBtn.parentNode.replaceChild(cloneBtn, depositBtn);
            
            cloneBtn.addEventListener('click', function() {
                console.log("充值按钮被点击");
                // 直接操作DOM元素显示/隐藏
                const depositForm = document.getElementById('depositForm');
                const withdrawForm = document.getElementById('withdrawForm');
                
                if (depositForm) depositForm.style.display = 'block';
                if (withdrawForm) withdrawForm.style.display = 'none';
                
                console.log("充值表单已显示，提现表单已隐藏");
            });
        }
        
        // 提现表单相关事件
        const withdrawBtn = document.getElementById('withdrawBtn');
        
        if (withdrawBtn) {
            // 清除之前的事件监听器
            const cloneBtn = withdrawBtn.cloneNode(true);
            withdrawBtn.parentNode.replaceChild(cloneBtn, withdrawBtn);
            
            cloneBtn.addEventListener('click', function() {
                console.log("提现按钮被点击");
                // 直接操作DOM元素显示/隐藏
                const withdrawForm = document.getElementById('withdrawForm');
                const depositForm = document.getElementById('depositForm');
                
                if (withdrawForm) withdrawForm.style.display = 'block';
                if (depositForm) depositForm.style.display = 'none';
                
                console.log("提现表单已显示，充值表单已隐藏");
            });
        }
        
        // 确认充值按钮
        const confirmDepositBtn = document.getElementById('confirmDepositBtn');
        if (confirmDepositBtn) {
            confirmDepositBtn.addEventListener('click', function() {
                console.log("确认充值按钮被点击");
                handleDeposit();
            });
        }
        
        // 确认提现按钮
        const confirmWithdrawBtn = document.getElementById('confirmWithdrawBtn');
        if (confirmWithdrawBtn) {
            confirmWithdrawBtn.addEventListener('click', function() {
                console.log("确认提现按钮被点击");
                handleWithdraw();
            });
        }
        
        // 取消充值按钮
        const cancelDepositBtn = document.getElementById('cancelDepositBtn');
        if (cancelDepositBtn) {
            cancelDepositBtn.addEventListener('click', function() {
                console.log("取消充值按钮被点击");
                const depositForm = document.getElementById('depositForm');
                if (depositForm) depositForm.style.display = 'none';
            });
        }
        
        // 取消提现按钮
        const cancelWithdrawBtn = document.getElementById('cancelWithdrawBtn');
        if (cancelWithdrawBtn) {
            cancelWithdrawBtn.addEventListener('click', function() {
                console.log("取消提现按钮被点击");
                const withdrawForm = document.getElementById('withdrawForm');
                if (withdrawForm) withdrawForm.style.display = 'none';
            });
        }
        
        // 好友添加相关事件
        document.getElementById('addFriendForm')?.addEventListener('submit', function(e) {
            e.preventDefault();
            handleAddFriend(e);
        });
        
        // 好友搜索相关事件
        document.getElementById('searchStudentsForm')?.addEventListener('submit', function(e) {
            e.preventDefault();
            handleSearchStudents();
        });
        
        // 好友过滤器
        document.getElementById('friendFilterInput')?.addEventListener('input', filterFriendList);
        
        // 主聊天窗口发送消息
        document.getElementById('mainChatMessageForm')?.addEventListener('submit', handleMainSendMessage);
        
        // 浮动聊天窗口相关事件
        document.getElementById('chatWindowMessageForm')?.addEventListener('submit', handleChatWindowSendMessage);
        document.getElementById('closeChatWindowBtn')?.addEventListener('click', function() {
            document.getElementById('chatWindow').style.display = 'none';
        });
        
        // 主聊天转账按钮
        document.getElementById('mainChatTransferBtn')?.addEventListener('click', handleMainChatTransfer);
        
        // 浮动聊天窗口转账按钮
        document.getElementById('chatWindowTransferBtn')?.addEventListener('click', handleChatWindowTransfer);
    }

    function showTab(tabId) {
        console.log(`显示Tab: ${tabId}`);
        
        // 检查Tab元素是否存在
        let tabElement = document.getElementById(tabId);
        if (!tabElement) {
            console.warn(`找不到ID为 ${tabId} 的Tab元素，尝试映射或查找替代元素...`);
            
            // 尝试查找替代元素
            if (tabId === 'myCourses' && document.getElementById('my-courses')) {
                console.log("映射 myCourses -> my-courses");
                tabId = 'my-courses';
                tabElement = document.getElementById('my-courses');
            } else if (tabId === 'wallet' && document.getElementById('payment')) {
                console.log("映射 wallet -> payment");
                tabId = 'payment';
                tabElement = document.getElementById('payment');
            } else {
                console.error(`未找到ID为 ${tabId} 的Tab元素，无法显示`);
                return;
            }
        }
        
        // 隐藏所有标签页
        document.querySelectorAll('.tab-content').forEach(tab => {
            tab.classList.remove('active');
        });
        
        // 显示选中的标签页
        tabElement.classList.add('active');
        
        // 根据选中的标签页执行特定的加载逻辑
        if (tabId === 'courses') {
            loadAllCourses();
            loadEnrolledCourses();
            loadDepartmentsForFilter();
        } else if (tabId === 'friends') {
            loadFriends();
            loadFriendRecommendations();
        } else if (tabId === 'messages') {
            // 加载消息相关内容
            loadRecentContacts();
            
            // 默认显示一条提示信息
            const chatContainer = document.getElementById('mainChatContainer');
            if (chatContainer) {
                chatContainer.innerHTML = `
                    <div class="d-flex flex-column justify-content-center align-items-center h-100 text-muted">
                        <i class="bi bi-chat-dots fs-1 mb-3"></i>
                        <p>选择一个联系人开始聊天</p>
                        <p class="small">或前往好友列表添加好友</p>
                    </div>
                `;
            }
            
            // 启动消息轮询
            startMessagePolling();
        } else if (tabId === 'payment') {
            // 清空之前的数据，显示加载中状态
            const balanceEl = document.getElementById('balanceAmount');
            const transactionListEl = document.getElementById('transactionList');
            
            if (balanceEl) balanceEl.textContent = '加载中...';
            if (transactionListEl) transactionListEl.innerHTML = '<li class="list-group-item text-center">加载中...</li>';
            
            // 加载钱包数据
            loadBalance();
            loadTransactions();
            
            // 确保钱包相关表单被隐藏
            console.log("确保充值和提现表单被隐藏");
            try {
                // 使用延时确保DOM元素已加载
                setTimeout(() => {
                    const depositForm = document.getElementById('depositForm');
                    const withdrawForm = document.getElementById('withdrawForm');
                    
                    if (depositForm) depositForm.style.display = 'none';
                    if (withdrawForm) withdrawForm.style.display = 'none';
                    
                    console.log("充值表单状态:", depositForm ? depositForm.style.display : "元素不存在");
                    console.log("提现表单状态:", withdrawForm ? withdrawForm.style.display : "元素不存在");
                }, 100);
            } catch (error) {
                console.error("隐藏钱包表单时出错:", error);
            }
        } else if (tabId === 'profile') {
            // 先清空个人信息数据，显示加载中状态
            const profileElements = [
                'profileStudentId', 'profileName', 'profileDeptName', 'profileBirthDate',
                'profileIdCard', 'profileAddress', 'profileEmail', 'profilePhone',
                'profileBalance', 'profileNameLarge', 'profileDeptNameLarge'
            ];
            
            profileElements.forEach(id => {
                const element = document.getElementById(id);
                if(element) element.textContent = '加载中...';
            });
            
            // 加载个人信息数据
            loadProfileInfo();
            
            // 确保基本信息选项卡是激活的
            const basicInfoTab = document.querySelector('#profileTabs a[href="#basicInfo"]');
            const accountSettingsTab = document.querySelector('#profileTabs a[href="#accountSettings"]');
            const basicInfoPane = document.getElementById('basicInfo');
            const accountSettingsPane = document.getElementById('accountSettings');
            
            if (basicInfoTab && accountSettingsTab && basicInfoPane && accountSettingsPane) {
                basicInfoTab.classList.add('active');
                accountSettingsTab.classList.remove('active');
                basicInfoPane.classList.add('show', 'active');
                accountSettingsPane.classList.remove('show', 'active');
                
                console.log("个人信息标签页已激活，基本信息标签页已选中");
            } else {
                console.error("无法找到个人信息相关元素：", {
                    basicInfoTab,
                    accountSettingsTab,
                    basicInfoPane,
                    accountSettingsPane
                });
            }
            
            // 确保查看模式而非编辑模式
            const profileInfoView = document.getElementById('profileInfoView');
            const profileInfoEdit = document.getElementById('profileInfoEdit');
            if (profileInfoView && profileInfoEdit) {
                profileInfoView.style.display = 'block';
                profileInfoEdit.style.display = 'none';
            }
        } else if (tabId === 'my-courses') {
            loadEnrolledCourses();
        }
        
        // 更新导航链接的 active 状态
        document.querySelectorAll('.sidebar .nav-link').forEach(link => {
            const linkTabId = link.getAttribute('data-tab');
            if (linkTabId === tabId) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
        
        // 保存当前选中的标签页到 localStorage
        localStorage.setItem('currentTab', tabId);
    }
    
    async function loadMainPageData() {
        try {
            const studentInfo = await API.student.getInfo();
            const balanceDisplay = document.getElementById('mainBalanceAmount');
            if (balanceDisplay) {
                balanceDisplay.textContent = `¥ ${parseFloat(studentInfo.balance || 0).toFixed(2)}`;
            }
        } catch (error) {
            console.error('获取主页余额失败:', error);
            const balanceDisplay = document.getElementById('mainBalanceAmount');
            if(balanceDisplay) balanceDisplay.textContent = '获取失败';
        }
    }

    async function logout() {
        try {
            // 停止消息轮询
            stopMessagePolling();
            
            await API.student.logout();
        localStorage.removeItem('user');
        localStorage.removeItem('userType');
        
            // 使用相同的登录页URL
            console.log("退出登录，跳转到:", loginPageUrl);
            window.location.href = loginPageUrl;
        } catch (error) {
            window.showToast('退出登录失败: ' + error.message, 'danger');
        }
    }

    // 停止轮询新消息
    function stopMessagePolling() {
        if (messagePollingInterval) {
            clearInterval(messagePollingInterval);
            messagePollingInterval = null;
            console.log("已停止消息轮询");
        }
    }
    
    // 检查新消息
    async function checkNewMessages() {
        try {
            console.log("检查新消息...");
            
            // 如果当前正在聊天，更新聊天窗口
            if (currentChatFriendId) {
                if (document.getElementById('messages').classList.contains('active')) {
                    await loadAndDisplayMessages(currentChatFriendId, 'mainChatMessagesContainer', false);
                }
                
                if (document.getElementById('chatWindow')?.style.display === 'flex') {
                    await loadAndDisplayMessages(currentChatFriendId, 'chatWindowMessages', false);
                }
            }
            
            // 如果消息页面处于活动状态，更新联系人列表
            if (document.getElementById('messages').classList.contains('active')) {
                await loadRecentContacts();
            }
            
            lastPollingTime = Date.now();
        } catch (error) {
            console.error("检查新消息失败:", error);
        }
    }
    
    // 启动轮询新消息
    function startMessagePolling() {
        // 先停止现有的轮询（如果有）
        stopMessagePolling();
        
        // 设置新的轮询间隔
        messagePollingInterval = setInterval(async () => {
            // 仅当用户未进行其他操作时才进行轮询
            const currentTime = Date.now();
            if (currentTime - lastPollingTime > 10000) { // 每10秒检查一次新消息
                await checkNewMessages();
            }
        }, 10000); // 每10秒检查一次是否需要轮询
        
        console.log("已启动消息轮询");
    }

    // 检查用户登录状态
    async function checkLoginStatus() {
        try {
            console.log("检查用户登录状态...");
            const response = await fetch('/course-selection/api/students/getInfo', {
                method: 'GET',
                credentials: 'include'
            });
            
            console.log("登录状态检查响应:", response.status);
            
            if (!response.ok) {
                return false;
            }
            
            const data = await response.json();
            console.log("登录状态检查结果:", data);
            
            if (!data.success) {
                return false;
            }
            
            // 更新本地存储的用户信息
            const studentInfo = data.data;
            const userData = {
                studentId: studentInfo.student_id,
                name: studentInfo.name,
                balance: studentInfo.balance,
                deptId: studentInfo.deptId,
                deptName: studentInfo.deptName
            };
            localStorage.setItem('user', JSON.stringify(userData));
            
            return true;
        } catch (error) {
            console.error("检查登录状态失败:", error);
            return false;
        }
    }

    // 加载最近联系人
    async function loadRecentContacts() {
        console.log("开始加载最近联系人列表");
        const listElement = document.getElementById('recentContactsList');
        if(!listElement) return;
        listElement.innerHTML = '<p class="list-group-item text-center">更新中...</p>'; // Loading state
        
        // 先检查登录状态
        const isLoggedIn = await checkLoginStatus();
        if (!isLoggedIn) {
            console.error("用户未登录，无法加载最近联系人");
            listElement.innerHTML = '<p class="list-group-item text-danger text-center">未登录或会话已过期</p>';
            
            const buttonDiv = document.createElement('div');
            buttonDiv.className = 'text-center mt-3';
            buttonDiv.innerHTML = `
                <button id="reloginBtn" class="btn btn-sm btn-primary">
                    <i class="bi bi-box-arrow-in-right"></i> 重新登录
                </button>
            `;
            listElement.appendChild(buttonDiv);
            
            document.getElementById('reloginBtn')?.addEventListener('click', () => {
                window.location.href = loginPageUrl;
            });
            
            return;
        }
        
        try {
            console.log("尝试获取最近联系人列表...");
            
            // 获取当前用户ID，用于调试
            const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
            console.log("当前用户ID:", currentUser.studentId);
            
            // 发送API请求，添加时间戳防止缓存
            const timestamp = new Date().getTime();
            const response = await fetch(`${API.baseUrl}/message/recent_contacts?_=${timestamp}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Cache-Control': 'no-cache, no-store, must-revalidate',
                    'Pragma': 'no-cache',
                    'Expires': '0'
                },
                credentials: 'include'
            });
            
            console.log("API响应状态:", response.status);
            
            // 如果响应不成功，尝试读取响应内容以获取更详细的错误信息
            if (!response.ok) {
                let errorText = `HTTP错误 ${response.status}`;
                try {
                    // 尝试读取响应内容
                    const errorResponse = await response.text();
                    console.error("API错误响应内容:", errorResponse);
                    
                    // 尝试解析JSON
                    try {
                        const errorJson = JSON.parse(errorResponse);
                        if (errorJson.message) {
                            errorText = errorJson.message;
                        }
                    } catch (jsonError) {
                        // 如果不是JSON，直接使用文本
                        if (errorResponse && errorResponse.trim()) {
                            errorText = errorResponse.trim();
                        }
                    }
                } catch (textError) {
                    console.error("无法读取错误响应内容:", textError);
                }
                
                throw new Error(errorText);
            }
            
            // 解析响应数据
            const result = await response.json();
            console.log("API原始返回结果:", result);
            
            if (!result.success) {
                throw new Error(result.message || "获取联系人失败");
            }
            
            // 确保我们正确提取数据
            const contacts = result.data || [];
            console.log("获取到的联系人数据:", contacts);
            
            // 清空联系人列表
            listElement.innerHTML = '';
            
            // 如果没有联系人数据或联系人为空
            if (!contacts || contacts.length === 0) {
                // 检查是否当前正在聊天，如果是，则创建一个临时的联系人项
                if (currentChatFriendId && currentChatFriendName) {
                    console.log(`当前正在与 ${currentChatFriendName} (${currentChatFriendId}) 聊天，但最近联系人列表为空，添加临时联系人项`);
                    
                    // 创建临时联系人列表，只包含当前聊天的好友
                    const item = document.createElement('a');
                    item.href = '#';
                    item.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center active';
                    
                    const avatarLetter = (currentChatFriendName || 'C').charAt(0).toUpperCase();
                    const avatarBgColor = getRandomColor(currentChatFriendId || Math.random().toString());
        
                    item.innerHTML = `
                        <div class="d-flex align-items-center">
                            <div class="friend-avatar me-2" style="background-color: ${avatarBgColor}; width: 40px; height: 40px; font-size: 0.9rem;">${avatarLetter}</div>
                            <div class="flex-grow-1">
                                <strong class="d-block">${currentChatFriendName || '未知联系人'}</strong>
                                <small class="d-block text-muted text-truncate" style="max-width: 150px;">正在聊天中...</small>
                            </div>
                        </div>
                    `;
                    
                    item.onclick = (e) => {
                        e.preventDefault();
                        openMainChat(currentChatFriendId, currentChatFriendName);
                    };
                    
                    listElement.appendChild(item);
                    
                    // 添加提示信息
                    const infoElement = document.createElement('div');
                    infoElement.className = 'alert alert-info mt-3 small';
                    infoElement.innerHTML = `
                        <i class="bi bi-info-circle-fill me-2"></i>
                        这次会话结束后，此联系人将出现在您的最近联系人列表中。
                    `;
                    listElement.appendChild(infoElement);
                    
                    // 添加前往好友列表的按钮
                    const buttonDiv = document.createElement('div');
                    buttonDiv.className = 'text-center mt-3';
                    buttonDiv.innerHTML = `
                        <button id="goToFriendsBtn" class="btn btn-primary btn-sm">
                            <i class="bi bi-people"></i> 前往好友列表
                        </button>
                    `;
                    listElement.appendChild(buttonDiv);
                    
                    // 添加前往好友列表的按钮点击事件
                    document.getElementById('goToFriendsBtn')?.addEventListener('click', () => {
                        showTab('friends');
                    });
                    
                    return;
                }
                
                // 如果没有当前聊天，显示默认的空联系人提示
                listElement.innerHTML = `
                    <div class="text-center p-4">
                        <i class="bi bi-chat-square-text fs-1 text-muted mb-3"></i>
                        <p class="text-muted">你还没有任何聊天记录</p>
                        <p class="small text-muted mb-3">从好友列表中选择一位好友开始聊天吧</p>
                        <button id="goToFriendsBtn" class="btn btn-primary btn-sm">
                            <i class="bi bi-people"></i> 前往好友列表
                        </button>
                        <button id="generateTestContactsBtn" class="btn btn-outline-secondary btn-sm ms-2">
                            <i class="bi bi-plus-circle"></i> 生成测试数据
                        </button>
                    </div>
                `;
                
                // 添加前往好友列表的按钮点击事件
                document.getElementById('goToFriendsBtn')?.addEventListener('click', () => {
                    showTab('friends');
                });
                
                // 添加生成测试数据的功能
                document.getElementById('generateTestContactsBtn')?.addEventListener('click', () => {
                    generateTestContacts();
                });
                
                return;
            }
        
            // 渲染联系人列表
            contacts.forEach(contact => {
                // 规范化contact对象字段，处理后端返回的可能字段名差异
                const normalizedContact = {
                    studentId: contact.studentId || contact.student_id,
                    name: contact.name,
                    lastMessage: contact.lastMessage || contact.last_message,
                    lastMessageTime: contact.lastMessageTime || contact.last_message_time ? new Date(contact.lastMessageTime || contact.last_message_time) : null,
                    unreadCount: contact.unreadCount || contact.unread_count || 0
                };
                
                console.log("处理联系人:", normalizedContact);
                
                const item = document.createElement('a');
                item.href = '#';
                // 如果是当前聊天的联系人，添加active类
                const isActive = normalizedContact.studentId === currentChatFriendId;
                item.className = `list-group-item list-group-item-action d-flex justify-content-between align-items-center ${isActive ? 'active' : ''}`;
                
                const avatarLetter = (normalizedContact.name || 'C').charAt(0).toUpperCase();
                const avatarBgColor = getRandomColor(normalizedContact.studentId || Math.random().toString());
                
                // 格式化最后一条消息时间
                let timeDisplay = '';
                if (normalizedContact.lastMessageTime) {
                    const msgDate = normalizedContact.lastMessageTime;
                    const now = new Date();
                    const yesterday = new Date(now);
                    yesterday.setDate(yesterday.getDate() - 1);
                    
                    if (msgDate.toDateString() === now.toDateString()) {
                        // 如果是今天，则显示时间
                        timeDisplay = msgDate.toLocaleTimeString('zh-CN', {hour: '2-digit', minute:'2-digit'});
                    } else if (msgDate.toDateString() === yesterday.toDateString()) {
                        // 如果是昨天，显示"昨天"
                        timeDisplay = '昨天';
                    } else if (now.getFullYear() === msgDate.getFullYear()) {
                        // 同一年内，显示月/日
                        timeDisplay = msgDate.toLocaleDateString('zh-CN', {month: 'numeric', day: 'numeric'});
                    } else {
                        // 不同年，显示年/月/日
                        timeDisplay = msgDate.toLocaleDateString('zh-CN', {year: 'numeric', month: 'numeric', day: 'numeric'});
                    }
                }
                
                item.innerHTML = `
                    <div class="d-flex align-items-center">
                        <div class="friend-avatar me-2" style="background-color: ${avatarBgColor}; width: 40px; height: 40px; font-size: 0.9rem;">${avatarLetter}</div>
                        <div class="flex-grow-1">
                            <strong class="d-block">${normalizedContact.name || '未知联系人'}</strong>
                            <small class="d-block text-muted text-truncate" style="max-width: 150px;">${normalizedContact.lastMessage ? normalizedContact.lastMessage : '暂无消息'}</small>
                        </div>
                    </div>
                    <div class="d-flex flex-column align-items-end">
                        <small class="text-muted">${timeDisplay}</small>
                        ${normalizedContact.unreadCount > 0 ? `<span class="badge bg-danger rounded-pill mt-1">${normalizedContact.unreadCount}</span>` : ''}
                    </div>
                `;
                item.onclick = (e) => {
                    e.preventDefault();
                    openMainChat(normalizedContact.studentId, normalizedContact.name);
                };
                listElement.appendChild(item);
            });
        } catch (error) {
            console.error('加载最近联系人失败:', error);
            
            // 清空联系人列表
            listElement.innerHTML = '';
            
            // 处理API失败的情况，检查是否有当前聊天的联系人
            if (currentChatFriendId && currentChatFriendName) {
                console.log(`API失败，但当前正在与 ${currentChatFriendName} (${currentChatFriendId}) 聊天，添加临时联系人项`);
                
                // 创建临时联系人项
                const item = document.createElement('a');
                item.href = '#';
                item.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center active';
                
                const avatarLetter = (currentChatFriendName || 'C').charAt(0).toUpperCase();
                const avatarBgColor = getRandomColor(currentChatFriendId || Math.random().toString());

                item.innerHTML = `
                    <div class="d-flex align-items-center">
                        <div class="friend-avatar me-2" style="background-color: ${avatarBgColor}; width: 40px; height: 40px; font-size: 0.9rem;">${avatarLetter}</div>
                        <div class="flex-grow-1">
                            <strong class="d-block">${currentChatFriendName || '未知联系人'}</strong>
                            <small class="d-block text-muted text-truncate" style="max-width: 150px;">正在聊天中...</small>
                        </div>
                    </div>
                `;
                
                item.onclick = (e) => {
                    e.preventDefault();
                    openMainChat(currentChatFriendId, currentChatFriendName);
                };
                
                listElement.appendChild(item);
                
                // 添加错误信息
                const errorElement = document.createElement('div');
                errorElement.className = 'alert alert-danger mt-3 small';
                errorElement.innerHTML = `
                    <i class="bi bi-exclamation-triangle-fill me-2"></i>
                    加载联系人失败: ${error.message || '未知错误'}
                `;
                listElement.appendChild(errorElement);
            } else {
                // 如果没有当前聊天，显示错误信息
                listElement.innerHTML = '<p class="list-group-item text-danger text-center">加载联系人失败: ' + (error.message || '未知错误') + '</p>';
            }
            
            // 添加重试按钮和生成测试数据按钮
            const buttonDiv = document.createElement('div');
            buttonDiv.className = 'text-center mt-3';
            buttonDiv.innerHTML = `
                <button id="retryLoadContactsBtn" class="btn btn-sm btn-primary me-2">
                    <i class="bi bi-arrow-clockwise"></i> 重试
                </button>
                <button id="generateTestContactsBtn" class="btn btn-sm btn-outline-secondary">
                    <i class="bi bi-plus-circle"></i> 生成测试联系人
                </button>
                <button id="goToFriendsBtn" class="btn btn-sm btn-outline-primary ms-2">
                    <i class="bi bi-people"></i> 前往好友列表
                </button>
            `;
            listElement.appendChild(buttonDiv);
            
            // 添加重试功能
            document.getElementById('retryLoadContactsBtn')?.addEventListener('click', () => {
                loadRecentContacts();
            });
            
            // 添加生成测试数据的功能
            document.getElementById('generateTestContactsBtn')?.addEventListener('click', () => {
                generateTestContacts();
            });
            
            // 添加前往好友列表的按钮点击事件
            document.getElementById('goToFriendsBtn')?.addEventListener('click', () => {
                showTab('friends');
            });
        }
    }

    // 生成测试联系人数据
    async function generateTestContacts() {
        try {
            console.log("生成测试联系人数据...");
            window.showToast("正在生成测试数据...", "info");
            
            // 当前用户信息
            const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
            if (!currentUser.studentId) {
                throw new Error("用户未登录");
            }
            
            // 获取所有好友
            const friendsData = await API.friendship.list();
            console.log("好友列表:", friendsData);
            
            // 规范化好友数据
            const friends = friendsData.filter(f => f.status === 'ACCEPTED').map(friend => ({
                studentId: friend.student_id || friend.studentId,
                name: friend.name
            }));
            
            if (friends.length === 0) {
                // 如果没有好友，创建一些测试好友数据
                const testFriends = [
                    { name: "测试好友1", studentId: "T0001" },
                    { name: "测试好友2", studentId: "T0002" },
                    { name: "测试好友3", studentId: "T0003" }
                ];
                
                // 模拟创建测试消息数据
                window.showToast("您没有好友，将创建测试数据（仅供展示用）", "warning");
                
                // 创建虚拟的联系人列表HTML
                const listElement = document.getElementById('recentContactsList');
                if (listElement) {
                    listElement.innerHTML = '';
                    
                    testFriends.forEach(friend => {
                        // 创建测试消息项
                        const testMessages = [
                            "你好！这是一条测试消息。",
                            "请问你最近怎么样？",
                            "周末有空一起去看电影吗？"
                        ];
                        const randomMessage = testMessages[Math.floor(Math.random() * testMessages.length)];
                        const randomUnread = Math.random() > 0.5 ? Math.floor(Math.random() * 5) : 0;
                        
                        const item = document.createElement('a');
                        item.href = '#';
                        item.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center';
                        const avatarLetter = friend.name.charAt(0).toUpperCase();
                        const avatarBgColor = getRandomColor(friend.studentId);
                        
                        item.innerHTML = `
                            <div class="d-flex align-items-center">
                                <div class="friend-avatar me-2" style="background-color: ${avatarBgColor}; width: 40px; height: 40px; font-size: 0.9rem;">${avatarLetter}</div>
                                <div class="flex-grow-1">
                                    <strong class="d-block">${friend.name} <small class="text-muted">(测试数据)</small></strong>
                                    <small class="d-block text-muted text-truncate" style="max-width: 150px;">${randomMessage}</small>
                                </div>
                            </div>
                            ${randomUnread > 0 ? `<span class="badge bg-danger rounded-pill">${randomUnread}</span>` : ''}
                        `;
                        
                        item.onclick = (e) => {
                            e.preventDefault();
                            window.showToast("这是测试数据，无法查看对话", "info");
                        };
                        
                        listElement.appendChild(item);
                    });
                    
                    // 添加提示信息
                    const infoElement = document.createElement('div');
                    infoElement.className = 'alert alert-info mt-3 small';
                    infoElement.innerHTML = `
                        <i class="bi bi-info-circle-fill me-2"></i>
                        这是模拟的测试数据，仅用于演示界面。请先添加好友并发送消息，才能看到真实的聊天记录。
                    `;
                    listElement.appendChild(infoElement);
                }
                
            return;
        }
        
            // 为每个好友发送一条测试消息
            for (const friend of friends) {
                const message = `这是一条测试消息，发送时间：${new Date().toLocaleString()}`;
                console.log(`向好友 ${friend.name}(${friend.studentId}) 发送测试消息: ${message}`);
                
                await API.message.send(friend.studentId, message);
            }
            
            window.showToast("测试数据生成成功，已向所有好友发送测试消息", "success");
            
            // 重新加载联系人列表
            await loadRecentContacts();
            
        } catch (error) {
            console.error("生成测试数据失败:", error);
            window.showToast(`生成测试数据失败: ${error.message}`, "danger");
        }
    }

    // 模拟最近联系人数据（仅在API调用失败时使用）
    function mockLoadRecentContacts() { 
        const contacts = [
            { studentId: '2024001', name: '张三', lastMessage: '你好啊！最近怎么样?', unreadCount: 2 },
            { studentId: '2024002', name: '李四', lastMessage: '晚上有空一起吃饭吗？', unreadCount: 0 },
            { studentId: '2024003', name: '王五', lastMessage: 'OK, 没问题', unreadCount: 5 },
        ];
        
        // 添加直接调试按钮
        const debugButton = document.createElement('div');
        debugButton.className = 'text-center mt-3';
        debugButton.innerHTML = `
            <button id="debugContactsBtn" class="btn btn-sm btn-warning">
                <i class="bi bi-bug"></i> 直接测试数据库
                    </button>
        `;
        
        const listElement = document.getElementById('recentContactsList');
        if(!listElement) return;
        listElement.innerHTML = '';
        contacts.forEach(contact => {
            const item = document.createElement('a');
            item.href = '#';
            item.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center';
            const avatarLetter = (contact.name || 'C').charAt(0).toUpperCase();
            const avatarBgColor = getRandomColor(contact.studentId || Math.random().toString());
            item.innerHTML = `
                <div class="d-flex align-items-center">
                    <div class="friend-avatar me-2" style="background-color: ${avatarBgColor}; width: 40px; height: 40px; font-size: 0.9rem;">${avatarLetter}</div>
                    <div class="flex-grow-1">
                        <strong class="d-block">${contact.name}</strong>
                        <small class="d-block text-muted text-truncate" style="max-width: 150px;">${contact.lastMessage ? contact.lastMessage : '暂无消息'}</small>
                </div>
                </div>
                ${contact.unreadCount > 0 ? `<span class="badge bg-danger rounded-pill">${contact.unreadCount}</span>` : ''}
            `;
            item.onclick = (e) => {
                e.preventDefault();
                openMainChat(contact.studentId, contact.name);
            };
            listElement.appendChild(item);
        });
        if(contacts.length === 0){
             listElement.innerHTML = '<p class="list-group-item text-muted text-center">没有最近联系人</p>';
        }
        
        // 添加调试按钮
        listElement.appendChild(debugButton);
        
        // 添加事件监听器
        document.getElementById('debugContactsBtn')?.addEventListener('click', () => {
            debugContactsDatabase();
        });
    }
    
    // 直接测试数据库联系人
    function debugContactsDatabase() {
        const debugInfo = document.createElement('div');
        debugInfo.className = 'mt-3 p-3 bg-light';
        debugInfo.innerHTML = `
            <h5>数据库调试信息</h5>
            <p>请打开控制台，查看以下信息并提供给开发者：</p>
            <ol class="text-start">
                <li>确认您已经登录，并且有效的会话Cookie</li>
                <li>确认您的账号有好友关系</li>
                <li>检查后端服务是否正常运行</li>
                <li>检查前端JS console是否有错误</li>
                <li>检查网络请求是否正确发送和接收</li>
            </ol>
            <p>您可以尝试手动发送消息，然后尝试再次加载联系人列表</p>
        `;
        
        // 显示调试信息
        const listElement = document.getElementById('recentContactsList');
        if (listElement) {
            listElement.innerHTML = '';
            listElement.appendChild(debugInfo);
            
            // 添加重新登录按钮
            const buttonDiv = document.createElement('div');
            buttonDiv.className = 'text-center mt-3';
            buttonDiv.innerHTML = `
                <button id="reloginBtn" class="btn btn-primary me-2">
                    <i class="bi bi-box-arrow-in-right"></i> 重新登录
                </button>
                <button id="manualTestBtn" class="btn btn-secondary me-2">
                    <i class="bi bi-pencil-square"></i> 手动发消息
                </button>
                <button id="retryLoadBtn" class="btn btn-success">
                    <i class="bi bi-arrow-repeat"></i> 重试加载
                </button>
            `;
            listElement.appendChild(buttonDiv);
            
            // 添加事件监听器
            document.getElementById('reloginBtn')?.addEventListener('click', () => {
                window.location.href = loginPageUrl;
            });
            
            document.getElementById('manualTestBtn')?.addEventListener('click', () => {
                // 打开好友列表，用户可以选择好友发送消息
                showTab('friends');
            });
            
            document.getElementById('retryLoadBtn')?.addEventListener('click', () => {
                loadRecentContacts();
            });
            
            // 输出技术细节到控制台，帮助调试
            console.log("调试信息汇总：");
            console.log("1. 当前用户信息:", JSON.parse(localStorage.getItem('user') || '{}'));
            console.log("2. API基础URL:", API.baseUrl);
            console.log("3. 当前页面路径:", window.location.pathname);
            console.log("4. 浏览器User-Agent:", navigator.userAgent);
            console.log("5. 操作系统:", navigator.platform);
            
            // 提示用户检查网络请求
            console.log("%c请检查Network面板中对/message/recent_contacts的请求", "color:red; font-size:16px; font-weight:bold");
        }
    }

    async function loadAllCourses() {
        console.log("开始加载所有课程");
        const tbody = document.getElementById('coursesList');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">更新中...</td></tr>';
        } else {
            console.warn("coursesList tbody not found");
            return;
        }

        try {
            // 检查API对象的可用性
            if (!API || !API.course || typeof API.course.list !== 'function') {
                console.error("API.course.list 未定义或不是函数", API);
                tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">API未正确加载，请刷新页面重试</td></tr>';
                return;
            }
            
            console.log("调用 API.course.list...");
            const allCourses = await API.course.list();
            console.log("课程数据获取结果:", allCourses);
            const enrolledCourses = await API.enrollment.list();
            const enrolledCourseIds = enrolledCourses.map(course => course.courseId);
            const availableCourses = allCourses.filter(course => !enrolledCourseIds.includes(course.courseId));
            renderCoursesTable(availableCourses, 'coursesList', false);
        } catch (error) {
            console.error('获取所有课程失败:', error);
            const courseListEl = document.getElementById('coursesList');
            if(courseListEl) courseListEl.innerHTML = `<tr><td colspan="5" class="text-center text-danger">获取课程列表失败: ${error.message}</td></tr>`;
        }
    }

    async function loadEnrolledCourses() {
        console.log("开始加载已选课程");
        const tbody = document.getElementById('myCoursesList');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">更新中...</td></tr>';
        } else {
            console.warn("myCoursesList tbody not found");
            return;
        }

        try {
            // 检查API对象的可用性
            if (!API || !API.enrollment || typeof API.enrollment.list !== 'function') {
                console.error("API.enrollment.list 未定义或不是函数", API);
                tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">API未正确加载，请刷新页面重试</td></tr>';
                return;
            }
            
            console.log("调用 API.enrollment.list...");
            const enrolledCourses = await API.enrollment.list();
            console.log("已选课程数据获取结果:", enrolledCourses);
            renderCoursesTable(enrolledCourses, 'myCoursesList', true);
        } catch (error) {
            console.error('获取已选课程失败:', error);
            const myCourseListEl = document.getElementById('myCoursesList');
            if(myCourseListEl) myCourseListEl.innerHTML = `<tr><td colspan="6" class="text-center text-danger">获取已选课程失败: ${error.message}</td></tr>`;
        }
    }
    
    function renderCoursesTable(courses, tableBodyId, isMyCourses) {
        const tbody = document.getElementById(tableBodyId);
        if (!tbody) return;
        tbody.innerHTML = ''; // This will clear the "更新中..." or "加载中..." message
        
        if (!courses || courses.length === 0) {
            const colspan = isMyCourses ? 6 : 5;
            tbody.innerHTML = `<tr><td colspan="${colspan}" class="text-center">暂无相关课程记录</td></tr>`;
            return;
        }
        
        courses.forEach(course => {
            const row = tbody.insertRow();
            row.insertCell().textContent = course.courseId;
            row.insertCell().textContent = course.courseName;
            row.insertCell().textContent = course.deptName || 'N/A';
            row.insertCell().textContent = course.credit;

            if (isMyCourses) {
                row.insertCell().textContent = course.grade || '暂无';
            }

            const actionCell = row.insertCell();
            const button = document.createElement('button');
            button.classList.add('btn', 'btn-sm');
            if (isMyCourses) {
                button.classList.add('btn-danger');
                button.innerHTML = '<i class="bi bi-trash"></i> 退课';
                button.onclick = () => dropCourse(course.courseId);
            } else {
                button.classList.add('btn-success');
                button.innerHTML = '<i class="bi bi-plus-circle"></i> 选课';
                button.onclick = () => enrollCourse(course.courseId);
            }
            actionCell.appendChild(button);
        });
    }

    async function enrollCourse(courseId) {
        try {
            await API.enrollment.add(courseId);
            window.showToast(`课程 ${courseId} 选课成功!`, 'success');
            loadAllCourses();
            loadEnrolledCourses();
        } catch (error) {
            window.showToast(`选课失败: ${error.message}`, 'danger');
        }
    }

    async function dropCourse(courseId) {
        // 获取已选课程列表，找到对应的课程名称
        try {
            const enrolledCourses = await API.enrollment.list();
            const course = enrolledCourses.find(c => c.courseId === courseId);
            const courseName = course ? course.courseName : courseId;
            
            if (confirm(`确定要退选课程 ${courseName} (${courseId}) 吗？`)) {
                try {
                    await API.enrollment.drop(courseId);
                    window.showToast(`课程 ${courseName} 退课成功!`, 'success');
                    loadAllCourses();
        loadEnrolledCourses();
                } catch (error) {
                    window.showToast(`退课失败: ${error.message}`, 'danger');
                }
            }
        } catch (error) {
            // 如果获取课程名称失败，仍然显示课程ID
            if (confirm(`确定要退选课程 ${courseId} 吗？`)) {
                try {
                    await API.enrollment.drop(courseId);
                    window.showToast(`课程 ${courseId} 退课成功!`, 'success');
                    loadAllCourses();
                    loadEnrolledCourses();
                } catch (error) {
                    window.showToast(`退课失败: ${error.message}`, 'danger');
                }
            }
        }
    }
    
    async function loadDepartmentsForFilter() {
        try {
            const courses = await API.course.list();
            const departments = [...new Set(courses.map(course => course.deptName).filter(Boolean))];
            const deptFilter = document.getElementById('deptFilter');
            if (!deptFilter) return;
            
            const firstOptionValue = deptFilter.options.length > 0 ? deptFilter.options[0].value : "";
            const firstOptionText = deptFilter.options.length > 0 ? deptFilter.options[0].textContent : "全部分院/系";
            
            deptFilter.innerHTML = ''; 
            
            const defaultOption = document.createElement('option');
            defaultOption.value = firstOptionValue;
            defaultOption.textContent = firstOptionText;
            deptFilter.appendChild(defaultOption);

            departments.forEach(deptName => {
                const option = document.createElement('option');
                option.value = deptName;
                option.textContent = deptName;
                deptFilter.appendChild(option);
            });
            
            deptFilter.onchange = function() {
                const selectedDept = this.value;
                const searchTerm = document.getElementById('courseSearchInput').value.toLowerCase();
                filterAndRenderAvailableCourses(searchTerm, selectedDept);
            };
            document.getElementById('courseSearchInput').oninput = function() {
                const searchTerm = this.value.toLowerCase();
                const selectedDept = document.getElementById('deptFilter').value;
                filterAndRenderAvailableCourses(searchTerm, selectedDept);
            };

        } catch (error) {
            console.error('加载院系失败:', error);
        }
    }

    async function filterAndRenderAvailableCourses(searchTerm, selectedDept) {
        try {
            const allCourses = await API.course.list();
            const enrolledCourses = await API.enrollment.list();
            const enrolledCourseIds = enrolledCourses.map(course => course.courseId);
            
            let availableCourses = allCourses.filter(course => !enrolledCourseIds.includes(course.courseId));

            if (selectedDept) {
                availableCourses = availableCourses.filter(course => course.deptName === selectedDept);
            }
            if (searchTerm) {
                availableCourses = availableCourses.filter(course => 
                    (course.courseName || '').toLowerCase().includes(searchTerm) || 
                    (course.courseId || '').toLowerCase().includes(searchTerm)
                );
            }
            renderCoursesTable(availableCourses, 'coursesList', false);
        } catch (error) {
             console.error('筛选课程失败:', error);
        }
    }

    // 添加切换支付表单的函数
    function togglePaymentForm(formId, show) {
        const form = document.getElementById(formId);
        if (form) {
            form.style.display = show ? 'block' : 'none';
        } else {
            console.error(`找不到ID为 ${formId} 的表单元素`);
        }
    }

    // 修改处理充值函数
    async function handleDeposit() {
        console.log("处理充值请求");
        const amountInput = document.getElementById('depositAmountInput');
        if (!amountInput) {
            window.showToast('找不到充值金额输入框', 'danger');
            return;
        }
        
        const amount = parseFloat(amountInput.value);
        if (isNaN(amount) || amount <= 0) {
            window.showToast('请输入有效的充值金额', 'danger');
            return;
        }
        
        // 禁用按钮防止重复提交
        const confirmDepositBtn = document.getElementById('confirmDepositBtn');
        if (confirmDepositBtn) {
            confirmDepositBtn.disabled = true;
            confirmDepositBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> 处理中...';
        }
        
        try {
            console.log(`尝试充值: ${amount}元`);
            await API.payment.deposit(amount);
            window.showToast('充值成功!', 'success');
            
            // 刷新余额和交易记录
            loadBalance();
            loadTransactions();
            
            // 清空输入框并隐藏表单
            amountInput.value = '';
            const depositForm = document.getElementById('depositForm');
            if (depositForm) depositForm.style.display = 'none';
            console.log("充值表单已隐藏");
        } catch (error) {
            console.error('充值失败:', error);
            // 显示更具体的错误信息
            let errorMsg = '充值失败';
            if (error.message) {
                errorMsg += ': ' + error.message;
            }
            window.showToast(errorMsg, 'danger', 5000); // 显示5秒
        } finally {
            // 恢复按钮状态
            if (confirmDepositBtn) {
                confirmDepositBtn.disabled = false;
                confirmDepositBtn.innerHTML = '确认充值';
            }
        }
    }

    async function handleWithdraw() {
        console.log("处理提现请求");
        const amountInput = document.getElementById('withdrawAmountInput');
        if (!amountInput) {
            window.showToast('找不到提现金额输入框', 'danger');
            return;
        }
        
        const amount = parseFloat(amountInput.value);
        if (isNaN(amount) || amount <= 0) {
            window.showToast('请输入有效的提现金额', 'danger');
            return;
        }
        
        // 禁用按钮防止重复提交
        const confirmWithdrawBtn = document.getElementById('confirmWithdrawBtn');
        if (confirmWithdrawBtn) {
            confirmWithdrawBtn.disabled = true;
            confirmWithdrawBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> 处理中...';
        }
        
        try {
            console.log(`尝试提现: ${amount}元`);
            await API.payment.withdraw(amount);
            window.showToast('提现成功!', 'success');
            
            // 刷新余额和交易记录
            loadBalance();
            loadTransactions();
            
            // 清空输入框并隐藏表单
            amountInput.value = '';
            const withdrawForm = document.getElementById('withdrawForm');
            if (withdrawForm) withdrawForm.style.display = 'none';
            console.log("提现表单已隐藏");
        } catch (error) {
            console.error('提现失败:', error);
            // 显示更具体的错误信息
            let errorMsg = '提现失败';
            if (error.message) {
                errorMsg += ': ' + error.message;
            }
            window.showToast(errorMsg, 'danger', 5000); // 显示5秒
        } finally {
            // 恢复按钮状态
            if (confirmWithdrawBtn) {
                confirmWithdrawBtn.disabled = false;
                confirmWithdrawBtn.innerHTML = '确认提现';
            }
        }
    }

    async function loadBalance() {
        console.log("开始加载账户余额");
        const balanceAmountEl = document.getElementById('balanceAmount');
        const profileBalanceEl = document.getElementById('profileBalance');
        const mainBalanceAmountEl = document.getElementById('mainBalanceAmount');

        if(balanceAmountEl) balanceAmountEl.textContent = '更新中...';
        if(profileBalanceEl) profileBalanceEl.textContent = '更新中...';
        if(mainBalanceAmountEl) mainBalanceAmountEl.textContent = '更新中...';
        
        try {
            // 检查API对象的可用性
            if (!API || !API.student || typeof API.student.getInfo !== 'function') {
                console.error("API.student.getInfo 未定义或不是函数", API);
                if(balanceAmountEl) balanceAmountEl.textContent = 'API错误';
                if(profileBalanceEl) profileBalanceEl.textContent = 'API错误';
                if(mainBalanceAmountEl) mainBalanceAmountEl.textContent = 'API错误';
                return;
            }
            
            console.log("调用 API.student.getInfo...");
            const studentInfo = await API.student.getInfo();
            console.log("学生信息获取结果:", studentInfo);
            
            const balance = parseFloat(studentInfo.balance || 0).toFixed(2);

            if(balanceAmountEl) balanceAmountEl.textContent = `¥ ${balance}`;
            if(profileBalanceEl) profileBalanceEl.textContent = `¥ ${balance}`;
            if(mainBalanceAmountEl) mainBalanceAmountEl.textContent = `¥ ${balance}`;

        } catch (error) {
            console.error('获取余额失败:', error);
            const balanceAmountEl = document.getElementById('balanceAmount');
            if(balanceAmountEl) balanceAmountEl.textContent = '获取失败';
        }
    }

    async function loadTransactions() {
        console.log("开始加载交易记录");
        const listElement = document.getElementById('transactionList');
        if(!listElement) return;
        listElement.innerHTML = '<li class="list-group-item text-center">更新中...</li>'; // Clear "加载中..." first
        
        try {
            // 检查API对象的可用性
            if (!API || !API.transaction || typeof API.transaction.list !== 'function') {
                console.error("API.transaction.list 未定义或不是函数", API);
                listElement.innerHTML = '<li class="list-group-item text-center text-danger">API未正确加载，请刷新页面重试</li>';
                return;
            }
            
            console.log("调用 API.transaction.list...");
            const transactions = await API.transaction.list();
            console.log("交易记录获取结果:", transactions);
            if(!listElement) return;
            listElement.innerHTML = '';
            if (!transactions || transactions.length === 0) {
                listElement.innerHTML = '<li class="list-group-item text-center">暂无交易记录</li>';
            return;
        }
        
            // 从本地存储获取当前用户ID，用于判断转账方向
            const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
            const currentUserId = currentUser.studentId;
            
            console.log("处理交易记录，当前用户ID:", currentUserId);
            console.log("交易记录原始数据:", transactions);
            
            transactions.forEach((t, index) => {
                console.log(`处理第${index+1}条交易记录:`, t);
                
                // 规范化字段名（处理可能的不一致）
                const transaction = {
                    transactionId: t.transaction_id || t.transactionId,
                    type: t.type,
                    amount: parseFloat(t.amount || 0),
                    fromStudentId: t.from_student_id || t.fromStudentId,
                    fromStudentName: t.from_student_name || t.fromStudentName,
                    toStudentId: t.to_student_id || t.toStudentId,
                    toStudentName: t.to_student_name || t.toStudentName,
                    description: t.description,
                    transactionDate: t.transaction_date || t.transactionDate || new Date()
                };
                
                console.log(`规范化后的交易数据:`, transaction);
                
            const item = document.createElement('li');
            item.className = 'list-group-item d-flex justify-content-between align-items-center';
                const date = new Date(transaction.transactionDate).toLocaleString();
                
                // 默认为支出（红色负数）
                let isPositive = false;
                
                // 充值和退款总是收入（绿色正数）
                if (transaction.type === 'DEPOSIT' || transaction.type === 'REFUND') {
                    isPositive = true;
                }
                // 提现总是支出（红色负数）
                else if (transaction.type === 'WITHDRAW') {
                    isPositive = false;
                }
                // 对于转账，需要根据当前用户是发送方还是接收方来确定
                else if (transaction.type === 'TRANSFER') {
                    // 如果当前用户是接收方，则为收入（绿色正数）
                    if (transaction.toStudentId && transaction.toStudentId === currentUserId) {
                        isPositive = true;
                        console.log(`用户是接收方，显示为收入（绿色正数）`);
                    }
                    // 如果当前用户是发送方，则为支出（红色负数）
                    else if (transaction.fromStudentId && transaction.fromStudentId === currentUserId) {
                        isPositive = false;
                        console.log(`用户是发送方，显示为支出（红色负数）`);
                    }
                    // 如果无法确定角色，则根据描述判断
                    else {
                        const desc = (transaction.description || '').toLowerCase();
                        isPositive = desc.includes('收到') || desc.includes('接收');
                        console.log(`无法确定角色，根据描述判断为: ${isPositive ? '收入' : '支出'}`);
                    }
                }
                
                const amountClass = isPositive ? 'text-success' : 'text-danger';
                const sign = isPositive ? '+' : '-';
                const amount = Math.abs(transaction.amount).toFixed(2);
                
                // 构建描述文本
                let description = transaction.description || '无描述';
                if (transaction.type === 'TRANSFER') {
                    if (transaction.toStudentId === currentUserId) {
                        const fromName = transaction.fromStudentName || transaction.fromStudentId || '未知用户';
                        description = `来自 ${fromName} 的转账${description ? ': ' + description : ''}`;
                    } else if (transaction.fromStudentId === currentUserId) {
                        const toName = transaction.toStudentName || transaction.toStudentId || '未知用户';
                        description = `转账给 ${toName}${description ? ': ' + description : ''}`;
                    }
                }
                
                // 翻译交易类型为中文
                let typeText = transaction.type;
                switch (transaction.type) {
                    case 'DEPOSIT': typeText = '充值'; break;
                    case 'WITHDRAW': typeText = '提现'; break;
                    case 'TRANSFER': typeText = '转账'; break;
                    case 'REFUND': typeText = '退款'; break;
                }
                
            item.innerHTML = `
                <div>
                    <strong class="d-block">${typeText}</strong>
                    <small class="text-muted">${description}</small>
                </div>
                <div>
                    <span class="${amountClass} fw-bold me-2">${sign}${amount}</span>
                    <small class="text-muted">${date}</small>
                </div>
            `;
                listElement.appendChild(item);
            });
        } catch (error) {
            console.error('获取交易记录失败:', error);
            const listElement = document.getElementById('transactionList');
            if(listElement) listElement.innerHTML = '<li class="list-group-item text-center text-danger">加载失败</li>';
        }
    }
    
    async function loadFriendsAndRecommendations() {
        try {
            // 先完整加载好友列表
            await loadFriends();
            console.log("好友列表加载完成，开始加载推荐");
            
            // 然后加载推荐
            await loadFriendRecommendations();
        } catch (error) {
            console.error("加载好友和推荐失败:", error);
            window.showToast("加载好友和推荐信息失败", "danger");
        }
    }

    async function loadFriends() {
        console.log("开始加载好友列表");
        const container = document.getElementById('friendListContainer');
        if (!container) {
            console.error("好友列表容器 'friendListContainer' 未找到。");
            return;
        }
        container.innerHTML = '<p class="text-center text-muted">更新中...</p>'; 

        try {
            // 检查API对象的可用性
            if (!API || !API.friendship || typeof API.friendship.list !== 'function') {
                console.error("API.friendship.list 未定义或不是函数", API);
                container.innerHTML = '<p class="text-center text-danger">API未正确加载，请刷新页面重试</p>';
                return;
            }
            
            console.log("调用 API.friendship.list...");
            const friendsData = await API.friendship.list(); 
            console.log("好友列表数据:", friendsData);
            
            container.innerHTML = ''; // 在获取数据后清空容器

            // 处理API返回的数据格式，统一字段名
            const normalizedFriends = friendsData.map(friend => ({
                studentId: friend.student_id || friend.studentId,
                name: friend.name,
                deptName: friend.dept_name || friend.deptName,
                deptId: friend.department_id || friend.deptId,
                status: friend.status,
                balance: friend.balance,
                rejectTime: friend.reject_time || friend.rejectTime
            }));

            const acceptedFriends = normalizedFriends.filter(f => f.status === 'ACCEPTED');
            const sentRequests = normalizedFriends.filter(f => f.status === 'PENDING_REQUEST');
            const receivedRequests = normalizedFriends.filter(f => f.status === 'PENDING_RECEIVED'); 
            const rejectedRequests = normalizedFriends.filter(f => f.status === 'REJECTED');

            console.log("已接受的好友:", acceptedFriends.length);
            console.log("已发送的请求:", sentRequests.length);
            console.log("收到的请求:", receivedRequests.length);
            console.log("被拒绝的请求:", rejectedRequests.length);

            renderFriendCategory(acceptedFriends, '我的好友', 'accepted', container);
            renderFriendCategory(receivedRequests, '收到的请求', 'received', container);
            renderFriendCategory(sentRequests, '已发送的请求', 'sent', container);
            renderFriendCategory(rejectedRequests, '被拒绝的请求', 'rejected', container);

        } catch (error) {
            console.error('加载好友列表失败:', error);
            const container = document.getElementById('friendListContainer');
            if(container) container.innerHTML = `<p class="text-center text-danger">加载好友列表失败: ${error.message || '未知错误'}</p>`;
        }
    }
    
    function renderFriendCategory(friends, title, type, parentContainer) {
        const section = document.createElement('div');
        section.className = 'mb-4';
        
        const heading = document.createElement('h5');
        heading.className = 'mb-3'; // Added margin to heading
        heading.innerHTML = `${title} <span class="badge bg-secondary rounded-pill">${friends.length}</span>`;
        section.appendChild(heading);

        const listGroup = document.createElement('div');
        listGroup.className = 'list-group'; // Use list-group for consistent styling

        if (friends.length === 0) {
            const placeholder = document.createElement('div'); // Changed to div for list-group-item
            placeholder.className = 'list-group-item text-muted';
            placeholder.textContent = `暂无${title.replace("我的","").toLowerCase()}`; // e.g. 暂无好友
            listGroup.appendChild(placeholder);
        } else {
            friends.forEach(friend => {
                const item = document.createElement('div');
                // Applied list-group-item for Bootstrap styling, friend-item for potential custom styling
                item.className = 'list-group-item friend-item d-flex align-items-center'; 
                
                const avatarLetter = (friend.name || 'N').charAt(0).toUpperCase();
                const avatarBgColor = getRandomColor(friend.studentId || Math.random().toString());

                let actionsHtml = '';
                // Note: onclick attributes call globally defined functions (window.functionName)
                if (type === 'accepted') {
                    actionsHtml = `
                        <button class="btn btn-sm btn-primary" onclick="window.openChatWindow('${friend.studentId}', '${friend.name}')"><i class="bi bi-chat-dots"></i></button>
                        <button class="btn btn-sm btn-success" onclick="window.openTransferModalFromButton(this)" data-friend-id="${friend.studentId}" data-friend-name="${friend.name}"><i class="bi bi-currency-dollar"></i></button>
                        <button class="btn btn-sm btn-danger" onclick="window.deleteFriend('${friend.studentId}')"><i class="bi bi-person-x"></i></button>
                    `;
                } else if (type === 'received') {
                    actionsHtml = `
                        <button class="btn btn-sm btn-success" onclick="window.acceptFriendRequest('${friend.studentId}')"><i class="bi bi-check-lg"></i> 接受</button>
                        <button class="btn btn-sm btn-danger" onclick="window.rejectFriendRequest('${friend.studentId}')"><i class="bi bi-x-lg"></i> 拒绝</button>
                    `;
                } else if (type === 'sent') {
                    actionsHtml = `
                        <button class="btn btn-sm btn-warning" onclick="window.cancelFriendRequest('${friend.studentId}')"><i class="bi bi-x-circle"></i> 取消</button>
                    `;
                } else if (type === 'rejected') {
                    const rejectTime = friend.rejectTime ? new Date(friend.rejectTime).toLocaleDateString() : '';
                    actionsHtml = `
                        <span class="badge bg-danger me-2">已拒绝 ${rejectTime}</span>
                        <button class="btn btn-sm btn-outline-primary" onclick="window.addFriendById('${friend.studentId}')"><i class="bi bi-arrow-repeat"></i></button>
                    `;
                }

            item.innerHTML = `
                    <div class="friend-avatar" style="background-color: ${avatarBgColor};">${avatarLetter}</div>
                    <div class="friend-info flex-grow-1">
                        <strong>${friend.name || '未知姓名'}</strong>
                        <small class="d-block text-muted">学号: ${friend.studentId || '未知学号'} | 院系: ${friend.deptName || '未知'}</small>
                        ${friend.recommendReason ? `<small class="text-success d-block fst-italic">${friend.recommendReason}</small>` : ''}
                </div>
                    <div class="friend-actions ms-auto d-flex gap-1">
                        ${actionsHtml}
                    </div>
                `;
                listGroup.appendChild(item);
            });
        }
        section.appendChild(listGroup);
        parentContainer.appendChild(section);
    }
    
    async function handleAddFriend(event) {
        event.preventDefault();
        const friendIdInput = document.getElementById('addFriendIdInput');
        if(!friendIdInput) return;
        const friendId = friendIdInput.value.trim();
        if (!friendId) {
            window.showToast('请输入要添加的好友学号', 'warning');
            return;
        }
        await addFriendById(friendId); // Use the global/shared function
        friendIdInput.value = '';
    }
    
    async function addFriendById(friendId) { // Made globally accessible for onclick
        // 检查ID参数是否有效
        if (!friendId || typeof friendId !== 'string' || friendId.trim() === '') {
            console.error(`添加好友失败: 无效的ID参数`, friendId);
            window.showToast('添加好友失败: 无效的ID参数', 'warning');
            return;
        }
        
        // 规范化ID，移除多余空格
        const trimmedId = friendId.trim();
        
        try {
            // 先检查是否已经是好友或已发送请求
            console.log(`尝试添加好友 ID: ${trimmedId}`);
            
            // 获取现有好友列表，检查重复添加
            const friends = await API.friendship.list();
            const existingFriend = friends.find(f => 
                (f.studentId === trimmedId || f.student_id === trimmedId) && 
                ['ACCEPTED', 'PENDING_REQUEST'].includes(f.status)
            );
            
            if (existingFriend) {
                if (existingFriend.status === 'ACCEPTED') {
                    window.showToast(`${existingFriend.name || trimmedId} 已经是您的好友`, 'info');
                } else {
                    window.showToast(`已向 ${existingFriend.name || trimmedId} 发送过好友请求`, 'info');
                }
                return;
            }
            
            // 发送添加好友请求
            await API.friendship.add(trimmedId);
            window.showToast('好友请求已发送!', 'success');
            console.log(`好友请求发送成功，ID: ${trimmedId}`);
            
            // 刷新好友列表，显示新发送的请求
            loadFriends(); 
        } catch (error) {
            console.error(`添加好友失败 (ID: ${trimmedId}):`, error);
            
            // 根据错误类型提供更友好的错误信息
            let errorMessage = error.message || '未知错误';
            if (errorMessage.includes('不存在')) {
                errorMessage = `学号为 ${trimmedId} 的学生不存在，请检查学号是否正确`;
            } else if (errorMessage.includes('已经发送')) {
                errorMessage = `已经向该学生发送过好友请求`;
            } else if (errorMessage.includes('自己')) {
                errorMessage = `不能添加自己为好友`;
            }
            
            window.showToast(`添加好友失败: ${errorMessage}`, 'danger');
        }
    }

    async function handleSearchStudents() {
        const keywordInput = document.getElementById('studentSearchKeywordInput');
        if(!keywordInput) return;
        const keyword = keywordInput.value.trim();
        if (!keyword) {
            window.showToast('请输入搜索关键词', 'warning');
            return;
        }
        try {
            console.log(`搜索学生，关键词: ${keyword}`);
            const students = await API.friendship.search(keyword);
            console.log("搜索结果:", students);
            
            const resultsList = document.getElementById('searchResultsList');
            if(!resultsList) return;
            resultsList.innerHTML = '';
            if (students.length === 0) {
                resultsList.innerHTML = '<li class="list-group-item text-muted">未找到相关学生</li>';
            return;
        }
        
            students.forEach(student => {
                // 处理API返回的字段名不一致问题
                const studentId = student.student_id || student.studentId;
                const studentName = student.name;
                const deptName = student.dept_name || student.deptName;
                
                if (!studentId) {
                    console.error("搜索结果数据异常，缺少学生ID:", student);
                    return; // 跳过没有ID的记录
                }
                
            const item = document.createElement('li');
                item.className = 'list-group-item search-result-item d-flex align-items-center';
                const avatarLetter = (studentName || 'S').charAt(0).toUpperCase();
                const avatarBgColor = getRandomColor(studentId || Math.random().toString());

            item.innerHTML = `
                    <div class="friend-avatar" style="background-color: ${avatarBgColor};">${avatarLetter}</div>
                    <div class="search-result-info flex-grow-1">
                        <strong>${studentName} (${studentId})</strong>
                        <small class="d-block text-muted">院系: ${deptName || '未知'}</small>
                </div>
                    <div class="search-result-actions ms-auto">
                        <button class="btn btn-sm btn-outline-primary search-add-friend-btn" data-student-id="${studentId}">
                            <i class="bi bi-person-plus"></i> 添加
                    </button>
                </div>
            `;
                resultsList.appendChild(item);
                
                // 使用事件委托添加点击事件
                const addButton = item.querySelector('.search-add-friend-btn');
                if (addButton) {
                    addButton.addEventListener('click', function() {
                        const friendId = this.dataset.studentId;
                        if (friendId) {
                            console.log(`点击搜索结果添加好友按钮，好友ID: ${friendId}`);
                            addFriendById(friendId);
                        } else {
                            console.error('添加好友失败: 未找到好友ID');
                            window.showToast('添加好友失败: 未找到好友ID', 'danger');
                        }
                    });
                }
            });
        } catch (error) {
            console.error("搜索学生失败:", error);
            window.showToast(`搜索学生失败: ${error.message}`, 'danger');
            const resultsList = document.getElementById('searchResultsList');
            if(resultsList) resultsList.innerHTML = '<li class="list-group-item text-danger">搜索失败</li>';
        }
    }

    async function loadFriendRecommendations() {
        console.log("开始加载好友推荐");
        const listElement = document.getElementById('recommendedFriendsList');
        if(!listElement) return;
        listElement.innerHTML = '<li class="list-group-item text-muted text-center">更新中...</li>';
            
        try {
            // 检查API对象的可用性
            if (!API || !API.friendship || typeof API.friendship.recommendations !== 'function') {
                console.error("API.friendship.recommendations 未定义或不是函数", API);
                listElement.innerHTML = '<li class="list-group-item text-center text-danger">API未正确加载，请刷新页面重试</li>';
            return;
        }
        
            console.log("调用 API.friendship.recommendations...");
            const recommendations = await API.friendship.recommendations('all', 5); 
            console.log("好友推荐数据:", recommendations);
            
            // 清空列表，删除"更新中..."
            listElement.innerHTML = '';
            
            if (!recommendations || recommendations.length === 0) {
                listElement.innerHTML = '<li class="list-group-item text-muted text-center">暂无好友推荐</li>';
                return;
            }
            
            // 处理后续的推荐项目渲染...

            recommendations.forEach(friend => {
                // 确保studentId字段存在，规范化API返回的数据
                const studentId = friend.student_id || friend.studentId;
                const name = friend.name || '未知姓名';
                const deptName = friend.dept_name || friend.deptName || '未知院系';
                
                if (!studentId) {
                    console.error("推荐好友数据异常，缺少学生ID:", friend);
                    return; // 跳过没有ID的记录
                }
                
            const item = document.createElement('li');
                item.className = 'list-group-item recommendation-item d-flex align-items-center';
                const avatarLetter = (name.charAt(0) || 'R').toUpperCase();
                const avatarBgColor = getRandomColor(studentId || Math.random().toString());
            
            item.innerHTML = `
                    <div class="friend-avatar" style="background-color: ${avatarBgColor};">${avatarLetter}</div>
                    <div class="recommendation-info flex-grow-1">
                        <strong>${name} (${studentId})</strong>
                        <small class="d-block text-muted">院系: ${deptName}</small>
                        ${friend.recommendReason ? `<small class="text-success d-block fst-italic">${friend.recommendReason}</small>` : ''}
                </div>
                    <div class="recommendation-actions ms-auto">
                        <button class="btn btn-sm btn-outline-primary add-friend-btn" data-student-id="${studentId}">
                            <i class="bi bi-person-plus"></i> 添加
                </button>
                    </div>
                `;
                
                listElement.appendChild(item);
                
                // 使用事件委托添加点击事件
                const addButton = item.querySelector('.add-friend-btn');
                if (addButton) {
                    addButton.addEventListener('click', function() {
                        const friendId = this.dataset.studentId;
                        if (friendId) {
                            console.log(`点击添加好友按钮，好友ID: ${friendId}`);
                            addFriendById(friendId);
                        } else {
                            console.error('添加好友失败: 未找到好友ID');
                            window.showToast('添加好友失败: 未找到好友ID', 'danger');
                        }
                    });
                }
            });
        } catch (error) {
            console.error('加载好友推荐失败:', error);
            const listElement = document.getElementById('recommendedFriendsList');
            if(listElement) {
                listElement.innerHTML = `
                    <li class="list-group-item text-danger text-center">加载推荐失败: ${error.message || '未知错误'}</li>
                    <li class="list-group-item text-center">
                        <button class="btn btn-sm btn-primary retry-recommendations-btn">
                            <i class="bi bi-arrow-clockwise"></i> 重试
                        </button>
                    </li>
                `;
                
                // 添加重试按钮事件
                const retryButton = listElement.querySelector('.retry-recommendations-btn');
                if (retryButton) {
                    retryButton.addEventListener('click', function() {
                        loadFriendRecommendations();
                    });
                }
            }
        }
    }
    
    function filterFriendList() {
        const searchInput = document.getElementById('friendSearchInput');
        if(!searchInput) return;
        const searchTerm = searchInput.value.toLowerCase();
        document.querySelectorAll('#friendListContainer .friend-item').forEach(item => {
            const nameElement = item.querySelector('.friend-info strong');
            const idElement = item.querySelector('.friend-info small'); // This contains more than just ID
            const name = nameElement ? nameElement.textContent.toLowerCase() : '';
            const detailsText = idElement ? idElement.textContent.toLowerCase() : '';
            
            if (name.includes(searchTerm) || detailsText.includes(searchTerm)) { // Search in name and details
                item.style.display = 'flex';
                } else {
                    item.style.display = 'none';
                }
            });
    }

    async function acceptFriendRequest(requesterId) { // Global
        if (!requesterId) {
            window.showToast('请求者ID不能为空', 'warning');
            return;
        }
        
        try {
            console.log(`接受好友请求，请求者ID: ${requesterId}`);
            await API.friendship.accept(requesterId);
            window.showToast('好友请求已接受!', 'success');
            console.log("好友请求已接受，重新加载好友列表");
            loadFriends();
        } catch (error) {
            console.error(`接受好友请求失败 (ID: ${requesterId}):`, error);
            window.showToast(`接受请求失败: ${error.message}`, 'danger');
        }
    }

    async function rejectFriendRequest(requesterId) { // Global
        if (!requesterId) {
            window.showToast('请求者ID不能为空', 'warning');
            return;
        }
        
        try {
            console.log(`拒绝好友请求，请求者ID: ${requesterId}`);
            await API.friendship.reject(requesterId);
            window.showToast('好友请求已拒绝!', 'info');
            console.log("好友请求已拒绝，重新加载好友列表");
            loadFriends();
        } catch (error) {
            console.error(`拒绝好友请求失败 (ID: ${requesterId}):`, error);
            window.showToast(`拒绝请求失败: ${error.message}`, 'danger');
        }
    }
    
    async function cancelFriendRequest(targetId) { // Global
         try {
            await API.friendship.delete(targetId); // Using delete for PENDING implies cancellation
            window.showToast('好友请求已取消!', 'info');
            loadFriends();
        } catch (error) {
            window.showToast(`取消请求失败: ${error.message}`, 'danger');
        }
    }

    async function deleteFriend(friendId) { // Global
        if (confirm(`确定要删除好友 ${friendId} 吗？此操作不可恢复。`)) {
            try {
                await API.friendship.delete(friendId);
                window.showToast('好友已删除!', 'success');
                loadFriends();
            } catch (error) {
                window.showToast(`删除好友失败: ${error.message}`, 'danger');
            }
        }
    }

    // 全局变量，跟踪当前聊天好友
    let currentChatFriendId = null;
    let currentChatFriendName = null;

    function openMainChat(friendId, friendName) {
        console.log(`打开主聊天窗口: 好友ID=${friendId}, 好友姓名=${friendName}`);
        currentChatFriendId = friendId;
        currentChatFriendName = friendName;

        const chattingWithNameEl = document.getElementById('chattingWithName');
        const mainMessageInputEl = document.getElementById('mainMessageInput');
        const mainSendMessageBtnEl = document.getElementById('mainSendMessageBtn');
        const mainChatTransferBtnEl = document.getElementById('mainChatTransferBtn');

        if(chattingWithNameEl) chattingWithNameEl.textContent = friendName || '...';
        if(mainMessageInputEl) mainMessageInputEl.disabled = false;
        if(mainSendMessageBtnEl) mainSendMessageBtnEl.disabled = false;
        if(mainChatTransferBtnEl) mainChatTransferBtnEl.disabled = false;
        
        loadAndDisplayMessages(friendId, 'mainChatMessagesContainer', true);
    }

    function openChatWindow(friendId, friendName) { // Global
        currentChatFriendId = friendId; 
        currentChatFriendName = friendName;
        const chatWindowEl = document.getElementById('chatWindow');
        const chatWindowFriendNameEl = document.getElementById('chatWindowFriendName');
        const chatWindowInputEl = document.getElementById('chatWindowInput');

        if(chatWindowFriendNameEl) chatWindowFriendNameEl.textContent = `与 ${friendName} 聊天中`;
        if(chatWindowEl) chatWindowEl.style.display = 'flex';
        if(chatWindowInputEl) {
            chatWindowInputEl.value = '';
            chatWindowInputEl.focus();
        }
        loadAndDisplayMessages(friendId, 'chatWindowMessages', true);
    }
    
    async function loadAndDisplayMessages(friendId, containerId, isOpening) {
        const container = document.getElementById(containerId);
        if(!container) return;

        if (isOpening) {
             container.innerHTML = '<p class="text-center my-auto text-muted">加载消息中...</p>';
        }

        try {
            const messages = await API.message.getConversation(friendId);
            container.innerHTML = ''; 
            
            console.log("收到的消息数据:", messages);
            
            if (!messages || !Array.isArray(messages) || messages.length === 0) {
                container.innerHTML = '<p class="text-center my-auto text-muted">暂无聊天记录</p>';
                return;
            }
        
            const currentUserId = user.studentId;
            messages.forEach(msg => {
                appendMessageToContainer(msg, currentUserId, container);
            });
            container.scrollTop = container.scrollHeight; 
        } catch (error) {
            console.error(`加载消息失败 (好友ID: ${friendId}):`, error);
            container.innerHTML = `<p class="text-center my-auto text-danger">加载消息失败: ${error.message || '未知错误'}</p>`;
            
            // 添加重试按钮
            const retryButton = document.createElement('div');
            retryButton.className = 'text-center mt-3';
            retryButton.innerHTML = `
                <button id="${containerId}_retryBtn" class="btn btn-sm btn-primary">
                    <i class="bi bi-arrow-clockwise"></i> 重试
                </button>
            `;
            container.appendChild(retryButton);
            
            document.getElementById(`${containerId}_retryBtn`)?.addEventListener('click', () => {
                loadAndDisplayMessages(friendId, containerId, true);
            });
        }
    }

    function appendMessageToContainer(message, currentUserId, containerElement) {
        const messageDiv = document.createElement('div');
        const isSent = message.fromStudentId === currentUserId;
        messageDiv.className = `chat-message ${isSent ? 'message-sent' : 'message-received'}`;
        
        let sendTime = '未知时间';
        if (message.sendTime) {
            const date = new Date(message.sendTime);
            const now = new Date();
            const yesterday = new Date(now);
            yesterday.setDate(yesterday.getDate() - 1);
            
            // 获取日期差（天数）
            const daysDiff = Math.floor((now - date) / (1000 * 60 * 60 * 24));
            
            if (date.toDateString() === now.toDateString()) {
                // 如果是今天，则显示时间
                sendTime = date.toLocaleTimeString('zh-CN', {hour: '2-digit', minute:'2-digit'});
            } else if (date.toDateString() === yesterday.toDateString()) {
                // 如果是昨天，显示"昨天"
                sendTime = `昨天 ${date.toLocaleTimeString('zh-CN', {hour: '2-digit', minute:'2-digit'})}`;
            } else if (daysDiff < 7) {
                // 如果是一周内，显示星期几
                const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
                sendTime = `${weekdays[date.getDay()]} ${date.toLocaleTimeString('zh-CN', {hour: '2-digit', minute:'2-digit'})}`;
            } else if (now.getFullYear() === date.getFullYear()) {
                // 同一年内，显示月/日 HH:MM
                sendTime = `${date.getMonth()+1}月${date.getDate()}日 ${date.toLocaleTimeString('zh-CN', {hour: '2-digit', minute:'2-digit'})}`;
            } else {
                // 不同年，显示年/月/日 HH:MM
                sendTime = `${date.getFullYear()}年${date.getMonth()+1}月${date.getDate()}日 ${date.toLocaleTimeString('zh-CN', {hour: '2-digit', minute:'2-digit'})}`;
            }
        }
        
        messageDiv.innerHTML = `
            <div>${escapeHTML(message.content)}</div>
            <small class="message-time">${sendTime}</small>
        `;
        containerElement.appendChild(messageDiv);
    }

    async function handleChatWindowSendMessage(event) {
        event.preventDefault();
        const input = document.getElementById('chatWindowInput');
        if(!input || !currentChatFriendId) return;
        const content = input.value.trim();
        
        if (content) {
            // 禁用发送按钮防止重复提交
            const sendButton = document.querySelector('#chatWindowMessageForm button[type="submit"]');
            if(sendButton) {
                sendButton.disabled = true;
                sendButton.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span>';
            }
            
            try {
                console.log(`发送消息到聊天窗口: ${content}`);
                const sentMessage = await API.message.send(currentChatFriendId, content);
                console.log("发送消息结果:", sentMessage);
                
                // 清空输入框并获取焦点
                input.value = '';
                input.focus();
                
                // 刷新当前聊天窗口以确保消息已更新
                await loadAndDisplayMessages(currentChatFriendId, 'chatWindowMessages', false);
                
                // 如果主消息页面也在显示与同一个好友的对话，也刷新它
                if (document.getElementById('messages').classList.contains('active') && 
                    document.getElementById('chattingWithName')?.textContent === currentChatFriendName) {
                    await loadAndDisplayMessages(currentChatFriendId, 'mainChatMessagesContainer', false);
                }
                
                // 强制立即刷新最近联系人列表
                if (document.getElementById('messages').classList.contains('active')) {
                    await loadRecentContacts();
                }
            } catch (error) {
                console.error("发送消息失败:", error);
                window.showToast(`发送消息失败: ${error.message}`, 'danger');
            } finally {
                // 恢复发送按钮状态
                if(sendButton) {
                    sendButton.disabled = false;
                    sendButton.innerHTML = '<i class="bi bi-send"></i>';
                }
            }
        }
    }
    
    async function handleMainSendMessage(event) {
        event.preventDefault();
        const input = document.getElementById('mainMessageInput');
        if(!input || !currentChatFriendId) return;
        const content = input.value.trim();

        if (content) {
            // 禁用发送按钮防止重复提交
            const sendButton = document.getElementById('mainSendMessageBtn');
            if(sendButton) {
                sendButton.disabled = true;
                sendButton.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span>';
            }
            
            try {
                console.log(`从主消息页面发送消息: ${content}`);
                const sentMessage = await API.message.send(currentChatFriendId, content);
                console.log("发送消息结果:", sentMessage);
                
                // 清空输入框并获取焦点
                input.value = '';
                input.focus();
                
                // 刷新主消息页面的聊天内容
                await loadAndDisplayMessages(currentChatFriendId, 'mainChatMessagesContainer', false);

                // 如果浮动聊天窗口也在显示与同一个好友的对话，也刷新它
                if (document.getElementById('chatWindow')?.style.display === 'flex' && 
                    document.getElementById('chatWindowFriendName')?.textContent.includes(currentChatFriendName)) {
                    await loadAndDisplayMessages(currentChatFriendId, 'chatWindowMessages', false);
                }
                
                // 强制立即刷新最近联系人列表
                await loadRecentContacts();
            } catch (error) {
                console.error("发送消息失败:", error);
                window.showToast(`发送消息失败: ${error.message}`, 'danger');
            } finally {
                // 恢复发送按钮状态
                if(sendButton) {
                    sendButton.disabled = false;
                    sendButton.innerHTML = '<i class="bi bi-send"></i>';
                }
            }
        }
    }

    function handleChatWindowTransfer() {
        if (currentChatFriendId && currentChatFriendName) {
            openTransferModal(currentChatFriendId, currentChatFriendName);
        }
    }
    
    function handleMainChatTransfer() {
        if (currentChatFriendId && currentChatFriendName) {
            openTransferModal(currentChatFriendId, currentChatFriendName);
        }
    }

    function openTransferModal(recipientId, recipientName) {
        console.log(`打开转账模态窗口：接收方=${recipientName}(${recipientId})`);
        
        const recipientNameEl = document.getElementById('transferRecipientName');
        const recipientIdEl = document.getElementById('transferRecipientId');
        const amountInputEl = document.getElementById('transferAmountInput_modal');
        const notesEl = document.getElementById('transferNotes');
        
        // 检查必要元素是否存在
        if (!recipientNameEl || !recipientIdEl || !amountInputEl || !notesEl) {
            console.error("转账模态窗口缺少必要元素");
            window.showToast("转账窗口初始化失败，请刷新页面重试", "danger");
            return;
        }

        // 设置接收方信息
        recipientNameEl.textContent = recipientName || '未知用户';
        recipientIdEl.value = recipientId;
        
        // 清空输入框
        amountInputEl.value = '';
        notesEl.value = '';
        
        // 获取或创建模态窗口实例
        const modalElement = document.getElementById('transferModal');
        if (!modalElement) {
            console.error("找不到转账模态窗口元素");
            window.showToast("无法打开转账窗口，请刷新页面重试", "danger");
            return;
        }
        
        // 尝试获取Bootstrap模态框实例
        let modal;
        try {
            modal = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
        } catch (error) {
            console.error("创建模态框实例失败:", error);
            window.showToast("无法创建转账窗口，请刷新页面重试", "danger");
            return;
        }
        
        // 获取确认按钮并重新绑定事件
        const confirmBtn = document.getElementById('confirmTransferBtn_modal');
        if (!confirmBtn) {
            console.error("找不到转账确认按钮");
            window.showToast("无法找到转账确认按钮，请刷新页面重试", "danger");
            return;
        }
        
        // 克隆并替换按钮以移除旧的事件监听器
        const newConfirmBtn = confirmBtn.cloneNode(true);
        confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);

        // 默认隐藏转账警告区域
        const warningEl = document.getElementById('transferWarningAlert');
        if (warningEl) {
            warningEl.style.display = 'none';
        }

        // 为金额输入框添加验证和警告显示
        if (amountInputEl && warningEl) {
            amountInputEl.addEventListener('input', function() {
                const amount = parseFloat(this.value);
                // 当金额超过500元时显示警告
                if (!isNaN(amount) && amount > 500) {
                    warningEl.style.display = 'block';
                    warningEl.innerHTML = `
                        <i class="bi bi-exclamation-triangle-fill me-2"></i>
                        <strong>转账金额较大！</strong> 您正准备向 ${recipientName} 转账 
                        <span class="text-danger fw-bold">${amount.toFixed(2)}元</span>，
                        请再次确认金额正确。
                    `;
                } else {
                    warningEl.style.display = 'none';
                }
            });
        }

        // 为新的确认按钮添加事件监听器
        newConfirmBtn.addEventListener('click', async function(event) {
            event.preventDefault();
            console.log("确认转账按钮被点击");
            
            // 重新获取输入元素，避免使用可能过期的引用
            const amountInput = document.getElementById('transferAmountInput_modal');
            const notesInput = document.getElementById('transferNotes');
            
            if (!amountInput || !notesInput) {
                console.error("找不到转账表单元素");
                window.showToast("表单元素缺失，请刷新页面重试", "danger");
                return;
            }
            
            const amountVal = parseFloat(amountInput.value);
            const notesVal = notesInput.value.trim();

            if (isNaN(amountVal) || amountVal <= 0) {
                window.showToast('请输入有效的转账金额', 'warning');
                return;
            }

            // 转账金额预警
            if (amountVal > 500) {
                // 显示确认对话框
                if (!confirm(`您确定要向 ${recipientName} 转账 ${amountVal.toFixed(2)}元吗？这是一笔大额转账，请再次确认。`)) {
                    console.log("用户取消了大额转账");
                    return;
                }
            }

            try {
                console.log(`准备向 ${recipientId} 转账 ${amountVal} 元, 备注: ${notesVal}`);
                
                // 确保API.payment.transfer存在并正确定义
                if (typeof API.payment.transfer !== 'function') {
                    throw new Error('API.payment.transfer 函数未定义。');
                }
                
                // 显示处理中状态
                this.disabled = true;
                this.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> 处理中...';
                
                // 调用API转账函数
                const result = await API.payment.transfer(recipientId, amountVal, notesVal);
                console.log("转账结果:", result);
                
                window.showToast(`向 ${recipientName} 转账 ${amountVal.toFixed(2)} 元成功!`, 'success');
                
                // 刷新余额和交易记录
                loadBalance();
                loadTransactions();
                
                // 同时发送一条消息通知对方
                try {
                    const transferMessage = `我已向你转账 ¥${amountVal.toFixed(2)}。${notesVal ? `备注: ${notesVal}` : ''}`;
                    await API.message.send(recipientId, transferMessage);
                    
                    // 如果聊天窗口打开，刷新消息
                    if (document.getElementById('chatWindow')?.style.display === 'flex' && currentChatFriendId === recipientId) {
                        loadAndDisplayMessages(recipientId, 'chatWindowMessages', false);
                    }
                    if (document.getElementById('messages')?.classList.contains('active') && currentChatFriendId === recipientId) {
                        loadAndDisplayMessages(recipientId, 'mainChatMessagesContainer', false);
                    }
                } catch (msgError) {
                    console.error("发送转账通知消息失败:", msgError);
                    // 转账成功但发送消息失败，不影响主流程
                }
                
                // 关闭模态框
                modal.hide();
                
            } catch (error) {
                console.error("转账失败:", error);
                window.showToast(`转账失败: ${error.message}`, 'danger');
            } finally {
                // 重置按钮状态
                this.disabled = false;
                this.innerHTML = '确认转账';
            }
        });
        
        // 显示模态窗口
        try {
            modal.show();
        } catch (error) {
            console.error("显示模态框失败:", error);
            window.showToast("无法显示转账窗口，请刷新页面重试", "danger");
        }
    }

    // 定义或替换 loadProfileInfo 函数
    async function loadProfileInfo() {
        console.log("加载个人信息开始");
        
        // 设置加载状态
        const profileElements = [
            'profileStudentId', 'profileName', 'profileDeptName', 'profileBirthDate',
            'profileIdCard', 'profileAddress', 'profileEmail', 'profilePhone',
            'profileBalance', 'profileBalanceDisplay', 'profileNameLarge', 'profileDeptNameLarge'
        ];
        
        profileElements.forEach(id => {
            const element = document.getElementById(id);
            if(element) element.textContent = '加载中...';
        });
        
        const avatarEl = document.getElementById('profileAvatar');
        if (avatarEl) {
            avatarEl.textContent = '?';
        }
        
        try {
            // 直接调用API获取学生信息，不依赖于缓存
            console.log("调用API.student.getInfo获取学生信息");
            const student = await API.student.getInfo();
            console.log("获取到的学生信息:", student);
            
            // 处理字段名可能的不一致
            const studentData = {
                studentId: student.student_id || student.studentId,
                name: student.name,
                deptId: student.dept_id || student.deptId,
                deptName: student.dept_name || student.deptName,
                birthDate: student.birth_date || student.birthDate,
                idCard: student.id_card || student.idCard,
                address: student.address,
                email: student.email,
                phone: student.phone,
                balance: student.balance
            };
            
            // 更新所有字段前检查DOM元素是否存在
            const updateElementText = (id, value, defaultValue = '-') => {
                const element = document.getElementById(id);
                if (element) {
                    element.textContent = value || defaultValue;
                } else {
                    console.warn(`找不到ID为 ${id} 的元素`);
                }
            };
            
            // 更新简单的文本字段
            updateElementText('profileStudentId', studentData.studentId);
            updateElementText('profileName', studentData.name);
            updateElementText('profileDeptName', studentData.deptName, '未设置');
            
            // 处理日期显示
            let birthDateDisplay = '-';
            if (studentData.birthDate) {
                try {
                    const birthDate = new Date(studentData.birthDate);
                    if (!isNaN(birthDate.getTime())) {
                        birthDateDisplay = birthDate.toLocaleDateString();
                    }
                } catch (e) {
                    console.error("日期格式化错误:", e);
                }
            }
            updateElementText('profileBirthDate', birthDateDisplay);
            
            updateElementText('profileIdCard', studentData.idCard);
            updateElementText('profileAddress', studentData.address);
            updateElementText('profileEmail', studentData.email);
            updateElementText('profilePhone', studentData.phone);
            
            // 更新余额显示
            const balance = parseFloat(studentData.balance || 0).toFixed(2);
            updateElementText('profileBalance', `¥ ${balance}`);
            updateElementText('profileBalanceDisplay', `¥ ${balance}`);
            
            // 更新左侧个人卡片
            updateElementText('profileNameLarge', studentData.name, '未知姓名');
            updateElementText('profileDeptNameLarge', studentData.deptName, '未知院系');
            
            // 设置头像
            const avatarEl = document.getElementById('profileAvatar');
            if (avatarEl && studentData.name) {
                avatarEl.textContent = studentData.name.charAt(0).toUpperCase();
                avatarEl.style.backgroundColor = getRandomColor(studentData.studentId || '');
            }
            
            // 初始化编辑表单
            initProfileEditForm(studentData);
            
            // 同时更新localStorage中的用户信息，确保用户院系信息也被更新
            if (studentData.deptId && studentData.deptName) {
                const userData = JSON.parse(localStorage.getItem('user') || '{}');
                userData.deptId = studentData.deptId;
                userData.deptName = studentData.deptName;
                localStorage.setItem('user', JSON.stringify(userData));
                console.log("用户院系信息已更新:", studentData.deptName);
            }
            
            // 加载院系数据用于编辑表单
            loadDepartments();
            console.log("个人信息加载完成");
            
        } catch (error) {
            console.error('获取个人信息失败:', error);
            window.showToast('获取个人信息失败: ' + error.message, 'danger');
            
            // 显示错误状态
            profileElements.forEach(id => {
                const element = document.getElementById(id);
                if(element) element.textContent = '加载失败';
            });
            
            // 添加重试按钮
            const profileContainer = document.querySelector('#profile .card-body');
            if (profileContainer) {
                const retryButton = document.createElement('div');
                retryButton.className = 'text-center mt-3';
                retryButton.innerHTML = `
                    <button id="retryLoadProfileBtn" class="btn btn-sm btn-primary">
                        <i class="bi bi-arrow-clockwise"></i> 重新加载个人信息
                    </button>
                `;
                profileContainer.appendChild(retryButton);
                
                document.getElementById('retryLoadProfileBtn')?.addEventListener('click', () => {
                    // 移除重试按钮
                    retryButton.remove();
                    // 重新加载
                    loadProfileInfo();
                });
            }
        }
    }
    
    // 初始化个人信息编辑表单
    function initProfileEditForm(studentData) {
        console.log("初始化个人信息编辑表单", studentData);
        
        // 先检查编辑表单是否存在
        const editForm = document.getElementById('editProfileForm');
        if (!editForm) {
            console.error("找不到编辑表单");
            return;
        }
        
        // 设置表单初始值
        const editName = document.getElementById('editName');
        if (editName) editName.value = studentData.name || '';
        
        // 出生日期需要格式化为YYYY-MM-DD格式
        const birthDateInput = document.getElementById('editBirthDate');
        if (birthDateInput) {
            if (studentData.birthDate) {
                try {
                    const birthDate = new Date(studentData.birthDate);
                    if (!isNaN(birthDate.getTime())) {
                        const year = birthDate.getFullYear();
                        const month = String(birthDate.getMonth() + 1).padStart(2, '0');
                        const day = String(birthDate.getDate()).padStart(2, '0');
                        birthDateInput.value = `${year}-${month}-${day}`;
                    }
                } catch (e) {
                    console.error("日期格式化错误:", e);
                    birthDateInput.value = '';
                }
            } else {
                birthDateInput.value = '';
            }
        }
        
        const editIdCard = document.getElementById('editIdCard');
        if (editIdCard) editIdCard.value = studentData.idCard || '';
        
        const editAddress = document.getElementById('editAddress');
        if (editAddress) editAddress.value = studentData.address || '';
        
        const editEmail = document.getElementById('editEmail');
        if (editEmail) editEmail.value = studentData.email || '';
        
        const editPhone = document.getElementById('editPhone');
        if (editPhone) editPhone.value = studentData.phone || '';
        
        // 清除之前的事件监听器
        const cloneForm = editForm.cloneNode(true);
        editForm.parentNode.replaceChild(cloneForm, editForm);
        
        // 注册表单提交事件
        cloneForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            console.log("提交个人信息表单");
            
            // 获取新的表单元素引用
            const editDeptId = document.getElementById('editDeptId');
            const editBirthDate = document.getElementById('editBirthDate');
            const editIdCard = document.getElementById('editIdCard');
            const editAddress = document.getElementById('editAddress');
            const editEmail = document.getElementById('editEmail');
            const editPhone = document.getElementById('editPhone');
            
            // 禁用提交按钮防止重复提交
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> 保存中...';
            }
            
            try {
                // 准备更新数据
                const updatedData = {
                    deptId: editDeptId ? editDeptId.value : '',
                    birthDate: editBirthDate ? editBirthDate.value : '',
                    idCard: editIdCard ? editIdCard.value : '',
                    address: editAddress ? editAddress.value : '',
                    email: editEmail ? editEmail.value : '',
                    phone: editPhone ? editPhone.value : ''
                };
                
                console.log('准备更新个人信息:', updatedData);
                
                // 调用API更新个人信息
                await API.student.updateInfo(updatedData);
                
                window.showToast('个人信息更新成功!', 'success');
                
                // 重新加载个人信息
                loadProfileInfo();
                
                // 切换回查看模式
                const profileInfoEdit = document.getElementById('profileInfoEdit');
                const profileInfoView = document.getElementById('profileInfoView');
                
                if (profileInfoEdit) profileInfoEdit.style.display = 'none';
                if (profileInfoView) profileInfoView.style.display = 'block';
                
            } catch (error) {
                console.error('更新个人信息失败:', error);
                window.showToast('更新个人信息失败: ' + error.message, 'danger');
            } finally {
                // 恢复提交按钮状态
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = '保存修改';
                }
            }
        });
        
        // 注册密码修改表单事件
        const changePasswordForm = document.getElementById('changePasswordForm');
        if (changePasswordForm) {
            // 清除之前的事件监听器
            const clonePasswordForm = changePasswordForm.cloneNode(true);
            changePasswordForm.parentNode.replaceChild(clonePasswordForm, changePasswordForm);
            
            clonePasswordForm.addEventListener('submit', async function(e) {
                e.preventDefault();
                console.log("提交密码修改表单");
                
                const currentPassword = document.getElementById('currentPassword').value;
                const newPassword = document.getElementById('newPassword').value;
                const confirmPassword = document.getElementById('confirmPassword').value;
                
                // 验证新密码
                if (newPassword.length < 6) {
                    window.showToast('新密码长度至少为6位', 'warning');
                    return;
                }
                
                if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/.test(newPassword)) {
                    window.showToast('新密码必须包含字母和数字', 'warning');
                    return;
                }
                
                if (newPassword !== confirmPassword) {
                    window.showToast('两次输入的密码不一致', 'warning');
                    return;
                }
                
                // 禁用提交按钮防止重复提交
                const submitBtn = this.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> 处理中...';
                }
                
                try {
                    // 调用API更改密码
                    await API.student.changePassword(currentPassword, newPassword);
                    
                    window.showToast('密码修改成功!', 'success');
                    
                    // 清空表单
                    this.reset();
                    
                } catch (error) {
                    console.error('修改密码失败:', error);
                    window.showToast('修改密码失败: ' + error.message, 'danger');
                } finally {
                    // 恢复提交按钮状态
                    if (submitBtn) {
                        submitBtn.disabled = false;
                        submitBtn.innerHTML = '更改密码';
                    }
                }
            });
        }
    }
    
    // 加载院系数据
    async function loadDepartments() {
        try {
            const deptSelect = document.getElementById('editDeptId');
            if (!deptSelect) {
                console.error("找不到院系选择下拉框");
                return;
            }
            
            // 清空现有选项
            deptSelect.innerHTML = '<option value="">-- 请选择院系 --</option>';
            
            // 获取当前用户信息
            const userData = JSON.parse(localStorage.getItem('user') || '{}');
            const currentDeptId = userData.deptId;
            
            // 获取所有院系
            const departments = await API.department.list();
            console.log('获取到院系数据:', departments);
            
            if (!departments || !departments.length) {
                console.warn('没有获取到院系数据');
                return;
            }
            
            // 添加院系选项
        departments.forEach(dept => {
                // 规范化字段名
                const deptId = dept.deptId || dept.dept_id;
                const deptName = dept.deptName || dept.dept_name;
                
            const option = document.createElement('option');
                option.value = deptId;
                option.textContent = deptName;
                
                // 设置当前院系为选中状态
                if (deptId === currentDeptId) {
                    option.selected = true;
                }
                
                deptSelect.appendChild(option);
            });
            
        } catch (error) {
            console.error('加载院系数据失败:', error);
            window.showToast('加载院系数据失败', 'danger');
        }
    }

    function getRandomColor(idSeed) { // Accept any seed
        const colors = ['#007bff', '#6f42c1', '#e83e8c', '#fd7e14', '#20c997', '#17a2b8', '#6610f2', '#28a745', '#dc3545', '#ffc107', '#0dcaf0', '#d63384'];
        let hash = 0;
        const strSeed = String(idSeed); // Ensure it's a string
        for (let i = 0; i < strSeed.length; i++) {
            hash = strSeed.charCodeAt(i) + ((hash << 5) - hash);
            hash = hash & hash; // Convert to 32bit integer
        }
        return colors[Math.abs(hash) % colors.length];
    }

    function escapeHTML(str) {
        if (typeof str !== 'string') return '';
        return str.replace(/[&<>"']/g, function (match) {
            return {
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                '"': '&quot;',
                "'": '&#39;'
            }[match];
        });
    }

    // Ensure showToast is globally available if HTML inline script also defines it
    if (!window.showToast) {
        window.showToast = function(message, type = 'info') { // success, info, warning, danger
            const toastContainer = document.querySelector('.toast-container');
            if (!toastContainer) { 
                alert(message); // Fallback
                return;
            }
            const toastId = 'toast-' + Date.now();
            const toastBgClass = {
                success: 'bg-success',
                info: 'bg-info',
                warning: 'bg-warning',
                danger: 'bg-danger'
            }[type] || 'bg-primary';

            const toastHTML = `
                <div id="${toastId}" class="toast align-items-center text-white ${toastBgClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                    <div class="d-flex">
                        <div class="toast-body">
                            ${escapeHTML(message)}
                        </div>
                        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                    </div>
                </div>
            `;
            toastContainer.insertAdjacentHTML('beforeend', toastHTML);
            const toastElement = document.getElementById(toastId);
            if(toastElement){
                const toast = new bootstrap.Toast(toastElement, { delay: 5000 });
                toast.show();
                toastElement.addEventListener('hidden.bs.toast', () => toastElement.remove());
            }
        };
    }

    // 在文件末尾添加以下代码，使关键函数全局可用
    window.showTab = showTab;
    window.loadRecentContacts = loadRecentContacts;
    window.generateTestContacts = generateTestContacts;
    window.mockLoadRecentContacts = mockLoadRecentContacts;
    window.debugContactsDatabase = debugContactsDatabase;
    window.checkLoginStatus = checkLoginStatus;
    window.loadProfileInfo = loadProfileInfo;
    window.checkNewMessages = checkNewMessages;
}); 