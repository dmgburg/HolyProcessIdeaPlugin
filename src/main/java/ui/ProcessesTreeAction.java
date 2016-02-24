package ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import stub.ProcessesDataKeys;

import javax.swing.JTree;

public abstract class ProcessesTreeAction extends AnAction {

    protected static JTree getTree(AnActionEvent e){
        return ProcessesDataKeys.PROCESSES_PROJECTS_TREE.getData(e.getDataContext());
    }

    public static class ExpandAll extends AnAction {
        @Override
        public void actionPerformed(AnActionEvent e) {
            JTree tree = getTree(e);
            if (tree == null) return;

            for (int i = 0; i < tree.getRowCount(); i++) {
                tree.expandRow(i);
            }
        }
    }

    public static class CollapseAll extends AnAction {
        public void actionPerformed(@NotNull AnActionEvent e) {
            JTree tree = getTree(e);
            if (tree == null) return;

            int row = tree.getRowCount() - 1;
            while (row >= 0) {
                tree.collapseRow(row);
                row--;
            }
        }
    }

}
