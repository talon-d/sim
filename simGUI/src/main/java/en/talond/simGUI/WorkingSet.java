package en.talond.simGUI;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonSyntaxException;

import en.talond.simGUI.data.Batch;
import en.talond.simGUI.data.CalTable;
import en.talond.simGUI.data.DataPacket;
import en.talond.simGUI.data.DataSet;
import en.talond.simGUI.data.Puma;
import en.talond.simGUI.data.Request;
import en.talond.simGUI.data.Result;
import en.talond.simGUI.data.CalTable.Range;
import en.talond.simGUI.io.SampleFactory;
import en.talond.simGUI.io.Storage;
import en.talond.simGUI.report.ReportTable.LC_BY_RT;



/**
 * The working set is a Singleton class for managing requests, their data, and
 * the data's calibration settings. It is tightly correlated with and used
 * extensively by the GUI.
 * @author talon
 * 
 */
public final class WorkingSet {

	
	
	//singleton instance data
	private static String srcpath;
	private static Map<Request,DataSet> workingSet;
	private static Map<Puma,DataSet[]> pumaSet;
	private static CalTable current;
	
	
	
	/**
	 * Singleton constructor method for the WorkingSet
	 * @param file path to the working set file
	 * @throws Exception
	 */
	public static final void open(String filepath) throws Exception {
		srcpath = filepath;
		File setFile = new File(filepath);
		workingSet = new HashMap<>();
		pumaSet = new HashMap<>();
		if(!setFile.exists()) {
			setFile.createNewFile();
			Terminal.say("No exising "+Storage.WORKING_SET_NAME+". Created new.");
		} else loadSetFile();
			
	}
	
	

