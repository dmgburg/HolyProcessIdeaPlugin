package domain.processes;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.BasicProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import domain.Action;
import domain.Unit;
import org.apache.commons.lang.StringUtils;
import org.intellij.lang.batch.runner.BatchConfigurationType;
import org.intellij.lang.batch.runner.BatchRunConfiguration;
import org.intellij.lang.batch.runner.BatchRunner;
import stub.Util;

import javax.swing.Icon;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CommonMyProcess extends MyProcess {
    private String appName;
    private String bootstrapClass;
    private String contextFile;
    private String workDir;
    private String batName;
    private List<String> jvmOptions;

    public CommonMyProcess(Unit unit,
                           String name,
                           String pattern,
                           boolean isMocked,
                           String bootstrapClass, List<String> jvmOptions,
                           String appName,
                           String contextFile,
                           String workDir,
                           String batName,
                           MockCallbackSpike spike) {
        super(unit, name, pattern, isMocked, spike);
        this.bootstrapClass = bootstrapClass;
        this.jvmOptions = jvmOptions;
        this.appName = appName;
        this.contextFile = contextFile;
        this.workDir = workDir;
        this.batName = batName;
    }

    @Override
    public Icon getIcon() {
        return AllIcons.RunConfigurations.Application;
    }

    @Override
    void doStart(Project project) throws Exception {
        runInternal(new BasicProgramRunner(), project);
    }

    @Override
    void doDebugStart(Project project) throws Exception {
        runInternal(new GenericDebuggerRunner(), project);
    }

    @Override
    void doBatStart(Project project) throws Exception {
        BatchRunConfiguration conf = (BatchRunConfiguration) new BatchConfigurationType().getConfigurationFactories()[0].createTemplateConfiguration(project);
        conf.setScriptName(batName);
        Executor runExecutor = DefaultRunExecutor.getRunExecutorInstance();
        BatchRunner runner = new BatchRunner();
        ExecutionEnvironment env = ExecutionEnvironmentBuilder.create(runExecutor, conf)
                .runner(runner)
                .build();
        try {
            runner.execute(env);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Action> getActions() {
        List<Action> result = new ArrayList<>();
        result.add(Action.START);
        result.add(Action.START_BAT);
        result.add(Action.START_DEBUG);
        result.add(Action.STOP);
        if (isMocked()) {
            result.add(Action.UNMOCK);
        } else {
            result.add(Action.MOCK);
        }
        result.add(Action.REFRESH);
        return result;
    }

    @Override
    public void doMock(boolean targetMocked) {
        doRefresh();
        if (isRunning()) {
            PopupUtil.showBalloonForActiveComponent("Cannot mock/unmock running process", MessageType.ERROR);
            return;
        }
        setMocked(targetMocked);
    }

    private void runInternal(ProgramRunner runner, Project project) {
        ApplicationConfiguration configuration = (ApplicationConfiguration) ApplicationConfigurationType
                .getInstance()
                .getConfigurationFactories()[0]
                .createTemplateConfiguration(project);
        configuration.setMainClassName(bootstrapClass);
        String context = contextFile;
        if (isMocked()) {
            context = contextFile.replace("-context", "-mocked-context");
        }
        configuration.setProgramParameters(appName + " " + context);
        configuration.setVMParameters(StringUtils.join(jvmOptions, " "));
        configuration.setModule(Util.getFileModule(LocalFileSystem.getInstance().findFileByIoFile(new File(batName)), project));
        configuration.setWorkingDirectory(workDir);
        Executor runExecutor = DefaultRunExecutor.getRunExecutorInstance();

        ExecutionEnvironment env = ExecutionEnvironmentBuilder.create(runExecutor, configuration)
                .runner(runner)
                .build();
        try {
            runner.execute(env);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected String getExecName() {
        return "java.exe";
    }
}
