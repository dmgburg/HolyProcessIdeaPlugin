package domain.descriptors;

import com.intellij.openapi.module.Module;

import java.util.List;

public class CommonMyProcessDescriptor extends MyProcessDescriptor{
    public Module module;
    public String appName;
    public String bootstrapClass;
    public String contextFile;
    public String workDir;
    public String batName;
    public List<String> jvmOptions;
}
