package edu.uw.cs.lil.amr.parser.rules.amrspecials;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeshifting.ReversibleApplicationTypeShifting;
import edu.uw.cs.lil.amr.lambda.AMRServices;

public class SkolemizeRule extends ReversibleApplicationTypeShifting {
    public SkolemizeRule(String label,
			ICategoryServices<LogicalExpression> categoryServices,
			boolean matchSyntax) {
        super(label, (ComplexCategory<LogicalExpression>) Category.create(
        		new ComplexSyntax(AMRServices.AMR, Syntax.S, Slash.FORWARD),
				Lambda.read("(lambda $0:<e,t> (a:<id,<<e,t>,e>> na:id $0))")
		), categoryServices, false, false, matchSyntax);
	}
}
