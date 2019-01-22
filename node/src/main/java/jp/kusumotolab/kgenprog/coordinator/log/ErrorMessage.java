package jp.kusumotolab.kgenprog.coordinator.log;

import java.io.PrintWriter;
import java.io.StringWriter;

@SuppressWarnings("unused") class ErrorMessage {

  public String message;
  public String stackTrace;

  public ErrorMessage(Throwable error) {
    message = error.getMessage();

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    error.printStackTrace(printWriter);
    printWriter.flush();
    stackTrace = stringWriter.toString();
  }
}