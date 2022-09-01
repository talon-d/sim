package en.talond.simGUI.report;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import en.talond.simGUI.Terminal;
import en.talond.simGUI.data.DataPacket;
import en.talond.simGUI.data.Request;
import en.talond.simGUI.report.Report.LCResult;







/**
 * This class uses the maps generated in the Report class
 * to generate well-formatted tables in excel sheets.
 * @author talon
 * 
 */
public abstract class ReportTable {

	
	
	// default values for the placement parameters
	private static final int
	DEFAULT_HEADER_ROW = 4,
	DEFAULT_START_COL  = 3;
	
	
	
	// report table instance parameters
	private final Sheet withTable;
	private final int headerRow, leftmostColumn;
	
	
	
	/**
	 * A report table is an abstract way to generate tables on the sheet of an excel file.
	 * @param the sheet to generate this table with (from org.apache.poi usermodel)
	 */
	private ReportTable(final Sheet toHaveTable) {
		withTable = toHaveTable;
		headerRow = DEFAULT_HEADER_ROW;
		leftmostColumn = DEFAULT_START_COL;
	}
	
	
	
	/**
	 * A report table is an abstract way to generate tables on the sheet of an excel file.
	 * @param sheet to have table
	 * @param integer row of header for table header
	 * @param integer column of leftmost cell of table
	 */
	private ReportTable(final Sheet toHaveTable, int headerRow, int leftmostColumn) {
		this.withTable = toHaveTable;
		this.headerRow = headerRow;
		this.leftmostColumn = leftmostColumn;
	}
	
	
	
	
	
	
	
	
	


	/**
	 * @return the sheet this table should be written to
	 */
	final Sheet getContainer() { return withTable; }
	
	/**
	 * @return the leftmost column of this table
	 */
	final int getLeftmost() { return leftmostColumn; }
	
	/**
	 * @return the row of the table header
	 */
	final int getHeaderRow() { return headerRow; }
	
	
	
	
	
	
	/**
	 * write the contents of this table to the sheet
	 * @throws IOException if the table contents are invalid.  
	 * IOException isn't descriptive but whatever its fine
	 */
	abstract void generate() throws IOException;
	
	/**
	 * should only ever be called within the implemented generate function.
	 * And it should definitely be called.
	 */
	abstract void generateHeader();


	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * @param sheet
	 * @param firstRow
	 * @param leftmostCol
	 */
	static final void generateKey(Sheet sheet, int firstRow, int leftmostCol) {
		Cell header = sheet.getRow(firstRow).getCell(leftmostCol);
		Cell lowConf = sheet.getRow(firstRow+1).getCell(leftmostCol);
		Cell highConf = sheet.getRow(firstRow+2).getCell(leftmostCol);
		header.setCellValue("- KEY -");
		lowConf.setCellValue("Potential Issue");
		highConf.setCellValue("Good Result");
		header.setCellStyle(Style.TABLE_HEADER);
		lowConf.setCellStyle(Style.ACLC_DATA);
		highConf.setCellStyle(Style.HCLC_DATA);
		sheet.setColumnWidth(header.getColumnIndex(), 5000);
	}
	
	
	
	


	
	

	// column numbers for source row elements
	private static final int
	NAME_COL = 0,
	COMP_COL = 1,
	DESC_COL = 2,
	AREA_COL = 3,
	R_TYPE_COL = 4,
	RESULT_COL = 5,
	SRC_COL = 6;


	
	/**
	 * The source table provides a traceable location to the data sources of a process report.
	 * @author talon
	 *
	 */
	static final class SourceTable extends ReportTable {
		
		// instance data
		private final Map<Request,Map<String,DataPacket[]>> dataUsedInReport;
		
		/**
		 * source table constructor
		 * @param toPutTableIn
		 * @param dataUsedInReport
		 * @see ProcessReport
		 */
		SourceTable(final Sheet toPutTableIn, final Map<Request,Map<String,DataPacket[]>> dataUsedInReport) {
			super(toPutTableIn);
			this.dataUsedInReport = dataUsedInReport;
		}
		
		
		
