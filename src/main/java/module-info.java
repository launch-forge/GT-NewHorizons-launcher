
module zi.zircky.gtnhlauncher {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.desktop;
  requires scribejava.core;
  requires scribejava.apis;
  requires org.slf4j;
  requires java.net.http;
  requires com.fasterxml.jackson.databind;
  requires jdk.httpserver;
  requires com.google.gson;
  requires java.logging;
  requires jdk.management;
  requires java.compiler;
  requires static lombok;
  requires org.json;

  opens zi.zircky.gtnhlauncher.auth to com.google.gson;
  opens zi.zircky.gtnhlauncher.service.settings to com.google.gson;

  opens zi.zircky.gtnhlauncher to javafx.fxml;
  exports zi.zircky.gtnhlauncher;
  exports zi.zircky.gtnhlauncher.controller;
  opens zi.zircky.gtnhlauncher.controller to javafx.fxml;
  exports zi.zircky.gtnhlauncher.service.settings.versionJava;
  opens zi.zircky.gtnhlauncher.service.settings.versionJava to javafx.fxml;
}
