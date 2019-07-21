package edu.uw.cs.lil.amr.lambda.monad;

import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.uw.cs.lil.amr.lambda.AMRServices;
import edu.uw.cs.lil.amr.lambda.GetAllSkolemTerms;

import java.util.HashSet;
import java.util.Set;

public class AMRMonadServices extends MonadServices {
    @Override
    protected Monad logicalExpressionToMonadImpl(LogicalExpression exp) {
        Literal skolemizedExp = AMRServices.skolemize(exp);
        Set<LogicalExpression> skolemTerms =
                GetAllSkolemTerms.of(skolemizedExp, false);
        Set<SkolemId> ids = new HashSet<>();
        for (LogicalExpression e : skolemTerms) {
            if (!(e instanceof Literal)) {
                continue;
            }
            Literal literal = (Literal) e;
            LogicalExpression arg0 = literal.getArg(0);
            if (!(arg0 instanceof SkolemId)) {
                continue;
            }
            ids.add((SkolemId) arg0);
        }
        return new StateMonad(exp, new State<>(ids));
    }

    @Override
    protected Set<LogicalExpression> logicalExpressionFromMonadImpl(Monad monad) {
        LogicalExpression body = monad.getBody();
        Set<LogicalExpression> ret = new HashSet<>();
        ret.add(body);
        if (AMRServices.isSkolemTerm(body)) {
            Literal literal = (Literal) body;
            if (literal.numArgs() == 2) {
                ret.add(literal.getArg(1));
            }
        }
        return ret;
    }

    @Override
    protected LogicalExpression bindStateImpl(LogicalExpression exp, Set<SkolemId> ids) {
        return BindMonadState.of(exp, ids);
    }

}
