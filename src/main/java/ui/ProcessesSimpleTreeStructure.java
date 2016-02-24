package ui;

import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import domain.processes.MyProcess;
import domain.Action;
import stub.HolyProjectProcessesManager;

import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

public class ProcessesSimpleTreeStructure extends SimpleTreeStructure {
    private final RootNode myRoot;

    public ProcessesSimpleTreeStructure(HolyProjectProcessesManager processesManager) {
        this.myRoot = new RootNode(processesManager);
    }

    @Override
    public Object getRootElement() {
        return myRoot;
    }

    public class RootNode extends SimpleNode {
        private final HolyProjectProcessesManager processesManager;

        public RootNode(HolyProjectProcessesManager processesManager) {
            this.processesManager = processesManager;
        }

        @Override
        public SimpleNode[] getChildren() {
            List<SimpleNode> simpleNodes = new ArrayList<>();
            for (MyProcess process : processesManager.getProcesses()){
                simpleNodes.add(new ProcessNode(process));
            }
            return simpleNodes.toArray(new SimpleNode[simpleNodes.size()]);
        }
    }

    public class ProcessNode extends SimpleNode{
        private final MyProcess process;

        public ProcessNode(MyProcess process) {
            this.process = process;
            getNodeName(process);
            setIcon(process.getIcon());
            myColor = process.getColor();
        }

        private void getNodeName(MyProcess process) {
            myName = process.getCaption();
        }

        @Override
        public SimpleNode[] getChildren() {
            List<ActionNode> nodes = new ArrayList<>();
            for(Action action : process.getActions()){
                nodes.add(new ActionNode(action, process));
            }
            return nodes.toArray(new SimpleNode[nodes.size()]);
        }
    }

    public class ActionNode extends SimpleNode{
        private final Action action;
        private final MyProcess process;

        public ActionNode(Action action, MyProcess process) {
            this.action = action;
            this.process = process;
            myName = action.getName();
            setIcon(action.getIcon());
        }

        @Override
        public SimpleNode[] getChildren() {
            return null;
        }

        @Override
        public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
            process.doAction(action);
        }


    }
}
