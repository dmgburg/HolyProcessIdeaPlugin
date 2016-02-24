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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import domain.Action;
import domain.Unit;
import domain.descriptors.CommonMyProcessDescriptor;
import org.apache.commons.lang.StringUtils;
import org.intellij.lang.batch.runner.BatchConfigurationType;
import org.intellij.lang.batch.runner.BatchRunConfiguration;
import org.intellij.lang.batch.runner.BatchRunner;
import stub.HolyProjectProcessesManager;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

public class CommonMyProcess extends MyProcess {
    private final Module module;
    private final String appName;
    private final String bootstrapClass;
    private final String contextFile;
    private final String workDir;
    private final String batName;
    private List<String> jvmOptions;


    public CommonMyProcess(Unit unit,
                           String name,
                           String pattern,
                           boolean isMocked,
                           Project project,
                           String bootstrapClass, List<String> jvmOptions,
                           Module module,
                           String appName,
                           String contextFile,
                           String workDir, String batName) {
        super(unit, name, pattern, isMocked, project);
        this.bootstrapClass = bootstrapClass;
        this.jvmOptions = jvmOptions;
        this.module = module;
        this.appName = appName;
        this.contextFile = contextFile;
        this.workDir = workDir;
        this.batName = batName;
    }

    public CommonMyProcess(CommonMyProcessDescriptor descriptor, Project project) {
        this(descriptor.unit,
                descriptor.name,
                descriptor.pattern,
                descriptor.isMocked,
                project,
                descriptor.bootstrapClass,
                descriptor.jvmOptions,
                HolyProjectProcessesManager.getFileModule(descriptor.batName,project),
                descriptor.appName,
                descriptor.contextFile,
                descriptor.workDir,
                descriptor.batName);
    }

    @Override
    public Icon getIcon() {
        return AllIcons.RunConfigurations.Application;
    }

    @Override
    void doStart() throws Exception {
        runInternal(new BasicProgramRunner());
    }

    @Override
    void doDebugStart() throws Exception {
        runInternal(new GenericDebuggerRunner());
    }

    @Override
    public List<Action> getActions() {
        List<Action> result = new ArrayList<>();
        if (isMocked()) {
            result.add(Action.UNMOCK);
        } else {
            result.add(Action.MOCK);
        }
        result.add(Action.START);
        result.add(Action.START_BAT);
        result.add(Action.START_DEBUG);
        result.add(Action.REFRESH);
        result.add(Action.STOP);
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

    private void runInternal(ProgramRunner runner) {
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
        configuration.setModule(module);
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
    void doBatStart() throws Exception {
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
    protected String getExecName() {
        return "java.exe";
    }
}
