package en.talond.simGUI.data;

import java.time.LocalDate;
import java.util.zip.DataFormatException;

import en.talond.simGUI.Terminal;
import en.talond.simGUI.io.SampleFactory;






/**
 * This class allows the user to infer, load, and satisfy a PUMA collection of sample requests.
 *
 * 	A PUMA collection is an ordered array of sample data which is as follows:
 * 		0		1		   2				 n
 * 		{ FEED, RAFFINATE, EXTRACT-1 ... EXTRACT-n }
 *
 * @author talon
 * 
 */
@SuppressWarnings("deprecation")
public class Puma {

	

	public static final int
	//ordered indices of the PUMA set elements
	FEED_INDEX = 0,
	RAFF_INDEX = 1,
	EXTR_START = 2;
	
	public static final String
	//Prefixes for PUMA set sample types
	FEED = "11AD",
	RAFF = "14S",
	EXTR = "15S",
	//prefixes as they should be found on GC sheets
	FEED_GC_PRE_EXP = "11[\\.]?[Aa][Dd]",
	RAFF_GC_PRE_EXP = "14[\\.]?[Ss]",
	EXTR_GC_PRE_EXP = "15[\\.]?[Ss]((\\-)?[1-5])?",
	//Prefixes as they should be found on raw Agilent data sheets
	SHEET_FEED = "FEED",
	SHEET_RAFF = "RAFFINATE",
	SHEET_EXTR = "EXTRACT",
	//An expression used to detect PUMA samples from the Agilent sheets
	SHEET_NAME_EXP = "(FEED|RAFFINATE|(EXTRACT[1-5]?))_[1-9]\\.[1-9]",
	PUMA_GC_RESULT_NAME = "(11[\\.]?AD|14[\\.]?S|15[\\.]?S(-[1-5])?)_[0-9]{8}_P[1-9]\\.[1-9](.*)?",
	//An expression used to find the date of a PUMA sample, inferred from its data source filename
	SOURCE_DATE_MARKER = "MOJO_SSMBEEW[0-9]C[1-9]\\.[0-9]_";

	private static final boolean
	//inferred analysis tags for puma set elements
	PUMA_CANNA_INFERENCE = true,
	PUMA_FTHC_INFERENCE = false,
	FEED_GC_INFERENCE = true,
	RAFF_GC_INFERENCE = true,
	EXTR_GC_INFERENCE = true;
	
	
	
	
	
	
	
	
	
	
	
	

	private final Type pumaType;
	private final LocalDate date;
	private final Batch id;

	
	
	/**
	 * Constructs a PUMA sample set submission.
	 * @param enumerated type of set
	 * @param date of collection
	 * @param block number
	 * @throws Exception
	 */
	public Puma(final Type t, final LocalDate ofSet, final Batch id) {
		pumaType = t;
		date = ofSet;
		this.id = id;
	}

	
	
	/**
	 * Obtain the enumerated type of this set.
	 * @return PUMA set type
	 */
	public final Type getType() { return this.pumaType; }
	
	
	
	/**
	 * Obtain the date this set was collected.
	 * @return date with year, month, day
	 */
	public final LocalDate getDate() { return this.date; }
	
	
	
	/**
	 * Obtain this suffix.
	 * @return string
	 */
	public final String getSuffix() { return pumaType.getSuffix(); }  



	/**
	 * Assemble the complete set name
	 * @return unique name string
	 */
	public final String getName() {
		return "PUMA"+"_"
				+SampleFactory.assembleDate(date)+"_"
				+pumaType.getSuffix()
				+id.toString();
	}
	
	
	
	
	
	
	
	
	
	/**
	 * Obtain the ordered names of each element in this puma set
	 * @return name strings
	 */
	public final String[] getNames() {
		final String postPrefix = SampleFactory.assembleDate(date) +"_"+pumaType.getSuffix() + id.toString();
		final String[] prefixes = pumaType.makePrefixes();
		final String[] names = new String[prefixes.length];
		for(int i = 0; i < names.length; i++)
			names[i] = prefixes[i]+"_"+postPrefix;
		return names;
	}
	
	
	
	
	
	
	
	
	
	
	/**
	 * Assembles an ordered array of inferred requests for this PUMA set
	 * @throws Exception if a request could be inferred
	 */
	public final Request[] generateRequests() throws Exception {
		final Request[] generatedSet = new Request[pumaType.getSize()];
		for(int i = 0; i < generatedSet.length; i++)
			generatedSet[i] = inferRequestFromSetIndex(i);
		return generatedSet;
	}
	
	
	
