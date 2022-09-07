package en.talond.simGUI.report;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import en.talond.simGUI.*;
import en.talond.simGUI.data.*;
import en.talond.simGUI.io.*;
import en.talond.simGUI.report.ReportTable.*;





/**
 * This class is used to construct excel cannabinoid report files.
 * 
 * When an instance is created, the workbook file is created in memory.
 * Use the writeReport function to write the file in memory to the disk,
 * then use the close function to erase close the file in memory.
 * @author talon
 *
 */
public class Report {

	
	
	
	
	
	//Workbook sheet names
	public static final String
	LC_SHEET_NAME 	= "LC Report",
	GC_SHEET_NAME 	= "GC Report",
	DATA_SHEET_NAME = "Audit Path";
	//markers to differentiate gc and lc compounds in the Audit Path sheet
	private static final String
	LC_SRC_MARK = " :LC",
	GC_SRC_MARK = " :GC";
	
	
	
	
	
	
	
	
	
	
	//report instance fields
	private final Workbook report;
	private final LocalDate date;
	private final List<Request> samples;
	private final List<Puma> pumas;
	private final CalTable calibration;
	
	
	
	/**
	 * Constructs a report given lists of samples to report.
	 * @param list of samples to report
	 * @param list of puma sets to report
	 * @param calibration of canna method
	 * @param date of report (only used for filename)
	 * @throws Exception if the report could not be assembled (likely invalid data)
	 */
	public Report(List<Request> toReport, List<Puma> setsToReport, CalTable ofCannaMethod, LocalDate ofReport) throws Exception {
		samples = toReport;
		pumas = setsToReport;
		calibration = ofCannaMethod;
		date = ofReport;
		report = assembleReport(samples,pumas,calibration);
	}

	
	
	
	
	
	
	
	
	/**
	 * Attempts to write the assembled inner workbook to an xlsx file
	 * @param file to write to
	 * @throws IOException if the file could not be written for any reason
	 */
	@SuppressWarnings("deprecation")
	public final void writeReport(File toWriteTo) throws IOException {
		Terminal.say("Writing report file...");
		if(!toWriteTo.isDirectory())
			throw new IOException("Couldn't write report as requested write location is not a directory!");
		if(!toWriteTo.exists())
			throw new IOException("Couldn't write report as the requested write location doesn't exist!");
		toWriteTo = new File(toWriteTo.getAbsolutePath()+"/ProcessReport_"+SampleFactory.assembleDate(date)+".xlsx");
		Storage.writeReport(report, toWriteTo);
		Terminal.say("Report file written!");
	}
	
	
	
	
	/**
	 * Destructor method
	 */
	public final void close() {
		try {
			report.close();
			Terminal.say("Closed Report file...");
		} catch (IOException e) {
			Terminal.say("Couldn't close report file!");
			Terminal.specifyFullError(e);
		}
	}
	
	
	
	
	
	
	