		/**
		 * Writes these data packets to this excel sheet
		 */
		@Override final void generate() {	
			//Iterates through the double nested map to produce a 1D list of source rows
			final List<Source> sourceRows = new LinkedList<>();
			for(final Request r : dataUsedInReport.keySet())
				for(final String compound : dataUsedInReport.get(r).keySet())
					for(final DataPacket dp : dataUsedInReport.get(r).get(compound))
						sourceRows.add(new Source(r.getName(),compound,dp.getDescription(),dp.getArea(),dp.getResultType(),dp.getResult(),dp.getSource()));
			//Create the header and note the actual first table row
			generateHeader();
			final int offset = getHeaderRow()+1;
			//Iterate through the source list and generate their respective rows
			for(int i = 0; i < sourceRows.size(); i++)
				sourceRows.get(i).generate(getContainer().createRow(i+offset),getLeftmost());
		}
		
		
		
		/**
		 * Generates and formats the source table header row
		 */
		@Override final void generateHeader() {
			final Row header = getContainer().createRow(getHeaderRow());
			for(int i = 0; i < TABLE_HEADERS.length; i++) {
				final Cell c = header.createCell(i+getLeftmost());
				c.setCellValue(TABLE_HEADERS[i]);
				c.setCellStyle(Style.TABLE_HEADER);
				getContainer().autoSizeColumn(i+getLeftmost());
			}
		}

		// labels for the source table's fields
		public static final String[] 
		TABLE_HEADERS = new String[] { "Sample Name", "Compound", "Description", "Area", "Result Type", "Result", "Agilent Datafile Source" };
	}


















	/**
	 * Data wrapper for a row of the legacy LC table
	 *
	 * @author talon
	 *
	 */
	static class OldLiquidRow
	{
		private final String sample;
		private final Map<LC_BY_RT,LCResult> resultMap;
		OldLiquidRow(final String ofSample, final Map<String,LCResult> map)
		{
			sample = ofSample;
			resultMap = new HashMap<>();
			for(final String detectedCompound : map.keySet()) {
				final LC_BY_RT toPutInNewMap = LC_BY_RT.getMatch(detectedCompound);
				getResultMap().put(toPutInNewMap, map.get(detectedCompound));
			}
		}


		void generateRow(final Row toGenerateIn, final int startCol) throws IOException
		{
			final Cell sample = toGenerateIn.getCell(startCol);
			sample.setCellValue(this.getSample());
			sample.setCellStyle(Style.TABLE_LABEL);
			int offset = startCol+1;
			double totalDetectedMass = 0.0;
			double totalCBD = 0.0;
			double totalTHC = 0.0;
			//Iterate through each compound in the LC enumeration and assign values from the original map
			for(int i = 0; i < LC_BY_RT.values().length; i++) {
				final LC_BY_RT compound = LC_BY_RT.values() [i];
				final Cell c = toGenerateIn.createCell(i+offset,CellType.NUMERIC);
				boolean highConf, aboveCal;
				try {						//Attempt to get the ratio result from the map
					LCResult lcr = getResultMap().get(compound);
					aboveCal = lcr.isAboveCalibration();
					highConf = lcr.isHighConfidence();
					final double ratio = lcr.getResultAsRatio();
					c.setCellValue(ratio);
					totalDetectedMass += ratio;
					if(compound == LC_BY_RT.CBD)
						totalCBD += ratio;
					else if (compound == LC_BY_RT.CBDa)
						totalCBD += ratio * ION_MASS_RATIO;
					else if (compound == LC_BY_RT.THC_d9)
						totalTHC += ratio;
					else if (compound == LC_BY_RT.THCa)
						totalTHC += ratio * ION_MASS_RATIO;
					if(totalDetectedMass > 1.0)
						throw new IOException(sample+" has >100% detected compounds "+totalDetectedMass*100.0);
				} catch (final NullPointerException e) {
					aboveCal = false;
					highConf = false;
					c.setCellValue(0.0);	//If the map accessor returns a null value, assume the compound was not detected
				}
				if(aboveCal)
					c.setCellStyle(Style.ACLC_DATA);
				else if (highConf)
					c.setCellStyle(Style.HCLC_DATA);
				else
					c.setCellStyle(Style.LC_DATA);
				toGenerateIn.getSheet().setColumnWidth(i+offset, 10*256);
			}
			toGenerateIn.getSheet().setColumnWidth(startCol, 30*256);
			offset += LC_BY_RT.values().length;
			final Cell tTHC = toGenerateIn.createCell(offset); offset++;
			final Cell tCBD = toGenerateIn.createCell(offset); offset++;
			final Cell total = toGenerateIn.createCell(offset);
			tTHC.setCellStyle(Style.TABLE_SUMMARY);
			tCBD.setCellStyle(Style.TABLE_SUMMARY);
			total.setCellStyle(Style.TABLE_SUMMARY);
			tTHC.setCellValue(totalTHC);
			tCBD.setCellValue(totalCBD);
			total.setCellValue(totalDetectedMass);
		}


