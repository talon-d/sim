package prosep.bossLLC.simGUI;


import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import prosep.bossLLC.simGUI.data.DataSet;
import prosep.bossLLC.simGUI.data.Puma;
import prosep.bossLLC.simGUI.data.Request;



/**
 * Main JavaFX Application class.
 * @author talon
 * @see Executable for higher-level entry point
 * 
 */
public class Gui extends Application {

	

	//top-level elements of the JavaFX GUI
	private static Stage stage;
    private static Scene scene;
    //FXML filenames, accessed implicitly via the resources folder
    public static final String 
    MAIN_FXML = "controller",
    DATA_FXML = "sheetHandler";
   
    
    
    /**
     * Launches the GUI
     */
    @Override public final void start(Stage primaryStage) throws Exception {
    	stage = primaryStage;
    	primaryStage.setTitle("sim");
    	changeScene(MAIN_FXML);
        primaryStage.show();
        updateSet();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Changes the scene to the parent of the given FXML URL
     * @param fxml resource filepath
     */
    public final static void changeScene(String fxml) {
    	Terminal.say("Attempting to load fxml from "+fxml+"...");
    	try {
    		//String fxmlpath = new File(".").getAbsolutePath()+fxml+".fxml";
			FXMLLoader fxmlLoader = new FXMLLoader(
					Gui.class.getClassLoader().getResource("prosep/bossLLC/simGUI/"+fxml+".fxml"));
	        Parent p = fxmlLoader.load();
			scene = new Scene(p);
			stage.setScene(scene);
			stage.show();
		} catch (IOException e) {
			Terminal.specifyFullError(e);
		}
    }
    
    
    
	/**
     * Searches the scene for a node with the given id
     * @param string identifier of node
     * @return found node or null
     */
    public static final Node getIdFromScene(final String id) {
    	return scene.lookup("#"+id);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Updates the workingSet list view to match the current working set.
     */
	public static final void updateSet() {
		@SuppressWarnings("unchecked")
		ListView<Group> list = (ListView<Group>) getIdFromScene("workingSet");
		List<Group> names = new LinkedList<>();
		for(Request r : WorkingSet.getRequests())
			names.add(makeRequestIndicator(r,WorkingSet.getDataSetOf(r)));
		for(Puma p : WorkingSet.getPumas())
			try {
				names.add(makeRequestIndicator(p,WorkingSet.getDataSetsOf(p)));
			} catch (Exception e) {
				MainMenu.HANDLER_INTERFACE.showException("Puma could not be added to menu during update", "Display Error", e);
			}
		ObservableList<Group> observedList = FXCollections.observableArrayList(names);
		list.setItems(observedList);
	}
	
	
	
	
	

    
    
    
	
	
	
	
	
	
	
	
	
    
    
    /**
     * Creates an indicator circle for the working set GUI view
     * @param request to indicate
     * @param data to fulfill with
     * @return indicator node for list view
     */
   private final static Group makeRequestIndicator(Request r, DataSet d) {
    	Circle indicator;
    	int fulfillmentCode = r.checkRequestRequirements(d);
    	switch(fulfillmentCode) {
    	case -1:
    		indicator = Indicator.forError();
    		break;
    	case 0:
    		indicator = Indicator.forWarning();
    		break;
    	case 1:
    		indicator = Indicator.forGood();
    		break;
    	default:
    		indicator = Indicator.forInvalid();
    		break;
    	}
    	indicator.setLayoutX(Indicator.X_OFFSET);
    	indicator.setLayoutY(Indicator.Y_OFFSET);
    	return new Group(new Label(r.getName()),indicator);
    }
    
    
    
    /**
     * Creates an indicator circle for a whole puma set
     * @param puma set to indicate
     * @param data sets to fulfill requests
     * @return indicator node
     * @throws Exception if given incongruent requests and sets
     */
    private final static Group makeRequestIndicator(Puma p, DataSet[] sets) throws Exception {
    	if(p.getNames().length != sets.length)
    		throw new Exception("Incongruent puma/set length while generating indicator...");
    	else {
	    	final Circle indicator;
	    	Request[] rs = p.generateRequests();
	    	boolean hasFoundError = false;
	    	boolean allArePerfect = true;
	    	for(int i = 0; i < rs.length; i++) {
	    		int code = rs[i].checkRequestRequirements(sets[i]);
	    		switch(code) {
	    		case -1:
	    			hasFoundError = true;
	    			break;
	    		case 0:
	    			allArePerfect = false;
	    			break;
	    		default: continue;
	    		}
	    	}
	    	if(hasFoundError)
	    		indicator = Indicator.forError();
	    	else if(allArePerfect)
	    		indicator = Indicator.forGood();
	    	else
	    		indicator = Indicator.forWarning();
	    	indicator.setLayoutX(Indicator.X_OFFSET);
	    	indicator.setLayoutY(Indicator.Y_OFFSET);
	    	return new Group(new Label(p.getName()),indicator);
    	}
    }
    
    
    
    /**
     * private container for indicator circle factory methods
     * @author talon
     *
     */
    private static final class Indicator {
    	
    	static final int Y_OFFSET = 8, X_OFFSET = 300, RADIUS = 6;
    	
	    static final Circle forError() {
	    	final Circle c = new Circle(RADIUS);
	    	c.setFill(Color.YELLOW);
	    	return c;
	    }
	    
	    static final Circle forWarning() {
	    	final Circle c = new Circle(RADIUS);
	    	c.setFill(Color.AQUAMARINE);
	    	return c;
	    }
	    
	    static final Circle forGood() {
	    	final Circle c = new Circle(RADIUS);
	    	c.setFill(Color.BLUE);
	    	return c;
	    }
	    
	    static final Circle forInvalid() {
	    	final Circle c = new Circle(RADIUS);
	    	c.setFill(Color.GREY);
	    	return c;
	    }
    }
    

}