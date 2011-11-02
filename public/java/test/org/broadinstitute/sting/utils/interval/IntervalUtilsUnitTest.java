package org.broadinstitute.sting.utils.interval;

import net.sf.picard.reference.ReferenceSequenceFile;
import net.sf.picard.util.IntervalUtil;
import net.sf.samtools.SAMFileHeader;
import org.broadinstitute.sting.BaseTest;
import org.broadinstitute.sting.gatk.datasources.reference.ReferenceDataSource;
import org.broadinstitute.sting.utils.GenomeLocSortedSet;
import org.testng.Assert;
import org.broadinstitute.sting.utils.exceptions.UserException;
import org.broadinstitute.sting.utils.GenomeLoc;
import org.broadinstitute.sting.utils.GenomeLocParser;
import org.broadinstitute.sting.utils.fasta.CachingIndexedFastaSequenceFile;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * test out the interval utility methods
 */
public class IntervalUtilsUnitTest extends BaseTest {
    // used to seed the genome loc parser with a sequence dictionary
    private SAMFileHeader hg18Header;
    private GenomeLocParser hg18GenomeLocParser;
    private List<GenomeLoc> hg18ReferenceLocs;
    private SAMFileHeader hg19Header;
    private GenomeLocParser hg19GenomeLocParser;
    private List<GenomeLoc> hg19ReferenceLocs;
    private List<GenomeLoc> hg19exomeIntervals;

    private List<GenomeLoc> getLocs(String... intervals) {
        return getLocs(Arrays.asList(intervals));
    }

    private List<GenomeLoc> getLocs(List<String> intervals) {
        if (intervals.size() == 0)
            return hg18ReferenceLocs;
        List<GenomeLoc> locs = new ArrayList<GenomeLoc>();
        for (String interval: intervals)
            locs.add(hg18GenomeLocParser.parseGenomeLoc(interval));
        return locs;
    }

    @BeforeClass
    public void init() {
        File hg18Ref = new File(BaseTest.hg18Reference);
        try {
            ReferenceDataSource referenceDataSource = new ReferenceDataSource(hg18Ref);
            hg18Header = new SAMFileHeader();
            hg18Header.setSequenceDictionary(referenceDataSource.getReference().getSequenceDictionary());
            ReferenceSequenceFile seq = new CachingIndexedFastaSequenceFile(hg18Ref);
            hg18GenomeLocParser = new GenomeLocParser(seq);
            hg18ReferenceLocs = Collections.unmodifiableList(GenomeLocSortedSet.createSetFromSequenceDictionary(referenceDataSource.getReference().getSequenceDictionary()).toList()) ;
        }
        catch(FileNotFoundException ex) {
            throw new UserException.CouldNotReadInputFile(hg18Ref,ex);
        }

        File hg19Ref = new File(BaseTest.hg19Reference);
        try {
            ReferenceDataSource referenceDataSource = new ReferenceDataSource(hg19Ref);
            hg19Header = new SAMFileHeader();
            hg19Header.setSequenceDictionary(referenceDataSource.getReference().getSequenceDictionary());
            ReferenceSequenceFile seq = new CachingIndexedFastaSequenceFile(hg19Ref);
            hg19GenomeLocParser = new GenomeLocParser(seq);
            hg19ReferenceLocs = Collections.unmodifiableList(GenomeLocSortedSet.createSetFromSequenceDictionary(referenceDataSource.getReference().getSequenceDictionary()).toList()) ;

            hg19exomeIntervals = Collections.unmodifiableList(IntervalUtils.parseIntervalArguments(hg19GenomeLocParser, Arrays.asList(hg19Intervals)));
        }
        catch(FileNotFoundException ex) {
            throw new UserException.CouldNotReadInputFile(hg19Ref,ex);
        }
    }

    // -------------------------------------------------------------------------------------
    //
    // tests to ensure the quality of the interval cuts of the interval cutting functions
    //
    // -------------------------------------------------------------------------------------

    private class IntervalSlicingTest extends TestDataProvider {
        public int parts;
        public double maxAllowableVariance;

        private IntervalSlicingTest(final int parts, final double maxAllowableVariance) {
            super(IntervalSlicingTest.class);
            this.parts = parts;
            this.maxAllowableVariance = maxAllowableVariance;
        }

        public String toString() {
            return String.format("IntervalSlicingTest parts=%d maxVar=%.2f", parts, maxAllowableVariance);
        }
    }

