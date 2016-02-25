package stub;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.DisposeAwareRunnable;

public class Util {
    public static final String NOTIFICATIONS_TOPIC = "HolyProjectProcesses";

    public static Module getFileModule(VirtualFile file, Project project) {
        Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module == null && file.getParent() != null && !file.getParent().equals(file)) {
            module = getFileModule(file.getParent(), project);
        }
        return module;
    }

    public static void runWhenInitialized(final Project project, final Runnable r) {
        if (project.isDisposed()) return;

        if (!project.isInitialized()) {
            StartupManager.getInstance(project).registerPostStartupActivity(DisposeAwareRunnable.create(r, project));
            return;
        }

        runDumbAware(project, r);
    }

    public static void runDumbAware(final Project project, final Runnable r) {
        if (DumbService.isDumbAware(r)) {
            r.run();
        }
        else {
            DumbService.getInstance(project).runWhenSmart(DisposeAwareRunnable.create(r, project));
        }
    }

    public static void notifyInfo(String header, String content){
        notify(header,content,NotificationType.INFORMATION);
    }

    public static void notifyWarn(String header, String content){
        notify(header,content,NotificationType.WARNING);
    }

    public static void notifyError(String header, String content){
        notify(header,content,NotificationType.ERROR);
    }

    private static void notify(String header, String content, NotificationType type){
        Notifications.Bus.notify(new Notification(NOTIFICATIONS_TOPIC, header, content, type));
    }
}
