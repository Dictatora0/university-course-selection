document.addEventListener('DOMContentLoaded', function() {
    // 检查用户登录状态
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const userType = localStorage.getItem('userType');
    
    if (!user.studentId || userType !== 'student') {
        // 未登录或不是学生，重定向到登录页
        window.location.href = 'index.html';
        return;
    }
    
    // 设置用户信息
    document.getElementById('studentName').textContent = user.name;
    document.getElementById('welcomeName').textContent = user.name;
    
    // 设置当前日期
    const today = new Date();
    const options = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' };
    document.getElementById('currentDate').textContent = today.toLocaleDateString('zh-CN', options);
    
    // 页面切换功能
    const pages = ['mainContent', 'courseSelectionPage', 'gradesPage', 'friendsPage', 'messagesPage'];
    
    function showPage(pageId) {
        pages.forEach(id => {
            document.getElementById(id).classList.add('d-none');
        });
        document.getElementById(pageId).classList.remove('d-none');
        
        // 重置导航栏激活状态
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
        });
        
        // 设置对应导航项为激活状态
        if (pageId === 'mainContent') {
            document.querySelector('a[href="student_dashboard.html"]').classList.add('active');
        } else if (pageId === 'courseSelectionPage') {
            document.getElementById('courseSelectionNav').classList.add('active');
        } else if (pageId === 'gradesPage') {
            document.getElementById('gradesNav').classList.add('active');
        } else if (pageId === 'friendsPage') {
            document.getElementById('friendsNav').classList.add('active');
        } else if (pageId === 'messagesPage') {
            document.getElementById('messagesNav').classList.add('active');
        }
    }
    
    // 绑定导航事件
    document.getElementById('courseSelectionNav').addEventListener('click', () => showPage('courseSelectionPage'));
    document.getElementById('gradesNav').addEventListener('click', () => showPage('gradesPage'));
    document.getElementById('friendsNav').addEventListener('click', () => showPage('friendsPage'));
    document.getElementById('messagesNav').addEventListener('click', () => showPage('messagesPage'));
    document.getElementById('selectCourseBtn').addEventListener('click', () => showPage('courseSelectionPage'));
    
    // 退出登录
    document.getElementById('logoutBtn').addEventListener('click', function() {
        // 清除本地存储数据
        localStorage.removeItem('user');
        localStorage.removeItem('userType');
        
        // 重定向到登录页
        window.location.href = 'index.html';
    });
    
    // 加载学生已选课程
    loadEnrolledCourses();
    
    // 加载登录记录
    loadLoginRecords();
    
    // 加载好友推荐
    loadFriendRecommendations();
    
    // 加载所有课程（用于选课页面）
    loadAllCourses();
    
    // 加载学生成绩
    loadGrades();
    
    // 加载好友列表
    loadFriends();
    
    // 加载联系人列表
    loadContacts();
    
    // 加载院系列表（用于选课筛选）
    loadDepartments();
    
    // 函数：加载学生已选课程
    function loadEnrolledCourses() {
        // 模拟数据
        const enrolledCourses = [
            { courseId: 'CS101', courseName: '计算机导论', credit: 3, deptName: '计算机科学系', grade: 92 },
            { courseId: 'CS201', courseName: '数据结构', credit: 4, deptName: '计算机科学系', grade: 85 },
            { courseId: 'MATH101', courseName: '高等数学', credit: 4, deptName: '数学系', grade: 88 },
            { courseId: 'ENG101', courseName: '大学英语', credit: 3, deptName: '外语系', grade: null }
        ];
        
        const tbody = document.getElementById('enrolledCoursesList');
        tbody.innerHTML = '';
        
        if (enrolledCourses.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">暂无选课记录</td></tr>';
            return;
        }
        
        enrolledCourses.forEach(course => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${course.courseId}</td>
                <td>${course.courseName}</td>
                <td>${course.credit}</td>
                <td>${course.deptName}</td>
                <td>${course.grade ? course.grade : '暂无'}</td>
            `;
            tbody.appendChild(row);
        });
    }
    
    // 函数：加载登录记录
    function loadLoginRecords() {
        // 模拟数据
        const loginLogs = [
            { loginTime: new Date().toISOString(), ipAddress: '192.168.1.1' },
            { loginTime: new Date(Date.now() - 86400000).toISOString(), ipAddress: '192.168.1.1' },
            { loginTime: new Date(Date.now() - 172800000).toISOString(), ipAddress: '192.168.1.100' }
        ];
        
        const list = document.getElementById('loginRecordsList');
        list.innerHTML = '';
        
        if (loginLogs.length === 0) {
            list.innerHTML = '<li class="list-group-item">暂无登录记录</li>';
            return;
        }
        
        loginLogs.forEach(log => {
            const item = document.createElement('li');
            item.className = 'list-group-item';
            
            const logDate = new Date(log.loginTime);
            const formattedDate = `${logDate.toLocaleDateString()} ${logDate.toLocaleTimeString()}`;
            
            item.innerHTML = `
                <div class="d-flex justify-content-between align-items-center">
                    <small>${formattedDate}</small>
                    <span class="badge bg-info">${log.ipAddress}</span>
                </div>
            `;
            list.appendChild(item);
        });
    }
    
    // 函数：加载好友推荐
    function loadFriendRecommendations() {
        // 模拟数据
        const recommendations = [
            { studentId: '2021002', name: '李四' },
            { studentId: '2021003', name: '王五' },
            { studentId: '2021004', name: '赵六' }
        ];
        
        const list = document.getElementById('recommendedFriendsList');
        list.innerHTML = '';
        
        if (recommendations.length === 0) {
            list.innerHTML = '<li class="list-group-item">暂无推荐</li>';
            return;
        }
        
        recommendations.forEach(friend => {
            const item = document.createElement('li');
            item.className = 'list-group-item d-flex justify-content-between align-items-center';
            item.innerHTML = `
                <div>
                    <strong>${friend.name}</strong>
                    <br>
                    <small>学号: ${friend.studentId}</small>
                </div>
                <button class="btn btn-sm btn-outline-primary add-friend-btn" data-student-id="${friend.studentId}">
                    添加
                </button>
            `;
            list.appendChild(item);
        });
        
        // 绑定添加好友事件
        document.querySelectorAll('.add-friend-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const friendId = this.getAttribute('data-student-id');
                addFriend(friendId);
            });
        });
    }
    
    // 函数：加载所有课程
    function loadAllCourses() {
        // 模拟数据
        const allCourses = [
            { courseId: 'CS101', courseName: '计算机导论', credit: 3, deptName: '计算机科学系' },
            { courseId: 'CS201', courseName: '数据结构', credit: 4, deptName: '计算机科学系' },
            { courseId: 'CS301', courseName: '数据库系统', credit: 4, deptName: '计算机科学系' },
            { courseId: 'CS401', courseName: '操作系统', credit: 4, deptName: '计算机科学系' },
            { courseId: 'MATH101', courseName: '高等数学', credit: 4, deptName: '数学系' },
            { courseId: 'MATH201', courseName: '线性代数', credit: 3, deptName: '数学系' },
            { courseId: 'PHYS101', courseName: '大学物理', credit: 4, deptName: '物理系' },
            { courseId: 'ENG101', courseName: '大学英语', credit: 3, deptName: '外语系' }
        ];
        
        const tbody = document.getElementById('availableCoursesList');
        tbody.innerHTML = '';
        
        if (allCourses.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">暂无可选课程</td></tr>';
            return;
        }
        
        allCourses.forEach(course => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${course.courseId}</td>
                <td>${course.courseName}</td>
                <td>${course.credit}</td>
                <td>${course.deptName}</td>
                <td>
                    <button class="btn btn-sm btn-success enroll-btn" data-course-id="${course.courseId}">
                        选课
                    </button>
                </td>
            `;
            tbody.appendChild(row);
        });
        
        // 绑定选课事件
        document.querySelectorAll('.enroll-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const courseId = this.getAttribute('data-course-id');
                enrollCourse(courseId);
            });
        });
        
        // 绑定搜索和筛选事件
        document.getElementById('courseSearchInput').addEventListener('input', filterCourses);
        document.getElementById('deptFilter').addEventListener('change', filterCourses);
    }
    
    // 筛选课程
    function filterCourses() {
        const searchTerm = document.getElementById('courseSearchInput').value.toLowerCase();
        const deptFilter = document.getElementById('deptFilter').value;
        
        document.querySelectorAll('#availableCoursesList tr').forEach(row => {
            const courseId = row.cells[0].textContent.toLowerCase();
            const courseName = row.cells[1].textContent.toLowerCase();
            const deptName = row.cells[3].textContent;
            
            const matchesSearch = courseId.includes(searchTerm) || courseName.includes(searchTerm);
            const matchesDept = deptFilter === '' || deptName === deptFilter;
            
            row.style.display = matchesSearch && matchesDept ? '' : 'none';
        });
    }
    
    // 选课函数
    function enrollCourse(courseId) {
        alert(`你已成功选修课程 ${courseId}，请在"我的课程"中查看。`);
        
        // 重新加载已选课程
        loadEnrolledCourses();
        
        // 切换到主页面
        showPage('mainContent');
    }
    
    // 函数：加载学生成绩
    function loadGrades() {
        // 模拟数据
        const grades = [
            { courseId: 'CS101', courseName: '计算机导论', credit: 3, grade: 92, enrollTime: '2023-09-01' },
            { courseId: 'CS201', courseName: '数据结构', credit: 4, grade: 85, enrollTime: '2023-09-01' },
            { courseId: 'MATH101', courseName: '高等数学', credit: 4, grade: 88, enrollTime: '2023-09-01' }
        ];
        
        const tbody = document.getElementById('gradesList');
        tbody.innerHTML = '';
        
        if (grades.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">暂无成绩记录</td></tr>';
            document.getElementById('totalCredits').textContent = '0';
            document.getElementById('gpa').textContent = '0.00';
            return;
        }
        
        let totalCredits = 0;
        let totalGradePoints = 0;
        
        grades.forEach(course => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${course.courseId}</td>
                <td>${course.courseName}</td>
                <td>${course.credit}</td>
                <td>${course.grade}</td>
                <td>${course.enrollTime}</td>
            `;
            tbody.appendChild(row);
            
            // 计算总学分和GPA
            totalCredits += course.credit;
            
            // 简单GPA计算（假设满分100对应4.0）
            const gradePoint = course.grade >= 90 ? 4.0 :
                               course.grade >= 80 ? 3.0 :
                               course.grade >= 70 ? 2.0 :
                               course.grade >= 60 ? 1.0 : 0;
            
            totalGradePoints += gradePoint * course.credit;
        });
        
        // 更新学分和GPA
        document.getElementById('totalCredits').textContent = totalCredits;
        document.getElementById('gpa').textContent = (totalGradePoints / totalCredits).toFixed(2);
    }
    
    // 函数：加载好友列表
    function loadFriends() {
        // 模拟数据
        const friends = [
            { studentId: '2021002', name: '李四', department: '计算机科学系' }
        ];
        
        const list = document.getElementById('friendsList');
        list.innerHTML = '';
        
        if (friends.length === 0) {
            list.innerHTML = '<li class="list-group-item text-center">暂无好友</li>';
            return;
        }
        
        friends.forEach(friend => {
            const item = document.createElement('li');
            item.className = 'list-group-item d-flex justify-content-between align-items-center';
            item.innerHTML = `
                <div>
                    <strong>${friend.name}</strong>
                    <br>
                    <small>学号: ${friend.studentId} | ${friend.department}</small>
                </div>
                <div>
                    <button class="btn btn-sm btn-outline-primary me-2 chat-btn" 
                            data-student-id="${friend.studentId}" 
                            data-student-name="${friend.name}">
                        聊天
                    </button>
                    <button class="btn btn-sm btn-outline-danger remove-friend-btn" 
                            data-student-id="${friend.studentId}">
                        删除
                    </button>
                </div>
            `;
            list.appendChild(item);
        });
        
        // 加载好友推荐
        const recommendations = [
            { studentId: '2021003', name: '王五', department: '数学系' },
            { studentId: '2021004', name: '赵六', department: '物理系' },
            { studentId: '2021005', name: '钱七', department: '外语系' }
        ];
        
        const recommendList = document.getElementById('friendRecommendations');
        recommendList.innerHTML = '';
        
        if (recommendations.length === 0) {
            recommendList.innerHTML = '<li class="list-group-item text-center">暂无推荐</li>';
            return;
        }
        
        recommendations.forEach(friend => {
            const item = document.createElement('li');
            item.className = 'list-group-item d-flex justify-content-between align-items-center';
            item.innerHTML = `
                <div>
                    <strong>${friend.name}</strong>
                    <br>
                    <small>学号: ${friend.studentId} | ${friend.department}</small>
                </div>
                <button class="btn btn-sm btn-outline-primary add-friend-btn" data-student-id="${friend.studentId}">
                    添加
                </button>
            `;
            recommendList.appendChild(item);
        });
        
        // 绑定事件
        document.querySelectorAll('.chat-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const friendId = this.getAttribute('data-student-id');
                const friendName = this.getAttribute('data-student-name');
                openChat(friendId, friendName);
            });
        });
        
        document.querySelectorAll('.remove-friend-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const friendId = this.getAttribute('data-student-id');
                if (confirm(`确定要删除好友 ${friendId} 吗？`)) {
                    alert('好友删除成功');
                    loadFriends(); // 重新加载好友列表
                }
            });
        });
        
        document.querySelectorAll('.add-friend-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const friendId = this.getAttribute('data-student-id');
                addFriend(friendId);
            });
        });
        
        // 绑定搜索事件
        document.getElementById('friendSearchInput').addEventListener('input', function() {
            const searchTerm = this.value.toLowerCase();
            
            document.querySelectorAll('#friendsList li').forEach(item => {
                const friendName = item.querySelector('strong').textContent.toLowerCase();
                const friendId = item.querySelector('small').textContent.toLowerCase();
                
                if (friendName.includes(searchTerm) || friendId.includes(searchTerm)) {
                    item.style.display = '';
                } else {
                    item.style.display = 'none';
                }
            });
        });
        
        // 绑定添加好友表单提交事件
        document.getElementById('addFriendForm').addEventListener('submit', function(e) {
            e.preventDefault();
            
            const friendId = document.getElementById('friendId').value;
            addFriend(friendId);
            
            document.getElementById('friendId').value = '';
        });
    }
    
    // 函数：添加好友
    function addFriend(friendId) {
        // 简单验证
        if (friendId === user.studentId) {
            alert('不能添加自己为好友');
            return;
        }
        
        alert(`已成功添加学号为 ${friendId} 的学生为好友`);
        
        // 重新加载好友列表
        loadFriends();
    }
    
    // 函数：加载联系人列表
    function loadContacts() {
        // 模拟数据
        const contacts = [
            { studentId: '2021002', name: '李四', unreadCount: 2 }
        ];
        
        const list = document.getElementById('contactsList');
        list.innerHTML = '';
        
        if (contacts.length === 0) {
            list.innerHTML = '<li class="list-group-item text-center">暂无联系人</li>';
            return;
        }
        
        contacts.forEach(contact => {
            const item = document.createElement('li');
            item.className = 'list-group-item d-flex justify-content-between align-items-center contact-item';
            item.setAttribute('data-student-id', contact.studentId);
            item.setAttribute('data-student-name', contact.name);
            
            item.innerHTML = `
                <div>
                    <strong>${contact.name}</strong>
                    <small class="d-block text-muted">学号: ${contact.studentId}</small>
                </div>
                ${contact.unreadCount > 0 ? `<span class="badge rounded-pill bg-danger">${contact.unreadCount}</span>` : ''}
            `;
            list.appendChild(item);
            
            // 绑定点击事件
            item.addEventListener('click', function() {
                const friendId = this.getAttribute('data-student-id');
                const friendName = this.getAttribute('data-student-name');
                openChat(friendId, friendName);
                
                // 移除未读标记
                const badge = this.querySelector('.badge');
                if (badge) {
                    badge.remove();
                }
            });
        });
    }
    
    // 函数：打开聊天窗口
    function openChat(friendId, friendName) {
        // 切换到消息页面
        showPage('messagesPage');
        
        // 设置当前聊天对象名称
        document.getElementById('chatName').textContent = friendName;
        
        // 显示转账按钮
        document.getElementById('transferMoneyBtn').classList.remove('d-none');
        
        // 绑定转账按钮事件
        document.getElementById('transferMoneyBtn').querySelector('button').onclick = function() {
            openTransferModal(friendId, friendName);
        };
        
        // 加载消息记录
        loadChatMessages(friendId);
        
        // 标记消息为已读
        markMessagesAsRead(friendId);
        
        // 绑定发送消息事件
        const messageForm = document.getElementById('messageForm');
        messageForm.onsubmit = function(e) {
            e.preventDefault();
            
            const messageInput = document.getElementById('messageInput');
            const content = messageInput.value.trim();
            
            if (content) {
                sendMessage(friendId, content);
                messageInput.value = '';
            }
        };
    }
    
    // 函数：加载聊天消息
    function loadChatMessages(friendId) {
        // 模拟数据
        const messages = [
            { id: 1, fromId: user.studentId, toId: friendId, content: '你好，最近怎么样？', sendTime: '2023-10-15 14:30:00', isRead: true },
            { id: 2, fromId: friendId, toId: user.studentId, content: '我很好，谢谢关心！你呢？', sendTime: '2023-10-15 14:35:00', isRead: true },
            { id: 3, fromId: user.studentId, toId: friendId, content: '我也不错，正在准备期末考试。', sendTime: '2023-10-15 14:40:00', isRead: true },
            { id: 4, fromId: friendId, toId: user.studentId, content: '加油，一起努力！', sendTime: '2023-10-15 14:45:00', isRead: false }
        ];
        
        const chatContainer = document.getElementById('chatMessages');
        chatContainer.innerHTML = '';
        
        if (messages.length === 0) {
            chatContainer.innerHTML = '<div class="text-center text-muted my-3">暂无消息记录</div>';
            return;
        }
        
        messages.forEach(message => {
            const messageDiv = document.createElement('div');
            const isSent = message.fromId === user.studentId;
            
            messageDiv.className = isSent ? 'message-sent' : 'message-received';
            messageDiv.innerHTML = `
                <div>${message.content}</div>
                <small class="text-white-50">${message.sendTime}</small>
            `;
            
            chatContainer.appendChild(messageDiv);
        });
        
        // 滚动到底部
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }
    
    // 函数：发送消息
    function sendMessage(toId, content) {
        // 获取当前时间
        const now = new Date();
        const formattedTime = `${now.getFullYear()}-${(now.getMonth() + 1).toString().padStart(2, '0')}-${now.getDate().toString().padStart(2, '0')} ${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
        
        // 创建消息元素
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message-sent';
        messageDiv.innerHTML = `
            <div>${content}</div>
            <small class="text-white-50">${formattedTime}</small>
        `;
        
        // 添加到聊天容器
        const chatContainer = document.getElementById('chatMessages');
        chatContainer.appendChild(messageDiv);
        
        // 滚动到底部
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }
    
    // 函数：标记消息为已读
    function markMessagesAsRead(fromId) {
        // 在实际应用中，这里会发送请求到后端
        console.log(`标记来自 ${fromId} 的消息为已读`);
        
        // 更新联系人列表，移除未读标记
        document.querySelectorAll('.contact-item').forEach(item => {
            if (item.getAttribute('data-student-id') === fromId) {
                const badge = item.querySelector('.badge');
                if (badge) {
                    badge.remove();
                }
            }
        });
    }
    
    // 函数：打开转账模态框
    function openTransferModal(toId, toName) {
        // 更新模态框内容
        document.getElementById('transferRecipient').textContent = toName;
        document.getElementById('recipientId').value = toId;
        
        // 打开模态框
        const transferModal = new bootstrap.Modal(document.getElementById('transferModal'));
        transferModal.show();
        
        // 绑定转账表单提交事件
        document.getElementById('transferForm').onsubmit = function(e) {
            e.preventDefault();
            
            const amount = document.getElementById('transferAmount').value;
            if (amount && !isNaN(amount) && amount > 0) {
                transferMoney(toId, amount);
                transferModal.hide();
            } else {
                alert('请输入有效的转账金额');
            }
        };
    }
    
    // 函数：转账
    function transferMoney(toId, amount) {
        alert(`成功向学号为 ${toId} 的同学转账 ${amount} 元`);
        
        // 发送一条消息通知对方
        sendMessage(toId, `我已向你转账 ${amount} 元`);
    }
    
    // 函数：加载院系列表
    function loadDepartments() {
        // 模拟数据
        const departments = [
            { deptId: 'CS', deptName: '计算机科学系' },
            { deptId: 'MATH', deptName: '数学系' },
            { deptId: 'PHYS', deptName: '物理系' },
            { deptId: 'ENG', deptName: '外语系' }
        ];
        
        const select = document.getElementById('deptFilter');
        
        departments.forEach(dept => {
            const option = document.createElement('option');
            option.value = dept.deptName;
            option.textContent = dept.deptName;
            select.appendChild(option);
        });
    }
}); 