package sql;

import app.DBApp;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import sql.antlrGenFiles.*;

import java.util.Iterator;

public class SQLParser {
    DBApp app;

    public SQLParser(DBApp app) {
        this.app = app;
    }

    public Iterator parse(StringBuffer input) {

        CharStream stream = CharStreams.fromString(input.toString());
        SQLiteLexer lexer = new SQLiteLexer(stream);
        CommonTokenStream token = new CommonTokenStream(lexer);
        SQLiteParser parser = new SQLiteParser(token);
        ParseTree tree = parser.parse();
        DBListener listener = new DBListener(app);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);
//        ParseTreeWalker.DEFAULT.walk(listener, tree);
        return listener.getResult();
    }
}