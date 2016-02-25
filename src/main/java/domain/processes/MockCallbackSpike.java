package domain.processes;

public interface MockCallbackSpike {
    public static MockCallbackSpike DO_NOTHING = new MockCallbackSpike() {
        @Override
        public void postMock(boolean newIsMocked, MyProcess process) {}
    };

    void postMock(boolean newIsMocked, MyProcess process);
}
