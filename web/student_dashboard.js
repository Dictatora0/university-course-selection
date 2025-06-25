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
        } catch (error) {
            console.error("初始化加载数据时出错:", error);
            alert("初始化数据加载失败，请尝试刷新页面或检查网络连接");
        }
    }, 500); // 延迟500毫秒以确保DOM完全加载
}

function showTab(tabId) {
    console.log(`显示Tab: ${tabId}`);
    // 修正Tab ID映射关系
    if (tabId === 'wallet') tabId = 'payment'; // wallet -> payment
    if (tabId === 'my-courses') tabId = 'myCourses'; // my-courses -> myCourses
    
    // 隐藏所有标签页
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // 显示选中的标签页
    const targetTab = document.getElementById(tabId);
    if (targetTab) {
        targetTab.classList.add('active');
        
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
                        <p>与 ... 聊天中</p>
                        <p class="small">选择一个联系人开始聊天</p>
                    </div>
                `;
            }
            
            // 启动消息轮询
            startMessagePolling();
        } else if (tabId === 'payment') {
            loadBalance();
            loadTransactions();
        } else if (tabId === 'profile') {
            loadProfileInfo();
        } else if (tabId === 'myCourses') {
            loadEnrolledCourses();
        }
        
        // 更新导航链接的 active 状态
        document.querySelectorAll('.main-nav-link').forEach(link => {
            const linkTarget = link.getAttribute('data-target');
            // 处理特殊情况
            let mappedLinkTarget = linkTarget;
            if (linkTarget === 'wallet') mappedLinkTarget = 'payment';
            if (linkTarget === 'my-courses') mappedLinkTarget = 'myCourses';
            
            if (mappedLinkTarget === tabId) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
        
        // 保存当前选中的标签页到 localStorage
        localStorage.setItem('currentTab', tabId);
    } else {
        console.error(`未找到ID为 ${tabId} 的Tab元素`);
    }
}

// 在文件末尾添加此代码，确保暴露关键函数到全局作用域
window.showTab = showTab; // 使showTab函数全局可用 