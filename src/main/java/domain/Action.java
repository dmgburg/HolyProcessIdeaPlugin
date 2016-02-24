package domain;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public enum  Action {
    START("start", AllIcons.Actions.Execute),
    START_DEBUG("debug", AllIcons.Actions.StartDebugger),
    START_BAT("startBat", IconLoader.findIcon("/fileTypes/batch.png")),
    STOP("stop",AllIcons.Actions.Suspend),
    MOCK("mock",IconLoader.findIcon("/icons/jesterhat.png")),
    UNMOCK("unmock",IconLoader.findIcon("/icons/graduation_hat.png")),
    REFRESH("refresh",AllIcons.Actions.Refresh);

    private final String name;
    private final Icon icon;

    Action(String name, Icon icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return icon;
    }
}
