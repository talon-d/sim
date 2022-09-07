package en.talond.simGUI.io;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;

import en.talond.simGUI.Terminal;
import en.talond.simGUI.data.Puma;







/**
 * This file wraps the data-containing xls files that come off the Agilent machines.
 * @author talon
 *
 */
public class DataSheet {



	private final Map<String,SheetData[]> contentMap;
	private final File src;
	
	
	
	/**
	 * Creates a DataSheet from an xls source file.
	 * @param src file
	 * @throws EncryptedDocumentException
	 * @throws IOException
	 */
	public DataSheet(final File src) throws EncryptedDocumentException, IOException {
		this.src = src;
		final Workbook w = WorkbookFactory.create(src);
		contentMap = generateContentMap( w );
		Terminal.say(this.stringify());
	}
	
	
	
	/**
	 * Gets the names of all the samples contained in this document.
	 * @return array of names
	 */
	public final String[] getSamples() { 
		return contentMap.keySet().toArray(new String[0]); 
	}
	
	/**
	 * Obtains the data for a given name (or null)
	 * @param sample name
	 * @return data of sample
	 */
	public final SheetData[] getDataOf(final String sample) { 
		return contentMap.get(sample); 
	}
	
	/**
	 * Gets the source file used to create this DataSheet
	 * @return src file
	 */
	public final File getSrc() { 
		return src; 
	}

	/**
	 * Wraps all contained data into one string
	 * For debugging purposes.
	 * @return string representation of this DataSheet
	 */
	public final String stringify() {
		String s = "";
		final String[] keys = getSamples();
		for(final String key : keys)
			for(final SheetData ofSample : getDataOf(key))
				s = "\t"+s+ofSample.stringify()+"\n";
		return s;
	}