    @DataProvider(name = "intervalslicingdata")
    public Object[][] createTrees() {
        new IntervalSlicingTest(1, 0);
        new IntervalSlicingTest(2, 1);
        new IntervalSlicingTest(5, 1);
        new IntervalSlicingTest(10, 1);
        new IntervalSlicingTest(67, 1);
        new IntervalSlicingTest(100, 1);
        new IntervalSlicingTest(500, 1);
        new IntervalSlicingTest(1000, 1);
        return IntervalSlicingTest.getTests(IntervalSlicingTest.class);
    }

    @Test(enabled = true, dataProvider = "intervalslicingdata")
    public void testFixedScatterIntervalsAlgorithm(IntervalSlicingTest test) {
        List<List<GenomeLoc>> splits = IntervalUtils.splitFixedIntervals(hg19exomeIntervals, test.parts);

        long totalSize = IntervalUtils.intervalSize(hg19exomeIntervals);
        long idealSplitSize = totalSize / test.parts;

        long sumOfSplitSizes = 0;
        int counter = 0;
        for ( final List<GenomeLoc> split : splits ) {
            long splitSize = IntervalUtils.intervalSize(split);
            double sigma = (splitSize - idealSplitSize) / (1.0 * idealSplitSize);
            //logger.warn(String.format("Split %d size %d ideal %d sigma %.2f", counter, splitSize, idealSplitSize, sigma));
            counter++;
            sumOfSplitSizes += splitSize;
            Assert.assertTrue(Math.abs(sigma) <= test.maxAllowableVariance, String.format("Interval %d (size %d ideal %d) has a variance %.2f outside of the tolerated range %.2f", counter, splitSize, idealSplitSize, sigma, test.maxAllowableVariance));
        }

        Assert.assertEquals(totalSize, sumOfSplitSizes, "Split intervals don't contain the exact number of bases in the origianl intervals");
    }

    // -------------------------------------------------------------------------------------
    //
    // tests to ensure the quality of the interval cuts of the interval cutting functions
    //
    // -------------------------------------------------------------------------------------

    private class IntervalRepartitionTest extends TestDataProvider {
        final List<GenomeLoc> originalIntervals;
        final public int parts;

        private IntervalRepartitionTest(final String name, List<GenomeLoc> originalIntervals, final int parts) {
            super(IntervalRepartitionTest.class, name);
            this.parts = parts;
            this.originalIntervals = originalIntervals;
        }

        public String toString() {
            return String.format("%s parts=%d", super.toString(), parts);
        }
    }

    @DataProvider(name = "IntervalRepartitionTest")
    public Object[][] createIntervalRepartitionTest() {
        for ( int parts : Arrays.asList(1, 10, 100, 1000, 10000) ) {
            new IntervalRepartitionTest("hg19RefLocs", hg19ReferenceLocs, parts);
            new IntervalRepartitionTest("hg19ExomeLocs", hg19exomeIntervals, parts);
        }

        return IntervalSlicingTest.getTests(IntervalRepartitionTest.class);
    }

    @Test(enabled = true, dataProvider = "IntervalRepartitionTest")
    public void testIntervalRepartition(IntervalRepartitionTest test) {
        List<List<GenomeLoc>> splitByLocus = IntervalUtils.splitLocusIntervals(test.originalIntervals, test.parts);
        Assert.assertEquals(test.parts, splitByLocus.size(), "SplitLocusIntervals failed to generate correct number of intervals");
        List<GenomeLoc> flat = IntervalUtils.flattenSplitIntervals(splitByLocus);

        // test sizes
        final long originalSize = IntervalUtils.intervalSize(test.originalIntervals);
        final long splitSize = IntervalUtils.intervalSize(flat);
        Assert.assertEquals(originalSize, splitSize, "SplitLocusIntervals locs cover an incorrect number of bases");

        // test that every base in original is covered once by a base in split by locus intervals
        // todo implement test (complex)
    }

    //
    // Misc. tests
    //

