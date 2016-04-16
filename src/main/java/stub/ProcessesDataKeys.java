package stub;

import com.intellij.openapi.actionSystem.DataKey;
import ui.MySimpleTreeStructure;

import javax.swing.JTree;

public class ProcessesDataKeys {
    public static final DataKey<JTree> PROCESSES_PROJECTS_TREE = DataKey.create("PROCESSES_PROJECTS_TREE");
    public static final DataKey<MySimpleTreeStructure> PROCESSES_PROJECTS_TREE_STRUCTURE = DataKey.create("PROCESSES_PROJECTS_TREE_STRUCTURE");
}
