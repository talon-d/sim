package prosep.bossLLC.simGUI.data;

import java.util.LinkedList;
import java.util.List;

import prosep.bossLLC.simGUI.Terminal;


/**
 * The data packet is a broad, all encompassing class to wrap data sets.
 * The generic nature of packets, and the ease with which they are made from
 * results (using the abstract packetize function), allows very generic
 * functions to be written simply for data handling and analysis, as compared
 * to results which are highly specific.
 * However, because of the danger produced by using a highly generic class for many
 * different sorts of analysis, it is NOT recommended to instantiate DataPackets.
 * Instead, a DataSet will create them automatically from instances of Results.
 * @author talon
 *
 */
public class DataPacket {
	
	
	
	public static final double
	SOLVENT_DENSITY = 8200.0,	//solvent density of the PUMA system. Used to derive valid mass percent
	MAX_PRECISE_RSD = 3.0;		//Maximum RSD for a set of results to be declared as minimized
	//Assignable packet types
	public static final String
	GC_TYPE = "PEAK AREA %",
	MASS_TYPE = "MASS %",
	PPM_TYPE = "CALC'D PPM",
	FTHC_TYPE = "FTHC MASS %";
	
	
	
	
	
	
	
	
	
	
	
	
	
	private final String source, description, rType;
	private final Double prepAmount, df, area, result;
	
	
	
	/**
	 * packet for data with df's (LC results)
	 * @param source
	 * @param type of results
	 * @param prepAmount
	 * @param dilution
	 * @param area
	 * @param result
	 */
	public DataPacket(final String source, final String rType, final Double prepAmount, 
			final Double dilution, final Double area, final Double result) {
		this.source = source;
		this.prepAmount = prepAmount;
		this.area = area;
		this.result = result;
		this.rType = rType;
		df = dilution;
		description = null;
	}
	
	
	
	/**
	 * packet for GC data
	 * @param source
	 * @param type of results
	 * @param description
	 * @param area
	 * @param result
	 */
	public DataPacket(final String source, final String rType, final String description, 
			final Double area, final Double result) {
		this.source = source;
		this.description = description;
		this.area = area;
		this.result = result;
		this.rType = rType;
		prepAmount = null;
		df = null;
	}
	

	
	public final Double getArea() 	 			{ return area; }
	public final Double getResult() 			{ return result; }
	public final String getSource() 			{ return source; }
	public final Double getAmount() 			{ return prepAmount; }
	public final Double getDilution() 			{ return df; }
	public final String getDescription()		{ if(description != null) return description; else return df+"x"; }
	public final Double getMassPercentFromPPM() { return result / SOLVENT_DENSITY; }
	public String getResultType() 				{ return rType; }
	
	
	
	public final String stringify() {
		try {
			return area+" "+df+" "+rType+" "+source;
		} catch (NullPointerException e) {
			return area+" "+description+" "+rType+" "+source;
		}
	}
	
	
	
	/**
	 * evaluates whether two data packets are the same with their respective:
	 * 		sources, types, prep amounts, and results
	 * @param other packet
	 * @return true if equivalent
	 */
	public final boolean equals(DataPacket other) {
		boolean sameArea = area.intValue() == other.area.intValue();
		boolean sameSource = source.equalsIgnoreCase(other.source);
		boolean sameType = rType.equals(other.rType);
		boolean isIdenticalResult = sameArea & sameSource & sameType;
		return isIdenticalResult;
	}
	
	

	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Factory method for generating standardized fTHC packets for missing results.
	 * Nondetect results from the fTHC method by definition are not including on any
	 * submitted data sheets, and thus have no underlying packets to source when 
	 * generating a report. Having a standardized, signed, nondetect packet (as
	 * assembled by the method) remedies this issue.
	 * @param intials of technician who verified a nondetect result
	 * @return standardized nondetect packet
	 */
	public static final DataPacket generateFTHCNondetect(String initialsOfTechnician) {
		return new DataPacket("nondetect fTHC result according to lab technician: "+initialsOfTechnician.toUpperCase(), DataPacket.FTHC_TYPE, 0.0, 0.0, 0.0, 0.0);
	}
	
	
	
