package stub;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.util.DisposeAwareRunnable;

public class Util {
    public static final String NOTIFICATIONS_TOPIC = "HolyProjectProcesses";

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
}
