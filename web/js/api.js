// API 客户端工具
const API = {
    // 通用请求方法
    async request(endpoint, method = 'GET', data = null) {
        const url = endpoint;
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
            const result = await response.json();
            
            if (!result.success) {
                throw new Error(result.message || '请求失败');
            }
            
            return result.data;
        } catch (error) {
            console.error('API请求错误:', error);
            throw error;
        }
    },
    
    // 学生API
    student: {
        login(studentId, password) {
            return API.request('/api/student/login', 'POST', { studentId, password });
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