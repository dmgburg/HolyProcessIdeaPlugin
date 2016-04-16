package stub;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;
import ui.HolyProjectPanel;

public class HolyProjectProcessesManager extends AbstractProjectComponent {
    private final HolyProjectPanel panel;

    protected HolyProjectProcessesManager(@NotNull final Project project) {
        super(project);
        panel = new HolyProjectPanel(project);
    }

    @Override
    public void initComponent() {
        super.initComponent();
        Util.runWhenInitialized(myProject, new Runnable() {
            @Override
            public void run() {
                initToolWindow();
            }
        });
    }

    private void initToolWindow() {
        final ToolWindowManagerEx manager = ToolWindowManagerEx.getInstanceEx(myProject);
        ToolWindowEx myToolWindow = (ToolWindowEx) manager.registerToolWindow(HolyProjectPanel.ID, false, ToolWindowAnchor.RIGHT, myProject, true);
        myToolWindow.setIcon(IconLoader.findIcon("/icons/jesterhat.png"));
        final ContentFactory contentFactory = ServiceManager.getService(ContentFactory.class);
        final Content content = contentFactory.createContent(panel, "", false);
        ContentManager contentManager = myToolWindow.getContentManager();
        contentManager.addContent(content);
    }
}
