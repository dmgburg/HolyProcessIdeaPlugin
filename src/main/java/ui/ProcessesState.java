package ui;

import com.intellij.util.xmlb.annotations.Tag;
import domain.processes.MyProcess;

import java.util.List;

public class ProcessesState {
    @Tag("processesState")
    public List<MyProcess> processes;

    public ProcessesState() {
    }

    public ProcessesState(List<MyProcess> processes) {
        this.processes = processes;
    }

    public List<MyProcess> getProcesses() {
        return processes;
    }

    public void setProcesses(List<MyProcess> processes) {
        this.processes = processes;
    }
}
