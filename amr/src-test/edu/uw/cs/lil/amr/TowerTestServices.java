/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 ******************************************************************************/
package edu.uw.cs.lil.amr;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.MonadServices;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.MonadCategoryServices;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.TowerCategoryServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleRecursiveParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.BackwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.BackwardApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.ForwardApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.BackwardComposition;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.ForwardComposition;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.delimit.DelimitLeft;
import edu.cornell.cs.nlp.utils.log.LogLevel;
import edu.cornell.cs.nlp.utils.log.Logger;
import edu.uw.cs.lil.amr.Init;
import edu.uw.cs.lil.amr.lambda.monad.AMRMonadServices;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TowerTestServices {

	public static final LogicalExpressionCategoryServices	CATEGORY_SERVICES;
	public static final TowerCategoryServices				TOWER_CATEGORY_SERVICES;
	public static final MonadCategoryServices MONAD_SERVICES;

	private static final File								DEFAULT_TYPES_FILE;

	public static final BinaryRuleSet<LogicalExpression> BASE_RULES;
	public static final List<IBinaryReversibleRecursiveParseRule<LogicalExpression>> RECURSIVE_RULES;
	public static final List<IBinaryReversibleParseRule<LogicalExpression>> BASE_REVERSIBLE_RULES;

	private TowerTestServices() {
		// Private ctor. Nothing to init. Service class.
	}

	static {
		LogLevel.DEBUG.set();
		Logger.setSkipPrefix(true);

		DEFAULT_TYPES_FILE = new File("../resources/amr.types");

		try {
			Init.init(
					DEFAULT_TYPES_FILE,
					new File("../resources/amr.specmap"),
					new File(
							"../resources/stanford-models/english-bidirectional-distsim.tagger"),
					false, null, null, null, false);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		// //////////////////////////////////////////////////
		// Category services for logical expressions.
		// //////////////////////////////////////////////////

		// CCG LogicalExpression category services for handling categories
		// with LogicalExpression as semantics
		CATEGORY_SERVICES = new LogicalExpressionCategoryServices(true);

		MONAD_SERVICES = new MonadCategoryServices();
		TOWER_CATEGORY_SERVICES = new TowerCategoryServices(true);

		// //////////////////////////////////////////////////
		// Initialize monad services
		// //////////////////////////////////////////////////
		MonadServices.setInstance(new AMRMonadServices.AMRBuilder().build());

		List<IBinaryParseRule<LogicalExpression>> baseRules = new ArrayList<>();
		baseRules.add(new ForwardApplication<>(CATEGORY_SERVICES));
		baseRules.add(new BackwardApplication<>(CATEGORY_SERVICES));
		baseRules.add(new ForwardComposition<>(CATEGORY_SERVICES, 1, false));
		baseRules.add(new BackwardComposition<>(CATEGORY_SERVICES, 1, false));
		BASE_RULES = new BinaryRuleSet<>(baseRules);

		List<IBinaryReversibleParseRule<LogicalExpression>> baseReversibleRules = new ArrayList<>();
		Set<String> attributes = new HashSet<>();
		attributes.add("sg");
		attributes.add("pl");
		ForwardReversibleApplication forwardApp = new ForwardReversibleApplication(
				CATEGORY_SERVICES, 3, 9, true, attributes);
		baseReversibleRules.add(forwardApp);
		baseReversibleRules.add(new BackwardReversibleApplication(
				CATEGORY_SERVICES, 3, 9, true, attributes));
		BASE_REVERSIBLE_RULES = baseReversibleRules;


		RECURSIVE_RULES = new ArrayList<>();
		RECURSIVE_RULES.add(new ReversibleCombination("C", TOWER_CATEGORY_SERVICES, BASE_RULES, new HashSet<>(baseReversibleRules), forwardApp));
		RECURSIVE_RULES.add(new ReversibleLiftLeft("^", TOWER_CATEGORY_SERVICES, BASE_RULES, new HashSet<>(baseReversibleRules), forwardApp));
		RECURSIVE_RULES.add(new ReversibleLiftRight("^", TOWER_CATEGORY_SERVICES, BASE_RULES, new HashSet<>(baseReversibleRules), forwardApp));
		RECURSIVE_RULES.add(new ReversibleDelimitLeft("DL", TOWER_CATEGORY_SERVICES, BASE_RULES, new HashSet<>(baseReversibleRules), forwardApp));
		RECURSIVE_RULES.add(new ReversibleDelimitRight("DR", TOWER_CATEGORY_SERVICES, BASE_RULES, new HashSet<>(baseReversibleRules), forwardApp));

		for (IBinaryReversibleRecursiveParseRule<LogicalExpression> rule : RECURSIVE_RULES) {
			for (IBinaryReversibleRecursiveParseRule<LogicalExpression> toAdd : RECURSIVE_RULES) {
				rule.addRecursiveRule(toAdd);
			}
		}
	}

	public static LogicalExpressionCategoryServices getCategoryServices() {
		return CATEGORY_SERVICES;
	}

	public static void init() {
		// Nothing to do.
	}

	public static TowerCategoryServices getTowerCategoryServices() {
	    return TOWER_CATEGORY_SERVICES;
	}

	public static MonadCategoryServices getMonadServices() {
		return MONAD_SERVICES;
	}

	public static BinaryRuleSet<LogicalExpression> getBaseRules() {
		return BASE_RULES;
	}

	public static List<IBinaryReversibleRecursiveParseRule<LogicalExpression>> getRecursiveRules() {
		return RECURSIVE_RULES;
	}

	public static List<IBinaryReversibleParseRule<LogicalExpression>> getBaseReversibleRules() {
		return BASE_REVERSIBLE_RULES;
	}
}
