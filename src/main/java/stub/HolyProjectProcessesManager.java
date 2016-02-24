package stub;

import com.google.common.collect.Lists;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import domain.processes.BatMyProcess;
import domain.processes.CommonMyProcess;
import domain.processes.MyProcess;
import domain.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.ProcessesState;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@State(name = "HolyProjectProcessesManager", storages = @Storage(file = "HolyProjectProcesses.xml"))
public class HolyProjectProcessesManager extends AbstractProjectComponent implements PersistentStateComponent<ProcessesState> {
    private final Pattern pattern = Pattern.compile("(.*\\\\(.*)\\\\.*\\\\target\\\\deploy-local)\\\\bin\\\\(.*).bat");
    private final Project project;
    public static String notificationsTopics = "HolyProjectProcesses";
    public CopyOnWriteArrayList<MyProcess> processes;
    final Unit unit1 = new Unit("unit1");

    protected HolyProjectProcessesManager(@NotNull final Project project) {
        super(project);
        this.project = project;
        this.processes = new CopyOnWriteArrayList<>();
    }

    public void refreshProcessStatus() {
        new Task.Backgroundable(project, "Refreshing Process") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                int i = 0;
                indicator.setFraction(0);
                indicator.setText("Getting processes info");
                for (MyProcess process : processes) {
                    process.doRefresh();
                    indicator.setFraction(i++ / processes.size());
                }
            }
        }.queue();

    }

    public void recreateProcesses() {
        try {
            processes.clear();
            final List<String> componentsNotFound = new ArrayList<>();
            String userprofile = System.getenv().get("USERPROFILE");
            final File configurationFolder = new File(userprofile + "/CONFIGURATION");
            final File secrets = new File(userprofile + "/SECRETS");
            if (!secrets.exists()) {
                FileUtil.createDirectory(secrets.getAbsoluteFile());
            }
            final Properties properties = parseConfiguration(new File(configurationFolder + "/processes.properties"));
            VirtualFile root = project.getBaseDir();
            VfsUtilCore.iterateChildrenRecursively(root, null, new ContentIterator() {
                @Override
                public boolean processFile(VirtualFile file) {
                    Matcher matcher = pattern.matcher(file.getPresentableUrl());
                    if (matcher.matches()) {
                        String processName = matcher.group(3);
                        String workDir = matcher.group(1);
                        String contextFile = properties.getProperty("hosts.localhost.processes." + processName + ".context");
                        String jvmOpts = properties.getProperty("hosts.localhost.processes." + processName + ".jvmOpts");
                        if (jvmOpts == null) {
                            componentsNotFound.add(processName);
                            return true;
                        }
                        CommonMyProcess appProcess = getAppProcess(file, processName, workDir, contextFile, jvmOpts, configurationFolder, secrets);
                        processes.add(appProcess);
                    }
                    return true;
                }
            });
            if (componentsNotFound.size() > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("The following processes not found in configuration:\n").append("<br>");
                for (String compName : componentsNotFound) {
                    sb.append("<br>").append(compName);
                }
                Notifications.Bus.notify(new Notification(notificationsTopics, "Configurations Not Found", sb.toString(), NotificationType.INFORMATION));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BatMyProcess getBatProcess(String processName) {
        return new BatMyProcess(unit1, processName, ".*" + processName + ".*", false, processName + ".bat", project);
    }

    @NotNull
    private CommonMyProcess getAppProcess(VirtualFile file, String processName, String workDir, String contextFile, String jvmOpts, File configurationFolder, File secrets) {
        List<String> splittedOptions = Lists.newArrayList(jvmOpts.split(" "));
        String appName = splittedOptions.remove(splittedOptions.size() - 1);
        String bootstrapClass = splittedOptions.remove(splittedOptions.size() - 1);
        splittedOptions.add("-Dserver.environment=dev");
        splittedOptions.add("-Dabos.environment=dev");
        splittedOptions.add("-Dlog.dir=" + workDir + "\\log");
        splittedOptions.add("-Dgc.log.file=" + "..\\" + processName + ".stdout.log");
        splittedOptions.add("-Xloggc:" + workDir + "\\" + processName + ".stdout.log");
        Module module = getFileModule(file, project);
        List<String> hardcodedFolders = Arrays.asList(configurationFolder.getAbsolutePath(), secrets.getAbsolutePath());
        addFolderToClasspath(project,module,hardcodedFolders);
        return new CommonMyProcess(unit1,
                processName,
                ".*" + processName + ".*",
                false,
                project,
                bootstrapClass,
                splittedOptions,
                module,
                appName,
                contextFile,
                workDir,
                file.getPresentableUrl());
    }

    private void addFolderToClasspath(final Project project, final Module module, List<String> folders){
        LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
        final LibraryTable.ModifiableModel projectLibraryModel = projectLibraryTable.getModifiableModel();
        Library library = projectLibraryModel.getLibraryByName("HARDCODED_FOLDERS");
        if (library == null){
            library = projectLibraryModel.createLibrary("HARDCODED_FOLDERS");
        }
        final Library lib = library;
        final Library.ModifiableModel libraryModel = library.getModifiableModel();
        for (String folder : folders){
            String url = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, folder);
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
            if(file == null){
                throw new IllegalStateException("Hardcoded folder not found: " + folder);
            }
            libraryModel.addRoot(file, OrderRootType.CLASSES);
        }
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                libraryModel.commit();
                projectLibraryModel.commit();
                ModuleRootModificationUtil.addDependency(module,lib);
            }
        });
    }

    public static Module getFileModule(String path, Project project) {
        String url = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, path);
        return getFileModule(VirtualFileManager.getInstance().findFileByUrl(url), project);
    }

    public static Module getFileModule(VirtualFile file, Project project) {
        Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module == null && file.getParent() != null && !file.getParent().equals(file)) {
            module = getFileModule(file.getParent(), project);
        }
        return module;
    }

    private Properties parseConfiguration(File processesFile) throws IOException {
        Properties prop = new Properties();
        if (!processesFile.isFile()) {
            throw new IllegalStateException("Properties file not found: " + processesFile);
        }
        prop.load(new FileInputStream(processesFile));
        return prop;
    }

    public static HolyProjectProcessesManager getInstance(@NotNull Project project) {
        return project.getComponent(HolyProjectProcessesManager.class);
    }

    public List<MyProcess> getProcesses() {
        return processes;
    }

    @Nullable
    @Override
    public ProcessesState getState() {
        return new ProcessesState(processes);
    }

    @Override
    public void loadState(ProcessesState state) {
        processes.clear();
        processes.addAll(state.getProcesses());
    }

}
