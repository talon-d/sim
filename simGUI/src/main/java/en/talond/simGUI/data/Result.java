package en.talond.simGUI.data;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import en.talond.simGUI.Terminal;
import en.talond.simGUI.io.DataSheet;
import en.talond.simGUI.io.DataSheet.Method;
import en.talond.simGUI.io.DataSheet.ResultRow;
import en.talond.simGUI.io.DataSheet.SheetData;
import en.talond.simGUI.report.ReportTable;









/**
 * A result is the final parameterized representation of a single
 * 	analysis result. Used by the DataSet class to contain
 * 	all the results of a single sample. Created from RawData
 * @author talon
 * @see DataSet, RawData
 *
 */
public abstract class Result {


	
	private final String source;
	private final Double amount;
	private final String compound;
	private final Double area;	
	
	
	
	/**
	 * 
	 * @param agilent data file source
	 * @param prep amount (mg/L), should be 0 for ppm or gc methods
	 * @param compound this result wraps
	 * @param detected chromatogram area
	 */
	public Result(final String source, final Double amount, final String compound, final Double area) {
		this.source = source;
		this.amount = amount;
		this.compound = compound;
		this.area = area;
	}
	
	
	
	public final String getSource() 		{ return source; }
	public final Double getAmount()			{ return amount; }
	public final String getCompound() 		{ return compound; }
	public Double getArea()					{ return area; }
	
	
	
	
	
	
	
	/**
	 * @param other result
	 * @return true if equivalent
	 */
	public final boolean equals(Result other) {
		return (source.equals(other.source) && compound.equals(other.compound)
				&& amount == other.amount && area == other.area);
	}
	
	
	
	
	
	
	
	/**
	 * Allows a result to transform its inner fields into the generic data packet object
	 * @return data packet representation of result
	 */
	public abstract DataPacket toPacket();
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Parses a DataSheet's RawData into a the given map and merges it
	 * @param sheet to parse
	 * @param map to merge results into
	 * @return merged map
	 */
	public static final Map<String,Result[]> parseSheet(final DataSheet s, final Map<String,Result[]> toMergeInto) {
		Terminal.say("\tTaking results off of "+s.getSrc().getAbsolutePath());
		//Get a list of samples found on the data sheet
		//Iterate through each sample, collect its raw data, then turn the raw data into Result classes
		for(final String sample : s.getSamples()) {
			Terminal.say("\t\tCollecting data of "+sample);
			final SheetData[] sampleResults = s.getDataOf(sample);
			Result[] results = parseSheetData(sample,sampleResults).toArray(new Result[] {});
			//Get existing results for this sample in the event the argument map already contains some results and merge with the new ones
			if(toMergeInto.containsKey(sample))
				results = mergeWithoutDuplication(toMergeInto.get(sample),results);
			//Place all the results into the map
			toMergeInto.put(sample,results);
		}
		return toMergeInto;
	}
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Merges the results of two samples with different names, or refactors the child name to the parent name.
	 * @param nameToInherit
	 * @param nameToPullFrom
	 * @param map to use
	 * @return altered map
	 */
	public static final Map<String,Result[]> mergeDifferentNames(final String nameToInherit, final String nameToPullFrom, final Map<String,Result[]> map) {
		Terminal.say("\t\tAttempting to add "+nameToPullFrom+" results to "+nameToInherit+" from Result.mergeDifferentNames");
		//Attempt to pull the child results and merge them into the parent results. Remove the child key from the map.
		final Result[] pulledResults = map.get(nameToPullFrom);		//Child results
		final Result[] destinationResults = map.get(nameToInherit);	//parent results
		final Result[] both;										//final array for merged results
		if(pulledResults != null && destinationResults != null) {
			Terminal.say("\t\t\tMerge operation detected!");
			both = mergeWithoutDuplication(destinationResults,pulledResults);
			map.remove(nameToPullFrom);
			map.put(nameToInherit, both);
		} else if (pulledResults != null) {
			Terminal.say("\t\t\tRefactor operation detected!");
			both = pulledResults;
			map.remove(nameToPullFrom);
			map.put(nameToInherit, both);
		} else
			Terminal.say("\t\t\tNo parent results available for operation. Returning unaltered map...");
		return map;
	}
	
	
	
	
	
	
	
	
	
	
	

