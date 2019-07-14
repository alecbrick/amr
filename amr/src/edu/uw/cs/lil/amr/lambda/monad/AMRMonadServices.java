package edu.uw.cs.lil.amr.lambda.monad;

import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.MonadServices;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.AllSubExpressions;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ReplaceExpression;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.uw.cs.lil.amr.lambda.AMRServices;
import edu.uw.cs.lil.amr.lambda.GetAllSkolemTerms;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AMRMonadServices extends MonadServices {
    @Override
    public Monad logicalExpressionToMonad(LogicalExpression exp) {
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

    public Set<LogicalExpression> logicalExpressionFromMonad(Monad monad) {
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


}
