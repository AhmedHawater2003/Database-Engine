package sql;

import app.DBApp;
import app.SQLTerm;
import exceptions.DBAppException;
import sql.antlrGenFiles.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

public class DBListener extends SQLiteParserBaseListener {
    private final DBApp app;
    private Iterator result = null;

    final String INTEGER_DATA_TYPE_NAME = "java.lang.Integer";
    final String STRING_DATA_TYPE_NAME = "java.lang.String";
    final String DOUBLE_DATA_TYPE_NAME = "java.lang.Double";

    public DBListener(DBApp app) {
        this.app = app;
    }

    public Iterator getResult() {
        return result;
    }

    public void enterCreate_table_stmt(SQLiteParser.Create_table_stmtContext ctx) {
        System.out.println("Create table statement");
        String tableName = ctx.table_name().getText();
        String clusteringKey = null;
        Hashtable<String,String>colNameType = new Hashtable<>();
        for(SQLiteParser.Column_defContext col : ctx.column_def()){
            String colName = col.column_name().getText();
            String colType = getDataType(col.type_name());
            colNameType.put(colName,colType);
            if(col.column_constraint().size()>0 && col.column_constraint(0).getText().equalsIgnoreCase("primarykey")) {
                clusteringKey = colName;
            }
        }

        //TODO: check if clustering key is in the table constraints
//        if(ctx.table_constraint().size()>0 && ctx.table_constraint(0).){
//           clusteringKey = ctx.table_constraint(0).column_name().getText();
//        }

        try {
            app.createTable(tableName,clusteringKey,colNameType);
        } catch (Exception e) {
            e.printStackTrace();
        }

        result=null;
    }

    private String getDataType(SQLiteParser.Type_nameContext typeNameContext) {
        String type = typeNameContext.getText().trim().toLowerCase();
        String res = "";
        if (type.equals("int"))
            res = INTEGER_DATA_TYPE_NAME;
        else if (type.contains("varchar"))
            res = STRING_DATA_TYPE_NAME;
        else if (type.equals("float") || type.equals("double") || type.contains("decimal"))
            res = DOUBLE_DATA_TYPE_NAME;

        return res;
    }