	/**
	 * This method is used to infer a puma sample request given an index from an ordered puma array
	 * @param index from an ordered array i.e.  { FEED, RAFF, EXT1 ... }
	 * @return inferred request
	 * @throws Exception 
	 */
	private final Request inferRequestFromSetIndex(final int indexOfSample) throws Exception {
		final String prefix = pumaType.makePrefixes()[indexOfSample];		//Get the prefix of the ordered index
		//declare and fill an array of analysis flags as arguments to be given to the constructor
		final boolean[] flags = new boolean[3];
		if(indexOfSample == FEED_INDEX) {
			flags[0] = PUMA_CANNA_INFERENCE;
			flags[1] = PUMA_FTHC_INFERENCE;
			flags[2] = FEED_GC_INFERENCE;
		} else if (indexOfSample == RAFF_INDEX) {
			flags[0] = PUMA_CANNA_INFERENCE;
			flags[1] = PUMA_FTHC_INFERENCE;
			flags[2] = RAFF_GC_INFERENCE;
		} else {
			flags[0] = PUMA_CANNA_INFERENCE;
			flags[1] = PUMA_FTHC_INFERENCE;
			flags[2] = EXTR_GC_INFERENCE;
		}
		return new Request(prefix+"_"+SampleFactory.assembleDate(date)+"_"+pumaType.getSuffix()+id.toString(),flags[0],flags[1],flags[2],new String[] {});
	}
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Checks if a sample name is classified as a puma set element
	 * @param name string
	 * @return true if part of a PUMA set
	 */
	public static final boolean isDataForPuma(String name) {
		return nameIsFromPumaLC(name) || nameIsFromPumaGC(name);
	}
	
	
	
	/**
	 * Checks if a sample name is from a PUMA LC sheet
	 * @param name
	 * @return true if is lc
	 */
	public static final boolean nameIsFromPumaLC(final String name) {
		return name.startsWith(SHEET_FEED) || name.startsWith(SHEET_RAFF) || name.startsWith(SHEET_EXTR);
	}


	
	/**
	 * Checks if a sample name is from a PUMA GC sheet
	 * @param name string
	 * @return true if name should belong to puma set
	 */
	public static final boolean nameIsFromPumaGC(final String name) {
		return name.matches("^("+FEED_GC_PRE_EXP+")") 
				|| name.matches("^("+RAFF_GC_PRE_EXP+")") 
				|| name.matches("^("+EXTR_GC_PRE_EXP+")");
	}
	
	
	
	
	
	
	/**
	 * Refactors GC puma element names so they merge appropriately in the result map.
	 * @param gc name string
	 * @return standardized name or simply the input if invalid
	 * @throws DataFormatException
	 * @see Result.mergeGcAndLcOfPuma
	 */
	public static final String standardizePumaGcName(final String gcName) throws DataFormatException {  
		final String[] tokens = gcName.split("_");
		String prefix = tokens[0];
		final String date = tokens[1];
		final String suffix = tokens[2].split("#")[0];
		if(prefix.matches(FEED_GC_PRE_EXP)) {
			prefix = SHEET_FEED;
		} else if (prefix.matches(RAFF_GC_PRE_EXP)) {
			prefix = SHEET_RAFF;
		} else if (prefix.matches(EXTR_GC_PRE_EXP)) {
			prefix = SHEET_EXTR;
			final String[] pt = prefix.split("-");
			if(pt.length == 2)
				prefix += pt[1];
		} else return gcName;
		String lcName =  prefix +"_"+ suffix +"_"+ date;
		Terminal.say("Rename operation successful: "+lcName);
		return lcName;
	}


	
	
	
	
	
	private static final String CUT = "!!!!";
	
	/**
	 * Finds the date when given a PUMA data source file
	 * @param source file path
	 * @return date from source file name
	 */
	public static final LocalDate parseDateFromSource(final String source) {
		String cutName = source.replaceFirst(SOURCE_DATE_MARKER, CUT);
		final int cutLocation = cutName.indexOf(CUT) + CUT.length();
		cutName = cutName.substring(cutLocation, cutLocation+8);
		return SampleFactory.parseDate(cutName);
	}
	
	
	
