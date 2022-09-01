package prosep.bossLLC.simGUI.data;






/**
 * This class provides a very simple abstraction for containing
 * and displaying information about a sample's block number.
 * The block number is used by operators to distinguish iterations
 * of a process.
 * @author talon
 *
 */
public class Batch {

	
	
	private final double batchNum;
	private final int topOfRange, lastOfRange;
	private final boolean nullRange;

	

	/**
	 * This constructs a simple batch.
	 * @param block number (should be integer or can have tens place filled out)
	 */
	public Batch(final double blockNum) {
		batchNum = blockNum;
		nullRange = true;
		topOfRange = 0;
		lastOfRange = 0;
	}
	
	
	
	/**
	 * This constructs a ranged batch.
	 * @param block number (should be integer or can have tens place filled out)
	 * @param first block agglomerated here
	 * @param last block agglomerated here
	 */
	public Batch(final double blockNum, final int topOfRange, final int lastOfRange) {
		batchNum = blockNum;
		this.topOfRange = topOfRange;
		this.lastOfRange = lastOfRange;
		nullRange = false;
	}
	
	
	
	
	
	
	
	
	
	
	//Static portions of block numbers
	private static final String n = "#", o = ":(", h = "-", c = ")";
	
	/**
	 * Builds the final block id string.
	 * Used when constructing sample names.
	 * @return batch display string ("#[0-9]*\\.[0-9]")
	 */
	@Override public String toString() {
		return getBatch()+getRange();
	}
	
	

	
	



	
	/**
	 * Assembles the string representation of the block 
	 * number from the inner double field. 
	 * @return block number display string
	 */
	private final String getBatch() {
		if(!isInt(batchNum)) {
			String id = Double.toString(batchNum);
			final int decimalIndex = id.indexOf(".");
			id = id.substring(0,decimalIndex+2);
			return n+id;
		} else return n+Integer.toString((int) batchNum);
	}
	
	
	
	/**
	 * Returns the display string of this sample's range.
	 * Will provide an empty string if this is a simple batch.
	 * @return range display string
	 */
	private final String getRange() {
		if(nullRange)
			return "";
		else
			return o+topOfRange+h+lastOfRange+c;
	}


	
	
	
	
	
	
	

	/**
	 * Checks if a double could be represented as an int.
	 * @param decimal number
	 * @return true if can have int representation
	 */
	private static final boolean isInt(final double num) {
		if(num % 1.0 == 0.0)
			return true;
		else return false;
	}


	
}