		public String getSample() {
			return sample;
		}


		public Map<LC_BY_RT,LCResult> getResultMap() {
			return resultMap;
		}
	}


	public static final double ION_MASS_RATIO = 0.877;


	/**
	 *
	 *
	 * @author talon
	 *
	 */
	static final class OldLiquidTable extends ReportTable
	{
		OldLiquidTable(final Sheet toGenerateWithin, final Map<Request,Map<String,LCResult>> mapOfSamples)
		{
			super(toGenerateWithin);
			sampleMap = mapOfSamples;
		}
		private final Map<Request,Map<String,LCResult>> sampleMap;

		@Override
		final void generate() throws IOException
		{
			final List<OldLiquidRow> tableRows = new LinkedList<>();
			for(final Request r : sampleMap.keySet())
				tableRows.add(new OldLiquidRow(r.getName(),sampleMap.get(r)));
			//Create the header and note the actual first table row
			generateHeader();
			final int offset = getHeaderRow()+1;
			for(int i = 0; i < tableRows.size(); i++)
				tableRows.get(i).generateRow(getContainer().createRow(i+offset), getLeftmost());
		}

		@Override final void generateHeader() {
			final LC_BY_RT[] values = LC_BY_RT.values();
			final Row header = getContainer().createRow(getHeaderRow());	//Create the header row
			final Cell sample = header.createCell(getLeftmost());			//Create the sample label cell
			final Cell title = header.createCell(getLeftmost()-1);
			sample.setCellValue("SAMPLE");								//	and label it
			sample.setCellStyle(Style.TABLE_HEADER);					//	and style it
			title.setCellValue("Mass %");
			title.setCellStyle(Style.TABLE_HEADER);
			int columnToResize = title.getColumnIndex();
			getContainer().setColumnWidth(columnToResize, 3000);
			int offset = getLeftmost()+1;								//note the offset from the sample label column
			//Iterate through each of the legacy LC compounds to fill out the table row
			for(int i = 0; i < values.length; i++) {
				final Cell c = header.createCell(i+offset);
				c.setCellValue(values[i].getName());
				c.setCellStyle(Style.TABLE_HEADER);
			}
			offset += values.length;									//note the offset from the primary table contents
			final Cell tTHC = header.createCell(offset); offset++;
			final Cell tCBD = header.createCell(offset); offset++;
			final Cell total = header.createCell(offset);
			tTHC.setCellStyle(Style.TABLE_HEADER);
			tCBD.setCellStyle(Style.TABLE_HEADER);
			total.setCellStyle(Style.TABLE_HEADER);
			tTHC.setCellValue("Total THC");
			tCBD.setCellValue("Total CBD");
			total.setCellValue("Total Mass");
		}
	}


	public static final int LC_TABLE_DISTANCE = 2, NUM_HEADERS = 2;
	
	static final class OldRelPerTable extends ReportTable {
		
		OldRelPerTable(final Sheet toGenerateWithin, final Map<Request,Map<String,LCResult>> mapOfSamples) {
			super(toGenerateWithin, NUM_HEADERS+LC_TABLE_DISTANCE+mapOfSamples.size()+4 ,DEFAULT_START_COL);
			sampleMap = mapOfSamples;
		}
		
