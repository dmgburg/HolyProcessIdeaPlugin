package actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.exception.ExceptionUtils;
import stub.HolyProjectProcessesManager;

public class RefreshProcesses extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        try {

            Project project = e.getData(DataKeys.PROJECT);
            if (project == null) {
                throw new IllegalStateException("Project is null");
            }
            HolyProjectProcessesManager manager = project.getComponent(HolyProjectProcessesManager.class);
            manager.refreshProcessStatus();
        } catch (Exception exeption){
            Notifications.Bus.notify(new Notification(HolyProjectProcessesManager.notificationsTopics,
                    "Failed to refresh processes state",
                    "Failed to refresh processes state: " + ExceptionUtils.getFullStackTrace(exeption),
                    NotificationType.ERROR));
        }
    }
}