	/**
	 * Used by the constructor to assemble the inner workbook in memory.
	 * @param samples to report
	 * @param puma sets to report
	 * @param calibration
	 * @return assembled workbook
	 * @throws Exception 
	 */
	private static final Workbook assembleReport(List<Request> toReport, List<Puma> setsToReport, CalTable toCheckAgainst) throws Exception {
		//excel workbook creation
		Terminal.say("Workbook contents initializing...");
		Workbook report = new XSSFWorkbook();
		Style.createCellStyles(report);
		final Sheet lc = report.createSheet(LC_SHEET_NAME),
				gc = report.createSheet(GC_SHEET_NAME),
				data = report.createSheet(DATA_SHEET_NAME);
		report.setMissingCellPolicy(MissingCellPolicy.CREATE_NULL_AS_BLANK);
		//raw data containers for report table elements
		final Map<Request,Map<String,DataPacket[]>> sourcesOfAll = new HashMap<>();
		final Map<Request,Map<String,LCResult>> lcDataOfAll = new HashMap<>();
		final Map<Request,Map<String,DataPacket>> gcDataOfAll = new HashMap<>();
		Terminal.say("Workbook contents initialized!");
		//iterate through each request and finalize its lc data, gc data, and their respective sources
		for(Request r : toReport) {
			String name = r.getName();
			Terminal.say("Reporting "+name+"...");
			final DataSet d = WorkingSet.getDataSetOf(r);
			Map<String,LCResult> lcData = createLCMapV2(r,d,toCheckAgainst);
			Map<String,DataPacket> gcData = createGCMap(d);
			Map<String,DataPacket[]> sources = createSourceMap(lcData,gcData);
			if(lcData != null)
				lcDataOfAll.put(r, lcData);
			if(gcData != null)
				gcDataOfAll.put(r, gcData);
			sourcesOfAll.put(r, sources);
		}
		//iterate through each puma set to finalize all the data
		for(Puma p : setsToReport) {
			//iterate through each element of the puma set to finalize all the data
			Request[] sampleSet = p.generateRequests();
			DataSet[] dataSet = WorkingSet.getDataSetsOf(p);
			for(int i = 0; i < sampleSet.length; i++) {
				Request ofSet = sampleSet[i];
				Terminal.say("Reporting "+ofSet.getName()+"...");
				DataSet d = dataSet[i];
				Map<String,LCResult> lcData = createLCMapV2(ofSet,d,toCheckAgainst);
				Map<String,DataPacket> gcData = createGCMap(d);
				Map<String,DataPacket[]> sources = createSourceMap(lcData,gcData);
				if(lcData != null)
					lcDataOfAll.put(ofSet, lcData);
				if(gcData != null)
					gcDataOfAll.put(ofSet, gcData);
				sourcesOfAll.put(ofSet, sources);
			}
		}
		Terminal.say("Generated report contents! Writing tables to spreadsheet...");
		//Use the generated lists to fill out the spreadsheet
		new OldLiquidTable(lc,lcDataOfAll).generate();
		new OldRelPerTable(lc,lcDataOfAll).generate();
		new OldGCTable(gc,gcDataOfAll).generate();
		new SourceTable(data,sourcesOfAll).generate();
		//place the color key
		ReportTable.generateKey(lc, 4, 0);
		Terminal.say("Report file generated!");
		return report;
	}
	
	
	private static final Map<String,LCResult> createLCMapV2(Request r, DataSet d, CalTable t) throws Exception {
		Terminal.say("Creating LC Map for "+r.getName());
		if(!d.hasCanna() && r.needsCanna())
			throw new Exception("Unable to make LC map for request which needs canna: "+r.getName());
		else if(r.needsFTHC() && !d.hasFthc())
			throw new Exception("No fTHC results found for sample which requires it: "+r.getName());
		else if (!d.hasCanna()) {
			Terminal.say("No canna results, and no requirement. Null map returned.");
			return null;
		} else {
			Map<String,LCResult> lcMap = new HashMap<>();
			boolean useMassPercent = d.shouldUseMassPercent();
			String[] compounds;
			if(useMassPercent)
				compounds = d.getCompoundsOfMass();
			else
				compounds = d.getCompoundsOfPPM();
			for(String compound : compounds) {
				Terminal.say("Finalizing LC data for "+compound);
				DataPacket[] data;
				boolean highConfidence;
				boolean anyBelowMaxCalibration = false;
				if(useMassPercent)
					data = d.getCannaMassResultsOf(compound);
				else
					data = d.getCannaPPMResultsOf(compound);
				Float minCal = t.getLow(compound);
				Float maxCal = t.getHigh(compound);
				Terminal.say("!!!"+minCal+"\t\t"+maxCal);
				Terminal.say("Is mass? "+useMassPercent+"\t\tHas "+data.length+" unfiltered packets");
				if(data.length > 1)
					data = useCalibrationToFinalizePackets(data,minCal,maxCal);
				if(r.isTargetCompound(compound) && data.length < 1)
					throw new Exception(compound+" is a target but there isn't enough results!");
				else if (data.length > 2)
					highConfidence = true;
				else highConfidence = false;
				for(DataPacket dp : data) {
					Double area = dp.getArea();
					if(area < maxCal) {
						anyBelowMaxCalibration = true;
						break;
					} else continue;
				}
				LCResult ofCompound = new LCResult(data,useMassPercent,highConfidence,!anyBelowMaxCalibration);
				lcMap.put(compound, ofCompound);
				Terminal.say(ofCompound.toString());
			}
			if (r.needsFTHC() || d.hasFthc()) {
				String thcKey = LC_BY_RT.THC_d9.getSheetName();
				DataPacket[] data = d.getFthcResults();
				lcMap.remove(thcKey);
				DataPacket finalized;
				if(data.length == 1)
					finalized = data[0];
				else if (data.length > 1)
					finalized = MainMenu.manuallySelectOnePacket(r.getName(), thcKey, data);
				else 
					finalized = DataPacket.generateFTHCNondetect("Autoselected");
				LCResult fromFTHC;
				if(finalized.getSource().contains("but below LoQ according to:"))
					fromFTHC = new LCResult(new DataPacket[] {finalized},true,false,true);
				else 
					fromFTHC = new LCResult(new DataPacket[] {finalized},true,true,false);
				lcMap.put(thcKey, fromFTHC);
			}
			return lcMap;
		}
	}
	
	
	
	
	
	
	
