package ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProcessesState {
    public List<String> bats;
    public Set<String> mocked;

    public ProcessesState() {
        bats = new ArrayList<>();
        mocked = new HashSet<>();
    }
}
