package prosep.bossLLC.simGUI;


import javafx.application.Application;
import prosep.bossLLC.simGUI.io.Storage;



/**
 * Entry point to the application. Registered as such in pom.xml, so the program
 * is built from here as well.
 * @author talon
 *
 */
public class Executable {

	
	
	
	
	
	
	/**
	 * Application entry function
	 * @param launch args
	 */
	public static void main(String[] args) {
		try { 
			Storage.init();						//write/load persistent files
			Application.launch(Gui.class);		//launch the JavaFX implementing class
			//thread will be locked until the window is closed, at which point persistent files will be updated
			Storage.close();
		} catch (Exception e) {
			Terminal.say("!!! SIM LAUNCH ERROR, UNRECOVERABLE !!!\n");
			Terminal.specifyFullError(e);
			MainMenu.HANDLER_INTERFACE.showErrorDialog("Could not initialize SIM. Check terminal for more details", "sim init error");
		}
	}

	
	
}