	/**
	 * Inner factory method to add prevent double-appending of results to a merged array
	 * @param parent results (all will be kept)
	 * @param child results (ones matching a parent will be ignored)
	 * @return array of unique results
	 */
	public static final Result[] mergeWithoutDuplication(Result[] parents, Result[] children) {
		Terminal.say("\t\t\t\t\tdoing result merge...");
		List<Result> merged = new LinkedList<>();
		List<Integer> duplicateChildren = new LinkedList<>();
		for(Result parent : parents) {
			for(int i = 0; i < children.length; i++)
				if(parent.equals(children[i]) && !duplicateChildren.contains(i))
					duplicateChildren.add(i);
			merged.add(parent);
		}
		for(int i = 0; i < children.length; i++)
			if(!duplicateChildren.contains(i))
				merged.add(children[i]);
		Terminal.say("\t\t\t\t\tomitted "+(parents.length+children.length-merged.size())+" identical results!");
		return merged.toArray(new Result[] {});
	}
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Uses sample name expressions to merge plain GC puma sample names with the special LC puma names
	 * For example, if given a map which contains the keys 14S_20220506_P1.3#103 and
	 * RAFFINATE_P1.3_20220506, both results will be merged into the key RAFFINATE_P1.3_20220506
	 * @param unmerged map of results
	 * @return potentially merged map
	 * @throws DataFormatException 
	 */
	public static final Map<String,Result[]> mergeGcAndLcOfPuma(Map<String,Result[]> map) throws DataFormatException {
		//log function args
		Terminal.say("\tMerging puma LC and GC results...");
    	Terminal.say("\t\tResults premerge: ");
    	for(String key : map.keySet())
    		Terminal.say("\t\t\t"+key);
		List<MergeCluster> pumasToMerge = new LinkedList<>();
		for(final String key : map.keySet())
			if(key.matches(Puma.PUMA_GC_RESULT_NAME)) {
				Terminal.say(key+"is a GC puma member!");
				final String lcNameToMergeInto = Puma.standardizePumaGcName(key);
				pumasToMerge.add(new MergeCluster(lcNameToMergeInto,key));
			}
		map = MergeCluster.mergeMap(map, pumasToMerge);
		//log function results
		Terminal.say("\t\tResults postmerge:");
    	for(String key : map.keySet())
    		Terminal.say("\t\t\t"+key);
    	Terminal.say("\tFinished merged. Returning map...");
		return map;
	}
	
	/**
	 * Data cluster to reduce method calls in above method
	 * @author talon
	 *
	 */
	private static final class MergeCluster {
		//instance fields
		private final String parent, child;
		/**
		 * Minor cluster object for elements to merge in a map.
		 * Helps prevent ConcurrentModificationException when managing a map from within an iterator on the key set.
		 * @param parent name
		 * @param results for parent
		 * @param child name
		 * @param results for child
		 */
		private MergeCluster(String parent, String child) {
			this.parent = parent;
			this.child = child;
		}
		/**
		 * Factory method for altering a map with a set of merge clusters
		 * @param toChange
		 * @param changes
		 * @return
		 */
		private static final Map<String,Result[]> mergeMap(Map<String,Result[]> toChange, List<MergeCluster> changes) {
			for(MergeCluster change : changes)
				toChange = Result.mergeDifferentNames(change.parent,change.child, toChange);
			return toChange;
		}
	}
	
	
	
	
	
	
	
	
	/**
	 * Parses raw sheet data into a list of results
	 * @param <Data> extends Result
	 * @param sample name
	 * @param raw data of sample
	 * @return parameterized data of sample
	 */
	private static final <Data extends Result> List<Data> parseSheetData(final String sample, final SheetData[] ofSample) {
		final List<Data> list = new LinkedList<>();
		for(final SheetData s : ofSample)
			for(final ResultRow r : s.getData())
				list.add(parseRaw(sample,r,s.getSource(),s.getAmount()));
		return list;
	}


	
	
	
	
	

