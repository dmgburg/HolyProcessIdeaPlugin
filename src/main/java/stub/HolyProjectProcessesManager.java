package stub;

import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import domain.Unit;
import domain.processes.BatMyProcess;
import domain.processes.CommonMyProcess;
import domain.processes.MockCallbackSpike;
import domain.processes.MyProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.HolyProjectPanel;
import ui.ProcessesState;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@State(name = "HolyProjectProcessesManager", storages = @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/HolyProjectProcesses.xml"))
public class HolyProjectProcessesManager extends AbstractProjectComponent implements PersistentStateComponent<ProcessesState> {
    private final Pattern pattern = Pattern.compile("(.*\\\\(.*)\\\\.*\\\\target\\\\deploy-local)\\\\bin\\\\(.*).bat");
    private final ConcurrentHashMap<String, MyProcess> processes;
    private ToolWindowEx myToolWindow;
    private final HolyProjectPanel panel;
    private ProcessesState state;
    final Unit unit1 = new Unit("unit1");

    protected HolyProjectProcessesManager(@NotNull final Project project) {
        super(project);
        this.processes = new ConcurrentHashMap<>();
        panel = new HolyProjectPanel(project);
    }

    @Override
    public void initComponent() {
        super.initComponent();
        Util.runWhenInitialized(myProject, new Runnable() {
            @Override
            public void run() {
                initToolWindow();
            }
        });
    }

    public void refreshProcessStatus() {
        new Task.Backgroundable(myProject, "Refreshing Process") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                int i = 0;
                indicator.setFraction(0);
                indicator.setText("Getting processes info");
                for (MyProcess process : processes.values()) {
                    process.doRefresh();
                    indicator.setFraction(i++ / processes.size());
                }
                panel.update();
            }
        }.queue();
    }

    private void initToolWindow() {
        final ToolWindowManagerEx manager = ToolWindowManagerEx.getInstanceEx(myProject);
        myToolWindow = (ToolWindowEx) manager.registerToolWindow(HolyProjectPanel.ID, false, ToolWindowAnchor.RIGHT, myProject, true);
        myToolWindow.setIcon(IconLoader.findIcon("/icons/DB_logo.png"));
        final ContentFactory contentFactory = ServiceManager.getService(ContentFactory.class);
        final Content content = contentFactory.createContent(panel, "", false);
        ContentManager contentManager = myToolWindow.getContentManager();
        contentManager.addContent(content);
        contentManager.setSelectedContent(content, false);
    }

    public void updateState() {
        state = new ProcessesState();
        VirtualFile root = myProject.getBaseDir();
        VfsUtilCore.iterateChildrenRecursively(root, null, new ContentIterator() {
            @Override
            public boolean processFile(VirtualFile file) {
                Matcher matcher = pattern.matcher(file.getPresentableUrl());
                if (matcher.matches()) {
                    state.bats.add(file.getPresentableUrl());
                }
                return true;
            }
        });
        stateUpdated();
    }

    public void stateUpdated() {
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
            for (String bat : state.bats) {
                Matcher matcher = pattern.matcher(bat);
                if (matcher.matches()) {
                    String processName = matcher.group(3);
                    String workDir = matcher.group(1);
                    String contextFile = properties.getProperty("hosts.localhost.processes." + processName + ".context");
                    String jvmOpts = properties.getProperty("hosts.localhost.processes." + processName + ".jvmOpts");
                    MyProcess appProcess;
                    if (jvmOpts == null) {
                        componentsNotFound.add(bat);
                        appProcess = getBatProcess(processName,bat);
                    } else {
                        appProcess = getAppProcess(bat, processName, workDir, contextFile, jvmOpts, configurationFolder, secrets, state.mocked.contains(processName));
                    }
                    processes.putIfAbsent(bat, appProcess);
                }
            }
            if (componentsNotFound.size() > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("The following processes not found in configuration:\n").append("<br>");
                for (String compName : componentsNotFound) {
                    sb.append("<br>").append(compName);
                }
                sb.append("<br>").append("Creating bat-only process");
                Util.notifyInfo("Configurations Not Found", sb.toString());
            }
            panel.update();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BatMyProcess getBatProcess(String processName,String batName) {
        return new BatMyProcess(unit1, processName, ".*" + batName + ".*", false, batName);
    }

    @NotNull
    private CommonMyProcess getAppProcess(String fileUrl, String processName, String workDir, String contextFile, String jvmOpts, File configurationFolder, final File secrets, boolean isMocked) {
        List<String> splittedOptions = Lists.newArrayList(jvmOpts.split(" "));
        String appName = splittedOptions.remove(splittedOptions.size() - 1);
        String bootstrapClass = splittedOptions.remove(splittedOptions.size() - 1);
        splittedOptions.add("-Dserver.environment=dev");
        splittedOptions.add("-Dabos.environment=dev");
        splittedOptions.add("-Dlog.dir=" + workDir + "\\log");
        splittedOptions.add("-Dgc.log.file=" + "..\\" + processName + ".stdout.log");
        splittedOptions.add("-Xloggc:" + workDir + "\\" + processName + ".stdout.log");
        final Module module = Util.getFileModule(LocalFileSystem.getInstance().findFileByIoFile(new File(fileUrl)), myProject);
        final List<String> hardcodedFolders = Arrays.asList(configurationFolder.getAbsolutePath(), secrets.getAbsolutePath());
        if (module != null) {                       // can be null on project startup
            AppUIUtil.invokeOnEdt(new Runnable() {
                @Override
                public void run() {
                    addFolderToClasspath(myProject, module, hardcodedFolders);
                    ;
                }
            }, myProject.getDisposed());
        }
        return new CommonMyProcess(unit1,
                processName,
                ".*" + processName + ".*",
                isMocked,
                bootstrapClass,
                splittedOptions,
                appName,
                contextFile,
                workDir,
                fileUrl,
                new MockCallbackSpike() {
                    @Override
                    public void postMock(boolean newIsMocked, MyProcess process) {
                        if(newIsMocked){
                            state.mocked.add(process.getName());
                        } else {
                            state.mocked.remove(process.getName());
                        }
                    }
                });
    }

    private void addFolderToClasspath(@NotNull final Project project, @NotNull final Module module, List<String> folders) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        String hardcoded_folders = "HARDCODED_FOLDERS";
        LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
        final LibraryTable.ModifiableModel projectLibraryModel = projectLibraryTable.getModifiableModel();
        Library library = projectLibraryModel.getLibraryByName(hardcoded_folders);
        if (library == null) {
            library = projectLibraryModel.createLibrary(hardcoded_folders);
        }
        final Library lib = library;
        final Library.ModifiableModel libraryModel = library.getModifiableModel();
        int addedLibs = 0;
        for (String folder : folders) {
            String url = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, folder);
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
            if (file == null) {
                throw new IllegalStateException("Hardcoded folder not found: " + folder);
            }
            OrderEntry orderEntryForFile = ModuleRootManager.getInstance(module).getFileIndex().getOrderEntryForFile(file);
            if (orderEntryForFile == null) {
                addedLibs++;
                libraryModel.addRoot(file, OrderRootType.CLASSES);
            }
        }
        if (addedLibs > 0) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    libraryModel.commit();
                    projectLibraryModel.commit();
                    ModuleRootModificationUtil.addDependency(module, lib);
                }
            });
        }
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

    public Collection<MyProcess> getProcesses() {
        return processes.values();
    }

    @Nullable
    @Override
    public ProcessesState getState() {
        return state;
    }


    @Override
    public void loadState(ProcessesState state) {
        this.state = state;
        stateUpdated();
    }
}
