package en.talond.simGUI.data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import en.talond.simGUI.Terminal;
import en.talond.simGUI.data.Result.*;
import en.talond.simGUI.report.ReportTable;






/**
 * This is used to wrap a set of result maps.
 * @author talon
 *
 */
public class DataSet {


	
	private final String sample;
	//analysis-specific mappings for detected compounds to results
	private final Map<String,List<DataPacket>> 
	ppmCannaResults,
	massCannaResults,
	gcResults,
	thcResults;
	
	
	
	/**
	 * Empty set constructor
	 * @param name of sample request
	 */
	public DataSet(final String ofSample) {
		this.sample = ofSample;
		gcResults = new HashMap<>();
		ppmCannaResults = new HashMap<>();
		massCannaResults = new HashMap<>();
		thcResults = new HashMap<>();
	}
	
	
	
	/**
	 * Constructor from parsed results
	 * @param sample name
	 * @param results
	 */
	public DataSet(final String ofSample, final Result[] results) {
		this.sample = ofSample;
		gcResults = Factory.generateGCMap(Factory.parseForGC(results),ofSample);
		ppmCannaResults = Factory.generatePPMMap(Factory.parseForPPM(results),ofSample);
		massCannaResults = Factory.generateMassMap(Factory.parseForCanna(results),ofSample);
		thcResults = Factory.generateTHCMap(Factory.parseForTHC(results),ofSample);
	}

	
	
	/**
	 * Constructs with defined inner fields.
	 * Used for JSON deserialization.
	 * @param sample name
	 * @param gc map
	 * @param ppm map
	 * @param canna map
	 * @param fthc map
	 */
	public DataSet(final String ofSample,
			final Map<String,List<DataPacket>> gc, final Map<String,List<DataPacket>> ppm,  
			final Map<String,List<DataPacket>> canna,  final Map<String,List<DataPacket>> thc) {
		this.sample = ofSample;
		gcResults = gc;
		ppmCannaResults = ppm;
		massCannaResults = canna;
		thcResults = thc;

	}
	
	
	
	
	
	
	/**
	 * @return the name of the sample this data set wraps
	 */
	public final String getSample() { 
		return sample; 
	}
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Fetches the most recently submitted data packet for a compound.
	 * The most recent SHOULD be the one at the end of the mapped list
	 * @param compound to look for
	 * @return null or most recent
	 */
	public final DataPacket getLatestGCResultOf(final String compound) {
		final DataPacket[] data = getGCResultsOf(compound);
		if(data.length > 0) 
			return data[data.length-1];
		else return null;
	}
	
	
	
	/**
	 * Attempts to find fTHC result for THC-d9
	 * @return null or data
	 */
	public final DataPacket[] getFthcResults() { return getTHCMassResultsOf(ReportTable.LC_BY_RT.THC_d9.getSheetName()); }
	
	
	
	//data indicators
	public final boolean hasCanna() { return massCannaResults.size() > 0 | ppmCannaResults.size() > 0; }
	public final boolean hasFthc() { return thcResults.size() > 0; }
	public final boolean hasGC() { return gcResults.size() > 0; }
	public final boolean canBeConfident(String onCompound) { return DataPacket.useRSDToMinimize(getCannaMassResultsOf(onCompound)).length > 1; }
	//name getters for detected compounds
	public final String[] getCompoundsOfGC() { return gcResults.keySet().toArray(new String[] {}); }
	public final String[] getCompoundsOfPPM() { return ppmCannaResults.keySet().toArray(new String[] {}); }
	public final String[] getCompoundsOfMass() { return massCannaResults.keySet().toArray(new String[] {}); }
	//result getters for individual compounds 
	public final DataPacket[] getGCResultsOf(String compound) 			{ return tryGettingResults(compound,gcResults); }	
	public final DataPacket[] getCannaPPMResultsOf(String compound) 	{ return tryGettingResults(compound,ppmCannaResults); }	
	public final DataPacket[] getCannaMassResultsOf(String compound) 	{ return tryGettingResults(compound,massCannaResults); }	
	public final DataPacket[] getTHCMassResultsOf(String compound) 		{ return tryGettingResults(compound,thcResults); }
	

	
	/**
	 * Attempts to fetch the results of a compound from a map.
	 * Automatically returns an empty array in the event of a null result
	 * from the map.
	 * @param name of compound to fetch
	 * @param result map to fetch from
	 * @return empty array OR collected data
	 */
	private static final DataPacket[] tryGettingResults(String ofCompound, Map<String,List<DataPacket>> fromMap) {
		try { 
			return fromMap.get(ofCompound).toArray(new DataPacket[] {});
		} catch (final NullPointerException e) {
			return new DataPacket[] {};
		}
	}

	
	
	
	
	
	
	
	