    @Test(expectedExceptions=UserException.class)
    public void testMergeListsBySetOperatorNoOverlap() {
        // a couple of lists we'll use for the testing
        List<GenomeLoc> listEveryTwoFromOne = new ArrayList<GenomeLoc>();
        List<GenomeLoc> listEveryTwoFromTwo = new ArrayList<GenomeLoc>();

        // create the two lists we'll use
        for (int x = 1; x < 101; x++) {
            if (x % 2 == 0)
                listEveryTwoFromTwo.add(hg18GenomeLocParser.createGenomeLoc("chr1",x,x));
            else
                listEveryTwoFromOne.add(hg18GenomeLocParser.createGenomeLoc("chr1",x,x));
        }

        List<GenomeLoc> ret = IntervalUtils.mergeListsBySetOperator(listEveryTwoFromTwo, listEveryTwoFromOne, IntervalSetRule.UNION);
        Assert.assertEquals(ret.size(), 100);
        ret = IntervalUtils.mergeListsBySetOperator(listEveryTwoFromTwo, listEveryTwoFromOne, IntervalSetRule.INTERSECTION);
        Assert.assertEquals(ret.size(), 0);
    }

    @Test
    public void testMergeListsBySetOperatorAllOverlap() {
        // a couple of lists we'll use for the testing
        List<GenomeLoc> allSites = new ArrayList<GenomeLoc>();
        List<GenomeLoc> listEveryTwoFromTwo = new ArrayList<GenomeLoc>();

        // create the two lists we'll use
        for (int x = 1; x < 101; x++) {
            if (x % 2 == 0)
                listEveryTwoFromTwo.add(hg18GenomeLocParser.createGenomeLoc("chr1",x,x));
            allSites.add(hg18GenomeLocParser.createGenomeLoc("chr1",x,x));
        }

        List<GenomeLoc> ret = IntervalUtils.mergeListsBySetOperator(listEveryTwoFromTwo, allSites, IntervalSetRule.UNION);
        Assert.assertEquals(ret.size(), 150);
        ret = IntervalUtils.mergeListsBySetOperator(listEveryTwoFromTwo, allSites, IntervalSetRule.INTERSECTION);
        Assert.assertEquals(ret.size(), 50);
    }

    @Test
    public void testMergeListsBySetOperator() {
        // a couple of lists we'll use for the testing
        List<GenomeLoc> allSites = new ArrayList<GenomeLoc>();
        List<GenomeLoc> listEveryTwoFromTwo = new ArrayList<GenomeLoc>();

        // create the two lists we'll use
        for (int x = 1; x < 101; x++) {
            if (x % 5 == 0) {
                listEveryTwoFromTwo.add(hg18GenomeLocParser.createGenomeLoc("chr1",x,x));
                allSites.add(hg18GenomeLocParser.createGenomeLoc("chr1",x,x));
            }
        }

        List<GenomeLoc> ret = IntervalUtils.mergeListsBySetOperator(listEveryTwoFromTwo, allSites, IntervalSetRule.UNION);
        Assert.assertEquals(ret.size(), 40);
        ret = IntervalUtils.mergeListsBySetOperator(listEveryTwoFromTwo, allSites, IntervalSetRule.INTERSECTION);
        Assert.assertEquals(ret.size(), 20);
    }

    @Test
    public void testOverlappingIntervalsFromSameSourceWithIntersection() {
        // a couple of lists we'll use for the testing
        List<GenomeLoc> source1 = new ArrayList<GenomeLoc>();
        List<GenomeLoc> source2 = new ArrayList<GenomeLoc>();

        source1.add(hg18GenomeLocParser.createGenomeLoc("chr1", 10, 20));
        source1.add(hg18GenomeLocParser.createGenomeLoc("chr1", 15, 25));

        source2.add(hg18GenomeLocParser.createGenomeLoc("chr1", 16, 18));
        source2.add(hg18GenomeLocParser.createGenomeLoc("chr1", 22, 24));

        List<GenomeLoc> ret = IntervalUtils.mergeListsBySetOperator(source1, source2, IntervalSetRule.INTERSECTION);
        Assert.assertEquals(ret.size(), 2);
    }

    @Test
    public void testGetContigLengths() {
        Map<String, Long> lengths = IntervalUtils.getContigSizes(new File(BaseTest.hg18Reference));
        Assert.assertEquals((long)lengths.get("chr1"), 247249719);
        Assert.assertEquals((long)lengths.get("chr2"), 242951149);
        Assert.assertEquals((long)lengths.get("chr3"), 199501827);
        Assert.assertEquals((long)lengths.get("chr20"), 62435964);
        Assert.assertEquals((long)lengths.get("chrX"), 154913754);
    }

