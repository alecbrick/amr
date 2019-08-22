package edu.uw.cs.lil.amr.parser;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryRecursiveParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower.ReversibleUnaryLower;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower.ReversibleUnaryTowerRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.TowerRule;
import edu.uw.cs.lil.amr.TestServices;
import edu.uw.cs.lil.amr.TowerTestServices;
import edu.uw.cs.lil.amr.parser.rules.amrspecials.SkolemizeRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UniversalTest {
    List<IBinaryRecursiveParseRule<LogicalExpression>> recursiveRules;
    TowerRule<LogicalExpression> towerRule;
    private final ReversibleUnaryTowerRule towerSkolemizeRule;
    private final ReversibleUnaryLower towerLowerRule;

    public UniversalTest() {
        TowerTestServices.init();
        this.recursiveRules = new ArrayList<>(TowerTestServices.getRecursiveRules());
        this.towerRule = new TowerRule<>(TowerTestServices.getTowerCategoryServices(), recursiveRules);
        this.towerSkolemizeRule = new ReversibleUnaryTowerRule(TowerTestServices.getTowerCategoryServices(),
                new SkolemizeRule("Sk", TowerTestServices.getCategoryServices(), true));
        this.towerLowerRule = new ReversibleUnaryLower("v", TowerTestServices.getTowerCategoryServices());
    }

        // i'm so sorry
    public Category<LogicalExpression> createIndefinite() {
        Lambda towerTop = (Lambda) LogicalExpression.read(
                "(lambda $0:t (lambda $1:<e,M[e]> (>>= $2:e (stateM (a:<id,<<e,t>,e>> !0  (lambda $3:e $0)) {!0})   ($1 $2))))");//[  (lambda $5:<e,t>  ($5 $3)  )    ]");
        Variable scopedVariable = ((Lambda) ((Literal) ((StateMonad) ((Binding) ((Lambda) towerTop.getBody()).getBody()).getLeft()).getBody()).getArg(1)).getArgument();
        LogicalExpressionReader.setLambdaWrapped(false);
        Variable baseArg = (Variable) Variable.read("$0:<e,t>");
        LogicalExpression[] args = {
                scopedVariable
        };
        LogicalExpression towerBase = new Lambda(baseArg, new Literal(baseArg, args));

        Category<LogicalExpression> indef = Category.create(
                Syntax.read("(A//NP\\\\A)//(S/N)\\\\S"),
                new Tower(towerTop, towerBase)
        );
        return indef;
    }

    public Category<LogicalExpression> createUniversal() {
        Lambda linearSem = (Lambda) LogicalExpression.read(
                "(lambda $1:<<<e,t>,t>,t> (lambda $2:<e,e> (stateM (a:<id,<<e,t>,e>> !1 (lambda $3:e (and:<t*,t>  " +
                        "($1 (lambda $4:<e,t> ($4 $3)))       " +
                        "(mod:<e,<e,t>> $3 all:e)        " +
                        "(scope-over:<e,<e,t>> $3 ($2 (ref:<id,e> !1)))   )  )) ())))");//[  (lambda $5:<e,t>  ($5 $3)  )    ]");
        Tower tower = TowerTestServices.getTowerCategoryServices().lambdaToTower(linearSem);

        Category<LogicalExpression> univ = Category.create(
                Syntax.read("(A//NP\\\\A)//(S/N)\\\\S"),
                tower
        );
        return univ;
    }


    public List<ParseRuleResult<LogicalExpression>> combineWithLeft(
            List<ParseRuleResult<LogicalExpression>> lefts,
            Category<LogicalExpression> right) {
        List<ParseRuleResult<LogicalExpression>> ret = new ArrayList<>();
        for (ParseRuleResult<LogicalExpression> left : lefts) {
            ret.addAll(towerRule.applyRecursive(left.getResultCategory(), right, null, null));
        }
        return ret;
    }

    public List<ParseRuleResult<LogicalExpression>> combineWithRight(
            Category<LogicalExpression> left,
            List<ParseRuleResult<LogicalExpression>> rights) {
        List<ParseRuleResult<LogicalExpression>> ret = new ArrayList<>();
        for (ParseRuleResult<LogicalExpression> right : rights) {
            ret.addAll(towerRule.applyRecursive(left, right.getResultCategory(), null, null));
        }
        return ret;
    }

    public List<ParseRuleResult<LogicalExpression>> combineAll(
            List<ParseRuleResult<LogicalExpression>> lefts,
            List<ParseRuleResult<LogicalExpression>> rights) {
        List<ParseRuleResult<LogicalExpression>> ret = new ArrayList<>();
        for (ParseRuleResult<LogicalExpression> left : lefts) {
            for (ParseRuleResult<LogicalExpression> right : rights) {
                ret.addAll(towerRule.applyRecursive(left.getResultCategory(), right.getResultCategory(), null, null));
            }
        }
        return ret;
    }

    public List<ParseRuleResult<LogicalExpression>> applyUnary(
            List<ParseRuleResult<LogicalExpression>> inputs,
            IUnaryParseRule<LogicalExpression> rule) {
        List<ParseRuleResult<LogicalExpression>> ret = new ArrayList<>();
        for (ParseRuleResult<LogicalExpression> input : inputs) {
            ParseRuleResult<LogicalExpression> result = rule.apply(input.getResultCategory(), null);
            if (result != null) {
                ret.add(result);
            }
        }
        return ret;
    }

    // If all of my relatives die, I'll inherit a fortune.
    @Test
    public void test() {
        LogicalExpressionCategoryServices categoryServices = TowerTestServices.getCategoryServices();
        Category<LogicalExpression> ifCat = Category.create(
                Syntax.read("S/S/<A>"),
                LogicalExpression.read(
                        "(lambda $0:e (lambda $1:<e,t> (lambda $2:e (and:<t*,t> " +
                                "($1 $2) " +
                                "(cond:<e,<e,t>> $2 $0)))))")
        );

        System.out.println(ifCat);

        Category<LogicalExpression> indef1 = createIndefinite();
        Category<LogicalExpression> univ = createUniversal();

        System.out.println(univ);

        Category<LogicalExpression> relative = Category.create(
                Syntax.read("N"),
                LogicalExpression.read(
                        "(lambda $0:e (relative:<e,t> $0))"
                )
        );
        List<ParseRuleResult<LogicalExpression>> results = towerRule.applyRecursive(univ, relative, null, null);
        System.out.println("All of my relatives");
        System.out.println(results);

        Category<LogicalExpression> die = categoryServices.read(
                "S\\NP : (lambda $0:e (lambda $1:e (and:<t*,t> (die:<e,t> $1) (ARG0:<e,<e,t>> $1 $0))))"
        );
        System.out.println("All of my relatives die");
        results = combineWithLeft(results, die);
        System.out.println(results);

        System.out.println("to AMR");
        results.addAll(applyUnary(results, this.towerSkolemizeRule));
        System.out.println(results);

        System.out.println("If all of my relatives die");
        results = combineWithRight(ifCat, results);
        System.out.println(results);

        Category<LogicalExpression> me = categoryServices.read(
                "NP : (a:<id,<<e,t>,e>> na:id (lambda $0:e (me:<e,t> $0)))"
        );

        Category<LogicalExpression> inherit = categoryServices.read(
                "S\\NP/NP : (lambda $0:e (lambda $1:e (lambda $2:e (and:<t*,t> (inherit:<e,t> $2) (ARG0:<e,<e,t>> $2 $1) (ARG1:<e,<e,t>> $2 $0))))"
        );

        Category<LogicalExpression> fortune = categoryServices.read(
                "N : (lambda $0:e (fortune:<e,t> $0))"
        );

        Category<LogicalExpression> indef2 = createIndefinite();

        System.out.println("a FORTUNE");
        List<ParseRuleResult<LogicalExpression>> aFortune = towerRule.applyRecursive(indef2, fortune, null, null);
        System.out.println(aFortune);

        System.out.println("inherit a fortune");
        List<ParseRuleResult<LogicalExpression>> inheritFortune = combineWithRight(inherit, aFortune);
        System.out.println(inheritFortune);

        System.out.println("I'll inherit a fortune");
        List<ParseRuleResult<LogicalExpression>> iInheritFortune = combineWithRight(me, inheritFortune);
        System.out.println(iInheritFortune);

        System.out.println("Skolemize");
        iInheritFortune.addAll(applyUnary(iInheritFortune, towerSkolemizeRule));
        System.out.println("If all of my relatives die, I'll inherit a fortune");
        results = combineAll(results, iInheritFortune);
        System.out.println(results);

        results = applyUnary(results, towerSkolemizeRule);
        System.out.println(results);
        results = applyUnary(results, towerLowerRule);
        System.out.println(results);

        System.out.println("RESULTS!");
        for (ParseRuleResult result : results) {
            System.out.println(result);
        }
    }
}
