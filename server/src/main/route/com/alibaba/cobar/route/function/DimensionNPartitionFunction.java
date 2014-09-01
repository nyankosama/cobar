package com.alibaba.cobar.route.function;

import com.alibaba.cobar.config.model.rule.RuleAlgorithm;
import com.alibaba.cobar.parser.ast.expression.Expression;
import com.alibaba.cobar.parser.ast.expression.primary.function.FunctionExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by hlr@superid.cn on 2014/9/1.
 */
public class DimensionNPartitionFunction extends FunctionExpression implements RuleAlgorithm{

    public DimensionNPartitionFunction(String functionName){
        super(functionName, null);
    }

    public DimensionNPartitionFunction(String functionName, List<Expression> arguments) {
        super(functionName, arguments);
    }

    @Override
    public Object evaluationInternal(Map<? extends Object, ? extends Object> parameters) {
        return calculate(parameters);
    }

    @Override
    public FunctionExpression constructFunction(List<Expression> arguments) {
        Object[] args = new Object[arguments.size()];
        int i = -1;
        for (Expression arg : arguments) {
            args[++i] = arg;
        }
        return (FunctionExpression) constructMe(args);
    }

    @Override
    public RuleAlgorithm constructMe(Object... objects) {
        List<Expression> args = new ArrayList<Expression>(objects.length);
        for (Object obj : objects) {
            args.add((Expression) obj);
        }
        DimensionNPartitionFunction func = new DimensionNPartitionFunction(functionName, args);
        //TODO 从prototype中初始化DimemsionNPartitionFunction的相关参数
        return func;
    }

    @Override
    public void init() {
        initialize();
    }

    @Override
    public void initialize() {
        //TODO 初始化多维值空间的初始index
    }

    @Override
    public Integer[] calculate(Map<? extends Object, ? extends Object> parameters) {
        int mapSize = parameters.size();
        ArrayList<Object> args = new ArrayList<Object>(mapSize);
        for (int i = 0; i < mapSize; i++){
            args.add(parameters.get(i));
        }
        return new Integer[0];
    }
}
