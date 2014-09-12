package com.alibaba.cobar.route.function;

import com.alibaba.cobar.config.model.rule.RuleAlgorithm;
import com.alibaba.cobar.parser.ast.expression.Expression;
import com.alibaba.cobar.parser.ast.expression.primary.function.FunctionExpression;

import java.util.List;
import java.util.Map;

/**
 * Created by hlr@superid.cn on 2014/9/12.
 */
public class PartitionByMachineId extends FunctionExpression implements RuleAlgorithm {
    
    public PartitionByMachineId(String functionName, List<Expression> arguments) {
        super(functionName, arguments);
    }

    @Override
    public FunctionExpression constructFunction(List<Expression> arguments) {
        return null;
    }

    @Override
    public RuleAlgorithm constructMe(Object... objects) {
        return null;
    }

    @Override
    public void initialize() {

    }

    @Override
    public Integer[] calculate(Map<? extends Object, ? extends Object> parameters) {
        return new Integer[0];
    }
}
