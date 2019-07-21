package edu.uw.cs.lil.amr.parser.factorgraph.assignmentgen;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicalConstant;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.SkolemServices;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetAllSimpleLogicalConstants;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetSkolemIds;
import edu.uw.cs.lil.amr.lambda.RestrictedLogicalConstant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MonadAssignmentGeneratorFactory extends AssignmentGeneratorFactory {
    public AssignmentGenerator create(LogicalExpression exp) {
        final Set<LogicalExpression> ids = new HashSet<>();
		ids.addAll(GetSkolemIds.of(exp));

		final Map<LogicalConstant, Set<LogicalExpression>> allAssignments = new HashMap<>();
		allAssignments.put(SkolemServices.getIdPlaceholder(), ids);

		Set<LogicalConstant> constants = GetAllSimpleLogicalConstants.of(exp);
		for (LogicalConstant constant : constants) {
			if (constant instanceof RestrictedLogicalConstant) {
				RestrictedLogicalConstant restrictedConstant = (RestrictedLogicalConstant) constant;
				Set<LogicalExpression> restrictedIds = new HashSet<>(restrictedConstant.getRefs());
				allAssignments.put(restrictedConstant, restrictedIds);
			}
		}

		return new AssignmentGenerator.Builder(allAssignments).build();
    }
}
