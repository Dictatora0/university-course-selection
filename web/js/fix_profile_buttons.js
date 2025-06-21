/**
 * 专门修复个人信息页面按钮和选项卡的脚本
 * 这个脚本使用最直接的方法确保按钮和选项卡正常工作
 */

// 使用DOMContentLoaded和立即执行函数来隔离作用域
document.addEventListener('DOMContentLoaded', function() {
    console.log("个人信息页面修复脚本已加载");
    
    // 使用setTimeout确保在其他脚本之后执行
    setTimeout(function() {
        console.log("开始修复个人信息页面按钮和选项卡");
        
        // 1. 修复编辑个人信息按钮
        var editBtn = document.getElementById('editProfileBtn');
        if (editBtn) {
            console.log("找到编辑个人信息按钮，开始修复");
            
            // 使用最直接的方式替换按钮内容并添加onclick处理函数
            var newEditBtn = document.createElement('button');
            newEditBtn.className = editBtn.className;
            newEditBtn.id = editBtn.id;
            newEditBtn.innerHTML = editBtn.innerHTML;
            
            // 使用直接的onclick处理
            newEditBtn.onclick = function(e) {
                console.log("编辑个人信息按钮被点击（直接处理）");
                e.preventDefault();
                
                // 直接操作DOM显示/隐藏
                var profileInfoView = document.getElementById('profileInfoView');
                var profileInfoEdit = document.getElementById('profileInfoEdit');
                
                if (profileInfoView && profileInfoEdit) {
                    profileInfoView.style.display = 'none';
                    profileInfoEdit.style.display = 'block';
                } else {
                    console.error("找不到视图或编辑元素");
                }
                
                return false;
            };
            
            // 替换原始按钮
            if (editBtn.parentNode) {
                editBtn.parentNode.replaceChild(newEditBtn, editBtn);
                console.log("编辑个人信息按钮已替换");
            }
        } else {
            console.error("未找到编辑个人信息按钮");
        }
        
        // 2. 修复取消按钮
        var cancelBtn = document.getElementById('cancelEditBtn');
        if (cancelBtn) {
            console.log("找到取消编辑按钮，开始修复");
            
            // 使用最直接的方式替换按钮
            var newCancelBtn = document.createElement('button');
            newCancelBtn.className = cancelBtn.className;
            newCancelBtn.id = cancelBtn.id;
            newCancelBtn.type = 'button';
            newCancelBtn.innerHTML = cancelBtn.innerHTML;
            
            // 使用直接的onclick处理
            newCancelBtn.onclick = function(e) {
                console.log("取消编辑按钮被点击（直接处理）");
                e.preventDefault();
                
                // 直接操作DOM显示/隐藏
                var profileInfoView = document.getElementById('profileInfoView');
                var profileInfoEdit = document.getElementById('profileInfoEdit');
                
                if (profileInfoView && profileInfoEdit) {
                    profileInfoEdit.style.display = 'none';
                    profileInfoView.style.display = 'block';
                } else {
                    console.error("找不到视图或编辑元素");
                }
                
                return false;
            };
            
            // 替换原始按钮
            if (cancelBtn.parentNode) {
                cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);
                console.log("取消编辑按钮已替换");
            }
        } else {
            console.error("未找到取消编辑按钮");
        }
        
        // 3. 修复选项卡按钮
        var profileTabs = document.querySelectorAll('#profileTabs .nav-link');
        if (profileTabs.length > 0) {
            console.log("找到个人信息选项卡，开始修复");
            
            profileTabs.forEach(function(tab) {
                // 克隆并替换每个选项卡以移除所有事件监听器
                var newTab = tab.cloneNode(true);
                
                // 添加新的点击事件处理
                newTab.onclick = function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    
                    // 获取目标面板ID
                    var targetId = this.getAttribute('href');
                    console.log("选项卡被点击（直接处理）:", targetId);
                    
                    // 处理所有选项卡和面板
                    profileTabs.forEach(function(t) {
                        t.classList.remove('active');
                    });
                    
                    document.querySelectorAll('.tab-pane').forEach(function(pane) {
                        pane.classList.remove('show', 'active');
                    });
                    
                    // 激活当前选项卡和面板
                    this.classList.add('active');
                    
                    var targetPane = document.querySelector(targetId);
                    if (targetPane) {
                        targetPane.classList.add('show', 'active');
                    } else {
                        console.error("找不到目标面板:", targetId);
                    }
                    
                    return false;
                };
                
                // 替换原始选项卡
                if (tab.parentNode) {
                    tab.parentNode.replaceChild(newTab, tab);
                }
            });
            
            console.log("个人信息选项卡已修复");
        } else {
            console.error("未找到个人信息选项卡");
        }
        
        console.log("个人信息页面按钮和选项卡修复完成");
    }, 1000); // 等待1秒确保其他脚本已加载
}); 