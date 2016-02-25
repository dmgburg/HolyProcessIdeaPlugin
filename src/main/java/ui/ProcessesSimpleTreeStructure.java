package ui;

import com.intellij.ide.util.treeView.IndexComparator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeBuilder;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import domain.Action;
import domain.processes.MyProcess;
import stub.HolyProjectProcessesManager;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProcessesSimpleTreeStructure extends SimpleTreeStructure {
    private Project project;
    private final RootNode myRoot;
    private final SimpleTreeBuilder myTreeBuilder;


    public ProcessesSimpleTreeStructure(Project project, JTree tree) {
        this.myRoot = new RootNode();
        this.project = project;
        myTreeBuilder = new SimpleTreeBuilder(tree, (DefaultTreeModel) tree.getModel(), this, new Comparator<SimpleNode>() {
            @Override
            public int compare(SimpleNode o1, SimpleNode o2) {
                if(o1 instanceof ProcessNode && o2 instanceof ProcessNode)
                   return ((ProcessNode) o1).getProcess().getName().compareTo(((ProcessNode) o2).getProcess().getName());
                else
                    return IndexComparator.INSTANCE.compare(o1, o2);
            }
        });
        Disposer.register(project, myTreeBuilder);
        myTreeBuilder.initRoot();
        myTreeBuilder.expand(myRoot, null);
    }

    public void updateFromRoot(){
        myTreeBuilder.addSubtreeToUpdateByElement(myRoot);
    }

    @Override
    public Object getRootElement() {
        return myRoot;
    }

    public class RootNode extends SimpleNode {

        public RootNode() {
            super();
        }

        @Override
        public SimpleNode[] getChildren() {
            List<SimpleNode> simpleNodes = new ArrayList<>();
            for (MyProcess process : HolyProjectProcessesManager.getInstance(project).getProcesses()) {
                simpleNodes.add(new ProcessNode(process, this));
            }
            return simpleNodes.toArray(new SimpleNode[simpleNodes.size()]);
        }
    }

    public class ProcessNode extends SimpleNode {
        private final MyProcess process;

        public ProcessNode(MyProcess process, SimpleNode aParent) {
            super(project, aParent);
            this.process = process;
            setIcon(process.getIcon());
            updateNodeName(process);
        }

        private void updateNodeName(MyProcess process) {
            myName = process.getCaption();
        }

        public MyProcess getProcess() {
            return process;
        }

        @Override
        protected void doUpdate() {
            super.doUpdate();
            updateNodeName(process);
        }

        @Override
        public SimpleNode[] getChildren() {
            List<ActionNode> nodes = new ArrayList<>();
            for (Action action : process.getActions()) {
                nodes.add(new ActionNode(action, this));
            }
            return nodes.toArray(new SimpleNode[nodes.size()]);
        }
    }

    public class ActionNode extends SimpleNode {
        private final Action action;

        public ActionNode(Action action, ProcessNode parent) {
            super(parent);
            this.action = action;
            myName = action.getName();
            setIcon(action.getIcon());
        }

        @Override
        public SimpleNode[] getChildren() {
            return new SimpleNode[0];
        }

        @Override
        public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
            ((ProcessNode)getParent()).getProcess().doAction(project, action, new Runnable() {
                @Override
                public void run() {
                    myTreeBuilder.addSubtreeToUpdateByElement(getParent());
                }
            });
        }
    }
}