		private final Map<Request,Map<String,LCResult>> sampleMap;
		
		
		@Override final void generate() throws IOException {
			final List<OldRelPerRow> tableRows = new LinkedList<>();
			for(final Request r : sampleMap.keySet())
				tableRows.add(new OldRelPerRow(r.getName(),sampleMap.get(r)));
			//Create the header and note the actual first table row
			generateHeader();
			final int offset = getHeaderRow()+1;
			for(int i = 0; i < tableRows.size(); i++)
				tableRows.get(i).generateRow(getContainer().createRow(i+offset), getLeftmost());
		}
		
		
		@Override final void generateHeader() {
			final LC_BY_RT[] values = LC_BY_RT.values();
			final Row header = getContainer().createRow(getHeaderRow());	//Create the header row
			final Cell sample = header.createCell(getLeftmost());			// Create the sample label cell
			final Cell title = header.createCell(getLeftmost()-1);
			sample.setCellValue("SAMPLE");									// and label it
			sample.setCellStyle(Style.TABLE_HEADER);						// and style it
			title.setCellValue("Relative %");								
			title.setCellStyle(Style.TABLE_HEADER);
			int offset = getLeftmost()+1;									//note the offset from the sample label column
			//Iterate through each of the legacy LC compounds to fill out the table row
			for(int i = 0; i < values.length; i++) {
				final Cell c = header.createCell(i+offset);
				c.setCellValue(values[i].getName());
				c.setCellStyle(Style.TABLE_HEADER);
			}
			offset += values.length;									//note the offset from the primary table contents
			final Cell tTHC = header.createCell(offset); offset++;
			final Cell tCBD = header.createCell(offset); offset++;
			final Cell total = header.createCell(offset);
			tTHC.setCellStyle(Style.TABLE_HEADER);
			tCBD.setCellStyle(Style.TABLE_HEADER);
			total.setCellStyle(Style.TABLE_HEADER);
			tTHC.setCellValue("Rel. THC");
			tCBD.setCellValue("Rel. CBD");
			total.setCellValue("Rel. Mass");
		}
	}
	
	
	
	
	static final class OldRelPerRow extends OldLiquidRow {
		
		private OldRelPerRow(String ofSample, Map<String,LCResult> mapOfSamples){
			super(ofSample,mapOfSamples);
		}
		
		
		final void generateRow(final Row toGenerateIn, final int startCol) throws IOException {
			final Cell sample = toGenerateIn.getCell(startCol);
			sample.setCellValue(this.getSample());
			sample.setCellStyle(Style.TABLE_LABEL);
			int offset = startCol+1;
			double totalDetectedMass = 0.0;
			double totalCBD = 0.0;
			double totalTHC = 0.0;
			double[] ratios = new double[LC_BY_RT.values().length];
			Cell[] valueCells = new Cell[LC_BY_RT.values().length];
			//Iterate through each compound in the LC enumeration and assign values from the original map
			for(int i = 0; i < LC_BY_RT.values().length; i++) {
				final LC_BY_RT compound = LC_BY_RT.values() [i];
				valueCells[i] = toGenerateIn.createCell(i+offset,CellType.NUMERIC);
				try {						//Attempt to get the ratio result from the map
					final double ratio = getResultMap().get(compound).getResultAsRatio();
					totalDetectedMass += ratio;
					if(compound == LC_BY_RT.CBD)
						totalCBD += ratio;
					else if (compound == LC_BY_RT.CBDa)
						totalCBD += ratio * ION_MASS_RATIO;
					else if (compound == LC_BY_RT.THC_d9)
						totalTHC += ratio;
					else if (compound == LC_BY_RT.THCa)
						totalTHC += ratio * ION_MASS_RATIO;
					if(totalDetectedMass > 1.0)
						throw new IOException(sample+" has >100% detected compounds "+totalDetectedMass*100.0);
					ratios[i] = ratio;
				} catch (final NullPointerException e) {
					ratios[i] = 0.0;	//If the map accessor returns a null value, assume the compound was not detected
				}
			}
			for(int i = 0; i < valueCells.length; i++) {
				valueCells[i].setCellStyle(Style.LC_DATA);
				valueCells[i].setCellValue(ratios[i]/totalDetectedMass);
			}
			offset += LC_BY_RT.values().length;
			final Cell tTHC = toGenerateIn.createCell(offset); offset++;
			final Cell tCBD = toGenerateIn.createCell(offset); offset++;
			final Cell total = toGenerateIn.createCell(offset);
			tTHC.setCellStyle(Style.TABLE_SUMMARY);
			tCBD.setCellStyle(Style.TABLE_SUMMARY);
			total.setCellStyle(Style.TABLE_SUMMARY);
			tTHC.setCellValue(totalTHC/totalDetectedMass);
			tCBD.setCellValue(totalCBD/totalDetectedMass);
			total.setCellValue(1.0);
		}
	}








