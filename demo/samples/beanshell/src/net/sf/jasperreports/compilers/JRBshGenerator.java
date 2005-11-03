/*
 * ============================================================================
 * GNU Lesser General Public License
 * ============================================================================
 *
 * JasperReports - Free Java report-generating library.
 * Copyright (C) 2001-2005 JasperSoft Corporation http://www.jaspersoft.com
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307, USA.
 * 
 * JasperSoft Corporation
 * 185, Berry Street, Suite 6200
 * San Francisco CA 94107
 * http://www.jaspersoft.com
 */

/*
 * Contributors:
 * Gaganis Giorgos - gaganis@users.sourceforge.net
 */
package net.sf.jasperreports.compilers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.crosstabs.design.JRDesignCrosstab;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRExpressionChunk;
import net.sf.jasperreports.engine.JRExpressionCollector;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRVariable;
import net.sf.jasperreports.engine.design.JRAbstractCompiler;
import net.sf.jasperreports.engine.design.JRDesignDataset;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRStringUtil;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 * @version $Id$
 */
public class JRBshGenerator
{
	
	
	/**
	 *
	 */
	protected JasperDesign jasperDesign = null;
	protected JRExpressionCollector expressionCollector;

	protected Map parametersMap;
	protected Map fieldsMap;
	protected Map variablesMap;
	protected JRVariable[] variables;
	
	protected String unitName;
	protected List expressions;
	
	protected boolean onlyDefaultEvaluation;

	private static Map fieldPrefixMap = null;
	private static Map variablePrefixMap = null;
	private static Map methodSuffixMap = null;

	static
	{
		fieldPrefixMap = new HashMap();
		fieldPrefixMap.put(new Byte(JRExpression.EVALUATION_OLD),       "Old");
		fieldPrefixMap.put(new Byte(JRExpression.EVALUATION_ESTIMATED), "");
		fieldPrefixMap.put(new Byte(JRExpression.EVALUATION_DEFAULT),   "");
		
		variablePrefixMap = new HashMap();
		variablePrefixMap.put(new Byte(JRExpression.EVALUATION_OLD),       "Old");
		variablePrefixMap.put(new Byte(JRExpression.EVALUATION_ESTIMATED), "Estimated");
		variablePrefixMap.put(new Byte(JRExpression.EVALUATION_DEFAULT),   "");
		
		methodSuffixMap = new HashMap();
		methodSuffixMap.put(new Byte(JRExpression.EVALUATION_OLD),       "Old");
		methodSuffixMap.put(new Byte(JRExpression.EVALUATION_ESTIMATED), "Estimated");
		methodSuffixMap.put(new Byte(JRExpression.EVALUATION_DEFAULT),   "");
	}
	

	protected JRBshGenerator(JasperDesign jrDesign, JRExpressionCollector expressionCollector,
			Map parametersMap, Map fieldsMap, Map variablesMap, JRVariable[] variables,
			String unitName, List expressions, boolean onlyDefaultEvaluation)
	{
		jasperDesign = jrDesign;
		this.expressionCollector = expressionCollector;
		
		this.parametersMap = parametersMap;
		this.fieldsMap = fieldsMap;
		this.variablesMap = variablesMap;
		this.variables = variables;
		
		this.unitName = unitName;
		this.expressions = expressions;
		
		this.onlyDefaultEvaluation = onlyDefaultEvaluation;
	}

	
	protected JRBshGenerator(JasperDesign jrDesign, JRDesignDataset dataset, JRExpressionCollector expressionCollector)
	{
		this(jrDesign, expressionCollector,
				dataset.getParametersMap(), dataset.getFieldsMap(), dataset.getVariablesMap(), dataset.getVariables(),
				JRAbstractCompiler.getUnitName(jrDesign, dataset), expressionCollector.getExpressions(dataset), false);
	}