	/**
	 * Parses the source file to map data to sample names.
	 * @param sheet to parse (Apache Workbook representation)
	 * @return content map
	 */
	@SuppressWarnings("deprecation")
	private static final Map<String,SheetData[]> generateContentMap(final Workbook w) {
		//Declaring a map of sample names to result sets
		final Map<String,SheetData[]> contentMap = new HashMap<>();
		//Iterates through each sheet of the workbook (mostly legacy feature; Agilent cross-sequence summaries never(?) create multiple sheets)
		for(int i = 0; i < w.getNumberOfSheets(); i++) {
			final Sheet s = w.getSheetAt(i);	//obtain the sheet for this iterations
			//Finds the keys and the number of preps/dilutions for each sample in the key
			final Queue<KeyRow> keys = parseKey(s);
			final Integer[] prepsPerSample = findPrepsPerSample(keys.toArray(new KeyRow[] {}));
			//Finds the all the result rows of the sheet and maps them to sample names
			final Map<String,ResultRow[]> results = parseResults(s);
			//Iterates through each sample
			for (final Integer element : prepsPerSample) {
				final int prepsOfListedSample = element;					//Collect the number of preparations for this sample
				final KeyRow[] preps = new KeyRow[prepsOfListedSample];		//Collect all the preparations from the queue and add it to an array
				//Polls the queue to create a key reference array to parse with
				for(int k = prepsOfListedSample; k > 0; k--)
					preps[prepsOfListedSample-k] = keys.poll();
				//finalizes the key name (adjusments MUST be made to Puma elements)
				String keyedSample = preps[0].getSample();
				final ResultRow[] resultsOfAllPreps = results.get(keyedSample);
				final String end = "_[Pp]?[1-9]\\.[1-9])";
				if(keyedSample.matches("^(FEED"+end) || keyedSample.matches("^(RAFFINATE"+end) || keyedSample.matches("^(EXTRACT[1-9]?"+end)) try {
					final String[] pumaKeyPieces = keyedSample.split("_");
					keyedSample = pumaKeyPieces[0]+"_P"+pumaKeyPieces[1]+"_"+SampleFactory.assembleDate(Puma.parseDateFromSource(preps[0].getPath()));
				} catch (Exception e) {
					Terminal.say("Error parsing PUMA sample from data sheet: "+preps[0].getSample());
				}
				//Requests the keyed sample from the result map
				//Creates a SheetData object for each instance of that sample in the key
				final SheetData[] finalRawData = new SheetData[prepsOfListedSample];
				for(int k = 0; k < finalRawData.length; k++)
					finalRawData[k] = new SheetData(preps[k]);
				for(int k = 0; k < resultsOfAllPreps.length; k++) {
					final ResultRow[] supposedlyOfSameCompound = Arrays.copyOfRange(resultsOfAllPreps, k, k+prepsOfListedSample);
					try {
						if(supposedlyOfSameCompound[0].getCompound().equalsIgnoreCase(supposedlyOfSameCompound[supposedlyOfSameCompound.length-1].getCompound())) {
							for(int x = 0; x < prepsOfListedSample; x++)
								finalRawData[x].addResult(supposedlyOfSameCompound[x]);
							k = k + prepsOfListedSample-1;
						} else {
							double maximalPrepAmount = 0.0;
							for(int x = 0; x < prepsOfListedSample; x++)
								if(maximalPrepAmount < finalRawData[x].getAmount())
									maximalPrepAmount = finalRawData[x].getAmount();
							SheetData leastDilutePrep = null;
							for(int x = 0; x < prepsOfListedSample; x++)
								if(finalRawData[x].getAmount() == maximalPrepAmount)
									leastDilutePrep = finalRawData[x];
							leastDilutePrep.addResult(resultsOfAllPreps[k]);
						}
					} catch (final NullPointerException e) {
						double maximalPrepAmount = 0.0;
						for(int x = 0; x < prepsOfListedSample; x++)
							if(maximalPrepAmount < finalRawData[x].getAmount())
								maximalPrepAmount = finalRawData[x].getAmount();
						SheetData leastDilutePrep = null;
						for(int x = 0; x < prepsOfListedSample; x++)
							if(finalRawData[x].getAmount() == maximalPrepAmount)
								leastDilutePrep = finalRawData[x];
						leastDilutePrep.addResult(resultsOfAllPreps[k]);
					}

				}
				contentMap.put(keyedSample,finalRawData);
			}
		}
		try {
			w.close();
		} catch (IOException e) {
			Terminal.specifyFullError(e);
		}
		return contentMap;
	}


	private static final Integer[] findPrepsPerSample(final KeyRow[] keys) {
		final List<Integer> prepsPerSample = new LinkedList<>();
		final Queue<String> names = new LinkedList<>();
		for(final KeyRow k : keys)
			names.add(k.getSample());
		String previousName = "";
		int tracker = 0;
		boolean first = true;
		while(names.peek() != null) {
			final String currentName = names.poll();
			if(first) {
				previousName = currentName;
				tracker = 1;
				first = false;
			} else if(currentName.equals(previousName))
				tracker++;
			else {
				prepsPerSample.add(tracker);
				tracker = 1;
				previousName = currentName;
			}
		}
		return prepsPerSample.toArray(new Integer[] {});
	}



	public static final class SheetData {
		
		private final KeyRow key;
		private final List<ResultRow> data;
		
		private SheetData(final KeyRow key) {
			this.key = key;
			data = new LinkedList<>();
		}
		/* used by the parse function to append results */
		private final void addResult(final ResultRow toAdd) { data.add(toAdd); }
		/* getters for key information */
		public final String getSample() { return key.getSample(); }
		public final String getSource() { return key.getPath(); }
		public final Double getAmount() { return key.getAmount(); }
		public final List<ResultRow> getData() { return data; }
		private final String stringify() {
			String s = key.stringify()+"\n";
			for(final ResultRow r : getData())
				s += r.stringify()+"\n";
			return s;
		}
	}





