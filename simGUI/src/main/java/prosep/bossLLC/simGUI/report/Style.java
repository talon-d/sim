package prosep.bossLLC.simGUI.report;

import java.io.IOException;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;






/**
 * Contains simple factory methods for creating cell styles when given
 * 	a workbook to hold the styles
 *
 * @author talon
 *
 */
@SuppressWarnings("unused")
public class Style
{



	/**
	 * Available cell styles
	 * @see createCellStyles(Workbook)
	 */
	public static CellStyle
	TABLE_HEADER,
	TABLE_LABEL,
	LC_DATA,
	HCLC_DATA,
	ACLC_DATA,
	GC_DATA,
	TABLE_SUMMARY,
	SOURCE;


	/*
	 * Reusable abbreviated style parameters
	 */
	private static final HorizontalAlignment
	CENTER 	= HorizontalAlignment.CENTER,
	RIGHT 	= HorizontalAlignment.RIGHT,
	LEFT 	= HorizontalAlignment.LEFT;
	private static final VerticalAlignment
	MIDDLE 	= VerticalAlignment.CENTER,
	BOTTOM 	= VerticalAlignment.BOTTOM,
	TOP 	= VerticalAlignment.TOP;
	private static final BorderStyle
	THIN = BorderStyle.HAIR,
	THICK = BorderStyle.MEDIUM;
	private static final FillPatternType
	FULL = FillPatternType.SOLID_FOREGROUND;
	private static final short
	HEADER_COLOR	= IndexedColors.PALE_BLUE.getIndex(),
	LABEL_COLOR		= IndexedColors.GREY_25_PERCENT.getIndex(),
	HIGH_CONF_COLOR	= IndexedColors.LIGHT_GREEN.getIndex(),
	LOW_CONF_COLOR	= IndexedColors.LIGHT_YELLOW.getIndex();



	/**
	 * Initializes all of the available styles when provided with a workbook
	 * @param workbook to provide styles for
	 */
	public static final void createCellStyles(final Workbook w){
		TABLE_HEADER = createHeaderStyle(w);
		TABLE_LABEL = createLabelStyle(w);
		LC_DATA = createLCDataStyle(w);
		HCLC_DATA = createHCLCDataStyle(w);
		ACLC_DATA = createACLCDataStyle(w);
		GC_DATA = createGCDataStyle(w);
		TABLE_SUMMARY = createTableSummaryStyle(w);
		SOURCE = createSourceStyle(w);
	}

	private static CellStyle createSourceStyle(Workbook w) {
		final CellStyle source = w.createCellStyle();
		source.setAlignment(RIGHT);
		return source;
	}

	private static CellStyle createACLCDataStyle(Workbook w) {
		final DataFormat percent = w.createDataFormat();
		final short percentDataFormat = percent.getFormat("0.00%");
		final CellStyle lcPercent = w.createCellStyle();
		lcPercent.setDataFormat(percentDataFormat);
		lcPercent.setAlignment(RIGHT);
		lcPercent.setVerticalAlignment(BOTTOM);
		lcPercent.setBorderTop(THIN);
		lcPercent.setBorderBottom(THIN);
		lcPercent.setBorderLeft(THIN);
		lcPercent.setBorderRight(THIN);
		lcPercent.setFillPattern(FULL);
		lcPercent.setFillForegroundColor(LOW_CONF_COLOR);
		return lcPercent;
	}

	private static CellStyle createHCLCDataStyle(Workbook w) {
		final DataFormat percent = w.createDataFormat();
		final short percentDataFormat = percent.getFormat("0.00%");
		final CellStyle lcPercent = w.createCellStyle();
		lcPercent.setDataFormat(percentDataFormat);
		lcPercent.setAlignment(RIGHT);
		lcPercent.setVerticalAlignment(BOTTOM);
		lcPercent.setBorderTop(THIN);
		lcPercent.setBorderBottom(THIN);
		lcPercent.setBorderLeft(THIN);
		lcPercent.setBorderRight(THIN);
		lcPercent.setFillPattern(FULL);
		lcPercent.setFillForegroundColor(HIGH_CONF_COLOR);
		return lcPercent;
	}

	public static final CellStyle createTableSummaryStyle(final Workbook w)
	{
		final DataFormat percent = w.createDataFormat();
		final short percentDataFormat = percent.getFormat("0.00%");
		final CellStyle tableSummary = w.createCellStyle();
		tableSummary.setDataFormat(percentDataFormat);
		tableSummary.setAlignment(CENTER);
		tableSummary.setVerticalAlignment(BOTTOM);
		tableSummary.setBorderTop(THIN);
		tableSummary.setBorderBottom(THIN);
		tableSummary.setBorderLeft(THIN);
		tableSummary.setBorderRight(THIN);
		tableSummary.setFillPattern(FULL);
		tableSummary.setFillForegroundColor(LABEL_COLOR);
		return tableSummary;
	}

	public static final CellStyle createLCDataStyle(final Workbook w)
	{
		final DataFormat percent = w.createDataFormat();
		final short percentDataFormat = percent.getFormat("0.00%");
		final CellStyle lcPercent = w.createCellStyle();
		lcPercent.setDataFormat(percentDataFormat);
		lcPercent.setAlignment(RIGHT);
		lcPercent.setVerticalAlignment(BOTTOM);
		lcPercent.setBorderTop(THIN);
		lcPercent.setBorderBottom(THIN);
		lcPercent.setBorderLeft(THIN);
		lcPercent.setBorderRight(THIN);
		return lcPercent;
	}



	public static final CellStyle createHeaderStyle(final Workbook w)
	{
		final CellStyle header = w.createCellStyle();
		header.setAlignment(CENTER);
		header.setVerticalAlignment(MIDDLE);
		header.setBorderBottom(THICK);
		header.setBorderTop(THICK);
		header.setBorderLeft(THIN);
		header.setBorderRight(THIN);
		header.setFillPattern(FULL);
		header.setFillForegroundColor(HEADER_COLOR);
		return header;
	}


	public static final CellStyle createLabelStyle(final Workbook w)
	{
		final CellStyle label = w.createCellStyle();
		label.setAlignment(LEFT);
		label.setVerticalAlignment(MIDDLE);
		label.setBorderTop(THIN);
		label.setBorderBottom(THIN);
		label.setBorderLeft(THICK);
		label.setBorderRight(THICK);
		label.setFillPattern(FULL);
		label.setFillForegroundColor(LABEL_COLOR);
		return label;
	}


	public static final CellStyle createGCDataStyle(final Workbook w)
	{
		final DataFormat notPercent = w.createDataFormat();
		final short percentDataFormat = notPercent.getFormat("0.00");
		final CellStyle gcPercent = w.createCellStyle();
		gcPercent.setDataFormat(percentDataFormat);
		gcPercent.setAlignment(RIGHT);
		gcPercent.setVerticalAlignment(BOTTOM);
		gcPercent.setBorderTop(THIN);
		gcPercent.setBorderBottom(THIN);
		gcPercent.setBorderLeft(THIN);
		gcPercent.setBorderRight(THIN);
		return gcPercent;
	}


}