	/**
	 * Data wrapper for the legacy GC sheet table rows
	 * @author talon
	 *
	 */
	private static final class LegacyGCRow {
		
		/**
		 * constructor
		 * @param name of sample this row reports
		 * @param map of compounds to final, valid data packets
		 */
		LegacyGCRow(final String ofSample, final Map<String,DataPacket> gcMap) {
			sample = ofSample;
			legacyMap = new HashMap<>();
			//Recreate the map with enumerated compounds instead of strings
			for(final String compound : gcMap.keySet()) {
				final GC_BY_RT valid = GC_BY_RT.getMatch(compound);
				if(valid != null)
					legacyMap.put(valid, gcMap.get(compound).getResult());
				else Terminal.say("Could not find enumerated "+compound+" from sample "+ofSample);
			}
		}
		private final String sample;
		private final Map<GC_BY_RT,Double> legacyMap;


		private final void generateRow(final Row toGenerateIn, final int startCol) {
			final Cell sample = toGenerateIn.getCell(startCol);
			sample.setCellValue(this.sample);
			sample.setCellStyle(Style.TABLE_LABEL);
			sample.getCellStyle().setBorderRight(BorderStyle.MEDIUM);
			final int offset = startCol+1;
			for(int i = 0; i < GC_BY_RT.values().length; i++) {
				final GC_BY_RT compound = GC_BY_RT.values()[i];
				final Cell c = toGenerateIn.getCell(i+offset);
				try {
					c.setCellValue(legacyMap.get(compound));
				} catch (final NullPointerException e) {
					c.setCellValue(0.0);
				}
				c.setCellStyle(Style.GC_DATA);
				toGenerateIn.getSheet().autoSizeColumn(i+startCol);
			}
		}
	}







	static final class OldGCTable extends ReportTable {
		
		OldGCTable(final Sheet toContainTable, final Map<Request,Map<String,DataPacket>> mapOfSamples) {
			super(toContainTable);
			sampleMap = mapOfSamples;
		}
		
		private final Map<Request,Map<String,DataPacket>> sampleMap;

		@Override final void generate() {
			final List<LegacyGCRow> rows = new LinkedList<>();
			for(final Request r : sampleMap.keySet())
				rows.add( new LegacyGCRow(r.getName(),sampleMap.get(r)) );
			generateHeader();
			for(int i = 0; i < rows.size(); i++)
				rows.get(i).generateRow(getContainer().createRow(i+1+getHeaderRow()), getLeftmost());
		}


		@Override final void generateHeader() {
			final GC_BY_RT[] values = GC_BY_RT.values();
			//Generates the header row
			final Row header = getContainer().createRow(getHeaderRow());
			final Cell s = header.createCell(getLeftmost());
			s.setCellValue("SAMPLE");
			s.setCellStyle(Style.TABLE_HEADER);
			final int offset = getLeftmost()+1;
			for(int i = 0; i < values.length; i++) {
				final Cell c = header.createCell(i+offset);
				c.setCellStyle(Style.TABLE_HEADER);
				c.setCellValue(values[i].getName());
			}
			for(int i = 0; i <= values.length; i++)
				getContainer().autoSizeColumn(i+getLeftmost());
		}
	}

	
	
	
	
	



	/**
	 * Data wrapper for a single row of the Audit Path sheet
	 * @author talon
	 *
	 */
	private static final class Source {
		
		// data fields for the columns of this row
		final String sampleName, compound, description, resultType, source;
		final Double area, result;
		
		/**
		 * source row constructor
		 * @param sample name 
		 * @param compound
		 * @param description	(usually dilution factor)
		 * @param area
		 * @param result		(could be ppm , area%, or mass%)
		 * @param source		(traceable path to Agilent data file | technician approval)
		 */
		Source(final String sampleName, final String compound, final String description, final Double area, 
				final String resultType, final Double result, final String source) {
			this.sampleName = sampleName;
			this.compound = compound;
			this.description = description;
			this.area = area;
			this.resultType = resultType;
			this.result = result;
			this.source = source;
		}

		