	/**
	 * Creates a queue from the key at the start of a data sheet.
	 * This function will look for a key header row. When one is found,
	 * 	it will begin parsing row by row, until an empty cell is found,
	 * 	at which point it is assumed the full array has been generated.
	 *
	 * The array should roughly follow this scheme:
	 *
	 * 					KeyRow[] = { sample0, sample0, sample1, sample2,  }
	 *
	 * 		Different preparations of the same sample should be all in a row,
	 * 		and each name on the sheet should be completely unique. Ambiguity
	 * 		is unacceptable.
	 * @param excel sheet to parse
	 * @return ordered array of rows of the key
	 */
	private static final Queue<KeyRow> parseKey(final Sheet s) {
		int keyStart = 0;	//Inclusive
		int keyEnd = 0;		//Exclusive
		//Iterate through the sheet to find the start of the key
		for(int i = 0; i < s.getLastRowNum(); i++)
			if(isKeyHeader(s.getRow(i))) {
				keyStart = i+1;
				break;
			} else continue;
		//Iterate after the start of the key until an empty sample cell is found to find the end of the key
		for(int i = keyStart; i < s.getLastRowNum(); i++)
			if(s.getRow(i).getCell(SAMPLE_COL).getStringCellValue().equals("")) {
				keyEnd = i;
				break;
			} else continue;
		//Collect the key between the found values
		final Queue<KeyRow> keys = new LinkedList<>();
		for(int i = keyStart; i <= keyEnd; i++)
			keys.add(new KeyRow(s.getRow(i)));
		return keys;
	}



	private static final Map<String,ResultRow[]> parseResults(final Sheet s)
	{
		final Map<String,ResultRow[]> allResults = new HashMap<>();
		//Obtains a list of row indices for data table headers
		final List<Integer> headerIndices = new LinkedList<>();
		for(int i = 0; i < s.getLastRowNum(); i++)
			if(isHeader(s.getRow(i)))
				headerIndices.add(i);
		//Goes through each header index that was found and creates a map entry
		for(int i = 0; i < headerIndices.size(); i++) {
			//Finds the indexes to parse between
			final int sampleRowNum = headerIndices.get(i)+1;	//The row below the header should contain the sample name
			int nextHeaderRowNum;						//The next header will tell the loop when to stop looking for this samples data
			try {											//Collects the next header index
				nextHeaderRowNum = headerIndices.get(i+1);
			} catch (final IndexOutOfBoundsException e) {			//Or if there isn't one, use the last row as the end
				nextHeaderRowNum = s.getLastRowNum()+2;			// 2 has to be added to account for the usual two blank rows under before a header
			}
			//
			final Row sampleRow = s.getRow(sampleRowNum);			//Collects supposed sample row
			final String sampleName = sampleRow.getCell(1).getStringCellValue().toUpperCase();
			final boolean validRow = sampleRow.getCell(0).getStringCellValue().equals(SAMPLE_FIELD);
			//Moves on to the next header index if the sample header row is invalid
			if(!validRow)
				;
			// otherwise...
			else {
				final ResultRow[] dataRows = new ResultRow[nextHeaderRowNum-sampleRowNum-3];	//Get the data rows between the current and next header
				for(int j = sampleRowNum+1; j < nextHeaderRowNum-2; j++)				//Iterate through each data row and create a RawData object
					dataRows[j-sampleRowNum-1] = new ResultRow(s.getRow(j));
				allResults.put(sampleName, dataRows);										//Places the rawData into the map with the listed sample
			}
		}
		return allResults;
	}













	//Valid key header
	private static final String
	SAMPLE_HEADER = "Sample  Name",
	AMOUNT_HEADER = "Sample Amt",
	PATH_HEADER = "Directory",
	FILE_HEADER = "Data File";
	//Locations of valid key data
	private static final int
	SAMPLE_COL = 0,
	AMOUNT_COL = 5,
	PATH_COL= 9,
	FILE_COL = 11;
	//Valid sample header
	private static final String
	SAMPLE_FIELD	= "Sample:";
	//Valid header contents
	private static final String
	COMPOUND_HEADER = "Compound",
	METHOD_HEADER	= "Method",
	DESC_HEADER		= "Desc.",
	AREA_HEADER		= "Area",
	AREAP_HEADER	= "Area %",
	MASSP_HEADER	= "Mass %",
	PPM_HEADER		= "Dilute PPM";
	//Locations of valid data
	private static final int
	COMPOUND_COL= 0,
	METHOD_COL	= 1,
	DESC_COL	= 4,
	AREA_COL	= 6,
	AREAP_COL	= 8,
	MASSP_COL	= 10,
	PPM_COL		= 12;



