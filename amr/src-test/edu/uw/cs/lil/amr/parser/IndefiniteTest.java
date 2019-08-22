package edu.uw.cs.lil.amr.parser;

import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.TowerCategoryServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower.ReversibleTowerRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower.ReversibleUnaryLower;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower.ReversibleUnaryTowerRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.ForwardApplication;
import edu.cornell.cs.nlp.utils.log.Log;
import edu.uw.cs.lil.amr.TestServices;
import edu.uw.cs.lil.amr.TowerTestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.TowerRule;
import edu.uw.cs.lil.amr.parser.rules.amrspecials.SkolemizeRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IndefiniteTest {
    List<IBinaryReversibleRecursiveParseRule<LogicalExpression>> recursiveRules;
    ReversibleTowerRule towerRule;
    private final ReversibleUnaryTowerRule towerSkolemizeRule;
    private final ReversibleUnaryLower towerLowerRule;

    public IndefiniteTest() {
        TowerTestServices.init();
        this.recursiveRules = new ArrayList<>(TowerTestServices.getRecursiveRules());
        this.towerRule = new ReversibleTowerRule(
                TowerTestServices.getTowerCategoryServices(),
                recursiveRules,
                TowerTestServices.getCategoryServices(),
                TowerTestServices.getBaseReversibleRules(),
                (ForwardReversibleApplication) TowerTestServices.getBaseReversibleRules().get(0));
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

    // If one of my relatives dies, I'll inherit a fortune.
    @Test
    public void ParseTest() {
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

        Category<LogicalExpression> relative = Category.create(
                Syntax.read("N"),
                LogicalExpression.read(
                        "(lambda $0:e (relative:<e,t> $0))"
                )
        );
        List<ParseRuleResult<LogicalExpression>> results = towerRule.applyRecursive(indef1, relative, null, null);
        System.out.println("One of my relatives");
        System.out.println(results);

        Category<LogicalExpression> die = categoryServices.read(
                "S\\NP : (lambda $0:e (lambda $1:e (and:<t*,t> (die:<e,t> $1) (ARG0:<e,<e,t>> $1 $0))))"
        );
        System.out.println(die);
        results = combineWithLeft(results, die);
        System.out.println(results);

        results.addAll(applyUnary(results, this.towerSkolemizeRule));
        System.out.println(results);

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

        List<ParseRuleResult<LogicalExpression>> aFortune = towerRule.applyRecursive(indef2, fortune, null, null);
        System.out.println(aFortune);

        List<ParseRuleResult<LogicalExpression>> inheritFortune = combineWithRight(inherit, aFortune);
        System.out.println(inheritFortune);

        List<ParseRuleResult<LogicalExpression>> iInheritFortune = combineWithRight(me, inheritFortune);
        System.out.println(iInheritFortune);

        iInheritFortune.addAll(applyUnary(iInheritFortune, towerSkolemizeRule));
        results = combineAll(results, iInheritFortune);
        System.out.println(results);

        results = applyUnary(results, towerSkolemizeRule);
        System.out.println(results);
        results = applyUnary(results, towerLowerRule);
        System.out.println("RESULTS!");
        for (ParseRuleResult result : results) {
            System.out.println(result);
        }
    }

    public Set<Category<LogicalExpression>> reverseApplyLeft(Category<LogicalExpression> left, Set<Category<LogicalExpression>> results) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();
        for (Category<LogicalExpression> result : results) {
            ret.addAll(towerRule.reverseApplyLeft(left, result, null));
        }
        return ret;
    }
    
    public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Set<Category<LogicalExpression>> results) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();
        for (Category<LogicalExpression> result : results) {
            ret.addAll(towerRule.reverseApplyRight(right, result, null));
        }
        return ret;
    }

    public Set<Category<LogicalExpression>> reverseApplyUnary(Set<Category<LogicalExpression>> results, IUnaryReversibleParseRule<LogicalExpression> rule) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();
        for (Category<LogicalExpression> result : results) {
            ret.addAll(rule.reverseApply(result, null));
        }
        return ret;
    }

    @Test
    public void GenlexTest() {
        TowerCategoryServices towerCategoryServices = TowerTestServices.getTowerCategoryServices();
        ICategoryServices<LogicalExpression> categoryServices = TowerTestServices.getCategoryServices();
        Category<LogicalExpression> output = categoryServices.read("A : (stateM (a:<id,<<e,t>,e>> na:id (lambda $0:e (and:<t*,t> (inherit:<e,t> $0) (ARG0:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $1:e (me:<e,t> $1)))) (ARG1:<e,<e,t>> $0 (a:<id,<<e,t>,e>> !1 (lambda $2:e (fortune:<e,t> $2)))) (cond:<e,<e,t>> $0 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (die:<e,t> $3) (ARG0:<e,<e,t>> $3 (a:<id,<<e,t>,e>> !2 (lambda $4:e (relative:<e,t> $4))))))))))) (!1 !2))]");
        System.out.println(output);

        System.out.println("Unlowering output");
        Set<Category<LogicalExpression>> results = towerLowerRule.reverseApply(output, null);
        System.out.println(results);
        System.out.println(results.size() + " results");

        System.out.println("Unskolemizing output");
        results = reverseApplyUnary(results, towerSkolemizeRule);
        System.out.println(results);
        System.out.println(results.size() + " results");

        Category<LogicalExpression> illInheritAFortune = categoryServices.read("A//S\\\\A : [(lambda $0:M[e] (>>= $1:e (stateM (a:<id,<<e,t>,e>> !1 (lambda $2:e (fortune:<e,t> $2))) (!1)) $0))][(lambda $3:e (and:<t*,t> (inherit:<e,t> $3) (ARG0:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (me:<e,t> $4)))) (ARG1:<e,<e,t>> $3 $1)))]");
        System.out.println("Splitting \"If one of my relatives dies, I'll inherit a fortune\"");
        results = reverseApplyRight(illInheritAFortune, results);
        System.out.println(results);
        System.out.println(results.size() + " results");

        Category<LogicalExpression> ifCat = Category.create(
                Syntax.read("S/S/<A>"),
                LogicalExpression.read(
                        "(lambda $0:e (lambda $1:<e,t> (lambda $2:e (and:<t*,t> " +
                                "($1 $2) " +
                                "(cond:<e,<e,t>> $2 $0)))))")
        );
        System.out.println("Splitting \"If one of my relatives dies\"");
        results = reverseApplyLeft(ifCat, results);
        System.out.println(results);
        System.out.println(results.size() + " results");

        System.out.println("Unskolemizing");
        results = reverseApplyUnary(results, towerSkolemizeRule);
        System.out.println(results);
        System.out.println(results.size() + " results");

        Category<LogicalExpression> die = categoryServices.read(
                "S\\NP : (lambda $0:e (lambda $1:e (and:<t*,t> (die:<e,t> $1) (ARG0:<e,<e,t>> $1 $0))))"
        );

        System.out.println("Splitting \"one of my relatives dies\"");
        results = reverseApplyRight(die, results);
        System.out.println(results);
        System.out.println(results.size() + " results");

        Category<LogicalExpression> relative = Category.create(
                Syntax.read("N"),
                LogicalExpression.read(
                        "(lambda $0:e (relative:<e,t> $0))"
                )
        );
        System.out.println("Splitting \"one of my relatives\"");
        results = reverseApplyRight(relative, results);
        System.out.println(results);
        System.out.println(results.size() + " results");
    }
}