    public void enterCreate_index_stmt(SQLiteParser.Create_index_stmtContext ctx) {
        System.out.println("Create index statement");

        String tableName = ctx.table_name().getText();
        String indexName = ctx.index_name().getText();
        String colName = ctx.indexed_column(0).getText();
        try {
            app.createIndex(tableName,colName,indexName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void enterUpdate_stmt(SQLiteParser.Update_stmtContext ctx) {
        System.out.println("Update statement");
        String tableName = ctx.qualified_table_name().getText();
        Hashtable<String,Object>colVal = new Hashtable<>();
        ArrayList<String> colNames = new ArrayList<>();

        int primaryKeyValueIdx = ctx.column_name().size();
        String primaryKeyValue = (String) ctx.expr(primaryKeyValueIdx).expr(1).getText();

        for (SQLiteParser.Column_nameContext col : ctx.column_name())
            colNames.add(col.getText());


        for (int i = 0; i < primaryKeyValueIdx; i++)
            colVal.put(colNames.get(i), parseObject(ctx.expr(i).getText()));
        //TODO: use CSVReader to get the column type

        try{
            app.updateTable(tableName,primaryKeyValue,colVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
        result = null;
    }

    public void enterInsert_stmt(SQLiteParser.Insert_stmtContext ctx) {
        System.out.println("Insert statement");
        String tableName = ctx.table_name().getText();

        for(int i=0;i<ctx.values_clause().value_row().size();i++){

            Hashtable<String,Object>colNameValue = new Hashtable<>();

//        if(ctx.column_name().size()!=ctx.values_clause().value_row(0).expr().size()){
//            throw new DBAppException("Number of columns doesn't match number of values");
//        }
            //TODO: check if the column names match the values

            for(int j=0;j<ctx.column_name().size();j++){
                String colName = ctx.column_name(j).getText();
                Object colValue = parseObject(ctx.values_clause().value_row(i).expr(j).getText());
                colNameValue.put(colName,colValue);
            }

            try {
                app.insertIntoTable(tableName,colNameValue);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        result=null;
    }

    public void enterDelete_stmt(SQLiteParser.Delete_stmtContext ctx) {
        System.out.println("Delete statement");
        String tableName = ctx.qualified_table_name().getText();
        Hashtable<String, Object> colVal = new Hashtable<>();

        ArrayList<SQLiteParser.ExprContext> conditions = new ArrayList<>();
        parseCondition(conditions, ctx.expr());

        for (SQLiteParser.ExprContext cond : conditions) {
            String col = cond.expr(0).getText();
            //TODO: use CSVReader to get the column type
            Object val = parseObject(cond.expr(1).getText());
            colVal.put(col, val);
        }

        try {
            app.deleteFromTable(tableName,colVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
        result=null;
    }
    public void enterSelect_stmt(SQLiteParser.Select_stmtContext ctx) {
        System.out.println("Select statement");
        ArrayList<SQLiteParser.ExprContext> conditions = new ArrayList<>();
        ArrayList<String> operators = new ArrayList<>();

        String table = ctx.select_core(0).table_or_subquery(0).getText();

        SQLiteParser.ExprContext conditionExpr = ctx.select_core(0).expr().get(0);
        parseWhereCondition(conditions, operators, conditionExpr);

        SQLTerm[] arrSQLTerms;
        try {
            arrSQLTerms = prepareSelectTerm(table, conditions);
        } catch (DBAppException e) {
            throw new RuntimeException(e);
        }
        String[] betweenConditionOperator = operators.toArray(new String[0]);
        try {
            result = app.selectFromTable(arrSQLTerms, betweenConditionOperator);
        } catch (DBAppException | IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Object parseObject(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {}

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {}

        return cleanString(value);
    }
    private boolean hasChild(SQLiteParser.ExprContext ctx) {
        return ctx.AND_()!=null || ctx.OR_()!=null || ctx.XOR_()!=null;
    }
    private void parseWhereCondition(ArrayList<SQLiteParser.ExprContext> conditions, ArrayList<String> operators, SQLiteParser.ExprContext conditionExpr) {

        parseOperation(operators, conditionExpr);
        parseCondition(conditions, conditionExpr);
    }
    private void parseCondition(ArrayList<SQLiteParser.ExprContext> conditions, SQLiteParser.ExprContext expr) {
        if (!hasChild(expr)) {
            conditions.add(expr);
            return;
        }
        for (int i = 0; i < expr.expr().size(); i++)
            parseCondition(conditions, expr.expr(i));
    }
    private int parseOperation(ArrayList<String> operation, SQLiteParser.ExprContext expr) {
        if (!hasChild(expr)) {
            String tmp = CheckANDOrORorXOR(expr);

            if (tmp != null) {
                operation.add(tmp);
                return 1;
            }
            return 0;

        }

        int count = 0;
        for (int i = 0; i < expr.expr().size() && i < 2; i++) {

            parseOperation(operation, expr.expr(i));

            if (i == 0) {
                String tmp = CheckANDOrORorXOR(expr);
                if (tmp != null)
                    operation.add(tmp);
            }
        }
        return count;
    }
    private String CheckANDOrORorXOR(SQLiteParser.ExprContext expr) {
        if (expr.AND_() != null)
            return "AND";
        else if (expr.OR_() != null)
            return "OR";
        else if(expr.XOR_()!=null)
            return "XOR";

        return null;
    }
    private SQLTerm[] prepareSelectTerm(String tableName, ArrayList<SQLiteParser.ExprContext> conditions)
            throws DBAppException {
        SQLTerm[] terms = new SQLTerm[conditions.size()];

        for (int i = 0; i < conditions.size(); i++) {
            String colName = conditions.get(i).expr(0).getText();
            Object value = cleanString(conditions.get(i).expr(1).getText());
            //TODO: use CSVReader to get the column type
            value=parseObject(value.toString());
            String strOperator = chooseInSqlOperator(conditions.get(i));
            terms[i] = new SQLTerm(tableName, colName, strOperator, value);
        }

        return terms;
    }

    private String chooseInSqlOperator(SQLiteParser.ExprContext cond) throws DBAppException {

        if (cond.GT() != null)
            return ">";

        else if (cond.LT() != null)
            return "<";

        else if (cond.EQ() != null || cond.ASSIGN() != null)
            return "=";

        else if (cond.GT_EQ() != null)
            return ">=";

        else if (cond.LT_EQ() != null)
            return "<=";
        else if (cond.NOT_EQ1() != null)
            return "!=";

        throw new DBAppException("not valid operator");
    }

    private Object cleanString(String value) {
        if (value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'')
            return value.substring(1, value.length() - 1);
        return value;
    }




}
