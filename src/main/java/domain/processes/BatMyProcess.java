package domain.processes;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import domain.Action;
import domain.Unit;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.intellij.lang.batch.runner.BatchConfigurationType;
import org.intellij.lang.batch.runner.BatchRunConfiguration;
import org.intellij.lang.batch.runner.BatchRunner;
import stub.Util;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.List;

public class BatMyProcess extends MyProcess {

    private final static Icon myIcon = IconLoader.getIcon("/fileTypes/batch.png");

    private String batName;

    public BatMyProcess(Unit unit, String name, String pattern, boolean isMocked, String batName) {
        super(unit, name, pattern, isMocked, MockCallbackSpike.DO_NOTHING);
        this.batName = batName;
    }

    public String getBatName() {
        return batName;
    }

    public void setBatName(String batName) {
        this.batName = batName;
    }

    @Override
    public List<Action> getActions() {
        return Arrays.asList(Action.START_BAT,Action.STOP,Action.REFRESH);
    }

    @Override
    public void doMock(boolean targetMocked) {
        Util.notifyError("Can't Mock","You can't mock bat-only process");
    }

    @Override
    public Icon getIcon() {
        return myIcon;
    }

    @Override
    void doStart(Project project) throws Exception {
        Util.notifyError("Cannot run this configuration as application", "Actually this action should not be rendered at all, contact Platform team");
    }

    @Override
    void doBatStart(Project project) {
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
            Util.notifyError("Failed to execute process", ExceptionUtils.getFullStackTrace(e));
        }
    }

    @Override
    void doDebugStart(Project project) throws Exception {
        doStart(project);
    }

    @Override
    protected String getExecName() {
        return "cmd.exe";
    }
}
