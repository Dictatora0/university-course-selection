// API 客户端工具
const API = {
    baseUrl: '/course-selection/api',
    
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
        login: async function(studentId, password) {
            const response = await fetch(`${API.baseUrl}/students/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    student_id: studentId,
                    password: password
                }),
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        register: async function(studentData) {
            const response = await fetch(`${API.baseUrl}/students/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(studentData),
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        logout: async function() {
            const response = await fetch(`${API.baseUrl}/students/logout`, {
                method: 'POST',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        getInfo: async function() {
            const response = await fetch(`${API.baseUrl}/students/getInfo`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        // 更新学生个人信息
        updateInfo: async function(studentData) {
            console.log(`更新个人信息:`, studentData);
            const response = await fetch(`${API.baseUrl}/students/updateInfo`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(studentData),
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        // 修改密码
        changePassword: async function(currentPassword, newPassword) {
            console.log(`修改密码`);
            const response = await fetch(`${API.baseUrl}/students/changePassword`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    currentPassword: currentPassword,
                    newPassword: newPassword
                }),
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        }
    },
    
    // 课程API
    course: {
        list: async function() {
            const response = await fetch(`${API.baseUrl}/course/list`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        getByDept: async function(deptId) {
            const response = await fetch(`${API.baseUrl}/course/getByDept?deptId=${deptId}`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        }
    },
    
    // 选课API
    enrollment: {
        list: async function() {
            const response = await fetch(`${API.baseUrl}/enrollment/list`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        add: async function(courseId) {
            const response = await fetch(`${API.baseUrl}/enrollment/add`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    courseId: courseId
                }),
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        drop: async function(courseId) {
            const response = await fetch(`${API.baseUrl}/enrollment/drop?courseId=${courseId}`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        }
    },
    
    // 支付API
    payment: {
        deposit: async function(amount) {
            try {
                const response = await fetch(`${API.baseUrl}/payment/deposit`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        amount: amount
                    }),
                    credentials: 'include'
                });
                
                const data = await response.json();
                if (data.success) {
                    return { success: true, message: data.message };
                } else {
                    throw new Error(data.message || '充值失败');
                }
            } catch (error) {
                console.error('充值失败:', error);
                throw error;
            }
        },
        
        withdraw: async function(amount) {
            try {
                const response = await fetch(`${API.baseUrl}/payment/withdraw`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        amount: amount
                    }),
                    credentials: 'include'
                });
                
                const data = await response.json();
                if (data.success) {
                    return { success: true, message: data.message };
                } else {
                    throw new Error(data.message || '提现失败');
                }
            } catch (error) {
                console.error('提现失败:', error);
                throw error;
            }
        },
        
        transfer: async function(toStudentId, amount, description = '') {
            try {
                const response = await fetch(`${API.baseUrl}/payment/transfer`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        toStudentId: toStudentId,
                        amount: amount,
                        description: description
                    }),
                    credentials: 'include'
                });
                
                const data = await response.json();
                if (data.success) {
                    // 检查是否有频繁转账警告
                    if (data.warning && data.warningType === 'FREQUENT_TRANSFER') {
                        // 显示警告给用户但不阻止转账
                        const warningConfirmed = confirm(`${data.message}\n\n${data.details}\n\n点击确定继续完成转账，或取消终止操作。`);
                        
                        if (!warningConfirmed) {
                            throw new Error('用户取消了转账操作');
                        }
                    }
                    
                    return { success: true, message: data.message };
                } else {
                    throw new Error(data.message || '转账失败');
                }
            } catch (error) {
                console.error('转账失败:', error);
                throw error;
            }
        }
    },
    
    // 交易API
    transaction: {
        list: async function() {
            const response = await fetch(`${API.baseUrl}/transactions/list`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        create: async function(type, amount, description = '') {
            const response = await fetch(`${API.baseUrl}/transactions/create`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ type, amount, description }),
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        }
    },
    
    // 好友API
    friendship: {
        list: async function() {
            const response = await fetch(`${API.baseUrl}/friendship`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        add: async function(friendId) {
            const response = await fetch(`${API.baseUrl}/friendship/add`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    friendId: friendId
                }),
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        delete: async function(friendId) {
            const response = await fetch(`${API.baseUrl}/friendship/${friendId}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        check: async function(friendId) {
            const response = await fetch(`${API.baseUrl}/friendship/check/${friendId}`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        count: async function() {
            const response = await fetch(`${API.baseUrl}/friendship/count`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        requests: async function() {
            const response = await fetch(`${API.baseUrl}/friendship/requests`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        search: async function(keyword) {
            const response = await fetch(`${API.baseUrl}/friendship/search?keyword=${encodeURIComponent(keyword)}`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        accept: async function(requesterId) {
            const response = await fetch(`${API.baseUrl}/friendship/accept/${requesterId}`, {
                method: 'POST',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        reject: async function(requesterId) {
            const response = await fetch(`${API.baseUrl}/friendship/reject/${requesterId}`, {
                method: 'POST',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        received: async function() {
            const response = await fetch(`${API.baseUrl}/friendship/received`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        recommendations: async function(type = 'all', limit = 10) {
            let url = `${API.baseUrl}/friendship/recommendations`;
            
            // 构建查询参数
            const params = new URLSearchParams();
            if (type !== 'all') {
                params.append('type', type);
            }
            if (limit) {
                params.append('limit', limit);
            }
            
            // 添加查询参数到URL
            const queryString = params.toString();
            if (queryString) {
                url += `?${queryString}`;
            }
            
            console.log(`获取好友推荐，类型: ${type}, 限制: ${limit}`);
            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        }
    },
    
    // 消息API
    message: {
        // 获取最近联系人列表
        getRecentContacts: async () => {
            console.log('获取最近联系人列表');
            try {
                const response = await fetch(`${API.baseUrl}/message/recent_contacts`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    credentials: 'include'
                });
                
                if (!response.ok) {
                    const errorData = await response.text();
                    console.error('API错误:', errorData);
                    throw new Error(errorData || '获取最近联系人失败');
                }
                
                const result = await response.json();
                if (!result.success) {
                    throw new Error(result.message || '获取最近联系人失败');
                }
                
                return result.data;
            } catch (error) {
                console.error('获取最近联系人失败:', error);
                throw error;
            }
        },
        
        // 获取与特定用户的聊天记录
        getConversation: async (friendId) => {
            console.log(`获取与 ${friendId} 的聊天记录`);
            if (!friendId) {
                console.error('获取聊天记录失败: friendId未定义');
                throw new Error('好友ID不能为空');
            }
            
            try {
                const response = await fetch(`${API.baseUrl}/message/conversation/${friendId}`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    credentials: 'include'
                });
                
                if (!response.ok) {
                    const errorData = await response.text();
                    console.error('API错误:', errorData);
                    throw new Error(errorData || '获取聊天记录失败');
                }
                
                const result = await response.json();
                console.log("获取到的聊天记录原始数据:", result);
                
                // 处理不同的返回数据格式
                let messages = [];
                
                if (Array.isArray(result)) {
                    // 直接返回数组的情况
                    messages = result;
                } else if (result.data && Array.isArray(result.data)) {
                    // 包含在data字段中的情况
                    messages = result.data;
                } else if (result.success && result.data && Array.isArray(result.data)) {
                    // 标准API响应格式
                    messages = result.data;
                } else {
                    console.warn("无法识别的消息数据格式:", result);
                    // 尝试在对象中找到数组字段
                    for (const key in result) {
                        if (Array.isArray(result[key])) {
                            console.log(`找到可能的消息数组字段: ${key}`);
                            messages = result[key];
                            break;
                        }
                    }
                    
                    if (messages.length === 0) {
                        // 如果还是找不到，则返回空数组
                        console.error("无法提取消息数据，返回空数组");
                        return [];
                    }
                }
                
                // 规范化消息数据格式
                return messages.map(msg => {
                    return {
                        messageId: msg.messageId || msg.message_id || msg.id || 0,
                        fromStudentId: msg.fromStudentId || msg.from_student_id || msg.fromId || msg.from_id,
                        toStudentId: msg.toStudentId || msg.to_student_id || msg.toId || msg.to_id,
                        content: msg.content || msg.message || '',
                        sendTime: msg.sendTime || msg.send_time || msg.time || msg.createTime || msg.create_time || new Date().toISOString(),
                        read: msg.read || msg.isRead || msg.is_read || false
                    };
                });
            } catch (error) {
                console.error('获取聊天记录失败:', error);
                throw error;
            }
        },
        
        // 发送消息
        send: async (toId, content) => {
            console.log(`发送消息给 ${toId}: ${content}`);
            if (!toId) {
                console.error('发送消息失败: toId未定义');
                throw new Error('接收者ID不能为空');
            }
            
            if (!content || content.trim() === '') {
                console.error('发送消息失败: 内容为空');
                throw new Error('消息内容不能为空');
            }
            
            try {
                const response = await fetch(`${API.baseUrl}/message/send`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        toStudentId: toId,
                        content: content
                    }),
                    credentials: 'include'
                });
                
                if (!response.ok) {
                    const errorData = await response.text();
                    console.error('API错误:', errorData);
                    throw new Error(errorData || '发送消息失败');
                }
                
                const result = await response.json();
                console.log("发送消息API返回结果:", result);
                
                if (!result.success) {
                    throw new Error(result.message || '发送消息失败');
                }
                
                // 获取当前用户信息，用于构建消息对象
                const user = JSON.parse(localStorage.getItem('user') || '{}');
                
                // 构建一个标准消息对象返回，即使服务器没有返回完整信息
                return {
                    messageId: result.data?.messageId || Date.now(),
                    fromStudentId: user.studentId,
                    toStudentId: toId,
                    fromStudentName: user.name,
                    content: content,
                    sendTime: new Date(),
                    read: false
                };
            } catch (error) {
                console.error('发送消息失败:', error);
                throw error;
            }
        },
        
        unread: async function() {
            const response = await fetch(`${API.baseUrl}/message/unread`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        unreadCount: async function() {
            const response = await fetch(`${API.baseUrl}/message/unread_count`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        markAsRead: async function(messageId) {
            const response = await fetch(`${API.baseUrl}/message/read/${messageId}`, {
                method: 'PUT',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        delete: async function(messageId) {
            const response = await fetch(`${API.baseUrl}/message/delete/${messageId}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        }
    },
    
    // 帮助函数 - 处理API响应
    handleResponse: async function(response) {
        const responseData = await response.json();
        
        if (!response.ok || (responseData && !responseData.success)) {
            throw new Error(responseData.message || '请求失败');
        }
        
        return responseData.data;
    },
    
    // 院系API
    department: {
        // 获取所有院系
        list: async function() {
            console.log(`获取所有院系`);
            const response = await fetch(`${API.baseUrl}/department/list`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        },
        
        // 获取院系详情
        get: async function(deptId) {
            console.log(`获取院系信息: ${deptId}`);
            const response = await fetch(`${API.baseUrl}/department/${deptId}`, {
                method: 'GET',
                credentials: 'include'
            });
            
            return API.handleResponse(response);
        }
    }
}; 