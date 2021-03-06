/*
 * Copyright [1999-2015] Wellcome Trust Sanger Institute and the EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ensembl.healthcheck.testcase.funcgen;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.DatabaseType;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;
import org.ensembl.healthcheck.util.DBUtils;
import org.ensembl.healthcheck.Team;


/**
 * Check Array xrefs: - that each chromosome has at least 1 Probe/Set xref
 *
 * Assumptions: Array Probe/ProbeSet xrefs and transcripts are both in the default chromosome coordinate system.
 *
 */


/**
 * To do
 * 1 Add support for Probe level xrefs
 * 2 Group counts by array? This is already done in ComparePreviousVersionArraysXrefs?
 */

public class ArrayXrefs extends SingleDatabaseTestCase {

	// if a database has more than this number of seq_regions in the chromosome coordinate system, it's ignored
	private static final int MAX_CHROMOSOMES = 75;

	/**
	 * Creates a new instance of OligoXrefs
	 */
	public ArrayXrefs() {

		//addToGroup("post_genebuild");
		addToGroup("funcgen");
		addToGroup("funcgen-release");
		setTeamResponsible(Team.FUNCGEN);
		setDescription("Check Array probe2transcript xrefs");
		setHintLongRunning(true);

	}

	public void types() {
		removeAppliesToType(DatabaseType.CORE);
		removeAppliesToType(DatabaseType.OTHERFEATURES);
		removeAppliesToType(DatabaseType.CDNA);
		removeAppliesToType(DatabaseType.VEGA);
	}