	/**
	 * Takes in a result row and its key elements to generate a result
	 * @param <Data> extends Result
	 * @param sample name
	 * @param raw data row
	 * @return interpreted result or null if it isn't a valid row
	 */
	@SuppressWarnings("unchecked")
	private static final <Data extends Result> Data parseRaw(final String sample, final ResultRow d, final String path, final Double amount) {
		//Makes sure the given row isn't null to prevent crashes
		if(d != null) {
			// This if-block finds the type of Result to create from the given RawData
			if(isValidGCRow(d)) {				//For GC results...
				final String compound = d.getCompound();
				final Double area = d.getArea();
				final Double peakAreaPercent = d.getAreaP();
				return (Data) new GCResult(path,d.getDesc(),compound,area,peakAreaPercent);
			} else if (isValidPPMRow(d)) {		//For PPM results...
				final String compound = d.getCompound();
				final Double area = d.getArea();
				final Double dilution = convertDescToDF(d.getDesc());
				final Double dilutePPM = d.getPPM();
				return (Data) new PPMResult(path,amount,compound,area,dilution,dilutePPM);
			} else if (isValidCannaRow(d)) {	//For canna results...
				final String compound = d.getCompound();
				final Double area = d.getArea();
				final Double dilution = convertDescToDF(d.getDesc());
				final double massPercent = d.getMassP();
				return (Data) new CannaResult(path,amount,compound,area,dilution,massPercent);
			} else if (isValidFTHCRow(d)) {		//For fTHC results...
				String compound = d.getCompound();
				if(compound.equalsIgnoreCase(ReportTable.LC_BY_RT.THC_d9.getSheetName()))
					compound = ReportTable.LC_BY_RT.THC_d9.getSheetName();
				final Double area = d.getArea();
				final Double dilution = convertDescToDF(d.getDesc());
				final double massPercent = d.getMassP();
				return (Data) new FTHCResult(path,amount,compound,area,dilution,massPercent);
			} else
				Terminal.say("\t\t\tInvalid result row given! Data not appended to "+sample+". Maybe implement new case in Result.parseRaw?");
		} else
			Terminal.say("\t\t\tNull result row given! Data not appendeOd to "+sample);
		return null;
	}

	

	
	
	
	
	
	/**
	 * PPM rows have the Canna method and an invalid mass percent
	 * @param data
	 * @return true if PPM
	 */
	private static final boolean isValidPPMRow(final ResultRow d) {
		return d.getMethod() == Method.CANNA && d.getMassP() == DataSheet.INVALID_MASS_PERCENT;
	}
	/**
	 * GC rows have the GC method and an invalid mass percent
	 * @param data
	 * @return true if GC
	 */
	private static final boolean isValidGCRow(final ResultRow d) {
		return d.getMethod() == Method.GC && d.getMassP() == DataSheet.INVALID_MASS_PERCENT;
	}
	/**
	 * Canna rows have the Canna method and a valid mass percent
	 * @param data
	 * @return true if Canna
	 */
	private static final boolean isValidCannaRow(final ResultRow d) {
		return d.getMethod() == Method.CANNA && d.getMassP() != DataSheet.INVALID_MASS_PERCENT;
	}
	/**
	 * fTHC rows have the fTHC method and a valid mass percent
	 * @param d
	 * @return
	 */
	private static final boolean isValidFTHCRow(final ResultRow d) {
		return d.getMethod() == Method.FTHC && d.getMassP() != DataSheet.INVALID_MASS_PERCENT;
	}
	
	
	
	/**
	 * Turns a RawData description into a numerical dilution factor
	 * @param description
	 * @return dilution factor
	 */
	private static final Double convertDescToDF(final String description) {
		try {
			return Double.parseDouble(description.substring(0, description.length()-1));
		} catch (final StringIndexOutOfBoundsException e) {
			return 0.0;
		}
	}




	
	
	
	
	
	
	
	
	





	/**
	 * Abstract intermediary class for results with valid mass percents
	 * @author talon
	 * @see FTHCResult, CannaResult
	 *
	 */
	public static abstract class MassPResult extends Result {
		//instance data
		private final Double dilution, massPercent;
		/**
		 * abstract constructor
		 * @param agilent data file path
		 * @param amount mg/L of prep
		 * @param compound detected
		 * @param area calculated from sheet
		 * @param dilution factor
		 * @param massPercent from sheet
		 */
		private MassPResult(final String path, final Double amount, final String compound, final Double area, final Double dilution, final Double massPercent) {
			super(path,amount,compound,area);
			this.dilution = dilution;
			this.massPercent = massPercent;
		}
		//getters
		public final Double getDilution() { return dilution; }
		protected Double getMassPercent() { return massPercent; }
	}



	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	