	/**
	 * Checks to see if the contents of a row match the expected values for a header row
	 * @param row to check
	 * @return true for a header row
	 */
	private static final boolean isHeader(final Row toCheck)
	{
		//Gets the contents of each each cell
		final String compoundHeader = toCheck.getCell(COMPOUND_COL).getStringCellValue();
		final String methodHeader	  = toCheck.getCell(METHOD_COL).getStringCellValue();
		final String descHeader	  = toCheck.getCell(DESC_COL).getStringCellValue();
		final String areaHeader	  = toCheck.getCell(AREA_COL).getStringCellValue();
		final String areapHeader	  = toCheck.getCell(AREAP_COL).getStringCellValue();
		final String masspHeader	  = toCheck.getCell(MASSP_COL).getStringCellValue();
		final String ppmHeader	  = toCheck.getCell(PPM_COL).getStringCellValue();
		//Checks the contents
		return compoundHeader.equals(COMPOUND_HEADER) && methodHeader.equals(METHOD_HEADER)
				&& descHeader.equals(DESC_HEADER) && areaHeader.equals(AREA_HEADER) && areapHeader.equals(AREAP_HEADER)
				&& masspHeader.equals(MASSP_HEADER) && ppmHeader.equals(PPM_HEADER);
	}


	private static final boolean isKeyHeader(final Row toCheck)
	{
		final String sampleHeader = toCheck.getCell(SAMPLE_COL).getStringCellValue();
		final String amountHeader = toCheck.getCell(AMOUNT_COL).getStringCellValue();
		final String pathHeader = toCheck.getCell(PATH_COL).getStringCellValue();
		final String fileHeader = toCheck.getCell(FILE_COL).getStringCellValue();
		return sampleHeader.equals(SAMPLE_HEADER) && amountHeader.equals(AMOUNT_HEADER)
				&& pathHeader.equals(PATH_HEADER) && fileHeader.equals(FILE_HEADER);
	}






	/**
	 * This instance class wraps the data of a key row from a data sheet
	 * @author talon
	 *
	 */
	private static final class KeyRow
	{
		/* instance data */
		private final String sample, filePath;
		private final Double amount;
		/* constructor from sheet */
		private KeyRow(final Row r) {
			sample = collectSample(r);
			amount = collectAmount(r);
			filePath = collectPath(r);
		}
		/* getters */
		private final String getSample() { return sample; }
		private final Double getAmount() { return amount; }
		private final String getPath()	 { return filePath; }

		private final String stringify() {
			return sample+" "+amount+":\t"+filePath;
		}
		/* construction helper methods */
		private static final String collectSample(final Row r) {
			return  r.getCell(SAMPLE_COL).getStringCellValue().toUpperCase();
		}
		private static final Double collectAmount(final Row r) {
			try {
				return Double.parseDouble(r.getCell(AMOUNT_COL).getStringCellValue());
			} catch (final NumberFormatException e) {
				return 0.0;
			}
		}
		private static final String collectPath(final Row r) 	 {
			return r.getCell(PATH_COL).getStringCellValue() + "/" + r.getCell(FILE_COL).getStringCellValue();
		}

	}




	/**
	 * This class wraps the cell contents of a data row into a much easier to use format
	 * @author talon
	 *
	 */
	public static final class ResultRow {
		/* data fields of row */
		private final Method method;
		private final String compound,desc;
		private final Double area, areaP, massP, dilutePPM;
		/* primary constructor */
		public ResultRow(final Row row) {
			compound = collectCompound(row);
			method = collectMethod(row);
			desc = collectDescription(row);
			area = collectArea(row);
			areaP = collectAreaP(row);
			massP = collectMassP(row);
			dilutePPM = collectPPM(row);
		}