	/**
	 * Factory method for generating standardized fTHC packets for >LoQ results.
	 * @param initials of technician
	 * @return standardized >LoQ packet
	 */
	public static final DataPacket generateFTHCBelowLoq(String initialsOfTechnician) {
		return new DataPacket("fTHC shows THC is present, but below LoQ according to: "+initialsOfTechnician.toUpperCase(), DataPacket.FTHC_TYPE, 0.0, 0.0, 0.0, 0.0);
	}
	
	
	
	
	

	
	
	
	
	/**
	 * Finds all the data packets within the calibration bounds.
	 * May return an empty array if nothing is within calibration bounds.
	 * @param all data of compound
	 * @param min calibration
	 * @param max calibration
	 * @return calibrated packets
	 */
	public static DataPacket[] findOnlyWithinCalibration(DataPacket[] allOfCompound, Float minCal, Float maxCal) {
		List<DataPacket> withinCal = new LinkedList<>();
		for(DataPacket dp : allOfCompound) {
			Terminal.say("Packet area: "+dp.getArea()+" "+maxCal+" "+minCal);
			if(dp.getArea() <= maxCal && dp.getArea() >= minCal)
				withinCal.add(dp);
			else continue;
		}
		return withinCal.toArray(new DataPacket[] {});
	}
	
	
	
	/**
	 * Finds the single data packet that best fits the calibration.
	 * It is assumed that packets closer to the midpoint of the calibration
	 * are generally more accurate. This factor is easily superseded by other
	 * methods of data analysis, and should generally be regarded as a last-resort
	 * for collecting results of minor compounds. 
	 * @param data packets
	 * @param calibrated compound
	 * @param calibration table
	 * @return packet with a detected area closest to the midpoint
	 */
	public static DataPacket findClosestToCalibrationMidpoint(DataPacket[] allOfCompound, Float minCal, Float maxCal) {
		//in the event that an empty data array is given, a null packet is returned
		try {
			final double calMidpoint = (maxCal - minCal) / 2 + minCal;
			int indexOfClosest = 0;
			double minDeviance = Math.abs(allOfCompound[0].getArea() - calMidpoint);
			for(int i = 1; i < allOfCompound.length; i++) {
				final double currentDeviance = Math.abs(allOfCompound[i].getArea() - calMidpoint);
				if(currentDeviance < minDeviance) {
					indexOfClosest = i;
					minDeviance = currentDeviance;
				} else continue;
			}
			return allOfCompound[indexOfClosest];
		} catch (ArrayIndexOutOfBoundsException e) {
			Terminal.say("");
			return null;
		}
	}
	
	
	
	/**
	 * Rejects packets recursively until the given array is <= rsdLimit or
	 * 	until the array has a length of 1
	 * @param data packets to finalize
	 * @param acceptable rsd limit
	 * @return rsdLimited result(s), or an empty array if a null array was given
	 */
	public static final DataPacket[] useRSDToRejectPackets(final DataPacket[] data, final Double rsdLimit) {
		if(data == null)
			return new DataPacket[0];
		else if((data.length == 1) || (calcRSD(data) <= rsdLimit))
			return data;
		else
			return useRSDToRejectPackets(removeOnePacketToMinimizeRSD(data),rsdLimit);
	}

	
	
