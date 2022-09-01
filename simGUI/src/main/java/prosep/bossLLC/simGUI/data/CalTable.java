package prosep.bossLLC.simGUI.data;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import prosep.bossLLC.simGUI.Terminal;
import prosep.bossLLC.simGUI.io.Storage;






/**
 * A CalTable (calibration table) is a parseable document that comes off the
 * 	Agilent chromatography machines as a csv file. When an instance is created, its
 * 	data is automatically parsed by an internal helper function.
 *
 *
 * To get the csv file from an agilent machine, select a sequence, hit "File" in the upper right,
 * 	then "Export File", then select "CSV", then select "Calibration Table". Hit "Ok" twice and select
 * 	a location to save it to. Then, import it into the program using UI/GUI functionality.
 * 
 * @author talon
 *
 */
public class CalTable {


	
	
	
	
	
	//Default values to return if asked for an uncalibrated compound
	public static final Float
	DEFAULT_MIN = (float) 250, 
	DEFAULT_MAX = (float) 4500;
	
	
	
	
	
	
	
	private final Map<String,Range> compoundRanges;
	private final LocalDate parsed;
	
	
	
	/**
	 * Attempts to parse an Agilent calibration table csv file.
	 * @param src file
	 * @throws IOException if the file cannot be read
	 */
	public CalTable(final File src) throws IOException {
		compoundRanges = parseRanges(src);
		parsed = LocalDate.now();
	}
	
	
	
	/**
	 * Reconstructs an existing calibration table from its inner data.
	 * @param map of compounds to calibration ranges
	 * @param date the map was constructed originally parsed
	 */
	public CalTable(final Map<String,Range> ranges, final LocalDate generated) {
		this.compoundRanges = ranges;
		this.parsed = generated;
	}
	
	



	
	
	
	
	
	
	
	
	
	/**
	 * Gets the date of original file parse
	 * @return date parsed
	 */
	public final LocalDate getDateGenerated() { 
		return parsed; 
	}
	
	
	
	
	
	
	
	/**
	 * Gets the names of the calibrated compounds listed in this table
	 * @return array of key strings
	 */
	public final String[] getCompounds() { 
		return compoundRanges.keySet().toArray(new String[0]); 
	}
	
	
	
	
	
	
	
	
	
	/**
	 * Returns the upper calibration bound of the given compound.
	 * In the event that the given compound is not calibrated, the
	 * compound is assumed to have DEFAULT_MAX.
	 * case insensitive mapping
	 * @param compound to check calibration of
	 * @return calibration max or default
	 */
	public final Float getHigh(final String compound) { 
		for(String calibrated : compoundRanges.keySet())
			if(compound.equalsIgnoreCase(calibrated))
				return compoundRanges.get(calibrated).getHigh();
		Terminal.say("\t\t\tSomething attempted to retrieve an uncalibrated compound: "+compound+", so it got "+DEFAULT_MAX);
		return DEFAULT_MAX;
	}
	
	
	
	/**
	 * Returns the lower calibration bound of the given compound.
	 * In the event that the given compound is not calibrated, the
	 * compound is assumed to have a min of DEFAULT_MIN.
	 * Case insensitive mapping
	 * @param compound to check calibration of
	 * @return calibration min or default
	 */
	public final Float getLow (final String compound) {
		for(String calibrated : compoundRanges.keySet())
			if(compound.equalsIgnoreCase(calibrated))
				return compoundRanges.get(calibrated).getLow();
		Terminal.say("\t\t\tSomething attempted to retrieve an uncalibrated compound: "+compound+", so it got "+DEFAULT_MIN);
		return DEFAULT_MIN;
	}
	
	

	/**
	 * Calculates the midpoint of the calibration range
	 * @param compound name
	 * @return cal midpoint
	 */
	public final Float getMidpoint(final String compound) {
		return getHigh(compound) - getLow(compound) / 2 + getLow(compound);
	}

	
	
	
	
	
	
	
	