	protected JRBshGenerator(JasperDesign jrDesign, JRDesignCrosstab crosstab, JRExpressionCollector expressionCollector)
	{
		this(jrDesign, expressionCollector,
				crosstab.getParametersMap(), null, crosstab.getVariablesMap(), crosstab.getVariables(),
				JRAbstractCompiler.getUnitName(jrDesign, crosstab, expressionCollector), expressionCollector.getExpressions(crosstab), true);
	}


	/**
	 *
	 */
	public static String generateScript(JasperDesign jrDesign, JRDesignDataset dataset, JRExpressionCollector expressionCollector)
	{
		JRBshGenerator generator = new JRBshGenerator(jrDesign, dataset, expressionCollector);
		return generator.generateScript();
	}


	/**
	 *
	 */
	public static String generateScript(JasperDesign jrDesign, JRDesignCrosstab crosstab, JRExpressionCollector expressionCollector)
	{
		JRBshGenerator generator = new JRBshGenerator(jrDesign, crosstab, expressionCollector);
		return generator.generateScript();
	}
	
	
	protected String generateScript()
	{
		StringBuffer sb = new StringBuffer();

		generateScriptStart(sb);

		generateDeclarations(sb);
		generateInitMethod(sb);
		
		sb.append("\n");
		sb.append("\n");

		sb.append(generateMethod(JRExpression.EVALUATION_DEFAULT, expressions));
		if (onlyDefaultEvaluation)
		{
			List empty = new ArrayList();
			sb.append(generateMethod(JRExpression.EVALUATION_OLD, empty));
			sb.append(generateMethod(JRExpression.EVALUATION_ESTIMATED, empty));
		}
		else
		{
			sb.append(generateMethod(JRExpression.EVALUATION_OLD, expressions));
			sb.append(generateMethod(JRExpression.EVALUATION_ESTIMATED, expressions));
		}

		generateScriptEnd(sb);

		return sb.toString();
	}


	protected final void generateScriptStart(StringBuffer sb)
	{
		/*   */
		sb.append("//\n");
		sb.append("// Generated by JasperReports - ");
		sb.append((new SimpleDateFormat()).format(new java.util.Date()));
		sb.append("\n");
		sb.append("//\n");
		sb.append("import net.sf.jasperreports.engine.*;\n");
		sb.append("import net.sf.jasperreports.engine.fill.*;\n");
		sb.append("\n");
		sb.append("import java.util.*;\n");
		sb.append("import java.math.*;\n");
		sb.append("import java.text.*;\n");
		sb.append("import java.io.*;\n");
		sb.append("import java.net.*;\n");
		sb.append("\n");
		
		/*   */
		String[] imports = jasperDesign.getImports();
		if (imports != null && imports.length > 0)
		{
			for (int i = 0; i < imports.length; i++)
			{
				sb.append("import ");
				sb.append(imports[i]);
				sb.append(";\n");
			}
		}

		/*   */
		sb.append("\n");
		sb.append("\n");
		sb.append("createBshEvaluator()\n");
		sb.append("{\n"); 
		sb.append("\n");
		sb.append("\n");
		sb.append("    JREvaluator evaluator = null;\n");
		sb.append("\n");
	}


	protected final void generateDeclarations(StringBuffer sb)
	{
		/*   */
		if (parametersMap != null && parametersMap.size() > 0)
		{
			Collection parameterNames = parametersMap.keySet();
			for (Iterator it = parameterNames.iterator(); it.hasNext();)
			{
				sb.append("    JRFillParameter parameter_");
				sb.append(JRStringUtil.getLiteral((String)it.next()));
				sb.append(" = null;\n");
			}
		}
		
		/*   */
		sb.append("\n");

		/*   */
		if (fieldsMap != null && fieldsMap.size() > 0)
		{
			Collection fieldNames = fieldsMap.keySet();
			for (Iterator it = fieldNames.iterator(); it.hasNext();)
			{
				sb.append("    JRFillField field_");
				sb.append(JRStringUtil.getLiteral((String)it.next()));
				sb.append(" = null;\n");
			}
		}
		
		/*   */
		sb.append("\n");

		/*   */
		if (variables != null && variables.length > 0)
		{
			for (int i = 0; i < variables.length; i++)
			{
				sb.append("    JRFillVariable variable_");
				sb.append(JRStringUtil.getLiteral(variables[i].getName()));
				sb.append(" = null;\n");
			}
		}
	}