	/**
	 * Shrinks the argument by one element to obtain a set of results with the lowest possible RSD
	 * @param data[]
	 * @return data[n-1], where the element removed minimizes RSD, or simply the input if the array is size 1
	 * TODO perhaps this could be simplified by simplify finding the outlier?
	 */
	private static final DataPacket[] removeOnePacketToMinimizeRSD(final DataPacket[] data) {
		//base case for algorithm
		if(data.length < 2)
			return data;
		//the algorithm implementation
		else {
			//invalid initializations for trackers. The function SHOULD error in the event that these trackers
			//are not changed during the calculations
			Double minRSD = 100.0;
			int indexOfRejectionForMin = -1;
			//this could probably be greatly simplified to a remove the nested for loop,
			//but I'm too scared to mess with it. It works.
			for(int i = 0; i < data.length; i++) {
				final DataPacket[] oneLess = new DataPacket[data.length-1];
				boolean foundIndex = false;
				int currentRejection = -1;
				for(int j = 0; j < data.length; j++)
					if(j == i) {
						foundIndex = true;
						currentRejection = j;
					} else if(foundIndex)
						oneLess[j-1] = data[j];
					else
						oneLess[j] = data[j];
				final Double currentRSD = calcRSD(oneLess);
				if(currentRSD < minRSD) {
					minRSD = currentRSD;
					indexOfRejectionForMin = currentRejection;
				}
			}
			//iterate through the arg array, omitting the minimizing packet, then return it
			final DataPacket[] finalArray = new DataPacket[data.length-1];
			boolean hasRejected = false;
			for(int i = 0; i < data.length; i++)
				if(i == indexOfRejectionForMin)
					hasRejected = true;
				else if(hasRejected)
					finalArray[i-1] = data[i];
				else
					finalArray[i] = data[i];
			return finalArray;
		}
	}

	
	
	
	
	
	
	
	
	
	
	
	
	


	/**
	 * Factory methods to simplify handling of data packets
	 * @param data packets to evaluate
	 * @return evaluation
	 */
	public static final Double calcRSD(final DataPacket[] data) 				{ return calcRSD(collectResults(data)); }
	public static final Double calcMean(final DataPacket[] data) 				{ return calcMean(collectResults(data)); }
	public static final Double calcMeanFromPPM(final DataPacket[] ppmData)		{ return calcMean(collectPPMResults(ppmData)); }
	public static final DataPacket[] useRSDToMinimize(final DataPacket[] data) 	{ return useRSDToRejectPackets(data,MAX_PRECISE_RSD); }

	
	
	/**
	 * Calculates the arithmetic mean of an array of numbers
	 * @param numbers
	 * @return average of numbers
	 */
	private static final Double calcMean(final Double[] data) {
		double mean = 0.0;
		for(final Double d : data)
			mean += d;
		return mean / data.length;
	}

	

	/**
	 * Calculates the relative standard deviation of an array of numbers
	 * @param array of numbers
	 * @return RSD of numerical array
	 */
	private static final Double calcRSD(final Double[] nums) {
		if(nums.length == 0)
			return -1.0;
		else if (nums.length == 1)
			return 0.0;
		else {
			final Double mean = calcMean(nums);
			final Double[] deviations = new Double[nums.length];
			for(int i = 0; i < deviations.length; i++)
				deviations[i] = (nums[i]-mean)*(nums[i]-mean);
			Double variation = 0.0;
			for(final Double deviation : deviations)
				variation += deviation;
			final Double stddev = Math.sqrt(variation);
			return stddev / mean * 100.0;
		}
	}



	/**
	 * Gets the raw mass percent from a standard LC packet
	 * @param data packets
	 * @return numerical results
	 */
	private static final Double[] collectResults(final DataPacket[] data) {
		final Double[] results = new Double[data.length];
		for(int i = 0; i < data.length; i++)
			results[i] = data[i].getResult();
		return results;
	}
	
	
	
	/**
	 * Gets the calculated Mass % results of an array of PPM packets
	 * @param data
	 * @return
	 */
	private static final Double[] collectPPMResults(DataPacket[] data) {
		final Double[] results = new Double[data.length];
		for(int i = 0; i < data.length; i++)
			results[i] = data[i].getMassPercentFromPPM();
		return results;
	}
	

}
