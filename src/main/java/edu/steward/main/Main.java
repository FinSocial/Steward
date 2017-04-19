package edu.steward.main;


import edu.steward.mock.GetGraphDataMock;
import edu.steward.mock.StockMock;
import edu.steward.stock.api.AlphaVantageAPI;
import edu.steward.stock.api.AlphaVantageConstants;
import com.google.common.collect.ImmutableList;
import edu.steward.analytics.SentimentAnalysis;
import edu.steward.analytics.TwitterSentiments;
import edu.steward.login.LoginConfigFactory;
import edu.steward.mock.GetStockDataMock;
import edu.steward.user.UserSession;
import freemarker.template.Configuration;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.pac4j.core.config.Config;
import org.pac4j.sparkjava.CallbackRoute;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.freemarker.FreeMarkerEngine;
import java.io.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.pac4j.sparkjava.SecurityFilter;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.post;


public class Main {
  private static final int DEFAULT_PORT = 4567;

  public static void main(String[] args) {
//    System.out.println(TwitterSentiments.sentiments(ImmutableList.<String>of
//            ("Trump", "Syria")));
    new Main(args).run();
  }

  private String[] args;

  private Main(String[] args) {
    this.args = args;
  }

  private void run() {
    // Parse command line arguments

    AlphaVantageAPI api = new AlphaVantageAPI();

    String k =
    api.getFromAlphaVantage(
            AlphaVantageConstants.FUNCTION.TIME_SERIES_DAILY,
            AlphaVantageConstants.SYMBOL.MSFT,
//            AlphaVantageConstants.INTERVAL.FIFTEEN_MIN,
            AlphaVantageConstants.OUTPUT_SIZE.COMPACT,
            AlphaVantageConstants.APIKEY.APIKEY);

    System.out.println(k);

    OptionParser parser = new OptionParser();
    parser.accepts("gui");
    parser.accepts("port").withRequiredArg().ofType(Integer.class)
            .defaultsTo(DEFAULT_PORT);
    OptionSet options = parser.parse(args);


    if (options.has("gui")) {
      runSparkServer((Integer) options.valueOf("port"));
    }
  }

  private static FreeMarkerEngine createEngine() {
    Configuration config = new Configuration();
    File templates =
            new File("src/main/resources/spark/template/freemarker");
    try {
      config.setDirectoryForTemplateLoading(templates);
    } catch (IOException ioe) {
      System.out.printf("ERROR: Unable use %s for template loading.%n",
              templates);
      System.exit(1);
    }
    return new FreeMarkerEngine(config);
  }

  private void runSparkServer(int port) {
    Spark.port(port);
    Spark.externalStaticFileLocation("src/main/resources/static");
    Spark.exception(Exception.class, new ExceptionPrinter());
    final Config config = new LoginConfigFactory().build();
    FreeMarkerEngine freeMarker = createEngine();
    final CallbackRoute callback = new CallbackRoute(config, null, true);

    // Todo: Set up Spark handlers
    Spark.post("/getStockData", new GetGraphDataMock());
    Spark.get("/stock/:ticker", new StockMock(), freeMarker);
    Spark.post("/getStockData", new GetStockDataMock());
    get("/callback", callback);
    post("/callback", callback);
    before("/google", new SecurityFilter(config,
        "GoogleClient"));
    get("/google", (req,res) -> {return UserSession.destPage(req,res);});
  }

  private static class ExceptionPrinter implements ExceptionHandler {
    @Override
    public void handle(Exception e, Request req, Response res) {
      res.status(500);
      StringWriter stacktrace = new StringWriter();
      try (PrintWriter pw = new PrintWriter(stacktrace)) {
        pw.println("<pre>");
        e.printStackTrace(pw);
        pw.println("</pre>");
      }
      res.body(stacktrace.toString());
    }
  }
}