package ui;

import com.intellij.ide.util.treeView.IndexComparator;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeBuilder;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import stub.HolyProjectProcessesManager;
import stub.ProcessesDataKeys;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class HolyProjectPanel extends SimpleToolWindowPanel implements DataProvider {
    private Project myProject;
    private SimpleTreeBuilder myBuilder;
    private SimpleTree myTree;
    private ProcessesSimpleTreeStructure myStructure;

    public HolyProjectPanel(Project project){
        super(true,true);
        myProject = project;
        final ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar actionToolbar = actionManager.createActionToolbar("HolyProject Processes Toolbar",
                (DefaultActionGroup)actionManager
                        .getAction("HolyProject.ProcessesToolbar"),
                true);

        setToolbar(actionToolbar.getComponent());
        final DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode());
        myTree = new SimpleTree(model);
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(true);
        myTree.setCellRenderer(new NodeRenderer());
        myStructure = new ProcessesSimpleTreeStructure(project.getComponent(HolyProjectProcessesManager.class));
        myBuilder = new SimpleTreeBuilder( myTree, model, myStructure, IndexComparator.INSTANCE);
        Disposer.register(myProject, myBuilder);
        TreeUtil.installActions(myTree);
        setContent(ScrollPaneFactory.createScrollPane(myTree));

    }

    @Nullable
    public Object getData(@NonNls String dataId) {
        if (ProcessesDataKeys.PROCESSES_PROJECTS_TREE.is(dataId)) {
            return myTree;
        }
        return super.getData(dataId);
    }
}
