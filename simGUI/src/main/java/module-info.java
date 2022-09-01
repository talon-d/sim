module prosep.bossLLC.simGUI {
    requires javafx.controls;
    requires javafx.fxml;
	requires javafx.graphics;
	requires javafx.base;
	requires org.apache.poi.poi;
	requires org.apache.poi.ooxml;
	requires com.google.common;
	requires UiBooster;
	requires com.google.gson;

    opens prosep.bossLLC.simGUI to javafx.fxml;
    opens prosep.bossLLC.simGUI.data to com.google.gson;
    exports prosep.bossLLC.simGUI;
}
