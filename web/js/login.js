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
            
            // 模拟登录功能（由于没有后端API，这里做前端模拟）
            // 检查是否是预设的测试账号
            if ((studentId === '2021001' || studentId === '2021002' || 
                 studentId === '2021003' || studentId === '2021004' || 
                 studentId === '2021005') && password === '123456') {
                
                // 保存用户信息到本地存储
                const user = {
                    studentId: studentId,
                    name: studentId === '2021001' ? '张三' : 
                          studentId === '2021002' ? '李四' :
                          studentId === '2021003' ? '王五' :
                          studentId === '2021004' ? '赵六' : '钱七'
                };
                
                localStorage.setItem('user', JSON.stringify(user));
                localStorage.setItem('userType', 'student');
                
                // 记录登录成功信息到控制台
                console.log('学生登录成功:', user);
                
                // 重定向到学生主页
                window.location.href = 'student_dashboard.html';
            } else {
                alert('账号或密码错误！请使用测试账号 (2021001-2021005) 和密码 (123456)');
            }
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
            
            // 模拟管理员登录
            if (adminId === 'admin' && password === 'admin123') {
                const admin = {
                    adminId: 'admin',
                    name: '系统管理员'
                };
                
                localStorage.setItem('user', JSON.stringify(admin));
                localStorage.setItem('userType', 'admin');
                
                // 记录登录成功信息到控制台
                console.log('管理员登录成功:', admin);
                
                alert('管理员登录功能尚未实现，请使用学生账号登录');
            } else {
                alert('管理员账号或密码错误！请使用账号 admin 和密码 admin123');
            }
        });
    }
}); 