	/**
	 * Determines if this data set represents a desolventized sample
	 * @return true if has an original mass
	 */
	public final boolean shouldUseMassPercent() {
		int numMassResults = getCompoundsOfMass().length;
		if(numMassResults > 0)
			return true;
		else return false;
	}
	
	
	
	
	
	
	
	/**
	 * Adds an individual packet to the fthc map.
	 * Almost all packets should be added upon original sheet parse or through
	 * merging with other data sets. However, the technicians need to be able to
	 * add individual nondetect packets from the MainMenu. Hence, this.
	 * @param compound packet belongs to (shouldn't really be anything other than THC-d9)
	 * @param fthc packet
	 * @see WorkingSet.insertFthcPacket		and 	 MainMenu.handleAddNondetect
	 */
	public final void insertFthcPacket(String compound, DataPacket fthc) {
		List<DataPacket> list = thcResults.remove(compound);
		try {
			list.add(fthc);
		} catch (NullPointerException e) {
			list = new LinkedList<>();
			list.add(fthc);
		}	thcResults.put(compound, list);
	}
	
	
	

	
	
	
	
	
	/**
	 * Merges two data sets
	 * @param data set to merge into
	 * @param data set to pull from
	 * @return merged data set
	 */
	public static final DataSet merge(final DataSet parent, final DataSet child) {
		//Generate a merged map for each of the analysis methods, omitting possible duplicate results
		final Map<String,List<DataPacket>> mergedGC = tryMerge(parent.gcResults,child.gcResults);
		final Map<String,List<DataPacket>> mergedPPM = tryMerge(parent.ppmCannaResults,child.ppmCannaResults);
		final Map<String,List<DataPacket>> mergedCanna = tryMerge(parent.massCannaResults,child.massCannaResults);
		final Map<String,List<DataPacket>> mergedTHC = tryMerge(parent.thcResults,child.thcResults);
		//assemble a new data set and return it
		return new DataSet(parent.getSample(),mergedGC,mergedPPM,mergedCanna,mergedTHC);
	}

	

	/**
	 * Merges two compound maps, ignoring any duplicate results.
	 * Duplicate results are specified by the DataSet's implementation of this equals(DataSet) function
	 * @param parent map
	 * @param child map
	 * @return parent map with merged lists
	 */
	private static final Map<String,List<DataPacket>> tryMerge(Map<String,List<DataPacket>> parent, Map<String,List<DataPacket>> child) {
		//Iterates through each compound in the child map
		for(final String compound : child.keySet())
			//Puts the child data into the parent list if the parent has no record of the current compound
			if(!parent.containsKey(compound))
				parent.put(compound,child.get(compound));
			//Merges the result lists if the parent DOES contain the current compound, ignoring duplicates
			else {
				//collect the result lists from both maps
				List<DataPacket> parentResults = parent.get(compound),
				childResults = child.get(compound);
				//Finds duplicate results in the child list and tells the user it will remove them
				for(DataPacket parentResult : parentResults) {
					DataPacket toRemove = null;			//attempt to find a duplicate result for each in parent
					for(DataPacket childResult : childResults)
						if(childResult.equals(parentResult))
							toRemove = childResult;
						else continue;
					if(toRemove != null) {				//if a duplicate was found, remove it and notify the user
						childResults.remove(toRemove);
						toRemove = null;
						Terminal.say("Highly likely that a duplicate result was found. Neglecting child...");
					}
				}
				//Adds remaining results to the parent's list
				parentResults.addAll(childResults);
				//Put the merged parent list in the parent map
				parent.put(compound, parentResults);
			}
		return parent;
	}




	
	
	
	
	
	