	/**
	 * Check all chromosomes have xrefs for each DISPLAYABLE array.
	 *
	 * Get a list of chromosomes, then check the number of xrefs associated with each one. Fail is any chromosome has 0 xrefs.
	 * Is this even possible now we have transcripts in a separate DB?
	 * This is really essential for new arrays, as we can't rely on ComparePrevious
	 * Could do this via cross DB query only if on same server
	 *
	 * @param dbre
	 *          The database to use.
	 * @return true if the test passed.
	 *
	 */
	public boolean run(DatabaseRegistryEntry dbre) {

		boolean result = true;
		Connection efgCon = dbre.getConnection();


		/** To do
		 *  Change xref counting to include all db versions, warn if more than one and fail if some are missing
		 *  Write perl script to log counts?
		 */



		// Check if there are any DISPLAYABLE Arrays - if so there should be  Xrefs
		// Checks EPXRESSION and CGH arrays only


//		Integer displayableArrays = DBUtils.getRowCount(efgCon, "SELECT COUNT(*) FROM array a, status s, status_name sn where sn.name='DISPLAYABLE' and " +
//														"sn.status_name_id=s.status_name_id and s.table_name='array' and s.table_id=a.array_id");

		int expressionArrays = DBUtils.getRowCount(efgCon, "SELECT COUNT(*) FROM array a where (format='EXPRESSION' OR format='CGH')");

		if ( expressionArrays == 0) { //Assume we should always have EXPRESSION arrays
			ReportManager.problem(this, efgCon, DBUtils.getShortDatabaseName(efgCon) +
								  " has no EXPRESSION Arrays, not checking for probe2transcript xrefs");
			return false;
		}
		else{
			ReportManager.correct(this, efgCon, DBUtils.getShortDatabaseName(efgCon) + " has " + expressionArrays +
						" EXPRESSION|CGH arrays");
		}



		//	if ( displayableArrays < expressionArrays ){
		//		ReportManager.problem(this, efgCon, "Database contains non-DISPLAYABLE EXPRESSION Arrays");
		//		result = false;
		//	}


		StringBuffer hiddenArrays = new StringBuffer();

		try {
			ResultSet rs = efgCon.createStatement().executeQuery("SELECT a.name, sn1.name from array a left join " +
					"(SELECT s.table_id, sn.name from status s, status_name sn where sn.name='DISPLAYABLE' and " +
					"sn.status_name_id=s.status_name_id and s.table_name='array') sn1 on sn1.table_id=a.array_id " +
					"WHERE (a.format ='EXPRESSION' OR a.format='CGH')");

			while (rs.next()){
				String arrayStatus = rs.getString(2);
				String arrayName   = rs.getString(1);

				if (arrayStatus == null)	hiddenArrays.append(arrayName + " ");
			}

			rs.close();

			if(hiddenArrays.length() != 0){
				result = false;
				ReportManager.problem(this, efgCon, "Database contains non-DISPLAYABLE EXPRESSION Arrays:\t" + hiddenArrays);
			}
			else{
				ReportManager.correct(this, efgCon, "All EXPRESSION|CGH arrays in " + DBUtils.getShortDatabaseName(efgCon) +
						" are DISPLAYABLE");

			}

		} catch (SQLException se) {
			se.printStackTrace();
			return false;
		}


		//Get the matching core dbre to do the cross DB join
		//Assume we have the standard name for the core DB and it is on the same host
		String schemaBuild = dbre.getSchemaVersion() + "_" + dbre.getGeneBuildVersion();
		String coreDBName = getCoreDbName(dbre, schemaBuild);

		//Never get's loaded if we specify a pattern
		//DatabaseRegistryEntry coreDbre = dbre.getDatabaseRegistry().getByExactName(coreDBName);

		System.out.println("Getting DBRE by pattern:\t" + coreDBName);

		DatabaseRegistryEntry coreDbre = getDatabaseRegistryEntryByPattern(coreDBName);

		//This may still not be on the same server if database.properties has two servers configured


		if (coreDbre == null){
			ReportManager.problem(this, efgCon, "Could not default core DB:\t" + coreDBName);
			return false;
		}

		//And test the hosts are the same
		//assume user/pass will be able to access DBs on the same server
		//do not test DatabaseServer object, it may be a different
		if (! coreDbre.getDatabaseServer().getDatabaseURL().equals(dbre.getDatabaseServer().getDatabaseURL())){
			ReportManager.problem(this, efgCon, "Unable to perform chromosome xref counts as efg and core DB are not on the same DatabaseServer:\t" +
								  "core " + coreDbre.getDatabaseServer().getDatabaseURL() + "\tefg " + dbre.getDatabaseServer().getDatabaseURL());
			return false;
		}


		// find all chromosomes in default assembly coordinate system
		// should really be parameterized
		Map<String, String> srID2name    = new HashMap<String, String>();
		Map<String, String> coreSrID2efg = new HashMap<String, String>();

		// Die if we don't see the current schema build and is the only one that is_current
		// Otherwise we cannot be sure that all seq_region records have been updated
		String csName = DBUtils.getRowColumnValue(coreDbre.getConnection(),  
      "SELECT name FROM coord_system order by rank desc limit 1");
		
		if(csName == null){
			ReportManager.problem(this, efgCon, "Could not identify coord_system entries for schema_build:\t" + schemaBuild);
			return false;	
		}


		try {

      ResultSet rs = efgCon.createStatement().executeQuery("SELECT coord_system_id, rank, schema_build " + 
        "FROM coord_system WHERE is_current=1 and schema_build is not null AND name='" + csName + "'");
      // Should never have null schema_build entries

      int csRank = 999999999;
      String csID = ""; // Can't just declare here and init in the while as this causes a compilation error

      while(rs.next()){

        if (! rs.getString(3).equals(schemaBuild)){
          ReportManager.problem(this, efgCon, 
            "Found an 'is_current' " + csName + "coord_system with unexpected schema_build:\t" + rs.getString(3));
          return false;
        }

        // Do we need a attrib 'default' check here too?

        // Get highest ranking csID
        if (rs.getInt(2) < csRank){
          csRank = rs.getInt(2);
          csID   = rs.getString(1);
        }
      }
      rs.close();

      rs = efgCon.createStatement().executeQuery("SELECT s.seq_region_id, s.name, s.core_seq_region_id " + 
        "FROM seq_region s WHERE s.coord_system_id=" + csID + 
        " and s.name not like '%\\_%' group by s.seq_region_id");
		
			//Do we even need this core_seq_region_id translation?
			//Just link via the sr.name!

			while (rs.next()){
				srID2name.put(rs.getString(1), rs.getString(2));
				coreSrID2efg.put(rs.getString(3), rs.getString(1));
			}
			rs.close();

			if (srID2name.size() > MAX_CHROMOSOMES) {
				ReportManager.problem(this, efgCon, "Database has more than " + MAX_CHROMOSOMES + " seq_regions in 'chromosome' coordinate system (actually " + srID2name.size() + ") - test skipped");
				return false;
			}

			// Count the number of xrefs for each chr
			Map<String, String> coreSrIDcounts = new HashMap<String, String>();
			// (Optimisation: faster to use "in list" of external_db_ids than SQL
			// join.)
			StringBuffer inList        = new StringBuffer();
			String       edbName       = dbre.getSpecies() + "_core_Transcript";
			String[]     assemblyBuild = schemaBuild.split("_");
			String       xrefQuery     = "";
			String       edbClause     = "";

			//We really need to match the genebuild between the edb and the schema_build
			//otherwise we have out of date data?
			//Not enirely true, there are plenty of data changes which can cause a version bump which don't affect array mapping

      //Let's just get all of them first, and warn if there are any which don't match the assemblyBuild

      //Update to count and list counts for each obj type, db_release and analysis_id?
      String [] exdbIDs = DBUtils.getColumnValues
          (
           efgCon, "select distinct edb.external_db_id from external_db edb " +
           " join xref x on edb.external_db_id=x.external_db_id join object_xref ox on x.xref_id=ox.xref_id " +
           " and ox.ensembl_object_type in ('Probe', 'ProbeFeature', 'ProbeSet')"
           );

      //need edb ids here as edbClause is used elsewhere
      //Update to count and list counts for each obj type, db_release and analysis_id?
      //select edb.db_release, ox.ensembl_object_type, count(*), edb.external_db_id, a.logic_name from external_db edb  join xref x on edb.external_db_id=x.external_db_id join object_xref ox on x.xref_id=ox.xref_id and ox.ensembl_object_type in ('Probe', 'ProbeFeature', 'ProbeSet') join analysis a on ox.analysis_id=a.analysis_id group by edb.db_release, ox.ensembl_object_type, a.logic_name;


      //Catch absent edbs

			if(exdbIDs.length == 0){
          ReportManager.problem(this, efgCon, "Could not identify external_db " + edbName + " with associated Probe, ProbeFeature or ProbeSet object_xrefs");
          result = false;
			}
			else{ //Handle mutliple edbs


				for (int i = 0; i < exdbIDs.length; i++) {
            inList.append(exdbIDs[i]);

            if(i != (exdbIDs.length -1)){
                inList.append(",");
            }
        }

				edbClause = "and x.external_db_id in ("	+ inList + ")";

        String dbReleases [] = DBUtils.getColumnValues
            (efgCon, "select db_release from external_db where external_db_id in(" + inList + ")");

        if(exdbIDs.length == 1){ // Test it matches the assembly/build

            if(! dbReleases[0].matches(".*" + assemblyBuild[1]) ){
                ReportManager.warning(this, efgCon,
                                      "Xrefs are associated with an external_db which does not match the current assembly/build:\t" +
                                      Arrays.toString(dbReleases));

                //This is actually unsafe now. As we can't identify whether a transcript set has been updated
                //simply using the schema_build. Will have to use meta keys

            }
        }
        else{ // >1 simply list all the db_releases
            //This maybe valid if we have ProbeFeature genomic and transcript xrefs on different releases
            ReportManager.warning(this, efgCon,
                                  "Found multiple external_db db_release versions with xrefs:\n\t" +
                                  Arrays.toString(dbReleases));
            //These may not also match the current build, but not test here
        }
      }


			//Set which objects we are looking for i.e. Probe or ProbeSets
			int[] xrefObjects = new int[2];
      xrefObjects[0] = DBUtils.getRowCount(efgCon, "select count(*) from array where vendor='AFFY'");
			xrefObjects[1] = DBUtils.getRowCount(efgCon, "select count(*) from array where vendor!='AFFY'");


			for (int i = 0; i <= 1; i++) {
				String xrefObj = (i == 0) ? "ProbeSet" : "Probe";
				String arrayVendor = (i == 0) ? "Affy" : "Non-Affy";

				if(xrefObjects[i] == 0){

					ReportManager.info(this, efgCon, "Has no " + arrayVendor + " arrays. " +
										  " " + xrefObj + " xref counts will be skipped");
					//result = false; This should be a wrning rathe than a problem as we are not returning false?
				}
				else{

					xrefQuery = "select t.seq_region_id, count(*) as count  from " + coreDBName + ".transcript t, " +
						" object_xref ox, xref x " +
						"where t.stable_id=x.dbprimary_acc " +
						"and ox.ensembl_object_type='" + xrefObj + "' and ox.xref_id=x.xref_id " +
						edbClause + " GROUP BY t.seq_region_id";


					//System.out.println(xrefQuery);
					//Restrict this to the core_coord_system_ids for the specific DB
					//other wise we may get odd counts where core_coord_system_ids have changed between releases on the same assembly

					ResultSet xrefCounts = efgCon
						.createStatement()
						.executeQuery(xrefQuery);



					//Capture empty set here
					boolean seenResults = false;

					while (xrefCounts.next()){
						seenResults = true;
						coreSrIDcounts.put(xrefCounts.getString(1), xrefCounts.getString(2));
					}
					rs.close();

					if(seenResults == false){
						result = false;

						//Change this to info and remove false?

						//This maybe that the new external_db exists, but the xrefs were loaded using an old version
						ReportManager.problem(this, efgCon, "Found " + arrayVendor + " " + xrefObj +
									" arrays but no associated transcript xrefs for external_db like " + assemblyBuild[1] +
											  "\nXrefs maybe present on a previous version");

						//redo this without assembly build clause, or just select and group by it in the first query and test in loop

					}
					else{

						// check every chr has >0 xrefs.
						for (Iterator<String> iter = coreSrIDcounts.keySet().iterator(); iter.hasNext();) {
							String coreSrID = (String) iter.next();
							String efgSrID  = (String) coreSrID2efg.get(coreSrID);
							String name = (String) srID2name.get(efgSrID);
						//System.out.println("core " + coreSrID + " efg " + efgSrID + " name " + name);

						//Skip nulls as these won't be chromosomes
						//But may have xrefs
						if ( name != null ){
							String label = name + " (seq_region_id=" + efgSrID + ")";
							long count = coreSrIDcounts.containsKey(coreSrID) ? Long.parseLong(coreSrIDcounts.get(coreSrID).toString()) : 0;

							//System.out.println(label + " " + count);

							if (count > 0) {
								ReportManager.correct(this, efgCon, "Chromosome " + label + " has " + coreSrIDcounts.get(coreSrID) +
											" associated " + xrefObj + " array xrefs.");
							} else {
								ReportManager.problem(this, efgCon, "Chromosome " + label + " has no associated " +
											xrefObj + " array xrefs.");
								result = false;
							}
						}
					}
					}
				}
			}
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
		}

		return result;

	} // run

	protected String getCoreDbName(DatabaseRegistryEntry dbre,
			String schemaBuild) {
		String coreDBName = dbre.getSpecies() + "_core_" + schemaBuild;
		return coreDBName;
	}

} // ArrayXrefs