	/**
	 * Generates a complete view of the calibration table and its parameters as a single string
	 * @return stringified object
	 */
	public final String stringify() {
		String stringified = "";
		final String[] compounds = compoundRanges.keySet().toArray(new String[0]);
		for(final String compound : compounds)
			stringified += compound+" "+compoundRanges.get(compound).stringify()+"\n\t";
		return stringified;
	}







	
	/**
	 * Minimal two-number data structure for containing the max and min of a compound's calibration area
	 * 	Used in the private calibration map, not intended for external access.
	 * @author talon
	 *
	 */
	public static final class Range {	
		private final Float low, high;
		/**
		 * Assigns the upper and lower bound automatically given two numbers
		 * @param a number
		 * @param another number
		 */
		Range(final Float one, final Float two) {
			if(one > two) {
				high = one; 
				low = two;
			} else {
				high = two; 
				low = one;
			}
		}
		private final Float getLow()  { return low; }
		private final Float getHigh() { return high; }
		private final String stringify() { return "Min Area: "+getLow()+" | Max Area: "+getHigh(); }
	}





	
	
	
	
	
	
	
	
	

	private static final Float  NIL_AREA = Float.valueOf("0");
	private static final String NIL_COMP = "INVALID COMPONENT";
	private static final int 
	START_COMP_ROW_SIZE = 12, CONTINUED_COMP_ROW_SIZE = 9,
	COMPOUND_COLUMN = 3, AREA_COLUMN = 7;




	/**
	 * Parses a canna CalTable csv file. It will map each compound name
	 * contained in the table to a max and min float value. Used by the novel file
	 * constructor.
	 * @param src file to parse
	 * @return mapping of compounds to ranges
	 * @throws IOException if the file cannot be read from
	 */
	private static final Map<String,Range> parseRanges(final File src) throws IOException {
		final Map<String,Range> ranges = new HashMap<>();		//declare map of compounds to ranges
		final String[] lines = Storage.loadUTF16File(src);		//collect lines of the file (with UTF-16 encoding)
		//Split each line's contents along indentation characters and fill the token array
		final String[][] tokens = new String[lines.length][];
		for(int i = 0 ; i < lines.length; i++)
			tokens[i] = lines[i].split("[\t]");
		//initialize compound trackers with null values
		String currentCompound = NIL_COMP;
		Float currentMax = NIL_AREA, currentMin = NIL_AREA;
		//Iterate through each tokenized line of the original file and assign values to the map
		for(int i = 0; i < tokens.length; i++) {
			final String[] line = tokens[i];	//collect the next line to be parsed
			//if this ^ is a new line, reassign the compound trackers
			if(line.length == START_COMP_ROW_SIZE) {
				currentCompound = unwrap(line[COMPOUND_COLUMN]);
				currentMax = currentMin = Float.parseFloat(line[AREA_COLUMN]);
			//Or if this line is a continuation of the tracked compound, evaluate the contents of this line and reassign as needed
			} else if (line.length == CONTINUED_COMP_ROW_SIZE) {	
				final Float area = Float.parseFloat(line[AREA_COLUMN]);
				if(area < currentMin)
					currentMin = area;
				if(area > currentMax)
					currentMax = area;
			//Otherwise, assume invalid input was given 
			} else throw new IOException("INVALID CAL TABLE FORMAT, NULL MAP GIVEN");
			//If this is the last line of the table, put the current ranges in the map
			if(i == tokens.length-1) {
				ranges.put(currentCompound, new Range(currentMin,currentMax));
				currentCompound = NIL_COMP;
				currentMin = currentMax = NIL_AREA;
			//Or try to look at the next row. If it is a new compound row, add the current values to the map and reassign trackers
			} else try {
				if(tokens[i+1].length == START_COMP_ROW_SIZE) {
					ranges.put(currentCompound, new Range(currentMin,currentMax));
					currentCompound = NIL_COMP;
					currentMin = currentMax = NIL_AREA;
				}
			//I'll be honest, I do not remember what this does, but I'm too scared to delete it. It looks important
			} catch (final ArrayIndexOutOfBoundsException e) {
				continue;
			}
		}
		return ranges;
	}

	
	
	/**
	 * factory method used to extract compound names from a raw csv.
	 * @param raw component name
	 * @return formatted compound name
	 */
	private static final String unwrap(final String component) { 
		return component.replace("\"", ""); 
	}



}
