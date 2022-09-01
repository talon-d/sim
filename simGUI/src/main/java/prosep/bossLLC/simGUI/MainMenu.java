package prosep.bossLLC.simGUI;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import prosep.bossLLC.simGUI.data.Request;

import de.milchreis.uibooster.UiBooster;
import de.milchreis.uibooster.model.Form;
import de.milchreis.uibooster.model.ListElement;
import javafx.fxml.FXML;
import prosep.bossLLC.simGUI.data.Batch;
import prosep.bossLLC.simGUI.data.CalTable;
import prosep.bossLLC.simGUI.data.DataPacket;
import prosep.bossLLC.simGUI.data.DataSet;
import prosep.bossLLC.simGUI.data.Puma;
import prosep.bossLLC.simGUI.data.Result;
import prosep.bossLLC.simGUI.io.DataSheet;
import prosep.bossLLC.simGUI.io.SampleFactory;
import prosep.bossLLC.simGUI.report.Report;
import prosep.bossLLC.simGUI.report.ReportTable;





/**
 * The MainMenu comprises most of the user functionality of this application.
 * Any of the "handle" functions interface directly with a button in the menu.
 * @author talon
 *
 */
public class MainMenu {

	
	
	
	
	
	
	
	//singleton implementation of the UiBooster pop up gui library 
    public static final UiBooster HANDLER_INTERFACE = new UiBooster();
    
    
    
    
    
    
    
    
    
    
    /**
     * Lets the user select a calibration .csv file for the CANNABINOIDS_XX-VWD
     * family of Agilent Chromatography methods.
     * @apiNote button id=calButton
     */
    @FXML private final void handleCalibrationUpdate() {
    	Terminal.say("Calibration update event recieved!");
    	//prompt the user to select a csv file
    	File selected = HANDLER_INTERFACE.showFileSelection("Select Calibration CSV File","csv","CSV");
    	Terminal.say("\tAttempting to parse "+selected.getAbsolutePath()+" as a calibration table...");
    	//attempt to load the file and parse the table
    	try {
    		CalTable t = new CalTable(selected);
    		WorkingSet.updateCalibration(t);
    		Terminal.say("\tParsed table:\n "+t.stringify());
    	} catch (Exception e1) {
    		Terminal.say("\tFailure to load calibration table: ");
    		Terminal.specifyFullError(e1);
    		HANDLER_INTERFACE.showErrorDialog("Couldn't load calibration table from "+selected.getAbsolutePath(), "File Read Error");
    	}
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Allows the user to add an fTHC nondetect result to a request without a sheet.
     * Much more expedient then having to integrate and reprocess obvious results.
     * @apiNote button id=thcNDAdder
     */
    @FXML private final void handleAddNondetect() {
    	Terminal.say("Add ND event fired!");
    	List<Request> requests = WorkingSet.getRequests();
    	List<ListElement> elements = new ArrayList<>(requests.size());
    	for(Request r : requests) {
    		Terminal.say("\tAdding "+r.getName()+"to the mergable list...");
    		elements.add(new ListElement(r.getName(),r.getFlags()));
    	}
    	ListElement sel = HANDLER_INTERFACE.showList(
    			"Pick the sample to add an fTHC nondetect result to:", "Manual ND Creator", elements.toArray(new ListElement[] {}));
    	String initials = HANDLER_INTERFACE.showTextInputDialog("Enter your intials:");
    	if(sel == null)
    		return;
    	else for(Request r : requests)
			if(r.getName().equalsIgnoreCase(sel.getTitle())) {
				DataPacket nd = DataPacket.generateFTHCNondetect(initials);
				WorkingSet.insertFthcPacket(r, nd);
				break;
			}
    	Gui.updateSet();
    }
    
    
    
    /**
     * Allows to user to add a special type of fTHC packet to a data set.
     * @apiNote button id=addBelowLoq
     */
    @FXML private final void handleAddBelowLoq() {
    	Terminal.say("Add >LoQ event fired!");
    	List<Request> requests = WorkingSet.getRequests();
    	List<ListElement> elements = new ArrayList<>(requests.size());
    	for(Request r : requests) {
    		Terminal.say("\tAdding "+r.getName()+"to the mergable list...");
    		elements.add(new ListElement(r.getName(),r.getFlags()));
    	}
    	ListElement sel = HANDLER_INTERFACE.showList(
    			"Pick the sample to add an fTHC >LoQ result to:", "Manual >LoQ Creator", elements.toArray(new ListElement[] {}));
    	String initials = HANDLER_INTERFACE.showTextInputDialog("Enter your intials:");
    	if(sel == null)
    		return;
    	else for (Request r : requests) 
    		if(r.getName().equalsIgnoreCase(sel.getTitle())) {
    			DataPacket belowLoq = DataPacket.generateFTHCBelowLoq(initials);
    			WorkingSet.insertFthcPacket(r, belowLoq);
    			break;
    		}
    	Gui.updateSet();
    }
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Lets the user generate a process report.
     * @apiNote button id=repButton
     */
    @FXML private final void handleReportGeneration() {
    	Terminal.say("Report generation event recieved!");
    	//report generation is an error prone process, so it is contained entirely within a try block
    	try {
    		//lists to hold user-selected items
    		List<Puma> reportedPuma = new LinkedList<>();
    		List<Request> reported = new LinkedList<>();
    		//lists to hold reportable elements
	    	List<Puma> reportablePuma = new LinkedList<>();
	    	List<Request> reportableSamples = new LinkedList<>();
	    	List<String> reportable = new LinkedList<>();		//name list for user dialogue
	    	//iterate through each request in the set to pick out the reportable ones
	    	Terminal.say("\tFetching reportable requests...");
	    	for(Request r : WorkingSet.getRequests()) {
	    		DataSet data = WorkingSet.getDataSetOf(r);
	    		int req = r.checkRequestRequirements(data);
	    		if(req > -1) {
	    			Terminal.say("\t\t"+r.getName()+" is reportable");
	    			reportableSamples.add(r);
	    			reportable.add(r.getName());
	    		} else Terminal.say("\t\t"+r.getName()+" is not reportable");	}
	    	//iterate through each puma set in the working set, 
	    	//find the minimal fulfillment code of the set, then add reportable to the list
	    	Terminal.say("\tFetching reportable pumas...");
	    	for(Puma p : WorkingSet.getPumas()) {
	    		DataSet[] data = WorkingSet.getDataSetsOf(p);
	    		int index = 0;
	    		int minReq = 2;
	    		for(Request r : p.generateRequests()) {
	    			int req = r.checkRequestRequirements(data[index]);
	    			if(req < minReq)
	    				minReq = req;
	    			index++;
	    		}
	    		if(minReq > -1) {
	    			reportablePuma.add(p);
	    			reportable.add(p.getName());
	    		} else
	    			Terminal.say("\t\t"+p.getName()+" has at least one element which is not reportable");
	    	}
	    	Terminal.say("\tCollecting user input");
	    	//Display the report generation form to allow user selections
	    	Form f = HANDLER_INTERFACE.createForm("Report Generation")
	    			.addDatePicker("Report Date")
	    			.addMultipleSelection("Samples to report", reportable)
	    			.show(); 
	    	//collect date from form
	    	LocalDate date = SampleFactory.convertUiDate((Date) f.getByIndex(0).getValue());
	    	//collect selected reportable pumas and requests from the form
	    	@SuppressWarnings("unchecked")
			List<String> selected = (List<String>) f.getByIndex(1).getValue();
	    	Terminal.say("\t\tThe following were selected:");
	    	for(String sel : selected) {
	    		boolean found = false;
	    		Terminal.say("\t\t\t"+sel);
	    		if(sel.startsWith("PUMA")) {
	    			for(Puma p : reportablePuma)
		    			if(p.getName().equalsIgnoreCase(sel)) {
		    				reportedPuma.add(p);
		    				found = true;
		    			}
	    		} else { 
		    		for(Request r : reportableSamples)
		    			if(sel.equalsIgnoreCase(r.getName())) {
		    				reported.add(r);
		    				found = true;
		    			}
	    		}
	    		if(found != true) throw new Exception("Couldn't find selected request: "+sel);
	    	}
	    	//Allow the user to select the directory the write the report to
	    	File dir = HANDLER_INTERFACE.showDirectorySelection();
	    	//Create and write the report
	    	try {
		    	Report r = new Report(reported,reportedPuma,WorkingSet.getCalibration(),date);
		    	Terminal.say("Report generate successfully. Writing...");
		    	r.writeReport(dir);
	    	} catch (Exception e) {
	    		Terminal.say("Report instantiation/write failure: ");
	    		Terminal.specifyFullError(e);
	    		HANDLER_INTERFACE.showErrorDialog("Couldn't generate report:\n\t"+e.getMessage(), "Handled Error");
	    	}
    	} catch (Exception e) {
    		Terminal.say("UNINTENDED EXCEPTION FOR REPORT GENERATION EVENT FUNCTION!");
    		Terminal.specifyFullError(e);
    		HANDLER_INTERFACE.showException("Report Generation Error, didnt make report", "Unhandled Error", e);
    	}
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    /**
     * Allows to user to alter the request requirements for a sample.
     * @apiNote button id=editRequirementsButton
     */
    @FXML private final void handleRequestAlteration() {
    	List<Request> requests = WorkingSet.getRequests();
    	List<ListElement> elements = new ArrayList<>(requests.size());
    	for(Request r : requests) {
    		Terminal.say("\tAdding "+r.getName()+"to the mergable list...");
    		elements.add(new ListElement(r.getName(),r.getFlags()));
    	}
    	ListElement sel = HANDLER_INTERFACE.showList("Pick a request to alter the requirements of:", 
    			"Request Editor", elements.toArray(new ListElement[] {}));
    	Request toEdit = null;
    	for(Request possible : requests)
    		if(sel.getTitle().equalsIgnoreCase(possible.getName()))
    			toEdit = possible;
    		else continue;
    	Request copy = toEdit;
    	try {
    		toEdit = useGuiToAssembleRequest(toEdit.getName());
    		WorkingSet.swapRequest(toEdit, copy);
    		Gui.updateSet();
    	} catch (NullPointerException e) {
    		Terminal.say("Selection was null. Returning...");
    	}
    }
    


    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Allows to user to submit data sheets to fill out the WorkingSet.
     * @apiNote button id=dataButton
     * @seeAlso BadSheetHandler
     */
    @FXML private final void handleDataSheetSubmission() {
    	//initialize
    	Terminal.say("Handling data sheet submission...");
    	//ask user for directory to parse
    	File dir = HANDLER_INTERFACE.showDirectorySelection();
    	//attempt to parse each sheet in the selected directory
    	Terminal.say("\tCollect data sheets from "+dir.getAbsolutePath());
    	List<DataSheet> sheets = new LinkedList<>();
    	for(File f : dir.listFiles()) try {
    		DataSheet s = new DataSheet(f);
    		sheets.add(s);
    		Terminal.say("\t\tParsed data from "+f.getName());
    	} catch (Exception e1) {
    		Terminal.say("\t\tCouldn't parse file as data sheet: "+f.getAbsolutePath()+". Continuing...");
    		Terminal.specifyFullError(e1);
    		continue;
    	}
    	Terminal.say("\tSuccessfully collected "+sheets.size()+" sheets!");
    	//assemble the collected sheets into an intermediary result map
    	Map<String,Result[]> resultMap = new HashMap<>();
    	if(sheets.size() == 0) {
    		HANDLER_INTERFACE.showErrorDialog("No sheets were parsed from selected directory, so nothing happened.", "Data Submission Error");
    		return;
    	} else for(DataSheet s : sheets)
    		resultMap = Result.parseSheet(s, resultMap);
    	//merge gc's and lc's
    	try {
    		Terminal.say("Attempting to merge GCs and LCs of puma samples...");
			resultMap = Result.mergeGcAndLcOfPuma(resultMap);
		} catch (DataFormatException e) {
			Terminal.say("Error occurred during merge process!");
			HANDLER_INTERFACE.showErrorDialog("Error while attempting to merge GC and LC puma results! Exiting...", "Data Submission Error");
			e.printStackTrace();
			return;
		}
    	List<String> handled = new LinkedList<>();
    	for(String request : resultMap.keySet()) {
    		Terminal.say("\tBeginning automatic handling of "+request+"...");
    		if(Puma.isDataForPuma(request)) {
    			Terminal.say("\t\tHandling as puma...");
	    		Puma existingPuma = WorkingSet.fetchPuma(request);
	    		//add the puma set to the working set if it is new
	    		if(existingPuma == null) {
	    			Terminal.say("\t\t\tDoesn't exist! Adding now...");
	    			existingPuma = handlePumaInference(request);
	    			WorkingSet.addPuma(existingPuma);
	    		} else Terminal.say("\t\t\tExists!");
	    		boolean added = WorkingSet.addPumaData(new DataSet(request,resultMap.get(request)));
	    		if(added) {
	    			Terminal.say("\t\t\t\tSuccessfully added puma data for "+request);
	    			handled.add(request);
	    		} else Terminal.say("\t\t\t\tError adding puma data to "+existingPuma.getName()+" from set element "+request);
    		} else {
    			Terminal.say("\t\tHandling as plain request...");
    			Request existing = WorkingSet.fetchRequest(request);
    			//add the request to the working set if it is new
    			boolean abort = false;
    			if(existing == null) {
    				Terminal.say(request+" does not exist! Adding now...");
    				existing = SampleFactory.parseBasicRequest(request);
    				if(existing != null)
    					WorkingSet.addRequest(existing);
    				else abort = true;
    			} else Terminal.say("Exists!");
    			if(!abort) {
    				boolean added = WorkingSet.addData(new DataSet(request,resultMap.get(request)), existing);
    				if(added) {
	    				Terminal.say("Successfully added data for "+request);
	    				handled.add(request);
    				} else Terminal.say("Failed apping data for "+request);
    			} else Terminal.say("Error adding data to "+request);
    		}
    	}
    	Gui.updateSet();
    	for(String request : handled)
    		resultMap.remove(request);
    	if(resultMap.size() > 0)
    		BadSheetHandler.initBadDataGUI(resultMap);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Allows the user to remove selected samples from the working set.
     * Opens a multiselection window which allows from 1 or many samples 
     * to be removed at once.
     */
    @FXML private void handleRequestRemoval() {
    	Terminal.say("Request removal event fired!");
    	//collects the name of each request in the working set
    	List<String> list = new LinkedList<>();
    	for(Request r : WorkingSet.getRequests())
    		list.add(r.getName());
    	for(Puma p : WorkingSet.getPumas())
    		list.add(p.getName());
    	Terminal.say("\tThe following samples are available for deletion:");
    	for(String available : list)
    		Terminal.say("\t\t"+available);
    	//opens the selection window
    	List<String> namesToRemove = HANDLER_INTERFACE.showMultipleSelection("Pick the elements to remove", "Request Removal", list.toArray(new String[] {}));
    	Terminal.say("\tThe user requested the following be deleted:");
    	for(String name : namesToRemove)
    		Terminal.say("\t\t"+name);
    	//handles the selected names
    	Terminal.say("\tBeginning removal process...");
    	for(String nameToRemove : namesToRemove)
    		if(nameToRemove.startsWith("PUMA")) {
    			Terminal.say("\t\tAttempting to remove PUMA set: "+nameToRemove);
    			WorkingSet.removePuma(nameToRemove);
    		} else {
    			Terminal.say("\t\tAttempting to remove request: "+nameToRemove);
	    		Request r = WorkingSet.fetchRequest(nameToRemove);
	    		WorkingSet.remove(r);
    		}
    	Gui.updateSet();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Allows the user to clear the working set of all request at once.
     * Displays a confirmation dialogue to ensure that is what the user wants.
     */
    @FXML public void handleDeleteAll() {
    	Terminal.say("Total deletion button fired!");
    	//display confirmation dialogue to affirm deletion request
    	HANDLER_INTERFACE.showConfirmDialog("Do you really wish to delete ALL the samples and data?", "Remove All", 
    			new Runnable() {
					@Override public void run() {
						Terminal.say("\tDeleting ALL samples...");
						WorkingSet.clear();
				    	Gui.updateSet();
					}
	    	},  new Runnable() {
					@Override public void run() {
						Terminal.say("\tNOT deleting all samples...");
					}
	    	});
    	
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Allows to user the merge two requests and their data sets
     * @apiNote button id=mergeButton
     */
    @FXML private final void handleExistingMerge() {
    	Terminal.say("Controller merge event fired!");
    	List<Request> requests = WorkingSet.getRequests();
    	List<ListElement> elements = new ArrayList<>(requests.size());
    	for(Request r : requests) {
    		Terminal.say("\tAdding "+r.getName()+"to the mergable list...");
    		elements.add(new ListElement(r.getName(),r.getFlags()));
    	}
    	ListElement[] toDisplay = elements.toArray(new ListElement[] {});
    	Form f = HANDLER_INTERFACE.createForm("Merge 2 DataSets...")
    			.addList("Parent Set", toDisplay)
    			.addList("Child Set", toDisplay)
    			.show();
    	Request parent = null, child = null;
    	ListElement pSel = (ListElement) f.getByIndex(0).getValue();
    	ListElement cSel = (ListElement) f.getByIndex(1).getValue();
    	for(Request possible : requests)
    		if(pSel.getTitle().equalsIgnoreCase(possible.getName()))
    			parent = possible;
    		else if(cSel.getTitle().equalsIgnoreCase(possible.getName()))
				child = possible;
    	if(parent == null || child == null) {
    		Terminal.say("Null merge selection, exiting...");
    		HANDLER_INTERFACE.showErrorDialog("A selection was invalid! Nothing happened...", "Missing Selection");
    	} else if(parent == child) {
    		Terminal.say("Duplicate merge selection, exiting...");
    		HANDLER_INTERFACE.showErrorDialog("You picked the same sample twice! Nothing happened...", "Double Selection");
    	} else {
    		WorkingSet.mergeRequests(parent,child);
    		Gui.updateSet();
    	}
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Lets the user create a request with the GUI.
     * In particular, this function creates a PopUp form with all the Request parameters, which the user 
     * can change and edit as much as they want. When the user submits the form, the main Thread will unlock
     * and the request will be generated, then added to the GUI.
     * @apiNote 
     */
    @FXML public void handleRequestGeneration() {
    	Terminal.say("Request generation button fired!");
		Request r = useGuiToAssembleRequest("");
		if(r != null) {
			Terminal.say("\tAdding generated request "+r.getName()+" to working set");
			WorkingSet.addRequest(r);
			Gui.updateSet();
		} else Terminal.say("\tNull request given, not added to the working set!");
    }
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Allows the user to create a PUMA set with the GUI
     * In all, a PUMA set needs a date, a batch id, and a
     * PUMA set type, specified in the Type enum in the Puma
     * class. Once created a Puma set will be added to the working
     * set and the GUI will be updated.
     * @apiNote
     */
	@FXML public void handlePumaGeneration() {
		Terminal.say("Beginning GUI generation of PUMA set...");
    	try {
    		Puma.Type[] enumerated = Puma.Type.values();
    		//collect enumerated Puma types
	    	ListElement[] pumaTypes = new ListElement[enumerated.length];
	    	for(int i = 0; i < enumerated.length; i++)
	    		pumaTypes[i] = new ListElement("",enumerated[i].getSuffix());
	    	//create and display a form with Puma set requirements
	    	Form f = MainMenu.HANDLER_INTERFACE.createForm("New PUMA Request")
	    			.addDatePicker("Date?")
	    			.addList("Puma Types", pumaTypes)
	    			.addText("Batch #")
	    			.show();
	    	//interpret and finalize input
	    	LocalDate date = SampleFactory.convertUiDate((Date) f.getByIndex(0).getValue());
	    	int index = 0;
	    	ListElement sel = (ListElement) f.getByIndex(1).getValue();
	    	for(ListElement possible : pumaTypes)
	    		if(sel.equals(possible) )
	    			break;
	    		else
	    			index++;
	    	Puma.Type type = enumerated[index];
	    	String batch = f.getByIndex(2).asString();
	    	Batch id = SampleFactory.parseBatch(batch);
	    	//attempt to create the Puma, add it to the working set, and update the GUI
	    	if(type == null || id == null) {
	    		Terminal.say("/tNull batch or type error! Is type? "+(type==null)+" Is id? "+(id==null));
	    		HANDLER_INTERFACE.showErrorDialog("Couldn't understand Puma Type or Batch selection", "Puma Generation Error");
	    	} else {
		    	Puma p = new Puma(type, date, id);
		    	WorkingSet.addPuma(p);
		    	Gui.updateSet();
		    	Terminal.say("/tOperation successful, created set "+p.getName());
	    	}
    	} catch (Exception e) {
    		Terminal.say("/tOperation unsuccessful with unhandled exception! (in Controller.handlePumaGeneration)");
    		Terminal.specifyFullError(e);
    		HANDLER_INTERFACE.showInfoDialog("Could not create Puma set. Check terminal for full error.");
    	}
    }
	
	
	
	
	
	
	
	
	
    
    
    
    
    
    /**
     * Uses a PUMA set element name to infer its set.
     * @param gcOrLcPumaName
     * @return generated PUMA set
     */
    private static final Puma handlePumaInference(String gcOrLcPumaName) {
    	//input name standardization/error handling
    	String lcName;
    	if(!Puma.isDataForPuma(gcOrLcPumaName))
    		Terminal.say("Couldn't handle puma inference; not a puma sample: "+gcOrLcPumaName);
    	else
    		Terminal.say("Handling puma inference for "+gcOrLcPumaName);
    	if(Puma.nameIsFromPumaGC(gcOrLcPumaName))
    		try {
				lcName = Puma.standardizePumaGcName(gcOrLcPumaName);
			} catch (DataFormatException e) {
				Terminal.say("Error while transforming GC name to LC name: "+gcOrLcPumaName);
				Terminal.specifyFullError(e);
				return null;
			}
    	else if (!Puma.nameIsFromPumaLC(gcOrLcPumaName)) {
    		Terminal.say("Couldn't attempt to handle puma inference; not a gc OR lc name: "+gcOrLcPumaName);
    		return null;
    	} else 
    		lcName = gcOrLcPumaName;
    	//required puma components
    	LocalDate date;
    	Batch id;
    	Puma.Type machine;
    	//attempt to disassemble name
    	String[] tokens = lcName.split("_");
    	String suffix = "", strDate = "";
    	try {
	    	suffix = tokens[1];
	    	strDate = tokens[2];
    	} catch (ArrayIndexOutOfBoundsException e) {
    		Terminal.say("Name wasn't long enough deconstructed: "+lcName);
    	}
    	//ensure that a valid Puma type is found
    	do {
    		machine = Puma.Type.inferFromSuffix(suffix);
    		if(machine == null)
    			suffix = HANDLER_INTERFACE.showTextInputDialog("Couldn't infer Puma type for "+lcName+"\nPlease enter the suffix of the Puma set manually: ");
    	} while (machine == null);
    	//ensure that a valid date is found
    	do {
    		date = SampleFactory.parseDate(strDate);
    		if(date == null)
    			date = SampleFactory.convertUiDate (
    					HANDLER_INTERFACE.showDatePicker("Couldn't autoparse puma date, \nEnter one now for"+lcName, strDate));
    	} while (date == null);
    	//ensure that a valid batch is found
    	do {
    		if(Puma.nameIsFromPumaGC(gcOrLcPumaName)) {
    			id = SampleFactory.parseBatch(gcOrLcPumaName.split("#")[0]);
    		} else {
    			String input = HANDLER_INTERFACE.showTextInputDialog("Enter the batch # for "+lcName);
        		id = SampleFactory.parseBatch(input);
    		}
    		if(id == null)
    			HANDLER_INTERFACE.showErrorDialog("Invalid batch input! You must try again...", lcName+" batch handler");
    	} while (id == null);
    	//assemble and return puma
    	return new Puma(machine,date,id);
    }
    
    
    
    
    
    
    
    

    
    
	
	
	/**
	 * Prompts the user to select a single data packet from an array
	 * In some cases, particularly with fTHC results, there may be multiple, 
	 * differing data packets, with no mathematical means by which to select
	 * the best. As this is a rare case, this method does not allow the full
	 * complexity of picking multiple good results to average. Instead, it
	 * simply allows to user to select one in which to place full confidence.
	 * @param sample name these packets quantify
	 * @param compound name these packets quantify
	 * @param packets to make a selection from
	 * @return user selected packet
	 */
	public static final DataPacket manuallySelectOnePacket(String sample, String compound, final DataPacket[] toChooseFrom) {
		String msg = "Multiple data packets found for "+compound+" on "+sample+", please pick ONE:";
		List<ListElement> choices = new LinkedList<>();
		for(DataPacket d : toChooseFrom)
			choices.add( new ListElement("Result: "+d.getResult(), "\tPrep Amount: "+d.getAmount()+"\tData Source: "+d.getSource()) );
		ListElement ele = MainMenu.HANDLER_INTERFACE.showList(msg,"Ambiguous Result Selector",choices.toArray(new ListElement[] {}));
		int index = choices.indexOf(ele);
		return toChooseFrom[index];
	}
	
	

    
    
	
	
	

	
	
	
	
	
	/**
	 * Assembles a sample request from user input.
	 * @param Unformatted inferred name of the sample to start assembling
	 * @return user generated request or null
	 */
    public static final Request useGuiToAssembleRequest(String starterName) {
    	starterName = starterName.replaceAll(" ", "-");				//insert hyphens in place of spaces
    	Terminal.say("Starting full GUI request generation...");
    	//request creation is error-prone, so the entirety of this function is in a generic try block
    	try {
    		//creates a form for the user to fill out which has all the parameters for a Request
    		Form f = MainMenu.HANDLER_INTERFACE.createForm("New Request")	//window title
	    			.addTextArea("Sample Name",starterName).setID("name")	//whole sample identifier
	    			.addCheckbox("Needs Canna?").setID("canna")				//canna requirement
	    			.addCheckbox("Needs fTHC?").setID("fthc")				//fthc requirement
	    			.addCheckbox("Needs GC?").setID("gc")					//gc requirement
	    			//multi selection window for High Confidence compounds (requires >1 results within RSD)
	    			.addMultipleSelection("Compound needing high confidence:", POSSIBLE_CONFIDENT_COMPOUNDS).setID("confident")
	    			.show();
    		//parses identifier elements from the given name
    		String[] tokens = f.getByIndex(0).asString().split("#");
    		String[] name = tokens[0].split("_");
    		String prefix = name[0];
    		LocalDate date = SampleFactory.parseDate(name[1]);
    		String suffix; try {
    			suffix = name[2]; 
    		} catch (ArrayIndexOutOfBoundsException e) {
    			suffix = "";
    		}
    		Batch id = SampleFactory.parseBatch(tokens[1]);
    		//collects analysis flags
    		boolean canna = (boolean) f.getByIndex(1).getValue();
    		boolean fthc = (boolean) f.getByIndex(2).getValue();
    		boolean gc = (boolean) f.getByIndex(3).getValue();
    		//interprets target compounds
    		@SuppressWarnings("unchecked")
			List<String> targets = (List<String>) f.getByIndex(4).getValue();
    		//create the request, notify the user, then return
    		Request r = new Request(prefix,date,suffix,id,canna,fthc,gc,targets.toArray(new String[] {}));
    		Terminal.say("\tOperation successful: "+r.getName());
    		return r;
    	} catch (Exception e) {
    		//displays error information to the user
    		Terminal.say("\tOperation unsuccessful, null request returned.\t"+e.getMessage());
			HANDLER_INTERFACE.showErrorDialog("Couldn't create the request! Check the terminal for more info.","Error");
			return null;
		}
    }
    
    
    
    // enumeration of compounds which CAN be specified as 'High Confidence'
    private static final String[] POSSIBLE_CONFIDENT_COMPOUNDS = new String[] {
    		ReportTable.LC_BY_RT.CBD.getSheetName(),		// CBD
    		ReportTable.LC_BY_RT.CBT.getSheetName(),		// CBT
    		ReportTable.LC_BY_RT.THC_d9.getSheetName()	// THC (high quantity)
    };
    
}