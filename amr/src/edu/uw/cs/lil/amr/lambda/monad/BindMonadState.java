package edu.uw.cs.lil.amr.lambda.monad;

import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.uw.cs.lil.amr.lambda.AMRServices;
import edu.uw.cs.lil.amr.lambda.RestrictedLogicalConstant;

import java.util.Set;

public class BindMonadState implements ILogicalExpressionVisitor {
    private LogicalExpression   result		= null;
    private Set<SkolemId>       state;

    // Use via "of" method.
    private BindMonadState(Set<SkolemId> state) {
        this.state = state;
    }

    public static LogicalExpression of(LogicalExpression exp,
                                       Set<SkolemId> state) {
        BindMonadState visitor = new BindMonadState(state);
        visitor.visit(exp);
        return visitor.result;
    }

    @Override
    public void visit(Lambda lambda) {
        lambda.getBody().accept(this);
        result = new Lambda(lambda.getArgument(), result);
    }

    @Override
    public void visit(Literal literal) {
		literal.getPredicate().accept(this);
		final LogicalExpression newPredicate = result;

		final int len = literal.numArgs();
		final LogicalExpression[] newArgs = new LogicalExpression[len];
		boolean argChanged = false;
		for (int i = 0; i < len; ++i) {
			final LogicalExpression arg = literal.getArg(i);
			arg.accept(this);
			newArgs[i] = result;
			if (arg != result) {
				argChanged = true;
			}
		}

		if (AMRServices.isRefLiteral(literal) &&
                !(newArgs[0] instanceof RestrictedLogicalConstant)) {
            newArgs[0] = new RestrictedLogicalConstant((LogicalConstant) newArgs[0], state);
            argChanged = true;
        }

		if (argChanged || newPredicate != literal.getPredicate()) {
			result = new Literal(newPredicate, newArgs);
		} else {
			result = literal;
		}
    }

    @Override
    public void visit(LogicalConstant logicalConstant) {
        result = logicalConstant;
    }

    @Override
    public void visit(Variable variable) {
        result = variable;
    }
}
