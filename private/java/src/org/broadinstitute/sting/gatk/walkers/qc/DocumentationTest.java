/*
 * Copyright (c) 2011, The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.gatk.walkers.qc;

import org.broadinstitute.sting.commandline.*;
import org.broadinstitute.sting.gatk.GenomeAnalysisEngine;
import org.broadinstitute.sting.gatk.arguments.StandardVariantContextInputArgumentCollection;
import org.broadinstitute.sting.gatk.contexts.AlignmentContext;
import org.broadinstitute.sting.gatk.contexts.ReferenceContext;
import org.broadinstitute.sting.gatk.io.StingSAMFileWriter;
import org.broadinstitute.sting.gatk.refdata.RefMetaDataTracker;
import org.broadinstitute.sting.gatk.walkers.RodWalker;
import org.broadinstitute.sting.utils.MathUtils;
import org.broadinstitute.sting.utils.MendelianViolation;
import org.broadinstitute.sting.utils.SampleUtils;
import org.broadinstitute.sting.utils.codecs.vcf.*;
import org.broadinstitute.sting.utils.exceptions.UserException;
import org.broadinstitute.sting.utils.text.XReadLines;
import org.broadinstitute.sting.utils.variantcontext.Allele;
import org.broadinstitute.sting.utils.variantcontext.Genotype;
import org.broadinstitute.sting.utils.variantcontext.VariantContext;
import org.broadinstitute.sting.utils.variantcontext.VariantContextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

/**
 * Summary test
 *
 * <p>Body test</p>
 */
public class DocumentationTest extends RodWalker<Integer, Integer> {
    @ArgumentCollection protected StandardVariantContextInputArgumentCollection variantCollection = new StandardVariantContextInputArgumentCollection();

    /**
     * detailed documentation about the argument goes here.
     */
    @Input(fullName="listofRodBinding", shortName = "disc", doc="Output variants that were not called in this Feature comparison track", required=false)
    private List<RodBinding<VariantContext>> listOfRodBinding = Collections.emptyList();

    @Input(fullName="optionalRodBinding", shortName = "conc", doc="Output variants that were also called in this Feature comparison track", required=false)
    private RodBinding<VariantContext> concordanceTrack;

    @Input(fullName="optionalRodBindingWithoutDefault", shortName = "optionalRodBindingWithoutDefault", doc="Output variants that were also called in this Feature comparison track", required=false)
    private RodBinding<VariantContext> noDefaultOptionalRodBinding;

    @Input(fullName="optionalRodBindingWithoutDefaultNull", shortName = "optionalRodBindingWithoutDefaultNull", doc="Output variants that were also called in this Feature comparison track", required=false)
    private RodBinding<VariantContext> noDefaultOptionalRodBindingNull = null;

    @Output(doc="VCFWriter",required=true)
    protected VCFWriter vcfWriter = null;

    @Argument(fullName="setString", shortName="sn", doc="Sample name to be included in the analysis. Can be specified multiple times.", required=false)
    public Set<String> sampleNames;

    @Argument(fullName="setStringInitialized", shortName="setStringInitialized", doc="Sample name to be included in the analysis. Can be specified multiple times.", required=false)
    public Set<String> setStringInitialized = new HashSet<String>();

    @Argument(shortName="optionalArgWithMissinglessDefault", doc="One or more criteria to use when selecting the data.  Evaluated *after* the specified samples are extracted and the INFO-field annotations are updated.", required=false)
    public ArrayList<String> SELECT_EXPRESSIONS = new ArrayList<String>();

    @Argument(fullName="booleanArg", shortName="env", doc="Don't include loci found to be non-variant after the subsetting procedure.", required=false)
    private boolean EXCLUDE_NON_VARIANTS = false;

    @Hidden
    @Argument(fullName="hiddenArg", shortName="keepAF", doc="Don't include loci found to be non-variant after the subsetting procedure.", required=false)
    private boolean KEEP_AF_SPECTRUM = false;

    public Integer map(RefMetaDataTracker tracker, ReferenceContext ref, AlignmentContext context) { return 0; }
    public Integer reduceInit() { return 0; }
    public Integer reduce(Integer value, Integer sum) { return value + sum; }
    public void onTraversalDone(Integer result) { }
}