	private static final DataPacket[] useCalibrationToFinalizePackets(DataPacket[] allOfCompound, Float minCal, Float maxCal) {
		Terminal.say("Finalizing data packets:");
		DataPacket[] finalized;
		for(DataPacket dp : allOfCompound)
			Terminal.say(dp.stringify());
		DataPacket[] calibrated = DataPacket.findOnlyWithinCalibration(allOfCompound,minCal,maxCal);
		Terminal.say("There are "+calibrated.length+" packets within calibration!");
		DataPacket best = DataPacket.findClosestToCalibrationMidpoint(allOfCompound, minCal, maxCal);
		if(calibrated.length >= 1) {
			finalized = DataPacket.useRSDToMinimize(calibrated);
			Terminal.say("\tUsing calibrated...");
		} else {
			finalized = new DataPacket[] {best};
			Terminal.say("\tUsing midpoint...");
		}
		Terminal.say("Finalized packets:");
		for(DataPacket dp : finalized)
			Terminal.say(dp.stringify());
		return finalized;
	}
	

	
	
	
	
	
	/**
	 * Creates a map with the latest detection of each GC compound.
	 * @param data to make gc map with 
	 * @return null of map
	 */
	private static final Map<String,DataPacket> createGCMap(DataSet d) {
		if(!d.hasGC()) {
			return null;
		} else {
			Map<String,DataPacket> gcMap = new HashMap<>();
			for(String compound : d.getCompoundsOfGC())
				gcMap.put(compound,d.getLatestGCResultOf(compound));
			return gcMap;
		}
	}
	
	
	
	
	
	
	private static final Map<String,DataPacket[]> createSourceMap(Map<String,LCResult> lcData, Map<String,DataPacket> gcData) {
		Map<String,DataPacket[]> sources = new HashMap<>();
		if(lcData != null)
			for(String compound : lcData.keySet())
				sources.put(compound+LC_SRC_MARK, lcData.get(compound).getOriginalData());
		if(gcData != null)
			for(String compound : gcData.keySet())
				sources.put(compound+GC_SRC_MARK, new DataPacket[] {gcData.get(compound)});
		return sources;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	static final class LCResult {
		/* final data to use in the report */
		private final DataPacket[] 	usedToCreate;
		private final boolean 
		highConfidence,
		aboveCalibration,
		alreadyMassPercent;
		/* constructor */
		private LCResult(final DataPacket[] declaredValid, final boolean alreadyMassPercent, 
				final boolean isHighConfidence, final boolean isAboveCalibration) {
			this.alreadyMassPercent = alreadyMassPercent;
			aboveCalibration = isAboveCalibration;
			usedToCreate = declaredValid;
			highConfidence = isHighConfidence && !isAboveCalibration;
		}
		final Double getAbsoluteMassPercent() {
			if(alreadyMassPercent)
				return DataPacket.calcMean(usedToCreate);
			else
				return DataPacket.calcMeanFromPPM(usedToCreate);
		}
		final Double getResultAsRatio() { return getAbsoluteMassPercent() / 100.0; }
		final boolean isHighConfidence()		{ return highConfidence; }
		final boolean isAboveCalibration()		{ return aboveCalibration; }
		final DataPacket[] getOriginalData()	{ return usedToCreate; }
		final String[] getSources() {
			final List<String> sources = new ArrayList<>(usedToCreate.length);
			for(final DataPacket dp : usedToCreate)
				if(!sources.contains(dp.getSource()))
					sources.add(dp.getSource());
				else continue;
			return sources.toArray(new String[] {});
		}
	}
	
	


}