		/**
		 * Will fill the given row at the given start column with the local instance data
		 * @param row to generate within
		 * @param start column
		 */
		final void generate(final Row toGenerateIn, final int startCol) {
			final Cell nameCell 	= toGenerateIn.createCell(NAME_COL+startCol, 	CellType.STRING);
			final Cell compCell 	= toGenerateIn.createCell(COMP_COL+startCol,	CellType.STRING);
			final Cell descCell 	= toGenerateIn.createCell(DESC_COL+startCol, 	CellType.STRING);
			final Cell areaCell 	= toGenerateIn.createCell(AREA_COL+startCol, 	CellType.NUMERIC);
			final Cell rTypeCell	= toGenerateIn.createCell(R_TYPE_COL+startCol,	CellType.STRING);
			final Cell resultCell 	= toGenerateIn.createCell(RESULT_COL+startCol,	CellType.NUMERIC);
			final Cell srcCell 		= toGenerateIn.createCell(SRC_COL+startCol, 	CellType.STRING);
			nameCell.setCellValue(sampleName);
			nameCell.setCellStyle(Style.TABLE_LABEL);
			compCell.setCellValue(compound);
			descCell.setCellValue(description);
			areaCell.setCellValue(area);
			rTypeCell.setCellValue(resultType);
			resultCell.setCellValue(result);
			srcCell.setCellValue(source);
			srcCell.setCellStyle(Style.SOURCE);
			for(int i = 0; i <= SRC_COL; i++)
				toGenerateIn.getSheet().autoSizeColumn(i+startCol);
		}

	}














	/**
	 * Ordered enumeration of legacy compounds for the GC table
	 * @author talon
	 *
	 */
	public static enum GC_BY_RT {
		SEQUITERPENOIDS,
		FATTY__ACIDS,
		CBDV,
		CBT,
		QUINONE,
		CBL,
		CBD_OR_CBC,
		CBE,
		THC,
		CBG_OR_CBN,
		MONOGLYCERIDE,
		HYDROCARBONS___C27_HYPHEN_C29,
		STEROIDS,
		DIGLYCERIDE,
		TRIGLYCERIDE;


		
		/**
		 * Follows the replacement logic to finalize the compound name from the raw enum name
		 * @return formatted name
		 */
		final String getName() {
			String name = name();
			name = name.replaceAll("___", "_");
			name = name.replaceAll("_HYPHEN_", "-");
			name = name.replaceAll("_OR_", "/");
			name = name.replaceAll("__", " ");
			return name;
		}

		

		/**
		 * Iterates through the enum to find a valid match
		 * @param name of compound to find in enum
		 * @return  valid enumerated compound
		 */
		static final GC_BY_RT getMatch(final String nameToMatch) {
			for(final GC_BY_RT compound : GC_BY_RT.values())
				if(compound.getName().equalsIgnoreCase(nameToMatch))
					return compound;
				else continue;
			return null;
		}
	}





	
	/**
	 * Enumeration of legacy LC compounds, ordered by retention times
	 * @author talon
	 *
	 */
	public static enum LC_BY_RT {
		CBDva	("CBDVA"),
		CBDv	("CBDV"),
		CBDa	("CBDA"),
		CBGa	("CBGA"),
		CBG		("CBG"),
		CBD		("CBD"),
		THCv	("THCV"),
		THVa	("THCVA"),
		CBN		("CBN"),
		CBNa	("CBNA"),
		THC_d9	("THC-d9"),
		d8_THC	("d8-THC"),
		CBL		("CBL"),
		CBC		("CBC"),
		THCa	("THCA"),
		CBCa	("CBCA"),
		CBT		("CBT"),
		CBE1	("CBE1"),
		CBE2	("CBE2");
		
		private final String sheetName;
		private LC_BY_RT(String sheetName) {
			this.sheetName = sheetName;
		}
		
		
		
		/**
		 * Follows the replacement logic to finalize the compound name from the raw enum name
		 * @return formatted name
		 */
		public String getName() { return name().replaceAll("_", "-"); }
		public String getSheetName() { return sheetName; }

		
		
		/**
		 * Iterates through the enum to find a valid match
		 * @param name of compound to find in enum
		 * @return  valid enumerated compound
		 */
		public static LC_BY_RT getMatch(final String name) {
			for(final LC_BY_RT possibleMatch : LC_BY_RT.values())
				if(possibleMatch.getSheetName().equalsIgnoreCase(name))
					return possibleMatch;
				else continue;
			return null;
		}
	}
}
