package edu.uw.cs.lil.amr.lambda.monad;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.AllSubExpressions;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetAllFreeVariables;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetSkolemIds;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ReplaceExpression;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.uw.cs.lil.amr.lambda.AMRServices;
import edu.uw.cs.lil.amr.lambda.GetAllSkolemTerms;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AMRMonadServices extends MonadServices {
    @Override
    protected Syntax getMonadSyntaxImpl() {
        return AMRServices.AMR;
    }

    /*
    remnants from a simpler time
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
    }*/

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

    private Set<SkolemId> skolemIdsFromMonad(LogicalExpression monad) {
        Set<SkolemId> ret = new HashSet<>();
        if (monad instanceof Binding) {
            Binding binding = (Binding) monad;
            ret.addAll(skolemIdsFromMonad(binding.getLeft()));
            ret.addAll(skolemIdsFromMonad(binding.getRight()));
        } else if (monad instanceof StateMonad) {
            StateMonad stateM = (StateMonad) monad;
            ret.addAll(stateM.getState().getState());
        }
        return ret;
    }

    @Override
    protected Set<Monad> unexecMonadImpl(Monad input) {
        List<LogicalExpression> exps = AllSubExpressions.of(input);
        Type amrType = LogicLanguageServices.getTypeRepository().getEntityType();
        Set<Monad> ret = new HashSet<>();
        Set<Variable> inputBoundVariables = new HashSet<>();
        if (input instanceof Binding) {
            inputBoundVariables.addAll(((Binding) input).getBoundVariables());
        }

        for (LogicalExpression exp : exps) {
            if (!exp.getType().equals(amrType)) {
                continue;
            }
            // Don't extract expressions containing bound variables
            if (inputBoundVariables.size() > 0) {
                Set<Variable> freeVars = GetAllFreeVariables.of(exp);
                freeVars.retainAll(inputBoundVariables);
                if (freeVars.size() > 0) {
                    continue;
                }
            }
            Variable variable = new Variable(amrType);
            Set<SkolemId> inputIDs = skolemIdsFromMonad(input);
            Monad left = new StateMonad(exp, new State<>(inputIDs));
            // LogicalExpression, since this could replace the entire expression.
            LogicalExpression right = ReplaceExpression.of(input, exp, variable);
            Binding binding = new Binding(left, right, variable);
            // Honestly maybe only need to run updateStates at the end
            ret.add((Binding) updateStates(binding));
        }
        return ret;
    }

    /*
    @Override
    protected LogicalExpression bindStateImpl(LogicalExpression exp, Set<SkolemId> ids) {
        return BindMonadState.of(exp, ids);
    }*/

    // Hacky. sorry not sorry.
    public static class AMRBuilder {
		public MonadServices build() {
			return new AMRMonadServices();
		}
	}
}