		/* testing constructor */
		public ResultRow(final String comp, final Method m, final String desc, 
				final Double area, final Double areaP, final Double massP, final Double dilutePPM) {
			compound = comp;
			method = m;
			this.desc = desc;
			this.area = area;
			this.areaP = areaP;
			this.massP = massP;
			this.dilutePPM = dilutePPM;
		}

		/* row field getters */
		public final String getCompound() 	{ return compound; }
		public final Method getMethod()		{ 
			if(method != null)
				return method; 
			else
				Terminal.say("WARNING: NULL METHOD FOR KEY ROW. Double check regexp strings.");
			return null;
		}
		public final String getDesc()		{ return desc; }
		public final Double getArea()		{ return area; }
		public final Double getAreaP()		{ return areaP; }
		public final Double getMassP()		{ return massP; }
		public final Double getPPM()		{ return dilutePPM; }
		public final String stringify() 	{ return method.name()+S+compound+S+desc+S+area+S+areaP+S+massP+S+dilutePPM; }	private static final String S = " ";
		/* Data collection helper methods */
		private static final String collectCompound(final Row row) 		{ return row.getCell(COMPOUND_COL).getStringCellValue();}
		private static final Method collectMethod(final Row row)		{ return Method.detect(row.getCell(METHOD_COL).getStringCellValue()); }
		private static final String collectDescription(final Row row)	{ return row.getCell(DESC_COL).getStringCellValue(); }
		private static final Double collectArea(final Row row)			{ return Double.parseDouble(row.getCell(AREA_COL).getStringCellValue()); }
		private static final Double collectAreaP(final Row row) 		{ return Double.parseDouble(row.getCell(AREAP_COL).getStringCellValue()); }
		private static final Double collectMassP(final Row row)			{ return wrapVal(row.getCell(MASSP_COL)); }
		private static final Double collectPPM(final Row row)			{ return Double.parseDouble(row.getCell(PPM_COL).getStringCellValue()); }
	}


	/**
	 * Attempts to retrieve the mass percent of a cell,
	 * 	and returns negative one is the cell is NaN
	 * @param cell
	 * @return mass percent from cell or -1.0
	 */
	private static final Double wrapVal(final Cell cell) {
		try {
			return Double.parseDouble(cell.getStringCellValue());
		} catch(final NumberFormatException e) {
			return INVALID_MASS_PERCENT;
		}
	}



	//Number to wrap with when given a result with no prep amount (ppm or gc)
	public static final Double
	INVALID_MASS_PERCENT = -1.0;


	//Method name elements
	public static final String
	EXT		= "\\.M",
	TWO_NUM	= "[0-9]{2}",
	AVIV	= "AVIV-FINGERPRINT",
	ADS		= AVIV+"-ADS[0-9](-PUMA)?";
	//Method name expressions
	public static final String
	CANNA_EXP	= "^(CANNABINOIDS_)"+TWO_NUM+"-VWD[C]?"+EXT,
	FTHC_EXP  	= "^(FTHC-)"+TWO_NUM+"(-GRADIENT)?(-210)?"+EXT,
	GC_EXP		= "^("+AVIV+"|"+ADS+")"+EXT;



	/**
	 * Enumerates possible analysis methods and their respective name expressions
	 * @author talon
	 *
	 */
	public static enum Method {
		CANNA (CANNA_EXP),
		FTHC  (FTHC_EXP),
		GC    (GC_EXP);

		private final String regexp;
		Method(final String regexp) { 
			this.regexp = regexp; 
		}

		/**
		 * Iterates through the enumeration to find a method pattern which matches the name given
		 * @param possibleMethod
		 * @return method found or null
		 */
		public static final Method detect(final String possibleMethod) {
			for(final Method m : Method.values())
				if(possibleMethod.toUpperCase().matches(m.regexp))
					return m;
				else continue;
			return null;
		}
	}





}