	/**
	 * Determines the PUMA element index of the given name.
	 * @param standardized LC sheet name
	 * @return index or -1 if this name doesn't belong to a puma set
	 */
	public static final int inferDataSetIndex(String sheetName) {
		Terminal.say("Attempting to infer dataset index of puma: "+sheetName);
		int index;
		if(!nameIsFromPumaLC(sheetName))
			index = -1;
		else {
			Terminal.say(sheetName+" does belong to puma");
			if(sheetName.startsWith(SHEET_FEED))
				index = 0;
			else if (sheetName.startsWith(SHEET_RAFF))
				index = 1;
			else if (sheetName.startsWith(SHEET_EXTR)) {
				char c = sheetName.charAt(SHEET_EXTR.length());
				try {
					int cutNum = Integer.parseInt(""+c);
					index = cutNum + 1;
				} catch (Exception e) {
					index = 2;
				}
			} else index = -1;
		}
		if (index == 0)
			Terminal.say(sheetName+" is feed!");
		else if (index == 1)
			Terminal.say(sheetName+" is raff!");
		else if (index > 1)
			Terminal.say(sheetName+" is extract (of index: "+index+")!");
		else
			Terminal.say("Operation unsuccessful for "+sheetName);
		return index;
	}
	
	
	
	



	/**
	 * enumeration of past and present PUMA set types
	 * @author talon
	 *
	 */
	public static enum Type	{
		ONE3 	(1,	3,	1),	//OBSOLETE
		TWO6 	(2,	6,	2),	//Recently revived for a short period for CBD?
		ONE1 	(1,	1,	1),	//THC d8/9 Separation system
		TWO3 	(2,	3,	1),	//former CBT Separation system
		TWO2	(2, 2,	1);	//current CBT Separation system
		private final int controller, cuts, columnWidth; /**
		 * @param index of the controller block
		 * @param width (diameter in inches rounded to the nearest int) of the column
		 * @param cuts of extract that are taken
		 */
		private Type(final int controller, final int width, final int cuts) {
			this.controller = controller;
			columnWidth = width;
			this.cuts = cuts;
		}
		
		
		
		/**
		 * Obtain the number of samples in this set type.
		 * @return 2+numExtracts
		 */
		private final int getSize() { return cuts + EXTR_START; }


		
		/**
		 * Assembles an ordered array of puma prefix strings
		 * @return 11AD, 14S, 15S(-n)? ...
		 */
		private final String[] makePrefixes() {
			final String[] prefixes = new String[EXTR_START+cuts];
			prefixes[FEED_INDEX] = "11AD";
			prefixes[RAFF_INDEX] = "14S";
			if(cuts == 1)
				prefixes[EXTR_START] = "15S";
			else for(int i = 1; i <= cuts; i++)
				prefixes[EXTR_START+i-1] = "15S-"+i;
			return prefixes;
		}



		/**
		 * Assembles an ordered array of raw sheet prefix strings.
		 * @return FEED, RAFFINATE, EXTRACT[n]? ...
		 */
		final String[] makeSheetPrefixes() {
			final String[] prefixes = new String[EXTR_START+cuts];
			prefixes[FEED_INDEX] = SHEET_FEED;
			prefixes[RAFF_INDEX] = SHEET_RAFF;
			if(cuts == 1)
				prefixes[EXTR_START] = SHEET_EXTR;
			else for(int i = 1; i <= cuts; i++)
				prefixes[EXTR_START+i-1] = SHEET_EXTR+i;
			return prefixes;
		}

		
		
		/**
		 * Assemble this set suffix.
		 * @return Px.y
		 */
		public final String getSuffix() { return "P"+controller+"."+columnWidth; }
		
		
		
		/**
		 * Attempt to find an enumerated PUMA type from its suffix
		 * @param suffix string
		 * @return enumerated type or null
		 */
		public static final Type inferFromSuffix(String suffix) {
			//standardize and split input
			suffix = suffix.replaceAll("P", "");
			final String[] tokens = suffix.split("\\.");
			int controller, width;
			//attempt to parse tokens
			try {
				if(tokens.length != 2) 
					throw new ArrayIndexOutOfBoundsException("Invalid number of tokens for puma type suffix inference");
				controller = Integer.parseInt(tokens[0]);
				width = Integer.parseInt(tokens[1]);
			} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
				Terminal.mention("Error infering Puma type from suffix: "+suffix);
				return null;
			}
			//attempt to find an enumerated type
			for(Type t : Type.values())
				if(controller == t.controller && width == t.columnWidth)
					return t;
				else continue;
			return null;	 //if nothing was found in the loop
		}
	}
}