	/**
	 * This is replete with boilerplate that would otherwise clutter
	 * the very important DataSet class
	 * @author talon
	 *
	 */
	private static final class Factory {

		/**
		 * @param unparameterized result array to parse
		 * @return only results of class GCResult
		 */
		private static final GCResult[] parseForGC(final Result[] toParse) {
			final List<GCResult> list = new LinkedList<>();
			for(final Result r : toParse)
				if(r instanceof GCResult)
					list.add((GCResult) r);
			return list.toArray(new GCResult[] {});
		}
	
		/**
		 * @param unparameterized result array to parse
		 * @return only results of class PPMResult
		 */
		private static final PPMResult[] parseForPPM(final Result[] toParse) {
			final List<PPMResult> list = new LinkedList<>();
			for(final Result r : toParse)
				if(r instanceof PPMResult)
					list.add((PPMResult) r);
			return list.toArray(new PPMResult[] {});
		}
	
		/**
		 * @param unparameterized result array to parse
		 * @return only results of class FTHCResult
		 */
		private static final FTHCResult[] parseForTHC(final Result[] toParse) {
			final List<FTHCResult> list = new LinkedList<>();
			for(final Result r : toParse)
				if(r instanceof FTHCResult)
					list.add((FTHCResult) r);
			return list.toArray(new FTHCResult[] {});
		}
	
		/**
		 * @param unparameterized result array to parse
		 * @return only results of class CannaResult
		 */
		private static final CannaResult[] parseForCanna(final Result[] toParse) {
			final List<CannaResult> list = new LinkedList<>();
			for(final Result r : toParse)
				if(r instanceof CannaResult)
					list.add((CannaResult) r);
			return list.toArray(new CannaResult[] {});
		}
	
		
		
		/**
		 * @param results to map
		 * @param sample name since this is a static method with no access to instance data
		 * @return mapping of compounds from the specified method to a list numerical result Pairs
		 */
		private static final Map<String,List<DataPacket>> generateGCMap(final GCResult[] results, final String ofSample) {
			Map<String,List<DataPacket>> compoundMap = new HashMap<>();
			for(GCResult r : results)
				compoundMap = insertCompound(compoundMap,r);
			return compoundMap;
		}
	
		/**
		 * @param results to map
		 * @param sample name since this is a static method with no access to instance data
		 * @return mapping of compounds from the specified method to a list numerical result Pairs
		 */
		private static final Map<String,List<DataPacket>> generatePPMMap(final PPMResult[] results, final String ofSample) {
			Map<String,List<DataPacket>> compoundMap = new HashMap<>();
			for(PPMResult r : results)
				compoundMap = insertCompound(compoundMap,r);
			return compoundMap;
		}
	
		/**
		 * @param results to map
		 * @param sample name since this is a static method with no access to instance data
		 * @return mapping of compounds from the specified method to a list numerical result Pairs
		 */
		private static final Map<String,List<DataPacket>> generateMassMap(final CannaResult[] results, final String ofSample) {
			Map<String,List<DataPacket>> compoundMap = new HashMap<>();
			for(final CannaResult r : results)
				compoundMap = insertCompound(compoundMap,r);
			return compoundMap;
		}
	
