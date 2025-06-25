document.addEventListener('DOMContentLoaded', function() {
    // 学生登录表单提交处理
    const studentLoginForm = document.getElementById('studentLoginForm');
    if (studentLoginForm) {
        studentLoginForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const studentId = document.getElementById('studentId').value;
            const password = document.getElementById('studentPassword').value;
            
            // 简单的客户端验证
            if (!studentId || !password) {
                alert('请输入学号和密码');
                return;
            }
            
            // 格式化学号，确保以S开头
            let formattedStudentId = studentId;
            if (!formattedStudentId.startsWith('S')) {
                formattedStudentId = 'S' + formattedStudentId;
            }
            
            // 调用API登录
            fetch('/course-selection/api/students/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    student_id: formattedStudentId,
                    password: password
                }),
                credentials: 'include'
            })
            .then(response => {
                console.log('登录响应状态:', response.status, response.statusText);
                return response.json().catch(error => {
                    console.error('解析JSON失败:', error);
                    throw new Error('服务器响应格式错误');
                });
            })
            .then(data => {
                console.log('登录响应数据:', data);
                
                if (data.success) {
                    // 登录成功，跳转到主页
                    console.log('登录成功，准备跳转到学生仪表盘');
                    window.location.href = 'student_dashboard.html';
                } else {
                    // 登录失败，显示错误信息
                    console.error('登录失败:', data.message);
                    alert(data.message || '登录失败，请检查学号和密码');
                }
            })
            .catch(error => {
                console.error('登录请求发生错误:', error);
                alert('网络错误，请稍后再试');
            });
        });
    }
    
    // 管理员登录表单提交处理
    const adminLoginForm = document.getElementById('adminLoginForm');
    if (adminLoginForm) {
        adminLoginForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const adminId = document.getElementById('adminId').value;
            const password = document.getElementById('adminPassword').value;
            
            // 简单的客户端验证
            if (!adminId || !password) {
                alert('请输入管理员ID和密码');
                return;
            }
            
            // 调用API登录
            fetch('/course-selection/api/admin/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    adminId: adminId,
                    password: password
                }),
                credentials: 'include'
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    window.location.href = 'admin_dashboard.html';
                } else {
                    alert(data.message || '管理员账号或密码错误');
                }
            })
            .catch(error => {
                console.error('管理员登录失败:', error);
                alert('管理员登录功能尚未实现，请使用学生账号登录');
            });
        });
    }
}); 