// API 客户端工具
const API = {
    // 通用请求方法
    async request(endpoint, method = 'GET', data = null) {
        // 添加上下文路径
        const contextPath = window.location.pathname.split('/')[1] ? '/' + window.location.pathname.split('/')[1] : '';
        const url = contextPath + endpoint;
        
        console.log(`[API] 发送请求: ${method} ${url}`);
        if (data) {
            console.log(`[API] 请求数据:`, data);
        }
        
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include' // 包含会话Cookie
        };
        
        if (data) {
            options.body = JSON.stringify(data);
        }
        
        try {
            const response = await fetch(url, options);
            console.log(`[API] 收到响应状态: ${response.status} ${response.statusText}`);
            
            // 检查HTTP状态
            if (!response.ok) {
                console.error(`[API] HTTP错误: ${response.status} ${response.statusText}`);
                
                // 尝试解析错误响应
                try {
                    const errorData = await response.json();
                    console.error(`[API] 错误详情:`, errorData);
                    throw new Error(errorData.message || `服务器返回错误: ${response.status}`);
                } catch (jsonError) {
                    throw new Error(`请求失败: ${response.status} ${response.statusText}`);
                }
            }
            
            const result = await response.json();
            console.log(`[API] 响应数据:`, result);
            
            if (!result.success) {
                console.error(`[API] 业务逻辑错误:`, result.message);
                throw new Error(result.message || '请求失败');
            }
            
            return result.data;
        } catch (error) {
            console.error('[API] 请求错误:', error);
            throw error;
        }
    },
    
    // 学生API
    student: {
        login(studentId, password) {
            return API.request('/api/student/login', 'POST', { studentId, password });
        },
        register(studentId, name, password) {
            // 确保所有字段都不为空
            if (!studentId || !name || !password) {
                return Promise.reject(new Error('学号、姓名和密码不能为空'));
            }
            
            return API.request('/api/student/register', 'POST', { 
                studentId, 
                name, 
                password,
                birthDate: null,
                idCard: null,
                address: null
            });
        },
        getInfo() {
            return API.request('/api/student/getInfo');
        },
        logout() {
            return API.request('/api/student/logout');
        }
    },
    
    // 课程API
    course: {
        list() {
            return API.request('/api/course/list');
        },
        getByDept(deptId) {
            return API.request(`/api/course/getByDept?deptId=${deptId}`);
        }
    },
    
    // 选课API
    enrollment: {
        list() {
            return API.request('/api/enrollment/list');
        },
        add(courseId) {
            return API.request('/api/enrollment/add', 'POST', { courseId });
        },
        drop(courseId) {
            return API.request(`/api/enrollment/drop?courseId=${courseId}`);
        }
    }
}; 