    @Test
    public void testParseIntervalArguments() {
        Assert.assertEquals(getLocs().size(), 45);
        Assert.assertEquals(getLocs("chr1", "chr2", "chr3").size(), 3);
        Assert.assertEquals(getLocs("chr1:1-2", "chr1:4-5", "chr2:1-1", "chr3:2-2").size(), 4);
    }

    @Test
    public void testIsIntervalFile() {
        Assert.assertTrue(IntervalUtils.isIntervalFile(BaseTest.validationDataLocation + "empty_intervals.list"));
        Assert.assertTrue(IntervalUtils.isIntervalFile(BaseTest.validationDataLocation + "empty_intervals.list", true));

        List<String> extensions = Arrays.asList("bed", "interval_list", "intervals", "list", "picard");
        for (String extension: extensions) {
            Assert.assertTrue(IntervalUtils.isIntervalFile("test_intervals." + extension, false), "Tested interval file extension: " + extension);
        }
    }

    @Test(expectedExceptions = UserException.CouldNotReadInputFile.class)
    public void testMissingIntervalFile() {
        IntervalUtils.isIntervalFile(BaseTest.validationDataLocation + "no_such_intervals.list");
    }

    @Test
    public void testFixedScatterIntervalsBasic() {
        GenomeLoc chr1 = hg18GenomeLocParser.parseGenomeLoc("chr1");
        GenomeLoc chr2 = hg18GenomeLocParser.parseGenomeLoc("chr2");
        GenomeLoc chr3 = hg18GenomeLocParser.parseGenomeLoc("chr3");

        List<File> files = testFiles("basic.", 3, ".intervals");

        List<GenomeLoc> locs = getLocs("chr1", "chr2", "chr3");
        List<List<GenomeLoc>> splits = IntervalUtils.splitFixedIntervals(locs, files.size());
        IntervalUtils.scatterFixedIntervals(hg18Header, splits, files);

        List<GenomeLoc> locs1 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(0).toString()));
        List<GenomeLoc> locs2 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(1).toString()));
        List<GenomeLoc> locs3 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(2).toString()));

        Assert.assertEquals(locs1.size(), 1);
        Assert.assertEquals(locs2.size(), 1);
        Assert.assertEquals(locs3.size(), 1);

        Assert.assertEquals(locs1.get(0), chr1);
        Assert.assertEquals(locs2.get(0), chr2);
        Assert.assertEquals(locs3.get(0), chr3);
    }

    @Test
    public void testScatterFixedIntervalsLessFiles() {
        GenomeLoc chr1 = hg18GenomeLocParser.parseGenomeLoc("chr1");
        GenomeLoc chr2 = hg18GenomeLocParser.parseGenomeLoc("chr2");
        GenomeLoc chr3 = hg18GenomeLocParser.parseGenomeLoc("chr3");
        GenomeLoc chr4 = hg18GenomeLocParser.parseGenomeLoc("chr4");

        List<File> files = testFiles("less.", 3, ".intervals");

        List<GenomeLoc> locs = getLocs("chr1", "chr2", "chr3", "chr4");
        List<List<GenomeLoc>> splits = IntervalUtils.splitFixedIntervals(locs, files.size());
        IntervalUtils.scatterFixedIntervals(hg18Header, splits, files);

        List<GenomeLoc> locs1 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(0).toString()));
        List<GenomeLoc> locs2 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(1).toString()));
        List<GenomeLoc> locs3 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(2).toString()));

        Assert.assertEquals(locs1.size(), 1);
        Assert.assertEquals(locs2.size(), 1);
        Assert.assertEquals(locs3.size(), 2);

        Assert.assertEquals(locs1.get(0), chr1);
        Assert.assertEquals(locs2.get(0), chr2);
        Assert.assertEquals(locs3.get(0), chr3);
        Assert.assertEquals(locs3.get(1), chr4);
    }

    @Test(expectedExceptions=UserException.BadArgumentValue.class)
    public void testSplitFixedIntervalsMoreFiles() {
        List<File> files = testFiles("more.", 3, ".intervals");
        List<GenomeLoc> locs = getLocs("chr1", "chr2");
        IntervalUtils.splitFixedIntervals(locs, files.size());
    }

    @Test(expectedExceptions=UserException.BadArgumentValue.class)
    public void testScatterFixedIntervalsMoreFiles() {
        List<File> files = testFiles("more.", 3, ".intervals");
        List<GenomeLoc> locs = getLocs("chr1", "chr2");
        List<List<GenomeLoc>> splits = IntervalUtils.splitFixedIntervals(locs, locs.size()); // locs.size() instead of files.size()
        IntervalUtils.scatterFixedIntervals(hg18Header, splits, files);
    }
    @Test
    public void testScatterFixedIntervalsStart() {
        List<String> intervals = Arrays.asList("chr1:1-2", "chr1:4-5", "chr2:1-1", "chr3:2-2");
        GenomeLoc chr1a = hg18GenomeLocParser.parseGenomeLoc("chr1:1-2");
        GenomeLoc chr1b = hg18GenomeLocParser.parseGenomeLoc("chr1:4-5");
        GenomeLoc chr2 = hg18GenomeLocParser.parseGenomeLoc("chr2:1-1");
        GenomeLoc chr3 = hg18GenomeLocParser.parseGenomeLoc("chr3:2-2");

        List<File> files = testFiles("split.", 3, ".intervals");

        List<GenomeLoc> locs = getLocs(intervals);
        List<List<GenomeLoc>> splits = IntervalUtils.splitFixedIntervals(locs, files.size());
        IntervalUtils.scatterFixedIntervals(hg18Header, splits, files);

        List<GenomeLoc> locs1 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(0).toString()));
        List<GenomeLoc> locs2 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(1).toString()));
        List<GenomeLoc> locs3 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(2).toString()));

        Assert.assertEquals(locs1.size(), 1);
        Assert.assertEquals(locs2.size(), 1);
        Assert.assertEquals(locs3.size(), 2);

        Assert.assertEquals(locs1.get(0), chr1a);
        Assert.assertEquals(locs2.get(0), chr1b);
        Assert.assertEquals(locs3.get(0), chr2);
        Assert.assertEquals(locs3.get(1), chr3);
    }

    @Test
    public void testScatterFixedIntervalsMiddle() {
        List<String> intervals = Arrays.asList("chr1:1-1", "chr2:1-2", "chr2:4-5", "chr3:2-2");
        GenomeLoc chr1 = hg18GenomeLocParser.parseGenomeLoc("chr1:1-1");
        GenomeLoc chr2a = hg18GenomeLocParser.parseGenomeLoc("chr2:1-2");
        GenomeLoc chr2b = hg18GenomeLocParser.parseGenomeLoc("chr2:4-5");
        GenomeLoc chr3 = hg18GenomeLocParser.parseGenomeLoc("chr3:2-2");

        List<File> files = testFiles("split.", 3, ".intervals");

        List<GenomeLoc> locs = getLocs(intervals);
        List<List<GenomeLoc>> splits = IntervalUtils.splitFixedIntervals(locs, files.size());
        IntervalUtils.scatterFixedIntervals(hg18Header, splits, files);

        List<GenomeLoc> locs1 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(0).toString()));
        List<GenomeLoc> locs2 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(1).toString()));
        List<GenomeLoc> locs3 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(2).toString()));

        Assert.assertEquals(locs1.size(), 1);
        Assert.assertEquals(locs2.size(), 1);
        Assert.assertEquals(locs3.size(), 2);

        Assert.assertEquals(locs1.get(0), chr1);
        Assert.assertEquals(locs2.get(0), chr2a);
        Assert.assertEquals(locs3.get(0), chr2b);
        Assert.assertEquals(locs3.get(1), chr3);
    }

    @Test
    public void testScatterFixedIntervalsEnd() {
        List<String> intervals = Arrays.asList("chr1:1-1", "chr2:2-2", "chr3:1-2", "chr3:4-5");
        GenomeLoc chr1 = hg18GenomeLocParser.parseGenomeLoc("chr1:1-1");
        GenomeLoc chr2 = hg18GenomeLocParser.parseGenomeLoc("chr2:2-2");
        GenomeLoc chr3a = hg18GenomeLocParser.parseGenomeLoc("chr3:1-2");
        GenomeLoc chr3b = hg18GenomeLocParser.parseGenomeLoc("chr3:4-5");

        List<File> files = testFiles("split.", 3, ".intervals");

        List<GenomeLoc> locs = getLocs(intervals);
        List<List<GenomeLoc>> splits = IntervalUtils.splitFixedIntervals(locs, files.size());
        IntervalUtils.scatterFixedIntervals(hg18Header, splits, files);

        List<GenomeLoc> locs1 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(0).toString()));
        List<GenomeLoc> locs2 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(1).toString()));
        List<GenomeLoc> locs3 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(2).toString()));

        Assert.assertEquals(locs1.size(), 2);
        Assert.assertEquals(locs2.size(), 1);
        Assert.assertEquals(locs3.size(), 1);

        Assert.assertEquals(locs1.get(0), chr1);
        Assert.assertEquals(locs1.get(1), chr2);
        Assert.assertEquals(locs2.get(0), chr3a);
        Assert.assertEquals(locs3.get(0), chr3b);
    }

    @Test
    public void testScatterFixedIntervalsFile() {
        List<File> files = testFiles("sg.", 20, ".intervals");
        List<GenomeLoc> locs = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(BaseTest.GATKDataLocation + "whole_exome_agilent_designed_120.targets.hg18.chr20.interval_list"));
        List<List<GenomeLoc>> splits = IntervalUtils.splitFixedIntervals(locs, files.size());

        int[] counts = {
                125, 138, 287, 291, 312, 105, 155, 324,
                295, 298, 141, 121, 285, 302, 282, 88,
                116, 274, 282, 248
//                5169, 5573, 10017, 10567, 10551,
//                5087, 4908, 10120, 10435, 10399,
//                5391, 4735, 10621, 10352, 10654,
//                5227, 5256, 10151, 9649, 9825
        };

        //String splitCounts = "";
        for (int i = 0; i < splits.size(); i++) {
            int splitCount = splits.get(i).size();
            Assert.assertEquals(splitCount, counts[i], "Num intervals in split " + i);
        }
        //System.out.println(splitCounts.substring(2));

        IntervalUtils.scatterFixedIntervals(hg18Header, splits, files);

        int locIndex = 0;
        for (int i = 0; i < files.size(); i++) {
            String file = files.get(i).toString();
            List<GenomeLoc> parsedLocs = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(file));
            Assert.assertEquals(parsedLocs.size(), counts[i], "Intervals in " + file);
            for (GenomeLoc parsedLoc: parsedLocs)
                Assert.assertEquals(parsedLoc, locs.get(locIndex), String.format("Genome loc %d from file %d", locIndex++, i));
        }
        Assert.assertEquals(locIndex, locs.size(), "Total number of GenomeLocs");
    }

    @Test
    public void testScatterFixedIntervalsMax() {
        List<File> files = testFiles("sg.", 85, ".intervals");
        List<List<GenomeLoc>> splits = IntervalUtils.splitFixedIntervals(hg19ReferenceLocs, files.size());
        IntervalUtils.scatterFixedIntervals(hg19Header, splits, files);

        for (int i = 0; i < files.size(); i++) {
            String file = files.get(i).toString();
            List<GenomeLoc> parsedLocs = IntervalUtils.parseIntervalArguments(hg19GenomeLocParser, Arrays.asList(file));
            Assert.assertEquals(parsedLocs.size(), 1, "parsedLocs[" + i + "].size()");
            Assert.assertEquals(parsedLocs.get(0), hg19ReferenceLocs.get(i), "parsedLocs[" + i + "].get()");
        }
    }

    @Test
    public void testScatterContigIntervalsOrder() {
        List<String> intervals = Arrays.asList("chr2:1-1", "chr1:1-1", "chr3:2-2");
        GenomeLoc chr1 = hg18GenomeLocParser.parseGenomeLoc("chr1:1-1");
        GenomeLoc chr2 = hg18GenomeLocParser.parseGenomeLoc("chr2:1-1");
        GenomeLoc chr3 = hg18GenomeLocParser.parseGenomeLoc("chr3:2-2");

        List<File> files = testFiles("split.", 3, ".intervals");

        IntervalUtils.scatterContigIntervals(hg18Header, getLocs(intervals), files);

        List<GenomeLoc> locs1 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(0).toString()));
        List<GenomeLoc> locs2 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(1).toString()));
        List<GenomeLoc> locs3 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(2).toString()));

        Assert.assertEquals(locs1.size(), 1);
        Assert.assertEquals(locs2.size(), 1);
        Assert.assertEquals(locs3.size(), 1);

        Assert.assertEquals(locs1.get(0), chr2);
        Assert.assertEquals(locs2.get(0), chr1);
        Assert.assertEquals(locs3.get(0), chr3);
    }

    @Test
    public void testScatterContigIntervalsBasic() {
        GenomeLoc chr1 = hg18GenomeLocParser.parseGenomeLoc("chr1");
        GenomeLoc chr2 = hg18GenomeLocParser.parseGenomeLoc("chr2");
        GenomeLoc chr3 = hg18GenomeLocParser.parseGenomeLoc("chr3");

        List<File> files = testFiles("contig_basic.", 3, ".intervals");

        IntervalUtils.scatterContigIntervals(hg18Header, getLocs("chr1", "chr2", "chr3"), files);

        List<GenomeLoc> locs1 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(0).toString()));
        List<GenomeLoc> locs2 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(1).toString()));
        List<GenomeLoc> locs3 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(2).toString()));

        Assert.assertEquals(locs1.size(), 1);
        Assert.assertEquals(locs2.size(), 1);
        Assert.assertEquals(locs3.size(), 1);

        Assert.assertEquals(locs1.get(0), chr1);
        Assert.assertEquals(locs2.get(0), chr2);
        Assert.assertEquals(locs3.get(0), chr3);
    }

    @Test
    public void testScatterContigIntervalsLessFiles() {
        GenomeLoc chr1 = hg18GenomeLocParser.parseGenomeLoc("chr1");
        GenomeLoc chr2 = hg18GenomeLocParser.parseGenomeLoc("chr2");
        GenomeLoc chr3 = hg18GenomeLocParser.parseGenomeLoc("chr3");
        GenomeLoc chr4 = hg18GenomeLocParser.parseGenomeLoc("chr4");

        List<File> files = testFiles("contig_less.", 3, ".intervals");

        IntervalUtils.scatterContigIntervals(hg18Header, getLocs("chr1", "chr2", "chr3", "chr4"), files);

        List<GenomeLoc> locs1 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(0).toString()));
        List<GenomeLoc> locs2 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(1).toString()));
        List<GenomeLoc> locs3 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(2).toString()));

        Assert.assertEquals(locs1.size(), 1);
        Assert.assertEquals(locs2.size(), 1);
        Assert.assertEquals(locs3.size(), 2);

        Assert.assertEquals(locs1.get(0), chr1);
        Assert.assertEquals(locs2.get(0), chr2);
        Assert.assertEquals(locs3.get(0), chr3);
        Assert.assertEquals(locs3.get(1), chr4);
    }

    @Test(expectedExceptions=UserException.BadArgumentValue.class)
    public void testScatterContigIntervalsMoreFiles() {
        List<File> files = testFiles("contig_more.", 3, ".intervals");
        IntervalUtils.scatterContigIntervals(hg18Header, getLocs("chr1", "chr2"), files);
    }

    @Test
    public void testScatterContigIntervalsStart() {
        List<String> intervals = Arrays.asList("chr1:1-2", "chr1:4-5", "chr2:1-1", "chr3:2-2");
        GenomeLoc chr1a = hg18GenomeLocParser.parseGenomeLoc("chr1:1-2");
        GenomeLoc chr1b = hg18GenomeLocParser.parseGenomeLoc("chr1:4-5");
        GenomeLoc chr2 = hg18GenomeLocParser.parseGenomeLoc("chr2:1-1");
        GenomeLoc chr3 = hg18GenomeLocParser.parseGenomeLoc("chr3:2-2");

        List<File> files = testFiles("contig_split_start.", 3, ".intervals");

        IntervalUtils.scatterContigIntervals(hg18Header, getLocs(intervals), files);

        List<GenomeLoc> locs1 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(0).toString()));
        List<GenomeLoc> locs2 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(1).toString()));
        List<GenomeLoc> locs3 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(2).toString()));

        Assert.assertEquals(locs1.size(), 2);
        Assert.assertEquals(locs2.size(), 1);
        Assert.assertEquals(locs3.size(), 1);

        Assert.assertEquals(locs1.get(0), chr1a);
        Assert.assertEquals(locs1.get(1), chr1b);
        Assert.assertEquals(locs2.get(0), chr2);
        Assert.assertEquals(locs3.get(0), chr3);
    }

    @Test
    public void testScatterContigIntervalsMiddle() {
        List<String> intervals = Arrays.asList("chr1:1-1", "chr2:1-2", "chr2:4-5", "chr3:2-2");
        GenomeLoc chr1 = hg18GenomeLocParser.parseGenomeLoc("chr1:1-1");
        GenomeLoc chr2a = hg18GenomeLocParser.parseGenomeLoc("chr2:1-2");
        GenomeLoc chr2b = hg18GenomeLocParser.parseGenomeLoc("chr2:4-5");
        GenomeLoc chr3 = hg18GenomeLocParser.parseGenomeLoc("chr3:2-2");

        List<File> files = testFiles("contig_split_middle.", 3, ".intervals");

        IntervalUtils.scatterContigIntervals(hg18Header, getLocs(intervals), files);

        List<GenomeLoc> locs1 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(0).toString()));
        List<GenomeLoc> locs2 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(1).toString()));
        List<GenomeLoc> locs3 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(2).toString()));

        Assert.assertEquals(locs1.size(), 1);
        Assert.assertEquals(locs2.size(), 2);
        Assert.assertEquals(locs3.size(), 1);

        Assert.assertEquals(locs1.get(0), chr1);
        Assert.assertEquals(locs2.get(0), chr2a);
        Assert.assertEquals(locs2.get(1), chr2b);
        Assert.assertEquals(locs3.get(0), chr3);
    }

    @Test
    public void testScatterContigIntervalsEnd() {
        List<String> intervals = Arrays.asList("chr1:1-1", "chr2:2-2", "chr3:1-2", "chr3:4-5");
        GenomeLoc chr1 = hg18GenomeLocParser.parseGenomeLoc("chr1:1-1");
        GenomeLoc chr2 = hg18GenomeLocParser.parseGenomeLoc("chr2:2-2");
        GenomeLoc chr3a = hg18GenomeLocParser.parseGenomeLoc("chr3:1-2");
        GenomeLoc chr3b = hg18GenomeLocParser.parseGenomeLoc("chr3:4-5");

        List<File> files = testFiles("contig_split_end.", 3 ,".intervals");

        IntervalUtils.scatterContigIntervals(hg18Header, getLocs(intervals), files);

        List<GenomeLoc> locs1 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(0).toString()));
        List<GenomeLoc> locs2 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(1).toString()));
        List<GenomeLoc> locs3 = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Arrays.asList(files.get(2).toString()));

        Assert.assertEquals(locs1.size(), 1);
        Assert.assertEquals(locs2.size(), 1);
        Assert.assertEquals(locs3.size(), 2);

        Assert.assertEquals(locs1.get(0), chr1);
        Assert.assertEquals(locs2.get(0), chr2);
        Assert.assertEquals(locs3.get(0), chr3a);
        Assert.assertEquals(locs3.get(1), chr3b);
    }

    @Test
    public void testScatterContigIntervalsMax() {
        List<File> files = testFiles("sg.", 85, ".intervals");
        IntervalUtils.scatterContigIntervals(hg19Header, hg19ReferenceLocs, files);

        for (int i = 0; i < files.size(); i++) {
            String file = files.get(i).toString();
            List<GenomeLoc> parsedLocs = IntervalUtils.parseIntervalArguments(hg19GenomeLocParser, Arrays.asList(file));
            Assert.assertEquals(parsedLocs.size(), 1, "parsedLocs[" + i + "].size()");
            Assert.assertEquals(parsedLocs.get(0), hg19ReferenceLocs.get(i), "parsedLocs[" + i + "].get()");
        }
    }

    private List<File> testFiles(String prefix, int count, String suffix) {
        ArrayList<File> files = new ArrayList<File>();
        for (int i = 1; i <= count; i++) {
            files.add(createTempFile(prefix + i, suffix));
        }
        return files;
    }

    @DataProvider(name="unmergedIntervals")
    public Object[][] getUnmergedIntervals() {
        return new Object[][] {
                new Object[] {"small_unmerged_picard_intervals.list"},
                new Object[] {"small_unmerged_gatk_intervals.list"}
        };
    }

    @Test(dataProvider="unmergedIntervals")
    public void testUnmergedIntervals(String unmergedIntervals) {
        List<GenomeLoc> locs = IntervalUtils.parseIntervalArguments(hg18GenomeLocParser, Collections.singletonList(validationDataLocation + unmergedIntervals));
        Assert.assertEquals(locs.size(), 2);

        List<GenomeLoc> merged = IntervalUtils.mergeIntervalLocations(locs, IntervalMergingRule.ALL);
        Assert.assertEquals(merged.size(), 1);
    }
}
