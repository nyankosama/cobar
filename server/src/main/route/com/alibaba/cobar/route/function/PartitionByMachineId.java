package com.alibaba.cobar.route.function;

import com.alibaba.cobar.CobarServer;
import com.alibaba.cobar.config.model.rule.RuleAlgorithm;
import com.alibaba.cobar.parser.ast.expression.Expression;
import com.alibaba.cobar.parser.ast.expression.primary.function.FunctionExpression;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by hlr@superid.cn on 2014/9/12.
 */
public class PartitionByMachineId extends FunctionExpression implements RuleAlgorithm {

    private Map<String, Map<Integer, Integer>> routeTableIndex;

    private static final Logger LOGGER = Logger.getLogger(PartitionByMachineId.class);

    public PartitionByMachineId(String functionName, List<Expression> arguments) {
        super(functionName, arguments);
    }

    public PartitionByMachineId(String functionName){
        this(functionName, null);
    }

    @Override
    public Object evaluationInternal(Map<? extends Object, ? extends Object> parameters) {
        return calculate(parameters)[0];
    }

    @Override
    public Integer[] calculate(Map<? extends Object, ? extends Object> parameters) {
        String tableName = (String) parameters.get("TABLE_NAME");
        Map<Integer, Integer> tableIndex = routeTableIndex.get(tableName);
        String arg = (String) arguments.get(0).evaluation(parameters);
        try {
            byte[] bytes = arg.getBytes("utf-8");
            int machineId = getMachineId(bytes);
            return new Integer[]{tableIndex.get(machineId)};
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            LOGGER.error("machine id encode does not be supported!");
        }
        return null;
    }

    private int getMachineId(byte[] bytes){
        byte highByte = (byte) (bytes[2]  & 0xFF);
        byte lowByte = (byte) (bytes[3] >>> 4);
        return (highByte << 4) & 0xFF |
                lowByte & 0xFF;
    }

    @Override
    public FunctionExpression constructFunction(List<Expression> arguments) {
        if (arguments == null || arguments.size() != 1)
            throw new IllegalArgumentException("function " + getFunctionName() + " must have 1 argument but is "
                    + arguments);
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
        PartitionByMachineId partitionFunc = new PartitionByMachineId(functionName, args);
        partitionFunc.routeTableIndex = CobarServer.getInstance().getConfig().getRouteTableIndex();
        return partitionFunc;
    }

    @Override
    public void initialize() {
        init();
    }

    @Override
    public void init(){
        //TODO
    }
}