		/**
		 * @param results to map
		 * @param sample name since this is a static method with no access to instance data
		 * @return mapping of compounds from the specified method to a list numerical result Pairs
		 */
		private static final Map<String,List<DataPacket>> generateTHCMap(final FTHCResult[] results, final String ofSample) {
			Map<String,List<DataPacket>> compoundMap = new HashMap<>();
			for(FTHCResult r : results)
				compoundMap = insertCompound(compoundMap,r);
			return compoundMap;
		}
	
	
		
		/**
		 * Contains logic for inserting/appending data into a compound map
		 * @param map to insert into
		 * @param result to insert
		 * @return modified map
		 */
		private static final Map<String,List<DataPacket>> insertCompound(Map<String,List<DataPacket>> toHold, Result toInsert) {
			String compound = toInsert.getCompound();		//obtains key for map
			DataPacket numerical = toInsert.toPacket();		//transforms abstract result
			//if the map already contains results for this compound, first ensure that is not a duplicate
			//before adding the result to the end of the value list
			if(toHold.containsKey(compound)) {
				List<DataPacket> previousResults = toHold.get(compound);	//collect the previous results
				//iterate through the existing results, ensuring no duplicates are found
				boolean isDuplicate = false;
				for(DataPacket dp : previousResults)
					if(dp.equals(numerical)) {
						isDuplicate = true;
						break;
					}
				//manage the insertion of the result based on duplication status
				if(isDuplicate) 
					Terminal.say("\t\t\t\t\tDuplicate result rejected during data set insertion, ignoring: "+toInsert.getCompound());
				else {
					previousResults.add(numerical);
					toHold.put(compound, previousResults);
				}
			//if the result is for a novel compound, create the container list and insert it
			} else {
				List<DataPacket> list = new LinkedList<>();
				list.add(numerical);
				toHold.put(compound, list);
			}
			return toHold;
		}
	}


	
	
	
	
	


	/**
	 * Turns this object into a string displaying the inner fields in a nice manner. 
	 * Primarily for debugging and testing purposes.
	 * @return stringified object
	 */
	public final String stringify() {
		String stringified = "DataSet for sample: "+sample+"\n";
		stringified += "\tGC RESULTS:\n";
		for(final String compound : gcResults.keySet()) {
			stringified += "\t\tCOMPOUND: "+compound+"\n";
			for(final DataPacket p : getGCResultsOf(compound))
				stringified = stringified + "\t\t\tAREA: "+p.getArea()+"\tAREA %: "+p.getResult()														+"\t\t"+p.getSource()+"\n";
		}
		stringified += "\tPPM RESULTS:\n";
		for(final String compound : ppmCannaResults.keySet()) {
			stringified += "\t\tCOMPOUND: "+compound+"\n";
			for(final DataPacket p : getCannaPPMResultsOf(compound))
				stringified = stringified + "\t\t\tAREA: "+p.getArea()+"\tDF: "+p.getDilution()+"\tPPM: "+p.getResult()									+"\t\t"+p.getSource()+"\n";
		}
		stringified += "\tTHC RESULTS:\n";
		for(final String compound : thcResults.keySet()) {
			stringified += "\t\tCOMPOUND: "+compound+"\n";
			for(final DataPacket p : getTHCMassResultsOf(compound))
				stringified = stringified + "\t\t\tAREA: "+p.getArea()+"\tDF: "+p.getDilution()+"\tMASS %: "+p.getResult()+"\tAMOUNT: "+p.getAmount()+"\t\t"+p.getSource()+"\n";
		}
		stringified += "\tCANNA RESULTS:\n";
		for(final String compound : massCannaResults.keySet()) {
			stringified += "\t\tCOMPOUND: "+compound+"\n";
			for(final DataPacket p : getCannaMassResultsOf(compound))
				stringified = stringified + "\t\t\tAREA: "+p.getArea()+"\tDF: "+p.getDilution()+"\tMASS %: "+p.getResult()+"\tAMOUNT: "+p.getAmount()+"\t\t"+p.getSource()+"\n";
		}
		return stringified;
	}

}
