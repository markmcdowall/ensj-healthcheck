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

/*
 * Copyright (C) 2003 EBI, GRL
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.ensembl.healthcheck.testcase.generic;

import java.util.Map;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.DatabaseType;
import org.ensembl.healthcheck.Team;

/**
 * Compare the xrefs in the current database with those from the equivalent database on the secondary server.
 */

public class ComparePreviousVersionAssociatedXrefs extends ComparePreviousVersionBase {

	/**
	 * Create a new XrefTypes testcase.
	 */
	public ComparePreviousVersionAssociatedXrefs() {

		addToGroup("core_xrefs");
		addToGroup("post-compara-handover");
                addToGroup("post-projection");
		
		setDescription("Compare the associated xrefs in the current database with those from the equivalent database on the secondary server");
		setTeamResponsible(Team.CORE);
		setSecondTeamResponsible(Team.GENEBUILD);

	}

	/**
	 * This test Does not apply to sangervega dbs
	 */
	public void types() {
		removeAppliesToType(DatabaseType.SANGER_VEGA);
	}

	// ----------------------------------------------------------------------

	protected Map<String, Integer> getCounts(DatabaseRegistryEntry dbre) {
		String sql = "SELECT"
				     + "  external_db.db_name AS db_name,"
				     + "  COUNT(DISTINCT associated_xref.associated_group_id) AS count "
				     + "FROM"
				     + "  object_xref"
				     + "  JOIN xref ON (object_xref.xref_id=xref.xref_id)"
				     + "  JOIN external_db ON (xref.external_db_id=external_db.external_db_id)"
				     + "  JOIN associated_xref ON (object_xref.object_xref_id=associated_xref.object_xref_id) "
				     + "GROUP BY external_db.db_name;";
		//System.out.println(sql);
		return getCountsBySQL(dbre, sql);

	} // ------------------------------------------------------------------------

	protected String entityDescription() {

		return "xrefs of type";

	}

	// ------------------------------------------------------------------------

	protected double threshold() {

		return 0.78;

	}

        // ------------------------------------------------------------------------

        protected double minimum() {

                return 100;

        }

	// ----------------------------------------------------------------------

} // ComparePreviousVersionAssociatedXrefs