	/**
	 * Cannabinoids PPM result from the LC
	 * @author talon
	 *
	 */
	public static class PPMResult extends Result {
		//instance data
		private final Double dilution, actualPPM;
		/**
		 * constructor
		 * @param agilent data file path
		 * @param amount mg/L of prep
		 * @param compound detected
		 * @param area calculated from sheet
		 * @param dilution factor
		 * @param massPercent from sheet
		 */
		private PPMResult(final String path, final Double amount, final String compound, final Double area, final Double dilution, final Double recordedPPM) {
			super(path,amount, compound, area);
			this.dilution = dilution;
			actualPPM = recordedPPM * dilution;
		}
		//getters
		public final Double getDilution()		{ return dilution; }
		public final Double getPPM() 	  	 	{ return actualPPM; }
		@Override public final DataPacket toPacket() {
			final String path = this.getSource();
			final Double prep = 0.0;
			final Double dilution = this.getDilution();
			final Double area = this.getArea();
			final Double ppm = this.getPPM();
			return new DataPacket(path,DataPacket.PPM_TYPE,prep,dilution,area,ppm);
		}
	}





	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	/**
	 * Result for the aviv family of gas chromatography methods
	 * @author talon
	 *
	 */
	public static class GCResult extends Result
	{
		/* instance data */
		private final Double areaP;
		private final String desc;
		/* constructor */
		private GCResult(final String path, final String description, final String compound, final Double area, final Double areaP)
		{
			super(path,0.0,compound,area);
			this.areaP = areaP;
			desc = description;
		}
		/* getters */
		public final Double getAreaPercent() { return areaP; }
		public final String getDescription() { return desc; }
		@Override public final DataPacket toPacket() {
			final String source = this.getSource();
			final String desc = this.getDescription();
			final Double area = this.getArea();
			final Double areaPercent = this.getAreaPercent();
			return new DataPacket(source,DataPacket.GC_TYPE,desc,area,areaPercent);
		}
	}



	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	/**
	 * Cannabinoid result for LC, mass %
	 * @author talon
	 *
	 */
	public static class CannaResult extends MassPResult {
		/**
		 * constructor
		 * @param agilent data file path
		 * @param amount mg/L of prep
		 * @param compound detected
		 * @param area calculated from sheet
		 * @param dilution factor
		 * @param massPercent from sheet
		 */
		private CannaResult(final String path, final Double amount, final String compound, final Double area, final Double dilution, final Double massPercent) { 
			super(path,amount,compound,area,dilution,massPercent); 
		}
		//getters
		@Override public final Double getMassPercent() { return super.getMassPercent(); }
		@Override public final DataPacket toPacket() {
			final String path = this.getSource();
			final Double amount = this.getAmount();
			final Double dilution = this.getDilution();
			final Double area = this.getArea();
			final Double massP = this.getMassPercent();
			return new DataPacket(path,DataPacket.MASS_TYPE,amount,dilution,area,massP);
		}
	}


	
	
	
	
	
	
	
	
	
	

	/**
	 * Mass result that automatically doubles any THC values given to it
	 * @author talon
	 *
	 */
	public static class FTHCResult extends MassPResult {
		private static final String SOLE_FTHC_TARGET = ReportTable.LC_BY_RT.THC_d9.getSheetName();
		/**
		 * constructor
		 * @param agilent data file path
		 * @param amount mg/L of prep
		 * @param compound detected
		 * @param area calculated from sheet
		 * @param dilution factor
		 * @param massPercent from sheet
		 */
		private FTHCResult(final String path, final Double amount, final String compound, final Double area, final Double dilution, final Double massPercent) { 
			super(path,amount,compound,area,dilution,massPercent); 
		}
		/**
		 * Automatically doubles this result if it is THC-d9.
		 * @return mass percent
		 */
		@Override public final Double getMassPercent() {
			Double result = super.getMassPercent();
			if(getCompound().equalsIgnoreCase(SOLE_FTHC_TARGET))
				result = result * 2;
			return result;
		}
		/**
		 * Automatically doubles this area if it is THC-d9.
		 * @return area
		 */
		@Override public final Double getArea() {
			Double area = super.getArea();
			if(getCompound().equalsIgnoreCase(SOLE_FTHC_TARGET))
				area = area * 2;
			return area;
		}
		@Override public final DataPacket toPacket() {
			final String path = this.getSource();
			final Double amount = this.getAmount();
			final Double dilution = this.getDilution();
			final Double area = this.getArea();
			final Double massP = this.getMassPercent();
			return new DataPacket(path,DataPacket.FTHC_TYPE,amount,dilution,area,massP);
		}
	}

}
