package actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;

import static stub.Util.NOTIFICATIONS_TOPIC;

public class MyToggleAction extends ToggleAction {
    public static boolean selected;

    @Override
    public boolean isSelected(AnActionEvent e) {
        return selected;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        selected = state;
        Notifications.Bus.notify(new Notification(NOTIFICATIONS_TOPIC, "header", "New state is " + state, NotificationType.INFORMATION));
    }
}
