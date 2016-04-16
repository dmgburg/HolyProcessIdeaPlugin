package actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import static stub.Util.NOTIFICATIONS_TOPIC;

public class CommonAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Notifications.Bus.notify(new Notification(NOTIFICATIONS_TOPIC, "header", "Common action", NotificationType.INFORMATION));
    }
}
