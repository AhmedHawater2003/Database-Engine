package handlers;

import app.SQLTerm;
import exceptions.DBAppException;
import helpers.MetaDataColumns;
import helpers.MetaDataManger;
import helpers.RecordsFetcher;
import storage.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.List;

public class SelectionHandler {
    private String tableName;
    private HashMap<String, String> columnWithIndex;

    public SelectionHandler(String tableName) throws IOException {
        this.tableName = tableName;
        this.columnWithIndex = new HashMap<>();
        List<List<String>> indexList = MetaDataManger.getInstance().getColumnsWithIndex(tableName);
        for (int i = 0; i < indexList.get(0).size(); i++) {
            this.columnWithIndex.put(indexList.get(0).get(i), indexList.get(1).get(i));
        }
    }

    public TreeSet<Tuple> process(SQLTerm term) throws IOException, ClassNotFoundException, DBAppException {
        TreeSet<Tuple> result = new TreeSet<>();

        if (columnWithIndex.containsKey(term._strColumnName))
            result = RecordsFetcher.fetchWithIndex(term,columnWithIndex.get(term._strColumnName) );
        else
            result = RecordsFetcher.fetchWithoutIndex(term);
        return result;
    }

    public TreeSet<Tuple> process(ArrayList<Object> operands, SQLOperator operator) throws IOException, ClassNotFoundException, DBAppException {
        TreeSet<Tuple> result = new TreeSet<>();
        if (operator == SQLOperator.AND) {
            result = processAND(operands.stream().map(operand -> (SQLTerm) operand).toList());
        } else if (operator == SQLOperator.OR) {
            result = processOR(operands);
        } else if (operator == SQLOperator.XOR) {
            result = processXOR(operands);
        }
        //TODO: else throw exception
        return result;
    }

    public TreeSet<Tuple> processAND(List<SQLTerm> operands) throws IOException, ClassNotFoundException, DBAppException {
        TreeSet<Tuple> result = new TreeSet<>();
        if (operands.size() == 0) throw new DBAppException("AND operator should have at least one operand");
        SQLTerm termWithIndex = null;
        for (SQLTerm term : operands) {
            if (columnWithIndex.containsKey(term._strColumnName)) {
                termWithIndex = term;
                break;
            }
        }
        //TODO: index naming convention missing
        if (termWithIndex != null)
            result = RecordsFetcher.fetchWithIndex(termWithIndex,columnWithIndex.get(termWithIndex._strColumnName) );
        else result = RecordsFetcher.fetchWithoutIndex(operands.get(0));

        for(int i = 1; i < operands.size(); i++){
            result=filterANDedResult(result, operands.get(i));
        }
        return result;
    }

    public TreeSet<Tuple> filterANDedResult(TreeSet<Tuple> result, SQLTerm term) {
        TreeSet<Tuple> temp = new TreeSet<>();
        for(Tuple tuple : result){
            if(checkTerm(tuple,term)){
                temp.add(tuple);
            }
        }
        return temp;
    }

    public boolean checkTerm(Tuple tuple, SQLTerm term){
        //TODO validate column existance
        Comparable tupleVal=(Comparable) tuple.getContent().get(term._strColumnName);
        Comparable termVal=(Comparable) term._objValue;

        switch (term._strOperator) {
            case "=" -> {
                return tupleVal.compareTo(termVal) == 0;
            }
            case "!=" -> {
                return tupleVal.compareTo(termVal) != 0;
            }
            case ">" -> {
                return tupleVal.compareTo(termVal) > 0;
            }
            case "<" -> {
                return tupleVal.compareTo(termVal) < 0;
            }
            case ">=" -> {
                return tupleVal.compareTo(termVal) >= 0;
            }
            case "<=" -> {
                return tupleVal.compareTo(termVal) <= 0;
            }
        }
        return false;
    }

    public TreeSet<Tuple> processOR(ArrayList<Object> operands) throws IOException, ClassNotFoundException, DBAppException {
        TreeSet<Tuple> result = new TreeSet<>();
        for (Object operand : operands) {
            if (operand instanceof SQLTerm term) {
                if (columnWithIndex.containsKey(term._strColumnName))
                    result.addAll(RecordsFetcher.fetchWithIndex(term,columnWithIndex.get(term._strColumnName)));
                else
                    result.addAll(RecordsFetcher.fetchWithoutIndex(term));
            } else result.addAll((TreeSet<Tuple>) operand);
        }
        return result;
    }

    public TreeSet<Tuple> processXOR(ArrayList<Object> operands) throws IOException, ClassNotFoundException, DBAppException {
        TreeSet<Tuple> result = new TreeSet<>();
        for (Object operand : operands) {
            if (operand instanceof SQLTerm term) {
                if (columnWithIndex.containsKey(term._strColumnName)) {
                    XORHelper(result, RecordsFetcher.fetchWithIndex(term,columnWithIndex.get(term._strColumnName)));
                } else
                    XORHelper(result, RecordsFetcher.fetchWithoutIndex(term));
            } else XORHelper(result, (TreeSet<Tuple>) operand);
        }
        return result;
    }

    public void XORHelper(TreeSet<Tuple> result, TreeSet<Tuple> set) {
        for (Tuple tuple : set) {
            if (!result.contains(tuple))
                result.add(tuple);

            else
                result.remove(tuple);
        }
    }

}
