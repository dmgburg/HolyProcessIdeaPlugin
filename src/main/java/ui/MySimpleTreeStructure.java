package ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.IndexComparator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.CachingSimpleNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeBuilder;
import com.intellij.ui.treeStructure.SimpleTreeStructure;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.util.Comparator;

public class MySimpleTreeStructure extends SimpleTreeStructure {
    private final RootNode myRoot;
    private final SimpleTreeBuilder myTreeBuilder;


    public MySimpleTreeStructure(Project project, JTree tree) {
        this.myRoot = new RootNode();
        myTreeBuilder = new SimpleTreeBuilder(tree, (DefaultTreeModel) tree.getModel(), this, new Comparator<SimpleNode>() {
            @Override
            public int compare(SimpleNode o1, SimpleNode o2) {
                if(o1 instanceof NamedNode && o2 instanceof NamedNode)
                   return o1.getName().compareTo(o2.getName());
                else
                    return IndexComparator.INSTANCE.compare(o1, o2);
            }
        });
        Disposer.register(project, myTreeBuilder);
        myTreeBuilder.initRoot().getRootNode();
        myTreeBuilder.expand(myRoot, null);
    }

    public void updateFromRoot(){
        myTreeBuilder.addSubtreeToUpdateByElement(myRoot);
    }

    @Override
    public Object getRootElement() {
        return myRoot;
    }

    public abstract class NamedNode extends CachingSimpleNode {

        protected NamedNode(SimpleNode aParent, String name) {
            super(aParent);
            myName = name;
        }
    }

    public class RootNode extends NamedNode {

        public RootNode() {
            super(null,"Root");
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return new SimpleNode[]{new Level1Node(this,"Level1Node1"), new Level1Node(this,"Level1Node2")};
        }
    }

    public class Level1Node extends NamedNode {

        public Level1Node(SimpleNode aParent, String name) {
            super(aParent, name);
            myClosedIcon = AllIcons.General.BalloonInformation;
            updatePresentation();
        }

        private void updatePresentation() {
            PresentationData presentation = getPresentation();
            presentation.clear();
            presentation.addText(myName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            presentation.addText(" Red", new SimpleTextAttributes(Font.PLAIN, Color.RED));
            presentation.addText(" Level1Node", SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
            update(presentation);
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return new SimpleNode[]{new Level2Node(this,"Level2Node1"), new Level2Node(this,"Level2Node2")};
        }
    }

    public class Level2Node extends NamedNode {
        private Color myColor = Color.RED;

        public Level2Node(SimpleNode aParent, String name) {
            super(aParent, name);
            myClosedIcon = AllIcons.General.BalloonWarning;
            updatePresentation();
        }

        private void updatePresentation() {
            PresentationData presentation = getPresentation();
            presentation.clear();
            presentation.addText(myName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            presentation.addText(" Red", new SimpleTextAttributes(Font.PLAIN, myColor));
            presentation.addText(" Level2Node", SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
        }

        @Override
        public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
            if(Color.RED.equals(myColor)){
                myColor = Color.BLUE;
            } else {
                myColor = Color.RED;
            }
            updatePresentation();
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return NO_CHILDREN;
        }
    }
}
