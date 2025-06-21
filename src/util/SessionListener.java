package util;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.logging.Logger;

@WebListener
public class SessionListener implements HttpSessionListener {
    
    private static final Logger LOGGER = Logger.getLogger(SessionListener.class.getName());
    
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        LOGGER.info("会话创建: " + session.getId());
    }
    
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        LOGGER.info("会话销毁: " + session.getId());
    }
} 