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


package org.ensembl.healthcheck.testcase.compara;

import java.sql.Connection;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.Team;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;
import org.ensembl.healthcheck.util.DBUtils;

/**
 * An EnsEMBL Healthcheck test case that looks for broken foreign-key
 * relationships.
 */

public class ForeignKeyGenomicAlignId extends SingleDatabaseTestCase {

    /**
     * Create an ForeignKeyGenomicAlignBlockId that applies to a specific set of databases.
     */
    public ForeignKeyGenomicAlignId() {

        addToGroup("compara_genomic");
        setDescription("Check for broken foreign-key relationships in ensembl_compara databases.");
        setTeamResponsible(Team.COMPARA);

    }

    /**
     * Run the test.
     * 
     * @param dbre
     *          The database to use.
     * @return true if the test passed.
     *  
     */
    public boolean run(DatabaseRegistryEntry dbre) {

        boolean result = true;

       Connection con = dbre.getConnection();

//        if (tableHasRows(con, "genomic_align_group") &&
//            tableHasRows(con, "genomic_align")) {
       if (tableHasRows(con, "genomic_align")) {

            //            result &= checkForOrphans(con, "genomic_align", "genomic_align_id", "genomic_align_group", "genomic_align_id");
            //            result &= checkForOrphans(con, "genomic_align_group", "genomic_align_id", "genomic_align", "genomic_align_id");

            // Check that all method_link_species_set_ids match the genomic_align_block table
            int mismatches = DBUtils.getRowCount(con, "SELECT COUNT(*) FROM genomic_align ga LEFT JOIN genomic_align_block gab" +
                " USING (genomic_align_block_id) WHERE ga.method_link_species_set_id != gab.method_link_species_set_id");
            if (mismatches > 0) {
                ReportManager.problem(this, con, mismatches + " entries in genomic_align table have a wrong" +
                    " method_link_species_set_id according to genomic_align_block table");
                result = false;
            } else {
                ReportManager.correct(this, con, "All entries in genomic_align table have a correct" +
                    " method_link_species_set_id according to genomic_align_block table");
            }
 
       } else {
               ReportManager.correct(this, con, "NO ENTRIES in genomic_align table, so nothing to test IGNORED");
       }

        return result;

    }

} // ForeignKeyGenomicAlignId
