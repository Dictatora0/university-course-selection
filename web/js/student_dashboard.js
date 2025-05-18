document.addEventListener('DOMContentLoaded', function() {
    console.log("学生仪表盘初始化开始...");
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
    }

    function setupEventListeners() {
        // Tab切换
        document.querySelectorAll('.sidebar .nav-link, [data-tab-target]').forEach(link => {
            link.addEventListener('click', function(e) {
                e.preventDefault();
                const tabId = this.dataset.tab || this.dataset.tabTarget;
                showTab(tabId);
            });
        });

        // 退出登录
        document.getElementById('logoutBtn').addEventListener('click', logout);

        // 钱包相关
        document.getElementById('depositBtn')?.addEventListener('click', () => togglePaymentForm('depositForm', true));
        document.getElementById('withdrawBtn')?.addEventListener('click', () => togglePaymentForm('withdrawForm', true));
        document.getElementById('cancelDepositBtn')?.addEventListener('click', () => togglePaymentForm('depositForm', false));
        document.getElementById('cancelWithdrawBtn')?.addEventListener('click', () => togglePaymentForm('withdrawForm', false));
        document.getElementById('confirmDepositBtn')?.addEventListener('click', handleDeposit);
        document.getElementById('confirmWithdrawBtn')?.addEventListener('click', handleWithdraw);

        // 好友页面相关
        document.getElementById('addFriendForm')?.addEventListener('submit', handleAddFriend);
        document.getElementById('searchStudentBtn')?.addEventListener('click', handleSearchStudents);
        document.getElementById('friendSearchInput')?.addEventListener('input', filterFriendList);
        
        // 浮动聊天窗口相关
        document.getElementById('closeChatWindowBtn')?.addEventListener('click', () => {
            const chatWindow = document.getElementById('chatWindow');
            if(chatWindow) chatWindow.style.display = 'none';
        });
        document.getElementById('chatWindowMessageForm')?.addEventListener('submit', handleChatWindowSendMessage);
        document.getElementById('chatWindowTransferBtn')?.addEventListener('click', handleChatWindowTransfer);
        
        // 主消息页面相关
        document.getElementById('mainMessageForm')?.addEventListener('submit', handleMainSendMessage);
        document.getElementById('mainChatTransferBtn')?.addEventListener('click', handleMainChatTransfer);
    }

    function showTab(tabId) {
        document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
        document.querySelectorAll('.sidebar .nav-link').forEach(link => link.classList.remove('active'));

        const activeTabContent = document.getElementById(tabId);
        const activeSidebarLink = document.querySelector(`.sidebar .nav-link[data-tab="${tabId}"]`);

        if (activeTabContent) activeTabContent.classList.add('active');
        if (activeSidebarLink) activeSidebarLink.classList.add('active');
        
        switch (tabId) {
            case 'dashboardMain':
                loadMainPageData();
                break;
            case 'courses':
                loadAllCourses();
                loadDepartmentsForFilter();
                break;
            case 'my-courses':
                loadEnrolledCourses();
                break;
            case 'payment':
                loadBalance();
                loadTransactions();
                break;
            case 'friends':
                loadFriendsAndRecommendations();
                break;
            case 'messages':
                loadRecentContacts();
                // Clear main chat area when switching to messages tab initially
                const mainChatContainer = document.getElementById('mainChatMessagesContainer');
                if(mainChatContainer) mainChatContainer.innerHTML = '<p class="text-center my-auto text-muted">选择一个联系人开始聊天</p>';
                document.getElementById('chattingWithName').textContent = '...';
                document.getElementById('mainMessageInput').disabled = true;
                document.getElementById('mainSendMessageBtn').disabled = true;
                document.getElementById('mainChatTransferBtn').disabled = true;
                break;    
            case 'profile':
                loadProfileInfo();
                break;
        }
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

    async function loadAllCourses() {
        try {
            const allCourses = await API.course.list();
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
        try {
            const enrolledCourses = await API.enrollment.list();
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
        tbody.innerHTML = '';
        
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

    function togglePaymentForm(formId, show) {
        const form = document.getElementById(formId);
        const otherFormId = formId === 'depositForm' ? 'withdrawForm' : 'depositForm';
        const otherForm = document.getElementById(otherFormId);

        if (form) form.style.display = show ? 'block' : 'none';
        if (show && otherForm) otherForm.style.display = 'none';
    }

    async function handleDeposit() {
        const amountInput = document.getElementById('depositAmountInput');
        const amount = parseFloat(amountInput.value);
        if (isNaN(amount) || amount <= 0) {
            window.showToast('请输入有效的充值金额', 'danger');
            return;
        }
        try {
            await API.payment.deposit(amount);
            window.showToast('充值成功!', 'success');
            loadBalance();
            loadTransactions();
            amountInput.value = '';
            togglePaymentForm('depositForm', false);
        } catch (error) {
            window.showToast('充值失败: ' + error.message, 'danger');
        }
    }

    async function handleWithdraw() {
        const amountInput = document.getElementById('withdrawAmountInput');
        const amount = parseFloat(amountInput.value);
        if (isNaN(amount) || amount <= 0) {
            window.showToast('请输入有效的提现金额', 'danger');
            return;
        }
        try {
            await API.payment.withdraw(amount);
            window.showToast('提现成功!', 'success');
            loadBalance();
            loadTransactions();
            amountInput.value = '';
            togglePaymentForm('withdrawForm', false);
        } catch (error) {
            window.showToast('提现失败: ' + error.message, 'danger');
        }
    }

    async function loadBalance() {
        try {
            const studentInfo = await API.student.getInfo();
            const balance = parseFloat(studentInfo.balance || 0).toFixed(2);
            const balanceAmountEl = document.getElementById('balanceAmount');
            const profileBalanceEl = document.getElementById('profileBalance');
            const mainBalanceAmountEl = document.getElementById('mainBalanceAmount');

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
        try {
            const transactions = await API.transaction.list();
            const listElement = document.getElementById('transactionList');
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
            
            transactions.forEach(t => {
                const item = document.createElement('li');
                item.className = 'list-group-item d-flex justify-content-between align-items-center';
                const date = new Date(t.transactionDate).toLocaleString();
                
                // 修正判断逻辑：根据交易类型和角色确定是收入还是支出
                let isPositive = t.type === 'DEPOSIT' || t.type === 'REFUND';
                
                // 对于转账类型，需要根据转入/转出方向来判断
                if (t.type === 'TRANSFER') {
                    // 如果当前用户是接收方，则显示为正数（收入）
                    if (t.toStudentId === currentUserId) {
                        isPositive = true;
                    } 
                    // 如果当前用户是发送方，则显示为负数（支出）
                    else if (t.fromStudentId === currentUserId) {
                        isPositive = false;
                    }
                    
                    console.log(`转账记录: ${t.fromStudentId} -> ${t.toStudentId}, 当前用户: ${currentUserId}, 显示正数: ${isPositive}`);
                }
                
                const amountClass = isPositive ? 'text-success' : 'text-danger';
                const sign = isPositive ? '+' : '-';
                
                // 构建描述文本，为转账提供更明确的方向说明
                let description = t.description || '无描述';
                if (t.type === 'TRANSFER') {
                    if (t.toStudentId === currentUserId) {
                        description = `来自 ${t.fromStudentName || t.fromStudentId || '未知'} 的转账${description ? ': ' + description : ''}`;
                    } else if (t.fromStudentId === currentUserId) {
                        description = `转账给 ${t.toStudentName || t.toStudentId || '未知'}${description ? ': ' + description : ''}`;
                    }
                }
                
                item.innerHTML = `
                <div>
                        <strong class="d-block">${t.type}</strong>
                        <small class="text-muted">${description}</small>
                </div>
                    <div>
                        <span class="${amountClass} fw-bold me-2">${sign}${parseFloat(t.amount).toFixed(2)}</span>
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
        loadFriends();
        loadFriendRecommendations();
    }

    async function loadFriends() {
        try {
            const friendsData = await API.friendship.list(); 
            console.log("获取好友列表数据:", friendsData);
            
            const container = document.getElementById('friendListContainer');
            if (!container) {
                console.error("好友列表容器 'friendListContainer' 未找到。");
                return;
            }
            container.innerHTML = ''; 

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
        if (!friendId) {
            window.showToast('好友ID不能为空', 'warning');
            return;
        }
        
        try {
            console.log(`尝试添加好友 ID: ${friendId}`);
            await API.friendship.add(friendId);
            window.showToast('好友请求已发送!', 'success');
            console.log("好友请求发送成功，重新加载好友列表");
            // 刷新好友列表，显示新发送的请求
            loadFriends(); 
        } catch (error) {
            console.error(`添加好友失败 (ID: ${friendId}):`, error);
            window.showToast(`添加好友失败: ${error.message}`, 'danger');
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
                        <button class="btn btn-sm btn-outline-primary" onclick="window.addFriendById('${studentId}')"><i class="bi bi-person-plus"></i> 添加</button>
                    </div>
                `;
                resultsList.appendChild(item);
            });
        } catch (error) {
            console.error("搜索学生失败:", error);
            window.showToast(`搜索学生失败: ${error.message}`, 'danger');
            const resultsList = document.getElementById('searchResultsList');
            if(resultsList) resultsList.innerHTML = '<li class="list-group-item text-danger">搜索失败</li>';
        }
    }

    async function loadFriendRecommendations() {
        try {
            const recommendations = await API.friendship.recommendations('all', 5); 
            const listElement = document.getElementById('recommendedFriendsList');
            if(!listElement) return;
            listElement.innerHTML = '';
        if (recommendations.length === 0) {
                listElement.innerHTML = '<li class="list-group-item text-muted text-center">暂无好友推荐</li>';
            return;
        }
        recommendations.forEach(friend => {
            const item = document.createElement('li');
                item.className = 'list-group-item recommendation-item d-flex align-items-center';
                const avatarLetter = (friend.name || 'R').charAt(0).toUpperCase();
                const avatarBgColor = getRandomColor(friend.studentId || Math.random().toString());
            item.innerHTML = `
                    <div class="friend-avatar" style="background-color: ${avatarBgColor};">${avatarLetter}</div>
                    <div class="recommendation-info flex-grow-1">
                        <strong>${friend.name} (${friend.studentId})</strong>
                        <small class="d-block text-muted">院系: ${friend.deptName || '未知'}</small>
                        ${friend.recommendReason ? `<small class="text-success d-block fst-italic">${friend.recommendReason}</small>` : ''}
                    </div>
                    <div class="recommendation-actions ms-auto">
                        <button class="btn btn-sm btn-outline-primary" onclick="window.addFriendById('${friend.studentId}')"><i class="bi bi-person-plus"></i> 添加</button>
                </div>
                `;
                listElement.appendChild(item);
            });
        } catch (error) {
            console.error('加载好友推荐失败:', error);
            const listElement = document.getElementById('recommendedFriendsList');
            if(listElement) listElement.innerHTML = '<li class="list-group-item text-danger text-center">加载推荐失败</li>';
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

    let currentChatFriendId = null;
    let currentChatFriendName = null;

    async function loadRecentContacts() {
        const listElement = document.getElementById('recentContactsList');
        if(!listElement) return;
        listElement.innerHTML = '<p class="list-group-item text-muted text-center">加载中...</p>'; // Loading state
        try {
            console.log("尝试获取最近联系人列表...");
            const contacts = await API.message.getRecentContacts();
            console.log("获取到的联系人:", contacts);
            
            if (!contacts || contacts.length === 0) {
                listElement.innerHTML = '<p class="list-group-item text-muted text-center">没有最近联系人</p>';
                return;
            }
            
            listElement.innerHTML = '';
            contacts.forEach(contact => {
                const item = document.createElement('a');
                item.href = '#';
                item.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center';
                const avatarLetter = (contact.name || 'C').charAt(0).toUpperCase();
                const avatarBgColor = getRandomColor(contact.studentId || Math.random().toString());
                
                // 格式化最后一条消息时间
                let timeDisplay = '';
                if (contact.lastMessageTime) {
                    const msgDate = new Date(contact.lastMessageTime);
                    const now = new Date();
                    if (msgDate.toDateString() === now.toDateString()) {
                        // 如果是今天，则显示时间
                        timeDisplay = msgDate.toLocaleTimeString('zh-CN', {hour: '2-digit', minute:'2-digit'});
                    } else {
                        // 否则显示日期
                        timeDisplay = msgDate.toLocaleDateString('zh-CN', {month: 'numeric', day: 'numeric'});
                    }
                }
                
                item.innerHTML = `
                    <div class="d-flex align-items-center">
                        <div class="friend-avatar me-2" style="background-color: ${avatarBgColor}; width: 40px; height: 40px; font-size: 0.9rem;">${avatarLetter}</div>
                        <div class="flex-grow-1">
                            <strong class="d-block">${contact.name}</strong>
                            <small class="d-block text-muted text-truncate" style="max-width: 150px;">${contact.lastMessage ? contact.lastMessage : '暂无消息'}</small>
                        </div>
                    </div>
                    <div class="d-flex flex-column align-items-end">
                        <small class="text-muted">${timeDisplay}</small>
                        ${contact.unreadCount > 0 ? `<span class="badge bg-danger rounded-pill mt-1">${contact.unreadCount}</span>` : ''}
                    </div>
                `;
                item.onclick = (e) => {
                    e.preventDefault();
                    openMainChat(contact.studentId, contact.name);
                };
                listElement.appendChild(item);
            });
        } catch (error) {
            console.error('加载最近联系人失败:', error);
            listElement.innerHTML = '<p class="list-group-item text-danger text-center">加载联系人失败</p>';
            // 如果API调用失败，则使用模拟数据作为备用
            mockLoadRecentContacts();
        }
    }
    
    function mockLoadRecentContacts() { 
        const contacts = [
            { studentId: '2024001', name: '张三', lastMessage: '你好啊！最近怎么样?', unreadCount: 2 },
            { studentId: '2024002', name: '李四', lastMessage: '晚上有空一起吃饭吗？', unreadCount: 0 },
            { studentId: '2024003', name: '王五', lastMessage: 'OK, 没问题', unreadCount: 5 },
        ];
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
    }

    function openMainChat(friendId, friendName) {
        currentChatFriendId = friendId;
        currentChatFriendName = friendName;

        const chattingWithNameEl = document.getElementById('chattingWithName');
        const mainMessageInputEl = document.getElementById('mainMessageInput');
        const mainSendMessageBtnEl = document.getElementById('mainSendMessageBtn');
        const mainChatTransferBtnEl = document.getElementById('mainChatTransferBtn');

        if(chattingWithNameEl) chattingWithNameEl.textContent = friendName;
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
            const response = await API.message.getConversation(friendId);
            container.innerHTML = ''; 
            
            // 修正: 从响应中提取消息数组
            // 检查响应格式并正确提取消息数组
            const messages = response.data || response;
            
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
        }
    }

    function appendMessageToContainer(message, currentUserId, containerElement) {
        const messageDiv = document.createElement('div');
        const isSent = message.fromStudentId === currentUserId;
        messageDiv.className = `chat-message ${isSent ? 'message-sent' : 'message-received'}`;
        
        let sendTime = '未知时间';
        if (message.sendTime) {
            const date = new Date(message.sendTime);
            sendTime = `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
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
            try {
                const sentMessage = await API.message.send(currentChatFriendId, content);
                appendMessageToContainer(sentMessage, user.studentId, document.getElementById('chatWindowMessages'));
                document.getElementById('chatWindowMessages').scrollTop = document.getElementById('chatWindowMessages').scrollHeight;
                input.value = '';
                
                if (document.getElementById('messages').classList.contains('active') && 
                    document.getElementById('chattingWithName')?.textContent === currentChatFriendName) {
                    appendMessageToContainer(sentMessage, user.studentId, document.getElementById('mainChatMessagesContainer'));
                     document.getElementById('mainChatMessagesContainer').scrollTop = document.getElementById('mainChatMessagesContainer').scrollHeight;
                }
            } catch (error) {
                window.showToast(`发送消息失败: ${error.message}`, 'danger');
            }
        }
    }
    
    async function handleMainSendMessage(event) {
        event.preventDefault();
        const input = document.getElementById('mainMessageInput');
         if(!input || !currentChatFriendId) return;
        const content = input.value.trim();

        if (content) {
            try {
                const sentMessage = await API.message.send(currentChatFriendId, content);
                appendMessageToContainer(sentMessage, user.studentId, document.getElementById('mainChatMessagesContainer'));
                document.getElementById('mainChatMessagesContainer').scrollTop = document.getElementById('mainChatMessagesContainer').scrollHeight;
                input.value = '';

                if (document.getElementById('chatWindow')?.style.display === 'flex' && 
                    document.getElementById('chatWindowFriendName')?.textContent.includes(currentChatFriendName)) {
                     appendMessageToContainer(sentMessage, user.studentId, document.getElementById('chatWindowMessages'));
                     document.getElementById('chatWindowMessages').scrollTop = document.getElementById('chatWindowMessages').scrollHeight;
                }
            } catch (error) {
                window.showToast(`发送消息失败: ${error.message}`, 'danger');
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
        const recipientNameEl = document.getElementById('transferRecipientName');
        const recipientIdEl = document.getElementById('transferRecipientId');
        const amountInputEl = document.getElementById('transferAmountInput_modal');
        const notesEl = document.getElementById('transferNotes');

        if(recipientNameEl) recipientNameEl.textContent = recipientName;
        if(recipientIdEl) recipientIdEl.value = recipientId;
        if(amountInputEl) amountInputEl.value = '';
        if(notesEl) notesEl.value = '';
        
        const modalElement = document.getElementById('transferModal');
        if(!modalElement) return;
        const modal = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
        
        // 正确获取模态框中的确认按钮
        const confirmBtn = document.getElementById('confirmTransferBtn_modal');
        // Clone and replace to remove old listeners if any were attached directly
        const newConfirmBtn = confirmBtn.cloneNode(true); 
        confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);

        // 确保我们为新的按钮添加事件监听器
        newConfirmBtn.addEventListener('click', async function confirmTransferHandler(event) {
            event.preventDefault(); // 防止表单默认提交
            console.log("确认转账按钮被点击");
            
            const amountVal = parseFloat(document.getElementById('transferAmountInput_modal').value);
            const notesVal = document.getElementById('transferNotes').value.trim();

            if (isNaN(amountVal) || amountVal <= 0) {
                window.showToast('请输入有效的转账金额', 'warning');
                return;
            }
            try {
                console.log(`准备向 ${recipientId} 转账 ${amountVal} 元, 备注: ${notesVal}`);
                 // 确保API.payment.transfer存在并正确定义
                if (typeof API.payment.transfer !== 'function') {
                    throw new Error('API.payment.transfer 函数未定义。');
                }
                await API.payment.transfer(recipientId, amountVal, notesVal); 
                window.showToast(`向 ${recipientName} 转账 ${amountVal.toFixed(2)} 元成功!`, 'success');
                loadBalance(); 
                loadTransactions(); 
                
                const transferMessage = `我已向你转账 ¥${amountVal.toFixed(2)}。${notesVal ? `备注: ${notesVal}` : ''}`;
                await API.message.send(recipientId, transferMessage);
                
                if (document.getElementById('chatWindow')?.style.display === 'flex' && currentChatFriendId === recipientId) {
                     loadAndDisplayMessages(recipientId, 'chatWindowMessages', false);
                }
                if (document.getElementById('messages')?.classList.contains('active') && currentChatFriendId === recipientId) {
                    loadAndDisplayMessages(recipientId, 'mainChatMessagesContainer', false);
                }
                modal.hide();
            } catch (error) {
                console.error("转账失败:", error);
                window.showToast(`转账失败: ${error.message}`, 'danger');
            }
        });
        modal.show();
    }

    async function loadProfileInfo() {
        try {
            const student = await API.student.getInfo();
            const profileStudentIdEl = document.getElementById('profileStudentId');
            const profileNameEl = document.getElementById('profileName');
            const profileDeptNameEl = document.getElementById('profileDeptName');
            const profileBirthDateEl = document.getElementById('profileBirthDate');
            const profileIdCardEl = document.getElementById('profileIdCard');
            const profileAddressEl = document.getElementById('profileAddress');

            if(profileStudentIdEl) profileStudentIdEl.textContent = student.studentId;
            if(profileNameEl) profileNameEl.textContent = student.name;
            if(profileDeptNameEl) profileDeptNameEl.textContent = student.deptName || '未设置';
            if(profileBirthDateEl) profileBirthDateEl.textContent = student.birthDate ? new Date(student.birthDate).toLocaleDateString() : '-';
            if(profileIdCardEl) profileIdCardEl.textContent = student.idCard || '-';
            if(profileAddressEl) profileAddressEl.textContent = student.address || '-';
            loadBalance(); 
            
            // 同时更新initUI中的用户信息，确保用户院系信息也被更新到localStorage中
            if(student.deptId && student.deptName) {
                const userData = JSON.parse(localStorage.getItem('user') || '{}');
                userData.deptId = student.deptId;
                userData.deptName = student.deptName;
                localStorage.setItem('user', JSON.stringify(userData));
                console.log("用户院系信息已更新:", student.deptName);
            }
        } catch (error) {
            console.error('获取个人信息失败:', error);
            window.showToast('获取个人信息失败', 'danger');
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
}); 