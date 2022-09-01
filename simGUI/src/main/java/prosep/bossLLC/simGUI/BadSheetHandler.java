package prosep.bossLLC.simGUI;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.milchreis.uibooster.model.Form;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import prosep.bossLLC.simGUI.data.DataSet;
import prosep.bossLLC.simGUI.data.Request;
import prosep.bossLLC.simGUI.data.Result;




/**
 * Displayable singleton interface for handling sheet data which couldn't
 * be handled in the traditional manner.
 * @author talon
 * @see MainMenu.handleDataSheetSubmission for entry point
 * @apiNote The JavaFX resource for this menu is called sheetHandler.fxml
 * 
 */
public class BadSheetHandler {




	
	
	//current menu data (should be null if the menu is not displayed)
	private static Map<String,Result[]> remaining;
	private static Label observed;									//focused element of list
	private static ObservableList<Label> remainingObservedList;		//internal backing of list view
	private static ListView<Label> uiObservedList;					//list view
	
	
	
	/**
	 * Singleton constructor method
	 * @param map of unhandled results
	 */
	public static void initBadDataGUI(Map<String,Result[]> unhandledData) {
		Terminal.say("initializing bad data handler...");
		Gui.changeScene(Gui.DATA_FXML);
		remaining = unhandledData;
		observed = null;
		update();
	}

	
	
	
	
	
	/**
	 * Used as a singleton destructor, triggered by user input.
	 * Nullifies the remaining data and changes the scene.
	 * @apiNote button id=finalForgetter
	 */
	@FXML public void ignoreRemainingAndReturn() {
		Terminal.say("Exiting menu and ignoring remaining...");
		Gui.changeScene(Gui.MAIN_FXML);		//Return the main menu
		//nullify singleton inner fields
		remainingObservedList = null;
		uiObservedList = null;
		remaining = null;
		observed = null;
		Gui.updateSet();					//update the main menu
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Allows the user to forget one data set (without exiting menu)
	 * @apiNote button id=forgetter
	 */
	@FXML public void removeSelected() {
		Terminal.say("Attempting removal...");
		try {
			String nameToRemove = observed.getText();
			Terminal.say("\tRemoving "+nameToRemove);
			remaining.remove(nameToRemove);
			update();
		} catch (NullPointerException e) {
			Terminal.say("\tNothing was selected, so nothing was removed.");
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Allows the user to merge two result sets that are both in the bad sheet menu.
	 * @apiNote button id = innerMerger
	 */
	@FXML public void mergeInwardly() {
		try {							Terminal.say("Inward merge requested!");
			String parentName = observed.getText();
			List<String> availableToMerge = getNamesOfRemaining();
			availableToMerge.remove(parentName);
			Form f = MainMenu.HANDLER_INTERFACE.createForm("Inward Merger")
					.addSelection("Child data", availableToMerge) .show();
			String childName = f.getByIndex(0).asString();
			if(childName != null) {
				Result[] both = Result.mergeWithoutDuplication(remaining.get(parentName), remaining.get(childName));
				remaining.remove(childName);
				remaining.put(parentName, both);			
				updateList(); 				Terminal.say("\tMerge complete for "+parentName+" and "+childName);
			} else  						Terminal.say("\tNull child given, so nothing happened.");
		} catch (NullPointerException e) {	Terminal.say("\tNo parent selected, so nothing happened");				
		} finally 					  { Terminal.say("Inward merge finished!"); 	}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Merges the selected key with some request in the working set
	 * @apiNote button id=merger
	 */
	@FXML public void mergeSelected() {
		Terminal.say("Outward merge signal recieved!");
		try {
			String selected = observed.getText();
			List<Request> available = WorkingSet.getRequests();
			List<String> namesForInterface = new LinkedList<>();
			for(Request r : available)
				namesForInterface.add(r.getName());
			String name = MainMenu.HANDLER_INTERFACE.showSelectionDialog("Pick an existing sample request to put data for "
					+selected+" into:","Outward Merger", namesForInterface);
			Request r = WorkingSet.fetchRequest(name);
			if(r != null) {
				WorkingSet.addData(new DataSet(r.getName(), remaining.get(selected)),r);
				remaining.remove(selected);
				Terminal.say("\tOutwardly merged "+selected+" to "+r.getName());
				update();
			} else Terminal.say("\tNo selection obtained from ui booster dialogue or working set, so nothing was changed.");
		} catch (NullPointerException e) {
			Terminal.say("\tNothing was selected so nothing was changed.");
		}
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Allows the user to generate a request for unhandled data
	 * @apiNote button id=maker
	 */
	@FXML public void inferThenHandleSelected() {
		Terminal.say("Generate signal recieved!");
		try {
			String nameToInfer = observed.getText();
			Terminal.say("\tInferring "+nameToInfer);
			Request r = MainMenu.useGuiToAssembleRequest(nameToInfer);
			DataSet d = new DataSet(r.getName(),remaining.get(nameToInfer));
			Terminal.say("\tSuccessfully inferred "+r.getName());
			if(r != null && d != null) {
				WorkingSet.addRequest(r);
				WorkingSet.addData(d,r);
				remaining.remove(nameToInfer);
				update();
			}
		} catch (NullPointerException e) {
			Terminal.say("\tNothing was selected so nothing was changed");
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Changes the observed item to match the one selected from the list with a mouse.
	 * @apiNote fired from list view onMouseClicked
	 */
	@FXML public void adjustFocus() {
		observed = uiObservedList.getSelectionModel().getSelectedItem();
		updateIndicators();
	}
	
	
	
	/**
	 * Updates logical and GUI menu elements
	 */
	private static final void update() {
		updateList();
		updateIndicators();
	}
	
	
	
	/**
	 * Updates the tracker maps/lists to reflect any changes that have been made
	 */
	@SuppressWarnings("unchecked")
	private static final void updateList() {
		Terminal.say("Updating list backing...");
		remainingObservedList = FXCollections.observableArrayList(getLabelsFromRemaining());
		uiObservedList = (ListView<Label>) Gui.getIdFromScene("remainingList");
		uiObservedList.setItems(remainingObservedList);
		try { if(!remaining.containsKey(observed.getText())) 
			observed = null;
		} catch (NullPointerException e) {
			//do nothing, as the observed key is already null
		}
	}
	
	
	
	/**
	 * Updates the data indicators to reflect the current focus.
	 */
	private static final void updateIndicators() {
		Terminal.say("Updating observed indicators...");
		Label marker = (Label) Gui.getIdFromScene("nameDisplay");
		Shape gcIndicator = (Shape) Gui.getIdFromScene("gcIndicator"),
		cannaIndicator = (Shape) Gui.getIdFromScene("cannaIndicator"),
		fthcIndicator = (Shape) Gui.getIdFromScene("fthcIndicator");
		try {
			String name = observed.getText();
			Terminal.say("\tUpdating indicators to reflect "+name);
			marker.setText(name);
			DataSet toCheck = new DataSet(name,remaining.get(name)); 
			//sets the indicator color according the the data set's availability
			if(toCheck.hasGC())
				gcIndicator.setFill(Color.BLUE);
			else
				gcIndicator.setFill(Color.YELLOW);
			if(toCheck.hasCanna())
				cannaIndicator.setFill(Color.BLUE);
			else
				cannaIndicator.setFill(Color.YELLOW);
			if(toCheck.hasFthc())
				fthcIndicator.setFill(Color.BLUE);
			else
				fthcIndicator.setFill(Color.YELLOW);
		} catch (NullPointerException e) {
			Terminal.say("\tNull focused label. Empty indications used...");
			marker.setText("No Data Selected!");
			gcIndicator.setFill(Color.BLACK);
			cannaIndicator.setFill(Color.BLACK);
			fthcIndicator.setFill(Color.BLACK);
		}
	}
	
	
	
	/**
	 * Inner factory method for obtaining a list of keys for the result map
	 * @return raw list of names
	 */
	private static final List<String> getNamesOfRemaining() {
		List<String> list = new ArrayList<>(remaining.size());
		for(String name : remaining.keySet())
			list.add(name);
		return list;	
	}

	
	
	/**
	 * Inner factory method for getting a label set of the map
	 * @return list of JavaFX labels
	 */
	private static final List<Label> getLabelsFromRemaining() {
		List<Label> list = new ArrayList<>(remaining.size());
		for(String name : getNamesOfRemaining())
			list.add(new Label(name));
		return list;
	}

	
	
}