	protected final void generateInitMethod(StringBuffer sb)
	{
		/*   */
		sb.append("\n");
		sb.append("\n");
		sb.append("    init(\n"); 
		sb.append("        JREvaluator evaluator,\n"); 
		sb.append("        Map parsm,\n"); 
		sb.append("        Map fldsm,\n"); 
		sb.append("        Map varsm\n");
		sb.append("        )\n");
		sb.append("    {\n");
		sb.append("        super.evaluator = evaluator;\n");
		sb.append("\n");

		/*   */
		if (parametersMap != null && parametersMap.size() > 0)
		{
			Collection parameterNames = parametersMap.keySet();
			String parameterName = null;
			for (Iterator it = parameterNames.iterator(); it.hasNext();)
			{
				parameterName = (String)it.next();
				sb.append("        super.parameter_");
				sb.append(JRStringUtil.getLiteral(parameterName));
				sb.append(" = (JRFillParameter)parsm.get(\"");
				sb.append(parameterName);
				sb.append("\");\n");
			}
		}
		
		/*   */
		sb.append("\n");

		/*   */
		if (fieldsMap != null && fieldsMap.size() > 0)
		{
			Collection fieldNames = fieldsMap.keySet();
			String fieldName = null;
			for (Iterator it = fieldNames.iterator(); it.hasNext();)
			{
				fieldName = (String)it.next();
				sb.append("        super.field_");
				sb.append(JRStringUtil.getLiteral(fieldName));
				sb.append(" = (JRFillField)fldsm.get(\"");
				sb.append(fieldName);
				sb.append("\");\n");
			}
		}
		
		/*   */
		sb.append("\n");

		/*   */
		if (variables != null && variables.length > 0)
		{
			String variableName = null;
			for (int i = 0; i < variables.length; i++)
			{
				variableName = variables[i].getName();
				sb.append("        super.variable_");
				sb.append(JRStringUtil.getLiteral(variableName));
				sb.append(" = (JRFillVariable)varsm.get(\"");
				sb.append(variableName);
				sb.append("\");\n");
			}
		}

		/*   */
		sb.append("    }\n");
	}


	protected void generateScriptEnd(StringBuffer sb)
	{
		sb.append("\n"); 
		sb.append("    str(String key)\n");
		sb.append("    {\n");
		sb.append("        return super.evaluator.str(key);\n");
		sb.append("    }\n");
		sb.append("\n"); 
		sb.append("    msg(String pattern, Object arg0)\n");
		sb.append("    {\n");
		sb.append("        return super.evaluator.msg(pattern, arg0);\n");
		sb.append("    }\n");
		sb.append("\n"); 
		sb.append("    msg(String pattern, Object arg0, Object arg1)\n");
		sb.append("    {\n");
		sb.append("        return super.evaluator.msg(pattern, arg0, arg1);\n");
		sb.append("    }\n");
		sb.append("\n"); 
		sb.append("    msg(String pattern, Object arg0, Object arg1, Object arg2)\n");
		sb.append("    {\n");
		sb.append("        return super.evaluator.msg(pattern, arg0, arg1, arg2);\n");
		sb.append("    }\n");
		sb.append("\n"); 
		sb.append("    return this;\n");
		sb.append("}\n");
	}		


