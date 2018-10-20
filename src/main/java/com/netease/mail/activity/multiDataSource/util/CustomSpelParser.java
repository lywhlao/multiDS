package com.netease.mail.activity.multiDataSource.util;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * spel parser
 */
public class CustomSpelParser {
    public static String getDynamicValue(String[] parameterNames, Object[] args, String key) {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        return (String) parser.parseExpression(key).getValue(context, String.class);
    }

    public static void main(String[] args) {
        String dynamicValue = CustomSpelParser.getDynamicValue(new String[]{"a", "b"}, new Object[]{1, 2}, "#a+'_'+#b");
        System.out.println(dynamicValue);
    }
}
