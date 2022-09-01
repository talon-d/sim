package prosep.bossLLC.simGUI.data;

import java.time.LocalDate;

import prosep.bossLLC.simGUI.io.SampleFactory;







/**
 * This class represents a BOSS sample request.
 * 		A sample request contains two major categories of data:
 * 				* sample identifiers
 * 				* analysis requirements
 * 		No two samples should ever share all 4 identifiers. Really, do not do this Gil. I would be impressed if you did.
 * 		Analysis requirements MUST be met in order to formally report a sample.
 * @author talon
 *
 */
public class Request {

	
	
	
	
	//characters which are not allowed within the prefix or suffix of a sample
	public static final String DISALLOWED_CHARACTERS = "[ >!\t#_]";

	
	
	
	
	/* Sample Name Components */
	private final String prefix, suffix;
	private final LocalDate date;
	private final Batch id;
	/* Analysis Requests */
	private final boolean 
	needsCanna,
	needsFTHC,
	needsGC;
	private final String[] targetCompounds;

	
	
	/**
	 * Sample analysis request constructor
	 * @param prefix
	 * @param date
	 * @param suffix
	 * @param batch id
	 * @param needsCanna
	 * @param needsFTHC
	 * @param needsGC
	 * @param targetCompounds
	 */
	public Request( /* Sample Name Components: */  final String prefix, final LocalDate date, final String suffix, final Batch id,
					/* Analysis Flags: */ final boolean needsCanna, final boolean needsFTHC, final boolean needsGC,
					/* Prep Flags */ final String[] targetCompounds) {
		// set name components
		this.prefix = reformIdentifier(prefix);
		this.suffix = reformIdentifier(suffix);
		this.date 	= date;
		this.id 	= id;
		// set analysis flags
		this.needsCanna = needsCanna;
		this.needsFTHC  = needsFTHC;
		this.needsGC    = needsGC;
		this.targetCompounds = targetCompounds;
	}



	/* returns the (hopefully) unique string object associated with the instance, assembled from its identifiers */
	public final String getName() 		{ return assembleName(this); }
	/* sample identifiers */
	final String getPrefix() 		{ return prefix; }
	final LocalDate getDate() 		{ return date; }
	final String getSuffix() 		{ return suffix; }
	final Batch getBatch() 			{ return id; }
	/* Analysis Request Getters */
	public final boolean needsCanna() 			{ return needsCanna; }
	public final boolean needsFTHC()  			{ return needsFTHC;  }
	public final boolean needsGC()				{ return needsGC;	 }
	public final String[] getTargetCompounds() 	{ return targetCompounds; }



	/**
	 * Used to display condensed analysis metadata to various menus.
	 * @return string explaining flags of this request
	 */
	public final String getFlags() {
		return "needs::\tgc:"+needsGC+"; canna:"+needsCanna+"; fthc:"+needsFTHC+"; #targets: "+targetCompounds.length;
	}
	
	
	
	
	
	
	
	
	
    /**
     * Examines how well a dataset fulfills its request.
     * Not perfect, but imperfections should be resolved as errors
     * during ProcessReport generation.
     * @param request to analyze
     * @param data to attempt fulfillment
     * @return -1 if typical error, 0 if warn, 1 if perfect, -2 for catastrophic logical error
     */
    public final int checkRequestRequirements(DataSet d) {
    	boolean cannaFulfilled = !needsCanna() || d.hasCanna();
    	boolean fthcFulfilled = !needsFTHC() || d.hasFthc();
    	boolean gcFulfilled = !needsGC() || d.hasGC();
    	boolean targetsFulfilled = true;
    	for(String target : getTargetCompounds())
    		targetsFulfilled = targetsFulfilled & d.canBeConfident(target);
    	if(!targetsFulfilled || !cannaFulfilled)
    		return -1;
    	else if (!gcFulfilled || !fthcFulfilled)
    		return 0;
    	else if (targetsFulfilled & cannaFulfilled)
    		return 1;
    	else return -2;
    }
    
    
    
    
    
    
	
	/**
	 * Uses the assembled name to assert equivalence
	 * @param other request
	 * @return true if identifiers match
	 */
	public final boolean equals(Request other) {
		return other.getName().equalsIgnoreCase(this.getName());
	}
	
	
	
	
	
	
	
	/**
	 * Boilerplate to check if a compound is targeted by this request
	 * @param compound name to check
	 * @return true if targeted by this
	 */
	public final boolean isTargetCompound(String compoundName) {
		for(String target: targetCompounds) 
			if(target.equalsIgnoreCase(compoundName))
				return true;
			else continue;
		return false;
	}
	
	
	
	
	
	
	
	
	/**
	 * Inner factory method to formalize prefixes and suffixes during construction
	 * @param prefixOrSuffix
	 * @return possibly altered identifier string
	 */
	private static final String reformIdentifier(String prefixOrSuffix) {
		String reformed = prefixOrSuffix.toUpperCase();
		return reformed.replaceAll(DISALLOWED_CHARACTERS, "-");
	}
	
	
	
	
	
	
	
	
	/**
	 * Inner factory method to assemble name from identifiers
	 * @param request
	 * @return request name
	 */
	private static final String assembleName(final Request sample) {
		return sample.getPrefix()+"_"
				+SampleFactory.assembleDate(sample.getDate())+"_"
				+sample.getSuffix()
				+sample.getBatch().toString();
	}


}
