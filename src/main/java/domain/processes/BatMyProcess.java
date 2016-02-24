package domain.processes;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import domain.Action;
import domain.Unit;
import domain.descriptors.BayMyProcessDescriptor;
import domain.descriptors.MyProcessDescriptor;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.intellij.lang.batch.runner.BatchConfigurationType;
import org.intellij.lang.batch.runner.BatchRunConfiguration;
import org.intellij.lang.batch.runner.BatchRunner;
import stub.HolyProjectProcessesManager;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.List;

public class BatMyProcess extends MyProcess {

    //TODO check out ScriptRunnerUtil and stuff
    private final String batName;
    private final static Icon myIcon = IconLoader.getIcon("/fileTypes/batch.png");

    public BatMyProcess(Unit unit, String name, String pattern, boolean isMocked, String batName, Project project) {
        super(unit, name, pattern, isMocked, project);
        this.batName = batName;
    }

    public  BatMyProcess(BayMyProcessDescriptor descriptor,Project project){
        this(descriptor.unit,
                descriptor.name,
                descriptor.pattern,
                descriptor.isMocked,
                descriptor.batName,
                project);
    }

    @Override
    public MyProcessDescriptor getDescriptor() {
        MyProcessDescriptor descriptor = new BayMyProcessDescriptor();

        return ;
    }

    @Override
    public List<Action> getActions() {
        return Arrays.asList(Action.START_BAT,Action.STOP,Action.REFRESH);
    }

    @Override
    public void doMock(boolean targetMocked) {
        Notifications.Bus.notify(new Notification(HolyProjectProcessesManager.notificationsTopics,
                "Can't Mock",
                "You can't mock bat-only process",
                NotificationType.ERROR));
    }

    @Override
    public Icon getIcon() {
        return myIcon;
    }

    @Override
    void doStart() throws Exception {
        Notifications.Bus.notify(new Notification(HolyProjectProcessesManager.notificationsTopics,
                "Cannot run this configuration as application",
                "Actually this action should not be rendered at all, contact Platform team", NotificationType.ERROR));
    }

    @Override
    void doBatStart() {
        BatchRunConfiguration conf =(BatchRunConfiguration) new BatchConfigurationType().getConfigurationFactories()[0].createTemplateConfiguration(project);
        conf.setScriptName(batName);
        Executor runExecutor = DefaultRunExecutor.getRunExecutorInstance();
        BatchRunner runner = new BatchRunner();
        ExecutionEnvironment env = ExecutionEnvironmentBuilder.create(runExecutor,conf)
                .runner(runner)
                .build();
        try {
            runner.execute(env);
        } catch (ExecutionException e) {
            Notifications.Bus.notify(new Notification(HolyProjectProcessesManager.notificationsTopics,
                    "Failed to execute process",
                    "Failed to execute process: " + ExceptionUtils.getFullStackTrace(e),
                    NotificationType.ERROR));
        }
    }

    @Override
    void doDebugStart() throws Exception {
        doStart();
    }

    @Override
    protected String getExecName() {
        return "cmd.exe";
    }
}
