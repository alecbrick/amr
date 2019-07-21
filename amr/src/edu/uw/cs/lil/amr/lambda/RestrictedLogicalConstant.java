/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
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
 *******************************************************************************/
package edu.uw.cs.lil.amr.lambda;

import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionReader.IReader;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import jregex.Matcher;
import jregex.Pattern;

import java.io.ObjectStreamException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Lambda calculus constant. A constant must have a unique name that matches the
 * regular expression [a-z0-9A-Z_]+:type_name. The marker "@" can be used as a
 * prefix to denote a dynamic constant.
 *
 * @author Yoav Artzi
 */
public class RestrictedLogicalConstant extends LogicalConstant {
	protected Set<SkolemId> refs;
	private final LogicalConstant	wrappedConstant;

	public RestrictedLogicalConstant(LogicalConstant constant,
										Set<SkolemId> refs) {
	    super(constant.getBaseName(), constant.getType(), true);
		this.refs = refs;
		this.wrappedConstant = constant;
	}

    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		final RestrictedLogicalConstant other = (RestrictedLogicalConstant) obj;

		if (!wrappedConstant.equals(other.wrappedConstant)) {
			return false;
		}
		return refs.equals(other.refs);
	}

	public Set<SkolemId> getRefs() {
		return refs;
	}

	public LogicalConstant getWrappedConstant() {
		return wrappedConstant;
	}

}
