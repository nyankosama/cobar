package com.alibaba.cobar.route.function;

import com.alibaba.cobar.config.model.rule.RuleAlgorithm;
import com.alibaba.cobar.parser.ast.expression.Expression;
import com.alibaba.cobar.parser.ast.expression.primary.function.FunctionExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by hlr@superid.cn on 2014/9/1.
 */
public class TestFunction extends FunctionExpression implements RuleAlgorithm {

    public TestFunction(String functionName){
        super(functionName, null);
    }

    public TestFunction(String functionName, List<Expression> arguments) {
        super(functionName, arguments);
    }

    @Override
    public Object evaluationInternal(Map<? extends Object, ? extends Object> parameters) {
        System.out.println("TestFunction evaluationInternal");
        return calculate(parameters)[0];
    }

    @Override
    public FunctionExpression constructFunction(List<Expression> arguments) {
        System.out.println("TestFunction constructFunction");
        Object[] args = new Object[arguments.size()];
        int i = -1;
        for (Expression arg : arguments) {
            args[++i] = arg;
        }
        return (FunctionExpression) constructMe(args);
    }

    @Override
    public RuleAlgorithm constructMe(Object... objects) {
        System.out.println("TestFunction constructMe");
        List<Expression> args = new ArrayList<Expression>(objects.length);
        for (Object obj : objects) {
            args.add((Expression) obj);
        }
        TestFunction testFunction = new TestFunction(functionName, args);
        return testFunction;
    }

    @Override
    public void initialize() {
        System.out.println("TestFunction init");
    }

    @Override
    public Integer[] calculate(Map<? extends Object, ? extends Object> parameters) {
        System.out.println("TestFunction calculate");
        System.out.println("Map size =" + parameters.size());
        Object arg1 = arguments.get(0).evaluation(parameters);
        Object arg2 = arguments.get(1).evaluation(parameters);
        Number key1 = getNumber(arg1);
        Number key2 = getNumber(arg2);
        Integer[] rst = new Integer[1];
        rst[0] = key1.intValue() + key2.intValue();
        return rst;
    }

    private Number getNumber(Object arg){
        Number key;
        if (arg instanceof Number) {
            key = (Number) arg;
        } else if (arg instanceof String) {
            key = Long.parseLong((String) arg);
        } else {
            throw new IllegalArgumentException("unsupported data type for partition key: " + arg.getClass());
        }
        return key;
    }

}