	/**
	 * Singleton destructor method for the WorkingSet.
	 * Saves contents to file given during construction
	 * @throws Exception
	 */
	public static final void close() throws Exception {
		write();
		clear();
		current = null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Clears mapped requests and data sets
	 * Also clears all puma stuff.
	 */
	public static final void clear() {
		workingSet.clear();
		pumaSet.clear();
	}
	
	
	
	/**
	 * Removes requests from the WorkingSet
	 * @param request to remove
	 * @return true if successful
	 */
	public static final boolean remove(Request r) {
		DataSet mapped = workingSet.remove(r);
		return mapped != null;
	}
	
	

	/**
	 * Allows for the removal of Puma sets from the WorkingSet
	 * @param nameToRemove
	 * @return true if successful
	 */
	public static final boolean removePuma(String nameToRemove) {
		//find the puma set associated with the given name
		Puma toRemove = null;
		for(Puma p : pumaSet.keySet())
			if(p.getName().equalsIgnoreCase(nameToRemove)) {
				toRemove = p;
				break;
			}
		//remove the set from the map if one was found
		if(toRemove != null) {
			pumaSet.remove(toRemove);
			return true;
		} else return false;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Setter for the canna calibration table
	 * @param new cal table to use
	 */
	public static final void updateCalibration(CalTable newer) {
		current = newer;
	}
	
	
	
	/**
	 * Getter for the canna calibration table
	 * @return cal table being used
	 */
	public static final CalTable getCalibration() {
		return current;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Merges the keys and values of the 2 mapped requests.
	 * The new key to be used will be the parent.
	 * @param parent request
	 * @param child request
	 */
	public static final void mergeRequests(Request parent, Request child) {
		DataSet cSet, pSet, merged;
		pSet = workingSet.get(parent);
		cSet = workingSet.get(child);
		merged = DataSet.merge(pSet, cSet);
		workingSet.remove(child);
		workingSet.put(parent, merged);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Adds a request to the working set.
	 * When added, an empty data set with the request name will be
	 * automatically mapped to it.
	 * If the working set already contains a request with the given name,
	 * the user will be prompted to overwrite the request. Existing data stays.
	 * @param request to add
	 */
	public static final void addRequest(Request toAdd) {
		if(contains(toAdd.getName()))
			MainMenu.HANDLER_INTERFACE.showConfirmDialog(
				"The Working Set already contains the following sample: "+toAdd.getName()
				+"\nWould you like to overwrite it?","Overwrite Confirmation",
				new Runnable() {		// runnable for affirmative
					@Override public void run() {
						Terminal.say("Proceeding to overwrite...");
						workingSet.put(toAdd, workingSet.get(toAdd));
					}
				},
				new Runnable() {		// runnable for negative
					@Override public void run() {
						Terminal.say("Proceeding to NOT overwrite...");
					}
				}
			);
		else {
			Terminal.say("Adding "+toAdd.getName()+" to the working set with an empty data set...");
			workingSet.put(toAdd, new DataSet(toAdd.getName()));
		}
	}
	
	
	
	/**
	 * Adds a Puma request set.
	 * If an existing instance of the puma set is found, nothing happens,
	 * since they ought to be completely the same anyway.
	 * @param puma set to add
	 */
	public static final void addPuma(Puma toAdd) {
		if(!containsPuma(toAdd.getName())) {
			Terminal.say("Adding "+toAdd.getName()+" to the working set with an empty data set...");
			DataSet[] emptySets = new DataSet[toAdd.getNames().length];
			for(int i = 0; i < emptySets.length; i++)
				emptySets[i] = new DataSet(toAdd.getNames()[i]);
			pumaSet.put(toAdd, emptySets);
		} else Terminal.say("Couldn't add PUMA set because it already exists: "+toAdd.getName());
	}
	
	
	
	
	
	
	
	
	
	
	

	
	
	
	/**
	 * Appends additional data to a request.
	 * If the request isn't found in the map, it will be automatically added.
	 * @param child data set to merge into map
	 * @param request key
	 * @return true if no exception occurred
	 */
	public static final boolean addData(DataSet toAdd, Request toAddTo) {
		try {
			DataSet parent = getDataSetOf(toAddTo);
			if(parent == null)
				Terminal.say("\t\t\t\t\tNo mapping for "+toAddTo.getName()+" in the working set. Adding now...");
			else {
				Terminal.say("\t\t\t\t\tAdding data to "+toAddTo.getName()+" in working set...");
				toAdd = DataSet.merge(parent,toAdd);
			}
			workingSet.put(toAddTo, toAdd);
			return true;
		} catch (NullPointerException e) {
			return false;
		}
	}
	
	
	
	/**
	 * Allows for the appending of additional data to a puma set element.
	 * Uses the sample name of the data set to try and find the specific puma set element.
	 * @param data set to add
	 * @return true if no exception was thrown
	 */
	public static final boolean addPumaData(DataSet toAdd) {
		try {
			Terminal.say("Attempting to add puma data for "+toAdd.getSample());
			if(containsPuma(toAdd.getSample())) {
				int index = Puma.inferDataSetIndex(toAdd.getSample());
				Terminal.say("Adding puma data to index "+index);
				Puma p = fetchPuma(toAdd.getSample());
				DataSet[] ds = pumaSet.get(p);
				ds[index] = DataSet.merge(ds[index],toAdd);
				pumaSet.put(p,ds);
				return true;
			} else Terminal.say("Couldn't add data set to puma element "+toAdd.getSample());
			return false;
		} catch(Exception e) {
			Terminal.say("Couldn't add data set to puma element due to exception"+toAdd.getSample());
			return false;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Check if the name is matched by any existing request
	 * @param request name
	 * @return true if contained
	 */
	public static final boolean contains(String requestName) {
		return fetchRequest(requestName) != null;
	}
	
	
	
	/**
	 * Checks if the puma sheet name is in an existing set.
	 * Puma Sheet Name style: RAFFINATE_P1.1_20210506
	 * @param name in puma sheet style
	 * @return true if part of an existing puma set
	 */
	public static final boolean containsPuma(String sheetName) {
		return fetchPuma(sheetName) != null;
	}
	
	
	
	
	

	
	
	
	
	
	
	
	
	/**
	 * Finds a mapping for the given request name
	 * @param request name string
	 * @return request instance with matching name or null
	 */
	public static final Request fetchRequest(String requestName) {
		for(Request r : workingSet.keySet())
			if(r.getName().equalsIgnoreCase(requestName))
				return r;
			else continue;
		return null;
	}
	 
	
	
	/**
	 * Finds a mapping for the given set name
	 * @param puma set name string
	 * @return set instance with matching name or null
	 */
	public static final Puma fetchPuma(String pumaName) {
		Terminal.say("\t\t\tTrying to find PUMA set for "+pumaName+" in the working set...");
		String suffix;
		LocalDate date;
		if(Puma.nameIsFromPumaLC(pumaName)) try {
				Terminal.say("\t\t\t\tThis puma is an LC sheet type");
				String[] tokens = pumaName.split("_");
				date =  SampleFactory.parseDate(tokens[2]);
				suffix = tokens[1];
				if(!suffix.startsWith("P"))
					suffix = "P"+suffix;
			} catch (Exception e) {
				Terminal.say("\t\t\t\tError parsing LC Puma name.");
				Terminal.specifyFullError(e);
				return null;
			}
		else try {
				Terminal.say("\t\t\t\tThis is probably a GC puma name...");
				String missingId = pumaName.split("#")[0];
				String[] tokens = missingId.split("_");
				date = SampleFactory.parseDate(tokens[1]);
				suffix = tokens[2];
			} catch (Exception e) {
				Terminal.say("\t\t\tError parsing GC Puma name.");
				Terminal.specifyFullError(e);
				return null;
			}
		for(Puma p : pumaSet.keySet())
			if(p.getSuffix().equalsIgnoreCase(suffix)
					&& SampleFactory.assembleDate(date).equalsIgnoreCase( SampleFactory.assembleDate(p.getDate()))) {
				Terminal.say("\t\t\t"+pumaName+" belongs to existing "+p.getName()+"!");
				return p;
			} else continue;
		Terminal.say("\t\t\tNo puma with the suffix "+suffix+" and date "+date+" was fetched!");
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Returns a list of current request mappings
	 * @return all requests
	 */
	public static List<Request> getRequests() {
		List<Request> list = new ArrayList<>();
		list.addAll(workingSet.keySet());
		return list;
	}
	
	
	
	/**
	 * Returns a list of current PUMA set mappings
	 * @return all pumas
	 */
	public static List<Puma> getPumas() {
		List<Puma> list = new ArrayList<>();
		list.addAll(pumaSet.keySet());
		return list;
	}
	
	
	
	
	
	
	
	
	
	
	/**
	 * Finds the data set of a request
	 * @param request to search for
	 * @return data set mapping found
	 */
	public static final DataSet getDataSetOf(Request r) {
		for(Request other : workingSet.keySet())
			if(other.getName().equalsIgnoreCase(r.getName()))
				return workingSet.get(other);
			else continue;
		return null;
	}
	
	
	
	/**
	 * Finds the data sets of a puma set
	 * @param puma set to search for
	 * @return ordered array of data sets found
	 */
	public static DataSet[] getDataSetsOf(Puma p) {
		return pumaSet.get(p);
	}
	
	
	
	
	
	
	
	
	


	/**
	 * Replaces a request mapping with another.
	 * Leaves the data unaltered.
	 * @param newer request
	 * @param older request
	 */
	public static final void swapRequest(Request newer, Request older) {
		DataSet data = workingSet.remove(older);
		workingSet.put(newer, data);
	}
	
	
	
	/**
	 * Used for inserting individual ND fTHC packets from the main menu.
	 * @param request needing packet
	 * @param nd packet
	 */
	public static void insertFthcPacket(Request r, DataPacket nd) {
		DataSet d = getDataSetOf(r);
		d.insertFthcPacket(LC_BY_RT.THC_d9.getSheetName(),nd);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//delimits the request and the data in the working set .dat file
	private static final String DELIMITER = "@DATA_DELIMITER";

	/**
	 * Loads singleton data from the persistent data file
	 * @throws Exception if the file couldn't be read
	 */
	private static final void loadSetFile() throws Exception {
		Terminal.say("\t\tLoading WorkingSet contents from "+srcpath+"...");
		final String[] lines = Storage.readLines(srcpath);
		boolean first = true;					//the first line is the canna cal table
		boolean failedRequestParse = false;
		for(String line : lines)
			//once a request parse if failed, it is assumed all remaining lines deserialize to PUMA sets
			if(failedRequestParse) {
				String[] tokens = line.split(DELIMITER);
				Terminal.say("\n\t\tDeserializing puma: "+tokens[0]);
				Terminal.say("\t\tDeserialized puma data: "+ tokens[1]);
				Puma key = Storage.deserialize(tokens[0], new Puma(Puma.Type.ONE3,LocalDate.now(),new Batch(0.0)));
				DataSet[] example = new DataSet[key.getNames().length];
				Arrays.fill(example, new DataSet(key.getName(),new Result[] {}));
				DataSet[] val = Storage.deserialize(tokens[1], example);
				pumaSet.put(key, val);
			//the first line should be the canna cal table
			} else if (first) {
				Terminal.say("\n\t\tDeserializing calibration...");
				current = Storage.deserialize(line, new CalTable(new HashMap<String,Range>() , LocalDate.now()));
				first = false;
			//All lines below the first and before the PUMAs are assumed to be typical requests
			} else try {
				String[] tokens = line.split(DELIMITER);
				Terminal.say("\n\t\tDeserializing request: "+tokens[0]);
				Terminal.say("\t\tDeserialized data set: "+ tokens[1]);
				Request key = Storage.deserialize(tokens[0], new Request("example_20220803_test#3",false,false,false,new String[] {"Not a Cannabinoid"}));
				DataSet val = Storage.deserialize(tokens[1], new DataSet(key.getName(),new Result[] {}));
				workingSet.put(key, val);
			//this is basically a carbon copy of the first case in this if block, it
			//just triggers the loop to start parsing PUMAs from here on out as well. Probably just shouldn't use a foreach, but too late.
			} catch (JsonSyntaxException e) {
				failedRequestParse = true;
				String[] tokens = line.split(DELIMITER);
				Terminal.say("\n\t\tDeserializing puma: "+tokens[0]);
				Terminal.say("\n\t\tDeserialized puma data: "+ tokens[1]);
				Puma key = Storage.deserialize(tokens[0], new Puma(Puma.Type.ONE3,LocalDate.now(),new Batch(0.0)));
				DataSet[] example = new DataSet[key.getNames().length];
				Arrays.fill(example, new DataSet(key.getName(),new Result[] {}));
				DataSet[] val = Storage.deserialize(tokens[1], example);
				pumaSet.put(key, val);
			//logs any unexpected parse errors
			} catch (Exception e) {
				Terminal.say("\n\t\tError in working set persistent file in line: "+line);
				continue;
			}
		Terminal.say("\n\tSuccessfully loaded WorkingSet contents from "+srcpath+"!");
	}
	
	
	
	/**
	 * Writes class data to persistent file
	 * @throws Exception if the file couldn't be written
	 */
	private static final void write() throws Exception {
		Terminal.say("\tWriting WorkingSet contents to the filesystem...");
		ArrayList<String> lines = new ArrayList<>(workingSet.size()+1);
		lines.add(Storage.serialize(current));
		for(Request r : workingSet.keySet())
			lines.add(Storage.serialize(r)+DELIMITER+Storage.serialize(workingSet.get(r)));
		for(Puma p : pumaSet.keySet())
			lines.add(Storage.serialize(p)+DELIMITER+Storage.serialize(pumaSet.get(p)));
		Storage.writeLines(lines.toArray(new String[]{}), srcpath);
		Terminal.say("\tSuccessfully wrote working set contents!");
	}


	
	
	
	
	





	
}
