package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.exception.ExceptionUtils;
import stub.HolyProjectProcessesManager;
import stub.Util;

public class RefreshProcesses extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        try {

            Project project = e.getData(DataKeys.PROJECT);
            if (project == null) {
                throw new IllegalStateException("Project is null");
            }
            HolyProjectProcessesManager.getInstance(project).refreshProcessStatus();
        } catch (Exception exeption){
            Util.notifyError("Failed to refresh processes state",ExceptionUtils.getFullStackTrace(exeption));
        }
    }
}
