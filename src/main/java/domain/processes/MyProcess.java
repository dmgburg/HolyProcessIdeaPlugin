package domain.processes;


import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import domain.Action;
import domain.Unit;
import domain.descriptors.MyProcessDescriptor;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jutils.jprocesses.JProcesses;
import org.jutils.jprocesses.model.ProcessInfo;
import stub.HolyProjectProcessesManager;

import javax.swing.Icon;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public abstract class MyProcess {
    protected final Project project;
    private int id = 0;
    private final AtomicBoolean isMocked;
    private final AtomicBoolean running;
    private Unit unit;
    private Pattern pattern;
    private String name;

    public MyProcess(Unit unit, String name, String pattern, boolean isMocked, Project project) {
        this.name = name;
        this.unit = unit;
        this.project = project;
        this.isMocked = new AtomicBoolean(isMocked);
        this.running = new AtomicBoolean(false);
        this.pattern = Pattern.compile(pattern);
    }

    public MyProcess(MyProcessDescriptor descriptor, Project project){
        this(descriptor.unit,descriptor.name,descriptor.pattern,descriptor.isMocked, project);
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

    public abstract MyProcessDescriptor getDescriptor();

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
        return running.get();
    }

    public void setRunning(boolean isRunning) {
        running.set(isRunning);
    }

    public boolean isMocked() {
        return isMocked.get();
    }

    public void setMocked(boolean mocked) {
        isMocked.set(mocked);
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

    public void doAction(final Action action) {
        switch (action) {
            case START:
            case START_DEBUG:
            case START_BAT:
                doRefresh();
                if (isRunning()) {
                    Notifications.Bus.notify(new Notification(HolyProjectProcessesManager.notificationsTopics, "Process is already runnng", "Process is already runnng, PID=" + getId(), NotificationType.INFORMATION));
                    return;
                }
                try {
                    switch (action) {
                        case START:
                            doStart();
                            break;
                        case START_BAT:
                            doBatStart();
                            break;
                        case START_DEBUG:
                            doDebugStart();
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown run action:" + action);
                    }
                } catch (Exception e) {
                    Notifications.Bus.notify(new Notification(HolyProjectProcessesManager.notificationsTopics,
                            "Application bootstrap failed with an exception",
                            "Application bootstrap failed with an exception: " + ExceptionUtils.getFullStackTrace(e),
                            NotificationType.ERROR));
                }
                break;
            case STOP:
                new Task.Backgroundable(project, "Stopping Process") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        doStop(indicator);
                    }
                }.queue();
                break;
            case REFRESH:
                new Task.Backgroundable(project, "Stopping Process") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setText("Getting process info");
                        doRefresh();
                    }
                }.queue();
                break;
            case MOCK:
                new Task.Backgroundable(project, "Mocking") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        doMock(true);
                    }
                }.queue();
                break;
            case UNMOCK:
                new Task.Backgroundable(project, "Unmocking") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        doMock(false);
                    }
                }.queue();
                break;
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    public abstract Icon getIcon();

    abstract void doStart() throws Exception;

    abstract void doBatStart() throws Exception;

    abstract void doDebugStart() throws Exception;

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
            if (info != null && pattern.matcher(info.getCommand()).matches()) {
                Notifications.Bus.notify(new Notification(HolyProjectProcessesManager.notificationsTopics,
                        "Process failed to stop gracefully",
                        "Process " + name + " failed to stop gracefully",
                        NotificationType.WARNING));
                JProcesses.killProcess(getIntId());
            }
        }
    }

    public void doRefresh() {
        ProcessInfo info = null;
        if (getIntId() == 0) {
            List<ProcessInfo> infos = JProcesses.getProcessList(getExecName());
            for (ProcessInfo it : infos) {
                if (pattern.matcher(it.getCommand()).matches()) {
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
        return info != null && pattern.matcher(info.getCommand()).matches();
    }


    @Override
    public String toString() {
        return "MyProcess{" +
                "name='" + name + '\'' +
                '}';
    }
}