	/**
	 *
	 */
	protected final String generateMethod(byte evaluationType, List expressionsList)
	{
		StringBuffer sb = new StringBuffer();

		/*   */
		sb.append("    Object evaluate");
		sb.append((String)methodSuffixMap.get(new Byte(evaluationType)));
		sb.append("(int id)\n");
		sb.append("    {\n");
		sb.append("        Object value = null;\n");
		sb.append("\n");
		sb.append("        switch (id)\n");
		sb.append("        {\n");

		if (expressionsList != null && !expressionsList.isEmpty())
		{
			JRExpression expression = null;
			for (Iterator it = expressionsList.iterator(); it.hasNext();)
			{
				expression = (JRExpression)it.next();
				
				sb.append("            case ");
				sb.append(expressionCollector.getExpressionId(expression));
				sb.append(" :\n");
				sb.append("            {\n");
				sb.append("                value = (");
				sb.append(expression.getValueClassName());
				sb.append(")(");
				sb.append(this.generateExpression(expression, evaluationType));
				sb.append(");\n");
				sb.append("                break;\n");
				sb.append("            }\n");
			}
		}

		/*   */
		sb.append("           default :\n");
		sb.append("           {\n");
		sb.append("           }\n");
		sb.append("        }\n");
		sb.append("        \n");
		sb.append("        return value;\n");
		sb.append("    }\n");
		sb.append("\n");
		sb.append("\n");
		
		return sb.toString();
	}


	/**
	 *
	 */
	private String generateExpression(
		JRExpression expression,
		byte evaluationType
		)
	{
		JRParameter jrParameter = null;
		JRField jrField = null;
		JRVariable jrVariable = null;

		StringBuffer sbuffer = new StringBuffer();

		JRExpressionChunk[] chunks = expression.getChunks();
		JRExpressionChunk chunk = null;
		String chunkText = null;
		if (chunks != null && chunks.length > 0)
		{
			for(int i = 0; i < chunks.length; i++)
			{
				chunk = chunks[i];

				chunkText = chunk.getText();
				if (chunkText == null)
				{
					chunkText = "";
				}
				
				switch (chunk.getType())
				{
					case JRExpressionChunk.TYPE_TEXT :
					{
						sbuffer.append(chunkText);
						break;
					}
					case JRExpressionChunk.TYPE_PARAMETER :
					{
						jrParameter = (JRParameter)parametersMap.get(chunkText);
	
						sbuffer.append("((");
						sbuffer.append(jrParameter.getValueClassName());
						sbuffer.append(")super.parameter_");
						sbuffer.append(JRStringUtil.getLiteral(chunkText));
						sbuffer.append(".getValue())");
	
						break;
					}
					case JRExpressionChunk.TYPE_FIELD :
					{
						jrField = (JRField)fieldsMap.get(chunkText);
	
						sbuffer.append("((");
						sbuffer.append(jrField.getValueClassName());
						sbuffer.append(")super.field_");
						sbuffer.append(JRStringUtil.getLiteral(chunkText));
						sbuffer.append(".get");
						sbuffer.append((String)fieldPrefixMap.get(new Byte(evaluationType)));
						sbuffer.append("Value())");
	
						break;
					}
					case JRExpressionChunk.TYPE_VARIABLE :
					{
						jrVariable = (JRVariable)variablesMap.get(chunkText);
	
						sbuffer.append("((");
						sbuffer.append(jrVariable.getValueClassName());
						sbuffer.append(")super.variable_");
						sbuffer.append(JRStringUtil.getLiteral(chunkText));
						sbuffer.append(".get");
						sbuffer.append((String)variablePrefixMap.get(new Byte(evaluationType)));
						sbuffer.append("Value())");
	
						break;
					}
					case JRExpressionChunk.TYPE_RESOURCE :
					{
						jrParameter = (JRParameter)parametersMap.get(chunkText);
	
						sbuffer.append("super.evaluator.str(\"");
						sbuffer.append(chunkText);
						sbuffer.append("\")");
	
						break;
					}
				}
			}
		}
		
		if (sbuffer.length() == 0)
		{
			sbuffer.append("null");
		}

		return sbuffer.toString();
	}
}
