# sim - a sample informatics machine
Data-Analysis software for generating report spreadsheets of GC and HPLC  analyses of cannabinoids.


Using Agilent cross-sequence summary tables (in the form of .xls files) and a calibration table
for the CANNABINOIDS_XX-VWD method family (also a .xls), this software can merge, supplement, source,
and finalize numerical results to generate human-readable reports for use with Microsoft Excel.

To build this software, import the repository using Eclipse's built-in git tooling.
Then, create a Maven run configuration with

      Goals: clean install compile package
      
This should generate a single fat, shaded jar file with all the code and dependencies bundled up.
Is this good practice?
      No.
Is this very convienient for a small project?
      Definitely.
  
Now one can simply run the jar file like any executable.
