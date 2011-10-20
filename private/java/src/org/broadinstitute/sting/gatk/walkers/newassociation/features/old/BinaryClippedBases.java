package org.broadinstitute.sting.gatk.walkers.newassociation.features.old;

import net.sf.samtools.SAMRecord;
import org.broadinstitute.sting.gatk.walkers.newassociation.RFAArgumentCollection;

/**
 * Created by IntelliJ IDEA.
 * User: Ghost
 * Date: 5/19/11
 * Time: 12:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class BinaryClippedBases extends BinaryFeatureAggregator {

    private short baseLim;
    private final byte baseQualLim = 20;

    public boolean extractFeature(SAMRecord read) {
        int firstClippedToAliStart = read.getUnclippedStart()-read.getAlignmentStart();
        int lastUnclippedToReadEnd = read.getUnclippedEnd()-read.getAlignmentEnd();

        byte[] quals = read.getBaseQualities();
        int nClipped = 0;
        for ( int offset = 0; offset < firstClippedToAliStart; offset++ ) {
            if ( quals[offset] >= baseQualLim ) {
                nClipped++;
            }
        }

        for ( int offset = quals.length - lastUnclippedToReadEnd; offset < quals.length ; offset++ ) {
            if ( quals[offset] >= baseQualLim ) {
                nClipped ++;
            }
        }

        return nClipped >= baseLim;
    }

    public boolean featureDefined(SAMRecord rec) { return ! rec.getReadPairedFlag() || Math.abs(rec.getInferredInsertSize()) > 100; } // unpaired or no adaptor sequence

    public BinaryClippedBases(RFAArgumentCollection col) {
        super(col);
        baseLim = col.clippedBases;
    }
}
