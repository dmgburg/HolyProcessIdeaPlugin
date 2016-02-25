package domain.processes;


import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import domain.Action;
import domain.Unit;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jutils.jprocesses.JProcesses;
import org.jutils.jprocesses.model.ProcessInfo;
import stub.Util;

import javax.swing.Icon;
import java.awt.Color;
import java.util.List;
import java.util.regex.Pattern;

public abstract class MyProcess {
    private int id = 0;
    private volatile boolean isMocked;
    private volatile boolean running;
    private Unit unit;
    private String pattern;
    private String name = "";
    private final MockCallbackSpike spike;

    public MyProcess(Unit unit, String name, String pattern, boolean isMocked, @Nullable MockCallbackSpike spike) {
        this.name = name;
        this.unit = unit;
        this.isMocked = isMocked;
        this.spike = spike;
        this.running = false;
        this.pattern = pattern;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getCaption() {
        StringBuilder sb = new StringBuilder(getName());

        if (isMocked()) {
            sb.append(" (Mocked)");
        }
        if (isRunning()) {
            sb.append(" PID=").append(getId());
        }
        return sb.toString();
    }

    public Unit getUnit() {
        return unit;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getId() {
        return Integer.toString(id);
    }

    public int getIntId() {
        return id;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean isRunning) {
        running = isRunning;
    }

    public boolean isMocked() {
        return isMocked;
    }

    public void setMocked(boolean mocked) {
        isMocked = mocked;
        spike.postMock(mocked, this);
    }

    public abstract List<Action> getActions();

    public abstract void doMock(boolean targetMocked);

    public Color getColor() {
        Color color;
        if (!isRunning()) {
            color = JBColor.BLACK;
        } else if (isMocked()) {
            color = JBColor.BLUE;
        } else {
            color = JBColor.GREEN;
        }
        return color;
    }

    public void doAction(@NotNull Project project, @NotNull final Action action, @Nullable final Runnable callback) {
        switch (action) {
            case START:
            case START_DEBUG:
            case START_BAT:
                doRefresh();
                if (isRunning()) {
                    Util.notifyInfo("Process is already runnng", "PID=" + getId());
                    return;
                }
                try {
                    switch (action) {
                        case START:
                            doStart(project);
                            break;
                        case START_BAT:
                            doBatStart(project);
                            break;
                        case START_DEBUG:
                            doDebugStart(project);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown run action:" + action);
                    }
                    doRefresh();
                    if(callback !=null) {
                        callback.run();
                    }
                } catch (Exception e) {
                    Util.notifyError("Application bootstrap failed with an exception", ExceptionUtils.getFullStackTrace(e));
                }
                break;
            case STOP:
                new Task.Backgroundable(project, "Stopping Process") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        doStop(indicator);
                        indicator.setText("Refreshing process info");
                        doRefresh();
                        if(callback !=null) {
                            callback.run();
                        }
                    }
                }.queue();
                break;
            case REFRESH:
                new Task.Backgroundable(project, "Stopping Process") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setText("Getting process info");
                        doRefresh();
                        if(callback !=null) {
                            callback.run();
                        }
                    }
                }.queue();
                break;
            case MOCK:
                new Task.Backgroundable(project, "Mocking") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        doMock(true);
                        if(callback !=null) {
                            callback.run();
                        }
                    }
                }.queue();
                break;
            case UNMOCK:
                new Task.Backgroundable(project, "Unmocking") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        doMock(false);
                        if(callback !=null) {
                            callback.run();
                        }
                    }
                }.queue();
                break;
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    public abstract Icon getIcon();

    abstract void doStart(Project project) throws Exception;

    abstract void doBatStart(Project project) throws Exception;

    abstract void doDebugStart(Project project) throws Exception;

    private void doStop(ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        indicator.setText("Getting process info");
        doRefresh();
        if (isRunning()) {
            indicator.setText("Trying to stop process gracefully");
            JProcesses.killProcessGracefully(getIntId());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ProcessInfo info = JProcesses.getProcess(getIntId());
            if (info != null && Pattern.compile(pattern).matcher(info.getCommand()).matches()) {
                Util.notifyWarn("Process failed to stop gracefully",
                        "Process " + name + " failed to stop gracefully");
                JProcesses.killProcess(getIntId());
            }
        }
    }

    public void doRefresh() {
        ProcessInfo info = null;
        if (getIntId() == 0) {
            List<ProcessInfo> infos = JProcesses.getProcessList(getExecName());
            for (ProcessInfo it : infos) {
                if (Pattern.compile(pattern).matcher(it.getCommand()).matches()) {
                    info = it;
                    setId(Integer.valueOf(it.getPid()));
                    break;
                }
            }
        } else {
            info = JProcesses.getProcess(getIntId());
        }
        if (info != null && isSameProcess(info)) {
            setId(Integer.valueOf(info.getPid()));
            setRunning(true);
        } else {
            setRunning(false);
            setId(0);
        }
    }

    protected abstract String getExecName();

    private boolean isSameProcess(@Nullable ProcessInfo info) {
        return info != null && Pattern.compile(pattern).matcher(info.getCommand()).matches();
    }


    @Override
    public String toString() {
        return "MyProcess{" +
                "name='" + name + '\'' +
                '}';
    }
}
