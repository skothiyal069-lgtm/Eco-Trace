public class TestGUI {
    public static void main(String[] args) {
        try {
            EcoTraceGUI gui = new EcoTraceGUI();
            gui.setVisible(true);
            Thread.sleep(1000);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
